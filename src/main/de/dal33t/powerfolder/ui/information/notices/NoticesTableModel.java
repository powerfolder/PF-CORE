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
 * $Id$
 */
package de.dal33t.powerfolder.ui.information.notices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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

    private final Controller controller;
    private final List<Notice> notices = new ArrayList<Notice>();
    private final List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    private final NoticeComparator[] columComparators = {
        NoticeComparator.BY_SEVERITY, NoticeComparator.BY_DATE,
        NoticeComparator.BY_SUMMARY};
    private NoticeComparator comparator;

    private boolean sortAscending;
    private int sortColumn;

    private static final String[] COLUMN_NAMES = {"",
        Translation.get("notices_table.date"),
        Translation.get("notices_table.summary")};

    public NoticesTableModel(Controller controller) {
        this.controller = controller;
        sortAscending = false;
        sortBy(1);
        reset();
    }

    /**
     * Clear all users and reload.
     */
    public void reset() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                notices.clear();
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

    public Object getValueAt(int rowIndex, int columnIndex) {
        int i = 0;
        for (Notice notice : notices) {
            if (i++ == rowIndex) {
                return notice;
            }
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("not editable");
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