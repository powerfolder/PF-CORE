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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.ListModel;

import com.jgoodies.binding.list.ArrayListModel;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.security.SecurityManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerListener;
import de.dal33t.powerfolder.util.compare.MemberComparator;

/**
 * Model for the node manager. Create filtered list of nodes based on friend and
 * lan values.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom</a>
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.5 $
 */
public class NodeManagerModel extends PFUIComponent {

    public enum Type {
        MY_COMPUTERS_INDEX, FRIENDS_INDEX, CONNECTED_LAN;
    }

    private ValueModel showOfflineModel;
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
    public void addNodeManagerModelListener(NodeManagerModelListener l) {
        listeners.add(l);
    }

    /**
     * Remove a node manager model listener.
     *
     * @param l
     */
    public void removeNodeManagerModelListener(NodeManagerModelListener l) {
        listeners.remove(l);
    }

    /**
     * Initalize all nessesary ui models
     */
    private synchronized void initalize() {

        PropertyChangeListener propertyChangeListener = new PropertyChangeListener()
        {

            public void propertyChange(PropertyChangeEvent evt) {
                rebuildSet();
            }
        };

        friendsListModel = new ArrayListModel<Member>();

        showOfflineModel = PreferencesEntry.NODE_MANAGER_MODEL_SHOW_OFFLINE
            .getModel(getController());
        showOfflineModel.addValueChangeListener(propertyChangeListener);

        rebuildSet();

        // Register listener on node manager
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        getController().getSecurityManager().addListener(
            new MySecurityManagerListener());
        getController().getOSClient().addListener(new MyServerClientListener());
    }

    /**
     * Rebuild the whole node list because of big change.
     */
    private void rebuildSet() {
        Collection<Member> nodeCollection = nodeManager.getNodesAsCollection();
        Set<Member> newSet = new TreeSet<Member>(MemberComparator.NICK);
        for (Member node : nodeCollection) {
            if (required(node)) {
                newSet.add(node);
            }
        }
        nodes.clear();
        nodes.addAll(newSet);
        for (NodeManagerModelListener listener : listeners) {
            listener.changed();
        }

        Member[] friends = getController().getNodeManager().getFriends();
        friendsListModel.clear();
        friendsListModel.addAll(Arrays.asList(friends));
        Collections.sort(friendsListModel, MemberComparator.IN_GUI);
    }

    /**
     * Answers if the node is required based on value models. This only returns
     * nodes that are my computer, friend or connected lan. Also filters online
     * based on showOfflineModel
     *
     * @param node
     * @return
     */
    private boolean required(Member node) {

        boolean connected = node.isCompletelyConnected();

        if (node.isMySelf()) {
            // Never add self
            return false;
        }

        // Only care about 1) my computers, 2) friends, and 3) connected lan
        if (node.isMyComputer() || node.isFriend() || node.isOnLAN()
            && connected)
        {
            // Wanted, now check online.
            boolean showOffline = (Boolean) showOfflineModel.getValue();
            boolean online = connected || node.isConnectedToNetwork();
            return showOffline || online;
        } else {
            return false;
        }

    }

    /**
     * Returns a Map of Members. Map 0 is My Computers Map 1 is Friends Map 2 is
     * Connected LAN
     *
     * @return
     */
    public Map<Type, Set<Member>> getNodesMap() {

        // Split nodes into three groups:
        // 1) My Computers,
        // 2) Friends and
        // 3) Connected LAN
        // Use maps to sort by name.
        Set<Member> myComputers = new HashSet<Member>();
        Set<Member> friends = new HashSet<Member>();
        Set<Member> connectedLans = new HashSet<Member>();

        // Make a copy, so nodes are not affected.
        Set<Member> copy = new HashSet<Member>();
        copy.addAll(nodes);

        for (Iterator<Member> iterator = copy.iterator(); iterator.hasNext();) {
            Member member = iterator.next();
            // My computers should get automatically friends by
            // ServerClient.updateFriendsList(..)
            if (member.isFriend() && member.isMyComputer()) {
                myComputers.add(member);
                iterator.remove();
            }
        }

        for (Iterator<Member> iterator = copy.iterator(); iterator.hasNext();) {
            Member member = iterator.next();
            if (member.isOnLAN() && member.isCompletelyConnected()) {
                connectedLans.add(member);
                iterator.remove();
            }
        }

        for (Member member : copy) {
            if (member.isFriend()) {
                friends.add(member);
            }
        }

        Map<Type, Set<Member>> resultsMap = new TreeMap<Type, Set<Member>>();
        resultsMap.put(Type.MY_COMPUTERS_INDEX, myComputers);
        resultsMap.put(Type.FRIENDS_INDEX, friends);
        resultsMap.put(Type.CONNECTED_LAN, connectedLans);

        return resultsMap;
    }

    /**
     * Returns a count of the nodes in the three groups.
     *
     * @return
     */
    public int getSize() {
        int count = 0;
        Map<Type, Set<Member>> map = getNodesMap();
        for (Set<Member> members : map.values()) {
            count += members.size();
        }
        return count;
    }

    /**
     * @return if offline friends should be shown.
     */
    public ValueModel getShowOfflineModel() {
        return showOfflineModel;
    }

    /**
     * @return a listmodel that contains the friendslist.
     */
    public ListModel getFriendsListModel() {
        return friendsListModel;
    }

    /**
     * Update method responding to changes of nodes. Fire changed if added,
     * removed or in list.
     *
     * @param node
     */
    private void updateNode(Member node) {
        boolean changed = false;
        boolean wanted = required(node);
        if (wanted) {
            if (!nodes.contains(node)) {
                nodes.add(node);
                changed = true;
            }
        } else {
            if (nodes.contains(node)) {
                nodes.remove(node);
                changed = true;
            }
        }
        if (nodes.contains(node)) {
            changed = true;
        }
        if (changed) {
            for (NodeManagerModelListener listener : listeners) {
                listener.changed();
            }
        }
    }

    /**
     * Listens for changes in the node manager
     */
    private class MyNodeManagerListener extends NodeManagerAdapter {

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

        public void nodeOffline(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void nodeOnline(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void nodeRemoved(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MySecurityManagerListener implements SecurityManagerListener {

        public void nodeAccountStateChanged(SecurityManagerEvent event) {
            updateNode(event.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            updateNode(getController().getMySelf());
        }

        public void login(ServerClientEvent event) {
            updateNode(getController().getMySelf());
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateNode(event.getServerNode());
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void serverDisconnected(ServerClientEvent event) {
        }


        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
