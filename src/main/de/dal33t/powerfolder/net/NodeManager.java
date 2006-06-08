/* $Id: NodeManager.java,v 1.123 2006/04/23 18:31:14 totmacherr Exp $
 */
package de.dal33t.powerfolder.net;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.prefs.Preferences;

import org.apache.commons.threadpool.DefaultThreadPool;
import org.apache.commons.threadpool.ThreadPoolMonitor;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;

/**
 * Managing class which takes care about all old and new nodes. reconnects those
 * who disconnected and connectes to new ones
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.123 $
 */
public class NodeManager extends PFComponent {
    // the number of seconds (aprox) of delay till the connection is tested and
    // a warning may be displayed
    private static final int TEST_CONNECTIVITY_DELAY = 300;

    // the pref name that holds a boolean value if the connection should be
    // tested and a warning displayed if no incomming connections
    private static final String PREF_NAME_TEST_CONNECTIVITY = "test_for_connectivity";

    // The central inet nodes file
    private static final String NODES_URL = "http://nodes.powerfolder.com/PowerFolder.nodes";

    private Thread myThread;
    private Thread workerThread;
    private DefaultThreadPool threadPool;

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
    private List<Member> onlineNodes;
    
    private Member mySelf;
    /**
     * Set containing all nodes, that went online in the meanwhile (since last
     * broadcast)
     */
    private Set<MemberInfo> nodesWentOnline;

    // Message listener which are fired for every member
    private MessageListenerSupport valveMessageListenerSupport;
    // Internal message listner which is will be linked to all nodes and
    // broadcasts to 'our' message listener
    private MessageListener valveMessageListener;

    private boolean started;
    private boolean nodefileLoaded;

    

    // private NodeManagerListener listenerSupport;
    private List<NodeManagerListener> listeners;

