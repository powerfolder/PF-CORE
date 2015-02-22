/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: InvitationNotice.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Invitation;

/**
 * Notice of a received invitation. Show in notification and add to app model.
 */
public class InvitationNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;

    private final Invitation invitation;

    public InvitationNotice(String title, String summary, Invitation invitation)
    {
        super(title, summary);
        this.invitation = invitation;
    }

    public Invitation getPayload(Controller controller) {
        return invitation;
    }

    public boolean isNotification() {
        return true;
    }

    public boolean isActionable() {
        return true;
    }

    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.INFORMATION;
    }

    public boolean isPersistable() {
        return true;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 0;
        result = prime * result
            + ((invitation == null) ? 0 : invitation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        InvitationNotice other = (InvitationNotice) obj;
        if (invitation == null) {
            if (other.invitation != null)
                return false;
        } else if (!invitation.equals(other.invitation))
            return false;
        return true;
    }

}
