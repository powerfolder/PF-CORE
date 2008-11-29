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
* $Id: DirectoryTreeModel.java 5816 2008-11-12 13:13:43Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.File;

/**
 * Tree model to hold tree structure of the directory files.
 */
public class DirectoryTreeModel extends DefaultTreeModel {

    public DirectoryTreeModel(TreeNode root) {
        super(root);
    }

    /**
     * Sets the model of the directory structure. Tree model is updated.
     * @param model
     */
    public void setTree(FilteredDirectoryModel model) {
        DirectoryTreeNodeUserObject rootUO =
                (DirectoryTreeNodeUserObject) ((DefaultMutableTreeNode) getRoot())
                        .getUserObject();

        if (rootUO != null && rootUO.getFile().equals(model.getFile())) {
            updateTree(model, (DefaultMutableTreeNode) getRoot());
        } else {
            // New tree.
            DirectoryTreeNodeUserObject newRootUO =
                    new DirectoryTreeNodeUserObject(model.getDisplayName(),
                            model.getFile(), model.hasDescendantNewFiles());
            DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(newRootUO);
            setRoot(newRoot);
            buildTree(model, (DefaultMutableTreeNode) getRoot());
        }
    }

    /**
     * Builds a tree structure for a node from a directory model.
     *
     * @param model
     * @param node
     */
    private void buildTree(FilteredDirectoryModel model,
                           DefaultMutableTreeNode node) {
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            if (subModel.hasDescendantFiles()) {
                File subFile = subModel.getFile();
                String subDisplayName = subModel.getDisplayName();
                boolean subNewFiles = subModel.hasDescendantNewFiles();
                DirectoryTreeNodeUserObject newSubUO =
                        new DirectoryTreeNodeUserObject(subDisplayName, subFile,
                                subNewFiles);
                DefaultMutableTreeNode newSubNode =
                        new DefaultMutableTreeNode(newSubUO);
                insertNodeInto(newSubNode, node, node.getChildCount());
                buildTree(subModel, newSubNode);
            }
        }
    }

    /**
     * Updated a tree structure for a node from a directory model.
     *
     * @param model
     * @param node
     */
    private void updateTree(FilteredDirectoryModel model,
                            DefaultMutableTreeNode node) {

        // Search for missing nodes to insert.
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {

            if (subModel.hasDescendantFiles()) {
                boolean found = false;
                int childCount = node.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)
                            node.getChildAt(i);
                    DirectoryTreeNodeUserObject subUO = (DirectoryTreeNodeUserObject)
                            subNode.getUserObject();
                    if (subModel.getFile().equals(subUO.getFile())) {
                        found = true;

                        // Side effect - this matching node needs to be updated.
                        updateTree(subModel, subNode);
                        break;
                    }
                }
                // Insert missing nodes.
                if (!found) {
                    File subFile = subModel.getFile();
                    String subDisplayName = subModel.getDisplayName();
                    boolean subNewFiles = subModel.hasDescendantNewFiles();
                    DirectoryTreeNodeUserObject newSubUO =
                            new DirectoryTreeNodeUserObject(subDisplayName,
                                    subFile, subNewFiles);
                    DefaultMutableTreeNode newSubNode =
                            new DefaultMutableTreeNode(newSubUO);
                    insertNodeInto(newSubNode, node, node.getChildCount());
                    buildTree(subModel, newSubNode);
                }
            }
        }

        // Search for extra nodes to remove.
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)
                    node.getChildAt(i);
            DirectoryTreeNodeUserObject subUO = (DirectoryTreeNodeUserObject)
                    subNode.getUserObject();
            boolean found = false;
            for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
                if (subModel.getFile().equals(subUO.getFile())) {
                    if (subModel.hasDescendantFiles()) {
                        found = true;
                        break;
                    }
                }
            }

            // Remove extra nodes.
            if (!found) {
                removeNodeFromParent(subNode);
            }
        }
    }
}
