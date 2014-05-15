/*
* Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * PFS-1244: Notify a client, that a file could not be uploaded because it would exceed the quota
 * 
 * @author <a href="mailto:sprajc@powerfolder.com>Christian Sprajc</a>
 */
public class QuotaExceeded extends FolderRelatedMessage {

    private static final long serialVersionUID = 100L;
    public FileInfo file;
    public AccountInfo account;

    public QuotaExceeded(FileInfo fInfo, AccountInfo account) {
        this.folder = fInfo.getFolderInfo();
        this.file = fInfo;
        this.account = account;
    }
}
