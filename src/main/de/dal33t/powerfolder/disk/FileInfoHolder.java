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

import java.util.*;

/**
 * Holds a FileInfo for each Member of the Folder
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FileInfoHolder {
    private FileInfo fileInfo;
    private Folder folder;
    boolean fileInfoIsMyOwn;
    /** For each member the fileInfo. key = member, value = FileInfo */
    private Map<Member, FileInfo> memberHasFileInfoMap;

    /**
     * the availability of this file (number of users that have the highest not
     * deleted version of this file
     */
    private int availability;

    /**
     * creates a FileInfoHolder and reads the fileData from the FileInfo and
     * addes the first relationship between Member and FileInfo (member has
     * file)
     * 
     * @param folder
     * @param member
     * @param fileInfo
     */
    public FileInfoHolder(Folder folder, Member member, FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.folder = folder;
        fileInfoIsMyOwn = member.isMySelf();

        // TODO The next line is very memory consuming.
        memberHasFileInfoMap = new HashMap<Member, FileInfo>(1);
        memberHasFileInfoMap.put(member, fileInfo);
        availability = 1;
    }

    /**
     * returns the FileInfo-mation about the file at this member
     * 
     * @param member
     * @return the fileinfo
     */
    public FileInfo getFileInfo(Member member) {
        FileInfo fInfo = memberHasFileInfoMap.get(member);
        if (fInfo == null) {
            throw new IllegalArgumentException("not has file " + member);
        }
        return fInfo;
    }

    /**
     * removes file for this member if there
     * 
     * @param member
     * @return true if empty as result of removal
     */
    public synchronized boolean removeFileOfMember(Member member) {
        memberHasFileInfoMap.remove(member);
        return memberHasFileInfoMap.isEmpty();
    }

    /**
     * returns true it this members has this file and the file is not remotely
     * deleted.
     * 
     * @param member
     * @return true if this member has the file
     */
    public boolean hasFile(Member member) {
        FileInfo fInfo = memberHasFileInfoMap.get(member);
        if (fInfo == null) {
            return false;
        }
        return !fInfo.isDeleted();
    }

    /** used to replace in converted to meta FileInfo (Mp3/Image) */
    void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public synchronized void put(Member member, FileInfo newFileInfo) {
        memberHasFileInfoMap.put(member, newFileInfo);
        if (fileInfoIsMyOwn) { // do not overwite myself
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
     * @return Returns the filename.
     */
    public String getFilename() {
        return fileInfo.getFilenameOnly();
    }

    /**
     * @return Returns the folder.
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * @return Returns the path (== LocationInFolder).
     */
    public String getPath() {
        return fileInfo.getLocationInFolder();
    }

    private synchronized void calcAvailability() {
        Iterator<FileInfo> fileInfos = memberHasFileInfoMap.values().iterator();
        int tmpAvailability = 0;
        int newestVersion = getNewestAvailableVersion();
        while (fileInfos.hasNext()) {
            FileInfo fInfo = fileInfos.next();
            if (newestVersion == fInfo.getVersion() && !fInfo.isDeleted())
            {
                tmpAvailability++;
            }
        }
        availability = tmpAvailability;
    }

    private synchronized int getNewestAvailableVersion() {
        Iterator<FileInfo> fileInfos = memberHasFileInfoMap.values().iterator();
        int tmpHighestVersion = -1;
        while (fileInfos.hasNext()) {
            FileInfo fInfo = fileInfos.next();
            // TODO SCHAATSER Check: Is this correct? maybe the newest version
            // IS the deleted one? Scenario:
            // initalfile(ver:0)->modified(ver:1)->deleted by
            // user(ver:2). The latest, deleted version of the file is the
            // newest version of the file.

            // Schaatser: Maybe the name of the method is wrong. It is used to
            // calcAvailability() of the file. And since deleted files are not
            // available the result of this method is what we want. So
            // the name should be "getNewestAvailableVersion" ?
            if (fInfo.isDeleted()) {
                continue;
            }
            tmpHighestVersion = Math.max(tmpHighestVersion, fInfo
                .getVersion());
        }
        return tmpHighestVersion;
    }

    /**
     * returns the number of complete files of the latest version in the network
     * 
     * @return the # of members this file is available
     */
    public int getAvailability() {
        return availability;
    }

    /**
     * returns a list of Members that have the file
     * 
     * @return the sources
     */
    public synchronized List<Member> getSources() {
        int newestVersion = getNewestAvailableVersion();
        Iterator<Member> members = memberHasFileInfoMap.keySet().iterator();
        List<Member> sources = new ArrayList<Member>();
        while (members.hasNext()) {
            Member member = members.next();
            FileInfo fInfo = memberHasFileInfoMap.get(member);
            if (fInfo.getVersion() == newestVersion && !fInfo.isDeleted())
            {
                sources.add(member);
            }
        }
        return sources;
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
    public synchronized boolean isValid() {
        Iterator<Member> members = memberHasFileInfoMap.keySet().iterator();
        while (members.hasNext()) {
            Member member = members.next();
            if (member.isConnected() || member.isMySelf()) {
                FileInfo fInfo = memberHasFileInfoMap.get(member);
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
}
