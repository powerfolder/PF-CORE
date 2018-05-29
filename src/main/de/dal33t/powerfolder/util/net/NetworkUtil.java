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
import java.math.BigInteger;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for all low level networking stuff.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.4 $
 */
public class NetworkUtil {
    private final static Logger LOG = Logger.getLogger(NetworkUtil.class
        .getName());

    /**
     * Network interfaces cache.
     */
    private static final long CACHE_TIMEOUT = 30 * 1000;

    private static long LAST_CHACHE_UPDATE = 0;
    private static Map<InterfaceAddress, NetworkInterface> LOCAL_NETWORK_ADDRESSES_CACHE;

    private static InetAddress NULL_IPv4;
    static {
        try {
            NULL_IPv4 = InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        } catch (UnknownHostException e) {
            NULL_IPv4 = null;
            e.printStackTrace();
        }
    }

    private static InetAddress NULL_IPv6;
    static {
        try {
            NULL_IPv6 = InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        } catch (UnknownHostException e) {
            NULL_IPv6 = null;
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
            if (bufferSize > 0) {
                socket.setReceiveBufferSize(bufferSize);
                socket.setSendBufferSize(bufferSize);
            }
        } else {
            int bufferSize = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_SIZE
                .getValueInt(controller);
            if (bufferSize > 0) {
                socket.setReceiveBufferSize(bufferSize);
                socket.setSendBufferSize(bufferSize);
            }
        }
        // socket.setTcpNoDelay(true);
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Socket setup: (" + socket.getSendBufferSize() + "/"
                    + socket.getReceiveBufferSize() + "/" + socket.getSoLinger()
                    + "ms) " + socket);
        }
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
            if (bufferSize > 0) {
                socket.setSoUDPReceiverBufferSize(bufferSize);
                socket.setSoUDPSenderBufferSize(bufferSize);
            }

