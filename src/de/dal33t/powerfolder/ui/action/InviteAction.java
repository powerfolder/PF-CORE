/* $Id: InviteAction.java,v 1.22 2006/04/14 20:28:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.DialogFactory;
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
        List possibleInvitations = new ArrayList(folders.length);
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
                member.sendMessageAsynchron(new Invitation(folder,
                    getController().getMySelf().getInfo()), null);
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
        Member[] nodes = getController().getNodeManager().getNodes();
        List possibleCanidates = new ArrayList(nodes.length + 1);
        if (Util.isWindowsSystem()) {
            possibleCanidates.add(TO_EMAIL_TEXT);
        }
        possibleCanidates.add(TO_DISK_TEXT);
        possibleCanidates.add(TO_CLIPBOARD_TEXT);

        for (int i = 0; i < nodes.length; i++) {
            if (!folder.hasMember(nodes[i]) && nodes[i].isConnected()) {
                // only show as invitation option, if not already in folder
                if (getController().isPublicNetworking()) {
                    // add all
                    possibleCanidates.add(new MemberWrapper(nodes[i]));
                } else { // private, only add friends
                    if (nodes[i].isFriend()) {
                        possibleCanidates.add(new MemberWrapper(nodes[i]));
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
            Invitation invitation = new Invitation(folder.getInfo(),
                getController().getMySelf().getInfo());

            if (result instanceof MemberWrapper) {
                Member member = ((MemberWrapper) result).getMember();
                member.sendMessageAsynchron(invitation, null);
                log().debug(
                    "Invited " + member.getNick() + " to folder "
                        + folder.getName());
            } else if (result == TO_DISK_TEXT) {
                // To disk... option selected
                invitationToDisk(invitation);
            } else if (result == TO_CLIPBOARD_TEXT) {
                // Copy link to clipboard
                Util.setClipboardContents(invitation.toPowerFolderLink());
            } else if (result == TO_EMAIL_TEXT) {
                invitationToMail(invitation);
            }
        }
    }

    /**
     * Handles the invitation to mail option
     */
    private void invitationToMail(Invitation invitation) {
        JFrame parent = getController().getUIController().getMainFrame()
            .getUIComponent();

        String to = (String) JOptionPane.showInputDialog(parent, Translation
            .getTranslation("sendinvitation.ask_emailaddres.message"),
            Translation.getTranslation("sendinvitation.ask_emailaddres.title"),
            JOptionPane.QUESTION_MESSAGE, null, null, Translation
                .getTranslation("sendinvitation.example_emailaddress"));
        if (to != null) { // null if canceled
            try {
                String tmpDir = System.getProperty("java.io.tmpdir");
                File file;
                if (tmpDir != null && tmpDir.length() > 0) {
                    // create in tmp dir if available
                    file = new File(tmpDir, invitation.folder.name
                        + ".invitation");
                } else {
                    // else create in working directory
                    file = new File(invitation.folder.name + ".invitation");
                }
                ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)));
                out.writeObject(invitation);
                out.writeObject(getController().getMySelf().getInfo());
                out.close();
                file.deleteOnExit();
                String invitationName = file.getName();
                String subject = Translation.getTranslation(
                    "sendinvitation.subject", invitation.folder.name);
                String body = Translation.getTranslation("sendinvitation.body",
                    to, getController().getMySelf().getNick(),
                    invitation.folder.name);
                if (!Util.sendMail(to, subject,  body , file)) {
                    log().error("sendmail failed");
                }
            } catch (IOException e) {
                log().error("sendmail failed", e);
            }
        }
    }

    /**
     * Handles the invitation to disk option
     */
    private void invitationToDisk(Invitation invitation) {
        // Select file
        JFileChooser fc = DialogFactory.createFileChooser();
        fc.setDialogTitle(Translation
            .getTranslation("sendinvitation.placetostore"));
        // Recommended file
        fc.setSelectedFile(new File(invitation.folder.name + ".invitation"));
        fc.setFileFilter(Util.createInvitationsFilefilter());
        int result = fc.showSaveDialog(getController().getUIController()
            .getMainFrame().getUIComponent());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // Store invitation to disk
        File file = fc.getSelectedFile();
        if (file == null) {
            return;
        }
        if (file.exists()) {
            // TODO: Add confirm dialog
        }
        log().warn("Writing invitation to " + file);
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(file));
            out.writeObject(invitation);
            out.writeObject(getController().getMySelf().getInfo());
            out.close();
        } catch (IOException e) {
            getController().getUIController().showErrorMessage(
                "Unable to write invitation",
                "Error while writing invitation, please try again.", e);
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