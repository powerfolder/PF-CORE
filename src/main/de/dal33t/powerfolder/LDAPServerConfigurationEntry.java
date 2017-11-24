package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.logging.Loggable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

/**
 * This class is a replacement for the LDAP-Configuration Entries previously in
 * {@link de.dal33t.powerfolder.ConfigurationServerEntry}
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @since 11.5 SP 5
 */
public class LDAPServerConfigurationEntry extends Loggable {
    public static final String LDAP_ENTRY_PREFIX = "ldap";

    static final String OPEN_LDAP_ROOT_DSE = "OpenLDAProotDSE";
    static final String ACTIVE_DIRECTORY = "1.2.840.113556.1.4.800";
    static final String AD_V51_WIN_SERVER_2003 = "1.2.840.113556.1.4.1670";
    static final String AD_LDAP_INTEGRATION = "1.2.840.113556.1.4.1791";
    static final String AD_V60_WIN_SERVER_2008 = "1.2.840.113556.1.4.1935";
    static final String AD_V61R2_WIN_SERVER_2008_R2 = "1.2.840.113556.1.4.2080";
    static final String AD_WINDOWS_8 = "1.2.840.113556.1.4.2237";

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_SERVER_URL = "serverURL";
    private static final String PROPERTY_SEARCH_USERNAME = "searchUsername";
    private static final String PROPERTY_PASSWORD_OBF = "passwordObf";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_SEARCH_BASE = "searchBase";
    private static final String PROPERTY_SYNC_TYPE = "syncType";
    private static final String PROPERTY_SYNC_TIME = "syncTime";
    private static final String PROPERTY_ORG_DEPTH = "orgDepth";
    private static final String PROPERTY_MATCH_EMAIL = "matchEmail";
    private static final String PROPERTY_SYNC_GROUPS_ENABLED = "syncGroupsEnabled";
    private static final String PROPERTY_GROUPS_EXPRESSION = "groupsExpression";
    private static final String PROPERTY_GROUPS_MEMBER = "groupsMember";
    private static final String PROPERTY_GROUPS_MEMBER_OF = "groupsMemberOf";
    private static final String PROPERTY_SEARCH_EXPRESSION = "searchExpression";
    private static final String PROPERTY_IMPORT_EXPRESSION = "importExpression";
    private static final String PROPERTY_MAPPING_MAIL = "mappingMail";
    private static final String PROPERTY_MAPPING_USERNAME = "mappingUsername";
    private static final String PROPERTY_MAPPING_GIVEN_NAME = "mappingGivenName";
    private static final String PROPERTY_MAPPING_COMMON_NAME = "mappingCommonName";
    private static final String PROPERTY_MAPPING_MIDDLE_NAME = "mappingMiddleName";
    private static final String PROPERTY_MAPPING_SURNAME = "mappingSurname";
    private static final String PROPERTY_MAPPING_DISPLAY_NAME = "mappingDisplayName";
    private static final String PROPERTY_MAPPING_TELEPHONE = "mappingTelephone";
    private static final String PROPERTY_MAPPING_EXPIRATION = "mappingExpiration";
    private static final String PROPERTY_MAPPING_VALID_FROM = "mappingValidFrom";
    private static final String PROPERTY_MAPPING_QUOTA = "mappingQuota";
    private static final String PROPERTY_QUOTA_UNIT = "quotaUnit";
    private static final String PROPERTY_USERNAME_SUFFIXES = "usernameSuffixes";
    private static final String PROPERTY_SERVER_TYPE = "serverType";

    private final int index;
    private final Properties properties;

    /**
     * Create a new LDAP server configuration with an {@code index}. The index
     * is used for storing to the properties file and for the order in the UI.
     *
     * @param index
     * @param properties
     *     The configuration to store new values. May be {@code null}, then the
     *     properties will not be changed.
     */
    public LDAPServerConfigurationEntry(int index, Properties properties) {
        this.index = index;
        this.properties = properties;
    }

    /**
     * A name to be shown in the UI
     */
    @ConfigurationEntryExtension(name = "server.name")
    private String name;

