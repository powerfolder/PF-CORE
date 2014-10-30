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
package de.dal33t.powerfolder.message;

/**
 * Indicated the accept of the identity, which was sent
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class IdentityReply extends Message {
    private static final long serialVersionUID = 100L;

    public boolean accepted;
    public String message;

	public IdentityReply() {
		// Serialisation constructor
	}

    /**
     * Builds a new identity reply
     * @param accepted
     * @param message
     */
    private IdentityReply(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message;
    }

    /**
     * Builds a identity reply rejecting the
     * identity. a cause should be declared
     *
     * @param why
     * @return
     */
    public static IdentityReply reject(String why) {
        return new IdentityReply(false, why);
    }

    /**
     * Builds a identity reply, accpeting identity
     * @return
     */
    public static IdentityReply accept() {
        return new IdentityReply(true, null);
    }

    public String toString() {
        String reply = accepted ? "accepted" : "rejected";
        return "Identity " + reply + (message == null ? "" : ": " + message);
    }
}
