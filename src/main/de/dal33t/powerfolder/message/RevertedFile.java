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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Notify a client, that a deleted file could not be deleted on the server and
 * was reverted.
 * 
 * @author <a href="mailto:krickl@powerfolder.com>Maximilian Krickl</a>
 */
public class RevertedFile extends FolderRelatedMessage {

    private static final long serialVersionUID = 100L;
    public FileInfo file;

    public RevertedFile(FolderInfo foInfo, FileInfo fInfo) {
        this.folder = foInfo;
        this.file = fInfo;
    }
}
