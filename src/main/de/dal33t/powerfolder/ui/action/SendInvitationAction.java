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
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.DialogFactory;

/**
 * Invites a user to a folder<BR>
 * TODO: Show the folder name<BR>
 * TODO: Show in selection box of folder icon in front of folder name
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class SendInvitationAction extends SelectionBaseAction {

    public SendInvitationAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("sendinvitation", controller, selectionModel);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = getSelectionModel().getSelection();
        /*
         * if (selection instanceof Member) { setEnabled(((Member)
         * selection).isConnected()); } else { setEnabled(true); }
         */
    }

    public void actionPerformed(ActionEvent e) {
        Object target = getUIController().getControlQuarter().getSelectedItem();
        if (target instanceof Member) {
            Member member = (Member) target;
            inviteMember(member);
        } else if (target instanceof Folder) {
            Folder folder = (Folder) target;
            PFWizard
                .openSendInvitationWizard(getController(), folder.getInfo());
        }
    }

    /**
     * Invites a user to a folder
     * 
     * @param member
     */
    private void inviteMember(Member member) {
        FolderRepository repo = getController().getFolderRepository();

        Folder[] folders = repo.getFolders();
        List<FolderInfo> possibleInvitations = new ArrayList<FolderInfo>(
            folders.length);
        for (Folder folder1 : folders) {
            if (!folder1.hasMember(member)) {
                // only show as invitation option, if not already in folder
                possibleInvitations.add(folder1.getInfo());
            }
        }

        if (possibleInvitations.isEmpty()) {
            // @todo add translation
            DialogFactory.showMessageDialog(
                    getUIController().getMainFrame().getUIComponent(),
                    member.getNick() +
                            " already on all folders",
                    "Unable to invite " +
                            member.getNick() +
                            " to any folder" +
                            "\nUser already joined all your folders",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            Object result = JOptionPane.showInputDialog(getUIController()
                    .getMainFrame().getUIComponent(), Translation.getTranslation(
                    "sendinvitation.user.text", member.getNick()), Translation
                    .getTranslation("sendinvitation.user.title", member.getNick()),
                    JOptionPane.QUESTION_MESSAGE,
                    (Icon) getValue(Action.SMALL_ICON), possibleInvitations
                    .toArray(), null);
            if (result != null) {
                FolderInfo folder = (FolderInfo) result;
                Invitation invitation = folder.getFolder(getController())
                        .createInvitation();
                invitation.suggestedLocalBase = folder.getFolder(
                        getController()).getLocalBase();
                InvitationUtil.invitationToNode(getController(), invitation,
                        member);
                log().debug("Invited " + member.getNick() +
                        " to folder " + folder.name);
            }
        }
    }
}