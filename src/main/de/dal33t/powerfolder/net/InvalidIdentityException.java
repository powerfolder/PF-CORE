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
* $Id$
*/
package de.dal33t.powerfolder.net;

/**
 * Throws if a client did identify wrong, member should be invalidated
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
@SuppressWarnings("serial")
public class InvalidIdentityException extends ConnectionException {
    private ConnectionHandler from;

    /**
     * @param message
     */
    public InvalidIdentityException(String message, ConnectionHandler from) {
        super(message);
        if (from == null) {
            throw new NullPointerException("From is null");
        }
        this.from = from;
    }

    /**
     * Returns the connection handler from which the wrong identity was received
     *
     * @return
     */
    public ConnectionHandler getFrom() {
        return from;
    }
}