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
* $Id: FilesTreePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.tree;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilterListener;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryEvent;
import de.dal33t.powerfolder.ui.util.UIUtil;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;
import java.awt.*;

public class FilesTreePanel extends PFUIComponent implements DirectoryFilterListener {

    private JPanel uiComponent;
    private DirectoryTreeModel directoryTreeModel;
    private JTree tree;

    public FilesTreePanel(Controller controller) {
        super(controller);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        directoryTreeModel = new DirectoryTreeModel(root);
        tree = new JTree(directoryTreeModel);
        tree.setCellRenderer(new MyTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    public void addTreeSelectionListener(TreeSelectionListener l) {
        tree.addTreeSelectionListener(l);
    }

    public void removeTreeSelectionListener(TreeSelectionListener l) {
        tree.removeTreeSelectionListener(l);
    }

    /**
     * Builds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:30:grow",
                "fill:pref:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JScrollPane scrollPane = new JScrollPane(tree);
        // Whitestrip
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);
        builder.add(scrollPane, cc.xy(1, 1));

        uiComponent = builder.getPanel();
        uiComponent.setBorder(BorderFactory.createEtchedBorder());
    }

    public void adviseOfChange(FilteredDirectoryEvent event) {
        directoryTreeModel.setTree(event.getModel());
        if (event.isFolderChanged()) {
            // New folder - select root so the table has something to display.
            // If there are subdirectories, jump to first, then back to root,
            // opening up the tree to the first level.
            DefaultMutableTreeNode root =
                    (DefaultMutableTreeNode) directoryTreeModel.getRoot();
            int count = root.getChildCount();
            if (count > 0) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) root.getChildAt(0);
                tree.getSelectionModel().setSelectionPath(new TreePath(
                        new Object[] {directoryTreeModel.getRoot(), node}));
            }
            tree.getSelectionModel().setSelectionPath(new TreePath(
                    directoryTreeModel.getRoot()));
        } else {
            setSelection(event.getModel().getDirectoryRelativeName());
        }
    }

    public void adviseOfFilteringBegin() {
    }

    public void invalidate() {
        directoryTreeModel.setRoot(new DefaultMutableTreeNode());
    }

    /**
     * Set the selected tree node to this directory.
     *
     * @param relativeName
     */
    private void setSelection(String relativeName) {
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) directoryTreeModel.getRoot();
        int count = root.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) root.getChildAt(i);
            DirectoryTreeNodeUserObject userObject =
                    (DirectoryTreeNodeUserObject) node.getUserObject();
            drill(relativeName, node, userObject, 0);
        }
    }

    /**
     * Drill down the directory stucture and try to find the file.
     *
     * @param relativeName
     *              name of directory relative to root, like bob/test/sub
     * @param node
     * @param userObject
     */
    private void drill(String relativeName, DefaultMutableTreeNode node,
                       DirectoryTreeNodeUserObject userObject, int level) {

        if (level > 100) {
            // Catch evil tail-recursive parent-directory references.
            return;
        }

        if (userObject.getRelativeName().equals(relativeName)) {
            tree.setSelectionPath(new TreePath(node.getPath()));
        } else {
            // Recurse.
            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                DefaultMutableTreeNode subNode =
                        (DefaultMutableTreeNode) node.getChildAt(i);
                DirectoryTreeNodeUserObject subUserObject =
                        (DirectoryTreeNodeUserObject) subNode.getUserObject();
                drill(relativeName, subNode, subUserObject, level + 1);
            }
        }
    }

    private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
            Object userObject = UIUtil.getUserObject(value);
            if (userObject instanceof DirectoryTreeNodeUserObject) {
                DirectoryTreeNodeUserObject dtnuo =
                        (DirectoryTreeNodeUserObject) userObject;
                setText(dtnuo.getDisplayName());
                if (dtnuo.hasNewFilesDeep()) {
                    setFont(new Font(getFont().getName(), Font.BOLD,
                            getFont().getSize()));
                } else {
                    setFont(new Font(getFont().getName(), Font.PLAIN,
                            getFont().getSize()));
                }
                if (expanded) {
                    setIcon(Icons.getIconById(Icons.DIRECTORY_OPEN));
                } else {
                    setIcon(Icons.getIconById(Icons.DIRECTORY));
                }
            }
            return this;
        }
    }
}