            int bufferLimit = ConfigurationEntry.NET_SOCKET_LAN_BUFFER_LIMIT
                .getValueInt(controller);
            if (bufferLimit > 0) {
                socket.setSoSenderBufferLimit(bufferLimit);
                socket.setSoReceiverBufferLimit(bufferLimit);
            }
        } else {
            int bufferSize = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_SIZE
                .getValueInt(controller);
            if (bufferSize > 0) {
                socket.setSoUDPReceiverBufferSize(bufferSize);
                socket.setSoUDPSenderBufferSize(bufferSize);
            }

            int bufferLimit = ConfigurationEntry.NET_SOCKET_INTERNET_BUFFER_LIMIT
                .getValueInt(controller);
            if (bufferLimit > 0) {
                socket.setSoSenderBufferLimit(bufferLimit);
                socket.setSoReceiverBufferLimit(bufferLimit);
            }
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
        try {
            boolean local = addr.isLoopbackAddress()
                || isFromThisComputer(addr);
            if (local) {
                return true;
            }
            // Try harder. Test all interface IP networks
            for (InterfaceAddress ia : getAllLocalNetworkAddressesCached()
                .keySet())
            {
                if (isOnInterfaceSubnet(ia, addr)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOG.warning("Unable to check network setup: " + e);
            return false;
        }
    }

    /**
     * Returns a Map with all detected local IP-addresses as keys and the
     * associated NetworkInterface as values.
     *
     * @return all local network addresses
     * @throws SocketException If an I/O error occurs
     */
    public static Map<InterfaceAddress, NetworkInterface> getAllLocalNetworkAddresses()
        throws SocketException
    {
        Map<InterfaceAddress, NetworkInterface> res = new HashMap<>();
        NetworkInterface ni = null;
        InterfaceAddress ia = null;
        try {
            for (Enumeration<NetworkInterface> eni = NetworkInterface
                .getNetworkInterfaces(); eni.hasMoreElements();)
            {
                ni = eni.nextElement();
                for (InterfaceAddress ia0 : ni.getInterfaceAddresses()) {
                    try {
                        ia = ia0;
                        if (ia != null) {
                            res.put(ia, ni);
                        }
                    } catch (Throwable e) {
                        LOG.warning("Unable to get network interface configuration of "
                            + ni + " address: " + ia + ": " + e);
                    }
                }
            }
        } catch (Error e) {
            LOG.warning("Unable to get network interface configuration of "
                + ni + " address: " + ia + ": " + e);
        }
        return res;
    }

    /**
     * Check if the address represented by {@code address} is within the range
     * from {@code start} to {@code end}
     *
     * @param start
     *     The starting address bytes
     * @param end
     *     The ending address bytes
     * @param address
     *     The address to check
     *
     * @return {@code True} if {@code address} is within range, {@code false} otherwise.
     *
     * @throws IllegalArgumentException
     *     If the lengths of all parameters does not match.
     */
    static boolean isAddressInRange(@NotNull byte[] start, @NotNull byte[] end, @NotNull byte[] address) {
        if (start.length != address.length || address.length != end.length) {
            throw new IllegalArgumentException(String.format("Length of addresses do not match. Start is %d, end is %d, address is %d", start.length, end.length, address.length));
        }

        BigInteger startIP = new BigInteger(1, start);
        BigInteger endIP = new BigInteger(1, end);
        BigInteger targetIP = new BigInteger(1, address);

        int st = startIP.compareTo(targetIP);
        int te = targetIP.compareTo(endIP);

        return st <= 0 && te <= 0;
    }

    /**
     * Checks if {@code start} and {@code end} are either both IPv4 or IPv6 Addresses
     *
     * @param address1
     * @param address2
     */
    public static boolean checkIfSameVersion(@NotNull InetAddress address1,
        @NotNull InetAddress address2)
    {
        return (address1 instanceof Inet4Address && address2 instanceof Inet4Address) ||
            (address1 instanceof Inet6Address && address2 instanceof Inet6Address);
    }

    static boolean isOnInterfaceSubnet(InterfaceAddress ia,
        InetAddress addr)
    {
        Reject.ifNull(addr, "Address");
        Reject.ifNull(ia, "InterfaceAddress");

        InetAddress interfaceAddress = ia.getAddress();

        if (!checkIfSameVersion(interfaceAddress, addr)) {
            return false;
        }
        short version = 4;
        if (addr instanceof Inet6Address) {
            version = 6;
        }
        short prefixLength = ia.getNetworkPrefixLength();

        byte[] addr1 = interfaceAddress.getAddress();
        byte[] addr2 = addr.getAddress();
        byte[] mask = makeMaskPrefixArray(version, prefixLength);

        for (int i = 0; i < addr1.length; i++)
            if ((addr1[i] & mask[i]) != (addr2[i] & mask[i]))
                return false;

        return true;
    }

    /**
     * Creates a byte array for IP network mask prefix.
     *
     * @param version the IP address version
     * @param prefixLength the length of the mask prefix. Must be in the
     * interval [0, 32] for IPv4, or [0, 128] for IPv6
     * @return a byte array that contains a mask prefix of the
     * specified length
     * @throws IllegalArgumentException if the arguments are invalid
     */
    private static byte[] makeMaskPrefixArray(int version, int prefixLength) {
        int addrByteLength = version == 4 ? 4 : 16;
        int addrBitLength = addrByteLength * Byte.SIZE;

        // Verify the prefix length
        if ((prefixLength < 0) || (prefixLength > addrBitLength)) {
            final String msg = "Invalid IP prefix length: " + prefixLength +
                ". Must be in the interval [0, " + addrBitLength + "].";
            throw new IllegalArgumentException(msg);
        }

        // Number of bytes and extra bits that should be all 1s
        int maskBytes = prefixLength / Byte.SIZE;
        int maskBits = prefixLength % Byte.SIZE;
        byte[] mask = new byte[addrByteLength];

        // Set the bytes and extra bits to 1s
        for (int i = 0; i < maskBytes; i++) {
            mask[i] = (byte) 0xff;              // Set mask bytes to 1s
        }
        for (int i = maskBytes; i < addrByteLength; i++) {
            mask[i] = 0;                        // Set remaining bytes to 0s
        }
        if (maskBits > 0) {
            mask[maskBytes] = (byte) (0xff << (Byte.SIZE - maskBits));
        }
        return mask;
    }

    public static boolean isFromThisComputer(InetAddress addr) {
        try {
            if (addr == null) {
                return false;
            }
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                return true;
            }
            for (InterfaceAddress ia : getAllLocalNetworkAddressesCached()
                .keySet())
            {
                if (ia == null || ia.getAddress() == null) {
                    continue;
                }
                if (ia.getAddress().equals(addr)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.severe("Unable to get network setup. " + e);
        }
        return false;
    }

    /**
     * Returns a Map with all detected local IP-addresses as keys and the
     * associated NetworkInterface as values. Caches the result for a certain
     * amount of time.
     *
     * @return the cached list al all network addresses
     * @throws SocketException If an I/O error occurs
     */
    public static Map<InterfaceAddress, NetworkInterface> getAllLocalNetworkAddressesCached()
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
    public static boolean isUDTSupported() {
        return UDTSocket.isSupported();
    }

    /**
     * Check if {@code address} is an all zero address (IPv4 0.0.0.0, IPv6 [::])
     *
     * @param address
     *     The address to check
     *
     * @return {@code True} if all bytes of {@code address} are zero, {@code
     * false} otherwise.
     */
    public static boolean isNullIP(InetAddress address) {
        Reject.ifNull(address, "Address is null");

        if (NULL_IPv4 != null && address instanceof Inet4Address) {
            return NULL_IPv4.equals(address);
        }

        if (NULL_IPv6 != null && address instanceof Inet6Address) {
            return NULL_IPv6.equals(address);
        }

        for (byte b : address.getAddress()) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tries to retrieve the hostname of the address if available, but returns
     * the IP only if not available. Does NOT perform a reverse lookup.
     *
     * @param address
     *     The address to get the host or IP address for.
     *
     * @return the hostname or IP of the address.
     */
    public static String getHostAddressNoResolve(InetAddress address) {
        Reject.ifNull(address, "Address is null");
        try {
            String[] str = address.toString().split("/");
            if (StringUtils.isNotBlank(str[0])) {
                return str[0];
            } else if (StringUtils.isNotBlank(str[1])) {
                return str[1];
            }
        } catch (Exception e) {
            LOG.warning("Unable to resolve hostname/ip from " + address + ". " + e);
        }
        // Fallback
        return address.getHostAddress();
    }

    /**
     * PFC-2670: Installs a <code>TrustManager</code> which does not validate SSL
     * certificates, thus also accepting self-signed certificates. ATTENTION:
     * Potential security hole, use it only if you know what you are doing.
     */
    public static void installAllTrustingSSLManager() {
        try {
            LOG.warning("Any certificate will be trusted for SSL connections");
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingSSLManager()};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            // Install the all-trusting trust manager
            HttpsURLConnection
                .setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Disables SNI. Fixes:
            // javax.net.ssl.SSLProtocolException: handshake alert:
            // unrecognized_name
            // Source:
            // http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
            System.setProperty("jsse.enableSNIExtension", "false");

            // Fixes:
            // "java.security.cert.CertificateException: No subject alternative
            // DNS name matching <hostname> found.
            // Source:
            // http://stackoverflow.com/questions/19540289/how-to-fix-the-java-security-cert-certificateexception-no-subject-alternative

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
        }
    }

    public static final class AllTrustingSSLManager implements X509TrustManager
    {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(
            java.security.cert.X509Certificate[] certs, String authType)
        {
        }

        public void checkServerTrusted(
            java.security.cert.X509Certificate[] certs, String authType)
        {
        }
    }
}
