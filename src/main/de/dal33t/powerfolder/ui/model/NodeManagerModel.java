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
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.NodeManagerModelEvent;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.compare.MemberComparator;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Model for the node manager. Create filtered list of nodes based on
 * friend and lan values.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.5 $
 */
public class NodeManagerModel extends PFUIComponent {

    private ValueModel hideOfflineFriendsModel;
    private ValueModel includeOnlineLanModel;
    private ArrayListModel<Member> friendsListModel;
    private final Set<Member> nodes;
    private final NodeManager nodeManager;
    private final Set<NodeManagerModelListener> listeners;

    /**
     * Constructor
     *
     * @param controller
     */
    public NodeManagerModel(Controller controller) {
        super(controller);
        nodes = new CopyOnWriteArraySet<Member>();
        nodeManager = getController().getNodeManager();
        listeners = new CopyOnWriteArraySet<NodeManagerModelListener>();
        initalize();
    }

    /**
     * Add a node manager model listener.
     *
     * @param l
     */
    public void addNodeManagerModelListener (NodeManagerModelListener l) {
        listeners.add(l);
    }

    /**
     * Remove a node manager model listener.
     *
     * @param l
     */
    public void removeNodeManagerModelListener (NodeManagerModelListener l) {
        listeners.remove(l);
    }

    /**
     * Initalize all nessesary ui models
     */
    private synchronized void initalize() {

        PropertyChangeListener propertyChangeListener =
                new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                rebuildSet();
            }
        };
        
        friendsListModel = new ArrayListModel<Member>();

        hideOfflineFriendsModel = PreferencesEntry
                .NODE_MANAGER_MODEL_HIDE_OFFLINE_FRIENDS.getModel(getController());
        hideOfflineFriendsModel.addValueChangeListener(propertyChangeListener);

        includeOnlineLanModel = PreferencesEntry
                .NODE_MANAGER_MODEL_INCLUDE_ONLINE_LAN_USERS.getModel(getController());
        includeOnlineLanModel.addValueChangeListener(propertyChangeListener);

        rebuildSet();

        // Register listener on node manager
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
    }

    /**
     * Rebuild the whole node list because of big change.
     */
    private void rebuildSet() {
        Collection<Member> nodeCollection = nodeManager.getNodesAsCollection();
        Set<Member> newSet = new TreeSet<Member>();
        for (Member node : nodeCollection) {
            if (required(node)) {
                newSet.add(node);
            }
        }
        nodes.clear();
        nodes.addAll(newSet);
        for (NodeManagerModelListener listener : listeners) {
            listener.rebuilt(new NodeManagerModelEvent(this, null));
        }
        
        Member[] friends = getController().getNodeManager().getFriends();
        friendsListModel.clear();
        friendsListModel.addAll(Arrays.asList(friends));
        Collections.sort(friendsListModel, MemberComparator.IN_GUI);
    }

    /**
     * Answers if the node is required based on value models.
     *
     * @param node
     * @return
     */
    private boolean required(Member node) {

        boolean hideOfflineFriends = (Boolean) hideOfflineFriendsModel.getValue();
        boolean includeOnlineLan = (Boolean) includeOnlineLanModel.getValue();
        boolean connected = node.isCompleteyConnected();
        boolean online = connected || node.isConnectedToNetwork();
        
        if (node.isFriend()) {
            if (hideOfflineFriends) {
                if (online) {
                    return true;
                }
            } else {
                return true;
            }
        }

        if (includeOnlineLan && node.isOnLAN() && connected) {
            return true;
        }

        return false;
    }

    /**
     * Gets a read-only set of filtered nodes.
     *
     * @return
     */
    public Set<Member> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * @return if offline friends should be shown.
     */
    public ValueModel getHideOfflineFriendsModel() {
        return hideOfflineFriendsModel;
    }

    /**
     * @return if online lan users should be shown.
     */
    public ValueModel getIncludeOnlineLanModel() {
        return includeOnlineLanModel;
    }

    /**
     * @return a listmodel that contains the friendslist.
     */
    public ListModel getFriendsListModel() {
        return friendsListModel;
    }

    /**
     * Update method responding to addition or removal of nodes.
     *
     * @param node
     */
    private void updateNode(Member node) {
        boolean wanted = required(node);
        if (wanted) {
            if (!nodes.contains(node)) {
                nodes.add(node);
                for (NodeManagerModelListener listener : listeners) {
                    listener.nodeAdded(new NodeManagerModelEvent(this, node));
                }
            }
        } else {
            if (nodes.contains(node)) {
                nodes.remove(node);
                for (NodeManagerModelListener listener : listeners) {
                    listener.nodeRemoved(new NodeManagerModelEvent(this, node));
                }
            }
        }
    }

    /**
     * Listens for changes in the node manager
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
            if (!friendsListModel.contains(e.getNode())) {
                friendsListModel.add(e.getNode());
                Collections.sort(friendsListModel, MemberComparator.IN_GUI);
            }
            updateNode(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            friendsListModel.remove(e.getNode());
            Collections.sort(friendsListModel, MemberComparator.IN_GUI);
            updateNode(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void nodeRemoved(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