    /**
     * Replacement of LDAP_SERVER_URL("ldap.server.url")
     * <br /><br />
     * Examples of valid LDAP URLs:
     * <p>
     * <ul>
     * <li><code>ldap://localhost/</code> -- LDAP server running on localhost on
     * standard port (389).</li>
     * <li><code>ldap://example.com:1234/</code> -- LDAP server running on
     * example.com using non-standard port.</li>
     * <li><code>ldaps://localhost/</code> -- Secure (TLS) LDAP server running
     * on localhost on standard port (636).</li>
     * <li><code>ldaps://example.com:9876/</code> -- Secure (TLS) LDAP server
     * running on example.com using non-standard port.</li>
     * <li><code>ldap://example.com/dc=example,dc=com</code> -- LDAP server
     * running on example.com using standard port with a custom base DN.</li>
     * </ul>
     */
    @ConfigurationEntryExtension(name = "server.url")
    private String serverURL;

    /**
     * Replacement of LDAP_SEARCH_USERNAME("ldap.search.username")
     * <br /><br />
     * #2133 the user to use for LDAP search queries.
     */
    @ConfigurationEntryExtension(name = "search.username")
    private String searchUsername;

    /**
     * Replacement of LDAP_SEARCH_PASSWORD_OBF("ldap.search.passwordobf")
     * <br /><br />
     * #2133 the password to use for LDAP search queries.
     */
    @ConfigurationEntryExtension(name = "search.passwordobf")
    private String passwordObf;

    /**
     * Replacement of LDAP_SEARCH_PASSWORD("ldap.search.password")
     * <br /><br />
     * #2133 the password to use for LDAP search queries.
     */
    @ConfigurationEntryExtension(name = "search.password")
    private String password;

    /**
     * Replacement of LDAP_SEARCH_BASE("ldap.search.base", "dc=company,dc=local")
     * <br /><br />
     * the search base to start the search for entries. e.g.
     * "dc=powerfolder, dc=com"
     */
    @DefaultValue(stringValue = "dc=company,dc=local")
    @ConfigurationEntryExtension(name = "search.base")
    private String searchBase;

    /**
     * Replacement of LDAP_SYNC_TYPE("ldap.sync.type", 2)
     * <br /><br />
     * The type of automatic LDAP synchronization.
     * 0 = No accounts
     * 1 = Only existing accounts
     * 2 = All accounts
     */
    @DefaultValue(intValue = 2)
    @ConfigurationEntryExtension(name = "sync.type")
    private Integer syncType;

    /**
     * Replacement of LDAP_SYNC_TIME("ldap.sync.time", 0)
     * <br /><br />
     * The time interval for LDAP synchronization in hours.
     */
    @DefaultValue(intValue = 0)
    @ConfigurationEntryExtension(name = "sync.time", oldName = "sync.enabled", oldType = Boolean.class, migrationMethodName = "migrateSyncTime")
    private Integer syncTime;

    public void migrateSyncTime(Boolean oldValue) {
        if (oldValue) {
            syncTime = 1;
        } else {
            syncTime = 0;
        }
    }

    /**
     * Replacement of LDAP_ORGANIZATION_DN_FIELD_DEPTH("ldap.search.org.depth", 0)
     * <br /><br />
     * The number of commas, that separate the attributes in the distinguished
     * name until the organization's attribute is reached.
     */
    @DefaultValue(intValue = 0)
    @ConfigurationEntryExtension(name = "search.org.depth")
    private Integer orgDepth;

    /**
     * Replacement of LDAP_ACCOUNTS_MATCH_EMAIL("ldap.accounts.match_email", true)
     * <br /><br />
     * Enable to merge accounts according to their email address
     */
    @DefaultValue(booleanValue = true)
    @ConfigurationEntryExtension(name = "accounts.match_email")
    private Boolean matchEmail;

    /**
     * Replacement of LDAP_SYNCHRONIZE_GROUPS("ldap.sync_groups.enabled", false)
     * <br /><br />
     * Enable automatic synchronization of LDAP/AD Groups.
     */
    @DefaultValue(booleanValue = false)
    @ConfigurationEntryExtension(name = "sync_groups.enabled")
    private Boolean syncGroupsEnabled;

