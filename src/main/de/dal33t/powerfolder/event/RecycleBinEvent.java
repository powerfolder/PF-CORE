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
package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;

/** Event fired if something changed in The PowerFolder Recycle Bin */
public class RecycleBinEvent extends EventObject {
    /** The file that was moved from or to the recycle bin */
    private FileInfo file;

    /**
     * Create a recycle Bin Event
     * 
     * @param recycleBin
     *            the source of the event
     * @param file
     *            the file that was moved from or to the recycle bin
     */
    public RecycleBinEvent(RecycleBin recycleBin, FileInfo file) {
        super(recycleBin);
        this.file = file;
    }

    /**
     * the file that was moved from or to the recycle bin
     * 
     * @return the file that was moved from or to the recycle bin
     */
    public FileInfo getFile() {
        return file;
    }

}
