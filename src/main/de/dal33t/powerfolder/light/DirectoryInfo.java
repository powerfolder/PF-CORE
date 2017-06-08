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
 * $Id: DirectoryInfo.java 12941 2010-07-12 16:38:01Z tot $
 */
package de.dal33t.powerfolder.light;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.util.Reject;

import java.nio.file.Path;
import java.util.Date;

/**
 * A lightweight object representing an actual directory in the PowerFolder.
 * <p>
 * Related ticket: #378
 *
 * @author sprajc
 */
public class DirectoryInfo extends FileInfo {
    private static final long serialVersionUID = 100L;

    DirectoryInfo() {
        super();
    }

    /** FileInfo
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    DirectoryInfo(AbstractMessage mesg)
    {
        super(mesg);
    }

    DirectoryInfo(String fileName, String oid, MemberInfo modifiedBy,
        AccountInfo modifiedByAccount, Date lastModifiedDate, int version,
        String hashes, boolean deleted, String tags, FolderInfo folderInfo)
    {
        super(fileName, oid, 0, modifiedBy, modifiedByAccount,
            lastModifiedDate, version, hashes, deleted, tags, folderInfo);
    }

    DirectoryInfo(String fileName, String oid, long size,
        MemberInfo modifiedBy, AccountInfo modifiedByAccount,
        Date lastModifiedDate, int version, String hashes, boolean deleted,
        String tags, FolderInfo folderInfo)
    {
        super(fileName, oid, size, modifiedBy, modifiedByAccount,
            lastModifiedDate, version, hashes, deleted, tags, folderInfo);
    }

    DirectoryInfo(FolderInfo folder, String name) {
        super(folder, name);
    }

    public static DirectoryInfo getTemplate(FolderInfo folder, String name) {
        return new DirectoryInfo(folder, name);
    }

    public boolean isDiretory() {
        return true;
    }

    public boolean isFile() {
        return false;
    }

    @Override
    public String toString() {
        return '[' + getFolderInfo().getLocalizedName() + "]:"
            + (isDeleted() ? "(del) /" : "/") + getRelativeName() + " (D)";
    }

    /**
     * appends to buffer
     *
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    private final void toDetailString(StringBuilder str) {
        str.append(toString());
        str.append(", version: ");
        str.append(getVersion());
        str.append(", modified: ");
        str.append(getModifiedDate());
        str.append(" (");
        if (getModifiedDate() != null) {
            str.append(getModifiedDate().getTime());
        } else {
            str.append("-n/a-");
        }
        str.append(") by '");
        if (getModifiedBy() == null) {
            str.append("-n/a-");
        } else {
            str.append(getModifiedBy().nick);
        }
        str.append('\'');
    }

    public String toDetailString() {
        StringBuilder str = new StringBuilder();
        toDetailString(str);
        return str.toString();
    }

    /**
     * @param ofInfo
     *            the other fileinfo.
     * @return if this file is newer than the other one. By file version ONLY.
     */
    public boolean isNewerThan(FileInfo ofInfo) {
        return isNewerThan(ofInfo, true);
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    public boolean inSyncWithDisk(Path diskFile) {
        Reject.ifNull(diskFile, "Diskfile is null");
        return inSyncWithDisk0(diskFile, true);
    }

    // hashCode() is used from FileInfo
//
//    @Override
//    public boolean equals(Object other) {
//        if (this == other) {
//            return true;
//        }
//        if (other instanceof DirectoryInfo) {
//            DirectoryInfo otherInfo = (DirectoryInfo) other;
//            boolean caseMatch = Util.equalsRelativeName(getRelativeName(),
//                otherInfo.getRelativeName());
//            return caseMatch
//                && Util.equals(getFolderInfo(), otherInfo.getFolderInfo());
//        }
//
//        return false;
//    }

}
