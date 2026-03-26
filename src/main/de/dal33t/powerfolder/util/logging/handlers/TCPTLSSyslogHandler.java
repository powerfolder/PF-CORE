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
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;

/**
 * TCP und TLS-fähiger SyslogHandler für java.util.logging.
 * <p>
 * Uses an internal bounded message buffer so that application threads
 * calling {@link #publish(LogRecord)} never block on TCP I/O.
 * A single background daemon thread drains the buffer and sends
 * messages over the wire.
 * <p>
 * Drop-in replacement alongside {@link UDPSyslogHandler} — both extend
 * {@link AbstractSyslogHandler} and follow the same init/connect/send/close
 * lifecycle.
 *
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public class TCPTLSSyslogHandler extends AbstractSyslogHandler {

    /** Default capacity of the internal message buffer. */
    private static final int DEFAULT_BUFFER_CAPACITY = 10_000;

    /** Max time (ms) the drain thread waits before retrying a failed connection. */
    private static final long RECONNECT_DELAY_MS = 5_000;

    private String host;
    private int port;
    private boolean useTLS;
    private int bufferCapacity;

    private Socket socket;
    private OutputStream outputStream;

    private BlockingQueue<byte[]> messageBuffer;
    private Thread drainThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long droppedMessages = 0;

    // ── Init (mirrors UDPSyslogHandler.init) ───────────────────────

    /**
     * Initialisiere den Handler mit Standard-Puffergröße.
     *
     * @param prefix Präfix für Hostnamen
     * @param host   Zielhost (Syslog-Server)
     * @param port   Zielport (514 = TCP, 6514 = TLS)
     * @param useTLS true für TLS, false für Plain TCP
     */
    public void init(String prefix, String host, int port, boolean useTLS) {
        init(prefix, host, port, useTLS, DEFAULT_BUFFER_CAPACITY);
    }

    /**
     * Initialisiere den Handler mit konfigurierbarer Puffergröße.
     */
    public void init(String prefix, String host, int port, boolean useTLS, int bufferCapacity) {
        super.init(prefix);
        this.host = host;
        this.port = port;
        this.useTLS = useTLS;
        this.bufferCapacity = bufferCapacity;
        this.messageBuffer = new ArrayBlockingQueue<>(bufferCapacity);

        startDrainThread();
    }

    // ── Connection (synchronized like UDPSyslogHandler) ────────────

    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void connect() throws IOException {
        closeSocket();
        String target = host + ":" + port;
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
                    + " (TLS=" + useTLS + "): " + e.getMessage(), e);
        }
    }

    // ── Send: the base class calls this from publish() ─────────────
    //    We override to enqueue instead of writing to the socket
    //    directly, making publish() non-blocking for the caller.

    /**
     * Called by {@link AbstractSyslogHandler#publish(LogRecord)}.
     * Instead of writing to the socket directly (which would block),
     * this enqueues the data into the internal buffer for async delivery.
     */
    @Override
    protected void send(byte[] data) throws IOException {
        boolean accepted = messageBuffer.offer(data);
        if (!accepted) {
            droppedMessages++;
            reportError(
                    "Syslog buffer full (" + bufferCapacity + "), message dropped. "
                            + "Total dropped: " + droppedMessages,
                    null, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Actual TCP write — called only by the drain thread.
     */
    private void sendToSocket(byte[] data) throws IOException {
        OutputStream out = outputStream;
        if (out != null) {
            out.write(data);
            out.write('\n');
            out.flush();
        } else {
            throw new IOException("No open Syslog output stream");
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

                ensureConnected();
                sendToSocket(message);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                reportError("Syslog send failed, will reconnect", e, ErrorManager.WRITE_FAILURE);
                closeSocket();
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        drainRemaining();
    }

    private void ensureConnected() throws IOException {
        if (!isConnected()) {
            connect();
        }
    }

    private void drainRemaining() {
        byte[] message;
        while ((message = messageBuffer.poll()) != null) {
            try {
                ensureConnected();
                sendToSocket(message);
            } catch (IOException e) {
                reportError("Failed to drain remaining message on shutdown",
                        e, ErrorManager.WRITE_FAILURE);
                break;
            }
        }
    }

    // ── Flush / Close (Handler contract) ───────────────────────────

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

    /** Number of messages dropped since init due to full buffer. */
    public long getDroppedMessages() {
        return droppedMessages;
    }

    /** Current number of messages waiting in the buffer. */
    public int getBufferSize() {
        return messageBuffer.size();
    }
}