/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.ByteSerializer;

/**
 * Handler for relayed connections to other clients. NO encrypted transfer.
 * <p>
 * TRAC #597.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public class PlainSocketConnectionHandler extends
    AbstractSocketConnectionHandler implements ConnectionHandler
{

    /**
     * Builds a new anonymous connection manager for the socket.
     * <p>
     * Should be called from <code>ConnectionHandlerFactory</code> only.
     *
     * @see ConnectionHandlerFactory
     * @param controller
     *            the controller.
     * @param socket
     *            the socket.
     * @throws ConnectionException
     */
    protected PlainSocketConnectionHandler(Controller controller, Socket socket)
    {
        super(controller, socket);
    }

    @Override
    protected Object deserialize(byte[] data, int len)
        throws ClassNotFoundException, ConnectionException
    {
        boolean expectCompressed = !isOnLAN();
        if (getIdentity() != null
            && getIdentity().isUseCompressedStream() != null)
        {
            expectCompressed = getIdentity().isUseCompressedStream();
        }
        try {
            return ByteSerializer.deserializeStatic(data, expectCompressed);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    @Override
    protected byte[] serialize(Message message) throws ConnectionException {
        try {
            boolean compressed = getMyIdentity().isUseCompressedStream();
            ByteSerializer serializer = getSerializer();
            if (serializer == null) {
                throw new IOException("Connection already closed");
            }
            return serializer.serialize(message, compressed, -1);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    @Override
    protected Identity createOwnIdentity() {
        return new Identity(getController(), getController().getMySelf()
            .getInfo(), getMyMagicId(), false, false, this);
    }

    // Logger methods *********************************************************

//    public String getLoggerName() {
//        String remoteInfo;
//        if (getSocket() != null) {
//            InetSocketAddress addr = (InetSocketAddress) getSocket()
//                .getRemoteSocketAddress();
//            remoteInfo = addr.getAddress().getHostAddress().replace('.', '_')
//                + "^" + addr.getPort();
//        } else {
//            remoteInfo = "<unknown>";
//        }
//        return super.getLoggerName() + " " + remoteInfo;
//    }
}