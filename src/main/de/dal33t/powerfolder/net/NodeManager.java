/* $Id: NodeManager.java,v 1.123 2006/04/23 18:31:14 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.event.AskForFriendshipHandler;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.KnownNodes;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.Notification;
import de.dal33t.powerfolder.message.Problem;
import de.dal33t.powerfolder.message.RequestNodeList;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.MessageListenerSupport;
import de.dal33t.powerfolder.util.NamedThreadFactory;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.net.AddressRange;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.task.SendMessageTask;

/**
 * Managing class which takes care about all old and new nodes. reconnects those
 * who disconnected and connectes to new ones
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.123 $
 */
public class NodeManager extends PFComponent {

    // The central inet nodes file
    private static final String NODES_URL = "http://nodes.powerfolder.com/PowerFolder.nodes";

    /** Timer for periodical tasks */
    private Timer timer;
    /**
     * Threadpool to handle incoming connections.
     */
    private ExecutorService threadPool;

    /** Queue holding all nodes, which are waiting to be reconnected */
    private List<Member> reconnectionQueue;
    /** The collection of reconnector */
    private List<Reconnector> reconnectors;
    /** The list of active acceptors for incoming connections */
    private List<Acceptor> acceptors;

    // Counter for started reconntor, running number
    private static int reconnectorCounter;

    // Lock which is hold while a acception is pending
    private Object acceptLock = new Object();

    private Map<String, Member> knownNodes;
    private List<Member> friends;
    private List<Member> connectedNodes;
    private List<AddressRange> lanRanges;

    private Member mySelf;
    /**
     * Set containing all nodes, that went online in the meanwhile (since last
     * broadcast)
     */
    private Set<MemberInfo> nodesWentOnline;

    // Message listener which are fired for every member
    private MessageListenerSupport valveMessageListenerSupport;

    private boolean started;
    private boolean nodefileLoaded;

    private NodeManagerListener listenerSupport;

    /** The handler that is called to ask for friendship if folders are joined */
    private AskForFriendshipHandler askForFriendshipHandler;

    public NodeManager(Controller controller) {
        super(controller);

        started = false;
        nodefileLoaded = false;
        // initzialize myself if available in config
        String nick = ConfigurationEntry.NICK.getValue(getController());

        if (controller.getCommandLine() != null
            && controller.getCommandLine().hasOption("n"))
        {
            // Take nick from command line
            nick = controller.getCommandLine().getOptionValue("n");
        }
        String idKey = "PowerFolder.nodeId";
        // check for manual id
        String id = ConfigurationEntry.NODE_ID.getValue(getController());
        if (id == null) {
            id = getController().getPreferences().get(idKey, null);
            if (id == null) {
                id = IdGenerator.makeId();
                // store ID
                log().info("Generated new ID for '" + nick + "': " + id);
                getController().getPreferences().put(idKey, id);
            }
        } else {
            log().warn("Using manual selected node id: " + id);
        }
        mySelf = new Member(getController(), nick, id);
        log().info("I am '" + mySelf.getNick() + "'");

        // Use concurrent hashmap
        knownNodes = new ConcurrentHashMap<String, Member>();

        friends = Collections.synchronizedList(new ArrayList<Member>());
        connectedNodes = Collections.synchronizedList(new ArrayList<Member>());

        // The nodes, that went online in the meantime
        nodesWentOnline = Collections
            .synchronizedSet(new HashSet<MemberInfo>());
        // Linkedlist, faster for queue useage
        reconnectionQueue = Collections
            .synchronizedList(new LinkedList<Member>());
        // All reconnectors
        reconnectors = Collections
            .synchronizedList(new ArrayList<Reconnector>());
        // Acceptors
        acceptors = Collections.synchronizedList(new ArrayList<Acceptor>());

        // Value message/event listner support
        valveMessageListenerSupport = new MessageListenerSupport(this);

        this.listenerSupport = (NodeManagerListener) ListenerSupportFactory
            .createListenerSupport(NodeManagerListener.class);

        lanRanges = new LinkedList<AddressRange>();
        String lrs[] = ConfigurationEntry.LANLIST.getValue(controller).split(
            ",");
        for (String ipr : lrs) {
            ipr = ipr.trim();
            if (ipr.length() > 0) {
                try {
                    lanRanges.add(AddressRange.parseRange(ipr));
                } catch (ParseException e) {
                    log().warn("Invalid IP range format: " + ipr);
                }
            }
        }
    }

    /**
     * Starts the node manager thread
     */
    public void start() {
        // Starting own threads, which cares about incoming node connections
        threadPool = Executors.newCachedThreadPool(new NamedThreadFactory(
            "Incoming-Connection-"));
        // Alternative:
        // threadPool = Executors.newFixedThreadPool(
        // Constants.MAX_INCOMING_CONNECTIONS, new NamedThreadFactory(
        // "Incoming-Connection-"));

        // load local nodes
        Thread nodefileLoader = new Thread("Nodefile loader") {
            public void run() {
                loadNodes();
                // Okay nodefile is loaded
                nodefileLoaded = true;
            }
        };
        nodefileLoader.start();
        if (!getController().isLanOnly()) {
            // we don't need supernodes if on lan
            // load (super) nodes from inet in own thread
            Thread inetNodeLoader = new Thread("Supernodes loader") {
                public void run() {
                    loadNodesFromInet();
                }
            };
            inetNodeLoader.start();
        }
        // Look for masternode
        Member masterNode = getMasterNode();
        if (masterNode != null) {
            log().info("My masternode is " + masterNode);
        }

        timer = new Timer("NodeManager timer for peridical tasks", true);
        setupPeridicalTasks();

        started = true;
        log().debug("Started");
    }

    /**
     * This starts the connecting process to other nodes, should be done, after
     * UI is opend.
     */
    public void startConnecting() {
        // Start reconnectors
        log().debug("Starting connection procees");
        buildReconnectionQueue();
        // Resize reconnector pool from time to time. First execution: NOW
        timer.schedule(new ReconnectorPoolResizer(), 0,
            Constants.RECONNECTOR_POOL_SIZE_RESIZE_TIME * 1000);
    }

    /**
     * Shuts the nodemanager down
     */
    public void shutdown() {
        // Remove listeners, not bothering them by boring shutdown events
        started = false;

        // Store the latest supernodes
        // Note: This call is here to save the nodes before shutting them down.
        storeOnlineSupernodes();

        // Stop threadpool
        if (threadPool != null) {
            log().debug("Shutting down incoming connection threadpool");
            threadPool.shutdown();
        }

        log().debug(
            "Shutting down " + acceptors.size()
                + " incoming connections (Acceptors)");
        List<Acceptor> tempList = new ArrayList<Acceptor>(acceptors);
        for (Acceptor acceptor : tempList) {
            acceptor.shutdown();
        }

        // Shutdown reconnectors
        synchronized (reconnectors) {
            log().debug(
                "Shutting down " + reconnectors.size() + " reconnectors");
            for (Iterator<Reconnector> it = reconnectors.iterator(); it
                .hasNext();)
            {
                Reconnector reconnector = it.next();
                reconnector.shutdown();
                it.remove();
            }
        }

        if (timer != null) {
            timer.cancel();
        }

        log().debug("Shutting down nodes");
        Member[] members = getNodes();
        log().debug("Shutting down " + members.length + " nodes");
        for (int i = 0; i < members.length; i++) {
            members[i].shutdown();
        }

        // first save current members connection state
        if (nodefileLoaded) {
            // Only store if was fully started
            storeNodes();
            // Shutdown, unloaded nodefile
            nodefileLoaded = false;
        }
        log().debug("Stopped");
    }