    /**
     * Replacement of LDAP_SEARCH_FILTER_GROUPS("ldap.search.expression.groups",
     * "(|(objectClass=group)(objectClass=groupOfNames)(objectCategory=group))")
     * <br /><br />
     * The field, that contains the information, if the entry is describing a
     * group.
     */
    @DefaultValue(stringValue = "(|(objectClass=group)(objectClass=groupOfNames)(objectCategory=group))")
    @ConfigurationEntryExtension(name = "search.expression.groups")
    private String groupsExpression;

    /**
     * Replacement of LDAP_SEARCH_GROUPS_MEMBER("ldap.search.groups.member", "member")
     * <br /><br />
     * The name of the identifier for members in a group.
     */
    @DefaultValue(stringValue = "member")
    @ConfigurationEntryExtension(name = "search.groups.member")
    private String groupsMember;

    /**
     * Replacement of LDAP_SEARCH_GROUPS_MEMBER_OF("ldap.search.groups.member_of", "memberOf")
     * <br /><br />
     * The name of the identifier for groups, that an account is member of.
     */
    @DefaultValue(stringValue = "memberOf")
    @ConfigurationEntryExtension(name = "search.groups.member_of")
    private String groupsMemberOf;

    /**
     * Replacement of LDAP_SEARCH_FILTER_EXPRESSIONS(
     *  "ldap.search.expression",
     *   "(|(sAMAccountName=$username)(mail=$username)(userPrincipalName=$username)(uid=$username)(distinguishedName=$username))")
     * <br /><br />
     * The expression to use to resolve the DN (distinguished name). use
     * $username
     */
    @DefaultValue(stringValue = "(|(sAMAccountName=$username)(mail=$username)(userPrincipalName=$username)(uid=$username)(distinguishedName=$username))")
    @ConfigurationEntryExtension(name = "search.expression")
    private String searchExpression;

    /**
     * Replacement of LDAP_IMPORT_FILTER_EXPRESSION("ldap.import.expression")
     * <br /><br />
     * PFS-1539 An LDAP search filter for the import such as
     * "(objectClass=person)" or "(&(objectClass=inetOrgPerson)(org=Sales))"
     */
    @ConfigurationEntryExtension(name = "import.expression")
    private String importExpression;

    /**
     * Replacement of LDAP_SEARCH_EMAIL_ADDRESSES("ldap.search.mail_addresses", "mailAddresses,proxyAddresses")
     * <br /><br />
     * Comma separated list of entries, that contain mail addresses
     */
    @DefaultValue(stringValue = "mailAddresses,proxyAddresses")
    @ConfigurationEntryExtension(name = "mapping.mail_addresses", oldName = "search.mail_addresses")
    private String mappingMail;

    // PFS-1619: Start
    /**
     * Replacement of LDAP_SEARCH_ACCOUNTNAME("ldap.search.account_name", "sAMAccountName,uid")
     * <br /><br />
     * Mapping of username
     */
    @DefaultValue(stringValue = "sAMAccountName,uid")
    @ConfigurationEntryExtension(name = "mapping.username", oldName = "search.account_name")
    private String mappingUsername;

    /**
     * Replacement of LDAP_SEARCH_GIVENNAME("ldap.search.given_name", "givenName")
     * <br /><br />
     * Mapping of given name
     */
    @DefaultValue(stringValue = "givenName")
    @ConfigurationEntryExtension(name = "mapping.given_name", oldName = "search.given_name")
    private String mappingGivenName;

    /**
     * Replacement of LDAP_SEARCH_COMMONNAME("ldap.search.common_name", "cn,commonName")
     * <br /><br />
     * Mapping of common name
     */
    @DefaultValue(stringValue = "cn,commonName")
    @ConfigurationEntryExtension(name = "mapping.common_name", oldName = "search.common_name")
    private String mappingCommonName;

