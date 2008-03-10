/* $Id: SocketUtil.java,v 1.4 2006/04/14 22:34:35 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
    private static final int LAN_SOCKET_BUFFER_LIMIT = 1024 * 1024;
    private static final int INET_SOCKET_BUFFER_LIMIT = 256 * 1024;

    private static final long CACHE_TIMEOUT = 10 * 1000;

    private static long LAST_CHACHE_UPDATE = 0;
    private static Map<InetAddress, NetworkInterface> LOCAL_NETWORK_ADDRESSES_CACHE;

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
     * Sets a socket up for use with PowerFolder
     * 
     * @param socket
     *            the Socket to setup
     * @throws SocketException
     */
    public static void setupSocket(UDTSocket socket, InetSocketAddress inetSocketAddress) throws IOException {
        Reject.ifNull(socket, "Socket is null");
        
        boolean onLan = inetSocketAddress != null ? isOnLanOrLoopback(inetSocketAddress.getAddress()) : false;

        socket.setSoUDPReceiverBufferSize(onLan
            ? LAN_SOCKET_BUFFER_SIZE
            : INET_SOCKET_BUFFER_SIZE);
        socket.setSoUDPSenderBufferSize(onLan
            ? LAN_SOCKET_BUFFER_SIZE
            : INET_SOCKET_BUFFER_SIZE);
        socket.setSoSenderBufferLimit(onLan
            ? LAN_SOCKET_BUFFER_LIMIT
            : INET_SOCKET_BUFFER_LIMIT);
        socket.setSoReceiverBufferLimit(onLan
            ? LAN_SOCKET_BUFFER_LIMIT
            : INET_SOCKET_BUFFER_LIMIT);
        LOG.verbose("Socket setup: (" + socket.getSoUDPSenderBufferSize() + "/"
            + socket.getSoUDPReceiverBufferSize() + " " + socket);
    }

    /**
     * @param addr
     *            the address to check
     * @return if the address is on lan or on loopback device
     */
    public static boolean isOnLanOrLoopback(InetAddress addr) {
        Reject.ifNull(addr, "Address is null");
        if (!(addr instanceof Inet4Address)) {
        	LOG.warn("Inet6 not supported yet: " + addr);
        }
        try {
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                || getAllLocalNetworkAddressesCached().containsKey(addr);
        } catch (SocketException e) {
            return false;
        }
    }

    /**
     * Returns a Map with all detected local IP-addresses as keys and the
     * associated NetworkInterface as values.
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

    /**
     * Returns a Map with all detected local IP-addresses as keys and the
     * associated NetworkInterface as values. Caches the result for a certain
     * amount of time.
     * 
     * @return
     * @throws SocketException
     */
    public static final Map<InetAddress, NetworkInterface> getAllLocalNetworkAddressesCached()
        throws SocketException
    {
        boolean cacheInvalid = LOCAL_NETWORK_ADDRESSES_CACHE == null
            || (System.currentTimeMillis() - CACHE_TIMEOUT > LAST_CHACHE_UPDATE);
        if (cacheInvalid) {
            LOCAL_NETWORK_ADDRESSES_CACHE = getAllLocalNetworkAddresses();
            LAST_CHACHE_UPDATE = System.currentTimeMillis();
        }
        return LOCAL_NETWORK_ADDRESSES_CACHE;
    }

}
