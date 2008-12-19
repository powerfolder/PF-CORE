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
* $Id: MembersTableModel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.members;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FolderMemberComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to model a folder's members.
 * provides columns for image, name, sync status, folder size, local size.
 */
public class MembersTableModel extends PFUIComponent implements TableModel,
        SortedTableModel {

    private static final int COL_TYPE = 0;
    private static final int COL_NICK = 1;
    private static final int COL_SYNC_STATUS = 2;
    private static final int COL_LOCAL_SIZE = 3;

    private final List<Member> members;
    private final List<TableModelListener> listeners;
    private Folder folder;
    private final FolderRepository folderRepository;

    private int sortColumn = -1;
    private boolean sortAscending = true;

    private String[] columnHeaders = new String[] {
        Translation.getTranslation("folder_member_table_model.icon"), // 0
        Translation.getTranslation("folder_member_table_model.name"), // 1
        Translation.getTranslation("folder_member_table_model.sync_status"), // 2
        Translation.getTranslation("folder_member_table_model.local_size")}; // 3

    /**
     * Constructor
     *
     * @param controller
     */
    public MembersTableModel(Controller controller) {
        super(controller);

        folderRepository = controller.getFolderRepository();
        members = new ArrayList<Member>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();

        // Node changes
        NodeManager nodeManager = controller.getNodeManager();
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
    }

    /**
     * Sets model for a new folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = folderRepository.getFolder(folderInfo);
        members.clear();
        for (Member member : folder.getMembersAsCollection()) {
            members.add(member);
        }
        modelChanged(new TableModelEvent(this, 0, members.size() - 1));
    }

    /**
     * Adds a listener to the list.
     *
     * @param l
     */
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    /**
     * Removes a listener from the list.
     *
     * @param l
     */
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    /**
     * Getst the column class.
     *
     * @param columnIndex
     * @return
     */
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Member.class;
            case 1:
            case 2:
            case 3:
                return String.class;
            default:
                throw new IllegalArgumentException("columnIndex too big: "
                        + columnIndex);

        }
    }

    /**
     * Gets a count of the displayable columns.
     *
     * @return
     */
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * Getst the column header name.
     *
     * @param columnIndex
     * @return
     */
    public String getColumnName(int columnIndex) {
        return columnHeaders[columnIndex];
    }

    /**
     * Gets a count of the rows.
     *
     * @return
     */
    public int getRowCount() {
        return members.size();
    }

    /**
     * Gets a value at a specific row / column.
     *
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Member member = members.get(rowIndex);
        FolderStatistic stats = folder.getStatistic();

        if (columnIndex == 0) {
            return member;
        } else if (columnIndex == 1) {
            return member.getNick();
        } else if (columnIndex == 2) {
            double sync = stats.getSyncPercentage(member);
            return SyncProfileUtil.renderSyncPercentage(sync);
        } else if (columnIndex == 3) {
            int filesRcvd = stats.getFilesCountInSync(member);
            long bytesRcvd = stats.getSizeInSync(member);
            return filesRcvd + " "
                    + Translation.getTranslation("general.files") + " ("
                    + Format.formatBytes(bytesRcvd) + ')';
        } else {
            return 0;
        }
    }

    /**
     * Answers if cell is editable - no, it is not!
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * Not implemented - cannot set values in this model.
     *
     * @param aValue
     * @param rowIndex
     * @param columnIndex
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in MembersTableModel; not editable");
    }

    /**
     * Handle node add event.
     *
     * @param e
     */
    private void handleNodeAdded(NodeManagerEvent e) {
        try {
            check(e);
            Member member = e.getNode();
            Collection<Member> folderMembers = folder.getMembersAsCollection();
            if (folderMembers.contains(member) &&
                    !members.contains(member)) {
                members.add(member);
            }
            modelChanged(new TableModelEvent(this, 0, members.size() - 1));
        } catch (IllegalStateException ex) {
            logSevere("IllegalStateException", ex);
        }
    }

    /**
     * Handle node add event.
     *
     * @param e
     */
    private void handleNodeRemoved(NodeManagerEvent e) {
        try {
            check(e);
            Member member = e.getNode();
            Collection<Member> folderMembers = folder.getMembersAsCollection();
            if (folderMembers.contains(member)) {
                members.remove(member);
            }
            modelChanged(new TableModelEvent(this, 0, members.size() - 1));
        } catch (IllegalStateException ex) {
            logSevere("IllegalStateException", ex);
        }
    }

    /**
     * Handle node add event.
     *
     * @param e
     */
    private void handleNodeChanged(NodeManagerEvent e) {
        try {
            check(e);
            Member eventMember = e.getNode();
            Collection<Member> folderMembers = folder.getMembersAsCollection();

            if (folderMembers.contains(eventMember)) {
                // Update member.
                members.add(eventMember);
                int row = 0;
                for (Member localMember : members) {
                    if (eventMember.equals(localMember)) {

                        // Found the member.
                        modelChanged(new TableModelEvent(this, row, row));
                        return;
                    }
                    row++;
                }
            }
        } catch (IllegalStateException ex) {
            logSevere("IllegalStateException", ex);
        }
    }

    /**
     * Checks that the folder and member are valid.
     *
     * @param e
     * @throws IllegalStateException
     */
    private void check(NodeManagerEvent e) throws IllegalStateException {
        if (folder == null) {
            throw new IllegalStateException("Folder not set");
        }
        Member member = e.getNode();
        if (member == null) {
            throw new IllegalStateException("Member not set in event: " + e);
        }
    }

    /**
     * Fires a model event to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (TableModelListener listener : listeners) {
                        listener.tableChanged(e);
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Returns the sorting column.
     *
     * @return
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * Answers if sorting ascending.
     *
     * @return
     */
    public boolean isSortAscending() {
        return sortAscending;
    }

    /**
     * Sorts by this column.
     * 
     * @param columnIndex
     * @return
     */
    public boolean sortBy(int columnIndex) {
        boolean newSortColumn = sortColumn != columnIndex;
        sortColumn = columnIndex;
        switch (columnIndex) {
            case COL_TYPE :
                sortMe(FolderMemberComparator.BY_TYPE, newSortColumn);
                break;
            case COL_NICK :
                sortMe(FolderMemberComparator.BY_NICK, newSortColumn);
                break;
            case COL_SYNC_STATUS :
                sortMe(FolderMemberComparator.BY_SYNC_STATUS, newSortColumn);
                break;
            case COL_LOCAL_SIZE :
                sortMe(FolderMemberComparator.BY_LOCAL_SIZE, newSortColumn);
                break;
        }
        return true;
    }

    private void sortMe(FolderMemberComparator comparator, boolean newSortColumn) {

        if (!newSortColumn) {
            // Reverse list.
            sortAscending = !sortAscending;
        }

        comparator.setFolder(folder);

        if (sortAscending) {
            Collections.sort(members, comparator);
        } else {
            Collections.sort(members, new ReverseComparator(comparator));
        }

        modelChanged(new TableModelEvent(this, 0, members.size() - 1));
    }

    /**
     * Listener for node events
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            handleNodeAdded(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            handleNodeRemoved(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            handleNodeAdded(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            handleNodeChanged(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            handleNodeChanged(e);
        }

        public void nodeRemoved(NodeManagerEvent e) {
            handleNodeRemoved(e);
        }

        public void settingsChanged(NodeManagerEvent e) {
            handleNodeChanged(e);
        }

        public void startStop(NodeManagerEvent e) {
            // Don't care.
        }
    }
}
