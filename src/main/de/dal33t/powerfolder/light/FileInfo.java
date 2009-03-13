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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * File information of a local or remote file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfo implements Serializable, DiskItem, Cloneable {

    private static final Logger log = Logger.getLogger(FileInfo.class.getName());
    private static final long serialVersionUID = 100L;

    /** The filename (including the path from the base of the folder) */
    private String fileName;

    /** The size of the file */
    private Long size;

    /** modified info */
    private MemberInfo modifiedBy;
    /** modified in folder on date */
    private Date lastModifiedDate;

    /** Version number of this file */
    private int version;

    /** the deleted flag */
    private boolean deleted;

    /** the folder */
    private FolderInfo folderInfo;

    /**
     * Contains some cached string.
     */
    private transient Reference<FileInfoStrings> cachedStrings;

    protected FileInfo() {
        // ONLY for backward compatibility to MP3FileInfo
    }

    /**
     * Used to initalize fileinfo from link
     * 
     * @param foInfo
     * @param name
     */
    public FileInfo(FolderInfo foInfo, String name) {
        if (foInfo == null) {
            throw new NullPointerException("Folderinfo is null");
        }
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Filename is empty");
        }
        this.folderInfo = foInfo;
        this.fileName = name;
    }

    /**
     * Initalize within a folder
     * 
     * @param folder
     * @param localFile
     */
    public FileInfo(Folder folder, File localFile) {
        Reject.ifNull(folder, "Folder is null");
        Reject.ifNull(localFile, "LocalFile is null");

        this.folderInfo = folder.getInfo();
        this.size = new Long(localFile.length());
        this.fileName = localFile.getName();
        this.lastModifiedDate = new Date(localFile.lastModified());
        this.deleted = false;

        File parent = localFile.getParentFile();
        File folderBase = folder.getLocalBase();

        while (!folderBase.equals(parent)) {
            if (parent == null) {
                throw new IllegalArgumentException(
                    "Local file seems not to be in a subdir of the local powerfolder copy");
            }
            fileName = parent.getName() + "/" + fileName;
            parent = parent.getParentFile();
        }
    }

    /**
     * Gets filled with all important data from the other file info
     * 
     * @param other
     */
    public void copyFrom(FileInfo other) {
        this.fileName = other.fileName;
        this.size = other.size;
        this.modifiedBy = other.modifiedBy;
        this.lastModifiedDate = other.lastModifiedDate;
        this.version = other.version;
        this.deleted = other.deleted;
        this.folderInfo = other.folderInfo;
    }

    /**
     * Syncs fileinfo with diskfile. If diskfile has other lastmodified date
     * that this. Assume that file has changed on disk and update its modified
     * info.
     * 
     * @param controller
     * @param diskFile
     *            the diskfile of this file, not gets it from controller !
     * @return if the file was synced or false if file is in sync
     */
    public boolean syncFromDiskIfRequired(Controller controller, File diskFile)
    {
        if (controller == null) {
            throw new NullPointerException("controller is null");
        }

        if (diskFile == null) {
            return false;
        }

        // Check if files match
        if (!diskFile.getName().equals(this.getFilenameOnly())) {
            throw new IllegalArgumentException(
                "Diskfile does not match fileinfo: " + this + ", diskfile: "
                    + diskFile);
        }

        // if (!diskFile.exists()) {
        // log.warning("File does not exsists on disk: " + toDetailString());
        // }

        boolean filesDiffered = !inSyncWithDisk(diskFile);
        if (filesDiffered) {
            this.version++;
            setDeleted(!diskFile.exists());

            if (!isDeleted()) {
                setModifiedInfo(controller.getMySelf().getInfo(), new Date(
                    diskFile.lastModified()));
                setSize(diskFile.length());
            } else {
                setModifiedInfo(controller.getMySelf().getInfo(), new Date());
                setSize(0);
            }
            // log.warning("File updated to: " + this.toDetailString());
        }

        return filesDiffered;
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    public boolean inSyncWithDisk(File diskFile) {
        Reject.ifNull(diskFile, "Diskfile is null");
        boolean diskFileDeleted = !diskFile.exists();
        boolean existanceSync = diskFileDeleted && isDeleted()
            || !diskFileDeleted && !isDeleted();
        boolean lastModificationSync = Util.equalsFileDateCrossPlattform(
            diskFile.lastModified(), lastModifiedDate.getTime());
        boolean sizeSync = size.longValue() == diskFile.length();
        return existanceSync && lastModificationSync && sizeSync;
    }

    /**
     * @param size
     */
    public void setSize(long size) {
        this.size = new Long(size);
    }

    /**
     * Sets the new folder info for this file
     * 
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "Folder is null");
        this.folderInfo = folderInfo;
    }

    /** @return The filename (including the path from the base of the folder) */
    public String getName() {
        return fileName;
    }

    /**
     * @return The filename (including the path from the base of the folder)
     *         converted to lowercase
     */
    public String getLowerCaseName() {
        if (Feature.CACHE_FILEINFO_STRINGS.isDisabled()) {
            return fileName.toLowerCase();
        }
        FileInfoStrings strings = getStringsCache();
        if (strings.getLowerCaseName() == null) {
            strings.setLowerCaseName(fileName.toLowerCase());
        }
        return strings.getLowerCaseName();
    }

    private FileInfoStrings getStringsCache() {
        FileInfoStrings stringsRef = cachedStrings != null ? cachedStrings
            .get() : null;
        if (stringsRef == null) {
            // Cache miss. create new entry
            stringsRef = new FileInfoStrings();
            cachedStrings = new WeakReference<FileInfoStrings>(stringsRef);
        }
        return stringsRef;
    }

    /**
     * @return everything after the last point (.) in the fileName in upper case
     */
    public String getExtension() {
        String tmpFileName = getFilenameOnly();
        int index = tmpFileName.lastIndexOf(".");
        if (index == -1)
            return null;
        return tmpFileName.substring(index + 1, tmpFileName.length())
            .toUpperCase();
    }

    /**
     * Gets the filename only, without the directory structure
     * 
     * @return the filename only of this file.
     */
    public String getFilenameOnly() {
        if (Feature.CACHE_FILEINFO_STRINGS.isDisabled()) {
            return getFilenameOnly0();
        }
        FileInfoStrings strings = getStringsCache();
        if (strings.getFileNameOnly() == null) {
            strings.setFileNameOnly(getFilenameOnly0());
        }
        return strings.getFileNameOnly();
    }

    private final String getFilenameOnly0() {
        int lastOffset = fileName.lastIndexOf('/');
        if (lastOffset < 0) {
            lastOffset = fileName.lastIndexOf('\\');
        }
        if (lastOffset < 0) {
            return fileName;
        }
        return fileName.substring(lastOffset + 1, fileName.length());
    }

    /**
     * Returns the location in folder (subdirectory) (path)
     * 
     * @return the location in folder
     */
    public String getLocationInFolder() {
        if (Feature.CACHE_FILEINFO_STRINGS.isDisabled()) {
            return getLocationInFolder0();
        }
        FileInfoStrings strings = getStringsCache();
        if (strings.getLocationInFolder() == null) {
            strings.setLocationInFolder(getLocationInFolder0());
        }
        return strings.getLocationInFolder();
    }

    private final String getLocationInFolder0() {
        String filenameOnly = getFilenameOnly();
        int filenameOnlyLength = filenameOnly.length();
        int filenameLength = fileName.length();

        if (filenameOnlyLength == filenameLength) {
            return "";
        }
        return fileName.substring(0, filenameLength - filenameOnlyLength - 1);
    }

    /**
     * @return if this file was deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param b
     */
    public void setDeleted(boolean b) {
        deleted = b;
    }

    /**
     * @param repo
     * @return if this file is expeced
     */
    public boolean isExpected(FolderRepository repo) {
        if (isDeleted()) {
            return false;
        }
        Folder folder = repo.getFolder(folderInfo);
        if (folder == null) {
            return false;
        }
        return !folder.isKnown(this);
    }

    /**
     * @param controller
     * @return if this file was modified by a friend
     */
    public boolean isModifiedByFriend(Controller controller) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        return getModifiedBy() != null && getModifiedBy().isFriend(controller);
    }

    /**
     * @param controller
     * @return if this file is currently downloading
     */
    public boolean isDownloading(Controller controller) {
        return controller.getTransferManager().isDownloadingActive(this);
    }

    /**
     * @param controller
     * @return if this file is currently uploading
     */
    public boolean isUploading(Controller controller) {
        return controller.getTransferManager().isUploading(this);
    }

    /**
     * @param controller
     * @return if the diskfile exists
     */
    public boolean diskFileExists(Controller controller) {
        File diskFile = getDiskFile(controller.getFolderRepository());
        return diskFile != null && diskFile.exists();
    }

    /**
     * @return the size of the file.
     */
    public long getSize() {
        return size.longValue();
    }

    /**
     * @return the modificator of this file.
     */
    public MemberInfo getModifiedBy() {
        return modifiedBy;
    }

    /**
     * @return the modification date.
     */
    public Date getModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @return the version of the file.
     */
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Sets the mofification info.
     * 
     * @param by
     * @param when
     */
    public void setModifiedInfo(MemberInfo by, Date when) {
        modifiedBy = by;
        lastModifiedDate = when;
    }

    /**
     * @param repo
     *            the folder repository.
     * @return if this file is newer on local disk than in folder.
     */
    public boolean isNewerOnDisk(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("FolderRepo is null");
        }
        File diskFile = getDiskFile(repo);
        if (diskFile == null) {
            return false;
        }
        return lastModifiedDate.getTime() < diskFile.lastModified();
    }

    /**
     * @param ofInfo
     *            the other fileinfo.
     * @return if this file is newer than the other one. By file version, or
     *         file modification date if version of both =0
     */
    public boolean isNewerThan(FileInfo ofInfo) {
        if (ofInfo == null) {
            throw new NullPointerException("Other file is null");
        }
        if (Feature.DETECT_UPDATE_BY_VERSION.isDisabled()) {
            // Directly detected by last modified
            return Util.isNewerFileDateCrossPlattform(getModifiedDate(), ofInfo
                .getModifiedDate());
        }
        if (getVersion() == ofInfo.getVersion()) {
            // /if (logEnabled) {
            // log()
            // .verbose(
            // "Inital version of two files detected, the one with newer
            // modification date is newer");
            // }
            // return Convert
            // .convertToGlobalPrecision(getModifiedDate().getTime()) > Convert
            // .convertToGlobalPrecision(ofInfo.getModifiedDate().getTime());
            return Util.isNewerFileDateCrossPlattform(getModifiedDate(), ofInfo
                .getModifiedDate());
        }
        return getVersion() > ofInfo.getVersion();
    }

    /**
     * Also considers myself.
     * 
     * @param repo
     *            the folder repository
     * @return if there is a newer version available of this file
     */
    public boolean isNewerAvailable(FolderRepository repo) {
        FileInfo newestFileInfo = getNewestVersion(repo);
        return newestFileInfo != null && newestFileInfo.isNewerThan(this);
    }

    /**
     * Also considers myself
     * 
     * @param repo
     * @return the newest available version of this file
     */
    public FileInfo getNewestVersion(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("FolderRepo is null");
        }
        Folder folder = getFolder(repo);
        if (folder == null) {
            throw new IllegalStateException(
                "Unable to determine newest version. Folder not joined "
                    + getFolderInfo());
        }
        FileInfo newestVersion = null;
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isCompleteyConnected() || member.isMySelf()) {
                // Get remote file
                FileInfo remoteFile = member.getFile(this);
                if (remoteFile == null) {
                    continue;
                }
                // Check if remote file in newer
                if (newestVersion == null
                    || remoteFile.isNewerThan(newestVersion))
                {
                    // log.finer("Newer version found at " + member);
                    newestVersion = remoteFile;
                }
            }
        }
        return newestVersion;
    }

    /**
     * @param repo
     * @return the newest available version of this file, excludes deleted
     *         remote files
     */
    public FileInfo getNewestNotDeletedVersion(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("FolderRepo is null");
        }
        Folder folder = getFolder(repo);
        if (folder == null) {
            log
                .warning("Unable to determine newest version. Folder not joined "
                    + getFolderInfo());
            return null;
        }
        FileInfo newestVersion = null;
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isCompleteyConnected() || member.isMySelf()) {
                // Get remote file
                FileInfo remoteFile = member.getFile(this);
                if (remoteFile == null || remoteFile.isDeleted()) {
                    continue;
                }
                // Check if remote file is newer
                if (newestVersion == null
                    || remoteFile.isNewerThan(newestVersion))
                {
                    // log.finer("Newer version found at " + member);
                    newestVersion = remoteFile;
                }
            }
        }
        return newestVersion;
    }

    /**
     * Resolves a file from local disk by folder repository, File MAY NOT Exist!
     * Returns null if folder was not found
     * 
     * @param repo
     * @return the file.
     */
    public File getDiskFile(FolderRepository repo) {
        Reject.ifNull(repo, "Repo is null");

        Folder folder = getFolder(repo);
        if (folder == null) {
            return null;
        }
        return folder.getDiskFile(this);
    }

    /**
     * Resolves a FileInfo from local folder db by folder repository, File MAY
     * NOT Exist! Returns null if folder was not found
     * 
     * @param repo
     * @return the FileInfo which is is in my own DB/knownfiles.
     */
    public FileInfo getLocalFileInfo(FolderRepository repo) {
        Reject.ifNull(repo, "Repo is null");
        Folder folder = getFolder(repo);
        if (folder == null) {
            return null;
        }
        return folder.getFile(this);
    }

    /**
     * @return the folderinfo this file belongs to.
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * @param repo
     *            the folder repository.
     * @return the folder for this file.
     */
    public Folder getFolder(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("Repository is null");
        }
        return repo.getFolder(folderInfo);
    }

    /*
     * General
     */
    
    /**
     * @param otherFile
     * @return true if the file name, version and date is equal.
     */
    public boolean isVersionAndDateIdentical(FileInfo otherFile) {
        if (otherFile == null) {
            return false;
        }

        if (!equals(otherFile)) {
            // not equals, return
            return false;
        }

        return this.getVersion() == otherFile.getVersion()
            && this.getModifiedDate().equals(otherFile.getModifiedDate());
    }

    /**
     * ATTENTION: BE WARNED USING THIS METHOD! It is possible that FileInfos
     * with version 0, same date BUT DIFFRENT modifier exists! This is caused by
     * initial scans on both sides. WHO wins then? NOBODY, FileInfos then have
     * version 0 same date but DIFFRENT modifiers. If you are seeking a way of
     * checking if a FileInfo is newer/or in sync use the method
     * <code>{@link #isNewerThan(FileInfo)}</code>
     * <p>
     * PROPOSED SOLUTION: Add a method which compares name AND version:
     * isIdenticalVersion.
     * 
     * @param otherFile
     *            the other file to compare with
     * @return if the the two files are completely identical, also checks
     *         version, date and modified user
     * @see #isNewerThan(FileInfo)
     * @deprecated
     */
    public boolean isCompletelyIdentical(FileInfo otherFile) {
        if (otherFile == null) {
            return false;
        }

        if (!equals(otherFile)) {
            // not equals, return
            return false;
        }

        boolean identical = this.getVersion() == otherFile.getVersion()
            && this.getModifiedDate().equals(otherFile.getModifiedDate())
            && this.getModifiedBy().equals(otherFile.getModifiedBy());

        if (this.getVersion() != 0
            && this.getVersion() == otherFile.getVersion()
            && this.getModifiedDate().equals(otherFile.getModifiedDate())
            && !this.getModifiedBy().equals(otherFile.getModifiedBy()))
        {
            log.severe("Found identical files, but diffrent modifier:"
                + toDetailString() + " other: " + otherFile.toDetailString());
        }
        return identical;
    }

    public int hashCode() {
        int hash = fileName.hashCode();
        hash += folderInfo.hashCode();
        return hash;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FileInfo) {
            FileInfo otherInfo = (FileInfo) other;
            return Util.equals(this.fileName, otherInfo.fileName)
                && Util.equals(this.folderInfo, otherInfo.folderInfo);
        }

        return false;
    }

    public String toString() {
        return "[" + folderInfo.name + "]:/" + (deleted ? "(del) " : "")
            + fileName;
    }

    @Override
    public Object clone() {
        try {
            FileInfo info = (FileInfo) super.clone();
            return info;
        } catch (CloneNotSupportedException e) {
            // Rethrow as unchecked exception
            throw new RuntimeException(e);
        }
    }

    /**
     * appends to buffer
     * 
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    private final void toDetailString(StringBuilder str) {
        if (deleted) {
            str.append("(del) ");
        }
        str.append(toString());
        str.append(", size: ");
        str.append(size);
        str.append(" bytes, version: ");
        str.append(getVersion());
        str.append(", modified: ");
        str.append(lastModifiedDate);
        str.append(" (");
        if (lastModifiedDate != null) {
            str.append(lastModifiedDate.getTime());
        } else {
            str.append("-n/a-");
        }
        str.append(") by '");
        if (modifiedBy == null) {
            str.append("-n/a-");
        } else {
            str.append(modifiedBy.nick);
        }
        str.append("'");
    }

    public String toDetailString() {
        StringBuilder str = new StringBuilder();
        toDetailString(str);
        return str.toString();
    }

    /**
     * Validates the state of the FileInfo. This should actually not be public -
     * checks should be made while constructing this class (by
     * constructor/deserialization).
     * 
     * @throws IllegalArgumentException
     *             if the state is corrupt
     */
    public void validate() {
        Reject.ifTrue(StringUtils.isEmpty(fileName), "Filename is empty");
        Reject.ifNull(size, "Size is null");
        Reject.ifFalse(size >= 0, "Negative file size");
        Reject.ifNull(lastModifiedDate, "Modification date is null");
        if (lastModifiedDate.getTime() < 0) {
            throw new IllegalStateException("Modification date is invalid: "
                + lastModifiedDate);
        }
        Reject.ifNull(folderInfo, "FolderInfo is null");
    }

    // Serialization optimization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        fileName = fileName.intern();
    }
}