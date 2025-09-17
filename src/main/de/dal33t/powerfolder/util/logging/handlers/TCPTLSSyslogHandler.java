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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.ErrorManager;

/**
 * TCP und TLS-fähiger SyslogHandler für java.util.logging.
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public class TCPTLSSyslogHandler extends AbstractSyslogHandler {

    private String host;
    private int port;
    private boolean useTLS;

    private Socket socket;
    private OutputStream outputStream;

    /**
     * Initialisiere den Handler.
     * @param prefix Präfix für Hostnamen
     * @param host Zielhost (Syslog-Server)
     * @param port Zielport (514 = TCP, 6514 = TLS)
     * @param useTLS true für TLS, false für Plain TCP
     */
    public void init(String prefix, String host, int port, boolean useTLS) {
        super.init(prefix);
        this.host = host;
        this.port = port;
        this.useTLS = useTLS;
    }

    @Override
    protected boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void connect() throws IOException {
        close();
        if (useTLS) {
            // 1. Erst normalen Socket mit Timeout bauen
            Socket plainSocket = new Socket();
            plainSocket.connect(new InetSocketAddress(host, port), 5000); // 5 Sek Timeout

            // 2. Dann upgraden auf TLS (SSLSocket)
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = factory.createSocket(plainSocket, host, port, true);
            ((SSLSocket) socket).startHandshake();
        } else {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
        }
        outputStream = socket.getOutputStream();
    }

    @Override
    protected void send(byte[] data) throws IOException {
        OutputStream thisOutputStream = outputStream;
        if (thisOutputStream != null) {
            thisOutputStream.write(data);
            thisOutputStream.write('\n');
            thisOutputStream.flush();
        } else {
            throw new IOException("No open Syslog output stream");
        }
    }

    @Override
    public void flush() {
        try {
            if (outputStream != null) outputStream.flush();
        } catch (IOException e) {
            reportError("Flush failed", e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public void close() throws SecurityException {
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException ignore) {}
        try {
            if (socket != null) socket.close();
        } catch (IOException ignore) {}
        outputStream = null;
        socket = null;
    }
}
