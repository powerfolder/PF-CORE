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

    /**
     * Tries establish a physical connection to that node.
     * 
     * @param node
     *            the node to reconnecc tot.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(MemberInfo remoteNode)
        throws ConnectionException
    {
        boolean relayedConsEnabled = ConfigurationEntry.RELAYED_CONNECTIONS_ENABLED
            .getValueBoolean(getController());

        try {
            return tryToConnect(remoteNode.getConnectAddress());
        } catch (ConnectionException e) {
            if (!relayedConsEnabled) {
                throw e;
            }
        }

        log().warn("Tryying relayed connection to " + remoteNode);
        ConnectionHandler conHan = null;
        try {
            conHan = getController().getIOProvider()
                .getRelayedConnectionManager().initRelayedConnectionHandler(
                    remoteNode);
            return conHan;
        } catch (ConnectionException e) {
            log().warn(
                "Unable to open relayed connection to " + remoteNode
                    + ", triing socket connection");
            if (conHan != null) {
                conHan.shutdown();
            }
            throw e;
        }
    }

    /**
     * Tries establish a physical connection to that node.
     * 
     * @param node
     *            the node to reconnecc tot.
     * @return a ready initializes connection handler.
     * @throws ConnectionException
     */
    public ConnectionHandler tryToConnect(InetSocketAddress remoteAddress)
        throws ConnectionException
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
}
