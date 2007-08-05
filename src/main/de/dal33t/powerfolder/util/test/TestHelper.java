/* $Id$
 */
package de.dal33t.powerfolder.util.test;

import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Reject;

/**
 * Offers several helping methods for junit tests.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class TestHelper extends Loggable {
    private static File testFile;

    private TestHelper() {
    }

    public static File getTestDir() {
        if (testFile == null) {
            File localBuildProperties = new File("build-local.properties");
            if (localBuildProperties.exists()) {
                BufferedInputStream bis = null;
                Properties props = new Properties();
                try {
                    bis = new BufferedInputStream(new FileInputStream(
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
                    testFile = new File(props.getProperty("test.dir"));
                    if (!testFile.exists()) {
                        testFile = null;
                    }
                }
            }
            if (testFile == null) {
                // propertie not set or not existing dir
                testFile = new File("build/test/");
            }
        }
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
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else if (file.isFile()) {
                    FileUtils.forceDelete(file);
                }
            } catch (IOException e) {
                // log().error(e);
            }
        }
        if (0 != testDir.listFiles().length) {
            StringBuilder b = new StringBuilder();
            for (File f : testDir.listFiles()) {
                b.append(f.getAbsolutePath() + ", ");
                // System.err.println(Arrays.asList(f.listFiles()[0].list()));
            }
            throw new IllegalStateException(
                "cleaning test dir not succeded. Files left:" + b.toString());
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
     */
    public static void waitForCondition(int secondsTimeout, Condition condition)
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
                String msg = "Timeout(" + secondsTimeout + "). Did not reach: "
                    + condition;
                if (condition instanceof ConditionWithMessage) {
                    msg = ((ConditionWithMessage) condition).message() + " | "
                        + msg;
                }
                throw new RuntimeException(msg);
            }
        }
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
        waitMilliSeconds(500);
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
            directory.mkdirs();
        }
        File randomFile;
        do {
            randomFile = new File(directory, createRandomFilename());
        } while (randomFile.exists());
        try {
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
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
        if (!file.exists() || !file.isFile() || !file.canWrite()) {
            throw new IllegalArgumentException(
                "file must be a writable existing file: "
                    + file.getAbsolutePath());
        }
        long size = (long) (500 + Math.random() * 1024);
        if (size == file.length()) {
            size += 10;
        }
        try {
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
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
            File file = new File(directory, filename);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            FileOutputStream fOut = new FileOutputStream(file);
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
        // Break scanning process
        folder.getController().setSilentMode(true);
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return folder.getController().getFolderRepository()
                    .getCurrentlyMaintainingFolder() == null;
            }
        });

        // Scan
        folder.forceScanOnNextMaintenance();
        folder.getController().setSilentMode(false);
        folder.maintain();
    }

    public static final boolean compareFiles(File a, File b) {
        FileInputStream ain, bin;
        try {
            if (a.length() != b.length()) {
                return false;
            }
            ain = new FileInputStream(a);
            bin = new FileInputStream(b);
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
}
