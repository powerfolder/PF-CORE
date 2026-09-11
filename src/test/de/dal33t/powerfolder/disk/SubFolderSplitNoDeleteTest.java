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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;

/**
 * PFC-3536: A directory reported as deleted while it is still on disk was split off, not deleted.
 * <p>
 * A scan of the top folder does not walk into an interrupted subfolder, while the row of that
 * subfolder's own directory stays with the top folder - so a scan that starts before the interruption
 * and crawls after it never meets that directory and reports it as deleted. On one workspace a
 * migration run interrupted the inheritance of 31 directories and the scan it raced against reported
 * exactly those as deleted: 42 shares were dissolved within six seconds, each one announced as "Share
 * dissolved" and created again right after, and nothing had been deleted at all.
 * <p>
 * The directory on disk is what tells the two cases apart, and that is all this test is about: the
 * share stays while the directory stands, and it is dissolved once the directory is really gone.
 */
public class SubFolderSplitNoDeleteTest extends TwoControllerTestCase {

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

    /** The directory is still there, so nothing was deleted and the share has to stay. */
    public void testADirectoryThatIsStillOnDiskKeepsItsShare() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path reports = createTree(topFolder);
        DirectoryInfo deleted = deletionMarkerFor(topFolder, "projects/reports");
        Folder subFolder = shareInterrupted(topFolder, "projects/reports");

        topFolder.unshareDeletedSubFolders(Collections.singletonList(deleted));

        assertNotNull("The share stays - the directory it lives in is untouched",
            getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()));
        assertTrue("... and so is its content", Files.exists(reports.resolve("Report.txt")));
        assertNotNull("... which is still the subfolder's", subFolder.getFileInfo("Report.txt"));
    }

    /** The directory really is gone: then the share has nothing left to hold on to. */
    public void testADirectoryThatIsGoneDissolvesItsShare() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path reports = createTree(topFolder);
        DirectoryInfo deleted = deletionMarkerFor(topFolder, "projects/reports");
        Folder subFolder = shareInterrupted(topFolder, "projects/reports");
        PathUtils.recursiveDelete(reports);
        assertFalse("Sanity: the directory is gone from disk", Files.exists(reports));

        topFolder.unshareDeletedSubFolders(Collections.singletonList(deleted));

        assertNull("The share is dissolved",
            getContollerBart().getFolderRepository().findSubFolder(subFolder.getInfo().getLocation()));
    }

    /** "projects/reports" with a file in it, scanned. Returns the "reports" directory. */
    private Path createTree(Folder topFolder) throws IOException {
        Path reports = topFolder.getPhysicalDir().resolve("projects/reports");
        TestHelper.createRandomFile(reports, "Report.txt");
        scanFolder(topFolder);
        return reports;
    }

    private Folder shareInterrupted(Folder topFolder, String relativeName) {
        FileInfo fInfo = topFolder.getFileInfo(relativeName);
        assertNotNull(topFolder + ": no row for " + relativeName, fInfo);
        Folder subFolder = topFolder.share((DirectoryInfo) fInfo);
        subFolder.setInheritsPermissions(false);
        assertFalse("Sanity: the subfolder is interrupted", subFolder.getInfo().inheritsPermissions());
        return subFolder;
    }

    /** What a scan hands in: the row of the subfolder's own directory, marked as deleted. */
    private DirectoryInfo deletionMarkerFor(Folder topFolder, String relativeName) {
        FileInfo dirInfo = topFolder.getFileInfo(relativeName);
        assertNotNull(topFolder + ": no row for " + relativeName, dirInfo);
        assertTrue(relativeName + " is no directory", dirInfo.isDiretory());
        return (DirectoryInfo) FileInfoFactory.deletedFile(dirInfo,
            getContollerBart().getMySelf().getInfo(),
            getContollerBart().getMySelf().getAccountInfo(), new Date());
    }
}
