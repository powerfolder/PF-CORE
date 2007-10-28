package de.dal33t.powerfolder.ui;

import java.awt.Component;

import javax.swing.tree.TreeNode;

import com.jgoodies.binding.value.ValueModel;

/**
 * A new item to be displayed in the application tree.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface TopLevelItem {
    /**
     * Must have RootNode as root!
     * 
     * @return the tree node for the navigation tree
     */
    TreeNode getTreeNode();

    /**
     * @return a world wide unique panel id.
     */
    String getPanelID();

    /**
     * @return the panel to be displayed in the information quarter.
     */
    Component getContentPanel();

    /**
     * @return the model containin the titel as <code>String</code>.
     */
    ValueModel getTitelModel();

    /**
     * @return the model holding the tooltip text as <code>String</code>.
     */
    ValueModel getTooltipModel();

    /**
     * @return model containing the <code>Icon</code>.
     */
    ValueModel getIconModel();
}
