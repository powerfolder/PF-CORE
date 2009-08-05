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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.SecurityManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;

/**
 * Class to model a folder's members. provides columns for image, name, sync
 * status, folder size, local size.
 */
public class MembersTableModel extends PFUIComponent implements TableModel,
    SortedTableModel
{

    public static final int COL_TYPE = 0;
    public static final int COL_COMPUTER_NAME = 1;
    public static final int COL_USERNAME = 2;
    public static final int COL_SYNC_STATUS = 3;
    public static final int COL_LOCAL_SIZE = 4;
    public static final int COL_PERMISSION = 5;

    private final List<FolderMember> members;
    private final List<TableModelListener> listeners;
    private final FolderRepository folderRepository;
    private Folder folder;

    private FolderListener folderListener;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    private String[] columnHeaders = {
        Translation.getTranslation("folder_member_table_model.icon"), // 0
        Translation.getTranslation("folder_member_table_model.name"), // 1
        Translation.getTranslation("folder_member_table_model.account"), // 2
        Translation.getTranslation("folder_member_table_model.sync_status"), // 3
        Translation.getTranslation("folder_member_table_model.local_size"), // 4
        Translation.getTranslation("folder_member_table_model.permission")}; // 5

    private FolderMemberComparator[] columnComparators = {
        FolderMemberComparator.BY_TYPE,// 0
        FolderMemberComparator.BY_COMPUTER_NAME, // 1
        FolderMemberComparator.BY_USERNAME, // 2
        FolderMemberComparator.BY_PERMISSION, // 3
        FolderMemberComparator.BY_SYNC_STATUS, // 4
        FolderMemberComparator.BY_LOCAL_SIZE}; // 5

    /**
     * Constructor
     * 
     * @param controller
     */
    public MembersTableModel(Controller controller) {
        super(controller);

        folderRepository = controller.getFolderRepository();
        members = new ArrayList<FolderMember>();
        listeners = new ArrayList<TableModelListener>();

        folderListener = new MyFolderListener();
        // Node changes
        NodeManager nodeManager = controller.getNodeManager();
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        getController().getSecurityManager().addListener(
            new MySecurityManagerListener());
    }

    /**
     * Sets model for a new folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        if (folder != null) {
            folder.removeFolderListener(folderListener);
        }
        folder = folderRepository.getFolder(folderInfo);
        folder.addFolderListener(folderListener);
        members.clear();
        for (Member member : folder.getMembersAsCollection()) {
            // TODO Default permission?
            members.add(new FolderMember(folder, member, member
                .getAccountInfo(), null, true));
        }
        modelChanged(new TableModelEvent(this, 0, members.size() - 1));
        new ModelRefresher().execute();
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
     * @param columnIndex
     * @return the column class.
     */
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0 :
                return Member.class;
            default :
                return String.class;

        }
    }

    /**
     * @return count of the displayable columns.
     */
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * @param columnIndex
     * @return the column header name.
     */
    public String getColumnName(int columnIndex) {
        return columnHeaders[columnIndex];
    }

    /**
     * @return count of the rows.
     */
    public int getRowCount() {
        return members.size();
    }

    public Member getMemberAt(int rowIndex) {
        if (rowIndex > getRowCount() - 1) {
            return null;
        }
        return members.get(rowIndex).getMember();
    }

    /**
     * @param rowIndex
     * @param columnIndex
     * @return the value at a specific row / column.
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex > getRowCount() - 1) {
            return null;
        }
        FolderMember folderMember = members.get(rowIndex);
        Member member = folderMember.getMember();
        AccountInfo aInfo = folderMember.getAccountInfo();
        FolderPermission foPermission = folderMember.getPermission();
        FolderStatistic stats = folder.getStatistic();

        if (columnIndex == COL_TYPE) {
            return folderMember;
        } else if (columnIndex == COL_COMPUTER_NAME) {
            if (member == null) {
                return new StatusText("not syncing");
            }
            return member.getNick();
        } else if (columnIndex == COL_USERNAME) {
            if (aInfo == null) {
                return new StatusText("not logged in");
            }
            return aInfo.getScrabledUsername();
        } else if (columnIndex == COL_PERMISSION) {
            if (foPermission == null) {
                return "";
            }
            if (folderMember.isDefaultPermission()) {
                return new StatusText(foPermission.getName());
            }
            return foPermission.getName();
        } else if (columnIndex == COL_SYNC_STATUS) {
            if (member == null) {
                return "";
            }
            double sync = stats.getSyncPercentage(member);
            return SyncProfileUtil.renderSyncPercentage(sync);
        } else if (columnIndex == COL_LOCAL_SIZE) {
            if (member == null) {
                return "";
            }
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
     * 
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
    private void handleNodeAdded(Member member) {
        try {
            check(member);
            Collection<Member> folderMembers = folder.getMembersAsCollection();
            if (folderMembers.contains(member) && !members.contains(member)) {
                members.add(new FolderMember(folder, member, member
                    .getAccountInfo(), null, true));
                modelChanged(new TableModelEvent(this, 0, members.size() - 1));
            }
        } catch (IllegalStateException ex) {
            logSevere("IllegalStateException", ex);
        }
    }

    /**
     * Handle node removed event.
     * 
     * @param e
     */
    private void handleNodeRemoved(Member member) {
        try {
            check(member);
            Collection<Member> folderMembers = folder.getMembersAsCollection();
            if (folderMembers.contains(member)) {
                members.remove(member);
                modelChanged(new TableModelEvent(this, 0, members.size() - 1));
            }
        } catch (IllegalStateException ex) {
            logSevere("IllegalStateException", ex);
        }
    }

    /**
     * Handle node add event.
     * 
     * @param e
     */
    private void handleNodeChanged(Member eventMember) {
        try {
            check(eventMember);
            for (int i = 0; i < members.size(); i++) {
                FolderMember localMember = members.get(i);
                if (eventMember.equals(localMember)) {
                    // Found the member.
                    modelChanged(new TableModelEvent(this, i, i));
                    return;
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
    private void check(Member member) throws IllegalStateException {
        if (folder == null) {
            throw new IllegalStateException("Folder not set");
        }
        if (member == null) {
            throw new IllegalStateException("Member not set in event");
        }
    }

    /**
     * Fires a model event to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    /**
     * @return the sorting column.
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * @return if sorting ascending.
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
        if (!newSortColumn) {
            // Reverse list.
            sortAscending = !sortAscending;
        }
        sortMe0(columnIndex);
        modelChanged(new TableModelEvent(this, 0, members.size() - 1));
        return true;
    }

    private void sortMe0(int columnIndex) {
        FolderMemberComparator comparator = columnComparators[columnIndex];
        if (comparator == null) {
            logWarning("Unknown sort column: " + columnIndex);
            return;
        }
        if (sortAscending) {
            Collections.sort(members, comparator);
        } else {
            Collections.sort(members, new ReverseComparator<FolderMember>(
                comparator));
        }
    }

    /**
     * Listener for node events
     */
    private class MyNodeManagerListener implements NodeManagerListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void startStop(NodeManagerEvent e) {
            // Don't care.
        }
    }

    private class MyFolderListener implements FolderListener,
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent event) {
            // handleNodeAdded(folderEvent.getMember());
            if (folder.getMembersAsCollection().contains(event.getMember())) {
                new ModelRefresher().execute();
            }
        }

        public void memberLeft(FolderMembershipEvent event) {
            // handleNodeRemoved(folderEvent.getMember());
            if (folder.getMembersAsCollection().contains(event.getMember())) {
                new ModelRefresher().execute();
            }
        }

        public void fileChanged(FolderEvent folderEvent) {
        }

        public void filesDeleted(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MembersTableModel.this, 0, members
                .size() - 1));
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MySecurityManagerListener implements SecurityManagerListener {

        public void nodeAccountStateChanged(SecurityManagerEvent event) {
            // handleNodeChanged(event.getNode());
            if (folder.getMembersAsCollection().contains(event.getNode())) {
                new ModelRefresher().execute();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class RefreshAction extends BaseAction {

        protected RefreshAction(Controller controller) {
            super("folder_member_table_model.refresh", controller);
        }

        public void actionPerformed(ActionEvent e) {
            new ModelRefresher().execute();
        }
    }

    private void rebuild(Map<AccountInfo, FolderPermission> permInfo,
        FolderPermission defaultPermission)
    {
        // Step 1) All computers.
        members.clear();
        for (Member member : folder.getMembersAsCollection()) {
            AccountInfo aInfo = member.getAccountInfo();
            FolderPermission folderPermission = null;
            boolean defPerm = false;
            if (aInfo != null) {
                folderPermission = permInfo.remove(aInfo);
            }
            if (folderPermission == null) {
                folderPermission = defaultPermission;
                defPerm = true;
            }
            FolderMember folderMember = new FolderMember(folder, member, aInfo,
                folderPermission, defPerm);
            members.add(folderMember);
        }

        // Step 2) All other users not joined with any computer.
        if (!permInfo.isEmpty()) {
            for (Entry<AccountInfo, FolderPermission> permissionInfo : permInfo
                .entrySet())
            {
                FolderMember folderMember = new FolderMember(folder, null,
                    permissionInfo.getKey(), permissionInfo.getValue(), false);
                members.add(folderMember);
            }
        }

        // Fresh sort
        sortMe0(sortColumn);

        modelChanged(new TableModelEvent(this, 0, getRowCount(),
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    private class ModelRefresher extends
        SwingWorker<Map<AccountInfo, FolderPermission>, Void>
    {
        private Folder refreshFor;
        private FolderPermission defaultPermission;

        @Override
        protected Map<AccountInfo, FolderPermission> doInBackground()
            throws Exception
        {
            refreshFor = folder;
            defaultPermission = getController().getSecurityManager()
                .getDefaultPermission(refreshFor.getInfo());
            return getController().getOSClient().getSecurityService()
                .getFolderPermission(refreshFor.getInfo());
        }

        @Override
        protected void done() {
            try {
                Map<AccountInfo, FolderPermission> res = get();
                if (!refreshFor.equals(folder)) {
                    // Folder has changed. discard result.
                    return;
                }
                if (res.isEmpty()) {
                    return;
                }

                rebuild(res, defaultPermission);
            } catch (InterruptedException e) {
                logWarning(e);
            } catch (ExecutionException e) {
                logWarning(e);
            }
        }
    }
}
