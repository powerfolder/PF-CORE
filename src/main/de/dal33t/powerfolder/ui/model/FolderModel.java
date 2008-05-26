package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.Directory;

import javax.swing.tree.MutableTreeNode;
import java.util.List;

/**
 * UI-Model for a folder. Prepare folder data in a "swing-compatible" way. E.g.
 * as <code>TreeNode</code> or <code>TableModel</code>.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
public class FolderModel extends PFUIComponent {

    /** The folder associated with this model. */
    private Folder folder;

    /** The ui node */
    private TreeNodeList treeNode;

    /** The base directory model for this folder. */
    private DirectoryModel directoryModel;

    /**
     * Constructor. Takes controller and the associated folder.
     * 
     * @param controller
     * @param folder
     */
    public FolderModel(Controller controller, Folder folder) {
        super(controller);
        Reject.ifNull(folder, "Folder can not be null");
        this.folder = folder;
        folder.addFolderListener(new MyFolderListener());
    }

    /**
     * @return the treenode representation of this object.
     */
    public MutableTreeNode getTreeNode() {
        if (treeNode == null) {
            initialize();
        }
        return treeNode;
    }

    /**
     * Initialize the tree node and rebuild.
     */
    private void initialize() {
        treeNode = new TreeNodeList(folder, getController().getUIController()
            .getFolderRepositoryModel().getMyFoldersTreeNode());
        rebuild();
    }

    /**
     * Build up the directory tree from the folder.
     */
    private void rebuild() {
        synchronized (treeNode) {
            treeNode.removeAllChildren();
            List<Directory> subDirectories = folder.getDirectory()
                .listSubDirectories();
            for (Directory subDirectory : subDirectories) {
                directoryModel = new DirectoryModel(treeNode, subDirectory);
                treeNode.addChild(directoryModel);
                scanDirectory(subDirectory, directoryModel);
            }
        }
    }

    public DirectoryModel getDirectoryModel() {
        if (treeNode == null) {
            initialize();
        }
        return directoryModel;
    }

    private static void scanDirectory(Directory directory, DirectoryModel model)
    {
        List<Directory> subDirectories = directory.listSubDirectories();
        for (Directory subDirectory : subDirectories) {
            DirectoryModel subdirectoryModel = new DirectoryModel(model,
                subDirectory);
            model.addChild(subdirectoryModel);
            scanDirectory(subDirectory, subdirectoryModel);
        }
    }

    private class MyFolderListener implements FolderListener {

        public void statisticsCalculated(FolderEvent folderEvent) {
            rebuild();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            rebuild();
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            rebuild();
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            rebuild();
        }

        public void fileChanged(FolderEvent folderEvent) {
            rebuild();
        }

        public void filesDeleted(FolderEvent folderEvent) {
            rebuild();
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }
}