    /**
     * Set the handler that should be called when a member joins a folder. The
     * handler will generely ask if that member should become a friend
     * 
     * @param newAskForFriendshipHandler
     */
    public void setAskForFriendshipHandler(
        AskForFriendshipHandler newAskForFriendshipHandler)
    {
        askForFriendshipHandler = newAskForFriendshipHandler;
    }

    /**
     * Asks the user, if this member should be added to friendlist if not
     * already done. Won't ask if user has disabled this in
     * CONFIG_ASKFORFRIENDSHIP. displays in the userinterface the list of
     * folders that that member has joined.
     * 
     * @param member
     *            the node which joined the folders
     * @param joinedFolders
     *            the folders the member joined.
     */
    public void askForFriendship(Member member,
        HashSet<FolderInfo> joinedFolders)
    {
        if (askForFriendshipHandler != null) {
            askForFriendshipHandler.askForFriendship(new AskForFriendshipEvent(
                member, joinedFolders));
        }
    }

    /**
     * Asks the user, if this member should be added to friendlist if not
     * already done. Won't ask if user has disabled this in
     * CONFIG_ASKFORFRIENDSHIP. displays in the userinterface the list of
     * folders that that member has joined.
     * 
     * @param member
     *            the node which joined the folders
     */
    public void askForFriendship(Member member) {
        if (askForFriendshipHandler != null) {
            askForFriendshipHandler.askForFriendship(new AskForFriendshipEvent(
                member));
        }
    }

    /**
     * for debug
     * 
     * @param suspended
     */
    public void setSuspendFireEvents(boolean suspended) {
        ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        log().debug("setSuspendFireEvents: " + suspended);
    }

