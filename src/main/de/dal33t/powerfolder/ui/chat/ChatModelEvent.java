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
 * $Id: ChatModelEvent.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.chat;

import java.util.EventObject;

import de.dal33t.powerfolder.Member;

/**
 * Event that indicates a chat event occurred.
 */
public class ChatModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    private boolean statusFlag;
    private String message = "";
    private boolean createdLocally;

    /**
     * Constructor, creating the event.
     * 
     * @param fromMember
     *          member who sent the message
     * @param message
     * @param statusFlag
     */
    ChatModelEvent(Member fromMember, String message, boolean statusFlag, boolean createdLocally) {
        super(fromMember);
        this.message = message;
        this.statusFlag = statusFlag;
        this.createdLocally = createdLocally;
    }

    /**
     * @return whether the event is a status type.
     */
    public boolean isStatusFlag() {
        return statusFlag;
    }

    /**
     * @return the event message.
     */
    public String getMessage() {
        return message;
    }

    public boolean isCreatedLocally() {
        return createdLocally;
    }

    public String toString() {
        return "ChatModelEvent " + getSource() + ". (" +
                (statusFlag ? "S" : "M") + (createdLocally ? "L" : "R") + "): "
                + message;
    }
}
