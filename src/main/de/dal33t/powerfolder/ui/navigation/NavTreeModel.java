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
package de.dal33t.powerfolder.ui.navigation;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.Set;

/**
 * The model for the navigation tree.
 * <p>
 * TODO Move remaining listener into "Model" for core components.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class NavTreeModel extends PFUIComponent implements TreeModel {
    private Set<TreeModelListener> listeners;
    private RootNode rootNode;

    public NavTreeModel(Controller controller) {
        super(controller);
        listeners = new HashSet<TreeModelListener>();

        // Listen on Recycle Bin
        controller.getRecycleBin().addRecycleBinListener(
            new MyRecycleBinListener());
    }

    // Component listener classes *********************************************

    private class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().RECYCLEBIN_NODE});
            fireTreeNodesChangedEvent(te);
        }

        public void fileRemoved(RecycleBinEvent e) {
            TreeModelEvent te = new TreeModelEvent(this, new Object[]{
                getRoot(), getRootNode().RECYCLEBIN_NODE});
            fireTreeNodesChangedEvent(te);
        }

        public void fileUpdated(RecycleBinEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    public RootNode getRootNode() {
        if (rootNode == null) {
//            // Init lazily
//            synchronized (this) {
//                if (rootNode == null) {
//                    rootNode = new RootNode(getController(), this);
//                    // rootNode.initalizeChildren();
//                }
//            }
        }
        return rootNode;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public Object getChild(Object parent, int index) {
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return ((TreeNode) parent).getIndex((TreeNode) child);
    }

    public Object getRoot() {
        return getRootNode();
    }

    public int getChildCount(Object parent) {
        return ((TreeNode) parent).getChildCount();
    }

    public boolean isLeaf(Object node) {
        if (node == this) {
            return true;
        }
        TreeNode treeNode = (TreeNode) node;
        return treeNode.getChildCount() == 0;
    }

    // Listener handling code *************************************************

    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    /**
     * Fires a treeStructureChanged on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeStructureChanged(final TreeModelEvent e) {
//        ControlQuarter controlQuarter = getController().getUIController()
//            .getControlQuarter();
//        if (controlQuarter == null) {
//            return;
//        }
//        final JTree tree = controlQuarter.getTree();
//        if (tree == null) {
//            return;
//        }
//
//        final TreePath path = e.getTreePath();
//        boolean expandedTmp = false;
//        boolean selectionExpandedTmp = false;
//        boolean selectionVisibleTmp = false;
//        if (path != null) {
//            expandedTmp = tree.isExpanded(path);
//        }
//        final TreePath selectedPath = tree.getSelectionPath();
//        if (selectedPath != null) {
//            selectionExpandedTmp = tree.isExpanded(selectedPath);
//            selectionVisibleTmp = tree.isVisible(selectedPath);
//        }
//        final boolean expandedFinal = expandedTmp;
//        final boolean selectionExpandedFinal = selectionExpandedTmp;
//        final boolean selectionVisibleFinal = selectionVisibleTmp;
//
//        for (TreeModelListener listener : listeners) {
//            listener.treeStructureChanged(e);
//        }
//
//        if (expandedFinal) {
//            tree.expandPath(path);
//        }
//        if (selectionExpandedFinal) {
//            tree.expandPath(selectedPath);
//        }
//        if (selectionVisibleFinal) {
//            tree.makeVisible(selectedPath);
//        }
//        TreePath lastExpanded = getController().getUIController()
//            .getControlQuarter().getLastExpandedPath();
//        if (lastExpanded != null) {
//            if (!tree.isExpanded(lastExpanded)) {
//                tree.expandPath(lastExpanded);
//            }
//        }
    }

    /**
     * Fires a changeevent on the nav ui tree
     * 
     * @param e
     */
    public void fireTreeNodesChangedEvent(final TreeModelEvent e) {
//        ControlQuarter controlQuarter = getController().getUIController()
//            .getControlQuarter();
//        if (controlQuarter == null) {
//            return;
//        }
//        final JTree tree = controlQuarter.getTree();
//        if (tree == null) {
//            return;
//        }
//        final TreePath path = e.getTreePath();
//        boolean expandedTmp = false;
//        boolean selectionExpandedTmp = false;
//        boolean selectionVisibleTmp = false;
//        if (path != null) {
//            expandedTmp = tree.isExpanded(path);
//        }
//        final TreePath selectedPath = tree.getSelectionPath();
//        if (selectedPath != null) {
//            selectionExpandedTmp = tree.isExpanded(selectedPath);
//            selectionVisibleTmp = tree.isVisible(selectedPath);
//        }
//        final boolean expandedFinal = expandedTmp;
//        final boolean selectionExpandedFinal = selectionExpandedTmp;
//        final boolean selectionVisibleFinal = selectionVisibleTmp;
//
//        for (TreeModelListener listener : listeners) {
//            listener.treeNodesChanged(e);
//        }
//
//        if (expandedFinal) {
//            tree.expandPath(path);
//        }
//        if (selectionExpandedFinal) {
//            tree.expandPath(selectedPath);
//        }
//        if (selectionVisibleFinal) {
//            tree.makeVisible(selectedPath);
//        }
    }
}