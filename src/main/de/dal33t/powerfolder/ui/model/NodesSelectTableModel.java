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
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
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
        .getTranslation("friendsearch.nodetable.name")};

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
            return Translation.getTranslation("friendsearch.no_user_found");
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
    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeRemoved(NodeManagerEvent e) {
            Member member = e.getNode();
            if (hideOffline) {
                if (member.isConnectedToNetwork()) {
                    if (!nodes.contains(member)) {
                        nodes.add(member);
                    }
                }
            } else {
                if (!nodes.contains(member)) {
                    nodes.add(member);
                }
            }
            fireModelStructureChanged();
        }

        public void nodeAdded(NodeManagerEvent e) {
            Member member = e.getNode();
            if (member.isFriend() || member.isOnLAN()) {
                if (hideOffline) {
                    if (e.getNode().isConnectedToNetwork()) {
                        if (!nodes.contains(member)) {
                            nodes.add(member);
                        }
                    }
                } else {
                    if (!nodes.contains(member)) {
                        nodes.add(member);
                    }
                }
                fireModelStructureChanged();
            }
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().isFriend() || e.getNode().isOnLAN()) {
                Member member = e.getNode();
                if (hideOffline) {
                    if (e.getNode().isConnectedToNetwork()) {
                        if (!nodes.contains(member)) {
                            nodes.add(member);
                        }
                    }
                } else {
                    if (!nodes.contains(member)) {
                        nodes.add(member);
                    }
                }
                fireModelStructureChanged();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (hideOffline) {
                Member member = e.getNode();
                nodes.remove(member);
                fireModelStructureChanged();
            }
        }

        public void friendAdded(NodeManagerEvent e) {
            Member member = e.getNode();
            if (hideOffline) {
                if (e.getNode().isConnectedToNetwork()) {
                    if (!nodes.contains(member)) {
                        nodes.add(member);
                    }
                }
            } else {
                if (!nodes.contains(member)) {
                    nodes.add(member);
                }
            }
            fireModelStructureChanged();
        }

        public void friendRemoved(NodeManagerEvent e) {
            nodes.remove(e.getNode());
            fireModelStructureChanged();
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

}
