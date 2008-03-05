/* $Id$
 */
package de.dal33t.powerfolder.net;

import java.io.IOException;
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
        } catch (ConnectionException exSocket) {
            if (useRelayedConnections()) {
                return tryToConnectRelayed(remoteNode);
            }
            throw exSocket;
        }
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
     *            the node to reconnecc tot.
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
            ConnectionHandler handler = createSocketConnectionHandler(
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
     * @return the ready-initalized connection handler
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectRelayed(MemberInfo remoteNode)
        throws ConnectionException
    {
        if (logVerbose) {
            log().verbose("Trying relayed connection to " + remoteNode.nick);
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
     * @return the ready-initalized connection handler
     * @throws ConnectionException
     *             if no connection is possible.
     */
    protected ConnectionHandler tryToConnectUDTSocket(MemberInfo remoteNode)
        throws ConnectionException
    {
        if (logVerbose) {
            log().verbose("Trying UDT socket connection to " + remoteNode.nick);
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
     * Creats a initalized connection handler for a socket based TCP/IP
     * connection.
     * 
     * @param controller
     *            the controller.
     * @param socket
     *            the tcp/ip socket
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public ConnectionHandler createSocketConnectionHandler(
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
     * Constructs a new relayed connection hanlder with the given configuration.
     * ConnectionHandler must not been initalized - That is done later.
     * 
     * @param destination
     *            the destination node
     * @param connectionId
     *            the unique connection id
     * @param relay
     *            the relay to use
     * @return the connection handler.
     */
    public AbstractRelayedConnectionHandler constructRelayedConnectionHandler(
        MemberInfo destination, long connectionId, Member relay)
    {
        return new PlainRelayedConnectionHandler(getController(), destination,
            connectionId, relay);
    }
    
    /**
     * Creates an initalized connection handler for a UDT socket based on UDP
     * connection.
     * 
     * @param controller
     *            the controller.
     * @param socket
     *            the UDT socket
     * @return the connection handler for basic IO connection.
     * @throws ConnectionException
     */
    public ConnectionHandler createUDTSocketConnectionHandler(
        Controller controller, UDTSocket socket) throws ConnectionException
    {
        ConnectionHandler conHan = new PlainUDTSocketConnectionHandler(controller,
            socket);
        try {
            conHan.init();
        } catch (ConnectionException e) {
            conHan.shutdown();
            throw e;
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
}
