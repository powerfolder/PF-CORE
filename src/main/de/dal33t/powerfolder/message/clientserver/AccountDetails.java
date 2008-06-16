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
package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Format;

/**
 * Capsulates a identity and adding additional information.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AccountDetails implements Serializable {
    private static final long serialVersionUID = 100L;

    private Account user;
    private long spaceUsed;
    private int nFolders;

    public AccountDetails(Account user, long spaceUsed, int nFolders) {
        super();
        this.user = user;
        this.spaceUsed = spaceUsed;
        this.nFolders = nFolders;
    }

    public Account getAccount() {
        return user;
    }

    public long getSpaceUsed() {
        return spaceUsed;
    }

    public int getNFolders() {
        return nFolders;
    }

    public String toString() {
        return "AccountDetails, " + user + ". " + nFolders + " folders, "
            + Format.formatBytesShort(spaceUsed);
    }
}