package de.dal33t.powerfolder.ui.recyclebin;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
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
        setRowHeight(Icons.NODE.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        setDefaultRenderer(FileInfo.class, new RecycleBinTableRenderer());
        // Set table columns
        setupColumns();
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(40);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(20);
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
            File diskFile = controller.getRecycleBin().getDiskFile(
                recycleBinFileInfo);

            switch (columnInModel) {
                case 0 : { // folder
                    newValue = (repository.getFolder(recycleBinFileInfo
                        .getFolderInfo())).getName();
                    setIcon(Icons.getIconFor(controller, recycleBinFileInfo
                        .getFolderInfo()));
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case 1 : { // file
                    setIcon(Icons.getIconFor(recycleBinFileInfo, controller));
                    newValue = recycleBinFileInfo.getName();
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                }
                case 2 : {// size now file size on disk
                    newValue = Format.formatBytesShort(diskFile.length()) + "";
                    setIcon(null);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 3 : { // modification date
                    newValue = Format.formatDate(new Date(diskFile
                        .lastModified()))
                        + "";
                    setIcon(null);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }
}
