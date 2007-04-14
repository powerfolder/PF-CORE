package de.dal33t.powerfolder.net;

import java.net.InetSocketAddress;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.Message;

/**
 * Base interface for all connection handlers doing basic I/O communication.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface ConnectionHandler {

    /**
     * Initalizes the connection handler.
     * 
     * @param controller
     * @param aSocket
     *            the tctp/ip socket.
     * @throws ConnectionException
     */
    public abstract void init() throws ConnectionException;

    /**
     * Shuts down the connection handler. The member is shut down optionally
     */
    public abstract void shutdown();

    /**
     * @return true if the connection is active
     */
    public abstract boolean isConnected();

    /**
     * @return false, no encryption supported.
     */
    public abstract boolean isEnrypted();

    /**
     * @return if this connection is on local net
     */
    public abstract boolean isOnLAN();

    public abstract void setOnLAN(boolean onlan);

    /**
     * Sets the member, which handles the remote messages
     * 
     * @param member
     */
    public abstract void setMember(Member member);

    public abstract Member getMember();

    /**
     * Sends a message to the remote peer. Waits until send is complete
     * 
     * @param message
     * @throws ConnectionException
     */
    public abstract void sendMessage(Message message)
        throws ConnectionException;

    /**
     * Sends multiple messages ansychron, all with error message = null
     * 
     * @param messages
     *            the messages to send
     */
    public abstract void sendMessagesAsynchron(Message... messages);

    /**
     * @return the time difference between this client and the remote client in
     *         milliseconds. If the remote client doesn't provide the time info
     *         (security setting or old client) this method returns 0. To check
     *         if the returned value would be valid, call
     *         canMeasureTimeDifference() first.
     */
    public abstract long getTimeDeltaMS();

    /**
     * @return true if we can measure the time difference between our location
     *         and the remote location.
     */
    public abstract boolean canMeasureTimeDifference();

    /**
     * Returns the remote identity of peer
     * 
     * @return the identity
     */
    public abstract Identity getIdentity();

    /**
     * @return our magic id, which has been sent to the remote side
     */
    public abstract String getMyMagicId();

    /**
     * @return the magic id, which has been sent by the remote side
     */
    public abstract String getRemoteMagicId();

    /**
     * Waits unitl remote peer has accepted our identity
     * 
     * @return true if our identity was accepted
     * @throws ConnectionException
     *             if not accepted
     */
    public abstract boolean waitForIdentityAccept() throws ConnectionException;

    /**
     * Waits for the send queue to get send
     */
    public abstract void waitForEmptySendQueue();

    /**
     * Callback method from <code>#Member.completeHandshake()</code>. Called
     * after a successfull handshake. At this point the connection handler can
     * insert a "veto" against this connection, which leads to a disconnect
     * before the node is completely connected.
     * <p>
     * ATTENTION: Never call this method from anywhere else!
     * 
     * @return true if this connection is accepted the handshake will be
     *         completed. false when the handshakre should be be aborted and the
     *         member disconnected
     */
    public abstract boolean acceptHandshake();

    /**
     * @return the remote socket address (ip/port)
     */
    public abstract InetSocketAddress getRemoteAddress();

    /**
     * @return the remote port to connect to
     */
    public abstract int getRemoteListenerPort();

}