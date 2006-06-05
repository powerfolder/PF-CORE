/* $Id: PublicFoldersTreeNode.java,v 1.2 2005/10/31 01:03:10 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.navigation;

import javax.swing.event.TreeModelEvent;

import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Treenode which holds all previewed unjoined folders. TODO: Maybe implement
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class PublicFoldersTreeNode extends TreeNodeList {
    private NavTreeModel navTreeModel;

    public PublicFoldersTreeNode(NavTreeModel navTreeModel) {
        super(navTreeModel.getRootNode());
        this.navTreeModel = navTreeModel;
    }

    public void addChildAt(Object child, int index) {
        if (indexOf(child) >= 0) {
            // Not add duplicate entry
            return;
        }
        if (!(child instanceof FolderDetails)) {
            throw new IllegalArgumentException(
                "PublicFoldersTreeNode only supports childs of type FolderDetails");
        }
        super.addChildAt(child, index);

        // Fire changes to ui
        log().warn("Adding to public folders: " + child);
        TreeModelEvent te = new TreeModelEvent(this, getPathTo());
        navTreeModel.fireTreeStructureChanged(te);
    }

    /**
     * Removes a child from the list, can be a treenode
     * 
     * @param child
     */
    public void removeChild(Object child) {
        if (!(child instanceof FolderDetails)) {
            throw new IllegalArgumentException(
                "PublicFoldersTreeNode only supports childs of type FolderDetails");
        }
        super.removeChild(child);

        // Fire changes to ui
        log().warn("Removing from public folders: " + child);
        TreeModelEvent te = new TreeModelEvent(this, getPathTo());
        navTreeModel.fireTreeStructureChanged(te);
    }
}
