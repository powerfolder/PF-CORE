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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.FileArchiverImpl;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SubFolderFileArchiverProxy;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * PFC-3633: Archived file versions follow a subfolder when its inheritance is interrupted - into its own
 * archive - and come back to the top folder's archive when it is restored. Neither step loses a version
 * from the history, and after the restore nothing is left in the subfolder's archive, so an unshare may
 * take its .PowerFolder directory away.
 */
public class SubFolderArchiveTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Without the feature every subfolder reports "inherits", and nothing here could be interrupted.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    public void testArchivedVersionsFollowInterruptAndRestore() throws IOException {
        final Folder topBart = getFolderAtBart();
        final Folder topLisa = getFolderAtLisa();

        // A file below "reports", synced to Lisa.
        Path reportsBart = Files.createDirectories(topBart.getPhysicalDir().resolve("reports"));
        Path fileBart = TestHelper.createRandomFile(reportsBart, "Report.txt");
        scanFolder(topBart);
        final Path fileLisa = topLisa.getPhysicalDir().resolve("reports/Report.txt");
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return Files.exists(fileLisa);
            }

            @Override
            public String message() {
                return "Lisa did not download " + fileLisa;
            }
        });

        // Lisa changes it: Bart downloads the new version and archives the old one - in the TOP archive.
        TestHelper.changeFile(fileLisa);
        scanFolder(topLisa);
        final FileInfo topRow = topBart.getFileInfo(fileBart);
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                FileInfo current = topBart.getFileInfo(fileBart);
                return current != null && !topBart.getFileArchiver().getArchivedFilesInfos(current).isEmpty();
            }

            @Override
            public String message() {
                return "Bart did not archive the previous version of " + topRow;
            }
        });
        Path topArchiveReports = archiveDir(topBart).resolve("reports");
        assertTrue("Sanity: the version sits in the top archive below the subfolder's path",
            Files.isDirectory(topArchiveReports));

        // --- Share + interrupt: the version moves into the subfolder's own archive ---
        Folder sub = topBart.share((DirectoryInfo) topBart.getFileInfo("reports"));
        assertTrue("Sanity: an inheriting subfolder archives through the top folder",
            sub.getFileArchiver() instanceof SubFolderFileArchiverProxy);
        sub.setInheritsPermissions(false);
        assertTrue("An interrupted subfolder has an archiver of its own",
            sub.getFileArchiver() instanceof FileArchiverImpl);
        FileInfo subRow = sub.getKnownFiles().iterator().next();
        assertEquals("Report.txt", subRow.getRelativeName());
        assertEquals("The interruption brought the archived version along", 1,
            sub.getFileArchiver().getArchivedFilesInfos(subRow).size());
        assertFalse("... and left nothing behind in the top archive", Files.exists(topArchiveReports));

        // A second version archived WHILE interrupted lands in the subfolder's own archive.
        sub.getFileArchiver().archive(subRow, sub.getLocalBase().resolve("Report.txt"), true);
        assertEquals("Sanity: two versions in the subfolder's archive", 2,
            sub.getFileArchiver().getArchivedFilesInfos(subRow).size());
        Path subArchive = archiveDir(sub);

        // --- Restore: both versions are back in the top archive, the own archive is empty ---
        sub.setInheritsPermissions(true);
        FileInfo restoredRow = topBart.getFileInfo(fileBart);
        assertNotNull("Sanity: the row is the top folder's again", restoredRow);
        assertEquals("The restore brought both versions back to the top archive", 2,
            topBart.getFileArchiver().getArchivedFilesInfos(restoredRow).size());
        assertTrue("The subfolder's archive holds no version any more", isEmptyOrMissing(subArchive));

        // --- Unshare: the subfolder's .PowerFolder directory may go now ---
        Path systemSubDir = sub.getSystemSubDir();
        topBart.unshare((DirectoryInfo) topBart.getFileInfo("reports"));
        assertFalse("The subfolder's .PowerFolder directory is gone", Files.exists(systemSubDir));
        assertEquals("... and the top folder still has both versions", 2,
            topBart.getFileArchiver().getArchivedFilesInfos(restoredRow).size());
    }

    private Path archiveDir(Folder folder) {
        return folder.getSystemSubDir().resolve(ConfigurationEntry.ARCHIVE_DIRECTORY_NAME.getValue(getContollerBart()));
    }

    private static boolean isEmptyOrMissing(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return true;
        }
        try (Stream<Path> entries = Files.walk(dir)) {
            return entries.noneMatch(Files::isRegularFile);
        }
    }
}