    /**
     * Replacement of LDAP_SEARCH_MIDDLENAME("ldap.search.middle_name", "middleName")
     * <br /><br />
     * Mapping of middle name
     */
    @DefaultValue(stringValue = "middleName")
    @ConfigurationEntryExtension(name = "mapping.middle_name", oldName = "search.middle_name")
    private String mappingMiddleName;

    /**
     * Replacement of LDAP_SEARCH_SURNAME("ldap.search.surname", "sn,surname")
     * <br /><br />
     * Mapping of surname
     */
    @DefaultValue(stringValue = "sn,surname")
    @ConfigurationEntryExtension(name = "mapping.surname", oldName = "search.surname")
    private String mappingSurname;

    /**
     * Replacement of LDAP_SEARCH_DISPLAYNAME("ldap.search.display_name", "displayName,name")
     * <br /><br />
     * Mapping of display name
     */
    @DefaultValue(stringValue = "displayName,name")
    @ConfigurationEntryExtension(name = "mapping.display_name", oldName = "search.display_name")
    private String mappingDisplayName;

    /**
     * Replacement of LDAP_SEARCH_TELEPHONE("ldap.search.telephone", "mobileTelephoneNumber,telephoneNumber,mobile")
     * <br /><br />
     * Mapping of telephone number
     */
    @DefaultValue(stringValue = "mobileTelephoneNumber,telephoneNumber,mobile")
    @ConfigurationEntryExtension(name = "mapping.telephone", oldName = "search.telephone")
    private String mappingTelephone;


    /**
     * Replacement of LDAP_SEARCH_EXPIRATION("ldap.search.expiration", "accountExpires")
     * <br /><br />
     * Mapping of expiration date
     */
    @DefaultValue(stringValue = "accountExpires")
    @ConfigurationEntryExtension(name = "mapping.expiration", oldName = "search.expiration")
    private String mappingExpiration;

    /**
     * Replacement of LDAP_SEARCH_VALID_FROM("ldap.search.valid_from", "validFrom")
     * <br /><br />
     * PFS-1647: Mapping of valid from date
     */
    @DefaultValue(stringValue = "validFrom")
    @ConfigurationEntryExtension(name = "mapping.valid_from", oldName = "search.valid_from")
    private String mappingValidFrom;

    /**
     * Replacement of LDAP_SEARCH_QUOTA("ldap.search.quota", "quota")
     * <br /><br />
     * Mapping of quota
     */
    @DefaultValue(stringValue = "quota")
    @ConfigurationEntryExtension(name = "mapping.quota", oldName = "search.quota")
    private String mappingQuota;

    /**
     * Replacement of LDAP_QUOTA_UNIT("ldap.quota.unit", "GB")
     * <br /><br />
     *
     */
    @DefaultValue(stringValue = "GB")
    @ConfigurationEntryExtension(name = "mapping.quota.unit", oldName = "quota.unit")
    private String quotaUnit;

    /**
     * List of username suffixes (domains) that are associated with this LDAP Server
     */
    @ConfigurationEntryExtension(name = "username_suffixes")
    private String usernameSuffixes;

    private LDAPType serverType;


    // -- Getter and Setter ----------------------------------------------------

    public int getIndex() {
        return index;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        return (String) getDefaultValue(PROPERTY_NAME);
    }


    public void setName(String name) {
        setValueForExtensionToConfig(PROPERTY_NAME, name);
        this.name = name;
    }

    public String getServerURL() {
        if (serverURL != null) {
            return serverURL;
        }

        return (String) getDefaultValue(PROPERTY_SERVER_URL);
    }

    public void setServerURL(String serverURL) {
        setValueForExtensionToConfig(PROPERTY_SERVER_URL, serverURL);
        this.serverURL = serverURL;
    }

    public String getSearchUsername() {
        if (searchUsername != null) {
            return searchUsername;
        }

        return (String) getDefaultValue(PROPERTY_SEARCH_USERNAME);
    }

    public void setSearchUsername(String searchUsername) {
        setValueForExtensionToConfig(PROPERTY_SEARCH_USERNAME, searchUsername);
        this.searchUsername = searchUsername;
    }

    public String getPasswordObf() {
        if (passwordObf != null) {
            return passwordObf;
        }

        return (String) getDefaultValue(PROPERTY_PASSWORD_OBF);
    }

