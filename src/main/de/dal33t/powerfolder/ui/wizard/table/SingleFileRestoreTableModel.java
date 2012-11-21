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
 * $Id: SingleFileRestoreTableModel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.table;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.data.FileInfoLocation;
import de.dal33t.powerfolder.ui.wizard.data.FileInfoLocationComparator;
import de.dal33t.powerfolder.util.Translation;
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

public class SingleFileRestoreTableModel  extends PFComponent implements TableModel, SortedTableModel {

    private static final String[] COLUMNS = {
            Translation.getTranslation("single_file_restore_table_model.modified_date"),
            Translation.getTranslation("single_file_restore_table_model.version"),
            Translation.getTranslation("single_file_restore_table_model.size"),
            Translation.getTranslation("single_file_restore_table_model.location")};

    static final int COL_MODIFIED_DATE = 0;
    static final int COL_VERSION = 1;
    static final int COL_SIZE = 2;
    static final int COL_LOCATION = 3;

    private final List<FileInfoLocation> fileInfoLocations;
    private int fileInfoLocationComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final List<TableModelListener> listeners;

    public SingleFileRestoreTableModel(Controller controller) {
        super(controller);
        fileInfoLocations = new ArrayList<FileInfoLocation>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    public int getRowCount() {
        return fileInfoLocations.size();
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public String getColumnName(int columnIndex) {
        return COLUMNS[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return FileInfoLocation.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return fileInfoLocations.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot modify SingleFileRestoreTableModel");
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
            case COL_VERSION:
                return sortMe(FileInfoLocationComparator.BY_VERSION);
            case COL_SIZE:
                return sortMe(FileInfoLocationComparator.BY_SIZE);
            case COL_MODIFIED_DATE:
                return sortMe(FileInfoLocationComparator.BY_MODIFIED_DATE);
            case COL_LOCATION:
                return sortMe(FileInfoLocationComparator.BY_LOCATION);
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
        if (fileInfoLocations.isEmpty()) {
            return false;
        }
        int oldComparatorType = fileInfoLocationComparatorType;
        fileInfoLocationComparatorType = newComparatorType;
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
        if (fileInfoLocationComparatorType != -1) {
            FileInfoLocationComparator comparator = new FileInfoLocationComparator(fileInfoLocationComparatorType);
            synchronized (fileInfoLocations) {
                if (sortAscending) {
                    Collections.sort(fileInfoLocations, comparator);
                } else {
                    Collections.sort(fileInfoLocations, new ReverseComparator<FileInfoLocation>(comparator));
                }
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (fileInfoLocations) {
            Collections.reverse(fileInfoLocations);
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

    public void setFileInfoLocations(List<FileInfoLocation> fileInfoLocations) {
        synchronized (this.fileInfoLocations) {
            this.fileInfoLocations.clear();
            this.fileInfoLocations.addAll(fileInfoLocations);
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

    public List<FileInfoLocation> getFileInfoLocations() {
        return Collections.unmodifiableList(fileInfoLocations);
    }
}
