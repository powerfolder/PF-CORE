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

import de.dal33t.powerfolder.ConfigurationEntry;
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
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());
        // Give it a couple of seconds to settle.
        TestHelper.waitMilliSeconds(2000);
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

        // Lisa's download should NOT have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager().getCompletedDownloadsCollection().size();
        assertEquals( 1, downloadsSize,"Expert No Cleanup");
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

        // Lisa's download should have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager().getCompletedDownloadsCollection().size();
        assertEquals( 0, downloadsSize,"Expert Immediate Cleanup");
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

        // Lisa's download should have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager()
            .getCompletedDownloadsCollection().size();
        assertEquals( 0, downloadsSize,"Novice No Cleanup");
    }

}
