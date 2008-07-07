/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
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
import java.util.Collections;
import java.util.ArrayList;

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

    /** sub directory models */
    private final List<DirectoryModel> subdirectories = Collections
        .synchronizedList(new ArrayList<DirectoryModel>());

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
            // Cleanup
            int childs = treeNode.getChildCount();
            for (int i = 0; i < childs; i++) {
                DirectoryModel model = (DirectoryModel) treeNode.getChildAt(0);
                treeNode.remove(0);
                model.dispose();
            }
            List<Directory> folderSubdirectories = folder.getDirectory()
                .listSubDirectories();
            for (Directory subdirectory : folderSubdirectories) {
                DirectoryModel directoryModel = new DirectoryModel(treeNode,
                    subdirectory);
                treeNode.addChild(directoryModel);
                buildSubDirectoryModels(subdirectory, directoryModel);
                subdirectories.add(directoryModel);
            }
        }
    }

    public List<DirectoryModel> getSubdirectories() {
        return subdirectories;
    }

    private static void buildSubDirectoryModels(Directory directory,
        DirectoryModel model)
    {
        List<Directory> subDirectories = directory.listSubDirectories();
        for (Directory subDirectory : subDirectories) {
            DirectoryModel subdirectoryModel = new DirectoryModel(model,
                subDirectory);
            model.addChild(subdirectoryModel);
            buildSubDirectoryModels(subDirectory, subdirectoryModel);
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
            // Move into EDT, otherwise another thread might change the model
            // during update of Tree.
            return true;
        }
    }
}
