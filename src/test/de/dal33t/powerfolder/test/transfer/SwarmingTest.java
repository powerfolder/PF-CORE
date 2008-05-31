package de.dal33t.powerfolder.test.transfer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.MultipleControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

public class SwarmingTest extends MultipleControllerTestCase {
    public void xtestAlotOfControllers() throws Exception {
        joinNTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        for (int i = 0; i < 10; i++) {
            nSetupControllers(5);
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

        joinNTestFolder(SyncProfile.HOST_FILES);
        Folder barts = getFolderOf("0");

        File tmpFile = TestHelper.createRandomFile(barts.getLocalBase(),
            1000000);
        scanFolder(barts);
        final FileInfo fInfo = barts.getKnowFilesAsArray()[0];

        setNSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                for (Controller c : getControllers()) {
                    if (getFolderOf(c).getKnownFilesCount() != 1) {
                        return false;
                    }
                }
                return true;
            }

        });
    }

    public void testFiveSwarmDownload() throws IOException {
        nSetupControllers(5);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");

        connectAll();

        joinNTestFolder(SyncProfile.HOST_FILES);

        File tmpFile = TestHelper.createRandomFile(getFolderOf("0")
            .getLocalBase(), 10000000);
        scanFolder(getFolderOf("0"));
        final FileInfo fInfo = getFolderOf("0").getKnowFilesAsArray()[0];

        for (int i = 0; i < 4; i++) {
            getFolderOf("" + i).setSyncProfile(
                SyncProfile.AUTOMATIC_DOWNLOAD);
        }

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                for (Controller c : getControllers()) {
                    if (c == getContoller("4")) {
                        continue;
                    }
                    if (c.getFolderRepository().getFolders()[0]
                        .getKnownFilesCount() != 1)
                    {
                        return false;
                    }
                }
                return true;
            }
        });

        TestHelper.waitForCondition(2, new Condition() {
            public boolean reached() {
                for (Controller c : getControllers()) {
                    if (c == getContoller("4")) {
                        if (c.getTransferManager().getActiveDownloadCount() != 0)
                        {
                            return false;
                        }
                    }
                    if (c.getTransferManager().getActiveDownloadCount() != 0
                        || c.getTransferManager().getActiveUploads().length != 0)
                    {
                        return false;
                    }
                }
                return true;
            }
        });

        for (Controller c : getControllers()) {
            c.getTransferManager().setAllowedDownloadCPSForLAN(500000);
            c.getTransferManager().setAllowedUploadCPSForLAN(500000);
        }
        TestHelper.waitMilliSeconds(1000);
        getFolderOf("4").setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(5, new ConditionWithMessage() {
            public boolean reached() {
                DownloadManager man = getContoller("4").getTransferManager()
                    .getActiveDownload(fInfo);
                if (man == null || man.getSources().size() != 4) {
                    return false;
                }
                for (Controller c : getControllers()) {
                    if (c == getContoller("4")) {
                        continue;
                    }
                    if (c.getTransferManager().getActiveUploads().length != 1) {
                        return false;
                    }
                }
                return true;
            }

            public String message() {
                return ""
                    + getContoller("4").getTransferManager().getActiveDownload(
                        fInfo);
            }
        });

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                DownloadManager man = getContoller("4").getTransferManager()
                    .getActiveDownload(fInfo);
                for (Download src : man.getSources()) {
                    if (src.getPendingRequests().isEmpty()) {
                        return false;
                    }
                }
                return true;
            }
        });

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                DownloadManager man = getContoller("4").getTransferManager()
                    .getActiveDownload(fInfo);
                for (Download dl : man.getSources()) {
                    if (dl.getCounter().getBytesTransferred() <= 0) {
                        return false;
                    }
                }
                return true;
            }
        });
        assertEquals(1, getContoller("4").getTransferManager()
            .getActiveDownloadCount());

        disconnectAll();

        // Was auto download
        assertEquals(0, getContoller("4").getTransferManager()
            .getPendingDownloads().size());

        connectAll();

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "0");

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return getContoller("4").getTransferManager()
                    .countCompletedDownloads() == 1;
            }

            public String message() {
                return ""
                    + getContoller("4").getTransferManager()
                        .countCompletedDownloads();
            }
        });
        DownloadManager man;
        man = getContoller("4").getTransferManager()
            .getCompletedDownloadsCollection().get(0);
        for (Download dl : man.getSources()) {
            assertTrue(dl.getCounter().getBytesTransferred() > 0);
        }
    }

    public void testFileAlterations() throws IOException {
        nSetupControllers(6);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");

        connectAll();

        joinNTestFolder(SyncProfile.HOST_FILES);

        File tmpFile = TestHelper.createRandomFile(getFolderOf("0")
            .getLocalBase(), 10000000);
        scanFolder(getFolderOf("0"));
        final FileInfo fInfo = getFolderOf("0").getKnowFilesAsArray()[0];

        setNSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                for (int i = 1; i < 5; i++) {
                    if (getFolderOf("" + i).getKnownFilesCount() != 1) {
                        return false;
                    }
                }
                return true;
            }

        });

        disconnectAll();
        tmpFile = getFolderOf("1").getFile(fInfo).getDiskFile(
            getContoller("1").getFolderRepository());
        assertTrue(tmpFile.delete());

        tmpFile = TestHelper.createRandomFile(tmpFile.getParentFile(), tmpFile
            .getName());
        scanFolder(getFolderOf("1"));

        assertTrue(tryToConnect(getContoller("1"), getContoller("5")));

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderOf("5").getKnownFiles().iterator().next()
                    .getVersion() == 1;
            }

            public String message() {
                return "Homer version:"
                    + getFolderOf("5").getKnownFiles().iterator().next()
                        .getVersion();
            }
        });

        tmpFile = getFolderOf("5").getFile(fInfo).getDiskFile(
            getContoller("5").getFolderRepository());
        assertTrue(tmpFile.delete());

        tmpFile = TestHelper.createRandomFile(tmpFile.getParentFile(), tmpFile
            .getName());
        scanFolder(getFolderOf("5"));

        assertTrue(tryToConnect(getContoller("4"), getContoller("5")));

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderOf("5").getKnownFiles().iterator().next()
                    .getVersion() == 2;
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

    public void testMultifileSwarmingWithHeavyModifications() throws IOException {
        Random prng = new Random();
        final int numC = 2;
        nSetupControllers(numC);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");
        setConfigurationEntry(ConfigurationEntry.USE_DELTA_ON_LAN, "true");

        for (Controller c : getControllers()) {
            c.getTransferManager().setAllowedDownloadCPSForLAN(500000);
            c.getTransferManager().setAllowedUploadCPSForLAN(500000);
        }

        connectAll();

        joinNTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
        
        for (int i = 0; i < 10; i++) {
            TestHelper.createRandomFile(getFolderOf("0").getLocalBase());
        }
        scanFolder(getFolderOf("0"));
        
        assertEquals(10, getFolderOf("0").getKnownFilesCount());
        
        for (int tries = 0; tries < 4; tries++) {

            for (int i = 0; i < 50; i++) {
                TestHelper.waitMilliSeconds(200);
                String cont = "" + prng.nextInt(numC);
                FileInfo[] fi = getFolderOf(cont).getKnowFilesAsArray();
                if (fi.length > 0) {
                    FileInfo chosen = fi[prng.nextInt(fi.length)];
                    if (chosen.diskFileExists(getContoller(cont))) {
                        RandomAccessFile raf = new RandomAccessFile(chosen
                            .getDiskFile(getContoller(cont).getFolderRepository()),
                            "rw");
                        if (prng.nextDouble() > 0.3) {
                            raf.seek(prng.nextInt(1000000 - 1000));
                            for (int j = 0; j < 1000; j++) {
                                raf.write(prng.nextInt(256));
                            }
                        } else {
                            raf.setLength(prng.nextInt(1000000));
                        }
                        raf.close();
                        if (prng.nextDouble() < 0.5) {
                            scanFolder(getFolderOf(cont));
                        }
                    }
                }
            }
            for (int i = 0; i < numC; i++) {
                scanFolder(getFolderOf("" + i));
            }

            TestHelper.waitForCondition(numC * 50, new ConditionWithMessage() {
                public boolean reached() {
                    for (int i = 0; i < numC; i++) {
                        if (getFolderOf("" + i).getKnownFilesCount() != 10) {
                            return false;
                        }
                    }
                    return true;
                }

                public String message() {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < numC; i++) {
                        if (i > 0) {
                            b.append("\n");
                        }
                        b.append(i).append(": ").append(
                            getFolderOf("" + i).getKnownFilesCount());
                        b.append(", ").append(
                            getContoller("" + i).getTransferManager()
                                .getActiveDownloads());
                        b.append(", ").append(
                            Arrays.toString(getContoller("" + i)
                                .getTransferManager().getActiveUploads()));
                    }
                    return b.toString();
                }
            });
        }
        TestHelper.waitForCondition(numC * 4, new ConditionWithMessage() {
            public boolean reached() {
                for (int i = 0; i < numC; i++) {
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    if (tm.getActiveDownloadCount() != 0
                        || tm.getActiveUploads().length != 0)
                    {
                        return false;
                    }
                }
                return true;
            }

            public String message() {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < numC; i++) {
                    if (i > 0) {
                        b.append(", ");
                    }
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    b.append(i).append(": ").append(
                        tm.getActiveDownloadCount() + "|"
                            + tm.getActiveUploads().length);
                }
                return b.toString();
            }
        });
    }
    
    public void testConcurrentModificationsLargeSwarmDeltaSync()
        throws IOException
    {
        Random prng = new Random();
        final int numC = 20;
        nSetupControllers(numC);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");
        setConfigurationEntry(ConfigurationEntry.USE_DELTA_ON_LAN, "true");

        for (Controller c : getControllers()) {
            c.getTransferManager().setAllowedDownloadCPSForLAN(500000);
            c.getTransferManager().setAllowedUploadCPSForLAN(500000);
        }

        connectAll();

        joinNTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);

        File tmpFile = TestHelper.createRandomFile(getFolderOf("0")
            .getLocalBase(), 1000000);
        scanFolder(getFolderOf("0"));
        final FileInfo fInfo = getFolderOf("0").getKnowFilesAsArray()[0];

        for (int tries = 0; tries < 4; tries++) {

            for (int i = 0; i < 50; i++) {
                TestHelper.waitMilliSeconds(200);
                String cont = "" + prng.nextInt(numC);
                FileInfo[] fi = getFolderOf(cont).getKnowFilesAsArray();
                if (fi.length > 0 && fi[0].diskFileExists(getContoller(cont))) {
                    RandomAccessFile raf = new RandomAccessFile(fi[0]
                        .getDiskFile(getContoller(cont).getFolderRepository()),
                        "rw");
                    if (prng.nextDouble() > 0.3) {
                        raf.seek(prng.nextInt(1000000 - 1000));
                        for (int j = 0; j < 1000; j++) {
                            raf.write(prng.nextInt(256));
                        }
                    } else {
                        raf.setLength(prng.nextInt(1000000));
                    }
                    raf.close();
                    if (prng.nextDouble() < 0.5) {
                        scanFolder(getFolderOf(cont));
                    }
                }
            }
            for (int i = 0; i < numC; i++) {
                scanFolder(getFolderOf("" + i));
            }

            TestHelper.waitForCondition(numC * 10, new ConditionWithMessage() {
                public boolean reached() {
                    for (int i = 0; i < numC; i++) {
                        if (getFolderOf("" + i).getKnownFilesCount() != 1) {
                            return false;
                        }
                    }
                    return true;
                }

                public String message() {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < numC; i++) {
                        if (i > 0) {
                            b.append("\n");
                        }
                        b.append(i).append(": ").append(
                            getFolderOf("" + i).getKnownFilesCount());
                        b.append(", ").append(
                            getContoller("" + i).getTransferManager()
                                .getActiveDownloads());
                        b.append(", ").append(
                            Arrays.toString(getContoller("" + i)
                                .getTransferManager().getActiveUploads()));
                    }
                    return b.toString();
                }
            });
        }

        int newestVersion = 0;
        for (int i = 0; i < numC; i++) {
            scanFolder(getFolderOf("" + i));
            int v = getFolderOf("" + i).getKnowFilesAsArray()[0].getVersion();
            if (v > newestVersion) {
                newestVersion = v;
            }
        }
        final int version = newestVersion;

        TestHelper.waitForCondition(numC * 20, new ConditionWithMessage() {
            public boolean reached() {
                for (int i = 0; i < numC; i++) {
                    if (getFolderOf("" + i).getKnowFilesAsArray()[0]
                        .getVersion() != version)
                    {
                        return false;
                    }
                }
                return true;
            }

            public String message() {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < numC; i++) {
                    if (i > 0) {
                        b.append("\n");
                    }
                    b.append(i).append(": ").append(
                        getFolderOf("" + i).getKnowFilesAsArray()[0]
                            .getVersion());
                    b.append(Arrays.toString(getContoller("" + i)
                        .getTransferManager().getActiveUploads()));
                }
                return b.toString();
            }
        });
        TestHelper.waitForCondition(numC * 4, new ConditionWithMessage() {
            public boolean reached() {
                for (int i = 0; i < numC; i++) {
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    if (tm.getActiveDownloadCount() != 0
                        || tm.getActiveUploads().length != 0)
                    {
                        return false;
                    }
                }
                return true;
            }

            public String message() {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < numC; i++) {
                    if (i > 0) {
                        b.append(", ");
                    }
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    b.append(i).append(": ").append(
                        tm.getActiveDownloadCount() + "|"
                            + tm.getActiveUploads().length);
                }
                return b.toString();
            }
        });
        File a = getFolderOf("0").getKnowFilesAsArray()[0]
            .getDiskFile(getContoller("0").getFolderRepository());
        for (int i = 1; i < numC; i++) {
            TestHelper.compareFiles(a, getFolderOf("" + i)
                .getKnowFilesAsArray()[0].getDiskFile(getContoller("" + i)
                .getFolderRepository()));
        }
    }
}
