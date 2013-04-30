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
 * $Id: TestHelper.java 18443 2012-04-01 01:40:52Z harry $
 */
package de.dal33t.powerfolder.util.test;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;

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

    private static Path testFile;

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
            Path transfers = f.getSystemSubDir().resolve("transfers");
            if (Files.notExists(transfers)) {
                return;
            }

            Filter<Path> filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    String name = entry.getFileName().toString();
                    return name.contains("(incomplete)") && name.length() == 0L;
                }
            };

            try (DirectoryStream<Path> files = Files.newDirectoryStream(
                transfers, filter)) {
                for (Path file : files) {
                    try {
                        Files.delete(file);
                    } catch (IOException ioe) {
                        TestCase
                            .fail("Incomplete file still open somewhere, couldn't delete: "
                                + file);
                    }
                }
                return;
            } catch (IOException ioe) {

            }
            TestCase
                .fail("(incomplete) files found, but all could be deleted!");
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

    public static Path getTestDir() {
        if (testFile == null) {
            Path localBuildProperties = Paths.get("build-local.properties")
                .toAbsolutePath();
            if (Files.exists(localBuildProperties)) {
                Properties props = new Properties();
                try (BufferedInputStream bis = new BufferedInputStream(
                    Files.newInputStream(localBuildProperties))) {
                    props.load(bis);
                } catch (IOException e) {

                }
                if (props.containsKey("test.dir")) {
                    testFile = Paths.get(props.getProperty("test.dir"))
                        .toAbsolutePath();
                    if (Files.notExists(testFile)) {
                        testFile = null;
                    }
                }
            }
            if (testFile == null) {
                // propertie not set or not existing dir
                testFile = Paths.get("build/test/").toAbsolutePath();
            }
        }

        try {
            Files.createDirectories(testFile);
        } catch (IOException ioe) {
            return null;
        }
        return testFile;
    }

    /** deletes all files in the test dir */
    public static void cleanTestDir() {
        Path testDir = getTestDir();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testDir)) {
            int count = PathUtils.getNumberOfSiblings(testDir);
            System.out.println("Cleaning test dir (" + testDir + ") (" + count
                + " files/dirs)");

            for (Path file : stream) {
                count--;
                try {
                    PathUtils.recursiveDelete(file);
                } catch (IOException e) {
                    TestHelper.waitMilliSeconds(250);
                    try {
                        PathUtils.recursiveDelete(file);
                    } catch (IOException e1) {
                        TestHelper.waitMilliSeconds(5000);
                        try {
                            PathUtils.recursiveDelete(file);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }

            if (0 != count) {
                StringBuilder b = new StringBuilder();
                listFiles(testDir, b);
                throw new IllegalStateException(
                    "cleaning test dir not succeded. " + count
                        + " files left: " + b.toString());
            }
        } catch (IOException ioe) {

            return;
        }
    }

    private static void listFiles(Path base, StringBuilder b) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    listFiles(file, b);
                } else {
                    b.append(file.toAbsolutePath() + ", ");
                }
            }
        } catch (IOException ioe) {
            return;
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

        long start = System.currentTimeMillis();
        while (!condition.reached()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
    public static Path createRandomFile(Path directory) {
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
    public static Path createRandomFile(Path directory, long size) {
        if (Files.notExists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ioe) {
                throw new RuntimeException(
                    "Unable to create directory of random file: "
                        + directory.toAbsolutePath());
            }
        }
        Path randomFile;
        do {
            randomFile = directory.resolve(createRandomFilename());
        } while (Files.exists(randomFile));
        try (OutputStream fOut = new BufferedOutputStream(
            Files.newOutputStream(randomFile, StandardOpenOption.CREATE))) {
            for (int i = 0; i < size; i++) {
                fOut.write((int) (Math.random() * 256));
            }

            if (Files.notExists(randomFile)) {
                throw new IOException("Could not create random file '"
                    + randomFile.toAbsolutePath() + "'");
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
    public static void changeFile(Path file) {
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
    public static void changeFile(Path file, long size) {
        if (Files.notExists(file) || !Files.isRegularFile(file)
            || !Files.isWritable(file))
        {
            throw new IllegalArgumentException(
                "file must be a writable existing file: "
                    + file.toAbsolutePath());
        }

        try {
            if (size < 0) {
                size = (long) (500 + Math.random() * 1024);
                if (size == Files.size(file)) {
                    size += 10;
                }
            }
        } catch (IOException ioe) {
            size = 10;
        }

        try (OutputStream fOut = new BufferedOutputStream(
            Files.newOutputStream(file))) {
            for (int i = 0; i < size; i++) {
                fOut.write((int) (Math.random() * 256));
            }
            if (Files.notExists(file)) {
                throw new IOException("Could not create random file '"
                    + file.toAbsolutePath() + "'");
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
    public static Path createRandomFile(Path directory, String filename) {
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
    public static Path createTestFile(Path directory, String filename,
        byte[] contents)
    {
        Path file = directory.resolve(filename);

        try (OutputStream fOut = Files.newOutputStream(file)) {
            Path parent = file.getParent();
            if (Files.notExists(parent)) {
                Files.createDirectories(parent);
            }

            fOut.write(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (Files.notExists(file)) {
            throw new RuntimeException("Could not create random file '"
                + file.toAbsolutePath() + "'");
        }

        return file;
    }

    /**
     * Creats a random name for a file.
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
        buf.append(".test");
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

    public static final boolean compareFiles(Path a, Path b) {
        try (InputStream ain = Files.newInputStream(a);
            InputStream bin = Files.newInputStream(b)) {
            if (Files.size(a) != Files.size(b)) {
                return false;
            }

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
