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

import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.event.InvitationReceivedListener;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

/**
 * Class to manage received invitations. Invitations can be added and removed.
 * Also a value model is available to count invites.
 * Listeners can be added to be notified of changes to the number of invites
 * in the model.
 */
public class ReceivedInvitationsModel {

    private final ValueModel receivedInvitationsCountVM = new ValueHolder();
    private List<InvitationReceivedListener> listeners;

    private List<Invitation> invitations =
            new CopyOnWriteArrayList<Invitation>();

    public ReceivedInvitationsModel() {
        receivedInvitationsCountVM.setValue(0);
        listeners =
                new CopyOnWriteArrayList<InvitationReceivedListener>();
    }

    public void addInvitationReceivedListener(InvitationReceivedListener l) {
        listeners.add(l);
    }

    public void removeInvitationReceivedListener(InvitationReceivedListener l) {
        listeners.remove(l);
    }

    /**
     * Add an invitation to the model.
     *
     * @param invitation
     */
    public void addInvitation(Invitation invitation) {
        invitations.add(invitation);
        receivedInvitationsCountVM.setValue(invitations.size());
        for (InvitationReceivedListener invitationReceivedListener
                : listeners) {
            invitationReceivedListener.invitationReceived(
                    new InvitationReceivedEvent(this));
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
            for (InvitationReceivedListener invitationReceivedListener
                    : listeners) {
                invitationReceivedListener.invitationReceived(
                        new InvitationReceivedEvent(this));
            }
            return invitation;
        }
        return null;
    }

    public ValueModel getReceivedInvitationsCountVM() {
        return receivedInvitationsCountVM;
    }
}
