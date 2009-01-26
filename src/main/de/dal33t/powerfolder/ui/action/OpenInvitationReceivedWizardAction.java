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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.model.ReceivedInvitationsModel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.TimerTask;

public class OpenInvitationReceivedWizardAction extends BaseAction {

    public OpenInvitationReceivedWizardAction(Controller controller) {
        super("action_open_invitation_received_wizard", controller);
    }

    public void actionPerformed(ActionEvent e) {

        if (!getController().isUIOpen()) {
            return;
        }

        ReceivedInvitationsModel invitationsModel = getController()
                .getUIController().getApplicationModel()
                .getReceivedInvitationsModel();

        if ((Integer) invitationsModel.getReceivedInvitationsCountVM().getValue()
                == 0) {
            return;
        }

        final Invitation invitation = invitationsModel.popInvitation();
        Runnable worker = new Runnable() {
            public void run() {
                TimerTask task = new TimerTask() {
                    public void run() {
                            PFWizard.openInvitationReceivedWizard(
                                    getController(), invitation);
                    }
                };
                task.run();
            }
        };

        // Invoke later
        SwingUtilities.invokeLater(worker);
    }
}
