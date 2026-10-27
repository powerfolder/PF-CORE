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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
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
        // PFC-3657: the leading check-box column is how several notifications
        // are picked for a bulk "Mark read"; row selection stays single (Details).
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

        // The check-box column carries a select-all box in its header; keep it
        // in sync when individual boxes are (un)ticked.
        getColumnModel().getColumn(NoticesTableModel.COL_CHECK)
            .setHeaderRenderer(new HeaderCheckBoxRenderer());
        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                getTableHeader().repaint();
            }
        });
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
                    case 1 : // severity
                        setHorizontalAlignment(LEFT);
                        Icon icon;
                        if (notice.getNoticeSeverity() == NoticeSeverity.INFORMATION)
                        {
                            icon = Icons.getIconById(Icons.INFORMATION);
                        } else if (notice.getNoticeSeverity() == NoticeSeverity.WARNING)
                        {
                            icon = Icons.getIconById(Icons.WARNING);
                        } else {
                            icon = Icons.getIconById(Icons.BLANK);
                        }
                        setIcon(icon);
                        break;
                    case 2 : // date
                        myValue = Format.formatDateShort(notice.getDate());
                        setHorizontalAlignment(RIGHT);
                        break;
                    case 3 : // summary
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
                    ? ColorUtil.getEvenTableRowColor()
                    : ColorUtil.getOddTableRowColor());
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
                    // The check-box column: its header toggles select-all.
                    if (modelColumnNo == NoticesTableModel.COL_CHECK) {
                        noticesTableModel.setAllChecked(
                            !noticesTableModel.isAllChecked());
                        tableHeader.repaint();
                        return;
                    }
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
     * Renders the check-box column: a centred check box on real notice rows, and
     * a blank cell on the empty placeholder row (so it shows no stray check box).
     */
    private static class CheckBoxRenderer implements TableCellRenderer {
        private final JCheckBox checkBox = new JCheckBox();
        private final JLabel blank = new JLabel();

        CheckBoxRenderer() {
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPainted(false);
            checkBox.setOpaque(true);
            blank.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            Color bg = isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0
                    ? ColorUtil.getEvenTableRowColor()
                    : ColorUtil.getOddTableRowColor());
            boolean realRow = table.getModel() instanceof NoticesTableModel
                && ((NoticesTableModel) table.getModel()).getNoticeAt(row) != null;
            if (!realRow) {
                blank.setBackground(bg);
                return blank;
            }
            checkBox.setSelected(Boolean.TRUE.equals(value));
            checkBox.setBackground(bg);
            return checkBox;
        }
    }

    /**
     * Header renderer for the check-box column: a select-all check box that
     * reflects whether every notice is currently ticked, styled like a header.
     */
    private static class HeaderCheckBoxRenderer extends JCheckBox implements
        TableCellRenderer
    {
        HeaderCheckBoxRenderer() {
            setHorizontalAlignment(CENTER);
            setBorderPainted(false);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            setSelected(table.getModel() instanceof NoticesTableModel
                && ((NoticesTableModel) table.getModel()).isAllChecked());
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

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        // Address columns by index: the check-box and severity columns share an
        // empty header name, so looking them up by name would be ambiguous.
        int checkWidth = 30;
        TableColumn checkColumn = getColumnModel().getColumn(0);
        checkColumn.setPreferredWidth(checkWidth);
        checkColumn.setMinWidth(checkWidth);
        checkColumn.setMaxWidth(checkWidth);
        // Draw no check-box on the empty placeholder row.
        checkColumn.setCellRenderer(new CheckBoxRenderer());

        int size = Icons.getIconById(Icons.INFORMATION).getIconHeight() + 3;
        TableColumn severityColumn = getColumnModel().getColumn(1);
        severityColumn.setPreferredWidth(size);
        severityColumn.setMinWidth(size);
        severityColumn.setMaxWidth(size);

        getColumnModel().getColumn(2).setPreferredWidth(20); // date
        getColumnModel().getColumn(3).setPreferredWidth(500); // summary
    }
}
