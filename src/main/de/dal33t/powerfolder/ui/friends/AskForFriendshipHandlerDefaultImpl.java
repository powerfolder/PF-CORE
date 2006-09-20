package de.dal33t.powerfolder.ui.friends;

import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.AskForFriendshipHandler;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

/**
 * Asks the user, if this member should be added to friendlist if not
 * already done. Won't ask if user has disabled this in CONFIG_ASKFORFRIENDSHIP.
 * displays in the userinterface the list of folders that that member has joined.
 */
public class AskForFriendshipHandlerDefaultImpl extends PFUIComponent implements
    AskForFriendshipHandler
{
    public AskForFriendshipHandlerDefaultImpl(Controller controller) {
        super(controller);
    }
    public void askForFriendship(
        AskForFriendshipEvent askForFriendshipEvent)
    {
        final Member member = askForFriendshipEvent.getMember();
        final Set<FolderInfo> joinedFolders = askForFriendshipEvent.getJoinedFolders();
        boolean neverAsk = getController().getPreferences()
        		.getBoolean(Member.CONFIG_ASKFORFRIENDSHIP, false);
              
        if (getController().isUIOpen() && !member.isFriend() && !neverAsk
            && !member.askedForFriendship())
        {
            // Okay we are asking for friendship now
            member.setAskedForFriendship(true);

            Runnable friendAsker = new Runnable() {
                public void run() {
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(Icons.ST_NODE);

                    String folderString = "";
                    for (FolderInfo folderInfo : joinedFolders) {
                        String secrectOrPublicText;
                        if (folderInfo.secret) {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.secret");
                        } else {
                            secrectOrPublicText = Translation
                                .getTranslation("folderjoin.public");
                        }
                        folderString += folderInfo.name + " ("
                            + secrectOrPublicText + ")\n";
                    }
                    Object[] options = {
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.yes"),
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.no"),
                        Translation
                            .getTranslation("dialog.addmembertofriendlist.button.no_neveraskagain")};
                    String text = Translation.getTranslation(
                        "dialog.addmembertofriendlist.question", member.getNick(),
                        folderString)
                        + "\n\n"
                        + Translation
                            .getTranslation("dialog.addmembertofriendlist.explain");
                    // if mainframe is hidden we should wait till its opened
                    int result = JOptionPane
                        .showOptionDialog(getController().getUIController()
                            .getMainFrame().getUIComponent(), text,
                            Translation
                                .getTranslation(
                                    "dialog.addmembertofriendlist.title",
                                    member.getNick()), JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);
                    member.setFriend(result == 0);
                    if (result == 2) {
                        member.setFriend(false);
                        // dont ask me again
                        getController().getPreferences().putBoolean(Member.CONFIG_ASKFORFRIENDSHIP, false);
                    }
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(null);
                }
            };
            SwingUtilities.invokeLater(friendAsker);
        } else {
            member.setFriend(false);
        }

    }

}
