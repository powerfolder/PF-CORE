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
 * $Id: FileInfo.java 20707 2013-01-28 05:42:50Z glasgow $
 */
package de.dal33t.powerfolder.light;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.NodeInfoProto;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File information of a local or remote file. NEVER USE A CONSTRUCTOR OF THIS
 * CLASS. YOU ARE DOING IT WRONG!. Use {@link FileInfoFactory}
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfo implements Serializable, DiskItem, Cloneable, D2DObject {

    public static final String UNIX_SEPARATOR = "/";
    private static final Logger log = Logger
        .getLogger(FileInfo.class.getName());

    /**
     * #1531: If this system should ignore cases of files in
     * {@link #equals(Object)} and {@link #hashCode()}
     */
    public static final boolean IGNORE_CASE = OSUtil.isWindowsSystem()
        || OSUtil.isMacOS();

    public static final String PROPERTYNAME_FILE_NAME = "fileName";
    public static final String PROPERTYNAME_SIZE = "size";
    public static final String PROPERTYNAME_MODIFIED_BY = "modifiedBy";
    public static final String PROPERTYNAME_LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String PROPERTYNAME_VERSION = "version";
    public static final String PROPERTYNAME_DELETED = "deleted";
    public static final String PROPERTYNAME_FOLDER_INFO = "folderInfo";

    private static final long serialVersionUID = 100L;

    /**
     * Unix-style separated path of the file relative to the folder base dir. So
     * like 'myFile.txt' or 'directory/myFile.txt' or
     * 'directory/subdirectory/myFile.txt'.
     */
    private String fileName;

    // PFC-2352
    private String oid;
    private String hashes;
    private String tags;

    /** The size of the file */
    private Long size;

    /**
     * modified info *
     * <p>
     * Actually 'final'. Only non-final because of serialization readObject()
     * MemberInfo.intern();
     */
    private MemberInfo modifiedBy;
    /**
     * PFC-2571
     */
    private AccountInfo modifiedByAccount;
    /** modified in folder on date */
    private Date lastModifiedDate;

    /** Version number of this file */
    private int version;

    /** the deleted flag */
    private boolean deleted;

    /**
     * the folder.
     * <p>
     * Actually 'final'. Only non-final because of serialization readObject()
     * folderInfo.intern();
     */
    private FolderInfo folderInfo;

    // Caching -----------------------------------

    /**
     * The cached hash info.
     */
    private transient int hash;

    /**
     * Contains some cached string.
     */
    private transient Reference<FileInfoStrings> cachedStrings;

    protected FileInfo() {
        // ONLY for backward compatibility to MP3FileInfo

        fileName = null;
        oid = null;
        hashes = null;
        tags = null;
        size = null;
        modifiedBy = null;
        lastModifiedDate = null;
        version = 0;
        deleted = false;
        folderInfo = null;

        // VERY IMPORANT. MUST BE DONE IN EVERY CONSTRUCTOR
        // this.hash = hashCode0();
    }

    protected FileInfo(String relativeName, String oid, long size,
        MemberInfo modifiedByDevice, AccountInfo modifiedByAccount, Date lastModifiedDate, int version,
        String hashes, boolean deleted, String tags, FolderInfo folderInfo)
    {
        Reject.ifNull(folderInfo, "folder is null!");
        Reject.ifNull(relativeName, "relativeName is null!");
        if (relativeName.contains("/../")) {
            throw new IllegalArgumentException(
                "relativeName must not contain /../: " + relativeName);
        }

        this.fileName = relativeName;
        this.oid = oid;
        this.hashes = hashes;
        this.tags = tags;
        this.size = size;
        this.modifiedBy = modifiedByDevice;
        this.modifiedByAccount = modifiedByAccount;
        this.lastModifiedDate = lastModifiedDate;
        this.version = version;
        this.deleted = deleted;
        this.folderInfo = folderInfo;
        validate();

        // VERY IMPORANT. MUST BE DONE IN EVERY CONSTRUCTOR
        // NOT LONGER NEEDED this.hash = hashCode0();
    }

    protected FileInfo(FolderInfo folder, String relativeName) {
        Reject.ifNull(folder, "folder is null!");
        Reject.ifNull(relativeName, "relativeName is null!");
        if (relativeName.contains("/../")) {
            throw new IllegalArgumentException(
                "relativeName must not contain /../: " + relativeName);
        }

        fileName = relativeName;
        folderInfo = folder;

        oid = null;
        hashes = null;
        tags = null;
        size = null;
        modifiedBy = null;
        lastModifiedDate = null;
        version = 0;
        deleted = false;

        // VERY IMPORANT. MUST BE DONE IN EVERY CONSTRUCTOR
        // this.hash = hashCode0();
    }

    /** FileInfo
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    FileInfo(AbstractMessage mesg)
    {
      initFromD2D(mesg);
    }

    /**
     * Syncs fileinfo with diskfile. If diskfile has other lastmodified date
     * that this. Assume that file has changed on disk and update its modified
     * info.
     *
     * @param folder
     *            the folder to sync with
     * @param diskFile
     *            the diskfile of this file, not gets it from controller !
     * @return the new FileInfo if the file was synced or null if the file is in
     *         sync
     */
    public FileInfo syncFromDiskIfRequired(Folder folder, Path diskFile) {
        Reject.ifNull(folder, "Folder is null");
        Reject.ifFalse(folder.getInfo().equals(folderInfo), "Folder mismatch");
        if (diskFile == null) {
            throw new NullPointerException("diskFile is null");
        }
        String diskFileName = FileInfoFactory.decodeIllegalChars(diskFile
            .getFileName().toString());
        boolean nameMatch = fileName.endsWith(diskFileName);

        if (!nameMatch && IGNORE_CASE) {
            // Try harder if ignore case
            nameMatch = diskFileName.equalsIgnoreCase(getFilenameOnly());
        }

        // Check if files match
        if (!nameMatch) {
            throw new IllegalArgumentException(
                "Diskfile does not match fileinfo name '" + getFilenameOnly()
                    + "', details: " + toDetailString() + ", diskfile name '"
                    + diskFile.getFileName().toString() + "', path: "
                    + diskFile);
        }

        // if (!diskFile.exists()) {
        // log.warning("File does not exsists on disk: " + toDetailString());
        // }

        if (!inSyncWithDisk(diskFile)) {
            MemberInfo mySelf = folder.getController().getMySelf().getInfo();
            AccountInfo myAccount = folder.getController().getMySelf()
                .getAccountInfo();
            if (Files.exists(diskFile)) {
                // PFC-2352: TODO: Calc new hashes
                String newHashes = null;
                return FileInfoFactory.modifiedFile(this, folder, diskFile,
                    mySelf, myAccount, newHashes);
            } else {
                return FileInfoFactory.deletedFile(this, mySelf, myAccount,
                    new Date());
            }
        }

        return null;
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    public boolean inSyncWithDisk(Path diskFile) {
        return inSyncWithDisk0(diskFile, false);
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @param ignoreSizeAndModDate
     *            ignore the reported size of the diskfile/dir.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    protected boolean inSyncWithDisk0(Path diskFile,
        boolean ignoreSizeAndModDate)
    {
        Reject.ifNull(diskFile, "Diskfile is null");

        // PFC-2849:
        boolean diskFileDeleted;
        Map<String, Object> attrs;
        try {
            attrs = Files.readAttributes(diskFile,
                "size,lastModifiedTime,isDirectory");
            diskFileDeleted = false;
        } catch (FileNotFoundException | NoSuchFileException e) {
            diskFileDeleted = true;
            attrs = null;
        } catch (IOException e) {
            diskFileDeleted = Files.notExists(diskFile);
            if (!diskFileDeleted) {
                log.warning("Could not access file attributes of file "
                    + diskFile.toAbsolutePath().toString() + "\n"
                    + toDetailString() + "\n" + e.toString());
                return false;
            }
            attrs = null;
        }

        boolean existanceSync = diskFileDeleted && deleted || !diskFileDeleted
            && !deleted;

        if (!existanceSync) {
            return false;
        }

        boolean diskIsDirectory;
        long diskLastMod;
        long diskSize;

        if (!diskFileDeleted) {
            try {
                diskSize = ((Long) attrs.get("size")).longValue();
                diskLastMod = ((FileTime) attrs.get("lastModifiedTime"))
                    .toMillis();
                diskIsDirectory = ((Boolean) attrs.get("isDirectory"))
                    .booleanValue();
            } catch (Exception e) {
                log.warning("Could not access file attributes of file "
                    + diskFile.toAbsolutePath().toString() + "\n"
                    + toDetailString() + "\n" + e.toString());
                return false;
            }

            if (ignoreSizeAndModDate) {
                boolean dirFileSync = diskFileDeleted
                    || (isDiretory() && diskIsDirectory);
                return existanceSync && dirFileSync;
            }
            boolean lastModificationSync = DateUtil
                .equalsFileDateCrossPlattform(diskLastMod,
                    lastModifiedDate.getTime());
            if (!lastModificationSync) {
                return false;
            }

            boolean sizeSync = size == diskSize;
            if (!sizeSync) {
                return false;
            }
        }
        return true;
        // return existanceSync && lastModificationSync && sizeSync;
    }

    /**
     * @return the name , relative to the folder base.
     */
    @Override
    public String getRelativeName() {
        return fileName;
    }

    /**
     * @return The filename (including the path from the base of the folder)
     *         converted to lowercase
     */
    @Override
    public String getLowerCaseFilenameOnly() {
        // if (Feature.CACHE_FILEINFO_STRINGS.isDisabled()) {
        // return getFilenameOnly0().toLowerCase();
        // }
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
    @Override
    public String getExtension() {
        String tmpFileName = getFilenameOnly();
        int index = tmpFileName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return tmpFileName.substring(index + 1, tmpFileName.length())
            .toUpperCase();
    }

    /**
     * Gets the filename only, without the directory structure
     *
     * @return the filename only of this file.
     */
    @Override
    public String getFilenameOnly() {
        // if (Feature.CACHE_FILEINFO_STRINGS.isDisabled()) {
        // return getFilenameOnly0();
        // }
        FileInfoStrings strings = getStringsCache();
        if (strings.getFileNameOnly() == null) {
            strings.setFileNameOnly(getFilenameOnly0());
        }
        return strings.getFileNameOnly();
    }

    private String getFilenameOnly0() {
        int index = fileName.lastIndexOf('/');
        if (index > -1) {
            return fileName.substring(index + 1);
        } else {
            return fileName;
        }
    }

    public String getOID() {
        return oid;
    }

    public String getHashes() {
        return hashes;
    }

    public String getTags() {
        return tags;
    }

    /**
     * @return if this file was deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param repo
     * @return if this file is expeced
     */
    public boolean isExpected(FolderRepository repo) {
        if (deleted) {
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
        Path diskFile = getDiskFile(controller.getFolderRepository());
        return diskFile != null && Files.exists(diskFile);
    }

    /**
     * @return the size of the file.
     */
    @Override
    public long getSize() {
        return size;
    }

    /**
     * @return the device this file was lasted changed on.
     */
    @Override
    public MemberInfo getModifiedBy() {
        return modifiedBy;
    }

    /**
     * @return the account info this file was lasted changed on.
     */
    public AccountInfo getModifiedByAccount() {
        return modifiedByAccount;
    }

    /**
     * @return the modification date.
     */
    @Override
    public Date getModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @return the version of the file.
     */
    public int getVersion() {
        return version;
    }

    public boolean isLookupInstance() {
        return size == null;
    }

    @Override
    public boolean isDiretory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    public boolean isBaseDirectory() {
        return StringUtils.isBlank(fileName);
    }

    /**
     * @return a lookup instance of the subdirectory this {@link FileInfo} is
     *         located in.
     */
    public DirectoryInfo getDirectory() {
        int i = fileName.lastIndexOf('/');
        if (i < 0) {
            return FileInfoFactory.createBaseDirectoryInfo(folderInfo);
        }
        String dirName = fileName.substring(0, i);
        return FileInfoFactory.lookupDirectory(folderInfo, dirName);
    }

    // PFC-1962: Deligating methods.

    public boolean lock(Controller controller) {
        return controller.getFolderRepository().getLocking().lock(this);
    }

    public boolean lock(Controller controller, AccountInfo by) {
        return controller.getFolderRepository().getLocking().lock(this, by);
    }

    public boolean unlock(Controller controller) {
        return controller.getFolderRepository().getLocking().unlock(this);
    }

    public boolean isLocked(Controller controller) {
        return controller.getFolderRepository().getLocking().isLocked(this);
    }

    /**
     * @param controller
     * @return The lock for this FileInfo or {@code null} if there is no lock OR the lock file could not be read
     */
    public Lock getLock(Controller controller) {
        return controller.getFolderRepository().getLocking().getLock(this);
    }

    // PFC-1962: End

    /**
     * @param ofInfo
     *            the other fileinfo.
     * @return if this file is newer than the other one. By file version, or
     *         file modification date if version of both =0
     */
    public boolean isNewerThan(FileInfo ofInfo) {
        return isNewerThan(ofInfo, false);
    }

    protected boolean isNewerThan(FileInfo ofInfo, boolean ignoreLastModified) {
        if (ofInfo == null) {
            throw new NullPointerException("Other file is null");
        }
        // if (Feature.DETECT_UPDATE_BY_VERSION.isDisabled()) {
        // // Directly detected by last modified
        // return DateUtil.isNewerFileDateCrossPlattform(lastModifiedDate,
        // ofInfo.lastModifiedDate);
        // }
        if (version == ofInfo.version) {
            if (ignoreLastModified) {
                return false;
            }
            return DateUtil.isNewerFileDateCrossPlattform(lastModifiedDate,
                ofInfo.lastModifiedDate);
        }
        return version > ofInfo.version;
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
            if (log.isLoggable(Level.FINER)) {
                log.finer("Unable to determine newest version. Folder not joined "
                    + folderInfo);
            }
            return null;
        }
        FileInfo newestVersion = null;
        for (Member member : folder.getMembersAsCollection()) {
            FileInfo remoteFile = member.getFile(this);
            if (remoteFile == null) {
                continue;
            }
            if (!remoteFile.isValid()) {
                continue;
            }
            // Check if remote file in newer
            if (newestVersion == null || remoteFile.isNewerThan(newestVersion))
            {
                // HACK(tm)
                if (!ServerClient.SERVER_HANDLE_MESSAGE_THREAD.get()
                    && !folder.hasWritePermission(member))
                {
                    continue;
                }
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
            log.warning("Unable to determine newest version. Folder not joined "
                + folderInfo);
            return null;
        }
        FileInfo newestVersion = null;
        for (Member member : folder.getMembersAsCollection()) {
            if (member.isCompletelyConnected() || member.isMySelf()) {
                // Get remote file
                FileInfo remoteFile = member.getFile(this);
                if (remoteFile == null || remoteFile.deleted) {
                    continue;
                }
                // Check if remote file is newer
                if (newestVersion == null
                    || remoteFile.isNewerThan(newestVersion))
                {
                    // HACK(tm)
                    if (!ServerClient.SERVER_HANDLE_MESSAGE_THREAD.get()
                        && !folder.hasWritePermission(member))
                    {
                        continue;
                    }
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
    public Path getDiskFile(FolderRepository repo) {
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
    @Override
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
    public boolean isVersionDateAndSizeIdentical(FileInfo otherFile) {
        if (otherFile == null) {
            return false;
        }
        if (version != otherFile.version) {
            // This is quick do it first
            return false;
        }
        if (!Util.equals(size, otherFile.size)) {
            return false;
        }
        if (!equals(otherFile)) {
            // not equals, return
            return false;
        }
        if (lastModifiedDate != null && otherFile.lastModifiedDate != null
            && !lastModifiedDate.equals(otherFile.lastModifiedDate))
        {
            return false;
        }
        // All match!
        return true;
    }

    /**
     * PFC-2352.
     *
     * @param hash
     * @return true if the hash matches any of the file hashes.
     */
    public boolean isMatchingHash(String hash) {
        Reject.ifBlank(hash, "Hash");
        if (StringUtils.isBlank(hashes)) {
            return false;
        }
        return hashes.contains(hash);
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            // Cache the hashcode
            hash = hashCode0();
        }
        return hash;
    }

    private int hashCode0() {
        int hash = IGNORE_CASE ? fileName.toLowerCase().hashCode() : fileName
            .hashCode();
        hash += folderInfo.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FileInfo) {
            FileInfo otherInfo = (FileInfo) other;
            boolean caseMatch = Util.equalsRelativeName(fileName,
                otherInfo.fileName);
            return caseMatch && Util.equals(folderInfo, otherInfo.folderInfo);
        }

        return false;
    }

    @Override
    public String toString() {
        return '[' + folderInfo.getName() + "]:" + (deleted ? "(del) /" : "/")
            + fileName;
    }

    /**
     * appends to buffer
     *
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    private void toDetailString(StringBuilder str) {
        str.append(toString());
        str.append(", size: ");
        str.append(size);
        str.append(" bytes, version: ");
        str.append(version);
        str.append(", modified: ");
        str.append(lastModifiedDate);
        str.append(" (");
        if (lastModifiedDate != null) {
            str.append(lastModifiedDate.getTime());
        } else {
            str.append("-n/a-");
        }
        str.append(") by '");
        if (modifiedByAccount == null) {
            if (modifiedBy != null) {
                str.append(modifiedBy.nick);
            }
        } else {
            str.append(modifiedByAccount.getUsername());
        }
        str.append('\'');
        if (modifiedBy != null) {
            str.append(" on " + modifiedBy.nick);
        } else {
        }
    }

    public String toDetailString() {
        StringBuilder str = new StringBuilder();
        toDetailString(str);
        return str.toString();
    }

    /**
     * @return true if this instance is valid. false if is broken,e.g. Negative
     *         Time
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (Exception e) {
            log.log(Level.WARNING, "Invalid: " + toDetailString() + ". " + e);
            return false;
        }
    }

    /**
     * Validates the state of the FileInfo. This should actually not be public -
     * checks should be made while constructing this class (by
     * constructor/deserialization).
     *
     * @throws IllegalArgumentException
     *             if the state is corrupt
     */
    private void validate() {
        Reject.ifNull(lastModifiedDate, "Modification date is null");
        if (lastModifiedDate.getTime() < 0) {
            throw new IllegalStateException("Modification date is invalid: "
                + lastModifiedDate);
        }
        Reject.ifTrue(StringUtils.isEmpty(fileName), "Filename is empty");
        char lastChar = fileName.charAt(fileName.length() - 1);
        if (lastChar == '/' || lastChar == '\\') {
            throw new IllegalStateException("Filename ends with slash: "
                + fileName);
        }

        //Reject.ifNull(size, "Size is null");
        //Reject.ifFalse(size < 0, "Negative file size");
        Reject.ifNull(folderInfo, "FolderInfo is null");
    }

    // Serialization optimization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();

        // #2037: Removed internalization
        // fileName = fileName.intern();

        // Oh! Default value. Better recalculate hashcode cache
        // if (hash == 0) {
        // hash = hashCode0();
        // }

        folderInfo = folderInfo != null ? folderInfo.intern() : null;
        modifiedBy = modifiedBy != null ? modifiedBy.intern() : null;
        // PFC-2571
        modifiedByAccount = modifiedByAccount != null ? modifiedByAccount.intern() : null;

        // #2159: Remove / in front and end of filename
        if (fileName.endsWith("/")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        // validate();
    }

    private static final long extVersion100UID = 100L;
    private static final long extVersionCurrentUID = 101L;

    void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersion100UID && extUID != extVersionCurrentUID) {
            throw new InvalidClassException(getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", supported: " + extVersion100UID + ", "
                    + extVersionCurrentUID);
        }
        fileName = in.readUTF();
        size = in.readLong();
        if (in.readBoolean()) {
            modifiedBy = MemberInfo.readExt(in);
            modifiedBy = modifiedBy != null ? modifiedBy.intern() : null;
        } else {
            modifiedBy = null;
        }
        lastModifiedDate = ExternalizableUtil.readDate(in);
        version = in.readInt();
        deleted = in.readBoolean();
        folderInfo = ExternalizableUtil.readFolderInfo(in);
        folderInfo = folderInfo != null ? folderInfo.intern() : null;

        if (extUID == extVersion100UID) {
            return;
        }
        // PFC-2352: Start
        if (in.readBoolean()) {
            oid = in.readUTF();
        } else {
            oid = null;
        }
        if (in.readBoolean()) {
            hashes = in.readUTF();
        } else {
            hashes = null;
        }
        if (in.readBoolean()) {
            tags = in.readUTF();
        } else {
            tags = null;
        }
        // PFC-2352: End
        // PFC-2571: Start
        if (in.readBoolean()) {
            modifiedByAccount = AccountInfo.readExt(in);
            modifiedByAccount = modifiedByAccount != null ? modifiedByAccount
                .intern() : null;
        }
        // PFC-2571: End
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        long extUID;
        if (oid == null && hashes == null && tags == null) {
            extUID = extVersion100UID;
        } else {
            extUID = extVersionCurrentUID;
        }
        out.writeInt(isFile() ? 0 : 1);
        out.writeLong(extUID);
        out.writeUTF(fileName);
        out.writeLong(size);
        out.writeBoolean(modifiedBy != null);
        if (modifiedBy != null) {
            modifiedBy.writeExternal(out);
        }
        ExternalizableUtil.writeDate(out, lastModifiedDate);
        out.writeInt(version);
        out.writeBoolean(deleted);
        ExternalizableUtil.writeFolderInfo(out, folderInfo);

        if (extUID == extVersion100UID) {
            return;
        }

        // PFC-2352: Start
        if (oid != null) {
            out.writeBoolean(true);
            out.writeUTF(oid);
        } else {
            out.writeBoolean(false);
        }
        if (hashes != null) {
            out.writeBoolean(true);
            out.writeUTF(hashes);
        } else {
            out.writeBoolean(false);
        }
        if (tags != null) {
            out.writeBoolean(true);
            out.writeUTF(tags);
        } else {
            out.writeBoolean(false);
        }
        // PFC-2352: End
        // PFC-2571: Start
        if (modifiedByAccount != null) {
            out.writeBoolean(true);
            modifiedByAccount.writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
        // PFC-2571: End
    }

    /**
     * Utility method for changing the fileName part of a relative file path.
     * Example renameRelativeFileName('directory/subdirectory/myFile.txt',
     * 'newFile.txt') ==> 'directory/subdirectory/newFile.txt' NOTE: This is
     * static, so does not affect a FileInfo.
     *
     * @param relativeName
     * @param newFileName
     * @return
     */
    public static String renameRelativeFileName(String relativeName,
        String newFileName)
    {
        if (newFileName.contains(UNIX_SEPARATOR)) {
            throw new IllegalArgumentException("newFileName must not contain "
                + UNIX_SEPARATOR + ": " + relativeName);
        }
        if (relativeName.contains(UNIX_SEPARATOR)) {
            String directoryPart = relativeName.substring(0,
                relativeName.lastIndexOf(UNIX_SEPARATOR));
            return directoryPart + UNIX_SEPARATOR + newFileName;
        } else {
            // No path - just use the relative filename.
            return newFileName;
        }
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FileInfoProto.FileInfo)
        {
          FileInfoProto.FileInfo finfo = (FileInfoProto.FileInfo)mesg;

          this.fileName          = finfo.getFileName();
          this.oid               = finfo.getOid();
          this.hashes            = finfo.getFileHashes();
          this.tags              = finfo.getTags();
          this.size              = finfo.getSize();
          this.modifiedBy        = new MemberInfo(finfo.getModifiedByNodeInfo());
          this.modifiedByAccount = new AccountInfo(finfo.getModifiedByAccountInfo());
          this.lastModifiedDate  = new Date(finfo.getLastModifiedDate());
          this.version           = finfo.getVersion();
          this.deleted           = finfo.getDeleted();
          this.folderInfo        = new FolderInfo(finfo.getFolderInfo());

          validate();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FileInfoProto.FileInfo.Builder builder = FileInfoProto.FileInfo.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      if (this.fileName != null) builder.setFileName(this.fileName);
      if (this.oid != null) builder.setOid(this.oid);
      if (this.hashes != null) builder.setFileHashes(this.hashes);
      if (this.tags != null) builder.setTags(this.tags);
      if (this.size != null) builder.setSize(this.size);

      if (this.modifiedBy != null) builder.setModifiedByNodeInfo(
        (NodeInfoProto.NodeInfo)this.modifiedBy.toD2D());
      if (this.modifiedByAccount != null) builder.setModifiedByAccountInfo(
        (AccountInfoProto.AccountInfo)this.modifiedByAccount.toD2D());

      if (this.lastModifiedDate != null) builder.setLastModifiedDate(this.lastModifiedDate.getTime());
      builder.setVersion(this.version);
      builder.setDeleted(this.deleted);

      if (this.folderInfo != null) builder.setFolderInfo(
        (FolderInfoProto.FolderInfo)this.folderInfo.toD2D());

      return builder.build();
    }
}
