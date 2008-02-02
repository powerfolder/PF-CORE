package de.dal33t.powerfolder.ui.friends;

import java.util.Set;
import java.awt.*;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.AskForFriendshipHandler;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import org.apache.commons.lang.StringUtils;

/**
 * Asks the user, if this member should be added to friendlist if not already
 * done. Won't ask if user has disabled this in CONFIG_ASKFORFRIENDSHIP.
 * displays in the userinterface the list of folders that that member has
 * joined if available or a simpler dialog if not.
 */
public class AskForFriendshipHandlerDefaultImpl extends PFUIComponent implements
    AskForFriendshipHandler
{
    public AskForFriendshipHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void askForFriendship(AskForFriendshipEvent askForFriendshipHandlerEvent) {
        final Member member = askForFriendshipHandlerEvent.getMember();
        final String personalMessage = askForFriendshipHandlerEvent.getPersonalMessage();
        final Set<FolderInfo> joinedFolders = askForFriendshipHandlerEvent
            .getJoinedFolders();

        if (joinedFolders == null) {
        	simpleAskForFriendship(askForFriendshipHandlerEvent);
        	return;
        }
        
        boolean joinedPrivateFolder = false;
        for (FolderInfo foInfo : joinedFolders) {
            if (foInfo.secret) {
                joinedPrivateFolder = true;
                break;
            }
        }
        boolean askForFriendShip = PreferencesEntry.
                ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
            .getValueBoolean(getController());

        if (getController().isUIOpen() && !member.isFriend()
            && askForFriendShip && !member.askedForFriendship()
            && joinedPrivateFolder)
        {
            // Okay we are asking for friendship now
            member.setAskedForFriendship(true);

            Runnable friendAsker = new Runnable() {
                public void run() {
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.ST_NODE);

                    StringBuilder folderString = new StringBuilder();
                    for (FolderInfo folderInfo : joinedFolders) {
                        String secrectOrPublicText;
                        if (folderInfo.secret) {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.secret");
                        } else {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.public");
                        }
                        folderString.append(folderInfo.name + " ("
                            + secrectOrPublicText + ")\n");
                    }
                    String[] options = {
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.add"),
                        Translation
                            .getTranslation("general.cancel")};
                    String text = Translation.getTranslation(
                        "dialog.addmembertofriendlist.question", member
                            .getNick(), folderString.toString())
                        + "\n\n"
                        + Translation
                            .getTranslation("dialog.addmembertofriendlist.explain");
                    // if mainframe is hidden we should wait till its opened
                    
                    FormLayout layout = new FormLayout("pref", "pref, 5dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    PanelBuilder panelBuilder = LinkedTextBuilder.build(text);
                    JPanel panel1 = panelBuilder.getPanel();
                    builder.add(panel1, cc.xy(1, 1));
                    if (!StringUtils.isEmpty(personalMessage)) {
                        builder.add(new JLabel(Translation.getTranslation(
                        "dialog.addmembertofriendlist.messageTitle",
                                member.getNick())), cc.xy(1, 3));
                        JTextArea textArea = new JTextArea(personalMessage);
                        textArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 200));
                        builder.add(scrollPane, cc.xy(1, 4));
                    }
                    JPanel panel = builder.getPanel();

                    NeverAskAgainResponse response =
                            DialogFactory.genericDialog(
                                    getController().getUIController().
                                            getMainFrame().getUIComponent(),
                            Translation.getTranslation(
                                    "dialog.addmembertofriendlist.title",
                                    member.getNick()),
                            panel, options, 0, GenericDialogType.QUESTION,
                            Translation.getTranslation("general.neverAskAgain"));
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
        } else {
            member.setFriend(false, null);
        }

    }

	private void simpleAskForFriendship(AskForFriendshipEvent askForFriendshipEvent) {
        final Member member = askForFriendshipEvent.getMember();
        final String personalMessage = askForFriendshipEvent.getPersonalMessage();
        boolean askForFriendShip = PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
        .getValueBoolean(getController());

        boolean hasMessage = StringUtils.isEmpty(askForFriendshipEvent
                .getPersonalMessage());
        if (getController().isUIOpen() && !member.isFriend() && askForFriendShip
                && (!member.askedForFriendship() || !hasMessage)) {

            // Okay we are asking for friendship now
            member.setAskedForFriendship(true);
            Runnable friendAsker = new Runnable() {
                public void run() {
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.ST_NODE);

                    String[] options = {
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.add"),
                        Translation
                            .getTranslation("general.cancel")};
                    String text = Translation.getTranslation(
                        "dialog.addmembertofriendlist.question2", member
                            .getNick())
                        + "\n\n"
                        + Translation
                            .getTranslation("dialog.addmembertofriendlist.explain");
                    // if mainframe is hidden we should wait till its opened

                    FormLayout layout = new FormLayout("pref", "pref, 5dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    PanelBuilder panelBuilder = LinkedTextBuilder.build(text);
                    JPanel panel1 = panelBuilder.getPanel();
                    builder.add(panel1, cc.xy(1, 1));
                    if (!StringUtils.isEmpty(personalMessage)) {
                        builder.add(new JLabel(Translation.getTranslation(
                        "dialog.addmembertofriendlist.messageTitle",
                                member.getNick())), cc.xy(1, 3));
                        JTextArea textArea = new JTextArea(personalMessage);
                        textArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 200));
                        builder.add(scrollPane, cc.xy(1, 4));
                    }
                    JPanel panel = builder.getPanel();

                    NeverAskAgainResponse response =
                            DialogFactory.genericDialog(
                                    getController().getUIController().
                                            getMainFrame().getUIComponent(),
                            Translation.getTranslation(
                                    "dialog.addmembertofriendlist.title",
                                    member.getNick()),
                            panel, options, 0, GenericDialogType.QUESTION,
                            Translation.getTranslation("general.neverAskAgain"));
                    member.setFriend(response.getButtonIndex() == 0, null);
                    if (response.isNeverAskAgain()) {
                        member.setFriend(false, null);
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
