package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.LDAPServerConfigurationEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:krickl@powerfolder.com>Maximilian Krickl</a>
 */
public class SplitConfigTest {

    private SplitConfig sc;

    @Before
    public void setUp() {
        sc = new SplitConfig();
        Logger.getGlobal().setLevel(Level.ALL);
    }

    @Test
    public void getIndexOfLDAPEntry() {
        assertEquals(3, sc.getIndexOfLDAPEntry("ldap.3.search.username"));
        assertEquals(6, sc.getIndexOfLDAPEntry("ldap.6.url"));
        assertEquals(981, sc.getIndexOfLDAPEntry("ldap.981.search.username"));
        assertEquals(2, sc.getIndexOfLDAPEntry("ldap.2"));
        assertEquals(-1, sc.getIndexOfLDAPEntry("ldap.search.username"));
        assertEquals(-1, sc.getIndexOfLDAPEntry("ldap"));
    }

    @Test
    public void getExtensionFromKey() {
        assertEquals("search.username",
            sc.getExtensionFromKey("ldap.3432.search.username"));
        assertEquals("search.username",
            sc.getExtensionFromKey("ldap.3.search.username"));
        assertEquals("ldap.search.username",
            sc.getExtensionFromKey("ldap.search.username"));
        assertEquals("", sc.getExtensionFromKey(null));
    }

    @Test
    public void addLDAPEntry() {
        // prerequisites
        assertNull(sc.getLDAPServer(0));

        // method to test
        sc.addLDAPEntry("ldap.0.search.username", "lecker@buetterchen.de");
        sc.addLDAPEntry("ldap.0.search.account_name", "sAMAccountName");
        sc.addLDAPEntry("ldap.3.sync_groups.enabled", true);

        // checking results
        assertEquals("lecker@buetterchen.de",
            sc.getLDAPServer(0).getSearchUsername());
        // check if renaming of entries works correctly
        assertEquals("sAMAccountName",
            sc.getLDAPServer(0).getMappingUsername());
        assertTrue(sc.getLDAPServer(3).isSyncGroupsEnabled());

        assertNull(sc.getLDAPServer(347));

        assertNull(sc.addLDAPEntry("ldap.NaN.search.username", "username"));
        assertNull(sc.addLDAPEntry("ldap.3.not.existing.extension", true));
    }

    @Test
    public void removeLDAPEntry() {
        assertNull(sc.removeLDAPEntry(0, "ldap.0.search.username"));

        // prerequisites
        sc.addLDAPEntry("ldap.0.search.username", "username");
        sc.addLDAPEntry("ldap.0.sync_groups.enabled", true);
        sc.addLDAPEntry("ldap.0.search.org.depth", 3);

        assertNull(sc.remove("ldap.3.search.username"));

        LDAPServerConfigurationEntry server = sc.getLDAPServer(0);

        assertEquals("username", server.getSearchUsername());
        assertEquals(true, server.isSyncGroupsEnabled());
        assertEquals(3, server.getOrgDepth());

        // method to test and checking results
        assertEquals("username", sc.remove("ldap.0.search.username"));
        assertEquals(true, sc.remove("ldap.0.sync_groups.enabled"));
        assertEquals(3, sc.remove("ldap.0.search.org.depth"));
        assertNull(sc.remove("ldap.0.not.existing.extension"));
    }

    @Test
    public void getLDAPServerConfigurationEntry() {
        // prerequisites
        //sc.
    }
}
