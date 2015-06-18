package de.dal33t.powerfolder.test.folder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        assertNull(testCorruptFile(Paths
            .get("src/test-resources/FolderStatisticInfo_OOM.txt")));
        assertNotNull(testCorruptFile(Paths
            .get("src/test-resources/FolderStatisticInfo_NPE.txt")));
        assertNotNull(testCorruptFile(Paths
            .get("src/test-resources/FolderStatisticInfo_OK.txt")));

    }

    private FolderStatisticInfo testCorruptFile(Path file) {
        if (Files.notExists(file)) {
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
}
