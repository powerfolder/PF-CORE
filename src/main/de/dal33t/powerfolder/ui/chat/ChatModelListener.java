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
 * $Id: ChatModelListener.java 5804 2008-11-10 22:53:09Z harry $
 */
package de.dal33t.powerfolder.ui.chat;

import de.dal33t.powerfolder.event.CoreListener;

/**
 * Interface to implement to listen to chat events from the ChatModel
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public interface ChatModelListener extends CoreListener {
    void chatChanged(ChatModelEvent event);
}

