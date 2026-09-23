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

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SubFolderFileArchiverProxy;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PFC-3543 / PFC-3565: Integration tests for interrupting and restoring the permission
 * inheritance of a subfolder ({@link Folder#setInheritsPermissions(boolean)}) and the
 * FileInfo database migration that goes with it.
 * <p>
 * Verified invariants:
 * <ul>
 * <li>Interrupt switches the subfolder from the shared top DAO to its own DAO and moves
 * the subtree's FileInfos into that own DAO, preserving their version.</li>
 * <li>Interrupt raw-removes the subtree from the top DAO - no deleted marker (tombstone)
 * is created, so nothing is propagated to peers as a deletion.</li>
 * <li>Restore moves the rows back into the top DAO and switches back to the shared DAO.</li>
 * <li>A synced peer keeps its physical files when a subfolder is interrupted at the
 * other side.</li>
 * </ul>
 */
public class SubFolderInterruptInheritanceTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        // The feature is process-wide and off by default in production; enable it for
        // these tests and always clear it again in tearDown so it never leaks.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    @Test
    public void testInterruptMovesSubtreeIntoOwnDAOPreservingVersion() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "projects/shared";

        Path subdirPath = Files.createDirectories(topFolder.getPhysicalDir().resolve(subDir));
        Path testFile = TestHelper.createRandomFile(subdirPath, "report.txt");
        TestHelper.scanFolder(topFolder);

        FileInfo topFileInfo = topFolder.getFileInfo(testFile);
        assertNotNull(topFileInfo, "File must be scanned into the top folder");
        int versionBefore = topFileInfo.getVersion();

        DirectoryInfo subDirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(subDirInfo);
        assertTrue(subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy,
            "Shared subfolder starts on the shared top DAO");

        // --- Interrupt ---
        subFolder.setInheritsPermissions(false);

        assertFalse(subFolder.getInfo().inheritsPermissions(),
            "Interrupted subfolder must no longer inherit");
        assertTrue(subFolder.getDAO() instanceof FileInfoDAOHashMapImpl,
            "Interrupted subfolder must switch to its own DAO");
        assertTrue(!(subFolder.getFileArchiver() instanceof SubFolderFileArchiverProxy),
            "Interrupted subfolder must use its own archiver, not the proxy");

        // The subtree is now gone from the top DAO - raw-removed, NOT tombstoned.
        assertNull(topFolder.getDAO().find(topFileInfo, null),
            "Interrupt must raw-remove the subtree from the top DAO (no tombstone)");

        // ...and present in the subfolder's own DAO, with its version preserved.
        FileInfo mappedInfo = FileInfoFactory.mapToSubFolder(topFileInfo, subFolder.getInfo());
        FileInfo inSub = subFolder.getDAO().find(mappedInfo, null);
        assertNotNull(inSub, "Migrated file must be in the subfolder's own DAO");
        assertEquals(versionBefore, inSub.getVersion(), "Migration must preserve the file version");
        assertEquals("report.txt", inSub.getRelativeName(),
            "Mapped relative name must be subfolder-relative");
    }

    @Test
    public void testRestoreMovesSubtreeBackIntoTopDAO() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "docs/team";

        Path subdirPath = Files.createDirectories(topFolder.getPhysicalDir().resolve(subDir));
        Path testFile = TestHelper.createRandomFile(subdirPath, "notes.txt");
        TestHelper.scanFolder(topFolder);

        FileInfo topFileInfo = topFolder.getFileInfo(testFile);
        int versionBefore = topFileInfo.getVersion();

        DirectoryInfo subDirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(subDirInfo);

        subFolder.setInheritsPermissions(false);
        assertNull(topFolder.getDAO().find(topFileInfo, null),
            "Sanity: interrupt removed the file from the top DAO");

        // --- Restore ---
        subFolder.setInheritsPermissions(true);

        assertTrue(subFolder.getInfo().inheritsPermissions(),
            "Restored subfolder must inherit again");
        assertTrue(subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy,
            "Restored subfolder must switch back to the shared top DAO");

        FileInfo backInTop = topFolder.getDAO().find(topFileInfo, null);
        assertNotNull(backInTop, "Restore must move the file back into the top DAO");
        assertEquals(versionBefore, backInTop.getVersion(), "Restore must preserve the file version");
    }

    @Test
    public void testInterruptIsNoOpWhenAlreadyInheriting() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "misc";

        Files.createDirectories(topFolder.getPhysicalDir().resolve(subDir));
        TestHelper.scanFolder(topFolder);

        DirectoryInfo subDirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(subDirInfo);
        int versionBefore = subFolder.getInfo().getVersion();

        // Already inheriting -> no change, no version bump, still on the shared DAO.
        subFolder.setInheritsPermissions(true);
        assertEquals(versionBefore, subFolder.getInfo().getVersion(),
            "No-op must not bump the folder version");
        assertTrue(subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy,
            "No-op must keep the shared top DAO");
    }

    @Test
    public void testInterruptDoesNotDeletePeerFiles() throws IOException {
        final Folder topFolderBart = getFolderAtBart();
        final Folder topFolderLisa = getFolderAtLisa();

        String subDir = "sync/shared";
        Path subdirPath = Files.createDirectories(topFolderBart.getPhysicalDir().resolve(subDir));
        TestHelper.createRandomFile(subdirPath, "synced.txt");
        TestHelper.scanFolder(topFolderBart);

        // Wait until Lisa has actually downloaded the physical file (not just its metadata).
        final Path lisaFile = topFolderLisa.getPhysicalDir().resolve(subDir + "/synced.txt");
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return Files.exists(lisaFile);
            }

            @Override
            public String message() {
                return "Lisa did not download the synced file: " + lisaFile
                    + " (known items: " + topFolderLisa.getKnownItemCount() + ")";
            }
        });

        // Interrupt on Bart. This raw-removes the subtree from Bart's top DAO; it must NOT
        // propagate any deletion to Lisa.
        DirectoryInfo subDirInfo = (DirectoryInfo) topFolderBart.getFileInfo(subDir);
        Folder subFolderBart = topFolderBart.share(subDirInfo);
        subFolderBart.setInheritsPermissions(false);

        // Give any (erroneous) deletion broadcast time to travel, then confirm the peer file survives.
        TestHelper.waitMilliSeconds(2000);
        assertTrue(Files.exists(lisaFile), "Interrupt must not delete the peer's physical file");
    }

    /**
     * PFC-3543: interruptions nest - an interrupted subfolder below an interrupted one, which is what
     * the Alfresco migration produces. The inner one must own its content, the outer one must still be
     * able to name it as its child, and the path resolution must land on the INNERMOST of the two. This
     * is the case that made everything below the second level invisible in the web portal.
     */
    @Test
    public void testNestedInterruptionResolvesToInnermostAndStaysListable() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderRepository repository = getContollerBart().getFolderRepository();
        FolderInfo topInfo = topFolder.getInfo();

        Path outerPath = Files.createDirectories(topFolder.getPhysicalDir().resolve("outer"));
        Path innerPath = Files.createDirectories(outerPath.resolve("inner"));
        TestHelper.createRandomFile(outerPath, "outer.txt");
        Path innerFile = TestHelper.createRandomFile(innerPath, "inner.txt");
        TestHelper.scanFolder(topFolder);

        FileInfo innerFileInfo = topFolder.getFileInfo(innerFile);
        assertNotNull(innerFileInfo, "File must be scanned into the top folder");

        Folder outer = topFolder.share((DirectoryInfo) topFolder.getFileInfo("outer"));
        Folder inner = topFolder.share((DirectoryInfo) topFolder.getFileInfo("outer/inner"));
        assertNotNull(outer);
        assertNotNull(inner);

        // --- Interrupt both, innermost first (the order the migration uses) ---
        inner.setInheritsPermissions(false);
        outer.setInheritsPermissions(false);
        assertFalse(inner.getInfo().inheritsPermissions());
        assertFalse(outer.getInfo().inheritsPermissions());

        // The outer subfolder can name its child - in ITS coordinates, so a listing of the outer
        // folder can show it even though the child leaves no row behind.
        Map<DirectoryInfo, Folder> childrenOfOuter = repository.getSubFolders(outer);
        DirectoryInfo childKey = FileInfoFactory.lookupDirectory(outer.getInfo(), "inner");
        assertTrue(childrenOfOuter.containsKey(childKey), "Nested subfolder must be a child of the outer one: " + childrenOfOuter);
        assertSame(inner, childrenOfOuter.get(childKey));

        // Resolution of a deep path lands on the INNERMOST barrier, and its own path is relative to it.
        assertEquals(inner.getInfo(),
            FolderInfo.findEnclosingInterruptedSubFolder(topInfo, "outer/inner/inner.txt"));
        assertEquals("inner.txt",
            FolderInfo.relativeNameIn(inner.getInfo(), topInfo, "outer/inner/inner.txt"));
        assertEquals(outer.getInfo(),
            FolderInfo.findEnclosingInterruptedSubFolder(topInfo, "outer/outer.txt"));

        // The content itself sits in the innermost folder's own database, reachable through it.
        FileInfo inInner = inner.getDAO()
            .find(FileInfoFactory.mapToSubFolder(innerFileInfo, inner.getInfo()), null);
        assertNotNull(inInner, "Nested content must live in the innermost subfolder's own DAO");
        assertNull(topFolder.getDAO().find(innerFileInfo, null), "The top folder must not keep the nested row");

        // --- Restoring the inner one hands its content back to the folder that owns the location ---
        inner.setInheritsPermissions(true);
        assertTrue(inner.getInfo().inheritsPermissions());
        assertNotNull(outer.getDAO().find(FileInfoFactory.mapToSubFolder(innerFileInfo, outer.getInfo()), null), "After the restore the outer folder must hold the content");
    }

    /**
     * PFC-3543: a subfolder points at the TOP folder. ALWAYS - the structure never chains, whatever the
     * order the subfolders are shared and interrupted in. A subfolder naming another subfolder as its
     * top folder holds itself in place: fk_fi_topfolder refuses to let the middle row go, and deleting
     * the workspace fails on its own foreign key.
     */
    @Test
    public void testSubFolderAlwaysPointsAtTheTopFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        FolderInfo topInfo = topFolder.getInfo();

        Path outerPath = Files.createDirectories(topFolder.getPhysicalDir().resolve("outer"));
        Files.createDirectories(outerPath.resolve("inner"));
        TestHelper.createRandomFile(outerPath.resolve("inner"), "inner.txt");
        TestHelper.scanFolder(topFolder);

        // The OUTER one first, and interrupted before the inner one is even shared: from here on the
        // nested rows are answered in its coordinates, which is what a delta run of the migration meets.
        Folder outer = topFolder.share((DirectoryInfo) topFolder.getFileInfo("outer"));
        outer.setInheritsPermissions(false);
        assertFalse(outer.getInfo().inheritsPermissions());
        assertEquals(topInfo, outer.getInfo().getTopFolder(), "Precondition: the outer one points at the top folder");

        // The nested row lives in the outer subfolder's database now and comes back in ITS coordinates.
        // Mapping it to the top folder before sharing is what the web API does - subfolders are
        // registered on the top folder, however deep they sit.
        DirectoryInfo innerInOuter = (DirectoryInfo) outer.getFileInfo("inner");
        Folder inner = topFolder.share((DirectoryInfo) FileInfoFactory.mapToTopFolder(innerInOuter));
        assertNotNull(inner, "The nested directory must be shareable through the top folder");
        inner.setInheritsPermissions(false);

        for (Folder subFolder : new Folder[]{outer, inner}) {
            FolderInfo subInfo = subFolder.getInfo();
            assertTrue(subInfo.isSubFolder(), subInfo + " must be a subfolder");
            assertEquals(topInfo, subInfo.getTopFolder(), subInfo + " must point at the top folder, not at another subfolder");
            assertFalse(subInfo.getTopFolder().isSubFolder(), "The top folder of " + subInfo + " must not itself be a subfolder");
        }
        // The nesting shows in the PATH, never in the top folder reference.
        assertEquals("outer", inner.getInfo().getTopPath());
        assertEquals("outer/inner", inner.getInfo().locationPath());
    }

    /**
     * PFC-3630: interrupting the OUTER folder first and the inner one afterwards - the order a
     * migration produces, and the one an admin produces who tightens a workspace from the top down.
     * The inner interruption has to read the rows where they live by then: in the outer subfolder's
     * database, not in the top folder's. Reading the top folder found nothing, so the content stayed
     * with the outer folder while the inner one came up empty - for everybody, its own holders
     * included.
     */
    @Test
    public void testInterruptingOuterFirstKeepsTheContentOfTheInnerOne() throws IOException {
        Folder topFolder = getFolderAtBart();

        Path innerPath = Files.createDirectories(
            topFolder.getPhysicalDir().resolve("outer").resolve("inner"));
        TestHelper.createRandomFile(innerPath, "inner.txt");
        TestHelper.createRandomFile(topFolder.getPhysicalDir().resolve("outer"), "outer.txt");
        TestHelper.scanFolder(topFolder);

        Folder outer = topFolder.share((DirectoryInfo) topFolder.getFileInfo("outer"));
        outer.setInheritsPermissions(false);
        DirectoryInfo innerInOuter = (DirectoryInfo) outer.getFileInfo("inner");
        Folder inner = topFolder.share((DirectoryInfo) FileInfoFactory.mapToTopFolder(innerInOuter));
        inner.setInheritsPermissions(false);

        FileInfo innerFile = FileInfoFactory.lookupInstance(inner.getInfo(), "inner.txt");
        assertNotNull(inner.getDAO().find(innerFile, null), "The inner folder must hold its own file after the interruption");
        assertNull(outer.getDAO().find(FileInfoFactory.lookupInstance(outer.getInfo(), "inner/inner.txt"), null), "The outer folder must not keep the row of the inner subtree");
        assertNotNull(outer.getDAO().find(FileInfoFactory.lookupInstance(outer.getInfo(), "outer.txt"), null), "The outer folder keeps its own file");
    }
}
