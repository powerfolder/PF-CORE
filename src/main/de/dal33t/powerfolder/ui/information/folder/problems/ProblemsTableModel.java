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
 * $Id: ProblemsTableModel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.problems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * Class to model a folder's problems. provides columns for date, description
 * and wiki.
 */
public class ProblemsTableModel extends PFUIComponent implements TableModel,
    SortedTableModel
{

    private static final int COL_DESCRIPTION = 0;
    private static final int COL_DATE = 3;
    private static final int COL_WIKI = 2;
    private static final int COL_SOLUTION = 1;

    private String[] columnHeaders = {
        Translation.getTranslation("folder_problem.table_model.description"), // 0
        Translation.getTranslation("folder_problem.table_model.solution"), // 2
        Translation.getTranslation("folder_problem.table_model.date") // 1
        };

    private final List<Problem> problems;

    private final List<TableModelListener> listeners;

    private int sortColumn = -1;
    private boolean sortAscending = true;

    public ProblemsTableModel(Controller controller) {
        super(controller);
        problems = new ArrayList<Problem>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    public int getRowCount() {
        return problems.size();
    }

    public int getColumnCount() {
        return columnHeaders.length;
    }

    public String getColumnName(int columnIndex) {
        return columnHeaders[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return Problem.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return problems.get(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in ProblemTableModel; not editable");
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
        boolean newSortColumn = sortColumn != columnIndex;
        sortColumn = columnIndex;
        switch (columnIndex) {
            case COL_DESCRIPTION :
                sortMe(FolderProblemComparator.BY_DESCRIPTION, newSortColumn);
                break;
            case COL_DATE :
                sortMe(FolderProblemComparator.BY_DATE, newSortColumn);
                break;
            case COL_WIKI :
                sortMe(FolderProblemComparator.BY_WIKI, newSortColumn);
                break;
            case COL_SOLUTION :
                sortMe(FolderProblemComparator.BY_WIKI, newSortColumn);
                break;
        }
        return true;
    }

    private void sortMe(FolderProblemComparator comparator,
        boolean newSortColumn)
    {

        if (!newSortColumn) {
            // Reverse list.
            sortAscending = !sortAscending;
        }

        if (sortAscending) {
            Collections.sort(problems, comparator);
        } else {
            Collections.sort(problems, new ReverseComparator(comparator));
        }

        modelChanged(new TableModelEvent(this, 0, problems.size() - 1));
    }

    public void updateProblems(List<Problem> problemList) {
        problems.clear();
        problems.addAll(problemList);
        modelChanged(new TableModelEvent(this));
    }

    /**
     * Fires a model event to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (TableModelListener listener : listeners) {
                        listener.tableChanged(e);
                    }
                }
            }
        };
        try {
            UIUtil.invokeAndWaitInEDT(runner);
        } catch (InterruptedException e1) {
        }
    }

    public void setAscending(boolean ascending) {
        sortAscending = ascending;
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private static class FolderProblemComparator implements Comparator<Problem>
    {

        private static final int TYPE_DESCRIPTION = 0;
        private static final int TYPE_DATE = 3;
        private static final int TYPE_WIKI = 2;
        private static final int TYPE_SOLUTION = 1;

        public static final FolderProblemComparator BY_DESCRIPTION = new FolderProblemComparator(
            TYPE_DESCRIPTION);

        public static final FolderProblemComparator BY_DATE = new FolderProblemComparator(
            TYPE_DATE);

        public static final FolderProblemComparator BY_WIKI = new FolderProblemComparator(
            TYPE_WIKI);

        public static final FolderProblemComparator BY_SOLUTION = new FolderProblemComparator(
            TYPE_SOLUTION);

        private int type;

        private FolderProblemComparator(int type) {
            this.type = type;
        }

        public int compare(Problem o1, Problem o2) {
            if (type == TYPE_DESCRIPTION) {
                return o1.getDescription().compareTo(o2.getDescription());
            } else if (type == TYPE_DATE) {
                return o1.getDate().compareTo(o2.getDate());
            } else if (type == TYPE_WIKI) {
                return o1.getWikiLinkKey().compareTo(o2.getWikiLinkKey());
            } else if (type == TYPE_SOLUTION) {
                // No real sort order available - sort on description
                return o1.getDescription().compareTo(o2.getDescription());
            }
            return 0;
        }
    }
}
