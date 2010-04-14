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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Helper to better access fileinfos across members.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FileInfoHolder {
    private FileInfo fileInfo;
    private Folder folder;
    boolean fileInfoIsMyOwn;

    /**
     * creates a FileInfoHolder and reads the fileData from the FileInfo.
     * 
     * @param folder
     * @param member
     * @param fileInfo
     */
    public FileInfoHolder(Folder folder, Member member, FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.folder = folder;
        fileInfoIsMyOwn = member.isMySelf();
    }

    /**
     * removes file for this member if there
     * 
     * @param member
     * @return true if empty as result of removal
     */
    boolean removeFileOfMember(Member member) {
        // memberHasFileInfoMap.remove(member);
        // return memberHasFileInfoMap.isEmpty();
        // TODO Implement
        return false;
    }

    /**
     * @return true if any version of this file is still available.
     */
    boolean isAnyVersionAvailable() {
        for (Member member : folder.getMembersAsCollection()) {
            if (member.hasFile(fileInfo)) {
                return true;
            }
        }
        return false;
    }

    /** used to replace in converted to meta FileInfo (Mp3/Image) */
    void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    synchronized void put(Member member, FileInfo newFileInfo) {
        // memberHasFileInfoMap.put(member, newFileInfo);
        if (fileInfoIsMyOwn) { // do not overwrite myself
            return;
        }
        if (member.isMySelf()) { // use myself as info
            this.fileInfo = newFileInfo;
            fileInfoIsMyOwn = true;
            return;
        }
        if (newFileInfo.isNewerThan(this.fileInfo)) {
            // TODO What's this!?

            if (newFileInfo.getVersion() > this.fileInfo.getVersion()) {
                this.fileInfo = newFileInfo;
            } else {
                if (newFileInfo.getSize() != this.fileInfo.getSize()
                    || !newFileInfo.getModifiedBy().equals(
                        this.fileInfo.getModifiedBy())
                    || newFileInfo.getModifiedDate().equals(
                        this.fileInfo.getModifiedDate()))
                {
                    // versions equal but filesize or modifier/date has
                    // changed! use the most recent one
                    if (newFileInfo.isNewerThan(this.fileInfo)) {
                        this.fileInfo = newFileInfo;
                    }
                }
            }
        }
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

}
