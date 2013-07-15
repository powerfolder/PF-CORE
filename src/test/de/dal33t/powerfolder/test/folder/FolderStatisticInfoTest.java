package de.dal33t.powerfolder.test.folder;

import java.io.File;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FolderStatisticInfo;

public class FolderStatisticInfoTest extends TestCase {

    // public void testLoadAll() {
    // File baseDir = new File(
    // "C:\\Users\\sprajc.POWERFOLDER\\Desktop\\powerfolder_daily_2013-06-15_04-10");
    // testLoad(baseDir);
    // }

    /**
     * PFS-818: Exceptions while running with dynamic folder mounting / shared
     * storage
     */
    public void testCorruptFiles() {
        assertNull(testCorruptFile(new File(
            "src/test-resources/FolderStatisticInfo_OOM.txt")));
        assertNull(testCorruptFile(new File(
            "src/test-resources/FolderStatisticInfo_NPE.txt")));
        assertNotNull(testCorruptFile(new File(
            "src/test-resources/FolderStatisticInfo_OK.txt")));

    }

    private FolderStatisticInfo testCorruptFile(File file) {
        if (!file.exists()) {
            fail("Testfile not found " + file);
        }
        try {
            return FolderStatisticInfo.load(file);
        } catch (Throwable t) {
            fail("Throwable " + t);
            t.printStackTrace();
        }
        return null;
    }

    private void testLoad(File dir) {
        File[] files = dir.listFiles();
        int i = 0;
        for (File file : files) {
            i++;
            if (files.length > 1) {
                // System.err.println("Processing " + i + "/" + files.length);
            }
            if (file.isDirectory()) {
                testLoad(file);
            } else {
                FolderStatisticInfo info = FolderStatisticInfo.load(file);
                if (info == null) {
                    System.err.println("Unable to read: " + file);
                } else if (!info.isValid()) {
                    System.err.println(info);
                }
            }
        }
    }
}
