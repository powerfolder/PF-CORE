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
 * $Id: MultiFileRestoreTableModel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.table;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.UIUtil;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class MultiFileRestoreTableModel extends PFComponent implements TableModel, SortedTableModel {

    private static final String[] COLUMNS = {
            Translation.get("multi_file_restore_table_model.file_name"),
        Translation.get("multi_file_restore_table_model.modified_date"),
        Translation.get("multi_file_restore_table_model.version"),
        Translation.get("multi_file_restore_table_model.size")};

    static final int COL_FILE_NAME = 0;
    static final int COL_MODIFIED_DATE = 1;
    static final int COL_VERSION = 2;
    static final int COL_SIZE = 3;

    private final List<FileInfo> fileInfos;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final List<TableModelListener> listeners;

    /**
     * Constructor
     *
     * @param controller
     */
    public MultiFileRestoreTableModel(Controller controller) {
        super(controller);
        fileInfos = new ArrayList<FileInfo>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    public int getRowCount() {
        return fileInfos.size();
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public String getColumnName(int columnIndex) {
        return COLUMNS[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return FileInfo.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return fileInfos.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot modify MultiFileRestoreTableModel");
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
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
        if (fileInfos.isEmpty()) {
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


    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            FileInfoComparator comparator = new FileInfoComparator(
                fileInfoComparatorType);
            synchronized (fileInfos) {
                if (sortAscending) {
                    Collections.sort(fileInfos, comparator);
                } else {
                    Collections.sort(fileInfos, new ReverseComparator<FileInfo>(
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
        fireModelChanged();
    }

    private void fireModelChanged() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    public void setFileInfos(List<FileInfo> fileInfos) {
        synchronized (this.fileInfos) {
            this.fileInfos.clear();
            this.fileInfos.addAll(fileInfos);
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

    public List<FileInfo> getFileInfos() {
        return Collections.unmodifiableList(fileInfos);
    }
}
