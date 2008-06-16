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

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.TopLevelItem;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;
import de.dal33t.powerfolder.ui.navigation.RootNode;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains basic model elements for the application such as the (additional)
 * top level items.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ApplicationModel extends PFUIComponent {
    private NavTreeModel navTreeModel;
    private RootTableModel rootTabelModel;

    private List<TopLevelItem> topLevelItems = new ArrayList<TopLevelItem>();

    public ApplicationModel(Controller controller) {
        super(controller);
        navTreeModel = new NavTreeModel(controller);
        rootTabelModel = new RootTableModel(controller, navTreeModel);
    }

    // Exposing ***************************************************************

    public NavTreeModel getNavTreeModel() {
        return navTreeModel;
    }

    public RootTableModel getRootTabelModel() {
        return rootTabelModel;
    }

    // Top Level Item API *****************************************************

    /**
     * Adds a new top level item to be displayed.
     * 
     * @param item
     *            the item to be displayed.
     */
    public void addTopLevelItem(TopLevelItem item) {
        Reject.ifNull(item, "Item is null");
        // Model MAY contain null be model itself must never be null!
        Reject.ifNull(item.getTitelModel(), "Titel model of item is null");
        Reject.ifNull(item.getIconModel(), "Icon model of item is null");
        Reject.ifNull(item.getTooltipModel(), "Tooltip model of item is null");
        Reject.ifBlank(item.getPanelID(), "Panel ID of item is null");
        Reject.ifTrue(alreadyRegistered(item),
            "Top level item already added. panelID: " + item.getPanelID());
        topLevelItems.add(item);

        // Plug into nav tree.
        RootNode rootNode = navTreeModel.getRootNode();
        rootNode.addChild(item.getTreeNode());
        navTreeModel.fireTreeStructureChanged(new TreeModelEvent(this,
            new Object[]{rootNode}));
    }

    /**
     * @param treeNode
     *            the tree node of the top level item.
     * @return the top level item
     */
    public TopLevelItem getItemByTreeNode(TreeNode treeNode) {
        for (TopLevelItem item : topLevelItems) {
            if (item.getTreeNode() == treeNode) {
                return item;
            }
        }
        return null;
    }

    // Internal helper ********************************************************

    private boolean alreadyRegistered(TopLevelItem item) {
        for (TopLevelItem canidate : topLevelItems) {
            if (canidate == item) {
                return true;
            }
            if (canidate.getPanelID().equals(item.getPanelID())) {
                return true;
            }
        }
        return false;
    }
}
