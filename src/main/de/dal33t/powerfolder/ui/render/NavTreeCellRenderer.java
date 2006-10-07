/* $Id: NavTreeCellRenderer.java,v 1.57 2006/03/25 03:52:04 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.render;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.model.FolderRepositoryModel;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
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
public class NavTreeCellRenderer extends DefaultTreeCellRenderer implements
    TreeCellRenderer
{
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
        Object parentObject = getParentObject(value);

        Icon icon = null;
        String text = null;
        String toolTip = null;
        NodeManagerModel nmModel = controller.getUIController()
            .getNodeManagerModel();
        FolderRepositoryModel folderRepoModel = controller.getUIController()
            .getFolderRepositoryModel();

        if (userObject instanceof RootNode) {
            // Render rootnode
            icon = Icons.ROOT;
            text = Translation.getTranslation("navtree.node", controller
                .getNodeManager().getMySelf().getNick());
        } else if (userObject instanceof Directory) {
            Directory directory = (Directory) userObject;
            // setIcon(Icons.getIconFor(directory, expanded));
            // this mimicks the behavior of windows file explorer:
            setIcon(Icons.getIconFor(directory, selected, controller));
            if (!selected) {
                if (directory.isDeleted()) {
                    setForeground(Color.RED);
                } else if (directory.isExpected(controller
                    .getFolderRepository()))
                {
                    setForeground(Color.GRAY);
                }
            }

        } else if (userObject instanceof Member) {
            Member node = (Member) userObject;

            // Get icon
            icon = treeBlinkManager.getIconFor(node, Icons.getIconFor(node));

            // General stuff (text)
            text = "";
            text += node.getNick() + " (";
            if (node.isMySelf()) {
                text += Translation.getTranslation("navtree.me");
            } else {
                text += node.isOnLAN() ? Translation
                    .getTranslation("general.localnet") : Translation
                    .getTranslation("general.inet");
            }

            if (node.getController().isVerbose() && node.getIdentity() != null
                && node.getIdentity().programVersion != null)
            {
                text += ", " + node.getIdentity().programVersion;
            }
            text += ")";
        } else if (userObject instanceof Folder) {
            Folder folder = (Folder) userObject;
            icon = treeBlinkManager.getIconFor(folder, Icons.getIconFor(
                controller, folder.getInfo()));
            text = folder.getName();
        } else if (userObject instanceof FolderInfo) {
            // TODO: Can be removed, obsolete since FolderDetails
            FolderInfo foInfo = (FolderInfo) userObject;
            icon = Icons.getIconFor(controller, foInfo);
            text = foInfo.name;
        } else if (userObject instanceof FolderDetails) {
            FolderDetails foDetails = (FolderDetails) userObject;
            icon = Icons.getIconFor(controller, foDetails.getFolderInfo());
            text = foDetails.getFolderInfo().name;
        } else if (value == folderRepoModel.getMyFoldersTreeNode()) {
            TreeNode node = (TreeNode) value;
            icon = Icons.FOLDERS;
            text = Translation.getTranslation("title.my.folders") + " ("
                + node.getChildCount() + ")";
        } else if (value == folderRepoModel.getPublicFoldersTreeNode()) {
            icon = Icons.FOLDERS_GRAY;
            int nPublicFolders = Math.max(controller.getFolderRepository()
                .getNumberOfNetworkFolder(), folderRepoModel
                .getPublicFoldersTreeNode().getChildCount());
            text = Translation.getTranslation("title.public.folders") + " ("
                + nPublicFolders + ")";
        } else if (userObject == RootNode.DOWNLOADS_NODE_LABEL) {
            TransferManager tm = controller.getTransferManager();
            text = Translation.getTranslation("general.downloads") + " ("
                + tm.getTotalDownloadCount() + ")";
            if (tm.getActiveDownloadCount() > 0) {
                icon = Icons.DOWNLOAD_ACTIVE;
            } else {
                icon = Icons.DOWNLOAD;
            }
        } else if (userObject == RootNode.UPLOADS_NODE_LABEL) {
            TransferManager tm = controller.getTransferManager();
            text = Translation.getTranslation("general.uploads") + " ("
                + tm.countUploads() + ")";
            if (tm.countUploads() > 0) {
                icon = Icons.UPLOAD_ACTIVE;
            } else {
                icon = Icons.UPLOAD;
            }
        } else if (userObject == RootNode.RECYCLEBIN_NODE_LABEL) {
            text = Translation.getTranslation("general.recyclebin") + " ("
                + controller.getRecycleBin().getSize() + ")";
            icon = Icons.RECYCLE_BIN;
        } else if (userObject == RootNode.DEBUG_NODE_LABEL) {
            text = "Debug";
            icon = Icons.DEBUG;
        } else if (value == nmModel.getFriendsTreeNode()) {
            text = Translation.getTranslation("rootpanel.friends") + " ("
                + nmModel.getFriendsTreeNode().getChildCount() + ")";
            icon = Icons.NODE;
        } else if (controller.isVerbose()
            && value == nmModel.getConnectedTreeNode())
        {
            text = Translation.getTranslation("navtree.onlinenodes", nmModel
                .getConnectedTreeNode().getChildCount()
                + "");
            icon = Icons.KNOWN_NODES;
        } else if (value == nmModel.getNotInFriendsTreeNodes()) {
            text = Translation.getTranslation("general.notonfriends") + " ("
                + nmModel.getNotInFriendsTreeNodes().getChildCount() + ")";
            icon = Icons.NODE_ORANGE;
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

    /**
     * Returns the parent object of that tree object.
     * 
     * @param possibleTreeNode
     * @return the userobject of parentnode of that treenode, or null if not
     *         available
     */
    private Object getParentObject(Object possibleTreeNode) {
        if (possibleTreeNode instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) possibleTreeNode;
            return UIUtil.getUserObject(treeNode.getParent());
        }
        return null;
    }
}