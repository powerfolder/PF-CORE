/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;

/**
 * UI component for the members information tab
 */
public class MembersExpertTab extends PFUIComponent implements MembersTab {

    private JPanel uiComponent;
    private MembersExpertTableModel model;
    private JScrollPane scrollPane;
    private BaseAction inviteAction;
    private Action openChatAction;
    private Action reconnectAction;
    private JButton refreshButton;
    private MembersExptertTable membersTable;
    private Member selectedMember;
    private JPopupMenu fileMenu;
    private JProgressBar refreshBar;
    private JComboBox defaultPermissionBox;

    /**
     * Constructor
     * 
     * @param controller
     */
    public MembersExpertTab(Controller controller) {
        super(controller);
        model = new MembersExpertTableModel(getController());
        model.sortBy(MembersExpertTableModel.COL_TYPE);
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.ui.information.folder.members.MembersTab#setFolderInfo(de.dal33t.powerfolder.light.FolderInfo)
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        model.setFolderInfo(folderInfo);
        inviteAction.allowWith(FolderPermission.admin(folderInfo));
    }

    /* (non-Javadoc)
     * @see de.dal33t.powerfolder.ui.information.folder.members.MembersTab#getUIComponent()
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    public void initialize() {
        inviteAction = new MyInviteAction();
        openChatAction = new MyOpenChatAction();
        reconnectAction = new MyReconnectAction();
        Action refreshAction = model.getRefreshAction();
        refreshButton = new JButton(refreshAction);

        refreshBar = new JProgressBar();
        refreshBar.setIndeterminate(true);
        refreshBar.setVisible(false);

        defaultPermissionBox = createdEditComboBox(model
            .getDefaultPermissionsListModel());

        membersTable = new MembersExptertTable(model);
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

        model.getRefreshingModel().addValueChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean refreshing = (Boolean) evt.getNewValue();
                    boolean permissionsRetrieved = model
                        .isPermissionsRetrieved();
                    // Cancel edit of current cell
                    membersTable.cancelCellEditing();
                    refreshBar.setVisible(refreshing);
                    refreshButton.setVisible(!refreshing);
                    boolean enabled = !refreshing && permissionsRetrieved;
                    FolderInfo folderInfo = model.getFolderInfo();
                    if (folderInfo != null) {
                        boolean admin = getController().getOSClient().getAccount()
                            .hasAdminPermission(folderInfo);
                        enabled = enabled && admin;
                    }
                    defaultPermissionBox.setEnabled(enabled);

                }
            });
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        fileMenu.add(openChatAction);
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow, 3dlu, pref");
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
        JButton inviteButton = new JButton(inviteAction);
        JButton openChatButton = new JButton(openChatAction);
        JButton reconnectButton = new JButton(reconnectAction);

        FormLayout layout = new FormLayout("0:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(refreshButton, cc.xy(1, 1));
        builder.add(refreshBar, cc.xy(1, 1));

        enableOnSelection();

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        if (ConfigurationEntry.SERVER_INVITE_ENABLED.getValueBoolean(
                getController())) {
            bar.addGridded(inviteButton);
        }
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            bar.addRelatedGap();
            bar.addGridded(openChatButton);
            bar.addRelatedGap();
            bar.addGridded(reconnectButton);
            bar.addRelatedGap();
            bar.addGridded(builder.getPanel());
        }
        JPanel buttonBarPanel = bar.getPanel();

        refreshButton.setMinimumSize(openChatButton.getMinimumSize());
        refreshButton.setMaximumSize(openChatButton.getMaximumSize());
        refreshButton.setPreferredSize(openChatButton.getPreferredSize());
        refreshBar.setMinimumSize(openChatButton.getMinimumSize());
        refreshBar.setMaximumSize(openChatButton.getMaximumSize());
        refreshBar.setPreferredSize(openChatButton.getPreferredSize());

        layout = new FormLayout(
            "pref, 0:grow, pref, 3dlu, pref, 3dlu, max(60dlu;pref)", "pref");
        builder = new PanelBuilder(layout);
        cc = new CellConstraints();
        builder.add(buttonBarPanel, cc.xy(1, 1));
        builder.add(Help.createWikiLinkButton(getController(),
            WikiLinks.SECURITY_PERMISSION), cc.xy(3, 1));
        builder.addLabel(Translation
            .getTranslation("folder_member.default_permission"), cc.xy(5, 1));
        builder.add(defaultPermissionBox, cc.xy(7, 1));

        return builder.getPanel();
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
        int selectedRow = membersTable.getSelectedRow();
        selectedMember = selectedRow >= 0 ? model.getMemberAt(membersTable
            .getSelectedRow()) : null;

        if (selectedMember != null) {
            if (selectedMember.equals(getController().getMySelf())) {
                openChatAction.setEnabled(false);
                reconnectAction.setEnabled(false);
            } else {
                openChatAction.setEnabled(true);
                reconnectAction.setEnabled(true);
            }
        } else {
            selectedMember = null;
            openChatAction.setEnabled(false);
            reconnectAction.setEnabled(false);
        }
    }

    private static JComboBox createdEditComboBox(
            SelectionInList<FolderPermission> folderPermissions) {
        return BasicComponentFactory.createComboBox(folderPermissions,
            new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                    Component comp = super.getListCellRendererComponent(list,
                        value, index, isSelected, cellHasFocus);
                    if (value instanceof FolderPermission) {
                        setText(((FolderPermission) value).getName());
                    } else {
                        setText(Translation.getTranslation(
                                "permissions.folder.no_access"));
                    }
                    return comp;
                }
            });
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    private class MyOpenChatAction extends BaseAction {

        private MyOpenChatAction() {
            super("action_open_chat", MembersExpertTab.this.getController());
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController()
                .openChat(selectedMember.getInfo());
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

    // Action to invite friend.
    private class MyInviteAction extends BaseAction {

        private MyInviteAction() {
            super("action_invite_friend", MembersExpertTab.this.getController());
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard.openSendInvitationWizard(getController(), model
                .getFolderInfo());
            model.refreshModel();
        }
    }

    private class MyReconnectAction extends BaseAction {

        MyReconnectAction() {
            super("action_reconnect", MembersExpertTab.this.getController());
            setIcon(null);
        }

        public void actionPerformed(ActionEvent e) {

            if (selectedMember == null) {
                return;
            }

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(
                getController(), UIUtil.getParentWindow(e));

            Runnable connector = new Runnable() {
                public void run() {

                    Member member = selectedMember;

                    // Open connect dialog if ui is open
                    connectDialog.open(member.getNick());

                    // Close connection first
                    member.shutdown();

                    // Now execute the connect
                    try {
                        if (member.reconnect().isFailure()) {
                            throw new ConnectionException(
                                Translation.getTranslation(
                                    "dialog.unable_to_connect_to_member",
                                    member.getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled()
                            && !member.isConnected())
                        {
                            // Show if user didn't cancel
                            ex.show(getController());
                        }
                    }

                    // Close dialog
                    connectDialog.close();
                }
            };

            // Start connect in anonymous thread
            new Thread(connector, "Reconnector to " + selectedMember.getNick())
                .start();
        }
    }
}