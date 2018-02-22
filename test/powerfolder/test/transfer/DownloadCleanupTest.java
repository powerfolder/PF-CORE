package de.dal33t.powerfolder.test.transfer;

import java.io.IOException;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class DownloadCleanupTest extends TwoControllerTestCase {

    @Override
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
    public void testExpertNoCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.setValue(getContollerLisa(), 4); // Never
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true); // Expert
        transferFile();

        // Lisa's download should NOT have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager().getCompletedDownloadsCollection().size();
        assertEquals("Expert No Cleanup", 1, downloadsSize);
    }

    /**
     * Expert user and download cleanup is immediate. Downloads will be cleaned up.
     *
     * @throws IOException
     */
    public void testExpertImmediateCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.setValue(getContollerLisa(), 0); // Immediate
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true); // Expert
        transferFile();

        // Lisa's download should have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager().getCompletedDownloadsCollection().size();
        assertEquals("Expert Immediate Cleanup", 0, downloadsSize);
    }

    /**
     * Novice user and download cleanup is never. Downloads will be cleaned up
     * even though the cleanup is 'never' - because novice users cannot clean up
     * downloads.
     *
     * @throws IOException
     */
    public void testBeginnerModeAutoCleanupOfDownloads() throws IOException {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.removeValue(getContollerLisa());
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), false);
        transferFile();

        // Lisa's download should have been cleaned up.
        int downloadsSize = getContollerLisa().getTransferManager()
            .getCompletedDownloadsCollection().size();
        assertEquals("Novice No Cleanup", 0, downloadsSize);
    }

}
