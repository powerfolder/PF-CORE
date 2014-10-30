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
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.wizard.data.SingleFileRestoreItem;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.logging.Loggable;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

public class SingleFileRestoreTableModel  extends PFComponent implements TableModel, SortedTableModel {

    private static final String[] COLUMNS = {
            Translation.getTranslation("single_file_restore_table_model.modified_date"),
            Translation.getTranslation("single_file_restore_table_model.version"),
            Translation.getTranslation("single_file_restore_table_model.size"),
            Translation.getTranslation("single_file_restore_table_model.local")
    };

    static final int COL_MODIFIED_DATE = 0;
    static final int COL_VERSION = 1;
    static final int COL_SIZE = 2;
    static final int COL_LOCAL = 3;

    private final List<SingleFileRestoreItem> fileInfos;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private final List<TableModelListener> listeners;

    public SingleFileRestoreTableModel(Controller controller) {
        super(controller);
        fileInfos = new ArrayList<SingleFileRestoreItem>();
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
                return sortMe(SingleFileRestoreItemComparator.BY_VERSION);
            case COL_SIZE:
                return sortMe(SingleFileRestoreItemComparator.BY_SIZE);
            case COL_MODIFIED_DATE:
                return sortMe(SingleFileRestoreItemComparator.BY_MODIFIED_DATE);
            case COL_LOCAL:
                return sortMe(SingleFileRestoreItemComparator.BY_MODIFIED_DATE);
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
            SingleFileRestoreItemComparator comparator = new SingleFileRestoreItemComparator(fileInfoComparatorType);
            synchronized (fileInfos) {
                if (sortAscending) {
                    Collections.sort(fileInfos, comparator);
                } else {
                    Collections.sort(fileInfos, new ReverseComparator<SingleFileRestoreItem>(comparator));
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

    public void setFileInfos(List<SingleFileRestoreItem> restoreItems) {
        synchronized (fileInfos) {
            fileInfos.clear();
            fileInfos.addAll(restoreItems);
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

    public List<SingleFileRestoreItem> getRestoreItems() {
        return Collections.unmodifiableList(fileInfos);
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

private static class SingleFileRestoreItemComparator extends Loggable implements Comparator<SingleFileRestoreItem> {

    // All the available file comparators
    public static final int BY_SIZE = 0;
    public static final int BY_MODIFIED_DATE = 1;
    public static final int BY_VERSION = 2;
    public static final int BY_LOCAL = 3;

    private static final int BEFORE = -1;
    private static final int AFTER = 1;

    private int sortBy;
    private static final SingleFileRestoreItemComparator[] COMPARATORS;

    static {
        COMPARATORS = new SingleFileRestoreItemComparator[8];
        COMPARATORS[BY_SIZE] = new SingleFileRestoreItemComparator(BY_SIZE);
        COMPARATORS[BY_MODIFIED_DATE] = new SingleFileRestoreItemComparator(BY_MODIFIED_DATE);
        COMPARATORS[BY_VERSION] = new SingleFileRestoreItemComparator(BY_VERSION);
    }

    SingleFileRestoreItemComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Compare by various types. If types are the same, sub-compare on file
     * name, for nice table display.
     *
     * @param o1
     * @param o2
     * @return the value
     */
    public int compare(SingleFileRestoreItem o1, SingleFileRestoreItem o2) {

        switch (sortBy) {
            case BY_SIZE :
                if (o1.getFileInfo().isLookupInstance() || o2.getFileInfo().isLookupInstance()) {
                    return sortByVersion(o1, o2);
                }
                if (o1.getFileInfo().getSize() < o2.getFileInfo().getSize()) {
                    return BEFORE;
                }
                if (o1.getFileInfo().getSize() > o2.getFileInfo().getSize()) {
                    return AFTER;
                }
                return sortByVersion(o1, o2);
            case BY_MODIFIED_DATE :
                if (o1.getFileInfo().getModifiedDate() == null
                    && o2.getFileInfo().getModifiedDate() == null)
                {
                    return sortByVersion(o1, o2);
                } else if (o1.getFileInfo().getModifiedDate() == null) {
                    return BEFORE;
                } else if (o2.getFileInfo().getModifiedDate() == null) {
                    return AFTER;
                }
                int x = o2.getFileInfo().getModifiedDate().compareTo(o1.getFileInfo().getModifiedDate());
                if (x == 0) {
                    return sortByVersion(o1, o2);
                }
                return x;
            case BY_VERSION :
                return sortByVersion(o1, o2);
            case BY_LOCAL :
                if (o1.isLocal() && o2.isLocal() || !o1.isLocal() && !o2.isLocal()) {
                    return sortByVersion(o1, o2);
                }
                return o1.isLocal() ? BEFORE : AFTER;
        }
        return 0;
    }

    private int sortByVersion(SingleFileRestoreItem o1, SingleFileRestoreItem o2) {
    if (o1.getFileInfo().getFolderInfo() == null && o2.getFileInfo().getFolderInfo() == null) {
        return 0;
    } else if (o1.getFileInfo().getFolderInfo() == null) {
        return BEFORE;
    } else if (o2.getFileInfo().getFolderInfo() == null) {
        return AFTER;
    } else if (o1.getFileInfo() instanceof DirectoryInfo || o2.getFileInfo() instanceof DirectoryInfo) {
        return 1;
    } else {
        return o1.getFileInfo().getVersion() - o2.getFileInfo().getVersion();
    }

}
}
}
