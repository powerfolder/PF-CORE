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
package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderAdminPermission;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.FolderReadPermission;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.security.SecurityManagerClient;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PFC-3536: Deleting a directory that holds a subfolder share.
 * <p>
 * The share is dissolved first, so what is deleted is an ordinary directory of the top folder. Without
 * that, an interrupted subfolder made the deletion impossible: its content belongs to its own folder
 * and database, which the top folder must not touch, so the directory never became empty, no row was
 * ever marked as deleted, nothing was broadcast - and the peers synchronized the content straight back.
 * <p>
 * Write access to the subfolder is what the dissolving takes - the same access deleting its content
 * takes. An interrupted subfolder answers to its own permissions though, so a member with read access
 * only keeps it: it stays, the path leading down to it stays with it, and everything else on the way is
 * deleted all the same.
 */
public class SubFolderDeleteTest extends TwoControllerTestCase {

    /** A member with write access, and nothing beyond it. */
    private static final AccountInfo WRITE_MEMBER = new AccountInfo("oid-member", "member@powerfolder.com");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Without the feature every subfolder reports "inherits", and nothing here could be interrupted.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.HOST_FILES);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * The whole tree goes: the share of the interrupted subfolder inside it is dissolved, its content
     * comes back to the top folder and is deleted with everything else.
     */
    public void testDeletingADirectoryDissolvesTheShareInsideIt() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        Path insideSub = projects.resolve("reports/Report.txt");
        assertNotNull("Sanity: the interrupted subfolder owns the row of its file",
            subFolder.getFileInfo("Report.txt"));

        topFolder.removeFilesLocal((AccountInfo) null, directory(topFolder, "projects"));

        assertNull("The share is dissolved",
            getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()));
        assertFalse("The file inside the former subfolder is deleted", Files.exists(insideSub));
        assertFalse("... and the whole directory with it", Files.exists(projects));
        assertTrue("The top folder reports the directory as deleted",
            topFolder.getFileInfo("projects").isDeleted());
        assertTrue("... and the file that was the subfolder's",
            topFolder.getFileInfo("projects/reports/Report.txt").isDeleted());
    }

    /**
     * Every subdirectory, not just the first one.
     * <p>
     * This is the recursive fallback of {@link Folder#removeFileLocal}, which takes over when the plain
     * delete finds the directory not empty (PFS-2002) - content on disk that the database does not know
     * about, so the ordinary pass over the known rows does not touch it. The return used to sit inside
     * the loop over the subdirectories, so one deletion took the first subdirectory and left the rest,
     * the direct files and the directory itself behind: nothing was marked as deleted, nothing was
     * broadcast, and the peers synchronized the content straight back.
     */
    public void testDeletingADirectoryTakesEverySubdirectoryAtOnce() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = Files.createDirectories(topFolder.getPhysicalDir().resolve("projects"));
        scanFolder(topFolder);
        assertNotNull("Sanity: the directory itself is known", topFolder.getFileInfo("projects"));

        // Deliberately NOT scanned: unknown content is what the recursive fallback is for.
        for (String name : new String[]{"a", "b", "c"}) {
            Path dir = Files.createDirectories(projects.resolve(name));
            TestHelper.createRandomFile(dir, "File.txt");
            TestHelper.createRandomFile(dir.resolve("deep"), "Deep.txt");
        }
        TestHelper.createRandomFile(projects, "Direct.txt");

        topFolder.removeFilesLocal((AccountInfo) null, directory(topFolder, "projects"));

        for (String name : new String[]{"a", "b", "c"}) {
            assertFalse(name + " is gone from disk", Files.exists(projects.resolve(name)));
        }
        assertFalse("The direct file of the directory is gone", Files.exists(projects.resolve("Direct.txt")));
        assertFalse("The whole tree is gone from disk", Files.exists(projects));
        assertTrue("The top folder reports the directory as deleted",
            topFolder.getFileInfo("projects").isDeleted());
    }

    /**
     * Write access to the subfolder is enough, and it is enough for an interrupted one as well: its own
     * permission structure does not outlive the directory it lives in, and whoever may empty that
     * directory file by file may take the share along.
     */
    public void testWriteAccessIsEnoughToDeleteADirectoryHoldingAnInterruptedSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        grantWriteAccessEverywhere(getContollerBart());

        topFolder.removeFilesLocal(WRITE_MEMBER, directory(topFolder, "projects"));

        assertNull("The share is dissolved",
            getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()));
        assertFalse("The directory is gone from disk", Files.exists(projects));
        assertTrue("The top folder reports the directory as deleted",
            topFolder.getFileInfo("projects").isDeleted());
    }

    /**
     * The interruption is where write access stops: the member may write in the directory above, but
     * only read inside the subfolder. That subfolder stays, and the path leading down to it stays with
     * it - otherwise it would hang in the air, with nothing above it that still knows the way. What is
     * deleted is everything else on the way, where the right to do so is there.
     */
    public void testReadAccessOnTheSubFolderKeepsItAndItsPathButNotTheRest() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Path sibling = TestHelper.createRandomFile(projects.resolve("archive"), "Old.txt");
        scanFolder(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        Path insideSub = projects.resolve("reports/Report.txt");
        grantReadAccessOnlyTo(getContollerBart(), subFolder.getInfo());

        topFolder.removeFilesLocal(WRITE_MEMBER, directory(topFolder, "projects"));

        assertNotNull("The share stays",
            getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()));
        assertTrue("The file inside the subfolder stays", Files.exists(insideSub));
        assertTrue("... and the path leading down to it", Files.exists(projects));
        assertFalse("The top folder does not report the directory as deleted",
            topFolder.getFileInfo("projects").isDeleted());

        assertFalse("Everything else on the way is deleted all the same", Files.exists(sibling));
        assertFalse("... its directory with it", Files.exists(projects.resolve("archive")));
        assertTrue("... and reported as deleted", topFolder.getFileInfo("projects/archive").isDeleted());
        assertFalse("... and the plain file of the directory", Files.exists(projects.resolve("Plain.txt")));
    }

    /** "projects" with a file of its own and a "reports" subdirectory holding one, scanned. */
    private Path createTree(Folder topFolder) throws IOException {
        Path projects = Files.createDirectories(topFolder.getPhysicalDir().resolve("projects"));
        TestHelper.createRandomFile(projects, "Plain.txt");
        TestHelper.createRandomFile(projects.resolve("reports"), "Report.txt");
        scanFolder(topFolder);
        return projects;
    }

    /**
     * Shares the given directory as a subfolder. The content is scanned before, so an interrupted
     * subfolder takes the rows of its files along and owns them - no scan of its own needed.
     */
    private Folder subFolderAt(Folder topFolder, String relativeName, boolean inherits) {
        Folder subFolder = topFolder.share(directory(topFolder, relativeName));
        if (!inherits) {
            subFolder.setInheritsPermissions(false);
        }
        assertEquals("Sanity: the subfolder's inheritance", inherits,
            subFolder.getInfo().inheritsPermissions());
        return subFolder;
    }

    private static DirectoryInfo directory(Folder folder, String relativeName) {
        FileInfo fInfo = folder.getFileInfo(relativeName);
        assertNotNull(folder + ": no row for " + relativeName, fInfo);
        assertTrue(relativeName + " is no directory", fInfo.isDiretory());
        return (DirectoryInfo) fInfo;
    }

    /** Answers like a server would for a member with write access everywhere, but no folder admin. */
    private static void grantWriteAccessEverywhere(Controller controller) {
        grantWriteAccessExceptOn(controller, null);
    }

    /** The same, but with read access only on the given folder - a member below an interruption. */
    private static void grantReadAccessOnlyTo(Controller controller, FolderInfo readOnly) {
        grantWriteAccessExceptOn(controller, readOnly);
    }

    private static void grantWriteAccessExceptOn(Controller controller, final FolderInfo readOnly) {
        controller.setSecurityManager(new SecurityManagerClient(controller, controller.getOSClient()) {
            @Override
            public boolean hasPermission(AccountInfo accountInfo, Permission permission) {
                if (!(permission instanceof FolderPermission)) {
                    return super.hasPermission(accountInfo, permission);
                }
                if (permission instanceof FolderAdminPermission
                    || permission instanceof FolderOwnerPermission)
                {
                    return false;
                }
                FolderInfo folder = ((FolderPermission) permission).getFolder();
                boolean readOnlyHere = readOnly != null && readOnly.equals(folder);
                return !readOnlyHere || permission instanceof FolderReadPermission;
            }
        });
    }
}
