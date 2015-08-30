/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Member.java 21238 2013-03-18 20:16:43Z sprajc $
 */
package de.dal33t.powerfolder;

import java.io.Externalizable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.problem.FolderReadOnlyProblem;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.AbortDownload;
import de.dal33t.powerfolder.message.AbortUpload;
import de.dal33t.powerfolder.message.AddFriendNotification;
import de.dal33t.powerfolder.message.ConfigurationLoadRequest;
import de.dal33t.powerfolder.message.DownloadQueued;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.FileHistoryReply;
import de.dal33t.powerfolder.message.FileHistoryRequest;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FileListRequest;
import de.dal33t.powerfolder.message.FileRequestCommand;
import de.dal33t.powerfolder.message.FolderDBMaintCommando;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.FolderListExt;
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
import de.dal33t.powerfolder.message.QuotaExceeded;
import de.dal33t.powerfolder.message.RelayedMessage;
import de.dal33t.powerfolder.message.ReplyFilePartsRecord;
import de.dal33t.powerfolder.message.RequestDownload;
import de.dal33t.powerfolder.message.RequestFilePartsRecord;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.message.RevertedFile;
import de.dal33t.powerfolder.message.ScanCommand;
import de.dal33t.powerfolder.message.SearchNodeRequest;
import de.dal33t.powerfolder.message.SettingsChange;
import de.dal33t.powerfolder.message.StartUpload;
import de.dal33t.powerfolder.message.StopUpload;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.message.UDTMessage;
import de.dal33t.powerfolder.message.clientserver.AccountStateChanged;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.InvalidIdentityException;
import de.dal33t.powerfolder.net.PlainSocketConnectionHandler;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.util.ConfigurationLoader;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Filter;
import de.dal33t.powerfolder.util.MessageListenerSupport;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * A full quailfied member, can have a connection to interact with remote
 * member/friend/peer.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.115 $
 */
public class Member extends PFComponent implements Comparable<Member> {

    /** Listener support for incoming messages */
    private MessageListenerSupport messageListenerSupport;

    /** The current connection handler */
    private volatile ConnectionHandler peer;

    /**
     * If this node has completely handshaked. TODO: Move this into
     * connectionHandler ?
     */
    private volatile boolean handshaked;

    /** The number of connection retries to the most recent known remote address */
    private volatile int connectionRetries;

    /** The total number of reconnection tries at this moment */
    private final AtomicInteger currentConnectTries = new AtomicInteger(0);

    /** his member information */
    private final MemberInfo info;

    /** The last time, the node was seen on the network */
    private Date lastNetworkConnectTime;

    /** Lock when peer is going to be initialized */
    private final Object peerInitializeLock = new Object();

    /** Folderlist waiter */
    private final Object folderListWaiter = new Object();

    /**
     * Lock to ensure that only one thread executes the folder membership
     * synchronization.
     */
    private final ReentrantLock folderJoinLock = new ReentrantLock();

    /**
     * The last message indicating that the handshake was completed
     */
    private volatile HandshakeCompleted lastHandshakeCompleted;

    /** Folder memberships received? */
    private volatile boolean folderListReceived;
    private FolderList lastFolderList;

    /**
     * The number of expected deltas to receive to have the filelist completed
     * on that folder. Might contain negativ values! means we received deltas
     * after the inital filelist.
     */
    private final Map<FolderInfo, Integer> expectedListMessages = Util
        .createConcurrentHashMap();

    /** Last trasferstatus */
    private TransferStatus lastTransferStatus;

    /**
     * the last problem
     */
    private volatile Problem lastProblem;

    /** maybe we cannot connect, but member might be online */
    private boolean isConnectedToNetwork;

    /** Flag if we received a wrong identity from remote side */
    private boolean receivedWrongRemoteIdentity;

    /** If already asked for friendship */
    private boolean askedForFriendship;

