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
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.ReceivedInvitationPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import javax.swing.*;
import java.awt.*;
import java.util.TimerTask;

/**
 * The default handler when an invitation is received by the
 * FolderRepositoryThis handler shows the user a dialog where he can should to
 * join the folder.
 */
public class InvitationReceivedHandlerDefaultImpl extends PFComponent implements
        InvitationReceivedHandler {
    public InvitationReceivedHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    /**
     * Called by the FolderRepository when an invitation is received. Shows a
     * dialog to give the user the option to join the folder. Depending on the
     * flag isProcessSilently and error message is shown if folder is already
     * joined.
     */
    public void invitationReceived(
            InvitationReceivedEvent invitationRecievedEvent) {
        final Invitation invitation = invitationRecievedEvent.getInvitation();
        final boolean processSilently = invitationRecievedEvent
                .isProcessSilently();
        final FolderRepository repository = invitationRecievedEvent
                .getFolderRepository();
        if (invitation == null || invitation.folder == null) {
            throw new NullPointerException("Invitation/Folder is null");
        }
        if (!getController().isUIOpen()) {
            return;
        }

        Runnable worker = new Runnable() {
            public void run() {
                // Check if already on folder
                if (repository.hasJoinedFolder(invitation.folder)) {
                    // Already on folder, show message if not processing
                    // silently
                    if (!processSilently) {
                        // Popup application
                        getController().getUIController().getMainFrame()
                                .getUIComponent().setVisible(true);
                        getController().getUIController().getMainFrame()
                                .getUIComponent().setExtendedState(Frame.NORMAL);

                        DialogFactory.genericDialog(
                                getController().getUIController().getMainFrame().getUIComponent(),
                                Translation.getTranslation("joinfolder.already_joined_title", invitation.folder.name),
                                Translation.getTranslation("joinfolder.already_joined_text", invitation.folder.name),
                                GenericDialogType.WARN);
                    }
                    return;
                }
                final ReceivedInvitationPanel panel = new ReceivedInvitationPanel(
                        getController(), invitation);
                TimerTask task = new TimerTask() {
                    public void run() {
                        PFWizard wizard = new PFWizard(getController());
                        wizard.open(panel);
                    }
                };
                getController().getUIController().notifyMessage(
                        Translation.getTranslation("invite_received_handler.notify.title")
                        , Translation.getTranslation("invite_received_handler.notify.message",
                        invitation.getInvitor().getNode(getController()).getNick()),
                        task);
            }
        };

        // Invoke later
        SwingUtilities.invokeLater(worker);
    }
}
