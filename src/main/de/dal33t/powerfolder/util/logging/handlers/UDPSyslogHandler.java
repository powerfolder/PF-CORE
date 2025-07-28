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

import de.dal33t.powerfolder.util.logging.handlers.AbstractSyslogHandler;

import java.io.IOException;
import java.net.*;

/**
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public class UDPSyslogHandler extends AbstractSyslogHandler {

    private DatagramSocket socket;
    private SocketAddress address;

    public void init(String prefix, String host, int port) {
        address = new InetSocketAddress(host, port);
        super.init(prefix);
    }
    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    @Override
    public void connect() throws IOException {
        socket = new DatagramSocket();
        socket.connect(address);
    }

    @Override
    protected void send(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);
    }
}
