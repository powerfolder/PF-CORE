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
 * $Id: AskForFriendshipEventNotice.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.Controller;

/**
 * Notice of a request for freindship. Show in notification and add to app
 * model.
 */
public class AskForFriendshipEventNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;

    private final AskForFriendshipEvent event;

    public AskForFriendshipEventNotice(String title, String summary,
        AskForFriendshipEvent event)
    {
        super(title, summary);
        Reject.ifNull(event, "Event is null");
        this.event = event;
    }

    public AskForFriendshipEvent getPayload(Controller controller) {
        return event;
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
        final int prime = 31;
        int result = 0;
        result = prime
            * result
            + ((event.getMemberInfo() == null) ? 0 : event.getMemberInfo()
                .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        AskForFriendshipEventNotice other = (AskForFriendshipEventNotice) obj;
        if (event.getMemberInfo() == null) {
            if (other.event.getMemberInfo() != null)
                return false;
        } else if (!event.getMemberInfo().equals(other.event.getMemberInfo()))
            return false;
        return true;
    }
}