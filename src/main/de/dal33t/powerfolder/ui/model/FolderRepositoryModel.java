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
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.FolderComparator;
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

    /**
     * @return a tablemodel containing my folders
     */
    public MyFoldersTableModel getMyFoldersTableModel() {
        return myFoldersTableModel;
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
        }

        public void folderCreated(FolderRepositoryEvent e) {
            if (myFoldersTreeNode.contains(e.getFolder().getTreeNode())) {
                return;
            }

            myFoldersTreeNode.addChild(e.getFolder().getTreeNode());

            // Fire tree model event
            TreeModelEvent te = new TreeModelEvent(e.getSource(),
                myFoldersTreeNode.getPathTo());
            navTreeModel.fireTreeStructureChanged(te);
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
