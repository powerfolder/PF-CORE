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
* $Id: NewFolderAction.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * Action which adds a member as a friend.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class AddFriendAction extends BaseAction {

    public AddFriendAction(Controller controller) {
        super("action_add_friend", controller);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source != null && source instanceof MemberInfo) {
            MemberInfo memberInfo = (MemberInfo) source;
            Member member = getController().getNodeManager().getNode(memberInfo);

            boolean askForFriendshipMessage = PreferencesEntry.
                    ASK_FOR_FRIENDSHIP_MESSAGE.getValueBoolean(getController());
            if (askForFriendshipMessage) {

                // Prompt for personal message.
                String[] options = {
                        Translation.getTranslation("general.ok"),
                        Translation.getTranslation("general.cancel")};

                FormLayout layout = new FormLayout("pref", "pref, 5dlu, pref, pref");
                PanelBuilder builder = new PanelBuilder(layout);
                CellConstraints cc = new CellConstraints();
                String nick = member.getNick();
                String text = Translation.getTranslation(
                        "friend.search.personal.message.text2", nick);
                builder.add(new JLabel(text), cc.xy(1, 1));
                JTextArea textArea = new JTextArea();
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 200));
                builder.add(scrollPane, cc.xy(1, 3));
                JPanel innerPanel = builder.getPanel();

                NeverAskAgainResponse response = DialogFactory.genericDialog(
                        getController().getUIController().
                        getMainFrame().getUIComponent(),
                        Translation.getTranslation("friend.search.personal.message.title"),
                        innerPanel, options, 0, GenericDialogType.INFO,
                        Translation.getTranslation("general.neverAskAgain"));
                if (response.getButtonIndex() == 0) { // == OK
                    String personalMessage = textArea.getText();
                    member.setFriend(true, personalMessage);
                }
                if (response.isNeverAskAgain()) {
                    // don't ask me again
                    PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(
                            getController(), false);
                }
            } else {
                // Send with no personal messages
                member.setFriend(true, null);
            }
        }
    }
}