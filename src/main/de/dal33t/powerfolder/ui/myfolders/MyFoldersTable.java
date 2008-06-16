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
package de.dal33t.powerfolder.ui.myfolders;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.UnsortedTableHeaderRenderer;

/**
 * Displays all Folders that are "joined" in a table.
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class MyFoldersTable extends JTable {

    public MyFoldersTable(TableModel tableModel) {
        super(tableModel);

        // Table setup.
        // Do not allow multi selection we don't want to leave more folders at
        // once
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // make sure the icons fit
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setShowGrid(false);
        setupColumns();

        // Associate a header renderer with all columns.
        UnsortedTableHeaderRenderer.associateHeaderRenderer(getColumnModel());

    }

    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));
        getTableHeader().setReorderingAllowed(true);
        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(40);

    }
}
