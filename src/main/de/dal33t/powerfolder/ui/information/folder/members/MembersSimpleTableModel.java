/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.problem.NoOwnerProblem;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.GroupInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.SecurityManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerListener;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * Class to model a folder's members. provides columns for image, name, sync
 * status, folder size, local size.
 */
public class MembersSimpleTableModel extends PFUIComponent implements
    TableModel, SortedTableModel
{

    static final int COL_TYPE = 0;
    static final int COL_USERNAME = 1;
    static final int COL_PERMISSION = 2;

    private static final String[] columnHeaders = {
        Translation.getTranslation("folder_member_table_model.icon"), // 0
        Translation.getTranslation("folder_member_table_model.account"), // 1
        Translation.getTranslation("folder_member_table_model.permission")}; // 2

    private static final FolderMemberComparator[] columnComparators = {
        FolderMemberComparator.BY_TYPE,// 0
        FolderMemberComparator.BY_DISPLAY_NAME, // 1
        FolderMemberComparator.BY_PERMISSION}; // 2

    private final List<FolderMember> members;
    private final List<TableModelListener> listeners;
    private final FolderRepository folderRepository;
    private Folder folder;

    private MyFolderListener folderListener;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    private ValueModel refreshingModel;
    private ValueModel permissionModel;
    private SelectionInList<FolderPermission> permissionsListModel;

    private boolean permissionsRetrieved;
    private boolean updatingDefaultPermissionModel;
    private ValueModel defaultPermissionModel;
    private SelectionInList<FolderPermission> defaultPermissionsListModel;

    /**
     * Constructor
     * 
     * @param controller
     */
    public MembersSimpleTableModel(Controller controller) {
        super(controller);

        folderRepository = controller.getFolderRepository();
        members = new ArrayList<FolderMember>();
        listeners = new ArrayList<TableModelListener>();
        refreshingModel = new ValueHolder(Boolean.FALSE, false);
        permissionModel = new ValueHolder(null, true);
        permissionsListModel = new SelectionInList<FolderPermission>();
        permissionsListModel.setSelectionHolder(permissionModel);
        defaultPermissionModel = new ValueHolder(null, true);
        defaultPermissionsListModel = new SelectionInList<FolderPermission>();
        defaultPermissionsListModel.setSelectionHolder(defaultPermissionModel);
        defaultPermissionModel
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (updatingDefaultPermissionModel) {
                        // Ignore non-user change
                        return;
                    }
                    FolderPermission newDefaultPermission = (FolderPermission) evt
                        .getNewValue();
                    refreshingModel.setValue(Boolean.TRUE);
                    new DefaultPermissionSetter(folder.getInfo(),
                        newDefaultPermission).execute();
                }
            });

        folderListener = new MyFolderListener();
        // Node changes
        NodeManager nodeManager = controller.getNodeManager();
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        getController().getSecurityManager().addListener(
            new MySecurityManagerListener());
    }

    SelectionInList<FolderPermission> getPermissionsListModel() {
        return permissionsListModel;
    }

    SelectionInList<FolderPermission> getDefaultPermissionsListModel() {
        return defaultPermissionsListModel;
    }

    FolderPermission getDefaultPermission() {
        return (FolderPermission) defaultPermissionModel.getValue();
    }

    boolean isPermissionsRetrieved() {
        return permissionsRetrieved;
    }

    public ValueModel getRefreshingModel() {
        return refreshingModel;
    }

    public FolderInfo getFolderInfo() {
        return folder != null ? folder.getInfo() : null;
    }

    /**
     * Sets model for a new folder.
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        if (folder != null) {
            folder.removeFolderListener(folderListener);
            folder.removeMembershipListener(folderListener);
        }
        folder = folderRepository.getFolder(folderInfo);
        folder.addFolderListener(folderListener);
        folder.addMembershipListener(folderListener);

        // members
        members.clear();
        for (Member member : folder.getMembersAsCollection()) {
            // TODO Default permission?
            members.add(new FolderMember(folder, member, member
                .getAccountInfo(), null, null, false));
        }
        // Fresh sort
        sortMe0(sortColumn);

        // Possible permissions
        permissionsListModel.clearSelection();
        permissionsListModel.getList().clear();

        modelChanged(new TableModelEvent(this, 0, members.size() - 1));
        refreshModel();
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

    public FolderMember getFolderMemberAt(int rowIndex) {
        if (rowIndex > getRowCount() - 1) {
            return null;
        }
        return members.get(rowIndex);
    }

    /**
     * @param columnIndex
     * @return the column class.
     */
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case COL_TYPE :
                return FolderMember.class;
            case COL_PERMISSION :
                return FolderPermission.class;
            default :
                return String.class;

        }
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
        AccountInfo aInfo = folderMember.getAccountInfo();
        GroupInfo gInfo = folderMember.getGroupInfo();

        if (columnIndex == COL_TYPE) {
            return folderMember;
        } else if (columnIndex == COL_USERNAME) {
            if (gInfo != null) {
                return gInfo;
            }

            return aInfo;
        } else if (columnIndex == COL_PERMISSION) {
            return folderMember.getPermission();
        } else {
            return 0;
        }
    }

    /**
     * @param rowIndex
     * @param columnIndex
     * @return if cell is editable - only permissions
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex != COL_PERMISSION) {
            return false;
        }
        return permissionsRetrieved
            && getFolderMemberAt(rowIndex).getAccountInfo() != null
            && !(getFolderMemberAt(rowIndex).getPermission() instanceof FolderOwnerPermission);
    }

    /**
     * Not implemented - cannot set values in this model.
     * 
     * @param aValue
     * @param rowIndex
     * @param columnIndex
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Reject.ifFalse(columnIndex == COL_PERMISSION,
            "Unable to set value in MembersTableModel; not editable");
        FolderMember folderMember = getFolderMemberAt(rowIndex);
        if (folderMember == null) {
            return;
        }
        if (folderMember.getAccountInfo() == null) {
            logSevere("Unable to set permission. No account for "
                + folderMember.getMember());
            return;
        }
        FolderPermission newPermission = (FolderPermission) aValue;
        if (!Util.equals(newPermission, folderMember.getPermission())) {
            if (newPermission instanceof FolderOwnerPermission) {
                // Show warning
                AccountInfo oldOwner = findFolderOwner();
                String oldOwnerStr = oldOwner != null
                    ? oldOwner.getDisplayName()
                    : Translation.getTranslation("folder_member.nobody");
                AccountInfo newOwner = folderMember.getAccountInfo();
                String newOwnerStr = newOwner != null
                    ? newOwner.getDisplayName()
                    : Translation.getTranslation("folder_member.nobody");
                int result = DialogFactory.genericDialog(getController(),
                    Translation
                        .getTranslation("folder_member.change_owner.title"),
                    Translation.getTranslation(
                        "folder_member.change_owner.message", oldOwnerStr,
                        newOwnerStr),
                    new String[]{
                        Translation.getTranslation("general.continue"),
                        Translation.getTranslation("general.cancel")}, 0,
                    GenericDialogType.WARN); // Default is
                // continue
                if (result != 0) { // Abort
                    return;
                }
            }
            new PermissionSetter(folderMember.getAccountInfo(), newPermission)
                .execute();
        }
    }

    private AccountInfo findFolderOwner() {
        for (FolderMember folderMember : members) {
            if (folderMember.getPermission() instanceof FolderOwnerPermission) {
                return folderMember.getAccountInfo();
            }
        }
        return null;
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

    void refreshModel() {
        refreshingModel.setValue(Boolean.TRUE);
        if (getController().getOSClient().isLoggedIn()) {
            new ModelRefresher().execute();
        } else {
            permissionsRetrieved = false;
            rebuild(new HashMap<Serializable, FolderPermission>(), null);
            refreshingModel.setValue(Boolean.FALSE);
        }
    }

    // Helpers ****************************************************************

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
     * Sorts by this column.
     * 
     * @param columnIndex
     * @return always tru.
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
    private class MyNodeManagerListener extends NodeManagerAdapter {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
            if (getController().getOSClient().isClusterServer(e.getNode())) {
                refreshModel();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            handleNodeChanged(e.getNode());
        }
    }

    private class MyFolderListener extends FolderAdapter implements
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent event) {
            // handleNodeAdded(folderEvent.getMember());
            refreshModel();
        }

        public void memberLeft(FolderMembershipEvent event) {
            // handleNodeRemoved(folderEvent.getMember());
            refreshModel();
        }
        public void statisticsCalculated(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MembersSimpleTableModel.this, 0,
                members.size() - 1));
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MySecurityManagerListener implements SecurityManagerListener {

        public void nodeAccountStateChanged(SecurityManagerEvent event) {
            // handleNodeChanged(event.getNode());
            if (folder.getMembersAsCollection().contains(event.getNode())) {
                refreshModel();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private void rebuild(Map<Serializable, FolderPermission> permInfo,
        FolderPermission defaultPermission)
    {
        // Step 1) All computers.
        members.clear();
        
        // Step 2) All other users not joined with any computer.
        if (!permInfo.isEmpty()) {
            for (Entry<Serializable, FolderPermission> permissionInfo : permInfo
                .entrySet())
            {
                FolderMember folderMember = null;
                if (permissionInfo.getKey() instanceof AccountInfo) {
                    folderMember = new FolderMember(folder, null,
                        (AccountInfo)permissionInfo.getKey(), null,
                        permissionInfo.getValue(), false);
                }
                else if (permissionInfo.getKey() instanceof GroupInfo) {
                    folderMember = new FolderMember(folder, null, null,
                        (GroupInfo)permissionInfo.getKey(),
                        permissionInfo.getValue(), false);
                }

                members.add(folderMember);
            }
        }

        // Step 3) Possible permissions
        permissionsListModel.clearSelection();
        permissionsListModel.getList().clear();
        if (permissionsRetrieved) {
            // Use default
            permissionsListModel.getList().add(null);
            permissionsListModel.getList().add(
                FolderPermission.read(folder.getInfo()));
            permissionsListModel.getList().add(
                FolderPermission.readWrite(folder.getInfo()));
            if (ConfigurationEntry.SECURITY_PERMISSIONS_SHOW_FOLDER_ADMIN.getValueBoolean(getController())) {
                permissionsListModel.getList().add(
                    FolderPermission.admin(folder.getInfo()));
            }
            if (getController().getOSClient().getAccount()
                .hasOwnerPermission(folder.getInfo()))
            {
                permissionsListModel.getList().add(
                    FolderPermission.owner(folder.getInfo()));
            }
        }

        updatingDefaultPermissionModel = true;
        defaultPermissionsListModel.clearSelection();
        defaultPermissionsListModel.getList().clear();
        if (permissionsRetrieved) {
            // No access
            defaultPermissionsListModel.getList().add(null);
            defaultPermissionsListModel.getList().add(
                FolderPermission.read(folder.getInfo()));
            defaultPermissionsListModel.getList().add(
                FolderPermission.readWrite(folder.getInfo()));
            if (ConfigurationEntry.SECURITY_PERMISSIONS_SHOW_FOLDER_ADMIN.getValueBoolean(getController())) {
                defaultPermissionsListModel.getList().add(
                    FolderPermission.admin(folder.getInfo()));
            }
            defaultPermissionModel.setValue(defaultPermission);
        }
        updatingDefaultPermissionModel = false;

        // Fresh sort
        sortMe0(sortColumn);

        modelChanged(new TableModelEvent(this, 0, getRowCount(),
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class PermissionSetter extends SwingWorker<Void, Void> {
        private AccountInfo aInfo;
        private FolderPermission newPermission;

        public PermissionSetter(AccountInfo aInfo,
            FolderPermission newPermission)
        {
            super();
            Reject.ifNull(aInfo, "AccountInfo is null");
            this.aInfo = aInfo;
            this.newPermission = newPermission;
        }

        @Override
        protected Void doInBackground() throws Exception {
            getController().getOSClient().getSecurityService()
                .setFolderPermission(aInfo, folder.getInfo(), newPermission);
            return null;
        }

        @Override
        protected void done() {
            refreshModel();
        }
    }

    private class DefaultPermissionSetter extends SwingWorker<Void, Void> {
        private FolderInfo folderInfo;
        private FolderPermission newPermission;

        public DefaultPermissionSetter(FolderInfo foInfo,
            FolderPermission newPermission)
        {
            super();
            Reject.ifNull(foInfo, "Folder info is null");
            this.folderInfo = foInfo;
            this.newPermission = newPermission;
        }

        @Override
        protected Void doInBackground() throws Exception {
            logInfo("Setting new default permission: " + newPermission);
            getController().getOSClient().getSecurityService()
                .setDefaultPermission(folderInfo, newPermission);

            getController().getFolderRepository()
                .triggerSynchronizeAllFolderMemberships();
            return null;
        }

        @Override
        protected void done() {
            refreshModel();
        }
    }

    private class ModelRefresher extends
        SwingWorker<Map<Serializable, FolderPermission>, Void>
    {
        private Folder refreshFor;
        private FolderPermission defaultPermission;

        @Override
        protected Map<Serializable, FolderPermission> doInBackground()
            throws Exception
        {
            refreshFor = folder;
            defaultPermission = getController().getOSClient()
                .getSecurityService().getDefaultPermission(folder.getInfo());

            String serverVersion  = getController().getOSClient().getServer().getIdentity().getProgramVersion();
            String featureVersion = "9.0.0";

            // TODO: remove in the future
            if (Util.compareVersions(serverVersion, featureVersion)) {
                return getController().getOSClient().getSecurityService()
                    .getAllFolderPermissions(refreshFor.getInfo());
            }
            else {
                Map<AccountInfo, FolderPermission> perm = getController().getOSClient()
                    .getSecurityService().getFolderPermissions(refreshFor.getInfo());
                Map<Serializable, FolderPermission> permissionMap = new HashMap<Serializable, FolderPermission>(perm.size());

                for (Entry<AccountInfo, FolderPermission> entry : perm.entrySet()) {
                    permissionMap.put(entry.getKey(), entry.getValue());
                }

                return permissionMap;
            }
        }

        @Override
        protected void done() {
            try {
                Map<Serializable, FolderPermission> res = get();
                logFine("Returned " + res);
                if (!refreshFor.equals(folder)) {
                    // Folder has changed. discard result.
                    logFine("Folder has changed. discard result.");
                    return;
                }

                // TODO Find a better place to check this:
                if (NoOwnerProblem.hasOwner(res)) {
                    for (Problem p : folder.getProblems()) {
                        if (p instanceof NoOwnerProblem) {
                            folder.removeProblem(p);
                        }
                    }
                } else {
                    NoOwnerProblem problem = new NoOwnerProblem(
                        folder.getInfo());
                    if (!folder.getProblems().contains(problem)) {
                        folder.addProblem(new NoOwnerProblem(folder.getInfo()));
                    }
                }

                permissionsRetrieved = true;
                rebuild(res, defaultPermission);
            } catch (Exception e) {
                logWarning(e.toString());
                permissionsRetrieved = false;
                rebuild(new HashMap<Serializable, FolderPermission>(), null);
            } finally {
                refreshingModel.setValue(Boolean.FALSE);
            }
        }
    }
}
