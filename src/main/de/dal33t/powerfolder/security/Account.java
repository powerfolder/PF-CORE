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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.db.PermissionUserType;

/**
 * A access to the system indentified by username & password.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
@TypeDef(name = "permissionType", typeClass = PermissionUserType.class)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Account implements Serializable {

    private static final Logger LOG = Logger.getLogger(Account.class.getName());
    private static final long serialVersionUID = 100L;

    // Properties
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_USERNAME = "username";
    public static final String PROPERTYNAME_PASSWORD = "password";
    public static final String PROPERTYNAME_LDAPDN = "ldapDN";
    public static final String PROPERTYNAME_LANGUAGE = "language";
    public static final String PROPERTYNAME_PERMISSIONS = "permissions";
    public static final String PROPERTYNAME_REGISTER_DATE = "registerDate";
    public static final String PROPERTYNAME_LAST_LOGIN_DATE = "lastLoginDate";
    public static final String PROPERTYNAME_LAST_LOGIN_FROM = "lastLoginFrom";
    public static final String PROPERTYNAME_NEWSLETTER = "newsLetter";
    public static final String PROPERTYNAME_PRO_USER = "proUser";
    public static final String PROPERTYNAME_NOTES = "notes";
    public static final String PROPERTYNAME_SERVER = "server";
    public static final String PROPERTYNAME_DEFAULT_SYNCHRONIZED_FOLDER = "defaultSynchronizedFolder";
    public static final String PROPERTYNAME_OS_SUBSCRIPTION = "osSubscription";
    public static final String PROPERTYNAME_LICENSE_KEY_FILES = "licenseKeyFiles";
    public static final String PROPERTYNAME_COMPUTERS = "computers";
    public static final String PROPERTYNAME_GROUPS = "groups";

    @Id
    private String oid;
    @Index(name = "IDX_USERNAME")
    @Column(nullable = false, unique = true)
    private String username;
    private String password;
    private String language;
    @Index(name = "IDX_LDAPDN")
    @Column(length = 512)
    private String ldapDN;
    private Date registerDate;
    private Date lastLoginDate;
    @ManyToOne
    @JoinColumn(name = "lastLoginFrom_id")
    private MemberInfo lastLoginFrom;
    private boolean proUser;

    // PFS-605
    private String custom1;
    private String custom2;
    private String custom3;

    @Column(length = 1024)
    private String notes;

    /**
     * The list of computers associated with this account.
     */
    @ManyToMany
    @JoinTable(name = "Account_Computers", joinColumns = @JoinColumn(name = "oid"), inverseJoinColumns = @JoinColumn(name = "id"))
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<MemberInfo> computers;

    /**
     * Server where the folders of this account are hosted on.
     */
    @ManyToOne
    @JoinColumn(name = "serverInfo_id")
    private ServerInfo server;

    @Deprecated
    @Transient
    private Collection<String> licenseKeyFiles;

    /**
     * The possible license key files of this account.
     * <code>AccountService.getValidLicenseKey</code>.
     */
    @CollectionOfElements
    @IndexColumn(name = "IDX_LICENSE", base = 0, nullable = false)
    @Cascade(value = {CascadeType.ALL})
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> licenseKeyFileList;

    /**
     * The maximum number of devices to automatically re-license if required
     */
    private int autoRenewDevices;

    /**
     * Don't auto re-license after that date.
     */
    private Date autoRenewTill;

    /**
     * The default-synced folder of the user. May be null.
     * <p>
     * TRAC #991.
     */
    @ManyToOne
    @JoinColumn(name = "defaultSyncFolder_id")
    private FolderInfo defaultSynchronizedFolder;

    @CollectionOfElements
    @Type(type = "permissionType")
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Permission> permissions;

    @ManyToMany
    @JoinTable(name = "Account_Groups", joinColumns = @JoinColumn(name = "Account_oid"), inverseJoinColumns = @JoinColumn(name = "AGroup_oid"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Collection<Group> groups;

    /**
     * The possible email address of this account.
     */
    @CollectionOfElements
    @IndexColumn(name = "IDX_EMAIL", base = 0, nullable = false)
    @Cascade(value = {CascadeType.ALL})
    @BatchSize(size = 1337)
    @Column(name = "element", length = 512)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> emails;

    @Embedded
    @Fetch(FetchMode.JOIN)
    private OnlineStorageSubscription osSubscription;

    Account() {
        // Generate unique id
        this(IdGenerator.makeId());
    }

    Account(String oid) {
        Reject.ifBlank(oid, "OID");
        this.oid = oid;
        this.permissions = new CopyOnWriteArrayList<Permission>();
        this.osSubscription = new OnlineStorageSubscription();
        this.licenseKeyFiles = new CopyOnWriteArrayList<String>();
        this.computers = new CopyOnWriteArrayList<MemberInfo>();
        this.licenseKeyFileList = new CopyOnWriteArrayList<String>();
        this.groups = new CopyOnWriteArrayList<Group>();
        this.emails = new CopyOnWriteArrayList<String>();
    }

    /**
     * @return a leightweight/reference object to this account.
     */
    public AccountInfo createInfo() {
        return new AccountInfo(oid, username, getDisplayName());
    }

    // Basic permission stuff *************************************************

    public synchronized void grant(Permission... newPermissions) {
        Reject.ifNull(newPermissions, "Permission is null");
        for (Permission p : newPermissions) {
            if (hasPermission(p)) {
                // Skip
                continue;
            }
            if (p instanceof FolderPermission) {
                FolderInfo foInfo = ((FolderPermission) p).getFolder();
                revokeAllFolderPermission(foInfo);
                if (foInfo.isMetaFolder()) {
                    LOG.severe(this + ": Not allowed to grant permissions "
                        + foInfo);
                    continue;
                }
            }
            permissions.add(p);
        }
        LOG.fine("Granted permission to " + this + ": "
            + Arrays.asList(newPermissions));
    }

    public synchronized void revoke(Permission... revokePermissions) {
        Reject.ifNull(revokePermissions, "Permission is null");
        for (Permission p : revokePermissions) {
            if (permissions.remove(p)) {
                LOG.fine("Revoked permission from " + this + ": " + p);
            }
        }
    }

    /**
     * Revokes any permission to a folders.
     *
     * @param foInfo
     *            the folder.
     */
    public void revokeAllFolderPermission(FolderInfo foInfo) {
        revoke(FolderPermission.read(foInfo),
            FolderPermission.readWrite(foInfo), FolderPermission.admin(foInfo),
            FolderPermission.owner(foInfo));
    }

    /**
     * Revokes permission on ALL folders
     */
    public void revokeAllFolderPermission() {
        // Revokes permission on ALL folders
        for (Permission p : permissions) {
            if (p instanceof FolderPermission) {
                revoke(p);
            }
        }
    }

    public synchronized void revokeAllPermissions() {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Revoking all permission from " + this + ": "
                + permissions);
        }
        permissions.clear();
    }

    public boolean hasPermission(Permission permission) {
        Reject.ifNull(permission, "Permission is null");
        if (permissions == null) {
            LOG.severe("Illegal account " + username + ", permissions is null");
            return false;
        }
        for (Permission p : permissions) {
            if (p == null) {
                LOG.severe("Got null permission on " + this);
                continue;
            }
            if (p.equals(permission)) {
                return true;
            }
            if (p.implies(permission)) {
                return true;
            }
        }
        for (Group g : groups) {
            if (g.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    /**
     * @param folder
     * @return the permission on the given folder. AccessMode.NO_ACCESS for no
     *         access.
     */
    public AccessMode getAllowedAccess(FolderInfo folder) {
        if (hasPermission(FolderPermission.owner(folder))) {
            return FolderPermission.owner(folder).getMode();
        } else if (hasPermission(FolderPermission.admin(folder))) {
            return FolderPermission.admin(folder).getMode();
        } else if (hasPermission(FolderPermission.readWrite(folder))) {
            return FolderPermission.readWrite(folder).getMode();
        } else if (hasPermission(FolderPermission.read(folder))) {
            return FolderPermission.read(folder).getMode();
        }
        return AccessMode.NO_ACCESS;
    }

    /**
     * @return the list of folders this account gets charged for.
     */
    public Collection<FolderInfo> getFoldersCharged() {
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<FolderInfo> folders = new ArrayList<FolderInfo>(
            permissions.size());
        for (Permission p : permissions) {
            if (p instanceof FolderOwnerPermission) {
                FolderPermission fp = (FolderPermission) p;
                folders.add(fp.getFolder());
            }
        }
        for (Group g : groups) {
            for (Permission p : g.getPermissions()) {
                if (p instanceof FolderOwnerPermission) {
                    FolderPermission fp = (FolderPermission) p;
                    folders.add(fp.getFolder());
                }
            }
        }
        return folders;
    }

    /**
     * @return all folders the account has directly at folder read permission
     *         granted.
     */
    public Collection<FolderInfo> getFolders() {
        List<FolderInfo> folderInfos = new ArrayList<FolderInfo>(
            permissions.size());
        for (Permission permission : permissions) {
            if (permission instanceof FolderPermission) {
                FolderPermission fp = (FolderPermission) permission;
                folderInfos.add(fp.getFolder());
            }
        }
        for (Group g : groups) {
            for (Permission p : g.getPermissions()) {
                if (p instanceof FolderPermission) {
                    FolderPermission fp = (FolderPermission) p;
                    folderInfos.add(fp.getFolder());
                }
            }
        }
        return folderInfos;
    }

    public void internFolderInfos() {
        for (Permission permission : permissions) {
            if (permission instanceof FolderPermission) {
                FolderPermission fp = (FolderPermission) permission;
                fp.folder = fp.folder.intern();
            }
        }
    }

    public void addGroup(Group... group) {
        Reject.ifNull(group, "Group is null");
        for (Group g : group) {
            if (!groups.contains(g)) {
                groups.add(g);
            }
        }
    }

    public void removeGroup(Group... group) {
        Reject.ifNull(group, "Group is null");
        for (Group g : group) {
            groups.remove(g);
        }
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
        this.username = username;
    }

    public String getDisplayName() {
        // TODO Rework completely
        if (authByShibboleth() && !emails.isEmpty()) {
            return emails.get(0);
        }
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPasswordPlain(String prefix, String password) {
        this.password = prefix + password;
    }

    public void setPasswordSalted(String password) {
        this.password = LoginUtil.hashAndSalt(password);
    }

    /**
     * @param pwCandidate
     * @return true if the password candidate matches the password of the
     *         account.
     */
    public boolean passwordMatches(char[] pwCandidate) {
        return LoginUtil.matches(pwCandidate, password);
    }

    /**
     * setLanguage Set account language
     *
     * @return Selected language
     */

    public String getLanguage() {
        return this.language;
    }

    /**
     * setLanguage Set account language
     *
     * @param lang
     *            New language
     */

    public void setLanguage(String lang) {
        this.language = lang;
    }

    public String getLdapDN() {
        return ldapDN;
    }

    public void setLdapDN(String ldapDN) {
        System.err.println("Setting LDAP DN to: " + ldapDN);
        this.ldapDN = ldapDN;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    public OnlineStorageSubscription getOSSubscription() {
        if (osSubscription == null) {
            // osSubscription = new OnlineStorageSubscription();
            // osSubscription.setType(OnlineStorageSubscriptionType.TRIAL_5GB);
        }
        return osSubscription;
    }

    public void setOSSubscription(OnlineStorageSubscription osSubscription) {
        this.osSubscription = osSubscription;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustom1() {
        return custom1;
    }

    public void setCustom1(String custom1) {
        this.custom1 = custom1;
    }

    public String getCustom2() {
        return custom2;
    }

    public void setCustom2(String custom2) {
        this.custom2 = custom2;
    }

    public String getCustom3() {
        return custom3;
    }

    public void setCustom3(String custom3) {
        this.custom3 = custom3;
    }

    public boolean authByShibboleth() {
        // Fine a better way:
        return username.contains(Constants.SHIBBOLETH_USERNAME_SEPARATOR);
    }

    public boolean authByLDAP() {
        return StringUtils.isNotBlank(ldapDN);
    }

    public boolean authByRADIUS() {
        // Fine a better way:
        return notes != null && notes.toLowerCase().contains("radius");
    }
    
    public boolean authByDatabase() {
        return !authByLDAP() && !authByRADIUS() && !authByShibboleth();
    }

    // PFS-742: TODO Add EXTRA Field for this later
    public boolean isSendEmail() {
        if (StringUtils.isBlank(custom2)) {
            return true;
        }
        return !custom2.toUpperCase().contains("NOEMAIL");
    }

    public void setSendEmail(boolean sendEmail) {
        if (!sendEmail) {
            custom2 = "NOEMAIL";
        } else {
            custom2 = null;
        }
    }

    /**
     * Adds a line of info with the current date to the notes of that account.
     * 
     * @param infoText
     */
    public void addNotesWithDate(String infoText) {
        if (StringUtils.isBlank(infoText)) {
            return;
        }
        String infoLine = Format.formatDateCanonical(new Date());
        infoLine += ": ";
        infoLine += infoText;
        if (StringUtils.isBlank(notes)) {
            setNotes(infoLine);
        } else {
            setNotes(notes + "\n" + infoLine);
        }
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public FolderInfo getDefaultSynchronizedFolder() {
        return defaultSynchronizedFolder;
    }

    public void setDefaultSynchronizedFolder(
        FolderInfo defaultSynchronizedFolder)
    {
        this.defaultSynchronizedFolder = defaultSynchronizedFolder;
    }

    public MemberInfo getLastLoginFrom() {
        return lastLoginFrom;
    }

    /**
     * @param lastLoginFrom
     * @return true if this is a new computer. false if not.
     */
    public boolean setLastLoginFrom(MemberInfo lastLoginFrom) {
        this.lastLoginFrom = lastLoginFrom;

        // Set login date
        touchLogin();

        // Ensure initialization
        getComputers();
        if (lastLoginFrom != null && !computers.contains(lastLoginFrom)) {
            computers.add(lastLoginFrom);
            return true;
        }
        return false;
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

    /**
     * @return the computers this account is associated with.
     */
    public Collection<MemberInfo> getComputers() {
        return computers;
    }

    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(groups);
    }

    public void addLicenseKeyFile(String filename) {
        if (licenseKeyFileList.contains(filename)) {
            return;
        }
        licenseKeyFileList.add(filename);
    }

    public List<String> getLicenseKeyFiles() {
        return licenseKeyFileList;
    }

    public boolean addEmail(String email) {
        Reject.ifBlank(email, "Email");
        email = email.trim().toLowerCase();
        if (emails.contains(email)) {
            return false;
        }
        emails.add(email);
        return true;
    }

    public boolean removeEmail(String email) {
        Reject.ifBlank(email, "Email");
        return emails.remove(email.toLowerCase().trim());
    }

    public List<String> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    public int getAutoRenewDevices() {
        return autoRenewDevices;
    }

    public Date getAutoRenewTill() {
        return autoRenewTill;
    }

    public boolean isProUser() {
        return proUser;
    }

    public void setAutoRenew(int autoRenewDevices, Date autoRenewTill,
        boolean proUser)
    {
        this.autoRenewDevices = autoRenewDevices;
        this.autoRenewTill = autoRenewTill;
        this.proUser = proUser;
    }

    public boolean willAutoRenew() {
        if (autoRenewDevices <= 0) {
            return false;
        }
        if (autoRenewTill == null) {
            return true;
        }
        return autoRenewTill.after(new Date());
    }

    /**
     * @return the days since the user has registered
     */
    public int getDaysSinceRegistration() {
        if (registerDate == null) {
            return -1;
        }
        long daysSinceRegistration = (System.currentTimeMillis() - registerDate
            .getTime()) / (1000L * 60 * 60 * 24);
        return (int) daysSinceRegistration;
    }

    public String toString() {
        return "Account '" + username + "', " + permissions.size()
            + " permissions";
    }

    public String toDetailString() {
        return toString() + ", pro? " + proUser + ", regdate: "
            + Format.formatDateShort(registerDate) + ", licenses: "
            + (licenseKeyFileList != null ? licenseKeyFileList.size() : "n/a")
            + ", " + osSubscription;
    }

    // Convenience/Applogic ***************************************************

    /**
     * Enables the selected account:
     * <p>
     * The Online Storage subscription
     * <P>
     * Sets all folders to SyncProfile.BACKUP_TARGET.
     * <p>
     * FIXME: Does only set the folders hosted on the CURRENT server to backup.
     * <p>
     * Account needs to be stored afterwards!!
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

        enableSync(controller);
    }

    /**
     * Sets all folders that have SyncProfile.DISABLED to
     * SyncProfile.BACKUP_TARGET_NO_CHANGE_DETECT.
     *
     * @param controller
     * @return the number of folder the sync was re-enabled.
     */
    public int enableSync(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        int n = 0;
        for (FolderInfo foInfo : getFoldersCharged()) {
            Folder f = foInfo.getFolder(controller);
            if (f == null) {
                continue;
            }
            if (f.getSyncProfile().equals(SyncProfile.DISABLED)) {
                n++;
                SyncProfile p = SyncProfile.getDefault(controller);
                f.setSyncProfile(p);
            }
        }
        return n;
    }

    /**
     * Sets all folders that don't have SyncProfile.DISABLED to
     * SyncProfile.DISABLED.
     *
     * @param controller
     * @return the number of folder the sync was disabled.
     */
    public int disableSync(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        int nNewDisabled = 0;
        for (FolderInfo foInfo : getFoldersCharged()) {
            Folder folder = foInfo.getFolder(controller);
            if (folder == null) {
                continue;
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Disable download of new files for folder: " + folder
                    + " for " + getUsername());
            }
            if (!folder.getSyncProfile().equals(SyncProfile.DISABLED)) {
                folder.setSyncProfile(SyncProfile.DISABLED);
                nNewDisabled++;
            }
        }
        if (nNewDisabled > 0) {
            LOG.info("Disabled " + nNewDisabled + " folder for "
                + getUsername());
        }
        return nNewDisabled;
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
        return hasPermission(FolderPermission.read(foInfo));
    }

    /**
     * Answers if the user is allowed to write into the folder.
     *
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to write into the folder.
     */
    public boolean hasReadWritePermissions(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(FolderPermission.readWrite(foInfo));
    }

    /**
     * Answers if the user is allowed to write into the folder.
     *
     * @param foInfo
     *            the folder to check
     * @return true if the user is allowed to write into the folder.
     */
    public boolean hasWritePermissions(FolderInfo foInfo) {
        return hasReadWritePermissions(foInfo);
    }

    /**
     * @param foInfo
     * @return true if the user is admin of the folder.
     */
    public boolean hasAdminPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(FolderPermission.admin(foInfo));
    }

    /**
     * @param foInfo
     * @return true if the user is owner of the folder.
     */
    public boolean hasOwnerPermission(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        return hasPermission(FolderPermission.owner(foInfo));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || !(obj instanceof Account)) {
            return false;
        }

        Account otherAccount = (Account) obj;

        return (this.oid.equals(otherAccount.oid));
    }

    public synchronized void convertCollections() {
        if (!(permissions instanceof CopyOnWriteArrayList<?>)) {
            Collection<Permission> newPermissions = new CopyOnWriteArrayList<Permission>(
                permissions);
            permissions = newPermissions;
        }

        if (!(groups instanceof CopyOnWriteArrayList<?>)) {
            Collection<Group> newGroups = new CopyOnWriteArrayList<Group>(
                groups);
            groups = newGroups;
            for (Group group : groups) {
                group.convertCollections();
            }
        }

        if (!(computers instanceof CopyOnWriteArrayList<?>)) {
            Collection<MemberInfo> newComputers = new CopyOnWriteArrayList<MemberInfo>(
                computers);
            computers = newComputers;
        }

        if (!(licenseKeyFileList instanceof CopyOnWriteArrayList<?>)) {
            List<String> newLicenseKeyFileList = new CopyOnWriteArrayList<String>(
                licenseKeyFileList);
            licenseKeyFileList = newLicenseKeyFileList;
        }

        if (!(emails instanceof CopyOnWriteArrayList<?>)) {
            List<String> newEmails = new CopyOnWriteArrayList<String>(emails);
            emails = newEmails;
        }
    }

    public void migrate() {
        if (licenseKeyFiles != null) {
            licenseKeyFileList = new CopyOnWriteArrayList<String>(
                licenseKeyFiles);
        }
        if (groups == null) {
            groups = new CopyOnWriteArrayList<Group>();
        }
        if (server != null) {
            server.migrateId();
        }
    }

    private void writeObject(java.io.ObjectOutputStream stream)
        throws IOException
    {
        licenseKeyFiles = new CopyOnWriteArrayList<String>(licenseKeyFileList);
        stream.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        if (oid == null) {
            // Migration.
            oid = IdGenerator.makeId();
        }
        if (groups == null) {
            groups = new CopyOnWriteArrayList<Group>();
        }
        if (emails == null) {
            emails = new CopyOnWriteArrayList<String>();
        }
    }
}
