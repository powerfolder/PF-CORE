/* $Id: FileInfo.java,v 1.33 2006/04/09 23:45:03 totmacherr Exp $
 */
package de.dal33t.powerfolder.light;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.zip.Adler32;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsRecordBuilder;

/**
 * File information of a local or remote file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 100L;

    private static final Logger LOG = Logger.getLogger(FileInfo.class);

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
    private transient SoftReference<FileInfoStrings> cachedStrings;

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

        // Check if files match
        if (!diskFile.getName().equals(this.getFilenameOnly())) {
            throw new IllegalArgumentException(
                "Diskfile does not match fileinfo: " + this + ", diskfile: "
                    + diskFile);
        }

        // if (!diskFile.exists()) {
        // log().warn("File does not exsists on disk: " + toDetailString());
        // }

        boolean filesDiffered = !inSyncWithDisk(diskFile);
        if (filesDiffered) {
            increaseVersion();
            setModifiedInfo(controller.getMySelf().getInfo(), new Date(diskFile
                .lastModified()));
            setSize(diskFile.length());
            setDeleted(!diskFile.exists());
            // log().warn("File updated to: " + this.toDetailString());
        }

        return filesDiffered;
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    public boolean inSyncWithDisk(File diskFile) {
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
            cachedStrings = new SoftReference<FileInfoStrings>(stringsRef);

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
     * Increases the version counter of this file by 1
     */
    private void increaseVersion() {
        this.version++;
        // if (logVerbose) {
        // / log().verbose(
        // "Increasing file version to " + version + " on "
        // + toDetailString());
        // }
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
        if (getVersion() == 0 && ofInfo.getVersion() == 0) {
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
        return (getVersion() > ofInfo.getVersion());
    }

    public boolean isSameVersion(FileInfo fInfo) {
        return this.version == fInfo.version;
    }

    /**
     * Also considers myself.
     * 
     * @param repo
     *            the folder repository
     * @return if there is a newer version available of this file
     */
    public boolean isNewerAvailable(FolderRepository repo) {
        return getNewestVersion(repo).isNewerThan(this);
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
        Collection<Member> members = new ArrayList<Member>(Arrays.asList(folder
            .getConnectedMembers()));
        // UARG, ugly access
        members.add(repo.getController().getMySelf());
        FileInfo newestVersion = this;

        for (Member member : members) {
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
     * @return the file.
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
     * General ****************************************************************
     */

    /**
     * @param otherFile
     *            the other file to compare with
     * @return if the the two files are completely identical, also checks
     *         version, date and modified user
     */
    public boolean isCompletelyIdentical(FileInfo otherFile) {
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
        return "[" + folderInfo.name + "]:/" + fileName;
    }

    /**
     * appends to buffer
     * 
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    public final void toDetailString(StringBuilder str) {
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
     * Converts this file into a powerfolder link
     * 
     * @return the converted string.
     */
    public String toPowerFolderLink() {
        return "PowerFolder://|file|" + Util.endcodeForURL(folderInfo.name)
            + "|" + (folderInfo.secret ? "S" : "P") + "|"
            + Util.endcodeForURL(folderInfo.id) + "|"
            + Util.endcodeForURL(this.fileName);
    }

    /**
     * Returns the FilePartsRecord of this file. This method will only yield
     * valid results if the file is locally available AND 100% downloaded.
     * 
     * @param repository
     *            the repository of the file
     * @return the FilePartsRecord
     * @throws FileNotFoundException
     *             if the file couldn't be found
     * @throws IOException
     *             if some read error occured
     */
    public FilePartsRecord getFilePartsRecord(FolderRepository repository,
        PropertyChangeListener l) throws FileNotFoundException, IOException
    {
        // DISABLED because of #644
        FilePartsRecord fileRecord = null;
        if (fileRecord == null) {
            FileInputStream in = null;
            try {
                long start = System.currentTimeMillis();
                FilePartsRecordBuilder b = new FilePartsRecordBuilder(
                    new Adler32(), MessageDigest.getInstance("SHA-256"),
                    MessageDigest.getInstance("MD5"));
                if (l != null) {
                    b.getProcessedBytesCount().addValueChangeListener(l);
                }
                File f = getDiskFile(repository);

                // TODO: Both, the RecordBuilder and the Matcher use "almost"
                // the same algorithms, there should be a shared config.
                // TODO: To select a part size I just took 4Gb as size and
                // wanted the result to be ~512kb.
                // But there should be a more thorough investigation on how to
                // calculate it.
                int partSize = Math.max(4096,
                    (int) (Math.pow(f.length(), 0.25) * 2048));
                in = new FileInputStream(f);
                fileRecord = b.buildFilePartsRecord(in, partSize);
                long took = System.currentTimeMillis() - start;
                LOG.warn("Built file parts for " + this + ". took " + took
                    + "ms");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return fileRecord;
    }

    // Serialization optimization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        fileName = fileName.intern();
    }
}