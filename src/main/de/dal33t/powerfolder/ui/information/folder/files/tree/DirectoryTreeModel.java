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
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectory;

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
        if (rootUO != null && rootUO.getDisplayName().equals(
                model.getRootFolderName())) {
            updateTree(model.getFilteredDirectory(), (DefaultMutableTreeNode) getRoot());
        } else {
            // New tree. Node is the folder.
            DirectoryTreeNodeUserObject newRootUO =
                    new DirectoryTreeNodeUserObject(model.getRootFolderName(),
                            "", model.hasFilesDeep());
            DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(newRootUO);
            setRoot(newRoot);
            buildTree(model.getFilteredDirectory(), newRoot);
        }
    }

    /**
     * Builds a tree structure for a node from a directory model.
     *
     * @param directory
     * @param node
     */
    private void buildTree(FilteredDirectory directory,
                           DefaultMutableTreeNode node) {

        // Use a map to order the directories alphabetically
        Map<String, FilteredDirectory> map = new TreeMap<String,
                FilteredDirectory>();
        for (FilteredDirectory sub : directory.getList()) {
            map.put(sub.getDisplayName(), sub);
        }
        for (FilteredDirectory sub : map.values()) {
            if (sub.hasFilesDeep()) {
                boolean subHasNewFilesDeep = sub.hasNewFilesDeep();
                DirectoryTreeNodeUserObject newSubUO =
                        new DirectoryTreeNodeUserObject(sub.getDisplayName(), sub.getRelativeName(),
                                subHasNewFilesDeep);
                DefaultMutableTreeNode newSubNode =
                        new DefaultMutableTreeNode(newSubUO);
                insertNodeInto(newSubNode, node, node.getChildCount());
                buildTree(sub, newSubNode);
            }
        }
    }

    /**
     * Updated a tree structure for a node from a directory model.
     *
     * @param directory
     * @param node
     */
    private void updateTree(FilteredDirectory directory,
                            DefaultMutableTreeNode node) {

        DirectoryTreeNodeUserObject existingNode =
                (DirectoryTreeNodeUserObject) node.getUserObject();

        // Updating root - show folder name
        DirectoryTreeNodeUserObject candidateNode = new DirectoryTreeNodeUserObject(directory.getDisplayName(),
                directory.getRelativeName(), directory.hasNewFilesDeep());

        if (!candidateNode.equals(existingNode)) {
            node.setUserObject(candidateNode);
            nodeChanged(node);
        }

        // Search for missing nodes to insert.
        for (FilteredDirectory subDirectory : directory.getList()) {
            if (subDirectory.hasFilesDeep()) {
                boolean found = false;
                int childCount = node.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    DefaultMutableTreeNode subNode = (DefaultMutableTreeNode)
                            node.getChildAt(i);
                    DirectoryTreeNodeUserObject subUO =
                            (DirectoryTreeNodeUserObject) subNode.getUserObject();
                    if (subDirectory.getDisplayName().equals(
                            subUO.getDisplayName())) {
                        found = true;

                        // Side effect - this matching node needs to be updated.
                        updateTree(subDirectory, subNode);
                        break;
                    }
                }

                // Insert missing node.
                if (!found) {
                    boolean subNewFilesDeep = subDirectory.hasNewFilesDeep();
                    DirectoryTreeNodeUserObject newSubUO =
                            new DirectoryTreeNodeUserObject(subDirectory.getDisplayName(),
                                    subDirectory.getRelativeName(),
                                    subNewFilesDeep);
                    DefaultMutableTreeNode newSubNode =
                            new DefaultMutableTreeNode(newSubUO);
                    insertNodeInto(newSubNode, node, node.getChildCount());
                    buildTree(subDirectory, newSubNode);
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
            for (FilteredDirectory subDirectory : directory.getList()) {
                if (subDirectory.getDisplayName().equals(subUO.getDisplayName())) {
                    if (subDirectory.hasFilesDeep()) {
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
