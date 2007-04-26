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

    // Basic initalization/shutdown *******************************************

    /**
     * Initalizes the connection handler.
     * 
     * @throws ConnectionException
     */
    void init() throws ConnectionException;

    /**
     * Shuts down the connection handler. The member is shut down optionally
     */
    void shutdown();

    // Handshake methods ******************************************************

    /**
     * Waits unitl remote peer has accepted our identity
     * 
     * @return true if our identity was accepted
     * @throws ConnectionException
     *             if not accepted
     */
    boolean waitForIdentityAccept() throws ConnectionException;

    /**
     * Waits for the send queue to get send
     */
    void waitForEmptySendQueue();

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
    boolean acceptHandshake();

    // Getters/Setters ********************************************************

    /**
     * @return true if the connection is active
     */
    boolean isConnected();

    /**
     * @return false, no encryption supported.
     */
    boolean isEnrypted();

    /**
     * @return the remote socket address (ip/port)
     */
    InetSocketAddress getRemoteAddress();

    /**
     * @return the remote port to connect to
     */
    int getRemoteListenerPort();

    /**
     * @return if this connection is on local net
     */
    boolean isOnLAN();

    /**
     * @param onlan
     *            true if this connection had be detected on lan
     */
    void setOnLAN(boolean onlan);

    /**
     * @return the time difference between this client and the remote client in
     *         milliseconds. If the remote client doesn't provide the time info
     *         (security setting or old client) this method returns 0. To check
     *         if the returned value would be valid, call
     *         canMeasureTimeDifference() first.
     */
    long getTimeDeltaMS();

    /**
     * @return true if we can measure the time difference between our location
     *         and the remote location.
     */
    boolean canMeasureTimeDifference();

    /**
     * @return the remote identity of peer
     */
    Identity getIdentity();

    /**
     * Sets the member, which handles the remote messages
     * 
     * @param member
     */
    void setMember(Member member);

    /**
     * @return the member associated with this connection handler.
     */
    Member getMember();

    /**
     * @return our magic id, which has been sent to the remote side
     */
    String getMyMagicId();

    /**
     * @return the magic id, which has been sent by the remote side
     */
    String getRemoteMagicId();

    // IO Operations **********************************************************

    /**
     * Sends a message to the remote peer. Waits until send is complete
     * 
     * @param message
     * @throws ConnectionException
     */
    void sendMessage(Message message) throws ConnectionException;

    /**
     * Sends multiple messages ansychron, all with error message = null
     * 
     * @param messages
     *            the messages to send
     */
    void sendMessagesAsynchron(Message... messages);
}