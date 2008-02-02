/* $Id: ChangeFriendStatusAction.java,v 1.14 2006/02/20 11:37:32 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.awt.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.ui.*;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Actions which switches the friend status of a member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class ChangeFriendStatusAction extends SelectionBaseAction {

    public ChangeFriendStatusAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("addfriend", controller, selectionModel);
        Object selection = selectionModel.getSelection();
        if (selection instanceof Member) {
            adaptFor((Member) selection);
        } else if (selection instanceof FileInfo) {
            adaptFor((FileInfo) selection);
        } else {
            setEnabled(false);
        }
    }

    public void selectionChanged(SelectionChangeEvent event) {
        Object selection = event.getSelection();

        if (selection instanceof Member) {
            adaptFor((Member) selection);
        } else if (selection instanceof FileInfo) {
            adaptFor((FileInfo) selection);
        } else {
            setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object selection = getSelectionModel().getSelection();

        log().verbose("Performing on item: " + selection);
        Member member = null;

        if (selection instanceof Member) {
            member = (Member) selection;
        } else if (selection instanceof FileInfo) {
            FileInfo file = (FileInfo) selection;
            member = file.getModifiedBy().getNode(getController(), true);
        }

        // Switch friend status
        if (member != null) {

            if (member.isFriend()) {
                member.setFriend(false, null);
                adaptFor(member);
            } else {
                boolean askForFriendshipMessage = PreferencesEntry.
                        ASK_FOR_FRIENDSHIP_MESSAGE
                    .getValueBoolean(getController());
                if (askForFriendshipMessage) {

                    // Prompt for personal message.
                    String[] options = {
                            Translation
                                    .getTranslation("general.ok"),
                            Translation
                                    .getTranslation("general.cancel")};

                    FormLayout layout = new FormLayout("pref", "pref, 5dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    builder.add(new JLabel(Translation.
                            getTranslation("change.friend.personal.message.text", member.getNick())), cc.xy(1, 1));
                    JTextArea textArea = new JTextArea();
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 3));
                    JPanel panel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory.genericDialog(getController().getUIController().
                            getMainFrame().getUIComponent(),
                            Translation.getTranslation("change.friend.personal.message.title"),
                            panel, options, 0, GenericDialogType.INFO,
                            Translation.getTranslation("general.neverAskAgain"));
                    if (response.getButtonIndex() == 0) { // == OK
                        String personalMessage = textArea.getText();
                        member.setFriend(true, personalMessage);
                        adaptFor(member);
                    }
                    if (response.isNeverAskAgain()) {
                        // dont ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE
                            .setValue(getController(), false);
                    }
                } else {
                    member.setFriend(true, null);
                    adaptFor(member);
                }
            }

        } else {
            log().warn("Unable to change friend status, member not found");
        }
    }

    private void configureButton(boolean isFriend) {
        if (isFriend) {
            configureFromActionId("removefriend");
        } else {
            configureFromActionId("addfriend");
        }
    }

    /**
     * Adapts the action text and icon for the member
     * 
     * @param member
     */
    private void adaptFor(Member member) {
        setEnabled(!member.isMySelf());
        configureButton(member.isFriend());
    }

    /**
     * Adapts action text and icon for a members file
     * 
     * @param file
     */
    private void adaptFor(FileInfo file) {
        MemberInfo member = file.getModifiedBy();
        // Exclude myself
        setEnabled(!member.matches(getController().getMySelf()));
        configureButton(member.isFriend(getController()));
    }
}