    public void setPasswordObf(String passwordObf) {
        setValueForExtensionToConfig(PROPERTY_PASSWORD_OBF, passwordObf);
        this.passwordObf = passwordObf;
    }

    public String getPassword() {
        if (password != null) {
            return password;
        }

        return (String) getDefaultValue(PROPERTY_PASSWORD);
    }

    public void setPassword(String password) {
        if (password != null) {
            setPasswordObf(LoginUtil.obfuscate(password.toCharArray()));
        } else {
            setValueForExtensionToConfig(PROPERTY_PASSWORD, password);
        }
        this.password = password;
    }

    public String getSearchBase() {
        if (searchBase != null) {
            return searchBase;
        }

        return (String) getDefaultValue(PROPERTY_SEARCH_BASE);
    }

    public void setSearchBase(String searchBase) {
        setValueForExtensionToConfig(PROPERTY_SEARCH_BASE, searchBase);
        this.searchBase = searchBase;
    }

    public int getSyncType() {
        if (syncType != null) {
            return syncType;
        }

        return (Integer) getDefaultValue(PROPERTY_SYNC_TYPE);
    }

    public void setSyncType(int syncType) {
        setValueForExtensionToConfig(PROPERTY_SYNC_TYPE, syncType);
        this.syncType = syncType;
    }

    public int getSyncTime() {
        if (syncTime != null) {
            return syncTime;
        }

        return (Integer) getDefaultValue(PROPERTY_SYNC_TIME);
    }

    public void setSyncTime(int syncTime) {
        setValueForExtensionToConfig(PROPERTY_SYNC_TIME, syncTime);
        this.syncTime = syncTime;
    }

    public int getOrgDepth() {
        if (orgDepth != null) {
            return orgDepth;
        }

        return (Integer) getDefaultValue(PROPERTY_ORG_DEPTH);
    }

    public void setOrgDepth(int orgDepth) {
        setValueForExtensionToConfig(PROPERTY_ORG_DEPTH, orgDepth);
        this.orgDepth = orgDepth;
    }

    public boolean isMatchEmail() {
        if (matchEmail != null) {
            return matchEmail;
        }

        return (Boolean) getDefaultValue(PROPERTY_MATCH_EMAIL);
    }

    public void setMatchEmail(boolean matchEmail) {
        setValueForExtensionToConfig(PROPERTY_MATCH_EMAIL, matchEmail);
        this.matchEmail = matchEmail;
    }

    public boolean isSyncGroupsEnabled() {
        if (syncGroupsEnabled != null) {
            return syncGroupsEnabled;
        }

        return (Boolean) getDefaultValue(PROPERTY_SYNC_GROUPS_ENABLED);
    }

    public void setSyncGroupsEnabled(boolean syncGroupsEnabled) {
        setValueForExtensionToConfig(PROPERTY_SYNC_GROUPS_ENABLED, syncGroupsEnabled);
        this.syncGroupsEnabled = syncGroupsEnabled;
    }

    public String getGroupsExpression() {
        if (groupsExpression != null) {
            return groupsExpression;
        }

        return (String) getDefaultValue(PROPERTY_GROUPS_EXPRESSION);
    }

    public void setGroupsExpression(String groupsExpression) {
        setValueForExtensionToConfig(PROPERTY_GROUPS_EXPRESSION, groupsExpression);
        this.groupsExpression = groupsExpression;
    }

    public String getGroupsMember() {
        if (groupsMember != null) {
            return groupsMember;
        }

        return (String) getDefaultValue(PROPERTY_GROUPS_MEMBER);
    }

    public void setGroupsMember(String groupsMember) {
        setValueForExtensionToConfig(PROPERTY_GROUPS_MEMBER, groupsMember);
        this.groupsMember = groupsMember;
    }

    public String getGroupsMemberOf() {
        if (groupsMemberOf != null) {
            return groupsMemberOf;
        }

        return (String) getDefaultValue(PROPERTY_GROUPS_MEMBER_OF);
    }

