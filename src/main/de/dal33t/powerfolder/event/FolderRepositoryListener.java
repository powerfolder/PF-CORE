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

/** interface to implement to receive events from the FolderRepository */
public interface FolderRepositoryListener extends CoreListener {
    /**
     * Fired by the FolderRepository if a Folder is removed from the list of
     * "joined Folders"
     */
    public void folderRemoved(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository if a Folder is added to the list of "joined
     * Folders"
     */
    public void folderCreated(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when the scans are started
     */
    public void maintenanceStarted(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when the scans are finished
     */
    public void maintenanceFinished(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when starting the cleanup process for a single Folder
     */
    public void cleanupStarted(FolderRepositoryEvent e);

    /**
     * Fired by the FolderRepository when starting the cleanup process for a single Folder
     */
    public void cleanupFinished(FolderRepositoryEvent e);
}
