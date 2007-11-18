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
import de.dal33t.powerfolder.PFComponent;
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
}
