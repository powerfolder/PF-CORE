/* $Id: SocketUtil.java,v 1.4 2006/04/14 22:34:35 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Utility class for all low level networking stuff.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.4 $
 */
public class NetworkUtil {
    private final static Logger LOG = Logger.getLogger(NetworkUtil.class);
    
    private static final int LAN_SOCKET_BUFFER_SIZE = 64 * 1024;
    private static final int INET_SOCKET_BUFFER_SIZE = 16 * 1024;

    private NetworkUtil() {
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

        boolean onLan = isOnLanOrLoopback(socket.getInetAddress());
        // socket.setSoTimeout(Constants.SOCKET_CONNECT_TIMEOUT);
        // socket.setSoLinger(true, 4000);
        // socket.setKeepAlive(true);

        socket.setReceiveBufferSize(onLan
            ? LAN_SOCKET_BUFFER_SIZE
            : INET_SOCKET_BUFFER_SIZE);
        socket.setSendBufferSize(onLan
            ? LAN_SOCKET_BUFFER_SIZE
            : INET_SOCKET_BUFFER_SIZE);
        // socket.setTcpNoDelay(true);
        LOG.verbose("Socket setup: (" + socket.getSendBufferSize() + "/"
            + socket.getReceiveBufferSize() + "/" + socket.getSoLinger()
            + "ms) " + socket);
    }

    /**
     * @param addr
     *            the address to check
     * @return if the address is on lan or on loopback device
     */
    public static boolean isOnLanOrLoopback(InetAddress addr) {
        Reject.ifNull(addr, "Address is null");
        try {
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || getAllLocalNetworkAddresses().containsKey(addr);
        } catch (SocketException e) {
            return false;
        }
    }

    /**
     * Returns a Map with all detected local IP-addresses as keys and the associated 
     * NetworkInterface as values.
     * 
     * @return
     * @throws SocketException
     */
    public static final Map<InetAddress, NetworkInterface> getAllLocalNetworkAddresses()
        throws SocketException
    {
        Map<InetAddress, NetworkInterface> res = new HashMap<InetAddress, NetworkInterface>();

        for (Enumeration<NetworkInterface> eni = NetworkInterface
            .getNetworkInterfaces(); eni.hasMoreElements();)
        {
            NetworkInterface ni = eni.nextElement();
            for (Enumeration<InetAddress> eia = ni.getInetAddresses(); eia
                .hasMoreElements();)
            {
                res.put(eia.nextElement(), ni);
            }
        }
        return res;
    }
    
    
}
