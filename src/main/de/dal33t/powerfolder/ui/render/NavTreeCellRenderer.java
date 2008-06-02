/* $Id: NavTreeCellRenderer.java,v 1.57 2006/03/25 03:52:04 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.render;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.TopLevelItem;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.model.DirectoryModel;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Main renderer for nav tree
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.57 $
 */
public class NavTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Logger LOG = Logger
        .getLogger(NavTreeCellRenderer.class);
    private Controller controller;
    private BlinkManager treeBlinkManager;

    public NavTreeCellRenderer(Controller controller) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;
        UIController uiController = controller.getUIController();
        treeBlinkManager = uiController.getBlinkManager();
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus)
    {

        super.getTreeCellRendererComponent(tree, value, selected, expanded,
            false, row, hasFocus);

        Object userObject = UIUtil.getUserObject(value);
        // Object parentObject = getParentObject(value);

        NodeManagerModel nmModel = controller.getUIController()
            .getNodeManagerModel();
        FolderRepositoryModel folderRepoModel = controller.getUIController()
            .getFolderRepositoryModel();

        TopLevelItem item = null;
        if (value instanceof TreeNode) {
            item = controller.getUIController().getApplicationModel()
                .getItemByTreeNode((TreeNode) value);
        }

        Icon icon = null;
        String text = null;
        String toolTip = null;
        if (item != null) {
            icon = (Icon) item.getIconModel().getValue();
            text = (String) item.getTitelModel().getValue();
            toolTip = (String) item.getTooltipModel().getValue();
        } else if (userObject instanceof RootNode) {
            // Render root node
            icon = Icons.ROOT;
            text = Translation.getTranslation("navtree.node", controller
                .getNodeManager().getMySelf().getNick());
        } else if (userObject instanceof DirectoryModel) {
            DirectoryModel directoryModel = (DirectoryModel) userObject;
            setIcon(Icons.getIconFor(directoryModel.getDirectory(), selected,
                controller));
            if (!selected) {
                if (directoryModel.getDirectory().isDeleted()) {
                    setForeground(Color.RED);
                } else if (directoryModel.getDirectory().isExpected(
                    controller.getFolderRepository()))
                {
                    setForeground(Color.GRAY);
                }
            }

        } else if (userObject instanceof Member) {
            Member node = (Member) userObject;

            // Get icon
            icon = treeBlinkManager.getIconFor(node, Icons.getIconFor(node));

            // General stuff (text)
            text = node.getNick();

            if (node.isCompleteyConnected()) {
                text += " (";
                if (node.isMySelf()) {
                    text += Translation.getTranslation("navtree.me");
                    // } else if (!node.isCompleteyConnected()) {
                    // text +=
                    // Translation.getTranslation("general.disconnected");
                } else {
                    text += node.isOnLAN() ? Translation
                        .getTranslation("general.localnet") : Translation
                        .getTranslation("general.inet");
                }

                if (node.isSecure()) {
                    text += ", "
                        + Translation.getTranslation("pro.security.secure");
                }

                if (node.getController().isVerbose()
                    && node.getIdentity() != null
                    && node.getIdentity().getProgramVersion() != null)
                {

                    text += ", " + node.getIdentity().getProgramVersion();
                    if (node.isSupernode()) {
                        text += "*";
                    }
                }
                text += ")";
            }
        } else if (userObject instanceof Folder) {
            Folder folder = (Folder) userObject;

            icon = Icons.getIconFor(folder);
            // icon = treeBlinkManager.getIconFor(folder, icon);

            text = folder.getName();
        } else if (userObject instanceof FolderInfo) {
            FolderInfo foInfo = (FolderInfo) userObject;
            icon = Icons.FOLDER;
            text = foInfo.name;
        } else if (value == folderRepoModel.getMyFoldersTreeNode()) {
            TreeNode node = (TreeNode) value;
            icon = Icons.FOLDERS;
            text = Translation.getTranslation("title.my.folders") + " ("
                + node.getChildCount() + ')';
        } else if (userObject == RootNode.DOWNLOADS_NODE_LABEL) {
            TransferManager tm = controller.getTransferManager();
            text = Translation.getTranslation("general.downloads") + " ("
                + tm.getTotalDownloadCount() + ')';
            if (tm.getActiveDownloadCount() > 0) {
                icon = Icons.DOWNLOAD_ACTIVE;
            } else {
                icon = Icons.DOWNLOAD;
            }
        } else if (userObject == RootNode.UPLOADS_NODE_LABEL) {
            TransferManager tm = controller.getTransferManager();
            text = Translation.getTranslation("general.uploads") + " ("
                + tm.countAllUploads() + ')';
            if (tm.countLiveUploads() > 0) {
                icon = Icons.UPLOAD_ACTIVE;
            } else {
                icon = Icons.UPLOAD;
            }
        } else if (userObject == RootNode.RECYCLEBIN_NODE_LABEL) {
            text = Translation.getTranslation("general.recyclebin") + " ("
                + controller.getRecycleBin().countAllRecycledFiles() + ')';
            icon = Icons.RECYCLE_BIN;
        } else if (userObject == RootNode.WEBSERVICE_NODE_LABEL) {
            // text = Translation.getTranslation("general.webservice");
            text = controller.getOSClient().getServer().getNick();
            icon = Icons.WEBSERVICE;
        } else if (userObject == RootNode.DEBUG_NODE_LABEL) {
            text = "Debug";
            icon = Icons.DEBUG;
        } else if (userObject == RootNode.DIALOG_TESTING_NODE_LABEL) {
            text = "Dialog Testing";
            icon = Icons.DIALOG_TESTING;
        } else if (value == nmModel.getFriendsTreeNode()) {
            text = Translation.getTranslation("rootpanel.friends") + " ("
                + nmModel.getFriendsTreeNode().getChildCount() + ')';
            icon = Icons.NODE_FRIEND_CONNECTED;
        } else if (controller.isVerbose()
            && value == nmModel.getConnectedTreeNode())
        {
            text = Translation.getTranslation("navtree.onlinenodes", String
                .valueOf(nmModel.getConnectedTreeNode().getChildCount()));
            icon = Icons.KNOWN_NODES;
        } else {
            LOG.warn("Unknown content: " + userObject);
        }

        if (icon != null) {
            setIcon(icon);
            setDisabledIcon(icon);
        }

        if (text != null) {
            setText(text);
        }

        if (toolTip != null) {
            setToolTipText(toolTip);
        }

        return this;
    }
}