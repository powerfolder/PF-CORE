package de.dal33t.powerfolder.ui.folder;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps a Directory to a tablemodel, optional uses a recursive list (all the
 * files in the sub directories), uses a FileFilter model to filter the file
 * list. If not recursive it also maps the Directories as rows in the table.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class DirectoryTableModel extends PFComponent implements TableModel {

    private Set<TableModelListener> tableListener = new HashSet<TableModelListener>();
    private Directory directory;

    private int fileInfoComparatorType = -1;

    private boolean sortAscending = true;

    private String[] columns = new String[]{"",
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.size"),
        Translation.getTranslation("filelist.modifiedby"),
        Translation.getTranslation("filelist.date"),
        Translation.getTranslation("filelist.availability")};

    private boolean recursive;
    private List displayList = Collections.synchronizedList(new ArrayList());
    private FileFilterModel fileFilterModel;
    private DirectoryTable table;
    /**
     * The selections that need to be restored if they are, after filtering this
     * directory, still present.
     */
    private Object[] oldSelections;

    private Folder getFolder() {
        if (directory != null) {
            return directory.getRootFolder();
        }
        return null;
    }

    public DirectoryTableModel(FileFilterModel fileFilterModel) {
        this.fileFilterModel = fileFilterModel;
        fileFilterModel
            .addFileFilterChangeListener(new FileFilterChangeListener() {
                public void filterChanged(FilterChangedEvent event) {
                    List filterdList = event.getFilteredList();
                    sort(filterdList);
                    synchronized (displayList) {
                        displayList = filterdList;
                    }
                    fireModelChanged();
                    Runnable runner = new Runnable() {
                        public void run() {
                            if (oldSelections != null) {
                                // retreive the current indexes of the objects
                                // that where seleted before
                                List<Integer> selectionInt = new ArrayList<Integer>();
                                for (Object object : oldSelections) {
                                    int index = displayList.indexOf(object);
                                    if (index != -1) {
                                        selectionInt.add(index);
                                    }
                                }
                                ListSelectionModel model = table
                                    .getSelectionModel();
                                model.clearSelection();
                                for (int index : selectionInt) {
                                    model.addSelectionInterval(index, index);
                                }
                                // scroll to make the first selection visible
                                if (!selectionInt.isEmpty()) {
                                    table
                                        .scrollToCenter(selectionInt.get(0), 0);
                                }
                            }
                        }
                    };
                    if (EventQueue.isDispatchThread()) {
                        runner.run();
                    } else {
                        EventQueue.invokeLater(runner);
                    }
                }

                public void countChanged(FilterChangedEvent event) {
                }
            });
    }

    public void setTable(DirectoryTable table) {
        this.table = table;

    }

    public int getComparatorType() {
        return fileInfoComparatorType;
    }

    /**
     * Display files in subdirectories?
     *
     * @return true if this is a recursive view on the Directory
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Set if this model should map files in subdirectries to the table model
     *
     * @param recursive
     *            enables of disables the recursive view
     * @param createList
     *            create a new display list now
     */
    public void setRecursive(boolean recursive, boolean createList) {
        if (recursive != this.recursive) {
            this.recursive = recursive;
            if (createList && directory != null) {
                createDisplayList();
            }
        }
    }

    /**
     * Set the Directory that should be visible in the Table
     *
     * @param directory
     *            the Directory to use
     * @param clear
     *            immediately clear the display list (eg use if new Folder is
     *            set)
     * @param oldSelections
     *            The selections that need to be restored if they are, after
     *            filtering this directory, still present.
     */
    public void setDirectory(Directory directory, boolean clear,
        Object[] oldSelections)
    {
        this.directory = directory;
        this.oldSelections = oldSelections;
        if (clear) {
            synchronized (displayList) {
                displayList.clear();
            }
            fireModelChanged();
        }
        createDisplayList();
    }

    Directory getDirectory() {
        return directory;
    }

    private void createDisplayList() {
        List allFiles;
        if (recursive) {
            allFiles = directory.getFilesRecursive();
        } else {
            allFiles = directory.getValidFiles();
        }
        if (!recursive) { // add the subdirectories
            allFiles.addAll(0, directory.listSubDirectories());
        }
        fileFilterModel.filter(directory.getRootFolder(), allFiles);
    }

    public Class getColumnClass(int columnIndex) {
        return Directory.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        synchronized (displayList) {
            if (displayList.isEmpty()) {
                return 1; // a row for the status message
            }
            return displayList.size();
        }
    }

    /** -1 if no such object */
    public int getIndexOf(Object object) {
        return displayList.indexOf(object);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (displayList) {
            if (rowIndex == 0 && displayList.isEmpty()) {
                // status line
                Folder folder = getFolder();
                FolderInfo folderInfo = folder.getInfo();
                Member[] members = folder.getMembers();
                for (Member member : members) {
                    if (member.isConnected()
                        && !member.hasFileListFor(folderInfo))
                    {
                        // if a file list is received from a member
                        return Translation
                            .getTranslation("filelist.status.no_files_available_add_files_in_folder");

                    }
                }
                // no fileslist received
                if (members.length > 1) { // myself is always in this list
                    // there are other members
                    return Translation
                        .getTranslation("filelist.status.no_files_available_yet_fetching_filelist");

                }
                // no other members
                return Translation
                    .getTranslation("filelist.status.no_files_available_add_files_and_invite");

            }
            if (rowIndex >= displayList.size() || rowIndex < 0) {
                log().error(
                    "Illegal access. want to get row " + rowIndex + ", have "
                        + displayList.size());
                return null;
            }
            return displayList.get(rowIndex);
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("editing not allowed");
    }

    public void addTableModelListener(TableModelListener l) {
        tableListener.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        tableListener.remove(l);
    }

    /**
     * Fires event, that table has changed
     */
    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    DirectoryTableModel.this);
                for (Object aTableListener : tableListener) {
                    TableModelListener listener =
                            (TableModelListener) aTableListener;
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public void markAsChanged(FileInfo fileInfo) {
        int index = getIndexOf(fileInfo);
        if (index != -1) {
            fireModelChanged(index);
        }
    }

    /**
     * Fires event, that table has changed at index
     */
    private void fireModelChanged(final int index) {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    DirectoryTableModel.this, index);
                for (Object aTableListener : tableListener) {
                    TableModelListener listener = (TableModelListener) aTableListener;
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Sorts the model by a column
     *
     * @param columnIndex
     * @return if the model was sorted freshly
     */
    public boolean sortBy(int columnIndex) {
        switch (columnIndex) {
            case 0 :
                return sortBy(FileInfoComparator.BY_FILETYPE, true);
            case 1 :
                return sortBy(FileInfoComparator.BY_NAME, true);
            case 2 :
                return sortBy(FileInfoComparator.BY_SIZE, true);
            case 3 :
                return sortBy(FileInfoComparator.BY_MEMBER, true);
            case 4 :
                return sortBy(FileInfoComparator.BY_MODIFIED_DATE, true);
            case 5 :
                return sortBy(FileInfoComparator.BY_AVAILABILITY, true);
        }
        return false;
    }

    /**
     * Re-sorts the file list with the new comparator only if comparator differs
     * from old one
     *
     * @param newComparator
     * @return if the table was freshly sorted
     */
    public boolean sortBy(int newComparatorType, boolean now) {
        int oldComparatorType = fileInfoComparatorType;

        fileInfoComparatorType = newComparatorType;
        if (now && directory != null) {
            if (oldComparatorType != newComparatorType) {
                boolean sorted;
                synchronized (displayList) {
                    sorted = sort(displayList);
                }
                if (sorted) {
                    fireModelChanged();
                }
            }
        }
        return false;
    }

    /**
     * Sorts the filelist
     *
     * @param dispList
     *            the list to sort
     * @return if the model was freshly sorted
     */
    private boolean sort(List dispList) {
        if (fileInfoComparatorType != -1) {
            FileInfoComparator comparator = new FileInfoComparator(
                fileInfoComparatorType, directory);

            if (sortAscending) {
                Collections.sort(dispList, comparator);
            } else {
                Collections.sort(dispList, new ReverseComparator(comparator));
            }
            return true;
        }
        return false;
    }

    /**
     * Reverses the sorting of the table
     */
    public void reverseList() {
        sortAscending = !sortAscending;
        List tmpDisplayList = Collections.synchronizedList(new ArrayList(
            displayList.size()));
        synchronized (displayList) {
            int size = displayList.size();
            for (int i = 0; i < size; i++) {
                tmpDisplayList.add(displayList.get(size - 1 - i));
            }
            displayList = tmpDisplayList;
        }
        fireModelChanged();
    }

}
