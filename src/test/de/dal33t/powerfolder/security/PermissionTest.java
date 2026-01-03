package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import junit.framework.TestCase;

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
        Permission fap = new FolderAdminPermission(FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
        assertEquals("FolderAdminPermission IDs not equal", fap.getId(),
            "4711_FP_FolderAdminPermission");
    }

    public void testFolderOwnerPermission() {
        FolderOwnerPermission fap = new FolderOwnerPermission(FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
        assertEquals("FolderOwnerPermission IDs not equal", fap.getId(),
            "4711_FP_FolderOwnerPermission");
    }

    public void testFolderReadPermission() {
        FolderReadPermission fap = new FolderReadPermission(FolderInfoFactory.newTopFolderForTest(
            "myFolder", "4711"));
        assertEquals("FolderReadPermission IDs not equal", fap.getId(),
            "4711_FP_FolderReadPermission");
    }

    public void testFolderReadWritePermission() {
        FolderReadWritePermission fap = new FolderReadWritePermission(
            FolderInfoFactory.newTopFolderForTest("myFolder", "4711"));
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
            assertFalse(p.getClass().getName() + " implies " + FolderOwnerPermission.class.getName(),
                p.implies(ownerPermission));
        }
    }

    public void testAllFoldersAdminPermission() {
        Account account;
        account = new AnonymousAccount();
        account.setUsername("ACCOUNT");
        account.grant(AdminPermission.INSTANCE);
        account.grant(AllFoldersAdminPermission.INSTANCE);
        assertTrue(account.hasAdminPermission(FolderInfoFactory.newTopFolderForTest("xx", "21")));
    }

    public void testFolderPermissionIdsUnified() {
        FolderInfo fi = FolderInfoFactory.newTopFolderForTest("myFolder", "4711");

        assertPermissionId(
                new FolderAdminPermission(fi),
                "4711_FP_FolderAdminPermission"
        );

        assertPermissionId(
                new FolderOwnerPermission(fi),
                "4711_FP_FolderOwnerPermission"
        );

        assertPermissionId(
                new FolderReadPermission(fi),
                "4711_FP_FolderReadPermission"
        );

        assertPermissionId(
                new FolderReadWritePermission(fi),
                "4711_FP_FolderReadWritePermission"
        );
    }

    private void assertPermissionId(Permission p, String expectedId) {
        assertEquals(
                p.getClass().getSimpleName() + " ID mismatch",
                expectedId,
                p.getId()
        );
    }
    public void testFolderPermissionHierarchy() {
        FolderInfo fi = FolderInfoFactory.newTopFolderForTest("folder", "42");

        Permission owner = new FolderOwnerPermission(fi);
        Permission admin = new FolderAdminPermission(fi);
        Permission readWrite = new FolderReadWritePermission(fi);
        Permission read = new FolderReadPermission(fi);

        assertTrue(owner.implies(admin));
        assertTrue(owner.implies(readWrite));
        assertTrue(owner.implies(read));

        assertTrue(admin.implies(readWrite));
        assertTrue(admin.implies(read));

        assertTrue(readWrite.implies(read));

        assertFalse(read.implies(readWrite));
        assertFalse(readWrite.implies(admin));
        assertFalse(admin.implies(owner));
    }

    public void testFolderPermissionIsolation() {
        FolderInfo fi1 = FolderInfoFactory.newTopFolderForTest("A", "1");
        FolderInfo fi2 = FolderInfoFactory.newTopFolderForTest("B", "2");

        Permission p1 = new FolderAdminPermission(fi1);
        Permission p2 = new FolderReadPermission(fi2);

        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

    public void testFolderPermissionSameIdDifferentInstance() {
        FolderInfo fi1 = FolderInfoFactory.newTopFolderForTest("folder", "99");
        FolderInfo fi2 = FolderInfoFactory.newTopFolderForTest("folder", "99");

        Permission p1 = new FolderReadPermission(fi1);
        Permission p2 = new FolderReadPermission(fi2);

        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));
    }

    public void testFolderOwnerImpliesAllFolderPermissions() {
        FolderInfo fi = FolderInfoFactory.newTopFolderForTest("folder", "7");

        Permission owner = new FolderOwnerPermission(fi);

        assertTrue(owner.implies(owner));
        assertTrue(owner.implies(new FolderAdminPermission(fi)));
        assertTrue(owner.implies(new FolderReadWritePermission(fi)));
        assertTrue(owner.implies(new FolderReadPermission(fi)));
    }

    public void testFolderPermissionImplicationMatrix() {
        FolderInfo fi = FolderInfoFactory.newTopFolderForTest("folder", "100");

        Permission owner = new FolderOwnerPermission(fi);
        Permission admin = new FolderAdminPermission(fi);
        Permission readWrite = new FolderReadWritePermission(fi);
        Permission read = new FolderReadPermission(fi);

        Permission[][] expectations = new Permission[][] {
                // source, target, shouldImply
                { owner, owner }, { owner, admin }, { owner, readWrite }, { owner, read },
                { admin, admin }, { admin, readWrite }, { admin, read },
                { readWrite, readWrite }, { readWrite, read },
                { read, read }
        };

        for (Permission[] pair : expectations) {
            Permission source = pair[0];
            Permission target = pair[1];
            assertTrue(
                    source.getClass().getSimpleName() + " should imply "
                            + target.getClass().getSimpleName(),
                    source.implies(target)
            );
        }

        // Explicit negative checks
        assertFalse(admin.implies(owner));
        assertFalse(readWrite.implies(admin));
        assertFalse(readWrite.implies(owner));
        assertFalse(read.implies(readWrite));
        assertFalse(read.implies(admin));
        assertFalse(read.implies(owner));
    }

    public void testFolderPermissionsDoNotCrossFolders() {
        FolderInfo fi1 = FolderInfoFactory.newTopFolderForTest("A", "1");
        FolderInfo fi2 = FolderInfoFactory.newTopFolderForTest("B", "2");

        Permission[] p1 = new Permission[] {
                new FolderOwnerPermission(fi1),
                new FolderAdminPermission(fi1),
                new FolderReadWritePermission(fi1),
                new FolderReadPermission(fi1)
        };

        Permission[] p2 = new Permission[] {
                new FolderOwnerPermission(fi2),
                new FolderAdminPermission(fi2),
                new FolderReadWritePermission(fi2),
                new FolderReadPermission(fi2)
        };

        for (Permission a : p1) {
            for (Permission b : p2) {
                assertFalse(
                        a.getClass().getSimpleName() + " must not imply "
                                + b.getClass().getSimpleName() + " of another folder",
                        a.implies(b)
                );
            }
        }
    }

    public void testAccountFolderPermissionResolution() {
        Account acc = new Account();
        FolderInfo fi = FolderInfoFactory.newTopFolderForTest("folder", "11");

        acc.grant(new FolderReadWritePermission(fi));

        assertTrue(acc.hasReadPermissions(fi));
        assertTrue(acc.hasReadWritePermissions(fi));
        assertFalse(acc.hasAdminPermission(fi));
        assertFalse(acc.hasOwnerPermission(fi));
    }

    public void testTopFolderPermissionImpliesSamePermissionOnSubfolder() {
        // --- given: top folder ---
        FolderInfo topFolder =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "TOP-INH-1");

        // --- and: subfolder created like runtime does ---
        FolderInfo subFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub")
                );

        // --- OWNER on top: implies all non-owner permissions on subfolder ---
        Permission topOwner = new FolderOwnerPermission(topFolder);

        assertTrue(topOwner.implies(new FolderAdminPermission(subFolder)));
        assertTrue(topOwner.implies(new FolderReadWritePermission(subFolder)));
        assertTrue(topOwner.implies(new FolderReadPermission(subFolder)));
        assertTrue(topOwner.implies(new FolderDeletePermission(subFolder)));

        // --- ADMIN on top: same permission on subfolder ---
        Permission topAdmin = new FolderAdminPermission(topFolder);
        assertTrue(topAdmin.implies(new FolderAdminPermission(subFolder)));

        // --- READ_WRITE on top ---
        Permission topReadWrite = new FolderReadWritePermission(topFolder);
        assertTrue(topReadWrite.implies(new FolderReadWritePermission(subFolder)));

        // --- READ on top ---
        Permission topRead = new FolderReadPermission(topFolder);
        assertTrue(topRead.implies(new FolderReadPermission(subFolder)));

        // --- DELETE on top ---
        Permission topDelete = new FolderDeletePermission(topFolder);
        assertTrue(topDelete.implies(new FolderDeletePermission(subFolder)));
    }

    public void testTopFolderAdminImpliesReadPermissionOnSubfolder() {
        // --- given: top folder ---
        FolderInfo topFolder =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "ADM-READ-1");

        // --- and: subfolder created like runtime does ---
        FolderInfo subFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub")
                );

        // --- when: admin permission on top folder ---
        Permission topAdmin =
                new FolderAdminPermission(topFolder);

        // --- then: read permission on subfolder must be implied ---
        Permission subRead =
                new FolderReadPermission(subFolder);

        assertTrue(
                "Admin permission on top folder must imply read permission on subfolder",
                topAdmin.implies(subRead)
        );
    }

    public void testTopFolderToSubfolderPermissionMatrix() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "MATRIX-NO-SUB-OWNER");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        // --- Top-level permissions ---
        Permission topOwner = new FolderOwnerPermission(top);
        Permission topAdmin = new FolderAdminPermission(top);
        Permission topReadWrite = new FolderReadWritePermission(top);
        Permission topRead = new FolderReadPermission(top);
        Permission topDelete = new FolderDeletePermission(top);

        // --- Subfolder permissions (NO OWNER!) ---
        Permission subAdmin = new FolderAdminPermission(sub);
        Permission subReadWrite = new FolderReadWritePermission(sub);
        Permission subRead = new FolderReadPermission(sub);
        Permission subDelete = new FolderDeletePermission(sub);

        // --- OWNER on top ---
        assertTrue(topOwner.implies(subAdmin));
        assertTrue(topOwner.implies(subReadWrite));
        assertTrue(topOwner.implies(subRead));
        assertTrue(topOwner.implies(subDelete));

        // --- ADMIN ---
        assertTrue(topAdmin.implies(subAdmin));
        assertTrue(topAdmin.implies(subReadWrite));
        assertTrue(topAdmin.implies(subRead));
        assertTrue(topAdmin.implies(subDelete));

        // --- READ_WRITE ---
        assertFalse(topReadWrite.implies(subAdmin));
        assertTrue(topReadWrite.implies(subReadWrite));
        assertTrue(topReadWrite.implies(subRead));
        assertFalse(topReadWrite.implies(subDelete));

        // --- READ ---
        assertFalse(topRead.implies(subAdmin));
        assertFalse(topRead.implies(subReadWrite));
        assertTrue(topRead.implies(subRead));
        assertFalse(topRead.implies(subDelete));

        // --- DELETE ---
        assertFalse(topDelete.implies(subAdmin));
        assertFalse(topDelete.implies(subReadWrite));
        assertFalse(topDelete.implies(subRead));
        assertTrue(topDelete.implies(subDelete));
    }

    public void testOwnerPermissionMustNotBeUsedOnSubfolder() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "NO-SUB-OWNER");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        try {
            new FolderOwnerPermission(sub);
            fail("FolderOwnerPermission must not be created for subfolders");
        } catch (Exception expected) {
            // correct
        }
    }

    public void testFolderDeletePermissionImpliesItself() {
        FolderInfo folder =
                FolderInfoFactory.newTopFolderForTest("Folder", "DEL-SELF");

        Permission delete = new FolderDeletePermission(folder);

        assertTrue(
                "FolderDeletePermission must imply itself",
                delete.implies(delete)
        );
    }

    public void testFolderDeletePermissionIsInheritedToSubfolder() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("Top", "DEL-INH-1");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        Permission topDelete = new FolderDeletePermission(top);
        Permission subDelete = new FolderDeletePermission(sub);

        assertTrue(
                "FolderDeletePermission must be inherited from top folder to subfolder",
                topDelete.implies(subDelete)
        );
    }


    public void testFolderDeletePermissionIsNotInheritedToParentFolder() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("Top", "DEL-UP");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        Permission subDelete = new FolderDeletePermission(sub);
        Permission topDelete = new FolderDeletePermission(top);

        assertFalse(
                "FolderDeletePermission must not be inherited from subfolder to parent folder",
                subDelete.implies(topDelete)
        );
    }

    public void testFolderDeletePermissionDoesNotCrossSiblingSubfolders() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("Top", "DEL-SIB");

        FolderInfo subA =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "subA")
                );

        FolderInfo subB =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "subB")
                );

        Permission deleteA = new FolderDeletePermission(subA);
        Permission deleteB = new FolderDeletePermission(subB);

        assertFalse(
                "FolderDeletePermission must not cross sibling subfolders",
                deleteA.implies(deleteB)
        );
    }

    public void testTopFolderPermissionIsInheritedToSubAndSubSubFolder() {
        // --- given: top folder ---
        FolderInfo topFolder =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "TOP-SUB-SUBSUB");

        // --- and: subfolder ---
        FolderInfo subFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub")
                );

        // --- and: sub-subfolder ---
        FolderInfo subSubFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub/deep")
                );

        // --- OWNER on top (strongest permission) ---
        Permission topOwner =
                new FolderOwnerPermission(topFolder);

        // OWNER must imply all non-owner permissions on subfolder
        assertTrue(topOwner.implies(new FolderAdminPermission(subFolder)));
        assertTrue(topOwner.implies(new FolderReadWritePermission(subFolder)));
        assertTrue(topOwner.implies(new FolderReadPermission(subFolder)));
        assertTrue(topOwner.implies(new FolderDeletePermission(subFolder)));

        // OWNER must imply all non-owner permissions on sub-subfolder
        assertTrue(topOwner.implies(new FolderAdminPermission(subSubFolder)));
        assertTrue(topOwner.implies(new FolderReadWritePermission(subSubFolder)));
        assertTrue(topOwner.implies(new FolderReadPermission(subSubFolder)));
        assertTrue(topOwner.implies(new FolderDeletePermission(subSubFolder)));

        // --- ADMIN on top ---
        Permission topAdmin =
                new FolderAdminPermission(topFolder);

        assertTrue(topAdmin.implies(new FolderAdminPermission(subSubFolder)));
        assertTrue(topAdmin.implies(new FolderReadWritePermission(subSubFolder)));
        assertTrue(topAdmin.implies(new FolderReadPermission(subSubFolder)));
        assertTrue(topAdmin.implies(new FolderDeletePermission(subSubFolder)));

        // --- READ_WRITE on top ---
        Permission topReadWrite =
                new FolderReadWritePermission(topFolder);

        assertTrue(topReadWrite.implies(new FolderReadWritePermission(subSubFolder)));
        assertTrue(topReadWrite.implies(new FolderReadPermission(subSubFolder)));
        assertFalse(topReadWrite.implies(new FolderDeletePermission(subSubFolder)));

        // --- READ on top ---
        Permission topRead =
                new FolderReadPermission(topFolder);

        assertTrue(topRead.implies(new FolderReadPermission(subSubFolder)));
        assertFalse(topRead.implies(new FolderReadWritePermission(subSubFolder)));
        assertFalse(topRead.implies(new FolderDeletePermission(subSubFolder)));

        // --- DELETE on top ---
        Permission topDelete =
                new FolderDeletePermission(topFolder);

        assertTrue(topDelete.implies(new FolderDeletePermission(subSubFolder)));
        assertFalse(topDelete.implies(new FolderReadPermission(subSubFolder)));
    }

    public void testSubfolderPermissionIsInheritedToSubSubFolder() {
        // --- given: top folder ---
        FolderInfo topFolder =
                FolderInfoFactory.newTopFolderForTest("TopFolder", "SUB-SUBSUB-1");

        // --- and: subfolder ---
        FolderInfo subFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub")
                );

        // --- and: sub-subfolder ---
        FolderInfo subSubFolder =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(topFolder, "sub/deep")
                );

        // --- ADMIN on subfolder ---
        Permission subAdmin =
                new FolderAdminPermission(subFolder);

        assertTrue(subAdmin.implies(new FolderAdminPermission(subSubFolder)));
        assertTrue(subAdmin.implies(new FolderReadWritePermission(subSubFolder)));
        assertTrue(subAdmin.implies(new FolderReadPermission(subSubFolder)));
        assertTrue(subAdmin.implies(new FolderDeletePermission(subSubFolder)));

        // --- READ_WRITE on subfolder ---
        Permission subReadWrite =
                new FolderReadWritePermission(subFolder);

        assertTrue(subReadWrite.implies(new FolderReadWritePermission(subSubFolder)));
        assertTrue(subReadWrite.implies(new FolderReadPermission(subSubFolder)));
        assertFalse(subReadWrite.implies(new FolderDeletePermission(subSubFolder)));

        // --- READ on subfolder ---
        Permission subRead =
                new FolderReadPermission(subFolder);

        assertTrue(subRead.implies(new FolderReadPermission(subSubFolder)));
        assertFalse(subRead.implies(new FolderReadWritePermission(subSubFolder)));
        assertFalse(subRead.implies(new FolderDeletePermission(subSubFolder)));

        // --- DELETE on subfolder ---
        Permission subDelete =
                new FolderDeletePermission(subFolder);

        assertTrue(subDelete.implies(new FolderDeletePermission(subSubFolder)));
        assertFalse(subDelete.implies(new FolderReadPermission(subSubFolder)));
    }

    public void testSubfolderDoesNotImplySimilarPrefixFolder() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("Top", "CC-1");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        FolderInfo subdir =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "subdir")
                );

        Permission subAdmin =
                new FolderAdminPermission(sub);

        assertFalse(
                "Subfolder 'sub' must not imply permissions on 'subdir'",
                subAdmin.implies(new FolderAdminPermission(subdir))
        );
    }

    public void testSubfolderImpliesNestedFolderWithSlashBoundary() {
        FolderInfo top =
                FolderInfoFactory.newTopFolderForTest("Top", "CC-2");

        FolderInfo sub =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub")
                );

        FolderInfo subDir =
                FolderInfoFactory.newFolder(
                        FileInfoFactory.lookupDirectory(top, "sub/dir")
                );

        Permission subAdmin =
                new FolderAdminPermission(sub);

        assertTrue(
                "Subfolder 'sub' must imply permissions on 'sub/dir'",
                subAdmin.implies(new FolderAdminPermission(subDir))
        );
    }


    private FolderInfo createTopFolder(String id) {
        return FolderInfoFactory.newTopFolderForTest("TopFolder", id);
    }

    private FolderInfo createSubFolder(FolderInfo topFolder, String relativePath) {
        // Wichtig: relativePath enthält den kompletten Pfad im Topfolder
        DirectoryInfo subDirInfo =
                FileInfoFactory.lookupDirectory(topFolder, relativePath);

        return FolderInfoFactory.newFolder(subDirInfo);
    }
}
