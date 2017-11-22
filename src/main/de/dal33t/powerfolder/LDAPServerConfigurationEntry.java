package de.dal33t.powerfolder;

import java.util.HashSet;
import java.util.Set;

/**
 * This class is a replacement for the LDAP-Configuration Entries previously in
 * {@link de.dal33t.powerfolder.ConfigurationServerEntry}
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @since 11.5 SP 5
 */
public class LDAPServerConfigurationEntry {
    public static final String LDAP_ENTRY_PREFIX = "ldap";

    private final int index;

    public LDAPServerConfigurationEntry(int index) {
        this.index = index;
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
     * Replacement of LDAP_SYNCHRONIZE_STRUCTURE("ldap.sync.enabled", false)
     * <br /><br />
     * Enable automatic synchronization of LDAP/AD. (deprecated)
     */
    @DefaultValue(booleanValue = false)
    @ConfigurationEntryExtension(name = "sync.enabled")
    private Boolean syncEnabled;

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
    @ConfigurationEntryExtension(name = "sync.time", oldName = "sync.enabled", oldType = Boolean.class)
    private Integer syncTime;
    /*LDAP_SYNC_TIME("ldap.sync.time", 0) {

        // Backward compatibility
        @Override
        public String getValue(Controller controller) {
            // Compare LDAP_SYNCHRONIZE_STRUCTURE and LDAP_SYNC_TYPE
            // If the values differ LDAP_SYNCHRONIZE_STRUCTURE wins
            // (setValue sets both variables to the same value, so this means
            // setValue has not been called yet and we have to import the old
            // settings)
            String value = super.getValue(controller);
            boolean oldValue = ConfigurationServerEntry.LDAP_SYNCHRONIZE_STRUCTURE
                .getValueBoolean(controller);
            // If LDAP_SYNCHRONIZE_STRUCTURE is false and LDAP_SYNC_TYPE is not,
            // return false
            if (oldValue == false && !value.equals("0")) {
                ConfigurationServerEntry.LDAP_SYNC_TIME.setValue(controller, "0");
                return "0";
            }
            // If LDAP_SYNCHRONIZE_STRUCTURE is true and LDAP_SYNC_TYPE is not,
            // return true
            if (oldValue == true && value.equals("0")) {
                ConfigurationServerEntry.LDAP_SYNC_TIME.setValue(controller, "1");
                return "1";
            }
            return value;
        }

        // Forward compatibility
        // When setting LDAP_SYNC_TIME also set LDAP_SYNCHRONIZE_STRUCTURE accordingly
        @Override
        public void setValue(Controller controller, String value) {
            super.setValue(controller, value);
            if (value.equals("0")) {
                ConfigurationServerEntry.LDAP_SYNCHRONIZE_STRUCTURE.setValue(controller, false);
            }
            else {
                ConfigurationServerEntry.LDAP_SYNCHRONIZE_STRUCTURE.setValue(controller, true);
            }
        }

        // Forward compatibility
        // When setting LDAP_SYNC_TIME also set LDAP_SYNCHRONIZE_STRUCTURE accordingly
        @Override
        public void setValue(Controller controller, int value) {
            super.setValue(controller, value);
            if (value == 0) {
                ConfigurationServerEntry.LDAP_SYNCHRONIZE_STRUCTURE.setValue(controller, false);
            }
            else {
                ConfigurationServerEntry.LDAP_SYNCHRONIZE_STRUCTURE.setValue(controller, true);
            }
        }

    },*/

    /**
     * Replacement of LDAP_ORGANIZATION_DN_FIELD_DEPTH("ldap.search.org.depth", 0)
     * <br /><br />
     * The number of commas, that separate the attributes in the destingushed
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
    private Boolean enableSyncGroups;

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

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getSearchUsername() {
        return searchUsername;
    }

    public void setSearchUsername(String searchUsername) {
        this.searchUsername = searchUsername;
    }

    public String getPasswordObf() {
        return passwordObf;
    }

    public void setPasswordObf(String passwordObf) {
        this.passwordObf = passwordObf;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public int getSyncType() {
        return syncType;
    }

    public void setSyncType(int syncType) {
        this.syncType = syncType;
    }

    public int getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(int syncTime) {
        this.syncTime = syncTime;
    }

    public int getOrgDepth() {
        return orgDepth;
    }

    public void setOrgDepth(int orgDepth) {
        this.orgDepth = orgDepth;
    }

    public boolean isMatchEmail() {
        return matchEmail;
    }

    public void setMatchEmail(boolean matchEmail) {
        this.matchEmail = matchEmail;
    }

    public boolean isSyncGroupsEnabled() {
        return enableSyncGroups;
    }

    public void setEnableSyncGroups(boolean enableSyncGroups) {
        this.enableSyncGroups = enableSyncGroups;
    }

    public String getGroupsExpression() {
        return groupsExpression;
    }

    public void setGroupsExpression(String groupsExpression) {
        this.groupsExpression = groupsExpression;
    }

    public String getGroupsMember() {
        return groupsMember;
    }

    public void setGroupsMember(String groupsMember) {
        this.groupsMember = groupsMember;
    }

    public String getGroupsMemberOf() {
        return groupsMemberOf;
    }

    public void setGroupsMemberOf(String groupsMemberOf) {
        this.groupsMemberOf = groupsMemberOf;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public void setSearchExpression(String searchExpression) {
        this.searchExpression = searchExpression;
    }

    public String getImportExpression() {
        return importExpression;
    }

    public void setImportExpression(String importExpression) {
        this.importExpression = importExpression;
    }

    public String getMappingMail() {
        return mappingMail;
    }

    public void setMappingMail(String mappingMail) {
        this.mappingMail = mappingMail;
    }

    public String getMappingUsername() {
        return mappingUsername;
    }

    public void setMappingUsername(String mappingUsername) {
        this.mappingUsername = mappingUsername;
    }

    public String getMappingGivenName() {
        return mappingGivenName;
    }

    public void setMappingGivenName(String mappingGivenName) {
        this.mappingGivenName = mappingGivenName;
    }

    public String getMappingCommonName() {
        return mappingCommonName;
    }

    public void setMappingCommonName(String mappingCommonName) {
        this.mappingCommonName = mappingCommonName;
    }

    public String getMappingMiddleName() {
        return mappingMiddleName;
    }

    public void setMappingMiddleName(String mappingMiddleName) {
        this.mappingMiddleName = mappingMiddleName;
    }

    public String getMappingSurname() {
        return mappingSurname;
    }

    public void setMappingSurname(String mappingSurname) {
        this.mappingSurname = mappingSurname;
    }

    public String getMappingDisplayName() {
        return mappingDisplayName;
    }

    public void setMappingDisplayName(String mappingDisplayName) {
        this.mappingDisplayName = mappingDisplayName;
    }

    public String getMappingTelephone() {
        return mappingTelephone;
    }

    public void setMappingTelephone(String mappingTelephone) {
        this.mappingTelephone = mappingTelephone;
    }

    public String getMappingExpiration() {
        return mappingExpiration;
    }

    public void setMappingExpiration(String mappingExpiration) {
        this.mappingExpiration = mappingExpiration;
    }

    public String getMappingValidFrom() {
        return mappingValidFrom;
    }

    public void setMappingValidFrom(String mappingValidFrom) {
        this.mappingValidFrom = mappingValidFrom;
    }

    public String getMappingQuota() {
        return mappingQuota;
    }

    public void setMappingQuota(String mappingQuota) {
        this.mappingQuota = mappingQuota;
    }

    public String getQuotaUnit() {
        return quotaUnit;
    }

    public void setQuotaUnit(String quotaUnit) {
        this.quotaUnit = quotaUnit;
    }

    public Set<String> getUsernameSuffixes() {
        String[] suffixes = new String[0];
        if (usernameSuffixes != null) {
            suffixes = usernameSuffixes.split(",");
        }
        Set<String> result = new HashSet<>();
        for (String suffix : suffixes) {
            result.add(suffix);
        }

        return result;
    }

    public void setUsernameSuffixes(String usernameSuffixes) {
        this.usernameSuffixes = usernameSuffixes;
    }

    private Object getDefaultValue() {
        
    }
}
