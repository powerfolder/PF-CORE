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
package de.dal33t.powerfolder.security;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class PermissionTest {
    @Test
    public void testAdminPermission() {
        Permission p = AdminPermission.INSTANCE;
        assertEquals( p.getId(),
            AdminPermission.class.getSimpleName(),"AdminPermission IDs not equal");
    }

    @Test
    public void testChangePreferencesPermission() {
        Permission p = ChangePreferencesPermission.INSTANCE;
        assertEquals( p.getId(),
            ChangePreferencesPermission.class.getSimpleName(),"ChangePreferencesPermission IDs not equal");
    }

    @Test
    public void testChangeTransferModePermission() {
        Permission p = ChangeTransferModePermission.INSTANCE;
        assertEquals( p.getId(),
            ChangeTransferModePermission.class.getSimpleName(),"ChangeTransferModePermission IDs not equal");
    }

    @Test
    public void testFolderCreatePermission() {
        Permission p = FolderCreatePermission.INSTANCE;
        assertEquals( p.getId(),
            FolderCreatePermission.class.getSimpleName(),"FolderCreatePermission IDs not equal");
    }

    @Test
    public void testFolderRemovePermission() {
        Permission p = FolderRemovePermission.INSTANCE;
        assertEquals( p.getId(),
            FolderRemovePermission.class.getSimpleName(),"FolderRemovePermission IDs not equal");
    }

    @Test
    public void testFolderAdminPermission() {
        Permission fap = new FolderAdminPermission(FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
        assertEquals( fap.getId(),
            "4711_FP_FolderAdminPermission","FolderAdminPermission IDs not equal");
    }

    @Test
    public void testFolderOwnerPermission() {
        FolderOwnerPermission fap = new FolderOwnerPermission(FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
        assertEquals( fap.getId(),
            "4711_FP_FolderOwnerPermission","FolderOwnerPermission IDs not equal");
    }

    @Test
    public void testFolderReadPermission() {
        FolderReadPermission fap = new FolderReadPermission(FolderInfoFactory.newTopFolderForTest(
            "myFolder", "4711"));
        assertEquals( fap.getId(),
            "4711_FP_FolderReadPermission","FolderReadPermission IDs not equal");
    }

    @Test
    public void testFolderReadWritePermission() {
        FolderReadWritePermission fap = new FolderReadWritePermission(
            FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
        assertEquals( fap.getId(),
            "4711_FP_FolderReadWritePermission","FolderReadWritePermission IDs not equal");
    }

    /**
     * Folder owner must be singular, therefore no other Permission should imply
     * FolderOwnerPermission.
     */
    @Test
    public void testFolderOwnerIndependence() {
        Organization org = new Organization();
        Group grp = new Group("testGroup");
        Account acc = new Account();
        acc.addGroup(grp);
        acc.setOrganizationOID(org.getOID());
        FolderInfo foInfo = FolderInfoFactory.backupFolderOfAccountForTest("testFolder", acc.createInfo());
        Permission[] allPermissions = new Permission[] {
            new FolderAdminPermission(foInfo),
            new FolderReadPermission(foInfo),
            new FolderReadWritePermission(foInfo),
            new GroupAdminPermission(grp),
            new OrganizationAdminPermission(org.getOID()),
            AdminPermission.INSTANCE,
            ChangePreferencesPermission.INSTANCE,
            ChangeTransferModePermission.INSTANCE,
            ComputersAppPermission.INSTANCE,
            ConfigAppPermission.INSTANCE,
            FolderCreatePermission.INSTANCE,
            FolderRemovePermission.INSTANCE,
            SystemSettingsPermission.INSTANCE
        };
        Permission ownerPermission = new FolderOwnerPermission(foInfo);
        for (Permission p : allPermissions) {
            assertFalse(
                p.implies(ownerPermission),p.getClass().getName() + " implies " + FolderOwnerPermission.class.getName());
        }
    }

    @Test
    public void testAllFoldersAdminPermission() {
        Account account;
        account = new AnonymousAccount();
        account.setUsername("ACCOUNT");
        account.grant(AdminPermission.INSTANCE);
        account.grant(AllFoldersAdminPermission.INSTANCE);
        assertTrue(account.hasAdminPermission(FolderInfoFactory.newTopFolderForTest("xx", "21")));
    }
}
