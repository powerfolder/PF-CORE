package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.disk.Directory;

import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * UI-Model for a directory. Prepare directory data in a
 * "swing-compatible" way. E.g. as <code>TreeNode</code> or
 * <code>TableModel</code>.
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
    private final List<DirectoryModel> subDirectories = Collections
            .synchronizedList(new ArrayList<DirectoryModel>());

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

    /**
     * Get the associated directory
     *
     * @return
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
        subDirectories.add(child);
    }

    public String toString() {
        return directory.getName();
    }

    public TreeNode getChildAt(int childIndex) {
        return subDirectories.get(childIndex);
    }

    public int getChildCount() {
        return subDirectories.size();
    }

    public TreeNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        if (!(node instanceof DirectoryModel)) {
            return -1;
        }
        return subDirectories.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return !subDirectories.isEmpty();
    }

    public Enumeration children() {
        return new MyChildrenEnum(subDirectories);
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


}
