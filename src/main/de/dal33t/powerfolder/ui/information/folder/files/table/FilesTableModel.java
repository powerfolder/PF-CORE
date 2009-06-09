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
* $Id: FilesTableModel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.table;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.PatternChangeListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.DiskItemComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to model files selected from the tree.
 */
public class FilesTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    private String[] columns = new String[]{"",
        Translation.getTranslation("files_table_model.name"),
        Translation.getTranslation("files_table_model.size"),
        Translation.getTranslation("files_table_model.modified_by"),
        Translation.getTranslation("files_table_model.date")};

    private static final int COL_FILE_TYPE = 0;
    private static final int COL_NAME = 1;
    public static final int COL_SIZE = 2;
    private static final int COL_MEMBER = 3;
    private static final int COL_MODIFIED_DATE = 4;

    private Folder folder;
    private File selectedDirectory;
    private final Map<File, List<DiskItem>> directories;
    private final List<DiskItem> diskItems;
    private final List<TableModelListener> tableModelListeners;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private PatternChangeListener patternChangeListener;

    /**
     * Constructor
     *
     * @param controller
     */
    public FilesTableModel(Controller controller) {
        super(controller);
        directories = new ConcurrentHashMap<File, List<DiskItem>>();
        diskItems = new ArrayList<DiskItem>();
        tableModelListeners = new CopyOnWriteArrayList<TableModelListener>();
        patternChangeListener = new MyPatternChangeListener();
    }

    /**
     * Set the folder for the model to get details from.
     *
     * @param folder
     */
    public void setFolder(Folder folder) {
        if (this.folder != null) {
            this.folder.getDiskItemFilter().removeListener(patternChangeListener);
        }
        this.folder = folder;
        this.folder.getDiskItemFilter().addListener(patternChangeListener);
        update();
    }

    /**
     * Set the directory selected in the tree.
     *
     * @param selectedDirectory
     */
    public void setSelectedDirectory(File selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        update();
    }

    /**
     * Pass the filtered directory model to get the file infos from.
     *
     * @param model
     */
    public void setFilteredDirectoryModel(FilteredDirectoryModel model,
                                          boolean flat) {
        directories.clear();
        walkFilteredDirectoryModel(model, flat);
        update();
    }

    /**
     * Walk the FilteredDirectoryModel to get map of directory / FileInfos.
     *
     * @param model
     */
    private void walkFilteredDirectoryModel(FilteredDirectoryModel model,
                                            boolean flat) {
        File file = model.getFile();
        List<DiskItem> diskItemList = new ArrayList<DiskItem>();
        diskItemList.addAll(model.getFiles());
        if (!flat) {
            diskItemList.addAll(model.getSubdirectoryDirectories());
        }
        directories.put(file, diskItemList);
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            walkFilteredDirectoryModel(subModel, flat);
        }
    }

    /**
     * Returns the set folder.
     * 
     * @return
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Update the model in response to a change.
     */
    private void update() {

        if (folder == null) {
            return;
        }

        if (selectedDirectory == null) {
            return;
        }

        if (directories.isEmpty()) {
            return;
        }

        boolean found = false;
        for (File file : directories.keySet()) {
            if (selectedDirectory.equals(file)) {
                logInfo("Found " + selectedDirectory + " in directories");
                found = true;
                break;
            }
        }

        if (!found) {
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {
                synchronized (diskItems) {

                    List<DiskItem> tempList = new ArrayList<DiskItem>(diskItems.size());
                    tempList.addAll(diskItems);

                    // Look for extra items in the selectedDiskItems list to insert.
                    List<DiskItem> selectedDiskItems = directories.get(selectedDirectory);
                    boolean changed = false;
                    for (DiskItem selectedDiskItem : selectedDiskItems) {
                        boolean b = false;
                        for (DiskItem diskItem : tempList) {
                            if (diskItem.equals(selectedDiskItem)) {
                                b = true;
                                break;
                            }
                        }
                        if (!b) {
                            diskItems.add(selectedDiskItem);
                            changed = true;
                        }
                    }

                    // Look for extra items in the diskItem list to remove.
                    for (DiskItem diskItem : tempList) {
                        boolean b = false;
                        for (DiskItem selectedDiskItem : selectedDiskItems) {
                            if (diskItem.equals(selectedDiskItem)) {
                                b = true;
                                break;
                            }
                        }
                        if (!b) {
                            diskItems.remove(diskItem);
                            changed = true;
                        }
                    }

                    if (changed) {
                        sort();
                        fireModelChanged();
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runnable);
    }

    public void addTableModelListener(TableModelListener l) {
        tableModelListeners.add(l);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return FileInfo.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        return diskItems.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return diskItems.get(rowIndex);
    }

    public FileInfo getFileInfoAtRow(int row) {
        Object at = getValueAt(row, COL_NAME);
        if (at instanceof FileInfo) {
            return (FileInfo) at;
        }
        return null;
    }

    public Directory getDirectoryAtRow(int row) {
        Object at = getValueAt(row, COL_NAME);
        if (at instanceof Directory) {
            return (Directory) at;
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
        tableModelListeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot modify FilesTableModel");
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public boolean sortBy(int columnIndex) {
        sortColumn = columnIndex;
        switch (columnIndex) {
            case COL_FILE_TYPE:
                return sortMe(DiskItemComparator.BY_FILE_TYPE);
            case COL_NAME :
                return sortMe(DiskItemComparator.BY_NAME);
            case COL_SIZE:
                return sortMe(DiskItemComparator.BY_SIZE);
            case COL_MEMBER:
                return sortMe(DiskItemComparator.BY_MEMBER);
            case COL_MODIFIED_DATE:
                return sortMe(DiskItemComparator.BY_MODIFIED_DATE);
        }

        sortColumn = -1;
        return false;
    }

    /**
     * Re-sorts the file list with the new comparator only if comparator differs
     * from old one
     *
     * @param newComparator
     * @return if the table was freshly sorted
     */
    public boolean sortMe(int newComparatorType) {
        int oldComparatorType = fileInfoComparatorType;

        fileInfoComparatorType = newComparatorType;
        if (oldComparatorType != newComparatorType) {
            boolean sorted = sort();
            if (sorted) {
                fireModelChanged();
                return true;
            }
        }
        return false;
    }

    public void sortLatestDate() {
        sortAscending = true;
        sortBy(COL_MODIFIED_DATE);
    }

    private void fireModelChanged() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : tableModelListeners) {
            listener.tableChanged(e);
        }
    }

    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            DiskItemComparator comparator = new DiskItemComparator(
                fileInfoComparatorType, folder == null ? null : folder.getDirectory());
            synchronized (diskItems) {
                if (sortAscending) {
                    Collections.sort(diskItems, comparator);
                } else {
                    Collections.sort(diskItems, new ReverseComparator(comparator));
                }
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (diskItems) {
            Collections.reverse(diskItems);
        }
        fireModelChanged();
    }

    private class MyPatternChangeListener implements PatternChangeListener {

        public void patternAdded(PatternChangedEvent e) {
            fireModelChanged();
        }

        public void patternRemoved(PatternChangedEvent e) {
            fireModelChanged();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}
