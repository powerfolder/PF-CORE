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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PFC-3632: A subfolder that inherits its permissions has no search index of its own - the top folder's
 * index holds its rows and answers its searches. Only an interrupted subfolder, which owns its database,
 * owns an index; the restore hands the rows back to the top folder's index and drops its own.
 */
public class SubFolderSearchIndexTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        // The index worker waits before its first pass and between commits - not in a test.
        System.setProperty("powerfolder.index.startupDelayMs", "0");
        System.setProperty("powerfolder.index.minCommitIntervalMs", "0");
        super.setUp();
        // Without the feature every subfolder reports "inherits", and nothing here could be interrupted.
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.enable();
        for (Controller controller : new Controller[]{getContollerBart(), getContollerLisa()}) {
            ConfigurationEntry.SEARCH_INDEX_ENABLED.setValue(controller, true);
            ConfigurationEntry.SEARCH_INDEX_CONTENT_EXTRACTION_ENABLED.setValue(controller, false);
            ConfigurationEntry.SEARCH_INDEX_OCR_ENABLED.setValue(controller, false);
        }
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    @Override
    protected void tearDown() throws Exception {
        Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.disable();
        super.tearDown();
    }

    public void testInheritingSubFolderSearchesTheTopFolderIndex() throws IOException {
        final Folder topFolder = getFolderAtBart();
        assertNotNull("Sanity: the top folder has an index", topFolder.getSearchIndexManager());

        Path reports = Files.createDirectories(topFolder.getPhysicalDir().resolve("reports"));
        TestHelper.createRandomFile(reports, "Quarterly Report.txt");
        TestHelper.scanFolder(topFolder);
        waitForIndex(topFolder);
        assertEquals("Sanity: the top folder's index finds the file (" + topFolder.getSearchIndexManager().getIndexEntryCount()
            + " entries)", 1, topFolder.searchFiles(criteria(topFolder, "quarterly")).size());
        FileInfoCriteria scoped = criteria(topFolder, "quarterly");
        scoped.setPath("reports");
        assertEquals("Sanity: the top folder's index finds the file below the path", 1, topFolder.searchFiles(scoped).size());

        final Folder subFolder = topFolder.share((DirectoryInfo) topFolder.getFileInfo("reports"));
        assertNull("An inheriting subfolder has no index of its own", subFolder.getSearchIndexManager());
        assertFalse("... and no index directory", Files.exists(subFolder.getSystemSubDir().resolve("index")));

        List<FileInfo> fromSub = subFolder.searchFiles(criteria(subFolder, "quarterly"));
        assertEquals("The subfolder search answers from the top folder's index", 1, fromSub.size());
        assertEquals("... and the hit is a row of the subfolder", subFolder.getInfo(), fromSub.get(0).getFolderInfo());
        assertEquals("Quarterly Report.txt", fromSub.get(0).getRelativeName());
        assertEquals("The top folder finds the file exactly once", 1,
            topFolder.searchFiles(criteria(topFolder, "quarterly")).size());

        // --- Interrupt: the subfolder owns its rows and gets an index for them ---
        subFolder.setInheritsPermissions(false);
        assertNotNull("An interrupted subfolder owns an index", subFolder.getSearchIndexManager());
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return subFolder.searchFiles(criteria(subFolder, "quarterly")).size() == 1;
            }

            @Override
            public String message() {
                return "The interrupted subfolder's own index did not pick up its file";
            }
        });
        assertEquals("The top folder's index let go of the rows", 0,
            topFolder.searchFiles(criteria(topFolder, "quarterly")).size());

        // --- Restore: the rows are the top folder's again, the own index is gone ---
        subFolder.setInheritsPermissions(true);
        assertNull("A restored subfolder has no index of its own", subFolder.getSearchIndexManager());
        assertFalse("... and its index directory is gone", Files.exists(subFolder.getSystemSubDir().resolve("index")));
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return topFolder.searchFiles(criteria(topFolder, "quarterly")).size() == 1;
            }

            @Override
            public String message() {
                return "The top folder's index did not take the rows back (pending "
                    + topFolder.getSearchIndexManager().getPendingCount() + ", entries "
                    + topFolder.getSearchIndexManager().getIndexEntryCount() + ")";
            }
        });
        assertEquals("The subfolder finds it through the top folder's index", 1,
            subFolder.searchFiles(criteria(subFolder, "quarterly")).size());
    }

    private static FileInfoCriteria criteria(Folder folder, String keyword) {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addMySelf(folder);
        criteria.addKeyWord(keyword);
        criteria.setRecursive(true);
        criteria.setPath("");
        return criteria;
    }

    private static void waitForIndex(final Folder folder) {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return folder.getSearchIndexManager() != null && !folder.getSearchIndexManager().isRebuilding()
                    && folder.getSearchIndexManager().getPendingCount() == 0;
            }

            @Override
            public String message() {
                return folder + ": index still busy";
            }
        });
    }
}
