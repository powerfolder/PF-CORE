/* $Id: UploadsTable.java,v 1.2 2006/03/04 10:36:27 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.TransferTableCellRenderer;

/**
 * A Table for displaying the uploads.
 * 
 * @version $Revision: 1.2 $
 */
public class UploadsTable extends JTable {

    /**
     * Initalizes the table.
     * 
     * @param transferManager
     *            the transfermanager
     */
    public UploadsTable(TransferManager transferManager) {
        super(new UploadsTableModel(transferManager, true));

        // Table setup
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        // setFocusable(false);

        // Setup renderer
        TableCellRenderer transferTableCellRenderer = new TransferTableCellRenderer(
            transferManager.getController());
        setDefaultRenderer(FileInfo.class, transferTableCellRenderer);
        setDefaultRenderer(FolderInfo.class, transferTableCellRenderer);
        setDefaultRenderer(Transfer.class, transferTableCellRenderer);
        setDefaultRenderer(Member.class, transferTableCellRenderer);
        setDefaultRenderer(Long.class, transferTableCellRenderer);

        // Set table columns
        setupColumns();
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
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(5));
        column.setPreferredWidth(40);
    }
}