    public void setGroupsMemberOf(String groupsMemberOf) {
        setValueForExtensionToConfig(PROPERTY_GROUPS_MEMBER_OF, groupsMemberOf);
        this.groupsMemberOf = groupsMemberOf;
    }

    public String getSearchExpression() {
        if (searchExpression != null) {
            return searchExpression;
        }

        return (String) getDefaultValue(PROPERTY_SEARCH_EXPRESSION);
    }

    public void setSearchExpression(String searchExpression) {
        setValueForExtensionToConfig(PROPERTY_SEARCH_EXPRESSION, searchExpression);
        this.searchExpression = searchExpression;
    }

    public String getImportExpression() {
        if (importExpression != null) {
            return importExpression;
        }

        return (String) getDefaultValue(PROPERTY_IMPORT_EXPRESSION);
    }

    public void setImportExpression(String importExpression) {
        setValueForExtensionToConfig(PROPERTY_IMPORT_EXPRESSION, importExpression);
        this.importExpression = importExpression;
    }

    public String getMappingMail() {
        if (mappingMail != null) {
            return mappingMail;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_MAIL);
    }

    public void setMappingMail(String mappingMail) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_MAIL, mappingMail);
        this.mappingMail = mappingMail;
    }

    public String getMappingUsername() {
        if (mappingUsername != null) {
            return mappingUsername;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_USERNAME);
    }

    public void setMappingUsername(String mappingUsername) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_USERNAME, mappingUsername);
        this.mappingUsername = mappingUsername;
    }

    public String getMappingGivenName() {
        if (mappingGivenName != null) {
            return mappingGivenName;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_GIVEN_NAME);
    }

    public void setMappingGivenName(String mappingGivenName) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_GIVEN_NAME, mappingGivenName);
        this.mappingGivenName = mappingGivenName;
    }

    public String getMappingCommonName() {
        if (mappingCommonName != null) {
            return mappingCommonName;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_COMMON_NAME);
    }

    public void setMappingCommonName(String mappingCommonName) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_COMMON_NAME, mappingCommonName);
        this.mappingCommonName = mappingCommonName;
    }

    public String getMappingMiddleName() {
        if (mappingMiddleName != null) {
            return mappingMiddleName;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_MIDDLE_NAME);
    }

    public void setMappingMiddleName(String mappingMiddleName) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_MIDDLE_NAME, mappingMiddleName);
        this.mappingMiddleName = mappingMiddleName;
    }

    public String getMappingSurname() {
        if (mappingSurname != null) {
            return mappingSurname;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_SURNAME);
    }

    public void setMappingSurname(String mappingSurname) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_SURNAME, mappingSurname);
        this.mappingSurname = mappingSurname;
    }

    public String getMappingDisplayName() {
        if (mappingDisplayName != null) {
            return mappingDisplayName;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_DISPLAY_NAME);
    }

    public void setMappingDisplayName(String mappingDisplayName) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_DISPLAY_NAME, mappingDisplayName);
        this.mappingDisplayName = mappingDisplayName;
    }

    public String getMappingTelephone() {
        if (mappingTelephone != null) {
            return mappingTelephone;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_TELEPHONE);
    }

    public void setMappingTelephone(String mappingTelephone) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_TELEPHONE, mappingTelephone);
        this.mappingTelephone = mappingTelephone;
    }

    public String getMappingExpiration() {
        if (mappingExpiration != null) {
            return mappingExpiration;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_EXPIRATION);
    }

    public void setMappingExpiration(String mappingExpiration) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_EXPIRATION, mappingExpiration);
        this.mappingExpiration = mappingExpiration;
    }

    public String getMappingValidFrom() {
        if (mappingValidFrom != null) {
            return mappingValidFrom;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_VALID_FROM);
    }

    public void setMappingValidFrom(String mappingValidFrom) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_VALID_FROM, mappingValidFrom);
        this.mappingValidFrom = mappingValidFrom;
    }

    public String getMappingQuota() {
        if (mappingQuota != null) {
            return mappingQuota;
        }

        return (String) getDefaultValue(PROPERTY_MAPPING_QUOTA);
    }

    public void setMappingQuota(String mappingQuota) {
        setValueForExtensionToConfig(PROPERTY_MAPPING_QUOTA, mappingQuota);
        this.mappingQuota = mappingQuota;
    }

    public String getQuotaUnit() {
        if (quotaUnit != null) {
            return quotaUnit;
        }

        return (String) getDefaultValue(PROPERTY_QUOTA_UNIT);
    }

    public void setQuotaUnit(String quotaUnit) {
        setValueForExtensionToConfig(PROPERTY_QUOTA_UNIT, quotaUnit);
        this.quotaUnit = quotaUnit;
    }

    public Set<String> getUsernameSuffixes() {
        String suffixes = "";
        if (usernameSuffixes == null) {
            suffixes = (String) getDefaultValue(PROPERTY_USERNAME_SUFFIXES);

            if (suffixes == null) {
                return new HashSet<>();
            }
        }

        suffixes = usernameSuffixes;

        Set<String> result = new HashSet<>();
        for (String suffix : suffixes.split(",")) {
            result.add(suffix);
        }

        return result;
    }

    public void setUsernameSuffixes(String usernameSuffixes) {
        setValueForExtensionToConfig(PROPERTY_USERNAME_SUFFIXES, usernameSuffixes);
        this.usernameSuffixes = usernameSuffixes;
    }

    public LDAPType getServerType() {
        return serverType;
    }


    // -- Helper methods -------------------------------------------------------

    /**
     * Return the default value as specified with {@link DefaultValue} of a
     * member of {@link LDAPServerConfigurationEntry}.
     *
     * @param memberName
     *     The name of a field/member of this class.
     * @return The default value as specified in the {@link DefaultValue}
     * annotation or {@code null} if either there is no member {@code
     * memberName} or there is no default value.
     */
    Object getDefaultValue(String memberName) {
        Field field;
        try {
            field = this.getClass().getDeclaredField(memberName);
        } catch (NoSuchFieldException nsfe) {
            logWarning("Could not find field by name " + memberName + " to get default value. " + nsfe);
            return null;
        }

        DefaultValue dv = field.getAnnotation(DefaultValue.class);
        if (dv == null) {
            logFine("No default value for " + memberName);
            return null;
        }

        if (field.getType() == String.class) {
            return dv.stringValue();
        } else if (field.getType() == Boolean.class) {
            return dv.booleanValue();
        } else if (field.getType() == Integer.class) {
            return dv.intValue();
        }

        return null;
    }

    /**
     * Update the properties when setting a new value for a member.
     *
     * @param memberName The name of the member to set.
     * @param value The value to set
     */
    void setValueForExtensionToConfig(String memberName, Object value) {
        if (properties == null) {
            return;
        }

        Field field;
        try {
            field = this.getClass().getDeclaredField(memberName);
        } catch (NoSuchFieldException nsfe) {
            logWarning("Could not find field by name " + memberName + " to set new value. " + nsfe);
            return;
        }

        ConfigurationEntryExtension cee = field.getDeclaredAnnotation(
            ConfigurationEntryExtension.class);
        if (cee == null) {
            logFine("No configuration entry extension for " + memberName);
            return;
        }

        String key = LDAP_ENTRY_PREFIX + "." + index + "." + cee.name();

        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value.toString());
        }
    }

    /**
     * Find out what LDAP server {@code ldapServerURL} is.<br />
     * <br />
     * To find out there are some base attributes, that give a hint. The search
     * has to be a search to base node {@code ""} (empty string). The scope has
     * to be {@code base}. <br />
     * <br />
     * If queried for {@code structuralObjectClass} an OpenLDAP server will
     * return
     * <ul>
     * <li>{@code structuralObjectClass: OpenLDAProotDSE}</li>
     * </ul>
     * <br />
     * Where as an Active Directory can be asked for
     * {@code supportedCapabilities} and contains on of
     * <ul>
     * <li>
     * {@code supportedCapabilities: 1.2.840.113556.1.4.800  (Active Directory)}
     * </li>
     * <li>
     * {@code supportedCapabilities: 1.2.840.113556.1.4.1670 (Active Directory V51: Windows Server® 2003)}
     * </li>
     * <li>
     * {@code supportedCapabilities: 1.2.840.113556.1.4.1791 (Active Directory LDAP Integration: signing and sealing on an NTLM authenticated connection)}
     * </li>
     * <li>
     * {@code supportedCapabilities: 1.2.840.113556.1.4.1935 (Active Directory V60: Server® 2008)}
     * </li>
     * <li>
     * {@code supportedCapabilities: 1.2.840.113556.1.4.2080 (Active Directory V61R2: Windows Server® 2008 R2)}
     * </li>
     * </ul>
     *
     */
    public void populateLDAPType()
    {
        DirContext ctx = null;
        try {
            ctx = getDirContext(serverURL, searchUsername,
                LoginUtil.deobfuscate(passwordObf).toString());

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.OBJECT_SCOPE);
            sc.setReturningAttributes(new String[]{"supportedCapabilities",
                "structuralObjectClass"});

            NamingEnumeration<SearchResult> results = ctx.search("",
                "objectClass=*", sc);

            while (results.hasMoreElements()) {
                SearchResult result = results.nextElement();
                Attributes attrs = result.getAttributes();

                Attribute strucObjClass = attrs.get("structuralObjectClass");
                if (strucObjClass != null) {
                    String sOC = strucObjClass.toString();
                    if (sOC != null
                        && sOC.trim().toLowerCase()
                        .contains(OPEN_LDAP_ROOT_DSE.trim().toLowerCase()))
                    {
                        serverType = LDAPType.OPEN_LDAP;
                        break;
                    }
                }

                Attribute supCaps = attrs.get("supportedCapabilities");
                if (supCaps != null) {
                    NamingEnumeration<?> all = supCaps.getAll();
                    String allSupportedCapabilities = "";
                    while (all.hasMoreElements()) {
                        String supCap = all.nextElement().toString();
                        if (supCap == null) {
                            continue;
                        }
                        allSupportedCapabilities += supCap + ",";
                    }

                    if (allSupportedCapabilities.contains(ACTIVE_DIRECTORY)
                        || allSupportedCapabilities.contains(AD_V51_WIN_SERVER_2003)
                        || allSupportedCapabilities.contains(AD_V60_WIN_SERVER_2008)
                        || allSupportedCapabilities.contains(AD_V61R2_WIN_SERVER_2008_R2)
                        || allSupportedCapabilities.contains(AD_WINDOWS_8)
                        || allSupportedCapabilities.contains(AD_LDAP_INTEGRATION))
                    {
                        serverType = LDAPType.ACTIVE_DIRECTORY;
                        break;
                    }
                }
            }
        } catch (NamingException ne) {
            // NOP
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ne) {
                    // NOP
                }
            }
        }
        serverType = LDAPType.UNKNOWN;
    }

    /**
     * Generic method to obtain a reference to a DirContext. Anonymous LDAP
     * login.
     *
     * @param ldapServerURL
     *            the URL of the LDAP server.
     * @param searchUsername
     *            #2133 the user to use for LDAP search queries
     * @param searchPassword
     * @return a DirContext for the specified server.
     * @throws NamingException
     *             if a naming exception is encountered.
     */
    private DirContext getDirContext(String ldapServerURL,
        String searchUsername, String searchPassword) throws NamingException
    {
        Reject.ifBlank(ldapServerURL, "LDAP Server url blank");
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");
        if (ldapServerURL != null && !ldapServerURL.startsWith("ldap")) {
            env.put(Context.PROVIDER_URL, "ldap://" + ldapServerURL);
        } else {
            env.put(Context.PROVIDER_URL, ldapServerURL);
        }
        if (StringUtils.isNotBlank(searchUsername)) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, searchUsername);
            env.put(Context.SECURITY_CREDENTIALS, searchPassword);
        }

        env.putAll(System.getProperties());

        // Create the initial context
        return new InitialDirContext(env);
    }

    public enum LDAPType {
        ACTIVE_DIRECTORY, OPEN_LDAP, UNKNOWN;
    }
}
