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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public class UDPSyslogHandler extends AbstractSyslogHandler {

    private DatagramSocket socket;
    private SocketAddress address;

    public void init(String prefix, String host, int port) {
        super.init(prefix);
        this.address = new InetSocketAddress(host, port);
    }

    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public synchronized void connect() throws IOException {
        close();
        socket = new DatagramSocket();
        socket.connect(address);
    }

    @Override
    protected synchronized void send(byte[] data) throws IOException {
        if (!isConnected()) {
            throw new IOException("UDP syslog socket is not connected");
        }
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
    }

    @Override
    public synchronized void close() throws SecurityException {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
