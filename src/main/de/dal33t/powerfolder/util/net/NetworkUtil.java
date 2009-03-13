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
package de.dal33t.powerfolder.util.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Reject;

/**
 * Utility class for all low level networking stuff.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.4 $
 */
public class NetworkUtil {
    private final static Logger LOG = Logger.getLogger(NetworkUtil.class.getName());

//    private static final int LAN_SOCKET_BUFFER_SIZE = 64 * 1024;
//    private static final int INET_SOCKET_BUFFER_SIZE = 16 * 1024;
//    private static final int LAN_SOCKET_BUFFER_LIMIT = 1024 * 1024;
//    private static final int INET_SOCKET_BUFFER_LIMIT = 256 * 1024;

    private static final long CACHE_TIMEOUT = 10 * 1000;

    private static long LAST_CHACHE_UPDATE = 0;
    private static Map<InetAddress, NetworkInterface> LOCAL_NETWORK_ADDRESSES_CACHE;

    private static InetAddress NULL_IP;
    static {
        try {
            NULL_IP = InetAddress.getByAddress("0.0.0.0",
                new byte[]{0, 0, 0, 0});
        } catch (UnknownHostException e) {
            NULL_IP = null;
            e.printStackTrace();
        }
    }

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
    public static void setupSocket(Socket socket, Controller controller)
        throws SocketException
    {
        Reject.ifNull(socket, "Socket is null");
        Reject.ifNull(controller, "Controller is null");

        boolean onLan = isOnLanOrLoopback(socket.getInetAddress());
        // socket.setSoTimeout(Constants.SOCKET_CONNECT_TIMEOUT);
        // socket.setSoLinger(true, 4000);
        // socket.setKeepAlive(true);

        if (onLan) {
            int bufferSize = ConfigurationEntry.NET_SOCKET_LAN_BUFFER_SIZE
                .getValueInt(controller);
            socket.setReceiveBufferSize(bufferSize);
            socket.setSendBufferSize(bufferSize);
        } else {
            int bufferSize = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_SIZE
                .getValueInt(controller);
            socket.setReceiveBufferSize(bufferSize);
            socket.setSendBufferSize(bufferSize);
        }
        // socket.setTcpNoDelay(true);
        LOG.finer("Socket setup: (" + socket.getSendBufferSize() + "/"
            + socket.getReceiveBufferSize() + "/" + socket.getSoLinger()
            + "ms) " + socket);
    }

    /**
     * Sets a socket up for use with PowerFolder
     * <p>
     * FIXME: Is broken. Does not consider LAN-IP list or computers discovered
     * by network broadcast. Recommended new API: setupSocket(UDTSocket socket,
     * boolean onLAN). Let the caller decide to setup with LAN optimized values.
     * 
     * @param socket
     *            the Socket to setup
     * @param inetSocketAddress
     *            the remote address
     * @throws IOException
     */
    public static void setupSocket(UDTSocket socket,
        InetSocketAddress inetSocketAddress, Controller controller)
        throws IOException
    {
        Reject.ifNull(socket, "Socket is null");
        Reject.ifNull(controller, "Controller is null");

        boolean onLan = (inetSocketAddress != null && inetSocketAddress
            .getAddress() != null) ? isOnLanOrLoopback(inetSocketAddress
            .getAddress()) : false;

        if (onLan) {
            int bufferSize = ConfigurationEntry.NET_SOCKET_LAN_BUFFER_SIZE
                .getValueInt(controller);
            socket.setSoUDPReceiverBufferSize(bufferSize);
            socket.setSoUDPSenderBufferSize(bufferSize);

            int bufferLimit = ConfigurationEntry.NET_SOCKET_LAN_BUFFER_LIMIT
                .getValueInt(controller);
            socket.setSoSenderBufferLimit(bufferLimit);
            socket.setSoReceiverBufferLimit(bufferLimit);
        } else {
            int bufferSize = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_SIZE
                .getValueInt(controller);
            socket.setSoUDPReceiverBufferSize(bufferSize);
            socket.setSoUDPSenderBufferSize(bufferSize);

            int bufferLimit = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_LIMIT
                .getValueInt(controller);
            socket.setSoSenderBufferLimit(bufferLimit);
            socket.setSoReceiverBufferLimit(bufferLimit);
        }

        LOG.finer("Socket setup: (" + socket.getSoUDPSenderBufferSize() + "/"
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
            LOG.warning("Inet6 not supported yet: " + addr);
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
     * @return all local network addresses
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
     * @return the cached list al all network addresses
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

    /**
     * @return true if this system has support for UDT based connections.
     */
    public static final boolean isUDTSupported() {
        return UDTSocket.isSupported();
    }

    /**
     * @param address
     * @return true if the address is 0.0.0.0
     */
    public static final boolean isNullIP(InetAddress address) {
        Reject.ifNull(address, "Address is null");
        boolean nullIP = false;
        if (NULL_IP != null) {
            // Using advanced check
            nullIP = Boolean.valueOf(NULL_IP.equals(address));
        } else {
            // Fallback, this works
            byte[] addr = address.getAddress();
            nullIP = Boolean.valueOf((addr[0] & 0xff) == 0
                && (addr[1] & 0xff) == 0 && (addr[2] & 0xff) == 0
                && (addr[3] & 0xff) == 0);
        }
        return nullIP;
    }

}
