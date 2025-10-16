/*
 * Copyright 2004 - 2022 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.domain;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.message.clientserver.ShareLinkInfo;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.FolderReadPermission;
import de.dal33t.powerfolder.security.FolderReadWritePermission;
import de.dal33t.powerfolder.util.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Date;

/**
 * PFS-623: Public file/directory links: Option to get an obfuscated link to
 * download a file with expiry date
 * <p>
 * Domain object.
 *
 * @author Sprajc
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FileLink implements Serializable {

    private static final long serialVersionUID = 100L;

    // Properties
    public static final String FILE_LINK_PREFIX = IdGenerator.FILE_LINK_PREFIX;
    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_RELATIVE_NAME = "relativeName";
    public static final String PROPERTYNAME_CREATION_DATE = "creationDate";
    public static final String PROPERTYNAME_FOLDER_INFO = "folderInfo";

    @Id
    private String id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "folderInfo_id")
    private FolderInfo folderInfo;
    @Index(name = "IDX_FIL_REL_NAME")
    @Column(length = 1024)
    private String relativeName;
    @Deprecated
    private String accountUsername;
    @Embedded
    private AccountInfo creator;
    @Deprecated
    private boolean publicAccess;
    @Type(type = "permissionType")
    private FolderPermission publicPermission;
    @Column(nullable=true)
    private Date expirationDate;
    private Date creationDate;
    private int downloads;
    private int maxDownloads;
    private String password;

    protected FileLink() {
        // NOP Hibernate only.
    }

    public FileLink(FileInfo fInfo, boolean generateRandomID) {
        super();
        Reject.ifNull(fInfo, "FileInfo");
        Reject.ifNull(fInfo.getFolderInfo(), "FolderInfo");
        this.folderInfo = fInfo.getFolderInfo();
        this.relativeName = fInfo.getRelativeName();
        this.maxDownloads = -1;
        if (generateRandomID) {
            this.id = IdGenerator.generateFileLinkRandomID();
        } else {
            this.id = IdGenerator.generateFileLinkID(fInfo);
        }
        this.creationDate = new Date();
    }

    public FileLink(ShareLinkInfo shareLinkInfo) {
        super();
        this.downloads = shareLinkInfo.getDownloads();
        this.expirationDate = shareLinkInfo.getExpirationDate();
        this.relativeName = shareLinkInfo.getFileRelativePath();
        this.folderInfo = FolderInfoFactory.lookupInstance(shareLinkInfo.getFolderId());
        this.id = shareLinkInfo.getId();
        this.maxDownloads = shareLinkInfo.getMaxDownloads();
        this.password = shareLinkInfo.getPassword();
        if (shareLinkInfo.isUploadEnabled()) {
            this.publicPermission = FolderPermission.readWrite(this.folderInfo);
        } else {
            this.publicPermission = FolderPermission.read(this.folderInfo);
        }
        this.creationDate = new Date();
    }

    // Immutable fields *******************************************************

    public String getId() {
        return id;
    }

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    public String getRelativeName() {
        return relativeName;
    }

    public String getFilenameOnly() {
        int index = relativeName.lastIndexOf('/');
        if (index > -1) {
            return relativeName.substring(index + 1);
        } else {
            return relativeName;
        }
    }

    /**
     * @return everything after the last point (.) in the fileName in upper case
     */
    public String getExtension() {
        String tmpFileName = getFilenameOnly();
        int index = tmpFileName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return tmpFileName.substring(index + 1).toUpperCase();
    }

    public String getLinkURI() {
        return Constants.GET_LINK_URI + "/" + id + "/" + Util.encodeForURL(getFilenameOnly());
    }

    public String getDownloadURI() {
        return Constants.DL_LINK_URI + "/" + id + "/" + Util.encodeForURL(getFilenameOnly());
    }

    // Helpers ****************************************************************

    public FileInfo getFileInfo(Controller controller) {
        FileInfo fInfo = FileInfoFactory.lookupDirectory(folderInfo,
            relativeName);
        Folder folder = folderInfo.getFolder(controller);
        if (folder != null) {
            FileInfo fLocalInfo = folder.getFile(fInfo);
            if (fLocalInfo != null) {
                fInfo = fLocalInfo;
            }
        }
        return fInfo;
    }

    public String getDisplayName() {
        if (StringUtils.isBlank(relativeName)) {
            return folderInfo.getName();
        }
        int index = relativeName.lastIndexOf('/');
        if (index > -1) {
            return relativeName.substring(index + 1);
        } else {
            return relativeName;
        }
    }

    public Path getDiskFile(Controller controllor) {
        Reject.ifNull(controllor, "Controller");
        FileInfo fInfo = FileInfoFactory.lookupDirectory(folderInfo,
            relativeName);
        return fInfo.getDiskFile(controllor.getFolderRepository());
    }

    // Accessing modalities ***************************************************

    /**
     * Checks if this FileLink grants read access to the given file.
     * Validates expiration, optional password, and path correctness.
     */
    public boolean hasReadPermissions(Controller controller, FileInfo fileInfo, String optionalPassword) {
        return checkAccessPermissions(controller, fileInfo, optionalPassword, false);
    }

    /**
     * Checks if this FileLink grants write access (e.g., upload) to the given file.
     * Validates expiration, optional password, and path correctness.
     */
    public boolean hasWritePermissions(Controller controller, FileInfo fileInfo, String optionalPassword) {
        return checkAccessPermissions(controller, fileInfo, optionalPassword, true);
    }

    /**
     * Internal helper for access control. Used by both read and write checks.
     *
     * @param controller Controller instance
     * @param fileInfo File to check access for
     * @param optionalPassword Optional password, may be null
     * @param write True if checking write access, false for read access
     * @return true if access is allowed
     */
    private boolean checkAccessPermissions(Controller controller, FileInfo fileInfo,
                                           String optionalPassword, boolean write) {
        Reject.ifNull(controller, "Controller");
        Reject.ifNull(fileInfo, "FileInfo");

        // Expired link
        if (isExpired()) {
            return false;
        }

        // Password check
        if (isPasswordProtected() && !isCorrectPassword(optionalPassword)) {
            return false;
        }

        // Folder must match
        if (!folderInfo.equals(fileInfo.getFolderInfo())) {
            return false;
        }

        // Determine if this link points to a file or directory
        FileInfo linkedFileInfo = getFileInfo(controller);
        if (linkedFileInfo == null) {
            return false;
        }

        // For file links, only the exact same file may be accessed
        if (!linkedFileInfo.isDiretory()) {
            if (!fileInfo.getRelativeName().equals(relativeName)) {
                return false;
            }
        } else {
            // Directory link: path must start with linked path
            if (!fileInfo.getRelativeName().startsWith(relativeName)) {
                return false;
            }
        }

        // Permission check
        if (write) {
            // Write requires full read/write permission
            return publicPermission instanceof FolderReadWritePermission;
        } else {
            // Read requires at least read or read/write
            return publicPermission instanceof FolderReadPermission
                    || publicPermission instanceof FolderReadWritePermission;
        }
    }

    public FolderPermission getPublicPermission(){
        return publicPermission;
    }

    /**
     * PFS-869: Permission handling when FileLinks allow public uploads.
     * <p>
     * 1. If publicFolderPermission is read/write -> FileLink uploads are allowed.
     * 2. If publicFolderPermission is read only -> Default FileLink behaviour without uploads.
     * 3. If publicFolderPermission is null -> This is the case if e.g. a FileLink requires
     * a password (set by PasswordPolicy.REQUIRED).
     * <p>
     * 4. publicAccessAllowed is for checking if ConfigurationServerEntry.WEB_PUBLIC_ACCESS_ALLOWED
     * is set on this server. This is a bit ugly but necessary because we have no controller in this class.
     */
    public void setPublicPermission(FolderPermission publicFolderPermission, boolean publicAccessAllowed){

        if (!publicAccessAllowed || publicFolderPermission == null){
            this.publicPermission = null;
        } else {
            Reject.ifFalse(publicFolderPermission instanceof FolderReadPermission
                    || publicFolderPermission instanceof FolderReadWritePermission, "Permission");
            this.publicPermission = publicFolderPermission;
        }
    }

    public boolean hasPublicPermission(FolderPermission folderPermission) {
        if (publicPermission == null) {
            return folderPermission == null;
        }
        return publicPermission.equals(folderPermission) || publicPermission.implies(folderPermission);
    }

    public int getDownloads() {
        return downloads;
    }

    public void increaseDownloads() {
        this.downloads++;
    }

    public int getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(int maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public boolean isDownloadLimitHit() {
        return maxDownloads >= 0 && downloads >= maxDownloads;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isExpired() {
        if (expirationDate == null) {
            return false;
        }
        return expirationDate.getTime() <= System.currentTimeMillis();
    }

    public Date getCreationDate() {
    	return creationDate;
    }

    public String getAccountUsername() {
        if (creator == null) {
            return accountUsername;
        } else {
            return creator.getUsername();
        }
    }

    public AccountInfo getCreator() {
        return creator;
    }

    public void setCreator(AccountInfo creator) {
        this.creator = creator;
    }

    public void setPassword(String password) {
        if (StringUtils.isBlank(password)) {
            this.password = null;
            return;
        }
        this.password = LoginUtil.hashAndSalt(password);
    }

    public boolean isPasswordProtected() {
        return StringUtils.isNotBlank(password);
    }

    public boolean isCorrectPassword(String password) {
        return LoginUtil.matches(Util.toCharArray(password), this.password);
    }

    @Override
    public String toString() {
        return "FileLink [id=" + id + ", folderInfo=" + folderInfo
            + ", relativeName=" + relativeName + ", publicPermission="
            + publicPermission + ", expirationDate=" + expirationDate
            + ", downloads=" + downloads + ", maxDownloads=" + maxDownloads
            + ", password=" + (password == null ? "no" : "yes") + "]";
    }


    /**
     * Converts a FileLink to a ShareLinkInfo
     * (FileLink cannot be moved to PF-PRO, so a ShareLinkInfo class is needed for protocol)
     *
     * @param serverInfo The serverinfo
     * @return The corresponding ShareLinkInfo object
     */
    public ShareLinkInfo toShareLinkInfo(ServerInfo serverInfo) {
        ShareLinkInfo shareLinkInfo = new ShareLinkInfo();
        shareLinkInfo.setDownloads(this.downloads);
        shareLinkInfo.setExpirationDate(this.expirationDate);
        shareLinkInfo.setFileRelativePath(this.relativeName);
        if (this.folderInfo != null) shareLinkInfo.setFolderId(this.folderInfo.getId());
        shareLinkInfo.setId(this.id);
        shareLinkInfo.setMaxDownloads(this.maxDownloads);
        shareLinkInfo.setPassword(this.password);
        if (this.publicPermission instanceof FolderReadWritePermission) {
            shareLinkInfo.setUploadEnabled(true);
        } else {
            shareLinkInfo.setUploadEnabled(false);
        }
        shareLinkInfo.setUrl(serverInfo.getURL(this.getLinkURI()));
        return shareLinkInfo;
    }

}
