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
 * $Id: NoticesCard.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.notices;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.notices.AskForFriendshipEventNotice;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;
import java.util.Set;

public class NoticesCard extends InformationCard {

    private JPanel uiComponent;
    private NoticesTableModel noticesTableModel;
    private NoticesTable noticesTable;
    private Action activateAction;

    public NoticesCard(Controller controller) {
        super(controller);
    }

    public Image getCardImage() {
        return Icons.getImageById(Icons.WARNING);
    }

    public String getCardTitle() {
        return Translation.getTranslation("dialog.notices.title");
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        PropertyChangeListener noticesListener = new MyPropertyChangeListener();
        getController().getUIController().getApplicationModel()
                .getNoticesModel().getReceivedNoticesCountVM()
                .addValueChangeListener(noticesListener);
        noticesTableModel = new NoticesTableModel(getController());
        noticesTable = new NoticesTable(noticesTableModel);
        noticesTable.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        enableActivate();
                    }
                });

        noticesTable.addMouseListener(new TableMouseListener());
        activateAction = new ActivateNoticeAction(getController());
        enableActivate();
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu");
        // tools sep table dets sep stats
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JScrollPane pane = new JScrollPane(noticesTable);
        builder.add(buildToolbar().getPanel(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(pane, cc.xy(2, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * Build the toolbar component.
     */
    private ButtonBarBuilder buildToolbar() {
        JButton activateButton = new JButton(activateAction);
        JButton clearAllButton = new JButton(new CleanupNoticesAction(getController()));

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(activateButton);
        bar.addRelatedGap();
        bar.addGridded(clearAllButton);
        return bar;
    }

    private void updateTableModel() {
        noticesTableModel.reset();
    }

    private void enableActivate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                activateAction.setEnabled(noticesTable.getSelectedRowCount() >
                        0);
            }
        });
    }

    /**
     * Handle a notice.
     *
     * @param notice
     */
    private void handleNotice(Notice notice) {

        if (notice instanceof InvitationNotice) {
            InvitationNotice invitationNotice = (InvitationNotice) notice;
            handleInvitationNotice(invitationNotice);
        } else if (notice instanceof AskForFriendshipEventNotice) {
            AskForFriendshipEventNotice eventNotice = (AskForFriendshipEventNotice) notice;
            handleAskForFriendshipEventNotice(eventNotice);
        } else if (notice instanceof WarningNotice) {
            WarningNotice eventNotice = (WarningNotice) notice;
            handleWarningEventNotice(eventNotice);
        } else {
            logWarning("Don't know what to do with notice: " +
                    notice.getClass().getName() + " : " + notice.toString());
        }
    }

    /**
     * Handle a warning event notice by running its runnable.
     *
     * @param eventNotice
     */
    private static void handleWarningEventNotice(WarningNotice eventNotice) {
        SwingUtilities.invokeLater(eventNotice.getPayload());
    }

    /**
     * Handle a request for friendship.
     *
     * @param eventNotice
     */
    private void handleAskForFriendshipEventNotice(
            AskForFriendshipEventNotice eventNotice) {
        AskForFriendshipEvent event = eventNotice.getPayload();
        Member node = getController().getNodeManager().getNode(
                event.getMemberInfo());
        if (node == null) {
            // Ignore friendship request from unknown node.
            return;
        }

        Set<FolderInfo> joinedFolders = event.getJoinedFolders();
        String message = event.getMessage();

        if (joinedFolders == null) {
            simpleAskForFriendship(node, message);
        } else {
            joinedAskForFriendship(node, joinedFolders, message);
        }
    }

    /**
     * Handle a freindship request with folders to join.
     *
     * @param member
     * @param joinedFolders
     * @param message
     */
    private void joinedAskForFriendship(final Member member,
                                        final Set<FolderInfo> joinedFolders,
                                        final String message) {
        Runnable friendAsker = new Runnable() {
            public void run() {

                StringBuilder folderString = new StringBuilder();
                for (FolderInfo folderInfo : joinedFolders) {
                    folderString.append(folderInfo.name + '\n');
                }
                String[] options = {
                        Translation.getTranslation(
                                "dialog.ask_for_friendship.button.add"),
                        Translation.getTranslation("general.cancel")};
                String text = Translation.getTranslation(
                        "dialog.ask_for_friendship.question",
                        member.getNick(),
                        folderString.toString()) + "\n\n" +
                        Translation.getTranslation(
                                "dialog.ask_for_friendship.explain");
                // if mainframe is hidden we should wait till its opened

                FormLayout layout = new FormLayout("pref",
                        "pref, 3dlu, pref, pref");
                PanelBuilder builder = new PanelBuilder(layout);
                CellConstraints cc = new CellConstraints();
                PanelBuilder panelBuilder = LinkedTextBuilder.build(getController(),
                        text);
                JPanel panel1 = panelBuilder.getPanel();
                builder.add(panel1, cc.xy(1, 1));
                if (!StringUtils.isEmpty(message)) {
                    builder.add(new JLabel(Translation.getTranslation(
                            "dialog.ask_for_friendship.message_title", member
                            .getNick())), cc.xy(1, 3));
                    JTextArea textArea = new JTextArea(message);
                    textArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 4));
                }
                JPanel panel = builder.getPanel();

                NeverAskAgainResponse response = DialogFactory
                        .genericDialog(getController(), Translation
                                .getTranslation(
                                "dialog.ask_for_friendship.title", member
                                .getNick()), panel, options, 0,
                                GenericDialogType.QUESTION, Translation
                                .getTranslation("general.neverAskAgain"));
                member.setFriend(response.getButtonIndex() == 0, null);
                if (response.isNeverAskAgain()) {
                    // dont ask me again
                    PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
                            .setValue(getController(), false);
                }
            }
        };
        SwingUtilities.invokeLater(friendAsker);

    }

    /**
     * Handle a simple freindship request.
     *
     * @param node
     * @param message
     */
    private void simpleAskForFriendship(final Member node,
                                        final String message) {

        if (getController().isUIOpen()) {

            // Okay we are asking for friendship now
            node.setAskedForFriendship(true);
            Runnable friendAsker = new Runnable() {
                public void run() {

                    String[] options = {
                        Translation
                            .getTranslation("dialog.ask_for_friendship.button.add"),
                        Translation.getTranslation("general.cancel")};
                    String text = Translation.getTranslation(
                        "dialog.ask_for_friendship.question2",
                            node.getNick()) + "\n\n"
                            + Translation.getTranslation(
                            "dialog.ask_for_friendship.explain");
                    // if mainframe is hidden we should wait till its opened

                    FormLayout layout = new FormLayout("pref",
                            "pref, 3dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    PanelBuilder panelBuilder = LinkedTextBuilder.build(getController(), text);
                    JPanel panel1 = panelBuilder.getPanel();
                    builder.add(panel1, cc.xy(1, 1));
                    if (!StringUtils.isEmpty(message)) {
                        builder.add(new JLabel(Translation.getTranslation(
                                "dialog.ask_for_friendship.message_title",
                                node.getNick())), cc.xy(1, 3));
                        JTextArea textArea = new JTextArea(message);
                        textArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 200));
                        builder.add(scrollPane, cc.xy(1, 4));
                    }
                    JPanel panel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory
                            .genericDialog(getController(),
                                    Translation.getTranslation(
                                    "dialog.ask_for_friendship.title",
                                            node.getNick()), panel, options, 0,
                                    GenericDialogType.QUESTION, Translation
                                    .getTranslation("general.neverAskAgain"));
                    node.setFriend(response.getButtonIndex() == 0, null);
                    if (response.isNeverAskAgain()) {
                        node.setFriend(false, null);
                        // dont ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
                                .setValue(getController(), false);
                    }
                }
            };
            SwingUtilities.invokeLater(friendAsker);
        }
    }

    /**
     * Handle an invitation notice.
     *
     * @param invitationNotice
     */
    private void handleInvitationNotice(InvitationNotice invitationNotice) {
        final Invitation invitation = invitationNotice.getPayload();
        Runnable worker = new Runnable() {
            public void run() {
                TimerTask task = new TimerTask() {
                    public void run() {
                        PFWizard.openInvitationReceivedWizard(
                                getController(), invitation);
                    }
                };
                task.run();
            }
        };

        // Invoke later
        SwingUtilities.invokeLater(worker);
    }

    /**
     * When a user double-clicks a row
     */
    private void activateNotice() {
        int row = noticesTable.getSelectedRow();
        if (row >= 0) {
            Object at = noticesTableModel.getValueAt(row, 0);
            if (at != null && at instanceof Notice) {
                Notice notice = (Notice) at;
                handleNotice(notice);
            }
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class CleanupNoticesAction extends BaseAction {

        CleanupNoticesAction(Controller controller) {
            super("action_cleanup_notices", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int row = noticesTable.getSelectedRow();
            if (row < 0) {
                getController().getUIController().getApplicationModel()
                        .getNoticesModel().clearAll();
            } else {
                Object at = noticesTableModel.getValueAt(row, 0);
                if (at != null && at instanceof Notice) {
                    Notice notice = (Notice) at;
                    getController().getUIController().getApplicationModel()
                            .getNoticesModel().clearNotice(notice);
                }
            }
        }
    }

    private class ActivateNoticeAction extends BaseAction {

        ActivateNoticeAction(Controller controller) {
            super("action_activate_notice", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int row = noticesTable.getSelectedRow();
            if (row >= 0) {
                Object at = noticesTableModel.getValueAt(row, 0);
                if (at != null && at instanceof Notice) {
                    Notice notice = (Notice) at;
                    handleNotice(notice);
                }
            }
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            updateTableModel();
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

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    activateNotice();
                } else if (e.getClickCount() == 1) {
                    if (noticesTableModel.getRowCount() == 1) {
                        activateNotice();
                    }
                }
            }
        }

        private void showContextMenu(MouseEvent evt) {
            // @todo ???
        }
    }

}
