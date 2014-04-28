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
* $Id: ProblemsTable.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.problems;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ResolvableProblem;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

public class ProblemsTable extends JTable {

    private final Controller controller;

    public ProblemsTable(ProblemsTableModel model, Controller controller) {
        super(model);
        this.controller = controller;
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        setupColumns();

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        ProblemTableCellRenderer problemTableCellRenderer =
                new ProblemTableCellRenderer();
        setDefaultRenderer(Problem.class, problemTableCellRenderer);

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model, getColumnModel(), 0, true);

    }

    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible.
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(350);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(300);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(100);
    }

    private class ProblemTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {

            Component defaultComp = super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);

            if (value instanceof Problem) {
                Problem problem = (Problem) value;
                if (column == 0) {
                    setText(problem.getDescription());
                } else if (column == 1) {
                    if (problem instanceof ResolvableProblem) {
                        ResolvableProblem solvableProblem = (ResolvableProblem) problem;
                        setText(solvableProblem.getResolutionDescription());
                    } else {
                        setText(Translation.getTranslation(
                            "folder_problem.table_model.not_available"));
                    }
                } else if (column == 2) {
                    setText(Format.formatDateShort(problem.getDate()));
                }
            }

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ColorUtil.EVEN_TABLE_ROW_COLOR
                        : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return defaultComp;
        }
    }

    private class TableHeaderMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof ProblemsTableModel) {
                    ProblemsTableModel problemsTableModel = (ProblemsTableModel) model;
                    problemsTableModel.sortBy(modelColumnNo);
                }
            }
        }
    }

}
