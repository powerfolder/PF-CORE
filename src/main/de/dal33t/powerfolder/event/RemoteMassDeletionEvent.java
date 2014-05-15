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
 * $Id: RemoteMassDeletionEvent.java 8169 2009-06-10 11:57:40Z harry $
 */
package de.dal33t.powerfolder.event;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Notification that a mass delete event happened
 */
public class RemoteMassDeletionEvent {

    private final FolderInfo folderInfo;
    private final MemberInfo memberInfo;
    private final int deleteFigure;
    private SyncProfile oldProfile;
    private SyncProfile newProfile;
    private final boolean percentage;

    /**
     *
     * @param folderInfo
     *              folder info of affected folder
     * @param memberInfo
     *              offending member
     * @param deleteFigure
     *              the percentage of files deleted or number of files deleted
     *              see percentage field
     * @param oldProfile
     *              the profile that was set before the event
     * @param newProfile
     *              the safe profile switched to
     * @param percentage
     *              true if the deleteFigure is the percentege of files deleted
     *              false if the deleteFigure is an absolute number
     */
    public RemoteMassDeletionEvent(FolderInfo folderInfo,
        MemberInfo memberInfo, int deleteFigure, SyncProfile oldProfile,
        SyncProfile newProfile, boolean percentage)
    {
        this.folderInfo = folderInfo;
        this.memberInfo = memberInfo;
        this.deleteFigure = deleteFigure;
        this.oldProfile = oldProfile;
        this.newProfile = newProfile;
        this.percentage = percentage;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public MemberInfo getMemberInfo() {
        return memberInfo;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public int getDeleteFigure() {
        return deleteFigure;
    }

    public SyncProfile getOldProfile() {
        return oldProfile;
    }

    public SyncProfile getNewProfile() {
        return newProfile;
    }

}
