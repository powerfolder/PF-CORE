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
 * $Id: AbstractUDTSocketConnectionHandler.java 19112 2012-06-04 00:38:24Z sprajc $
 */
package de.dal33t.powerfolder.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.IdentityReply;
import de.dal33t.powerfolder.message.LimitBandwidth;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.Pong;
import de.dal33t.powerfolder.message.Problem;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.net.UDTSocket;

/**
 * WARNING: This code is actually copied and reused for UDT sockets.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public abstract class AbstractUDTSocketConnectionHandler extends PFComponent
    implements ConnectionHandler
{

    /** The basic io socket */
    private UDTSocket socket;

    /** The assigned member */
    private Member member;

    // Our identity
    private Identity myIdentity;

    // Identity of remote peer
    private Identity identity;
    private IdentityReply identityReply;
    // The magic id, which has been send to the remote peer
    private String myMagicId;

    private LimitedOutputStream out;
    private LimitedInputStream in;
    private ByteSerializer serializer;

    // The send buffer
    private Queue<Message> messagesToSendQueue;

    private boolean started;
    // Flag if client is on lan
    private boolean onLAN;

    // Locks
    private final Object identityWaiter = new Object();
    private final Object identityAcceptWaiter = new Object();
    // Lock for sending message
    private final Object sendLock = new Object();

    /**
     * The current active sender.
     */
    private Runnable sender;

    /**
     * Lock to ensure that modifications to senders are performed by one thread
     * only.
     */
    private Lock senderSpawnLock;

    // Keepalive stuff
    private Date lastKeepaliveMessage;

    /**
     * If true all bandwidth limits are omitted, if false it's handled message
     * based
     */
    private boolean omitBandwidthLimit;

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
    protected AbstractUDTSocketConnectionHandler(Controller controller,
        UDTSocket socket)
    {
        super(controller);
        this.socket = socket;
        this.serializer = new ByteSerializer();
    }

    // Abstract behaviour *****************************************************

    /**
     * Called before the message gets actally written into the socket.
     * 
     * @param message
     *            the message to serialize
     * @return the serialized message
     */
    protected abstract byte[] serialize(Message message)
        throws ConnectionException;

    /**
     * Called when the data got read from the socket. Should re-construct the
     * serialized object from the data.
     * 
     * @param data
     *            the serialized data
     * @param len
     *            the actual size of the data in data buffer
     * @return the deserialized object
     */
    protected abstract Object deserialize(byte[] data, int len)
        throws ConnectionException, ClassNotFoundException;

    /**
     * (Optional) Handles the received object.
     * 
     * @param obj
     *            the obj that was received
     * @return true if this object/message was handled.
     * @throws ConnectionException
     *             if something is broken.
     */
    protected boolean receivedObject(Object obj) throws ConnectionException {
        return false;
    }

    /**
     * @return an identity that gets send to the remote side.
     */
    protected abstract Identity createOwnIdentity();

    /**
     * @return the internal used serializer
     */
    protected ByteSerializer getSerializer() {
        return serializer;
    }

    /**
     * @return the tcp/ip socket
     */
    protected UDTSocket getSocket() {
        return socket;
    }

    /**
     * Initializes the connection handler.
     * 
     * @throws ConnectionException
     */
    public void init() throws ConnectionException {
        if (socket.isClosed() || !socket.isConnected()) {
            throw new ConnectionException("Connection to peer is closed")
                .with(this);
        }
        this.started = true;
        this.identity = null;
        this.identityReply = null;
        this.messagesToSendQueue = new ConcurrentLinkedQueue<Message>();
        this.senderSpawnLock = new ReentrantLock();
        long startTime = System.currentTimeMillis();

        try {
            out = new LimitedOutputStream(getController().getTransferManager()
                .getOutputLimiter(this), new BufferedOutputStream(socket
                .getOutputStream(), 1024));

            in = new LimitedInputStream(getController().getTransferManager()
                .getInputLimiter(this), new BufferedInputStream(socket
                .getInputStream(), 1024));
            if (isFiner()) {
                logFiner("Got streams");
            }

            // Generate magic id, 16 byte * 8 * 8 bit = 1024 bit key
            myMagicId = IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId();

            // Create identity
            myIdentity = createOwnIdentity();
            if (isFiner()) {
                logFiner("Sending my identity, nick: '"
                    + myIdentity.getMemberInfo().nick + "', ID: "
                    + myIdentity.getMemberInfo().id);
            }

            // Start receiver
            getController().getIOProvider().startIO(new Receiver());

            // Send identity
            sendMessagesAsynchron(myIdentity);
        } catch (IOException e) {
            throw new ConnectionException("Unable to open connection", e)
                .with(this);
        }
        waitForRemoteIdentity();

        if (!isConnected()) {
            shutdown();
            throw new ConnectionException(
                "Remote peer disconnected while waiting for his identity")
                .with(this);
        }
        if (identity == null || identity.getMemberInfo() == null) {
            throw new ConnectionException(
                "Did not receive a valid identity from peer after 60s")
                .with(this);
        }

        // Check if IP is on LAN
        // onLAN = getController().getBroadcastManager().receivedBroadcastFrom(
        // socket.getInetAddress());
        // logWarning("Received broadcast from ? " + onLAN);

        long took = System.currentTimeMillis() - startTime;
        if (isFiner()) {
            logFiner("Connect took " + took + "ms, time differ: "
                + ((getTimeDeltaMS() / 1000) / 60) + " min, remote ident: "
                + getIdentity());
        }

        // Analyse connection
        analyseConnection();

        // Check this connection for keep-alive
        getController().getIOProvider().startKeepAliveCheck(this);
    }

    /**
     * Shuts down this connection handler by calling shutdown of member. If no
     * associated member is found, the con handler gets directly shut down.
     * <p>
     */
    public void shutdownWithMember() {
        if (getMember() != null) {
            // Shutdown member. This means this connection handler gets shut
            // down by member
            getMember().shutdown();
        }

        if (started) {
            // Not shutdown yet, just shut down
            shutdown();
        }
    }

    /**
     * Shuts down the connection handler. The member is shut down optionally
     */
    public void shutdown() {
        if (!started) {
            return;
        }
        if (isFiner()) {
            logFiner("Shutting down");
        }
        // if (isConnected() && started) {
        // // Send "EOF" if possible, the last thing you see
        // sendMessagesAsynchron(new Problem("Closing connection, EOF", true,
        // Problem.DISCONNECTED));
        // // Give him some time to receive the message
        // waitForEmptySendQueue(1000);
        // }
        started = false;
        // Clear magic ids
        // myMagicId = null;
        // identity = null;
        // Remove link to member
        setMember(null);
        // Clear send queue
        messagesToSendQueue.clear();

        getController().getIOProvider().removeKeepAliveCheck(this);

        // close out stream
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ioe) {
            logSevere("Could not close out stream", ioe);
        }

        // close in stream
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ioe) {
            logSevere("Could not close in stream", ioe);
        }

        // close socket
        UDTSocket thisSocket = socket;
        if (thisSocket != null) {
            try {
                InetSocketAddress addr = thisSocket.getLocalAddress();
                if (addr != null) {
                    getController().getIOProvider()
                        .getUDTSocketConnectionManager().releaseSlot(
                            addr.getPort());
                }
                thisSocket.close();
            } catch (IOException e) {
                logFiner("IOException", e);
            }
        }

        // Trigger all waiting treads
        synchronized (identityWaiter) {
            identityWaiter.notifyAll();
        }
        synchronized (identityAcceptWaiter) {
            identityAcceptWaiter.notifyAll();
        }
        synchronized (messagesToSendQueue) {
            messagesToSendQueue.notifyAll();
        }

        // make sure the garbage collector gets this
        serializer = null;
    }

    /**
     * @return true if the connection is active
     */
    public boolean isConnected() {
        return (socket != null && in != null && out != null
            && socket.isConnected() && !socket.isClosed() && serializer != null);
    }

    public boolean isEncrypted() {
        return false;
    }

    public boolean isOnLAN() {
        return onLAN;
    }

    public void setOnLAN(boolean onlan) {
        onLAN = onlan;
        out.setBandwidthLimiter(getController().getTransferManager()
            .getOutputLimiter(this));
        in.setBandwidthLimiter(getController().getTransferManager()
            .getInputLimiter(this));
    }

    public void setMember(Member member) {
        this.member = member;
        // Logic moved into central place <code>Member.isOnLAN()</code>
        // if (!isOnLAN()
        // && member != null
        // && getController().getNodeManager().isNodeOnConfiguredLan(
        // member.getInfo()))
        // {
        // setOnLAN(true);
        // }
    }

    public Member getMember() {
        return member;
    }

    public Date getLastKeepaliveMessageTime() {
        return lastKeepaliveMessage;
    }

    /**
     * Reads a specific amout of data from a stream. Wait util enough data is
     * available
     * 
     * @param inStr
     *            the inputstream
     * @param buffer
     *            the buffer to put in the data
     * @param offset
     *            the start offset in the buffer
     * @param size
     *            the number of bytes to read
     * @throws IOException
     *             if stream error
     */
    private void read(InputStream inStr, byte[] buffer, int offset, int size)
        throws IOException
    {
        StreamUtils.read(inStr, buffer, offset, size);
        getController().getTransferManager().getTotalDownloadTrafficCounter()
            .bytesTransferred(size);
    }

    public void sendMessage(Message message) throws ConnectionException {
        if (message == null) {
            throw new NullPointerException("Message is null");
        }

        if (!isConnected()) {
            throw new ConnectionException("Connection to remote peer closed")
                .with(this);
        }

        // break if remote peer did no identitfy
        if (identity == null && (!(message instanceof Identity))) {
            throw new ConnectionException(
                "Unable to send message, peer did not identify yet").with(this);
        }

        try {
            synchronized (sendLock) {
                if (isFiner()) {
                    logFiner("-- (sending) -> " + message);
                }
                // logWarning("-- (sending) -> " + message);
                if (!isConnected() || !started) {
                    throw new ConnectionException(
                        "Connection to remote peer closed").with(this);
                }

                // Not limit some pakets
                boolean omittBandwidthLimit = !(message instanceof LimitBandwidth)
                    || this.omitBandwidthLimit;

                byte[] data = serialize(message);

                // Write paket header / total length
                out.write(Convert.convert2Bytes(data.length));
                getController().getTransferManager()
                    .getTotalUploadTrafficCounter().bytesTransferred(
                        data.length + 4);
                // out.flush();

                // Do some calculations before send
                int offset = 0;

                // if (message instanceof Ping) {
                // logWarning("Ping packet size: " + data.length);
                // }

                int remaining = data.length;
                // synchronized (out) {
                while (remaining > 0) {
                    int allowed = remaining;
                    if (!started) {
                        throw new ConnectionException(
                            "Unable to send message to peer, connection shutdown")
                            .with(member).with(this);
                    }
                    out.write(data, offset, allowed, omittBandwidthLimit);
                    offset += allowed;
                    remaining -= allowed;
                }
                // }

                // Flush
                out.flush();

                // long took = System.currentTimeMillis() - started;

                // if (took > 500) {
                // logWarning(
                // "Message (" + data.length + " bytes) took " + took
                // + "ms.");
                // }
            }
        } catch (IOException e) {
            // shutdown this peer
            shutdownWithMember();
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e).with(
                member).with(this);
        } catch (ConnectionException e) {
            // Ensure shutdown
            shutdownWithMember();
            throw e;
        } catch (RuntimeException e) {
            logSevere("Runtime exception while serializing: " + message, e);
            // Ensure shutdown
            shutdownWithMember();
            throw e;
        }
    }

    public void sendMessagesAsynchron(Message... messages) {
        for (Message message : messages) {
            sendMessageAsynchron(message, null);
        }
    }

    /**
     * A message to be send later. code execution does not wait util message was
     * sent successfully
     * 
     * @param message
     *            the message to be sent
     * @param errorMessage
     *            the error message to be logged on connection problem
     */
    private void sendMessageAsynchron(Message message, String errorMessage) {
        Reject.ifNull(message, "Message is null");

        senderSpawnLock.lock();
        try {
            messagesToSendQueue.offer(message);
            if (messagesToSendQueue
                .size() > Constants.WARN_MESSAGES_IN_SEND_QUEUE
                && isWarning())
            {
                String msg = "Many messages in send queue: "
                    + messagesToSendQueue.size() + ": " + messagesToSendQueue;
                if (msg.length() > 300) {
                    msg = msg.substring(0, 300);
                    msg += "...";
                }
                logWarning(msg);
            }
            // PFC-2591/PFC-2742: Start
            if (messagesToSendQueue
                .size() > Constants.MAX_MESSAGES_IN_SEND_QUEUE)
            {
                String msg = "Disconnecting " + getIdentity()
                    + ": Too many messages in send queue: "
                    + messagesToSendQueue.size();
                logWarning(msg);
                Runnable shutdownWithMember = new Runnable() {
                    @Override
                    public void run() {
                        shutdownWithMember();
                    }
                };
                getController().getIOProvider().startIO(shutdownWithMember);
                return;
            }
            // PFC-2591/PFC-2742: End
            if (sender == null) {
                sender = new Sender();
                getController().getIOProvider().startIO(sender);
            }
        } finally {
            senderSpawnLock.unlock();
        }
    }

    public long getTimeDeltaMS() {
        if (identity.getTimeGMT() == null)
            return 0;
        return myIdentity.getTimeGMT().getTimeInMillis()
            - identity.getTimeGMT().getTimeInMillis();
    }

    public boolean canMeasureTimeDifference() {
        return identity.getTimeGMT() != null;
    }

    public Identity getIdentity() {
        return identity;
    }

    public Identity getMyIdentity() {
        return myIdentity;
    }

    public String getMyMagicId() {
        return myMagicId;
    }

    public String getRemoteMagicId() {
        return identity != null ? identity.getMagicId() : null;
    }

    public ConnectionQuality getConnectionQuality() {
        // FIXME: Direct UDP should be GOOD.
        return ConnectionQuality.MEDIUM;
    }

    /**
     * Waits until we received the remote identity
     */
    private void waitForRemoteIdentity() {
        synchronized (identityWaiter) {
            if (identity == null) {
                // wait for remote identity
                try {
                    identityWaiter.wait(60000);
                } catch (InterruptedException e) {
                    // Ignore
                    logFiner("InterruptedException", e);
                }
            }
        }
    }

    public boolean acceptIdentity(Member node) {
        Reject.ifNull(node, "node is null");
        // Connect member with this node
        member = node;

        // now handshake
        if (isFiner()) {
            logFiner("Sending accept of identity to " + this);
        }
        sendMessagesAsynchron(IdentityReply.accept());

        // wait for accept of our identity
        long start = System.currentTimeMillis();
        synchronized (identityAcceptWaiter) {
            if (identityReply == null) {
                try {
                    identityAcceptWaiter.wait(20000);
                } catch (InterruptedException e) {
                    logFiner("InterruptedException", e);
                }
            }
        }

        long took = (System.currentTimeMillis() - start) / 1000;
        if (identityReply != null && !identityReply.accepted) {
            logWarning("Remote peer '" + node + "' rejected our connection: "
                + identityReply.message);
            member = null;
            return false;
        }

        if (!isConnected()) {
            logWarning("Remote member disconnected while waiting for identity reply. "
                + identity);
            member = null;
            return false;
        }

        if (identityReply == null) {
            logWarning("Did not receive a identity reply after " + took
                + "s. Connected? " + isConnected() + ". remote id: " + identity);
            member = null;
            return false;
        }

        if (identityReply.accepted) {
            if (isFiner()) {
                logFiner("Identity accepted by remote peer. " + this);
            }
        } else {
            member = null;
            logWarning("Identity rejected by remote peer. " + this);
        }

        return identityReply.accepted;
    }

    public boolean waitForEmptySendQueue(long ms) {
        long waited = 0;
        while (!messagesToSendQueue.isEmpty() && isConnected()) {
            try {
                // logWarning("Waiting for empty send buffer to " +
                // getMember());
                waited += 50;
                // Wait a bit the let the send queue get empty
                Thread.sleep(50);

                if (ms >= 0 && waited >= ms) {
                    // Stop waiting
                    break;
                }
            } catch (InterruptedException e) {
                logFiner("InterruptedException", e);
                break;
            }
        }
        if (waited > 0) {
            if (isFiner()) {
                logFiner("Waited " + waited
                    + "ms for empty sendbuffer, clear now, proceeding to "
                    + getMember());
            }
        }
        return messagesToSendQueue.isEmpty();
    }

    /**
     * Analysese the connection of the user
     */
    private void analyseConnection() {
        if (Feature.CORRECT_LAN_DETECTION.isDisabled()) {
            logWarning("ON LAN because of correct connection analyse disabled");
            setOnLAN(true);
            return;
        }
        if (Feature.CORRECT_INTERNET_DETECTION.isDisabled()) {
            logWarning("ON Internet because of correct connection analyse disabled");
            setOnLAN(false);
            return;
        }
        if (identity != null && identity.isTunneled()) {
            setOnLAN(false);
            return;
        }
        if (getRemoteAddress() != null
            && getRemoteAddress().getAddress() != null)
        {
            InetAddress adr = getRemoteAddress().getAddress();
            setOnLAN(getController().getNodeManager().isOnLANorConfiguredOnLAN(
                adr));
            // Check if the remote address is one of this machine's
            // interfaces.
            try {
                omitBandwidthLimit = NetworkUtil.isFromThisComputer(socket
                    .getRemoteAddress().getAddress());
            } catch (SocketException e) {
                logSevere("Omitting bandwidth", e);
            }
        }

        if (isFiner()) {
            logFiner("analyse connection: lan: " + onLAN);
        }
    }

    public boolean acceptHandshake() {
        return true;
    }

    public InetSocketAddress getRemoteAddress() {
        return socket.getRemoteAddress();
    }

    public int getRemoteListenerPort() {
        if (identity == null || identity.getMemberInfo() == null
            || identity.getMemberInfo().getConnectAddress() == null)
        {
            return -1;
        }
        if (identity.isTunneled()) {
            // No reconnection available to a tunneled connection.
            return -1;
        }

        return identity.getMemberInfo().getConnectAddress().getPort();
    }

    /**
     * Logs a connection closed event
     * 
     * @param e
     */
    private void logConnectionClose(Exception e) {
        String msg = "Connection closed to "
            + ((member == null) ? this.toString() : member.toString());

        if (e != null) {
            msg += ". Cause: " + e.toString();
        }
        logFine(msg);
        logFiner("Exception", e);
    }

    // General ****************************************************************

    public String toString() {
        if (socket == null) {
            return "-disconnected-";
        }
        synchronized (socket) {
            return socket.getRemoteAddress().getAddress() + ":"
                + socket.getRemoteAddress().getPort();
        }
    }

    // Inner classes **********************************************************

    /**
     * The sender class, handles all asynchron messages
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.72 $
     */
    class Sender implements Runnable {
        public void run() {
            if (isFiner()) {
                logFiner("Asynchron message send triggered, sending "
                    + messagesToSendQueue.size() + " message(s)");
            }

            if (!isConnected()) {
                // Client disconnected, stop
                logFine("Peer disconnected while sender got active. Msgs in queue: "
                    + messagesToSendQueue.size() + ": " + messagesToSendQueue);
                return;
            }

            // logWarning(
            // "Sender started with " + messagesToSendQueue.size()
            // + " messages in queue");

            Message msg;
            // long start = System.currentTimeMillis();
            while (true) {
                senderSpawnLock.lock();
                msg = messagesToSendQueue.poll();
                if (msg == null) {
                    sender = null;
                    senderSpawnLock.unlock();
                    break;
                }
                senderSpawnLock.unlock();

                if (!started) {
                    logFine("Peer shutdown while sending: " + msg);
                    senderSpawnLock.lock();
                    sender = null;
                    senderSpawnLock.unlock();
                    shutdownWithMember();
                    break;
                }
                try {
                    // logWarning(
                    // "Sending async (" + messagesToSendQueue.size()
                    // + "): " + asyncMsg.getMessage());
                    sendMessage(msg);
                    // logWarning("Send complete: " +
                    // asyncMsg.getMessage());
                } catch (ConnectionException e) {
                    logFine("Unable to send message asynchronly. " + e);
                    logFiner("ConnectionException", e);
                    senderSpawnLock.lock();
                    sender = null;
                    senderSpawnLock.unlock();
                    shutdownWithMember();
                    // Stop thread execution
                    break;
                } catch (Throwable t) {
                    logSevere("Unable to send message asynchronly. " + t, t);
                    senderSpawnLock.lock();
                    sender = null;
                    senderSpawnLock.unlock();
                    shutdownWithMember();
                    // Stop thread execution
                    break;
                }
            }
            // logWarning("Sender finished after sending " + i + " messages");
        }
    }

    /**
     * Receiver, responsible to deserialize messages
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.72 $
     */
    class Receiver implements Runnable {
        public void run() {
            byte[] sizeArr = new byte[4];
            while (started) {
                // check connection status
                if (!isConnected()) {
                    break;
                }

                try {
                    // Read data header, total size
                    read(in, sizeArr, 0, sizeArr.length);
                    int totalSize = Convert.convert2Int(sizeArr);
                    if (!started) {
                        // Do not process this message
                        break;
                    }
                    if (totalSize == -1393754107) {
                        throw new IOException("Client has old protocol version");
                    }
                    if (totalSize == -1) {
                        // logFiner(
                        // "Connection closed (-1) to "
                        // + ConnectionHandler.this);
                        break;
                    }
                    if (totalSize <= 0) {
                        throw new IOException("Illegal paket size: "
                            + totalSize);
                    }

                    byte[] data = serializer.read(in, totalSize);
                    Object obj = deserialize(data, totalSize);

                    lastKeepaliveMessage = new Date();
                    getController().getTransferManager()
                        .getTotalDownloadTrafficCounter().bytesTransferred(
                            totalSize);

                    // Consistency check:
                    // if (getMember() != null
                    // && getMember().isCompletelyConnected()
                    // && getMember().getPeer() !=
                    // AbstractSocketConnectionHandler.this)
                    // {
                    // logSevere(
                    // "DEAD connection handler found for member: "
                    // + getMember());
                    // shutdown();
                    // return;
                    // }
                    if (isFiner()) {
                        logFiner("<- (received, "
                            + Format.formatBytes(totalSize) + ") - " + obj);
                    }

                    if (!getController().isStarted()) {
                        logFiner("Peer still active, shutting down "
                            + getMember());
                        break;
                    }

                    if (obj instanceof Identity) {
                        if (isFiner()) {
                            logFiner("Received remote identity: " + obj);
                        }

                        // Trigger identitywaiter
                        synchronized (identityWaiter) {
                            // the remote identity
                            identity = (Identity) obj;
                            identityWaiter.notifyAll();
                        }

                        // Get magic id
                        if (isFiner()) {
                            logFiner("Received magicId: "
                                + identity.getMagicId());
                        }

                    } else if (obj instanceof IdentityReply) {
                        if (isFiner()) {
                            logFiner("Received identity reply: " + obj);
                        }

                        // Trigger identity accept waiter
                        synchronized (identityAcceptWaiter) {
                            // remote side accpeted our identity
                            identityReply = (IdentityReply) obj;
                            identityAcceptWaiter.notifyAll();
                        }

                    } else if (obj instanceof Pong) {
                        // Do nothing.
                        // TRAC #812: Ping is answered on Member, not here!

                    } else if (obj instanceof Problem) {
                        Problem problem = (Problem) obj;
                        if (member != null) {
                            member.handleMessage(problem,
                                AbstractUDTSocketConnectionHandler.this);
                        } else {
                            logFine("("
                                + (identity != null
                                    ? identity.getMemberInfo().nick
                                    : "-") + ") Problem received: "
                                + problem.message);
                            if (problem.fatal) {
                                // Fatal problem, disconnecting
                                break;
                            }
                        }

                    } else if (receivedObject(obj)) {
                        // The object was handled by the subclass.
                        // OK pass through
                    } else if (obj instanceof Message) {

                        if (member != null) {
                            member.handleMessage((Message) obj,
                                AbstractUDTSocketConnectionHandler.this);
                        } else if (!isConnected()) {
                            // Simply break. Already disconnected
                            break;
                        } else {
                        	logWarning("Connection closed, message received, before peer identified itself: "
                                + obj);
                            // connection closed
                            break;
                        }
                    } else {
                        logWarning("Received unknown message from peer: " + obj);
                    }
                } catch (SocketTimeoutException e) {
                    logFiner("Socket timeout on read, not disconnecting. " + e);
                } catch (SocketException e) {
                    logConnectionClose(e);
                    // connection closed
                    break;
                } catch (EOFException e) {
                    logConnectionClose(e);
                    // connection closed
                    break;
                } catch (InvalidClassException e) {
                    logFiner("InvalidClassException", e);
                    String from = getMember() != null
                        ? getMember().getNick()
                        : toString();
                    logWarning("Received unknown packet/class: "
                        + e.getMessage() + " from " + from);
                    // do not break connection
                } catch (InvalidObjectException e) {
                    logFiner("InvalidObjectException", e);
                    String from = getMember() != null
                        ? getMember().getNick()
                        : toString();
                    logWarning("Received invalid object: " + e.getMessage()
                        + " from " + from);
                    // do not break connection
                } catch (IOException e) {
                    logFiner("IOException", e);
                    logConnectionClose(e);
                    break;
                } catch (ConnectionException e) {
                    logFiner("ConnectionException", e);
                    logConnectionClose(e);
                    break;
                } catch (ClassNotFoundException e) {
                    logFiner("ClassNotFoundException", e);
                    logWarning("Received unknown packet/class: "
                        + e.getMessage() + " from "
                        + AbstractUDTSocketConnectionHandler.this);
                    // do not break connection
                } catch (RuntimeException e) {
                    logSevere("RuntimeException", e);
                    shutdownWithMember();
                    throw e;
                }
            }

            // Shut down
            shutdownWithMember();
        }
    }
}