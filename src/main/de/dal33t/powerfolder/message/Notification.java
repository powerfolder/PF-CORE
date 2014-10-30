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
* $Id: Notification.java 4854 2008-08-04 00:34:13Z tot $
*/
package de.dal33t.powerfolder.message;


/**
 * This message represents a small event that another peer might be interested in.
 *
 * ===================================================================
 * === Note that this is a legacy class: Use AddFriendNotification ===
 * ===================================================================
 *
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision:$
 */
public class Notification extends Message {
	private static final long serialVersionUID = 100L;

	public enum Event {
		ADDED_TO_FRIENDS;
	}

	private Event event;
    private String personalMessage;

    public Notification(Event event, String personalMessage) {
		this.event = event;
        this.personalMessage = personalMessage;
    }

    /**
     * Returns the event that occured on the remote client.
     *
     * @return an Notification.Event
     */
    public Event getEvent() {
        return event;
    }

    public String getPersonalMessage() {
        return personalMessage;
    }

    public String toString() {
        return "Notification (" + event + "): '" + personalMessage + "'";
    }
}
