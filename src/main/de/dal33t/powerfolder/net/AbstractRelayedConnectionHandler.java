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
package de.dal33t.powerfolder.net;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.IdentityReply;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.Pong;
import de.dal33t.powerfolder.message.Problem;
import de.dal33t.powerfolder.message.RelayedMessage;
import de.dal33t.powerfolder.message.RelayedMessage.Type;
import de.dal33t.powerfolder.message.RelayedMessageExt;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;

/**
 * The base super class for connection which get relayed through a third node.
 * <p>
 * TRAC #597
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class AbstractRelayedConnectionHandler extends PFComponent
    implements ConnectionHandler
{

    /** The relay to use */
    private Member relay;

    /**
     * The connection id
     */
    private long connectionId;

    /**
     * The aimed remote for this connection.
     */
    private MemberInfo remote;

    /** The assigned member */
    private Member member;

    // Our identity
    private Identity myIdentity;

    // Identity of remote peer
    private Identity identity;
    private IdentityReply identityReply;
    // The magic id, which has been send to the remote peer
    private String myMagicId;

    private ByteSerializer serializer;

    // The send buffer
    private Queue<Message> messagesToSendQueue;

    private boolean started;

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
     * Flag that indicates a received ACK. relyed connection is ready for
     * traffic.
     */
    private boolean ackReceived;

    /**
     * Flag that indicates a received NACK. relyed connection is cannot be
     * established
     */
    private boolean nackReceived;

    /**
     * Builds a new anonymous connection manager using the given relay.
     * <p>
     * Should be called from <code>ConnectionHandlerFactory</code> only.
     * 
     * @see ConnectionHandlerFactory
     * @param controller
     *            the controller.
     * @param remote
     *            the aimed remote side to connect.
     * @param relay
     *            the relay to use.
     * @throws ConnectionException
     */
    protected AbstractRelayedConnectionHandler(Controller controller,
        MemberInfo remote, long connectionId, Member relay)
    {
        super(controller);
        Reject.ifNull(remote, "Remote is null");
        Reject.ifNull(relay, "Relay is null");
        this.remote = remote;
        this.relay = relay;
        this.serializer = new ByteSerializer();
        this.connectionId = connectionId;
    }

    // Abstract behaviour *****************************************************

    /**
     * Called before the message gets actally written into the
     * <code>RelayedMessage</code>
     * 
     * @param message
     *            the message to serialize
     * @return the serialized message
     */
    protected abstract byte[] serialize(Message message)
        throws ConnectionException;

    /**
     * Called when the data got read from the <code>RelayedMessage</code>.
     * Should re-construct the serialized object from the data.
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
     * @return the relay
     */
    protected Member getRelay() {
        return relay;
    }

    /**
     * Initializes the connection handler.
     * 
     * @throws ConnectionException
     */
    public void init() throws ConnectionException {
        if (!relay.isCompletelyConnected()) {
            throw new ConnectionException("Connection to peer is closed")
                .with(this);
        }
        this.started = true;
        // Don't clear, might have been already received!
        // this.identity = null;
        // this.identityReply = null;
        this.messagesToSendQueue = new ConcurrentLinkedQueue<Message>();
        this.senderSpawnLock = new ReentrantLock();
        long startTime = System.currentTimeMillis();

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

        // Send identity
        sendMessagesAsynchron(myIdentity);

        waitForRemoteIdentity();

        if (!isConnected()) {
            shutdown();
            throw new ConnectionException(
                "Remote peer disconnected while waiting for his identity")
                .with(this);
        }
        if (identity == null || identity.getMemberInfo() == null) {
            throw new ConnectionException(
                "Did not receive a valid identity from peer after 60s: "
                    + getRemote()).with(this);
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

        getController().getIOProvider().getRelayedConnectionManager()
            .removePedingRelayedConnectionHandler(this);
        getController().getIOProvider().removeKeepAliveCheck(this);

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
        return started && relay.isConnected();
    }

    public boolean isEncrypted() {
        return false;
    }

    public boolean isOnLAN() {
        return false;
    }

    public void setOnLAN(boolean onlan) {
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
     * @return the aimed remote destination for this connection.
     */
    public MemberInfo getRemote() {
        return remote;
    }

    /**
     * @return the unique connection id.
     */
    public long getConnectionId() {
        return connectionId;
    }

    public boolean isAckReceived() {
        return ackReceived;
    }

    public void setAckReceived(boolean ackReceived) {
        this.ackReceived = ackReceived;
    }

    public boolean isNackReceived() {
        return nackReceived;
    }

    public void setNackReceived(boolean nackReceived) {
        this.nackReceived = nackReceived;
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
                if (!isConnected() || !started) {
                    throw new ConnectionException(
                        "Connection to remote peer closed").with(this);
                }
                byte[] data = serialize(message);
                RelayedMessage dataMsg = identity != null
                    && identity.getProtocolVersion() >= Identity.PROTOCOL_VERSION_108
                    && relay.getProtocolVersion() >= Identity.PROTOCOL_VERSION_108
                    ? new RelayedMessageExt(Type.DATA_ZIPPED, getController()
                        .getMySelf().getInfo(), remote, connectionId, data)
                    : new RelayedMessage(Type.DATA_ZIPPED, getController()
                        .getMySelf().getInfo(), remote, connectionId, data);
                relay.sendMessage(dataMsg);

                getController().getTransferManager()
                    .getTotalUploadTrafficCounter()
                    .bytesTransferred(data.length + 4);
            }
        } catch (RuntimeException e) {
            logSevere("Runtime exception while serializing: " + message, e);
            // Ensure shutdown
            shutdownWithMember();
            throw e;
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

        senderSpawnLock.lock();
        messagesToSendQueue.offer(message);
        if (messagesToSendQueue.size() > Constants.WARN_MESSAGES_IN_SEND_QUEUE
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
        if (messagesToSendQueue.size() > Constants.MAX_MESSAGES_IN_SEND_QUEUE) {
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
        senderSpawnLock.unlock();
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
        return ConnectionQuality.POOR;
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
            logFine("Remote member disconnected while waiting for identity reply. "
                + identity);
            member = null;
            return false;
        }

        if (identityReply == null) {
            logFine("Did not receive a identity reply after " + took
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

    public boolean acceptHandshake() {
        // IS not longer pending. Now connected to Member.
        getController().getIOProvider().getRelayedConnectionManager()
            .removePedingRelayedConnectionHandler(this);
        return true;
    }

    public InetSocketAddress getRemoteAddress() {
        return getMember() != null ? getMember().getReconnectAddress() : null;
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

    // Receiving **************************************************************

    private ConcurrentLinkedQueue<RelayedMessage> receiveQueue = new ConcurrentLinkedQueue<RelayedMessage>();
    private AtomicBoolean receiving = new AtomicBoolean(false);

    /**
     * Receives and processes the relayed message.
     * 
     * @param message
     *            the message received from a relay.
     */
    public void receiveRelayedMessage(RelayedMessage message) {
        // in queue for later processing in own thread
        boolean startReceiver = false;
        synchronized (receiveQueue) {
            receiveQueue.offer(message);
            startReceiver = receiving.compareAndSet(false, true);
        }
        if (startReceiver) {
            getController().getIOProvider().startIO(new Runnable() {
                public void run() {
                    while (true) {
                        RelayedMessage rm;
                        synchronized (receiveQueue) {
                            rm = receiveQueue.poll();
                            if (rm == null) {
                                receiving.set(false);
                                break;
                            }
                        }
                        receiveRelayedMessage0(rm);
                    }
                }
            });
        }
    }

    /**
     * Receives and processes the relayed message.
     * 
     * @param message
     *            the message received from a relay.
     */
    private void receiveRelayedMessage0(RelayedMessage message) {
        try {
            // if (!started) {
            // // Do not process this message
            // return;
            // }

            byte[] data = message.getPayload();
            Object obj = deserialize(data, data.length);

            lastKeepaliveMessage = new Date();
            getController().getTransferManager()
                .getTotalDownloadTrafficCounter().bytesTransferred(data.length);

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
                logFiner("<- (received, " + Format.formatBytes(data.length)
                    + ") - " + obj);
            }

            if (!getController().isStarted()) {
                logFiner("Peer still active, shutting down " + getMember());
                shutdownWithMember();
                return;
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
                    logFiner("Received magicId: " + identity.getMagicId());
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
                    member.handleMessage(problem, this);
                } else {
                    logWarning("("
                        + (identity != null
                            ? identity.getMemberInfo().nick
                            : "-") + ") Problem received: " + problem.message);
                    if (problem.fatal) {
                        // Fatal problem, disconnecting
                        shutdown();
                    }
                }

            } else if (receivedObject(obj)) {
                // The object was handled by the subclass.
                // OK pass through
            } else if (obj instanceof Message) {

                if (member != null) {
                    member.handleMessage((Message) obj, this);
                } else if (!isConnected()) {
                    // Simply break. Already disconnected
                    shutdownWithMember();
                } else {
                    logWarning("Connection closed, message received, before peer identified itself: "
                        + obj);
                    // connection closed
                    shutdownWithMember();
                }
            } else {
                logWarning("Received unknown message from peer: " + obj);
            }

        } catch (ConnectionException e) {
            logFiner("ConnectionException", e);
            logConnectionClose(e);

        } catch (ClassNotFoundException e) {
            logFiner("ClassNotFoundException", e);
            logWarning("Received unknown packet/class: " + e.getMessage()
                + " from " + AbstractRelayedConnectionHandler.this);

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < message.getPayload().length; i++) {
                hexString.append(Integer.toHexString(0xFF & message
                    .getPayload()[i]));
            }
            logWarning("On message: " + message + ": " + hexString);

            // do not break connection
        } catch (RuntimeException e) {
            logSevere("RuntimeException", e);
            shutdownWithMember();

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < message.getPayload().length; i++) {
                hexString.append(Integer.toHexString(0xFF & message
                    .getPayload()[i]));
            }
            logWarning("On message: " + message + ": " + hexString);

            throw e;
        }

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
        return "RelayedConHan '" + remote.nick + "-" + connectionId + "'";
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

            int i = 0;
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

                i++;
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
}
