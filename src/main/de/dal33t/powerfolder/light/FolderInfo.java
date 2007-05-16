/* $Id: FolderInfo.java,v 1.9 2005/07/18 15:14:08 schaatser Exp $
 */
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
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
            return new String(encodeHex(md5(hexId)));
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

    // Encoding stuff *********************************************************

    /**
     * Used building output as Hex
     */
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Calculates the MD5 digest and returns the value as a 16 element
     * <code>byte[]</code>.
     * 
     * @param data
     *            Data to digest
     * @return MD5 digest
     */
    private static byte[] md5(byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Returns a MessageDigest for the given <code>algorithm</code>.
     * 
     * @param algorithm
     *            The MessageDigest algorithm name.
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns an MD5 MessageDigest.
     * 
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getMd5Digest() {
        return getDigest("MD5");
    }

    /**
     * Converts an array of bytes into an array of characters representing the
     * hexidecimal values of each byte in order. The returned array will be
     * double the length of the passed array, as it takes two characters to
     * represent any given byte.
     * 
     * @param data
     *            a byte[] to convert to Hex characters
     * @return A char[] containing hexidecimal characters
     */
    private static char[] encodeHex(byte[] data) {
        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }

    // Serialization optimization *********************************************

    private void readObject(java.io.ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        name = name.intern();
        id = id.intern();
    }
}