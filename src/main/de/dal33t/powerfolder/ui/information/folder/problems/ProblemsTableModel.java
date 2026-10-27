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
package de.dal33t.powerfolder.ui.information.folder.problems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;

/**
 * Class to model a folder's problems. provides columns for date, description
 * and wiki.
 */
public class ProblemsTableModel extends PFUIComponent implements TableModel,
    SortedTableModel
{

    /** PFC-3657: leading check-box column to tick problems for a bulk action. */
    public static final int COL_CHECK = 0;
    public static final int COL_ICON = 1;
    public static final int COL_DESCRIPTION = 2;
    public static final int COL_SOLUTION = 3;
    public static final int COL_DATE = 4;

    private String[] columnHeaders = {
        "", // 0 Check box
        "", // 1 Icon
        Translation.get("folder_problem.table_model.description"), // 2
        Translation.get("folder_problem.table_model.solution"), // 3
        Translation.get("folder_problem.table_model.date") // 4
        };

    private final List<Problem> problems;
    /** The problems the user has ticked in the check-box column. */
    private final Set<Problem> checked = new LinkedHashSet<Problem>();

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
        if (columnIndex == COL_CHECK) {
            return Boolean.class;
        }
        return Problem.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only the check-box column is editable.
        return columnIndex == COL_CHECK && getProblemAt(rowIndex) != null;
    }

    /**
     * The problem at a row, or null if the row is out of range.
     */
    public Problem getProblemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < problems.size()) {
            return problems.get(rowIndex);
        }
        return null;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Problem problem = getProblemAt(rowIndex);
        if (columnIndex == COL_CHECK) {
            return Boolean.valueOf(problem != null && checked.contains(problem));
        }
        return problem;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COL_CHECK) {
            Problem problem = getProblemAt(rowIndex);
            if (problem != null) {
                if (Boolean.TRUE.equals(aValue)) {
                    checked.add(problem);
                } else {
                    checked.remove(problem);
                }
                modelChanged(new TableModelEvent(this, rowIndex, rowIndex));
            }
            return;
        }
        throw new IllegalStateException(
            "Unable to set value in ProblemTableModel; not editable");
    }

    /**
     * The ticked problems, in table order. Empty if nothing is ticked.
     */
    public List<Problem> getCheckedProblems() {
        List<Problem> result = new ArrayList<Problem>();
        for (Problem problem : problems) {
            if (checked.contains(problem)) {
                result.add(problem);
            }
        }
        return result;
    }

    /**
     * Whether every problem is ticked (and there is at least one). Drives the
     * select-all check box in the column header.
     */
    public boolean isAllChecked() {
        return !problems.isEmpty() && checked.containsAll(problems);
    }

    /**
     * Ticks or unticks every problem at once (the header select-all check box).
     */
    public void setAllChecked(boolean value) {
        checked.clear();
        if (value) {
            checked.addAll(problems);
        }
        modelChanged(new TableModelEvent(this));
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
            case COL_SOLUTION :
                sortMe(FolderProblemComparator.BY_SOLUTION, newSortColumn);
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
            Collections.sort(problems, new ReverseComparator<Problem>(comparator));
        }

        modelChanged(new TableModelEvent(this, 0, problems.size() - 1));
    }

    public void updateProblems(List<Problem> problemList) {
        problems.clear();
        checked.clear();
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
