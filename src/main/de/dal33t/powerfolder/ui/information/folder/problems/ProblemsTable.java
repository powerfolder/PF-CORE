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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ResolvableProblem;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
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
        setRowHeight(Icons.getIconById(Icons.PROBLEMS).getIconHeight() + 4);

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        ProblemTableCellRenderer problemTableCellRenderer = new ProblemTableCellRenderer();
        setDefaultRenderer(Problem.class, problemTableCellRenderer);

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model, getColumnModel(), 0, true);

        // The check-box column carries a select-all box in its header; keep it
        // in sync when individual boxes are (un)ticked.
        getColumnModel().getColumn(ProblemsTableModel.COL_CHECK)
            .setHeaderRenderer(new HeaderCheckBoxRenderer());
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                getTableHeader().repaint();
            }
        });
    }

    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible.
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        // Address the check-box and icon columns by index: both have an empty
        // header name, so looking them up by name would be ambiguous.
        int checkWidth = 30;
        TableColumn checkColumn = getColumnModel().getColumn(
            ProblemsTableModel.COL_CHECK);
        checkColumn.setPreferredWidth(checkWidth);
        checkColumn.setMinWidth(checkWidth);
        checkColumn.setMaxWidth(checkWidth);
        checkColumn.setCellRenderer(new CheckBoxRenderer());

        getColumnModel().getColumn(ProblemsTableModel.COL_ICON)
            .setPreferredWidth(Icons.getIconById(Icons.PROBLEMS).getIconWidth() + 4);
        getColumnModel().getColumn(ProblemsTableModel.COL_DESCRIPTION)
            .setPreferredWidth(350);
        getColumnModel().getColumn(ProblemsTableModel.COL_SOLUTION)
            .setPreferredWidth(300);
        getColumnModel().getColumn(ProblemsTableModel.COL_DATE)
            .setPreferredWidth(100);
    }

    /**
     * Renders the check-box column as a centred check box, following the table's
     * row striping and selection colours.
     */
    private static class CheckBoxRenderer extends JCheckBox implements
        TableCellRenderer
    {
        CheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(false);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0
                    ? ColorUtil.getEvenTableRowColor()
                    : ColorUtil.getOddTableRowColor()));
            return this;
        }
    }

    /**
     * Header renderer for the check-box column: a select-all check box that
     * reflects whether every problem is currently ticked, styled like a header.
     */
    private static class HeaderCheckBoxRenderer extends JCheckBox implements
        TableCellRenderer
    {
        HeaderCheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(false);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            setSelected(table.getModel() instanceof ProblemsTableModel
                && ((ProblemsTableModel) table.getModel()).isAllChecked());
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setBackground(header.getBackground());
                setForeground(header.getForeground());
                setFont(header.getFont());
            }
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }
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
                if (column == ProblemsTableModel.COL_ICON) {
                    setIcon(Icons.getIconById(Icons.WARNING));
                    setText("");
                } else if (column == ProblemsTableModel.COL_DESCRIPTION) {
                    setIcon(null);
                    setText(problem.getDescription());
                } else if (column == ProblemsTableModel.COL_SOLUTION) {
                    if (problem instanceof ResolvableProblem) {
                        ResolvableProblem solvableProblem = (ResolvableProblem) problem;
                        setText(solvableProblem.getResolutionDescription());
                    } else {
                        setText(Translation
                            .get(
                            "folder_problem.table_model.not_available"));
                    }
                } else if (column ==  ProblemsTableModel.COL_DATE) {
                    setText(Format.formatDateShort(problem.getDate()));
                }
            }

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ColorUtil.getEvenTableRowColor()
                        : ColorUtil.getOddTableRowColor());
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
                    // The check-box column: its header toggles select-all.
                    if (modelColumnNo == ProblemsTableModel.COL_CHECK) {
                        problemsTableModel.setAllChecked(
                            !problemsTableModel.isAllChecked());
                        tableHeader.repaint();
                        return;
                    }
                    problemsTableModel.sortBy(modelColumnNo);
                }
            }
        }
    }

}
