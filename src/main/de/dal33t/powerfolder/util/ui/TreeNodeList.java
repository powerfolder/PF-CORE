/* $Id: TreeNodeList.java,v 1.5 2005/11/19 23:21:17 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.ui;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.util.Loggable;

/**
 * A List which is accesible as treenode. Threadsafe.
 * <P>
 * TODO: Make a sub-class, which can act upon NavTreeModel. Changeevents should
 * be fired automatically
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class TreeNodeList extends Loggable implements MutableTreeNode {
    private TreeNode parent;
    private Object userObject;
    private Comparator comparator;
    private TreePath path;
    // List of the children, containing always TreeNodes
    private List<TreeNode> list = Collections
        .synchronizedList(new ArrayList<TreeNode>());

    public TreeNodeList(Object userObject, TreeNode parent) {
        this.userObject = userObject;
        this.parent = parent;
    }

    /**
     * Initalizes with no user object, this way it will return itself (this) as
     * userobject
     * 
     * @param parent
     */
    public TreeNodeList(TreeNode parent) {
        this(null, parent);
    }

    /**
     * Returns the treepath to this treenode, builds the path only once, does
     * not support changes in parent path. structure should stay the same
     * <p>
     * FIXME: Check this method
     * 
     * @return
     */
    public TreePath getPathTo() {
        if (path == null) {
            List<TreeNode> alist = new ArrayList<TreeNode>();
            TreeNode node = this;
            do {
                alist.add(0, node);
                node = node.getParent();
            } while (node != null);
            Object[] pathArr = new Object[alist.size()];
            alist.toArray(pathArr);
            path = new TreePath(pathArr);
        }
        return path;
    }

    /**
     * Sets the comparator. having a comparator set the list will automatically
     * resorted on any modification
     * 
     * @param newComparator
     */
    public void sortBy(Comparator newComparator) {
        Comparator oldComp = comparator;
        this.comparator = newComparator;
        if (newComparator != oldComp) {
            sort();
        }
    }

    /**
     * Sorts the list with the comparator
     */
    public void sort() {
        if (comparator != null) {
            synchronized (list) {
                Collections.sort(list, new WrappingComparator());
            }
        }
    }

    /**
     * Simple helper wrapper comparator
     */
    private class WrappingComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            o1 = UIUtil.getUserObject(o1);
            o2 = UIUtil.getUserObject(o2);
            return comparator.compare(o1, o2);
        }
    }

    /**
     * Adds a child to the list
     * 
     * @param child
     */
    public void addChild(Object child) {
        addChildAt(child, list.size());
    }

    /**
     * Adds a collection of childs
     * 
     * @param childs
     */
    public void addChilds(Collection childs) {
        for (Iterator it = childs.iterator(); it.hasNext();) {
            addChild(it.next());
        }
    }

    /**
     * Adds a child at the specified index
     * 
     * @param index
     * @param child
     */
    public void addChildAt(Object child, int index) {
        if (child == null) {
            return;
        }
        synchronized (list) {
            if (!(child instanceof TreeNode)) {
                // build treenode
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(child);
                node.setParent(this);
                child = node;
            }
            list.add(index, (TreeNode) child);
            sort();
        }
    }

    /**
     * Removes a child from the list, can be a treenode
     * 
     * @param child
     * @return true if the list contained the child.
     */
    public boolean removeChild(Object child) {
        if (child == null) {
            return false;
        }
        synchronized (list) {
            int index = indexOf(child);
            if (index >= 0) {
                return list.remove(index) != null;
            }
        }
        return false;
    }

    /**
     * Removes all children
     */
    public void removeAllChildren() {
        list.clear();
    }

    /**
     * Replances the userobject at a specified index
     * 
     * @param obj
     * @param i
     */
    public void setUserObjectAt(Object obj, int i) {
        addChildAt(obj, i);
        remove(i + 1);
    }

    /**
     * Answers if the list contains the obj
     * 
     * @param obj
     * @return
     */
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    /**
     * Answers the index of the element
     * 
     * @param obj
     * @return
     */
    public int indexOf(Object obj) {
        if (obj instanceof TreeNode) {
            return list.indexOf(obj);
        }
        synchronized (list) {
            // uhh, we have to search
            for (int i = 0; i < list.size(); i++) {
                Object next = list.get(i);
                next = UIUtil.getUserObject(next);
                if (obj.equals(next)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Answers the userobject of the child at index i
     * 
     * @param i
     * @return
     */
    public Object getChildUserObjectAt(int i) {
        return UIUtil.getUserObject(list.get(i));
    }

    /**
     * Returns the treenode of that children with the userobject. Returns null
     * if userobject was not found in child list
     * 
     * @param aUserObject
     * @return
     */
    public TreeNode getChildTreeNode(Object aUserObject) {
        synchronized (list) {
            int i = indexOf(aUserObject);
            if (i < 0) {
                return null;
            }
            return getChildAt(i);
        }
    }

    /*
     * TreeNode interface
     */

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    public int getChildCount() {
        return list.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    public boolean isLeaf() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#children()
     */
    public Enumeration children() {
        return Collections.enumeration(list);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#getParent()
     */
    public TreeNode getParent() {
        return parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#getChildAt(int)
     */
    public TreeNode getChildAt(int childIndex) {
        return list.get(childIndex);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    public int getIndex(TreeNode node) {
        return list.indexOf(node);
    }

    /**
     * @return
     */
    public Object getUserObject() {
        return userObject;
    }

    /**
     * @param object
     */
    public void setUserObject(Object object) {
        userObject = object;
    }

    /*
     * General
     */

    public String toString() {
        if (userObject != null) {
            return "" + userObject;
        }
        return super.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.MutableTreeNode#removeFromParent()
     */
    public void removeFromParent() {
        parent = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.MutableTreeNode#remove(int)
     */
    public void remove(int index) {
        list.remove(index);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.MutableTreeNode#remove(javax.swing.tree.MutableTreeNode)
     */
    public void remove(MutableTreeNode node) {
        removeChild(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.MutableTreeNode#setParent(javax.swing.tree.MutableTreeNode)
     */
    public void setParent(MutableTreeNode newParent) {
        parent = newParent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.MutableTreeNode#insert(javax.swing.tree.MutableTreeNode,
     *      int)
     */
    public void insert(MutableTreeNode child, int index) {
        addChildAt(child, index);
    }
}