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
package de.dal33t.powerfolder.util.ui.directory;

import de.dal33t.powerfolder.util.os.OSUtil;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.Cursor;
import java.io.File;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Class to render a tree of file system directories.
 */
public class DirectoryTree extends JTree {

    /**
     * Constructor
     *
     * @param newModel
     */
    public DirectoryTree(TreeModel newModel) {
        super(newModel);
    }

    /**
     * Expands a path to the initially supplied diredctory.
     *
     * @param file
     */
    public void initializePath(File file) {

        // If the file is dud, show the roots,
        // so the user does not see a blank tree.
        if (file == null || !file.exists()) {
            TreeNode node = (TreeNode) getModel().getRoot();
            if (node instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) node;
                TreeNode[] path = new TreeNode[] {node};
                TreePath tp = new TreePath(path);
                expandPath(tp);
            }
            return;
        }

        // Set cursor to hourglass while drilling.
        Cursor initialCursor = getCursor();
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            StringBuilder sb = new StringBuilder();

            // Split the path on file separator.
            StringTokenizer st = new StringTokenizer(file.getAbsolutePath(), File.separator);

            // Add the root element as the first path element.
            TreeNode node = (TreeNode) getModel().getRoot();
            Collection<TreeNode> pathElements = new ArrayList<TreeNode>();
            pathElements.add(node);

            // Expand recursivly through each path element.
            boolean first = true;
            while (st.hasMoreTokens()) {

                if ((OSUtil.isLinux() || OSUtil.isMacOS()) && first) {
                    // First element of a Linux box is '/'.
                    sb.append(File.separator);
                } else {
                    // Build file path
                    sb.append(st.nextToken()).append(File.separator);
                }
                File f = new File(sb.toString());

                // Strange, but root files appear as hidden. Security?
                // So do not check hidden attribute at first level.
                if (!f.exists() ||
                        !f.canRead() ||
                        !f.isDirectory() ||
                        f.isHidden() && !first) {

                    // Abort if cannot access file at any level.
                    return;
                }

                // Try to find this path in the tree.
                boolean found = false;
                for (int i = 0; i < node.getChildCount(); i++) {
                    TreeNode node1 = node.getChildAt(i);
                    if (node1 instanceof DirectoryTreeNode) {
                        DirectoryTreeNode dtn = (DirectoryTreeNode) node1;
                        File dtnFile = (File) dtn.getUserObject();
                        if (fileCompare(dtnFile, f)) {

                            // Set node for next loop.
                            node = node1;
                            pathElements.add(node);
                            dtn.scan();
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    // Do tree expansion to current path.
                    TreeNode[] path = new TreeNode[pathElements.size()];
                    int i = 0;
                    for (TreeNode pathElement : pathElements) {
                        path[i++] = pathElement;
                    }
                    TreePath tp = new TreePath(path);
                    expandPath(tp);
                    setSelectionPath(tp);
                    scrollPathToVisible(tp);
                    first = false;
                } else {

                    // Lost the thread.
                    // Perhaps file system changed since last time?
                    return;
                }
            }
        } finally {
            setCursor(initialCursor);
        }
    }

    /**
     * Utility method to compare two directories,
     * ignoring trminating file separator charactes.
     * @param file1
     * @param file2
     * @return
     */
    private static boolean fileCompare(File file1, File file2) {
        String fileName1 = file1.getAbsolutePath();
        while(fileName1.endsWith(File.separator)) {
            fileName1 = fileName1.substring(0, fileName1.length() - 1);
        }
        String fileName2 = file2.getAbsolutePath();
        while(fileName2.endsWith(File.separator)) {
            fileName2 = fileName2.substring(0, fileName2.length() - 1);
        }
        return fileName1.equals(fileName2);
    }

    /**
     * Override tree expandPath to do a scan first,
     * so that the nodes are populated with subdirectories.
     *
     * @param path
     */
    public void expandPath(TreePath path) {
        Cursor initialCursor = getCursor();
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            scanPath(path);
            super.expandPath(path);
        } finally {
            setCursor(initialCursor);
        }
    }

    /**
     * Ensure that all directories in a path are scanned,
     * so that they are all populated with subdirectories.
     *
     * @param path
     */
    private static void scanPath(TreePath path) {
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {

            // Recurse, from root up.
            // Root element is not a DirectoryTreeNode
            if (parentPath.getLastPathComponent() instanceof DirectoryTreeNode) {
                scanPath(parentPath);
            }

            // Ensure the node is scanned.
            if (path.getLastPathComponent() instanceof DirectoryTreeNode) {
                DirectoryTreeNode dtn = (DirectoryTreeNode) path.getLastPathComponent();
                if (!dtn.isScanned()) {
                    dtn.scan();
                }
            }
        }
    }
}
