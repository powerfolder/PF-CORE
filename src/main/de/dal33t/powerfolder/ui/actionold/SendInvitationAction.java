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
* $Id$
*/
package de.dal33t.powerfolder.ui.actionold;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
        super("send_invitation", controller, selectionModel);
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
            DialogFactory.genericDialog(
                    getUIController().getMainFrame().getUIComponent(),
                    Translation.getTranslation("send_invitation.allFolders.title",
                            member.getNick()),
                    Translation.getTranslation("send_invitation.allFolders.text",
                            member.getNick()),
                    GenericDialogType.WARN);
        } else {
            PFWizard.openSelectInvitationWizard(getController(), member,
                    possibleInvitations);
        }
    }
}