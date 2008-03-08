/* $Id: Member.java,v 1.115 2006/04/23 16:01:35 totmacherr Exp $
 */
package de.dal33t.powerfolder;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.AbortDownload;
import de.dal33t.powerfolder.message.AbortUpload;
import de.dal33t.powerfolder.message.DownloadQueued;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.FolderRelatedMessage;
import de.dal33t.powerfolder.message.HandshakeCompleted;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.IdentityReply;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.KnownNodes;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.NodeInformation;
import de.dal33t.powerfolder.message.Notification;
import de.dal33t.powerfolder.message.Ping;
import de.dal33t.powerfolder.message.Pong;
import de.dal33t.powerfolder.message.Problem;
import de.dal33t.powerfolder.message.RelayedMessage;
import de.dal33t.powerfolder.message.ReplyFilePartsRecord;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFileList;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.ScanCommand;
import de.dal33t.powerfolder.message.SearchNodeRequest;
import de.dal33t.powerfolder.message.SettingsChange;
import de.dal33t.powerfolder.message.StartUpload;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.message.UDTMessage;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.InvalidIdentityException;
import de.dal33t.powerfolder.net.PlainSocketConnectionHandler;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.MessageListenerSupport;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * A full quailfied member, can have a connection to interact with remote
 * member/fried/peer.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.115 $
 */
public class Member extends PFComponent {

    /** Listener support for incoming messages */
    private MessageListenerSupport messageListenerSupport;

    /** The current connection handler */
    private ConnectionHandler peer;

    /**
     * If this node has completely handshaked. TODO: Move this into
     * connectionHandler ?
     */
    private boolean handshaked;

    /** The number of connection retries to the most recent known remote address */
    private int connectionRetries;

    /** The total number of reconnection tries at this moment */
    private int currentReconTries;

    /** Number of interesting marks, set by markAsInteresting */
    private int interestMarks;

    /** his member information */
    private MemberInfo info;

    /** The last time, the node was seen on the network */
    private Date lastNetworkConnectTime;

    /** Lock when peer is going to be initalized */
    private Object peerInitalizeLock = new Object();

    /** Folderlist waiter */
    private Object folderListWaiter = new Object();

    /** Handshake completed waiter */
    private Object handshakeCompletedWaiter = new Object();

    /**
     * Lock to ensure that only one thread executes the folder membership
     * synchronization.
     */
    private Lock folderJoinLock = new ReentrantLock();

    /**
     * The last message indicating that the handshake was completed
     */
    private HandshakeCompleted lastHandshakeCompleted;

    /** Last folder memberships */
    private FolderList lastFolderList;

    /** Last know file list */
    private Map<FolderInfo, Map<FileInfo, FileInfo>> lastFiles;
    /**
     * The number of expected deltas to receive to have the filelist completed
     * on that folder. Might contain negativ values! means we received deltas
     * after the inital filelist.
     */
    private Map<FolderInfo, Integer> expectedListMessages;

    /** Last trasferstatus */
    private TransferStatus lastTransferStatus;

    /**
     * the last problem
     */
    private Problem lastProblem;

    /** maybe we cannot connect, but member might be online */
    private boolean isConnectedToNetwork;

    /** Flag if we received a wrong identity from remote side */
    private boolean receivedWrongRemoteIdentity;

    /** If already asked for friendship */
    private boolean askedForFriendship;

    /**
     * Constructs a member using parameters from another member. nick, id ,
     * connect address.
     * <p>
     * Attention:Does not takes friend status from memberinfo !! you have to
     * manually
     * 
     * @param controller
     *            Reference to the Controller
     * @param mInfo
     *            memberInfo to clone
     */
    public Member(Controller controller, MemberInfo mInfo) {
        super(controller);
        // Clone memberinfo
        // this.info = (MemberInfo) mInfo.clone();
        // HACK: Side effects on memberinfo
        this.info = mInfo;
        this.receivedWrongRemoteIdentity = false;
    }

    /**
     * Constructs a new local member without a connection
     * 
     * @param controller
     * @param nick
     * @param id
     */
    public Member(Controller controller, String nick, String id) {
        this(controller, new MemberInfo(nick, id));
        handshaked = false;
    }

    /**
     * @param searchString
     * @return if this member matches the search string or if it equals the IP
     *         nick contains the search String
     * @see MemberInfo#matches(String)
     */
    public boolean matches(String searchString) {
        return info.matches(searchString);
    }

    public String getHostName() {
        if (getReconnectAddress() == null) {
            return null;
        }
        return getReconnectAddress().getHostName();
    }

    public String getIP() {
        // if (ip == null) {
        if (getReconnectAddress() == null
            || getReconnectAddress().getAddress() == null)
        {
            return null;
        }
        return getReconnectAddress().getAddress().getHostAddress();
        // }
        // return ip;
    }

    public int getPort() {
        if (getReconnectAddress() == null
            || getReconnectAddress().getAddress() == null)
        {
            return 0;
        }
        return getReconnectAddress().getPort();
    }

    /**
     * @return true if the connection to this node is secure.
     */
    public boolean isSecure() {
        return peer != null && peer.isEncrypted();
    }

    /**
     * Answers if this is myself
     * 
     * @return true if this object references to "myself" else false
     */
    public boolean isMySelf() {
        return equals(getController().getMySelf());
    }

    /**
     * Answers if this member is a friend, also true if isMySelf()
     * 
     * @return true if this user is a friend or myself.
     */
    public boolean isFriend() {
        return getController().getNodeManager().isFriend(this);
    }

    /**
     * Sets friend status of this member
     * 
     * @param newFriend
     *            The new friend status.
     */
    public void setFriend(boolean newFriend, String personalMessage) {
        // Inform node manager
        getController().getNodeManager().friendStateChanged(this, newFriend,
            personalMessage);
    }

    /**
     * Marks a node as interesting.
     * <p>
     * Be sure to remove your mark with <code>removeInterestingMark</code>
     * 
     * @see #removedInterestingMark()
     */
    public void markAsIntersting() {
        interestMarks++;
        log().warn("Marked as interesting (" + interestMarks + " marks)");
    }

    /**
     * Marks the node for immediate connection
     */
    public void markForImmediateConnect() {
        getController().getReconnectManager().markNodeForImmediateReconnection(
            this);
    }

    /**
     * Removes the interesting mark and disconnects if node got uninteresting
     * 
     * @see #markAsIntersting()
     */
    public void removedInterestingMark() {
        interestMarks--;
        if (interestMarks < 0) {
            interestMarks = 0;
        }
        boolean wasConnected = isConnected();
        if (!isInteresting()) {
            shutdown();
        }
        log().warn(
            "Removed interesting mark (now " + interestMarks
                + " marks), disconnected ? " + (wasConnected != isConnected()));
    }

