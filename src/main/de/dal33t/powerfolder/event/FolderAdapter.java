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

/**
 * Convinience adataper for <code>FolderListener</code>
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public abstract class FolderAdapter implements FolderListener {

    public void remoteContentsChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void scanResultCommited(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void fileChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void statisticsCalculated(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void syncProfileChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void archiveSettingsChanged(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public void filesDeleted(FolderEvent folderEvent) {
        // Do nothing by default
    }

    public abstract boolean fireInEventDispatchThread();
}
