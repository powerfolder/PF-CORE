package de.dal33t.powerfolder.ui.model;

import java.util.*;

import javax.swing.event.*;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A table model which contains nodes that are friends, may or may not hideOffline friends.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FriendsNodeTableModel extends PFUIComponent implements TableModel,
SortedTableModel {
    private List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    private List<Member> friends = new ArrayList<Member>();
    private boolean hideOffline = false;
    private boolean includeLan = false;
    private boolean sortAscending = true;
    private int sortColumn;
    private Comparator comparator;

    /**
     * The comparators for the columns, initalized in constructor
     */
    private Comparator[] columComparators = new Comparator[COLUMN_NAMES.length];

    private static final String[] COLUMN_NAMES = new String[]{
        Translation.getTranslation("friendsearch.nodetable.name"),
        Translation.getTranslation("friendsearch.nodetable.last_seen_online"),
        Translation.getTranslation("friendsearch.nodetable.ip"),
        Translation.getTranslation("friendsearch.nodetable.on_local_network")};

    /**
     * Initalizes the node table model which contains user/nodes
     * 
     * @param controller
     */
    public FriendsNodeTableModel(Controller controller) {
        super(controller);

        // Add listener to nodemanager
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        columComparators[0] = MemberComparator.NICK;
        columComparators[1] = MemberComparator.BY_LAST_CONNECT_DATE;
        columComparators[2] = MemberComparator.IP;
        columComparators[3] = MemberComparator.BY_CONNECTION_TYPE;
        reset();
    }

    /**
     * Clear list and repopulate with freinds and connected nodes.
     */
    private void reset() {
        friends.clear();

        Member[] allFriends = getController().getNodeManager().getFriends();
        for (Member friend : allFriends) {
            addNode(friend);
        }

        Collection<Member> connectedNodes = getController().getNodeManager().getConnectedNodes();
        for (Member connectedNode : connectedNodes) {
            addNode(connectedNode);
        }

        sort();
    }

    public void setIncludeLan(boolean include) {
        if (include != includeLan) {
            includeLan = include;
            reset();
            fireModelStructureChanged();
        }
    }
                
    public void setHideOffline(boolean hide) {
        if (hide != hideOffline) {
            hideOffline = hide;
            reset();
            fireModelStructureChanged();
        }
    }

    /**
     * Sorts the filelist
     */
    private boolean sort() {
        if (comparator != null) {
            synchronized (friends) {
                if (sortAscending) {
                    Collections.sort(friends, comparator);
                } else {
                    Collections
                        .sort(friends, new ReverseComparator(comparator));
                }
            }
            fireModelChanged();
            return true;
        }
        return false;
    }

    /**
     * Sorts the model by a column
     * 
     * @param columnIndex
     * @return if the model was sorted freshly
     */
    public boolean sortBy(int columnIndex) {
        // Do not sort if no comparator given for that column
        if (columnIndex < 0 && columnIndex > columComparators.length
            || columComparators[columnIndex] == null)
        {
            comparator = null;
            sortColumn = -1;
            return false;
        }
        sortColumn = columnIndex;
        return sortBy(columComparators[columnIndex]);
    }

    /**
     * Reverses the sorting of the table
     */
    public void reverseList() {
        sortAscending = !sortAscending;
        sort();
    }

    /**
     * Re-sorts the folder list with the new comparator only if comparator
     * differs from old one
     * 
     * @param newComparator
     * @return if the table was freshly sorted
     */
    private boolean sortBy(Comparator newComparator) {
        Comparator oldComparator = comparator;
        comparator = newComparator;
        if (!Util.equals(oldComparator, newComparator)) {
            return sort();
        }
        return false;
    }
   
    // TableModel interface ***************************************************

    public int getRowCount() {        
        return Math.max(friends.size(), 1);
    }

    public Object getDataAt(int rowIndex) {
        if (friends.isEmpty()) {
            return Translation.getTranslation("friendsearch.no_user_found");
        }
        return friends.get(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getDataAt(rowIndex);
    }

    public Class<Member> getColumnClass(int columnIndex) {
        return Member.class;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("not editable");
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void addTableModelListener(TableModelListener listener) {
        listeners.add(listener);
    }

    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }

    private void fireModelChanged() {
        TableModelEvent te = new TableModelEvent(this, 0, getRowCount(),
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireTableModelEvent(te);
    }

    private void fireModelStructureChanged() {
        TableModelEvent te = new TableModelEvent(this);
        fireTableModelEvent(te);
    }

    private void fireTableModelEvent(final TableModelEvent te) {
        Runnable runner = new Runnable() {
            public void run() {
                for (TableModelListener listener : listeners) {
                    listener.tableChanged(te);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    private void removeNode(Member node) {

        // Do not remove if not in list
        if (!friends.contains(node)) {
            return;
        }

        // Remove if not wanted
        if (!wantedNode(node)) {
            friends.remove(node);
        }

        // Fire anyway incase it is a status change.
        fireModelStructureChanged();
    }

    private void addNode(Member node) {

        // Do not add if already in list
        if (friends.contains(node)) {
            return;
        }

        // Add if wanted
        if (wantedNode(node)) {
            friends.add(node);
            sort();
        }

        // Fire anyway incase it is a status change.
        fireModelStructureChanged();
    }

    /**
     * Returns true if the node is wanted,
     * that is, if it should be in the table.
     *
     * @param node
     * @return
     */
    private boolean wantedNode(Member node) {

        boolean friend = node.isFriend();
        boolean connected = node.isConnectedToNetwork();
        boolean onLan = node.isOnLAN();

        if (hideOffline) {
            if (includeLan) {
                // want online (friends or lan users)
                if (connected && (friend || onLan)) {
                    return true;
                }
            } else {
                // want online friends
                if (connected && friend) {
                    return true;
                }
            }
        } else {
            if (includeLan) {
                // want friends or lan users
                if (friend || onLan) {
                    return true;
                }
            } else {
                // want friends
                if (friend) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adapter between TableModel and NodeManager. Listens on changes of the
     * nodes and fires tablemodel events.
     */
    private final class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            removeNode(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
            addNode(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            addNode(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            removeNode(e.getNode());
        }

        public void friendAdded(NodeManagerEvent e) {
            addNode(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            removeNode(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            fireModelChanged();
        }
        
        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }
}