    /**
     * Answers if this node is interesting for us, that is defined as friends
     * users on LAN and has joined one of our folders. Or if its a supernode of
     * we are a supernode and there are still open connections slots.
     * 
     * @return true if this node is interesting for us
     */
    public boolean isInteresting() {
        // log().debug("isOnLAN(): " + isOnLAN());
        // log().debug("getController().isLanOnly():" +
        // getController().isLanOnly());

        if (getController().isLanOnly() && !isOnLAN()) {
            return false;
        }

        if (getController().getWebServiceClient().isWebService(this)) {
            // Always interesting is the webservice!
            return true;
        }

        if (getController().getIOProvider().getRelayedConnectionManager()
            .isRelay(getInfo()))
        {
            // Always interesting a relay is!
            return true;
        }

        // log().debug("isFriend(): " + isFriend());
        // log().debug("hasJoinedAnyFolder(): " + hasJoinedAnyFolder());

        if ((interestMarks > 0) || isFriend() || isOnLAN()
            || hasJoinedAnyFolder())
        {
            return true;
        }

        // Still capable of new connections?
        boolean conSlotAvail = !getController().getNodeManager()
            .maxConnectionsReached();
        if (conSlotAvail && getController().getMySelf().isSupernode()) {
            return true;
        }

        // Try to hold connection to supernode if max connections not reached
        // yet.
        if (conSlotAvail && isSupernode()) {
            return getController().getNodeManager().countConnectedSupernodes() < Constants.N_SUPERNODES_TO_CONNECT;
        }

        return false;
    }

    /**
     * Answers if this node is currently reconnecting
     * 
     * @return true if currently reconnecting
     */
    public boolean isReconnecting() {
        return currentReconTries > 0;
    }

    /**
     * Answers if this member has a connected peer (a open socket). To check if
     * a node is completey connected & handshaked see
     * <code>isCompletelyConnected</code>
     * 
     * @see #isCompleteyConnected()
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return peer != null && peer.isConnected();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Answers if this node is completely connected & handshaked
     * 
     * @return true if connected & handshaked
     */
    public boolean isCompleteyConnected() {
        return handshaked && isConnected();
    }

    /**
     * Convinience method
     * 
     * @return true if the node is a supernode
     */
    public boolean isSupernode() {
        return info.isSupernode;
    }

    public boolean isSupportingPartTransfers() {
        return isCompleteyConnected()
            && getPeer().getIdentity().isSupportingPartTransfers();
    }

    /**
     * Answers if this member is on the local area network.
     * 
     * @return true if this member is on LAN.
     */
    public boolean isOnLAN() {
        if (peer != null) {
            return peer.isOnLAN();
        }
        if (info.getConnectAddress() == null) {
            return false;
        }
        InetAddress adr = info.getConnectAddress().getAddress();
        if (adr == null) {
            return false;
        }
        return NetworkUtil.isOnLanOrLoopback(adr)
            || getController().getNodeManager().isNodeOnConfiguredLan(adr);
    }

    /**
     * To set the lan status of the member for external source
     * 
     * @param onlan
     *            new LAN status
     */
    public void setOnLAN(boolean onlan) {
        if (peer != null) {
            peer.setOnLAN(onlan);
        }
    }

    /**
     * Answers if we received a wrong identity on reconnect
     * 
     * @return true if we received a wrong identity on reconnect
     */
    public boolean receivedWrongIdentity() {
        return receivedWrongRemoteIdentity;
    }

    /**
     * removes the peer handler, and shuts down connection
     */
    private void shutdownPeer() {
        if (peer != null) {
            peer.shutdown();
            synchronized (peerInitalizeLock) {
                peer = null;
            }
        }
    }

    /**
     * @return the peer of this member.
     */
    public ConnectionHandler getPeer() {
        return peer;
    }

    /**
     * Sets the new connection handler for this member
     * 
     * @param newPeer
     *            The peer / connection handler to set
     * @throws InvalidIdentityException
     *             if peer identity doesn't match this member.
     * @return true if the peer was accepted and is now active.
     */
    public boolean setPeer(ConnectionHandler newPeer)
        throws InvalidIdentityException
    {
        Reject.ifNull(newPeer, "Illegal call of setPeer(null)");

        if (!newPeer.isConnected()) {
            log().warn(
                "Peer disconnected while initializing connection: " + newPeer);
            return false;
        }

        if (logVerbose) {
            log().verbose("Setting peer to " + newPeer);
        }

        Identity identity = newPeer.getIdentity();
        MemberInfo remoteMemberInfo = identity != null ? identity
            .getMemberInfo() : null;

        // check if identity is valid and matches the this member
        if (identity == null || !identity.isValid()
            || !remoteMemberInfo.matches(this))
        {
            // Wrong identity from remote side ? set our flag
            receivedWrongRemoteIdentity = remoteMemberInfo != null
                && !remoteMemberInfo.matches(this);

            String identityId = identity != null
                ? identity.getMemberInfo().id
                : "n/a";

            // tell remote client
            try {
                newPeer.sendMessage(IdentityReply.reject("Invalid identity: "
                    + identityId + ", expeced " + info));
            } catch (ConnectionException e) {
                log().verbose("Unable to send identity reject", e);
            } finally {
                newPeer.shutdown();
            }
            throw new InvalidIdentityException(this
                + " Remote peer has wrong identity. remote ID: " + identityId
                + ", our ID: " + this.getId(), newPeer);
        }

        // Complete low-level handshake
        boolean accepted = newPeer.acceptIdentity(this);

        if (!accepted) {
            // Shutdown this member
            newPeer.shutdown();
            log()
                .verbose("Remote side did not accept our identity: " + newPeer);
            return false;
        }

        synchronized (peerInitalizeLock) {
            // ok, we accepted, kill old peer and shutdown.
            // shutdown old peer
            shutdownPeer();

            // Set the new peer
            peer = newPeer;
        }

        // Update infos!
        if (newPeer.getRemoteListenerPort() > 0) {
            // get the data from remote peer
            // connect address is his currently connected ip + his
            // listner port if not supernode
            if (newPeer.isOnLAN()) {
                // Supernode state no nessesary on lan
                // Take socket ip as reconnect address
                info.isSupernode = false;
                info.setConnectAddress(new InetSocketAddress(newPeer
                    .getRemoteAddress().getAddress(), newPeer
                    .getRemoteListenerPort()));
            } else if (identity.getMemberInfo().isSupernode) {
                // Remote peer is supernode, take his info, he knows
                // about himself (=reconnect hostname)
                info.isSupernode = true;
                info.setConnectAddress(identity.getMemberInfo()
                    .getConnectAddress());
            } else {
                // No supernode. take socket ip as reconnect address.
                info.isSupernode = false;
                info.setConnectAddress(new InetSocketAddress(newPeer
                    .getRemoteAddress().getAddress(), newPeer
                    .getRemoteListenerPort()));
            }
        } else if (!identity.isTunneled()) {
            // Remote peer has no listener running
            info.setConnectAddress(null);
            // Don't change the connection address on a tunneled connection.
        }

        info.id = identity.getMemberInfo().id;
        info.nick = identity.getMemberInfo().nick;
        // Reset the last connect time
        info.lastConnectTime = new Date();

        return completeHandshake();
    }

    /**
     * Calls which can only be executed with connection
     * 
     * @throws ConnectionException
     *             if not connected
     */
    private void checkPeer() throws ConnectionException {
        if (!isConnected()) {
            shutdownPeer();
            throw new ConnectionException("Not connected").with(this);
        }
    }

