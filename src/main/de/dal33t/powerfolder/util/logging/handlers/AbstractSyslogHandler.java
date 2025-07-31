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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public abstract class AbstractSyslogHandler extends Handler {

    private String prefix;
    private SimpleDateFormat smf;

    public void init(String prefix) {
        smf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        this.prefix = prefix;
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) return;

        try {
            ensureConnected();
            String header = buildHeader(record);
            String message = formatMessage(record);
            send(concat(header.getBytes(StandardCharsets.US_ASCII), message.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            reportError("Publish failed", e, ErrorManager.WRITE_FAILURE);
            attemptReconnect();
        }
    }

    private final Object lock = new Object();
    private long lastConnectAttempt = 0;
    private static final long reconnectDelayMillis = 10_000;

    private void ensureConnected() throws IOException {
        synchronized (lock) {
            if (!isConnected()) {
                if (System.currentTimeMillis() - lastConnectAttempt > reconnectDelayMillis) {
                    connect();
                } else {
                    throw new IOException("Syslog connection unavailable");
                }
            }
            lastConnectAttempt = System.currentTimeMillis();
        }
    }

    private void attemptReconnect() {
        try {
            Thread.sleep(500); // short backoff
            connect();
        } catch (Exception e) {
            // Suppress repeated error spam
        }
    }

    abstract boolean isConnected();

    public abstract void connect() throws IOException;

    private String getLoggerName(LogRecord record) {
        String loggerName = record.getLoggerName();
        int pos = loggerName.lastIndexOf('.');
        if (pos >= 0) {
            loggerName = loggerName.substring(pos + 1, loggerName.length());
        }
        return loggerName;
    }

    private int getLevelPrio(LogRecord record) {
        Level lvl = record.getLevel();
        if (lvl.equals(Level.FINER)) {
            return 7;
        } else if (lvl.equals(Level.FINE)) {
            return 6;
        } else if (lvl.equals(Level.INFO)) {
            return 5;
        } else if (lvl.equals(Level.WARNING)) {
            return 4;
        } else if (lvl.equals(Level.SEVERE)) {
            return 3;
        }
        // defaults to debug logging
        return 7;
    }

    private String buildHeader(LogRecord record) {
        int facility = 16 * 8; // local0
        int severity = getLevelPrio(record);
        String pri = "<" + (facility + severity) + ">";
        return pri + "1 " + smf.format(new Date(record.getMillis())) + " " +
                prefix + " PowerFolder " + getPID("-") + " - - ";
    }

    private String formatMessage(LogRecord record) {
        StringBuilder message = new StringBuilder();
        message.append("[").append(getLoggerName(record)).append("] ");
        message.append(getFormatter() != null ? getFormatter().formatMessage(record) : record.getMessage());
        return message.toString();
    }

    protected abstract void send(byte[] data) throws IOException;

    protected byte[] concat(byte[] headerData, byte[] messageData) {
        int headerLength = headerData.length;
        int bataLength = messageData.length;

        byte[] result = new byte[headerLength + bataLength];

        System.arraycopy(headerData, 0, result, 0, headerLength);
        System.arraycopy(messageData, 0, result, headerLength, bataLength);
        return result;
    }

    private String getPID(String fallback) {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();

        if (jvmName.contains("@")) {
            String[] parts = jvmName.split("@");
            return parts[0];
        } else {
            return fallback;
        }
    }

    @Override
    public void flush() {
        // all data is written directly, no need to flush.
    }

    @Override
    public void close() throws SecurityException {
    }
}
