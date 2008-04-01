package de.dal33t.powerfolder.ui.previewFolders;

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
public class PreviewFoldersTable extends JTable {

    public PreviewFoldersTable(TableModel tableModel) {
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