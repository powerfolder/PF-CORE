/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
package de.dal33t.powerfolder.ui.cloudproviders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

/**
 * Unit tests for the pure logic of the Linux libcloudproviders integration
 * (PFC-3643). These cover the sync-state mapping, the D-Bus property maps, the
 * ObjectManager layout and the provider registration file - none of which need a
 * live session bus or a running controller. The D-Bus connection/export code
 * itself requires on-device (GNOME/KDE) QA and is intentionally not unit-tested.
 */
public class CloudProvidersIntegrationTest {

    // ---- CloudProvidersAccountStatus enum values (must match libcloudproviders) ----

    @Test
    public void statusEnumValuesMatchSpec() {
        assertEquals(0, CloudProvidersIntegration.STATUS_INVALID);
        assertEquals(1, CloudProvidersIntegration.STATUS_IDLE);
        assertEquals(2, CloudProvidersIntegration.STATUS_SYNCING);
        assertEquals(3, CloudProvidersIntegration.STATUS_ERROR);
    }

    // ---- statusFor() mapping ----

    @Test
    public void statusForNotConnectedIsInvalid() {
        assertEquals(CloudProvidersIntegration.STATUS_INVALID,
            CloudProvidersIntegration.statusFor(false, false));
        // Not connected wins even if a folder appears to be syncing.
        assertEquals(CloudProvidersIntegration.STATUS_INVALID,
            CloudProvidersIntegration.statusFor(false, true));
    }

    @Test
    public void statusForConnectedAndSyncingIsSyncing() {
        assertEquals(CloudProvidersIntegration.STATUS_SYNCING,
            CloudProvidersIntegration.statusFor(true, true));
    }

    @Test
    public void statusForConnectedAndInSyncIsIdle() {
        assertEquals(CloudProvidersIntegration.STATUS_IDLE,
            CloudProvidersIntegration.statusFor(true, false));
    }

    // ---- Provider properties ----

    @Test
    public void providerPropertiesExposeName() {
        Map<String, Variant<?>> p =
            CloudProvidersIntegration.providerProperties("PowerFolder");
        assertEquals(1, p.size());
        assertNotNull(p.get("Name"));
        assertEquals("PowerFolder", p.get("Name").getValue());
    }

    // ---- Account properties ----

    @Test
    public void accountPropertiesExposeAllFieldsWithCorrectTypes() {
        Map<String, Variant<?>> a = CloudProvidersIntegration.accountProperties(
            "LRZ Sync+Share", "/home/user/PowerFolders", "powerfolder",
            CloudProvidersIntegration.STATUS_SYNCING, "Uploading 3 files");

        assertEquals(5, a.size());
        assertEquals("LRZ Sync+Share", a.get("Name").getValue());
        assertEquals("/home/user/PowerFolders", a.get("Path").getValue());
        assertEquals("powerfolder", a.get("Icon").getValue());
        assertEquals("Uploading 3 files", a.get("StatusDetails").getValue());

        // Status must marshal as D-Bus "i" -> an Integer, value 2 (syncing).
        Object status = a.get("Status").getValue();
        assertTrue("Status must be an Integer (D-Bus 'i')",
            status instanceof Integer);
        assertEquals(Integer.valueOf(CloudProvidersIntegration.STATUS_SYNCING),
            status);
    }

    // ---- ObjectManager.GetManagedObjects layout ----

    @Test
    public void managedObjectsContainProviderAndAccount() {
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> managed =
            CloudProvidersIntegration.managedObjects("PowerFolder",
                "/home/user/PowerFolders", "powerfolder",
                CloudProvidersIntegration.STATUS_IDLE, "");

        assertEquals("Exactly the provider + account objects", 2,
            managed.size());

        boolean foundProvider = false;
        boolean foundAccount = false;
        for (Map<String, Map<String, Variant<?>>> ifaces : managed.values()) {
            if (ifaces.containsKey(CloudProvidersIntegration.PROVIDER_IFACE)) {
                foundProvider = true;
                Map<String, Variant<?>> props =
                    ifaces.get(CloudProvidersIntegration.PROVIDER_IFACE);
                assertEquals("PowerFolder", props.get("Name").getValue());
            }
            if (ifaces.containsKey(CloudProvidersIntegration.ACCOUNT_IFACE)) {
                foundAccount = true;
                Map<String, Variant<?>> props =
                    ifaces.get(CloudProvidersIntegration.ACCOUNT_IFACE);
                assertEquals(5, props.size());
                assertEquals("/home/user/PowerFolders",
                    props.get("Path").getValue());
                assertEquals(
                    Integer.valueOf(CloudProvidersIntegration.STATUS_IDLE),
                    props.get("Status").getValue());
            }
        }
        assertTrue("Provider object present", foundProvider);
        assertTrue("Account object present", foundAccount);
    }

    @Test
    public void managedObjectPathsAreDistinctChildrenOfBase() {
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> managed =
            CloudProvidersIntegration.managedObjects("PowerFolder", "/p", "i",
                CloudProvidersIntegration.STATUS_IDLE, "");
        boolean provider = false;
        boolean account = false;
        for (DBusPath path : managed.keySet()) {
            String p = path.getPath();
            assertTrue("Managed objects must be under the base path: " + p,
                p.startsWith(CloudProvidersIntegration.BASE_PATH + "/"));
            if (p.equals(CloudProvidersIntegration.PROVIDER_PATH)) {
                provider = true;
            }
            if (p.equals(CloudProvidersIntegration.ACCOUNT_PATH)) {
                account = true;
            }
        }
        assertTrue(provider);
        assertTrue(account);
    }

    // ---- Interface names + object paths ----

    @Test
    public void dbusInterfaceNamesMatchSpec() {
        assertEquals("org.freedesktop.CloudProviders.Provider",
            CloudProvidersIntegration.PROVIDER_IFACE);
        assertEquals("org.freedesktop.CloudProviders.Account",
            CloudProvidersIntegration.ACCOUNT_IFACE);
    }

    // ---- Provider registration file content ----

    @Test
    public void providerFileContentHasCorrectKeyFileFormat() {
        String content = CloudProvidersIntegration.providerFileContent(
            "de.dal33t.powerfolder.CloudProvider",
            "/de/dal33t/powerfolder/CloudProvider");

        assertTrue("must start with the group header",
            content.startsWith("[Cloud Providers]\n"));
        assertTrue(content.contains(
            "\nBusName=de.dal33t.powerfolder.CloudProvider\n"));
        assertTrue(content.contains(
            "\nObjectPath=/de/dal33t/powerfolder/CloudProvider\n"));
        // No stray keys.
        assertFalse(content.toLowerCase().contains("password"));
    }
}
