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
package de.dal33t.powerfolder.ui.information.folder.files.tree;

import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tree model to hold tree structure of the directory files.
 */
public class DirectoryTreeModel extends DefaultTreeModel {

    public DirectoryTreeModel(TreeNode root) {
        super(root);
    }

    /**
     * Sets the model of the directory structure. Tree model is updated.
     * 
     * @param model
     */
    public void setTree(FilteredDirectoryModel model) {
        DirectoryTreeNodeUserObject rootUO =
                (DirectoryTreeNodeUserObject) ((DefaultMutableTreeNode) getRoot())
                        .getUserObject();
        if (rootUO != null && rootUO.getDisplayName().equals(model.getRootFolder().getName())) {
            updateTree(model, (DefaultMutableTreeNode) getRoot());
        } else {
            // New tree. Node is the folder.
            DirectoryTreeNodeUserObject newRootUO =
                    new DirectoryTreeNodeUserObject(model.getRootFolder().getName(),
                            "", model.hasDescendantNewFiles());
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

        // Use a map to order the directories alphabetically
        Map<String, FilteredDirectoryModel> map = new TreeMap<String,
                FilteredDirectoryModel>();
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            map.put(subModel.getFilenameOnly(), subModel);
        }
        for (FilteredDirectoryModel subModel : map.values()) {
            if (subModel.hasDescendantFiles()) {
                String subFilenameOnly = subModel.getFilenameOnly();
                String subRelativeName = subModel.getRelativeName();
                boolean subNewFiles = subModel.hasDescendantNewFiles();
                DirectoryTreeNodeUserObject newSubUO =
                        new DirectoryTreeNodeUserObject(subFilenameOnly,
                                subRelativeName, subNewFiles);
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

        DirectoryTreeNodeUserObject existingNode =
                (DirectoryTreeNodeUserObject) node.getUserObject();

        // Has the node changed? Like newFiles changed perhaps.
        DirectoryTreeNodeUserObject candidateNode;
        if (existingNode.getDisplayName().length() == 0) {

            // Updating root - show folder name
            candidateNode = new DirectoryTreeNodeUserObject(
                    model.getRootFolder().getName(), "",
                    model.hasDescendantNewFiles());
        } else {

            // Updating branch
            candidateNode = new DirectoryTreeNodeUserObject(
                    existingNode.getDisplayName(),
                    existingNode.getRelativeName(),
                    model.hasDescendantNewFiles());
        }
        if (!candidateNode.equals(existingNode)) {
            node.setUserObject(candidateNode);
            nodeChanged(node);
        }

        // Search for missing nodes to insert.
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {

            if (subModel.hasDescendantFiles()) {
                boolean found = false;
                int childCount = node.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)
                            node.getChildAt(i);
                    DirectoryTreeNodeUserObject subUO =
                            (DirectoryTreeNodeUserObject) subNode.getUserObject();
                    if (subModel.getRelativeName().equals(subUO.getRelativeName())) {
                        found = true;

                        // Side effect - this matching node needs to be updated.
                        updateTree(subModel, subNode);
                        break;
                    }
                }

                // Insert missing node.
                if (!found) {
                    String subFilenameOnly = subModel.getFilenameOnly();
                    String subRelativeName = subModel.getRelativeName();
                    boolean subNewFiles = subModel.hasDescendantNewFiles();
                    DirectoryTreeNodeUserObject newSubUO =
                            new DirectoryTreeNodeUserObject(subFilenameOnly,
                                    subRelativeName, subNewFiles);
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
                if (subModel.getRelativeName().equals(subUO.getRelativeName())) {
                    if (subModel.hasDescendantFiles()) {
                        found = true;
                        break;
                    }
                }
            }

            // Remove extra node.
            if (!found) {
                removeNodeFromParent(subNode);
            }
        }
    }
}
