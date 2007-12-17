/* $Id: DownloadsTable.java,v 1.3 2006/04/13 17:40:33 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.TransferTableCellRenderer;
import com.jgoodies.binding.value.ValueModel;

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
    public DownloadsTable(Controller controller, ValueModel autoCleanupModel) {
        super(new DownloadsTableModel(controller.getTransferManager(), autoCleanupModel));

        // Table setup
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        //setFocusable(false);

        // Setup renderer
        TableCellRenderer transferTableCellRenderer = new TransferTableCellRenderer(
            controller);
        setDefaultRenderer(Download.class, transferTableCellRenderer);
        
        // Set table columns
        setupColumns();
    }

    // Helper methods *********************************************************

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        //otherwise the table header is not visible:
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