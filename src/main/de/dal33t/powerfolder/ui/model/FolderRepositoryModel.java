/* $Id$
 */
package de.dal33t.powerfolder.ui.model;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.FolderComparator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Prepares core data as (swing) ui models. e.g. <code>TreeModel</code>
 * <p>
 * TODO Move table models for public and my folders in here.
 * 
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderRepositoryModel extends PFUIComponent {
    private NavTreeModel navTreeModel;
    // a list containing all joined folder
    private TreeNodeList myFoldersTreeNode;
    private TreeNodeList publicFolders;
    private MyFoldersTableModel myFoldersTableModel;

    public FolderRepositoryModel(Controller controller,
        NavTreeModel aNavTreeModel)
    {
        super(controller);
        Reject.ifNull(aNavTreeModel, "Nav tree model is null");
        navTreeModel = aNavTreeModel;

        TreeNode rootNode = navTreeModel.getRootNode();
        myFoldersTreeNode = new TreeNodeList(rootNode);
        myFoldersTreeNode.sortBy(new FolderComparator());

        publicFolders = new TreeNodeList(rootNode);

        // Table model initalization
        myFoldersTableModel = new MyFoldersTableModel(getController()
            .getFolderRepository());
    }

    // Inizalization **********************************************************

    /**
     * Initalizes the listener on the core components and sets an inital state
     * of the repo model. Adds all folder models as childs
     */
    public void initalize() {
        // Register listener
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());

        // Add inital model state
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            myFoldersTreeNode.addChild(folder.getTreeNode());
        }
    }

    // Exposing ***************************************************************

    public TreeNodeList getMyFoldersTreeNode() {
        return myFoldersTreeNode;
    }

    public TreeNodeList getPublicFoldersTreeNode() {
        return publicFolders;
    }

    /**
     * @return a tablemodel containing my folders
     */
    public MyFoldersTableModel getMyFoldersTableModel() {
        return myFoldersTableModel;
    }

    // Mutation ***************************************************************

    /**
     * Adds a folder to the public folders treenode and fires the tree changed
     * event
     * 
     * @param foDetails
     */
    public void addPublicFolder(FolderDetails foDetails) {
        Reject.ifNull(foDetails, "Folder details is null");
        if (publicFolders.indexOf(foDetails) >= 0) {
            // Not add duplicate entry
            return;
        }
        publicFolders.addChild(foDetails);

        // Fire changes to ui
        log().warn("Adding to public folders: " + foDetails);
        TreeModelEvent te = new TreeModelEvent(this, publicFolders.getPathTo());
        navTreeModel.fireTreeStructureChanged(te);
    }

    /**
     * Removes a folder from the public folders treenode and fires the tree
     * changed events
     * 
     * @param foDetails
     */
    public void removePublicFolder(FolderDetails foDetails) {
        Reject.ifNull(foDetails, "Folder details is null");
        publicFolders.removeChild(foDetails);

        // Fire changes to ui
        log().warn("Removing from public folders: " + foDetails);
        TreeModelEvent te = new TreeModelEvent(this, publicFolders.getPathTo());
        navTreeModel.fireTreeStructureChanged(te);
    }

    // Internal code **********************************************************

    /**
     * Listens for changes in the folder repository. Changes the prepared model
     * and fires the appropiate events on the swing models.
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void folderRemoved(FolderRepositoryEvent e) {
            myFoldersTreeNode.remove(e.getFolder().getTreeNode());

            // Fire tree model event
            TreeModelEvent te = new TreeModelEvent(e.getSource(),
                myFoldersTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(te);

            // Add folder to list of public folders
            if (!e.getFolder().isSecret()) {
                addPublicFolder(new FolderDetails(e.getFolder().getInfo()));
            }
        }

        public void folderCreated(FolderRepositoryEvent e) {
            if (myFoldersTreeNode.contains(e.getFolder().getTreeNode())) {
                return;
            }

            // Remove from public folders
            removePublicFolder(new FolderDetails(e.getFolder().getInfo()));

            myFoldersTreeNode.addChild(e.getFolder().getTreeNode());

            // Fire tree model event
            TreeModelEvent te = new TreeModelEvent(e.getSource(),
                myFoldersTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(te);
        }

        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
            TreeModelEvent te = new TreeModelEvent(this, publicFolders
                .getPathTo());
            navTreeModel.fireTreeStructureChanged(te);
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
        }

        public void scansStarted(FolderRepositoryEvent e) {
        }

        public void scansFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
