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

/** implement when you want to receive events from the PowerFolder Recycle Bin. */
public interface RecycleBinListener extends CoreListener {
    /** A file was added to the recycle bin */
    public void fileAdded(RecycleBinEvent e);

    /**
     * A file was removed from the recycle bin, this means either permanently
     * deleted, moved to the system recycle bin or restored.
     */
    public void fileRemoved(RecycleBinEvent e);

    /**
     * A file was updated in the recycle bin, this happens when the same file is
     * overwritten with a new file
     */
    public void fileUpdated(RecycleBinEvent e);
}
