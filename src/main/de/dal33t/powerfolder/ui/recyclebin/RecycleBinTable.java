package de.dal33t.powerfolder.ui.recyclebin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Shows the contents of the internal RecycleBin in a Table.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinTable extends JTable {
    private Controller controller;

    public RecycleBinTable(Controller controller,
        RecycleBinTableModel recycleBinTableModel)
    {
        super(recycleBinTableModel);
        this.controller = controller;
        // Table setup
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        setDefaultRenderer(FileInfo.class, new RecycleBinTableRenderer());
        // Set table columns
        setupColumns();
        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(recycleBinTableModel,
                        getColumnModel(), 0);

    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0)); // Folder
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(1)); // Type
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(2)); // File
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(3)); // Size
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(4)); // Modified
        column.setPreferredWidth(100);
    }

    private class RecycleBinTableRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            int columnInModel = UIUtil.toModel(table, column);
            FileInfo recycleBinFileInfo = (FileInfo) value;
            String newValue = null;
            FolderRepository repository = controller.getFolderRepository();
            if (controller.getRecycleBin().isInRecycleBin(recycleBinFileInfo)) {

                File diskFile = controller.getRecycleBin().getDiskFile(
                        recycleBinFileInfo);

                switch (columnInModel) {
                    case 0: { // folder
                        newValue = repository.getFolder(recycleBinFileInfo.getFolderInfo()).getName();
                        setIcon(Icons.FOLDER);
                        setHorizontalAlignment(LEFT);
                        break;
                    }
                    case 1: { // file
                        setIcon(Icons
                                .getIconFor(recycleBinFileInfo, controller));
                        break;
                    }
                    case 2: { // file
                        setIcon(null);
                        newValue = recycleBinFileInfo.getName();
                        setHorizontalAlignment(LEFT);
                        break;
                    }
                    case 3: {// size now file size on disk
                        newValue = Format.formatBytesShort(diskFile.length());
                        setIcon(null);
                        setHorizontalAlignment(RIGHT);
                        break;
                    }
                    case 4: { // modification date
                        newValue = Format.formatDate(new Date(diskFile
                                .lastModified()));
                        setIcon(null);
                        setHorizontalAlignment(RIGHT);
                        break;
                    }
                }
            } else {
                // file removed in the mean time fixes a NPE during removing
                // files from RB
                newValue = "";
                setIcon(null);
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
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
                if (model instanceof RecycleBinTableModel) {
                    RecycleBinTableModel recycleBinTableModel = (RecycleBinTableModel) model;
                    boolean freshSorted = recycleBinTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        recycleBinTableModel.reverseList();
                    }
                }
            }
        }
    }

}
