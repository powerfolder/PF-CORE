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
package de.dal33t.powerfolder.ui;

import java.awt.Component;

import javax.swing.tree.TreeNode;

import com.jgoodies.binding.value.ValueModel;

/**
 * A new item to be displayed in the application tree.
 * <p>
 * TODO #278 Add getter for element in MainWindow? getMainItem?
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface TopLevelItem {
    /**
     * Must have RootNode as root!
     * <p>
     * TODO #278 Not longer required.
     * 
     * @return the tree node for the navigation tree
     */
    TreeNode getTreeNode();

    /**
     * TODO #278 Still required?
     * 
     * @return a world wide unique panel id.
     */
    String getPanelID();

    /**
     * TODO #278 Rename to getInfoPanel for InformationWindow
     * 
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
