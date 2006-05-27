package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.FolderInfoComparator;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Util;

/**
 * Holds all public Folders in a table
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.9 $
 */
public class PublicFoldersTable extends JTable {
    private static final Logger LOG = Logger
        .getLogger(PublicFoldersTable.class);

    private Controller controller;

    public PublicFoldersTable(Controller controller,
        FolderInfoFilterModel folderInfoFilterModel)
    {
        // Panel
        super(new PublicFoldersTableModel(controller.getFolderRepository(),
            folderInfoFilterModel));
        this.controller = controller;

        // Table setup.
        // Do not allow multi selection we don't want to join more folders at
        // once
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // make sure the icons fit
        setRowHeight(Icons.NODE.getIconHeight() + 3);
        // Tables knows its renderer
        setDefaultRenderer(Folder.class, new PublicFolderTableCellRenderer());

        setupColumns();
        getPublicFoldersTableModel().sortBy(FolderInfoComparator.BY_AVAILABILITY);
    }

    /**
     * Returns the table model (casted) from this table
     * 
     * @return
     */
    public PublicFoldersTableModel getPublicFoldersTableModel() {
        return (PublicFoldersTableModel) getModel();
    }

    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));
        getTableHeader().setReorderingAllowed(true);
        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(180);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(80);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(20);
        column = getColumn(getColumnName(5));
        column.setPreferredWidth(40);

    }

    // renders the cell contents
    private class PublicFolderTableCellRenderer extends
        DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            TableColumnModel columns = table.getColumnModel();
            TableColumn tableColumn = columns.getColumn(column);
            int currentColumnWidth = tableColumn.getWidth();
            FolderInfo folderInfo = (FolderInfo) value;
            String newValue = "";
            setIcon(null);
            setToolTipText(null);

            // default alignment, overwite if cell needs other alignment
            setHorizontalAlignment(SwingConstants.RIGHT);

            switch (Util.toModel(table, column)) {
                case 0 : { // Folder (name)
                    newValue = folderInfo.name;
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setIcon(Icons.FOLDER_GREY);
                    setToolTipText(newValue);
                    break;
                }
                case 1 : { // Number of files
                    newValue = folderInfo.filesCount + "";
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 2 : {// size of folder
                    newValue = Format.formatBytesShort(folderInfo.bytesTotal);
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 3 : {// Members
                    FolderDetails details = folderInfo
                        .getFolderDetails(controller);
                    if (details == null) {
                        newValue = "?";
                    } else {
                        MemberInfo[] members = details.getMembers();
                        String separetor = "";
                        for (int i = 0; i < members.length; i++) {
                            if (members[i] == null) {
                                LOG.warn("Havining null member @ " + details);
                            }
                            newValue += separetor + members[i].nick;
                            separetor = ", ";                            
                        }

                        Component component = super
                            .getTableCellRendererComponent(table, newValue,
                                isSelected, hasFocus, row, column);
                        int prefWidth = component.getPreferredSize().width;
                        if (currentColumnWidth < prefWidth) {
                            newValue = Translation.getTranslation(
                                "myfolderstable.number_of_members",
                                members.length + "");
                        }
                        String toolTipValue = "<HTML><BODY>";
                        separetor = "";
                        for (int i = 0; i < members.length; i++) {
                            toolTipValue += separetor + members[i].nick;
                            separetor = "<BR>";
                            if (i>=15) {
                                toolTipValue +=separetor + " ...";
                                break;
                            }
                        }
                        toolTipValue += "</BODY></HTML>";
                        setToolTipText(toolTipValue);
                    }
                    break;
                }
                case 4 : { // Availibility
                    setHorizontalAlignment(SwingConstants.LEFT);
                    FolderDetails foDetails = controller.getFolderRepository()
                        .getFolderDetails(folderInfo);
                    int nOnline = -1;
                    if (foDetails != null) {
                        nOnline = foDetails.countOnlineMembers(controller);
                    }
                    if (nOnline >= 5) {
                        setIcon(Icons.FOLDER_SYNC_3);
                    } else if (nOnline < 5 && nOnline >= 3) {
                        setIcon(Icons.FOLDER_SYNC_2);
                    } else if (nOnline < 3 && nOnline >= 1) {
                        setIcon(Icons.FOLDER_SYNC_1);
                    } else if (nOnline == 0) {
                        setIcon(Icons.FOLDER_SYNC_0);
                    } else {
                        setIcon(Icons.FOLDER_SYNC_UNKNOWN);
                    }
                    newValue = nOnline + " online";
                    setToolTipText(nOnline + " Users of this folder online");
                    break;
                }
                case 5 : {// LastModified
                    FolderDetails details = folderInfo
                        .getFolderDetails(controller);
                    if (details == null) {
                        newValue = "";
                    } else {
                        newValue = Format.formatDate(details
                            .getLastModifiedDate());
                    }
                    break;
                }
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }

}
