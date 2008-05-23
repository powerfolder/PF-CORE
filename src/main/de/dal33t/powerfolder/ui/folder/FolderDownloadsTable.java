package de.dal33t.powerfolder.ui.folder;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.UnsortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.render.FolderDownloadsTableCellRenderer;

/**
 * A table that acts on downloads for a folder
 *
 * @author <A HREF="mailto:hglasgow@powerfolder.com">Harry Glasgow</A>
 * @version $Revision: 3.1 $
 */
public class FolderDownloadsTable extends JTable {

    public FolderDownloadsTable(Controller controller)
    {
        super(new FolderDownloadsTableModel(controller.getUIController()
                .getTransferManagerModel()));
        setDefaultRenderer(FileInfo.class, new FolderDownloadsTableCellRenderer(
            controller));
        setColumnSizes();
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        setShowGrid(false);

        // Associate a header renderer with all columns.
        UnsortedTableHeaderRenderer.associateHeaderRenderer(getColumnModel());
    }

    public FolderDownloadsTableModel getFolderDownloadsTableModel() {
        return (FolderDownloadsTableModel) getModel();
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