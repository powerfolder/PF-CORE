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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.DiskItemFilterListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * Class to model files selected from the tree.
 */
public class FilesTableModel extends PFComponent implements TableModel,
    SortedTableModel
{

    private String[] columns;

    private static final int COL_FILE_TYPE = 0;
    private static final int COL_NAME = 1;
    public static final int COL_SIZE = 2;
    private static final int COL_MEMBER = 3;
    private static final int COL_MODIFIED_DATE = 4;

    private Folder folder;
    private final List<FileInfo> fileInfos;

    private final AtomicBoolean significantlyChanged;
    private Folder previousFolder;
    private final List<FileInfo> previousFileInfos;
    private boolean previouslySorted;

    private final List<TableModelListener> tableModelListeners;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private DiskItemFilterListener patternChangeListener;
    private DelayedUpdater modelChangedUpdater;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FilesTableModel(Controller controller) {
        super(controller);
        fileInfos = new ArrayList<FileInfo>();
        previousFileInfos = new ArrayList<FileInfo>();
        tableModelListeners = new CopyOnWriteArrayList<TableModelListener>();
        patternChangeListener = new MyPatternChangeListener();
        significantlyChanged = new AtomicBoolean();
        modelChangedUpdater = new DelayedUpdater(getController());

        int colLength = ConfigurationEntry.MEMBERS_ENABLED
            .getValueBoolean(controller) ? 5 : 4;

        columns = new String[colLength];

        columns[0] = "";
        columns[1] = Translation.getTranslation("files_table_model.name");
        columns[2] = Translation.getTranslation("files_table_model.size");

        if (ConfigurationEntry.MEMBERS_ENABLED.getValueBoolean(controller)) {
            columns[3] = Translation
                .getTranslation("files_table_model.modified_by");
            columns[4] = Translation.getTranslation("files_table_model.date");
        } else {
            columns[3] = Translation.getTranslation("files_table_model.date");
        }
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
     * Pass the filtered directory model to get the file infos from.
     * 
     * @param model
     */
    public void setFilteredDirectoryModel(FilteredDirectoryModel model) {
        fileInfos.clear();
        fileInfos.addAll(model.getFileInfos());
        update();
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

        Runnable runnable = new Runnable() {
            public void run() {
                synchronized (fileInfos) {
                    sort();
                    preprocessSignificantlyChanged();
                    fireModelChanged0();
                    postprocessSignificantlyChanged();
                }
            }
        };
        modelChangedUpdater.schedule(runnable);
    }

    /**
     * Manually sorted? Folder changed? FileInfos different?
     */
    private void preprocessSignificantlyChanged() {
        if (previouslySorted) {
            significantlyChanged.set(true);
            return;
        }
        if (folder == null || previousFolder == null
            || !folder.equals(previousFolder))
        {
            significantlyChanged.set(true);
            return;
        }
        if (fileInfos.size() != previousFileInfos.size()) {
            significantlyChanged.set(true);
            return;
        }
        for (FileInfo fileInfo : fileInfos) {
            boolean same = false;
            for (FileInfo previousFileInfo : previousFileInfos) {
                if (fileInfo.equals(previousFileInfo)) {
                    same = true;
                    break;
                }
            }
            if (!same) {
                significantlyChanged.set(true);
                return;
            }
        }
        significantlyChanged.set(false);
    }

    /**
     * Remember state for next time.
     */
    private void postprocessSignificantlyChanged() {
        previousFolder = folder;
        previousFileInfos.clear();
        previousFileInfos.addAll(fileInfos);
        previouslySorted = false;
    }

    /**
     * During an update, has the model data changed significantly?
     * 
     * @return
     */
    public boolean isSignificantlyChanged() {
        return significantlyChanged.get();
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
        return fileInfos.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return fileInfos.get(rowIndex);
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
        DiskItem[] items = new DiskItem[fileInfos.size()];
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
        previouslySorted = true;
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
                fireModelChanged0();
                return true;
            }
        }
        return false;
    }

    public void sortLatestDate() {
        sortAscending = true;
        sortBy(COL_MODIFIED_DATE);
    }

    private void fireModelChanged0() {
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
            synchronized (fileInfos) {
                if (sortAscending) {
                    Collections.sort(fileInfos, comparator);
                } else {
                    Collections.sort(fileInfos, new ReverseComparator(
                        comparator));
                }
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (fileInfos) {
            Collections.reverse(fileInfos);
        }
        fireModelChanged0();
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyPatternChangeListener implements DiskItemFilterListener {

        public void patternAdded(PatternChangedEvent e) {
            fireModelChanged0();
        }

        public void patternRemoved(PatternChangedEvent e) {
            fireModelChanged0();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}
