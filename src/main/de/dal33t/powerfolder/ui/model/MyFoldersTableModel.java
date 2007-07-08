package de.dal33t.powerfolder.ui.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectionBox;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps all joined Folders to a table model
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class MyFoldersTableModel implements TableModel {
    private Collection<TableModelListener> listeners;
    private String[] columnHeaders = new String[]{Translation.getTranslation("general.folder"), // 0
        Translation.getTranslation("myfolderstable.type"), // 1
        Translation.getTranslation("myfolderstable.sync"), // 2
        Translation.getTranslation("myfolderstable.syncprofile"), // 3
        Translation.getTranslation("myfolderstable.members"), // 4
        Translation.getTranslation("myfolderstable.number_of_local_files"), // 5
        Translation.getTranslation("myfolderstable.local_size"), // 6
        Translation.getTranslation("myfolderstable.number_of_deleted_files"), // 7
        Translation.getTranslation("myfolderstable.number_of_available_files"), // 8
        Translation.getTranslation("myfolderstable._total_number_of_files"), // 9
        Translation.getTranslation("myfolderstable.total_size")}; // 10

    // TODO: Is this a good place?
    private boolean[] defaultVisibility = new boolean[]{
        true, false, true, true, true, true, true, false, false, true, true};
         //0    1      2    3     4    5      6      7       8     9    10
    private List folders;
    private FolderRepository repository;
    private FolderListener folderListener;
    private FolderMembershipListener folderMembershipListener;

    public MyFoldersTableModel(FolderRepository repository) {
        this.listeners = Collections.synchronizedList(new LinkedList<TableModelListener>());
        this.repository = repository;
        folders = repository.getFoldersAsSortedList();
        repository
            .addFolderRepositoryListener(new MyFolderRepositoryListener());
        folderListener = new MyFolderListener();
        folderMembershipListener = new MyFolderMembershipListener();
        //add listeners to all folders
        addListeners(folders);
        
    }
    
    private void addListeners(List somefolders) {
        for (int i=0;i<somefolders.size();i++) {
            Folder folder = (Folder) somefolders.get(i);
            folder.addMembershipListener(folderMembershipListener);
            folder.addFolderListener(folderListener);
        }
    }
    
    /**
     * @return the default visibility of the columns
     */
    public boolean[] getDefaultVisibilities()  {
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
        if (columnIndex == 3) { // sync profile
            return true;
        }
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 3) {// sync profile
            // save edit
            Folder folder = (Folder) folders.get(rowIndex);
            SyncProfileSelectionBox.vetoableFolderSyncProfileChange(folder, (SyncProfile)aValue);
        } else {
            throw new IllegalStateException(
                "Unable to set value in MyFolderTableModel, not editable");
        }
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    // Helper method **********************************************************

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

    /** listens to a folder for changes **/
    private class MyFolderListener implements FolderListener{
        public void remoteContentsChanged(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));            
        }
        
        public void folderChanged(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
        }
        
        public void statisticsCalculated(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));            
        }
        
        public void syncProfileChanged(FolderEvent folderEvent) {
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
        }    
        
        public void scanResultCommited(FolderEvent folderEvent) {
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }
    }   
    
    /** listens to a folder for changes **/
    private class MyFolderMembershipListener implements FolderMembershipListener{
        public void memberJoined(FolderMembershipEvent folderEvent) {            
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
        }
        
        public void memberLeft(FolderMembershipEvent folderEvent) {            
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }
    }  
    
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void folderCreated(FolderRepositoryEvent e) {
            folders = repository.getFoldersAsSortedList();
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
            Folder folder = e.getFolder();
            folder.addFolderListener(folderListener);
            folder.addMembershipListener(folderMembershipListener);
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            folders = repository.getFoldersAsSortedList();
            modelChanged(new TableModelEvent(MyFoldersTableModel.this));
            Folder folder = e.getFolder();
            folder.removeFolderListener(folderListener);
            folder.addMembershipListener(folderMembershipListener);
        }

        public void maintenanceStarted(FolderRepositoryEvent e) { 
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