    /**
     * @return the number of nodes, which are online on the network.
     */
    public int countOnlineNodes() {
        int nConnected = 1;
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isConnected() || node.isConnectedToNetwork()) {
                    nConnected++;
                }
            }
        }
        return nConnected;
    }

    /**
     * @return the number of supernodes, which are online on the network.
     */
    public int countOnlineSupernodes() {
        int nConnected = 1;
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isSupernode()
                    && (node.isConnected() || node.isConnectedToNetwork()))
                {
                    nConnected++;
                }
            }
        }
        return nConnected;
    }

    /**
     * @return the number of known supernodes.
     */
    public int countSupernodes() {
        int nSupernodes = 0;
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isSupernode()) {
                    nSupernodes++;
                }
            }
        }
        return nSupernodes;
    }

    /**
     * @return the number of connected supernodes.
     */
    public int countConnectedSupernodes() {
        int nSupernodes = 0;
        synchronized (connectedNodes) {
            for (Member node : connectedNodes) {
                if (node.isSupernode()) {
                    nSupernodes++;
                }
            }
        }
        return nSupernodes;
    }

    /**
     * @return if we have reached the maximum number of connections allwed
     */
    public boolean maxConnectionsReached() {
        // Assume unlimited upload
        if (getController().getTransferManager().getAllowedUploadCPSForWAN() <= 0)
        {
            // Unlimited upload
            return false;
        }
        // log().warn("Max allowed: " +
        // getController().getTransferManager().getAllowedUploadCPS());
        double uploadKBs = ((double) getController().getTransferManager()
            .getAllowedUploadCPSForWAN()) / 1024;
        int nConnected = countConnectedNodes();
        int maxConnectionsAllowed = (int) (uploadKBs * Constants.MAX_NODES_CONNECTIONS_PER_KBS_UPLOAD);

        if (nConnected > maxConnectionsAllowed) {
            log().verbose(
                "Not more connection slots open. Used " + nConnected + "/"
                    + maxConnectionsAllowed);
        }

        // Still capable of new connections?
        return nConnected > maxConnectionsAllowed;
    }

    /**
     * @return the own identity, of course with no connection
     */
    public Member getMySelf() {
        return mySelf;
    }

    /**
     * @return the master node
     */
    public Member getMasterNode() {
        return getNode(ConfigurationEntry.MASTER_NODE_ID
            .getValue(getController()));
    }

    /**
     * @param member
     *            the member to check
     * @return if we know this member
     */
    public boolean knowsNode(Member member) {
        if (member == null) {
            return false;
        }
        // do i know him ?
        return knowsNode(member.getId());
    }

    /**
     * Returns true if the IP of the given member is within one of the
     * configured ranges Those are setup in advanced settings "LANlist".
     * 
     * @param adr
     *            the internet addedss
     * @return true if the member's ip is within one of the ranges
     */
    public boolean isNodeOnConfiguredLan(InetAddress adr) {
        for (AddressRange ar : lanRanges) {
            if (ar.contains((Inet4Address) adr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param member
     * @return if we know this member
     */
    public boolean knowsNode(MemberInfo member) {
        if (member == null) {
            return false;
        }
        return knowsNode(member.id);
    }

    /**
     * @return the number of connected nodes
     */
    public int countConnectedNodes() {
        return connectedNodes.size();
    }

    public List<Member> getConnectedNodes() {
        return new ArrayList<Member>(connectedNodes);
    }

    /**
     * @param id
     *            the id of the member
     * @return true if we know this member
     */
    public boolean knowsNode(String id) {
        if (id == null) {
            return false;
        }
        // do i know him ?
        return id.equals(mySelf.getId()) || knownNodes.containsKey(id);
    }

    /**
     * @param mInfo
     *            the memberinfo
     * @return the member for a memberinfo
     */
    public Member getNode(MemberInfo mInfo) {
        if (mInfo == null) {
            return null;
        }
        return getNode(mInfo.id);
    }

    /**
     * @param id
     * @return the member for this id
     */
    public Member getNode(String id) {
        if (id == null) {
            return null;
        }
        if (mySelf.getId().equals(id)) {
            return mySelf;
        }
        return knownNodes.get(id);
    }

    /**
     * @return all known nodes
     */
    public Member[] getNodes() {
        return knownNodes.values().toArray(new Member[0]);
    }

    /**
     * Gets the list of nodes, which have filelist for the given folder.
     * 
     * @param foInfo
     *            the folder to search for
     * @return the list of members, which have a filelist for the folder.
     */
    public List<Member> getNodeWithFileListFrom(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder is null");
        List<Member> found = new ArrayList<Member>();
        synchronized (knownNodes) {
            for (Member canidate : knownNodes.values()) {
                if (canidate.hasFileListFor(foInfo)) {
                    found.add(canidate);
                }
            }
        }
        return found;
    }

    public int countNodes() {
        return knownNodes.size();
    }

    /**
     * @return all valid nodes
     */
    public Member[] getValidNodes() {
        Member[] nodes = getNodes();
        // init with initial cap. to reduce growth problems
        List<Member> validNodes = new ArrayList<Member>(nodes.length);

        for (Member node : nodes) {
            if (node == null) {
                System.err.println("Node null");
            }
            if (!node.getInfo().isInvalid(getController())) {
                validNodes.add(node);
            }
        }

        nodes = new Member[validNodes.size()];
        validNodes.toArray(nodes);
        return nodes;
    }

    /**
     * Removes a member from the known list
     * 
     * @param node
     */
    private void removeNode(Member node) {
        log().warn("Removing " + node.getNick() + " from nodelist");
        // Shut down node
        node.shutdown();

        // removed from folders
        getController().getFolderRepository().removeFromAllFolders(node);
        knownNodes.remove(node.getId());

        // Remove all his listeners
        node.removeAllListeners();

        // Fire event
        fireNodeRemoved(node);
        log().debug("Node remove complete. " + node);
    }

    /**
     * Counts the number of friends that are online
     * 
     * @return the number of friends that are online
     */
    public int countOnlineFriends() {
        int nOnlineFriends = 0;
        for (Member friend : getFriends()) {
            if (friend.isConnectedToNetwork()) {
                nOnlineFriends++;
            }
        }
        return nOnlineFriends;
    }

    /**
     * @return the number of friends
     */
    public int countFriends() {
        return friends.size();
    }

    /**
     * @return the list of friends
     */
    public Member[] getFriends() {
        synchronized (friends) {
            Member[] friendsArr = new Member[friends.size()];
            friends.toArray(friendsArr);
            return friendsArr;
        }
    }

    /**
     * Called by member. Not getting this event from event handling because we
     * have to handle things definitivily before other elements work on that
     * 
     * @param node
     * @param friend
     */
    public void friendStateChanged(Member node, boolean friend) {
        if (node.isMySelf()) {
            // Ignore change on myself
            return;
        }

        if (friend) {
            // Mark node for immideate connection
            markNodeForImmediateReconnection(node);
            friends.add(node);
            fireFriendAdded(node);

            // Send a "you were added"
            getController().getTaskManager().scheduleTask(
                new SendMessageTask(new Notification(
                    Notification.Event.ADDED_TO_FRIENDS), node.getInfo()));
        } else {
            friends.remove(node);
            fireFriendRemoved(node);
        }

        if (nodefileLoaded) {
            // Only store after start
            // Store nodes
            storeNodes();

            // Broadcast single node list
            broadcastMessage(new KnownNodes(node.getInfo()));
        }
    }

    /**
     * Callback method from node to inform nodemanager about an online state
     * change
     * 
     * @param node
     */
    public void onlineStateChanged(Member node) {
        boolean nodeConnected = node.isCompleteyConnected();
        if (nodeConnected) {
            // Add to online nodes
            connectedNodes.add(node);
            // add to broadcastlist
            nodesWentOnline.add(node.getInfo());
        } else {
            // Node went offline. Break all downloads from him
            getController().getTransferManager().breakTransfers(node);
            if (node.hasJoinedAnyFolder()) {
                // Trigger filerequesting. we may want re-request files on a
                // folder he joined.
                getController().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
            }

            // Remove from list
            connectedNodes.remove(node);
            nodesWentOnline.remove(node.getInfo());
        }

        // Event handling
        if (nodeConnected) {
            fireNodeConnected(node);
        } else {
            fireNodeDisconnected(node);
        }
    }

    /**
     * Callback method from Member.
     * 
     * @param from
     * @param message
     */
    public void messageReceived(Member from, Message message) {
        valveMessageListenerSupport.fireMessage(from, message);
    }

    /**
     * Disconnects from all un-interesting nodes. Usful when switching from
     * public to private mode
     */
    public void disconnectUninterestingNodes() {
        Member[] nodes = getNodes();
        for (int i = 0; i < nodes.length; i++) {
            Member node = nodes[i];
            if (node.isCompleteyConnected() && !node.isInteresting()) {
                log().warn("Shutting down unintersting node " + node.getNick());
                node.shutdown();
            }
        }
    }

    /**
     * Processes a request for nodelist.
     * 
     * @param request
     *            the request.
     * @param from
     *            the origin of the request
     */
    public void receivedRequestNodeList(RequestNodeList request, Member from) {
        List<MemberInfo> list;
        synchronized (knownNodes) {
            list = request.filter(knownNodes.values());
        }
        from.sendMessagesAsynchron(KnownNodes.createKnownNodesList(list));
    }

    /**
     * Creates the default request for nodelist according to our own status. In
     * supernode mode we might want to request more node information that in
     * normal peer mode.
     * <p>
     * Attention: This method synchronizes on the internal friendlist. Avoid
     * holding a lock while calling this method.
     * 
     * @return the message.
     */
    public RequestNodeList createDefaultNodeListRequestMessage() {
        if (mySelf.isSupernode()) {
            return RequestNodeList.createRequestAllNodes();
        }
        synchronized (friends) {
            // TODO Change second paramter to RequestNodeList.NodesCriteria.NONE
            // when network folder list also covers private folders.
            return RequestNodeList.createRequest(friends,
                RequestNodeList.NodesCriteria.ONLINE,
                RequestNodeList.NodesCriteria.ONLINE);
        }
    }

    /**
     * Processes new node informations. Reconnects if required.
     * 
     * @param newNodes
     *            the new nodes to queue.
     */
    public void queueNewNodes(MemberInfo[] newNodes) {
        if (newNodes == null || newNodes.length == 0) {
            return;
        }

        // queue new members
        if (logVerbose) {
            log().verbose("Received new list of " + newNodes.length + " nodes");
        }

        int nNewNodes = 0;
        int nQueuedNodes = 0;

        for (int i = 0; i < newNodes.length; i++) {
            MemberInfo newNode = newNodes[i];

            // just check, for a faster shutdown
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (newNode == null || newNode.isInvalid(getController())) {
                // Member is too old, ignore
                if (logVerbose) {
                    log().verbose("Not adding new node: " + newNode);
                }
                continue;
            }
            Member thisNode = getNode(newNode);

            if (newNode.matches(mySelf)) {
                // ignore myself
                continue;
            } else if (getController().isLanOnly()
                && (newNode.getConnectAddress() != null)
                && (newNode.getConnectAddress().getAddress() != null)
                && !NetworkUtil.isOnLanOrLoopback(newNode.getConnectAddress()
                    .getAddress()))
            {
                // ignore if lan only mode && newNode not is onlan
                continue;
            } else if (thisNode == null) {
                // add node
                thisNode = addNode(newNode);
                nNewNodes++;
            } else {
                // update own information if more valueable
                thisNode.updateInfo(newNode);
            }

            if (newNode.isConnected) {
                // Node is connected to the network
                thisNode.setConnectedToNetwork(newNode.isConnected);
                if (shouldBeAddedToReconQueue(thisNode)) {
                    synchronized (reconnectionQueue) {
                        // Add node to reconnection queue
                        if (!reconnectionQueue.contains(thisNode)) {
                            reconnectionQueue.add(thisNode);
                            nQueuedNodes++;
                        }
                    }
                }
            }

        }

        if (nQueuedNodes > 0 || nNewNodes > 0) {
            if (nQueuedNodes > 0) {
                // Resort reconnection queue
                synchronized (reconnectionQueue) {
                    Collections.sort(reconnectionQueue,
                        MemberComparator.BY_RECONNECTION_PRIORITY);
                }

            }
            if (logVerbose) {
                log().verbose(
                    "Queued " + nQueuedNodes + " new nodes for reconnection, "
                        + nNewNodes + " added");
            }
        }
    }

    /**
     * Accpets a node, method does not block like
     * <code>acceptNode(Socket)</code>
     * 
     * @see #acceptNode(Socket)
     * @param socket
     */
    public void acceptNodeAsynchron(Socket socket) {
        // Create acceptor on socket

        if (!started) {
            log().warn(
                "Not accepting node from " + socket
                    + ". NodeManager is not started");
            try {
                socket.close();
            } catch (IOException e) {
                log().verbose("Unable to close incoming connection", e);
            }
            return;
        }

        if (logVerbose) {
            log().verbose("Connection queued for acception: " + socket + "");
        }
        Acceptor acceptor = new Acceptor(socket);

        // Enqueue for later processing
        acceptors.add(acceptor);
        threadPool.submit(acceptor);

        if (acceptors.size() > Constants.MAX_INCOMING_CONNECTIONS) {
            // Show warning
            log().warn(
                "Processing too much incoming connections (" + acceptors.size()
                    + "), throttled");
            try {
                Thread.sleep(acceptors.size() * getController().getWaitTime()
                    / 10);
            } catch (InterruptedException e) {
                log().verbose(e);
            }
        }
    }

    /**
     * Main method for a new member connection. Connection will be validated
     * against own member database. Duplicate connections will be dropped.
     * 
     * @param socket
     * @throws ConnectionException
     */
    public void acceptNode(Socket socket) throws ConnectionException {
        if (logVerbose) {
            log().verbose("Accepting member on socket: " + socket);
        }

        if (!started) {
            try {
                log().warn(
                    "NodeManager already shut down. Not accepting any more nodes. Closing socket "
                        + socket);
                socket.close();
            } catch (IOException e) {
                throw new ConnectionException("Unable to close socket", e);
            }
            return;
        }

        // Build handler around socket, will do handshake
        if (logVerbose) {
            log().verbose("Initalizing connection handler to " + socket);
        }

        ConnectionHandler handler = null;
        try {
            handler = getController().getIOProvider()
                .getConnectionHandlerFactory().createSocketConnectionHandler(
                    getController(), socket);
        } catch (ConnectionException e) {
            if (handler != null) {
                handler.shutdown();
            }
            throw e;
        }

        if (logVerbose) {
            log().verbose("Connection handler ready " + handler);
        }

        // Accept node
        acceptNode(handler);
    }

    /**
     * Internal method for accepting nodes on a connection handler
     * 
     * @param handler
     * @throws ConnectionException
     */
    private void acceptNode(ConnectionHandler handler)
        throws ConnectionException
    {
        if (!started) {
            log().warn(
                "Not accepting node from " + handler
                    + ". NodeManager is not started");
            handler.shutdown();
            return;
        }

        // Accepts a node from a connection handler
        Identity remoteIdentity = handler.getIdentity();

        // check for valid identity
        if (remoteIdentity == null || !remoteIdentity.isValid()) {
            log().warn(
                "Received an illegal identity from " + handler
                    + ". disconnecting. " + remoteIdentity);
            handler.shutdown();
            return;
        }

        if (getMySelf().getInfo().equals(remoteIdentity.getMemberInfo())) {
            log().warn(
                "Loopback connection detected to " + handler
                    + ", disconnecting");
            handler.shutdown();
            return;
        }

        Member member;
        // Accept node ?
        boolean acceptHandler;
        String rejectCause = null;

        // Accept only one node at a time
        synchronized (acceptLock) {
            if (logVerbose) {
                log().verbose(
                    "Accept lock taken. Member: "
                        + remoteIdentity.getMemberInfo() + ", Handler: "
                        + handler);
            }
            // Is this member already known to us ?
            member = getNode(remoteIdentity.getMemberInfo());

            if (member == null) {
                // Create new node
                member = new Member(getController(), handler.getIdentity()
                    .getMemberInfo());

                // add node
                addNode(member);

                // Accept handler
                acceptHandler = true;
            } else {
                // Old member, check its connection state
                if (!member.isOnLAN() && handler.isOnLAN()) {
                    // Only accept hanlder, if our one is disco! or our is not
                    // on LAN
                    acceptHandler = true;
                } else if (member.isConnected()) {
                    rejectCause = "Duplicate connection detected to " + member
                        + ", disconnecting";
                    // Not accept node
                    acceptHandler = false;
                } else {
                    // Otherwise accept. (our member = disco)
                    acceptHandler = true;
                }
            }
            if (logVerbose) {
                log().verbose(
                    "Accept lock released. Member: "
                        + remoteIdentity.getMemberInfo() + ", Handler: "
                        + handler);
            }
        }

        if (acceptHandler) {
            if (member.isConnected()) {
                log()
                    .warn("Taking a better conHandler for " + member.getNick());
            }
            // Complete handshake
            member.setPeer(handler);
            member.completeHandshake();
        } else {
            log().warn(rejectCause);
            // Tell remote side, fatal problem
            handler.sendMessagesAsynchron(new Problem(rejectCause, true));
            handler.shutdown();
        }
    }

    /**
     * Adds a new node to the nodemanager. Initalize from memberinfo
     * 
     * @param newNode
     * @return the new node
     */
    public Member addNode(MemberInfo newNode) {
        Member member = new Member(getController(), newNode);
        addNode(member);
        return member;
    }

    /**
     * Adds a new node to the nodemanger
     * 
     * @param node
     *            the new node
     */
    private void addNode(Member node) {
        if (node == null) {
            throw new NullPointerException("Node is null");
        }
        // log().verbose("Adding new node: " + node);

        Member oldNode = knownNodes.get(node.getId());
        if (oldNode != null) {
            log().warn("Overwriting old node: " + node);
            removeNode(oldNode);
        }

        knownNodes.put(node.getId(), node);

        // Fire new node event
        fireNodeAdded(node);
    }

    /**
     * Broadcasts a message to all nodes, does not block. Message enqueued to be
     * sent asynchron
     * 
     * @param message
     */
    public void broadcastMessage(Message message) {
        if (logVerbose) {
            log().verbose("Broadcasting message: " + message);
        }
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isCompleteyConnected()) {
                    // Only broadcast after completely connected
                    node.sendMessageAsynchron(message, null);
                }
            }
        }
    }

    /**
     * Broadcasts a message along a number of supernodes
     * 
     * @param message
     *            the message to broadcast
     * @param nSupernodes
     *            the maximum numbers to supernodes to send the message to. 0 or
     *            lower means to all supernodes
     * @return the number of nodes where the message has been broadcasted
     */
    public int broadcastMessageToSupernodes(Message message, int nSupernodes) {
        if (logVerbose) {
            log().verbose("Broadcasting message to supernodes: " + message);
        }
        int nNodes = 0;
        List<Member> supernodes = new LinkedList<Member>();
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isCompleteyConnected() && node.isSupernode()) {
                    // Only broadcast after completely connected
                    supernodes.add(node);
                }
            }
        }
        if (nSupernodes <= 0) {
            // Broadcast to all supernodes
            nSupernodes = supernodes.size();
        }

        nSupernodes = Math.min(supernodes.size(), nSupernodes);
        for (int i = 0; i < nSupernodes; i++) {
            // Take a random supernode
            int index = (int) (Math.random() * supernodes.size());
            Member supernode = supernodes.get(index);
            supernodes.remove(index);
            log().debug(
                "Sending message to supernode: " + supernode.getNick() + ". "
                    + message);
            supernode.sendMessageAsynchron(message, null);
            nNodes++;
        }

        return nNodes;
    }

    /**
     * Broadcasts a message along a number of nodes on lan
     * 
     * @param message
     *            the message to broadcast
     * @param nBroadcasted
     *            the maximum numbers of lan nodes to send the message to. 0 or
     *            lower means to all nodes on lan
     * @return the number of nodes where the message has been broadcasted
     */
    public int broadcastMessageLANNodes(Message message, int nBroadcasted) {
        if (logVerbose) {
            log().verbose("Broadcasting message to LAN nodes: " + message);
        }
        int nNodes = 0;
        List<Member> lanNodes = new LinkedList<Member>();
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                if (node.isCompleteyConnected() && node.isOnLAN()) {
                    // Only broadcast after completely connected
                    lanNodes.add(node);
                }
            }
        }
        if (nBroadcasted <= 0) {
            // Broadcast to all supernodes
            nBroadcasted = lanNodes.size();
        }

        nBroadcasted = Math.min(lanNodes.size(), nBroadcasted);
        for (int i = 0; i < nBroadcasted; i++) {
            // Take a random supernode
            int index = (int) (Math.random() * lanNodes.size());
            Member node = lanNodes.get(index);
            lanNodes.remove(index);
            log().debug(
                "Sending message to lan node: " + node.getNick() + ". "
                    + message);
            node.sendMessageAsynchron(message, null);
            nNodes++;
        }

        return nNodes;
    }

    /**
     * Loads members from disk and adds them
     * 
     * @param nodeList
     */
    private boolean loadNodesFrom(String filename) {
        File nodesFile = new File(Controller.getMiscFilesLocation(), filename);
        if (!nodesFile.exists()) {
            // Try harder in local base
            nodesFile = new File(filename);
        }

        if (!nodesFile.exists()) {
            log().debug(
                "Unable to load nodes, file not found "
                    + nodesFile.getAbsolutePath());
            return false;
        }

        try {
            NodeList nodeList = new NodeList();
            nodeList.load(nodesFile);

            log().info(
                "Loaded " + nodeList.getNodeList().size() + " nodes from "
                    + nodesFile.getAbsolutePath());
            queueNewNodes(nodeList.getNodeList().toArray(new MemberInfo[0]));

            for (MemberInfo friend : nodeList.getFriendsSet()) {
                Member node = friend.getNode(getController(), true);
                node.setFriend(true);
                if (!this.friends.contains(node) && !node.isMySelf()) {
                    this.friends.add(node);
                }
            }
            return !nodeList.getNodeList().isEmpty();
        } catch (IOException e) {
            log().warn(
                "Unable to load nodes from file '" + filename + "'. "
                    + e.getMessage());
            log().verbose(e);
        } catch (ClassCastException e) {
            log().warn(
                "Illegal format of supernodes files '" + filename
                    + "', deleted");
            log().verbose(e);
            nodesFile.delete();
        } catch (ClassNotFoundException e) {
            log().warn(
                "Illegal format of supernodes files '" + filename
                    + "', deleted");
            log().verbose(e);
            nodesFile.delete();
        }
        return false;
    }

    /**
     * Loads members from disk and connects to them
     */
    private void loadNodes() {
        String filename = getController().getConfigName() + ".nodes";
        if (!loadNodesFrom(filename)) {
            filename += ".backup";
            log().debug(
                "Failed to load nodes, trying backup nodefile '" + filename
                    + "'");
            if (!loadNodesFrom(filename)) {
                return;
            }
        }
    }

    /**
     * Saves the state of current members/connections. members will get
     * reconnected at start
     */
    private void storeNodes() {
        Collection<MemberInfo> allNodesInfos = Convert.asMemberInfos(knownNodes
            .values());
        allNodesInfos.add(getMySelf().getInfo());
        // Add myself to know nodes
        Collection<MemberInfo> friendInfos = Convert.asMemberInfos(friends);
        NodeList nodeList = new NodeList(allNodesInfos, friendInfos);

        storeNodes0(getController().getConfigName() + ".nodes", nodeList);
        storeNodes0(getController().getConfigName() + ".nodes.backup", nodeList);
    }

    /**
     * Stores the supernodes that are currently online in a separate file.
     */
    private void storeOnlineSupernodes() {
        Member[] nodes = getNodes();
        Collection<MemberInfo> latestSupernodesInfos = new ArrayList<MemberInfo>();
        Collection<Member> latestSupernodes = new ArrayList<Member>();
        for (Member node : nodes) {
            if (!node.isSupernode()) {
                // Skip non-supernode
                continue;
            }
            if (!node.isConnectedToNetwork()) {
                continue;
            }
            latestSupernodesInfos.add(node.getInfo());
            latestSupernodes.add(node);
        }
        if (getMySelf().isSupernode()) {
            latestSupernodesInfos.add(getMySelf().getInfo());
            latestSupernodes.add(getMySelf());
        }
        if (getController().isVerbose()) {
            Debug.writeNodeListCSV(latestSupernodes, "SupernodesOnline.csv");
        }
        NodeList nodeList = new NodeList(latestSupernodesInfos, null);
        storeNodes0(getController().getConfigName() + "-Supernodes.nodes",
            nodeList);
    }

    /**
     * Internal method for storing nodes into a files
     * <p>
     */
    private void storeNodes0(String filename, NodeList nodeList) {
        File nodesFile = new File(Controller.getMiscFilesLocation(), filename);
        if (!nodesFile.getParentFile().exists()) {
            // for testing this directory needs to be created because we have
            // subs in the config name
            nodesFile.getParentFile().mkdirs();
        }

        if (nodeList.getNodeList().isEmpty()) {
            log().debug("Not storing list of nodes, none known");
            return;
        }

        log().debug(
            "Saving known nodes/friends with " + nodeList.getNodeList().size()
                + " nodes to " + filename);
        try {
            nodeList.save(nodesFile);
        } catch (IOException e) {
            log().warn(
                "Unable to write supernodes to file '" + filename + "'. "
                    + e.getMessage());
            log().verbose(e);
        }
    }

    /**
     * Loads supernodes from inet and connects to them
     */
    private void loadNodesFromInet() {
        log().info("Loading nodes from inet: " + NODES_URL);
        URL url;
        try {
            url = new URL(NODES_URL);
        } catch (MalformedURLException e) {
            log().verbose(e);
            return;
        }

        NodeList inetNodes = new NodeList();
        try {
            inetNodes.load(url);
            List<MemberInfo> supernodes = inetNodes.getNodeList();

            // Sort by connet time
            Collections.sort(supernodes, MemberComparator.BY_LAST_CONNECT_DATE);

            for (Iterator<MemberInfo> it = supernodes.iterator(); it.hasNext();)
            {

                MemberInfo node = it.next();
                if (knowsNode(node) || !node.isSupernode) {
                    it.remove();
                } else {
                    if (logVerbose) {
                        log().verbose(
                            node.toString() + " ,last connect: "
                                + node.lastConnectTime);
                    }

                    // If supernode is outdated, fix date
                    if (node.lastConnectTime == null
                        || node.lastConnectTime.getTime() < (System
                            .currentTimeMillis() - Constants.MAX_NODE_OFFLINE_TIME))
                    {
                        log().verbose(
                            "Fixed date of internet supernode list " + node);
                        // Give supernode date (<20 days)
                        node.lastConnectTime = new Date(System
                            .currentTimeMillis()
                            - Constants.MAX_NODE_OFFLINE_TIME
                            + 1000
                            * 60
                            * 60
                            * 4);
                    }
                }
            }

            log().info(
                "Loaded " + supernodes.size() + " new supernodes from "
                    + NODES_URL);

            MemberInfo[] supernodesArr = new MemberInfo[supernodes.size()];
            supernodes.toArray(supernodesArr);
            queueNewNodes(supernodesArr);
        } catch (IOException e) {
            log().warn("Unable to read supernodes files from " + NODES_URL, e);
        } catch (ClassCastException e) {
            log().warn("Illegal format of supernodes files on " + NODES_URL, e);
        } catch (ClassNotFoundException e) {
            log().warn("Illegal format of supernodes files on " + NODES_URL, e);
        }
    }

    /**
     * Marks a node for immediate reconnection. Actually puts it in front of
     * reconnection queue
     * 
     * @param node
     * @return true if node was put in front of reconnection line
     */
    public boolean markNodeForImmediateReconnection(Member node) {
        if (!shouldBeAddedToReconQueue(node)) {
            return false;
        }
        if (logVerbose) {
            log().verbose("Marking node for immediate reconnect: " + node);
        }
        synchronized (reconnectionQueue) {
            // Remove node
            reconnectionQueue.remove(node);
            // Add at start
            reconnectionQueue.add(0, node);
        }

        return true;
    }

    /**
     * Freshly refills the reconnection queue. The nodes contained are tried to
     * reconnected. Also removes unused nodes
     */
    private void buildReconnectionQueue() {
        // Process only valid nodes
        Member[] nodes = getValidNodes();
        int nBefore = reconnectionQueue.size();
        synchronized (reconnectionQueue) {
            reconnectionQueue.clear();

            for (int i = 0; i < nodes.length; i++) {
                Member node = nodes[i];
                if (shouldBeAddedToReconQueue(node)) {
                    reconnectionQueue.add(node);
                }
            }

            // Lately connect first
            Collections.sort(reconnectionQueue,
                MemberComparator.BY_RECONNECTION_PRIORITY);

            if (logVerbose) {
                log().verbose(
                    "Freshly filled reconnection queue with "
                        + reconnectionQueue.size() + " nodes, " + nBefore
                        + " were in queue before");
            }

            if (getController().isVerbose()) {
                Debug.writeNodeListCSV(reconnectionQueue,
                    "ReconnectionQueue.csv");
            }

            if (reconnectionQueue.size() > 200) {
                log().warn("Reconnection queue contains more than 200 nodes");
            }

            // Notify threads
            if (!reconnectionQueue.isEmpty()) {
                reconnectionQueue.notify();
            }
        }
    }

    private boolean shouldBeAddedToReconQueue(Member node) {
        Reject.ifNull(node, "Node is null");

        if (node.getReconnectAddress() == null
            || node.getReconnectAddress().getAddress() == null)
        {
            // Pretty basic illegal/useless.
            return false;
        }
        if (node.isConnected() || node.isMySelf()) {
            // not process already connected nodes
            return false;
        }
        if (node.isReconnecting()) {
            return false;
        }
        if (!node.isInteresting()) {
            return false;
        }
        if (node.receivedWrongIdentity()) {
            return false;
        }

        // Offline limit time, all nodes before this time are not getting
        // reconnected
        Date offlineLimitTime = new Date(System.currentTimeMillis()
            - Constants.MAX_NODE_OFFLINE_TIME);

        // Check if node was offline too long
        Date lastConnectTime = node.getLastNetworkConnectTime();
        boolean offlineTooLong = true;

        offlineTooLong = lastConnectTime != null ? lastConnectTime
            .before(offlineLimitTime) : true;

        // Always add friends
        // Add supernodes only if not offline too long
        // Other nodes only if no wrong identity received and not
        // offline too long
        if (node.isFriend()) {
            // Always try to connect to friends
            return true;
        }
        if (offlineTooLong) {
            return false;
        }
        if (node.isSupernode()) {
            // Connect to supernodes that are not offline too long
            return true;
        }
        if (node.isDontConnect()) {
            // Last resort: This is a usual node, very uninteresting.
            // Don't connect if we already did and he rejected us.
            return false;
        }

        // Lets try it when we are supernode.
        return getMySelf().isSupernode();
    }

    // Message listener code **************************************************

    public void addMessageListenerToAllNodes(MessageListener listener) {
        valveMessageListenerSupport.addMessageListener(listener);
    }

    public void addMessageListenerToAllNodes(Class messageType,
        MessageListener listener)
    {
        valveMessageListenerSupport.addMessageListener(messageType, listener);
    }

    public void removeMessageListener(MessageListener listener) {
        valveMessageListenerSupport.removeMessageListener(listener);
    }

    // Internal classes *******************************************************

    /**
     * Processor for one incoming connection on a socket
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class Acceptor implements Runnable {
        private Socket socket;
        private Date startTime;

        private Acceptor(Socket aSocket) {
            Reject.ifNull(aSocket, "Socket is null");
            socket = aSocket;
        }

        /**
         * Shuts the acceptor down and closes the socket
         */
        private void shutdown() {
            try {
                socket.close();
            } catch (IOException e) {
                log().verbose("Unable to close socket from acceptor", e);
            }

            // Remove from acceptors list
            acceptors.remove(this);
        }

        /**
         * @return if this acceptor has a timeout
         */
        private boolean hasTimeout() {
            if (startTime == null) {
                // Not started yet
                return false;
            }
            return (System.currentTimeMillis() > startTime.getTime()
                + (Constants.INCOMING_CONNECTION_TIMEOUT * 1000));
        }

        public void run() {
            try {
                startTime = new Date();
                log().verbose(
                    "Accepting connection from: " + socket.getInetAddress()
                        + ":" + socket.getPort());
                acceptNode(socket);
            } catch (ConnectionException e) {
                log().verbose("Unable to connect to " + socket, e);
                shutdown();
            } finally {
                // Remove from acceptors list
                acceptors.remove(this);
            }
            long took = System.currentTimeMillis() - startTime.getTime();
            if (logEnabled) {
                log().verbose(
                    "Acceptor finished to " + socket + ", took " + took + "ms");
            }
        }

        public String toString() {
            return "Acceptor for " + socket;
        }
    }

    /**
     * Reconnector thread. Periodically tries to reconnect to nodes in the
     * reconnection queue. Automatically starts new child reconnector if work is
     * getting to hard (too much nodes)
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class Reconnector extends Thread {
        private boolean reconStarted;
        private Member currentNode;

        private Reconnector() {
            super("Reconnector " + ++reconnectorCounter);
        }

        public void start() {
            if (!started) {
                throw new IllegalStateException(
                    "Unable to start reconnector. NodeManager not started");
            }
            super.start();
            reconStarted = true;
        }

        public void shutdown() {
            reconStarted = false;
            reconnectorCounter--;
            interrupt();
            if (currentNode != null) {
                currentNode.shutdown();
            }
        }

        public void run() {
            if (logVerbose) {
                log().verbose("Starting reconnector: " + getName());
            }
            long waitTime = Constants.SOCKET_CONNECT_TIMEOUT / 2;

            while (this.reconStarted) {
                synchronized (reconnectionQueue) {
                    if (!started) {
                        log()
                            .warn("Stopping " + this + ". NodeManager is down");
                        break;
                    }
                    if (reconnectionQueue.isEmpty()) {
                        // Rebuilds reconnection queue if required
                        buildReconnectionQueue();
                        // Check if we need more reconnectors
                    }

                    if (!reconnectionQueue.isEmpty()) {
                        // Take the first node out of the reconnection queue

                        currentNode = reconnectionQueue.remove(0);
                        if (currentNode.isConnected()
                            || currentNode.isReconnecting())
                        {
                            // Already reconnecting, now reconnect to node
                            if (logVerbose) {
                                log().verbose(
                                    "Not reconnecting to "
                                        + currentNode.getNick()
                                        + ", already reconnecting/connected");
                            }
                            currentNode = null;
                        }
                    }
                }

                // log().warn(this + ": Still active");

                if (currentNode != null) {
                    if (currentNode.isConnected()) {
                        // Node is connected, skip
                        continue;
                    }

                    // A node could be obtained from the reconnection queue, try
                    // to connect now
                    long start = System.currentTimeMillis();
                    try {
                        // Reconnect
                        currentNode.reconnect();
                    } catch (InvalidIdentityException e) {
                        log().warn(
                            "Invalid identity from " + currentNode
                                + ". Triing to connect to IP", e);

                        boolean connectToIP = true;

                        if (e.getFrom().getIdentity() != null
                            && e.getFrom().getIdentity().getMemberInfo() != null
                            && e.getFrom().getIdentity().getMemberInfo()
                                .getNode(getController()).isConnected())
                        {
                            // We are already connected to that node!
                            // Not connect to ip
                            connectToIP = false;
                            log()
                                .warn(
                                    "Already connected to "
                                        + e.getFrom().getIdentity()
                                            .getMemberInfo().nick
                                        + " not connecting to ip");
                        }

                        if (connectToIP) {
                            // Ok connect to that ip, there is a powerfolder
                            // node
                            try {
                                getController().connect(
                                    e.getFrom().getRemoteAddress());
                            } catch (ConnectionException e1) {
                                log().verbose(e1);
                            }
                        }
                    }
                    long reconnectTook = System.currentTimeMillis() - start;
                    long waitUntilNextTry = Constants.SOCKET_CONNECT_TIMEOUT
                        / 2 - reconnectTook;
                    if (waitUntilNextTry > 0) {
                        try {
                            if (logVerbose) {
                                log().verbose(
                                    this + ": Going on idle for "
                                        + waitUntilNextTry + "ms");
                            }
                            Thread.sleep(waitUntilNextTry);
                        } catch (InterruptedException e) {
                            log().verbose(this + " interrupted, breaking");
                            break;
                        }
                    }
                } else {
                    if (logVerbose) {
                        log().verbose(this + " is on idle");
                    }
                    // Otherwise wait a bit
                    try {
                        Waiter waiter = new Waiter(waitTime);
                        while (!waiter.isTimeout()
                            && reconnectionQueue.isEmpty())
                        {
                            waiter.waitABit();
                        }
                    } catch (RuntimeException e) {
                        log().debug(this + " Stopping. cause: " + e.toString());
                        break;
                    }

                }
            }
        }

        public String toString() {
            return getName();
        }
    }

    /**
     * Sets up all tasks, that needs to be periodically executed.
     * <p>
     * One task is NOT setup here: The <code>ReconectorPoolResizer</code>.
     * That is done in <code>#startConnection()</code>.
     * 
     * @see #startConnecting()
     */
    private void setupPeridicalTasks() {
        Reject.ifNull(timer, "Timer is null to setup periodical tasks");
        // Broadcast transfer status
        timer.schedule(new TransferStatusBroadcaster(),
            Constants.TRANSFER_STATUS_BROADCAST_INTERVAL * 1000 / 2,
            Constants.TRANSFER_STATUS_BROADCAST_INTERVAL * 1000);
        // Request network folder list
        // timer.schedule(new NetworkFolderListRequestor(),
        // Constants.NETWORK_FOLDER_LIST_REQUEST_INTERVAL * 1000 / 2,
        // Constants.NETWORK_FOLDER_LIST_REQUEST_INTERVAL * 1000);
        // Request new node list from time to time
        timer.schedule(new NodeListRequestor(),
            Constants.NODE_LIST_REQUEST_INTERVAL * 1000 / 2,
            Constants.NODE_LIST_REQUEST_INTERVAL * 1000);
        // Broadcast the nodes that went online
        timer.schedule(new NodesThatWentOnlineListBroadcaster(),
            Constants.NODES_THAN_WENT_ONLINE_BROADCAST_TIME * 1000 / 2,
            Constants.NODES_THAN_WENT_ONLINE_BROADCAST_TIME * 1000);
        // Check incoming connection tries
        timer.schedule(new IncomingConnectionChecker(), 0,
            Constants.INCOMING_CONNECTION_CHECK_TIME * 1000);

        // Write statistics and other infos.
        if (getController().isVerbose()) {
            timer.schedule(new StatisticsWriter(), 59 * 1000, 60 * 1000);
        }
    }

    // Workers ****************************************************************

    /**
     * Broadcasts the transferstatus
     */
    private class TransferStatusBroadcaster extends TimerTask {
        @Override
        public void run()
        {
            // Broadcast new transfer status
            TransferStatus status = getController().getTransferManager()
                .getStatus();
            if (logVerbose) {
                log().verbose("Broadcasting transfer status: " + status);
            }
            broadcastMessage(status);
        }
    }

    /**
     * Broadcasts all nodes, that went online since the last execution.
     */
    private class NodesThatWentOnlineListBroadcaster extends TimerTask {
        @Override
        public void run()
        {
            if (nodesWentOnline.isEmpty()) {
                return;
            }
            log().debug(
                "Broadcasting " + nodesWentOnline.size()
                    + " nodes that went online");
            KnownNodes nodesWentOnlineMessage;
            synchronized (nodesWentOnline) {
                MemberInfo[] nodes = new MemberInfo[nodesWentOnline.size()];
                nodesWentOnline.toArray(nodes);
                nodesWentOnlineMessage = new KnownNodes(nodes);
                nodesWentOnline.clear();
            }
            broadcastMessage(nodesWentOnlineMessage);
        }
    }

    /**
     * Checks the currently atempted connection tries for timeouts.
     */
    private class IncomingConnectionChecker extends TimerTask {
        @Override
        public void run()
        {
            List<Acceptor> tempList = new ArrayList<Acceptor>(acceptors);
            log().debug(
                "Checking incoming connection queue (" + tempList.size() + ")");
            if (tempList.size() > Constants.MAX_INCOMING_CONNECTIONS) {
                log().warn(
                    "Processing too much incoming connections ("
                        + tempList.size() + ")");
            }
            for (Acceptor acceptor : tempList) {
                if (acceptor.hasTimeout()) {
                    log().warn("Acceptor has timeout: " + acceptor);
                    acceptor.shutdown();
                }
            }
        }
    }

    /**
     * Resizes the pool of active reconnectors
     */
    private class ReconnectorPoolResizer extends TimerTask {
        @Override
        public void run()
        {
            synchronized (reconnectors) {
                // Remove dead reconnectors.
                for (Iterator<Reconnector> it = reconnectors.iterator(); it
                    .hasNext();)
                {
                    Reconnector reconnector = it.next();
                    if (!reconnector.isAlive() || reconnector.isInterrupted()) {
                        it.remove();
                    }
                }

                // Now do the actual resizing.
                int nReconnector = reconnectors.size();

                // Calculate required reconnectors. check min / max number.
                int reqReconnectors = Math.max(
                    Constants.MIN_NUMBER_RECONNECTORS, Math.min(
                        Constants.MAX_NUMBER_RECONNECTORS, (reconnectionQueue
                            .size() / 4)));

                int reconDiffer = reqReconnectors - nReconnector;

                if (logVerbose) {
                    log().verbose(
                        "Got " + reconnectionQueue.size()
                            + " nodes queued for reconnection");
                }

                if (reconDiffer > 0) {
                    // We have to less reconnectors, spawning one...

                    for (int i = 0; i < reconDiffer; i++) {
                        Reconnector reconnector = new Reconnector();
                        // add reconnector to nodemanager
                        reconnectors.add(reconnector);
                        // and start
                        reconnector.start();
                    }

                    log().debug(
                        "Spawned " + reconDiffer + " reconnectors. "
                            + reconnectors.size() + "/" + reqReconnectors
                            + ", nodes in reconnection queue: "
                            + reconnectionQueue.size());
                } else if (reconDiffer < 0) {
                    log().debug(
                        "Killing " + -reconDiffer
                            + " Reconnectors. Currently have: " + nReconnector
                            + " Reconnectors");
                    for (int i = 0; i < -reconDiffer; i++) {
                        // Kill one reconnector
                        if (reconnectors.size() <= 1) {
                            log().warn("Not killing last reconnector");
                            // Have at least one reconnector
                            break;
                        }
                        Reconnector reconnector = reconnectors.remove(0);
                        if (reconnector != null) {
                            log().verbose("Killing reconnector " + reconnector);
                            reconnector.shutdown();
                        }
                    }
                }
            }
        }
    }

    /**
     * Requests the required nodelist.
     */
    private class NodeListRequestor extends TimerTask {
        @Override
        public void run()
        {
            // Request new nodelist from supernodes
            RequestNodeList request = createDefaultNodeListRequestMessage();
            if (logEnabled) {
                log().debug("Requesting nodelist: " + request);
            }
            broadcastMessageToSupernodes(request,
                Constants.N_SUPERNODES_TO_CONTACT_FOR_NODE_LIST);
        }
    }

    /**
     * Writes the statistic to disk.
     */
    private class StatisticsWriter extends TimerTask {
        @Override
        public void run()
        {
            Debug.writeStatistics(getController());
            Debug.writeNodeListCSV(reconnectionQueue, "ReconnectionQueue.csv");
        }
    }

    // Listener support *******************************************************

    public void addNodeManagerListener(NodeManagerListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeNodeManagerListener(NodeManagerListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    // Helper *****************************************************************

    private void fireNodeRemoved(Member node) {
        listenerSupport.nodeRemoved(new NodeManagerEvent(this, node));

    }

    private void fireNodeAdded(final Member node) {
        listenerSupport.nodeAdded(new NodeManagerEvent(this, node));
    }

    private void fireNodeConnected(final Member node) {
        listenerSupport.nodeConnected(new NodeManagerEvent(this, node));
    }

    private void fireNodeDisconnected(final Member node) {
        listenerSupport.nodeDisconnected(new NodeManagerEvent(this, node));
    }

    private void fireFriendAdded(final Member node) {
        listenerSupport.friendAdded(new NodeManagerEvent(this, node));
    }

    private void fireFriendRemoved(final Member node) {
        listenerSupport.friendRemoved(new NodeManagerEvent(this, node));
    }

    public void fireNodeSettingsChanged(final Member node) {
        listenerSupport.settingsChanged(new NodeManagerEvent(this, node));
    }
}