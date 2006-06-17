/* $Id: Member.java,v 1.115 2006/04/23 16:01:35 totmacherr Exp $
 */
package de.dal33t.powerfolder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.InvalidIdentityException;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.net.NetworkUtil;

/**
 * A full quailfied member, can have a connection to interact with remote
 * member/fried/peer <BR>
 * FIXME: Start send/receive of message only after handshake
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.115 $
 */
public class Member extends PFComponent {
    public static final String CONFIG_ASKFORFRIENDSHIP = "askforfriendship";

    // Listener support for incoming messages
    private MessageListenerSupport messageListenerSupport = new MessageListenerSupport(
        this);

    // The current connection handler
    private ConnectionHandler peer;

    // If this node has completely handshaked. TODO: Move this into
    // connectionHandler ?
    private boolean handshaked;

    // last know address
    private int connectionRetries;

    // The total number of reconnection tries at this moment
    private int currentReconTries;

    // Flag, that we are not able to connect directly
    private boolean unableToConnect;

    // Number of interesting marks, set by markAsInteresting
    private int interestMarks;

    // his member information
    private MemberInfo info;

    // The last time, the node was seen on the network
    private Date lastNetworkConnectTime;

    // Lock when peer is going to be initalized
    private Object peerInitalizeLock = new Object();

    // Folderlist waiter
    private Object folderListWaiter = new Object();

    // Last folder memberships
    private FolderList lastFolderList;
    // Last know file list
    private Map<FolderInfo, Map<FileInfo, FileInfo>> lastFiles = Collections
        .synchronizedMap(new HashMap());
    // Last trasferstatus
    private TransferStatus lastTransferStatus;

    // Mutal friend status cache
    private boolean mutalFriend;

    // the average response time of a request
    private long averageResponseTime;

    // maybe we cannot connect, but member might be online
    private boolean isConnectedToNetwork;

    // Flag if we received a wrong identity from remote side
    private boolean receivedWrongRemoteIdentity;

    // If already asked for friendship
    private boolean askedForFriendship;

    // the cached ip adress
    private String ip;

    // the cached hostname
    private String hostname;

    /**
     * Constructs a member using parameters from another member. nick, id ,
     * connect address.
     * <p>
     * Attention:Does not takes friend status from memberinfo !! you have to
     * manually
     * 
     * @param controller
     * @param from
     */
    public Member(Controller controller, MemberInfo mInfo) {
        super(controller);
        // Clone memberinfo
        this.info = (MemberInfo) mInfo.clone();
        this.info.isFriend = false;
        this.receivedWrongRemoteIdentity = false;
        this.unableToConnect = false;
    }

    /**
     * Constructs a new local member without a connection
     */
    public Member(Controller controller, String nick, String id) {
        this(controller, new MemberInfo(nick, id));
        handshaked = false;
    }

    /**
     * true if this member matches the search string or if it equals the IP nick
     * contains the search String
     */
    public boolean matchesFast(String searchString) {
        String ip = getIP();
        if (ip != null && ip.equals(searchString)) {
            return true;
        }
        return ((getNick().toLowerCase().indexOf(searchString.toLowerCase()) >= 0));
    }

    /**
     * true if this member matches the search string true if equals hostname or
     * IP , or if nick contains the search String
     */
    public boolean matches(String searchString) {
        String hostName = getHostName();
        if (hostName != null && hostName.equals(searchString)) {
            return true;
        }
        String ip = getIP();
        if (ip != null && ip.equals(searchString)) {
            return true;
        }
        return ((getNick().toLowerCase().indexOf(searchString.toLowerCase()) >= 0));
    }

    public String getHostName() {
        if (hostname == null) {
            if (getReconnectAddress() == null) {
                return null;
            }
            hostname = getReconnectAddress().getHostName();
        }
        return hostname;
    }

    public String getIP() {
        if (ip == null) {
            if (getReconnectAddress() == null
                || getReconnectAddress().getAddress() == null)
            {
                return null;
            }
            ip = getReconnectAddress().getAddress().getHostAddress();
        }
        return ip;
    }

    /**
     * Answers if this is myself
     * 
     * @return
     */
    public boolean isMySelf() {
        return equals(getController().getMySelf());
    }

    /**
     * Answers if this node is our masternode
     * 
     * @return
     */
    public boolean isMaster() {
        return this.equals(getController().getNodeManager().getMasterNode());
    }

    /**
     * Answers if this member is a friend, also true if isMySelf()
     * 
     * @return
     */
    public boolean isFriend() {
        return info.isFriend || isMySelf();
    }

