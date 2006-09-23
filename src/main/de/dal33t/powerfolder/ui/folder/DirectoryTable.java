package de.dal33t.powerfolder.ui.folder;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.DirectoryTableCellRenderer;

/**
 * A table that acts on a Directory and uses a FileFilter Model to filter the
 * file list.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class DirectoryTable extends JTable {

    public DirectoryTable(Controller controller, FileFilterModel fileFilterModel)
    {
        super(new DirectoryTableModel(fileFilterModel));
        DirectoryTableModel directoryTableModel = (DirectoryTableModel) getModel();
        directoryTableModel.setTable(this);
        setDefaultRenderer(Directory.class, new DirectoryTableCellRenderer(
            controller, directoryTableModel));
        setColumnSizes();
        setRowHeight(Icons.NODE.getIconHeight() + 3);
        
    }

    /**
     * @param clear
     *            immediately clear the display list (eg use if new Folder is
     *            set)
     */
    public void setDirectory(Directory directory, boolean clear,
        Object[] oldSelections)
    {
        DirectoryTableModel directoryTableModel = (DirectoryTableModel) getModel();
        directoryTableModel.setDirectory(directory, clear, oldSelections);
    }

    public Directory getDirectory() {
        return getDirectoryTableModel().getDirectory();
    }

    public DirectoryTableModel getDirectoryTableModel() {
        return (DirectoryTableModel) getModel();
    }

    public void setColumnSizes() {
        int totalWidth = getWidth();

        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));
        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(totalWidth / 2);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(totalWidth / 8);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(totalWidth / 8);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(totalWidth / 8);
        column = getColumn(getColumnName(5));
        column.setPreferredWidth(20);
    }

    /**
     * Assumes table is contained in a JScrollPane. Scrolls the cell (rowIndex,
     * vColIndex) so that it is visible at the center of viewport.
     */
    public void scrollToCenter(int rowIndex, int vColIndex) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = getCellRect(rowIndex, vColIndex, true);

        // The location of the view relative to the table
        Rectangle viewRect = viewport.getViewRect();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }
}
