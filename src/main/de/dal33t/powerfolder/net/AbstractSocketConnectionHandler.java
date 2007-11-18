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
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.IdentityReply;
import de.dal33t.powerfolder.message.LimitBandwidth;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.Ping;
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

/**
 * Abstract version of a connection handler acting upon
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public abstract class AbstractSocketConnectionHandler extends PFComponent
    implements ConnectionHandler
{
    private static final long CONNECTION_KEEP_ALIVE_TIMOUT_MS = Constants.CONNECTION_KEEP_ALIVE_TIMOUT * 1000L;
    private static final long TIME_WITHOUT_KEEPALIVE_UNTIL_PING = CONNECTION_KEEP_ALIVE_TIMOUT_MS / 3L;

    /** The basic io socket */
    private Socket socket;

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
    private boolean shutdown = false;
    // Flag if client is on lan
    private boolean onLAN;

    // Locks
    private final Object identityWaiter = new Object();
    private final Object identityAcceptWaiter = new Object();
    // Lock for sending message
    private final Object sendLock = new Object();

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
    protected AbstractSocketConnectionHandler(Controller controller,
        Socket socket)
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
    @SuppressWarnings("unused")
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
    protected Socket getSocket() {
        return socket;
    }

    /**
     * Initalizes the connection handler.
     * 
     * @param controller
     * @param aSocket
     *            the tctp/ip socket.
     * @throws ConnectionException
     */
    public void init() throws ConnectionException {
        if (socket == null) {
            throw new NullPointerException("Socket is null");
        }
        if (socket.isClosed() || !socket.isConnected()) {
            throw new ConnectionException("Connection to peer is closed")
                .with(this);
        }
        this.started = false;
        this.identity = null;
        this.identityReply = null;
        this.shutdown = false;
        this.messagesToSendQueue = new ConcurrentLinkedQueue<Message>();
        long startTime = System.currentTimeMillis();

        try {
            out = new LimitedOutputStream(getController().getTransferManager()
                .getOutputLimiter(this), new BufferedOutputStream(socket
                .getOutputStream(), 1024));

            in = new LimitedInputStream(getController().getTransferManager()
                .getInputLimiter(this), new BufferedInputStream(socket
                .getInputStream(), 1024));
            if (logVerbose) {
                log().verbose("Got streams");
            }

            // Start receiver
            getController().getIOProvider().startIO(new Sender(),
                new Receiver());

            // ok, we are connected
            // Generate magic id, 16 byte * 8 * 8 bit = 1024 bit key
            myMagicId = IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId();

            // now send identity
            myIdentity = createOwnIdentity();
            if (logVerbose) {
                log().verbose(
                    "Sending my identity, nick: '"
                        + myIdentity.getMemberInfo().nick + "', ID: "
                        + myIdentity.getMemberInfo().id);
            }
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
        // log().warn("Received broadcast from ? " + onLAN);

        long took = System.currentTimeMillis() - startTime;
        if (logVerbose) {
            log().verbose(
                "Connect took " + took + "ms, time differ: "
                    + ((getTimeDeltaMS() / 1000) / 60) + " min, remote ident: "
                    + getIdentity());
        }

        // Analyse connection
        analyseConnection();

        // Check this connection for keep-alive
        installKeepAliveCheck();
    }

    private void installKeepAliveCheck() {
        if (logVerbose) {
            log().verbose("Installing keep-alive check");
        }
        TimerTask task = new KeepAliveChecker();
        getController().getIOProvider().getKeepAliveTimer().schedule(task,
            TIME_WITHOUT_KEEPALIVE_UNTIL_PING,
            TIME_WITHOUT_KEEPALIVE_UNTIL_PING);
    }

    /**
     * Shuts down this connection handler by calling shutdown of member. If no
     * associated member is found, the con handler gets directly shut down.
     * <p>
     */
    protected void shutdownWithMember() {
        if (getMember() != null) {
            // Shutdown member. This means this connection handler gets shut
            // down by member
            getMember().shutdown();
        }

        if (!shutdown) {
            // Not shutdown yet, just shut down
            shutdown();
        }
    }

    /**
     * Shuts down the connection handler. The member is shut down optionally
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        if (logVerbose) {
            log().verbose("Shutting down");
        }
        boolean wasStarted = started;
        if (isConnected() && wasStarted) {
            // Send "EOF" if possible, the last thing you see
            // FIXME Actually not working.
            sendMessagesAsynchron(new Problem("Closing connection, EOF", true,
                Problem.DISCONNECTED));
        }
        started = false;
        // Clear magic ids
        myMagicId = null;
        identity = null;
        // Remove link to member
        setMember(null);
        // Clear send queue
        messagesToSendQueue.clear();

        // close out stream
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ioe) {
            log().error("Could not close out stream", ioe);
        }

        // close in stream
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ioe) {
            log().error("Could not close in stream", ioe);
        }

        // close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log().verbose(e);
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
                if (logVerbose) {
                    log().verbose("-- (sending) -> " + message);
                }
                // log().warn("-- (sending) -> " + message);
                if (!isConnected()) {
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
                // log().warn("Ping packet size: " + data.length);
                // }

                int remaining = data.length;
                // synchronized (out) {
                while (remaining > 0) {
                    int allowed = remaining;
                    if (shutdown) {
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
                // log().warn(
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

        synchronized (messagesToSendQueue) {
            messagesToSendQueue.offer(message);
            messagesToSendQueue.notifyAll();
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

    public String getMyMagicId() {
        return myMagicId;
    }

    public String getRemoteMagicId() {
        return identity != null ? identity.getMagicId() : null;
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
                    log().verbose(e);
                }
            }
        }
    }

    public boolean acceptIdentity(Member node) {
        Reject.ifNull(node, "node is null");
        // Connect member with this node
        member = node;

        // now handshake
        log().debug("Sending accept of identity to " + this);
        sendMessagesAsynchron(IdentityReply.accept());

        // wait for accept of our identity
        long start = System.currentTimeMillis();
        synchronized (identityAcceptWaiter) {
            if (identityReply == null) {
                try {
                    identityAcceptWaiter.wait(60000);
                } catch (InterruptedException e) {
                    log().verbose(e);
                }
            }
        }

        long took = (System.currentTimeMillis() - start) / 1000;
        if (!isConnected()) {
            log().warn(
                "Remote member disconnected while waiting for identity reply. "
                    + identity);
            member = null;
            return false;
        }

        if (identityReply == null) {
            log().warn(
                "Did not receive a identity reply after " + took
                    + "s. Connected? " + isConnected() + ". remote id: "
                    + identity);
            member = null;
            return false;
        }

        if (identityReply.accepted) {
            if (logVerbose) {
                log().verbose("Identity accepted by remote peer. " + this);
            }
        } else {
            member = null;
            log().warn("Identity rejected by remote peer. " + this);
        }

        return identityReply.accepted;
    }

    /**
     * Waits for the send queue to get send
     */
    public void waitForEmptySendQueue() {
        boolean waited = false;
        while (!messagesToSendQueue.isEmpty() && isConnected()) {
            try {
                // log().warn("Waiting for empty send buffer to " +
                // getMember());
                waited = true;
                // Wait a bit the let the send queue get empty
                Thread.sleep(50);
            } catch (InterruptedException e) {
                log().verbose(e);
                break;
            }
        }
        if (waited) {
            if (logVerbose) {
                log().verbose(
                    "Waited for empty sendbuffer, clear now, proceeding to "
                        + getMember());
            }
        }
    }

    /**
     * Analysese the connection of the user
     */
    private void analyseConnection() {
        if (Feature.CORRECT_LAN_DETECTION.isDisabled()) {
            log().warn("ON LAN because of correct connection analyse disabled");
            setOnLAN(true);
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
            setOnLAN(NetworkUtil.isOnLanOrLoopback(adr)
                || getController().getNodeManager().isNodeOnConfiguredLan(adr));
            // Check if the remote address is one of this machine's
            // interfaces.
            try {
                omitBandwidthLimit = NetworkUtil
                    .getAllLocalNetworkAddressesCached().containsKey(
                        socket.getInetAddress());
            } catch (SocketException e) {
                log().error("Omitting bandwidth", e);
            }
        }

        if (logVerbose) {
            log().verbose("analyse connection: lan: " + onLAN);
        }
    }

    public boolean acceptHandshake() {
        return true;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) socket.getRemoteSocketAddress();
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
        log().debug(msg);
        log().verbose(e);
    }

    // General ****************************************************************

    public String toString() {
        if (socket == null) {
            return "-disconnected-";
        }
        synchronized (socket) {
            return socket.getInetAddress() + ":" + socket.getPort();
        }
    }

    // Inner classes **********************************************************

    private final class KeepAliveChecker extends TimerTask {
        @Override
        public void run() {
            if (shutdown) {
                return;
            }
            boolean newPing;
            if (lastKeepaliveMessage == null) {
                newPing = true;
            } else {
                long timeWithoutKeepalive = System.currentTimeMillis()
                    - lastKeepaliveMessage.getTime();
                newPing = timeWithoutKeepalive >= TIME_WITHOUT_KEEPALIVE_UNTIL_PING;
                if (logVerbose) {
                    log().verbose(
                        "Keep-alive check. Received last keep alive message "
                            + timeWithoutKeepalive + "ms ago, ping required? "
                            + newPing + ". Node: " + getMember());
                }
                if (timeWithoutKeepalive > CONNECTION_KEEP_ALIVE_TIMOUT_MS) {
                    log().warn(
                        "Shutting down. Dead connection detected ("
                            + timeWithoutKeepalive + "ms timeout) to "
                            + getMember());
                    shutdownWithMember();
                    return;
                }
            }
            if (newPing) {
                // Send new ping
                sendMessagesAsynchron(new Ping(-1));
            }
        }
    }

    /**
     * The sender class, handles all asynchron messages
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.72 $
     */
    class Sender implements Runnable {
        long waitTime = getController().getWaitTime();

        public void run() {
            while (!shutdown) {
                if (logVerbose) {
                    log().verbose(
                        "Asynchron message send triggered, sending "
                            + messagesToSendQueue.size() + " message(s)");
                }

                if (!isConnected()) {
                    // Client disconnected, stop
                    break;
                }

                int i = 0;
                Message msg;
                // long start = System.currentTimeMillis();
                while ((msg = messagesToSendQueue.poll()) != null) {
                    i++;
                    if (shutdown) {
                        break;
                    }
                    try {
                        // log().warn(
                        // "Sending async (" + messagesToSendQueue.size()
                        // + "): " + asyncMsg.getMessage());
                        sendMessage(msg);

                        // log().warn("Send complete: " +
                        // asyncMsg.getMessage());
                    } catch (ConnectionException e) {
                        log().warn("Unable to send message asynchronly. " + e);
                        log().verbose(e);
                        // Stop thread execution
                        break;
                    }
                }
                // long took = System.currentTimeMillis() - start;
                // log().warn("Sending of " + i + " messages took " + took +
                // "ms");

                synchronized (messagesToSendQueue) {
                    if (messagesToSendQueue.isEmpty()) {
                        try {
                            messagesToSendQueue.wait();
                        } catch (InterruptedException e) {
                            log().verbose(e);
                            break;
                        }
                    }
                }
            }

            // Cleanup
            shutdownWithMember();
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
            // go in ready state
            started = true;
            byte[] sizeArr = new byte[4];

            while (!shutdown) {
                // check connection status
                if (!isConnected()) {
                    break;
                }

                try {
                    // Read data header, total size
                    read(in, sizeArr, 0, sizeArr.length);
                    int totalSize = Convert.convert2Int(sizeArr);
                    if (shutdown) {
                        // Do not process this message
                        break;
                    }
                    if (totalSize == -1393754107) {
                        throw new IOException("Client has old protocol version");
                    }
                    if (totalSize == -1) {
                        // log().verbose(
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
                    // && getMember().isCompleteyConnected()
                    // && getMember().getPeer() !=
                    // AbstractSocketConnectionHandler.this)
                    // {
                    // log().error(
                    // "DEAD connection handler found for member: "
                    // + getMember());
                    // shutdown();
                    // return;
                    // }
                    if (logVerbose) {
                        log().verbose(
                            "<- (received, " + Format.formatBytes(totalSize)
                                + ") - " + obj);
                    }

                    if (!getController().isStarted()) {
                        log().verbose(
                            "Peer still active, shutting down " + getMember());
                        break;
                    }

                    if (obj instanceof Identity) {
                        if (logVerbose) {
                            log().verbose("Received remote identity: " + obj);
                        }
                        // the remote identity
                        identity = (Identity) obj;

                        // Get magic id
                        if (logVerbose) {
                            log().verbose(
                                "Received magicId: " + identity.getMagicId());
                        }

                        // Trigger identitywaiter
                        synchronized (identityWaiter) {
                            identityWaiter.notifyAll();
                        }

                    } else if (obj instanceof IdentityReply) {
                        if (logVerbose) {
                            log().verbose("Received identity reply: " + obj);
                        }
                        // remote side accpeted our identity
                        identityReply = (IdentityReply) obj;

                        // Trigger identity accept waiter
                        synchronized (identityAcceptWaiter) {
                            identityAcceptWaiter.notifyAll();
                        }
                    } else if (obj instanceof Ping) {
                        // Answer the ping
                        Pong pong = new Pong((Ping) obj);
                        sendMessagesAsynchron(pong);

                    } else if (obj instanceof Pong) {
                        // Do nothing.

                    } else if (obj instanceof Problem) {
                        Problem problem = (Problem) obj;
                        if (member != null) {
                            member.handleMessage(problem);
                        } else {
                            log().warn(
                                "("
                                    + (identity != null ? identity
                                        .getMemberInfo().nick : "-")
                                    + ") Problem received: " + problem.message);
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
                            member.handleMessage((Message) obj);
                        } else {
                            log().error(
                                "Connection closed, message received, before peer identified itself: "
                                    + obj);
                            // connection closed
                            break;
                        }
                    } else {
                        log().error(
                            "Received unknown message from peer: " + obj);
                    }
                } catch (SocketTimeoutException e) {
                    log().warn("Socket timeout on read, not disconnecting");
                } catch (SocketException e) {
                    logConnectionClose(e);
                    // connection closed
                    break;
                } catch (EOFException e) {
                    logConnectionClose(e);
                    // connection closed
                    break;
                } catch (InvalidClassException e) {
                    log().verbose(e);
                    String from = getMember() != null
                        ? getMember().getNick()
                        : this.toString();
                    log().warn(
                        "Received unknown packet/class: " + e.getMessage()
                            + " from " + from);
                    // do not break connection
                } catch (InvalidObjectException e) {
                    log().verbose(e);
                    String from = getMember() != null
                        ? getMember().getNick()
                        : this.toString();
                    log().warn(
                        "Received invalid object: " + e.getMessage() + " from "
                            + from);
                    // do not break connection
                } catch (IOException e) {
                    log().verbose(e);
                    logConnectionClose(e);
                    break;
                } catch (ConnectionException e) {
                    log().verbose(e);
                    logConnectionClose(e);
                    break;
                } catch (ClassNotFoundException e) {
                    log().verbose(e);
                    log().warn(
                        "Received unknown packet/class: " + e.getMessage()
                            + " from " + AbstractSocketConnectionHandler.this);
                    // do not break connection
                } catch (RuntimeException e) {
                    log().error(e);
                    throw e;
                }
            }

            // Shut down
            shutdownWithMember();
        }
    }
}