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

import de.dal33t.powerfolder.message.Invitation;

/**
 * Notice of a received invitation.
 * Show in notification and add to app model.
 */
public class InvitationNotice extends NoticeBase {

    private final Invitation invitation;

    public InvitationNotice(String title, String summary,
                            Invitation invitation) {
        super(title, summary);
        this.invitation = invitation;
    }

    public Invitation getPayload() {
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
}
