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

import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.util.Reject;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * UI-Model for a directory. Prepare directory data in a "swing-compatible" way.
 * E.g. as <code>TreeNode</code> or <code>TableModel</code>.
 * <p>
 * ATTENTION: MODEL is NOT THREAD SAFE! It should be modified by the EDT ONLY
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
public class DirectoryModel implements TreeNode {

    /** Parent node of this directory (DirectoryModel or FolderModel) */
    private TreeNode parent;

    /** The associated directory */
    private Directory directory;

    /** sub directory models */
    private List<DirectoryModel> subdirectories = new ArrayList<DirectoryModel>();

    /**
     * Constructor, taking parent and directory.
     * 
     * @param parent
     * @param directory
     */
    public DirectoryModel(TreeNode parent, Directory directory) {
        Reject.ifNull(parent, "Parent is null");
        Reject.ifNull(directory, "Directory is null");
        this.parent = parent;
        this.directory = directory;
    }

    public void sortSubDirectories() {
        Collections.sort(subdirectories, new Comparator());
    }

    /**
     * @return the associated directory
     */
    public Directory getDirectory() {
        return directory;
    }

    /**
     * Add a sub directory.
     * 
     * @param child
     */
    public void addChild(DirectoryModel child) {
        subdirectories.add(child);
    }

    public String toString() {
        return directory.getName();
    }

    public TreeNode getChildAt(int childIndex) {
        return subdirectories.get(childIndex);
    }

    public int getChildCount() {
        return subdirectories.size();
    }

    public TreeNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        if (!(node instanceof DirectoryModel)) {
            return -1;
        }
        return subdirectories.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return !subdirectories.isEmpty();
    }

    public Enumeration children() {
        return new MyChildrenEnum(subdirectories);
    }

    private static class MyChildrenEnum implements Enumeration {

        private List<DirectoryModel> children;
        private int index = -1;

        public MyChildrenEnum(List<DirectoryModel> children) {
            this.children = children;
        }

        public boolean hasMoreElements() {
            return index < children.size() - 1;
        }

        public Object nextElement() {
            return children.get(++index);
        }
    }

    /**
     * @param candidateDirectory
     * @return a tree node list for this folder up to the directory.
     */
    public TreeNode[] getPathTo(Directory candidateDirectory) {
        if (candidateDirectory.equals(directory)) {
            return new TreeNode[]{this};
        }

        for (DirectoryModel subDirectory : subdirectories) {
            TreeNode[] to = subDirectory.getPathTo(candidateDirectory);
            if (to != null) {
                TreeNode[] path = new TreeNode[to.length + 1];
                path[0] = this;
                System.arraycopy(to, 0, path, 1, to.length);
                return path;
            }
        }
        return null;
    }

    public static final class Comparator implements
        java.util.Comparator<DirectoryModel>
    {
        public int compare(DirectoryModel o1, DirectoryModel o2) {
            if (o1 == o2) {
                return 0;
            }
            return o1.getDirectory().getPath().compareToIgnoreCase(
                o2.getDirectory().getPath());
        }

    }
}
