package de.dal33t.powerfolder.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.compare.MemberComparator;

/**
 * Responsible for reconnecting to remote nodes.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ReconnectManager extends PFComponent {

    /** Queue holding all nodes, which are waiting to be reconnected */
    private List<Member> reconnectionQueue;
    /** The collection of reconnector */
    private List<Reconnector> reconnectors;
    // Counter for started reconntor, running number
    private static int reconnectorCounter;
    private boolean started;

    public ReconnectManager(Controller controller) {
        super(controller);

        // Linkedlist, faster for queue useage
        reconnectionQueue = new LinkedList<Member>();
        // All reconnectors
        reconnectors = Collections
            .synchronizedList(new ArrayList<Reconnector>());
    }

    /**
     * This starts the connecting process to other nodes, should be done, after
     * UI is opend.
     */
    public void start() {
        started = true;
        // Start reconnectors
        log()
            .debug(
                "Starting Reconnection Manager. Going to connect to other nodes now...");
        buildReconnectionQueue();
        // Resize reconnector pool from time to time. First execution: NOW
        getController().getController().scheduleAndRepeat(
            new ReconnectorPoolResizer(), 0,
            Constants.RECONNECTOR_POOL_SIZE_RESIZE_TIME * 1000);
    }

    public void shutdown() {
        started = false;
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
    }

    /**
     * @return the size of the reconnection queue
     */
    public int countReconnectionQueue() {
        return reconnectionQueue.size();
    }

    /**
     * @return unmodifiable reference to the internal reconnection queue.
     */
    public Collection<Member> getReconnectionQueue() {
        return Collections.unmodifiableCollection(reconnectionQueue);
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
     * Checks if a reconnection to this node would be useful.
     * 
     * @param node
     *            the node to connect to
     * @return true if added to the reconnection queue, false if not.
     */
    public boolean considerReconnectionTo(Member node) {
        if (shouldBeAddedToReconQueue(node)) {
            synchronized (reconnectionQueue) {
                // Add node to reconnection queue
                if (!reconnectionQueue.contains(node)) {
                    reconnectionQueue.add(node);
                    // Resort reconnection queue
                    Collections.sort(reconnectionQueue,
                        MemberComparator.BY_RECONNECTION_PRIORITY);

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Freshly refills the reconnection queue. The nodes contained are tried to
     * reconnected. Also removes unused nodes
     */
    public void buildReconnectionQueue() {
        int nBefore = reconnectionQueue.size();
        synchronized (reconnectionQueue) {
            reconnectionQueue.clear();

            for (Member node : getController().getNodeManager()
                .getNodesAsCollection())
            {
                if (shouldBeAddedToReconQueue(node)) {
                    reconnectionQueue.add(node);
                }
            }

            // Lately connect first
            Collections.sort(reconnectionQueue,
                MemberComparator.BY_RECONNECTION_PRIORITY);

            if (logDebug) {
                log().debug(
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

    // Internal methods *******************************************************

    private boolean shouldBeAddedToReconQueue(Member node) {
        Reject.ifNull(node, "Node is null");
        if (!started) {
            return false;
        }
        if (node.getInfo().isInvalid(getController())) {
            // Invalid
            return false;
        }
        if (node.isConnected() || node.isMySelf()) {
            // not process already connected nodes
            return false;
        }
        if (node.isReconnecting()) {
            return false;
        }
        if (node.receivedWrongIdentity()) {
            return false;
        }
        if (!node.isInteresting()) {
            return false;
        }
        // Always add friends
        if (node.isFriend()) {
            // Always try to connect to friends
            return true;
        }
        // Results in server shutdown by HETZNER!!
        // if (node.isOnLAN()) {
        // // Always try to connect to LAN users
        // return true;
        // }
        // Disable, could cause #609
        // if (node.isUnableToConnect()) {
        // boolean causedByDupeConnection = node.getLastProblem() != null
        // && node.getLastProblem().problemCode == Problem.DUPLICATE_CONNECTION;
        //
        // if (!causedByDupeConnection) {
        // // Do not connect if not connection is possible
        // // But RE-try if this was caused by a dupe connection.
        // log().verbose(
        // "Not tring to connect because of unable to connect: "
        // + node);
        // return false;
        // }
        // }

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
        if (node.isDontConnect()) {
            // Don't connect if the node doesn't want to be connected!
            return false;
        }

        int nConnectedSupernodes = getController().getNodeManager()
            .countConnectedSupernodes();

        if (node.isSupernode()
            && nConnectedSupernodes < Constants.N_SUPERNODES_TO_CONNECT)
        {
            // Connect to supernodes that are not offline too long
            return true;
        }
        return false;
    }

    // Inner classes **********************************************************

    /**
     * Resizes the pool of active reconnectors
     */
    private class ReconnectorPoolResizer extends TimerTask {
        @Override
        public void run() {
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
                            .size() / 3)));

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
                            reconnector.softShutdown();
                        }
                    }
                }
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
                    "Unable to start reconnector. ReconnectManager not started");
            }
            super.start();
            reconStarted = true;
        }

        /**
         * soft shutdown. does not stop the current recon try.
         */
        public void softShutdown() {
            reconStarted = false;
            reconnectorCounter--;
            // interrupt();
            // if (currentNode != null) {
            // currentNode.shutdown();
            // }
        }

        public void shutdown() {
            softShutdown();
            interrupt();
            if (currentNode != null) {
                currentNode.shutdown();
            }
        }

        public void run() {
            if (logVerbose) {
                log().verbose("Starting reconnector: " + getName());
            }

            while (this.reconStarted) {
                synchronized (reconnectionQueue) {
                    if (!started) {
                        log().warn(
                            "Stopping " + this + ". ReconnectManager is down");
                        break;
                    }
                    if (reconnectionQueue.isEmpty()) {
                        // Rebuilds reconnection queue if required
                        buildReconnectionQueue();
                        if (reconnectionQueue.isEmpty()) {
                            // Throttle rebuilding of queue go on idle for 30
                            // secs
                            log().debug(
                                "Reconnection queue empty after rebuild."
                                    + "Going on idle for 5 seconds");
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                log().verbose(e);
                                break;
                            }
                        }
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

                if (currentNode == null) {
                    if (logVerbose) {
                        log().verbose(this + " is on idle");
                    }
                    // Otherwise wait a bit
                    try {
                        Waiter waiter = new Waiter(
                            Constants.SOCKET_CONNECT_TIMEOUT / 2);
                        while (!waiter.isTimeout()
                            && reconnectionQueue.isEmpty())
                        {
                            waiter.waitABit();
                        }
                    } catch (RuntimeException e) {
                        log().debug(this + " Stopping. cause: " + e.toString());
                        break;
                    }
                    // Idle time over. continue!
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

                    Identity otherNodeId = e.getFrom().getIdentity();
                    MemberInfo otherNodeInfo = otherNodeId != null
                        && otherNodeId.getMemberInfo() != null ? otherNodeId
                        .getMemberInfo() : null;

                    if (otherNodeInfo != null) {
                        try {
                            getController().getIOProvider()
                                .getConnectionHandlerFactory().tryToConnect(
                                    otherNodeInfo);
                        } catch (ConnectionException e1) {
                            log().verbose(e1);
                        }
                    }
                }

                long reconnectTook = System.currentTimeMillis() - start;
                long waitUntilNextTry = Constants.SOCKET_CONNECT_TIMEOUT / 2
                    - reconnectTook;
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
            }
        }

        public String toString() {
            return getName();
        }
    }

}
