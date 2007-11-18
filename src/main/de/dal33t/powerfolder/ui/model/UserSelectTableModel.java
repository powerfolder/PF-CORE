package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to hole selected users.
 */
public class UserSelectTableModel implements TableModel {

    private final Controller controller;
    private final List<Member> friends = new ArrayList<Member>();
    private boolean hideOffline;
    private final List<TableModelListener> listeners = new LinkedList<TableModelListener>();

    private static final String[] COLUMN_NAMES = new String[] {
            Translation.getTranslation("friendsearch.nodetable.name")};

    public UserSelectTableModel(Controller controller) {
        this.controller = controller;
        controller.getNodeManager().addNodeManagerListener(new MyNodeManagerListener());
        reset();
    }

    /**
     * Clear all users and reload.
     */
    private void reset() {
        friends.clear();
        Member[] allFriends = controller.getNodeManager().getFriends();
        for (Member friend : allFriends) {
            if (hideOffline) {
                if (friend.isConnectedToNetwork()) {
                    friends.add(friend);
                }
            } else {
                friends.add(friend);
            }
        }
        fireModelStructureChanged();
    }

    /**
     * Hide / show offline users in table.
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
        return Math.max(friends.size(), 1);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (friends.isEmpty()) {
            return Translation.getTranslation("friendsearch.no_user_found");
        }
        int i = 0;
        for (Member friend : friends) {
            if (i++ == rowIndex) {
                return friend;
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
        Runnable runner = new Runnable() {
            public void run() {
                for (int i = 0; i < listeners.size(); i++) {
                    TableModelListener listener = listeners.get(i);
                    listener.tableChanged(te);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Adapter between TableModel and NodeManager.
     * Listens on changes to nodes and fires events.
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeRemoved(NodeManagerEvent e) {
            Member member = e.getNode();
            if (hideOffline) {
                if (member.isConnectedToNetwork()) {
                    if (!friends.contains(member)) {
                        friends.add(member);
                    }
                }
            } else {
                if (!friends.contains(member)) {
                    friends.add(member);
                }
            }
            fireModelStructureChanged();
        }

        public void nodeAdded(NodeManagerEvent e) {
            Member member = e.getNode();
            if (member.isFriend()) {
                if (hideOffline) {
                    if (e.getNode().isConnectedToNetwork()) {
                        if (!friends.contains(member)) {
                            friends.add(member);
                        }
                    }
                } else {
                    if (!friends.contains(member)) {
                        friends.add(member);
                    }
                }
                fireModelStructureChanged();
            }
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().isFriend()) {
                Member member = e.getNode();
                if (hideOffline) {
                    if (e.getNode().isConnectedToNetwork()) {
                        if (!friends.contains(member)) {
                            friends.add(member);
                        }
                    }
                } else {
                    if (!friends.contains(member)) {
                        friends.add(member);
                    }
                }
                fireModelStructureChanged();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (hideOffline) {
                Member member = e.getNode();
                friends.remove(member);
                fireModelStructureChanged();
            }
        }

        public void friendAdded(NodeManagerEvent e) {
            Member member = e.getNode();
            if (hideOffline) {
                if (e.getNode().isConnectedToNetwork()) {
                    if (!friends.contains(member)) {
                        friends.add(member);
                    }
                }
            } else {
                if (!friends.contains(member)) {
                    friends.add(member);
                }
            }
            fireModelStructureChanged();
        }

        public void friendRemoved(NodeManagerEvent e) {
            friends.remove(e.getNode());
            fireModelStructureChanged();
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

}
