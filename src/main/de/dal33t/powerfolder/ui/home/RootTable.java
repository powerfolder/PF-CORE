package de.dal33t.powerfolder.ui.home;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.UnsortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps the root items to a table.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class RootTable extends JTable {

    private Controller controller;

    public RootTable(TableModel tableModel, Controller controller) {
        super(tableModel);
        this.controller = controller;
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 10);
        setShowGrid(false);
        setupColumns();
        setDefaultRenderer(Object.class, new RootTableRenderer());

        // Associate a header renderer with all columns.
        UnsortedTableHeaderRenderer.associateHeaderRenderer(getColumnModel());
    }

    private void setupColumns() {
        int totalWidth = getWidth();
        // otherwise the table header may not be visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));
        getTableHeader().setReorderingAllowed(true);
        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(400);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(40);
    }

    private class RootTableRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            int columnInModel = UIUtil.toModel(table, column);
            String newValue = "";
            TreeNode node = (TreeNode) value;
            Object userObject = UIUtil.getUserObject(value);
            if (columnInModel == 0) { // name
                if (value == controller.getUIController()
                    .getFolderRepositoryModel().getMyFoldersTreeNode())
                {
                    setIcon(Icons.FOLDERS);
                    newValue = Translation.getTranslation("title.my.folders");
                } else if (value == controller.getUIController()
                        .getFolderRepositoryModel().getPreviewFoldersTreeNode())
                    {
                        setIcon(Icons.PREVIEW_FOLDERS);
                        newValue = Translation.getTranslation("title.preview.folders");
                } else if (userObject == RootNode.DOWNLOADS_NODE_LABEL) {
                    TransferManager tm = controller.getTransferManager();
                    newValue = Translation.getTranslation("general.downloads");
                    if (tm.getActiveDownloadCount() > 0) {
                        setIcon(Icons.DOWNLOAD_ACTIVE);
                    } else {
                        setIcon(Icons.DOWNLOAD);
                    }
                } else if (userObject == RootNode.UPLOADS_NODE_LABEL) {
                    TransferManager tm = controller.getTransferManager();
                    newValue = Translation.getTranslation("general.uploads");
                    if (tm.countLiveUploads() > 0) {
                        setIcon(Icons.UPLOAD_ACTIVE);
                    } else {
                        setIcon(Icons.UPLOAD);
                    }
                } else if (userObject == RootNode.RECYCLEBIN_NODE_LABEL) {
                    newValue = Translation.getTranslation("general.recyclebin");
                    setIcon(Icons.RECYCLE_BIN);
                } else if (userObject == RootNode.WEBSERVICE_NODE_LABEL) {
                    newValue = Translation.getTranslation("general.webservice");
                    setIcon(Icons.WEBSERVICE);
                } else if (userObject == RootNode.DIALOG_TESTING_NODE_LABEL) {
                    newValue = "Dialog Testing";
                    setIcon(Icons.DIALOG_TESTING);
                } else if (userObject == RootNode.DEBUG_NODE_LABEL) {
                    newValue = "Debug";
                    setIcon(Icons.DEBUG);
                } else if (controller.isVerbose()
                    && value == controller.getUIController()
                        .getNodeManagerModel().getConnectedTreeNode())
                {
                    newValue = Translation
                        .getTranslation("rootpanel.connected_users");
                    setIcon(Icons.KNOWN_NODES);
                } else if (value == controller.getUIController()
                    .getNodeManagerModel().getFriendsTreeNode())
                {
                    newValue = Translation.getTranslation("rootpanel.friends");
                    setIcon(Icons.NODE_FRIEND_CONNECTED);
                } else if (value == controller.getUIController()
                    .getNodeManagerModel().getNotInFriendsTreeNodes())
                {
                    newValue = Translation
                        .getTranslation("general.notonfriends");
                    setIcon(Icons.NODE_NON_FRIEND_CONNECTED);
                }
            } else {// size
                setIcon(null);
                if (userObject == RootNode.DOWNLOADS_NODE_LABEL) {
                    TransferManager tm = controller.getTransferManager();
                    newValue = String.valueOf(tm.getTotalDownloadCount());
                } else if (userObject == RootNode.UPLOADS_NODE_LABEL) {
                    TransferManager tm = controller.getTransferManager();
                    newValue = String.valueOf(tm.countAllUploads());
                } else if (userObject == RootNode.RECYCLEBIN_NODE_LABEL) {
                    newValue = String.valueOf(controller.getRecycleBin().countAllRecycledFiles());
                } else {
                    newValue = String.valueOf(node.getChildCount());
                }
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }
}
