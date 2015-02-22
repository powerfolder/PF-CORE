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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Format;

/**
 * Table showing notices in dialog
 */
public class NoticesTable extends JTable {

    public NoticesTable(NoticesTableModel tableModel) {
        super(tableModel);
        setShowGrid(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(Notice.class, new MyDefaultTreeCellRenderer());

        setRowHeight(Icons.getIconById(Icons.INFORMATION).getIconHeight() + 3);

        // add sorting
        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Set table columns
        setupColumns();

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(tableModel,
            getColumnModel(), tableModel.getSortColumn(), tableModel
                .isSortAscending());
    }

    /**
     * Make the height same as viewport (JScrollPane) if bigger than this.
     *
     * @return
     */
    public boolean getScrollableTracksViewportHeight() {
        Container viewport = getParent();
        return viewport instanceof JViewport
            && getPreferredSize().height < viewport.getHeight();
    }

    private static class MyDefaultTreeCellRenderer extends
        DefaultTableCellRenderer
    {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            String myValue = "";
            boolean bold = false;
            if (value != null) {
                Notice notice = (Notice) value;
                setIcon(null);
                switch (column) {
                    case 0 : // severity
                        setHorizontalAlignment(LEFT);
                        Icon icon;
                        if (notice.getNoticeSeverity() == NoticeSeverity.INFORMATION)
                        {
                            icon = Icons.getIconById(Icons.INFORMATION);
                        } else if (notice.getNoticeSeverity() == NoticeSeverity.WARINING)
                        {
                            icon = Icons.getIconById(Icons.WARNING);
                        } else {
                            icon = Icons.getIconById(Icons.BLANK);
                        }
                        setIcon(icon);
                        break;
                    case 1 : // date
                        myValue = Format.formatDateShort(notice.getDate());
                        setHorizontalAlignment(RIGHT);
                        break;
                    case 2 : // summary
                        myValue = notice.getSummary();
                        setHorizontalAlignment(LEFT);
                        break;
                }

                bold = !notice.isRead();
            }

            Component c = super.getTableCellRendererComponent(table, myValue,
                isSelected, hasFocus, row, column);

            if (bold) {
                c.setFont(new Font(c.getFont().getFontName(), Font.BOLD, c
                    .getFont().getSize()));
            } else {
                c.setFont(new Font(c.getFont().getFontName(), Font.PLAIN, c
                    .getFont().getSize()));
            }

            if (!isSelected) {
                setBackground(row % 2 == 0
                    ? ColorUtil.EVEN_TABLE_ROW_COLOR
                    : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return c;
        }
    }

    /**
     * Listner on table header, takes care about the sorting of table
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof NoticesTableModel) {
                    NoticesTableModel noticesTableModel = (NoticesTableModel) model;
                    boolean freshSorted = noticesTableModel
                        .sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        noticesTableModel.reverseList();
                    }
                }
            }
        }
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        int size = Icons.getIconById(Icons.INFORMATION).getIconHeight() + 3;
        column.setPreferredWidth(size);
        column.setMinWidth(size);
        column.setMaxWidth(size);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(500);
    }
}