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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.UDTSocket;

/**
 * The default factory which creates <code>ConnectionHandler</code>s. Is
 * capable of connecting to a remote node by {@link #tryToConnect(MemberInfo)}
 * or to a remote address by {@link #tryToConnect(InetSocketAddress)}.
 * <p>
 * The connection attempt by {@link #tryToConnect(MemberInfo)} should always be
 * prefered since it include the logical peer address (<code>MemberInfo</code>)
 * of the remote node. Fully relayed connections for exampled don't require a
 * physical TCP address, but require that logical peer address.
 * 
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
     * @param remoteNode
     *            the node to reconnecc tot.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(MemberInfo remoteNode)
        throws ConnectionException
    {
        boolean nullIP = remoteNode.getConnectAddress() == null
            || NetworkUtil
                .isNullIP(remoteNode.getConnectAddress().getAddress());

        if (!nullIP) {
            try {
                return tryToConnectTCP(remoteNode.getConnectAddress());
            } catch (ConnectionException e) {
                logFiner(e);
            }
        }
        try {
            if (useUDTConnections() && !isOnLAN(remoteNode) && !nullIP) {
                return tryToConnectUDTRendezvous(remoteNode);
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
     * @param remoteAddress
     *            the address to connect to
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(InetSocketAddress remoteAddress)
        throws ConnectionException
    {
        if (NetworkUtil.isNullIP(remoteAddress.getAddress())) {
            throw new ConnectionException("Unable to connect to null IP: "
                + remoteAddress);
        }
        return tryToConnectTCP(remoteAddress);
    }

    // Factory methods ********************************************************

    /**
     * Creates a initialized connection handler for a socket based TCP/IP
     * connection.
     * 
     * @param socket
     *            the tcp/ip socket
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public ConnectionHandler createAndInitSocketConnectionHandler(Socket socket)
        throws ConnectionException
    {
        ConnectionHandler conHan = new PlainSocketConnectionHandler(
            getController(), socket);
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
     * @param socket
     *            the UDT socket
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public AbstractUDTSocketConnectionHandler createAndInitUDTSocketConnectionHandler(
        UDTSocket socket) throws ConnectionException
    {
        AbstractUDTSocketConnectionHandler conHan = new PlainUDTSocketConnectionHandler(
            getController(), socket);
        try {
            conHan.init();
        } catch (ConnectionException e) {
            conHan.shutdown();
            throw e;
        }
        return conHan;
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
    protected ConnectionHandler tryToConnectTCP(
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
            return createAndInitSocketConnectionHandler(socket);
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
        if (isFiner()) {
            logFiner("Trying relayed connection to " + remoteNode.nick);
        }
        return getController().getIOProvider().getRelayedConnectionManager()
            .initRelayedConnectionHandler(remoteNode);
    }

    /**
     * Tries to establish a UDT connection in rendezvous mode via relay.
     * 
     * @param remoteNode
     *            the node to connect to
     * @return the ready-initialized connection handler
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectUDTRendezvous(MemberInfo remoteNode)
        throws ConnectionException
    {
        if (isFiner()) {
            logFiner("Trying UDT socket connection to " + remoteNode.nick);
        }
        return getController().getIOProvider().getUDTSocketConnectionManager()
            .initRendezvousUDTConnectionHandler(remoteNode);
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
