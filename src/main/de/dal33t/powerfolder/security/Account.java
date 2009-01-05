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
package de.dal33t.powerfolder.security;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;

/**
 * A access to the system indentified by username & password.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Account extends Model implements Serializable {

    private static final Logger log = Logger.getLogger(Account.class.getName());
    private static final long serialVersionUID = 100L;

    // Properties
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_USERNAME = "username";
    public static final String PROPERTYNAME_PASSWORD = "password";
    public static final String PROPERTYNAME_PERMISSIONS = "permissions";
    public static final String PROPERTYNAME_REGISTER_DATE = "registerDate";
    public static final String PROPERTYNAME_LAST_LOGIN_DATE = "lastLoginDate";
    public static final String PROPERTYNAME_LAST_LOGIN_FROM = "lastLoginFrom";
    public static final String PROPERTYNAME_NEWSLETTER = "newsLetter";
    public static final String PROPERTYNAME_PRO_USER = "proUser";
    public static final String PROPERTYNAME_SERVER = "server";
    public static final String PROPERTYNAME_DEFAULT_SYNCHRONIZED_FOLDER = "defaultSynchronizedFolder";
    public static final String PROPERTYNAME_OS_SUBSCRIPTION = "OSSubscription";
    public static final String PROPERTYNAME_LICENSE_KEY_FILES = "licenseKeyFiles";

    private String oid;
    private String username;
    private String password;
    private Date registerDate;
    private Date lastLoginDate;
    private MemberInfo lastLoginFrom;
    private boolean newsLetter;
    private boolean proUser;

    /**
     * Server where the folders of this account are hosted on.
     */
    private ServerInfo server;

    /**
     * The possible license key files of this account.
     * <code>AccountService.getValidLicenseKey</code>.
     */
    private Collection<String> licenseKeyFiles;

    /**
     * The default-synced folder of the user. May be null.
     * <p>
     * TRAC #991.
     */
    private FolderInfo defaultSynchronizedFolder;

    private Collection<Permission> permissions;
    private OnlineStorageSubscription osSubscription;

    public Account() {
        // Generate unique id
        this.oid = IdGenerator.makeId();
        this.permissions = new CopyOnWriteArrayList<Permission>();
        this.osSubscription = new OnlineStorageSubscription();
        this.osSubscription.setType(OnlineStorageSubscriptionType.NONE);
        this.licenseKeyFiles = new CopyOnWriteArrayList<String>();
    }

    // Basic permission stuff *************************************************

    public void grant(Permission... newPermissions) {
        Reject.ifNull(newPermissions, "Permission is null");
        log.fine("Granted permission to " + this + ": "
            + Arrays.asList(newPermissions));
        for (Permission p : newPermissions) {
            if (hasPermission(p)) {
                // Skip
                continue;
            }
            permissions.add(p);
        }
    }

    public void revoke(Permission... revokePermissions) {
        Reject.ifNull(revokePermissions, "Permission is null");
        log.fine("Revoked permission from " + this + ": "
            + Arrays.asList(revokePermissions));
        for (Permission p : revokePermissions) {
            permissions.remove(p);
        }
    }

    public void revokeAllPermissions() {
        permissions.clear();
    }

    public boolean hasPermission(Permission permission) {
        Reject.ifNull(permission, "Permission is null");
        if (permissions == null) {
            log.severe("Illegal account " + username + ", permissions is null");
            return false;
        }

        // TODO Solve this a better way.
        if (permissions.contains(AdminPermission.INSTANCE)) {
            // Admin- everything allowed.
            return true;
        }
        return permissions.contains(permission);
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    public boolean isIllegal() {
        return permissions == null || osSubscription == null;
    }

    // Accessing / API ********************************************************

    /**
     * @return true if this is a valid account
     */
    public boolean isValid() {
        return username != null;
    }

    public String getOID() {
        return oid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Object oldValue = getUsername();
        this.username = username;
        firePropertyChange(PROPERTYNAME_USERNAME, oldValue, this.username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        Object oldValue = getPassword();
        this.password = password;
        firePropertyChange(PROPERTYNAME_PASSWORD, oldValue, this.password);
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        Object oldValue = getRegisterDate();
        this.registerDate = registerDate;
        firePropertyChange(PROPERTYNAME_REGISTER_DATE, oldValue,
            this.registerDate);
    }

    public boolean isNewsLetter() {
        return newsLetter;
    }

    public void setNewsLetter(boolean newsLetter) {
        Object oldValue = isNewsLetter();
        this.newsLetter = newsLetter;
        firePropertyChange(PROPERTYNAME_NEWSLETTER, oldValue, this.newsLetter);
    }

    public OnlineStorageSubscription getOSSubscription() {
        if (osSubscription == null) {
            // osSubscription = new OnlineStorageSubscription();
            // osSubscription.setType(OnlineStorageSubscriptionType.TRIAL_5GB);
        }
        return osSubscription;
    }

    public void setOSSubscription(OnlineStorageSubscription osSubscription) {
        Object oldValue = getOSSubscription();
        this.osSubscription = osSubscription;
        firePropertyChange(PROPERTYNAME_OS_SUBSCRIPTION, oldValue,
            this.osSubscription);
    }

    public boolean isProUser() {
        return proUser;
    }

    public void setProUser(boolean proUser) {
        Object oldValue = isProUser();
        this.proUser = proUser;
        firePropertyChange(PROPERTYNAME_PRO_USER, oldValue, this.proUser);
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        Object oldValue = getServer();
        this.server = server;
        firePropertyChange(PROPERTYNAME_SERVER, oldValue, this.server);
    }

    public FolderInfo getDefaultSynchronizedFolder() {
        return defaultSynchronizedFolder;
    }

    public void setDefaultSynchronizedFolder(
        FolderInfo defaultSynchronizedFolder)
    {
        Object oldValue = getDefaultSynchronizedFolder();
        this.defaultSynchronizedFolder = defaultSynchronizedFolder;
        firePropertyChange(PROPERTYNAME_DEFAULT_SYNCHRONIZED_FOLDER, oldValue,
            this.defaultSynchronizedFolder);
    }

    public MemberInfo getLastLoginFrom() {
        return lastLoginFrom;
    }

    public void setLastLoginFrom(MemberInfo lastLoginFrom) {
        Object oldValue = getLastLoginFrom();
        this.lastLoginFrom = lastLoginFrom;
        firePropertyChange(PROPERTYNAME_LAST_LOGIN_FROM, oldValue,
            this.lastLoginFrom);
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    /**
     * Sets the last login date to NOW.
     */
    public void touchLogin() {
        lastLoginDate = new Date();
    }

    public Collection<String> getLicenseKeyFiles() {
        if (licenseKeyFiles == null) {
            // Migrate
            licenseKeyFiles = new CopyOnWriteArrayList<String>();
        }
        return licenseKeyFiles;
    }

    public String toString() {
        return "Account '" + username + "', pro: " + proUser + ", "
            + permissions.size() + " permissions";
    }

    // Convenience/Applogic ***************************************************

    /**
     * @param controller
     * @return the total size used by this user
     */
    public long calulateTotalFoldersSize(Controller controller) {
        long totalSize = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    continue;
                }
                totalSize += f.getStatistic().getSize(controller.getMySelf());
            }
        }
        return totalSize;
    }

    /**
     * @param controller
     * @return the total size of recycle bin
     */
    public long calulateTotalRecycleBinSize(Controller controller) {
        long recycleSize = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    continue;
                }
                for (FileInfo fInfo : controller.getRecycleBin()
                    .getAllRecycledFiles())
                {
                    if (fInfo.getFolderInfo().equals(f.getInfo())) {
                        recycleSize += fInfo.getSize();
                    }
                }
            }
        }
        return recycleSize;
    }

    /**
     * @param controller
     * @return the mirrored # of folders by this user
     */
    public int countNumberOfFolders(Controller controller) {
        int nFolders = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    continue;
                }
                nFolders++;
            }
        }
        return nFolders;
    }

    /**
     * Enables the selected account:
     * <p>
     * The Online Storage subscription
     * <P>
     * Sets all folders to SyncProfile.BACKUP_TARGET.
     * <p>
     * FIXME: Does only set the folders hosted on the CURRENT server to backup.
     * 
     * @param controller
     *            the controller
     */
    public void enable(Controller controller) {
        Reject.ifNull(controller, "Controller is null");

        getOSSubscription().setWarnedUsageDate(null);
        getOSSubscription().setDisabledUsageDate(null);
        getOSSubscription().setWarnedExpirationDate(null);
        getOSSubscription().setDisabledExpirationDate(null);
        controller.getSecurityManager().getDAO().store(this);

        for (Permission p : getPermissions()) {
            if (!(p instanceof FolderAdminPermission)) {
                continue;
            }
            FolderAdminPermission fp = (FolderAdminPermission) p;
            Folder f = fp.getFolder().getFolder(controller);
            if (f == null) {
                continue;
            }
            if (f.getSyncProfile().equals(SyncProfile.DISABLED)) {
                f.setSyncProfile(SyncProfile.BACKUP_TARGET_NO_CHANGE_DETECT);
            }
        }
    }

    // Permission convenience ************************************************

    /**
     * Answers if the user is allowed to read the folder contents.
     * 
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to read the folder contents
     */
    public boolean hasReadPermissions(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(new FolderAdminPermission(foInfo))
            || hasPermission(new FolderReadPermission(foInfo));
    }

    /**
     * Answers if the user is allowed to write into the folder.
     * 
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to write into the folder.
     */
    public boolean hasWritePermissions(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(new FolderAdminPermission(foInfo))
            || hasPermission(new FolderWritePermission(foInfo));
    }

    /**
     * @param foInfo
     * @return true if the user is admin of the folder.
     */
    public boolean hasAdminPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(new FolderAdminPermission(foInfo));
    }

    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (oid == null) {
            // Migration.
            oid = IdGenerator.makeId();
        }
    }
}
