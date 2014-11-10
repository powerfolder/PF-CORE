/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 *
 * $Id: ConsoleHandler.java 4734 2008-07-28 03:14:24Z harry $
 */
package de.dal33t.powerfolder.util.logging;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.util.Convert;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class SyslogHandler extends Handler {

    private String prefix;
    private DatagramSocket socket;
    private SocketAddress address;
    SimpleDateFormat smf;

    public void init(String prefix, String host, int port)
        throws SocketException
    {
        socket = new DatagramSocket();
        address = new InetSocketAddress(host, port);
        smf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        socket.connect(address);
        this.prefix = prefix;
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        if (socket == null || address == null) {
            return;
        }
        try {
            StringBuilder header = new StringBuilder();

            int facility = 16 * 8; // 16: local use 0
            int severity = getLevelPrio(record);

            String pri = "<" + Integer.toString(facility + severity) + ">";
            String version = "1";

            header.append(pri);
            header.append(version);
            header.append(" ");
            header.append(smf.format(new Date(record.getMillis())));
            header.append(" ");
            header.append(prefix);
            header.append(" PowerFodler ");
            header.append(getPID("-"));
            header.append(" - - "); // MSGID and STRUCTURED-DATA are NILVALUE i.
                                    // e. not used

            send(header.toString(), record);
        } catch (IOException e) {
            reportError(e.getMessage(), e, ErrorManager.WRITE_FAILURE);
        }
    }

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

    private String getLevelName(LogRecord record) {
        String levelDescription;
        if (record.getLevel().equals(Level.OFF)) {
            levelDescription = "OFF   ";
        } else if (record.getLevel().equals(Level.WARNING)) {
            levelDescription = "WARN  ";
        } else if (record.getLevel().equals(Level.SEVERE)) {
            levelDescription = "SEVERE";
        } else if (record.getLevel().equals(Level.FINE)) {
            levelDescription = "FINE  ";
        } else if (record.getLevel().equals(Level.FINER)) {
            levelDescription = "FINER ";
        } else if (record.getLevel().equals(Level.INFO)) {
            levelDescription = "INFO  ";
        } else {
            levelDescription = record.getLevel().getName();
        }
        return levelDescription;
    }

    private void send(String headerString, LogRecord record) throws IOException
    {
        StringBuilder message = new StringBuilder();
        message.append(" ");
        message.append(getLevelName(record));
        message.append(" [");
        message.append(getLoggerName(record));
        message.append(" ");
        message.append(record.getMessage());

        byte[] headerData = headerString.getBytes(Charset.forName("ASCII"));
        byte[] messageData = message.toString().getBytes(
            Charset.forName(Convert.UTF8.toString()));

        byte[] data = concat(headerData, messageData);
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
    }

    private byte[] concat(byte[] headerData, byte[] messageData) {
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
        socket.close();
    }
}
