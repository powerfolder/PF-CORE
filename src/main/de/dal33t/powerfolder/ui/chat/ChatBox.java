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
 * $Id: ChatBox.java 5804 2008-11-10 22:53:09Z harry $
 */
package de.dal33t.powerfolder.ui.chat;

import de.dal33t.powerfolder.Constants;

import java.util.LinkedList;
import java.util.List;

/**
 * Class representing a set of chat lines.
 */
public class ChatBox {

    private List<ChatLine> chatLines = new LinkedList<ChatLine>();

    /**
     * Adds a chat line to the list, deleting the oldest if necessary.
     *
     * @param line
     */
    public void addLine(ChatLine line) {
        chatLines.add(line);
        if (chatLines.size() > Constants.MAX_CHAT_LINES) {
            chatLines.remove(0);
        }
    }

    /**
     * Returns a safe copy of the lines.
     *
     * @return
     */
    public ChatLine[] getChatText() {
        ChatLine[] lines = new ChatLine[chatLines.size()];
        int i = 0;
        for (ChatLine chatLine : chatLines) {
            lines[i++] = chatLine;
        }
        return lines;
    }

}
