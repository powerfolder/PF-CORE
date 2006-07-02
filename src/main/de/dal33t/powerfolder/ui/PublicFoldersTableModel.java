package de.dal33t.powerfolder.ui;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.event.FolderInfoFilterChangeListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.util.FolderInfoComparator;
import de.dal33t.powerfolder.util.ReverseComparator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps all public Folders to a table model
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.13 $
 */
public class PublicFoldersTableModel extends PFComponent implements
    TableModel
{

    private Collection listeners;
    private FolderRepository repository;
    private FolderInfoFilterModel folderInfoFilterModel;
    private List unJoinedFolders;
    private List displayList;

    private String[] columnHeaders = new String[]{
        Translation.getTranslation("general.folder"), // 0
        Translation.getTranslation("publicfolders.number_of_files"), // 1
        Translation.getTranslation("publicfolders.size"), // 2
        Translation.getTranslation("publicfolders.members"), // 3
        Translation.getTranslation("general.availability"), // 4
        Translation.getTranslation("publicfolders.last_activity") // 5
    };

    /**
     * The comparators for the columns, initalized in constructor
     */
    private Comparator[] columComparators = new Comparator[6];

    public PublicFoldersTableModel(FolderRepository repository,
        FolderInfoFilterModel folderInfoFilterModel)
    {
        super(repository.getController());

        this.columComparators[0] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_NAME);
        this.columComparators[1] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_NUMBER_OF_FILES);
        this.columComparators[2] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_SIZE);
        this.columComparators[3] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_NUMBER_OF_MEMBERS);
        this.columComparators[4] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_AVAILABILITY);
        this.columComparators[5] = new FolderInfoComparator(getController(),
            FolderInfoComparator.BY_MODIFIED_DATE);

        this.repository = repository;
        this.folderInfoFilterModel = folderInfoFilterModel;
        this.listeners = Collections.synchronizedList(new LinkedList());
        unJoinedFolders = repository.getUnjoinedFoldersList();
        displayList = folderInfoFilterModel.filter(unJoinedFolders);

        folderInfoFilterModel
            .addFolderInfoFilterChangeListener(new FolderInfoFilterChangeListener()
            {
                public void filterChanged(FilterChangedEvent event) {
                    synchronized (unJoinedFolders) {
                        displayList.clear();
                        displayList.addAll(event.getFilteredList());
                    }
                    sort();
                    modelChanged(new TableModelEvent(
                        PublicFoldersTableModel.this));
                }
            });
        repository
            .addFolderRepositoryListener(new MyFolderRepositoryListener());
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
        return displayList.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return displayList.get(rowIndex);
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

    /**
     * Fires an modelevent to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (Iterator it = listeners.iterator(); it.hasNext();) {
                        TableModelListener listener = (TableModelListener) it
                            .next();
                        listener.tableChanged(e);
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    private void update() {
        unJoinedFolders = folderInfoFilterModel.filter(repository
            .getUnjoinedFoldersList());

        sort();
        modelChanged(new TableModelEvent(this));
    }

    private boolean sortAscending;
    private Comparator comparator;

    /**
     * Sorts the filelist
     */
    private boolean sort() {
        if (comparator != null) {
            synchronized (displayList) {
                if (sortAscending) {
                    Collections.sort(displayList, comparator);
                } else {
                    Collections.sort(displayList, new ReverseComparator(
                        comparator));
                }
            }
            modelChanged(new TableModelEvent(this));
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
            return false;
        }
        return sortBy(columComparators[columnIndex]);
    }

    /**
     * Re-sorts the folder list with the new comparator only if comparator
     * differs from old one
     * 
     * @param newComparator
     * @return if the table was freshly sorted
     */
    public boolean sortBy(Comparator newComparator) {
        Comparator oldComparator = comparator;
        comparator = newComparator;
        if (!Util.equals(oldComparator, newComparator)) {
            return sort();
        }
        return false;
    }

    /**
     * Reverses the sorting of the table
     */
    public void reverseList() {
        sortAscending = !sortAscending;
        sort();
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
            update();
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
            update();
        }

        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void scansStarted(FolderRepositoryEvent e) {
        }

        public void scansFinished(FolderRepositoryEvent e) {
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
