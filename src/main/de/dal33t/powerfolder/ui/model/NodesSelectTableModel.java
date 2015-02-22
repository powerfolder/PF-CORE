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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.MemberComparator;

/**
 * Class to hole selected users.
 */
public class NodesSelectTableModel implements TableModel {

    private final Controller controller;
    private final List<Member> nodes = new ArrayList<Member>();
    private boolean hideOffline;
    private final List<TableModelListener> listeners = new LinkedList<TableModelListener>();

    private static final String[] COLUMN_NAMES = new String[]{Translation
        .get("friend_search.node_table.name")};

    public NodesSelectTableModel(Controller controller) {
        this.controller = controller;
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        reset();
    }

    /**
     * Clear all users and reload.
     */
    private void reset() {
        nodes.clear();
        Member[] allFriends = controller.getNodeManager().getFriends();
        for (Member friend : allFriends) {
            if (hideOffline) {
                if (friend.isConnectedToNetwork()) {
                    nodes.add(friend);
                }
            } else {
                nodes.add(friend);
            }
        }
        for (Member node : controller.getNodeManager().getConnectedNodes()) {
            if (node.isOnLAN() && !nodes.contains(node)) {
                nodes.add(node);
            }
        }
        Collections.sort(nodes, MemberComparator.IN_GUI);
        fireModelStructureChanged();
    }

    /**
     * Hide / show offline users in table.
     *
     * @param hide
     */
    public void setHideOffline(boolean hide) {
        boolean old = hideOffline;
        hideOffline = hide;
        if (old != hideOffline) {
            reset();
        }
    }

    // TableModel interface ***************************************************

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return Member.class;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    public int getRowCount() {
        return Math.max(nodes.size(), 1);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (nodes.isEmpty()) {
            return Translation
                .get("exp.friend_search.no_computers_found");
        }
        int i = 0;
        for (Member node : nodes) {
            if (i++ == rowIndex) {
                return node;
            }
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("not editable");
    }

    private void fireModelStructureChanged() {
        TableModelEvent te = new TableModelEvent(this);
        fireTableModelEvent(te);
    }

    private void fireTableModelEvent(final TableModelEvent te) {
        for (int i = 0; i < listeners.size(); i++) {
            TableModelListener listener = listeners.get(i);
            listener.tableChanged(te);
        }
    }

    /**
     * Adapter between TableModel and NodeManager. Listens on changes to nodes
     * and fires events.
     */
    private class MyNodeManagerListener extends NodeManagerAdapter {

        public void nodeRemoved(NodeManagerEvent e) {
            if (nodes.remove(e.getNode())) {
                fireModelStructureChanged();
            }
        }

        public void nodeAdded(NodeManagerEvent e) {
            addNodeIfRequired(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            addNodeIfRequired(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (nodes.remove(e.getNode())) {
                fireModelStructureChanged();
            }
        }

        public void nodeOffline(NodeManagerEvent e) {
            addNodeIfRequired(e.getNode());
        }

        public void nodeOnline(NodeManagerEvent e) {
            addNodeIfRequired(e.getNode());
        }

        public void friendAdded(NodeManagerEvent e) {
            addNodeIfRequired(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            if (nodes.remove(e.getNode())) {
                fireModelStructureChanged();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

        private void addNodeIfRequired(Member node) {
            if (node.isFriend()
                || (node.isOnLAN() && node.isCompletelyConnected()))
            {
                if (hideOffline) {
                    if (node.isConnectedToNetwork()) {
                        if (!nodes.contains(node)) {
                            nodes.add(node);
                        }
                    }
                } else {
                    if (!nodes.contains(node)) {
                        nodes.add(node);
                    }
                }
                // Always fire!
                fireModelStructureChanged();
            }
        }
    }
}
