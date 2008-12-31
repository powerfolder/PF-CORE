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
* $Id: OpenInvitationReceivedWizardAction.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.model.ReceivedAskedForFriendshipModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Set;

public class AskForFriendshipAction extends BaseAction {

    public AskForFriendshipAction(Controller controller) {
        super("action_ask_for_friendship", controller);
    }

    public void actionPerformed(ActionEvent e) {

        if (!getController().isUIOpen()) {
            return;
        }

        ReceivedAskedForFriendshipModel model = getUIController()
                .getApplicationModel().getReceivedAskedForFriendshipModel();
        if ((Integer) model.getReceivedAskForFriendshipCountVM().getValue()
                <= 0) {
            return;
        }

        AskForFriendshipEvent event = model.popEvent();

        Member node = getController().getNodeManager().getNode(event.getMemberInfo());
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

    private void joinedAskForFriendship(final Member member,
                                        final Set<FolderInfo> joinedFolders,
                                        final String message) {
        Runnable friendAsker = new Runnable() {
            public void run() {
                getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.SYSTRAY_FRIEND_ICON);

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
                        "pref, 5dlu, pref, pref");
                PanelBuilder builder = new PanelBuilder(layout);
                CellConstraints cc = new CellConstraints();
                PanelBuilder panelBuilder = LinkedTextBuilder.build(text);
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
                        .genericDialog(getController().getUIController()
                                .getMainFrame().getUIComponent(), Translation
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
                getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(null);
            }
        };
        SwingUtilities.invokeLater(friendAsker);

    }

    private void simpleAskForFriendship(final Member node,
                                        final String message) {

        boolean askForFriendShip =
                PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
                        .getValueBoolean(getController());

        if (getController().isUIOpen()) {

            // Okay we are asking for friendship now
            node.setAskedForFriendship(true);
            Runnable friendAsker = new Runnable() {
                public void run() {
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.SYSTRAY_FRIEND_ICON);

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
                            "pref, 5dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    PanelBuilder panelBuilder = LinkedTextBuilder.build(text);
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
                            .genericDialog(getController().getUIController()
                                    .getMainFrame().getUIComponent(),
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
                    getController().getUIController().getBlinkManager()
                            .setBlinkingTrayIcon(null);
                }
            };
            SwingUtilities.invokeLater(friendAsker);
        }
    }
}