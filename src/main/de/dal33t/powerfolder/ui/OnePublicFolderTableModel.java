package de.dal33t.powerfolder.ui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.folder.FileFilterModel;
import de.dal33t.powerfolder.util.FileInfoComparator;
import de.dal33t.powerfolder.util.ReverseComparator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps the files of a Filelist of a folderInfo to a table
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class OnePublicFolderTableModel implements TableModel {
    private Set tableListener = new HashSet();
    private FileInfoComparator comparator;
    private boolean sortAscending = true;
    private FolderInfo folderInfo;
    private FileInfo[] fileInfos;

    private String[] columns = new String[]{"",
        Translation.getTranslation("filelist.name"),
        Translation.getTranslation("general.size"),
        Translation.getTranslation("filelist.modifiedby"),
        Translation.getTranslation("filelist.date")};

    private List displayList = Collections.synchronizedList(new LinkedList());
    private FileFilterModel fileFilterModel;

    public OnePublicFolderTableModel(FileFilterModel fileFilterModel) {
        this.fileFilterModel = fileFilterModel;

        fileFilterModel
            .addFileFilterChangeListener(new FileFilterChangeListener() {
                public void filterChanged(FilterChangedEvent event) {
                    synchronized (displayList) {
                        displayList.clear();
                        displayList.addAll(event.getFilteredList());
                    }
                    fireModelChanged();
                }

                public void countChanged(FilterChangedEvent event) {

                }
            });
    }

    public void setFileList(FolderInfo folderInfo, FileInfo[] fileInfos) {
        // reset the filter if different folder selected
        if (this.folderInfo != null && folderInfo != this.folderInfo) {
            fileFilterModel.reset();
        }
        this.folderInfo = folderInfo;
        this.fileInfos = fileInfos;
        createDisplayList();
        fireModelChanged();
    }

    private void createDisplayList() {
        List allFiles = new LinkedList();
        // java 1.5:
        // Collections.addAll(allFiles, fileInfos);  
        for (int i = 0; i < fileInfos.length; i++) {
            allFiles.add(fileInfos[i]);
        }
        List filteredFiles = fileFilterModel.filter(null, allFiles);
        //quick fix null pointer exception
        //FIXME: need to follow same patern as in DirectoryTableModel
        if (filteredFiles != null) {
            synchronized (displayList) {
                displayList.clear();
                displayList.addAll(filteredFiles);
            }
            sort();
        }
    }

    public Class getColumnClass(int columnIndex) {
        return FileInfo.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        synchronized (displayList) {
            return displayList.size();
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized(displayList) {
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
                    OnePublicFolderTableModel.this);
                for (Iterator it = tableListener.iterator(); it.hasNext();) {
                    TableModelListener listener = (TableModelListener) it
                        .next();
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
                return sortBy(FileInfoComparator
                    .getComparator(FileInfoComparator.BY_FILETYPE));
            case 1 :
                return sortBy(FileInfoComparator
                    .getComparator(FileInfoComparator.BY_NAME));
            case 2 :
                return sortBy(FileInfoComparator
                    .getComparator(FileInfoComparator.BY_SIZE));
            case 3 :
                return sortBy(FileInfoComparator
                    .getComparator(FileInfoComparator.BY_MEMBER));
            case 4 :
                return sortBy(FileInfoComparator
                    .getComparator(FileInfoComparator.BY_MODIFIED_DATE));
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
    public boolean sortBy(FileInfoComparator newComparator) {
        FileInfoComparator oldComparator = comparator;
        comparator = newComparator;
        if (oldComparator != newComparator) {
            return sort();
        }
        return false;
    }

    /**
     * Sorts the filelist
     * 
     * @return if the model was freshly sorted
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
            fireModelChanged();
            return true;
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
}
