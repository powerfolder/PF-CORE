/* $Id: FileInfo.java,v 1.33 2006/04/09 23:45:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.light;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Util;

/**
 * File information of a local or remote file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfo extends Loggable implements Serializable {
    private static final long serialVersionUID = 100L;

    private String fileName;
    private Long size;

    // Caching transient field for the filename only
    private transient SoftReference<String> fileNameOnly;
    // Caching lowercase filename for sorting
    private transient SoftReference<String> lowerCaseName;
    // Caching location in folder
    private transient SoftReference<String> locationInFolder;
    // modified info
    private MemberInfo modifiedBy;
    // modified in folder on date
    private Date lastModifiedDate;

    // Version number of this file
    private int version;

    // the deleted flag
    private boolean deleted;

    // the folder
    private FolderInfo folderInfo;

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
        if (localFile == null) {
            throw new NullPointerException("LocalFile is null");
        }

        setFolder(folder);
        this.size = new Long(localFile.length());
        this.fileName = localFile.getName();
        this.fileNameOnly = null;
        this.lastModifiedDate = new Date(localFile.lastModified());
        this.deleted = false;

        File parent = localFile.getParentFile();
        File folderBase = folder.getLocalBase();

        while (!folderBase.equals(parent)) {
            if (parent == null) {
                throw new NullPointerException(
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

        // if (!diskFile.exists()) {
        // log().warn("File does not exsists on disk: " + toDetailString());
        // }

        boolean filesDiffered = false;

        // Check if files match
        if (!diskFile.getName().equals(this.getFilenameOnly())) {
            throw new IllegalArgumentException(
                "Diskfile does not match fileinfo: " + this + ", diskfile: "
                    + diskFile);
        }

        if (diskFile.exists() && isDeleted()) {
            // File has been recovered, exists on disk, remove deleted flag
            if (logVerbose) {
                log().verbose("File recovered from: " + toDetailString());
            }

            setDeleted(false);
            filesDiffered = true;
            // Set us as modifier
            setModifiedInfo(controller.getMySelf().getInfo(), new Date(diskFile
                .lastModified()));
        }

        if (!diskFile.exists()) {
            filesDiffered = !isDeleted();

            if (filesDiffered && logVerbose) {
                log().verbose("File deleted from: " + toDetailString());
            }

            setDeleted(true);
            // differed when file was removed from disk and flagged
            // asnot-deleted
        }

        // update size
        if (size.longValue() != diskFile.length()) {
            setSize(diskFile.length());
            filesDiffered = true;
        }

        if (diskFile.lastModified() > lastModifiedDate.getTime()) {
            if (logVerbose) {
                log().verbose(
                    "File on disk is newer from: " + this.toDetailString());
            }
            // If file is newer on disk, we have the latest version
            // and update modified info.
            setModifiedInfo(controller.getMySelf().getInfo(), new Date(diskFile
                .lastModified()));
            filesDiffered = true;
        }

        if (filesDiffered) {
            increaseVersion();
            log().warn("File updated to: " + this.toDetailString());
        }

        return filesDiffered;
    }

    /**
     * @param size
     */
    public void setSize(long size) {
        this.size = new Long(size);
    }

    /**
     * Sets the new folder for this file
     * 
     * @param folder
     */
    public void setFolder(Folder folder) {
        if (folder == null) {
            throw new NullPointerException("Folder is null");
        }
        this.folderInfo = folder.getInfo();
    }

    public String getName() {
        return fileName;
    }

    public String getLowerCaseName() {
        if (lowerCaseName == null) {
            String lowCase = fileName.toLowerCase();
            lowerCaseName = new SoftReference<String>(lowCase);
            return lowCase;
        }
        String obj = lowerCaseName.get();
        if (obj == null) {
            String lowCase = fileName.toLowerCase();
            lowerCaseName = new SoftReference<String>(lowCase);
            return lowCase;
        }
        return obj;
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
     * @return
     */
    public String getFilenameOnly() {
        if (fileNameOnly == null) {
            String fileNOnly = getFilenameOnly0();
            fileNameOnly = new SoftReference<String>(fileNOnly);
            return fileNOnly;
        }
        String obj = fileNameOnly.get();
        if (obj == null) {
            String fileNOnly = getFilenameOnly0();
            fileNameOnly = new SoftReference<String>(fileNOnly);
            return fileNOnly;
        }
        return obj;
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
        if (locationInFolder == null) {
            String locInFolder = getLocationInFolder0();
            locationInFolder = new SoftReference<String>(locInFolder);
            return locInFolder;
        }
        String obj = locationInFolder.get();
        if (obj == null) {
            String locInFolder = getLocationInFolder0();
            locationInFolder = new SoftReference<String>(locInFolder);
            return locInFolder;
        }
        return obj;
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
     * @return
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
     * Answers if this file is expeced
     * 
     * @param repo
     * @return
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
     * Answers if this file was modified by a friend
     * 
     * @param controller
     * @return
     */
    public boolean isModifiedByFriend(Controller controller) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        return getModifiedBy() != null && getModifiedBy().isFriend(controller);
    }

    /**
     * Answers if this file is currently downloading
     * 
     * @param tm
     * @return
     */
    public boolean isDownloading(Controller controller) {
        return controller.getTransferManager().isDownloadingActive(this);
    }

    /**
     * Answers if this file is currently uploading
     * 
     * @param tm
     * @return
     */
    public boolean isUploading(Controller controller) {
        return controller.getTransferManager().isUploading(this);
    }

    /**
     * Answers if the diskfile exists
     * 
     * @param controller
     * @return
     */
    public boolean diskFileExists(Controller controller) {
        File diskFile = getDiskFile(controller.getFolderRepository());
        return diskFile != null && diskFile.exists();
    }

    /**
     * @return
     */
    public long getSize() {
        return size.longValue();
    }

    /**
     * @return
     */
    public MemberInfo getModifiedBy() {
        return modifiedBy;
    }

    /**
     * @return
     */
    public Date getModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Increases the version counter of this file by 1
     */
    private void increaseVersion() {
        this.version++;
        if (logVerbose) {
            log().verbose(
                "Increasing file version to " + version + " on "
                    + toDetailString());
        }
    }

    /**
     * Sets the added by info
     * 
     * @param info
     */
    public void setModifiedInfo(MemberInfo by, Date when) {
        modifiedBy = by;
        lastModifiedDate = when;
    }

    /**
     * Answers if this file has been added before the other one
     * 
     * @param other
     */
    public boolean modifiedBefore(FileInfo other) {
        if (lastModifiedDate == null) {
            return false;
        }
        if (other == null || other.getModifiedDate() == null) {
            return true;
        }
        return lastModifiedDate.before(other.getModifiedDate());
    }

    /**
     * Answers if this file is newer on local disk than in folder
     * 
     * @param file
     * @return
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
     * Answers if this file is newer than the other one. By file version
     * 
     * @param ofInfo
     * @return
     */
    public boolean isNewerThan(FileInfo ofInfo) {
        if (ofInfo == null) {
            throw new NullPointerException("Other file is null");
        }
        return (getVersion() > ofInfo.getVersion());
    }

    /**
     * Answers if there is a newer version available of this file
     */
    public boolean isNewerAvailable(FolderRepository repo) {
        return getNewestVersion(repo).isNewerThan(this);
    }

    /**
     * Returns the newest available version of this file
     * 
     * @param repo
     * @return
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
        Member[] members = folder.getConnectedMembers();
        FileInfo newestVersion = this;

        for (Member member : members) {
            if (!member.isConnected()) {
                // disconnected in the meantime
                // Ignore offline user
                continue;
            }
            // Get remote file
            FileInfo remoteFile = member.getFile(this);
            if (remoteFile == null) {
                continue;
            }
            // Check if remote file in newer
            if (remoteFile.isNewerThan(newestVersion)) {
                // log().verbose("Newer version found at " + member);
                newestVersion = remoteFile;
            }
        }
        return newestVersion;
    }

    /**
     * Returns the newest available version of this file, excludes deleted
     * remote files
     * 
     * @param repo
     * @return
     */
    public FileInfo getNewestNotDeletedVersion(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("FolderRepo is null");
        }
        Folder folder = getFolder(repo);
        if (folder == null) {
            throw new IllegalStateException(
                "Unable to determine newest version. Folder not joined "
                    + getFolderInfo());
        }
        Member[] members = folder.getConnectedMembers();
        FileInfo newestVersion = this;
        for (Member member : members) {
            if (!member.isConnected()) {
                // Disconnected in the meantime
                // Ignore offline user
                continue;
            }
            // Get remote file
            FileInfo remoteFile = member.getFile(this);
            if (remoteFile == null) {
                continue;
            }

            if (remoteFile.isDeleted()) {
                continue;
            }

            // Check if remote file is newer
            if (remoteFile.isNewerThan(newestVersion)) {
                // log().verbose("Newer version found at " + member);
                newestVersion = remoteFile;
            }
        }
        return newestVersion;
    }

    /**
     * Resolves a file from local disk by folder repository, File MAY NOT Exist!
     * Returns null if folder was not found
     * 
     * @param repo
     * @return
     */
    public File getDiskFile(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("Repository is null");
        }

        Folder folder = getFolder(repo);
        if (folder == null) {
            return null;
        }
        return folder.getDiskFile(this);
    }

    /**
     * @return
     */
    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * Gets the folder for this file
     * 
     * @param repo
     * @return
     */
    public Folder getFolder(FolderRepository repo) {
        if (repo == null) {
            throw new NullPointerException("Repository is null");
        }
        return repo.getFolder(folderInfo);
    }

    /*
     * General ****************************************************************
     */

    /**
     * Answers if the the two files are completely identical, also checks
     * version, date and modified user
     * 
     * @return
     */
    public boolean completelyIdentical(FileInfo otherFile) {
        if (otherFile == null) {
            return false;
        }

        if (!equals(otherFile)) {
            // not equals, return
            return false;
        }

        return this.getVersion() == otherFile.getVersion()
            && this.getModifiedDate().equals(otherFile.getModifiedDate())
            && this.getModifiedBy().equals(otherFile.getModifiedBy());
    }

    public int hashCode() {
        int hash = fileName.hashCode();
        hash += folderInfo.hashCode();
        return hash;
    }

    public boolean equals(Object other) {
        if (other instanceof FileInfo) {
            FileInfo otherInfo = (FileInfo) other;
            return Util.equals(this.fileName, otherInfo.fileName)
                && Util.equals(this.folderInfo, otherInfo.folderInfo);
        }

        return false;
    }

    public String toString() {
        return "[" + folderInfo.name + "]:/" + fileName;
    }

    public String toDetailString() {
        String modifiedNick;
        if (modifiedBy == null) {
            modifiedNick = "-unknown-";
        } else {
            modifiedNick = modifiedBy.nick;
        }
        return (deleted ? "(del) " : "") + toString() + ", size: " + size
            + " bytes, version: " + getVersion() + ", modified: "
            + lastModifiedDate + " by '" + modifiedNick + "'";
    }

    /**
     * Converts this file into a powerfolder link
     * 
     * @return
     */
    public String toPowerFolderLink() {
        return "PowerFolder://|file|" + Util.endcodeForURL(folderInfo.name)
            + "|" + (folderInfo.secret ? "S" : "P") + "|"
            + Util.endcodeForURL(folderInfo.id) + "|"
            + Util.endcodeForURL(this.fileName);
    }
}