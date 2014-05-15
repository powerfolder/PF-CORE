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
package de.dal33t.powerfolder.ui.information.downloads;

import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.render.DownloadManagerTableCellRenderer;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;

/**
 * A Table for displaying the downloads.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class DownloadManagersTable extends JTable {

    /**
     * Initalizes
     *
     * @param transferManagerModel
     */
    public DownloadManagersTable(TransferManagerModel transferManagerModel) {
        super(transferManagerModel.getDownloadsTableModel());

        // Table setup
        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED)
            .getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        // Setup renderer
        DownloadManagerTableCellRenderer renderer = new DownloadManagerTableCellRenderer(
            transferManagerModel.getController());
        setDefaultRenderer(DownloadManager.class, renderer);

        // Set table columns
        setupColumns();

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(transferManagerModel
            .getDownloadsTableModel(), getColumnModel(),
            DownloadManagersTableModel.COLPROGRESS, true);

        addHierarchyListener(new MyDisplayabilityListener());
    }

    private class MyDisplayabilityListener implements HierarchyListener {
        public void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == HierarchyEvent.DISPLAYABILITY_CHANGED)
            {
                boolean showing = e.getChanged().isShowing();
                DownloadManagersTableModel m = (DownloadManagersTableModel) getModel();
                m.setPeriodicUpdate(showing);
            }
        }
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