    /**
     * Sets fried status of this member
     * 
     * @param friend
     */
    public void setFriend(boolean newFriend) {
        boolean oldValue = info.isFriend;
        info.isFriend = newFriend;

        if (oldValue != newFriend) {
            // Inform node manager first
            getController().getNodeManager()
                .friendStateChanged(this, newFriend);
        }
    }

    /**
     * Answers if this node is running in private networking mode
     * 
     * @return
     */
    public boolean isPrivateNetworking() {
        return peer != null && peer.getIdentity() != null
            && peer.getIdentity().privateMode;
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
        getController().getNodeManager().markNodeForImmediateReconnection(this);
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
     * Answers if this node is interesting for us
     * 
     * @return
     */
    public boolean isInteresting() {
        if (!isOnLAN() && getController().isLanOnly()) {
            return false;
        }
        boolean interesting = (interestMarks > 0) || isFriend() || isOnLAN()
            || hasJoinedAnyFolder();

        if (!interesting
            && (getController().getMySelf().isSupernode() || isSupernode()))
        {
            // Still capable of new connections?
            interesting = !getController().getNodeManager()
                .maxConnectionsReached();
        }

        return interesting;
    }

    /**
     * Answers if this node is currently reconnecting
     * 
     * @return
     */
    public boolean isReconnecting() {
        return currentReconTries > 0;
    }

    /**
     * Answers if this member has a connected peer. To check if a node is
     * completey connected & handshaked see <code>isCompletelyConnected</code>
     * 
     * @see #isCompleteyConnected()
     * @return
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
     * @return
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

    /**
     * Answers if this member is on the local net.
     * 
     * @return
     */
    public boolean isOnLAN() {
        return peer != null && peer.isOnLAN();
    }

    /**
     * To set the lan status of the member for external source
     * 
     * @param onlan
     */
    public void setOnLAN(boolean onlan) {
        if (peer != null) {
            if (!isOnLAN() && onlan) {
                // Node is on lan, lets use our connect address as reconnect
                // address
                if (logVerbose) {
                    log().verbose(
                        "Node is on lan, take connect address for reconnect");
                }
                // COMMENTED until onlan 100% working
                // info.connectAddress = new InetSocketAddress(peer
                // .getRemoteAddress().getAddress(), info.connectAddress
                // .getPort());
            }

            peer.setOnLAN(onlan);
        }
    }

    /**
     * Answers if we received a wrong identity on reconnect
     * 
     * @return
     */
    public boolean receivedWrongIdentity() {
        return receivedWrongRemoteIdentity;
    }

    /**
     * removes the peer handler, and shuts down connection
     */
    private void shutdownPeer() {
        if (peer != null) {
            peer.setMember(null);
            peer.shutdown();
        }
        synchronized (peerInitalizeLock) {
            peer = null;
        }
    }

    /**
     * Sets the new connection handler for this member
     * 
     * @param handler
     * @throws ConnectionException
     *             if peer has no identity
     */
    public void setPeer(ConnectionHandler newPeer) throws ConnectionException {
        if (newPeer == null) {
            throw new NullPointerException(
                "Illegal call of setPeer(null), use removePeer()");
        }

        if (!newPeer.isConnected()) {
            log().warn(
                "Peer disconnected while initializing connection: " + newPeer);
            return;
        }

        if (logVerbose) {
            log().verbose("Setting peer to " + newPeer);
        }

        synchronized (peerInitalizeLock) {
            if (peer != null) {
                // shutdown old peer
                peer.shutdown();
            }

            Identity identity = newPeer.getIdentity();

            // check if identity is valid and matches the this member
            if (identity == null || !identity.isValid()
                || !identity.member.matches(this))
            {
                // Wrong identity from remote side ? set our flag
                receivedWrongRemoteIdentity = !identity.member.matches(this);

                // tell remote client
                newPeer.sendMessage(IdentityReply.reject("Invalid identity: "
                    + (identity != null ? identity.member.id : "-none-")
                    + ", expeced " + this));
                throw new InvalidIdentityException(this
                    + " Remote peer has wrong identity. remote ID: "
                    + identity.member.id + ", our ID: " + this.getId(), newPeer);
            }

            // Take his supernode state
            info.isSupernode = identity.member.isSupernode;

            if (newPeer.getRemoteListenerPort() >= 0) {
                // get the data from remote peer
                // connect address is his currently connected ip + his
                // listner port if not supernode
                if (identity.member.isSupernode) {
                    // Remote peer is supernode, take his info, he knows
                    // about himself
                    info.setConnectAddress(identity.member.getConnectAddress());
                } else {
                    info.setConnectAddress(new InetSocketAddress(newPeer
                        .getRemoteAddress().getAddress(), newPeer
                        .getRemoteListenerPort()));
                }
            } else {
                // Remote peer has no listener running
                info.setConnectAddress(null);
            }

            info.id = identity.member.id;
            info.nick = identity.member.nick;

            // ok, we accepted, set peer

            // Set the new peer
            synchronized (peerInitalizeLock) {
                peer = newPeer;
            }
            newPeer.setMember(this);

            // now handshake
            log().debug("Sending accept of identity to " + this);
            newPeer.sendMessageAsynchron(IdentityReply.accept(), null);
        }

        // wait if we get accepted, AFTER holding PeerInitalizeLock! otherwise
        // lock will be hold up to 60 secs
        boolean accepted = newPeer.waitForIdentityAccept();

        if (!accepted) {
            newPeer.shutdown();
            throw new ConnectionException(
                "Remote side did not accept our identity");
        }

        // Reset the last connect time
        info.lastConnectTime = new Date();
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
        if (logVerbose) {
            log().verbose(
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
            String hostname = getHostName(); // cached hostname
            if (logVerbose) {
                log().verbose(
                    "Reconnect hostname to " + getNick() + " is: " + hostname);
            }
            if (!StringUtils.isBlank(hostname)) {
                info.setConnectAddress(new InetSocketAddress(hostname, info
                    .getConnectAddress().getPort()));
            }

            // Another check: do not reconnect if controller is not running
            if (!getController().isStarted()) {
                return false;
            }

            String cfgBind = getController().getConfig().getProperty(
                "net.bindaddress");
            Socket socket = new Socket();
            if (!StringUtils.isEmpty(cfgBind)) {
                socket.bind(new InetSocketAddress(cfgBind, 0));
            }
            socket.connect(info.getConnectAddress(),
                Constants.SOCKET_CONNECT_TIMEOUT);
            NetworkUtil.setupSocket(socket);

            handler = new ConnectionHandler(getController(), socket);
            setPeer(handler);
            // Complete handshake now
            // if (completeHandshake() && logEnabled) {
            if (completeHandshake()) {
                log().debug(
                    "Reconnect successfull ("
                        + getController().getNodeManager()
                            .countConnectedNodes() + " connected)");
            }

            successful = true;
        } catch (InvalidIdentityException e) {
            log().verbose(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
            throw e;
        } catch (IOException e) {
            log().verbose(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
        } catch (ConnectionException e) {
            log().warn(e);
            // Shut down reconnect handler
            if (handler != null) {
                handler.shutdown();
            }
        } finally {
            currentReconTries--;
        }

        if (!successful) {
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
    public boolean completeHandshake() {
        if (!isConnected()) {
            return false;
        }

        // Get know nodes, before getting peerinit lock
        Message[] nodeLists = KnownNodes.createKnowNodesList(getController()
            .getNodeManager());

        synchronized (peerInitalizeLock) {
            if (!isConnected()) {
                log().error("Disconnected while completing handshake");
                return false;
            }
            // Send node informations now
            // Send joined folders to synchronize
            FolderList folderList = new FolderList(getController()
                .getFolderRepository().getJoinedFolderInfos(), peer
                .getRemoteMagicId());
            peer.sendMessageAsynchron(folderList, null);

            // Send our transfer status
            peer.sendMessageAsynchron(getController().getTransferManager()
                .getStatus(), null);

            // Send known nodes
            peer.sendMessagesAsynchron(nodeLists);
        }

        // Handshaked, but now check if node is intersting
        if (!isInteresting()) {
            // Okey, give him some seconds to make himself interesting
            // e.g. filelist
            waitForFolderList();
        }

        synchronized (peerInitalizeLock) {
            if (!isConnected()) {
                log().error("Disconnected while completing handshake");
                return false;
            }

            if (!isInteresting()) {
                log().warn("Rejected, Node not interesting");
                // Tell remote side
                try {
                    peer.sendMessage(new Problem("You are boring", true,
                        Problem.YOU_ARE_BORING));
                } catch (ConnectionException e) {
                    log().verbose(e);
                }
                handshaked = false;
            }
        }

        // Handshaked ?
        handshaked = isConnected();

        if (handshaked) {
            // Reset things
            connectionRetries = 0;
            unableToConnect = false;

            // if (logEnabled) {
            log().info(
                "Connected ("
                    + getController().getNodeManager().countConnectedNodes()
                    + " total)");
            // }

            // Supernode <-> Supernode communication on public networking
            if (getController().isPublicNetworking() && isSupernode()
                && getController().getMySelf().isSupernode())
            {
                sendMessageAsynchron(RequestNetworkFolderList.COMPLETE_LIST,
                    "Unable to request network folder list");
            }

            // Inform nodemanger about it
            getController().getNodeManager().onlineStateChanged(this);

            if (getController().isVerbose()) {
                // Running in verbose mode, directly request node information
                sendMessageAsynchron(new RequestNodeInformation(), null);
            }
        } else {
            // Not handshaked, shutdown
            shutdown();
        }

        return handshaked;
    }

    /**
     * Waits some time for the folder list
     * 
     * @return true if list was received successfully
     */
    private boolean waitForFolderList() {
        if (logVerbose) {
            log().verbose("Waiting for folderlist");
        }
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
     * Shuts the member and its connection down
     */
    public void shutdown() {
        boolean wasCompletelyConnected = isCompleteyConnected();
        // Disco, assume completely
        setConnectedToNetwork(false);
        handshaked = false;
        shutdownPeer();
        if (wasCompletelyConnected) {
            // Inform nodemanger about it
            getController().getNodeManager().onlineStateChanged(this);

            // if (logEnabled) {
            log().info(
                "Disconnected ("
                    + getController().getNodeManager().countConnectedNodes()
                    + " still connected)");
            // }
        } else {
            // log().verbose("Shutdown");
        }
    }

    /**
     * Helper method for sending messages on peer handler. Method waits for the
     * sendmessagebuffer to get empty
     * 
     * @param message
     * @throws ConnectionException
     */
    public void sendMessage(Message message) throws ConnectionException {
        checkPeer();

        if (peer != null) {
            // wait
            peer.waitForEmptySendQueue();
            synchronized (peerInitalizeLock) {
                if (peer != null) {
                    // send
                    peer.sendMessage(message);
                }
            }

        }
    }

    /**
     * Enque one messages for sending. code execution does not wait util message
     * was sent successfully
     * 
     * @see ConnectionHandler#sendMessageAsynchron(Message, String)
     * @param message
     *            the message to send
     * @param errorMessage
     *            the error message to be logged on connection problem
     */
    public void sendMessageAsynchron(Message message, String errorMessage) {
        // synchronized (peerInitalizeLock) {
        if (peer != null && peer.isConnected()) {
            peer.sendMessageAsynchron(message, errorMessage);
        }
        // }
    }

    /**
     * Enque multiple messages for sending. code execution does not wait util
     * message was sent successfully
     * 
     * @see ConnectionHandler#sendMessageAsynchron(Message, String)
     * @param messages
     *            the messages to send
     */
    public void sendMessagesAsynchron(Message... messages) {
        peer.sendMessagesAsynchron(messages);
    }

    /**
     * Handles a message from the remote peer
     * 
     * @param message
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

        if (message instanceof FolderList) {
            FolderList fList = (FolderList) message;
            lastFolderList = fList;
            joinToLocalFolders(fList);

            // Notify waiting ppl
            synchronized (folderListWaiter) {
                folderListWaiter.notifyAll();
            }

            // Inform folder repo
            getController().getFolderRepository().receivedFolderList(this,
                fList);

        } else if (message instanceof RequestNetworkFolderList) {
            RequestNetworkFolderList request = (RequestNetworkFolderList) message;
            // Build answer
            NetworkFolderList netList = new NetworkFolderList(getController()
                .getFolderRepository(), request);
            // Split lists
            NetworkFolderList[] netLists = netList
                .split(Constants.NETWORK_FOLDER_LIST_MAX_FOLDERS);
            for (int i = 0; i < netLists.length; i++) {
                sendMessageAsynchron(netLists[i],
                    "Unable to send network folder list");
            }

        } else if (message instanceof NetworkFolderList) {
            NetworkFolderList netFolderList = (NetworkFolderList) message;
            // Inform repo
            getController().getFolderRepository().receivedNetworkFolderList(
                this, netFolderList);

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

        } else if (message instanceof RequestDownload) {
            // a download is requested
            RequestDownload dlReq = (RequestDownload) message;
            getController().getTransferManager().queueUpload(this, dlReq);

        } else if (message instanceof DownloadQueued) {
            // set queued flag here, if we received status from other side
            DownloadQueued dlQueued = (DownloadQueued) message;
            getController().getTransferManager().setQueued(dlQueued);

        } else if (message instanceof AbortDownload) {
            AbortDownload abort = (AbortDownload) message;
            // Abort the upload
            getController().getTransferManager().abortUpload(abort.file, this);

        } else if (message instanceof FileChunk) {
            // File chunk received
            FileChunk chunk = (FileChunk) message;
            getController().getTransferManager().chunkReceived(chunk, this);

        } else if (message instanceof RequestNodeList) {
            // Send nodelist
            sendMessagesAsynchron(KnownNodes
                .createKnowNodesList(getController().getNodeManager()));

        } else if (message instanceof KnownNodes) {
            KnownNodes newNodes = (KnownNodes) message;
            // if (newNodes.nodes.length == 1) {
            // log().warn("Received single node list: " + newNodes.nodes[0]);
            // } else {
            // //log().warn("Received node list ( " + newNodes.nodes.length +
            // // " )");
            // }

            if (isMaster()) {
                // Set friendship
                setFriend(true);
                log().verbose("Syncing friendlist with master");
            }

            // This might also just be a search result and thus not include us
            // mutalFriend = false;
            for (int i = 0; i < newNodes.nodes.length; i++) {
                MemberInfo remoteNodeInfo = newNodes.nodes[i];
                if (remoteNodeInfo == null) {
                    continue;
                }

                if (getInfo().equals(remoteNodeInfo)) {
                    // Take his info
                    updateInfo(remoteNodeInfo);

                }

                if (remoteNodeInfo
                    .equals(getController().getMySelf().getInfo()))
                {
                    // Check for mutal friendship
                    mutalFriend = remoteNodeInfo.isFriend;
                }

                // Syncing friendlist with master
                if (isMaster()) {
                    Member node = remoteNodeInfo.getNode(getController(), true);
                    node.setFriend(remoteNodeInfo.isFriend);
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
            log().debug(
                remoteFileList.folder + ": Received new filelist ("
                    + remoteFileList.folder.filesCount + " file(s)) from "
                    + this);

            // Add filelist to filelist cache
            Map<FileInfo, FileInfo> cachedFileList = Collections
                .synchronizedMap(new HashMap(remoteFileList.files.length));
            for (int i = 0; i < remoteFileList.files.length; i++) {
                cachedFileList.put(remoteFileList.files[i],
                    remoteFileList.files[i]);
            }
            lastFiles.put(remoteFileList.folder, cachedFileList);

            // Trigger requesting
            if (targetFolder != null) {
                // Write filelist
                if (Logger.isLogToFileEnabled()) {
                    // Write filelist to disk
                    File debugFile = new File("debug/" + targetFolder.getName()
                        + "/" + getNick() + ".list.txt");
                    Debug.writeFileList(cachedFileList.keySet(),
                        "FileList of folder " + targetFolder.getName()
                            + ", member " + this + ":", debugFile);
                }
                // Inform folder
                targetFolder.fileListChanged(this, remoteFileList);
            }
        } else if (message instanceof FolderFilesChanged) {
            if (logVerbose) {
                log().verbose("FileListChange received: " + message);
            }
            FolderFilesChanged changes = (FolderFilesChanged) message;

            // Correct filelist
            Map<FileInfo, FileInfo> cachedFileList = getLastFileList0(changes.folder);
            if (cachedFileList == null) {
                log().warn(
                    "Received folder changes on " + changes.folder.name
                        + ", but not received the full filelist");
                return;
            }
            synchronized (cachedFileList) {
                if (changes.added != null) {
                    for (int i = 0; i < changes.added.length; i++) {
                        FileInfo file = changes.added[i];
                        cachedFileList.remove(file);
                        cachedFileList.put(file, file);
                    }
                }
                if (changes.modified != null) {
                    for (int i = 0; i < changes.modified.length; i++) {
                        FileInfo file = changes.modified[i];
                        cachedFileList.remove(file);
                        cachedFileList.put(file, file);
                    }
                }
                if (changes.removed != null) {
                    for (int i = 0; i < changes.removed.length; i++) {
                        FileInfo file = changes.removed[i];
                        cachedFileList.remove(file);
                        cachedFileList.put(file, file);
                    }
                }
            }

            if (targetFolder != null) {
                // Write filelist
                if (Logger.isLogToFileEnabled()) {
                    // Write filelist to disk
                    File debugFile = new File("debug/" + targetFolder.getName()
                        + "/" + getNick() + ".list.txt");
                    Debug.writeFileList(cachedFileList.keySet(),
                        "FileList of folder " + targetFolder.getName()
                            + ", member " + this + ":", debugFile);
                }
                // Inform folder
                targetFolder.fileListChanged(this, changes);
            }
        } else if (message instanceof Invitation) {
            // Invitation to folder
            Invitation invitation = (Invitation) message;
            // To ensure invitor is correct
            invitation.invitor = this.getInfo();

            getController().getFolderRepository().invitationReceived(
                invitation, true, false);

        } else if (message instanceof RequestBackup) {
            RequestBackup backupRequest = (RequestBackup) message;
            // Backup folder
            getController().getFolderRepository().backupRequestReceived(this,
                backupRequest);

        } else if (message instanceof Problem) {
            Problem problem = (Problem) message;

            if (problem.problemCode == Problem.YOU_ARE_BORING) {
                // Finds us boring
                // set unable to connect
                log().debug("Node finds us boring, not longer connect");
                unableToConnect = true;
                // Not connected to public network
                isConnectedToNetwork = true;
            } else {
                log().warn("Problem received: " + problem);
            }

            if (problem.fatal) {
                // Shutdown
                shutdown();
            }
        } else if (message instanceof SearchNodeRequest) {
            // Send nodelist that matches the search.
            // Since this request is only sent to supernodes, accept it as
            // supernode only
            if (getController().getMySelf().isSupernode()) {
                final SearchNodeRequest request = (SearchNodeRequest) message;
                new Thread() {
                    public void run() {
                        List<MemberInfo> reply = new LinkedList<MemberInfo>();
                        for (Member m : getController().getNodeManager()
                            .getValidNodes())
                        {
                            if (m.matches(request.searchString)) {
                                reply.add(m.getInfo());
                            }
                        }

                        if (!reply.isEmpty()) {
                            sendMessageAsynchron(new KnownNodes(reply
                                .toArray(new MemberInfo[0])), null);
                        }
                    }
                }.start();
            }
        } else {
            log().warn(
                "Unknown message received from peer: "
                    + message.getClass().getName());
        }

        // now give the message to all message listeners
        fireMessageToListeners(message);
    }

    /**
     * Adds a message listener
     * 
     * @param aListener
     */
    public void addMessageListener(MessageListener aListener) {
        messageListenerSupport.addMessageListener(aListener);
    }

    /**
     * Adds a message listener, which is only triggerd if a message of type
     * <code>messageType</code> is received.
     * 
     * @param messageType
     * @param aListener
     */
    public void addMessageListener(Class messageType, MessageListener aListener)
    {
        messageListenerSupport.addMessageListener(messageType, aListener);
    }

    /**
     * Removes a message listener completely from this member
     * 
     * @param aListener
     */
    public void removeMessageListener(MessageListener aListener) {
        messageListenerSupport.removeMessageListener(aListener);
    }

    /**
     * Overriden, removes message listeners also
     * 
     * @see de.dal33t.powerfolder.PFComponent#removeAllListener()
     */
    public void removeAllListener() {
        log().verbose("Removing all listeners from member. " + this);
        super.removeAllListener();
        // Remove message listeners
        messageListenerSupport.removeAllListener();
    }

    /**
     * Fires a message to all message listeners
     * 
     * @param message
     *            the message to fire
     */
    private void fireMessageToListeners(Message message) {
        messageListenerSupport.fireMessage(this, message);
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
        if (isMySelf()) {
            return;
        }
        if (!isConnected()) {
            return;
        }
        if (joinedFolders == null) {
            throw new NullPointerException("Joined folders is null");
        }
        FolderList folderList = getLastFolderList();
        if (folderList != null) {
            FolderList myFolderList = new FolderList(joinedFolders, peer
                .getRemoteMagicId());
            sendMessageAsynchron(myFolderList, null);
            // Rejoin to local folders
            joinToLocalFolders(folderList);
        } else {
            log()
                .error(
                    "Unable to synchronize memberships, did not received folderlist from remote");
        }
    }

    /**
     * Joins member to all local folders which are also available on remote
     * peer, removes member from all local folders, if not longer member of
     * 
     * @throws ConnectionException
     */
    private void joinToLocalFolders(FolderList folderList) {
        FolderInfo[] remoteFolders = folderList.folders;
        log()
            .verbose(
                "Joining into folders, he has " + remoteFolders.length
                    + " folders");
        FolderRepository repo = getController().getFolderRepository();

        HashSet<FolderInfo> joinedFolder = new HashSet();
        int newUnjoinedFolders = 0;

        // join all, which exists here and there
        for (int i = 0; i < remoteFolders.length; i++) {
            Folder folder = repo.getFolder(remoteFolders[i]);
            if (folder != null) {
                // we have the folder here, join member to now
                joinedFolder.add(folder.getInfo());
                // Always rejoin folder, folder information should be sent to
                // him
                folder.join(this);
            } else if (isInteresting()) {
                // Add unjoined folder info if is interesting
                boolean folderNew = repo.addUnjoinedFolder(remoteFolders[i],
                    this);
                if (folderNew) {
                    newUnjoinedFolders++;
                }
            }
        }

        // Process secrect folders now
        if (folderList.secretFolders != null
            && folderList.secretFolders.length > 0)
        {

            // Step 1: Calculate encrypted folder ids for local secret folders
            Map localSecretFolders = new HashMap();
            FolderInfo[] localFolders = repo.getJoinedFolderInfos();
            synchronized (peerInitalizeLock) {
                if (peer != null) {
                    for (int i = 0; i < localFolders.length; i++) {
                        if (localFolders[i].secret) {
                            FolderInfo secretFolderCanidate = (FolderInfo) localFolders[i]
                                .clone();
                            if (!StringUtils.isEmpty(peer.getMyMagicId())) {
                                // Calculate id with my magic id
                                secretFolderCanidate.id = secretFolderCanidate
                                    .calculateSecureId(peer.getMyMagicId());
                                // Add to local secret folder list
                                localSecretFolders.put(secretFolderCanidate,
                                    repo.getFolder(localFolders[i]));
                            }
                        }
                    }
                }
            }
            // Step 2: Check if remote side has joined one of our secret folders
            for (int i = 0; i < folderList.secretFolders.length; i++) {
                FolderInfo secretFolder = folderList.secretFolders[i];
                if (localSecretFolders.containsKey(secretFolder)) {
                    log().verbose("Also has secret folder: " + secretFolder);
                    Folder folder = (Folder) localSecretFolders
                        .get(secretFolder);
                    // Okay, join him into folder
                    joinedFolder.add(folder.getInfo());
                    // Join him into our folder
                    folder.join(this);
                }
            }
        }

        // ok now remove member from not longer joined folders
        FolderInfo[] localFolders = repo.getJoinedFolderInfos();
        for (int i = 0; i < localFolders.length; i++) {
            Folder localFolder = repo.getFolder(localFolders[i]);
            if (localFolder != null
                && !joinedFolder.contains(localFolder.getInfo()))
            {
                // remove this member from folder, if not on new folder
                localFolder.remove(this);
            }
        }

        if (joinedFolder.size() > 0) {
            log().info(
                getNick() + " joined " + joinedFolder.size() + " folder(s)");
            if (!isFriend()) {
                // Ask for friendship of guy
                askForFriendship(joinedFolder);
            }
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
     * @return
     */
    public FolderList getLastFolderList() {
        return lastFolderList;
    }

    /**
     * Answers if user has a filelist for the folder
     * 
     * @param foInfo
     * @return
     */
    public boolean hasFileListFor(FolderInfo foInfo) {
        return getLastFileList0(foInfo) != null;
    }

    /**
     * Answers the last filelist of a member/folder May return null.
     * 
     * @param folder
     * @return
     */
    private Map<FileInfo, FileInfo> getLastFileList0(FolderInfo foInfo) {
        FolderList list = getLastFolderList();
        // FIXME: Check if node still on folder
        if (list == null) {
            // Node not on folder or did not send a folder list yet
            // Thus we do not return any filelist
            return null;
        }
        return lastFiles.get(foInfo);
    }

    /**
     * Answers the last filelist of a member/folder. Returns null if no filelist
     * has been received yet. But may return empty collection
     * 
     * @param folder
     * @return
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
     * Returns the last transfer status of this node
     * 
     * @return
     */
    public TransferStatus getLastTransferStatus() {
        if (isMySelf()) {
            return getController().getTransferManager().getStatus();
        }
        return lastTransferStatus;
    }

    /**
     * Answers if user joined any folder. TODO: Add if the user is on any folder
     * based on network folder list.
     * 
     * @return
     */
    public boolean hasJoinedAnyFolder() {
        FolderInfo[] folders = getController().getFolderRepository()
            .getJoinedFolderInfos();
        for (int i = 0; i < folders.length; i++) {
            Folder folder = getController().getFolderRepository().getFolder(
                folders[i]);
            if (folder != null) {
                if (folder.hasMember(this)) {
                    // Okay, on folder
                    return true;
                }
            }
        }
        // Not found on any folder
        return false;
    }

    /**
     * Answers if member has the file available to download. Does NOT check
     * version match
     * 
     * @param file
     * @return
     */
    public boolean hasFile(FileInfo file) {
        FileInfo remoteFile = getFile(file);
        return remoteFile != null && !remoteFile.isDeleted();
    }

    /**
     * Returns the remote file info from the node. May return null if file is
     * not known by remote or no filelist was received yet
     * 
     * @param the
     *            local file
     * @return file the file of remote side, or null
     */
    public FileInfo getFile(FileInfo file) {
        if (file == null) {
            throw new NullPointerException("File is null");
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
     * @return
     */
    public String getId() {
        return info.id;
    }

    public String getNick() {
        return info.nick;
    }

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
     * @return
     */
    public InetSocketAddress getReconnectAddress() {
        return info.getConnectAddress();
    }

    /**
     * Answers when the member connected last time or null, if member never
     * connected
     * 
     * @return
     */
    public Date getLastConnectTime() {
        return info.lastConnectTime;
    }

    /**
     * Answers the last connect time of the user to the network. Last connect
     * time is determinded by the information about users from other nodes and
     * own last connection date to that node
     * 
     * @return
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
     * Answers if this member is a mutal friend
     * 
     * @return
     */
    public boolean isMutalFriend() {
        if (isMySelf()) {
            return true;
        } else if (!isFriend()) {
            return false;
        }
        return mutalFriend;
    }

    /**
     * @return
     */
    public Controller getController() {
        return super.getController();
    }

    /**
     * Returns the member information. add connected info
     * 
     * @return
     */
    public MemberInfo getInfo() {
        info.isConnected = isConnected();
        return info;
    }

    /**
     * @return
     */
    public long getAverageResponseTime() {
        return averageResponseTime;
    }

    /**
     * Answers if this member is conencted to the PF network
     * 
     * @return
     */
    public boolean isConnectedToNetwork() {
        return isConnected() || isConnectedToNetwork;
    }

    /**
     * @param b
     */
    public void setConnectedToNetwork(boolean b) {
        isConnectedToNetwork = b;
    }

    /**
     * Answers if we are unable to connect to this node directly
     * 
     * @return
     */
    public boolean isUnableToConnect() {
        return unableToConnect;
    }

    /**
     * Updates connection information, if the other is more 'valueable'
     * 
     * @param newInfo
     * @return true if we found valueable information
     */
    public boolean updateInfo(MemberInfo newInfo) {
        boolean updated = false;
        if (newInfo.isSupernode || (!isConnected() && newInfo.isConnected)) {
            // take info, if this is now a supernode
            if (newInfo.isSupernode && !info.isSupernode) {
                if (logVerbose) {
                    log().verbose(
                        "Received new supernode information: " + newInfo);
                }
                info.isSupernode = true;
                updated = true;
            }
            // if (!isOnLAN()) {
            // Take his dns address, but only if not on lan
            // (Otherwise our ip to him will be used as reconnect address)
            // Commentend until 100% LAN/inet detection accurate
            info.setConnectAddress(newInfo.getConnectAddress());
            // }
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

        return updated;
    }

    /**
     * Asks the user, if this member should be added to friendlist if not
     * already done. Wont ask if user don't
     * 
     * @return
     */
    private void askForFriendship(final HashSet<FolderInfo> joinedFolders) {
        boolean neverAsk = "false".equalsIgnoreCase(getController().getConfig()
            .getProperty(CONFIG_ASKFORFRIENDSHIP));
        if (getController().isUIOpen() && !isFriend() && !neverAsk
            && !askedForFriendship)
        {
            // Okay we are asking for friendship now
            askedForFriendship = true;

            Runnable friendAsker = new Runnable() {
                public void run() {
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.ST_NODE);

                    String folderString = "";
                    for (FolderInfo folderInfo : joinedFolders) {
                        String secrectOrPublicText;
                        if (folderInfo.secret) {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.secret");
                        } else {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.public");
                        }
                        folderString += folderInfo.name + " ("
                            + secrectOrPublicText + ")\n";
                    }
                    Object[] options = {
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.yes"),
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.no"),
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.no_neveraskagain")};
                    String text = Translation.getTranslation(
                        "dialog.addmembertofriendlist.question", getNick(),
                        folderString)
                        + "\n\n"
                        + Translation
                            .getTranslation("dialog.addmembertofriendlist.explain");
                    // if mainframe is hidden we should wait till its opened
                    int result = JOptionPane
                        .showOptionDialog(getController().getUIController()
                            .getMainFrame().getUIComponent(), text,
                            Translation
                                .getTranslation(
                                    "dialog.addmembertofriendlist.title",
                                    getNick()), JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);
                    setFriend(result == 0);
                    if (result == 2) {
                        setFriend(false);
                        // dont ask me again                        
                        getController().getConfig().setProperty(
                            CONFIG_ASKFORFRIENDSHIP, "false");
                        getController().saveConfig();
                    }
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(null);
                }
            };
            SwingUtilities.invokeLater(friendAsker);
        } else {
            setFriend(false);
        }
    }

    // Logger methods *********************************************************

    public String getLoggerName() {
        return "Node '" + getNick() + "'";
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

    public boolean equals(Object other) {
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