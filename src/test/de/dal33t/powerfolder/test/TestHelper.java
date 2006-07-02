/* $Id$
 */
package de.dal33t.powerfolder.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import de.dal33t.powerfolder.util.Reject;

/**
 * Offers several helping methods for junit tests.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class TestHelper {

    private TestHelper() {
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
     * @param condition
     *            the contition to wait for
     * @return true when condition was succesfully reached. false when reached
     *         timeout
     */
    public static boolean waitForCondition(int secondsTimeout,
        Condition condition)
    {
        Reject.ifNull(condition, "Task is null");

        int i = 0;
        while (!condition.reached()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            i++;
            if (i > secondsTimeout * 100) {
                return false;
            }
        }
        return true;
    }

    /**
     * General condition which can be reached
     */
    public static interface Condition {
        /**
         * @return if the condition is reached
         */
        boolean reached();
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
            randomFile = new File(directory, UUID.randomUUID().toString()
                + ".test");
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
     * Creates a test file with name and contents in a specified directory
     * 
     * @param directory
     * @param filename
     * @param contents
     * @return
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
}
