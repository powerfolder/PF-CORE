/* $Id: InviteAction.java,v 1.22 2006/04/14 20:28:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Invites a user to a folder<BR>
 * TODO: Show the folder name<BR>
 * TODO: Show in selection box of folder icon in front of folder name
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class InviteAction extends SelectionBaseAction {

    private final String TO_EMAIL_TEXT = Translation
        .getTranslation("sendinvitation.email");
    private final String TO_DISK_TEXT = Translation
        .getTranslation("sendinvitation.todisk");
    private final String TO_CLIPBOARD_TEXT = Translation
        .getTranslation("sendinvitation.toclipboard");

    public InviteAction(Icon icon, Controller controller,
        SelectionModel selectionModel)
    {
        super("sendinvitation", controller, selectionModel);
        putValue(Action.SMALL_ICON, icon);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = getSelectionModel().getSelection();
        if (selection instanceof Member) {
            setEnabled(((Member) selection).isConnected());
        } else {
            setEnabled(true);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object target = getUIController().getControlQuarter().getSelectedItem();
        if (target instanceof Member) {
            Member member = (Member) target;
            inviteMember(member);
        } else if (target instanceof Folder) {
            Folder folder = (Folder) target;
            inviteToFolder(folder);
        }
    }

    /**
     * Invites a user to a folder
     * 
     * @param member
     */
    private void inviteMember(Member member) {
        FolderRepository repo = getController().getFolderRepository();

        FolderInfo[] folders = repo.getJoinedFolderInfos();
        List<FolderInfo> possibleInvitations = new ArrayList<FolderInfo>(
            folders.length);
        for (int i = 0; i < folders.length; i++) {
            if (!repo.getFolder(folders[i]).hasMember(member)) {
                // only show as invitation option, if not already in folder
                possibleInvitations.add(folders[i]);
            }
        }

        if (!possibleInvitations.isEmpty()) {
            Object result = JOptionPane.showInputDialog(getUIController()
                .getMainFrame().getUIComponent(), Translation.getTranslation(
                "sendinvitation.user.text", member.getNick()), Translation
                .getTranslation("sendinvitation.user.title", member.getNick()),
                JOptionPane.QUESTION_MESSAGE,
                (Icon) getValue(Action.SMALL_ICON), possibleInvitations
                    .toArray(), null);
            if (result != null) {
                FolderInfo folder = (FolderInfo) result;
                InvitationUtil.invitationToNode(getController(),
                    new Invitation(folder, getController().getMySelf()
                        .getInfo()), member);
                log()
                    .debug(
                        "Invited " + member.getNick() + " to folder "
                            + folder.name);
            }
        } else {
            JOptionPane.showMessageDialog(getUIController().getMainFrame()
                .getUIComponent(), "Unable to invite " + member.getNick()
                + " to any folder" + "\nUser already joined all your folders",
                member.getNick() + " already on all folders",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Invites a user to a folder
     * 
     * @param folder
     */
    private void inviteToFolder(Folder folder) {
        List<Member> conNodes = getController().getNodeManager()
            .getConnectedNodes();
        List possibleCanidates = new ArrayList(conNodes.size() + 1);
        if (OSUtil.isWindowsSystem()) {
            possibleCanidates.add(TO_EMAIL_TEXT);
        }
        possibleCanidates.add(TO_DISK_TEXT);
        possibleCanidates.add(TO_CLIPBOARD_TEXT);

        for (Member node : conNodes) {
            if (!folder.hasMember(node) && node.isConnected()) {
                // only show as invitation option, if not already in folder
                if (getController().isPublicNetworking()) {
                    // add all
                    possibleCanidates.add(new MemberWrapper(node));
                } else { // private, only add friends
                    if (node.isFriend()) {
                        possibleCanidates.add(new MemberWrapper(node));
                    }
                }
            }
        }

        Object result = JOptionPane.showInputDialog(getUIController()
            .getMainFrame().getUIComponent(), Translation
            .getTranslation("sendinvitation.folder.text"), Translation
            .getTranslation("sendinvitation.folder.title"),
            JOptionPane.QUESTION_MESSAGE, (Icon) getValue(Action.SMALL_ICON),
            possibleCanidates.toArray(), null);
        if (result != null) {
            Invitation invitation = folder.getInvitation();

            if (result instanceof MemberWrapper) {
                Member member = ((MemberWrapper) result).getMember();
                member.sendMessageAsynchron(invitation, null);
                log().debug(
                    "Invited " + member.getNick() + " to folder "
                        + folder.getName());
            } else if (result == TO_DISK_TEXT) {
                // To disk... option selected
                InvitationUtil.invitationToDisk(getController(), invitation, null);
            } else if (result == TO_CLIPBOARD_TEXT) {
                // Copy link to clipboard
                Util.setClipboardContents(invitation.toPowerFolderLink());
            } else if (result == TO_EMAIL_TEXT) {
                InvitationUtil.invitationToMail(getController(), invitation, null);
            }
        }
    }

    /**
     * Helper class for option pane
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.22 $
     */
    private class MemberWrapper {
        private Member member;

        private MemberWrapper(Member member) {
            this.member = member;
        }

        public Member getMember() {
            return member;
        }

        public String toString() {
            return member.getNick()
                + " ("
                + (member.isOnLAN() ? Translation
                    .getTranslation("general.localnet") : Translation
                    .getTranslation("general.inet")) + ")";
        }
    }
}