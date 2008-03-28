package de.dal33t.powerfolder.test.transfer;

import java.io.File;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.MultiSourceDownload;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.MultipleControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class Swarming extends MultipleControllerTestCase {
    public void testSwarmDownload() throws Exception {
        final Controller a = startControllerWithDefaultConfig("A");
        final Controller b = startControllerWithDefaultConfig("B");
        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "10");
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");

        connectAll();
        
        File tmpFile = TestHelper.createRandomFile(getFolderOf("A").getLocalBase(), 100000);
        scanFolder(getFolderOf("A"));
        final FileInfo info = getFolderOf("A").getKnowFilesAsArray()[0];
        assertTrue(tmpFile.equals(info.getDiskFile(a.getFolderRepository())));
        getFolderOf("B").setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return b.getTransferManager().getActiveDownloadCount() == 1
                    && b.getTransferManager().getDownloadManagerFor(info).getSources().size() == 1;
            }
        });
        final MultiSourceDownload manager = b.getTransferManager().getDownloadManagerFor(info);
        assertNotNull(manager);
        assertEquals(1, manager.getSources().size());
        final Download dl = manager.getSources().iterator().next();
        assertEquals(dl, manager.getSourceFor(a.getMySelf()));
        assertTrue(dl.getPartner().getIdentity().isSupportingPartRequests());
        assertTrue(manager.isUsingPartRequests());
        
        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "10000");
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return dl.isCompleted() && manager.isCompleted();
            }
        });
    }
}
