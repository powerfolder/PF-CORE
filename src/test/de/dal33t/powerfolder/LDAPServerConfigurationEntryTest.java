package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.LoginUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:krickl@powerfolder.com>Maximilian Krickl</a>
 */
public class LDAPServerConfigurationEntryTest {

    LDAPServerConfigurationEntry ldapConfig;
    Properties properties;

    @Before
    public void setUp() {
        properties = new Properties();
        ldapConfig = new LDAPServerConfigurationEntry(0, properties);
    }

    @Test
    public void testGetDefaultValue() {

        assertNull(ldapConfig.getDefaultValue("name"));
        assertEquals(2, ldapConfig.getDefaultValue("syncType"));
        assertEquals(true, ldapConfig.getDefaultValue("matchEmail"));
        assertEquals("quota", ldapConfig.getDefaultValue("mappingQuota"));
    }

    @Test
    public void testDefaultValues() {
        assertNull(ldapConfig.getName());
        assertNull(ldapConfig.getServerURL());
        assertNull(ldapConfig.getSearchUsername());
        assertNull(ldapConfig.getPasswordObf());
        assertNull(ldapConfig.getPassword());

        assertEquals("dc=company,dc=local", ldapConfig.getSearchBase());
        assertEquals(2, ldapConfig.getSyncType());
        assertEquals(0, ldapConfig.getSyncTime());
        assertEquals(0, ldapConfig.getOrgDepth());
        assertEquals(true, ldapConfig.isMatchEmail());
        assertEquals(false, ldapConfig.isSyncGroupsEnabled());
        assertEquals("(|(objectClass=group)(objectClass=groupOfNames)(objectCategory=group))", ldapConfig.getGroupsExpression());
        assertEquals("member", ldapConfig.getGroupsMember());
        assertEquals("memberOf", ldapConfig.getGroupsMemberOf());
        assertEquals("(|(sAMAccountName=$username)(mail=$username)(userPrincipalName=$username)(uid=$username)(distinguishedName=$username))", ldapConfig.getSearchExpression());;

        assertNull(ldapConfig.getImportExpression());

        assertEquals("mailAddresses,proxyAddresses", ldapConfig.getMappingMail());
        assertEquals("sAMAccountName,uid", ldapConfig.getMappingUsername());
        assertEquals("givenName", ldapConfig.getMappingGivenName());
        assertEquals("cn,commonName", ldapConfig.getMappingCommonName());
        assertEquals("middleName", ldapConfig.getMappingMiddleName());
        assertEquals("sn,surname", ldapConfig.getMappingSurname());
        assertEquals("displayName,name", ldapConfig.getMappingDisplayName());
        assertEquals("mobileTelephoneNumber,telephoneNumber,mobile", ldapConfig.getMappingTelephone());
        assertEquals("accountExpires", ldapConfig.getMappingExpiration());
        assertEquals("validFrom", ldapConfig.getMappingValidFrom());
        assertEquals("quota", ldapConfig.getMappingQuota());
        assertEquals("GB", ldapConfig.getQuotaUnit());

        assertEquals(0, ldapConfig.getUsernameSuffixes().size());
    }

    @Test
    public void testChangedValue() {
        // prerequisites
        assertEquals(2, ldapConfig.getSyncType());
        assertEquals(true, ldapConfig.isMatchEmail());
        assertEquals("accountExpires", ldapConfig.getMappingExpiration());
        assertEquals(0, ldapConfig.getUsernameSuffixes().size());
        assertNull(ldapConfig.getImportExpression());

        // change values
        ldapConfig.setSyncType(1);
        ldapConfig.setMatchEmail(false);
        ldapConfig.setMappingExpiration("expires");
        ldapConfig.setUsernameSuffixes("example.org,sub.example.org,example.com");
        ldapConfig.setImportExpression("(mail=$username)");
        ldapConfig.setName("");

        // check if the set values are coming back
        assertEquals(1, ldapConfig.getSyncType());
        assertEquals(false, ldapConfig.isMatchEmail());
        assertEquals("expires", ldapConfig.getMappingExpiration());
        assertEquals(3, ldapConfig.getUsernameSuffixes().size());
        assertEquals("(mail=$username)", ldapConfig.getImportExpression());
        assertEquals("", ldapConfig.getName());
    }

