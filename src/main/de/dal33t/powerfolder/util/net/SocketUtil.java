/* $Id: SocketUtil.java,v 1.4 2006/04/14 22:34:35 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.net;

import java.net.Socket;
import java.net.SocketException;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Utility clas to setup a socket
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * 
 * @version $Revision: 1.4 $
 */
public class SocketUtil {
    private final static Logger LOG = Logger.getLogger(SocketUtil.class);
    
    private SocketUtil() {
        // No instance allowed
    }
    
    /**
     * Sets a socket up for use with PowerFolder
     * 
     * @param socket
     *            the Socket to setup
     * @throws SocketException
     */
    public static void setupSocket(Socket socket) throws SocketException {
        Reject.ifNull(socket, "Socket is null");

        //socket.setSoTimeout(Constants.SOCKET_CONNECT_TIMEOUT);
        //socket.setSoLinger(true, 4000);
        //socket.setKeepAlive(true);
        socket.setReceiveBufferSize(1024 * 32);
        socket.setSendBufferSize(1024 * 32);
        // socket.setTcpNoDelay(true);
        LOG.verbose("Socket setup: (" + socket.getSendBufferSize() + "/"
            + socket.getReceiveBufferSize() + "/" + socket.getSoLinger()
            + "ms) " + socket);
    }
}
