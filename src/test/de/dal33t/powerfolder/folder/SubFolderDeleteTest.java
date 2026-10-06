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
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderAdminPermission;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.FolderReadPermission;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.security.SecurityManagerClient;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Without the feature every subfolder reports "inherits", and nothing here could be interrupted.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    /**
     * The whole tree goes: the share of the interrupted subfolder inside it is dissolved, its content
     * comes back to the top folder and is deleted with everything else.
     */
    @Test
    public void testDeletingADirectoryDissolvesTheShareInsideIt() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        Path insideSub = projects.resolve("reports/Report.txt");
        assertNotNull(subFolder.getFileInfo("Report.txt"), "Sanity: the interrupted subfolder owns the row of its file");

        topFolder.removeFilesLocal((AccountInfo) null, directory(topFolder, "projects"));

        assertNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "The share is dissolved");
        assertFalse(Files.exists(insideSub), "The file inside the former subfolder is deleted");
        assertFalse(Files.exists(projects), "... and the whole directory with it");
        assertTrue(topFolder.getFileInfo("projects").isDeleted(), "The top folder reports the directory as deleted");
        assertTrue(topFolder.getFileInfo("projects/reports/Report.txt").isDeleted(), "... and the file that was the subfolder's");
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
    @Test
    public void testDeletingADirectoryTakesEverySubdirectoryAtOnce() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = Files.createDirectories(topFolder.getPhysicalDir().resolve("projects"));
        scanFolder(topFolder);
        assertNotNull(topFolder.getFileInfo("projects"), "Sanity: the directory itself is known");

        // Deliberately NOT scanned: unknown content is what the recursive fallback is for.
        for (String name : new String[]{"a", "b", "c"}) {
            Path dir = Files.createDirectories(projects.resolve(name));
            TestHelper.createRandomFile(dir, "File.txt");
            TestHelper.createRandomFile(dir.resolve("deep"), "Deep.txt");
        }
        TestHelper.createRandomFile(projects, "Direct.txt");

        topFolder.removeFilesLocal((AccountInfo) null, directory(topFolder, "projects"));

        for (String name : new String[]{"a", "b", "c"}) {
            assertFalse(Files.exists(projects.resolve(name)), name + " is gone from disk");
        }
        assertFalse(Files.exists(projects.resolve("Direct.txt")), "The direct file of the directory is gone");
        assertFalse(Files.exists(projects), "The whole tree is gone from disk");
        assertTrue(topFolder.getFileInfo("projects").isDeleted(), "The top folder reports the directory as deleted");
    }

    /**
     * Write access to the subfolder is enough, and it is enough for an interrupted one as well: its own
     * permission structure does not outlive the directory it lives in, and whoever may empty that
     * directory file by file may take the share along.
     */
    @Test
    public void testWriteAccessIsEnoughToDeleteADirectoryHoldingAnInterruptedSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        grantWriteAccessEverywhere(getContollerBart());

        topFolder.removeFilesLocal(WRITE_MEMBER, directory(topFolder, "projects"));

        assertNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "The share is dissolved");
        assertFalse(Files.exists(projects), "The directory is gone from disk");
        assertTrue(topFolder.getFileInfo("projects").isDeleted(), "The top folder reports the directory as deleted");
    }

    /**
     * The interruption is where write access stops: the member may write in the directory above, but
     * only read inside the subfolder. That subfolder stays, and the path leading down to it stays with
     * it - otherwise it would hang in the air, with nothing above it that still knows the way. What is
     * deleted is everything else on the way, where the right to do so is there.
     */
    @Test
    public void testReadAccessOnTheSubFolderKeepsItAndItsPathButNotTheRest() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Path sibling = TestHelper.createRandomFile(projects.resolve("archive"), "Old.txt");
        scanFolder(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        Path insideSub = projects.resolve("reports/Report.txt");
        grantReadAccessOnlyTo(getContollerBart(), subFolder.getInfo());

        topFolder.removeFilesLocal(WRITE_MEMBER, directory(topFolder, "projects"));

        assertNotNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "The share stays");
        assertTrue(Files.exists(insideSub), "The file inside the subfolder stays");
        assertTrue(Files.exists(projects), "... and the path leading down to it");
        assertFalse(topFolder.getFileInfo("projects").isDeleted(), "The top folder does not report the directory as deleted");

        assertFalse(Files.exists(sibling), "Everything else on the way is deleted all the same");
        assertFalse(Files.exists(projects.resolve("archive")), "... its directory with it");
        assertTrue(topFolder.getFileInfo("projects/archive").isDeleted(), "... and reported as deleted");
        assertFalse(Files.exists(projects.resolve("Plain.txt")), "... and the plain file of the directory");
    }

    /**
     * The deletion arrives from another client instead of through the web interface: the peer deletes
     * the directory, and this side syncs that deletion. Same dead end without the pass - the delete
     * fails on the subfolder inside, the fallback only deletes what this folder's database knows, and
     * the directory stays round after round.
     */
    @Test
    public void testADeletionFromAnotherClientDissolvesTheShareAsWell() throws IOException {
        final Folder topBart = getFolderAtBart();
        Folder topLisa = getFolderAtLisa();
        createTree(topBart);
        final Path projectsLisa = topLisa.getPhysicalDir().resolve("projects");
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return Files.exists(projectsLisa.resolve("reports/Report.txt"));
            }

            @Override
            public String message() {
                return "Lisa did not download the tree";
            }
        });
        final Folder subFolder = subFolderAt(topBart, "projects/reports", false);
        final Path projectsBart = topBart.getPhysicalDir().resolve("projects");

        // Lisa deletes it and reports the deletion; Bart takes it over.
        PathUtils.recursiveDelete(projectsLisa);
        scanFolder(topLisa);
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                topBart.syncRemoteDeletedFiles(true);
                return !Files.exists(projectsBart);
            }

            @Override
            public String message() {
                return "Bart did not delete " + projectsBart;
            }
        });

        assertNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "The share is dissolved");
        assertTrue(topBart.getFileInfo("projects").isDeleted(), "The directory is reported as deleted");
    }

    /**
     * The subfolder's OWN directory is deleted. A path resolves to the folder that owns it, so the
     * request lands on the subfolder - which cannot do the job: only the top folder takes a share away,
     * and until it does, the directory holds the subfolder's .PowerFolder and will not go. That is what
     * four directories on the test system reported as "Not deleted, content left behind".
     */
    @Test
    public void testDeletingTheSubFoldersOwnDirectoryGoesThroughTheTopFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        Path reports = projects.resolve("reports");
        assertTrue(Files.isDirectory(subFolder.getSystemSubDir()), "Sanity: the subfolder has a system directory of its own");

        // As the web interface does it: the base directory of the folder the path resolved to.
        subFolder.removeFilesLocal((AccountInfo) null, subFolder.getBaseDirectoryInfo());

        assertNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "The share is dissolved");
        assertFalse(Files.exists(reports), "The directory is gone from disk, .PowerFolder and all");
        assertTrue(topFolder.getFileInfo("projects/reports").isDeleted(), "The top folder reports it as deleted");
        assertTrue(Files.isDirectory(projects), "The directory above it is untouched");
    }

    /**
     * A row that outlived its content: the directory is gone from disk, the row still says it is there.
     * Every deletion used to be a silent no-op - the API answered "deleted" and the entry stayed in the
     * folder view, attempt after attempt, which is what five directories on the test system showed.
     */
    @Test
    public void testADirectoryWhoseContentIsAlreadyGoneIsStillReportedDeleted() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Path orphan = projects.resolve("archive");
        TestHelper.createRandomFile(orphan, "Old.txt");
        scanFolder(topFolder);
        assertFalse(topFolder.getFileInfo("projects/archive").isDeleted(), "Sanity: the row is alive");

        // Gone behind the folder's back - no scan, so the row still says it is there.
        PathUtils.recursiveDelete(orphan);
        assertFalse(Files.exists(orphan));

        topFolder.removeFilesLocal((AccountInfo) null, directory(topFolder, "projects/archive"));

        assertTrue(topFolder.getFileInfo("projects/archive").isDeleted(), "The row is reported as deleted");
    }

    /**
     * A row that stayed behind inside an interrupted subfolder can be deleted.
     * <p>
     * The barrier keeps foreign content out of this database (PFC-3543). A row that is already in it -
     * older than the interruption, left where the subtree moved away - was kept in by the same guard:
     * the deletion marker was built and then dropped, on every attempt, so nobody could delete the
     * directory. This folder was refused the write and the subfolder does not own the row.
     */
    @Test
    public void testARowLeftBehindInsideAnInterruptedSubFolderCanBeDeleted() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path projects = createTree(topFolder);
        Folder subFolder = subFolderAt(topFolder, "projects/reports", false);
        assertTrue(topFolder.isInInterruptedSubFolder(projects.resolve("reports")), "Sanity: the subfolder owns its subtree");

        // A row of the top folder inside that subtree, as the migration left them behind.
        FileInfo leftBehind = FileInfoFactory.unmarshallExistingFile(topFolder.getInfo(),
            "reports/Leftover.txt", null, 12, getContollerBart().getMySelf().getInfo(), null,
            new Date(), 1, null, false, null);
        topFolder.getDAO().store(null, leftBehind);
        assertNotNull(topFolder.getFileInfo("reports/Leftover.txt"), "Sanity: the row is in the top folder's database");

        topFolder.removeFilesLocal((AccountInfo) null, topFolder.getFileInfo("reports/Leftover.txt"));

        FileInfo after = topFolder.getFileInfo("reports/Leftover.txt");
        assertNotNull(after);
        assertTrue(after.isDeleted(), "The row left behind is reported as deleted");
        assertNotNull(getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()), "Sanity: the subfolder keeps its share");
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
        assertEquals(inherits, subFolder.getInfo().inheritsPermissions(), "Sanity: the subfolder's inheritance");
        return subFolder;
    }

    private static DirectoryInfo directory(Folder folder, String relativeName) {
        FileInfo fInfo = folder.getFileInfo(relativeName);
        assertNotNull(fInfo, folder + ": no row for " + relativeName);
        assertTrue(fInfo.isDiretory(), relativeName + " is no directory");
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
