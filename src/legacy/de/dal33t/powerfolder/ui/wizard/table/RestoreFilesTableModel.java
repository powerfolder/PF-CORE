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
 * $Id: RestoreFilesTableModel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.wizard.table;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to model files selected from the tree.
 */
public class RestoreFilesTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    private String[] columns = {
            Translation.getTranslation("restore_files_table_model.file_name"),
        Translation.getTranslation("restore_files_table_model.modified_date"),
        Translation.getTranslation("restore_files_table_model.version"),
        Translation.getTranslation("restore_files_table_model.size")};

    static final int COL_FILE_NAME = 0;
    static final int COL_MODIFIED_DATE = 1;
    static final int COL_VERSION = 2;
    static final int COL_SIZE = 3;

    private final List<FileInfo> versions;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final List<TableModelListener> listeners;

    /**
     * Constructor
     *
     * @param controller
     */
    public RestoreFilesTableModel(Controller controller) {
        super(controller);
        versions = new ArrayList<FileInfo>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    /**
     * Set the folder for the model to get details from.
     *
     * @param versions
     */
    public void setVersions(List<FileInfo> versions) {
        synchronized (this.versions) {
            this.versions.clear();
            this.versions.addAll(versions);
        }
        update();
    }
    
    /**
     * Adds the folder for the model to get details from.
     *
     * @param versions
     */
    public void addVersions(List<FileInfo> versions) {
        synchronized (this.versions) {
            this.versions.addAll(versions);
        }
        update();
    }

    /**
     * Update the model in response to a change.
     */
    private void update() {

        Runnable runnable = new Runnable() {
            public void run() {
                sort();
                fireModelChanged();
            }
        };
        UIUtil.invokeLaterInEDT(runnable);
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
        return versions.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return versions.get(rowIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
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
            case COL_FILE_NAME:
                return sortMe(FileInfoComparator.BY_NAME);
            case COL_VERSION:
                return sortMe(FileInfoComparator.BY_VERSION);
            case COL_SIZE:
                return sortMe(FileInfoComparator.BY_SIZE);
            case COL_MODIFIED_DATE:
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
        if (versions.isEmpty()) {
            return false;
        }
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

    private void fireModelChanged() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            FileInfoComparator comparator = new FileInfoComparator(
                fileInfoComparatorType);
            synchronized (versions) {
                if (sortAscending) {
                    Collections.sort(versions, comparator);
                } else {
                    Collections.sort(versions, new ReverseComparator<FileInfo>(
                        comparator));
                }
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (versions) {
            Collections.reverse(versions);
        }
        fireModelChanged();
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

}