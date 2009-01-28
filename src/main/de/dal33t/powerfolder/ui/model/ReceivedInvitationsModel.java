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
 * $Id: ReceivedInvitationsModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to manage received invitations. Invitations can be added and removed.
 * Also a value model is available to count invites.
 */
public class ReceivedInvitationsModel extends PFComponent implements InvitationHandler {

    private final ValueModel receivedInvitationsCountVM = new ValueHolder();

    private List<Invitation> invitations =
            new CopyOnWriteArrayList<Invitation>();

    /**
     * Constructor
     *
     * @param controller
     */
    public ReceivedInvitationsModel(Controller controller) {
        super(controller);
        receivedInvitationsCountVM.setValue(0);
        getController().addInvitationHandler(this);
    }

    /**
     * Received invitation in the context of an InvitationHandler. Ask user if
     * the invitation should be added.
     *
     * @param invitation
     * @param sendIfJoined
     */
    public void gotInvitation(final Invitation invitation, boolean sendIfJoined) {

        final FolderRepository repository = getController().getFolderRepository();

        if (sendIfJoined) {

            if (!getController().isUIOpen()) {
                return;
            }

            Runnable worker = new Runnable() {
                public void run() {
                    if (repository.hasJoinedFolder(invitation.folder)) {
                        PFWizard.openSendInvitationWizard(getController(),
                                invitation.folder);
                    } else {
                        PFWizard.openInvitationReceivedWizard(getController(),
                                invitation);
                    }
                }
            };

            // Invoke later
            SwingUtilities.invokeLater(worker);

            return;
        }

        // Normal - add to invitation model.
        if (!repository.hasJoinedFolder(invitation.folder)) {
            invitations.add(invitation);
            receivedInvitationsCountVM.setValue(invitations.size());
        }
    }

    /**
     * Remove an invitation from the model for display, etc.
     *
     * @return
     */
    public Invitation popInvitation() {
        if (!invitations.isEmpty()) {
            Invitation invitation = invitations.remove(0);
            receivedInvitationsCountVM.setValue(invitations.size());
            return invitation;
        }
        return null;
    }

    /**
     * Value model with integer count of received invitations.
     *
     * @return
     */
    public ValueModel getReceivedInvitationsCountVM() {
        return receivedInvitationsCountVM;
    }
}
