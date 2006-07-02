package de.dal33t.powerfolder.ui.model;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.jgoodies.binding.list.LinkedListModel;
import com.jgoodies.binding.list.ObservableList;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A general table model which contains nodes.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class NodeTableModel extends PFUIComponent implements TableModel {
    private List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    private ObservableList members = new LinkedListModel();

    private static final String[] COLUMN_NAMES = new String[]{
        Translation.getTranslation("friendsearch.nodetable.name"),
        Translation.getTranslation("friendsearch.nodetable.last_seen_online"),
        Translation.getTranslation("friendsearch.nodetable.hostname"),
        Translation.getTranslation("friendsearch.nodetable.ip"),
        Translation.getTranslation("friendsearch.nodetable.on_local_network")};

    /**
     * Initalizes the node table model which contains user/nodes
     * 
     * @param controller
     */
    public NodeTableModel(Controller controller) {
        super(controller);

        // Add listener to nodemanager
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        members.addListDataListener(new ListModelListener());
    }

    /**
     * Clears the model and displays "No users found" text in the first row.
     */
    public void clear() {
        members.clear();
    }

    /** add a member */
    public void add(Member member) {
        Reject.ifNull(member, "Member is null");
        members.add(member);
    }

    /**
     * @param member
     * @return if the member is contained in the model.
     */
    public boolean contains(Member member) {
        return members.contains(member);
    }

    /** removes the first occurence of a member */
    public void remove(Member member) {
        Reject.ifNull(member, "Member is null");
        members.remove(member);
    }

    /**
     * @return if there are no users found
     */
    public boolean containsNoUsers() {
        return members.isEmpty();
    }

    // Exposing ***************************************************************

    /**
     * Returns the listmodel containing the nodes of the tablemodel. Changes in
     * the model will be reflected in the list.
     * 
     * @return the listmodel containing the nodes.
     */
    public ObservableList getListModel() {
        return members;
    }

    // TableModel interface ***************************************************

    public int getRowCount() {
        return Math.max(members.size(), 1);
    }

    public Object getDataAt(int rowIndex) {
        if (members.isEmpty()) {
            return Translation.getTranslation("friendsearch.no_user_found");
        }
        return members.get(rowIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getDataAt(rowIndex);
    }

    public Class getColumnClass(int columnIndex) {
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
                for (int i = 0; i < listeners.size(); i++) {
                    TableModelListener listener = listeners.get(i);
                    listener.tableChanged(te);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Listens for changes on the listmodel and fires the appropriate table
     * model events.
     */
    private final class ListModelListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            fireModelStructureChanged();
        }

        public void intervalRemoved(ListDataEvent e) {
            fireModelStructureChanged();
        }

        public void contentsChanged(ListDataEvent e) {
            fireModelChanged();
        }
    }

    /**
     * Adapter between TableModel and NodeManager. Listens on changes of the
     * nodes and fires tablemodel events.
     */
    private final class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            fireModelChanged();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            fireModelChanged();
        }

        public void friendAdded(NodeManagerEvent e) {
            fireModelStructureChanged();
        }

        public void friendRemoved(NodeManagerEvent e) {
            fireModelStructureChanged();
        }

        public void settingsChanged(NodeManagerEvent e) {
            fireModelChanged();
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
