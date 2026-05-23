/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Base class for syslog handlers (UDP, TCP, TLS).
 * <p>
 * Provides RFC 5424 message formatting and centralized reconnection logic.
 * Subclasses only need to implement the transport:
 * {@link #isConnected()}, {@link #connect()}, {@link #send(byte[])},
 * {@link #close()}.
 * <p>
 * Reconnection behaviour: if the connection is lost, the handler will
 * attempt to reconnect at most once every {@link #RECONNECT_DELAY_MILLIS}
 * milliseconds. Messages arriving while disconnected are silently dropped
 * with an {@link ErrorManager} notification — they never block the caller
 * beyond the formatting cost.
 *
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public abstract class AbstractSyslogHandler extends Handler {

    private String prefix;
    private SimpleDateFormat smf;

    /** Minimum interval between reconnection attempts. */
    protected static final long RECONNECT_DELAY_MILLIS = 10_000;

    private final Object lock = new Object();
    private long lastConnectAttempt = 0;
    private boolean connectFailed = false;

    // ── Init ───────────────────────────────────────────────────────

    public void init(String prefix) {
        smf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        this.prefix = prefix;
    }

    // ── Publish ────────────────────────────────────────────────────

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        try {
            ensureConnected();

            byte[] payload = buildPayload(record);
            send(payload);
        } catch (IOException e) {
            reportError("Publish failed", e, ErrorManager.WRITE_FAILURE);
        }
    }

    // ── Reconnection logic (centralized) ───────────────────────────

    /**
     * Ensures the connection is established. If disconnected, attempts
     * to reconnect — but at most once per {@link #RECONNECT_DELAY_MILLIS}
     * to avoid hammering the server.
     * <p>
     * Subclasses that handle connections in a background thread
     * (e.g. TCP with async buffer) should override this to be a no-op.
     */
    protected void ensureConnected() throws IOException {
        synchronized (lock) {
            if (isConnected()) {
                connectFailed = false;
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastConnectAttempt < RECONNECT_DELAY_MILLIS) {
                if (connectFailed) {
                    throw new IOException("Syslog connection unavailable, "
                            + "next retry in "
                            + (RECONNECT_DELAY_MILLIS - (now - lastConnectAttempt)) + " ms");
                }
            }

            lastConnectAttempt = now;
            try {
                connect();
                connectFailed = false;
            } catch (IOException e) {
                connectFailed = true;
                throw e;
            }
        }
    }

    /**
     * Resets the reconnection delay so the next call will attempt
     * immediately. Useful when external conditions change.
     */
    protected void resetReconnectDelay() {
        synchronized (lock) {
            lastConnectAttempt = 0;
            connectFailed = false;
        }
    }

    // ── Abstract transport methods ─────────────────────────────────

    protected abstract boolean isConnected();

    public abstract void connect() throws IOException;

    protected abstract void send(byte[] data) throws IOException;

    // ── Message formatting (protected for subclass access) ─────────

    protected byte[] buildPayload(LogRecord record) {
        String header = buildHeader(record);
        String message = formatMessage(record);
        return concat(
                header.getBytes(StandardCharsets.US_ASCII),
                message.getBytes(StandardCharsets.UTF_8)
        );
    }

    protected String buildHeader(LogRecord record) {
        int facility = 16 * 8; // local0
        int severity = getLevelPrio(record);
        String pri = "<" + (facility + severity) + ">";

        return pri + "1 " +
                smf.format(new Date(record.getMillis())) + " " +
                prefix + " PowerFolder " +
                getPID("-") + " - - ";
    }

    protected String formatMessage(LogRecord record) {
        StringBuilder message = new StringBuilder();

        message.append("[")
                .append(getLoggerName(record))
                .append("] ");

        String baseMessage = getFormatter() != null
                ? getFormatter().formatMessage(record)
                : record.getMessage();

        if (baseMessage != null) {
            message.append(baseMessage.replace("\0", ""));
        }

        if (record.getThrown() != null) {
            message.append(System.lineSeparator());
            message.append(stackTraceToString(record.getThrown()));
        }

        return message.toString();
    }

    protected byte[] concat(byte[] headerData, byte[] messageData) {
        int headerLength = headerData.length;
        int messageLength = messageData.length;

        byte[] result = new byte[headerLength + messageLength];
        System.arraycopy(headerData, 0, result, 0, headerLength);
        System.arraycopy(messageData, 0, result, headerLength, messageLength);
        return result;
    }

    // ── Private helpers ────────────────────────────────────────────

    private String getLoggerName(LogRecord record) {
        String loggerName = record.getLoggerName();
        if (loggerName == null || loggerName.isBlank()) {
            return "unknown";
        }

        int pos = loggerName.lastIndexOf('.');
        if (pos >= 0 && pos + 1 < loggerName.length()) {
            loggerName = loggerName.substring(pos + 1);
        }
        return loggerName;
    }

    private int getLevelPrio(LogRecord record) {
        Level lvl = record.getLevel();
        if (lvl.equals(Level.FINER) || lvl.equals(Level.FINEST)) {
            return 7;
        } else if (lvl.equals(Level.FINE) || lvl.equals(Level.CONFIG)) {
            return 6;
        } else if (lvl.equals(Level.INFO)) {
            return 5;
        } else if (lvl.equals(Level.WARNING)) {
            return 4;
        } else if (lvl.equals(Level.SEVERE)) {
            return 3;
        }
        return 7;
    }

    private String stackTraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString().replace("\0", "");
    }

    private String getPID(String fallback) {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        if (jvmName.contains("@")) {
            String[] parts = jvmName.split("@");
            return parts[0];
        }
        return fallback;
    }

    @Override
    public void flush() {
        // subclasses may override if needed
    }

    @Override
    public void close() throws SecurityException {
        // subclasses may override if needed
    }
}
