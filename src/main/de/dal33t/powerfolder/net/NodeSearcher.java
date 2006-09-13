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
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.SearchNodeRequest;
import de.dal33t.powerfolder.util.Reject;

/**
 * This class searches nodes matching a given pattern.
 * 
 * @author Dennis "Dante" Waldherr
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 */
public final class NodeSearcher extends PFComponent {
    private String pattern;
    /** for convienience a reference to the member representing myself */
    private Member myself;
    /** indicates that we want to interrupt a search */
    private boolean stopSearching = false;
    /** exclude friends from this search result */
    private boolean ignoreFriends;
    /** exclude offline users from search result */
    private boolean hideOffline;
    private Thread searchThread;
    private NodeManagerListener nodeListener;
    private Queue<Member> canidatesFromSupernodes;
    private List<Member> searchResultListModel;

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
    public NodeSearcher(Controller controller, String thePattern,
        List<Member> resultListModel, boolean ignoreFriends, boolean hideOffline)
    {
        super(controller);
        Reject.ifNull(resultListModel, "Result list model is null");
        Reject.ifBlank(thePattern, "The search pattern is blank");

        nodeListener = new MyNodeManagerListener();
        searchThread = new Thread(new Searcher(),
            "NodeSearcher - searching for " + pattern);
        searchThread.setDaemon(true);
        pattern = thePattern;
        canidatesFromSupernodes = new LinkedList<Member>();
        searchResultListModel = resultListModel;

        this.ignoreFriends = ignoreFriends;
        this.hideOffline = hideOffline;
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
        if (!member.matches(pattern) || member.equals(myself)
            || searchResultListModel.contains(member))
        {
            return;
        }
        searchResultListModel.add(member);
    }

    private void checkAndAddMemberFast(Member member) {
        if (hideOffline && !member.isConnectedToNetwork()) {
            return;
        }
        if (ignoreFriends && member.isFriend()) {
            return;
        }
        if (!member.matchesFast(pattern) || member.equals(myself)
            || searchResultListModel.contains(member))
        {
            return;
        }
        searchResultListModel.add(member);
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
            Member[] members = getController().getNodeManager().getNodes();
            for (int i = 0; i < members.length; i++) {
                checkAndAddMemberFast(members[i]);
            }

            for (int i = 0; i < members.length; i++) {
                // This part maybe slow (the first time) needs
                // interuption
                if (stopSearching) {
                    // Hej a new search!, stop this one
                    break;
                }
                checkAndAddMember(members[i]);
            }
        }

        private void searchSupernodes() {
            // Listen for fresh canidates
            getController().getNodeManager().addNodeManagerListener(
                nodeListener);
            Message msg = new SearchNodeRequest(pattern);
            getController().getNodeManager().broadcastMessageToSupernodes(msg,
                Constants.N_SUPERNODES_TO_CONTACT_FOR_NODE_SEARCH);

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
            synchronized (searchThread) {
                searchThread.notifyAll();
            }
        }

    }
}