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
* $Id: FileVersionsTable.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.versions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.ColorUtil;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.Controller;

/**
 * Table to display file versions of a file.
 */
public class FileVersionsTable extends JTable {

    private Controller controller;

    /**
     * Constructor
     *
     * @param model
     */
    public FileVersionsTable(FileVersionsTableModel model, Controller controller) {
        super(model);
        this.controller = controller;

        setColumnSelectionAllowed(false);
        setShowGrid(false);
        getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        setupColumns();

        setDefaultRenderer(FileInfo.class, new MyDefaultTreeCellRenderer());

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model, getColumnModel(), 0, false);
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(40);
    }

    public FileInfo getSelectedInfo() {
        int row = getSelectedRow();
        if (row >= 0) {
            FileVersionsTableModel model = (FileVersionsTableModel) getModel();
            return (FileInfo) model.getValueAt(row, 0);
        }
        return null;
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
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
                if (model instanceof FileVersionsTableModel) {
                    FileVersionsTableModel tableModel = (FileVersionsTableModel) model;
                    boolean freshSorted = tableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        tableModel.reverseList();
                    }
                }
            }
        }
    }

    private class MyDefaultTreeCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            FileInfo info = (FileInfo) value;
            String myValue = "";
            switch (column) {
                case 0:  // version
                    myValue = String.valueOf(info.getVersion());
                    setHorizontalAlignment(RIGHT);
                    break;
                case 1:  // size
                    myValue = Format.formatBytesShort(info.getSize());
                    setHorizontalAlignment(RIGHT);
                    break;
                case 2:  // date
                    myValue = Format.formatDateShort(info.getModifiedDate());
                    setHorizontalAlignment(RIGHT);
                    break;
            }

            Component c = super.getTableCellRendererComponent(table, myValue,
                    isSelected, hasFocus, row, column);

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ColorUtil.EVEN_TABLE_ROW_COLOR
                        : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return c;
        }
    }
}