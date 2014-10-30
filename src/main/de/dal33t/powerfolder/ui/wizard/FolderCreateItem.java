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
* $Id: FolderCreateItem.java 6995 2009-02-11 12:44:25Z harry $
*/
package de.dal33t.powerfolder.ui.wizard;

import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * This class encapsulates requirements to create a folder in the Wizard flow.
 * Used where creating multiple folders.
 */
public class FolderCreateItem {

    private Path localBase;
    private FolderInfo folderInfo;
    private SyncProfile syncProfile;
    private int archiveHistory;
    private String linkToOnlineFolder;

    public FolderCreateItem(Path localBase) {
        this.localBase = localBase;
    }

    public Path getLocalBase() {
        return localBase;
    }

    public void setLocalBase(Path localBase) {
        Reject.ifNull(localBase, "Local base cannot be set null");
        this.localBase = localBase;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
    }

    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    public void setSyncProfile(SyncProfile syncProfile) {
        this.syncProfile = syncProfile;
    }

    public int getArchiveHistory() {
        return archiveHistory;
    }

    public void setArchiveHistory(int archiveHistory) {
        this.archiveHistory = archiveHistory;
    }

    public String getLinkToOnlineFolder() {
        return linkToOnlineFolder;
    }

    public void setLinkToOnlineFolder(String linkToOnlineFolder) {
        this.linkToOnlineFolder = linkToOnlineFolder;
    }
}
