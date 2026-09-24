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
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PFS-5867: what a folder reports as its size when the inheritance of a subfolder below it is
 * interrupted.
 * <p>
 * The interruption moves the rows of the subtree into the subfolder's own database, through the DAO
 * raw so that no deletion travels to the peers - no scan, no file event, and therefore nothing that
 * would recalculate a statistic on its own. Both sides kept the numbers of the state before: the
 * folder that gave the rows away still counted their bytes, the one that received them did not count
 * them yet, and the owner of both was charged twice for the same bytes.
 */
public class SubFolderStatisticTest extends TwoControllerTestCase {

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

    /** The bytes move with the rows: off the top folder, onto the subfolder, and the sum stays. */
    public void testTheBytesMoveToTheInterruptedSubFolder() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path outside = TestHelper.createRandomFile(topFolder.getPhysicalDir(), "Outside.txt");
        Path inside = TestHelper.createRandomFile(topFolder.getPhysicalDir().resolve("projects/reports"),
            "Report.txt");
        scanFolder(topFolder);
        topFolder.getStatistic().calculate0();
        long sizeOutside = Files.size(outside);
        long sizeInside = Files.size(inside);
        assertEquals("Sanity: the folder counts both files",
            sizeOutside + sizeInside, topFolder.getStatistic().getLocalSize());

        Folder subFolder = shareInterrupted(topFolder, "projects/reports");

        assertSize("The top folder keeps what is still its own", topFolder, sizeOutside);
        assertSize("The subfolder counts what moved to it", subFolder, sizeInside);
        assertEquals("Together: what the folder held before", sizeOutside + sizeInside,
            topFolder.getStatistic().getLocalSize() + subFolder.getStatistic().getLocalSize());
    }

    /** And back: restoring the inheritance hands the bytes to the top folder again. */
    public void testRestoringTheInheritanceHandsTheBytesBack() throws IOException {
        Folder topFolder = getFolderAtBart();
        Path outside = TestHelper.createRandomFile(topFolder.getPhysicalDir(), "Outside.txt");
        Path inside = TestHelper.createRandomFile(topFolder.getPhysicalDir().resolve("projects/reports"),
            "Report.txt");
        scanFolder(topFolder);
        long both = Files.size(outside) + Files.size(inside);
        Folder subFolder = shareInterrupted(topFolder, "projects/reports");
        assertSize("Sanity: the bytes moved", subFolder, Files.size(inside));

        subFolder.setInheritsPermissions(true);

        assertSize("The top folder carries everything again", topFolder, both);
        assertSize("... and the subfolder counts what it borrows", subFolder, Files.size(inside));
    }

    private Folder shareInterrupted(Folder topFolder, String relativeName) {
        FileInfo fInfo = topFolder.getFileInfo(relativeName);
        assertNotNull(topFolder + ": no row for " + relativeName, fInfo);
        Folder subFolder = topFolder.share((DirectoryInfo) fInfo);
        subFolder.setInheritsPermissions(false);
        return subFolder;
    }

    /** The interruption schedules the recalculation itself - nobody scans, nobody calls calculate. */
    private static void assertSize(final String message, final Folder folder, final long expected) {
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return folder.getStatistic().getLocalSize() == expected;
            }

            @Override
            public String message() {
                return message + " - " + folder + " reports " + folder.getStatistic().getLocalSize()
                    + " instead of " + expected;
            }
        });
    }
}
