package de.dal33t.powerfolder.test.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

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
        joinTestFolder(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
    }

    public void testFileCopyCert8() throws IOException {
        File testFileBart = new File(getFolderAtBart().getLocalBase(),
            "cert8.db");
        File origFile = new File("src/test-resources/cert8.db");
        FileUtils.copyFile(origFile, testFileBart);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals(1, getFolderAtBart().getKnownFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownFilesCount();
            }
        });

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());

        File testFileLisa = new File(getFolderAtLisa().getLocalBase(),
            "cert8.db");
        assertEquals(origFile.length(), testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());
    }

    public void testFileCopyURLclassifier2() throws IOException {
        File testFileBart = new File(getFolderAtBart().getLocalBase(),
            "urlclassifier2.sqlite");
        File origFile = new File("src/test-resources/urlclassifier2.sqlite");
        FileUtils.copyFile(origFile, testFileBart);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals(1, getFolderAtBart().getKnownFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownFilesCount();
            }
        });

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());

        File testFileLisa = new File(getFolderAtLisa().getLocalBase(),
            "urlclassifier2.sqlite");
        assertEquals(origFile.length(), testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());
    }

    public void testSmallFileCopy() throws IOException {
        File testFileBart = new File(getFolderAtBart().getLocalBase(),
            "TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFileBart);
        byte[] testContent = "This is the contenent of the testfile".getBytes();
        fOut.write(testContent);
        fOut.close();

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        assertEquals(1, getFolderAtBart().getKnownFilesCount());

        // Give them time to copy
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return 1 == getFolderAtLisa().getKnownFilesCount();
            }
        });

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());

        File testFileLisa = new File(getFolderAtLisa().getLocalBase(),
            "TestFile.txt");
        assertEquals(testContent.length, testFileLisa.length());
        assertEquals(testFileBart.length(), testFileLisa.length());
    }

    public void testFileUpdate() throws IOException {
        // First copy file
        testSmallFileCopy();

        final File testFile1 = new File(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1, true);
        fOut.write("-> Next content<-".getBytes());
        fOut.close();

        // Readin file content
        FileInputStream fIn = new FileInputStream(testFile1);
        byte[] content1 = new byte[fIn.available()];
        fIn.read(content1);
        fIn.close();

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        // Give them time to copy
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return testFile1.length() == getFolderAtLisa().getKnownFiles()
                    .iterator().next().getSize();
            }
        });

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        FileInfo testFileInfo2 = getFolderAtLisa().getKnownFiles().iterator()
            .next();
        assertEquals(testFile1.length(), testFileInfo2.getSize());

        // Read content
        File testFile2 = testFileInfo2.getDiskFile(getContollerLisa()
            .getFolderRepository());
        fIn = new FileInputStream(testFile2);
        byte[] conten2 = new byte[fIn.available()];
        fIn.read(conten2);
        fIn.close();

        // Check version
        assertEquals(1, testFileInfo2.getVersion());

        // Check content
        assertEquals(new String(content1), new String(conten2));
    }

    public void testEmptyFileCopy() throws IOException {
        // Register listeners
        final MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        File testFile1 = new File(getFolderAtBart().getLocalBase()
            + "/TestFile.txt");
        FileOutputStream fOut = new FileOutputStream(testFile1);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue(testFile1.exists());

        File testFile2 = new File(getFolderAtBart().getLocalBase()
            + "/subdir/TestFile2.txt");
        assertTrue(testFile2.getParentFile().mkdirs());
        fOut = new FileOutputStream(testFile2);
        fOut.write(new byte[]{});
        fOut.close();
        assertTrue(testFile2.exists());

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(100, new ConditionWithMessage() {
            public boolean reached() {
                return tm2Listener.downloadRequested >= 2
                    && tm2Listener.downloadCompleted >= 2
                /* && tm1Listener.uploadCompleted >= 1 */;
            }

            public String message() {
                return "Lisa dls requested " + tm2Listener.downloadRequested
                    + ", completed " + tm2Listener.downloadCompleted;
            }
        });

        // Check correct event fireing
        // assertEquals(1, tm1Listener.uploadRequested);
        // assertEquals(1, tm1Listener.uploadStarted);
        // assertEquals(1, tm1Listener.uploadCompleted);
        assertEquals(0, tm1Listener.uploadRequested);
        assertEquals(0, tm1Listener.uploadStarted);
        assertEquals(0, tm1Listener.uploadCompleted);
        assertEquals(0, tm1Listener.uploadAborted);
        assertEquals(0, tm1Listener.uploadBroken);

        // Check correct event fireing
        assertEquals(2, tm2Listener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(1, tm2Listener.downloadQueued);
        // assertEquals(1, tm2Listener.downloadStarted);
        assertEquals(2, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadAborted);
        assertEquals(0, tm2Listener.downloadBroken);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(2, getFolderAtLisa().getKnownFilesCount());
        // 3 physical files (1 file + 1 system dir + 1 subdir)
        assertEquals(3, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        // give time for event firering
        // Thread.sleep(500);
        // assertEquals(2, tm2Listener.downloadsCompletedRemoved);
    }

    /**
     * 
     */
    private void clearCompletedDownloadsAtLisa() {
        // Clear completed downloads
        List<DownloadManager> list = getContollerLisa()
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
        final MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        // 12 Meg testfile
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            12 * 1024 * 1024);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadRequested >= 1
                    && tm2Listener.downloadCompleted >= 1
                    && tm1Listener.uploadCompleted >= 1;
            }
        });

        // Check correct event fireing
        assertEquals(1, tm1Listener.uploadRequested);
        assertEquals(1, tm1Listener.uploadStarted);
        assertEquals(1, tm1Listener.uploadCompleted);
        assertEquals(0, tm1Listener.uploadAborted);
        assertEquals(0, tm1Listener.uploadBroken);

        // Check correct event fireing
        assertEquals(1, tm2Listener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(1, tm2Listener.downloadQueued);
        assertEquals(1, tm2Listener.downloadStarted);
        assertEquals(1, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadAborted);
        assertEquals(0, tm2Listener.downloadBroken);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals(2, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(1, tm2Listener.downloadsCompletedRemoved);
    }

    public void testMultipleFilesCopy() {
        // Register listeners
        final MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        final int nFiles = 35;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());

        // Wait for copy (timeout 50)
        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadRequested >= nFiles
                    && tm2Listener.downloadCompleted >= nFiles
                    && tm1Listener.uploadCompleted >= nFiles;
            }
        });

        // Check correct event fireing
        assertEquals(0, tm1Listener.uploadAborted);
        assertEquals(0, tm1Listener.uploadBroken);
        assertEquals(nFiles, tm1Listener.uploadRequested);
        assertEquals(nFiles, tm1Listener.uploadStarted);
        assertEquals(nFiles, tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertEquals(nFiles, tm2Listener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(nFiles, tm2Listener.downloadQueued);
        assertEquals(nFiles, tm2Listener.downloadStarted);
        assertEquals(nFiles, tm2Listener.downloadCompleted);
        assertEquals(0, tm2Listener.downloadAborted);
        assertEquals(0, tm2Listener.downloadBroken);
        assertEquals(0, tm2Listener.downloadsCompletedRemoved);

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, tm2Listener.downloadsCompletedRemoved);
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
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());

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
        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());
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
        // assertEquals(nFiles, tm2Listener.downloadQueued);
        assertEquals(nFiles, lisasListener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);
    }

    public void testManyPow2FilesCopy() {
        // Register listeners
        final MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        final int nFiles = 450;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
                1 << (i % 18));
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());

        // Wait for copy
        TestHelper.waitForCondition(200, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadRequested >= nFiles
                    && tm2Listener.downloadCompleted >= nFiles
                    && tm1Listener.uploadCompleted >= nFiles;
            }
        });

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, tm2Listener.downloadsCompletedRemoved);
    }

    public void testMultipleManPow2() throws Exception {
        for (int i = 0; i < 10; i++) {
            testManyPow2FilesCopy();
            tearDown();
            setUp();
        }
    }

    public void testManyIncreasingFilesCopy() {
        // Register listeners
        final MyTransferManagerListener tm1Listener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(tm1Listener);
        final MyTransferManagerListener tm2Listener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(tm2Listener);

        final int nFiles = 100;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
                (long) ((i << 7) + Math.random() * (1 << 7) - 1) + 1);
        }

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());

        // Wait for copy
        TestHelper.waitForCondition(200, new Condition() {
            public boolean reached() {
                return tm2Listener.downloadRequested >= nFiles
                    && tm2Listener.downloadCompleted >= nFiles
                    && tm1Listener.uploadCompleted >= nFiles;
            }
        });
        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, tm2Listener.downloadsCompletedRemoved);
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
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
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
        assertEquals(nFiles, getFolderAtBart().getKnownFilesCount());

        // Wait for copy
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return lisasListener.downloadCompleted >= nFiles
                    && lisasListener.downloadRequested >= nFiles
                    && lisasListener.downloadRequested >= nFiles;
                /* && tm1Listener.uploadCompleted >= nFiles */
            }
        });

        // Test ;)
        assertEquals(nFiles, getFolderAtLisa().getKnownFilesCount());
        // test physical files (1 + 1 system dir)
        assertEquals(nFiles + 1, getFolderAtLisa().getLocalBase().list().length);

        // Check correct event fireing
        assertEquals(0, bartsListener.uploadAborted);
        assertEquals(0, bartsListener.uploadBroken);
        // assertEquals(nFiles, tm1Listener.uploadRequested);
        // assertEquals(nFiles, tm1Listener.uploadStarted);
        // assertEquals(nFiles, tm1Listener.uploadCompleted);

        // Check correct event fireing
        assertEquals(0, lisasListener.downloadAborted);
        assertEquals(0, lisasListener.downloadBroken);
        assertEquals(0, lisasListener.downloadsCompletedRemoved);
        assertEquals(nFiles, lisasListener.downloadRequested);
        // We can't rely on that all downloads have been queued.
        // Might be started fast! So now queued message is sent
        // assertEquals(nFiles, tm2Listener.downloadQueued);
        // assertEquals(nFiles, tm2Listener.downloadStarted);
        assertEquals(nFiles, lisasListener.downloadCompleted);

        // No active downloads?!
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(nFiles, lisasListener.downloadsCompletedRemoved);
    }

    public void testMultipleResumeTransfer() throws Exception {
        for (int i = 0; i < 10; i++) {
            testResumeTransfer();
            tearDown();
            setUp();
        }
    }

    /**
     * Tests the copy and download resume of a big file.
     * <p>
     * TRAC #415
     */
    public void testResumeTransfer() {
        getContollerBart().setSilentMode(true);
        getContollerLisa().setSilentMode(true);
        ConfigurationEntry.UPLOADLIMIT_LAN.setValue(getContollerBart(), "2000");
        ConfigurationEntry.UPLOADLIMIT_LAN.setValue(getContollerLisa(), "2000");

        // Register listeners
        final MyTransferManagerListener bartsListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartsListener);
        final MyTransferManagerListener lisasListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisasListener);

        // testfile
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 8 * 1024 * 1024);
        testFile.setLastModified(System.currentTimeMillis() - 1000L * 60 * 60);

        // Let him scan the new content
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getIncomingFiles(true).size() >= 1;
            }
        });

        FileInfo fInfo = getFolderAtLisa().getIncomingFiles(true).iterator()
            .next();
        File file = fInfo.getDiskFile(getContollerLisa().getFolderRepository());
        final File incompleteFile = new File(file.getParentFile(),
            "(incomplete) " + file.getName());
        FileInfo bartFInfo = getFolderAtBart().getKnownFiles().iterator()
            .next();
        File bartFile = bartFInfo.getDiskFile(getContollerBart()
            .getFolderRepository());
        assertEquals(bartFile.lastModified(), bartFInfo.getModifiedDate()
            .getTime());

        // Let them copy some ~1 megs
        final long mbUntilBreak = 1;
        TestHelper.waitForCondition(100, new Condition() {
            public boolean reached() {
                return incompleteFile.length() > mbUntilBreak * 1024 * 1024;
            }
        });

        // Disconnected
        disconnectBartAndLisa();

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
        assertEquals(bytesDownloaded, incompleteFile.length());
        // System.err.println("Incomplete file: " +
        // incompleteFile.lastModified()
        // + ", size: " + incompleteFile.length());
        assertEquals(bartFile.lastModified(), bartFInfo.getModifiedDate()
            .getTime());
        assertTrue(
            "Last modified date mismatch of orignial file and incompleted dl file",
            Util.equalsFileDateCrossPlattform(bartFile.lastModified(),
                incompleteFile.lastModified()));
        assertEquals(bartFInfo.getModifiedDate().getTime(), incompleteFile
            .lastModified());

        System.err.println("Transferred " + incompleteFile.length() + " bytes");

        // Reconnect /Resume transfer
        connectBartAndLisa();

        // Wait untill download is started
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadStarted >= 2;
            }

            public String message() {
                return "Lisa download started: "
                    + lisasListener.downloadStarted;
            }
        });

        // Temp file should be greater than 3mb already
        // TODO: I added speed limits above because on my machine the transfer
        // was too fast and the
        // file was completed already. Please check if this test is correct.
        assertTrue("Temp file should be greater than " + mbUntilBreak
            + "mb already. got " + Format.formatBytes(incompleteFile.length()),
            incompleteFile.length() > mbUntilBreak * 1024 * 1024);

        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public boolean reached() {
                return lisasListener.downloadRequested >= 1
                    && lisasListener.downloadCompleted >= 1
                    && bartsListener.uploadCompleted >= 1;
            }

            public String message() {
                return "Lisa downloads completed "
                    + lisasListener.downloadCompleted
                    + ". Bart uploads completed "
                    + bartsListener.uploadCompleted;
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
        // assertEquals(2, tm2Listener.downloadQueued);
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
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        // 2 physical files (1 + 1 system dir)
        assertEquals(2, getFolderAtLisa().getLocalBase().list().length);

        // No active downloads?
        assertEquals(0, getContollerLisa().getTransferManager()
            .getActiveDownloadCount());

        clearCompletedDownloadsAtLisa();
        assertEquals(1, lisasListener.downloadsCompletedRemoved);
    }

    public void testBrokenTransferFileChanged() {
        // Register listeners
        final MyTransferManagerListener bartListener = new MyTransferManagerListener();
        getContollerBart().getTransferManager().addListener(bartListener);
        final MyTransferManagerListener lisaListener = new MyTransferManagerListener();
        getContollerLisa().getTransferManager().addListener(lisaListener);

        // 1 Meg testfile
        File testFile = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase(), 1024);

        // Let him scan the new content
        scanFolder(getFolderAtBart());
        getContollerBart().setSilentMode(true);

        // Now change the file
        TestHelper.changeFile(testFile);

        // Database not in sync!
        TestHelper.waitMilliSeconds(2000);

        // Check correct event fireing
        assertEquals(0, bartListener.uploadRequested);
        assertEquals(0, bartListener.uploadStarted);
        assertEquals(0, bartListener.uploadCompleted);
        assertEquals(0, bartListener.uploadAborted);
        assertEquals(0, bartListener.uploadBroken);

        // Check correct event fireing
        assertEquals(1, lisaListener.downloadRequested);
        // assertEquals(2, tm2Listener.downloadQueued);
        assertEquals(0, lisaListener.downloadStarted);
        assertEquals(0, lisaListener.downloadCompleted);
        assertEquals(1, lisaListener.downloadAborted);
        assertEquals(0, lisaListener.downloadBroken);
        assertEquals(0, lisaListener.downloadsCompletedRemoved);
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
                    && bartListener.uploadCompleted >= 1;
            }
        });
        FileInfo linfo = getFolderAtLisa().getKnownFiles().iterator().next();
        File flisa = linfo
            .getDiskFile(getContollerLisa().getFolderRepository());

        assertTrue(TestHelper.compareFiles(fbart, flisa));

        disconnectBartAndLisa();
        fbart.setLastModified(System.currentTimeMillis());

        long oldByteCount = getFolderAtLisa().getStatistic()
            .getDownloadCounter().getBytesTransferred();

        // Scan changed file
        assertTrue(fbart.lastModified() > flisa.lastModified());

        do {
            fbart.setLastModified(System.currentTimeMillis());
            scanFolder(getFolderAtBart());
            Thread.sleep(500);
        } while (!getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        FileInfo binfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(fbart, binfo, getContollerBart());
        assertEquals(1, binfo.getVersion());
        assertEquals(0, linfo.getVersion());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        connectBartAndLisa();
        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return lisaListener.downloadCompleted >= 2
                    && lisaListener.downloadRequested >= 2
                    && bartListener.uploadCompleted >= 2;
            }

            public String message() {
                return "lisa: completed dl= " + lisaListener.downloadCompleted
                    + ", req dl= " + lisaListener.downloadRequested
                    + "; bart: completed ul= " + bartListener.uploadCompleted;
            }
        });

        assertTrue(TestHelper.compareFiles(fbart, flisa));
        assertTrue("Failed: "
            + getFolderAtLisa().getStatistic().getDownloadCounter()
                .getBytesTransferred() + " == " + oldByteCount,
            getFolderAtLisa().getStatistic().getDownloadCounter()
                .getBytesTransferred() == oldByteCount);
    }

    public void testDeltaFileChanged() throws IOException, InterruptedException
    {
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
                    && bartListener.uploadCompleted >= 1;
            }
        });
        FileInfo linfo = getFolderAtLisa().getKnownFiles().iterator().next();
        File flisa = linfo
            .getDiskFile(getContollerLisa().getFolderRepository());

        assertTrue(TestHelper.compareFiles(fbart, flisa));

        disconnectBartAndLisa();
        // Make a modification in bart's file
        int modSize = (int) (1024 + Math.random() * 8192);
        RandomAccessFile rbart = new RandomAccessFile(fbart, "rw");
        rbart.seek((long) (Math.random() * (fbart.length() - modSize)));
        for (int i = 0; i < modSize; i++) {
            rbart.write((int) (Math.random() * 256));
        }
        rbart.close();

        long oldByteCount = getFolderAtLisa().getStatistic()
            .getDownloadCounter().getBytesTransferred();

        // Scan changed file
        assertTrue(fbart.lastModified() > flisa.lastModified());

        do {
            fbart.setLastModified(System.currentTimeMillis());
            scanFolder(getFolderAtBart());
            Thread.sleep(500);
        } while (!getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        FileInfo binfo = getFolderAtBart().getKnownFiles().iterator().next();
        assertFileMatch(fbart, binfo, getContollerBart());
        assertEquals(1, binfo.getVersion());
        assertEquals(0, linfo.getVersion());
        assertTrue(getFolderAtBart().getKnownFiles().iterator().next()
            .isNewerThan(getFolderAtLisa().getKnownFiles().iterator().next()));
        connectBartAndLisa();
        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                return lisaListener.downloadCompleted >= 2
                    && lisaListener.downloadRequested >= 2
                    && bartListener.uploadCompleted >= 2;
            }

            public String message() {
                return "lisas dl completed: " + lisaListener.downloadCompleted
                    + ", barts ul completed: " + bartListener.uploadCompleted;
            }
        });

        assertTrue(TestHelper.compareFiles(fbart, flisa));
        assertTrue(getFolderAtLisa().getStatistic().getDownloadCounter()
            .getBytesTransferred()
            - oldByteCount < fbart.length() / 2);
    }

    /**
     * Checks the problem detection that get caused by diffrent cases of
     * filenames on a non-case sensitive OS (like Windows).
     * <P>
     * TRAC #232
     */
    public void testFilenameCaseProblemDetection() {
        MyFilenameProblemHandler lisasHandler = new MyFilenameProblemHandler();
        MyFilenameProblemHandler bartsHandler = new MyFilenameProblemHandler();
        getContollerLisa().getFolderRepository().setFileNameProblemHandler(
            lisasHandler);
        getContollerBart().getFolderRepository().setFileNameProblemHandler(
            bartsHandler);

        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            "TESTFILE.TXT");
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
            "testFile.txt");
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            "samedir/OTHER/case/xxx.xls");
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase(),
            "SAMEDIR/other/case/xxx.xls");

        disconnectBartAndLisa();

        // Let them scan the new content
        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        connectBartAndLisa();

        // Problem detection should happen
        if (OSUtil.isWindowsSystem()) {
            assertEquals(0, lisasHandler.events.size());
            assertEquals(0, bartsHandler.events.size());
        } else {
            assertEquals(1, lisasHandler.events.size());
            assertEquals(1, bartsHandler.events.size());
        }
    }

    /**
     * Tests load/store of pending downloads.
     * <p>
     * TRAC #629
     */
    public void testPendingDownloadsResotre() {
        // Prepare
        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase(),
            8 * 1024 * 1024);
        scanFolder(getFolderAtBart());

        FileInfo fInfo = getFolderAtBart().getKnownFiles().iterator().next();
        getContollerLisa().getTransferManager().downloadNewestVersion(fInfo);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisa active downloads: "
                    + getContollerLisa().getTransferManager()
                        .getActiveDownloadCount();
            }

            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getActiveDownloadCount() > 0;
            }
        });

        // Break connection
        disconnectBartAndLisa();

        // Lisa should have 1 pending download
        assertEquals(1, getContollerLisa().getTransferManager()
            .getPendingDownloads().size());

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
                        .getActiveDownloadCount();
            }

            public boolean reached() {
                return getContollerLisa().getTransferManager()
                    .getActiveDownloadCount() > 0;
            }
        });
        // No pending download no more
        assertEquals(0, getContollerLisa().getTransferManager()
            .getPendingDownloads().size());
    }

    private final class MyFilenameProblemHandler implements
        FileNameProblemHandler
    {
        public List<FileNameProblemEvent> events = new ArrayList<FileNameProblemEvent>();

        public void fileNameProblemsDetected(
            FileNameProblemEvent fileNameProblemEvent)
        {
            events.add(fileNameProblemEvent);
        }
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

        public int uploadRequested;
        public int uploadStarted;
        public int uploadBroken;
        public int uploadAborted;
        public int uploadCompleted;

        private int transferProblems;

        public List<FileInfo> uploadsRequested = new ArrayList<FileInfo>();
        public List<FileInfo> downloadsRequested = new ArrayList<FileInfo>();

        public synchronized void downloadRequested(TransferManagerEvent event) {
            downloadRequested++;
            if (downloadsRequested.contains(event.getFile())) {
                System.err.println("Second download request for "
                    + event.getFile().toDetailString());
            }
            downloadsRequested.add(event.getFile());
        }

        public synchronized void downloadQueued(TransferManagerEvent event) {
            downloadQueued++;
        }

        public synchronized void downloadStarted(TransferManagerEvent event) {
            downloadStarted++;
        }

        public synchronized void downloadAborted(TransferManagerEvent event) {
            downloadAborted++;
        }

        public synchronized void downloadBroken(TransferManagerEvent event) {
            downloadBroken++;
        }

        public synchronized void downloadCompleted(TransferManagerEvent event) {
            downloadCompleted++;
        }

        public synchronized void completedDownloadRemoved(
            TransferManagerEvent event)
        {
            downloadsCompletedRemoved++;
        }

        public synchronized void pendingDownloadEnqueud(
            TransferManagerEvent event)
        {
            pendingDownloadEnqued++;
        }

        public synchronized void uploadRequested(TransferManagerEvent event) {
            uploadRequested++;

            if (uploadsRequested.contains(event.getFile())) {
                System.err.println("Second upload request for "
                    + event.getFile().toDetailString());
            }
            uploadsRequested.add(event.getFile());
        }

        public synchronized void uploadStarted(TransferManagerEvent event) {
            uploadStarted++;

        }

        public synchronized void uploadAborted(TransferManagerEvent event) {
            uploadAborted++;
        }

        public synchronized void uploadBroken(TransferManagerEvent event) {
            uploadAborted++;
        }

        public synchronized void uploadCompleted(TransferManagerEvent event) {
            uploadCompleted++;
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }
}
