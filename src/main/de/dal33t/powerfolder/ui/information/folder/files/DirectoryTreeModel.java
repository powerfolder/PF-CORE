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

import de.dal33t.powerfolder.Controller;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;

/**
 * Tree model to hold tree structure of the directory files.
 */
public class DirectoryTreeModel extends DefaultTreeModel {

    public DirectoryTreeModel(Controller controller, TreeNode root) {
        super(root);
    }

    public void setTree(FilteredDirectoryModel model) {
        DirectoryTreeNodeUserObject currentUserObject = (DirectoryTreeNodeUserObject)
                ((DefaultMutableTreeNode) getRoot()).getUserObject();
        DirectoryTreeNodeUserObject newUserObject = new DirectoryTreeNodeUserObject(
                model.getDisplayName(), model.getFile());

        if (currentUserObject != null &&
                currentUserObject.getFile().equals(newUserObject.getFile())) {
            updateTree(model, currentUserObject, newUserObject);
        } else {
            // New tree.
            ((MutableTreeNode) getRoot()).setUserObject(newUserObject);
            DirectoryTreeNodeUserObject rootUO =
                    new DirectoryTreeNodeUserObject(model.getDisplayName(),
                            model.getFile());
            DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(rootUO);
            setRoot(newRoot);
            buildTree((DefaultMutableTreeNode) getRoot(), model);
        }
    }

    private void buildTree(DefaultMutableTreeNode node,
                           FilteredDirectoryModel model) {
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            File subFile = subModel.getFile();
            String subDisplayName = subModel.getDisplayName();
            DirectoryTreeNodeUserObject subUserObject =
                    new DirectoryTreeNodeUserObject(subDisplayName, subFile);
            DefaultMutableTreeNode subNode =
                    new DefaultMutableTreeNode(subUserObject);
            insertNodeInto(subNode, node, node.getChildCount());
            buildTree(subNode, subModel);
        }
    }

    private void updateTree(FilteredDirectoryModel model,
                            DirectoryTreeNodeUserObject currentUserObject,
                            DirectoryTreeNodeUserObject newUserObject) {
        // @todo
    }
}
