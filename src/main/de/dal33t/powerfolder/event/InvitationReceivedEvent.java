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
package de.dal33t.powerfolder.event;

import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Reject;

import java.util.EventObject;

/**
 * Event which gets fired to <code>InvitationReceivedHandler</code> that is
 * listening to the <code>FolderRepository</code>.
 *
 * @see de.dal33t.powerfolder.event.InvitationReceivedHandler
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @see de.dal33t.powerfolder.ui.InvitationReceivedHandlerDefaultImpl
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class InvitationReceivedEvent extends EventObject {

    private Invitation invitation;
    private boolean sendInvitationIfJoined;

    /**
     * @param source
     *            the source folder repo
     * @param invitation
     * @param sendInvitationIfJoined
     *            If folder joined, offer to send invitation
     */
    public InvitationReceivedEvent(FolderRepository source,
        Invitation invitation, boolean sendInvitationIfJoined)
    {
        super(source);
        Reject.ifNull(source, "Folder Repository is null");
        this.invitation = invitation;
        this.sendInvitationIfJoined = sendInvitationIfJoined;
    }

    public FolderRepository getFolderRepository() {
        return (FolderRepository) getSource();
    }

    public Invitation getInvitation() {
        return invitation;
    }

    public boolean isSendInvitationIfJoined() {
        return sendInvitationIfJoined;
    }
}
