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
 * $Id: DirectoryTree.java 5177 2008-09-10 14:56:49Z harry $
 */
package de.dal33t.powerfolder.ui.dialog.directory;

import java.awt.Cursor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Class to render a tree of file system directories.
 *
 * NOTE: This class is package-private, not public, because it should only
 * be accessed through DirectoryChooser.
 */
class DirectoryTree extends JTree {

    private static final Logger log = Logger.getLogger(DirectoryTree.class.getName());

    /**
     * List of online folder names. Display with a different icon in the tree
     * if they do not exist.
     */
    private final List<String> onlineFolders;

    /**
     * Constructor
     *
     * @param newModel
     */
    DirectoryTree(TreeModel newModel, List<String> onlineFolders) {
        super(newModel);
        this.onlineFolders = onlineFolders;
    }

    /**
     * Expands a path to the initially supplied directory.
     *
     * @param file
     */
    public void initializePath(Path file) {
        if (file == null) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "No file supplied.");
            }
            doDefault();
            return;
        }

        // If the file is dud.
        if (Files.notExists(file)) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "File does not exist : " + file.toAbsolutePath());
            }
            doDefault();
            return;
        }

        // Set cursor to hourglass while drilling.
        Cursor c = CursorUtils.setWaitCursor(this);
        try {
            StringBuilder sb = new StringBuilder();

            // Split the path on file separator.
            StringTokenizer st = new StringTokenizer(file.toAbsolutePath().toString(),
                file.getFileSystem().getSeparator());

            // Add the root element as the first path element.
            TreeNode node = (TreeNode) getModel().getRoot();
            Collection<TreeNode> pathElements = new ArrayList<TreeNode>();
            pathElements.add(node);

            // Expand recursivly through each path element.
            boolean first = true;
            long depth = 0;
            while (st.hasMoreTokens()) {

                if ((OSUtil.isLinux() || OSUtil.isMacOS()) && first) {
                    // First element of a Linux box is '/'.
                    sb.append(file.getFileSystem().getSeparator());
                } else {
                    // Build file path
                    String next = st.nextToken();
                    sb.append(next).append(file.getFileSystem().getSeparator());
                }
                Path f = Paths.get(sb.toString());

                // Strange, but root files appear as hidden. Security?
                // So do not check hidden attribute at first level.
                if (Files.notExists(f) || !Files.isReadable(f) || !Files.isDirectory(f)
                    || Files.isHidden(f) && !first)
                {
                    // Abort if cannot access file at any level.
                    if (first) {
                        // Interesting. Why can we not access the root of an element?
                        if (log.isLoggable(Level.FINE)) {
                            String why = "Cannot access the base of " +
                                    file.toAbsolutePath() + " because" +
                                    (Files.exists(f) ? "" : " (file does not exist)") +
                                    (Files.isReadable(f) ? "" : " (file cannot be read)") +
                                    (Files.isDirectory(f) ? "" : " (file is not a directory)");
                            log.log(Level.FINE, why.trim());
                        }
                    }
                    doDefault();
                    return;
                }

                // Try to find this path in the tree.
                int count = node.getChildCount();
                boolean found = false;
                for (int i = 0; i < count; i++) {
                    TreeNode node1 = node.getChildAt(i);
                    if (node1 instanceof DirectoryTreeNode) {
                        DirectoryTreeNode dtn = (DirectoryTreeNode) node1;
                        Path dtnFile = (Path) dtn.getUserObject();
                        if (fileCompare(dtnFile, f)) {

                            // Set node for next loop.
                            node = node1;
                            pathElements.add(node);
                            dtn.scan(onlineFolders);
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
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Failed to navigate the tree depth "
                                + depth + " for " +
                                file.toAbsolutePath());
                    }
                    doDefault();
                    return;
                }
                depth++;
            }
        } catch (IOException e) {
            log.warning(e.getMessage());
        } finally {
            CursorUtils.returnToOriginal(this, c);
        }
    }

    /**
     * Can't display the idean path, so just show the roots, so the user does
     * not see a blank tree.
     */
    private void doDefault() {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Doing the default tree expansion.");
        }
        TreeNode node = (TreeNode) getModel().getRoot();
        if (node instanceof DefaultMutableTreeNode) {
            TreeNode[] path = {node};
            TreePath tp = new TreePath(path);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Expanding default root tree path...");
            }
            expandPath(tp);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Expanded default root tree path.");
            }
        }
    }

    /**
     * Utility method to compare two directories, ignoring trminating file
     * separator charactes.
     *
     * @param file1
     * @param file2
     * @return
     */
    private static boolean fileCompare(Path file1, Path file2) {
        String fileName1 = file1.toAbsolutePath().toString();
        while (fileName1.endsWith(file1.getFileSystem().getSeparator())) {
            fileName1 = fileName1.substring(0, fileName1.length() - 1);
        }
        String fileName2 = file2.toAbsolutePath().toString();
        while (fileName2.endsWith(file2.getFileSystem().getSeparator())) {
            fileName2 = fileName2.substring(0, fileName2.length() - 1);
        }
        return fileName1.equals(fileName2);
    }

    /**
     * Override tree expandPath to do a scan first, so that the nodes are
     * populated with subdirectories.
     *
     * @param path
     */
    public void expandPath(TreePath path) {
        Cursor c = CursorUtils.setWaitCursor(this);
        try {
            scanPath(path);
            super.expandPath(path);
        } finally {
            CursorUtils.returnToOriginal(this, c);
        }
    }

    /**
     * Ensure that all directories in a path are scanned, so that they are all
     * populated with subdirectories.
     *
     * @param path
     */
    private void scanPath(TreePath path) {
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {

            // Recurse, from root up.
            // Root element is not a DirectoryTreeNode
            if (parentPath.getLastPathComponent() instanceof DirectoryTreeNode)
            {
                scanPath(parentPath);
            }

            // Ensure the node is scanned.
            if (path.getLastPathComponent() instanceof DirectoryTreeNode) {
                DirectoryTreeNode dtn = (DirectoryTreeNode) path
                    .getLastPathComponent();
                if (!dtn.isScanned()) {
                    dtn.scan(onlineFolders);
                }
            }
        }
    }
}
