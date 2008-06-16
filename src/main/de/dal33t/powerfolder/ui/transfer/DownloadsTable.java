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
package de.dal33t.powerfolder.ui.transfer;

import java.awt.*;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.render.TransferTableCellRenderer;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;

/**
 * A Table for displaying the downloads.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class DownloadsTable extends JTable {

    /**
     * Initalizes
     * 
     * @param controller
     * @param autoCleanupModel
     */
    public DownloadsTable(TransferManagerModel model) {
        super(model.getDownloadsTableModel());

        // Table setup
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        // setFocusable(false);

        // Setup renderer
        TableCellRenderer transferTableCellRenderer = new TransferTableCellRenderer(
            model.getController());
        setDefaultRenderer(Download.class, transferTableCellRenderer);

        // Set table columns
        setupColumns();

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model.getDownloadsTableModel(), getColumnModel(), 1);

    }

    // Helper methods *********************************************************

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(80);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(5));
        column.setPreferredWidth(20);
    }
}