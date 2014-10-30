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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import de.dal33t.powerfolder.util.StringUtils;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class SyslogHandler extends Handler {

    private String prefix;
    private DatagramSocket socket;
    private SocketAddress address;
    private static ThreadLocal<LoggingFormatter> formatterThreadLocal = new ThreadLocal<LoggingFormatter>()
    {
        protected LoggingFormatter initialValue() {
            return new LoggingFormatter();
        }
    };

    public void init(String prefix, String host, int port)
        throws SocketException
    {
        socket = new DatagramSocket();
        address = new InetSocketAddress(host, port);
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
            String formattedMessage = formatterThreadLocal.get().format(record);
            if (StringUtils.isNotBlank(prefix)) {
                formattedMessage = prefix + " " + formattedMessage;
            }
            byte[] data = formattedMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length,
                address);
            socket.send(packet);
        } catch (IOException e) {
            reportError(e.getMessage(), e, ErrorManager.WRITE_FAILURE);
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
