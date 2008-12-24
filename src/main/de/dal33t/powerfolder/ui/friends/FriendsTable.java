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
package de.dal33t.powerfolder.ui.friends;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.model.FriendsNodeTableModel;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FriendsTable extends JTable {

    public FriendsTable(FriendsNodeTableModel tableModel) {
        super(tableModel);
        
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setShowGrid(false);
        setDefaultRenderer(Member.class, new MemberTableCellRenderer());
        // TODO Support multi selection. not possible atm
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(true);
        // add sorting
        getTableHeader().addMouseListener(
            new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(tableModel,
                        getColumnModel(), 0);
    }

    /**
     * Sets the column sizes of the table
     */
    public void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(
            new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(140);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(20);
    }

    /**
     * Listner on table header, takes care about the sorting of table
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public final void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof FriendsNodeTableModel) {
                    FriendsNodeTableModel nodeTableModel = (FriendsNodeTableModel) model;
                    boolean freshSorted = nodeTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        nodeTableModel.reverseList();
                    }
                }
            }
        }
    }
    
}
