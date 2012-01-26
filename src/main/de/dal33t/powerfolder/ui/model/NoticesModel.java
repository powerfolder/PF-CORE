/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: NoticesModel.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.notices.*;
import de.dal33t.powerfolder.ui.notification.SystemNotificationHandler;
import de.dal33t.powerfolder.ui.wizard.*;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;

/**
 * Model of the notices awaiting action by the user.
 */
public class NoticesModel extends PFUIComponent {

    private final ValueModel allNoticesCountVM = new ValueHolder();
    private final ValueModel unreadNoticesCountVM = new ValueHolder();

    private List<Notice> notices = new CopyOnWriteArrayList<Notice>();

    /**
     * Constructor
     * 
     * @param controller
     */
    public NoticesModel(Controller controller) {
        super(controller);
        updateNoticeCounts();
    }

    /**
     * @return Value model with integer count of received invitations.
     */
    public ValueModel getAllNoticesCountVM() {
        return allNoticesCountVM;
    }

    /**
     * @return Value model with integer count of unread invitations.
     */
    public ValueModel getUnreadNoticesCountVM() {
        return unreadNoticesCountVM;
    }

    private void addNotice(Notice notice) {
        if (notices.contains(notice)) {
            logFine("Ignoring existing notice: " + notice);
        }
        notices.add(notice);
        updateNoticeCounts();
    }

    /**
     * @return a reference list of the notices in the model.
     */
    public List<Notice> getAllNotices() {
        return Collections.unmodifiableList(notices);
    }

    public Notice getFirstUnread() {
        for (Notice notice : notices) {
            if (!notice.isRead()) {
                return notice;
            }
        }
        return null;
    }
    /**
     * This handles a notice object. If it is a notification, show in a
     * notification handler. If it is actionable, add to the app model notices.
     * 
     * @param notice
     *            the Notice to handle
     */
    public void handleNotice(Notice notice) {
        handleSystemNotice(notice, false);
    }

    /**
     * This handles a system notice object. If it is a notification, show in a
     * notification handler. If it is actionable, add to the app model notices.
     * 
     * @param notice
     *            the Notice to handle
     */
    public void handleSystemNotice(Notice notice, boolean suppressPopup) {
        if (!getUIController().isStarted() || getController().isShuttingDown()
            || notices.contains(notice))
        {
            return;
        }

        // Show notice?
        if ((Boolean) getApplicationModel().getSystemNotificationsValueModel()
            .getValue()
            && notice.isNotification()
            && !notice.isRead()
            && !PFWizard.isWizardOpen() && !suppressPopup)
        {
            SystemNotificationHandler noticeHandler = new SystemNotificationHandler(getController(),
                notice);
            noticeHandler.show();
        }

        if (notice.isActionable()) {
            // Invitations are a special case. We do not care about
            // invitations to folders that we have already joined.
            if (notice instanceof InvitationNotice) {
                InvitationNotice in = (InvitationNotice) notice;
                Invitation i = in.getPayload(getController());
                FolderInfo fi = i.folder;
                if (getController().getFolderRepository().hasJoinedFolder(fi)) {
                    return;
                }
            }
            addNotice(notice);
        }
    }

    /**
     * Handle a notice.
     * 
     * @param notice
     */
    public void activateNotice(Notice notice) {
        // @todo Refactor this, moving activation to within notices.
        // There is no need for 'instanceof' handling.
        if (notice instanceof InvitationNotice) {
            InvitationNotice invitationNotice = (InvitationNotice) notice;
            handleInvitationNotice(invitationNotice);
        } else if (notice instanceof AskForFriendshipEventNotice) {
            AskForFriendshipEventNotice eventNotice = (AskForFriendshipEventNotice) notice;
            handleAskForFriendshipEventNotice(eventNotice);
        } else if (notice instanceof WarningNotice) {
            WarningNotice eventNotice = (WarningNotice) notice;
            SwingUtilities.invokeLater(eventNotice.getPayload(getController()));
        } else if (notice instanceof RunnableNotice) {
            RunnableNotice eventNotice = (RunnableNotice) notice;
            SwingUtilities.invokeLater(eventNotice.getPayload(getController()));
        } else if (notice instanceof LocalDeleteNotice) {
            LocalDeleteNotice eventNotice = (LocalDeleteNotice) notice;
            SwingUtilities.invokeLater(eventNotice.getPayload(getController()));
        } else if (notice instanceof FolderAutoCreateNotice) {
            FolderAutoCreateNotice eventNotice = (FolderAutoCreateNotice) notice;
            handleFolderAutoCreateNotice(eventNotice);
        } else if (notice instanceof OutOfMemoryNotice) {
            OutOfMemoryNotice eventNotice = (OutOfMemoryNotice) notice;
            handleOutOfMemoryNotice(eventNotice);
        } else {
            logWarning("Don't know what to do with notice: "
                + notice.getClass().getName() + " : " + notice.toString());
        }
        markRead(notice);
    }

