/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.lang.Thread.State;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.SearchNodeRequest;
import de.dal33t.powerfolder.util.Reject;

/**
 * This class searches nodes matching a given pattern.
 * <p>
 * TODO Check if search request should really send to LAN nodes.
 * 
 * @author Dennis "Dante" Waldherr
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 */
public class NodeSearcher extends PFComponent {
    private String pattern;
    /** indicates that we want to interrupt a search */
    private boolean stopSearching;
    /** exclude friends from this search result */
    private boolean ignoreFriends;
    /** exclude offline users from search result */
    private boolean hideOffline;
    private Thread searchThread;
    private NodeManagerListener nodeListener;
    private Queue<Member> canidatesFromSupernodes;
    private List<Member> searchResultListModel;
    private NodeSearchFilter nodeSearchFilter;

    /**
     * Constructs a new node searcher with giben pattern and a result listmodel.
     * <p>
     * If you want to get informed about the changes in the list it is
     * recommended to use a <code>ObservableList</code>.
     * <p>
     * Changes of the list model will be done in a own thread (not swing event
     * dispatcher thread!)
     * 
     * @param controller
     * @param thePattern
     * @param resultListModel
     *            the list that will contain the results of the search.
     * @param ignoreFriends
     *            if true friends will be ignored in this list
     * @param hideOffline
     *            hides the users that are offline
     */
    public NodeSearcher(Controller controller, String pattern,
        List<Member> resultListModel, boolean ignoreFriends, boolean hideOffline)
    {
        super(controller);
        Reject.ifNull(resultListModel, "Result list model is null");
        Reject.ifBlank(pattern, "The search pattern is blank");

        nodeListener = new MyNodeManagerListener();
        searchThread = new Thread(new Searcher(),
            "NodeSearcher - searching for " + pattern);
        searchThread.setDaemon(true);
        this.pattern = pattern;
        canidatesFromSupernodes = new LinkedList<Member>();
        searchResultListModel = resultListModel;

        this.ignoreFriends = ignoreFriends;
        this.hideOffline = hideOffline;
        nodeSearchFilter = new NodeSearchFilter();
    }

    /**
     * Starts the search
     */
    public void start() {
        searchThread.start();
    }

    /**
     * Cancels this search. This method will block until the searching has
     * stopped.
     */
    public void cancelSearch() {
        try {
            stopSearching = true;
            synchronized (searchThread) {
                // Wake the searcher Thread
                searchThread.notifyAll();
            }
            // give some time to shutdown the running search
            searchThread.join(500);
            if (isSearching()) { // Searching didn't stop within 500ms
                // Interrupt it
                searchThread.interrupt();
            }
        } catch (InterruptedException ie) {
            // This might mean that 2 Threads called cancelSearch()
            log().error(ie);
        }
    }

    /**
     * @return true if this Thread is still searching new members
     */
    public boolean isSearching() {
        return !searchThread.getState().equals(State.TERMINATED);
    }

    // Internal code **********************************************************

    private void checkAndAddMember(Member member) {
        if (hideOffline && !member.isConnectedToNetwork()) {
            return;
        }
        if (ignoreFriends && member.isFriend()) {
            return;
        }
        if (!member.matches(pattern) || searchResultListModel.contains(member))
        {
            return;
        }
        searchResultListModel.add(member);
    }

    private class NodeSearchFilter implements NodeFilter {
        public boolean shouldAddNode(MemberInfo nodeInfo) {
            return nodeInfo.matches(pattern);
        }
    }

    /**
     * Listens to the nodemanager for fresh canidates.
     */
    private final class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
            synchronized (searchThread) {
                canidatesFromSupernodes.add(e.getNode());
                searchThread.notifyAll();
            }
        }

        public void nodeConnected(NodeManagerEvent e) {
            // if connected and hiding of offline is enabled the should popup in
            // search result
            synchronized (searchThread) {
                canidatesFromSupernodes.add(e.getNode());
                searchThread.notifyAll();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (hideOffline) {
                searchResultListModel.remove(e.getNode());
            }
        }

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
            // if friend status removed it should popup in search result (rare)
            synchronized (searchThread) {
                canidatesFromSupernodes.add(e.getNode());
                searchThread.notifyAll();
            }
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * The actual search is done here.
     */
    private class Searcher implements Runnable {
        public void run() {
            searchResultListModel.clear();

            // Search local database first
            searchLocal();

            // Ask connected SuperNodes for search results
            searchSupernodes();
        }

        private void searchLocal() {
            for (Member member : getController().getNodeManager()
                .getNodesAsCollection())
            {
                checkAndAddMember(member);
            }
        }

        private void searchSupernodes() {
            // Listen for fresh canidates
            getController().getNodeManager().addNodeManagerListener(
                nodeListener);
            getController().getNodeManager().addNodeFilter(nodeSearchFilter);
            Message msg = new SearchNodeRequest(pattern);
            getController().getNodeManager().broadcastMessageToSupernodes(msg,
                Constants.N_SUPERNODES_TO_CONTACT_FOR_NODE_SEARCH);

            getController().getNodeManager().broadcastMessageLANNodes(msg,
                Constants.N_LAN_NODES_TO_CONTACT_FOR_NODE_SEARCH);

            while (!stopSearching) {
                synchronized (searchThread) {
                    while (!canidatesFromSupernodes.isEmpty()) {
                        checkAndAddMember(canidatesFromSupernodes.remove());
                    }
                    try {
                        searchThread.wait();
                    } catch (InterruptedException e) {
                        log().warn("Search was interrupted", e);
                        break;
                    }
                }
            }

            getController().getNodeManager().removeNodeManagerListener(
                nodeListener);
            getController().getNodeManager().removeNodeFilter(nodeSearchFilter);
            synchronized (searchThread) {
                searchThread.notifyAll();
            }
        }
    }
}