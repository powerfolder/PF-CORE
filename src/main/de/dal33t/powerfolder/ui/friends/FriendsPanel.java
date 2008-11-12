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
package de.dal33t.powerfolder.ui.friends;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.actionold.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays all friends in a list.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FriendsPanel extends PFUIPanel {

    private FriendsQuickInfoPanel quickinfo;

    /** this panel */
    private JComponent panel;
    /** bottom toolbar */
    private JComponent toolbar;

    /** the ui of the list of friends. */
    private FriendsTable friendsTable;
    private JScrollPane friendsPane;

    private JComboBox statusFilterCombo;

    // Actions
    private ChatAction chatAction;
    private Action findFriendsAction;
    private ChangeFriendStatusAction changeFriendStatusAction;
    private SelectionModel memberSelectionModel;


    public FriendsPanel(Controller controller) {
        super(controller);

        memberSelectionModel = new SelectionModel();
        changeFriendStatusAction = new ChangeFriendStatusAction(controller,
            memberSelectionModel);

    }

    public String getTitle() {
        return Translation.getTranslation("general.friend_list");
    }

    /** @return this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickinfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(friendsPane);
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        // Actions
        chatAction = new ChatAction();
        findFriendsAction = getUIController().getFindFriendAction();

        quickinfo = new FriendsQuickInfoPanel(getController(), Translation
            .getTranslation("general.friend_list"));

        friendsTable = new FriendsTable(getApplicationModel().getNodeManagerModel()
            .getFriendsTableModel());
        

        friendsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateActions();
                }
            });
        // Connect table to core
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        friendsPane = new JScrollPane(friendsTable);
        UIUtil.whiteStripTable(friendsTable);
        UIUtil.removeBorder(friendsPane);
        UIUtil.setZeroHeight(friendsPane);
        friendsTable.setupColumns();

        toolbar = createToolBar();

        friendsTable.addMouseListener(new PopupMenuOpener(createPopupMenu()));
        friendsTable.addMouseListener(new DoubleClickAction(chatAction));

        updateActions();
    }

    /**
     * Creates the bottom toolbar
     * 
     * @return The bottom tool bar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(findFriendsAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(chatAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(changeFriendStatusAction));
        bar.addRelatedGap();

        statusFilterCombo = new JComboBox();
        statusFilterCombo.addItem(Translation.getTranslation("friends_panel.show_all_friends_and_connected_lan"));
        statusFilterCombo.addItem(Translation.getTranslation("friends_panel.show_online_friends_only"));
        statusFilterCombo.addItem(Translation.getTranslation("friends_panel.show_online_friends_and_connected_lan"));

        NodeManagerModel model = getApplicationModel().getNodeManagerModel();
        final ValueModel hideOfflineUsersModel = model.getHideOfflineUsersModel();
        final ValueModel includeLanUsersModel = model.getIncludeLanUsersModel();
        if ((Boolean) hideOfflineUsersModel.getValue()) {
            if ((Boolean) includeLanUsersModel.getValue()) {
                // Show online friends and connected LAN computers
                statusFilterCombo.setSelectedIndex(2);
            } else {
                // Show online friends only
                statusFilterCombo.setSelectedIndex(1);
            }
        } else {
            // Show all friends and connected LAN computers
            statusFilterCombo.setSelectedIndex(0);
        }
        statusFilterCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selection = statusFilterCombo.getSelectedIndex();

                switch (selection) {
                    case 1:
                        // Show online friends only
                        hideOfflineUsersModel.setValue(true);
                        includeLanUsersModel.setValue(false);
                        break;

                    case 2:
                        // Show online friends and connected LAN computers
                        hideOfflineUsersModel.setValue(true);
                        includeLanUsersModel.setValue(true);
                        break;

                    case 0:
                    default:
                        // Show all friends and connected LAN computers
                        hideOfflineUsersModel.setValue(false);
                        includeLanUsersModel.setValue(true);
                        break;
                }
            }
        });
        bar.addFixed(statusFilterCombo);
        bar.addRelatedGap();

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);
        return barPanel;
    }

    /**
     * Creates the popup menu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
        popupMenu.add(chatAction);
        popupMenu.add(changeFriendStatusAction);
        return popupMenu;
    }

    // App logic **************************************************************

    /** called if button chat clicked or if in popupmenu selected */
    private void chatWithSelected() {
        synchronized (getApplicationModel().getNodeManagerModel()
            .getFriendsTableModel())
        {
            int[] selectedIndexes = friendsTable.getSelectedRows();
            // only single selection for chat
            if (selectedIndexes.length != 1) {
                return;
            }
            int index = selectedIndexes[0];
            Member member = (Member) getApplicationModel().getNodeManagerModel()
                .getFriendsTableModel().getDataAt(index);
            if (!getApplicationModel().getNodeManagerModel().hasMemberNode(member))
            {
                getApplicationModel().getNodeManagerModel().addChatMember(member);
            }
        }

        updateActions();
    }

    // Actions/Inner classes **************************************************

    /** The Chat action to preform for button and popup menu item */
    private class ChatAction extends BaseAction {
        public ChatAction() {
            super("open_chat", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            chatWithSelected();
        }
    }

    /**
     * Listens for changes in the friendlist
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateActions();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateActions();
        }

        public void friendAdded(NodeManagerEvent e) {
            updateActions();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateActions();
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    // UI Helper code *********************************************************

    /**
     * Updates the state of all actions upon the current selection
     */
    private void updateActions() {
        chatAction.setEnabled(false);
        changeFriendStatusAction.setEnabled(false);
        int[] selectedIndexes = friendsTable.getSelectedRows();

        // only single selection for chat and with connected members
        if (selectedIndexes.length == 1) {
            int index = selectedIndexes[0];
            Object item = getApplicationModel().getNodeManagerModel()
                .getFriendsTableModel().getDataAt(index);
            if (item instanceof Member) {
                Member member = (Member) item;
                chatAction.setEnabled(member.isCompleteyConnected());
            }
        }

        // if at least one member selected
        for (int index : selectedIndexes) {
            Object item = getApplicationModel().getNodeManagerModel()
                    .getFriendsTableModel().getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                changeFriendStatusAction.setEnabled(true);
                memberSelectionModel.setSelection(user);
                break;
            }
        }
    }

}
