package de.dal33t.powerfolder.security;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
 */
public class PermissionTest extends TestCase {
    public void testAdminPermission() {
        Permission p = AdminPermission.INSTANCE;
        assertEquals("AdminPermission IDs not equal", p.getId(), AdminPermission.class.getSimpleName());
    }
    
    public void testChangePreferencesPermission() {
        Permission p = ChangePreferencesPermission.INSTANCE;
        assertEquals("ChangePreferencesPermission IDs not equal", p.getId(), ChangePreferencesPermission.class.getSimpleName());
    }
    
    public void testChangeTransferModePermission() {
        Permission p = ChangeTransferModePermission.INSTANCE;
        assertEquals("ChangeTransferModePermission IDs not equal", p.getId(), ChangeTransferModePermission.class.getSimpleName());
    }
    
    public void testFolderCreatePermission() {
        Permission p = FolderCreatePermission.INSTANCE;
        assertEquals("FolderCreatePermission IDs not equal", p.getId(), FolderCreatePermission.class.getSimpleName());
    }
    
    public void testFolderRemovePermission() {
        Permission p = FolderRemovePermission.INSTANCE;
        assertEquals("FolderRemovePermission IDs not equal", p.getId(), FolderRemovePermission.class.getSimpleName());
    }

    public void testFolderAdminPermission() {
        Permission fap = new FolderAdminPermission(new FolderInfo("myFolder", "4711"));
        assertEquals("FolderAdminPermission IDs not equal", fap.getId(), "4711_FolderAdminPermission");
    }

    public void testFolderOwnerPermission() {
        FolderOwnerPermission fap = new FolderOwnerPermission(new FolderInfo("myFolder", "4711"));
        assertEquals("FolderOwnerPermission IDs not equal", fap.getId(), "4711_FolderOwnerPermission");
    }

    public void testFolderReadPermission() {
        FolderReadPermission fap = new FolderReadPermission(new FolderInfo("myFolder", "4711"));
        assertEquals("FolderReadPermission IDs not equal", fap.getId(), "4711_FolderReadPermission");
    }

    public void testFolderReadWritePermission() {
        FolderReadWritePermission fap = new FolderReadWritePermission(new FolderInfo("myFolder", "4711"));
        assertEquals("FolderReadWritePermission IDs not equal", fap.getId(), "4711_FolderReadWritePermission");
    }
}
