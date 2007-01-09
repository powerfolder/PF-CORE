package de.dal33t.powerfolder.ui;

import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.folder.FileFilterModel;
import de.dal33t.powerfolder.ui.render.OnePublicFolderTableCellRenderer;

/**
 * Holds a table that maps a FileInfo list to a Table 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class OnePublicFolderTable extends JTable {

    public OnePublicFolderTable(Controller controller, FileFilterModel fileFilterModel) {        
        super(new OnePublicFolderTableModel(fileFilterModel));
        setDefaultRenderer(FileInfo.class, new OnePublicFolderTableCellRenderer(controller));
        setColumnSizes();
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
    }
        
    public void setColumnSizes() {
        int totalWidth = getWidth();        
        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(
            new Dimension(totalWidth, 20));
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
}
