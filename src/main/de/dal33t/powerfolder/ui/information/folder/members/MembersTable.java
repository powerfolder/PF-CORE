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
* $Id: MemberTable.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.members;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Table to display members of a folder.
 */
public class MembersTable extends JTable {

    /**
     * Constructor
     *
     * @param model
     */
    public MembersTable(MembersTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_FRIEND_CONNECTED).getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        setupColumns();

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Setup renderer
        MemberTableCellRenderer memberCellRenderer =
                new MemberTableCellRenderer();
        setDefaultRenderer(Member.class, memberCellRenderer);

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model, getColumnModel(), 1);
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        
        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(80);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(80);
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
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
                if (model instanceof MembersTableModel) {
                    MembersTableModel membersTableModel = (MembersTableModel) model;
                    membersTableModel.sortBy(modelColumnNo);
                }
            }
        }
    }

    private static class MemberTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {

            Component defaultComp = super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);

            if (value instanceof Member) {
                Member node = (Member) value;
                Icon icon = Icons.getIconFor(node);
                setIcon(icon);
            }

            return defaultComp;
        }
    }
}
