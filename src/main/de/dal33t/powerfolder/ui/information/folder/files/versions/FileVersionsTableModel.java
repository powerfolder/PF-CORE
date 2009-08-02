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
package de.dal33t.powerfolder.ui.information.folder.files.versions;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FileVersionInfo;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.compare.FileVersionItemComparator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to model file versions for the versions table.
 */
public class FileVersionsTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    private String[] columns = {Translation.getTranslation(
            "file_versions_table_model.version"),
            Translation.getTranslation("file_versions_table_model.size"),
            Translation.getTranslation("file_versions_table_model.date")};

    private static final int COL_VERSION = 0;
    private static final int COL_SIZE = 1;
    public static final int COL_DATE = 2;

    private final List<FileVersionInfo> versionInfos;
    private int comparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final List<TableModelListener> listeners;

    /**
     * Constructor
     *
     * @param controller
     */
    public FileVersionsTableModel(Controller controller) {
        super(controller);
        versionInfos = new ArrayList<FileVersionInfo>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    /**
     * Set the file version infos for the model to get details from.
     *
     * @param infos
     */
    public void setVersionInfos(Set<FileVersionInfo> infos) {
        versionInfos.clear();
        versionInfos.addAll(infos);
        fireModelChanged();
    }

    public Class<?> getColumnClass(int columnIndex) {
        return FileVersionInfo.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        return versionInfos.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return versionInfos.get(rowIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot modify FileVersionsTableModel");
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
                return sortMe(FileVersionItemComparator.BY_VERSION);
            case COL_SIZE :
                return sortMe(FileVersionItemComparator.BY_SIZE);
            case COL_DATE:
                return sortMe(FileVersionItemComparator.BY_DATE);
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
        int oldComparatorType = comparatorType;

        comparatorType = newComparatorType;
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
        if (comparatorType != -1) {
            FileVersionItemComparator comparator =
                    FileVersionItemComparator.getComparator(comparatorType);
            synchronized (versionInfos) {
                if (sortAscending) {
                    Collections.sort(versionInfos, comparator);
                } else {
                    Collections.sort(versionInfos, new ReverseComparator(comparator));
                }
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (versionInfos) {
            Collections.reverse(versionInfos);
        }
        fireModelChanged();
    }
}