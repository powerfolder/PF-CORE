/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
package de.dal33t.powerfolder.clientserver;

import java.io.Serializable;

import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.util.Reject;

/**
 * Represents a request to send an invitation to another member by email.
 *
 * @author Dennis "Bytekeeper" Waldherr
 */
public class SendInvitationEmail implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String recipient;
    private final boolean ccMe;
    private final Invitation invitation;

    /**
     * Creates a new request to send an email to the given recipient.
     *
     * @param invitation
     *            the invitation to send
     * @param recipient
     *            the recipient's email address
     */
    public SendInvitationEmail(Invitation invitation, String recipient) {
        this.invitation = invitation;
        this.recipient = recipient;
        ccMe = false;
        validate();
    }

    /**
     * Creates a new request to send an email to the given recipient and the
     * given carbon copy recipient.
     *
     * @param invitation
     *            the invitation to send
     * @param recipient
     *            the recipient's email address
     * @param ccMe
     *            add requester as CC recipient
     */
    public SendInvitationEmail(Invitation invitation, String recipient,
        boolean ccMe)
    {
        this.invitation = invitation;
        this.recipient = recipient;
        this.ccMe = ccMe;
        validate();
    }

    public static SendInvitationEmail create(MemberInfo invitorDevice,
        FolderPermission foPermission, Account invitor, String inviteeName)
    {
        Invitation inv = new Invitation(foPermission);
        inv.setSenderDevice(invitorDevice);
        inv.setSender(invitor.getUsername());
        inv.setRecipient(inviteeName);
        return new SendInvitationEmail(inv, inviteeName);
    }

    // Serialization and validation

    private void validate() {
        Reject.ifNull(invitation, "Invitation is null!");
        // validateRecipient();
    }

    // private void validateRecipient() {
    // Reject.ifNull(getRecipient(), "Recipient is null!");
    // validateEmail(getRecipient());
    // }

    // private void validateEmail(String address) {
    // Reject.ifTrue(address.indexOf('@') < 0,
    // "Address has to be an email address: " + address);
    // }

    /**
     * @return the invitation
     */
    public Invitation getInvitation() {
        return invitation;
    }

    /**
     * @return the carbon copy recipient, or null
     */
    public boolean getCCMe() {
        return ccMe;
    }

    /**
     * @return the recipient of the email
     */
    public String getRecipient() {
        return recipient;
    }
}
