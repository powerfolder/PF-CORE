/* $Id: ConnectionHandler.java,v 1.72 2006/04/16 21:39:41 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang.ClassUtils;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.transfer.LimitedInputStream;
import de.dal33t.powerfolder.transfer.LimitedOutputStream;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * Handler for socket connections to other clients messages
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.72 $
 */
public class ConnectionHandler extends PFComponent {
    // The maximum size of a message until a waring is shown. For debugging
    private static final int MESSAGE_SIZE_WARNING = 50 * 1024;

    private Thread receiverThread;
    private Thread senderThread;
    private Socket socket;

    private Member member;

    // Our identity
    private Identity myIdentity;

    // Identity of remote peer
    private Identity identity;
    private IdentityReply identityReply;
    // The magic id, which has been send to the remote peer
    private String myMagicId;
    // Magic id received from the remote side
    private String remoteMagicId;

    private LimitedOutputStream out;
    private LimitedInputStream in;
    private ByteSerializer serializer;

    // The send buffer
    private List<AsynchronMessage> messagesToSend;
    // The time since the first buffer overrun occoured
    private Date sendBufferOverrunSince;

    private boolean started;
    // Flag if client is on lan
    private boolean onLAN;

    // Locks
    private final Object identityWaiter = new Object();
    private final Object identityAcceptWaiter = new Object();
    private final Object pongWaiter = new Object();
    // Lock for sending message
    private final Object sendLock = new Object();

    /**
     * If true all bandwidth limits are omitted, if false it's handled message
     * based
     */
    private boolean omitBandwidthLimit;