    public NodeManager(Controller controller) {
        super(controller);

        
        started = false;
        nodefileLoaded = false;
        // initzialize myself if available in config
        String nick = getController().getConfig().getProperty("nick");

        if (controller.getCommandLine() != null
            && controller.getCommandLine().hasOption("n"))
        {
            // Take nick from command line
            nick = controller.getCommandLine().getOptionValue("n");
        }
        if (nick == null) {
            nick = System.getProperty("user.name");
            log().warn("Nick not set, use nick=<yournick> in config file");
        }
        String idKey = "PowerFolder.nodeId";
        // check for manual id
        String id = getController().getConfig().getProperty("nodeid");
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
        knownNodes = Collections.synchronizedMap(new HashMap<String, Member>());

        friends = Collections.synchronizedList(new ArrayList<Member>());
        onlineNodes = Collections.synchronizedList(new ArrayList<Member>());

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
        // Basically broadcasts all incoming messages from nodes to all
        // messagelisters
        valveMessageListener = new MessageListener() {
            public void handleMessage(Member source, Message message) {
                // Fire message to listeners
                valveMessageListenerSupport.fireMessage(source, message);
            }
        };
        getMySelf().addMessageListener(valveMessageListener);
        listeners = Collections
            .synchronizedList(new ArrayList<NodeManagerListener>());
        // this.listenerSupport = (NodeManagerListener) ListenerSupportFactory
        // .createListenerSupport(NodeManagerListener.class);
        
        //listen to Controller for networking mode change
        getController().addPropertyChangeListener(Controller.NETWORKING_MODE_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                shutdown();
                start();
            }
            
        });
    }

    /**
     * Starts the node manager thread
     */
    public void start() {
        // Starting own threads, which cares about node connections
        ThreadPoolMonitor monitor = new ThreadPoolMonitor() {
            public void handleThrowable(Class clazz, Runnable runnable,
                Throwable t)
            {
                log().error(runnable + ": " + t.toString(), t);
            }
        };
        threadPool = new DefaultThreadPool(monitor,
            Constants.MAX_INCOMING_CONNECTIONS, Thread.MIN_PRIORITY);

        // Start worker
        workerThread = new Thread(new PeriodicalWorker(), "NodeManager worker");
        workerThread.start();

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

        started = true;
        log().debug("Started");
    }

    /**
     * This starts the connecting process to other nodes, should be done, after
     * UI is opend.
     */
    public void startConnecting() {
        // Start reconnectors
        // These will spawn themself
        log().debug("Starting connection procees");
        Reconnector reconnector = new Reconnector();
        reconnectors.add(reconnector);
        reconnector.start();
    }

    /**
     * Shuts the nodemanager down
     */
    public void shutdown() {
        // Remove listeners, not bothering them by boring shutdown events
        // ListenerSupportFactory.removeAllListeners(listenerSupport);
        synchronized (listeners) {
            listeners.clear();
        }
        started = false;

        if (myThread != null) {
            myThread.interrupt();
        }

        // Stop threadpool
        if (threadPool != null) {
            log().debug("Shutting down incoming connection threadpool");
            threadPool.stop();
        }

        log().debug(
            "Shutting down " + acceptors.size()
                + " incoming connections (Acceptors)");
        List<Acceptor> tempList = new ArrayList<Acceptor>(acceptors);
        for (Acceptor acceptor : tempList) {
            acceptor.shutdown();
        }

        // Shutdown reconnectors
        log().debug("Shutting down " + reconnectors.size() + " reconnectors");
        for (Iterator<Reconnector> it = reconnectors.iterator(); it.hasNext();)
        {
            Reconnector reconnector = it.next();
            reconnector.shutdown();
            it.remove();
        }

        if (workerThread != null) {
            log().debug("Shutting down broadcaster");
            workerThread.interrupt();
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
            if (getController().getMySelf().isSupernode()) {
                // Store supernodes
                storeSupernodes();
            }
            // Shutdown, unloaded nodefile
            nodefileLoaded = false;
        }
        log().debug("Stopped");
    }

    /** for debug * */
    public void setSuspendFireEvents(boolean suspended) {
        // ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        // log().debug("setSuspendFireEvents: " + suspended);
        log().error("setSuspendFireEvents Not implemented");
    }

    

    /**
     * Answers the number of nodes, which are online on the network
     * 
     * @return
     */
    public int countOnlineNodes() {
        Member[] nodes = getNodes();
        int nConnected = 1;
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].isConnected() || nodes[i].isConnectedToNetwork()) {
                nConnected++;
            }
        }
        return nConnected;
    }

    /**
     * Counts the number of know supernodes
     * 
     * @return
     */
    public int countSupernodes() {
        Member[] nodes = getNodes();
        int nSupernodes = 0;
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].isSupernode()) {
                nSupernodes++;
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
            log().warn(
                "Not more connection slots open. Used " + nConnected + "/"
                    + maxConnectionsAllowed);
        }

        // Still capable of new connections?
        return nConnected > maxConnectionsAllowed;
    }

    /**
     * Answers the own identity, of course with no connection
     * 
     * @return
     */
    public Member getMySelf() {
        return mySelf;
    }

    /**
     * Returns its masternode FIXME: java.lang.NullPointerException at
     * de.dal33t.powerfolder.net.NodeManager.getMasterNode(NodeManager.java:280)
     * at de.dal33t.powerfolder.Member.isMaster(Member.java:198) at
     * de.dal33t.powerfolder.Member.handleMessage(Member.java:842) at
     * de.dal33t.powerfolder.net.ConnectionHandler$Receiver.run(ConnectionHandler.java:859)
     * at java.lang.Thread.run(Thread.java:534)
     * 
     * @return
     */
    public Member getMasterNode() {
        return getNode(getController().getConfig().getProperty("masternodeid"));
    }

    /**
     * Answers if we know this member
     * 
     * @param member
     * @return
     */
    public boolean knowsNode(Member member) {
        if (member == null) {
            return false;
        }
        // do i know him ?
        return knowsNode(member.getId());
    }

    /**
     * Answers if we know this member
     * 
     * @param member
     * @return
     */
    public boolean knowsNode(MemberInfo member) {
        if (member == null) {
            return false;
        }
        return knowsNode(member.id);
    }
    

    /**
     * Answers the number of connected nodes
     * 
     * @return
     */
    public int countConnectedNodes() {
        return onlineNodes.size();
    }
    

    /**
     * Answers if we know this member
     * 
     * @param id
     *            the id of the member
     * @return
     */
    public boolean knowsNode(String id) {
        if (id == null) {
            return false;
        }
        // do i know him ?
        return id.equals(mySelf.getId()) || knownNodes.containsKey(id);
    }

    /**
     * Returns the member from a memberinfo
     * 
     * @param mInfo
     * @return
     */
    public Member getNode(MemberInfo mInfo) {
        if (mInfo == null) {
            return null;
        }
        return getNode(mInfo.id);
    }

    /**
     * Returns the member for this id
     * 
     * @param id
     * @return
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
     * Answers all known nodes
     * 
     * @return
     */
    public Member[] getNodes() {
        synchronized (knownNodes) {
            Member[] nodes = new Member[knownNodes.size()];
            knownNodes.values().toArray(nodes);
            return nodes;
        }
    }

    /**
     * Returns all valid nodes
     * 
     * @return
     */
    public Member[] getValidNodes() {
        Member[] nodes = getNodes();
        // init with initial cap. to reduce growt problems
        List validNodes = new ArrayList(nodes.length);

        for (Member node : nodes) {
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
        node.removeAllListener();

        // Fire event
        fireNodeRemoved(node);
        log().debug("Node remove complete. " + node);
    }

    /**
     * Counts the number of friends that are online
     * 
     * @return
     */
    public int countOnlineFriends() {
        int nOnlineFriends = 0;        
        for (Member friend : friends) {
            if (friend.isConnectedToNetwork()) {
                nOnlineFriends++;
            }
        }        
        return nOnlineFriends;
    }

    /**
     * Counts the number of friends
     * 
     * @return
     */
    public int countFriends() {
        return friends.size();
        // return getFriends0().size();
    }

   

    /**
     * Returns an list of <code>MemberInfo</code> s which contains all friends
     * 
     * @return
     */
    // private Set<Member> getFriends0() {
    // Set<Member> friends = new HashSet<Member>();
    // synchronized (knownNodes) {
    // for (Member node : knownNodes.values()) {
    // if (node.isFriend()) {
    // friends.add(node);
    // }
    // }
    // }
    // return friends;
    // }
    /**
     * Returns the list of friends
     * 
     * @return
     */
    public Member[] getFriends() {
        // Set friends = getFriends0();
        Member[] friendsArr = new Member[friends.size()];
        friends.toArray(friendsArr);
        return friendsArr;
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
            onlineNodes.add(node);
            // add to broadcastlist
            nodesWentOnline.add(node.getInfo());
        } else {
            // Node went offline. Break all downloads from him
            getController().getTransferManager().breakTransfers(node);

            // Remove from list
            onlineNodes.remove(node);
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
     * Queues new nodes for connection
     * 
     * @param members
     */
    public void queueNewNodes(MemberInfo[] newNodes) {
        if (newNodes == null || newNodes.length == 0) {
            return;
        }

        // queue new members
        log().verbose("Received new list of " + newNodes.length + " nodes");

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
                log().verbose("Not adding new node: " + newNode);
                continue;
            }
            Member thisNode = getNode(newNode);

            if (newNode.matches(mySelf)) {
                // ignore myself
                continue;
            } else if (getController().isLanOnly()
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
                synchronized (reconnectionQueue) {
                    // Add node to reconnection queue
                    if (!thisNode.isReconnecting()
                        && reconnectionQueue.indexOf(thisNode) < 0)
                    {
                        reconnectionQueue.add(thisNode);
                        nQueuedNodes++;
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
            log().verbose(
                "Queued " + nQueuedNodes + " new nodes for reconnection, "
                    + nNewNodes + " added");
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

        log().verbose("Connection queued for acception: " + socket + "");
        Acceptor acceptor = new Acceptor(socket);

        // Enqueue for later processing
        acceptors.add(acceptor);
        threadPool.invokeLater(acceptor);

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
     * Synchronized because we can only accept one member at a time, there may
     * be duplicate connections
     * 
     * @param socket
     */
    public void acceptNode(Socket socket) throws ConnectionException {
        log().verbose("Accepting member on socket: " + socket);

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
        log().verbose("Initalizing connection handler to " + socket);
        ConnectionHandler handler = new ConnectionHandler(getController(),
            socket);
        log().verbose("Connection handler ready " + handler);

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

        if (getMySelf().getInfo().equals(remoteIdentity.member)) {
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
            log().verbose(
                "Accept lock taken. Member: " + remoteIdentity.member
                    + ", Handler: " + handler);
            // Is this member already known to us ?
            member = getNode(remoteIdentity.member);

            if (member == null) {
                // Create new node
                member = new Member(getController(),
                    handler.getIdentity().member);

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
            log().verbose(
                "Accept lock released. Member: " + remoteIdentity.member
                    + ", Handler: " + handler);
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
            handler.sendMessageAsynchron(new Problem(rejectCause, true), null);
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

        // Add our valve listener
        node.addMessageListener(valveMessageListener);

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
        broadcastMessage(message, false);
    }

    /**
     * Broadcasts a message to all nodes, does not block. Message enqueued to be
     * sent asynchron.
     * 
     * @param message
     * @param omittPrivateNetworkers
     *            if the message should not be sent to private networking users
     */
    public void broadcastMessage(Message message, boolean omittPrivateNetworkers)
    {
        log().verbose("Broadcasting message: " + message);
        synchronized (knownNodes) {
            for (Member node : knownNodes.values()) {
                boolean omitt = omittPrivateNetworkers
                    && node.isPrivateNetworking();
                if (!omitt && node.isCompleteyConnected()) {
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
        log().verbose("Broadcasting message to supernodes: " + message);
        int nNodes = 0;
        List<Member> supernodes = new LinkedList();
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
            log().warn(
                "Sending message to supernode: " + supernode.getNick() + ". "
                    + message);
            supernode.sendMessageAsynchron(message, null);
            nNodes++;
        }

        return nNodes;
    }

    /**
     * Broadcast nodelist to all nodes
     */
    private void broadcastNodeList() {
        if (!started) {
            // not started, dont broadcast
            return;
        }
        log().verbose("Broadcasting nodelist");
        // broadcast nodes
        Message[] nodeLists = KnownNodes.createKnowNodesList(this);
        for (Message message : nodeLists) {
            // To all supernodes
            broadcastMessageToSupernodes(message, 0);
        }
    }

    /**
     * Broadcasts all nodes, that went online since the last call
     */
    private void broadcastNodesThatWentOnline() {
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

    /**
     * Loads members from disk and connects to them
     */
    private void loadNodes() {

        String filename = getController().getConfigName() + ".nodes";
        File nodesFile = new File(Controller.getMiscFilesLocation(), filename);

        if (!nodesFile.exists()) {
            // Try harder in local base
            nodesFile = new File(filename);
        }

        if (!nodesFile.exists()) {
            log().warn(
                "Unable to load nodes, file not found "
                    + nodesFile.getAbsolutePath());
            return;
        }

        ObjectInputStream oIn = null;

        try {
            InputStream fIn = new BufferedInputStream(new FileInputStream(
                nodesFile));
            oIn = new ObjectInputStream(fIn);
            // Load nodes
            List nodes = (List) oIn.readObject();

            // Load friends
            Set friends = (Set) oIn.readObject();

            log().info("Loaded " + nodes.size() + " nodes");
            MemberInfo[] supernodesArr = new MemberInfo[nodes.size()];
            nodes.toArray(supernodesArr);
            queueNewNodes(supernodesArr);

            // Set friendstatus on nodes
            for (Iterator it = friends.iterator(); it.hasNext();) {
                MemberInfo friend = (MemberInfo) it.next();
                Member node = friend.getNode(getController(), true);
                node.setFriend(true);
                if (!this.friends.contains(node) && !node.isMySelf()) {
                    this.friends.add(node);
                }
            }

            // trigger connect to them
            triggerConnect();
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
        } finally {
            try {
                oIn.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Saves the state of current members/connections. members will get
     * reconnected at start
     */
    private void storeNodes() {
        storeNodes0(getController().getConfigName() + ".nodes", false);
    }

    /**
     * Stores the supernodes in a separate file
     */
    private void storeSupernodes() {
        storeNodes0(getController().getConfigName() + "-Supernodes.nodes", true);
    }

    /**
     * Internal method for storing nodes into a files
     * 
     * @param onlySupernodes
     */
    private void storeNodes0(String filename, boolean onlySupernodes) {
        synchronized (this) {
            File nodesFile = new File(Controller.getMiscFilesLocation(),
                filename);

            // store supernodes only
            List storingNodes = new ArrayList();
            Member[] members = getNodes();
            for (int i = 0; i < members.length; i++) {
                if (onlySupernodes && !members[i].isSupernode()) {
                    // Omitt non-supernodes if only supernodes wanted
                    continue;
                }
                storingNodes.add(members[i].getInfo());
            }

            // Add myself to know nodes
            storingNodes.add(getMySelf().getInfo());

            if (storingNodes.isEmpty()) {
                log().verbose("Not storing list of nodes, none known");
                return;
            }

            try {
                log().debug("Saving known nodes/friends");
                OutputStream fOut = new BufferedOutputStream(
                    new FileOutputStream(nodesFile));
                ObjectOutputStream oOut = new ObjectOutputStream(fOut);

                // supernodes
                oOut.writeObject(storingNodes);
                // then friends (has to be stored as Set, to keep compatibility)
                oOut.writeObject(new HashSet(Arrays.asList(Util
                    .asMemberInfos(getFriends()))));
                oOut.flush();
                oOut.close();
                fOut.close();
            } catch (IOException e) {
                log().warn(
                    "Unable to write supernodes to file '" + filename + "'. "
                        + e.getMessage());
                log().verbose(e);
            }
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
        ObjectInputStream oIn = null;
        try {
            Object content = url.getContent();
            if (content instanceof InputStream) {
                oIn = new ObjectInputStream((InputStream) content);
                List supernodes = (List) oIn.readObject();

                // Sort by connet time
                Collections.sort(supernodes,
                    MemberComparator.BY_LAST_CONNECT_DATE);

                for (Iterator it = supernodes.iterator(); it.hasNext();) {

                    MemberInfo node = (MemberInfo) it.next();

                    if (knowsNode(node) || !node.isSupernode) {
                        it.remove();
                    } else {
                        log().verbose(
                            node.toString() + " ,last connect: "
                                + node.lastConnectTime);

                        // If supernode is outdated, fix date
                        if (node.lastConnectTime == null
                            || node.lastConnectTime.getTime() < (System
                                .currentTimeMillis() - Constants.MAX_NODE_OFFLINE_TIME))
                        {
                            log()
                                .verbose(
                                    "Fixed date of internet supernode list "
                                        + node);
                            // Give supernode date (<20 days)
                            node.lastConnectTime = new Date(System
                                .currentTimeMillis()
                                - Constants.MAX_NODE_OFFLINE_TIME
                                + 1000
                                * 60
                                * 60 * 4);
                        }
                    }
                }

                log().info(
                    "Loaded " + supernodes.size() + " new supernodes from "
                        + NODES_URL);

                MemberInfo[] supernodesArr = new MemberInfo[supernodes.size()];
                supernodes.toArray(supernodesArr);
                queueNewNodes(supernodesArr);

                // Trigger connection to these nodes
                triggerConnect();
            }
        } catch (IOException e) {
            log().warn("Unable to read supernodes files from " + NODES_URL, e);
        } catch (ClassCastException e) {
            log().warn("Illegal format of supernodes files on " + NODES_URL, e);
        } catch (ClassNotFoundException e) {
            log().warn("Illegal format of supernodes files on " + NODES_URL, e);
        } finally {
            if (oIn != null) {
                try {
                    oIn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Triggers the connection thread, if waiting
     */
    private void triggerConnect() {
        log().verbose("Connect triggered");
        buildReconnectionQueue();
    }

    /**
     * Marks a node for immediate reconnection. Actually puts it in front of
     * reconnection queue
     * 
     * @param node
     * @return true if node was put in front of reconnection line
     */
    public boolean markNodeForImmediateReconnection(Member node) {
        if (node.isConnected() || node.isReconnecting() || node.isMySelf()
            || node.isUnableToConnect() || !node.isInteresting())
        {
            // Not reconnect nesseary
            return false;
        }

        if (getController().isLanOnly()
            && !NetworkUtil.isOnLanOrLoopback(node.getReconnectAddress()
                .getAddress()))
        {
            // no strangers in lan only mode
            return false;
        }

        // TODO: This code is also in buildReconnectionQueue
        // Offline limit time, all nodes before this time are not getting
        // reconnected
        Date offlineLimitTime = new Date(System.currentTimeMillis()
            - Constants.MAX_NODE_OFFLINE_TIME);
        // Check if node was offline too long
        Date lastConnectTime = node.getLastNetworkConnectTime();
        boolean offlineTooLong = true;

        offlineTooLong = lastConnectTime != null ? lastConnectTime
            .before(offlineLimitTime) : true;

        if (offlineTooLong) {
            return false;
        }

        log().verbose("Marking node for immediate reconnect: " + node);
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

            // Offline limit time, all nodes before this time are not getting
            // reconnected
            Date offlineLimitTime = new Date(System.currentTimeMillis()
                - Constants.MAX_NODE_OFFLINE_TIME);

            for (int i = 0; i < nodes.length; i++) {
                Member node = nodes[i];
                if (node.isConnected() || node.isMySelf()) {
                    // not process already connected nodes
                    continue;
                }

                if (getController().isLanOnly() && !node.isOnLAN()) {
                    // in lanOnly mode we don't want strangers
                    continue;
                }

                // Check if node was offline too long
                Date lastConnectTime = node.getLastNetworkConnectTime();
                boolean offlineTooLong = true;

                offlineTooLong = lastConnectTime != null ? lastConnectTime
                    .before(offlineLimitTime) : true;

                // If node is interesting
                if (node.isInteresting()) {
                    // Always add friends and supernodes to reconnect queue
                    if (node.isFriend() || node.isSupernode()
                        || (!node.receivedWrongIdentity() && !offlineTooLong))
                    {
                        reconnectionQueue.add(node);
                    }
                }
            }

            // Lately connect first
            Collections.sort(reconnectionQueue,
                MemberComparator.BY_RECONNECTION_PRIORITY);

            log().verbose(
                "Freshly filled reconnection queue with "
                    + reconnectionQueue.size() + " nodes, " + nBefore
                    + " were in queue before");

            // Notify threads
            if (!reconnectionQueue.isEmpty()) {
                reconnectionQueue.notify();
            }
        }
    }

    /**
     * Checks the incoming connection queue and cleans it up if nessesary
     */
    private void checkIncomingConnectionQueue() {
        List<Acceptor> tempList = new ArrayList<Acceptor>(acceptors);
        log().debug(
            "Checking incoming connection queue (" + tempList.size() + ")");
        if (tempList.size() > Constants.MAX_INCOMING_CONNECTIONS) {
            log().warn(
                "Processing too much incoming connections (" + tempList.size()
                    + ")");
        }
        for (Acceptor acceptor : tempList) {
            if (acceptor.hasTimeout()) {
                log().warn("Acceptor has timeout: " + acceptor);
                acceptor.shutdown();
            }
        }
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
         * Answers if this acceptor has a timeout
         * 
         * @return
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
                // Do reverse lookup
                if (socket.getInetAddress() != null) {
                    socket.getInetAddress().getHostName();
                }

                log().debug(
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
            log().verbose("Acceptor finished to " + socket);
        }

        public String toString() {
            return "Acceptor for " + socket;
        }
    }

    /**
     * Worker for periodically occouring things
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class PeriodicalWorker implements Runnable {

        public void run() {
            // this thread runs very fast, approx every second
            long waitTime = getController().getWaitTime() / 5;
            int runs = 0;

            // try {
            // Thread.sleep(getController().getWaitTime());
            // } catch (InterruptedException e) {
            // log().verbose(e);
            // return;
            // }

            // Wait cycles for wait to check for new development version
            // Testers will check every 30 seconds, normal users every hour
            int updateCheckTime = getController().isTester() ? 30 : 3600;
            Boolean limitedConnectivity = null;

            while (!Thread.currentThread().isInterrupted()) {
                // Write statistic approx every minute
                if (getController().isVerbose() && runs % 60 == 59) {
                    // Write network useage statistics information
                    Debug.writeStatistics(getController());
                }

                if (runs % updateCheckTime == 0) {
                    // Check for an update
                    new UpdateChecker(getController()).start();
                }

                if (runs % Constants.NODE_LIST_BROADCAST_INTERVAL == 0) {
                    broadcastNodeList();
                }

                // The transfer status
                if (runs % Constants.TRANSFER_STATUS_BROADCAST_INTERVAL == 0) {
                    // Broadcast new transfer status
                    TransferStatus status = getController()
                        .getTransferManager().getStatus();
                    log().verbose("Broadcasting transfer status: " + status);
                    broadcastMessage(status);
                }

                // Request network folder list from other supernodes if
                // supernode
                if (getController().getMySelf().isSupernode()
                    && runs % Constants.NETWORK_FOLDER_LIST_REQUEST_INTERVAL == 0)
                {
                    getController().getFolderRepository()
                        .requestNetworkFolderListIfRequired();
                }

                if (runs % Constants.NODES_THAN_WENT_ONLINE_BROADCAST_TIME == 0)
                {
                    // Broadcast all nodes, that went online in the last time
                    broadcastNodesThatWentOnline();
                }

                if (runs % Constants.INCOMING_CONNECTION_CHECK_TIME == 0) {
                    // Check inco connections
                    checkIncomingConnectionQueue();
                }

                if (runs % 30 == 0) {
                    // Check inco connections
                    log().warn("Triggered garbage collection");
                    System.gc();
                }

                final Preferences pref = getController().getPreferences();
                boolean testConnectivity = pref.getBoolean(
                    PREF_NAME_TEST_CONNECTIVITY, true); // true = default
                if (testConnectivity && getController().isPublicNetworking()
                    && !getController().isLanOnly()
                    && limitedConnectivity == null
                    && runs > TEST_CONNECTIVITY_DELAY)
                {
                    log().warn("Checking connecvitivty");
                    limitedConnectivity = new Boolean(getController()
                        .hasLimitedConnectivity());
                    if (limitedConnectivity.booleanValue()
                        && getController().isUIOpen())
                    {
                        Runnable showMessage = new Runnable() {
                            public void run() {
                                boolean showAgain = DialogFactory
                                    .showNeverAskAgainMessageDialog(
                                        getController().getUIController()
                                            .getMainFrame().getUIComponent(),
                                        Translation
                                            .getTranslation("limitedconnection.title"),
                                        Translation
                                            .getTranslation("limitedconnection.text"),
                                        Translation
                                            .getTranslation("general.show_never_again"));
                                if (!showAgain) {

                                    pref.putBoolean(
                                        PREF_NAME_TEST_CONNECTIVITY, false);
                                    log().warn(
                                        "store do not show this dialog again");
                                }
                            }
                        };
                        getController().getUIController().invokeLater(
                            showMessage);

                    }
                }

                // wait
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    break;
                }
                runs++;
            }
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
            if (currentNode != null) {
                currentNode.shutdown();
            }
            interrupt();
        }

        public void run() {
            log().verbose("Starting reconnector: " + getName());
            long waitTime = Constants.SOCKET_CONNECT_TIMEOUT;

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
                            log().verbose(
                                "Not reconnecting to " + currentNode.getNick()
                                    + ", already reconnecting/connected");
                            currentNode = null;
                        }

                        // Resize recon pool
                        resizeReconnectorPool();
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
                            && e.getFrom().getIdentity().member != null
                            && e.getFrom().getIdentity().member.getNode(
                                getController()).isConnected())
                        {
                            // We are already connected to that node!
                            // Not connect to ip
                            connectToIP = false;
                            log().warn(
                                "Already connected to "
                                    + e.getFrom().getIdentity().member.nick
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
                        - reconnectTook;
                    if (waitUntilNextTry > 0) {
                        try {
                            log().verbose(
                                this + ": Going on idle for "
                                    + waitUntilNextTry + "ms");
                            Thread.sleep(waitUntilNextTry);
                        } catch (InterruptedException e) {
                            log().verbose(this + " interrupted, breaking");
                            break;
                        }
                    }
                } else {
                    log().verbose(this + " is on idle");
                    // Otherwise wait a bit
                    Waiter waiter = new Waiter(waitTime);
                    while (!waiter.isTimeout() && reconnectionQueue.isEmpty()) {
                        waiter.waitABit();
                    }
                }
            }
        }

        /**
         * Resizes the pool of active reconnectors
         * 
         * @return the new spawned reconnector or null if non was required
         */
        private void resizeReconnectorPool() {
            int nReconnector = reconnectors.size();

            // Calculate required reconnectors
            int reqReconnectors = Math.min(Constants.NUMBER_RECONNECTORS, Math
                .max(Constants.NUMBER_RECONNECTORS_PRIVATE_NETWORKING,
                    reconnectionQueue.size() / 3));

            int reconDiffer = reqReconnectors - nReconnector;

            log().verbose(
                "Got " + reconnectionQueue.size()
                    + " nodes queued for reconnection");

            // TODO: Remove reconnectors if not longer needed

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
                for (int i = 0; i < -reconDiffer; i++) {
                    // Kill one reconnector
                    Reconnector reconnector = reconnectors.remove(0);
                    if (reconnector != null) {
                        log().debug("Killing reconnector " + reconnector);
                        reconnector.shutdown();
                    }
                }
            }
        }

        public String toString() {
            return getName();
        }
    }

    // UI-Methods *************************************************************

    
    

   

    // Listener support *******************************************************

    public void addNodeManagerListener(NodeManagerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        // ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeNodeManagerListener(NodeManagerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
        // ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    // Helper *****************************************************************

    private void fireNodeRemoved(Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.nodeRemoved(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.nodeRemoved(new NodeManagerEvent(this, node));

    }

    private void fireNodeAdded(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.nodeAdded(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.nodeAdded(new NodeManagerEvent(this, node));
    }

    private void fireNodeConnected(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.nodeConnected(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.nodeConnected(new NodeManagerEvent(this, node));
    }

    private void fireNodeDisconnected(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.nodeDisconnected(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.nodeDisconnected(new NodeManagerEvent(this, node));
    }

    private void fireFriendAdded(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.friendAdded(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.friendAdded(new NodeManagerEvent(this, node));
    }

    private void fireFriendRemoved(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.friendRemoved(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.friendRemoved(new NodeManagerEvent(this, node));
    }

    public void fireNodeSettingsChanged(final Member node) {
        synchronized (listeners) {
            for (NodeManagerListener listener : listeners) {
                listener.settingsChanged(new NodeManagerEvent(this, node));
            }
        }
        // listenerSupport.settingsChanged(new NodeManagerEvent(this, node));
    }
}