package de.dal33t.powerfolder.security;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class PermissionTest extends TestCase {
    public void testAdminPermission() {
        Permission p = AdminPermission.INSTANCE;
        assertEquals("AdminPermission IDs not equal", p.getId(),
            AdminPermission.class.getSimpleName());
    }

    public void testChangePreferencesPermission() {
        Permission p = ChangePreferencesPermission.INSTANCE;
        assertEquals("ChangePreferencesPermission IDs not equal", p.getId(),
            ChangePreferencesPermission.class.getSimpleName());
    }

    public void testChangeTransferModePermission() {
        Permission p = ChangeTransferModePermission.INSTANCE;
        assertEquals("ChangeTransferModePermission IDs not equal", p.getId(),
            ChangeTransferModePermission.class.getSimpleName());
    }

    public void testFolderCreatePermission() {
        Permission p = FolderCreatePermission.INSTANCE;
        assertEquals("FolderCreatePermission IDs not equal", p.getId(),
            FolderCreatePermission.class.getSimpleName());
    }

    public void testFolderRemovePermission() {
        Permission p = FolderRemovePermission.INSTANCE;
        assertEquals("FolderRemovePermission IDs not equal", p.getId(),
            FolderRemovePermission.class.getSimpleName());
    }

    public void testFolderAdminPermission() {
        Permission fap = new FolderAdminPermission(new FolderInfo("myFolder",
            "4711"));
        assertEquals("FolderAdminPermission IDs not equal", fap.getId(),
            "4711_FP_FolderAdminPermission");
    }

    public void testFolderOwnerPermission() {
        FolderOwnerPermission fap = new FolderOwnerPermission(new FolderInfo(
            "myFolder", "4711"));
        assertEquals("FolderOwnerPermission IDs not equal", fap.getId(),
            "4711_FP_FolderOwnerPermission");
    }

    public void testFolderReadPermission() {
        FolderReadPermission fap = new FolderReadPermission(new FolderInfo(
            "myFolder", "4711"));
        assertEquals("FolderReadPermission IDs not equal", fap.getId(),
            "4711_FP_FolderReadPermission");
    }

    public void testFolderReadWritePermission() {
        FolderReadWritePermission fap = new FolderReadWritePermission(
            new FolderInfo("myFolder", "4711"));
        assertEquals("FolderReadWritePermission IDs not equal", fap.getId(),
            "4711_FP_FolderReadWritePermission");
    }

    /**
     * Folder owner must be singular, therefore no other Permission should imply
     * FolderOwnerPermission.
     */
    public void testFolderOwnerIndependence() {
        Organization org = new Organization();
        Group grp = new Group("testGroup");
        Account acc = new Account();
        acc.addGroup(grp);
        acc.setOrganizationOID(org.getOID());
        FolderInfo foInfo = new FolderInfo("testFolder", acc.createInfo());
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
            assertFalse(p.getClass().getName() + " implies " + FolderOwnerPermission.class.getName(),
                p.implies(ownerPermission));
        }
    }
}
