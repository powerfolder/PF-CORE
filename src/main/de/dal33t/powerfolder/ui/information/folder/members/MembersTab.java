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
* $Id: MembersTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.members;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UI component for the members information tab
 */
public class MembersTab extends PFUIComponent {

    private JPanel uiComponent;
    private MembersTableModel model;
    private JScrollPane scrollPane;
    private MyOpenChatAction openChatAction;
    private MyAddRemoveFriendAction addRemoveFriendAction;
    private MyReconnectAction reconnectAction;
    private MembersTable membersTable;
    private Member selectedMember;
    private JPopupMenu fileMenu;

    /**
     * Constructor
     *
     * @param controller
     */
    public MembersTab(Controller controller) {
        super(controller);
        controller.getNodeManager().addNodeManagerListener(new MyNodeManagerListener());
        model = new MembersTableModel(getController());
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        model.setFolderInfo(folderInfo);
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    public void initialize() {
        openChatAction = new MyOpenChatAction(getController());
        addRemoveFriendAction = new MyAddRemoveFriendAction(getController());
        reconnectAction = new MyReconnectAction(getController());
        membersTable = new MembersTable(model);
        membersTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        membersTable.getSelectionModel().addListSelectionListener(
                new MySelectionListener());
        scrollPane = new JScrollPane(membersTable);
        membersTable.addMouseListener(new TableMouseListener());

        // Whitestrip
        UIUtil.whiteStripTable(membersTable);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);

        enableOnSelection();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        fileMenu.add(openChatAction);
        fileMenu.add(addRemoveFriendAction);
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(scrollPane, cc.xy(2, 6));

        buildPopupMenus();

        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {

        enableOnSelection();

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(openChatAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(addRemoveFriendAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(reconnectAction));
        return bar.getPanel();
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
        int selectedRow = membersTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedMember = (Member) model.getValueAt(
                    membersTable.getSelectedRow(), 0);

            if (selectedMember.equals(getController().getMySelf())) {
                selectedMember = null;
                openChatAction.setEnabled(false);
                addRemoveFriendAction.setEnabled(false);
                reconnectAction.setEnabled(false);
            } else {
                openChatAction.setEnabled(true);
                reconnectAction.setEnabled(true);
                addRemoveFriendAction.setEnabled(true);
                if (selectedMember.isFriend()) {
                    addRemoveFriendAction.setAdd(false);
                } else {
                    addRemoveFriendAction.setAdd(true);
                }
            }
        } else {
            selectedMember = null;
            openChatAction.setEnabled(false);
            addRemoveFriendAction.setEnabled(false);
            reconnectAction.setEnabled(false);
        }
    }

    /**
     * Method to redisplay the table if one of our members.
     */
    private void redisplay(Member node) {
        boolean found = false;
        if (node != null) {
            for (int i = 0; i < model.getRowCount(); i++) {
                Member member = model.getMemberAt(i);
                if (member.equals(node)) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            // One of our folder members - redisplay.
            TableModelEvent event = new TableModelEvent(model);
            membersTable.tableChanged(event);
        }
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class MyOpenChatAction extends BaseAction {

        private MyOpenChatAction(Controller controller) {
            super("action_open_chat", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openChat(selectedMember.getInfo());
        }
    }

    /**
     * Class to detect table selection changes.
     */
    private class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableOnSelection();
        }
    }

    private class TableMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }

    }

    private class MyAddRemoveFriendAction extends BaseAction {

        private boolean add = true;

        private MyAddRemoveFriendAction(Controller controller) {
            super("action_add_friend", controller);
        }

        public void setAdd(boolean add) {
            this.add = add;
            if (add) {
                configureFromActionId("action_add_friend");
            } else {
                configureFromActionId("action_remove_friend");
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (add) {
                boolean askForFriendshipMessage = PreferencesEntry.
                        ASK_FOR_FRIENDSHIP_MESSAGE.getValueBoolean(getController());
                if (askForFriendshipMessage) {

                    // Prompt for personal message.
                    String[] options = {
                            Translation.getTranslation("general.ok"),
                            Translation.getTranslation("general.cancel")};

                    FormLayout layout = new FormLayout("pref", "pref, 3dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    String nick = selectedMember.getNick();
                    String text = Translation.getTranslation(
                            "friend.search.personal.message.text2", nick);
                    builder.add(new JLabel(text), cc.xy(1, 1));
                    JTextArea textArea = new JTextArea();
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 3));
                    JPanel innerPanel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory.genericDialog(
                            getController(),
                            Translation.getTranslation("friend.search.personal.message.title"),
                            innerPanel, options, 0, GenericDialogType.INFO,
                            Translation.getTranslation("general.neverAskAgain"));
                    if (response.getButtonIndex() == 0) { // == OK
                        String personalMessage = textArea.getText();
                        selectedMember.setFriend(true, personalMessage);
                    }
                    if (response.isNeverAskAgain()) {
                        // don't ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(
                                getController(), false);
                    }
                } else {
                    // Send with no personal messages
                    selectedMember.setFriend(true, null);
                }
            } else {
                selectedMember.setFriend(false, null);
            }
        }
    }

    private class MyReconnectAction extends BaseAction {

        MyReconnectAction(Controller controller) {
            super("action_reconnect", controller);
        }

        public void actionPerformed(ActionEvent e) {

            if (selectedMember == null) {
                return;
            }

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(getController());

            Runnable connector = new Runnable() {
                public void run() {

                    // Open connect dialog if ui is open
                    connectDialog.open(selectedMember.getNick());

                    // Close connection first
                    selectedMember.shutdown();

                    // Now execute the connect
                    try {
                        if (!selectedMember.reconnect()) {
                            throw new ConnectionException(Translation.getTranslation(
                                    "dialog.unable_to_connect_to_member",
                                selectedMember.getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled() && !selectedMember.isConnected()) {
                            // Show if user didn't cancel
                            ex.show(getController());
                        }
                    }

                    // Close dialog
                    connectDialog.close();
                }
            };

            // Start connect in anonymous thread
            new Thread(connector, "Reconnector to " + selectedMember.getNick()).start();
        }
    }

    private class MyNodeManagerListener implements NodeManagerListener {

        public void nodeRemoved(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void friendAdded(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void settingsChanged(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public void startStop(NodeManagerEvent e) {
            redisplay(e.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}