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

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.protocol.*;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.db.PermissionUserType;
import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Domain class of an user account.
 *
 * @author Christian Sprajc
 * @version $Revision: 1.5 $
 */
@TypeDef(name = "permissionType", typeClass = PermissionUserType.class)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Account implements Serializable, D2DObject {

    private static final Logger LOG = Logger.getLogger(Account.class.getName());
    private static final long serialVersionUID = 100L;

    // Properties
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_USERNAME = "username";
    public static final String PROPERTYNAME_PASSWORD = "password";
    public static final String PROPERTYNAME_OTP = "otp";
    public static final String PROPERTYNAME_LDAPDN = "ldapDN";
    public static final String PROPERTYNAME_SHIBBOLETH_PERSISTENT_ID = "shibbolethPersistentID";
    public static final String PROPERTYNAME_LANGUAGE = "language";
    public static final String PROPERTYNAME_PERMISSIONS = "permissions";
    public static final String PROPERTYNAME_REGISTER_DATE = "registerDate";
    public static final String PROPERTYNAME_LAST_LOGIN_DATE = "lastLoginDate";
    public static final String PROPERTYNAME_LAST_LOGIN_FROM = "lastLoginFrom";
    public static final String PROPERTYNAME_NEWSLETTER = "newsLetter";
    public static final String PROPERTYNAME_PRO_USER = "proUser";
    public static final String PROPERTYNAME_NOTES = "notes";
    public static final String PROPERTYNAME_SERVER = "server";
    public static final String PROPERTYNAME_BASE_PATH = "basePath";
    public static final String PROPERTYNAME_DEFAULT_SYNCHRONIZED_FOLDER = "defaultSynchronizedFolder";
    public static final String PROPERTYNAME_OS_SUBSCRIPTION = "osSubscription";
    public static final String PROPERTYNAME_LICENSE_KEY_FILES = "licenseKeyFiles";
    public static final String PROPERTYNAME_COMPUTERS = "computers";
    public static final String PROPERTYNAME_GROUPS = "groups";
    public static final String PROPERTYNAME_DISPLAYNAME = "displayName";
    public static final String PROPERTYNAME_FIRSTNAME = "firstname";
    public static final String PROPERTYNAME_SURNAME = "surname";
    public static final String PROPERTYNAME_TELEPHONE = "telephone";
    public static final String PROPERTYNAME_EMAILS = "emails";
    public static final String PROPERTYNAME_ORGANIZATION_ID = "organizationOID";
    public static final String PROPERTYNAME_AGREED_TOS_VERSION = "agreedToSVersion";

    @Id
    private String oid;
    @Index(name = "IDX_USERNAME")
    @Column(nullable = false, unique = true)
    private String username;
    private String password;
    // PFS-862: One time token password
    @Index(name = "IDX_AOTP")
    private String otp;
    
    // PFC-2455: Tokens for federated services
    @CollectionOfElements(targetElement = String.class)
    @MapKeyManyToMany(targetEntity = ServerInfo.class, joinColumns=@JoinColumn(name="serviceInfo_id"))
    @JoinTable(name = "Account_tokens", joinColumns = @JoinColumn(name = "oid"))
    @Column(name = "tokenSecret")
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<ServerInfo, String> tokens;
    
    private String language;

    @Index(name = "IDX_LDAPDN")
    @Column(length = 512)
    private String ldapDN;
    
    @Index(name = "IDX_SHIB_PID")
    @Column(length = 2048)
    private String shibbolethPersistentID;
    private Date registerDate;
    private Date lastLoginDate;
    @ManyToOne
    @JoinColumn(name = "lastLoginFrom_id")
    private MemberInfo lastLoginFrom;
    private boolean proUser;

    @Column(length = 255)
    @Index(name = "IDX_ACC_FIRSTNAME")
    private String firstname;
    @Column(length = 255)
    @Index(name = "IDX_ACC_SURNAME")
    private String surname;
    @Column(length = 255)
    private String telephone;

    // PFS-605
    private String custom1;
    private String custom2;
    private String custom3;
    
    // PFS-1656
    @Column(length = 4000)
    private String jsonData;

    @Column(length = 2048)
    private String notes;
    
    // PFS-1446     
    @Column(length = 512)
    private String basePath;

    @Index(name = "IDX_ACC_ORG_ID")
    @Column(nullable = true, unique = false)
    private String organizationOID;

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

    /**
     * PFS-992: If this account is static and won't be switch automatically by
     * ClusterManager.
     */
    private boolean serverStatic;

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

    private int agreedToSVersion;

    Account() {
        // Generate unique id
        this(IdGenerator.makeId());
    }

    Account(String oid) {
        Reject.ifBlank(oid, "OID");
        this.oid = oid;
        this.permissions = new CopyOnWriteArrayList<Permission>();
        this.osSubscription = new OnlineStorageSubscription();
        this.licenseKeyFiles = new CopyOnWriteArrayList<>();
        this.computers = new CopyOnWriteArrayList<>();
        this.licenseKeyFileList = new CopyOnWriteArrayList<>();
        this.groups = new CopyOnWriteArrayList<>();
        this.emails = new CopyOnWriteArrayList<>();
        this.tokens = new ConcurrentHashMap<>();
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public Account(AbstractMessage mesg) {
        initFromD2D(mesg);
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

    public boolean hasAnyFolderAdmin() {
        for (Permission p : permissions) {
            if (p == null) {
                continue;
            }
            if (p instanceof FolderPermission) {
                AccessMode mode = ((FolderPermission) p).getMode();
                if (mode.equals(AccessMode.ADMIN)
                    || mode.equals(AccessMode.OWNER))
                {
                    return true;
                }
            }
        }

        for (Group g : groups) {
            for (Permission p : g.getPermissions()) {
                if (p == null) {
                    continue;
                }
                if (p instanceof FolderPermission) {
                    AccessMode mode = ((FolderPermission) p).getMode();
                    if (mode.equals(AccessMode.ADMIN)
                        || mode.equals(AccessMode.OWNER))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    /**
     * Returns the {@link FolderPermission} this {@link Account} has for a
     * certain {@code Folder}.
     * 
     * @param foInfo
     * @return A FolderPermission with the correct {@code AccessMode}.
     */
    public FolderPermission getPermissionFor(FolderInfo foInfo) {
        for (Permission perm : permissions) {
            if (perm instanceof FolderPermission) {
                FolderPermission foPerm = (FolderPermission) perm;
                if (foPerm.getFolder().equals(foInfo)) {
                    return foPerm;
                }
            }
        }

        return FolderPermission.get(foInfo, AccessMode.NO_ACCESS);
    }

    /**
     * @return An unmodifiable collection of all
     *         {@link OrganizationAdminPermission OrganizationAdminPermissions}.
     *         If the user does not have any OrganizationAdminPermission, an
     *         empty collection will be returned.
     */
    public Collection<OrganizationAdminPermission> getOrgAdminPermissions() {
        Collection<OrganizationAdminPermission> orgAdmins = new ArrayList<>();
        for (Permission perm : permissions) {
            if (perm instanceof OrganizationAdminPermission) {
                orgAdmins.add((OrganizationAdminPermission) perm);
            }
        }
        return Collections.unmodifiableCollection(orgAdmins);
    }

    /**
     * 
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
     * @return {@code True} if user is member of an {@link Organization} and has
     *         {@link OrganizationAdminPermission} for this organization, {@code false} otherwise.
     */
    public boolean isAdminOfOwnOrganization() {
        if (StringUtils.isBlank(organizationOID)) {
            return false;
        }
        return hasPermission(new OrganizationAdminPermission(organizationOID));
    }

    /**
     * @return {@code True} if user has any {@link OrganizationAdminPermission}
     *         or {@link OrganizationCreatePermission}, {@code false} otherwise.
     */
    public boolean isAdminOfAnyOrganization() {
        if (hasPermission(OrganizationCreatePermission.INSTANCE)) {
            return true;
        }

        for (Permission p : permissions) {
            if (p instanceof OrganizationAdminPermission) {
                return true;
            }
        }

        return false;
    }

    public boolean isInSameOrganization(Account other) {
        return Util.equals(organizationOID, other.getOrganizationOID());
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
        if (StringUtils.isNotBlank(firstname)
            || StringUtils.isNotBlank(surname))
        {
            String fn = (firstname == null ? "" : firstname).trim();
            String sn = (surname == null ? "" : surname).trim();

            if (StringUtils.isBlank(fn)) {
                return sn;
            }
            if (StringUtils.isBlank(sn)) {
                return fn;
            }

            return (fn + " " + sn).trim();
        } else if (StringUtils.isNotBlank(username) && authByShibboleth()
            && !emails.isEmpty())
        {
            return this.getEmails().get(0);
        } else if (!emails.isEmpty() && StringUtils.isNotBlank(emails.get(0))) {
            return this.getEmails().get(0);
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

    //  PFS-862: OTP Handling

    public String getOTP() {
        return otp;
    }

    public boolean isOTPValid() {
        return LoginUtil.isOTPValid(otp);
    }

    public String generateAndSetOTP() {
        this.otp = LoginUtil.generateOTP();
        return this.otp;
    }

    public void invalidateOTP() {
        this.otp = null;
    }

    // PFS-2455: Start
    
    /**
     * Sets a new token to authenticate at the federated service/server
     * 
     * @param fedServiceInfo
     *            the federated service
     * @param tokenSecret the token secret, which can be used for authentication.
     * @return if the token is new.
     */
    public void setToken(ServerInfo fedServiceInfo, String tokenSecret) {
        Reject.ifNull(fedServiceInfo, "fedServiceInfo");
        Reject.ifFalse(fedServiceInfo.isFederatedService(),
            "Setting token only possible for federated services");
        tokens.put(fedServiceInfo, tokenSecret);
    }

    /**
     * Removes the token for the federated service.
     * 
     * @param fedServiceInfo
     *            the federated service
     */
    public void removeToken(ServerInfo fedServiceInfo) {
        Reject.ifNull(fedServiceInfo, "fedServiceInfo");
        Reject.ifFalse(fedServiceInfo.isFederatedService(),
            "Setting token only possible for federated services");
        tokens.remove(fedServiceInfo);
    }

    /**
     * @param fedServiceInfo
     *            the federated service
     * @return the token secret for authentication or null.
     */
    public String getToken(ServerInfo fedServiceInfo) {
        return tokens.get(fedServiceInfo);
    }
    
    public Map<ServerInfo, String> getTokens() {
        return Collections.unmodifiableMap(tokens);
    }
    
    // PFS-2455: End

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
        this.ldapDN = ldapDN;
    }

    public String getShibbolethPersistentID() {
        return shibbolethPersistentID;
    }

    public void setShibbolethPersistentID(String shibbolethPersistentID) {
        this.shibbolethPersistentID = shibbolethPersistentID;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(Date registerDate) {
        this.registerDate = registerDate;
    }

    public OnlineStorageSubscription getOSSubscription() {
        return osSubscription;
    }

    public void setOSSubscription(OnlineStorageSubscription osSubscription) {
        this.osSubscription = osSubscription;
    }

    public boolean hasOwnStorage() {
        return osSubscription.getStorageSize() != 0;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getOrganizationOID() {
        return organizationOID;
    }

    public void setOrganizationOID(String organizationOID) {
        this.organizationOID = organizationOID;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
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
    
    public String getJSONData() {
        return jsonData;
    }
    
    public void setJSONData(String jsonData) {
        this.jsonData = jsonData;
    }

    public JSONObject getJSONObject() {
        if (StringUtils.isBlank(jsonData)) {
            return new JSONObject();
        }
        try {
            return new JSONObject(jsonData);
        } catch (JSONException e) {
            LOG.severe("Illegal JSON data for " + username + ": " + jsonData
                + ". " + e);
            return new JSONObject();
        }
    }

    public void setJSONObject(JSONObject jsonObject) {
        if (jsonObject == null) {
            this.jsonData = null;
            return;
        }
        this.jsonData = jsonObject.toString();
    }

    public void put(String key, String value) {
        JSONObject o = getJSONObject();
        try {
            o.put(key, value);
        } catch (JSONException e) {
            LOG.severe("Unable to set extra information in JSON format to "
                + username + ": " + key + "=" + value + ". " + e);
            return;
        }
        setJSONObject(o);
    }
    
    public void put(String key, long value) {
        JSONObject o = getJSONObject();
        try {
            o.put(key, value);
        } catch (JSONException e) {
            LOG.severe("Unable to set extra information in JSON format to "
                + username + ": " + key + "=" + value + ". " + e);
            return;
        }
        setJSONObject(o);
    }

    public boolean authByShibboleth() {
        return StringUtils.isNotBlank(shibbolethPersistentID);
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = StringUtils.cutNotes(notes);
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
        String newNotes;
        if (StringUtils.isBlank(notes)) {
            newNotes = infoLine;
        } else {
            newNotes = notes + "\n" + infoLine;
        }
        setNotes(newNotes);
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public boolean isServerStatic() {
        return serverStatic;
    }

    public void setServerStatic(boolean serverStatic) {
        this.serverStatic = serverStatic;
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
        getDevices();
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
     * @return the devices this account is associated with.
     */
    public Collection<MemberInfo> getDevices() {
        return Collections.unmodifiableCollection(computers);
    }

    /**
     * Adds a device/computer to the list of associated devices.
     * 
     * @param nodeInfo
     */
    public void addDevice(MemberInfo nodeInfo) {
        Reject.ifNull(nodeInfo, "NodeInfo");
        synchronized (computers) {
            if (!computers.contains(nodeInfo)) {
                computers.add(nodeInfo);
            }
        }
    }

    /**
     * Removes a device/computer from the list of associated devices.
     * 
     * @param nodeInfo
     * @return true if the device was removed, false if the device was not in
     *         list
     */
    public boolean removeDevice(MemberInfo nodeInfo) {
        Reject.ifNull(nodeInfo, "NodeInfo");
        return computers.remove(nodeInfo);
    }

    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(groups);
    }

    /**
     * @return all groups that were synchronized via LDAP/AD
     */
    public Collection<Group> getLdapGroups() {
        Collection<Group> ldapGroups = new ArrayList<>();

        for (Group g : groups) {
            if (StringUtils.isNotBlank(g.getLdapDN())) {
                ldapGroups.add(g);
            }
        }

        return ldapGroups;
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
    
    /**
     * Adds an email address to the account, combined with its corresponding LDAP identifier
     * 
     * @param email
     *              The email address
     * @param ldap
     *              The LDAP search context of the email address
     * @return true if the email address was added
     */
    public boolean addEmail(String email, String ldapSearchBase) {
        Reject.ifBlank(email, "Email");
        Reject.ifBlank(ldapSearchBase, "LDAP");
        email = email.trim().toLowerCase();
        ldapSearchBase = ldapSearchBase.trim().toLowerCase();
        // If email address with LDAP search context is already in email list, return
        if (emails.contains(email + ":" + ldapSearchBase)) {
            boolean store = false;
            // Remove possible duplicates without LDAP search context
            while (emails.contains(email)) {
                emails.remove(email);
                store = true;
            }
            return store;
        }
        // If email address without LDAP search context is already in emails list, replace the email address
        // (This way the same email address with ANOTHER LDAP context stays unaffected)
        if (emails.contains(email)) {
            int index = emails.indexOf(email);
            emails.remove(index);
            emails.add(index, email + ":" + ldapSearchBase);
            return true;
        }
        // Add email address
        emails.add(email + ":" + ldapSearchBase);
        return true;
    }

    public boolean removeEmail(String email) {
        Reject.ifBlank(email, "Email");
        email = email.toLowerCase().trim();
        // Do only a partial match of the email address because it may also contain LDAP information separated by ":"
        for (String element : emails) {
            if (element.startsWith(email)) {
                return emails.remove(element);
            }
        }
        return emails.remove(email);
    }

    /**
     * Remove the email addresses stored for the account, that are not in the
     * list {@code ldapEmails} but contain the {@code ldapSearchBase} as suffix.
     * 
     * @param ldapEmails
     * @param ldapSearchBase
     * @return
     */
    public boolean removeNonExistingLdapEmails(List<String> ldapEmails,
        String ldapSearchBase) {
        ldapSearchBase = ldapSearchBase.trim().toLowerCase();
        // Append LDAP context to emails
        for (final ListIterator<String> i = ldapEmails.listIterator(); i
            .hasNext();) {
            final String email = i.next().trim().toLowerCase();
            i.set(email + ":" + ldapSearchBase);
        }
        boolean store = false;
        for (String email : emails) {
            // Only check email addresses belonging to the LDAP context
            if ((email.indexOf(":") > 0) && (email.split(":")[1].equalsIgnoreCase(ldapSearchBase))) {
                // If email is no longer existing in LDAP, remove it
                if (!ldapEmails.contains(email)) {
                    emails.remove(email);
                    store = true;
                }
            }
        }
        return store;
    }

    public List<String> getEmails() {
        // Create list of emails without LDAP search context information
        List<String> result = new ArrayList<>();
        for (String email : emails) {
            int index = email.indexOf(':');
            if (index > 0) {
                result.add(email.substring(0, index));
            } else {
                result.add(email);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Start PFS-1568
     */
    public void clearEmails() {
        emails.clear();
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

    public boolean isLimitedUser() {
        if (!authByDatabase()) {
            return false;
        }
        if (osSubscription.getStorageSize() > 0) {
            return false;
        }
        return true;
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

    public int getAgreedToSVersion() {
        return agreedToSVersion;
    }

    public void setAgreedToSVersion(int agreedToSVersion) {
        this.agreedToSVersion = agreedToSVersion;
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

    @Override
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
     * Transfer the information of {@code account} into {@code this}.<br />
     * <br />
     * Information that is transferred:
     * <ul>
     * <li>Username as e-mail (if username is a formally valid e-mail address
     * and the account is not authenticated by Shibboleth).</li>
     * <li>All e-mails</li>
     * <li>Quota (if bigger)</li>
     * <li>License files</li>
     * <li>Organization OID (if this is blank)</li>
     * <li>LDAP Distinguished Name (if this is blank)</li>
     * <li>Shibboleth persisten ID (if this is blank)</li>
     * <li>All folder permissions</li>
     * <li>All groups</li>
     * <li>All computers/devices</li>
     * <li>Notes are appended</li>
     * </ul>
     * 
     * @param account
     */
    public void mergeAccounts(Account account) {
        Reject.ifNull(account, "Account is null");
        Reject.ifTrue(this.equals(account), "Unable to merge account with itself");

        // Add Username and Emails
        if (Util.isValidEmail(account.getUsername())
            && !account.authByShibboleth())
        {
            this.addEmail(account.getUsername());
        }

        for (String email : account.emails) {
            this.addEmail(email);
        }

        // Use the OSSubscription that has provides more space
        if (account.getOSSubscription().getStorageSizeGB() > this.osSubscription.getStorageSizeGB()) {
            this.osSubscription = account.osSubscription;
        }

        // Combine License Key Files
        this.licenseKeyFileList.addAll(account.licenseKeyFileList);

        // Set the Organization OID if this is account is not yet in an Organization
        if (StringUtils.isBlank(this.organizationOID)) {
            this.organizationOID = account.organizationOID;
        }

        if (StringUtils.isBlank(this.ldapDN)) {
            this.ldapDN = account.ldapDN;
        }

        if (StringUtils.isBlank(this.shibbolethPersistentID)) {
            this.shibbolethPersistentID = account.shibbolethPersistentID;
        }

        // Add permissions
        this.grant(account.permissions.toArray(new Permission[0]));

        // Combine groups
        for (Group newGroup : account.groups) {
            if (!this.groups.contains(newGroup)) {
                this.groups.add(newGroup);
            }
        }

        // Combine computers
        this.computers.addAll(account.computers);

        boolean containsMergeNote = StringUtils.isNotBlank(account.notes)
            && StringUtils.isNotBlank(this.notes)
            && this.notes.contains("Merged with account '"
                + account.getUsername() + "'");

        // Combine Notes
        if (!containsMergeNote) {
            StringBuilder sb = new StringBuilder();
            if (this.notes != null) {
                sb.append(this.notes);
                sb.append("\n");
            }
            if (StringUtils.isNotBlank(account.notes)) {
                sb.append("Begin of notes of " + account.getUsername() + "\n");
                sb.append(account.notes);
                sb.append("\nEND of notes of " + account.getUsername());
            }
            this.notes = sb.toString();
            this.addNotesWithDate("Merged with account "
                + account.getUsername());

            int len = sb.length();
            if (len >= 1000) {
                int cut = len - 1000;
                sb.replace(0, cut, "");
                this.notes = sb.toString().trim();
            }
        }
    }

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

    @Override
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
            Collection<Group> newGroups = new CopyOnWriteArrayList<>(groups);
            groups = newGroups;
            for (Group group : groups) {
                group.convertCollections();
            }
        }

        if (!(computers instanceof CopyOnWriteArrayList<?>)) {
            Collection<MemberInfo> newComputers = new CopyOnWriteArrayList<>(
                computers);
            computers = newComputers;
        }

        if (!(licenseKeyFileList instanceof CopyOnWriteArrayList<?>)) {
            List<String> newLicenseKeyFileList = new CopyOnWriteArrayList<>(
                licenseKeyFileList);
            licenseKeyFileList = newLicenseKeyFileList;
        }
        
        if (!(licenseKeyFiles instanceof CopyOnWriteArrayList<?>)) {
            List<String> newlicenseKeyFiles = new CopyOnWriteArrayList<>(
                licenseKeyFiles);
            licenseKeyFiles = newlicenseKeyFiles;
        }

        if (!(emails instanceof CopyOnWriteArrayList<?>)) {
            List<String> newEmails = new CopyOnWriteArrayList<>(emails);
            emails = newEmails;
        }

        if (!(tokens instanceof ConcurrentHashMap<?, ?>)) {
            Map<ServerInfo, String> newTokens = new ConcurrentHashMap<>(tokens);
            tokens = newTokens;
        }
    }

    public void migrate() {
        if (licenseKeyFileList == null) {
            licenseKeyFileList = new CopyOnWriteArrayList<String>();
        }
        if (groups == null) {
            groups = new CopyOnWriteArrayList<Group>();
        }
        if (tokens == null) {
            tokens = new ConcurrentHashMap<>();
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
            groups = new CopyOnWriteArrayList<>();
        }
        if (emails == null) {
            emails = new CopyOnWriteArrayList<>();
        }
        if (tokens == null) {
            tokens = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof AccountProto.Account) {
            AccountProto.Account proto = (AccountProto.Account)mesg;
            this.oid                        = proto.getOid();
            this.username                   = proto.getUsername();
            this.password                   = proto.getPassword();
            this.otp                        = proto.getOtp();
            this.language                   = proto.getLanguage();
            this.ldapDN                     = proto.getLdapDn();
            this.shibbolethPersistentID     = proto.getShibbolethPersistentId();
            this.registerDate               = new Date(proto.getRegisterDate());
            this.lastLoginDate              = new Date(proto.getLastLoginDate());
            this.lastLoginFrom              = new MemberInfo(proto.getLastLoginFromNodeInfo());
            this.proUser                    = proto.getProUser();
            this.firstname                  = proto.getFirstname();
            this.surname                    = proto.getSurname();
            this.telephone                  = proto.getTelephone();
            this.custom1                    = proto.getCustom1();
            this.custom2                    = proto.getCustom2();
            this.custom3                    = proto.getCustom3();
            this.notes                      = proto.getNotes();
            this.basePath                   = proto.getBasePath();
            this.organizationOID            = proto.getOrganizationOid();
            this.computers                  = new CopyOnWriteArrayList<MemberInfo>();
            for(NodeInfoProto.NodeInfo nodeInfoProto: proto.getNodeInfosList()) {
                this.computers.add(new MemberInfo(nodeInfoProto));
            }
            this.serverStatic               = proto.getServerStatic();
            this.licenseKeyFileList         = proto.getLicenseKeyFileListList();
            this.autoRenewDevices           = proto.getAutoRenewDevices();
            this.autoRenewTill              = new Date(proto.getAutoRenewTill());
            this.defaultSynchronizedFolder  = new FolderInfo(proto.getDefaultSynchronizedFolder());
            this.permissions                = new CopyOnWriteArrayList<Permission>();
            for (PermissionProto.Permission permissionProto: proto.getPermissionsList()) {
                switch(permissionProto.getPermissionType()) {
                    case ADMIN :
                        this.permissions.add(new AdminPermission(permissionProto));
                        break;
                    case CHANGE_PREFERENCES :
                        this.permissions.add(new ChangePreferencesPermission(permissionProto));
                        break;
                    case CHANGE_TRANSFER_MODE :
                        this.permissions.add(new ChangeTransferModePermission(permissionProto));
                        break;
                    case COMPUTERS_APP :
                        this.permissions.add(new ComputersAppPermission(permissionProto));
                        break;
                    case CONFIG_APP :
                        this.permissions.add(new ConfigAppPermission(permissionProto));
                        break;
                    case FOLDER_ADMIN :
                        this.permissions.add(new FolderAdminPermission(permissionProto));
                        break;
                    case FOLDER_CREATE :
                        this.permissions.add(new FolderCreatePermission(permissionProto));
                        break;
                    case FOLDER_OWNER :
                        this.permissions.add(new FolderOwnerPermission(permissionProto));
                        break;
                    case FOLDER_READ :
                        this.permissions.add(new FolderReadPermission(permissionProto));
                        break;
                    case FOLDER_READ_WRITE :
                        this.permissions.add(new FolderReadWritePermission(permissionProto));
                        break;
                    case FOLDER_REMOVE :
                        this.permissions.add(new FolderRemovePermission(permissionProto));
                        break;
                    case GROUP_ADMIN :
                        this.permissions.add(new GroupAdminPermission(permissionProto));
                        break;
                    case ORGANIZATION_ADMIN :
                        this.permissions.add(new OrganizationAdminPermission(permissionProto));
                        break;
                    case ORGANIZATION_CREATE :
                        this.permissions.add(new OrganizationCreatePermission(permissionProto));
                        break;
                    case SYSTEM_SETTINGS :
                        this.permissions.add(new SystemSettingsPermission(permissionProto));
                        break;
                    case UNRECOGNIZED :
                        break;
                    default :
                        break;
                }
            }
            this.groups                     = new CopyOnWriteArrayList<Group>();
            for(GroupProto.Group groupProto: proto.getGroupsList()) {
                this.groups.add(new Group(groupProto));
            }
            this.emails                     = new CopyOnWriteArrayList<String>();
            for(String email: proto.getEmailsList()) {
                this.emails.add(new String(email));
            }
            this.osSubscription             = new OnlineStorageSubscription(proto.getOsSubscription());
            this.agreedToSVersion           = proto.getAgreedToSversion();
        }
    }
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        AccountProto.Account.Builder builder = AccountProto.Account.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.oid != null) builder.setOid(this.oid);
        if (this.username != null) builder.setUsername(this.username);
        if (this.password != null) builder.setPassword(this.password);
        if (this.otp != null) builder.setOtp(this.otp);
        if (this.language != null) builder.setLanguage(this.language);
        if (this.ldapDN != null) builder.setLdapDn(this.ldapDN);
        if (this.shibbolethPersistentID != null) builder.setShibbolethPersistentId(this.shibbolethPersistentID);
        if (this.registerDate != null) builder.setRegisterDate(this.registerDate.getTime());
        if (this.lastLoginDate != null) builder.setLastLoginDate(this.lastLoginDate.getTime());
        if (this.lastLoginFrom != null) builder.setLastLoginFromNodeInfo((NodeInfoProto.NodeInfo)this.lastLoginFrom.toD2D());
        builder.setProUser(this.proUser);
        if (this.firstname != null) builder.setFirstname(this.firstname);
        if (this.surname != null) builder.setSurname(this.surname);
        if (this.telephone != null) builder.setTelephone(this.telephone);
        if (this.custom1 != null) builder.setCustom1(this.custom1);
        if (this.custom2 != null) builder.setCustom2(this.custom2);
        if (this.custom3 != null) builder.setCustom3(this.custom3);
        if (this.notes != null) builder.setNotes(this.notes);
        if (this.basePath != null) builder.setBasePath(this.basePath);
        if (this.organizationOID != null) builder.setOrganizationOid(this.organizationOID);
        for (MemberInfo computer: this.computers) {
            builder.addNodeInfos((NodeInfoProto.NodeInfo) computer.toD2D());
        }
        builder.setServerStatic(this.serverStatic);
        for (String licenseKeyFile: this.licenseKeyFileList) {
            builder.addLicenseKeyFileList(licenseKeyFile);
        }
        builder.setAutoRenewDevices(this.autoRenewDevices);
        if (this.autoRenewTill != null) builder.setAutoRenewTill(this.autoRenewTill.getTime());
        if (this.defaultSynchronizedFolder != null) builder.setDefaultSynchronizedFolder((FolderInfoProto.FolderInfo)this.defaultSynchronizedFolder.toD2D());
        for (Permission permission: this.permissions) {
            // Since the different permission classes do not have one common superclass we have to decide for each class separately
            if (permission instanceof FolderPermission) {
                builder.addPermissions((PermissionProto.Permission)((FolderPermission)permission).toD2D());
            }
            else if (permission instanceof GroupAdminPermission) {
                builder.addPermissions((PermissionProto.Permission)((GroupAdminPermission)permission).toD2D());
            }
            else if (permission instanceof OrganizationAdminPermission) {
                builder.addPermissions((PermissionProto.Permission)((OrganizationAdminPermission)permission).toD2D());
            }
            else if (permission instanceof SingletonPermission) {
                builder.addPermissions((PermissionProto.Permission)((SingletonPermission)permission).toD2D());
            }
        }
        for (Group group: this.groups) {
            builder.addGroups((GroupProto.Group) group.toD2D());
        }
        for (String email: this.emails) {
            builder.addEmails(email);
        }
        if (this.osSubscription != null) builder.setOsSubscription((OnlineStorageSubscriptionProto.OnlineStorageSubscription) this.osSubscription.toD2D());
        builder.setAgreedToSversion(this.agreedToSVersion);
        return builder.build();
    }
}
