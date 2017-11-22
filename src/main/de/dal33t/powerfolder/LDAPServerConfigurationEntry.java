package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.logging.Loggable;

import java.lang.reflect.Field;
import java.util.HashSet;
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

    private final int index;

    /**
     * Create a new LDAP server configuration with an {@code index}. The index
     * is used for storing to the config file and for the order in the UI.
     *
     * @param index
     */
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

    public int getIndex() {
        return index;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        return (String) getDefaultValue("name");
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getServerURL() {
        if (serverURL != null) {
            return serverURL;
        }

        return (String) getDefaultValue("serverURL");
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getSearchUsername() {
        if (searchUsername != null) {
            return searchUsername;
        }

        return (String) getDefaultValue("searchUsername");
    }

    public void setSearchUsername(String searchUsername) {
        this.searchUsername = searchUsername;
    }

    public String getPasswordObf() {
        if (passwordObf != null) {
            return passwordObf;
        }

        return (String) getDefaultValue("passwordObf");
    }

    public void setPasswordObf(String passwordObf) {
        this.passwordObf = passwordObf;
    }

    public String getPassword() {
        if (password != null) {
            return password;
        }

        return (String) getDefaultValue("password");
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSearchBase() {
        if (searchBase != null) {
            return searchBase;
        }

        return (String) getDefaultValue("searchBase");
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public boolean isSyncEnabled() {
        if (syncEnabled != null) {
            return syncEnabled;
        }

        return (Boolean) getDefaultValue("syncEnabled");
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public int getSyncType() {
        if (syncType != null) {
            return syncType;
        }

        return (Integer) getDefaultValue("syncType");
    }

    public void setSyncType(int syncType) {
        this.syncType = syncType;
    }

    public int getSyncTime() {
        if (syncTime != null) {
            return syncTime;
        }

        return (Integer) getDefaultValue("syncTime");
    }

    public void setSyncTime(int syncTime) {
        this.syncTime = syncTime;
    }

    public int getOrgDepth() {
        if (orgDepth != null) {
            return orgDepth;
        }

        return (Integer) getDefaultValue("orgDepth");
    }

    public void setOrgDepth(int orgDepth) {
        this.orgDepth = orgDepth;
    }

    public boolean isMatchEmail() {
        if (matchEmail != null) {
            return matchEmail;
        }

        return (Boolean) getDefaultValue("matchEmail");
    }

    public void setMatchEmail(boolean matchEmail) {
        this.matchEmail = matchEmail;
    }

    public boolean isSyncGroupsEnabled() {
        if (syncGroupsEnabled != null) {
            return syncGroupsEnabled;
        }

        return (Boolean) getDefaultValue("syncGroupsEnabled");
    }

    public void setSyncGroupsEnabled(boolean syncGroupsEnabled) {
        this.syncGroupsEnabled = syncGroupsEnabled;
    }

    public String getGroupsExpression() {
        if (groupsExpression != null) {
            return groupsExpression;
        }

        return (String) getDefaultValue("groupsExpression");
    }

    public void setGroupsExpression(String groupsExpression) {
        this.groupsExpression = groupsExpression;
    }

    public String getGroupsMember() {
        if (groupsMember != null) {
            return groupsMember;
        }

        return (String) getDefaultValue("groupsMember");
    }

    public void setGroupsMember(String groupsMember) {
        this.groupsMember = groupsMember;
    }

    public String getGroupsMemberOf() {
        if (groupsMemberOf != null) {
            return groupsMemberOf;
        }

        return (String) getDefaultValue("groupsMemberOf");
    }

    public void setGroupsMemberOf(String groupsMemberOf) {
        this.groupsMemberOf = groupsMemberOf;
    }

    public String getSearchExpression() {
        if (searchExpression != null) {
            return searchExpression;
        }

        return (String) getDefaultValue("searchExpression");
    }

    public void setSearchExpression(String searchExpression) {
        this.searchExpression = searchExpression;
    }

    public String getImportExpression() {
        if (importExpression != null) {
            return importExpression;
        }

        return (String) getDefaultValue("importExpression");
    }

    public void setImportExpression(String importExpression) {
        this.importExpression = importExpression;
    }

    public String getMappingMail() {
        if (mappingMail != null) {
            return mappingMail;
        }

        return (String) getDefaultValue("mappingMail");
    }

    public void setMappingMail(String mappingMail) {
        this.mappingMail = mappingMail;
    }

    public String getMappingUsername() {
        if (mappingUsername != null) {
            return mappingUsername;
        }

        return (String) getDefaultValue("mappingUsername");
    }

    public void setMappingUsername(String mappingUsername) {
        this.mappingUsername = mappingUsername;
    }

    public String getMappingGivenName() {
        if (mappingGivenName != null) {
            return mappingGivenName;
        }

        return (String) getDefaultValue("mappingGivenName");
    }

    public void setMappingGivenName(String mappingGivenName) {
        this.mappingGivenName = mappingGivenName;
    }

    public String getMappingCommonName() {
        if (mappingCommonName != null) {
            return mappingCommonName;
        }

        return (String) getDefaultValue("mappingCommonName");
    }

    public void setMappingCommonName(String mappingCommonName) {
        this.mappingCommonName = mappingCommonName;
    }

    public String getMappingMiddleName() {
        if (mappingMiddleName != null) {
            return mappingMiddleName;
        }

        return (String) getDefaultValue("mappingMiddleName");
    }

    public void setMappingMiddleName(String mappingMiddleName) {
        this.mappingMiddleName = mappingMiddleName;
    }

    public String getMappingSurname() {
        if (mappingSurname != null) {
            return mappingSurname;
        }

        return (String) getDefaultValue("mappingSurname");
    }

    public void setMappingSurname(String mappingSurname) {
        this.mappingSurname = mappingSurname;
    }

    public String getMappingDisplayName() {
        if (mappingDisplayName != null) {
            return mappingDisplayName;
        }

        return (String) getDefaultValue("mappingDisplayName");
    }

    public void setMappingDisplayName(String mappingDisplayName) {
        this.mappingDisplayName = mappingDisplayName;
    }

    public String getMappingTelephone() {
        if (mappingTelephone != null) {
            return mappingTelephone;
        }

        return (String) getDefaultValue("mappingTelephone");
    }

    public void setMappingTelephone(String mappingTelephone) {
        this.mappingTelephone = mappingTelephone;
    }

    public String getMappingExpiration() {
        if (mappingExpiration != null) {
            return mappingExpiration;
        }

        return (String) getDefaultValue("mappingExpiration");
    }

    public void setMappingExpiration(String mappingExpiration) {
        this.mappingExpiration = mappingExpiration;
    }

    public String getMappingValidFrom() {
        if (mappingValidFrom != null) {
            return mappingValidFrom;
        }

        return (String) getDefaultValue("mappingValidFrom");
    }

    public void setMappingValidFrom(String mappingValidFrom) {
        this.mappingValidFrom = mappingValidFrom;
    }

    public String getMappingQuota() {
        if (mappingQuota != null) {
            return mappingQuota;
        }

        return (String) getDefaultValue("mappingQuota");
    }

    public void setMappingQuota(String mappingQuota) {
        this.mappingQuota = mappingQuota;
    }

    public String getQuotaUnit() {
        if (quotaUnit != null) {
            return quotaUnit;
        }

        return (String) getDefaultValue("quotaUnit");
    }

    public void setQuotaUnit(String quotaUnit) {
        this.quotaUnit = quotaUnit;
    }

    public Set<String> getUsernameSuffixes() {
        String suffixes = "";
        if (usernameSuffixes == null) {
            suffixes = (String) getDefaultValue("usernameSuffixes");

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
        this.usernameSuffixes = usernameSuffixes;
    }

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
        } catch (NoSuchFieldException e) {
            logWarning("Could not find field by name " + memberName);
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
}
