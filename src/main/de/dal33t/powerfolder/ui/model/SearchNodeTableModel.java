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

import java.util.Collections;
import java.util.Comparator;
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
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerListener;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * A table model which contains the search result.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class SearchNodeTableModel extends PFUIComponent implements TableModel,
    SortedTableModel
{

    private List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    private final ObservableList<Member> members = new LinkedListModel<Member>();

    /**
     * The comparators for the columns, initalized in constructor
     */
    private Comparator[] columComparators = new Comparator[COLUMN_NAMES.length];
    private boolean sortAscending;
    private int sortColumn;
    private Comparator<Member> comparator;

    private static final String[] COLUMN_NAMES = {
        Translation.get("exp.friend_search.node_table.name"),
        Translation.get("exp.friend_search.node_table.account"),
        Translation.get("exp.friend_search.node_table.last_seen_online"),
        Translation.get("exp.friend_search.node_table.ip"),
        Translation.get("exp.friend_search.node_table.on_local_network")};

    /**
     * Initalizes the node table model which contains user/nodes
     *
     * @param controller
     */
    public SearchNodeTableModel(Controller controller) {
        super(controller);

        // Add listener to nodemanager
        controller.getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        controller.getSecurityManager().addListener(
            new MySecurityManagerListener());

        members.addListDataListener(new ListModelListener());
        columComparators[0] = MemberComparator.NICK;
        columComparators[1] = MemberComparator.DISPLAY_NAME;
        columComparators[2] = MemberComparator.BY_LAST_CONNECT_DATE;
        columComparators[3] = MemberComparator.IP;
        columComparators[4] = MemberComparator.BY_CONNECTION_TYPE;
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
        logFine("add member id: '" + member.getId() + '\'');
        members.add(member);
        sort();

    }

    /**
     * Sorts the filelist
     */
    private boolean sort() {
        if (comparator != null) {
            synchronized (members) {
                if (sortAscending) {
                    Collections.sort(members, comparator);
                } else {
                    Collections
                        .sort(members, new ReverseComparator<Member>(comparator));
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
    private boolean sortBy(Comparator<Member> newComparator) {
        Comparator<Member> oldComparator = comparator;
        comparator = newComparator;
        if (!Util.equals(oldComparator, newComparator)) {
            return sort();
        }
        return false;
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
    public ObservableList<Member> getListModel() {
        return members;
    }

    // TableModel interface ***************************************************

    public int getRowCount() {
        return Math.max(members.size(), 1);
    }

    public Object getDataAt(int rowIndex) {

        if (members.isEmpty()) {
            return "";
        }
        return members.get(rowIndex);
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

    private void updateNode(Member node) {
        int row = members.indexOf(node);
        if (row < 0) {
            return;
        }
        TableModelEvent te = new TableModelEvent(this, row, row,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireTableModelEvent(te);
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

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    /**
     * Listens for changes on the listmodel and fires the appropriate table
     * model events.
     */
    private class ListModelListener implements ListDataListener {
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
    private class MyNodeManagerListener extends NodeManagerAdapter {

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

        public void friendAdded(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateNode(e.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }

    private class MySecurityManagerListener implements SecurityManagerListener {

        public void nodeAccountStateChanged(SecurityManagerEvent e) {
            updateNode(e.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }
}
