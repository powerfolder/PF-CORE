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
 * $Id$
 */
package de.dal33t.powerfolder.util.test;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import junit.framework.Assert;
import junit.framework.TestCase;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * Offers several helping methods for junit tests.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class TestHelper {
    /**
     * FIXME: Change to "server.powerfolder.com" after successfully migration.
     */
    public static final String DEV_SYSTEM_CONNECT_STRING = "relay001.node.powerfolder.com";

    public static final InetSocketAddress ONLINE_STORAGE_ADDRESS = new InetSocketAddress(
        "access.powerfolder.com", 1337);

    private static final Collection<Controller> STARTED_CONTROLLER = Collections
        .synchronizedCollection(new ArrayList<Controller>());

    private static File testFile;

    private TestHelper() {
    }

    public static void addStartedController(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        STARTED_CONTROLLER.add(controller);
    }

    public static void removeStartedController(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        STARTED_CONTROLLER.remove(controller);
    }

    public static void shutdownStartedController() {
        synchronized (STARTED_CONTROLLER) {
            for (Controller controller : STARTED_CONTROLLER) {
                System.err.println("Shutting down started/left controller: "
                    + controller);
                controller.shutdown();
            }
            STARTED_CONTROLLER.clear();
        }
    }

    public static String deadlockCheck() {
        try {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            long[] ids = mx.findDeadlockedThreads();
            if (ids == null) {
                return "NO DEADLOCKS!";
            }
            Assert.assertTrue(ids.length > 0);
            ThreadInfo[] info = mx.getThreadInfo(ids, true, true);
            StringWriter lout = new StringWriter();
            PrintWriter out = new PrintWriter(lout);
            for (ThreadInfo i : info) {
                out.println("Thread " + i);
                out.println("Complete Trace:");
                Exception tmp = new Exception();
                tmp.setStackTrace(i.getStackTrace());
                tmp.printStackTrace(out);
            }
            out.close();
            return lout.toString();
        } catch (UnsupportedOperationException e) {
            return e.toString();
        }
    }

    /**
     * Makes sure that no (incomplete) files are left over.
     * 
     * @param folderList
     */
    public static void assertIncompleteFilesGone(List<Folder> folderList) {
        for (Folder f : folderList) {
            File transfers = new TFile(f.getSystemSubDir(), "transfers");
            if (!transfers.exists()) {
                return;
            }
            File[] list = transfers.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().contains("(incomplete)")
                        && pathname.length() == 0L;
                }
            });
            if (list != null && list.length != 0) { // Always fail in here
                for (File file : list) {
                    boolean deleted = file.delete();
                    TestCase.assertTrue(
                        "Incomplete file still open somewhere, couldn't delete: "
                            + file, deleted);
                }
                TestCase
                    .fail("(incomplete) files found, but all could be deleted!");
            }
        }
    }

    public static void assertIncompleteFilesGone(Folder... folders) {
        assertIncompleteFilesGone(Arrays.asList(folders));
    }

    public static void assertIncompleteFilesGone(
        final MultipleControllerTestCase testCase)
    {
        waitForCondition(10, new Condition() {
            public boolean reached() {
                for (Controller c : testCase.getControllers()) {
                    if (c.getTransferManager().countActiveDownloads() != 0
                        || c.getTransferManager().countActiveUploads() != 0)
                    {
                        return false;
                    }
                }
                return true;
            }
        });
        List<Folder> list = new LinkedList<Folder>();
        for (Controller c : testCase.getControllers()) {
            list.add(testCase.getFolderOf(c));
        }
        assertIncompleteFilesGone(list);
    }

    public static void assertIncompleteFilesGone(
        final TwoControllerTestCase testCase)
    {
        waitForCondition(20, new Condition() {
            public boolean reached() {
                for (Controller c : new Controller[]{
                    testCase.getContollerLisa(), testCase.getContollerBart()})
                {
                    if (c.getTransferManager().countActiveDownloads() != 0) {
                        return false;
                    }
                }
                return true;
            }
        });

        assertIncompleteFilesGone(testCase.getFolderAtBart(),
            testCase.getFolderAtLisa());
    }

    public static File getTestDir() {
        if (testFile == null) {
            File localBuildProperties = new TFile("build-local.properties");
            if (localBuildProperties.exists()) {
                BufferedInputStream bis = null;
                Properties props = new Properties();
                try {
                    bis = new BufferedInputStream(new TFileInputStream(
                        localBuildProperties));
                    props.load(bis);
                } catch (IOException e) {

                } finally {
                    try {
                        if (bis != null) {
                            bis.close();
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
                if (props.containsKey("test.dir")) {
                    testFile = new TFile(props.getProperty("test.dir"));
                    if (!testFile.exists()) {
                        testFile = null;
                    }
                }
            }
            if (testFile == null) {
                // propertie not set or not existing dir
                testFile = new TFile("build/test/");
            }
        }
        testFile.mkdirs();
        return testFile;
    }

    /** deletes all files in the test dir */
    public static void cleanTestDir() {
        File testDir = getTestDir();

        File[] files = testDir.listFiles();
        if (files == null) {
            return;
        }
        System.out.println("Cleaning test dir (" + testDir + ") ("
            + files.length + " files/dirs)");
        for (File file : files) {
            try {
                FileUtils.recursiveDelete(file);
            } catch (IOException e) {
                TestHelper.waitMilliSeconds(250);
                try {
                    FileUtils.recursiveDelete(file);
                } catch (IOException e1) {
                    TestHelper.waitMilliSeconds(5000);
                    try {
                        FileUtils.recursiveDelete(file);
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        if (0 != testDir.listFiles().length) {
            StringBuilder b = new StringBuilder();
   listFiles(testDir, b);
            throw new IllegalStateException(
                "cleaning test dir not succeded. Files left: " + b.toString());
        }
    }

    private static void listFiles(File base, StringBuilder b) {
        File[] files = base.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                listFiles(file, b);
            } else {
                b.append(file.getAbsolutePath() + ", ");
            }
        }
    }

    /**
     * Wraps <code>Thread.sleep()</code> and just try/catches the
     * InterruptedException
     * 
     * @param ms
     * @throws RuntimeException
     *             if InterruptedException occoured
     */
    public static void waitMilliSeconds(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits for a condition to reach and/or a timeout.
     * 
     * @param secondsTimeout
     *            the timeout in seconds to wait for the condition.
     * @param condition
     *            the contition to wait for
     * @throws RuntimeException
     *             if timeout occoured
     * @return the number of miliseconds waited.
     */
    public static long waitForCondition(int secondsTimeout, Condition condition)
    {
        Reject.ifNull(condition, "Task is null");

        int i = 0;
        long start = System.currentTimeMillis();
        while (!condition.reached()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            i++;
            if (System.currentTimeMillis() > start + ((long) secondsTimeout)
                * 1000)
            {
                String msg = "Timeout(" + secondsTimeout + ") on " + condition;
                if (condition instanceof ConditionWithMessage) {
                    msg = ((ConditionWithMessage) condition).message() + " | "
                        + msg;
                }
                throw new RuntimeException(msg);
            }
        }
        return System.currentTimeMillis() - start;
    }

    /**
     * Waits for all events in the Event dispatching thread to complete.
     */
    public static void waitForEmptyEDT() {
        Runnable nothing = new Runnable() {
            public void run() {
            }
        };
        try {
            EventQueue.invokeAndWait(nothing);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for EDT", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error while waiting for EDT", e);
        }
    }

    /**
     * Creates a file with a random name and random content in the directory.
     * 
     * @param directory
     *            the dir to place the file
     * @return the file that was created
     * @throws RuntimeException
     *             if something went wrong
     */
    public static File createRandomFile(File directory) {
        return createRandomFile(directory, (long) (500 + Math.random() * 1024));
    }

    /**
     * Creates a file with a random name and random content with a defined size
     * in the directory. The file is guaranteed to be new.
     * 
     * @param directory
     *            the dir to place the file
     * @param size
     *            the size of the file
     * @return the file that was created
     * @throws RuntimeException
     *             if something went wrong
     */
    public static File createRandomFile(File directory, long size) {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException(
                    "Unable to create directory of random file: "
                        + directory.getAbsolutePath());
            }
        }
        File randomFile;
        do {
            randomFile = new TFile(directory, createRandomFilename());
        } while (randomFile.exists());
        try {
            OutputStream fOut = new BufferedOutputStream(new TFileOutputStream(
                randomFile));
            for (int i = 0; i < size; i++) {
                fOut.write((int) (Math.random() * 256));
            }

            fOut.close();
            if (!randomFile.exists()) {
                throw new IOException("Could not create random file '"
                    + randomFile.getAbsolutePath() + "'");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return randomFile;
    }

    /**
     * will overwrite file with random contents.
     * 
     * @param file
     *            the file to change.
     */
    public static void changeFile(File file) {
        changeFile(file, -1);
    }

    /**
     * will overwrite file with random contents.
     * 
     * @param file
     *            the file to change.
     * @param size
     *            the size of the file.
     */
    public static void changeFile(File file, long size) {
        if (!file.exists() || !file.isFile() || !file.canWrite()) {
            throw new IllegalArgumentException(
                "file must be a writable existing file: "
                    + file.getAbsolutePath());
        }
        if (size < 0) {
            size = (long) (500 + Math.random() * 1024);
            if (size == file.length()) {
                size += 10;
            }
        }
        try {
            OutputStream fOut = new BufferedOutputStream(new TFileOutputStream(
                file));
            for (int i = 0; i < size; i++) {
                fOut.write((int) (Math.random() * 256));
            }
            fOut.close();
            if (!file.exists()) {
                throw new IOException("Could not create random file '"
                    + file.getAbsolutePath() + "'");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a test file with name with random content in a specified
     * directory
     * 
     * @param directory
     * @param filename
     * @return the created file
     * @throws RuntimeException
     *             if something went wrong
     */
    public static File createRandomFile(File directory, String filename) {
        byte[] content = new byte[400 + (int) (Math.random() * 10000)];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (Math.random() * 256);
        }
        return createTestFile(directory, filename, content);
    }

    /**
     * Creates a test file with name and contents in a specified directory
     * 
     * @param directory
     * @param filename
     * @param contents
     * @return the created file.
     * @throws RuntimeException
     *             if something went wrong
     */
    public static File createTestFile(File directory, String filename,
        byte[] contents)
    {
        try {
            File file = new TFile(directory, filename);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            OutputStream fOut = new TFileOutputStream(file);
            fOut.write(contents);
            fOut.close();

            if (!file.exists()) {
                throw new IOException("Could not create random file '"
                    + file.getAbsolutePath() + "'");
            }

            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a random name for a file.
     * <p>
     * Ensures, that there are upper and lower case characters in the filename.
     * 
     * @return the filename with upper/lower case characters.
     */
    public static final String createRandomFilename() {
        String str = UUID.randomUUID().toString();
        StringBuffer buf = new StringBuffer();
        int l = 1 + (int) (Math.random() * (str.length() - 1));
        for (int i = 0; i < l; i++) {
            char c;
            if (i % 2 == 0) {
                c = Character.toLowerCase(str.charAt(i));
            } else {
                c = Character.toUpperCase(str.charAt(i));
            }
            buf.append(c);
        }
        
        // Not all file names have extensions. So half the time, do not add an extension.
        if (Math.random() >= 0.5) {
            buf.append(".test");
        }
        return buf.toString();
    }

    // Scanning help **********************************************************

    /**
     * Scans a folder and waits for the scan to complete.
     * 
     * @param folder
     */
    public static void scanFolder(final Folder folder) {
        // if (!folder.getSyncProfile().isInstantSync()) {
        // throw new IllegalStateException(
        // "Folder has auto-detect of local files disabled: " + folder
        // + ". sync profile: " + folder.getSyncProfile());
        // }
        boolean pausedBefore = folder.getController().isPaused();
        // Break scanning process
        folder.getController().setPaused(true);
        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                return folder.getController().getFolderRepository()
                    .getCurrentlyMaintainingFolder() == null
                    && folder.getController().getFolderRepository()
                        .getFolderScanner().getCurrentScanningFolder() == null;
            }
        });

        // Scan // Ignore mass deletion
        if (!folder.scanLocalFiles(true)) {
            throw new RuntimeException("Unable to scan " + folder
                + ". Last scan result: " + folder.getLastScanResultState()
                + ". Device disconnected? " + folder.isDeviceDisconnected());
        }
        folder.getController().setPaused(pausedBefore);
    }

    public static final boolean compareFiles(File a, File b) {
        InputStream ain, bin;
        try {
            if (a.length() != b.length()) {
                return false;
            }
            ain = new TFileInputStream(a);
            bin = new TFileInputStream(b);
            byte[] abuf = new byte[8192], bbuf = new byte[8192];
            int aread;
            while ((aread = ain.read(abuf)) > 0) {
                int bread, bpos = 0, rem = aread;
                while ((bread = bin.read(bbuf, bpos, rem)) > 0) {
                    bpos += bread;
                    rem -= bread;
                }
                for (int i = 0; i < aread; i++) {
                    if (abuf[i] != bbuf[i]) {
                        return false;
                    }
                }
            }

            ain.close();
            bin.close();
            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Searches for transfers that don't have an associated counter-part.
     * 
     * @param swarmingTest
     * @return
     */
    public static String findUnmatchedTransfers(
        MultipleControllerTestCase swarmingTest)
    {
        return findUnmatchedTransfers(swarmingTest.getControllers());
    }

    public static String findUnmatchedTransfers(
        Collection<Controller> controllers)
    {
        List<Upload> uploads = new ArrayList<Upload>();
        for (Controller c : controllers) {
            uploads.addAll(c.getTransferManager().getActiveUploads());
        }
        for (Controller c : controllers) {
            for (DownloadManager dm : c.getTransferManager()
                .getActiveDownloads())
            {
                for (Iterator<Upload> i = uploads.iterator(); i.hasNext();) {
                    Upload u = i.next();
                    if (u.getFile().equals(dm.getFileInfo())
                        && u.getPartner().getId().equals(c.getMySelf().getId()))
                    {
                        i.remove();
                    }
                }
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("Unmatched uploads:");
        for (Upload u : uploads) {
            b.append(u).append(',');
        }
        return b.toString();
    }

}
