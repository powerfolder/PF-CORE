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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.NavigationEvent;
import de.dal33t.powerfolder.event.NavigationListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.TopLevelItem;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Holds the up/forward and back buttons and acts on a NavigationModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.12 $
 */

public class NavigationToolBar extends PFUIComponent implements
    NavigationListener
{
    private JToolBar toolbar;
    // buttons
    private JButton backButton;
    private JButton forwardButton;
    private JButton upButton;

    private NavigationModel navigationModel;

    public NavigationToolBar(Controller controller,
        NavigationModel navigationModel)
    {
        super(controller);
        this.navigationModel = navigationModel;
        navigationModel.addNavigationListener(this);
    }

    public JToolBar getUIComponent() {
        if (toolbar == null) {
            initComponents();

            toolbar = new JToolBar();
            toolbar.add(backButton);
            toolbar.add(forwardButton);
            toolbar.add(upButton);
        }
        return toolbar;
    }

    private void initComponents() {
        backButton = new JButton(Icons.ARROW_LEFT);
        backButton.setDisabledIcon(Icons.ARROW_LEFT_GRAY);
        forwardButton = new JButton(Icons.ARROW_RIGHT);
        forwardButton.setDisabledIcon(Icons.ARROW_RIGHT_GRAY);
        upButton = new JButton(Icons.ARROW_UP);
        upButton.setDisabledIcon(Icons.ARROW_UP_GRAY);

        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        upButton.setEnabled(false);

        // no fancy background in this button
        backButton.setOpaque(false);
        forwardButton.setOpaque(false);
        upButton.setOpaque(false);

        backButton.setBorder(new EmptyBorder(2, 2, 2, 2));
        forwardButton.setBorder(new EmptyBorder(2, 2, 2, 2));
        upButton.setBorder(new EmptyBorder(2, 2, 2, 2));

        // listen for button clicks
        ButtonListener buttonListener = new ButtonListener();
        backButton.addActionListener(buttonListener);
        forwardButton.addActionListener(buttonListener);
        upButton.addActionListener(buttonListener);

        // to draw the raised border
        MouseListener mouseListener = new ButtonMouseListener();
        backButton.addMouseListener(mouseListener);
        forwardButton.addMouseListener(mouseListener);
        upButton.addMouseListener(mouseListener);
    }

    private void setButtonStates() {
        upButton.setEnabled(navigationModel.hasParent());
        backButton.setEnabled(navigationModel.hasBack());
        forwardButton.setEnabled(navigationModel.hasForward());

        String tooltext;
        Object up = navigationModel.peekUp();
        if (up != null) {
            tooltext = Translation.getTranslation("navigationbuttons.up_to",
                getText(up));
        } else {
            tooltext = Translation.getTranslation("navigationbuttons.up");
        }
        upButton.setToolTipText(tooltext);

        Object back = navigationModel.peekBack();
        if (back != null) {
            tooltext = Translation.getTranslation("navigationbuttons.back_to",
                getText(back));
        } else {
            tooltext = Translation.getTranslation("navigationbuttons.back");
        }
        backButton.setToolTipText(tooltext);

        Object forward = navigationModel.peekForward();
        if (forward != null) {
            tooltext = Translation.getTranslation(
                "navigationbuttons.forward_to", getText(forward));
        } else {
            tooltext = Translation.getTranslation("navigationbuttons.forward");
        }
        forwardButton.setToolTipText(tooltext);
    }

    private String getText(Object navObject) {
        Object userObject = UIUtil.getUserObject(navObject);

        TopLevelItem item = null;
        if (navObject instanceof TreeNode) {
            item = getApplicationModel()
                .getItemByTreeNode((TreeNode) navObject);
        }

        if (item != null) {
            return (String) item.getTitelModel().getValue();
        } else if (userObject instanceof RootNode) {
            return Translation.getTranslation("navtree.node", getController()
                .getNodeManager().getMySelf().getNick());
        } else if (userObject instanceof Directory) {
            Directory directory = (Directory) userObject;
            return directory.getName();
        } else if (userObject instanceof Member) {
            Member node = (Member) userObject;
            String text = "";
            text += node.getNick() + " (";
            if (node.isMySelf()) {
                text += Translation.getTranslation("navtree.me");
            } else {
                text += node.isOnLAN() ? Translation
                    .getTranslation("general.localnet") : Translation
                    .getTranslation("general.inet");
            }
            if (node.getController().isVerbose() && node.getIdentity() != null
                && node.getIdentity().getProgramVersion() != null)
            {
                text += ", " + node.getIdentity().getProgramVersion();
            }
            text += ")";
            return text;
        } else if (userObject instanceof Folder) {
            Folder folder = (Folder) userObject;
            return folder.getName() + " (" + folder.getMembersCount() + ')';
        } else if (navObject == getUIController().getFolderRepositoryModel()
            .getMyFoldersTreeNode())
        {
            TreeNode node = (TreeNode) navObject;
            return Translation.getTranslation("title.my.folders") + " ("
                + node.getChildCount() + ')';
        } else if (userObject == RootNode.DOWNLOADS_NODE_LABEL) {
            Object value = getController().getUIController()
                    .getTransferManagerModel().getAllDownloadsCountVM().getValue();
            return Translation.getTranslation("general.downloads") + " ("
                + (value == null ? "0" : value.toString()) + ')';
        } else if (userObject == RootNode.UPLOADS_NODE_LABEL) {
            Object value = getController().getUIController()
                    .getTransferManagerModel().getAllUploadsCountVM().getValue();
            return Translation.getTranslation("general.uploads") + " ("
                + (value == null ? "0" : value.toString()) + ')';
        } else if (userObject == RootNode.RECYCLEBIN_NODE_LABEL) {
            return Translation.getTranslation("general.recyclebin") + " ("
                + getController().getRecycleBin().countAllRecycledFiles() + ')';
        } else if (userObject == RootNode.WEBSERVICE_NODE_LABEL) {
            return getController().getOSClient().getServer().getNick();
            // return Translation.getTranslation("general.webservice");
        } else if (navObject == getUIController().getNodeManagerModel()
            .getFriendsTreeNode())
        {
            return Translation.getTranslation("rootpanel.friends")
                + " ("
                + getUIController().getNodeManagerModel().getFriendsTreeNode()
                    .getChildCount() + ')';
        } else if (getController().isVerbose()
            && navObject == getUIController().getNodeManagerModel()
                .getConnectedTreeNode())
        {
            return Translation.getTranslation("navtree.onlinenodes",
                    String.valueOf(getUIController().getNodeManagerModel()
                            .getConnectedTreeNode().getChildCount()));

        } else if (userObject == RootNode.DEBUG_NODE_LABEL) {
            return "Debug";
        } else {
            logWarning("Unknown content: " + userObject);
            return "";
        }
    }

    private class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == upButton) {
                navigationModel.up();
                setButtonStates();
            } else if (event.getSource() == backButton) {
                navigationModel.back();
                setButtonStates();
            } else if (event.getSource() == forwardButton) {
                navigationModel.forward();
                setButtonStates();
            }
        }
    }

    /** called from the NavigationModel */
    public void navigationChanged(NavigationEvent event) {
        setButtonStates();
    }

    private static void raiseButton(JButton button) {
        if (button.isEnabled()) {
            button.setBorder(BorderFactory.createRaisedBevelBorder());
        }
    }

    private static void lowerButton(JButton button) {
        button.setBorder(new EmptyBorder(2, 2, 2, 2));
    }

    private class ButtonMouseListener extends MouseAdapter {
        public void mouseEntered(MouseEvent e) {
            raiseButton((JButton) e.getSource());
        }

        public void mouseExited(MouseEvent e) {
            lowerButton((JButton) e.getSource());
        }
    }
}
