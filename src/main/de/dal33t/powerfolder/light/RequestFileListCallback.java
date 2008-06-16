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
package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.FileList;

/**
 * Callback, used to inform requestor of about received filelists from nodes.
 * <p>
 * TODO Refactor this hell! Whole FolderDetails/Folder preview code needs major
 * refactorings.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public interface RequestFileListCallback {
    /**
     * Informs that a filelist has been received from a node
     * 
     * @param from
     * @param filelist
     */
    public void fileListReceived(Member from, FileList filelist);

    /**
     * Informs that the request is over. No more filelist can be expected
     */
    public void fileListRequestOver();
}