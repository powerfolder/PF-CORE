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

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.model.SortedTableModel;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to model a folder's problems.
 * provides columns for date, description and wiki.
 */
public class ProblemsTableModel extends PFUIComponent implements TableModel,
        SortedTableModel {

    private String[] columnHeaders = {
        Translation.getTranslation("folder_problem.table_model.date"), // 0
        Translation.getTranslation("folder_problem.table_model.description"), // 1
        Translation.getTranslation("folder_problem.table_model.wiki")}; // 2

    private final List<Problem> problems;

    public ProblemsTableModel(Controller controller) {
        super(controller);
        problems = new ArrayList<Problem>();

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
    }

    public void removeTableModelListener(TableModelListener l) {
    }

    public int getSortColumn() {
        return 0;
    }

    public boolean isSortAscending() {
        return false;
    }

    public boolean sortBy(int columnIndex) {
        return false;
    }
}