    /**
     * Builds a new anonymous connection manager for the socket.
     * 
     * @param socket
     * @throws ConnectionException
     */
    public ConnectionHandler(Controller controller, Socket socket)
        throws ConnectionException
    {
        super(controller);
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
        this.socket = socket;
        this.serializer = new ByteSerializer();
        this.messagesToSend = Collections
            .synchronizedList(new LinkedList<AsynchronMessage>());
        long startTime = System.currentTimeMillis();

        try {
            out = new LimitedOutputStream(controller.getTransferManager()
                .getOutputLimiter(this), new BufferedOutputStream(socket
                .getOutputStream(), 1024));

            in = new LimitedInputStream(controller.getTransferManager()
                .getInputLimiter(this), new BufferedInputStream(socket
                .getInputStream(), 1024));
            if (logVerbose) {
                log().verbose("Got streams");
            }

            // Start receiver
            receiverThread = new Thread(new Receiver(),
                "ConHandler (recv) for " + socket.getInetAddress() + ":"
                    + socket.getPort());
            // Deamon thread, killed when program is at end
            receiverThread.setDaemon(true);
            receiverThread.start();

            // Start async sender later
            senderThread = new Thread(new Sender(), "ConHandler (send) for "
                + socket.getInetAddress() + ":" + socket.getPort());
            // Deamon thread, killed when program is at end
            senderThread.setDaemon(true);
            senderThread.start();

            // ok, we are connected
            // Generate magic id, 16 byte * 8 * 8 bit = 1024 bit key
            myMagicId = IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId()
                + IdGenerator.makeId() + IdGenerator.makeId();

            // now send identity
            myIdentity = new Identity(controller, controller.getMySelf()
                .getInfo(), myMagicId);
            if (logVerbose) {
                log().verbose(
                    "Sending my identity, nick: '" + myIdentity.member.nick
                        + "', ID: " + myIdentity.member.id);
            }
            sendMessageAsynchron(myIdentity, null);
        } catch (IOException e) {
            shutdownWithMember();
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
        if (identity == null || identity.member == null) {
            shutdown();
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
            log().verbose("Connect took " + took + "ms");
        }

        // Analyse connection
        analyseConnection();
    }

    private boolean shutdown = false;

    /**
     * Shuts down this connection handler by calling shutdown of member. If no
     * associated member is found, the con handler gets directly shut down.
     * <p>
     */
    private void shutdownWithMember() {
        if (getMember() != null) {
            // Shutdown member. This means this connection handle get shut down
            // by member
            getMember().shutdown();
        } else {
            // No member? directly shut down this connection handler
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
        started = false;

        // Clear magic ids
        myMagicId = null;
        remoteMagicId = null;

        // Trigger all waiting treads
        synchronized (identityWaiter) {
            identityWaiter.notifyAll();
        }
        synchronized (identityAcceptWaiter) {
            identityAcceptWaiter.notifyAll();
        }

        if (isConnected() && wasStarted) {
            // Send "EOF" if possible, the last thing you see
            sendMessageAsynchron(new Problem("Closing connection, EOF", true,
                Problem.DISCONNECTED), null);
        }

        if (receiverThread != null) {
            receiverThread.interrupt();
        }

        if (senderThread != null) {
            senderThread.interrupt();
        }

        // close in stream
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ioe) {
            log().error("Could not close in stream", ioe);
        }

        // close out stream
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ioe) {
            log().error("Could not close out stream", ioe);
        }

        // close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log().verbose(e);
            }
        }

        // make sure the garbage collector gets this
        serializer = null;

    }

    /**
     * Answers if the connection is active
     * 
     * @return
     */
    public boolean isConnected() {
        return (socket != null && in != null && out != null
            && socket.isConnected() && !socket.isClosed() && serializer != null);
    }

    /**
     * Answers if this connection is on local net
     * 
     * @return
     */
    public boolean isOnLAN() {
        return onLAN;
    }

    public void setOnLAN(boolean onlan) {
        onLAN = onlan;
        synchronized (out) {
            out.setBandwidthLimiter(getController().getTransferManager()
                .getOutputLimiter(this));
            // System.err.println("LAN:" + out.getBandwidthLimiter());
        }

        // TODO: BYTEKEEPR: from tot: I removed the synchronized block, since
        // this kills connection process under some cirumstances
        // TODO: TOT: from Bytekeeper: This circumstances most likly mean that
        // setOnLAN is called from different threads within a short amount of
        // time.
        // synchronized (in) {
        in.setBandwidthLimiter(getController().getTransferManager()
            .getInputLimiter(this));
        // }
    }

    /**
     * Sets the member, which handles the remote messages
     * 
     * @param member
     */
    public void setMember(Member member) {
        this.member = member;
        // if (member != null && member.isOnLAN()) {
        // getController().getNodeManager().addChatMember(member);
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
        boolean ready = false;
        int nRead = 0;

        synchronized (inStr) {
            do {
                try {
                    nRead += inStr.read(buffer, offset + nRead, size - nRead);
                } catch (IndexOutOfBoundsException e) {
                    log().error("buffer.lenght: " + buffer.length + ", offset");
                    throw e;
                }
                if (nRead < 0) {
                    throw new IOException("EOF, nothing more to read");
                }
                if (nRead >= size) {
                    ready = true;
                }
            } while (!ready);
        }

        // for (int i = 0; i < size; i++) {
        // int read = inStr.read();
        // buffer[offset + i] = (byte) read;
        // }
    }

    /**
     * Sends a message to the remote peer. Waits until send is complete
     * 
     * @param message
     * @throws ConnectionException
     */
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
                // long started = System.currentTimeMillis();

                // byte[] data = ByteSerializer.serialize(message,
                // USE_COMPPRESSION);
                //
                if (!isConnected()) {
                    throw new ConnectionException(
                        "Connection to remote peer closed").with(this);
                }

                // Serialize message, don't compress on LAN
                // unless config says otherwise
                boolean compressed = !onLAN
                    || (onLAN && getController().useZipOnLan());
                // Not reuse old serializer. The serializer does not free up
                // memory
                // byte[] data = serializer.serialize2(message, compressed);
                byte[] data = serializer.serialize(message, compressed);

                if (data.length >= MESSAGE_SIZE_WARNING) {
                    log().error(
                        "Message size exceeds "
                            + Format.formatBytes(MESSAGE_SIZE_WARNING)
                            + "!. Type: "
                            + ClassUtils.getShortClassName(message.getClass())
                            + ", size: " + Format.formatBytes(data.length)
                            + ", message: " + message);
                }

                // byte[] uncompressed = ByteSerializer.serialize(message,
                // false);
                // double compFactor = (-1 + ((double) uncompressed.length)
                // / data.length) * 100;
                // log().warn(
                // "Compressed: " + data.length + " bytes, uncompressed: "
                // + uncompressed.length + ", (" + compFactor
                // + " %), message: " + message);

                // Not limit some pakets
                boolean omittBandwidthLimit = !(message instanceof LimitBandwidth)
                    || this.omitBandwidthLimit;

                // omittBandwidthLimit = true;
                /*
                 * if (!(message instanceof OmitBandwidthLimit)) { log().warn(
                 * "NOT Omitting bandwidth on " + message + ", but should not
                 * omit !"); }
                 */

                // if (message instanceof KnownNodes) {
                // KnownNodes nodeList = (KnownNodes) message;
                // log().warn("NodeList with " + nodeList.nodes.length + ",
                // size: " + Util.formatBytes(data.length));
                // }
                // if (message instanceof FileChunk) {
                // FileChunk fc = (FileChunk) message;
                // log().warn(
                // "Sending filechunk, payload: " + fc.data.length
                // + ", final size: " + data.length + ", overhead: "
                // + (((double) data.length / fc.data.length) - 1)
                // * 100 + " %");
                // }
                // Write paket header / total length
                out.write(Convert.convert2Bytes(data.length));
                // out.flush();

                // Do some calculations before send
                int offset = 0;

                int remaining = data.length;
                synchronized (out) {

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
                }

                // Flush
                out.flush();

                // long took = System.currentTimeMillis() - started;

                // log().warn(
                // "Message (" + data.length + " bytes) took " + took + "ms.");
            }
        } catch (IOException e) {
            // shutdown this peer
            shutdownWithMember();
            throw new ConnectionException(
                "Unable to send message to peer, connection closed", e).with(
                member).with(this);
        }
    }

    /**
     * Sends multiple messages ansychron, all with error message = null
     * 
     * @param messages
     *            the messages to send
     */
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
    public void sendMessageAsynchron(Message message, String errorMessage) {
        Reject.ifNull(message, "Message is null");

        boolean breakConnection = false;
        synchronized (messagesToSend) {
            // Check buffer overrun
            boolean heavyOverflow = messagesToSend.size() >= Constants.HEAVY_OVERFLOW_SEND_BUFFER;
            boolean lightOverflow = messagesToSend.size() >= Constants.LIGHT_OVERFLOW_SEND_BUFFER;
            if (lightOverflow || heavyOverflow) {
                log().warn(
                    "Send buffer overflow, " + messagesToSend.size()
                        + " in buffer. Message: " + message);
                if (sendBufferOverrunSince == null) {
                    sendBufferOverrunSince = new Date();
                }

                breakConnection = System.currentTimeMillis()
                    - Constants.MAX_TIME_WITH_SEND_BUFFER_OVERFLOW > sendBufferOverrunSince
                    .getTime()
                    || heavyOverflow;
            } else {
                // No overrun
                sendBufferOverrunSince = null;
            }

            messagesToSend.add(new AsynchronMessage(message, errorMessage));
            messagesToSend.notifyAll();
        }

        if (breakConnection) {
            // Overflow is too heavy. kill handler
            log().warn("Send buffer overrun is to heavy, disconnecting");
            shutdownWithMember();
        }
    }

    /**
     * Returns the time difference between this client and the remote client in
     * milliseconds. If the remote client doesn't provide the time info
     * (security setting or old client) this method returns 0. To check if the
     * returned value would be valid, call canMeasureTimeDifference() first.
     * 
     * @return
     */
    public long getTimeDeltaMS() {
        if (identity.getTimeGMT() == null)
            return 0;
        return myIdentity.getTimeGMT().getTimeInMillis()
            - identity.getTimeGMT().getTimeInMillis();
    }

    /**
     * Checks if we can measure the time difference between our location and the
     * remote location.
     * 
     * @return
     */
    public boolean canMeasureTimeDifference() {
        return identity.getTimeGMT() != null;
    }

    /**
     * Returns the remote identity of peer
     * 
     * @return the identity
     */
    public Identity getIdentity() {
        return identity;
    }

    /**
     * Returns our magic id, which has been sent to the remote side
     * 
     * @return
     */
    public String getMyMagicId() {
        return myMagicId;
    }

    /**
     * Returns the magic id, which has been sent by the remote side
     * 
     * @return
     */
    public String getRemoteMagicId() {
        return remoteMagicId;
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

    /**
     * Waits unitl remote peer has accepted our identity
     * 
     * @return true if our identity was accepted
     * @throws ConnectionException
     *             if not accepted
     */
    public boolean waitForIdentityAccept() throws ConnectionException {
        // wait for accept of our identity
        synchronized (identityAcceptWaiter) {
            if (identityReply == null) {
                try {
                    identityAcceptWaiter.wait(60000);
                } catch (InterruptedException e) {
                    log().verbose(e);
                }
            }
        }

        if (!isConnected()) {
            throw new ConnectionException(
                "Remote member disconnected while waiting for handshake. "
                    + identity).with(this);
        }

        if (identityReply == null) {
            throw new ConnectionException(
                "Remote peer timed out, while waiting for accept").with(this);
        }

        if (identityReply.accepted) {
            log().debug("Identity accepted by remote peer. " + this);
        } else {
            log().warn("Identity rejected by remote peer. " + this);
        }

        return identityReply.accepted;
    }

    /**
     * Waits for the send queue to get send
     */
    public void waitForEmptySendQueue() {
        boolean waited = false;
        while (!messagesToSend.isEmpty() && isConnected()) {
            try {
                // log().verbose("Waiting for empty send buffer");
                waited = true;
                // Wait a bit the let the send queue get empty
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log().verbose(e);
                break;
            }
        }
        if (waited) {
            if (logVerbose) {
                log().verbose(
                    "Waited for empty sendbuffer, clear now, proceeding");
            }
        }
    }

    /**
     * Analysese the connection of the user
     */
    public void analyseConnection() {
        if (getRemoteAddress() != null
            && getRemoteAddress().getAddress() != null)
        {
            // The NetworkHelper class supports only windows atm 
            // bytekeeper your code was flawed, the method isOnAnySubnet() would
            // return null if dll was not loaded, so the onLan status was set to
            // false.
            // Now I check if the dll is really loaded:
            if (NetworkUtil.isOnAnySubnetSupported()) {
                log().debug("isOnAnySubnetSupported");

                Inet4Address addr = (Inet4Address) getRemoteAddress()
                    .getAddress();
                setOnLAN(NetworkUtil.isOnAnySubnet(addr)
                    || NetworkUtil.isOnLanOrLoopback(addr));

            } else {
                log().debug("NOT isOnAnySubnetSupported");
                setOnLAN(NetworkUtil.isOnLanOrLoopback(getRemoteAddress()
                    .getAddress()));
            }

            // Check if the remote address is one of this machine's
            // interfaces.
            try {
                omitBandwidthLimit = NetworkUtil.getAllLocalNetworkAddresses()
                    .containsKey(socket.getInetAddress());
            } catch (SocketException e) {
                log().error("Omitting bandwidth", e);
            }
        }

        if (!onLAN && !getController().isSilentMode()) {
            // testConnection();
        }
    }

    /**
     * Returns the remote socket address (ip/port)
     * 
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    /**
     * Get the remote port to connect to
     * 
     * @return
     */
    public int getRemoteListenerPort() {
        if (identity != null && identity.member != null
            && identity.member.getConnectAddress() != null)
        {
            return identity.member.getConnectAddress().getPort();
        }
        return -1;
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

    // Logger methods *********************************************************

    public String getLoggerName() {
        String remoteInfo;
        if (socket != null) {
            InetSocketAddress addr = (InetSocketAddress) socket
                .getRemoteSocketAddress();
            remoteInfo = addr.getAddress().getHostAddress() + "^"
                + addr.getPort();
        } else {
            remoteInfo = "<unknown>";
        }
        return "ConnectionHandler " + remoteInfo;
    }

    /*
     * General ****************************************************************
     */

    public String toString() {
        if (socket == null) {
            return "-disconnected-";
        }
        synchronized (socket) {
            return socket.getInetAddress() + ":" + socket.getPort();
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
        long waitTime = getController().getWaitTime();

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (logVerbose) {
                    log().verbose(
                        "Asynchron message send triggered, sending "
                            + messagesToSend.size() + " message(s)");
                }

                if (!isConnected()) {
                    // Client disconnected, stop
                    break;
                }

                while (!messagesToSend.isEmpty()) {
                    // Send as single message
                    AsynchronMessage asyncMsg = messagesToSend.remove(0);

                    try {
                        // log().warn("Sending async: " +
                        // asyncMsg.getMessage());
                        sendMessage(asyncMsg.getMessage());

                        // log().warn("Send complete: " +
                        // asyncMsg.getMessage());
                    } catch (ConnectionException e) {
                        log().verbose(
                            "Unable to send message asynchronly. "
                                + e.getMessage(), e);
                        // Stop thread execution
                        return;
                    }
                }

                // Wait to be notified of new messages
                synchronized (messagesToSend) {
                    try {
                        messagesToSend.wait();
                    } catch (InterruptedException e) {
                        log().verbose(e);
                        break;
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

            while (!Thread.currentThread().isInterrupted()) {
                // check connection status
                if (!isConnected()) {
                    break;
                }

                try {
                    // Read data header, total size
                    read(in, sizeArr, 0, sizeArr.length);
                    int totalSize = Convert.convert2Int(sizeArr);
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

                    boolean expectCompressed = !onLAN;
                    // Allocate receive buffer
                    // byte[] receiveBuffer = new byte[totalSize];
                    // read(in, receiveBuffer, 0, totalSize);
                    Object obj = serializer.deserialize(in, totalSize,
                        expectCompressed);
                    // log().warn("Received " + data.length + " bytes");

                    // Object obj =
                    // ByteSerializer.deserializeStatic(receiveBuffer,
                    // expectCompressed);

                    if (logVerbose) {
                        log().verbose("<- (received) - " + obj);
                    }

                    // if (receiveBuffer.length >= MESSAGE_SIZE_WARNING) {
                    // log().warn(
                    // "Recived buffer exceeds 50KB!. Type: "
                    // + ClassUtils.getShortClassName(obj.getClass())
                    // + ", size: " + Format.formatBytes(receiveBuffer.length)
                    // + ", message: " + obj);
                    // }

                    if (!getController().isStarted()) {
                        log().error("Peer still active! " + getMember());
                        break;
                    }

                    if (obj instanceof Identity) {
                        if (logVerbose) {
                            log().verbose("Received remote identity: " + obj);
                        }
                        // the remote identity
                        identity = (Identity) obj;

                        // Get magic id
                        remoteMagicId = identity.magicId;
                        if (logVerbose) {
                            log().verbose("Received magicId: " + remoteMagicId);
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
                        sendMessageAsynchron(pong, null);

                    } else if (obj instanceof Pong) {
                        // Notify pong waiters
                        synchronized (pongWaiter) {
                            pongWaiter.notifyAll();
                        }

                    } else if (obj instanceof Problem) {
                        Problem problem = (Problem) obj;
                        if (member != null) {
                            member.handleMessage(problem);
                        } else {
                            log().error(
                                "("
                                    + (identity != null
                                        ? identity.member.nick
                                        : "-") + ") Problem received: "
                                    + problem.message);
                            if (problem.fatal) {
                                // Fatal problem, disconnecting
                                break;
                            }
                        }

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
                } catch (IOException e) {
                    log().verbose(e);
                    logConnectionClose(e);
                    break;
                } catch (ClassNotFoundException e) {
                    log().verbose(e);
                    log().warn(
                        "Received unknown packet/class: " + e.getMessage()
                            + " from " + this);
                    // do not break connection
                }
            }

            // Shut down
            shutdownWithMember();
        }
    }
}