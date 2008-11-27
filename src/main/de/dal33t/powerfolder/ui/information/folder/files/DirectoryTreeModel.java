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
import de.dal33t.powerfolder.PFUIComponent;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree model to hold tree structure of the directory files.
 */
public class DirectoryTreeModel extends PFUIComponent implements TreeModel {

    private List<TreeModelListener> listeners;
    private MutableTreeNode rootNode;

    public DirectoryTreeModel(Controller controller) {
        super(controller);
        listeners = new ArrayList<TreeModelListener>();
        rootNode = new DefaultMutableTreeNode();
    }

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public Object getChild(Object parent, int index) {
        return null;
    }

    public int getChildCount(Object parent) {
        return 0;
    }

    public int getIndexOfChild(Object parent, Object child) {
        return 0;
    }

    public Object getRoot() {
        return rootNode;
    }

    public boolean isLeaf(Object node) {
        return true;
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public void setTree(FilteredDirectoryModel model) {
        DirectoryTreeNodeUserObject uo = new DirectoryTreeNodeUserObject(
                model.getDisplayName(), model.getFile());
        rootNode.setUserObject(uo);
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(new TreeModelEvent(this,
                    new Object[]{uo}));
        }
    }
}
