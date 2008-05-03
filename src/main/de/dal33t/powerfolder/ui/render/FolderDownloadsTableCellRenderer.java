package de.dal33t.powerfolder.ui.render;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renders a Directory, FileInfo or Status line for the DirectoryTable.
 *
 * @author <A HREF="mailto:hglasgow@powerfolder.com">Harry Glasgow</A>
 * @version $Revision: 3.1 $
 */
public class FolderDownloadsTableCellRenderer extends DefaultTableCellRenderer {
    private Controller controller;

    /**
     * Initalizes a FileTableCellrenderer upon a <code>FileListTableModel</code>
     *
     * @param controller the controller
     * @param tableModel the table model to act on
     */
    public FolderDownloadsTableCellRenderer(Controller controller) {
        this.controller = controller;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setIcon(null);
        setToolTipText(null);
        int columnInModel = UIUtil.toModel(table, column);
        if (value instanceof FileInfo) {
            return render((FileInfo) value, columnInModel, table,
                    isSelected, hasFocus, row, column);
        } else if (value instanceof String) {
            return render((String) value, columnInModel, table,
                    isSelected, hasFocus, row, column);
        }
        throw new IllegalStateException(
                "expected FileInfo, Directory or String not: "
                        + value.getClass().getName());

    }

    /**
     * renders the status line if folder empty
     */
    private Component render(String statusLine, int columnInModel,
                             JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        if (columnInModel == 0) {
            // filename column is the most visible so insert status line there..
            setHorizontalAlignment(LEFT);
            setToolTipText(statusLine);
            return super.getTableCellRendererComponent(table, statusLine,
                    isSelected, hasFocus, row, column);
        }
        // no text in other columns
        return super.getTableCellRendererComponent(table, "", isSelected,
                hasFocus, row, column);

    }

    private Component render(FileInfo fileInfo, int columnInModel,
                             JTable table, boolean isSelected, boolean hasFocus, int row, int column) {
        String newValue = "";
        switch (columnInModel) {
            case 0: { // filename

                newValue = fileInfo.getFilenameOnly();
                setHorizontalAlignment(LEFT);
                break;
            }
            case 1: { // file size
                newValue = Format.formatBytesShort(fileInfo.getSize());
                setToolTipText(String.valueOf(fileInfo.getSize()));
                setHorizontalAlignment(RIGHT);
                break;
            }
            case 2: { // member nick
                MemberInfo member = fileInfo.getModifiedBy();
                newValue = member.nick;
                setIcon(Icons.getSimpleIconFor(member.getNode(controller)));
                setHorizontalAlignment(LEFT);
                break;
            }
            case 3: {// modified date
                newValue = Format.formatDate(fileInfo.getModifiedDate());
                setHorizontalAlignment(RIGHT);
                break;
            }
        }
        return super.getTableCellRendererComponent(table, newValue, isSelected,
                hasFocus, row, column);
    }
}