    @Test
    public void testSetValueForExtensionToConfig() {
        assertFalse(properties.containsKey("ldap.0.server.name"));
        ldapConfig.setName("newName");
        assertTrue(properties.containsKey("ldap.0.server.name"));
        assertEquals("newName", properties.getProperty("ldap.0.server.name"));

        assertFalse(properties.containsKey("ldap.0.server.url"));
        ldapConfig.setServerURL("newURL");
        assertTrue(properties.containsKey("ldap.0.server.url"));
        assertEquals("newURL", properties.getProperty("ldap.0.server.url"));

        assertFalse(properties.containsKey("ldap.0.search.username"));
        ldapConfig.setSearchUsername("newUsername");
        assertTrue(properties.containsKey("ldap.0.search.username"));
        assertEquals("newUsername", properties.getProperty("ldap.0.search.username"));

        assertFalse(properties.containsKey("ldap.0.search.passwordobf"));
        ldapConfig.setPasswordObf("newPasswordObf");
        assertTrue(properties.containsKey("ldap.0.search.passwordobf"));
        assertEquals("newPasswordObf", properties.getProperty("ldap.0.search.passwordobf"));

        // the plain text password gets obfuscated, stored to passwordobj and is not stored to the config file
        properties.remove("ldap.0.search.passwordobf");
        assertFalse(properties.containsKey("ldap.0.search.passwordobf"));
        assertFalse(properties.containsKey("ldap.0.search.password"));
        ldapConfig.setPassword("abc123");
        assertFalse(properties.containsKey("ldap.0.search.password"));
        assertTrue(properties.containsKey("ldap.0.search.passwordobf"));
        assertEquals(LoginUtil.obfuscate("abc123".toCharArray()), properties.getProperty("ldap.0.search.passwordobf"));

        assertFalse(properties.containsKey("ldap.0.search.base"));
        ldapConfig.setSearchBase("newSearchBase");
        assertTrue(properties.containsKey("ldap.0.search.base"));
        assertEquals("newSearchBase", properties.getProperty("ldap.0.search.base"));

        assertFalse(properties.containsKey("ldap.0.sync.type"));
        ldapConfig.setSyncType(1);
        assertTrue(properties.containsKey("ldap.0.sync.type"));
        assertEquals(Integer.valueOf(1), Integer.valueOf(properties.getProperty("ldap.0.sync.type")));

        assertFalse(properties.containsKey("ldap.0.sync.time"));
        ldapConfig.setSyncTime(5);
        assertTrue(properties.containsKey("ldap.0.sync.time"));
        assertEquals(Integer.valueOf(5), Integer.valueOf(properties.getProperty("ldap.0.sync.time")));

        assertFalse(properties.containsKey("ldap.0.search.org.depth"));
        ldapConfig.setOrgDepth(82);
        assertTrue(properties.containsKey("ldap.0.search.org.depth"));
        assertEquals(Integer.valueOf(82), Integer.valueOf(properties.getProperty("ldap.0.search.org.depth")));

        assertFalse(properties.containsKey("ldap.0.accounts.match_email"));
        ldapConfig.setMatchEmail(false);
        assertTrue(properties.containsKey("ldap.0.accounts.match_email"));
        assertFalse(Boolean.valueOf(properties.getProperty("ldap.0.accounts.match_email")));

        assertFalse(properties.containsKey("ldap.0.sync_groups.enabled"));
        ldapConfig.setSyncGroupsEnabled(true);
        assertTrue(properties.containsKey("ldap.0.sync_groups.enabled"));
        assertTrue(Boolean.valueOf(properties.getProperty("ldap.0.sync_groups.enabled")));

        assertFalse(properties.containsKey("ldap.0.search.expression.groups"));
        ldapConfig.setGroupsExpression("newGroupsExpression");
        assertTrue(properties.containsKey("ldap.0.search.expression.groups"));
        assertEquals("newGroupsExpression", properties.getProperty("ldap.0.search.expression.groups"));

        assertFalse(properties.containsKey("ldap.0.search.groups.member"));
        ldapConfig.setGroupsMember("newMember");
        assertTrue(properties.containsKey("ldap.0.search.groups.member"));
        assertEquals("newMember", properties.getProperty("ldap.0.search.groups.member"));

        assertFalse(properties.containsKey("ldap.0.search.groups.member_of"));
        ldapConfig.setGroupsMemberOf("newMemberOf");
        assertTrue(properties.containsKey("ldap.0.search.groups.member_of"));
        assertEquals("newMemberOf", properties.getProperty("ldap.0.search.groups.member_of"));

        assertFalse(properties.containsKey("ldap.0.search.expression"));
        ldapConfig.setSearchExpression("newExpression");
        assertTrue(properties.containsKey("ldap.0.search.expression"));
        assertEquals("newExpression", properties.getProperty("ldap.0.search.expression"));

        assertFalse(properties.containsKey("ldap.0.import.expression"));
        ldapConfig.setImportExpression("newImportExpression");
        assertTrue(properties.containsKey("ldap.0.import.expression"));
        assertEquals("newImportExpression", properties.getProperty("ldap.0.import.expression"));

        assertFalse(properties.containsKey("ldap.0.mapping.mail_addresses"));
        ldapConfig.setMappingMail("newMappingMail");
        assertTrue(properties.containsKey("ldap.0.mapping.mail_addresses"));
        assertEquals("newMappingMail", properties.getProperty("ldap.0.mapping.mail_addresses"));

        assertFalse(properties.containsKey("ldap.0.mapping.username"));
        ldapConfig.setMappingUsername("newMappingUsername");
        assertTrue(properties.containsKey("ldap.0.mapping.username"));
        assertEquals("newMappingUsername", properties.getProperty("ldap.0.mapping.username"));

        assertFalse(properties.containsKey("ldap.0.mapping.given_name"));
        ldapConfig.setMappingGivenName("newMappingGivenName");
        assertTrue(properties.containsKey("ldap.0.mapping.given_name"));
        assertEquals("newMappingGivenName", properties.getProperty("ldap.0.mapping.given_name"));

        assertFalse(properties.containsKey("ldap.0.mapping.common_name"));
        ldapConfig.setMappingCommonName("newMappingCommonName");
        assertTrue(properties.containsKey("ldap.0.mapping.common_name"));
        assertEquals("newMappingCommonName", properties.getProperty("ldap.0.mapping.common_name"));

        assertFalse(properties.containsKey("ldap.0.mapping.middle_name"));
        ldapConfig.setMappingMiddleName("newMappingMiddleName");
        assertTrue(properties.containsKey("ldap.0.mapping.middle_name"));
        assertEquals("newMappingMiddleName", properties.getProperty("ldap.0.mapping.middle_name"));

        assertFalse(properties.containsKey("ldap.0.mapping.surname"));
        ldapConfig.setMappingSurname("newMappingSurname");
        assertTrue(properties.containsKey("ldap.0.mapping.surname"));
        assertEquals("newMappingSurname", properties.getProperty("ldap.0.mapping.surname"));

        assertFalse(properties.containsKey("ldap.0.mapping.display_name"));
        ldapConfig.setMappingDisplayName("newMappingDisplayName");
        assertTrue(properties.containsKey("ldap.0.mapping.display_name"));
        assertEquals("newMappingDisplayName", properties.getProperty("ldap.0.mapping.display_name"));

        assertFalse(properties.containsKey("ldap.0.mapping.telephone"));
        ldapConfig.setMappingTelephone("newMappingTelephone");
        assertTrue(properties.containsKey("ldap.0.mapping.telephone"));
        assertEquals("newMappingTelephone", properties.getProperty("ldap.0.mapping.telephone"));

        assertFalse(properties.containsKey("ldap.0.mapping.expiration"));
        ldapConfig.setMappingExpiration("newMappingExpiration");
        assertTrue(properties.containsKey("ldap.0.mapping.expiration"));
        assertEquals("newMappingExpiration", properties.getProperty("ldap.0.mapping.expiration"));

        assertFalse(properties.containsKey("ldap.0.mapping.valid_from"));
        ldapConfig.setMappingValidFrom("newMappingValidFrom");
        assertTrue(properties.containsKey("ldap.0.mapping.valid_from"));
        assertEquals("newMappingValidFrom", properties.getProperty("ldap.0.mapping.valid_from"));

        assertFalse(properties.containsKey("ldap.0.mapping.quota"));
        ldapConfig.setMappingQuota("newMappingQuota");
        assertTrue(properties.containsKey("ldap.0.mapping.quota"));
        assertEquals("newMappingQuota", properties.getProperty("ldap.0.mapping.quota"));

        assertFalse(properties.containsKey("ldap.0.mapping.quota.unit"));
        ldapConfig.setQuotaUnit("newMappingQuotaUnit");
        assertTrue(properties.containsKey("ldap.0.mapping.quota.unit"));
        assertEquals("newMappingQuotaUnit", properties.getProperty("ldap.0.mapping.quota.unit"));

        assertFalse(properties.containsKey("ldap.0.username_suffixes"));
        ldapConfig.setUsernameSuffixes("newUsernameSuffixes");
        assertTrue(properties.containsKey("ldap.0.username_suffixes"));
        assertEquals("newUsernameSuffixes", properties.getProperty("ldap.0.username_suffixes"));
    }
}
