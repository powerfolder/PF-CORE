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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.ConnectResult;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.MemberComparator;

/**
 * Responsible for reconnecting to remote nodes.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ReconnectManager extends PFComponent {

    /** Queue holding all nodes, which are waiting to be reconnected */
    private List<Member> reconnectionQueue;
    /** The collection of reconnector */
    private List<Reconnector> reconnectors;
    // Counter for started reconntor, running number
    private AtomicInteger reconnectorCounter = new AtomicInteger(0);
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
        logFine("Starting Reconnection Manager. Going to connect to other nodes now...");
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
            logFine("Shutting down " + reconnectors.size() + " reconnectors");
            for (Iterator<Reconnector> it = reconnectors.iterator(); it
                .hasNext();)
            {
                Reconnector reconnector = it.next();
                reconnector.shutdown();
                it.remove();
            }
        }
        synchronized (reconnectionQueue) {
            reconnectionQueue.clear();
            reconnectionQueue.notifyAll();
        }
    }

    public boolean isStarted() {
        return started;
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
     * reconnection queue and ensures, that is gets reconnected immediately.
     * 
     * @param node
     */
    public void markNodeForImmediateReconnection(Member node) {
        if (!started) {
            logFine("ReconnectManager not started. Unable to spawn new reconnector to "
                + node + ". Queue: " + reconnectionQueue);
            return;
        }
        if (isFiner()) {
            logFiner("Marking node for immediate reconnect: " + node);
        }
        if (node.isConnected() || node.isConnecting()) {
            // Skip, not necessary.
            return;
        }
        synchronized (reconnectionQueue) {
            // Remove node
            reconnectionQueue.remove(node);
            // Add at start
            reconnectionQueue.add(0, node);
            reconnectionQueue.notify();
        }

        // Wait 20 ms to let one reconnector grab the node.
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            logFiner("InterruptedException", e);
            return;
        }

        // None has take the node to reconnect.
        // Spawn new reconnector
        if (reconnectionQueue.contains(node)) {
            if (isFine()) {
                logFine("Spawning new Reconnector (" + (reconnectors.size() + 1)
                    + " total) to get faster reconnect to " + node);
            }
            if (!started) {
                logSevere("ReconnectManager not started. Unable to spawn new reconnector to "
                    + node + ". Queue: " + reconnectionQueue);
                return;
            }
            synchronized (reconnectors) {
                Reconnector reconnector = new Reconnector();
                // add reconnector to nodemanager
                reconnectors.add(reconnector);
                // and start
                reconnector.start();
            }
        } else if (isFiner()) {
            logFiner("Not required to spawn new reconnector to " + node
                + ". Queue: " + reconnectionQueue);
        }
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
                    reconnectionQueue.notify();
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

            if (isFiner()) {
                logFiner("Freshly filled reconnection queue with "
                    + reconnectionQueue.size() + " nodes, " + nBefore
                    + " were in queue before");
            }

            if (getController().isVerbose()) {
                Debug.writeNodeListCSV(reconnectionQueue,
                    "ReconnectionQueue.csv");
            }

            if (reconnectionQueue.size() > 100) {
                logWarning("Reconnection queue contains more than 200 nodes");
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
        if (node.isConnecting()) {
            return false;
        }
        if (node.receivedWrongIdentity()) {
            return false;
        }
        if (!node.isInteresting()) {
            return false;
        }
        if (node.isServer() || ServerClient.isTempServerNode(node.getInfo())) {
            // Server nodes get pushed into reconnection queue by own thread
            return false;
        }
        if (getController().getIOProvider().getRelayedConnectionManager()
            .isRelay(node))
        {
            // Relay nodes get reconnected by own thread
            return false;
        }
        if (getController().getNetworkingMode().equals(
            NetworkingMode.SERVERONLYMODE))
        {
            // Never connect this way to the server, only thru ServerClient.
            return false;
        }
        if (!node.isOnSameNetwork()) {
            // Don't try to connect to nodes on different network.
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
        // logFiner(
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
            if (!started) {
                return;
            }
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

                if (isFiner()) {
                    logFiner("Got " + reconnectionQueue.size()
                        + " nodes queued for reconnection");
                }

                if (reconDiffer > 0) {
                    // We have to less reconnectors, spawning one...

                    for (int i = 0; i < reconDiffer; i++) {
                        final Reconnector reconnector = new Reconnector();
                        // add reconnector to nodemanager
                        reconnectors.add(reconnector);
                        // and start time shifted
                        getController().schedule(new Runnable() {
                            public void run() {
                                reconnector.start();
                            }
                        }, i * 500L);

                    }

                    logFine("Spawned " + reconDiffer + " reconnectors. "
                        + reconnectors.size() + "/" + reqReconnectors
                        + ", nodes in reconnection queue: "
                        + reconnectionQueue.size());
                } else if (reconDiffer < 0) {
                    logFine("Killing " + -reconDiffer
                        + " Reconnectors. Currently have: " + nReconnector
                        + " Reconnectors");
                    for (int i = 0; i < -reconDiffer; i++) {
                        // Kill one reconnector
                        if (reconnectors.size() <= 1) {
                            logWarning("Not killing last reconnector");
                            // Have at least one reconnector
                            break;
                        }
                        Reconnector reconnector = reconnectors.remove(0);
                        if (reconnector != null) {
                            logFiner("Killing reconnector " + reconnector);
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
            super("Reconnector " + reconnectorCounter.addAndGet(1));
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
            reconnectorCounter.decrementAndGet();
        }

        public void shutdown() {
            softShutdown();
            interrupt();
            if (currentNode != null) {
                currentNode.shutdown();
            }
        }

        private int getIdleWaitSeconds() {
            return ConfigurationEntry.CONNECT_WAIT.getValueInt(getController());
        }

        public void run() {
            if (isFiner()) {
                logFiner("Starting reconnector: " + getName());
            }

            while (this.reconStarted) {
                if (!started) {
                    logFine("Stopping " + this + ". ReconnectManager is down");
                    break;
                }
                if (!getController().getNodeManager().isStarted()) {
                    logFine("Stopping " + this + ". NodeManager is down");
                    break;
                }
                synchronized (reconnectionQueue) {
                    if (reconnectionQueue.isEmpty()) {
                        int idleSeconds = getIdleWaitSeconds();
                        logFine("Reconnection queue empty. " + this
                            + " going on idle for " + idleSeconds + " seconds");
                        try {
                            reconnectionQueue.wait(1000L * idleSeconds);
                        } catch (InterruptedException e) {
                            logFiner(e);
                            break;
                        }
                        if (reconnectionQueue.isEmpty()) {
                            // Rebuilds reconnection queue if required
                            buildReconnectionQueue();
                        }
                    }
                }
                synchronized (reconnectionQueue) {
                    if (!reconnectionQueue.isEmpty()) {
                        // Take the first node out of the reconnection queue

                        currentNode = reconnectionQueue.remove(0);
                        if (currentNode.isConnected()
                            || currentNode.isConnecting())
                        {
                            // Already reconnecting. Skip
                            if (isFiner()) {
                                logFiner("Not reconnecting to "
                                    + currentNode.getNick()
                                    + ", already reconnecting/connected");
                            }
                            currentNode = null;
                        }
                    }
                    if (currentNode == null) {
                        continue;
                    }

                    // MARK connecting ***
                    if (currentNode.markConnecting() >= 2) {
                        currentNode.unmarkConnecting();
                        if (isFine()) {
                            logFine("Skipping: " + currentNode);
                        }
                        continue;
                    } else {
                        if (isFiner()) {
                            logFiner("Picked node for reconnect: "
                                + currentNode);
                        }
                    }
                }

                long start = System.currentTimeMillis();
                try { // UNMARK connecting try/finally ***
                    // A node could be obtained from the reconnection queue, try
                    // to connect now
                    if (!ServerClient.isTempServerNode(currentNode.getInfo())) {
                        try {
                            // Reconnect, Don't mark connecting. already done.
                            ConnectResult res = currentNode.reconnect(false);
                            if (isFiner()) {
                                logFiner("Reconnect to " + currentNode + ": "
                                    + res);
                            }
                        } catch (InvalidIdentityException e) {
                            Identity otherNodeId = e.getFrom().getIdentity();
                            MemberInfo otherNodeInfo = otherNodeId != null
                                && otherNodeId.getMemberInfo() != null
                                ? otherNodeId.getMemberInfo()
                                : null;

                            if (otherNodeInfo != null
                                && otherNodeInfo
                                    .isOnSameNetwork(getController()))
                            {
                                Member otherNode = otherNodeInfo.getNode(
                                    getController(), true);
                                boolean rec = considerReconnectionTo(otherNode);
                                logFine("Invalid identity from " + currentNode
                                    + ". Found: " + otherNode
                                    + ". Going to reconned it ? " + rec);
                            }
                        }
                    } else {
                        // Temporary server node, directly connect to
                        // IP/hostname
                        if (isFine()) {
                            logFine("Tring to connect to temporary server node at "
                                + currentNode.getHostName()
                                + ":"
                                + currentNode.getPort()
                                + ". ID: "
                                + currentNode.getId());
                        }
                        try {
                            ConnectionHandler conHan = getController()
                                .getIOProvider().getConnectionHandlerFactory()
                                .tryToConnect(currentNode.getInfo());
                            getController().getNodeManager().acceptConnection(
                                conHan);
                        } catch (ConnectionException e1) {
                            logFiner("ConnectionException", e1);
                        }
                    }
                } finally {
                    currentNode.unmarkConnecting();
                }
            }
        }

        public String toString() {
            return getName();
        }
    }

}
