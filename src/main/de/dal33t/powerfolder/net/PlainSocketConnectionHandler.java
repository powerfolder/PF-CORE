/* $Id: ConnectionHandler.java,v 1.72 2006/04/16 21:39:41 totmacherr Exp $
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
 * Handler for socket connections to other clients messages
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
        try {
            return ByteSerializer.deserializeStatic(data, expectCompressed);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    @Override
    protected byte[] serialize(Message message) throws ConnectionException
    {
        try {
            // Serialize message, don't compress on LAN
            // unless config says otherwise
            boolean compressed = !isOnLAN()
                || (isOnLAN() && getController().useZipOnLan());
            return getSerializer().serialize(message, compressed, -1);
        } catch (IOException e) {
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e)
                .with(this);
        }
    }

    @Override
    protected Identity createOwnIdentity()
    {
        return new Identity(getController(), getController().getMySelf()
            .getInfo(), getMyMagicId(), false, false);
    }

    // Logger methods *********************************************************

    public String getLoggerName() {
        String remoteInfo;
        if (getSocket() != null) {
            InetSocketAddress addr = (InetSocketAddress) getSocket()
                .getRemoteSocketAddress();
            remoteInfo = addr.getAddress().getHostAddress() + "^"
                + addr.getPort();
        } else {
            remoteInfo = "<unknown>";
        }
        return "PSConnectionHandler " + remoteInfo;
    }
}