    /**
     * Tries to reconnect peer
     * 
     * @return true if succeeded
     * @throws InvalidIdentityException
     */
    public boolean reconnect() throws InvalidIdentityException {
        // do not reconnect if controller is not running
        if (!getController().isStarted()) {
            return false;
        }
        if (isConnected()) {
            return true;
        }
        if (info.getConnectAddress() == null) {
            return false;
        }
        if (logDebug) {
            log().debug(
                "Reconnecting (tried " + connectionRetries + " time(s) to "
                    + this + ")");
        }

        connectionRetries++;
        boolean successful = false;
        ConnectionHandler handler = null;
        try {
            if (info.getConnectAddress().getPort() <= 0) {
                log().warn(
                    this + " has illegal connect port "
                        + info.getConnectAddress().getPort());
                return false;
            }

            // Set reconnecting state
            currentReconTries++;

            // Re-resolve connect address
            String theHostname = getHostName(); // cached hostname
            if (logVerbose) {
                log().verbose(
                    "Reconnect hostname to " + getNick() + " is: "
                        + theHostname);
            }
            if (!StringUtils.isBlank(theHostname)) {
                info.setConnectAddress(new InetSocketAddress(theHostname, info
                    .getConnectAddress().getPort()));
            }

            // Another check: do not reconnect if controller is not running
            if (!getController().isStarted()) {
                return false;
            }

            // Try to establish a low-level connection.
            handler = getController().getIOProvider()
                .getConnectionHandlerFactory().tryToConnect(this.getInfo());
            successful = setPeer(handler);
        } catch (InvalidIdentityException e) {
            log().verbose(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
            throw e;
        } catch (ConnectionException e) {
            log().debug(e.getMessage());
            log().verbose(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
        } finally {
            currentReconTries--;
        }

        if (successful) {
            isConnectedToNetwork = true;
            connectionRetries = 0;
        } else {
            if (connectionRetries >= 15 && isConnectedToNetwork) {
                log().warn("Unable to connect directly");
                // FIXME: Find a better ways
                // unableToConnect = true;
                isConnectedToNetwork = false;
            }
        }

        // log().warn("Reconnect over, now connected: " + successful);

        return successful;
    }

    /**
     * Completes the handshake between nodes. Exchanges the relevant information
     * 
     * @return true when handshake was successfully and user is now connected
     */
    private boolean completeHandshake() {
        if (!isConnected()) {
            return false;
        }
        boolean thisHandshakeCompleted = true;
        Identity identity = peer.getIdentity();

        synchronized (peerInitalizeLock) {
            if (!isConnected() || identity == null) {
                log().debug("Disconnected while completing handshake");
                return false;
            }
            // Send node informations now
            // Send joined folders to synchronize
            FolderList folderList = new FolderList(getController()
                .getFolderRepository().getJoinedFolderInfos(), peer
                .getRemoteMagicId());
            peer.sendMessagesAsynchron(folderList);
        }

        // My messages sent, now wait for his folder list.
        boolean receivedFolderList = waitForFolderList();
        synchronized (peerInitalizeLock) {
            if (!isConnected()) {
                log().debug("Disconnected while completing handshake");
                return false;
            }
            if (!receivedFolderList) {
                if (isConnected()) {
                    log()
                        .debug(
                            "Did not receive a folder list after 60s, disconnecting");
                    return false;
                }
                peer.shutdown();
                return false;
            }
            if (!isConnected()) {
                log().debug("Disconnected while waiting for folder list");
                return false;
            }
        }

        // Create request for nodelist.
        RequestNodeList request = getController().getNodeManager()
            .createDefaultNodeListRequestMessage();

        synchronized (peerInitalizeLock) {
            if (!isConnected()) {
                log().debug("Disconnected while completing handshake");
                return false;
            }

            if (!isInteresting()) {
                log().debug("Rejected, Node not interesting");
                // Tell remote side
                try {
                    peer.sendMessage(new Problem("You are boring", true,
                        Problem.DO_NOT_LONGER_CONNECT));
                } catch (ConnectionException e) {
                    // Ignore
                }
                thisHandshakeCompleted = false;
            } else {
                // Send request for nodelist.
                peer.sendMessagesAsynchron(request);

                // Send our transfer status
                peer.sendMessagesAsynchron(getController().getTransferManager()
                    .getStatus());
            }
        }

        boolean acceptByConnectionHandler = peer != null
            && peer.acceptHandshake();
        // Handshaked ?
        thisHandshakeCompleted = thisHandshakeCompleted && isConnected()
            && acceptByConnectionHandler;

        if (!thisHandshakeCompleted) {
            if (logVerbose) {
                log().verbose(
                    "not handshaked: connected? " + isConnected()
                        + ", acceptByCH? " + acceptByConnectionHandler
                        + ", interesting? " + isInteresting() + ", peer "
                        + peer);
            }
            shutdown();
            return false;
        }

        List<Folder> joinedFolders = getJoinedFolders();
        if (joinedFolders.size() > 0) {
            log()
                .debug(
                    "Joined " + joinedFolders.size() + " folders: "
                        + joinedFolders);
        }

        for (Folder folder : joinedFolders) {
            // Send filelist of joined folders
            sendMessagesAsynchron(FileList.createFileListMessages(folder));
        }

        boolean ok = waitForFileLists(joinedFolders);
        if (!ok) {
            log().warn("Disconnecting. Did not receive the full filelists");

            for (Folder folder : joinedFolders) {
                log().debug(
                    "Got filelist for " + folder.getName() + " ? "
                        + hasCompleteFileListFor(folder.getInfo()));
            }
            shutdown();
            return false;
        }
        if (logVerbose) {
            log().verbose("Got complete filelists");
        }

        // Wait for acknowledgement from remote side
        if (identity.isAcknowledgesHandshakeCompletion()) {
            sendMessageAsynchron(new HandshakeCompleted(), null);
            long start = System.currentTimeMillis();
            if (!waitForHandshakeCompletion()) {
                long took = System.currentTimeMillis() - start;
                if (peer == null || !peer.isConnected()) {
                    if (lastProblem == null) {
                        log()
                            .warn(
                                "Peer disconnected while waiting for handshake acknownledge (or problem)");
                    }
                } else {
                    if (lastProblem == null) {
                        log()
                            .warn(
                                "Did not receive a handshake not acknownledged (or problem) by remote side after "
                                    + (int) (took / 1000) + "s");
                    }
                }
                if (peer != null) {
                    peer.shutdown();
                }
                return false;
            } else if (logVerbose) {
                log().verbose("Got handshake completion!!");
            }
        }

        synchronized (peerInitalizeLock) {
            if (peer != null && !peer.isConnected()) {
                peer.shutdown();
                return false;
            }
        }
        handshaked = thisHandshakeCompleted;
        // Reset things
        connectionRetries = 0;

        // Inform nodemanger about it
        getController().getNodeManager().onlineStateChanged(this);

        if (logEnabled) {
            log().info(
                "Connected ("
                    + getController().getNodeManager().countConnectedNodes()
                    + " total)");
        }

        // Request files
        for (Folder folder : joinedFolders) {
            // Trigger filerequesting. we may want re-request files on a
            // folder he joined.
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(folder.getInfo());
        }

        if (getController().isDebugReports()) {
            // Running with debugReports enabled (which incorporates verbose
            // mode)
            // then directly request node information.
            sendMessageAsynchron(new RequestNodeInformation(), null);
        }

        return handshaked;
    }

    /**
     * Waits for the filelists on those folders. After a certain amount of time
     * it runs on a timeout if no filelists were received. Waits max 2 minutes.
     * 
     * @param folders
     * @return true if the filelists of those folders received successfully.
     */
    private boolean waitForFileLists(List<Folder> folders) {
        if (logVerbose) {
            log().verbose("Waiting for complete fileslists...");
        }
        Waiter waiter = new Waiter(1000L * 60 * 50);
        boolean fileListsCompleted = false;
        while (!waiter.isTimeout() && isConnected()) {
            fileListsCompleted = true;
            for (Folder folder : folders) {
                if (!hasCompleteFileListFor(folder.getInfo())) {
                    fileListsCompleted = false;
                    break;
                }
            }
            if (fileListsCompleted) {
                break;
            }
            waiter.waitABit();
        }
        if (waiter.isTimeout()) {
            log().error(
                "Got timeout (" + (waiter.getTimoutTimeMS() / (1000 * 60))
                    + " minutes) while waiting for filelist");
        }
        if (!isConnected()) {
            log().error("Disconnected while waiting for filelist");
        }
        return fileListsCompleted;
    }

    /**
     * Waits some time for the folder list
     * 
     * @return true if list was received successfully
     */
    private boolean waitForFolderList() {
        synchronized (folderListWaiter) {
            if (getLastFolderList() == null) {
                try {
                    if (logVerbose) {
                        log().verbose("Waiting for folderlist");
                    }
                    folderListWaiter.wait(60000);
                } catch (InterruptedException e) {
                    log().verbose(e);
                }
            }
        }
        return getLastFolderList() != null;
    }

    /**
     * Waits some time for the handshake to be completed
     * 
     * @return true if list was received successfully
     */
    private boolean waitForHandshakeCompletion() {
        synchronized (handshakeCompletedWaiter) {
            if (lastHandshakeCompleted == null) {
                try {
                    if (logVerbose) {
                        log().verbose("Waiting for handshake completions");
                    }
                    handshakeCompletedWaiter
                        .wait(Constants.INCOMING_CONNECTION_TIMEOUT * 1000);
                } catch (InterruptedException e) {
                    log().verbose(e);
                }
            }
        }
        return lastHandshakeCompleted != null;
    }

    /**
     * Shuts the member and its connection down
     */
    public void shutdown() {
        boolean wasHandshaked = handshaked;

        // Notify waiting locks.
        synchronized (folderListWaiter) {
            folderListWaiter.notifyAll();
        }
        synchronized (handshakeCompletedWaiter) {
            handshakeCompletedWaiter.notifyAll();
        }

        lastFiles = null;
        lastFolderList = null;
        // Disco, assume completely
        setConnectedToNetwork(false);
        handshaked = false;
        lastHandshakeCompleted = null;
        lastTransferStatus = null;
        expectedListMessages = null;
        shutdownPeer();
        messageListenerSupport = null;
        if (wasHandshaked) {
            // Node went offline. Break all downloads from him
            getController().getTransferManager().breakTransfers(this);
            // Inform nodemanger about it
            getController().getNodeManager().onlineStateChanged(this);

            if (logEnabled) {
                log().info(
                    "Disconnected ("
                        + getController().getNodeManager()
                            .countConnectedNodes() + " still connected)");
            }
        } else {
            // log().verbose("Shutdown");
        }
    }

    /**
     * Helper method for sending messages on peer handler. Method waits for the
     * sendmessagebuffer to get empty
     * 
     * @param message
     *            The message to send
     * @throws ConnectionException
     */
    public void sendMessage(Message message) throws ConnectionException {
        checkPeer();

        if (peer != null) {
            // wait
            peer.waitForEmptySendQueue(-1);
            // synchronized (peerInitalizeLock) {
            if (peer != null) {
                // send
                peer.sendMessage(message);
            }
            // }

        }
    }

    /**
     * Enque one messages for sending. code execution does not wait util message
     * was sent successfully
     * 
     * @see PlainSocketConnectionHandler#sendMessagesAsynchron(Message[])
     * @param message
     *            the message to send
     * @param errorMessage
     *            the error message to be logged on connection problem
     */
    public void sendMessageAsynchron(Message message, String errorMessage) {
        // synchronized (peerInitalizeLock) {
        if (peer != null && peer.isConnected()) {
            peer.sendMessagesAsynchron(message);
        }
        // }
    }

    /**
     * Enque multiple messages for sending. code execution does not wait util
     * message was sent successfully
     * 
     * @see PlainSocketConnectionHandler#sendMessagesAsynchron(Message[])
     * @param messages
     *            the messages to send
     */
    public void sendMessagesAsynchron(Message... messages) {
        if (peer != null && peer.isConnected()) {
            peer.sendMessagesAsynchron(messages);
        }
    }

    /**
     * Handles an incomming message from the remote peer (ConnectionHandler)
     * 
     * @param message
     *            The message to handle
     */
    public void handleMessage(Message message) {
        if (message == null) {
            throw new NullPointerException(
                "Unable to handle message, message is null");
        }

        // related folder is filled if message is a folder related message
        FolderInfo targetedFolderInfo = null;
        Folder targetFolder = null;
        if (message instanceof FolderRelatedMessage) {
            targetedFolderInfo = ((FolderRelatedMessage) message).folder;
            targetFolder = getController().getFolderRepository().getFolder(
                targetedFolderInfo);
        }

        // do all the message processing
        // Processing of message also should take only
        // a short time, because member is not able
        // to received any other message meanwhile !

        // Identity is not handled HERE !
        if (message instanceof Ping) {
            // TRAC #812: Answer the ping here. PONG is handled in
            // ConnectionHandler!
            Pong pong = new Pong((Ping) message);
            sendMessagesAsynchron(pong);
        } else if (message instanceof HandshakeCompleted) {
            lastHandshakeCompleted = (HandshakeCompleted) message;
            // Notify waiting ppl
            synchronized (handshakeCompletedWaiter) {
                handshakeCompletedWaiter.notifyAll();
            }

        } else if (message instanceof FolderList) {
            FolderList fList = (FolderList) message;
            lastFolderList = fList;
            joinToLocalFolders(fList);

            // Notify waiting ppl
            synchronized (folderListWaiter) {
                folderListWaiter.notifyAll();
            }
        } else if (message instanceof RequestFileList) {
            if (targetFolder != null && !targetFolder.isSecret()) {
                // a file list of a folder
                if (logVerbose) {
                    log().verbose(
                        targetFolder + ": Sending new filelist to " + this);
                }
                sendMessagesAsynchron(FileList
                    .createFileListMessages(targetFolder));
            } else {
                // Send folder not found if not found or folder is secret
                sendMessageAsynchron(new Problem("Folder not found: "
                    + targetedFolderInfo, false), null);
            }

        } else if (message instanceof ScanCommand) {
            if (targetFolder != null
                && targetFolder.getSyncProfile().isAutoDetectLocalChanges())
            {
                log()
                    .verbose("Remote sync command received on " + targetFolder);
                getController().setSilentMode(false);
                // Now trigger the scan
                targetFolder.recommendScanOnNextMaintenance();
                getController().getFolderRepository().triggerMaintenance();
            }
        } else if (message instanceof RequestDownload) {
            // a download is requested
            RequestDownload dlReq = (RequestDownload) message;
            Upload ul = getController().getTransferManager().queueUpload(this,
                dlReq);
            if (ul == null) {
                // Send abort
                log().warn("Sending abort of " + dlReq.file);
                sendMessagesAsynchron(new AbortUpload(dlReq.file));
            }

        } else if (message instanceof DownloadQueued) {
            // set queued flag here, if we received status from other side
            DownloadQueued dlQueued = (DownloadQueued) message;
            getController().getTransferManager().setQueued(dlQueued);

        } else if (message instanceof AbortDownload) {
            AbortDownload abort = (AbortDownload) message;
            // Abort the upload
            getController().getTransferManager().abortUpload(abort.file, this);

        } else if (message instanceof AbortUpload) {
            AbortUpload abort = (AbortUpload) message;
            // Abort the upload
            getController().getTransferManager()
                .abortDownload(abort.file, this);

        } else if (message instanceof FileChunk) {
            // File chunk received
            FileChunk chunk = (FileChunk) message;
            getController().getTransferManager().chunkReceived(chunk, this);

        } else if (message instanceof RequestNodeList) {
            // Nodemanager will handle that
            RequestNodeList request = (RequestNodeList) message;
            getController().getNodeManager().receivedRequestNodeList(request,
                this);

        } else if (message instanceof KnownNodes) {
            KnownNodes newNodes = (KnownNodes) message;
            // TODO Move this code into NodeManager.receivedKnownNodes(....)
            // TODO This code should be done in NodeManager
            // This might also just be a search result and thus not include us
            for (int i = 0; i < newNodes.nodes.length; i++) {
                MemberInfo remoteNodeInfo = newNodes.nodes[i];
                if (remoteNodeInfo == null) {
                    continue;
                }

                if (getInfo().equals(remoteNodeInfo)) {
                    // Take his info
                    updateInfo(remoteNodeInfo);
                }
            }

            // Queue arrived node list at nodemanager
            getController().getNodeManager().queueNewNodes(newNodes.nodes);
        } else if (message instanceof RequestNodeInformation) {
            // send him our node information
            sendMessageAsynchron(new NodeInformation(getController()), null);

        } else if (message instanceof TransferStatus) {
            // Hold transfer status
            lastTransferStatus = (TransferStatus) message;

        } else if (message instanceof NodeInformation) {
            if (logVerbose) {
                log().verbose("Node information received");
            }
            if (Logger.isLogToFileEnabled()) {
                Debug.writeNodeInformation((NodeInformation) message);
            }
            // Cache the last node information
            // lastNodeInformation = (NodeInformation) message;

        } else if (message instanceof SettingsChange) {
            SettingsChange settingsChange = (SettingsChange) message;
            if (settingsChange.newInfo != null) {
                log().debug(
                    this.getInfo().nick + " changed nick to "
                        + settingsChange.newInfo.nick);
                setNick(settingsChange.newInfo.nick);
            }

        } else if (message instanceof FileList) {
            FileList remoteFileList = (FileList) message;
            Convert.cleanFileList(getController(), remoteFileList.files);
            if (logDebug) {
                log().debug(
                    remoteFileList.folder + ": Received new filelist ("
                        + remoteFileList.folder.filesCount
                        + "total file(s)) from " + this);
            }
            if (expectedListMessages == null) {
                // Lazy init
                expectedListMessages = new ConcurrentHashMap<FolderInfo, Integer>();
            }
            // Reset counter of expected filelists
            expectedListMessages.put(remoteFileList.folder, Integer
                .valueOf(remoteFileList.nFollowingDeltas));

            // Add filelist to filelist cache
            Map<FileInfo, FileInfo> cachedFileList = new ConcurrentHashMap<FileInfo, FileInfo>(
                remoteFileList.files.length, 0.75f, 1);

            for (int i = 0; i < remoteFileList.files.length; i++) {
                cachedFileList.put(remoteFileList.files[i],
                    remoteFileList.files[i]);
            }
            if (lastFiles == null) {
                // Initalize lazily
                lastFiles = new ConcurrentHashMap<FolderInfo, Map<FileInfo, FileInfo>>(
                    16, 0.75f, 1);
            }
            lastFiles.put(remoteFileList.folder, cachedFileList);

            // Trigger requesting
            // FIXME: Really inform folder on first list messag on complete file
            // list?.
            if (targetFolder != null) {
                // Write filelist
                if (Logger.isLogToFileEnabled()) {
                    // Write filelist to disk
                    File debugFile = new File(Logger.getDebugDir(),
                        targetFolder.getName() + "/" + getNick() + ".list.txt");
                    Debug.writeFileListCSV(cachedFileList.keySet(),
                        "FileList of folder " + targetFolder.getName()
                            + ", member " + this + ":", debugFile);
                }
                // Inform folder
                targetFolder.fileListChanged(this, remoteFileList);
            }
        } else if (message instanceof FolderFilesChanged) {
            if (logDebug) {
                log().debug("FileListChange received: " + message);
            }
            FolderFilesChanged changes = (FolderFilesChanged) message;
            Convert.cleanFileList(getController(), changes.added);
            Convert.cleanFileList(getController(), changes.removed);

            Integer nExpected = expectedListMessages.get(changes.folder);
            // Correct filelist
            Map<FileInfo, FileInfo> cachedFileList = getLastFileList0(changes.folder);
            if (cachedFileList == null || nExpected == null) {
                log().warn(
                    "Received folder changes on " + changes.folder.name
                        + ", but not received the full filelist");
                return;
            }
            nExpected = Integer.valueOf(nExpected.intValue() - 1);
            expectedListMessages.put(changes.folder, nExpected);
            TransferManager tm = getController().getTransferManager();
            synchronized (cachedFileList) {
                if (changes.added != null) {
                    for (int i = 0; i < changes.added.length; i++) {
                        FileInfo file = changes.added[i];
                        cachedFileList.remove(file);
                        cachedFileList.put(file, file);
                    }
                }
                if (changes.removed != null) {
                    for (int i = 0; i < changes.removed.length; i++) {
                        FileInfo file = changes.removed[i];
                        cachedFileList.remove(file);
                        cachedFileList.put(file, file);
                        // file removed so if downloading break the download
                        if (logVerbose) {
                            log().verbose(
                                "downloading removed file breaking it! " + file
                                    + " " + this);
                        }
                        tm.abortDownload(file, this);
                    }
                }
            }

            if (targetFolder != null && nExpected.intValue() <= 0) {
                // Write filelist
                if (Logger.isLogToFileEnabled()) {
                    // Write filelist to disk
                    File debugFile = new File(Logger.getDebugDir(),
                        targetFolder.getName() + "/" + getNick() + ".list.txt");
                    Debug.writeFileListCSV(cachedFileList.keySet(),
                        "FileList of folder " + targetFolder.getName()
                            + ", member " + this + ":", debugFile);
                }
                // Inform folder
                targetFolder.fileListChanged(this, changes);
            }

            if (logDebug) {
                log().debug(
                    "Received folder change on '" + targetedFolderInfo.name
                        + ". Expecting "
                        + expectedListMessages.get(targetedFolderInfo)
                        + " more deltas");
            }
        } else if (message instanceof Invitation) {
            // Invitation to folder
            Invitation invitation = (Invitation) message;
            // To ensure invitor is correct
            invitation.invitor = this.getInfo();

            getController().getFolderRepository().invitationReceived(
                invitation, true, false);

        } else if (message instanceof Problem) {
            lastProblem = (Problem) message;

            if (lastProblem.problemCode == Problem.DO_NOT_LONGER_CONNECT) {
                // Finds us boring
                // set unable to connect
                log().debug(
                    "Problem received: Node reject our connection, "
                        + "we should not longer try to connect");
                // Not connected to public network
                isConnectedToNetwork = true;
            } else if (lastProblem.problemCode == Problem.DUPLICATE_CONNECTION)
            {
                log()
                    .warn(
                        "Problem received: Node thinks we have a dupe connection to him");
            } else {
                log().warn("Problem received: " + lastProblem);
            }

            if (lastProblem.fatal) {
                // Shutdown
                shutdown();
            }
        } else if (message instanceof SearchNodeRequest) {
            // Send nodelist that matches the search.
            final SearchNodeRequest request = (SearchNodeRequest) message;
            Runnable searcher = new Runnable() {
                public void run() {
                    List<MemberInfo> reply = new LinkedList<MemberInfo>();
                    for (Member m : getController().getNodeManager()
                        .getNodesAsCollection())
                    {
                        if (m.getInfo().isInvalid(getController())) {
                            continue;
                        }
                        if (m.matches(request.searchString)) {
                            reply.add(m.getInfo());
                        }
                    }

                    if (!reply.isEmpty()) {
                        sendMessageAsynchron(new KnownNodes(reply
                            .toArray(new MemberInfo[0])), null);
                    }
                }
            };
            getController().getThreadPool().execute(searcher);
        } else if (message instanceof Notification) {
            Notification not = (Notification) message;
            if (not.getEvent() == null) {
                log().warn("Unknown event from peer");
            } else {
                switch (not.getEvent()) {
                    case ADDED_TO_FRIENDS :
                        getController().getNodeManager().askForFriendship(this,
                            not.getPersonalMessage());
                        break;
                    default :
                        log().warn("Unhandled event: " + not.getEvent());
                }
            }
        } else if (message instanceof RequestPart) {
            RequestPart pr = (RequestPart) message;
            Upload up = getController().getTransferManager().getUpload(this,
                pr.getFile());
            if (up != null) { // If the upload isn't broken
                up.enqueuePartRequest(pr);
            }
        } else if (message instanceof StartUpload) {
            StartUpload su = (StartUpload) message;
            Download dl = getController().getTransferManager().getDownload(
                this, su.getFile());
            if (dl != null) {
                dl.uploadStarted();
            } else {
                log().warn("Download not found: " + su.getFile());
            }
        } else if (message instanceof StopUpload) {
            StopUpload su = (StopUpload) message;
            Upload up = getController().getTransferManager().getUpload(this,
                su.getFile());
            if (up != null) { // If the upload isn't broken
                up.stopUploadRequest(su);
            }
        } else if (message instanceof RequestFilePartsRecord) {
            RequestFilePartsRecord req = (RequestFilePartsRecord) message;
            Upload up = getController().getTransferManager().getUpload(this,
                req.getFile());
            if (up != null) { // If the upload isn't broken
                up.receivedFilePartsRecordRequest(req);
            }
        } else if (message instanceof ReplyFilePartsRecord) {
            ReplyFilePartsRecord rep = (ReplyFilePartsRecord) message;
            Download dl = getController().getTransferManager().getDownload(
                this, rep.getFile());
            if (dl != null) {
                dl.receivedFilePartsRecord(rep.getRecord());
            } else {
                log().warn("Download not found: " + dl);
            }
        } else if (message instanceof RelayedMessage) {
            RelayedMessage relMsg = (RelayedMessage) message;
            getController().getIOProvider().getRelayedConnectionManager()
                .handleRelayedMessage(this, relMsg);
        } else if (message instanceof UDTMessage) {
        	getController().getIOProvider().getUDTSocketConnectionManager()
        		.handleUDTMessage(this, (UDTMessage) message);
        } else {
            log().verbose(
                "Message not known to message handling code, "
                    + "maybe handled in listener: " + message);
        }

        // Give message to nodemanager
        getController().getNodeManager().messageReceived(this, message);
        // now give the message to all message listeners
        fireMessageToListeners(message);
    }

    /**
     * Adds a message listener
     * 
     * @param aListener
     *            The listener to add
     */
    public void addMessageListener(MessageListener aListener) {
        getMessageListenerSupport().addMessageListener(aListener);
    }

    /**
     * Adds a message listener, which is only triggerd if a message of type
     * <code>messageType</code> is received.
     * 
     * @param messageType
     *            The type of messages to register too.
     * @param aListener
     *            The listener to add
     */
    public void addMessageListener(Class<?> messageType, MessageListener aListener)
    {
        getMessageListenerSupport().addMessageListener(messageType, aListener);
    }

    /**
     * Removes a message listener completely from this member
     * 
     * @param aListener
     *            The listener to remove
     */
    public void removeMessageListener(MessageListener aListener) {
        getMessageListenerSupport().removeMessageListener(aListener);
    }

    /**
     * Overriden, removes message listeners also
     * 
     * @see de.dal33t.powerfolder.PFComponent#removeAllListeners()
     */
    public void removeAllListeners() {
        log().verbose("Removing all listeners from member. " + this);
        super.removeAllListeners();
        // Remove message listeners
        getMessageListenerSupport().removeAllListeners();
    }

    /**
     * Fires a message to all message listeners
     * 
     * @param message
     *            the message to fire
     */
    private void fireMessageToListeners(Message message) {
        getMessageListenerSupport().fireMessage(this, message);
    }

    private synchronized MessageListenerSupport getMessageListenerSupport() {
        if (messageListenerSupport == null) {
            messageListenerSupport = new MessageListenerSupport(this);
        }
        return messageListenerSupport;
    }

    /*
     * Remote group joins
     */

    /**
     * Synchronizes the folder memberships on both sides
     * 
     * @param joinedFolders
     *            the currently joined folders of ourself
     * @throws ConnectionException
     */
    public void synchronizeFolderMemberships(FolderInfo[] joinedFolders) {
        Reject.ifNull(joinedFolders, "Joined folders is null");
        if (isMySelf()) {
            return;
        }
        if (!isCompleteyConnected()) {
            return;
        }
        FolderList folderList = getLastFolderList();
        if (folderList == null) {
            log()
                .error(
                    "Unable to synchronize memberships, did not received folderlist from remote");
            return;
        }
        // Rejoin to local folders
        joinToLocalFolders(folderList);
        FolderList myFolderList = new FolderList(joinedFolders, peer
            .getRemoteMagicId());
        sendMessageAsynchron(myFolderList, null);
    }

    /**
     * Joins member to all local folders which are also available on remote
     * peer, removes member from all local folders, if not longer member of
     * 
     * @throws ConnectionException
     */
    private void joinToLocalFolders(FolderList folderList) {
        folderJoinLock.lock();
        try {
            FolderInfo[] remoteFolders = folderList.folders;
            log().verbose(
                "Joining into folders, he has " + remoteFolders.length
                    + " folders");
            FolderRepository repo = getController().getFolderRepository();

            HashSet<FolderInfo> joinedFolder = new HashSet<FolderInfo>();

            // join all, which exists here and there
            for (int i = 0; i < remoteFolders.length; i++) {
                Folder folder = repo.getFolder(remoteFolders[i]);
                if (folder == null) {
                    continue;
                }
                // we have the folder here, join member to now
                joinedFolder.add(folder.getInfo());
                // Always rejoin folder, folder information should be sent to
                // him
                folder.join(this);
            }

            Collection<Folder> localFolders = repo.getFoldersAsCollection();

            String myMagicId = peer != null ? peer.getMyMagicId() : null;
            if (peer == null) {
                log()
                    .verbose(
                        "Unable to join to local folders. peer is null/disconnected");
                return;
            }
            if (StringUtils.isBlank(myMagicId)) {
                log().error(
                    "Unable to join to local folders. Own magic id of peer is blank: "
                        + peer);
                return;
            }
            // Process secrect folders now
            if (folderList.secretFolders != null
                && folderList.secretFolders.length > 0)
            {
                // Step 1: Calculate secure folder ids for local secret folders
                Map<FolderInfo, Folder> localSecretFolders = new HashMap<FolderInfo, Folder>();

                for (Folder folder : localFolders) {
                    if (!folder.isSecret()) {
                        continue;
                    }
                    FolderInfo secretFolderCanidate = (FolderInfo) folder
                        .getInfo().clone();
                    // Calculate id with my magic id
                    secretFolderCanidate.id = secretFolderCanidate
                        .calculateSecureId(myMagicId);
                    // Add to local secret folder list
                    localSecretFolders.put(secretFolderCanidate, folder);
                }

                // Step 2: Check if remote side has joined one of our secret
                // folders
                for (int i = 0; i < folderList.secretFolders.length; i++) {
                    FolderInfo secretFolder = folderList.secretFolders[i];
                    if (localSecretFolders.containsKey(secretFolder)) {
                        log()
                            .verbose("Also has secret folder: " + secretFolder);
                        Folder folder = localSecretFolders.get(secretFolder);
                        // Okay, join him into folder
                        joinedFolder.add(folder.getInfo());
                        // Join him into our folder
                        folder.join(this);
                    }
                }
            }

            // ok now remove member from not longer joined folders
            for (Folder folder : localFolders) {
                if (folder != null && !joinedFolder.contains(folder.getInfo()))
                {
                    // remove this member from folder, if not on new folder
                    folder.remove(this);
                }
            }

            if (joinedFolder.size() > 0) {
                log()
                    .info(
                        getNick() + " joined " + joinedFolder.size()
                            + " folder(s)");
                if (!isFriend()) {
                    // Ask for friendship of guy
                    getController().getNodeManager().askForFriendship(this,
                        joinedFolder, null);
                }
            }
        } finally {
            folderJoinLock.unlock();
        }

        //
        // if (joinedFolder.size() > 0 || newUnjoinedFolders > 0) {
        // fireEvent(new MemberStateChanged());
        // }
    }

    /*
     * Request to remote peer *************************************************
     */

    /**
     * Answers the latest received folder list
     * 
     * @return the latest received folder list
     */
    public FolderList getLastFolderList() {
        return lastFolderList;
    }

    /**
     * Answers if user has a filelist for the folder
     * 
     * @param foInfo
     *            the FolderInfo to check if there is a file list filelist for.
     * @return true if user has a filelist for the folder
     */
    public boolean hasFileListFor(FolderInfo foInfo) {
        return getLastFileList0(foInfo) != null;
    }

    /**
     * Answers if we received the complete filelist (+all nessesary deltas) on
     * that folder.
     * 
     * @param foInfo
     * @return true if we received the complete filelist (+all nessesary deltas)
     *         on that folder.
     */
    public boolean hasCompleteFileListFor(FolderInfo foInfo) {
        Map<FileInfo, FileInfo> files = getLastFileList0(foInfo);
        if (files == null) {
            return false;
        }
        if (expectedListMessages == null) {
            return false;
        }
        Integer nUpcomingMsgs = expectedListMessages.get(foInfo);
        if (nUpcomingMsgs == null) {
            return false;
        }
        // nUpcomingMsgs might have negativ values! means we received deltas
        // after the inital filelist.
        return nUpcomingMsgs.intValue() <= 0;
    }

    /**
     * Answers the last filelist of a member/folder May return null.
     * 
     * @param foInfo
     *            The folder to get the listlist for
     * @return A Map<FileInfo, FileInfo> for this folder (foInfo)
     */
    private Map<FileInfo, FileInfo> getLastFileList0(FolderInfo foInfo) {
        FolderList list = getLastFolderList();
        // FIXME: Check if node still on folder
        if (list == null) {
            // Node not on folder or did not send a folder list yet
            // Thus we do not return any filelist
            return null;
        }
        if (lastFiles == null) {
            return null;
        }
        return lastFiles.get(foInfo);
    }

    /**
     * Answers the last filelist of a member/folder. Returns null if no filelist
     * has been received yet. But may return empty collection
     * 
     * @param foInfo
     *            The folder to get the listlist for
     * @return A Array containing the FileInfo s
     * @deprecated Use {@link #getLastFileListAsCollection(FolderInfo)}
     */
    public FileInfo[] getLastFileList(FolderInfo foInfo) {
        Map<FileInfo, FileInfo> list = getLastFileList0(foInfo);
        if (list == null) {
            return null;
        }
        synchronized (list) {
            FileInfo[] tempList = new FileInfo[list.size()];
            list.keySet().toArray(tempList);
            return tempList;
        }
    }

    /**
     * Answers the last filelist of a member/folder. Returns null if no filelist
     * has been received yet. But may return empty collection.
     * <p>
     * Avoids temporary list creation by returning an (unmodifiable) reference
     * to the keyset of the cached filelist.
     * 
     * @param foInfo
     *            The folder to get the listlist for
     * @return an collection unmodifieable containing the fileinfos.
     */
    public Collection<FileInfo> getLastFileListAsCollection(FolderInfo foInfo) {
        Map<FileInfo, FileInfo> list = getLastFileList0(foInfo);
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableCollection(list.keySet());
    }

    /**
     * Returns the last transfer status of this node
     * 
     * @return the last transfer status of this node
     */
    public TransferStatus getLastTransferStatus() {
        if (isMySelf()) {
            return getController().getTransferManager().getStatus();
        }
        return lastTransferStatus;
    }

    /**
     * Answers if user joined any folder.
     * <p>
     * TODO: Add if the user is on any folder based on network folder list.
     * 
     * @return true if user joined any folder
     */
    public boolean hasJoinedAnyFolder() {
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (folder.hasMember(this)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the list of joined folders.
     */
    public List<Folder> getJoinedFolders() {
        List<Folder> joinedFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (folder.hasMember(this)) {
                joinedFolders.add(folder);
            }
        }
        return joinedFolders;
    }

    /**
     * Answers if member has the file available to download. Does NOT check
     * version match
     * 
     * @param file
     *            the FileInfo to find at this user
     * @return true if this user has this file, or false if not or if no
     *         filelist received (yet)
     */
    public boolean hasFile(FileInfo file) {
        FileInfo remoteFile = getFile(file);
        return remoteFile != null && !remoteFile.isDeleted();
    }

    /**
     * Returns the remote file info from the node. May return null if file is
     * not known by remote or no filelist was received yet. Does return the
     * internal database file if myself.
     * 
     * @param file
     *            local file
     * @return the fileInfo of remote side, or null
     */
    public FileInfo getFile(FileInfo file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        if (isMySelf()) {
            Folder folder = file.getFolder(getController()
                .getFolderRepository());
            if (folder == null) {
                // Folder not joined, so we don't have the file.
                return null;
            }
            return folder.getFile(file);
        }
        Map<FileInfo, FileInfo> list = getLastFileList0(file.getFolderInfo());
        if (list == null) {
            return null;
        }
        return list.get(file);
    }

    /*
     * Simple getters *********************************************************
     */

    /**
     * @return The ID of this member
     */
    public String getId() {
        return info.id;
    }

    /**
     * @return nick name of the member
     */
    public String getNick() {
        return info.nick;
    }

    /**
     * set the nick name of this member
     * 
     * @param nick
     *            The nick to set
     */
    public void setNick(String nick) {
        info.nick = nick;
        // Fire event on nodemanager
        getController().getNodeManager().fireNodeSettingsChanged(this);
    }

    /**
     * Returns the identity of this member.
     * 
     * @return the identity if connection is established, otherwise null
     */
    public Identity getIdentity() {
        if (peer != null) {
            return peer.getIdentity();
        }
        return null;
    }

    /**
     * @return the ip + portnumber in InetSocketAddress to connect to.
     */
    public InetSocketAddress getReconnectAddress() {
        return info.getConnectAddress();
    }

    /**
     * Answers when the member connected last time or null, if member never
     * connected
     * 
     * @return Date Object representing the last connect time or null, if member
     *         never connected
     */
    public Date getLastConnectTime() {
        return info.lastConnectTime;
    }

    /**
     * Answers the last connect time of the user to the network. Last connect
     * time is determinded by the information about users from other nodes and
     * own last connection date to that node
     * 
     * @return Date object representing the last time on the network
     */
    public Date getLastNetworkConnectTime() {
        if (info.lastConnectTime == null) {
            return lastNetworkConnectTime;
        } else if (lastNetworkConnectTime == null) {
            return info.lastConnectTime;
        }
        if (info.lastConnectTime.after(lastNetworkConnectTime)) {
            return info.lastConnectTime;
        }
        return lastNetworkConnectTime;
    }

    /**
     * Returns the member information. add connected info
     * 
     * @return the MemberInfo object
     */
    public MemberInfo getInfo() {
        info.isConnected = isConnected();
        return info;
    }

    /**
     * @return
     */
    /*
     * private long getAverageResponseTime() { return averageResponseTime; }
     */

    /**
     * Answers if this member is connected to the PF network
     * 
     * @return true if this member is connected to the PF network
     */
    public boolean isConnectedToNetwork() {
        return isCompleteyConnected() || isConnectedToNetwork;
    }

    /**
     * set the connected to network status
     * 
     * @param connected
     *            flag indicating if this member is connected
     */
    public void setConnectedToNetwork(boolean connected) {
        isConnectedToNetwork = connected;
    }

    /**
     * Answers if we the remote node told us not longer to connect.
     * 
     * @return true if the remote side didn't want to be connected.
     */
    public boolean isDontConnect() {
        return lastProblem != null
            && lastProblem.problemCode == Problem.DO_NOT_LONGER_CONNECT;
    }

    /**
     * @return true if no direct connection to this member is possible. (At
     *         least 2 tries)
     */
    public boolean isUnableToConnect() {
        return connectionRetries >= 3;
    }

    /**
     * @return the last problem received from this node.
     */
    public Problem getLastProblem() {
        return lastProblem;
    }

    /**
     * Updates connection information, if the other is more 'valueble'.
     * <p>
     * TODO CLEAN UP THIS MESS!!!! -> Define behaviour and write tests.
     * 
     * @param newInfo
     *            The new MemberInfo to use if more valueble
     * @return true if we found valueble information
     */
    public boolean updateInfo(MemberInfo newInfo) {
        boolean updated = false;
        if (!isConnected() && newInfo.isConnected) {
            // take info, if this is now a supernode
            if (newInfo.isSupernode && !info.isSupernode) {
                if (logWarn) {
                    log()
                        .warn("Received new supernode information: " + newInfo);
                }
            }
            info.isSupernode = newInfo.isSupernode;
            // if (!isOnLAN()) {
            // Take his dns address, but only if not on lan
            // (Otherwise our ip to him will be used as reconnect address)
            // Commentend until 100% LAN/inet detection accurate
            info.setConnectAddress(newInfo.getConnectAddress());
            // }
            updated = true;
        }

        // Take his last connect time if newer
        boolean updateLastNetworkConnectTime = (lastNetworkConnectTime == null && newInfo.lastConnectTime != null)
            || (newInfo.lastConnectTime != null && lastNetworkConnectTime
                .before(newInfo.lastConnectTime));

        if (!isConnected() && updateLastNetworkConnectTime) {
            // log().verbose(
            // "Last connect time fresher on remote side. this "
            // + lastNetworkConnectTime + ", remote: "
            // + newInfo.lastConnectTime);
            lastNetworkConnectTime = newInfo.lastConnectTime;
            updated = true;
        }

        if (updated) {
            // Re try connection
            connectionRetries = 0;
        }
        return updated;
    }

    public boolean askedForFriendship() {
        return askedForFriendship;
    }

    public void setAskedForFriendship(boolean flag) {
        askedForFriendship = flag;
    }

    // Logger methods *********************************************************
    public String getLoggerName() {
        return "Node '" + getNick() + "'" + (isSupernode() ? " (s)" : "");
    }

    /*
     * General ****************************************************************
     */

    public String toString() {
        String connect;

        if (isConnected()) {
            connect = peer + "";
        } else {
            connect = isMySelf() ? "myself" : "-disco.-, " + "recon. at "
                + getReconnectAddress();
        }

        return "Member '" + info.nick + "' (" + connect + ")";
    }

    /**
     * true if the ID's of the memberInfo objects are equal
     * 
     * @param other
     * @return true if the ID's of the memberInfo objects are equal
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Member) {
            Member oM = (Member) other;
            return Util.equals(this.info.id, oM.info.id);
        }

        return false;
    }

    public int hashCode() {
        return (info.id == null) ? 0 : info.id.hashCode();
    }
}