    /** If the remote node is a server. */
    private boolean server;

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
        info = mInfo;
    }

    /**
     * @param searchString
     * @return if this member matches the search string or if it equals the IP
     *         nick contains the search String
     * @see MemberInfo#matches(String)
     */
    public boolean matches(String searchString) {
        return matches(searchString, false);
    }

    /**
     * @param searchString
     * @param matchAccount
     *            true if the Account username should be also considerd for
     *            matching.
     * @return if this member matches the search string or if it equals the IP
     *         nick contains the search String
     * @see MemberInfo#matches(String)
     */
    public boolean matches(String searchString, boolean matchAccount) {
        if (info.matches(searchString)) {
            return true;
        }
        if (!matchAccount) {
            return false;
        }
        AccountInfo aInfo = getAccountInfo();
        if (aInfo == null) {
            return false;
        }
        if (aInfo.getUsername() != null
            && aInfo.getUsername().toLowerCase().indexOf(searchString) >= 0)
        {
            return true;
        }
        return aInfo.getDisplayName() != null
            && aInfo.getDisplayName().toLowerCase().indexOf(searchString) >= 0;
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

    private Boolean mySelf;

    /**
     * Answers if this is myself
     *
     * @return true if this object references to "myself" else false
     */
    public boolean isMySelf() {
        if (mySelf != null) {
            // Use cache
            return mySelf;
        }
        mySelf = equals(getController().getMySelf());
        return mySelf;
    }

    /**
     * #1646
     *
     * @return true if this computer is one of mine computers (same login).
     */
    public boolean isMyComputer() {
        AccountInfo aInfo = getAccountInfo();
        if (aInfo == null) {
            return false;
        }
        return aInfo.equals(getController().getOSClient().getAccountInfo());
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
     * @param personalMessage
     *            the personal message to send to the remote user.
     */
    public void setFriend(boolean newFriend, String personalMessage) {
        boolean stateChanged = isFriend() ^ newFriend;
        // Inform node manager
        if (stateChanged) {
            getController().getNodeManager().friendStateChanged(this,
                newFriend, personalMessage);

            // Remove from folders.
            if (!newFriend && !isCompletelyConnected() && hasJoinedAnyFolder())
            {
                for (Folder folder : getController().getFolderRepository()
                    .getFolders(true))
                {
                    folder.remove(this);
                }
            }
        }
    }

    /**
     * Marks the node for immediate connection
     */
    public void markForImmediateConnect() {
        getController().getReconnectManager().markNodeForImmediateReconnection(
            this);
    }

    /**
     * Answers if this node is interesting for us, that is defined as friends
     * users on LAN and has joined one of our folders. Or if its a supernode of
     * we are a supernode and there are still open connections slots.
     *
     * @return true if this node is interesting for us
     */
    public boolean isInteresting() {
        // logFine("isOnLAN(): " + isOnLAN());
        // logFine("getController().isLanOnly():" +
        // getController().isLanOnly());

        boolean isRelay = getController().getIOProvider()
            .getRelayedConnectionManager().isRelay(getInfo());
        boolean isServer = getController().getOSClient().isClusterServer(this);
        boolean isRelayOrServer = isServer || isRelay;

        if (getController().getNetworkingMode() == NetworkingMode.SERVERONLYMODE
            && !isRelayOrServer)
        {
            return false;
        }

        boolean ignoreLAN2Internet = isServer
            && ConfigurationEntry.SERVER_CONNECT_FROM_LAN_TO_INTERNET
                .getValueBoolean(getController());

        if (!ignoreLAN2Internet && getController().isLanOnly() && !isOnLAN()) {
            return false;
        }

        // FIXME Does not work with temporary server nodes.
        if (isServer || isRelay) {
            // Always interesting is the server!
            // Always interesting a relay is!
            return true;
        }

        Identity id = getIdentity();
        if (id != null) {
            if (Util.compareVersions("2.0.0", id.getProgramVersion())) {
                logWarning("Rejecting connection to old program client: " + id
                    + " v" + id.getProgramVersion());
                return false;
            }
            // FIX for #1124. Might produce problems!
            if (id.isPendingMessages()) {
                return true;
            }
        }

        // logFine("isFriend(): " + isFriend());
        // logFine("hasJoinedAnyFolder(): " + hasJoinedAnyFolder());

        if (isFriend() || isOnLAN() || hasJoinedAnyFolder()) {
            return true;
        }

        // Still capable of new connections?
        boolean conSlotAvail = !getController().getNodeManager()
            .maxConnectionsReached();
        if (conSlotAvail
            && (getController().getMySelf().isSupernode() || getController()
                .getMySelf().isServer()))
        {
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
     * @return true if this node is currently reconnecting (outbound) or
     *         connecting inbound
     */
    public boolean isConnecting() {
        return currentConnectTries.get() > 0;
    }

    /**
     * Marks the node as connecting (inbound or outbound).
     * <P>
     * Make sure to unmark the connecting status
     *
     * @return the number of currently running connection tries. Should be 1
     */
    public int markConnecting() {
        int tries = currentConnectTries.incrementAndGet();
        getController().getNodeManager().connectingStateChanged(this);
        return tries;
    }

    /**
     * @return the current connection tries. 0 if not longer connecting.
     */
    public int unmarkConnecting() {
        int tries = currentConnectTries.decrementAndGet();
        getController().getNodeManager().connectingStateChanged(this);
        return tries;
    }

    /**
     * Answers if this member has a connected peer (a open socket). To check if
     * a node is completey connected & handshaked see
     * <code>isCompletelyConnected</code>
     *
     * @see #isCompletelyConnected()
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return peer != null && peer.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Answers if this node is completely connected & handshaked
     *
     * @return true if connected & handshaked
     */
    public boolean isCompletelyConnected() {
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

    /**
     * Answers if this member is on the local area network.
     *
     * @return true if this member is on LAN.
     */
    public boolean isOnLAN() {
        try {
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
            return getController().getNodeManager().isOnLANorConfiguredOnLAN(
                adr);
        } catch (RuntimeException e) {
            logWarning("Unable to check if client is on LAN: " + this + ". "
                + e);
            return false;
        }
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
        ConnectionHandler thisPeer = peer;
        if (thisPeer != null) {
            thisPeer.shutdown();
            synchronized (peerInitializeLock) {
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
     * @return the result of the connection attempt
     */
    public ConnectResult setPeer(ConnectionHandler newPeer)
        throws InvalidIdentityException
    {
        Reject.ifNull(newPeer, "Illegal call of setPeer(null)");

        if (isFiner()) {
            logFiner("Setting peer to " + newPeer);
        }

        Identity identity = newPeer.getIdentity();
        MemberInfo remoteMemberInfo = identity != null ? identity
            .getMemberInfo() : null;

        // #1373
        if (remoteMemberInfo != null
            && !remoteMemberInfo.isOnSameNetwork(getController()))
        {
            if (isFine()) {
                logFine("Closing connection to node with diffrent network ID. Our netID: "
                    + getController().getNodeManager().getNetworkId()
                    + ", remote netID: "
                    + remoteMemberInfo.networkId
                    + " on "
                    + remoteMemberInfo);
            }
            newPeer.shutdown();
            setConnectedToNetwork(false);
            lastProblem = new Problem("Network ID mismatch", true,
                Problem.NETWORK_ID_MISMATCH);
            throw new InvalidIdentityException(
                "Closing connection to node with diffrent network ID. Our netID: "
                    + getController().getNodeManager().getNetworkId()
                    + ", remote netID: " + remoteMemberInfo.networkId + " on "
                    + remoteMemberInfo, newPeer);
        }

        if (!newPeer.isConnected()) {
            logFine("Peer disconnected while initializing connection: "
                + newPeer);
            return ConnectResult
                .failure("Peer disconnected while initializing connection");
        }

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
                logFiner("Unable to send identity reject", e);
            } finally {
                newPeer.shutdown();
            }
            throw new InvalidIdentityException(this
                + " Remote peer has wrong identity. remote ID: " + identityId
                + ", expected ID: " + getId(), newPeer);
        }

        // Complete low-level handshake
        // FIXME: Problematic situation: Now we probably accept the new peer.
        // Messages received from this new peer can be delivered to the
        // Member which not get the correct peer since "peer" field is set
        // later...
        boolean accepted = newPeer.acceptIdentity(this);

        if (!accepted) {
            // Shutdown this member
            newPeer.shutdown();
            logFiner("Remote side did not accept our identity: " + newPeer);
            return ConnectResult
                .failure("Remote side did not accept our identity");
        }

        if (!identity.getMemberInfo().id.equals(info.id)) {
            logSevere("Got wrong indentity from peer. Expected: " + info
                + ". got: " + identity.getMemberInfo());
            newPeer.shutdown();
            return ConnectResult
                .failure("Got wrong indentity from peer. Expected: " + info
                    + ". got: " + identity.getMemberInfo());
        }

        info.nick = identity.getMemberInfo().nick;
        // Reset the last connect time
        info.setLastConnectNow();

        synchronized (peerInitializeLock) {
            ConnectionHandler oldPeer = peer;
            // Set the new peer
            peer = newPeer;

            // ok, we accepted, kill old peer and shutdown.
            if (oldPeer != null) {
                oldPeer.shutdown();
            }
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
     * @return the result of the connection attempt.
     * @throws InvalidIdentityException
     */
    public ConnectResult reconnect() throws InvalidIdentityException {
        return reconnect(true);
    }

    /**
     * Tries to reconnect peer
     *
     * @param markConnecting
     *            true if this member should be marked as connecting. sometimes
     *            this has already been done by calling code.
     * @return the result of the connection attempt.
     * @throws InvalidIdentityException
     */
    public ConnectResult reconnect(boolean markConnecting)
        throws InvalidIdentityException
    {
        // do not reconnect if controller is not running
        if (!getController().isStarted()) {
            return ConnectResult.failure("Controller is not started");
        }
        if (isCompletelyConnected()) {
            return ConnectResult.success();
        }
        // #1334
        // if (info.getConnectAddress() == null) {
        // return false;
        // }
        if (isFine()) {
            logFine("Reconnecting (tried " + connectionRetries + " time(s) to "
                + this + ')');
        }

        connectionRetries++;
        ConnectResult connectResult;
        ConnectionHandler handler = null;
        try {
            // #1334
            // if (info.getConnectAddress().getPort() <= 0) {
            // logWarning(this + " has illegal connect port "
            // + info.getConnectAddress().getPort());
            // return false;
            // }

            // Set reconnecting state
            if (markConnecting) {
                markConnecting();
            }

            // Re-resolve connect address
            String theHostname = getHostName(); // cached hostname
            if (isFiner()) {
                logFiner("Reconnect hostname to " + getNick() + " is: "
                    + theHostname);
            }
            if (!StringUtils.isBlank(theHostname)) {
                info.setConnectAddress(new InetSocketAddress(theHostname, info
                    .getConnectAddress().getPort()));
            }

            // Another check: do not reconnect if controller is not running
            if (!getController().isStarted()) {
                return ConnectResult.failure("Controller is not started");
            }

            // Try to establish a low-level connection.
            handler = getController().getIOProvider()
                .getConnectionHandlerFactory().tryToConnect(getInfo());
            connectResult = setPeer(handler);
        } catch (InvalidIdentityException e) {
            logFiner(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
            throw e;
        } catch (ConnectionException e) {
            logFine(e.getMessage());
            logFiner(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
            connectResult = ConnectResult.failure(e.getMessage());
        } finally {
            if (markConnecting) {
                // Was marked, unmark it.
                unmarkConnecting();
            }
        }

        if (connectResult.isSuccess()) {
            setConnectedToNetwork(true);
            connectionRetries = 0;
        } else {
            if (isUnableToConnect() && isConnectedToNetwork) {
                logWarning("Unable to connect directly to "
                    + getReconnectAddress());
                // FIXME: Find a better ways
                setConnectedToNetwork(false);
            }
        }

        return connectResult;
    }

    /**
     * Completes the handshake between nodes. Exchanges the relevant information
     *
     * @return the result of the connection attempt.
     */
    private ConnectResult completeHandshake() {
        if (!isConnected()) {
            return ConnectResult.failure("Not connected");
        }
        ConnectionHandler thisPeer = peer;
        if (thisPeer == null) {
            return ConnectResult.failure("Peer is not set");
        }

        if (getController().getOSClient().isPrimaryServer(this)) {
            getController().getOSClient().primaryServerConnected(this);
        }

        boolean wasHandshaked = handshaked;
        Identity identity = thisPeer.getIdentity();

        boolean receivedFolderList = false;
        // #2569: Server waits for client list of folders first.
        if (getController().getMySelf().isServer() && identity != null
            && !identity.isRequestFullFolderlist())
        {
            receivedFolderList = waitForFoldersJoin();
        }

        synchronized (peerInitializeLock) {
            if (!isConnected() || identity == null) {
                logFine("Disconnected while completing handshake");
                return ConnectResult
                    .failure("Disconnected while completing handshake");
            }
            // Send node informations now
            // Send joined folders to synchronize
            FolderList remoteFolderList = getLastFolderList();
            Collection<FolderInfo> folders2node = getFilteredFolderList(
                remoteFolderList, identity.isRequestFullFolderlist());
            FolderList folderList;
            if (getProtocolVersion() >= Identity.PROTOCOL_VERSION_106) {
                folderList = new FolderListExt(folders2node,
                    peer.getRemoteMagicId());
            } else {
                folderList = new FolderList(folders2node,
                    peer.getRemoteMagicId());
            }
            if (isFiner()) {
                logFiner("Sending CH " + folderList);
            }
            peer.sendMessagesAsynchron(folderList);
        }

        // My messages sent, now wait for his folder list.
        receivedFolderList = waitForFoldersJoin();
        synchronized (peerInitializeLock) {
            if (!isConnected()) {
                logFine("Disconnected while completing handshake");
                return ConnectResult
                    .failure("Disconnected while completing handshake");
            }
            if (!receivedFolderList) {
                if (isConnected()) {
                    logFine("Did not receive a folder list after 60s, disconnecting");
                    return ConnectResult
                        .failure("Did not receive a folder list after 60s, disconnecting (1)");
                }
                shutdown();
                return ConnectResult
                    .failure("Did not receive a folder list after 60s, disconnecting (2)");
            }
            if (!isConnected()) {
                logFine("Disconnected while waiting for folder list");
                return ConnectResult
                    .failure("Disconnected while waiting for folder list");
            }
        }

        // Create request for nodelist.
        RequestNodeList request = getController().getNodeManager()
            .createDefaultNodeListRequestMessage();

        boolean thisHandshakeCompleted = true;
        synchronized (peerInitializeLock) {
            if (!isConnected()) {
                logFine("Disconnected while completing handshake");
                return ConnectResult
                    .failure("Disconnected while completing handshake");
            }

            if (isInteresting()) {
                // Send request for nodelist.
                peer.sendMessagesAsynchron(request);

                // Send our transfer status
                peer.sendMessagesAsynchron(getController().getTransferManager()
                        .getStatus());
            } else {
                logFine("Rejected, Node not interesting");
                // Tell remote side
                try {
                    peer.sendMessage(new Problem("You are boring", true,
                            Problem.DO_NOT_LONGER_CONNECT));
                } catch (ConnectionException e) {
                    // Ignore
                }
                thisHandshakeCompleted = false;
            }
        }

        boolean acceptByConnectionHandler = peer != null
            && peer.acceptHandshake();
        // Handshaked ?
        thisHandshakeCompleted = thisHandshakeCompleted && isConnected()
            && acceptByConnectionHandler;

        if (!thisHandshakeCompleted) {
            String message = "not handshaked: connected? " + isConnected()
                + ", acceptByCH? " + acceptByConnectionHandler
                + ", interesting? " + isInteresting() + ", peer " + peer;
            if (isFiner()) {
                logFiner(message);
            }
            shutdown();
            return ConnectResult.failure(message);
        }

        List<Folder> foldersJoined = sendFilelists();
        if (foldersJoined == null) {
            return ConnectResult.failure("Unable to send filelists to "
                + getNick());
        }
        boolean ok = waitForFileLists(foldersJoined);
        if (!ok) {
            String reason = "Disconnecting. Did not receive the full filelists for "
                + foldersJoined.size() + " folders: " + foldersJoined;
            logWarning(reason);
            if (isFine()) {
                for (Folder folder : foldersJoined) {
                    logFine("Got filelist for " + folder.getName() + " ? "
                        + hasCompleteFileListFor(folder.getInfo()));
                }
            }
            shutdown();
            return ConnectResult.failure(reason);
        }
        if (isFiner()) {
            logFiner("Got complete filelists");
        }

        // Wait for acknowledgement from remote side
        if (identity.isAcknowledgesHandshakeCompletion()) {
            sendMessageAsynchron(new HandshakeCompleted());
            long start = System.currentTimeMillis();
            if (!waitForHandshakeCompletion()) {
                long took = System.currentTimeMillis() - start;
                String message = null;
                if (peer == null || !peer.isConnected()) {
                    if (lastProblem == null) {
                        message = "Peer disconnected while waiting for handshake acknownledge (or problem)";
                    }
                } else {
                    if (lastProblem == null) {
                        message = "Did not receive a handshake not acknownledged (or problem) by remote side after "
                            + (int) (took / 1000) + 's';
                    }
                }
                shutdown();
                if (message != null && isWarning()) {
                    logWarning(message);
                }
                return ConnectResult.failure(message);
            } else if (isFiner()) {
                logFiner("Got handshake completion!!");
            }
        } else if (peer != null && peer.isConnected()) {
            // Handshaked
            handshaked = true;
        } else {
            shutdown();
            return ConnectResult.failure("Unknown reason");
        }

        // Reset things
        connectionRetries = 0;

        if (wasHandshaked != handshaked) {
            // Inform nodemanger about it
            getController().getNodeManager().connectStateChanged(this);

            // Inform security manager to update account state.
            boolean syncFolderMemberships = Feature.P2P_REQUIRES_LOGIN_AT_SERVER
                .isEnabled();
            getController().getSecurityManager().nodeAccountStateChanged(this,
                syncFolderMemberships);
        }

        if (isInfo()) {
            logInfo(getNick() + " " + (isOnLAN() ? "(LAN)" : "(Internet)")
                + " connected ("
                + getController().getNodeManager().countConnectedNodes()
                + " total)");
        }

        // Request files
        for (Folder folder : foldersJoined) {
            // Trigger filerequesting. we may want re-request files on a
            // folder he joined.
            if (folder.getSyncProfile().isAutodownload()) {
                getController().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting(folder.getInfo());
            }
            if (folder.getSyncProfile().isSyncDeletion()) {
                folder.triggerSyncRemoteDeletedFiles(Collections
                    .singleton(this), false);
            }
        }

        if (getController().isDebugReports()) {
            // Running with debugReports enabled (which incorporates verbose
            // mode)
            // then directly request node information.
            sendMessageAsynchron(new RequestNodeInformation());
        }

        if (handshaked) {
            return ConnectResult.success();
        } else {
            return ConnectResult.failure("Not handshaked");
        }
    }

    /**
     * Sends complete filelists for all folders, this node is an actual member
     * of.
     *
     * @return the list of actually allowed to join folder for which filelists
     *         have been sent.
     */
    private List<Folder> sendFilelists() {
        List<Folder> foldersJoined = getFoldersActuallyJoined();
        List<Folder> foldersRequested = getFoldersRequestedToJoin();
        if (isFine() && !foldersJoined.isEmpty()) {
            logFine("Joined " + foldersJoined.size() + " folders: "
                + foldersJoined);
        } else if (isFiner()) {
            logFiner("Joined " + foldersJoined.size() + " folders: "
                + foldersJoined);
        }

        for (Folder folder : foldersJoined) {
            // FIX for #924
            folder.waitForScan();
            // Send filelist of joined folders

            Message[] filelistMsgs;
            if (folder.hasOwnDatabase()) {
                filelistMsgs = FileList.create(folder,
                    folder.supportExternalizable(this));
            } else {
                filelistMsgs = new Message[1];
                filelistMsgs[0] = FileList.createEmpty(folder.getInfo(),
                    folder.supportExternalizable(this));
            }

            for (Message message : filelistMsgs) {
                try {
                    sendMessage(message);
                } catch (ConnectionException e) {
                    shutdown();
                    return null;
                }
            }
            foldersRequested.remove(folder);
        }
        if (!foldersRequested.isEmpty()) {
            if (isFine()) {
                logFine("Requested join : " + foldersRequested);
                logFine("Actually joined: " + foldersJoined);
            }
            for (Folder folder : foldersRequested) {
                sendMessagesAsynchron(FileList.createEmpty(folder.getInfo(),
                    folder.supportExternalizable(this)));
            }
        }
        return foldersJoined;
    }

    /**
     * Waits for the filelists on those folders. After a certain amount of time
     * it runs on a timeout if no filelists were received. Waits max 2 minutes.
     *
     * @param folders
     * @return true if the filelists of those folders received successfully.
     */
    private boolean waitForFileLists(List<Folder> folders) {
        if (isFiner()) {
            logFiner("Waiting for complete fileslists...");
        }
        // 120 minutes. Should never occur.
        Waiter waiter = new Waiter(1000L * 60 * 120);
        boolean fileListsCompleted = false;
        Date lastMessageReceived = null;
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

            lastMessageReceived = peer != null ? peer
                .getLastKeepaliveMessageTime() : null;
            if (lastMessageReceived == null) {
                logSevere("Unable to check last received message date. got null while waiting for filelist");
                return false;
            }
            boolean noChangeReceivedSineOneMinute = System.currentTimeMillis()
                - lastMessageReceived.getTime() > 1000L * 60;
            if (noChangeReceivedSineOneMinute) {
                logWarning("No message received since 1 minute while waiting for filelist");
                return false;
            }

            try {
                waiter.waitABit();
            } catch (Exception e) {
                return false;
            }
        }
        if (waiter.isTimeout()) {
            logSevere("Got timeout ("
                + (waiter.getTimoutTimeMS() / (1000 * 60))
                + " minutes) while waiting for filelist");
        }
        if (!isConnected()) {
            logWarning(getNick() + ": Disconnected while waiting for filelist");
        }
        return fileListsCompleted;
    }

    /**
     * Waits some time for the folder list
     *
     * @return true if list was received successfully
     */
    private boolean waitForFoldersJoin() {
        synchronized (folderListWaiter) {
            if (!folderListReceived) {
                try {
                    if (isFiner()) {
                        logFiner("Waiting for folderlist");
                    }
                    folderListWaiter.wait(60000);
                } catch (InterruptedException e) {
                    logFiner(e);
                }
            }
        }
        // Wait for joinToLocalFolders
        folderJoinLock.lock();
        folderJoinLock.unlock();
        return folderListReceived;
    }

    /**
     * Waits some time for the handshake to be completed
     *
     * @return true if list was received successfully
     */
    private boolean waitForHandshakeCompletion() {
        // 120 minutes. Should never occur.
        Waiter waiter = new Waiter(1000L * 60 * 120);

        while (!waiter.isTimeout()) {
            if (lastHandshakeCompleted != null && handshaked) {
                return true;
            }
            if (isFiner()) {
                logFiner("Waiting for handshake complete message");
            }
            Date lastMessageReceived = peer != null ? peer
                .getLastKeepaliveMessageTime() : null;
            if (lastMessageReceived == null) {
                logFine("Unable to check last received message date. Got disconnected while waiting for handshake complete");
                return false;
            }
            boolean noChangeReceivedSineOneMinute = System.currentTimeMillis()
                - lastMessageReceived.getTime() > 1000L * 60;
            if (noChangeReceivedSineOneMinute) {
                logWarning("No message received since 1 minute while waiting for handshake complete");
                return false;
            }
            if (!isConnected()) {
                return false;
            }
            waiter.waitABit();
        }
        return lastHandshakeCompleted != null && handshaked;
    }

    /**
     * Shuts the member and its connection down
     */
    public void shutdown() {
        boolean wasHandshaked = handshaked;

        shutdownPeer();

        // Notify waiting locks.
        synchronized (folderListWaiter) {
            folderListWaiter.notifyAll();
        }

        folderListReceived = false;
        lastFolderList = null;
        // Disco, assume completely
        setConnectedToNetwork(false);
        handshaked = false;
        lastHandshakeCompleted = null;
        lastTransferStatus = null;
        expectedListMessages.clear();
        messageListenerSupport = null;
        
        // Remove filelist to save memory.
        for (Folder folder : getFoldersActuallyJoined()) {
            folder.getDAO().deleteDomain(getId(), -1);
        }

        if (wasHandshaked) {
            // Reset the last connect time
            info.setLastConnectNow();

            // Inform security manager to update account state.
            getController().getSecurityManager().nodeAccountStateChanged(this,
                false);

            // Inform nodemanger about it
            getController().getNodeManager().connectStateChanged(this);

            if (isInfo()) {
                logInfo(getNick() + " disconnected ("
                    + getController().getNodeManager().countConnectedNodes()
                    + " still connected)");
            }
        } else {
            // logFiner("Shutdown");
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
     */
    public void sendMessageAsynchron(Message message) {
        if (peer != null && peer.isConnected()) {
            peer.sendMessagesAsynchron(message);
        }
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
     * @param fromPeer
     *            the peer this message has been received from.
     */
    public void handleMessage(final Message message,
        final ConnectionHandler fromPeer)
    {

        if (message == null) {
            throw new NullPointerException(
                "Unable to handle message, message is null");
        }

        // Profile this execution.
        ProfilingEntry profilingEntry = null;
        if (Profiling.ENABLED) {
            profilingEntry = Profiling.start("Member.handleMessage", message
                .getClass().getSimpleName());
        }

        int expectedTime = -1;
        long start = System.currentTimeMillis();
        try {
            if (getController().getOSClient().isPrimaryServer(this)) {
                ServerClient.SERVER_HANDLE_MESSAGE_THREAD.set(true);
            }
            // related folder is filled if message is a folder related message
            final FolderInfo targetedFolderInfo;
            final Folder targetFolder;
            if (message instanceof FolderRelatedMessage) {
                targetedFolderInfo = ((FolderRelatedMessage) message).folder;
                if (targetedFolderInfo != null) {
                    targetFolder = getController().getFolderRepository()
                        .getFolder(targetedFolderInfo);
                } else {
                    targetFolder = null;
                    logSevere("Got folder message without FolderInfo: "
                        + message);
                }
            } else {
                targetedFolderInfo = null;
                targetFolder = null;
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
                expectedTime = 50;
            } else if (message instanceof HandshakeCompleted) {
                lastHandshakeCompleted = (HandshakeCompleted) message;
                // Notify waiting ppl
                handshaked = true;
                expectedTime = 100;
            } else if (message instanceof FolderList) {
                final FolderList fList = (FolderList) message;
                // #2569
                if (isWarning()
                    && !isServer()
                    && fList.secretFolders != null
                    && fList.secretFolders.length > 100
                    && getController().getFolderRepository().getFoldersCount() < 100)
                {
                    logWarning("Received large " + fList);
                }
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        folderJoinLock.lock();
                        try {
                            lastFolderList = fList;
                            // fList.store(Member.this);
                            folderListReceived = true;
                            // Send filelist only during handshake
                            joinToLocalFolders(fList, fromPeer);

                            // #2569: Send only "filtered" client specific
                            // folder list. Send renewed list.
                            ConnectionHandler thisPeer = peer;
                            Identity identity = thisPeer != null ? thisPeer
                                .getIdentity() : null;
                            boolean fullList = identity != null
                                && identity.isRequestFullFolderlist();
                            if (getController().getMySelf().isServer()
                                && !fullList && thisPeer != null)
                            {
                                String remoteMagicId = thisPeer
                                    .getRemoteMagicId();
                                Collection<FolderInfo> folders2node = getFilteredFolderList(
                                    fList, fullList);
                                FolderList myFolderList;
                                if (getProtocolVersion() >= Identity.PROTOCOL_VERSION_106) {
                                    myFolderList = new FolderListExt(
                                        folders2node, remoteMagicId);
                                } else {
                                    myFolderList = new FolderList(folders2node,
                                        remoteMagicId);
                                }
                                if (isFine()) {
                                    logFine("Sending HM " + myFolderList);
                                }
                                sendMessageAsynchron(myFolderList);
                            }

                        } finally {
                            folderJoinLock.unlock();
                        }
                        // Notify waiting ppl
                        synchronized (folderListWaiter) {
                            folderListWaiter.notifyAll();
                        }
                    }
                };
                getController().getIOProvider().startIO(r);

                expectedTime = 300;
            } else if (message instanceof ScanCommand) {
                if (targetFolder != null) {
                    if (targetFolder.getSyncProfile().isInstantSync()
                        || targetFolder.getSyncProfile().isPeriodicSync())
                    {
                        logFiner("Remote sync command received on "
                            + targetFolder);
                        getController().setPaused(false);
                        // Now trigger the scan
                        targetFolder.recommendScanOnNextMaintenance();
                        getController().getFolderRepository()
                            .triggerMaintenance();
                    }
                    if (targetFolder.getSyncProfile().isAutodownload()) {
                        getController().getFolderRepository()
                            .getFileRequestor()
                            .triggerFileRequesting(targetedFolderInfo);
                    }
                }
                expectedTime = 50;

            } else if (message instanceof FileRequestCommand) {
                if (targetFolder != null) {
                    if (targetFolder.getSyncProfile().isAutodownload()) {
                        getController().getFolderRepository()
                            .getFileRequestor()
                            .triggerFileRequesting(targetedFolderInfo);
                    }
                }
                expectedTime = 50;

            } else if (message instanceof FolderDBMaintCommando) {
                final FolderDBMaintCommando m = (FolderDBMaintCommando) message;
                if (targetFolder != null) {
                    getController().getIOProvider().startIO(new Runnable() {
                        @Override
                        public void run() {
                            targetFolder
                                .maintainFolderDB(m.getDate().getTime());
                        }
                    });
                }
                expectedTime = 50;

            } else if (message instanceof RequestDownload) {
                final RequestDownload dlReq = (RequestDownload) message;
                // a download is requested. Put handling in background thread
                // for faster processing.
                if (getController().isPaused()) {
                    // Send abort
                    logFine("Sending abort (paused) of " + dlReq.file);
                    sendMessagesAsynchron(new AbortUpload(dlReq.file));
                } else {
                    Runnable runner = new Runnable() {
                        @Override
                        public void run() {
                            Upload ul = getController().getTransferManager()
                                .queueUpload(Member.this, dlReq);
                            if (ul == null && isCompletelyConnected()) {
                                // Send abort
                                logWarning("Sending abort of " + dlReq.file);
                                sendMessagesAsynchron(new AbortUpload(
                                    dlReq.file));
                            }
                            if (getController().isPaused()) {
                                // Send abort
                                logWarning("Sending abort (paused) of "
                                    + dlReq.file);
                                sendMessagesAsynchron(new AbortUpload(
                                    dlReq.file));
                            }
                        }
                    };
                    getController().getIOProvider().startIO(runner);
                }
                expectedTime = 100;

            } else if (message instanceof DownloadQueued) {
                // set queued flag here, if we received status from other side
                DownloadQueued dlQueued = (DownloadQueued) message;
                Download dl = getController().getTransferManager()
                    .getActiveDownload(this, dlQueued.file);
                if (dl != null) {
                    dl.setQueued(dlQueued.file);
                } else if (!downloadRecentlyCompleted(dlQueued.file)) {
                    logFine("Remote side queued non-existant download: "
                        + dlQueued.file);
                    sendMessageAsynchron(new AbortDownload(dlQueued.file));
                }
                expectedTime = 100;

            } else if (message instanceof AbortDownload) {
                AbortDownload abort = (AbortDownload) message;
                // Abort the upload
                logFine("Received " + abort + " from " + this);
                getController().getTransferManager().abortUpload(abort.file,
                    this);
                expectedTime = 100;

            } else if (message instanceof AbortUpload) {
                AbortUpload abort = (AbortUpload) message;
                // Abort the upload
                getController().getTransferManager().abortDownload(abort.file,
                    this);
                expectedTime = 100;

            } else if (message instanceof FileChunk) {
                // File chunk received
                FileChunk chunk = (FileChunk) message;
                Download d = getController().getTransferManager()
                    .getActiveDownload(this, chunk.file);
                if (d != null) {
                    d.addChunk(chunk);
                } else if (downloadRecentlyCompleted(chunk.file)) {
                    sendMessageAsynchron(new AbortDownload(chunk.file));
                }
                expectedTime = -1;

            } else if (message instanceof RequestNodeList) {
                // Nodemanager will handle that
                RequestNodeList request = (RequestNodeList) message;
                getController().getNodeManager().receivedRequestNodeList(
                    request, this);
                expectedTime = 100;

            } else if (message instanceof KnownNodes) {
                KnownNodes newNodes = (KnownNodes) message;
                // TODO Move this code into NodeManager.receivedKnownNodes(....)
                // TODO This code should be done in NodeManager
                // This might also just be a search result and thus not include
                // us
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
                expectedTime = 200;

            } else if (message instanceof RequestNodeInformation) {
                if (getController().isDebugReports()) {
                    // send him our node information, if allowed/set
                    sendMessageAsynchron(new NodeInformation(getController()));
                    expectedTime = 50;                    
                }

            } else if (message instanceof TransferStatus) {
                // Hold transfer status
                lastTransferStatus = (TransferStatus) message;
                expectedTime = 50;

            } else if (message instanceof NodeInformation) {
                if (isFiner()) {
                    logFiner("Node information received");
                }
                if (LoggingManager.isLogToFile()) {
                    Debug.writeNodeInformation((NodeInformation) message);
                }
                // Cache the last node information
                // lastNodeInformation = (NodeInformation) message;
                expectedTime = -1;

            } else if (message instanceof SettingsChange) {
                SettingsChange settingsChange = (SettingsChange) message;
                if (settingsChange.newInfo != null) {
                    logFine(getInfo().nick + " changed nick to "
                        + settingsChange.newInfo.nick);
                    setNick(settingsChange.newInfo.nick);
                }
                expectedTime = 50;
            } else if (message instanceof FileListRequest) {
                // Re-Send file list to client.

                if (targetFolder != null) {
                    Runnable filelistSender = new Runnable() {
                        @Override
                        public void run() {
                            if (targetFolder.hasReadPermission(Member.this)) {
                                // FIX for #924
                                targetFolder.waitForScan();
                                // Send filelist of joined folders
                                logInfo("Resending file list of "
                                    + targetFolder.getName() + " to "
                                    + getNick());
                                Message[] filelistMsgs = FileList.create(
                                    targetFolder, targetFolder
                                        .supportExternalizable(Member.this));
                                for (Message filelistMsg : filelistMsgs) {
                                    try {
                                        sendMessage(filelistMsg);
                                    } catch (ConnectionException e) {
                                        logWarning("Unable to send new filelist of "
                                            + targetFolder.getName()
                                            + " to "
                                            + getNick());
                                    }
                                }
                            }
                        }
                    };
                    getController().getIOProvider().startIO(filelistSender);
                }

            } else if (message instanceof FileList) {
                final FileList remoteFileList = (FileList) message;

                if (isFine()) {
                    logFine("Received new filelist. Expecting "
                        + remoteFileList.nFollowingDeltas + " more deltas. "
                        + message);
                }
                // Reset counter of expected filelists
                expectedListMessages.put(remoteFileList.folder,
                    remoteFileList.nFollowingDeltas);

                if (targetFolder != null) {
                    // Inform folder
                    targetFolder.fileListChanged(Member.this, remoteFileList);
                }
                expectedTime = 250;

            } else if (message instanceof FolderFilesChanged) {
                final FolderFilesChanged changes = (FolderFilesChanged) message;
                Integer nExpected = expectedListMessages.get(changes.folder);
                if (nExpected == null) {
                    logWarning("Disconnecting: Received folder changes, but not received the full filelist from "
                        + getNick() + ": " + changes);
                    shutdown();
                    return;
                }
                nExpected -= 1;
                expectedListMessages.put(changes.folder, nExpected);

                TransferManager tm = getController().getTransferManager();
                if (changes.getFiles() != null) {
                    for (int i = 0; i < changes.getFiles().length; i++) {
                        FileInfo file = changes.getFiles()[i];
                        // TODO Optimize: Don't break if files are same.
                        tm.abortDownload(file, this);
                    }
                }
                if (changes.getRemoved() != null) {
                    for (int i = 0; i < changes.getRemoved().length; i++) {
                        FileInfo file = changes.getRemoved()[i];
                        // TODO Optimize: Don't break if files are same.
                        tm.abortDownload(file, this);
                    }
                }

                if (isFine()) {
                    int msgs = expectedListMessages.get(targetedFolderInfo);
                    if (msgs >= 0) {
                        logFine("Received folder change. Expecting " + msgs
                            + " more deltas. " + message);
                    } else {
                        logFine("Received folder change. Received " + (-msgs)
                            + " additional deltas. " + message);
                    }
                }

                if (targetFolder != null) {
                    // Inform folder
                    targetFolder.fileListChanged(Member.this, changes);
                }
                expectedTime = 250;

            } else if (message instanceof Invitation) {

                // Invitation to folder
                Invitation invitation = (Invitation) message;

                // Server is the only one who is allowed to send invitations
                // with a different invitor
                if (!getController().getOSClient().isPrimaryServer(this)) {
                    // To ensure invitor is correct for all other computers
                    invitation.setInvitor(getInfo());
                }

                getController().invitationReceived(invitation);
                expectedTime = 100;

            } else if (message instanceof Problem) {
                lastProblem = (Problem) message;

                if (lastProblem.problemCode == Problem.DO_NOT_LONGER_CONNECT) {
                    // Finds us boring
                    // set unable to connect
                    logFine("Problem received: Node reject our connection, "
                        + "we should not longer try to connect");
                    // Not connected to public network
                    setConnectedToNetwork(true);
                } else if (lastProblem.problemCode == Problem.DUPLICATE_CONNECTION)
                {
                    logWarning("Problem received: Node thinks we have a dupe connection to him");
                } else {
                    logWarning("Problem received: " + lastProblem);
                }

                if (lastProblem.fatal) {
                    // Shutdown
                    shutdown();
                }
                expectedTime = 100;

            } else if (message instanceof SearchNodeRequest) {
                // Send nodelist that matches the search.
                final SearchNodeRequest request = (SearchNodeRequest) message;
                getController().getNodeManager().receivedSearchNodeRequest(
                    request, this);
                expectedTime = 50;

            } else if (message instanceof AddFriendNotification) {
                AddFriendNotification notification = (AddFriendNotification) message;
                getController().makeFriendship(notification.getMemberInfo());
                expectedTime = 50;
            } else if (message instanceof Notification) {
                // This is the V3 friendship notification class.
                // V4 uses AddFriendNotification.
                Notification not = (Notification) message;
                if (not.getEvent() == null) {
                    logWarning("Unknown event from peer");
                } else {
                    switch (not.getEvent()) {
                        case ADDED_TO_FRIENDS :
                            getController().makeFriendship(getInfo());
                            break;
                        default :
                            logWarning("Unhandled event: " + not.getEvent());
                    }
                }
                expectedTime = 50;
            } else if (message instanceof RevertedFile) {
                RevertedFile msg = (RevertedFile) message;
                if (targetFolder != null) {
                    Path path = msg.file.getDiskFile(getController().getFolderRepository());
                    FolderReadOnlyProblem problem = new FolderReadOnlyProblem(
                        targetFolder, path, true);
                    targetFolder.addProblem(problem);
                }

            } else if (message instanceof QuotaExceeded) {
                QuotaExceeded msg = (QuotaExceeded) message;
                if (targetFolder != null && getController().isUIEnabled()) {
                    WarningNotice notice = new WarningNotice(
                        Translation.get("warning_notice.title"),
                        Translation
                            .get("warning_notice.insufficient_quota_summary"),
                        Translation.get(
                            "warning_notice.insufficient_quota_message",
                            msg.account.getDisplayName(),
                            msg.file.getFilenameOnly()));
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().handleNotice(notice);
                }

            } else if (message instanceof RequestPart) {
                final RequestPart pr = (RequestPart) message;
                Upload up = getController().getTransferManager().getUpload(
                    Member.this, pr.getFile());
                if (up != null) { // If the upload isn't broken
                    up.enqueuePartRequest(pr);
                } else {
                    sendMessageAsynchron(new AbortUpload(pr.getFile()));
                }
                expectedTime = 100;

            } else if (message instanceof StartUpload) {
                StartUpload su = (StartUpload) message;
                Download dl = getController().getTransferManager()
                    .getActiveDownload(this, su.getFile());
                if (dl != null) {
                    dl.uploadStarted(su.getFile());
                } else if (downloadRecentlyCompleted(su.getFile())) {
                    logFine("Download invalid or obsolete:" + su.getFile());
                    sendMessageAsynchron(new AbortDownload(su.getFile()));
                }
                expectedTime = 100;

            } else if (message instanceof StopUpload) {
                StopUpload su = (StopUpload) message;
                Upload up = getController().getTransferManager().getUpload(
                    this, su.getFile());
                if (up != null) { // If the upload isn't broken
                    up.stopUploadRequest(su);
                }
                expectedTime = 100;

            } else if (message instanceof RequestFilePartsRecord) {
                RequestFilePartsRecord req = (RequestFilePartsRecord) message;
                Upload up = getController().getTransferManager().getUpload(
                    this, req.getFile());
                if (up != null) { // If the upload isn't broken
                    up.receivedFilePartsRecordRequest(req);
                } else {
                    sendMessageAsynchron(new AbortUpload(req.getFile()));
                }
                expectedTime = 100;

            } else if (message instanceof ReplyFilePartsRecord) {
                ReplyFilePartsRecord rep = (ReplyFilePartsRecord) message;
                Download dl = getController().getTransferManager()
                    .getActiveDownload(this, rep.getFile());
                if (dl != null) {
                    dl.receivedFilePartsRecord(rep.getFile(), rep.getRecord());
                } else if (downloadRecentlyCompleted(rep.getFile())) {
                    logInfo("Download not found: " + dl);
                    sendMessageAsynchron(new AbortDownload(rep.getFile()));
                }
                expectedTime = 100;

            } else if (message instanceof RelayedMessage) {
                RelayedMessage relMsg = (RelayedMessage) message;
                getController().getIOProvider().getRelayedConnectionManager()
                    .handleRelayedMessage(this, relMsg);
                expectedTime = -1;

            } else if (message instanceof UDTMessage) {
                getController().getIOProvider().getUDTSocketConnectionManager()
                    .handleUDTMessage(this, (UDTMessage) message);
                expectedTime = 50;

            } else if (message instanceof FileHistoryRequest) {
                final FileInfo requested = ((FileHistoryRequest) message)
                    .getFileInfo();
                // No need to wait for the FileDAO to have built the FileHistory
                getController().getIOProvider().startIO(new Runnable() {
                    @Override
                    public void run() {
                        Folder f = getController().getFolderRepository()
                            .getFolder(requested.getFolderInfo());
                        if (f == null) {
                            logWarning("Illegal FileHistoryRequest from "
                                + this
                                + ": This client is not member of the folder.");
                            return;
                        }
                        sendMessageAsynchron(new FileHistoryReply(f.getDAO()
                            .getFileHistory(requested), requested));
                    }
                });

            } else if (message instanceof FileHistoryReply) {
                getController().getFolderRepository().getFileRequestor()
                    .receivedFileHistory((FileHistoryReply) message);

            } else if (message instanceof AccountStateChanged) {
                AccountStateChanged asc = (AccountStateChanged) message;
                if (isFine()) {
                    logFine("Received: " + asc);
                }
                Member node = asc.getNode().getNode(getController(), false);
                if (node != null) {
                    getController().getSecurityManager()
                        .nodeAccountStateChanged(node, true);
                }
                asc.decreaseTTL();
                if (asc.isAlive()) {
                    // Continue broadcast.
                    getController().getNodeManager().broadcastMessage(asc,
                        new Filter<Member>() {
                            // Don't send the message back to the source.
                            @Override
                            public boolean accept(Member item) {
                                return !equals(item) && !item.isServer();
                            }
                        });
                }
            } else if (message instanceof ConfigurationLoadRequest) {
                if (isServer()) {
                    ConfigurationLoadRequest clr = (ConfigurationLoadRequest) message;
                    if (!getController().getMySelf().isServer()) {
                        ConfigurationLoader
                            .processMessage(getController(), clr);
                    } else if (clr.isKeyValue()) {
                        ConfigurationLoader
                            .processMessage(getController(), clr);
                    } else {
                        logWarning("Ignoring full reload config request for myself being server: "
                            + message);
                    }

                } else {
                    logWarning("Ignoring reload config request from non server: "
                        + message);
                }
            } else {
                if (isFiner()) {
                    logFiner("Message not known to message handling code, "
                        + "maybe handled in listener: " + message);
                }
            }

            // Give message to node manager
            getController().getNodeManager().messageReceived(this, message);
            // now give the message to all message listeners
            fireMessageToListeners(message);
        } finally {
            ServerClient.SERVER_HANDLE_MESSAGE_THREAD.set(false);
            Profiling.end(profilingEntry, expectedTime);
            long took = System.currentTimeMillis() - start;
            if (took > 60000) {
                logWarning("Handling took " + (took/1000) + "s: " + message);
            }
        }
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
    public void addMessageListener(Class<?> messageType,
        MessageListener aListener)
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
     * Overridden, removes message listeners.
     */
    @Override
    public void removeAllListeners() {
        if (isFiner()) {
            logFiner("Removing all listeners from member. " + this);
        }
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
     */
    public void synchronizeFolderMemberships() {
        if (isMySelf()) {
            return;
        }
        if (!isCompletelyConnected()) {
            return;
        }
        ConnectionHandler thisPeer = peer;
        String remoteMagicId = thisPeer != null
            ? thisPeer.getRemoteMagicId()
            : null;
        if (thisPeer == null || StringUtils.isBlank(remoteMagicId)) {
            return;
        }
        try {
            folderJoinLock.lock();
            FolderList folderList = getLastFolderList();
            if (folderList != null) {
                // Rejoin to local folders
                joinToLocalFolders(folderList, thisPeer);
            } else {
                // Hopefully we receive this later.
                logWarning("Unable to synchronize memberships, "
                    + "did not received folderlist from " + getNick());
            }

            // Send node informations now
            // Send joined folders to synchronize
            Identity identity = thisPeer != null
                ? thisPeer.getIdentity()
                : null;
            boolean fullList = identity != null
                && identity.isRequestFullFolderlist();
            Collection<FolderInfo> folders2node = getFilteredFolderList(
                folderList, fullList);
            FolderList myFolderList;
            if (getProtocolVersion() >= Identity.PROTOCOL_VERSION_106) {
                myFolderList = new FolderListExt(folders2node, remoteMagicId);
            } else {
                myFolderList = new FolderList(folders2node, remoteMagicId);
            }
            if (isFiner()) {
                logFiner("Sending SFM " + myFolderList);
            }
            sendMessageAsynchron(myFolderList);
        } finally {
            folderJoinLock.unlock();
        }
    }

    /**
     * #2569: Send only "filtered" client specific folder list if myself is a
     * server (server->client). For client<->client, server<->server and
     * client->server the full list of joined folders is returned.
     *
     * @param remoteFolderList
     * @return
     */
    private Collection<FolderInfo> getFilteredFolderList(
        FolderList remoteFolderList, boolean fullList)
    {
        Collection<FolderInfo> allFolders = getController()
            .getFolderRepository().getJoinedFolderInfos();
        Collection<FolderInfo> folders2node = allFolders;
        folders2node = allFolders;
        ConnectionHandler thisPeer = peer;

        // #2569: Send "filtered" folder list if no full list is requested.
        if (getController().getMySelf().isServer() && !fullList
            && remoteFolderList != null && thisPeer != null
            && StringUtils.isNotBlank(thisPeer.getMyMagicId()))
        {
            String magicId = thisPeer.getMyMagicId();
            folders2node = new LinkedList<FolderInfo>();
            for (FolderInfo folderInfo : allFolders) {
                if (remoteFolderList.contains(folderInfo, magicId)) {
                    folders2node.add(folderInfo);
                }
            }
            if (isFiner() && allFolders.size() != folders2node.size()) {
                logFiner("Generated optimized folder list: "
                    + folders2node.size() + "/" + allFolders.size());
            }
        }
        return folders2node;
    }

    /**
     * Joins member to all local folders which are also available on remote
     * peer, removes member from all local folders, if not longer member of
     *
     * @throws ConnectionException
     */
    private void joinToLocalFolders(FolderList folderList,
        ConnectionHandler fromPeer)
    {
        // logWarning("joinToLocalFolders: " + folderList);
        folderJoinLock.lock();
        try {
            FolderRepository repo = getController().getFolderRepository();
            Set<FolderInfo> joinedFolders = new HashSet<FolderInfo>();
            Collection<Folder> localFolders = repo.getFolders();

            String myMagicId = fromPeer != null
                ? fromPeer.getMyMagicId()
                : null;
            if (fromPeer == null) {
                logWarning("Unable to join to local folders. peer is null/disconnected");
                return;
            }
            if (StringUtils.isBlank(myMagicId)) {
                logSevere("Unable to join to local folders. Own magic id of peer is blank: "
                    + peer);
                return;
            }

            // Process secret folders now
            if (folderList.secretFolders != null
                && folderList.secretFolders.length > 0)
            {
                // Step 1: Calculate secure folder ids for local secret folders
                Map<String, Folder> localSecretFolders = new HashMap<String, Folder>();
                for (Folder folder : localFolders) {
                    // Calculate id with my magic id
                    String secureId = folder.getInfo().calculateSecureId(
                        myMagicId);
                    // Add to local secret folder list
                    localSecretFolders.put(secureId, folder);
                }

                // Step 2: Check if remote side has joined one of our secret
                // folders
                for (int i = 0; i < folderList.secretFolders.length; i++) {
                    FolderInfo secretFolder = folderList.secretFolders[i];
                    if (localSecretFolders.containsKey(secretFolder.id)) {
                        Folder folder = localSecretFolders.get(secretFolder.id);
                        // Join him into our folder if possible.
                        if (folder.join(this)) {
                            if (isFiner()) {
                                logFiner("Joined " + folder);
                            }
                            joinedFolders.add(folder.getInfo());
                            if (folderList.joinedMetaFolders) {
                                Folder metaFolder = repo
                                    .getMetaFolderForParent(folder.getInfo());
                                if (metaFolder != null) {
                                    if (metaFolder.join(this)) {
                                        joinedFolders.add(metaFolder.getInfo());
                                        if (isFiner()) {
                                            logFiner("Joined meta folder: "
                                                + metaFolder);

                                        }
                                    } else {
                                        logFine("Unable to join meta folder of "
                                            + folder);
                                    }
                                } else {
                                    logFine("Unable to join meta folder. Not found "
                                        + folder);
                                }
                            }
                        } else {
                            if (isFine()) {
                                logFine(this + " did not join into: " + folder);
                            }
                        }
                    }
                }
            }

            // ok now remove member from no longer joined folders
            for (Folder folder : repo.getFolders(true)) {
                if (!joinedFolders.contains(folder.getInfo())) {
                    // remove this member from folder, if not on new folder
                    folder.remove(this);
                }
            }

            if (!joinedFolders.isEmpty()) {
                if (isFine()) {
                    logFine(getNick() + " joined " + joinedFolders.size()
                        + " folder(s)");
                }
                if (!isFriend() && !server) {
                    getController().makeFriendship(getInfo());
                }
            }
        } finally {
            folderJoinLock.unlock();
        }
    }

    /*
     * Request to remote peer
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
     * Answers if we received the complete filelist (+all nessesary deltas) on
     * that folder.
     *
     * @param foInfo
     * @return true if we received the complete filelist (+all nessesary deltas)
     *         on that folder.
     */
    public boolean hasCompleteFileListFor(FolderInfo foInfo) {
        Integer nUpcomingMsgs = expectedListMessages.get(foInfo);
        if (nUpcomingMsgs == null) {
            return false;
        }
        // nUpcomingMsgs might have negativ values! means we received deltas
        // after the initial filelist.
        return nUpcomingMsgs <= 0;
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
     * TODO Performance bottleneck.
     *
     * @return true if user joined any folder
     */
    public boolean hasJoinedAnyFolder() {
        for (Folder folder : getController().getFolderRepository().getFolders())
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
    private List<Folder> getFoldersActuallyJoined() {
        List<Folder> joinedFolders = new LinkedList<Folder>();
        for (Folder folder : getController().getFolderRepository().getFolders(
            true))
        {
            if (folder.hasMember(this)) {
                joinedFolders.add(folder);
            }
        }
        return joinedFolders;
    }

    /**
     * @return the list folders in common.
     */
    private List<Folder> getFoldersRequestedToJoin() {
        ConnectionHandler thisPeer = peer;
        if (thisPeer == null) {
            logWarning("Node disconnected while getting folders");
            return Collections.emptyList();
        }
        String magicId = thisPeer.getMyMagicId();
        // TODO Think about a better way
        FolderList fList = getLastFolderList();
        if (fList == null) {
            logWarning("Unable to get last folder list");
            return Collections.emptyList();
        }
        List<Folder> requestedFolders = new LinkedList<Folder>();
        for (Folder folder : getController().getFolderRepository().getFolders(
            true))
        {
            FolderInfo foInfo = folder.getInfo();

            if (fList.joinedMetaFolders && foInfo.isMetaFolder()) {
                Folder parentFolder = getController().getFolderRepository()
                    .getParentFolder(folder.getInfo());
                if (parentFolder != null) {
                    foInfo = parentFolder.getInfo();
                }
            }

            if (fList.contains(foInfo, magicId)) {
                requestedFolders.add(folder);
            }
        }
        return requestedFolders;
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
        Folder folder = file.getFolder(getController().getFolderRepository());
        if (folder == null) {
            // Folder not joined, so we don't have the file.
            return null;
        }
        if (isMySelf()) {
            return folder.getDAO().find(file, null);
        }
        return folder.getDAO().find(file, getId());
    }

    /*
     * Simple getters
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
     * #1373
     *
     * @return true if this node is on the same network.
     */
    public boolean isOnSameNetwork() {
        return info.isOnSameNetwork(getController());
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
     * @return #2072: the protocol version of the {@link Externalizable}s
     */
    public int getProtocolVersion() {
        Identity id = getIdentity();
        if (id == null) {
            return 0;
        }
        return id.getProtocolVersion();
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
        return info.getLastConnectTime();
    }

    /**
     * Answers the last connect time of the user to the network. Last connect
     * time is determinded by the information about users from other nodes and
     * own last connection date to that node
     *
     * @return Date object representing the last time on the network
     */
    public Date getLastNetworkConnectTime() {
        Date lastConnectTime = getLastConnectTime();
        if (lastConnectTime == null) {
            return lastNetworkConnectTime;
        } else if (lastNetworkConnectTime == null) {
            return lastConnectTime;
        }
        if (lastConnectTime.after(lastNetworkConnectTime)) {
            return lastConnectTime;
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
     * Answers if this member is connected to the PF network
     *
     * @return true if this member is connected to the PF network
     */
    public boolean isConnectedToNetwork() {
        return isCompletelyConnected() || isConnectedToNetwork;
    }

    /**
     * set the connected to network status
     *
     * @param connected
     *            flag indicating if this member is connected
     */
    public void setConnectedToNetwork(boolean connected) {
        boolean changed = isConnectedToNetwork != connected;
        isConnectedToNetwork = connected;
        if (changed) {
            getController().getNodeManager()
                .networkConnectionStateChanged(this);
        }
    }

    /**
     * Answers if we the remote node told us not longer to connect.
     *
     * @return true if the remote side didn't want to be connected.
     */
    public boolean isDontConnect() {
        return lastProblem != null
            && (lastProblem.problemCode == Problem.DO_NOT_LONGER_CONNECT || lastProblem.problemCode == Problem.NETWORK_ID_MISMATCH);
    }

    /**
     * @return true if no direct connection to this member is possible. (At
     *         least 2 tries)
     */
    public boolean isUnableToConnect() {
        return connectionRetries >= 2;
    }

    /**
     * @return the last problem received from this node.
     */
    public Problem getLastProblem() {
        return lastProblem;
    }

    /**
     * @return true if this is a server that should be reconnected
     */
    public boolean isServer() {
        return server;
    }

    /**
     * Sets/Unsets this member as server that should be reconnected.
     *
     * @param server
     */
    public void setServer(boolean server) {
        boolean oldValue = this.server;
        this.server = server;
        // Notify nodemanager
        if (oldValue != server) {

            if (!server) {
                logFine("Not longer server: " + this);
            } else {
                logFine("Is server of cluster: " + this);
            }

            // #2569: Server 2 server connection. don't wait for folder lists
            if (getController().getMySelf().isServer() && server) {
                synchronized (folderListWaiter) {
                    folderListWaiter.notifyAll();
                }
            }

//            if (server && hasJoinedAnyFolder()) {
//                synchronizeFolderMemberships();
//            }

            getController().getNodeManager().serverStateChanged(this, server);
        }
    }

    /**
     * @return the account info of the user logged in at the remote node.
     */
    public AccountInfo getAccountInfo() {
        return getController().getSecurityManager().getAccountInfo(this);
    }

    public boolean updateInfo(MemberInfo newInfo) {
        return updateInfo(newInfo, false);
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
    public boolean updateInfo(MemberInfo newInfo, boolean force) {
        boolean updated = false;
        if (force || (!isConnected() && newInfo.isConnected)) {
            // take info, if this is now a supernode
            if (newInfo.isSupernode && !info.isSupernode) {
                if (isFiner()) {
                    logFiner("Received new supernode information: " + newInfo);
                }
            }
            info.isSupernode = newInfo.isSupernode;
            info.networkId = newInfo.networkId;
            // Update address only if not null IP.

            InetSocketAddress newAddress = newInfo.getConnectAddress();
            if (newAddress != null) {
                boolean updateConnectAddress = false;

                if (newAddress.isUnresolved()) {
                    updateConnectAddress = true;
                }

                if (newAddress.getAddress() != null
                    && !NetworkUtil.isNullIP(newAddress.getAddress()))
                {
                    updateConnectAddress = true;
                }

                if (updateConnectAddress) {
                    info.setConnectAddress(newAddress);
                } else if (isFiner()) {
                    logFiner("Not updating address. Got: "
                        + info.getConnectAddress() + ". New: " + newAddress);
                }
            }
            updated = true;
        }

        // Take his last connect time if newer
        Date newLastConnectTime = newInfo.getLastConnectTime();
        boolean updateLastNetworkConnectTime = (lastNetworkConnectTime == null && newLastConnectTime != null)
            || (newLastConnectTime != null && lastNetworkConnectTime
                .before(newLastConnectTime));

        if (!isConnected() && updateLastNetworkConnectTime) {
            // logFiner(
            // "Last connect time fresher on remote side. this "
            // + lastNetworkConnectTime + ", remote: "
            // + newInfo.lastConnectTime);
            lastNetworkConnectTime = newLastConnectTime;
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

    private boolean downloadRecentlyCompleted(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo is null");
        // Can't tell. So maybe yes!
        if (ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
            .getValueInt(getController()) == 0)
        {
            return true;
        }
        Download dl = getController().getTransferManager()
            .getCompletedDownload(this, fInfo);
        if (dl != null) {
            return true;
        }
        return false;
    }

    // Logger methods *********************************************************

//    @Override
//    public String getLoggerName() {
//        return super.getLoggerName() + " '" + getNick() + '\''
//            + (isSupernode() ? " (s)" : "");
//    }

    /*
     * General
     */

    @Override
    public String toString() {
        String connect;

        if (isConnected()) {
            connect = peer + "";
        } else {
            connect = isMySelf() ? "myself" : "-disco.-, " + "recon. at "
                + getReconnectAddress();
        }

        return "Member '" + info.nick + "' (" + connect + ')';
    }

    /**
     * true if the ID's of the memberInfo objects are equal
     *
     * @param other
     * @return true if the ID's of the memberInfo objects are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Member) {
            Member oM = (Member) other;
            return Util.equals(info.id, oM.info.id);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (info.id == null) ? 0 : info.id.hashCode();
    }

    @Override
    public int compareTo(Member m) {
        return info.id.compareTo(m.info.id);
    }
}
