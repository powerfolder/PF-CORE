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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * A access to the system indentified by username & password.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Account extends Model implements Serializable {
    private static final Logger LOG = Logger.getLogger(Account.class);
    private static final long serialVersionUID = 100L;

    private String username;
    private String password;
    private Date registerDate;
    private boolean newsLetter;
    private boolean proUser;

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
        this.permissions = new CopyOnWriteArrayList<Permission>();
        this.osSubscription = new OnlineStorageSubscription();
        this.osSubscription.setType(OnlineStorageSubscriptionType.NONE);
        this.licenseKeyFiles = new CopyOnWriteArrayList<String>();
    }

    // Basic permission stuff *************************************************

    public void grant(Permission... newPermissions) {
        Reject.ifNull(newPermissions, "Permission is null");
        LOG.debug("Granted permission to " + this + ": "
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
        LOG.debug("Revoked permission from " + this + ": "
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

        // TODO Solve this a better way.
        if (permissions.contains(Permissions.ADMIN_PERMISSIONS)) {
            // Admin- everything allowed.
            return true;
        }
        return permissions.contains(permission);
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    // Accessing / API ********************************************************

    /**
     * @return true if this is a valid account
     */
    public boolean isValid() {
        return username != null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    public boolean isNewsLetter() {
        return newsLetter;
    }

    public void setNewsLetter(boolean newsLetter) {
        this.newsLetter = newsLetter;
    }

    public OnlineStorageSubscription getOSSubscription() {
        return osSubscription;
    }

    public boolean isProUser() {
        return proUser;
    }

    public void setProUser(boolean proUser) {
        this.proUser = proUser;
    }

    public FolderInfo getDefaultSynchronizedFolder() {
        return defaultSynchronizedFolder;
    }

    public void setDefaultSynchronizedFolder(
        FolderInfo defaultSynchronizedFolder)
    {
        this.defaultSynchronizedFolder = defaultSynchronizedFolder;
    }

    public Collection<String> getLicenseKeyFiles() {
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
     * @return the mirrored # of folders by this user
     */
    public int countNumberOfFolders(Controller controller) {
        int nFolders = 0;
        for (Permission p : getPermissions()) {
            if (p instanceof FolderAdminPermission) {
                FolderAdminPermission fp = (FolderAdminPermission) p;
                Folder f = fp.getFolder().getFolder(controller);
                if (f == null) {
                    LOG.warn("Got unjoined folder: " + fp.getFolder());
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
        controller.getSecurityManager().saveIdentity(this);

        for (Permission p : getPermissions()) {
            if (!(p instanceof FolderAdminPermission)) {
                continue;
            }
            FolderAdminPermission fp = (FolderAdminPermission) p;
            Folder f = fp.getFolder().getFolder(controller);
            if (f == null) {
                continue;
            }
            f.setSyncProfile(SyncProfile.BACKUP_TARGET);
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

}
