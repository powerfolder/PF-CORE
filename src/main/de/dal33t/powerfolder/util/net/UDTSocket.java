/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Basic wrapper for a UDT socket in java. Currently it's not possible to adjust
 * the buffer sizes used in the native implementation in java. For this reason,
 * expect alot of memory usage from creating such a socket. <br>
 * Actually, the amount used should be around 22MB per socket (if one looks at
 * the default buffer sizes).
 *
 * @author Dennis "Bytekeeper" Waldherr
 */
public class UDTSocket {
    private static final Logger LOG = Logger.getLogger(UDTSocket.class
        .getName());

    private class UDTInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            byte b[] = new byte[1];
            try {
                return recv(b, 0, 1);
            } catch (IOException e) {
                connected = false;
                throw e;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(b.length + " from:" + off
                    + " len:" + len);
            }
            try {
                return recv(b, off, len);
            } catch (IOException e) {
                connected = false;
                throw e;
            }
        }
    }

    private class UDTOutputStream extends OutputStream {

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException(b.length + " from:" + off
                    + " len:" + len);
            }
            try {
                send(b, off, len);
            } catch (IOException e) {
                connected = false;
                throw e;
            }
        }

        @Override
        public void write(int b) throws IOException {
            byte buf[] = new byte[]{(byte) b};
            try {
                send(buf, 0, 1);
            } catch (IOException e) {
                connected = false;
                throw e;
            }
        }

    }

    private static boolean supported = false;
    public static final AtomicInteger openSockets = new AtomicInteger(0);

    static {
        // Supported on windows only ATM.
        if (OSUtil.isWindowsSystem()
            && OSUtil.loadLibrary(UDTSocket.class, "udt")
            && OSUtil.loadLibrary(UDTSocket.class, "udt4j"))
        {
            initIDs();
            supported = true;
        }
    }

    /**
     * @return true if UDTSockets are supported.
     */
    public static boolean isSupported() {
        return supported;
    }

    private volatile boolean closed = false;

    private volatile boolean connected = false;

    private InputStream in;

    private OutputStream out;

    // Used in native code!
    private int sock = -1;

    private InetSocketAddress remoteAddress;

    /**
     * Creates an unbound, unconnected socket.
     */
    public UDTSocket() {
        sock = socket();
        if (openSockets.incrementAndGet() > 20) {
            LOG.warning("Many open UDT sockets (" + openSockets.get() + ')');
        }

    }

    // Used in native code!
    private UDTSocket(int sock) {
        this.sock = sock;
        connected = true;
    }

    /**
     * Accepts a connection. This socket must be bound and in listen state
     * before calling this method. Otherwise an IOException is thrown
     *
     * @return the accepted connection socket
     * @throws IOException
     */
    public UDTSocket accept() throws IOException {
        UDTSocket s = acceptImpl();
        s.remoteAddress = s.getRemoteAddressImpl();
        return s;
    }

    /**
     * Binds this socket to a local address.
     *
     * @param bindPoint
     * @throws IOException
     */
    public native void bind(InetSocketAddress bindPoint) throws IOException;

    /**
     * Closes the socket. Releases all native resources. Further reads and
     * writes on this socket will fail after calling this method.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        // Whatever happens in the actual native code - we can't use this object
        // anymore
        closed = true;
        connected = false;
        closeImpl();
        openSockets.decrementAndGet();
    }

    /**
     * Tries to connect to the given address. There is no timeout option exposed
     * by the native API, but the connection attempt will fail after some
     * hardcoded internal value.
     *
     * @param endPoint
     * @throws IOException
     *             if the connection attempt failed.
     */
    public void connect(InetSocketAddress endPoint) throws IOException {
        connectImpl(endPoint);
        // If no exception occurred, we're now connected
        connected = true;
        remoteAddress = getRemoteAddressImpl();
    }

    /**
     * Returns an InputStream for this socket.
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new UDTInputStream();
        }
        return in;
    }

    /**
     * Returns the address this socket was bound to.
     *
     * @return the address or null, if it hasn't been bound yet.
     */
    public native InetSocketAddress getLocalAddress();

    /**
     * Returns an OutputStream for this socket.
     *
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new UDTOutputStream();
        }
        return out;
    }

    /**
     * Returns the address of the remote peer.
     *
     * @return the address or null, if it's not connected
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns true if the socket is closed
     *
     * @return
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns true if the socket is connected
     *
     * @return
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Sets this socket to listen state (ServerSocket). Bind() must be be called
     * before invoking this method.
     *
     * @param backlog
     *            the maximum number of pending incoming connections
     * @throws IOException
     *             if an error occured
     */
    public native void listen(int backlog) throws IOException;

    /**
     * Sets the option value for UDT_RENDEZVOUS. In rendezvous mode, both peers
     * have to to connect to each other, instead of one peer listening and the
     * other connecting. This can be (and is) used to perform UDP hole punching.
     *
     * @param enabled
     */
    public native void setSoRendezvous(boolean enabled);

    /**
     * Returns the option value for UDT_RENDEZVOUS.
     *
     * @return
     */
    public native boolean getSoRendezvous();

    /**
     * Sets the SO_LINGER option. Sets the time close() will wait for
     * sending/receiving before closing the connection.
     *
     * @param true to linger on
     * @param seconds
     *            how long to linger, if on is true
     */
    public native void setSoLinger(boolean on, int seconds);

    /**
     * Returns the time to linger in seconds.
     *
     * @return the linger setting, or -1 if it's not on
     */
    public native int getSoLinger();

    /**
     * The receiver buffer limit is used to limit the size of temporary storage
     * of receiving data. Recommended size: Bandwidth * RTT
     *
     * @param value
     */
    public native void setSoReceiverBufferLimit(int value) throws IOException;

    /**
     * The receiver buffer limit is used to limit the size of temporary storage
     * of receiving data. Recommended size: Bandwidth * RTT
     *
     * @return the currrent buffer limit
     */
    public native int getSoReceiverBufferLimit() throws IOException;

    /**
     * The sender buffer limit is used to limit the size of temporary storage of
     * sending data. Recommended size: Bandwidth * RTT
     *
     * @param value
     */
    public native void setSoSenderBufferLimit(int value) throws IOException;

    /**
     * The sender buffer limit is used to limit the size of temporary storage of
     * sending data. Recommended size: Bandwidth * RTT
     *
     * @return the current buffer limit
     */
    public native int getSoSenderBufferLimit() throws IOException;

    /**
     * The UDP buffer size used for receiving. This can be relatively small.
     *
     * @param value
     */
    public native void setSoUDPReceiverBufferSize(int value) throws IOException;

    /**
     * The UDP buffer size used for receiving. This can be relatively small.
     *
     * @return the current buffer size
     */
    public native int getSoUDPReceiverBufferSize() throws IOException;

    /**
     * The UDP buffer size used for sending. This can be relatively small.
     *
     * @param value
     */
    public native void setSoUDPSenderBufferSize(int value) throws IOException;

    /**
     * The UDP buffer size used for sending. This can be relatively small.
     *
     * @return the current buffer size
     */
    public native int getSoUDPSenderBufferSize() throws IOException;

    @Override
    public String toString() {
        return "{UDTSocket: closed: " + closed + ", connected:" + connected
            + ", fd:" + sock + "}";
    }

    @Override
    protected void finalize() throws Throwable {
        if (sock != -1) {
            close();
        }
    }

    private native UDTSocket acceptImpl() throws IOException;

    private native void closeImpl() throws IOException;

    private native void connectImpl(InetSocketAddress endPoint)
        throws IOException;

    private native InetSocketAddress getRemoteAddressImpl();

    private native int recv(byte[] buffer, int off, int len) throws IOException;

    private native void send(byte[] buffer, int off, int len)
        throws IOException;

    /**
     * Initializes access IDs in JNI wrapper
     */
    private native static void initIDs();

    /**
     * Allocates a new UDT Socket.
     */
    private native static int socket();
}
