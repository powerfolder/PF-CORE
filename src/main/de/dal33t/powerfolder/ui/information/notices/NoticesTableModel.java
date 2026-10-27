/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.information.notices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.NoticeComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * Class to hold notices for table.
 */
public class NoticesTableModel implements TableModel, SortedTableModel {

    /** PFC-3657: leading check-box column to tick notices for a bulk action. */
    public static final int COL_CHECK = 0;

    private final Controller controller;
    private final List<Notice> notices = new ArrayList<Notice>();
    /** The notices the user has ticked in the check-box column. */
    private final Set<Notice> checked = new LinkedHashSet<Notice>();
    private final List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    private final NoticeComparator[] columComparators = {null,
        NoticeComparator.BY_SEVERITY, NoticeComparator.BY_DATE,
        NoticeComparator.BY_SUMMARY};
    private NoticeComparator comparator;

    private boolean sortAscending;
    private int sortColumn;

    private static final String[] COLUMN_NAMES = {"", "",
        Translation.get("notices_table.date"),
        Translation.get("notices_table.summary")};

    public NoticesTableModel(Controller controller) {
        this.controller = controller;
        sortAscending = false;
        sortBy(2);
        reset();
    }

    /**
     * Clear all users and reload.
     */
    public void reset() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                notices.clear();
                checked.clear();
                List<Notice> allNotices = controller.getUIController()
                    .getApplicationModel().getNoticesModel().getAllNotices();
                notices.addAll(allNotices);
                sort();
                fireModelStructureChanged();
            }
        });
    }

    // TableModel interface ***************************************************

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == COL_CHECK) {
            return Boolean.class;
        }
        return Notice.class;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    public int getRowCount() {
        return Math.max(notices.size(), 1);
    }

    /**
     * The notice at a row, or null for the placeholder row of an empty table.
     */
    public Notice getNoticeAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < notices.size()) {
            return notices.get(rowIndex);
        }
        return null;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Notice notice = getNoticeAt(rowIndex);
        if (columnIndex == COL_CHECK) {
            return Boolean.valueOf(notice != null && checked.contains(notice));
        }
        return notice;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only the check-box column is editable, and only on real notice rows.
        return columnIndex == COL_CHECK && getNoticeAt(rowIndex) != null;
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COL_CHECK) {
            Notice notice = getNoticeAt(rowIndex);
            if (notice != null) {
                if (Boolean.TRUE.equals(aValue)) {
                    checked.add(notice);
                } else {
                    checked.remove(notice);
                }
                fireModelChanged();
            }
            return;
        }
        throw new IllegalStateException("not editable");
    }

    /**
     * The ticked notices, in table order. Empty if nothing is ticked.
     */
    public List<Notice> getCheckedNotices() {
        List<Notice> result = new ArrayList<Notice>();
        for (Notice notice : notices) {
            if (checked.contains(notice)) {
                result.add(notice);
            }
        }
        return result;
    }

    /**
     * Whether every notice is ticked (and there is at least one). Drives the
     * select-all check box in the column header.
     */
    public boolean isAllChecked() {
        return !notices.isEmpty() && checked.containsAll(notices);
    }

    /**
     * Ticks or unticks every notice at once (the header select-all check box).
     */
    public void setAllChecked(boolean value) {
        checked.clear();
        if (value) {
            checked.addAll(notices);
        }
        fireModelChanged();
    }

    private void fireModelStructureChanged() {
        TableModelEvent te = new TableModelEvent(this);
        fireTableModelEvent(te);
    }

    private void fireTableModelEvent(TableModelEvent te) {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(te);
        }
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    public boolean sortBy(int columnIndex) {
        // Do not sort if no comparator given for that column
        if (columnIndex < 0 && columnIndex > columComparators.length
            || columComparators[columnIndex] == null)
        {
            sortColumn = -1;
            comparator = null;
            return false;
        }
        sortColumn = columnIndex;
        return sortBy(columComparators[columnIndex]);
    }

    /**
     * Reverses the sorting of the table
     */
    public void reverseList() {
        sortAscending = !sortAscending;
        sort();
    }

    /**
     * Re-sorts the folder list with the new comparator only if comparator
     * differs from old one
     *
     * @param newComparator
     * @return if the table was freshly sorted
     */
    private boolean sortBy(NoticeComparator newComparator) {
        Comparator<Notice> oldComparator = comparator;
        comparator = newComparator;
        if (!Util.equals(oldComparator, newComparator)) {
            return sort();
        }
        return false;
    }

    /**
     * Sorts the filelist
     */
    private boolean sort() {
        if (comparator != null) {
            synchronized (notices) {
                if (sortAscending) {
                    Collections.sort(notices, comparator);
                } else {
                    Collections
                        .sort(notices, new ReverseComparator<Notice>(comparator));
                }
            }
            fireModelChanged();
            return true;
        }
        return false;
    }

    private void fireModelChanged() {
        TableModelEvent te = new TableModelEvent(this, 0, getRowCount(),
            TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE);
        fireTableModelEvent(te);
    }
}
