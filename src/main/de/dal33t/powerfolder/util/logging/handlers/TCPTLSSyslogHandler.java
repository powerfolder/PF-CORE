/*
 * Copyright 2004 - 2025 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.util.logging.handlers;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;

/**
 * TCP und TLS-fähiger SyslogHandler für java.util.logging.
 * <p>
 * Fully non-blocking for application threads: {@link #publish(LogRecord)}
 * formats the message and enqueues it into a bounded buffer. A single
 * background daemon thread handles connecting, reconnecting, and sending.
 * <p>
 * Connection and reconnection are managed by the drain thread using
 * {@link AbstractSyslogHandler#ensureConnected()} with its built-in
 * rate limiting. The application thread never touches the network.
 *
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public class TCPTLSSyslogHandler extends AbstractSyslogHandler {

    private static final int DEFAULT_BUFFER_CAPACITY = 10_000;

    private String host;
    private int port;
    private boolean useTLS;
    private int bufferCapacity;

    private Socket socket;
    private OutputStream outputStream;

    private BlockingQueue<byte[]> messageBuffer;
    private Thread drainThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong droppedMessages = new AtomicLong(0);

    // ── Init ───────────────────────────────────────────────────────

    public void init(String prefix, String host, int port, boolean useTLS) {
        init(prefix, host, port, useTLS, DEFAULT_BUFFER_CAPACITY);
    }

    public void init(String prefix, String host, int port, boolean useTLS, int bufferCapacity) {
        super.init(prefix);
        this.host = host;
        this.port = port;
        this.useTLS = useTLS;
        this.bufferCapacity = bufferCapacity;
        this.messageBuffer = new ArrayBlockingQueue<>(bufferCapacity);
        startDrainThread();
    }

    // ── Publish: non-blocking, bypasses base class ensureConnected ─

    /**
     * Overrides base class to avoid any network I/O on the caller thread.
     * Formats the record and enqueues the payload for async delivery.
     */
    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        try {
            byte[] payload = buildPayload(record);
            enqueue(payload);
        } catch (Exception e) {
            reportError("Failed to enqueue syslog message",
                    e, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * No-op: the drain thread calls {@code super.ensureConnected()} itself.
     * This override prevents the base class {@code publish()} path from
     * ever being used to connect on an application thread.
     */
    @Override
    protected void ensureConnected() {
        // connection managed by drain thread only
    }

    // ── Buffer ─────────────────────────────────────────────────────

    private void enqueue(byte[] data) {
        boolean accepted = messageBuffer.offer(data);
        if (!accepted) {
            long dropped = droppedMessages.incrementAndGet();
            reportError(
                    "Syslog buffer full (" + bufferCapacity + ") for "
                            + host + ":" + port + ", message dropped. Total dropped: " + dropped,
                    null, ErrorManager.WRITE_FAILURE);
        }
    }

    // ── Connection ─────────────────────────────────────────────────

    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void connect() throws IOException {
        closeSocket();
        String target = host + ":" + port + " (TLS=" + useTLS + ")";
        try {
            if (useTLS) {
                Socket plainSocket = new Socket();
                plainSocket.connect(new InetSocketAddress(host, port), 5000);
                plainSocket.setSoTimeout(5000);

                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(plainSocket, host, port, true);
                SSLSocket sslSocket = (SSLSocket) socket;
                sslSocket.setSoTimeout(5000);

                try {
                    sslSocket.startHandshake();
                } catch (IOException e) {
                    reportTLSFailure(sslSocket, e);
                    closeSocket();
                    throw e;
                }
                logTLSSuccess(sslSocket);
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(5000);
            }
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new IOException("Syslog connect failed to " + target
                    + ": " + e.getMessage(), e);
        }
    }

    // ── Send ───────────────────────────────────────────────────────

    /**
     * Required by abstract parent. Only called from drain thread via
     * {@link #sendToSocket(byte[])}.
     */
    @Override
    protected void send(byte[] data) throws IOException {
        sendToSocket(data);
    }

    private void sendToSocket(byte[] data) throws IOException {
        OutputStream out = outputStream;
        if (out != null) {
            out.write(data);
            out.write('\n');
            out.flush();
        } else {
            throw new IOException("No open Syslog output stream to " + host + ":" + port);
        }
    }

    // ── Drain thread ───────────────────────────────────────────────

    private void startDrainThread() {
        running.set(true);
        drainThread = new Thread(this::drainLoop, "SyslogDrain-" + host + ":" + port);
        drainThread.setDaemon(true);
        drainThread.start();
    }

    private void drainLoop() {
        while (running.get()) {
            try {
                byte[] message = messageBuffer.poll(1, TimeUnit.SECONDS);
                if (message == null) {
                    continue;
                }

                // Use the base class reconnection logic (rate-limited)
                super.ensureConnected();
                sendToSocket(message);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                reportError("Syslog send failed to " + host + ":" + port
                        + " (TLS=" + useTLS + "), will reconnect: "
                        + e.getMessage(), e, ErrorManager.WRITE_FAILURE);
                closeSocket();
            }
        }
        drainRemaining();
    }

    private void drainRemaining() {
        byte[] message;
        while ((message = messageBuffer.poll()) != null) {
            try {
                super.ensureConnected();
                sendToSocket(message);
            } catch (IOException e) {
                reportError("Failed to drain remaining syslog message to "
                        + host + ":" + port, e, ErrorManager.WRITE_FAILURE);
                break;
            }
        }
    }

    // ── Flush / Close ──────────────────────────────────────────────

    @Override
    public void flush() {
        try {
            OutputStream out = outputStream;
            if (out != null) out.flush();
        } catch (IOException e) {
            reportError("Flush failed", e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        running.set(false);
        if (drainThread != null) {
            drainThread.interrupt();
            try {
                drainThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            drainThread = null;
        }
        closeSocket();
    }

    private void closeSocket() {
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException ignore) {}
        try {
            if (socket != null) socket.close();
        } catch (IOException ignore) {}
        outputStream = null;
        socket = null;
    }

    // ── TLS diagnostics ────────────────────────────────────────────

    private void reportTLSFailure(SSLSocket sslSocket, IOException cause) {
        StringBuilder diag = new StringBuilder();
        diag.append("TLS handshake failed to ").append(host).append(':').append(port).append(". ");
        diag.append("Enabled protocols: ")
                .append(String.join(", ", sslSocket.getEnabledProtocols())).append(". ");
        diag.append("Enabled ciphers: ")
                .append(String.join(", ", sslSocket.getEnabledCipherSuites())).append(". ");
        try {
            SSLSession session = sslSocket.getSession();
            if (session != null) {
                diag.append("Negotiated protocol: ").append(session.getProtocol()).append(". ");
                diag.append("Negotiated cipher: ").append(session.getCipherSuite()).append(". ");
            }
        } catch (Exception ignore) {}
        diag.append("Cause: ").append(cause.getMessage());
        reportError(diag.toString(), cause, ErrorManager.OPEN_FAILURE);
    }

    private void logTLSSuccess(SSLSocket sslSocket) {
        try {
            SSLSession session = sslSocket.getSession();
            reportError(
                    "TLS connected to " + host + ":" + port
                            + " | Protocol: " + session.getProtocol()
                            + " | Cipher: " + session.getCipherSuite()
                            + " | Peer: " + session.getPeerPrincipal(),
                    null, ErrorManager.GENERIC_FAILURE);
        } catch (Exception ignore) {}
    }

    // ── Monitoring ─────────────────────────────────────────────────

    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    public int getBufferSize() {
        return messageBuffer.size();
    }
}
