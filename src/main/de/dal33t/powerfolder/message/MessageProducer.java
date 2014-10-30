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
 * $Id: Folder.java 13672 2010-09-03 11:00:20Z tot $
 */
package de.dal33t.powerfolder.message;

import java.io.Externalizable;

/**
 * Something that produces messages.
 *
 * @author sprajc
 */
public interface MessageProducer {
    /**
     * @param useExt
     *            #2072: if use {@link Externalizable} versions of the messages.
     * @return the produces messages.
     */
    Message[] getMessages(boolean useExt);
}