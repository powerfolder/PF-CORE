package de.dal33t.powerfolder.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.ui.navigation.NavigationModel;

/** Events from the NavigationModel */
public class NavigationEvent extends EventObject {
    private TreePath treePath;
    public NavigationEvent(NavigationModel source, TreePath treePath) {
        super(source);
        this.treePath = treePath;        
    }
    
    public TreePath getTreePath() {
        return treePath;
    }
}
