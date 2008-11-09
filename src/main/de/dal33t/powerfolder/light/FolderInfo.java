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
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Util;

/**
 * A Folder hash info
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = 102L;

    public String name;
    public String id;
    public int filesCount;
    public long bytesTotal;

    public FolderInfo(Folder folder) {
        name = folder.getName();
        id = folder.getId();
        filesCount = folder.getKnownFilesCount();
    }

    public FolderInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Returns the joined folder, or null if folder is not joined
     * 
     * @param controller
     * @return the folder
     */
    public Folder getFolder(Controller controller) {
        return controller.getFolderRepository().getFolder(this);
    }

    /**
     * Updates the folderinfo with added file
     * 
     * @param fInfo
     */
    public synchronized void addFile(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }
        filesCount++;
        bytesTotal += fInfo.getSize();
    }

    /**
     * Updates folder info with removed files
     * 
     * @param fInfo
     */
    public synchronized void removeFile(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }
        filesCount--;
        bytesTotal -= fInfo.getSize();
    }

    // Security ****************************************************************

    /**
     * Calculates the secure Id for this folder with magicid from remote
     * 
     * @param magicId
     * @return the secure Id for this folder with magicid from remote
     */
    public String calculateSecureId(String magicId) {
        // Do the magic...
        try {
            byte[] mId = magicId.getBytes("UTF-8");
            byte[] fId = id.getBytes("UTF-8");
            byte[] hexId = new byte[mId.length * 2 + fId.length];

            // Build secure ID base: [MAGIC_ID][FOLDER_ID][MAGIC_ID]
            System.arraycopy(mId, 0, hexId, 0, mId.length);
            System.arraycopy(fId, 0, hexId, mId.length - 1, fId.length);
            System.arraycopy(mId, 0, hexId, mId.length + fId.length - 2,
                mId.length);
            return new String(Util.encodeHex(Util.md5(hexId)));
        } catch (UnsupportedEncodingException e) {
            throw (IllegalStateException) new IllegalStateException(
                "Fatal problem: UTF-8 encoding not found").initCause(e);
        }
    }

    /*
     * General
     */

    public Object clone() {
        FolderInfo fi = new FolderInfo(name, id);
        fi.bytesTotal = bytesTotal;
        fi.filesCount = filesCount;
        return fi;
    }

    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FolderInfo) {
            FolderInfo otherInfo = (FolderInfo) other;
            return Util.equals(this.id, otherInfo.id);
        }

        return false;
    }

    // used for sorting ignores case
    public int compareTo(Object other) {
        FolderInfo otherFolderInfo = (FolderInfo) other;
        return name.compareToIgnoreCase(otherFolderInfo.name);
    }

    public String toString() {
        return "Folder '" + name + '\'';
    }

}