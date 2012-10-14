package de.dal33t.powerfolder.ui.wizard.table;

import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Format;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MultiFileRestoreTable extends JTable {

    /**
     * Constructor
     *
     * @param model
     */
    public MultiFileRestoreTable(MultiFileRestoreTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED).getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);
        setupColumns();
        setDefaultRenderer(FileInfo.class, new MyDefaultTreeCellRenderer());
        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(
                model, getColumnModel(), 0, true);
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(70);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(70);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(60);
    }

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof MultiFileRestoreTableModel) {
                    MultiFileRestoreTableModel restoreFilesTableModel
                            = (MultiFileRestoreTableModel) model;
                    boolean freshSorted = restoreFilesTableModel.sortBy(
                            modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        restoreFilesTableModel.reverseList();
                    }
                }
            }
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            FileInfo fileInfo = (FileInfo) value;
            setIcon(null);
            String myValue = "";
            switch (column) {
                case MultiFileRestoreTableModel.COL_FILE_NAME:  // file name
                    myValue = fileInfo.getFilenameOnly();
                    setHorizontalAlignment(LEFT);
                    break;
                case MultiFileRestoreTableModel.COL_MODIFIED_DATE: // modified date
                    myValue = Format.formatDateShort(fileInfo.getModifiedDate());
                    setHorizontalAlignment(RIGHT);
                    break;
                case MultiFileRestoreTableModel.COL_VERSION:  // version
                    myValue = Format.formatLong(fileInfo.getVersion());
                    setHorizontalAlignment(RIGHT);
                    break;
                case MultiFileRestoreTableModel.COL_SIZE:  // size
                    myValue = Format.formatBytesShort(fileInfo.getSize());
                    setHorizontalAlignment(RIGHT);
                    break;
            }

            Component c = super.getTableCellRendererComponent(table, myValue,
                    isSelected, hasFocus, row, column);

            if (!isSelected) {
                setBackground(row % 2 == 0 ? ColorUtil.EVEN_TABLE_ROW_COLOR
                        : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return c;
        }
    }
}
