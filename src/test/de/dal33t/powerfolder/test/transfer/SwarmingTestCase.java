package de.dal33t.powerfolder.test.transfer;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.MultiSourceDownload;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class SwarmingTestCase extends FiveControllerTestCase {
    public void xtestSwarmDownload() throws Exception {
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
    
    public void testFiveSwarmDownload() throws IOException {
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");

        connectAll();

        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);

        File tmpFile = TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());
        final FileInfo fInfo = getFolderAtBart().getKnowFilesAsArray()[0];

        setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtBart().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtMaggie().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtMarge().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        getFolderAtHomer().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                for (Controller c: getControllers()) {
                    if (c == getContollerHomer()) {
                        continue;
                    }
                    if (c.getFolderRepository().getFolders()[0].getKnownFilesCount() != 1) {
                        return false;
                    }
                }
                return true;
            }
        });

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "10");
        getFolderAtHomer().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                MultiSourceDownload man = getContollerHomer().getTransferManager().getActiveDownload(fInfo);
                return man != null && man.getSources().size() == 4;
            }

            public String message() {
                return "" + getContollerHomer().getTransferManager().getActiveDownload(fInfo);
            }
        });

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                MultiSourceDownload man = getContollerHomer().getTransferManager().getActiveDownload(fInfo);
                if (man == null) {
                    return true;
                }
                for (Download src: man.getSources()) {
                    if (src.getPendingRequests().isEmpty()) {
                        return false;
                    }
                }
                return true;
            }
        });

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "0");
        
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerHomer().getTransferManager().countCompletedDownloads() == 1;
            }

            public String message() {
                return "" + getContollerHomer().getTransferManager().countCompletedDownloads();
            }
        });
        MultiSourceDownload man = getContollerHomer().getTransferManager().getCompletedDownloadsCollection().get(0);
        for (Download dl: man.getSources()) {
            assertTrue(dl.getCounter().getBytesTransferred() > 0);
        }
    }
}
