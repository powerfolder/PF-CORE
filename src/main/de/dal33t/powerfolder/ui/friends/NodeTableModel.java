package de.dal33t.powerfolder.ui.friends;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.util.Translation;
/**
 * Holds the search a list of members, for a table
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 *
 */
public class NodeTableModel extends PFUIComponent implements TableModel {
    List listeners = new LinkedList();
    List members = new LinkedList();

    String[] columnNames = new String[]{Translation.getTranslation("friendsearch.nodetable.name"), 
        Translation.getTranslation("friendsearch.nodetable.last_seen_online"), 
        Translation.getTranslation("friendsearch.nodetable.hostname"),
        Translation.getTranslation("friendsearch.nodetable.ip"), 
        Translation.getTranslation("friendsearch.nodetable.on_local_network")};

    /**
     * Initalizes the node table model which contains user/nodes
     * @param controller
     */
    public NodeTableModel(Controller controller) {
        super(controller);

        // Add listener to nodemanager
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /** removes all members * */
    public void clear() {
        members.clear();
        fireModelChanged();
    }

    /** add a member */
    public void add(Member member) {
        if (!members.contains(member)) {
            members.add(member);
            fireModelChanged();
        }
    }

    /** remove a member */
    public void remove(Member member) {
        members.remove(member);
        fireModelChanged();
    }

    /** add a String (eg for "no users found" */
    public void add(String str) {
        members.add(str);
        fireModelChanged();
    }
    
    /** Remove a String (eg for "no users found") */
    public void remove(String str) {
        members.remove(str);
        fireModelChanged();
    }

    /** */
    public Object getDataAt(int rowIndex) {
        return members.get(rowIndex);

    }

    public Class getColumnClass(int columnIndex) {
        return Member.class;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public int getRowCount() {
        return members.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return members.get(rowIndex);
    }

    public void addTableModelListener(TableModelListener listener) {
        listeners.add(listener);
    }

    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }

    private void fireModelChanged() {
        TableModelEvent te = new TableModelEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            TableModelListener listener = (TableModelListener) listeners.get(i);
            listener.tableChanged(te);
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("not editable");
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Adapter between TableModel and NodeManager. Listens on changes of the
     * nodes and fires tablemodel events.
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {

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
            fireModelChanged();
        }

        public void friendRemoved(NodeManagerEvent e) {
            fireModelChanged();
        }

        public void settingsChanged(NodeManagerEvent e) {
            fireModelChanged();
        }        
    }
}
