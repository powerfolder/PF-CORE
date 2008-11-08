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

import java.util.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;

/**
 * Maps all joined Folders to a table model
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class FoldersTableModel extends PFUIComponent implements TableModel {
    private static final long UPDATE_TIME_MS = 300;

    private final Collection<TableModelListener> listeners;
    private String[] columnHeaders = new String[]{
        Translation.getTranslation("general.folder"), // 0
        Translation.getTranslation("myfolderstable.sync"), // 1
        Translation.getTranslation("myfolderstable.syncprofile"), // 2
        Translation.getTranslation("myfolderstable.members"), // 3
        Translation.getTranslation("myfolderstable.number_of_local_files"), // 4
        Translation.getTranslation("myfolderstable.local_size"), // 5
        Translation.getTranslation("myfolderstable.number_of_incoming_files"), // 6
        Translation.getTranslation("myfolderstable._total_number_of_files"), // 7
        Translation.getTranslation("myfolderstable.total_size"), // 8
        Translation.getTranslation("myfolderstable.new_files")}; // 9

    // TODO: Is this a good place?
    private boolean[] defaultVisibility = new boolean[]{true, true, true, true,
        true, true, false, true, true, true};
    // 0 1 2 3 4 5 6 7 8 9
    private final List<Folder> folders;
    private FolderRepository repository;
    private FolderListener folderListener;
    private FolderMembershipListener folderMembershipListener;

    public FoldersTableModel(FolderRepository repository, Controller controller)
    {
        super(controller);
        this.listeners = Collections
            .synchronizedList(new LinkedList<TableModelListener>());
        this.repository = repository;
        folders = filterPreviews();
        repository
            .addFolderRepositoryListener(new MyFolderRepositoryListener());
        folderListener = new MyFolderListener();
        folderMembershipListener = new MyFolderMembershipListener();
        // add listeners to all folders
        addListeners(folders);
        repository.getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListner());
        repository.getController().scheduleAndRepeat(new UpdateTask(),
            UPDATE_TIME_MS);
    }

    private List<Folder> filterPreviews() {
        List<Folder> folders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (!hideFolder(folder)) {
                folders.add(folder);
            }
        }
        Collections.sort(folders, FolderComparator.INSTANCE);
        return folders;
    }

    private void addListeners(List<Folder> somefolders) {
        for (Folder folder : somefolders) {
            folder.addMembershipListener(folderMembershipListener);
            folder.addFolderListener(folderListener);
        }
    }

    /**
     * @return the default visibility of the columns
     */
    public boolean[] getDefaultVisibilities() {
        return defaultVisibility;
    }

    public Class getColumnClass(int columnIndex) {
        return Folder.class;
    }

    public int getColumnCount() {
        return columnHeaders.length;
    }

    public String getColumnName(int columnIndex) {
        return columnHeaders[columnIndex];
    }

    public int getRowCount() {
        return folders.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return folders.get(rowIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in MyFolderTableModel, not editable");
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    // Helper method **********************************************************

    private void fireFullModelChanged() {
        modelChanged(new TableModelEvent(this, 0, getRowCount() - 1));
    }

    /**
     * Fires an modelevent to all listeners, that model has changed
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

    /** listens to a folder for changes * */
    private class MyFolderListener extends FolderAdapter {
        public void remoteContentsChanged(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public void fileChanged(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public void filesDeleted(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            fireFullModelChanged();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /** listens to a folder for changes * */
    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            fireFullModelChanged();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            fireFullModelChanged();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    public void folderStructureChanged() {
        synchronized (folders) {
            List<Folder> list = new ArrayList<Folder>(repository
                .getFoldersAsCollection());
            Collections.sort(list, FolderComparator.INSTANCE);
            for (Folder folder : list) {
                if (folders.contains(folder) && hideFolder(folder)) {
                    removeFolder(folder);
                } else if (!folders.contains(folder) && !hideFolder(folder)) {
                    addFolder(folder);
                }
            }
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void folderCreated(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            if (!folders.contains(folder) && !hideFolder(folder)) {
                addFolder(folder);
            }
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            Folder folder = e.getFolder();
            if (folders.contains(folder)) {
                removeFolder(folder);
            }
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyNodeManagerListner extends NodeManagerAdapter {

        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().hasJoinedAnyFolder()) {
                fireFullModelChanged();
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (e.getNode().hasJoinedAnyFolder()) {
                fireFullModelChanged();
            }
        }

    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            if (repository.isAnyFolderTransferring()
                || repository.getFolderScanner().getCurrentScanningFolder() != null)
            {
                fireFullModelChanged();
            }
        }
    }

    /**
     * Only show folders if not preview or show preview config is true.
     * 
     * @param folder
     * @return
     */
    private boolean hideFolder(Folder folder) {
        return folder.isPreviewOnly()
            && ConfigurationEntry.HIDE_PREVIEW_FOLDERS
                .getValueBoolean(getController());
    }

    private void addFolder(Folder folder) {
        folders.add(folder);
        Collections.sort(folders, FolderComparator.INSTANCE);
        folder.addFolderListener(folderListener);
        folder.addMembershipListener(folderMembershipListener);
        modelChanged(new TableModelEvent(this));
    }

    private void removeFolder(Folder folder) {
        folders.remove(folder);
        folder.removeFolderListener(folderListener);
        folder.removeMembershipListener(folderMembershipListener);
        modelChanged(new TableModelEvent(this));
    }

}
