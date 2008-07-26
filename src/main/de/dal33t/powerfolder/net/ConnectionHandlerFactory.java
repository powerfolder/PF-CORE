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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.UDTSocket;

/**
 * The default factory which creates <code>ConnectionHandler</code>s.
 * 
 * @see PlainSocketConnectionHandler
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ConnectionHandlerFactory extends PFComponent {
    public ConnectionHandlerFactory(Controller controller) {
        super(controller);
    }

    // Main connect methods ***************************************************

    /**
     * Tries establish a physical connection to that node.
     * <p>
     * Connection strategy when using this method:
     * </p>
     * A) Socket connection
     * <p>
     * B) Relayed connection
     * <p>
     * B) PRO only: HTTP tunneled connection
     * 
     * @param node
     *            the node to reconnecc tot.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(MemberInfo remoteNode)
        throws ConnectionException
    {
        try {
            return tryToConnectSocket(remoteNode.getConnectAddress());
        } catch (ConnectionException e) {
            logFiner(e);
        }

        try {
            if (useUDTConnections() && !isOnLAN(remoteNode)) {
                return tryToConnectUDTSocket(remoteNode);
            }
        } catch (ConnectionException e) {
            logFiner(e);
        }

        try {
            if (useRelayedConnections() && !isOnLAN(remoteNode)) {
                return tryToConnectRelayed(remoteNode);
            }
        } catch (ConnectionException e) {
            logFiner(e);
        }
        throw new ConnectionException("No further connection alternatives.");
    }

    /**
     * Tries establish a physical connection to that node.
     * <p>
     * Connection strategy when using this method:
     * </p>
     * A) Socket connection
     * <p>
     * B) PRO only: HTTP tunneled connection
     * 
     * @param node
     *            the node to reconnect to.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(InetSocketAddress remoteAddress)
        throws ConnectionException
    {
        return tryToConnectSocket(remoteAddress);
    }

    // Connection layer specific connect methods ******************************

    /**
     * Tries establish a physical socket connection to that node.
     * 
     * @param remoteAddress
     *            the address to connect to.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectSocket(
        InetSocketAddress remoteAddress) throws ConnectionException
    {
        try {
            Socket socket = new Socket();
            String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
                .getValue(getController());
            if (!StringUtils.isEmpty(cfgBind)) {
                socket.bind(new InetSocketAddress(cfgBind, 0));
            }
            socket.connect(remoteAddress, Constants.SOCKET_CONNECT_TIMEOUT);
            NetworkUtil.setupSocket(socket);
            ConnectionHandler handler = createAndInitSocketConnectionHandler(
                getController(), socket);
            return handler;
        } catch (IOException e) {
            throw new ConnectionException("Unable to connect to: "
                + remoteAddress, e);
        }
    }

    /**
     * Tries to establish a relayed connection to that remote node.
     * 
     * @param remoteNode
     *            the node to connect to
     * @return the ready-initialized connection handler
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectRelayed(MemberInfo remoteNode)
        throws ConnectionException
    {
        if (isLogFiner()) {
            logFiner("Trying relayed connection to " + remoteNode.nick);
        }
        ConnectionHandler conHan = null;
        try {
            conHan = getController().getIOProvider()
                .getRelayedConnectionManager().initRelayedConnectionHandler(
                    remoteNode);
            return conHan;
        } catch (ConnectionException e) {
            if (conHan != null) {
                conHan.shutdown();
            }
            throw e;
        }
    }

    /**
     * Tries to establish a relayed connection to that remote node.
     * 
     * @param remoteNode
     *            the node to connect to
     * @return the ready-initialized connection handler
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectUDTSocket(MemberInfo remoteNode)
        throws ConnectionException
    {
        if (isLogFiner()) {
            logFiner("Trying UDT socket connection to " + remoteNode.nick);
        }
        ConnectionHandler conHan = null;
        try {
            conHan = getController().getIOProvider()
                .getUDTSocketConnectionManager().initUDTConnectionHandler(
                    remoteNode);
            return conHan;
        } catch (ConnectionException e) {
            if (conHan != null) {
                conHan.shutdown();
            }
            throw e;
        }
    }

    // Factory methods ********************************************************

    /**
     * Creates a initialized connection handler for a socket based TCP/IP
     * connection.
     * 
     * @param controller
     *            the controller.
     * @param socket
     *            the tcp/ip socket
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public ConnectionHandler createAndInitSocketConnectionHandler(
        Controller controller, Socket socket) throws ConnectionException
    {
        ConnectionHandler conHan = new PlainSocketConnectionHandler(controller,
            socket);
        try {
            conHan.init();
        } catch (ConnectionException e) {
            conHan.shutdown();
            throw e;
        }
        return conHan;
    }

    /**
     * Constructs a new relayed connection handler with the given configuration.
     * ConnectionHandler must not been initialized - That is done later.
     * 
     * @param destination
     *            the destination node
     * @param connectionId
     *            the unique connection id
     * @param relay
     *            the relay to use
     * @return the connection handler.
     */
    public AbstractRelayedConnectionHandler createRelayedConnectionHandler(
        MemberInfo destination, long connectionId, Member relay)
    {
        return new PlainRelayedConnectionHandler(getController(), destination,
            connectionId, relay);
    }

    /**
     * Creates an initialized connection handler for a UDT socket based on UDP
     * connection.
     * 
     * @param controller
     *            the controller.
     * @param socket
     *            the UDT socket
     * @param port
     * @param dest
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public AbstractUDTSocketConnectionHandler createAndInitUDTSocketConnectionHandler(
        Controller controller, UDTSocket socket, MemberInfo dest, int port)
        throws ConnectionException
    {
        AbstractUDTSocketConnectionHandler conHan = new PlainUDTSocketConnectionHandler(
            controller, socket);
        try {
            // In PowerFolder UDT sockets will always rendezvous
            socket.setSoRendezvous(true);
            MemberInfo myInfo = dest.getNode(getController(), true).getInfo();
            logFine(
                "UDT connect to " + dest + " at " + myInfo.getConnectAddress());
            socket.connect(new InetSocketAddress(myInfo.getConnectAddress()
                .getAddress(), port));
            logFine(
                "UDT socket is connected to " + dest + " at "
                    + myInfo.getConnectAddress() + "!!");
            conHan.init();
        } catch (ConnectionException e) {
            logSevere(e);
            conHan.shutdown();
            throw e;
        } catch (IOException e) {
            logSevere(e);
            conHan.shutdown();
            throw new ConnectionException(e);
        }
        return conHan;
    }

    // Internal helper ********************************************************

    protected boolean useRelayedConnections() {
        return !getController().isLanOnly()
            && ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED
                .getValueBoolean(getController())
            && !getController().getIOProvider().getRelayedConnectionManager()
                .isRelay(getController().getMySelf().getInfo());
    }

    protected boolean useUDTConnections() {
        return !getController().isLanOnly()
            && ConfigurationEntry.UDT_CONNECTIONS_ENABLED
                .getValueBoolean(getController())
            && !getController().getIOProvider().getRelayedConnectionManager()
                .isRelay(getController().getMySelf().getInfo());
    }

    protected boolean isOnLAN(MemberInfo node) {
        InetAddress adr = node.getConnectAddress() != null
            && node.getConnectAddress().getAddress() != null ? node
            .getConnectAddress().getAddress() : null;
        if (adr == null) {
            return false;
        }
        return getController().getNodeManager().isOnLANorConfiguredOnLAN(adr);
    }
}
