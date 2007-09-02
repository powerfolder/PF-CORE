/* $Id: FolderInfo.java,v 1.9 2005/07/18 15:14:08 schaatser Exp $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Util;

/**
 * A Folder hash info
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderInfo implements Serializable, Cloneable, Comparable {
    private static final long serialVersionUID = 102L;
    
    public String name;
    public String id;
    public int filesCount;
    public long bytesTotal;
    public boolean secret;

    public FolderInfo(Folder folder) {
        name = folder.getName();
        id = folder.getId();
        filesCount = folder.getKnownFilesCount();
        secret = folder.isSecret();
    }

    public FolderInfo(String name, String id, boolean secret) {
        this.name = name;
        this.id = id;
        this.secret = secret;
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
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new RuntimeException("Unable to clone folderinfo", e);
        }
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
        return "Folder '" + name + "'";
    }

}