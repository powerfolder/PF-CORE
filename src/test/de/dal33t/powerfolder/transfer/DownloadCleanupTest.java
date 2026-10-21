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
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

public class DownloadCleanupTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestFolderContents();
        connectBartAndLisa();
        // Join on testfolder
        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        getFolderAtBart().getFolderWatcher().setIngoreAll(true);
        getFolderAtLisa().getFolderWatcher().setIngoreAll(true);
    }

    /**
     * Transfer a file from Bart to Lisa.
     */
    private void transferFile() {
        final Path created = TestHelper
            .createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        final FileInfo expected = FileInfoFactory
            .lookupInstance(getFolderAtBart(), created);
        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                FileInfo atLisa = getFolderAtLisa().getFile(expected);
                return atLisa != null && !atLisa.isDeleted()
                    && atLisa.inSyncWithDisk(atLisa.getDiskFile(
                        getContollerLisa().getFolderRepository()))
                    && getContollerLisa().getTransferManager()
                        .getActiveDownload(expected) == null;
            }

            @Override
            public String message() {
                return "Lisa did not finish downloading "
                    + expected.toDetailString() + ". Known at lisa: "
                    + getFolderAtLisa().getKnownFiles()
                    + ", active download of it: "
                    + getContollerLisa().getTransferManager()
                        .getActiveDownload(expected)
                    + ", completed downloads: "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection();
            }
        });
    }

    private void assertCompletedDownloads(final int wanted, final String what) {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return completedDownloads() == wanted;
            }

            @Override
            public String message() {
                return what + ": Lisa holds " + completedDownloads()
                    + " completed download(s), not " + wanted + ": "
                    + getContollerLisa().getTransferManager()
                        .getCompletedDownloadsCollection();
            }
        });
        assertEquals(wanted, completedDownloads(), what);
    }

    private int completedDownloads() {
        return getContollerLisa().getTransferManager()
            .getCompletedDownloadsCollection().size();
    }


    /**
     * Expert user and download cleanup is never. Downloads will NOT be cleaned up.
     *
     * @throws IOException
     */
    @Test
    public void testExpertNoCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.setValue(getContollerLisa(), 4); // Never
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true); // Expert
        transferFile();

        assertCompletedDownloads(1, "Expert No Cleanup");
    }

    /**
     * Expert user and download cleanup is immediate. Downloads will be cleaned up.
     *
     * @throws IOException
     */
    @Test
    public void testExpertImmediateCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.setValue(getContollerLisa(), 0); // Immediate
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true); // Expert
        transferFile();

        assertCompletedDownloads(0, "Expert Immediate Cleanup");
    }

    /**
     * Novice user and download cleanup is never. Downloads will be cleaned up
     * even though the cleanup is 'never' - because novice users cannot clean up
     * downloads.
     *
     * @throws IOException
     */
    @Test
    public void testBeginnerModeAutoCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.removeValue(getContollerLisa());
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), false);
        transferFile();

        assertCompletedDownloads(0, "Novice No Cleanup");
    }

}
