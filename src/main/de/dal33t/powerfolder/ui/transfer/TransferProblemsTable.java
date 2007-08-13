package de.dal33t.powerfolder.ui.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferProblemBean;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.awt.Dimension;

/**
 * A Table for displaying the transfer problems.
 *
 * @author <a href="mailto:harryglasgow@gmail.com">Harry Glasgow</a>
 * @version $Revision: 2.0 $
 */
public class TransferProblemsTable extends JTable {

    /**
     * The controller
     */
    private Controller controller;

    /**
     * Initalizes
     *
     * @param transferManager the transfermanager
     */
    public TransferProblemsTable(Controller controller, final SelectionModel selectionModel) {
        super(new TransferProblemsTableModel(controller.getTransferManager()));
        this.controller = controller;

        // Table setup
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        // Setup renderer
        TableCellRenderer transferTableCellRenderer = new MyTableCellRenderer();
        setDefaultRenderer(Object.class, transferTableCellRenderer);

        // Set table columns
        setupColumns();

        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                selectionModel.setSelection(getModel().getValueAt(e.getFirstIndex(), 0));
            }
        });
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(totalWidth / 4);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(totalWidth / 4);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(totalWidth / 4);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(totalWidth / 4);
    }

    /**
     * Renderer to display the columns in the table.
     */
    private class MyTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {

            // Extract useful objects.
            TransferProblemsTableModel transferProblemsTableModel = (TransferProblemsTableModel) getModel();
            TransferProblemBean problemBean = transferProblemsTableModel.getTransferProblemItemAt(row);
            FileInfo fileInfo = problemBean.getFileInfo();
            Folder folder = controller.getFolderRepository().getFolder(fileInfo.getFolderInfo());

            // Default stuff.
            setIcon(null);
            String newValue = "";

            switch (column) {
                case 0:  // file type
                    Icon icon = Icons.getIconFor(fileInfo, controller);
                    setIcon(icon);
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 1: // filename
                    newValue = fileInfo.getFilenameOnly();
                    if (folder.getBlacklist().isIgnored(fileInfo)) {
                        setIcon(Icons.IGNORE);
                    }
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case 2: // folder
                    newValue = folder.getName();
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setIcon(Icons.getIconFor(controller, fileInfo.getFolderInfo()));
                    break;
                case 3: // date
                    newValue = Format.formatDate(fileInfo.getModifiedDate());
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                case 4: // problem
                    newValue = problemBean.getProblemDetail();
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
            }
            return super.getTableCellRendererComponent(table, newValue, isSelected,
                    hasFocus, row, column);
        }
    }
}
