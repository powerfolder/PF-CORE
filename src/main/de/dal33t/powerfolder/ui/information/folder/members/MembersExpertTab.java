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

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
    private Action reconnectAction;
    private JButton refreshButton;
    private MembersExpertTable membersTable;
    private Member selectedMember;
    //private JPopupMenu fileMenu;
    private JProgressBar refreshBar;

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
        reconnectAction = new MyReconnectAction();
        Action refreshAction = model.getRefreshAction();
        refreshButton = new JButton(refreshAction);

        refreshBar = new JProgressBar();
        refreshBar.setIndeterminate(true);
        refreshBar.setVisible(false);

        membersTable = new MembersExpertTable(model);
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
     * Builds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(scrollPane, cc.xy(2, 6));

        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton inviteButton = new JButton(inviteAction);
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
            bar.addGridded(reconnectButton);
            bar.addRelatedGap();
            bar.addGridded(builder.getPanel());
        }
        JPanel buttonBarPanel = bar.getPanel();

        layout = new FormLayout(
            "pref, 0:grow, pref", "pref");
        builder = new PanelBuilder(layout);
        cc = new CellConstraints();
        builder.add(buttonBarPanel, cc.xy(1, 1));
        builder.add(Help.createWikiLinkButton(getController(),
            WikiLinks.SECURITY_PERMISSION), cc.xy(3, 1));

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
                reconnectAction.setEnabled(false);
            } else {
                reconnectAction.setEnabled(true);
            }
        } else {
            selectedMember = null;
            reconnectAction.setEnabled(false);
        }
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

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
            //fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
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
            super("exp.action_reconnect", MembersExpertTab.this.getController());
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