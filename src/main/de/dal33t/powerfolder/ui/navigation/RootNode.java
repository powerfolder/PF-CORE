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
package de.dal33t.powerfolder.ui.navigation;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * The root node of the navigation tree
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class RootNode extends TreeNodeList {

    private static final Logger log = Logger.getLogger(RootNode.class.getName());

    // All nodes un
    public static final String DOWNLOADS_NODE_LABEL = "DOWNLOADS_NODE";
    public static final String UPLOADS_NODE_LABEL = "UPLOADS_NODE";
    public static final String RECYCLEBIN_NODE_LABEL = "RECYCLEBIN_NODE";
    public static final String WEBSERVICE_NODE_LABEL = "WEBSERVICE_NODE";
    public static final String DEBUG_NODE_LABEL = "DEBUG_NODE";
    public static final String DIALOG_TESTING_NODE_LABEL = "DIALOG_TESTING";

    // FIXME The following should be refactored into own "Models" for the core
    // components.
    final DefaultMutableTreeNode RECYCLEBIN_NODE = new DefaultMutableTreeNode(
        RECYCLEBIN_NODE_LABEL);
    public final DefaultMutableTreeNode WEBSERVICE_NODE = new DefaultMutableTreeNode(
        WEBSERVICE_NODE_LABEL);
    final DefaultMutableTreeNode DEBUG_NODE = new DefaultMutableTreeNode(
        DEBUG_NODE_LABEL);
    final DefaultMutableTreeNode DIALOG_TESTING_NODE = new DefaultMutableTreeNode(
        DIALOG_TESTING_NODE_LABEL);

    private Controller controller;
    private boolean initalized;

    public RootNode(Controller controller) {
        super(null, null);
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
//        if (navTreeModel == null) {
//            throw new NullPointerException("Navtreemodel is null");
//        }
        this.controller = controller;
        initalized = false;
    }

    public int getChildCount() {
        initalizeChildren();
        return super.getChildCount();
    }

    public Enumeration children() {
        initalizeChildren();
        return super.children();
    }

    public TreeNode getChildAt(int childIndex) {
        initalizeChildren();
        return super.getChildAt(childIndex);
    }

    public int getIndex(TreeNode node) {
        initalizeChildren();
        return super.getIndex(node);
    }

    /**
     * Initalized the root node. done lazy
     */
    private void initalizeChildren() {
        if (initalized) {
            return;
        }
        log.finer("Initalizing Children");
        initalized = true;
        addChild(controller.getUIController().getApplicationModel()
                .getFolderRepositoryModel().getMyFoldersTreeNode());
        addChild(WEBSERVICE_NODE);
        addChild(RECYCLEBIN_NODE);
        addChild(controller.getUIController().getApplicationModel()
                .getTransferManagerModel().getDownloadsTreeNode());
        addChild(controller.getUIController().getApplicationModel()
                .getTransferManagerModel().getUploadsTreeNode());
        addChild(controller.getUIController().getApplicationModel()
                .getNodeManagerModel().getFriendsTreeNode());

        // Only add if verbose mode.
        if (controller.isVerbose()) {

            // Only show the connected user node if debug reports is true.
            if (ConfigurationEntry.DEBUG_REPORTS.getValueBoolean(controller)) {
                addChild(controller.getUIController().getApplicationModel()
                        .getNodeManagerModel().getConnectedTreeNode());
            }

            // Add debug node
            addChild(DEBUG_NODE);
        }

        // Only show dialog testing if test mode is true.
        if (ConfigurationEntry.DIALOG_TESTING.getValueBoolean(controller)) {
            addChild(DIALOG_TESTING_NODE);
        }
    }
}