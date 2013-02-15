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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.FolderWatcher;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * Tests file transfer between nodes.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FileTransferTest extends TwoControllerTestCase {

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

    public void testNonExtractZIPJAR() throws IOException {
        assertTrue(getFolderAtBart().isEncrypted());
        assertEquals(0, getFolderAtBart().getKnownItemCount());
        TFile zip = new TFile("src/test-resources/testzip.zip");
        TFile tzip = new TFile(getFolderAtBart().getLocalBase(), zip.getName());
        zip.cp(tzip);
        TFile jar = new TFile("src/test-resources/testjar.jar");
        TFile tjar = new TFile(getFolderAtBart().getLocalBase(), jar.getName());
        jar.cp(tjar);
        scanFolder(getFolderAtBart());
        assertEquals(2, getFolderAtBart().getKnownItemCount());
    }

    /**
     * #2480: Filename imcompatibilties between Mac client and Windows server
     * 
     * @throws IOException
     */
    public void testSpecialChars() throws IOException {
        // |, ?, ", *, <, :, >
        String filename = "  subdir1|/  %%::::          ./  |||:::*?<>  . ";
        File f = TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            FileInfoFactory.encodeIllegalChars(filename));
        assertTrue(f.toString(), f.exists());
        scanFolder(getFolderAtBart());
        assertEquals(getFolderAtBart().getKnownFiles().toString(), 1,
            getFolderAtBart().getKnownFiles().size());
        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertEquals(filename, fInfo.getRelativeName());
        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getContollerLisa().getTransferManager()
                    .countCompletedDownloads();
            }
        });
        FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        assertEquals(filename, fInfoLisa.getRelativeName());
        assertEquals(fInfo, fInfoLisa);

        File fLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());
        assertTrue(
            fInfoLisa + " @ " + fLisa.toString() + ". EXPECED: "
                + FileInfoFactory.encodeIllegalChars(filename),
            fLisa.getAbsolutePath().replace("\\", "/")
                .endsWith(FileInfoFactory.encodeIllegalChars(filename)));

        // Test update
        TestHelper.changeFile(fLisa);
        scanFolder(getFolderAtLisa());
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getContollerBart().getTransferManager()
                    .countCompletedDownloads();
            }
        });
        assertEquals(getFolderAtBart().getKnownFiles().toString(), 1,
            getFolderAtBart().getKnownFiles().size());
        fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertEquals(filename, fInfo.getRelativeName());

        // Test restore from archive
        assertTrue("Bart does not have file in archive: " + fInfo,
            getFolderAtBart().getFileArchiver().hasArchivedFileInfo(fInfo));
        FileInfo aFInfo = getFolderAtBart().getFileArchiver()
            .getArchivedFilesInfos(fInfo).get(0);
        assertTrue(
            "Bart was unable to restore file from archive: " + fInfo,
            getFolderAtBart().getFileArchiver().restore(aFInfo,
                aFInfo.getDiskFile(getContollerBart().getFolderRepository())));
    }

    public void testFileCopyCert8() throws IOException {
        File testFileBart = new TFile(getFolderAtBart().getLocalBase(),
            "cert8.db");
        File origFile = new TFile("src/test-resources/cert8.db");
        FileUtils.copyFile(origFile, testFileBart);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals("Known items at bart: "
            + getFolderAtBart().getKnownItemCount(), 1, getFolderAtBart()
            .getKnownItemCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownItemCount();
            }
        });

        // Test ;)
        assertEquals("Known items at lisa: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());

        File testFileLisa = new TFile(getFolderAtLisa().getLocalBase(),
            "cert8.db");
        assertEquals(origFile.length(), testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testFileCopyURLclassifier2() throws IOException {
        File testFileBart = new TFile(getFolderAtBart().getLocalBase(),
            "urlclassifier2.sqlite");
        File origFile = new TFile("src/test-resources/urlclassifier2.sqlite");
        FileUtils.copyFile(origFile, testFileBart);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals("Known items at bart: "
            + getFolderAtBart().getKnownItemCount(), 1, getFolderAtBart()
            .getKnownItemCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownItemCount();
            }
        });

        // Test ;)
        assertEquals("Known items at lisa: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());

        File testFileLisa = new TFile(getFolderAtLisa().getLocalBase(),
            "urlclassifier2.sqlite");
        assertEquals(origFile.length(), testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testSmallFileCopy() throws IOException {
        File testFileBart = new TFile(getFolderAtBart().getLocalBase(),
            "TestFile.txt");
        TFileOutputStream fOut = new TFileOutputStream(testFileBart);
        byte[] testContent = "This is the contenent of the testfile".getBytes();
        fOut.write(testContent);
        fOut.close();

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals("Known items at bart: "
            + getFolderAtBart().getKnownItemCount(), 1, getFolderAtBart()
            .getKnownItemCount());

        // Give them time to copy
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownItemCount();
            }
        });

        // Test ;)
        assertEquals("Known items at lisa: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());

        File testFileLisa = new TFile(getFolderAtLisa().getLocalBase(),
            "TestFile.txt");
        assertEquals(testContent.length, testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testFileUpdate() throws IOException {
        // First copy file
        testSmallFileCopy();

        final File testFile1 = new TFile(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        OutputStream fOut = new TFileOutputStream(testFile1, true);
        fOut.write("-> Next content<-".getBytes());
        fOut.close();

        // Readin file content
        InputStream fIn = new TFileInputStream(testFile1);
        byte[] content1 = new byte[(int) testFile1.length()];
        fIn.read(content1);
        fIn.close();

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Icoming files at lisa: "
                    + getFolderAtLisa().getIncomingFiles().size();
            }

            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles().size() == 1;
            }

        });
        // Give them time to copy
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return testFile1.length() == getFolderAtLisa().getKnownFiles()
                    .iterator().next().getSize();
            }

            public String message() {
                return "Testfile length: "
                    + testFile1.length()
                    + ". Known size: "
                    + getFolderAtLisa().getKnownFiles().iterator().next()
                        .getSize();
            }
        });

        // Test ;)
        assertEquals("Known items at lisa: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());
        FileInfo testFileInfo2 = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        assertEquals(testFile1.length(), testFileInfo2.getSize());

        // Read content
        File testFile2 = testFileInfo2.getDiskFile(getContollerLisa()
            .getFolderRepository());
        fIn = new TFileInputStream(testFile2);
        byte[] conten2 = new byte[fIn.available()];
        fIn.read(conten2);
        fIn.close();

        // Check version
        assertEquals(
            "Test file version not 1: " + testFileInfo2.toDetailString(), 1,
            testFileInfo2.getVersion());

        // Check content
        assertEquals(new String(content1), new String(conten2));

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testEmptyFileCopy() throws IOException {
        // Register listeners
        MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        File testFile1 = new TFile(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        OutputStream fOut = new TFileOutputStream(testFile1);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue("Testfile does not exists:" + testFile1, testFile1.exists());

        File testFile2 = new TFile(getFolderAtBart().getLocalBase()
            + "/subdir/TestFile2.txt");
        assertTrue("Unable mkdirs: " + testFile2.getParentFile(), testFile2
            .getParentFile().mkdirs());
        fOut = new TFileOutputStream(testFile2);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue("Testfile does not exists:" + testFile2, testFile2.exists());

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadCompleted >= 2;
            }

            public String message() {
                return "Lisa dls requested " + lisasListener.downloadRequested
                    + ", completed " + lisasListener.downloadCompleted;
            }
        });

        // Check correct event fireing
        assertEquals("Bart uploadRequested: " + bartsListener.uploadRequested,
            0, bartsListener.uploadRequested);
        assertEquals("Bart uploadStarted " + bartsListener.uploadStarted, 0,
            bartsListener.uploadStarted);
        assertEquals("Bart uploadCompleted " + bartsListener.uploadCompleted,
            0, bartsListener.uploadCompleted);
        assertEquals("Bart uploadAborted " + bartsListener.uploadAborted, 0,
            bartsListener.uploadAborted);
        assertEquals("Bart uploadBroken " + bartsListener.uploadBroken, 0,
            bartsListener.uploadBroken);

        // Check correct event fireing
        assertEquals("Lisa downloadRequested "
            + lisasListener.downloadRequested, 0,
            lisasListener.downloadRequested);
        assertEquals("Lisa downloadQueued " + lisasListener.downloadQueued, 0,
            lisasListener.downloadQueued);
        assertEquals("Lisa downloadStarted " + lisasListener.downloadStarted,
            0, lisasListener.downloadStarted);
        assertEquals("Lisa downloadCompleted "
            + lisasListener.downloadCompleted, 2,
            lisasListener.downloadCompleted);
        assertEquals("Lisa downloadAborted " + lisasListener.downloadAborted,
            0, lisasListener.downloadAborted);
        assertEquals("Lisa downloadBroken " + lisasListener.downloadBroken, 0,
            lisasListener.downloadBroken);
        assertEquals("Lisa downloadsCompletedRemoved "
            + lisasListener.downloadsCompletedRemoved, 0,
            lisasListener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals("Lisa.getKnownItemCount: "
            + getFolderAtLisa().getKnownItemCount(), 3, getFolderAtLisa()
            .getKnownItemCount());
        // 3 physical files (1 file + 1 system dir + 1 subdir)
        assertEquals("Lisa.getLocalBase.list: "
            + getFolderAtLisa().getLocalBase().list().length, 3,
            getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }

            public String message() {
                return "Lisa.countActiveDownloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        clearCompletedDownloadsAtLisa();
        // give time for event firering
        // Thread.sleep(500);
        // assertEquals(2, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    /**
     * 
     */
    private void clearCompletedDownloadsAtLisa() {
        // Clear completed downloads
        Collection<DownloadManager> list = getContollerLisa()
            .getTransferManager().getCompletedDownloadsCollection();
        for (DownloadManager download : list) {
            getContollerLisa().getTransferManager().clearCompletedDownload(
                download);
        }
    }

    /**
     * Tests the copy of a big file. approx. 10 megs.
     */
    public void testBigFileCopy() {
        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener(
            true);
        getContollerLisa().getTransferManager().addListener(lisasListener);

        // 12 Meg testfile
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            12 * 1024 * 1024);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return lisasListener.downloadRequested >= 1
                    && lisasListener.downloadCompleted >= 1
                    && bartsListener.uploadCompleted >= 1;
            }
        });

        // Check correct event fireing
        assertEquals("Bart uploadRequested: " + bartsListener.uploadRequested,
            1, bartsListener.uploadRequested);
        assertEquals("Bart uploadStarted: " + bartsListener.uploadStarted, 1,
            bartsListener.uploadStarted);
        assertEquals("Bart uploadCompleted: " + bartsListener.uploadCompleted,
            1, bartsListener.uploadCompleted);
        assertEquals("Bart uploadAborted: " + bartsListener.uploadAborted, 0,
            bartsListener.uploadAborted);
        assertEquals("Bart uploadBroken: " + bartsListener.uploadBroken, 0,
            bartsListener.uploadBroken);

        // Check correct event fireing
        assertEquals("Lisa downloadRequested "
            + lisasListener.downloadCompleted, 1,
            lisasListener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(1, lisasListener.downloadQueued);
        assertEquals("Lisa downloadStarted " + lisasListener.downloadStarted,
            1, lisasListener.downloadStarted);
        assertEquals("Lisa downloadCompleted "
            + lisasListener.downloadCompleted + ": "
            + lisasListener.downloadsCompleted, 1,
            lisasListener.downloadCompleted);
        assertEquals("Lisa downloadAborted " + lisasListener.downloadAborted,
            0, lisasListener.downloadAborted);
        assertEquals("Lisa downloadBroken " + lisasListener.downloadBroken, 0,
            lisasListener.downloadBroken);
        assertEquals("Lisa downloadsCompletedRemoved "
            + lisasListener.downloadsCompletedRemoved, 0,
            lisasListener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals("Lisa.getKnownItemCount: "
            + getFolderAtLisa().getKnownItemCount(), 1, getFolderAtLisa()
            .getKnownItemCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals("Lisa.getLocalBase.list: "
            + getFolderAtLisa().getLocalBase().list().length, 2,
            getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }

            public String message() {
                return "Lisa.countActiveDownloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        clearCompletedDownloadsAtLisa();
        assertEquals("Lisa downloadsCompletedRemoved "
            + lisasListener.downloadsCompletedRemoved, 1,
            lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMultipleFilesCopy() {
        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());
        assertEquals("Connected nodes at bart: "
            + getContollerBart().getNodeManager().getConnectedNodes(), 1,
            getContollerBart().getNodeManager().getConnectedNodes().size());
        assertEquals("Connected nodes at lisa: "
            + getContollerLisa().getNodeManager().getConnectedNodes(), 1,
            getContollerLisa().getNodeManager().getConnectedNodes().size());

        // Wait for copy (timeout 50)
        TestHelper.waitForCondition(200, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadCompleted >= nFiles
                    && bartsListener.uploadCompleted >= nFiles;
            }

            public String message() {
                return "lisa: " + lisasListener.downloadRequested
                    + ", lisa dl comp: " + lisasListener.downloadCompleted
                    + ", bart ul comp: " + bartsListener.uploadCompleted
                    + " should be " + nFiles;
            }
        });

        // Check correct event fireing
        assertEquals("Bart uploadAborted: " + bartsListener, 0,
            bartsListener.uploadAborted);
        assertEquals("Bart uploadBroken: " + bartsListener, 0,
            bartsListener.uploadBroken);
        assertEquals("Bart uploadRequested: " + bartsListener, nFiles,
            bartsListener.uploadRequested);
        assertEquals("Bart uploadStarted: " + bartsListener, nFiles,
            bartsListener.uploadStarted);
        assertEquals("Bart uploadCompleted: " + bartsListener, nFiles,
            bartsListener.uploadCompleted);

        // Check correct event fireing
        assertEquals("Lisa downloadRequested " + lisasListener, nFiles,
            lisasListener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(nFiles, lisasListener.downloadQueued);
        assertEquals("Lisa downloadStarted " + lisasListener, nFiles,
            lisasListener.downloadStarted);
        assertEquals("Lisa downloadCompleted " + lisasListener, nFiles,
            lisasListener.downloadCompleted);
        assertEquals("Lisa downloadAborted " + lisasListener, 0,
            lisasListener.downloadAborted);
        assertEquals("Lisa downloadBroken " + lisasListener, 0,
            lisasListener.downloadBroken);
        assertEquals("Lisa downloadsCompletedRemoved " + lisasListener, 0,
            lisasListener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals("Lisa.getKnownItemCount: "
            + getFolderAtLisa().getKnownItemCount(), nFiles, getFolderAtLisa()
            .getKnownItemCount());
        // test physical files (1 + 1 system dir)
        assertEquals("Lisa.getLocalBase.list: "
            + getFolderAtLisa().getLocalBase().list().length, nFiles + 1,
            getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }

            public String message() {
                return "Lisa.countActiveDownloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        clearCompletedDownloadsAtLisa();
        assertEquals("Lisa downloadsCompletedRemoved " + lisasListener, nFiles,
            lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMultipleFilesCopyWithFolderWatcher() {
        // Register listeners
        if (!FolderWatcher.isLibLoaded()) {
            return;
        }
        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().getFolderWatcher().setIngoreAll(false);
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtLisa().getFolderWatcher().setIngoreAll(false);
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(2500);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtBart().getKnownItemCount() == 35;
            }

            public String message() {
                return "Known files at bart: "
                    + getFolderAtBart().getKnownItemCount() + " Expected: "
                    + 35;
            }
        });
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());
        assertEquals("Connected nodes at bart: "
            + getContollerBart().getNodeManager().getConnectedNodes(), 1,
            getContollerBart().getNodeManager().getConnectedNodes().size());
        assertEquals("Connected nodes at lisa: "
            + getContollerLisa().getNodeManager().getConnectedNodes(), 1,
            getContollerLisa().getNodeManager().getConnectedNodes().size());

        // Wait for copy (timeout 50)
        TestHelper.waitForCondition(200, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadCompleted >= nFiles
                    && bartsListener.uploadCompleted >= nFiles;
            }

            public String message() {
                return "lisa dl req: " + lisasListener.downloadRequested
                    + ", lisa dl comp: " + lisasListener.downloadCompleted
                    + ", bart ul comp: " + bartsListener.uploadCompleted
                    + " should be " + nFiles;
            }
        });

        // Check correct event fireing
        assertEquals(0, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);
        assertEquals(nFiles, bartsListener.uploadRequested);
        assertEquals(nFiles, bartsListener.uploadStarted);
        assertEquals(nFiles, bartsListener.uploadCompleted);

        // Check correct event fireing
        assertEquals(nFiles, lisasListener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(nFiles, lisasListener.downloadQueued);
        assertEquals(nFiles, lisasListener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(0, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownItemCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMultipleMultipleFilesCopy() throws Exception {
        for (int i = 0; i < 10; i++) {
            testMultipleFilesCopy();
            tearDown();
            setUp();
        }
    }

    public void testManySmallFilesCopy() {
        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        long totalSize = 0;
        final int nFiles = 450;
        for (int i = 0; i < nFiles; i++) {
            File f = TestHelper.createRandomFile(getFolderAtBart()
                .getLocalBase(), (long) (Math.random() * 40) + 1);
            totalSize += f.length();
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());

        long start = System.currentTimeMillis();
        // Wait for copy
        TestHelper.waitForCondition(300, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadCompleted >= nFiles
                    && bartsListener.uploadCompleted >= nFiles;
            }

            public String message() {
                return "Lisa downloads completed: "
                    + lisasListener.downloadCompleted
                    + ". Bart uploads completed: "
                    + bartsListener.uploadCompleted;
            }
        });
        long took = System.currentTimeMillis() - start;
        double kbs = ((double) totalSize) / took;

        System.err.println("Transfer stats: " + nFiles + " files, "
            + Format.formatBytesShort(totalSize) + ", " + kbs / 1024 + ", "
            + took + "ms");

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownItemCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // Check correct event fireing
        assertEquals(0, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);
        assertEquals(nFiles, bartsListener.uploadRequested);
        assertEquals(nFiles, bartsListener.uploadStarted);
        assertEquals(nFiles, bartsListener.uploadCompleted);

        // Check correct event fireing
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(0, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);
        assertEquals(nFiles, lisasListener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(nFiles, lisasListener.downloadQueued);
        assertEquals(nFiles, lisasListener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testManyPow2FilesCopy() {
        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener(
            true);
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener(
            true);
        getContollerLisa().getTransferManager().addListener(lisasListener);

        final int nFiles = 450;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
                1 << (i % 18));
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals("getFolderAtBart().getKnownItemCount(): "
            + getFolderAtBart().getKnownItemCount(), nFiles, getFolderAtBart()
            .getKnownItemCount());

        // Wait for copy
        TestHelper.waitForCondition(200, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadCompleted >= nFiles
                    && bartsListener.uploadCompleted >= nFiles;
            }

            public String message() {
                return "lisa.dreq: " + lisasListener.downloadRequested
                    + " lisa.dcomp: " + lisasListener.downloadCompleted
                    + " bart.upcomp: " + bartsListener.uploadCompleted + " vs "
                    + nFiles;
            }
        });

        // Give the system a small amount of time to acknowledge the completion
        // of all downloads
        TestHelper.waitMilliSeconds(100);

        // No active downloads?
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }

            public String message() {
                return "Lisa.countActiveDownloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        clearCompletedDownloadsAtLisa();
        assertEquals("Lisa downloadsCompletedRemoved "
            + lisasListener.downloadsCompletedRemoved, nFiles,
            lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMultipleManPow2() throws Exception {
        // Logger l =
        // Logger.getLogger(AbstractDownloadManager.class.getRelativeName());
        // l.setFilter(null);
        // l.setLevel(Level.ALL);
        // ConsoleHandler ch = new ConsoleHandler();
        // ch.setLevel(Level.ALL);
        // l.addHandler(ch);
        for (int i = 0; i < 10; i++) {
            if (i > 0) {
                tearDown();
                setUp();
            }
            testManyPow2FilesCopy();
        }
    }

    public void testManyIncreasingFilesCopy() {
        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        final int nFiles = 100;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
                (long) ((i << 7) + Math.random() * (1 << 7) - 1) + 1);
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());

        // Wait for copy
        TestHelper.waitForCondition(200, new Condition() {
            public boolean reached() {
                return lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadCompleted >= nFiles
                    && bartsListener.uploadCompleted >= nFiles;
            }
        });
        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMany0SizeFilesCopyMulti() throws Exception {
        for (int i = 0; i < 5; i++) {
            testMany0SizeFilesCopy();
            tearDown();
            setUp();
        }
    }

    public void testMany0SizeFilesCopy() {
        // Register listeners
        MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        final int nFiles = 450;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 0);
        }
        System.err.println("Created!");

        assertEquals(0, lisasListener.downloadCompleted);

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // Wait for copy
        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadCompleted >= nFiles
                    && getFolderAtLisa().getKnownItemCount() == nFiles;
            }

            public String message() {
                return "Completed downloads at lisa actual: "
                    + lisasListener.downloadCompleted + ". Expected: " + nFiles;
            }
        });

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownItemCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // Check correct event fireing
        assertEquals(0, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);
        assertEquals(0, bartsListener.uploadRequested);
        assertEquals(0, bartsListener.uploadStarted);
        assertEquals(0, bartsListener.uploadCompleted);

        // Check correct event fireing
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(0, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);
        // 0 bytes downloads never get requested, nor queued or started.
        assertEquals(0, lisasListener.downloadRequested);
        assertEquals(0, lisasListener.downloadQueued);
        assertEquals(0, lisasListener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testMany0SizeFilesCopyDeltaSync() {
        ConfigurationEntry.USE_DELTA_ON_LAN
            .setValue(getContollerBart(), "true");
        ConfigurationEntry.USE_DELTA_ON_LAN
            .setValue(getContollerLisa(), "true");

        // Register listeners
        MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        final int nFiles = 450;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(), 0);
        }
        System.err.println("Created!");

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownItemCount());

        // Wait for copy
        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadCompleted >= nFiles;
            }

            public String message() {
                return "Completed downloads at lisa. Actual: "
                    + lisasListener.downloadCompleted + ", Expected: " + nFiles;
            }
        });

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownItemCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // Check correct event fireing
        assertEquals(0, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);
        assertEquals(0, bartsListener.uploadRequested);
        assertEquals(0, bartsListener.uploadStarted);
        assertEquals(0, bartsListener.uploadCompleted);

        // Check correct event fireing
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(0, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);
        assertEquals(0, lisasListener.downloadRequested);
        // 0 bytes downloads never get requested, nor queued or started.
        assertEquals(0, lisasListener.downloadRequested);
        assertEquals(0, lisasListener.downloadQueued);
        assertEquals(0, lisasListener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void xtestMultipleResumeTransfer() throws Exception {
        // Logger l =
        // Logger.getLogger(AbstractDownloadManager.class.getRelativeName());
        // l.setFilter(null);
        // l.setLevel(Level.ALL);
        // ConsoleHandler ch = new ConsoleHandler();
        // ch.setLevel(Level.ALL);
        // l.addHandler(ch);
        for (int i = 0; i < 20; i++) {
            testResumeTransfer();
            tearDown();
            setUp();
        }
    }

    public void testRecoverFromMD5ErrorMultipe() throws Exception {
        for (int i = 0; i < 10; i++) {
            testRecoverFromMD5Error();
            tearDown();
            setUp();
        }
    }

    /**
     * TRAC #1904
     */
    public void testRecoverFromMD5Error() {
        getContollerBart().getTransferManager().setUploadCPSForLAN(400000);
        getContollerBart().getTransferManager().setUploadCPSForWAN(400000);
        ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getContollerBart(), true);
        ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getContollerLisa(), true);
        // testfile
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());
        testFile.setLastModified(System.currentTimeMillis() - 6000);
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getKnownItemCount() == 1;
            }

            public String message() {
                return "getFolderAtLisa().getKnownItemCount(): "
                    + getFolderAtLisa().getKnownItemCount();
            }
        });

        TestHelper.changeFile(testFile, 20 * 1024 * 1024);
        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() > 0;
            }

            public String message() {
                return "getContollerLisa.countActiveDownloads(): "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        final File tempFile = getContollerLisa().getTransferManager()
            .getActiveDownloads().iterator().next().getTempFile();
        // Let them copy some ~1 megs
        final long mbUntilBreak = 1;
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return tempFile.length() > mbUntilBreak * 1024 * 1024;
            }
        });
        disconnectBartAndLisa();
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }

            public String message() {
                return "Active downloads @ lisa: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }
        });

        LoggingManager.setConsoleLogging(Level.FINE);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 0);
        assertTrue(tempFile.length() < testFile.length());
        assertTrue("Size inc. file: " + tempFile.length() + ", size testfile: "
            + testFile.length(), tempFile.length() < testFile.length());

        // Now mess up the tempfile = Force a MD5_ERROR
        long tempMod = tempFile.lastModified();
        TestHelper.changeFile(tempFile, tempFile.length());
        tempFile.setLastModified(tempMod);

        // Reconnect /Resume transfer
        connectBartAndLisa();

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return !tempFile.exists();
            }

            public String message() {
                return "Tempfile does exist although MD5_ERROR has been observed:"
                    + tempFile;
            }
        });

        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                getContollerLisa().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() > 1;
            }

            public String message() {
                return "getContollerLisa.countCompletedDownloads(): "
                    + getContollerLisa().getTransferManager()
                        .countCompletedDownloads();
            }
        });

        FileInfo fBart = getFolderAtBart().getKnownFiles().iterator().next();
        File fileBart = fBart.getDiskFile(getContollerBart()
            .getFolderRepository());
        FileInfo fLisa = getFolderAtLisa().getKnownFiles().iterator().next();
        File fileLisa = fLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());
        assertEquals("File version at bart not 1: " + fBart.toDetailString(),
            1, fBart.getVersion());
        assertEquals("File version at lisa not 1: " + fLisa.toDetailString(),
            1, fLisa.getVersion());
        assertTrue("File content mismatch: " + fileBart + " and " + fileLisa,
            TestHelper.compareFiles(fileBart, fileLisa));
    }

    /**
     * Tests the copy and download resume of a big file.
     * <p>
     * TRAC #415
     */
    public void testResumeTransfer() {
        getContollerBart().getTransferManager().setUploadCPSForLAN(100000);
        getContollerBart().getTransferManager().setUploadCPSForWAN(100000);
        getContollerBart().getReconnectManager().shutdown();
        getContollerLisa().getReconnectManager().shutdown();
        assertFalse(getContollerBart().getReconnectManager().isStarted());

        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        // testfile
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 30 * 1024 * 1024);
        testFile.setLastModified(System.currentTimeMillis() - 1000L * 60 * 60);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() > 0
                    && new TFile(getFolderAtLisa().getSystemSubDir(),
                        "transfers").listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.contains("(incomplete) ");
                        }
                    }) != null;
            }
        });

        FileInfo fInfo = getFolderAtLisa().getIncomingFiles().iterator().next();
        File file = fInfo.getDiskFile(getContollerLisa().getFolderRepository());
        final File incompleteFile = new TFile(getFolderAtLisa()
            .getSystemSubDir(), "transfers").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains("(incomplete) ");
            }
        })[0];
        FileInfo bartFInfo = getFolderAtBart().getKnownFiles().iterator()
            .next();
        File bartFile = bartFInfo.getDiskFile(getContollerBart()
            .getFolderRepository());
        assertFileMatch(bartFile, bartFInfo, getContollerBart());

        // Let them copy some ~1 megs
        final long mbUntilBreak = 1;
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return incompleteFile.length() > mbUntilBreak * 1024 * 1024;
            }
        });
        assertEquals(1, getContollerLisa().getTransferManager()
            .getActiveDownload(fInfo).getSources().size());

        assertEquals(0, lisasListener.downloadBroken);

        // Disconnected
        disconnectBartAndLisa();

        TestHelper.waitMilliSeconds(100);

        assertTrue(incompleteFile.length() < testFile.length());

        assertTrue(incompleteFile.exists());
        assertTrue(incompleteFile.length() > 0);
        assertTrue("Size inc. file: " + incompleteFile.length()
            + ", size testfile: " + testFile.length(),
            incompleteFile.length() < testFile.length());

        assertEquals(1, bartsListener.uploadRequested);
        assertEquals(1, bartsListener.uploadStarted);
        assertEquals(0, bartsListener.uploadCompleted);
        assertEquals(1, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);

        assertEquals(1, lisasListener.downloadRequested);
        assertEquals(1, lisasListener.downloadStarted);
        assertEquals(0, lisasListener.downloadCompleted);
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(1, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);

        assertFalse(file.exists());
        assertTrue(incompleteFile.exists());
        assertTrue(incompleteFile.length() > mbUntilBreak * 1024 * 1024);
        long bytesDownloaded = getContollerLisa().getTransferManager()
            .getDownloadCounter().getBytesTransferred();
        // Test has to be >= because it could happen that the download gets
        // broken before the received data is written
        assertTrue("Downloaded: " + bytesDownloaded + " filesize: "
            + incompleteFile.length(),
            bytesDownloaded >= incompleteFile.length());
        // System.err.println("Incomplete file: " +
        // incompleteFile.lastModified()
        // + ", size: " + incompleteFile.length());
        assertEquals(bartFile.lastModified(), bartFInfo.getModifiedDate()
            .getTime());
        assertTrue(
            "Last modified date mismatch of orignial file and incompleted dl file",
            DateUtil.equalsFileDateCrossPlattform(bartFile.lastModified(),
                incompleteFile.lastModified()));
        assertEquals(bartFInfo.getModifiedDate().getTime(),
            incompleteFile.lastModified());

        System.err.println("Transferred " + incompleteFile.length() + " bytes");

        // Reconnect /Resume transfer
        connectBartAndLisa();
        // Trigger. FIXME: Should be automatically done on connect!
        getContollerLisa().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Wait untill download is started
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadStarted >= 2;
            }

            public String message() {
                return "Lisa download started: "
                    + lisasListener.downloadStarted + " requested, "
                    + lisasListener.downloadRequested + ", broken: "
                    + lisasListener.downloadBroken + ", last problem: "
                    + lisasListener.lastEvent.getTransferProblem() + ": "
                    + lisasListener.lastEvent.getProblemInformation();
            }
        });

        // Temp file should be greater than 3mb already
        // TODO: I added speed limits above because on my machine the transfer
        // was too fast and the
        // file was completed already. Please check if this test is correct.
        assertTrue("Tempfile already removed", incompleteFile.exists());
        assertTrue("Temp file should be greater than " + mbUntilBreak
            + "mb already. got " + Format.formatBytes(incompleteFile.length()),
            incompleteFile.length() > mbUntilBreak * 1024 * 1024);

        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadCompleted == 1
                    && lisasListener.downloadRequested == 2
                    && lisasListener.downloadStarted == 2
                    && lisasListener.downloadAborted == 0
                    && lisasListener.downloadBroken == 1
                    && bartsListener.uploadRequested == 2
                    && bartsListener.uploadStarted == 2
                    && bartsListener.uploadAborted == 1
                    && bartsListener.uploadBroken == 0
                    && bartsListener.uploadCompleted == 1;
            }

            public String message() {
                return "lisa: completed dl= " + lisasListener.downloadCompleted
                    + ", req dl= " + lisasListener.downloadRequested
                    + ", srtd dl= " + lisasListener.downloadStarted
                    + ", brkn dl= " + lisasListener.downloadBroken
                    + ", abrt dl= " + lisasListener.downloadAborted
                    + "; bart: req ul= " + bartsListener.uploadRequested
                    + ", srtd ul= " + bartsListener.uploadStarted
                    + ", cmpld ul= " + bartsListener.uploadCompleted
                    + ", brkn ul= " + bartsListener.uploadBroken + ", abrt ul="
                    + bartsListener.uploadAborted;
            }
        });

        // Check correct event fireing
        assertEquals(2, bartsListener.uploadRequested);
        assertEquals(2, bartsListener.uploadStarted);
        assertEquals(1, bartsListener.uploadCompleted);
        assertEquals(1, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);

        // Check correct event fireing
        assertEquals(2, lisasListener.downloadRequested);
        // assertEquals(2, lisasListener.downloadQueued);
        assertEquals(2, lisasListener.downloadStarted);
        assertEquals(1, lisasListener.downloadCompleted);
        assertEquals("Aborted dl found! broken: "
            + lisasListener.downloadBroken, 0, lisasListener.downloadAborted);
        assertEquals(1, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);

        assertTrue(file.exists());
        // Total bytes downloaded should be == file size
        // More bytes downloaded means no resume!
        bytesDownloaded = getContollerLisa().getTransferManager()
            .getDownloadCounter().getBytesTransferred();
        // assertEquals("Mismatch, bytes Downloaded: " + bytesDownloaded
        // + ", file size: " + file.length(), bytesDownloaded, file.length());

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownItemCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals(2, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .countActiveDownloads());

        clearCompletedDownloadsAtLisa();
        assertEquals(1, lisasListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testBrokenTransferFileChanged() {
        // Register listeners
        MyTransferManagerListener bartListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartListener);
        MyTransferManagerListener lisaListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisaListener);

        // 1 Meg testfile
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1024);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        // Now change the file
        TestHelper.changeFile(testFile);

        // Database not in sync!
        TestHelper.waitMilliSeconds(2500);

        // Check correct event fireing
        assertEquals(
            "Bart. Uploads requested: " + bartListener.uploadRequested, 0,
            bartListener.uploadRequested);
        assertEquals("Bart. Uploads started: " + bartListener.uploadStarted, 0,
            bartListener.uploadStarted);
        assertEquals(
            "Bart. Uploads completed: " + bartListener.uploadCompleted, 0,
            bartListener.uploadCompleted);
        assertEquals("Bart. Uploads aborted: " + bartListener.uploadAborted, 0,
            bartListener.uploadAborted);
        assertEquals(
            "Bart. Uploads uploadBroken: " + bartListener.uploadBroken, 0,
            bartListener.uploadBroken);

        // Check correct event fireing
        assertEquals("Lisa. downloadRequested: "
            + lisaListener.downloadRequested, 1, lisaListener.downloadRequested);
        // assertEquals(2, lisasListener.downloadQueued);
        assertEquals("Lisa. downloadStarted: " + lisaListener.downloadStarted,
            0, lisaListener.downloadStarted);
        assertEquals("Lisa. downloadCompleted: "
            + lisaListener.downloadCompleted, 0, lisaListener.downloadCompleted);
        assertEquals("Lisa. downloadAborted: " + lisaListener.downloadAborted,
            1, lisaListener.downloadAborted);
        assertEquals("Lisa. downloadBroken: " + lisaListener.downloadBroken, 0,
            lisaListener.downloadBroken);
        assertEquals("Lisa. downloadsCompletedRemoved: "
            + lisaListener.downloadsCompletedRemoved, 0,
            lisaListener.downloadsCompletedRemoved);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void xtestDeltaFileNotChangedMultipe() throws Exception {
        for (int i = 0; i < 50; i++) {
            testDeltaFileNotChanged();
            tearDown();
            setUp();
        }
    }

    public void testDeltaFileNotChanged() throws InterruptedException {
        ConfigurationEntry.USE_DELTA_ON_LAN
            .setValue(getContollerBart(), "true");
        ConfigurationEntry.USE_DELTA_ON_LAN
            .setValue(getContollerLisa(), "true");

        // Register listeners
        final MyTransferManagerListener bartListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartListener);
        final MyTransferManagerListener lisaListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisaListener);

        // 1 Meg testfile
        File fbart = TestHelper
            .createRandomFile(getFolderAtBart().getLocalBase(),
                (long) (1024 * 1024 + Math.random() * 1024 * 1024));

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return lisaListener.downloadRequested >= 1
                    && lisaListener.downloadCompleted >= 1
                    && bartListener.uploadCompleted >= 1
                    && getFolderAtLisa().getKnownFiles().size() >= 1;
            }
        });
        FileInfo linfo = getFolderAtLisa().getKnownFiles().iterator().next();
        File flisa = linfo
            .getDiskFile(getContollerLisa().getFolderRepository());

        assertTrue(TestHelper.compareFiles(fbart, flisa));
        disconnectBartAndLisa();

        long oldByteCount = getFolderAtLisa().getStatistic()
            .getDownloadCounter().getBytesTransferred();
        Thread.sleep(3000);

        // Change and scan file.
        assertTrue(fbart.setLastModified(System.currentTimeMillis() + 4000));
        assertTrue("Bart lastmod: " + fbart.lastModified() + ", Lisa lastmod: "
            + flisa.lastModified(),
            fbart.lastModified() - 2000 > flisa.lastModified());
        scanFolder(getFolderAtBart());
        assertEquals(1, getFolderAtBart().getKnownItemCount());
        assertEquals(1, getFolderAtLisa().getKnownItemCount());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        FileInfo binfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(fbart, binfo, getContollerBart());
        assertEquals(1, binfo.getVersion());
        assertEquals(0, linfo.getVersion());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        assertTrue(binfo.inSyncWithDisk(fbart));
        connectBartAndLisa();

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisaListener.downloadCompleted >= 2
                    && lisaListener.downloadRequested >= 2
                    && lisaListener.downloadStarted >= 2
                    && lisaListener.downloadAborted == 0
                    && lisaListener.downloadBroken == 0
                    && bartListener.uploadRequested >= 2
                    && bartListener.uploadStarted >= 2
                    && bartListener.uploadAborted == 0
                    && bartListener.uploadBroken == 0
                    && bartListener.uploadCompleted >= 2;
            }

            public String message() {
                return "lisa: " + lisaListener + " bart: " + bartListener;
            }
        });

        assertTrue(TestHelper.compareFiles(fbart, flisa));
        assertTrue("Failed: "
            + getFolderAtLisa().getStatistic().getDownloadCounter()
                .getBytesTransferred() + " == " + oldByteCount,
            getFolderAtLisa().getStatistic().getDownloadCounter()
                .getBytesTransferred() == oldByteCount);

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testDeltaFileChanged() throws IOException {
        ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getContollerBart(),
            Boolean.TRUE.toString());
        ConfigurationEntry.USE_DELTA_ON_LAN.setValue(getContollerLisa(),
            Boolean.TRUE.toString());

        // Register listeners
        final MyTransferManagerListener bartListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartListener);
        final MyTransferManagerListener lisaListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisaListener);

        // 1 Meg testfile
        File fbart = TestHelper
            .createRandomFile(getFolderAtBart().getLocalBase(),
                (long) (1024 * 1024 + Math.random() * 1024 * 1024));

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return lisaListener.downloadCompleted >= 1
                    && lisaListener.downloadRequested >= 1
                    && bartListener.uploadCompleted >= 1
                    && getFolderAtLisa().getKnownFiles().size() == 1;
            }
        });
        FileInfo linfo = getFolderAtLisa().getKnownFiles().iterator().next();
        File flisa = linfo
            .getDiskFile(getContollerLisa().getFolderRepository());

        assertTrue(TestHelper.compareFiles(fbart, flisa));

        disconnectBartAndLisa();

        // Wait at least 3000ms
        TestHelper.waitMilliSeconds(3000);
        // Make a modification in bart's file
        TFile tmpCopy = new TFile(System.getProperty("tmp.dir"),
            fbart.getName());
        FileUtils.copyFile(fbart, tmpCopy);

        int modSize = (int) (1024 + Math.random() * 8192);
        long seek = (long) (Math.random() * (fbart.length() - modSize));

        byte[] buf = new byte[(int) seek];
        TFileInputStream in = new TFileInputStream(tmpCopy);
        in.read(buf);
        TFileOutputStream out = new TFileOutputStream(fbart);
        out.write(buf);
        for (int i = 0; i < modSize; i++) {
            out.write((int) (Math.random() * 256));
        }
        out.close();
        in.close();
        tmpCopy.rm();

        // RandomAccessFile rbart = new RandomAccessFile(fbart, "rw");
        // rbart.seek(seek);
        // for (int i = 0; i < modSize; i++) {
        // rbart.write((int) (Math.random() * 256));
        // }
        // rbart.close();

        long oldByteCount = getFolderAtLisa().getStatistic()
            .getDownloadCounter().getBytesTransferred();

        // Scan changed file
        assertTrue(fbart.lastModified() > flisa.lastModified());
        scanFolder(getFolderAtBart());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        FileInfo binfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(fbart, binfo, getContollerBart());
        assertEquals("Bart version: " + binfo.getVersion(), 1,
            binfo.getVersion());
        assertEquals(0, linfo.getVersion());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        connectBartAndLisa();
        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return lisaListener.downloadCompleted >= 2
                    && lisaListener.downloadRequested >= 2
                    && lisaListener.downloadAborted == 0
                    && lisaListener.downloadBroken == 0
                    && bartListener.uploadRequested >= 2
                    && bartListener.uploadAborted == 0
                    && bartListener.uploadBroken == 0
                    && bartListener.uploadStarted >= 2
                    && bartListener.uploadCompleted >= 2;
            }

            public String message() {
                return "lisa: completed dl= " + lisaListener.downloadCompleted
                    + ", req dl= " + lisaListener.downloadRequested
                    + ", srtd dl= " + lisaListener.downloadStarted
                    + ", brkn dl= " + lisaListener.downloadBroken
                    + ", abrt dl= " + lisaListener.downloadAborted
                    + "; bart: req ul= " + bartListener.uploadRequested
                    + ", srtd ul= " + bartListener.uploadStarted
                    + ", cmpld ul= " + bartListener.uploadCompleted
                    + ", brkn ul= " + bartListener.uploadBroken + ", abrt ul="
                    + bartListener.uploadAborted;
            }
        });

        assertTrue(TestHelper.compareFiles(fbart, flisa));
        assertTrue(
            "Expected "
                + getFolderAtLisa().getStatistic().getDownloadCounter()
                    .getBytesTransferred() + " - " + oldByteCount + " < "
                + fbart.length() / 2, getFolderAtLisa().getStatistic()
                .getDownloadCounter().getBytesTransferred()
                - oldByteCount < fbart.length() / 2);

        TestHelper.assertIncompleteFilesGone(this);
    }

    /**
     * Tests load/store of pending downloads.
     * <p>
     * TRAC #629
     */
    public void testPendingDownloadsRestore() {
        // Prepare
        getFolderAtLisa().setSyncProfile(SyncProfile.HOST_FILES);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            8 * 1024 * 1024);
        scanFolder(getFolderAtBart());

        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        getContollerLisa().getTransferManager().downloadNewestVersion(fInfo);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisa active downloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }

            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() > 0;
            }
        });

        // Break connection
        disconnectBartAndLisa();

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getPendingDownloads().size() >= 1;
            }

            public String message() {
                return "Pending downloads at lisa: "
                    + getContollerLisa().getTransferManager()
                        .getPendingDownloads().size();
            }
        });

        // Shutdown and restart lisa
        getContollerLisa().shutdown();
        startControllerLisa();
        // Lisa should have 1 pending download
        assertEquals(1, getContollerLisa().getTransferManager()
            .getPendingDownloads().size());
        connectBartAndLisa();

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisa active downloads: "
                    + getContollerLisa().getTransferManager()
                        .countActiveDownloads();
            }

            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() > 0;
            }
        });

        // No pending download no more
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisa pending downloads: "
                    + getContollerLisa().getTransferManager()
                        .getPendingDownloads();
            }

            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getPendingDownloads().isEmpty();
            }
        });

        TestHelper.assertIncompleteFilesGone(this);
    }

    public void testSwitchSyncProfileMultiple() throws Exception {
        for (int i = 0; i < 10; i++) {
            testSwitchSyncProfile();
            tearDown();
            setUp();
        }
    }

    /**
     * Test to switch sync profiles during active transfers.
     * <p>
     * TRAC #1086
     */
    public void testSwitchSyncProfile() {

        getContollerBart().getTransferManager().setUploadCPSForLAN(1000000);
        getContollerLisa().getTransferManager().setUploadCPSForLAN(1000000);

        // Prepare
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            4 * 1024 * 1024);
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 1;
            }
        });
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countAllUploads() == 1;
            }
        });

        // Switch!
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);

        // Download should break
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .countActiveDownloads() == 0;
            }
        });
        // Since this download was auto-requested it shouldn't be added to the
        // list of pending downloads.
        assertEquals(0, getContollerLisa().getTransferManager()
            .getPendingDownloads().size());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countAllUploads() == 0;
            }
        });

        // Switch back, dls should continue
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        TestHelper.waitForCondition(11, new ConditionWithMessage() {
            public boolean reached() {
                // #2557
                getContollerLisa().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
                return getContollerLisa().getTransferManager()
                    .countCompletedDownloads() == 1;
            }

            public String message() {
                return "Completed downloads at lisa: "
                    + getContollerLisa().getTransferManager()
                        .countCompletedDownloads();
            }
        });
        TestHelper.waitForCondition(10, new ConditionWithMessage() {

            public boolean reached() {
                return getContollerBart().getTransferManager()
                    .countAllUploads() == 1
                    && getContollerBart().getTransferManager()
                        .countLiveUploads() == 0;
            }

            public String message() {
                return "Barts uploads: "
                    + getContollerBart().getTransferManager().countAllUploads();
            }
        });

        TestHelper.assertIncompleteFilesGone(this);
    }

    /**
     * For checking the correct events.
     */
    private class MyTransferManagerListener implements TransferManagerListener {
        public int downloadRequested;
        public int downloadQueued;
        public int pendingDownloadEnqued;
        public int downloadStarted;
        public int downloadBroken;
        public int downloadAborted;
        public int downloadCompleted;
        public int downloadsCompletedRemoved;

        public int uploadsCompletedRemoved;
        public int uploadRequested;
        public int uploadStarted;
        public int uploadBroken;
        public int uploadAborted;
        public int uploadCompleted;

        private TransferManagerEvent lastEvent;

        public List<FileInfo> uploadsRequested = new ArrayList<FileInfo>();
        public List<FileInfo> downloadsRequested = new ArrayList<FileInfo>();
        public List<FileInfo> downloadsCompleted = new ArrayList<FileInfo>();
        private final boolean failOnSecondRequest;

        public MyTransferManagerListener(boolean failOnSecondRequest) {
            this.failOnSecondRequest = failOnSecondRequest;
        }

        public MyTransferManagerListener() {
            failOnSecondRequest = false;
        }

        public synchronized void downloadRequested(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadRequested++;
            if (downloadsRequested.contains(event.getFile())) {
                if (failOnSecondRequest) {
                    fail("Second download request for "
                        + event.getFile().toDetailString());
                } else {
                    System.err.println("Second download request for "
                        + event.getFile().toDetailString());
                }
            }
            downloadsRequested.add(event.getFile());
        }

        public synchronized void downloadQueued(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadQueued++;
            lastEvent = event;
        }

        public synchronized void downloadStarted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadStarted++;
            lastEvent = event;
        }

        public synchronized void downloadAborted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadAborted++;
        }

        public synchronized void downloadBroken(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadBroken++;
            lastEvent = event;
        }

        public synchronized void downloadCompleted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadsCompleted.add(event.getFile());
            downloadCompleted++;
            lastEvent = event;
        }

        public synchronized void completedDownloadRemoved(
            TransferManagerEvent event)
        {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            downloadsCompletedRemoved++;
            lastEvent = event;
        }

        public synchronized void pendingDownloadEnqueued(
                TransferManagerEvent event)
        {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            pendingDownloadEnqued++;
            lastEvent = event;
        }

        public synchronized void uploadRequested(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadRequested++;
            lastEvent = event;

            if (uploadsRequested.contains(event.getFile())) {
                System.err.println("Second upload request for "
                    + event.getFile().toDetailString());
            }
            uploadsRequested.add(event.getFile());
        }

        public synchronized void uploadStarted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadStarted++;
            lastEvent = event;
        }

        public synchronized void uploadAborted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadAborted++;
            lastEvent = event;
        }

        public synchronized void uploadBroken(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadAborted++;
            lastEvent = event;
        }

        public synchronized void uploadCompleted(TransferManagerEvent event) {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadCompleted++;
            lastEvent = event;
        }

        public synchronized void completedUploadRemoved(
            TransferManagerEvent event)
        {
            if (event.getFile().getFolderInfo().isMetaFolder()) {
                return;
            }
            uploadsCompletedRemoved++;
            lastEvent = event;
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

        @Override
        public String toString() {
            return "[pendingDownloadEnqued=" + pendingDownloadEnqued
                + ", downloadRequested=" + downloadRequested
                + ", downloadQueued=" + downloadQueued + ", downloadStarted="
                + downloadStarted + ", downloadBroken=" + downloadBroken
                + ", downloadAborted=" + downloadAborted
                + ", downloadCompleted=" + downloadCompleted
                + ", downloadsCompletedRemoved=" + downloadsCompletedRemoved
                + ", uploadRequested=" + uploadRequested + ", uploadStarted="
                + uploadStarted + ", uploadBroken=" + uploadBroken
                + ", uploadAborted=" + uploadAborted + ", uploadCompleted="
                + uploadCompleted + ", uploadsCompletedRemoved="
                + uploadsCompletedRemoved + "]";
        }
    }
}
