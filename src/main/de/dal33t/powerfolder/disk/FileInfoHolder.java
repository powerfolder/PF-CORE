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
     * the availability of this file (number of users that have the highest not
     * deleted version of this file
     */
    private int availability;

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
        availability = 1;
    }

    /**
     * removes file for this member if there
     * 
     * @param member
     * @return true if empty as result of removal
     */
    public boolean removeFileOfMember(Member member) {
        // memberHasFileInfoMap.remove(member);
        // return memberHasFileInfoMap.isEmpty();
        // TODO Implement
        return false;
    }

    /**
     * @return true if any version of this file is still available.
     */
    public boolean isAnyVersionAvailable() {
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

    public synchronized void put(Member member, FileInfo newFileInfo) {
        // memberHasFileInfoMap.put(member, newFileInfo);
        if (fileInfoIsMyOwn) { // do not overwrite myself
            calcAvailability();
            return;
        }
        if (member.isMySelf()) { // use myself as info
            this.fileInfo = newFileInfo;
            fileInfoIsMyOwn = true;
            calcAvailability();
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
        calcAvailability();
    }

    /**
     * returns the number of complete files of the latest version in the network
     * 
     * @return the # of members this file is available
     */
    public int getAvailability() {
        return availability;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * valid if at least one connected member has a not deleted version or
     * member with deleted version is myself
     * 
     * @return true if the file is valid
     */
    public boolean isValid() {
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isConnected() || member.isMySelf()) {
                FileInfo fInfo = member.getFile(fileInfo);
                if (fInfo == null) {
                    continue;
                }
                if (fInfo.isDeleted()) {
                    if (member.isMySelf()) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private void calcAvailability() {
        FileInfo newestNotDeleted = fileInfo.getNewestNotDeletedVersion(folder
            .getController().getFolderRepository());
        int tmpAvailability = 0;
        int newestVersion = newestNotDeleted != null ? newestNotDeleted
            .getVersion() : 0;
        for (Member member : folder.getMembersAsCollection()) {
            if (!member.isCompleteyConnected() && !member.isMySelf()) {
                continue;
            }
            FileInfo memberFileInfo = member.getFile(fileInfo);
            if (memberFileInfo == null || memberFileInfo.isDeleted()) {
                continue;
            }
            if (memberFileInfo.getVersion() == newestVersion) {
                tmpAvailability++;
            }
        }
        availability = tmpAvailability;
    }
}
