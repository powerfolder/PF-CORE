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

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Notification that 
 */
public class RemoteMassDeletionEvent {

    private final FolderInfo folderInfo;
    private final MemberInfo memberInfo;
    private final int deletePercentage;
    private String oldProfileName;
    private String newProfileName;

    public RemoteMassDeletionEvent(FolderInfo folderInfo, MemberInfo memberInfo,
                                   int deletePercentage, String oldProfileName,
                                   String newProfileName) {
        this.folderInfo = folderInfo;
        this.memberInfo = memberInfo;
        this.deletePercentage = deletePercentage;
        this.oldProfileName = oldProfileName;
        this.newProfileName = newProfileName;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public MemberInfo getMemberInfo() {
        return memberInfo;
    }

    public int getDeletePercentage() {
        return deletePercentage;
    }

    public String getOldProfileName() {
        return oldProfileName;
    }

    public String getNewProfileName() {
        return newProfileName;
    }
}
