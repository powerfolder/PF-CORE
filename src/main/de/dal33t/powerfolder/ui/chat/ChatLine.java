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
 * $Id: ChatLine.java 5804 2008-11-10 22:53:09Z harry $
 */
package de.dal33t.powerfolder.ui.chat;

import de.dal33t.powerfolder.Member;

/**
 * Class representing a chat line.
 */
public class ChatLine {

    private Member fromMember;
    private String text;
    private final boolean status;

    /**
     * Constructor
     *
     * @param fromMember
     * @param text
     */
    ChatLine(Member fromMember, String text) {
        this.fromMember = fromMember;
        this.text = text;
        status = false;
    }

    /**
     * Constructor, with status option.
     *
     * @param fromMember
     * @param text
     * @param status
     */
    ChatLine(Member fromMember, String text, boolean status) {
        this.fromMember = fromMember;
        this.text = text;
        this.status = status;
    }

    /**
     * Returns the member that the message was from.
     *
     * @return
     */
    public Member getFromMember() {
        return fromMember;
    }

    /**
     * Returns the message text.
     *
     * @return
     */
    public String getText() {
        return text;
    }

    /**
     * Answers if the message is a status line.
     *
     * @return
     */
    public boolean isStatus() {
        return status;
    }
}
