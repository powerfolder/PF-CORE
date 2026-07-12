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
import de.dal33t.powerfolder.disk.SubFolderFileArchiverProxy;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // The feature is process-wide and off by default in production; enable it for
        // these tests and always clear it again in tearDown so it never leaks.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    public void testInterruptMovesSubtreeIntoOwnDAOPreservingVersion() throws IOException {
        Folder topFolder = getFolderAtBart();
        String subDir = "projects/shared";

        Path subdirPath = Files.createDirectories(topFolder.getPhysicalDir().resolve(subDir));
        Path testFile = TestHelper.createRandomFile(subdirPath, "report.txt");
        TestHelper.scanFolder(topFolder);

        FileInfo topFileInfo = topFolder.getFileInfo(testFile);
        assertNotNull("File must be scanned into the top folder", topFileInfo);
        int versionBefore = topFileInfo.getVersion();

        DirectoryInfo subDirInfo = (DirectoryInfo) topFolder.getFileInfo(subDir);
        Folder subFolder = topFolder.share(subDirInfo);
        assertTrue("Shared subfolder starts on the shared top DAO",
            subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy);

        // --- Interrupt ---
        subFolder.setInheritsPermissions(false);

        assertFalse("Interrupted subfolder must no longer inherit",
            subFolder.getInfo().inheritsPermissions());
        assertTrue("Interrupted subfolder must switch to its own DAO",
            subFolder.getDAO() instanceof FileInfoDAOHashMapImpl);
        assertTrue("Interrupted subfolder must use its own archiver, not the proxy",
            !(subFolder.getFileArchiver() instanceof SubFolderFileArchiverProxy));

        // The subtree is now gone from the top DAO - raw-removed, NOT tombstoned.
        assertNull("Interrupt must raw-remove the subtree from the top DAO (no tombstone)",
            topFolder.getDAO().find(topFileInfo, null));

        // ...and present in the subfolder's own DAO, with its version preserved.
        FileInfo mappedInfo = FileInfoFactory.mapToSubFolder(topFileInfo, subFolder.getInfo());
        FileInfo inSub = subFolder.getDAO().find(mappedInfo, null);
        assertNotNull("Migrated file must be in the subfolder's own DAO", inSub);
        assertEquals("Migration must preserve the file version", versionBefore, inSub.getVersion());
        assertEquals("Mapped relative name must be subfolder-relative", "report.txt",
            inSub.getRelativeName());
    }

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
        assertNull("Sanity: interrupt removed the file from the top DAO",
            topFolder.getDAO().find(topFileInfo, null));

        // --- Restore ---
        subFolder.setInheritsPermissions(true);

        assertTrue("Restored subfolder must inherit again",
            subFolder.getInfo().inheritsPermissions());
        assertTrue("Restored subfolder must switch back to the shared top DAO",
            subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy);

        FileInfo backInTop = topFolder.getDAO().find(topFileInfo, null);
        assertNotNull("Restore must move the file back into the top DAO", backInTop);
        assertEquals("Restore must preserve the file version", versionBefore, backInTop.getVersion());
    }

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
        assertEquals("No-op must not bump the folder version",
            versionBefore, subFolder.getInfo().getVersion());
        assertTrue("No-op must keep the shared top DAO",
            subFolder.getDAO() instanceof SubFolderFileInfoDAOProxy);
    }

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
        assertTrue("Interrupt must not delete the peer's physical file", Files.exists(lisaFile));
    }
}
