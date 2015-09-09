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
 * The default factory which creates <code>ConnectionHandler</code>s. Is capable
 * of connecting to a remote node by {@link #tryToConnect(MemberInfo)} or to a
 * remote address by {@link #tryToConnect(InetSocketAddress)}.
 * <p>
 * The connection attempt by {@link #tryToConnect(MemberInfo)} should always be
 * prefered since it include the logical peer address (<code>MemberInfo</code>)
 * of the remote node. Fully relayed connections for exampled don't require a
 * physical TCP address, but require that logical peer address.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ConnectionHandlerFactory extends PFComponent {

    public ConnectionHandlerFactory(Controller controller) {
        super(controller);
    }

    // Main connect methods ***************************************************

    /**
     * Tries to establish a connection to that node.
     * <p>
     * Connection strategy when using this method:
     * </p>
     * A) Socket connection
     * <p>
     * B) Relayed connection
     * <p>
     * C) PRO only: HTTP tunneled connection
     *
     * @param remoteNode
     *            the node to reconnect to.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(MemberInfo remoteNode)
        throws ConnectionException
    {
        boolean nullIP = remoteNode.getConnectAddress() == null
            || remoteNode.getConnectAddress().getAddress() == null
            || NetworkUtil
                .isNullIP(remoteNode.getConnectAddress().getAddress());

        if (!nullIP) {
            try {
                ConnectionHandler handler = tryToConnectTCP(remoteNode
                    .getConnectAddress());
                return handler;
            } catch (ConnectionException e) {
                logFiner(e);
            }
        }

        try {
            if (useUDTConnections() && useRelayedTunneledConnection(remoteNode)
                && !nullIP)
            {
                ConnectionHandler handler = tryToConnectUDTRendezvous(remoteNode);
                return handler;
            }
        } catch (ConnectionException e) {
            logFiner(e);
        }

        try {
            if (useRelayedConnections()
                && useRelayedTunneledConnection(remoteNode))
            {
                ConnectionHandler handler = tryToConnectRelayed(remoteNode);
                return handler;
            }
        } catch (ConnectionException e) {
            logFiner(e);
        }
        throw new ConnectionException("No further connection alternatives.");
    }

    /**
     * Tries to establish a connection to that node.
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
    public ConnectionHandler
    tryToConnect(InetSocketAddress remoteAddress)
      throws ConnectionException
    {
      return tryToConnect(remoteAddress, false);
    }

    /** tryToConnect
     * Check address and try to connect via TCP
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  remoteAddress  {@link InetSocketAddress Remote address} to connect to
     * @param  useD2D         Whether to use D2D proto
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return An initialized {@link ConnectionHandler}
     **/

    public ConnectionHandler
    tryToConnect(InetSocketAddress remoteAddress,
      boolean useD2D) throws ConnectionException
    {
        if(NetworkUtil.isNullIP(remoteAddress.getAddress()))
          {
            throw new ConnectionException("Unable to connect to null IP: "
              + remoteAddress);
          }

        return tryToConnectTCP(remoteAddress, useD2D);
    }

    // Factory methods ********************************************************

    /**
     * Creates a initialized connection handler for a socket based TCP/IP
     * connection.
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  socket  The TCP/IP {@link socket}
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return {@link ConnectionHandler} for basic IO connection.
     **/

    public ConnectionHandler createAndInitSocketConnectionHandler(Socket socket)
        throws ConnectionException
    {
      return createAndInitSocketConnectionHandler(socket, false);
    }

    /** createAndInitSocketConnectionHandler
     * Creates a initialized connection handler for a socket based TCP/IP
     * connection.
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  socket  The TCP/IP {@link socket}
     * @param  useD2D  Whether to use D2D proto
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return {@link ConnectionHandler} for basic IO connection.
     **/

    public ConnectionHandler
    createAndInitSocketConnectionHandler(Socket socket,
      boolean useD2D) throws ConnectionException
    {
      ConnectionHandler conHan;

      /* Check which type we need here */
      if(useD2D)
        {
          conHan = new D2DPlainSocketConnectionHandler(
            getController(), socket);
        }
      else conHan = new PlainSocketConnectionHandler(
        getController(), socket);

      /* Finally init this handler */
      try
        {
          conHan.init();
        }
      catch(ConnectionException e)
        {
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
     * Tries to establish a socket connection to that node.
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  remoteAddress  {@link InetSocketAddress Remote address} address to connect to
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return An initialized {@link ConnectionHandler}
     **/

    protected ConnectionHandler
    tryToConnectTCP(InetSocketAddress remoteAddress)
      throws ConnectionException
    {
      return tryToConnectTCP(remoteAddress, false);
    }

    /** tryToConnectTCP
     * Tries to establish a socket connection to given remote address
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  remoteAddress  {@link InetSocketAddress Remote address} address to connect to
     * @param  useD2D         Whether to useD2D proto
     * @throw {@link ConnectionException} Raised when something is wrong
     * @return An initialized {@link ConnectionHandler}
     **/

    protected ConnectionHandler
    tryToConnectTCP(InetSocketAddress remoteAddress,
      boolean useD2D) throws ConnectionException
    {
      try
        {
          Socket socket = new Socket();
          String cfgBind = ConfigurationEntry.NET_BIND_ADDRESS
            .getValue(getController());

          if(!StringUtils.isEmpty(cfgBind))
            socket.bind(new InetSocketAddress(cfgBind, 0));

          socket.connect(remoteAddress, Constants.SOCKET_CONNECT_TIMEOUT);
          NetworkUtil.setupSocket(socket, getController());

          return createAndInitSocketConnectionHandler(socket, useD2D);
        }
      catch(IOException e)
        {
          throw new ConnectionException("Unable to connect to "
            + remoteAddress + ": " + e.getMessage(), e);
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
        return UDTSocket.isSupported()
            && !getController().isLanOnly()
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

    protected boolean useRelayedTunneledConnection(MemberInfo node) {
        boolean onLan = isOnLAN(node);
        if (!onLan) {
            // Always try for Internet connection.
            return true;
        } else {
            // Only if really wanted.
            return ConfigurationEntry.NET_USE_RELAY_TUNNEL_ON_LAN
                .getValueBoolean(getController());
        }
    }

    protected boolean useRelayedTunneledConnection(InetAddress adr) {
        boolean onLan = getController().getNodeManager()
            .isOnLANorConfiguredOnLAN(adr);
        if (!onLan) {
            // Always try for Internet connection.
            return true;
        } else {
            // Only if really wanted.
            return ConfigurationEntry.NET_USE_RELAY_TUNNEL_ON_LAN
                .getValueBoolean(getController());
        }
    }

    public ConnectionQuality getConnectionQuality() {
        int good = 0;
        int medium = 0;
        int poor = 0;
        for (Member node : getController().getNodeManager().getConnectedNodes())
        {
            ConnectionHandler peer = node.getPeer();
            ConnectionQuality qual = peer != null
                ? peer.getConnectionQuality()
                : null;
            if (qual == null) {
                continue;
            }
            if (qual.equals(ConnectionQuality.GOOD)) {
                good++;
            } else if (qual.equals(ConnectionQuality.MEDIUM)) {
                medium++;
            } else if (qual.equals(ConnectionQuality.POOR)) {
                poor++;
            }
        }

        if (isFiner()) {
            logFiner("Connections ==> good: " + good + ", medium: " + medium
                + ", poor: " + poor);
        }

        if (good == 0 && medium == 0 && poor <= 1) {
            return null;
        }

        if (good > poor && good > medium) {
            return ConnectionQuality.GOOD;
        } else if (medium > poor) {
            return ConnectionQuality.MEDIUM;
        }
        return ConnectionQuality.POOR;
    }
}