    private void handleOutOfMemoryNotice(OutOfMemoryNotice notice) {
        // http\://www.powerfolder.com/wiki/Memory_configuration
        String memoryConfigHelp = Help.getWikiArticleURL(getController(),
            WikiLinks.MEMORY_CONFIGURATION);
        String infoText = Translation.getTranslation(
            "low_memory.error.text", memoryConfigHelp);
        int response = DialogFactory.genericDialog(
            getController(),
            Translation.getTranslation("low_memory.error.title"),
            infoText,
            new String[]{
                Translation.getTranslation("general.ok"),
                Translation
                    .getTranslation("dialog.already_running.exit_button")},
            0, GenericDialogType.ERROR);
        if (response == 1) { // Exit
            getController().exit(0);
        }
    }

    private void handleFolderAutoCreateNotice(FolderAutoCreateNotice
            eventNotice) {
        PFWizard.openFolderAutoCreateWizard(getController(),
                eventNotice.getFolderInfo());
    }

    /**
     * Marks the notice as read.
     * 
     * @param notice
     */
    public void markRead(Notice notice) {
        notice.setRead();
        updateNoticeCounts();
    }

    /**
     * Handle a request for friendship.
     * 
     * @param eventNotice
     */
    private void handleAskForFriendshipEventNotice(
        AskForFriendshipEventNotice eventNotice)
    {
        AskForFriendshipEvent event = eventNotice.getPayload(getController());
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
        final Set<FolderInfo> joinedFolders, final String message)
    {
        Runnable friendAsker = new Runnable() {
            public void run() {

                StringBuilder folderString = new StringBuilder();
                for (FolderInfo folderInfo : joinedFolders) {
                    if (!folderInfo.isMetaFolder()) {
                        folderString.append(folderInfo.name + '\n');
                    }
                }
                String[] options = {
                    Translation
                        .getTranslation("dialog.ask_for_friendship.button.add"),
                    Translation.getTranslation("general.cancel")};
                String text = Translation.getTranslation(
                    "dialog.ask_for_friendship.question", member.getNick(),
                    folderString.toString())
                    + "\n\n"
                    + Translation
                        .getTranslation("dialog.ask_for_friendship.explain");
                // if mainframe is hidden we should wait till its opened

                FormLayout layout = new FormLayout("pref",
                    "pref, 3dlu, pref, pref");
                PanelBuilder builder = new PanelBuilder(layout);
                CellConstraints cc = new CellConstraints();
                PanelBuilder panelBuilder = LinkedTextBuilder.build(
                    getController(), text);
                JPanel panel1 = panelBuilder.getPanel();
                builder.add(panel1, cc.xy(1, 1));
                if (!StringUtils.isEmpty(message)) {
                    builder.add(
                        new JLabel(Translation.getTranslation(
                            "dialog.ask_for_friendship.message_title",
                            member.getNick())), cc.xy(1, 3));
                    JTextArea textArea = new JTextArea(message);
                    textArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 4));
                }
                JPanel panel = builder.getPanel();

                NeverAskAgainResponse response = DialogFactory.genericDialog(
                    getController(), Translation.getTranslation(
                        "dialog.ask_for_friendship.title", member.getNick()),
                    panel, options, 0, GenericDialogType.QUESTION, Translation
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
    private void simpleAskForFriendship(final Member node, final String message)
    {

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
                        "dialog.ask_for_friendship.question2", node.getNick())
                        + "\n\n"
                        + Translation
                            .getTranslation("dialog.ask_for_friendship.explain");
                    // if mainframe is hidden we should wait till its opened

                    FormLayout layout = new FormLayout("pref",
                        "pref, 3dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    PanelBuilder panelBuilder = LinkedTextBuilder.build(
                        getController(), text);
                    JPanel panel1 = panelBuilder.getPanel();
                    builder.add(panel1, cc.xy(1, 1));
                    if (!StringUtils.isEmpty(message)) {
                        builder.add(
                            new JLabel(Translation.getTranslation(
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
                        .genericDialog(getController(), Translation
                            .getTranslation("dialog.ask_for_friendship.title",
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
        Invitation invitation = invitationNotice.getPayload(getController());
        PFWizard.openInvitationReceivedWizard(getController(), invitation);
    }

    public void clearAll() {
        notices.clear();
        updateNoticeCounts();
    }

    public void clearNotice(Notice notice) {
        for (Notice n : notices) {
            if (notice.equals(n)) {
                notices.remove(notice);
                updateNoticeCounts();
                return;
            }
        }
    }

    private void updateNoticeCounts() {
        allNoticesCountVM.setValue(notices.size());

        int count = 0;
        for (Notice notice : notices) {
            if (!notice.isRead()) {
                count++;
            }
        }
        unreadNoticesCountVM.setValue(count);
    }
}
