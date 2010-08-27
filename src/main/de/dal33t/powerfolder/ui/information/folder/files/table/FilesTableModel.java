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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.DiskItemFilterListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Class to model files selected from the tree.
 */
public class FilesTableModel extends PFComponent implements TableModel,
    SortedTableModel
{

    private String[] columns = {"",
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
    private DirectoryInfo selectedDirectoryInfo;
    /** A map of relativeName, DiskItem */
    private final ConcurrentMap<DirectoryInfo, List<FileInfo>> directories;
    private final List<FileInfo> diskItems;
    private final List<TableModelListener> tableModelListeners;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private DiskItemFilterListener patternChangeListener;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FilesTableModel(Controller controller) {
        super(controller);
        directories = Util.createConcurrentHashMap();
        diskItems = new ArrayList<FileInfo>();
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
            this.folder.getDiskItemFilter().removeListener(
                patternChangeListener);
        }
        this.folder = folder;
        this.folder.getDiskItemFilter().addListener(patternChangeListener);
        update();
    }

    /**
     * Set the directory selected in the tree.
     * 
     * @param selectedDirectoryInfo
     */
    public void setSelectedDirectoryInfo(DirectoryInfo selectedDirectoryInfo) {
        this.selectedDirectoryInfo = selectedDirectoryInfo;
        update();
    }

    /**
     * Pass the filtered directory model to get the file infos from.
     * 
     * @param model
     * @param flat
     */
    public void setFilteredDirectoryModel(FilteredDirectoryModel model,
        boolean flat)
    {
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
        boolean flat)
    {
        if (model == null) {
            return;
        }
        List<FileInfo> diskItemList = new ArrayList<FileInfo>();
        diskItemList.addAll(model.getFileInfos());
        if (!flat) {
            for (FilteredDirectoryModel directoryModel : model
                .getSubdirectories())
            {
                diskItemList.add(directoryModel.getDirectoryInfo());
            }
        }
        directories.putIfAbsent(model.getDirectoryInfo(), diskItemList);
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            walkFilteredDirectoryModel(subModel, flat);
        }
    }

    /**
     * @return the set folder.
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

        if (selectedDirectoryInfo == null) {
            return;
        }

        if (directories.isEmpty()) {
            return;
        }

        if (directories.keySet().contains(selectedDirectoryInfo)) {
            if (isFine()) {
                logFine("Found '" + selectedDirectoryInfo + "' in directories");
            }
        } else {
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {
                synchronized (diskItems) {
                    if (selectedDirectoryInfo == null) {
                        logWarning("??? selectedDirectoryInfo == null ???");
                        return;
                    }
                    List<FileInfo> selectedDiskItems = directories
                        .get(selectedDirectoryInfo);
                    if (selectedDiskItems == null) {
                        selectedDiskItems = new ArrayList<FileInfo>();
                    }

                    if (diskItems.size() != selectedDiskItems.size()) {
                        // There are structural differences - reload.
                        diskItems.clear();
                        diskItems.addAll(selectedDiskItems);
                        sort();
                        fireModelChanged();
                        return;
                    }

                    // Same size sets.
                    boolean allSame = true;
                    for (DiskItem selectedDiskItem : selectedDiskItems) {
                        boolean found = false;
                        for (DiskItem diskItem : diskItems) {
                            if (selectedDiskItem instanceof FileInfo
                                && diskItem instanceof FileInfo)
                            {
                                if (((FileInfo) selectedDiskItem)
                                    .isVersionDateAndSizeIdentical((FileInfo) diskItem))
                                {
                                    found = true;
                                    break;
                                }
                            } else {
                                if (selectedDiskItem.equals(diskItem)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            allSame = false;
                            break;
                        }
                    }
                    if (allSame) {
                        return;
                    }

                    // There are differences - reload.
                    diskItems.clear();
                    diskItems.addAll(selectedDiskItems);
                    sort();
                    fireModelChanged();
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

    public DiskItem[] getDiskItemsAtRows(int[] rows) {
        DiskItem[] items = new DiskItem[rows.length];
        int i = 0;
        for (int row : rows) {
            Object at = getValueAt(row, COL_NAME);
            if (at instanceof DirectoryInfo) {
                items[i] = (DirectoryInfo) at;
            } else if (at instanceof FileInfo) {
                items[i] = (FileInfo) at;
            } else {
                logSevere("Object was a " + at.getClass().getName() + "???");
            }
            i++;
        }
        return items;
    }

    public DiskItem[] getAllDiskItems() {
        DiskItem[] items = new DiskItem[diskItems.size()];
        for (int i = 0; i < items.length; i++) {
            Object at = getValueAt(i, COL_NAME);
            if (at instanceof DirectoryInfo) {
                items[i] = (DirectoryInfo) at;
            } else if (at instanceof FileInfo) {
                items[i] = (FileInfo) at;
            } else {
                logSevere("Object was a " + at.getClass().getName() + "???");
            }
        }
        return items;
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
            case COL_FILE_TYPE :
                return sortMe(FileInfoComparator.BY_FILE_TYPE);
            case COL_NAME :
                return sortMe(FileInfoComparator.BY_NAME);
            case COL_SIZE :
                return sortMe(FileInfoComparator.BY_SIZE);
            case COL_MEMBER :
                return sortMe(FileInfoComparator.BY_MEMBER);
            case COL_MODIFIED_DATE :
                return sortMe(FileInfoComparator.BY_MODIFIED_DATE);
        }

        sortColumn = -1;
        return false;
    }

    /**
     * Re-sorts the file list with the new comparator only if comparator differs
     * from old one
     * 
     * @param newComparatorType
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

    @SuppressWarnings({"unchecked"})
    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            FileInfoComparator comparator = new FileInfoComparator(
                fileInfoComparatorType);
            synchronized (diskItems) {
                if (sortAscending) {
                    Collections.sort(diskItems, comparator);
                } else {
                    Collections.sort(diskItems, new ReverseComparator(
                        comparator));
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

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyPatternChangeListener implements DiskItemFilterListener {

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
