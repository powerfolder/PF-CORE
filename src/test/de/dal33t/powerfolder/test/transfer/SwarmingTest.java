package de.dal33t.powerfolder.test.transfer;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.FiveControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class SwarmingTest extends FiveControllerTestCase {
    public void xtestAlotOfControllers() throws Exception {
        joinTestFolder(SyncProfile.SYNCHRONIZE_PCS);
        
        for (int i = 0; i < 10; i++) {
            connectAll();
            TestHelper.waitMilliSeconds(2000);
            tearDown();
            setUp();
        }
    }
    
    public void testKillerSwarm() throws IOException {
        nSetupControllers(20);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");
        setConfigurationEntry(ConfigurationEntry.DOWNLOADLIMIT_LAN, "100");
        
        connectAll();
        
        joinNTestFolder(SyncProfile.MANUAL_DOWNLOAD);
        Folder barts = getFolderOf(getContollerBart()); 

        File tmpFile = TestHelper.createRandomFile(barts.getLocalBase(), 1000000);
        scanFolder(barts);
        final FileInfo fInfo = barts.getKnowFilesAsArray()[0];
        
        setNSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                for (Controller c: getControllers()) {
                    if (getFolderOf(c).getKnownFilesCount() != 1) {
                        return false;
                    }
                }
                return true;
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

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "50");
        getFolderAtHomer().setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
        
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                DownloadManager man = getContollerHomer().getTransferManager().getActiveDownload(fInfo);
                return man != null && man.getSources().size() == 4;
            }

            public String message() {
                return "" + getContollerHomer().getTransferManager().getActiveDownload(fInfo);
            }
        });

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                DownloadManager man = getContollerHomer().getTransferManager().getActiveDownload(fInfo);
                for (Download src: man.getSources()) {
                    if (src.getPendingRequests().isEmpty()) {
                        return false;
                    }
                }
                return true;
            }
        });

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                DownloadManager man = getContollerHomer().getTransferManager().getActiveDownload(fInfo);
                for (Download dl: man.getSources()) {
                    if (dl.getCounter().getBytesTransferred() <= 0) {
                        return false;
                    }
                }
                return true;
            }
        });
        assertEquals(1, getContollerHomer().getTransferManager().getActiveDownloadCount());

        disconnectAll();

        // Was auto download
        assertEquals(0, getContollerHomer().getTransferManager().getPendingDownloads().size());
        
        connectAll();

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "0");
        
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerHomer().getTransferManager().countCompletedDownloads() == 1;
            }

            public String message() {
                return "" + getContollerHomer().getTransferManager().countCompletedDownloads();
            }
        });
        DownloadManager man;
        man = getContollerHomer().getTransferManager().getCompletedDownloadsCollection().get(0);
        for (Download dl: man.getSources()) {
            assertTrue(dl.getCounter().getBytesTransferred() > 0);
        }
    }
    
    public void testFileAlterations() {
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");

        connectAll();

        joinTestFolder(SyncProfile.MANUAL_DOWNLOAD);

        File tmpFile = TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 10000000);
        scanFolder(getFolderAtBart());
        final FileInfo fInfo = getFolderAtBart().getKnowFilesAsArray()[0];
        
        setSyncProfile(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() == 1
                && getFolderAtHomer().getKnownFilesCount() == 1
                && getFolderAtMaggie().getKnownFilesCount() == 1
                && getFolderAtMarge().getKnownFilesCount() == 1;
            }
            
        });

        disconnectAll();
        tmpFile = getFolderAtLisa().getFile(fInfo).getDiskFile(getContollerLisa().getFolderRepository());
        assertTrue(tmpFile.delete());
        
        tmpFile = TestHelper.createRandomFile(tmpFile.getParentFile(), tmpFile.getName());
        scanFolder(getFolderAtLisa());
        
        assertTrue(tryToConnect(getContollerLisa(), getContollerHomer()));

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtHomer().getKnownFiles().iterator().next().getVersion() == 1;
            }

            public String message() {
                return "Homer version:" + getFolderAtHomer().getKnownFiles().iterator().next().getVersion();
            }
        });
        
        tmpFile = getFolderAtHomer().getFile(fInfo).getDiskFile(getContollerHomer().getFolderRepository());
        assertTrue(tmpFile.delete());
        
        tmpFile = TestHelper.createRandomFile(tmpFile.getParentFile(), tmpFile.getName());
        scanFolder(getFolderAtHomer());
        
        assertTrue(tryToConnect(getContollerMaggie(), getContollerHomer()));

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtHomer().getKnownFiles().iterator().next().getVersion() == 2;
            }
        });
    }
    
    public void testMultiFileAlterations() throws Exception {
        for (int i = 0; i < 10; i++) {
            testFileAlterations();
            tearDown();
            setUp();
        }
    }
}
