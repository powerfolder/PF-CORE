/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
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
        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testFiveSwarmMulti() throws Exception {
        for (int i = 0; i < 5; i++) {
            if (i != 0) {
                tearDown();
                setUp();
            }
            testFiveSwarmDownload();
        }
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
            getFolderOf("" + i).setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
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
                        if (c.getTransferManager().countActiveDownloads() != 0)
                        {
                            return false;
                        }
                    }
                    if (c.getTransferManager().countActiveDownloads() != 0
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
            .countActiveDownloads());

        disconnectAll();

        // Was auto download
        assertEquals(0, getContoller("4").getTransferManager()
            .getPendingDownloads().size());

        connectAll();

        setConfigurationEntry(ConfigurationEntry.UPLOADLIMIT_LAN, "0");

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
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

        TestHelper.assertIncompleteFilesGone(this);
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
                return getFolderOf("5").getKnownFilesCount() == 1
                    && getFolderOf("5").getKnownFiles().iterator().next()
                        .getVersion() == 1;
            }

            public String message() {
                int ver = -1;
                if (getFolderOf("5").getKnownFilesCount() > 0) {
                    ver = getFolderOf("5").getKnownFiles().iterator().next()
                        .getVersion();
                }
                return "Homer version:" + ver;
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

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMultiFileAlterations() throws Exception {
        for (int i = 0; i < 20; i++) {
            testFileAlterations();
            tearDown();
            TestHelper.waitMilliSeconds(1000);
            setUp();
        }
    }

    public void testMultifileSwarmingWithHeavyModifications()
        throws IOException
    {
        Random prng = new Random();
        final int numC = 8;
        nSetupControllers(numC);
        setConfigurationEntry(ConfigurationEntry.USE_SWARMING_ON_LAN, "true");
        setConfigurationEntry(ConfigurationEntry.USE_DELTA_ON_LAN, "true");

        for (Controller c : getControllers()) {
            c.getTransferManager().setAllowedDownloadCPSForLAN(1000000);
            c.getTransferManager().setAllowedUploadCPSForLAN(1000000);
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
                // TODO: After PF gets real conflict resolution remove this:
                cont = "0";
                FileInfo[] fi = getFolderOf(cont).getKnowFilesAsArray();
                if (fi.length > 0) {
                    FileInfo chosen = fi[prng.nextInt(fi.length)];
                    if (chosen.diskFileExists(getContoller(cont))) {
                        RandomAccessFile raf = new RandomAccessFile(chosen
                            .getDiskFile(getContoller(cont)
                                .getFolderRepository()), "rw");
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
        };

        TestHelper.waitForCondition(numC * 5 + 10, new ConditionWithMessage() {
            public boolean reached() {
                boolean test = true;
                int ups = 0, downs = 0;
                for (int i = 0; i < numC; i++) {
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    if (tm.countActiveDownloads() != 0
                        || tm.countLiveUploads() != 0)
                    {
                        test = false;
                    }
                    int dloads = 0;
                    for (DownloadManager m : tm.getActiveDownloads()) {
                        dloads += m.getSources().size();
                    }
                    downs += dloads;
                    ups += tm.countLiveUploads();
                }
                // System.out.println("D:" + downs + " U:" + ups);
                return test;
            }

            public String message() {
                /*
                 * PrintWriter out = null; try { out = new
                 * PrintWriter("debug.txt"); } catch (FileNotFoundException e) {
                 * e.printStackTrace(); }
                 */
                StringBuilder b = new StringBuilder();
                int ups = 0, downs = 0;
                for (int i = 0; i < numC; i++) {
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    // out.println("Controller " + i + ":");
                    // out.println("Downloads:");
                    // for (DownloadManager m: tm.getActiveDownloads()) {
                    // out.println("<-- " + m);
                    // }
                    // out.println("Uploads:");
                    // for (Upload u: tm.getActiveUploads()) {
                    // out.println("--> " + u);
                    // }
                    // out.println("Queued Uploads:");
                    // for (Upload u: tm.getQueuedUploads()) {
                    // out.println("q-> " + u);
                    // }
                    if (i > 0) {
                        b.append(", ");
                    }
                    int dloads = 0;
                    for (DownloadManager m : tm.getActiveDownloads()) {
                        dloads += m.getSources().size();
                        if (!m.isStarted()) {
                            b.append(" " + m + " hasn't been started!\n");
                        }
                        if (m.isDone()) {
                            b.append(" " + m + " is already done!\n");
                        }
                    }
                    downs += dloads;
                    ups += tm.countLiveUploads();
                    b.append(i).append(": ").append(
                        "Download Managers: " + tm.getActiveDownloads()
                            + ", Total downloads: " + dloads + ", uploads:"
                            + Arrays.toString(tm.getActiveUploads())
                            + ", Queued uploads:"
                            + tm.getQueuedUploads().length).append('\n');
                }
                // out.close();
                return b.toString() + "\nUploads total: " + ups
                    + "\nDownloads total: " + downs + "\n"
                    + TestHelper.deadlockCheck()
                    + TestHelper.findUnmatchedTransfers(SwarmingTest.this);
            }
        });
        TestHelper.waitForCondition(numC, new ConditionWithMessage() {
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
                    if (getFolderOf("" + i).getKnownFilesCount() == 10) {
                        continue;
                    }
                    if (b.length() > 0) {
                        b.append("\n\n");
                    }
                    b.append(i).append(": ").append(
                        getFolderOf("" + i).getKnownFilesCount());
                    b.append(", ").append(
                        getContoller("" + i).getTransferManager()
                            .getActiveDownloads());
                    b.append(", ").append(
                        Arrays.toString(getContoller("" + i)
                            .getTransferManager().getActiveUploads())).append(
                        '\n');
                }
                return b.toString() + " " + TestHelper.deadlockCheck();
            }
        });

        TestHelper.assertIncompleteFilesGone(this);
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

                // TODO: After PF gets real conflict resolution remove this:
                cont = "0";

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

            TestHelper.waitForCondition(numC * 4 + 30,
                new ConditionWithMessage() {
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

        TestHelper.waitForCondition(numC * 15 + 30, new ConditionWithMessage() {
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
                int nver = 0;
                for (int i = 0; i < numC; i++) {
                    nver = Math.max(nver, getFolderOf("" + i)
                        .getKnowFilesAsArray()[0].getVersion());
                }
                StringBuilder b = new StringBuilder();
                b.append("Newest version is " + nver);

                File reference = null;

                for (int i = 0; i < numC; i++) {
                    if (getFolderOf("" + i).getKnowFilesAsArray()[0]
                        .getVersion() == nver)
                    {
                        File fb = getFolderOf("" + i).getKnowFilesAsArray()[0]
                            .getDiskFile(getContoller("" + i)
                                .getFolderRepository());
                        b.append("Filecheck of " + i + ": ");
                        if (reference == null) {
                            reference = fb;
                            b.append("Using as reference file");
                        } else {
                            if (TestHelper.compareFiles(reference, fb)) {
                                b.append("File is ok");
                            } else {
                                b
                                    .append("File mismatch, although version is same!!!!!!!!!!!!");
                            }
                        }
                        continue;
                    }
                    b.append("\n---\n");
                    b.append(i).append(": ").append(
                        getFolderOf("" + i).getKnowFilesAsArray()[0]
                            .getVersion());
                    b.append(", uploads: ");
                    b.append(Arrays.toString(getContoller("" + i)
                        .getTransferManager().getActiveUploads()));
                    b.append(", downloads: ");
                    b.append(getContoller("" + i).getTransferManager()
                        .getActiveDownloads());
                }
                return b.toString() + " " + TestHelper.deadlockCheck();
            }
        });
        TestHelper.waitForCondition(numC * 3 + 30, new ConditionWithMessage() {
            public boolean reached() {
                for (int i = 0; i < numC; i++) {
                    TransferManager tm = getContoller("" + i)
                        .getTransferManager();
                    if (tm.countActiveDownloads() != 0
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
                        tm.countActiveDownloads() + "|"
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

        TestHelper.assertIncompleteFilesGone(this);
    }
}
