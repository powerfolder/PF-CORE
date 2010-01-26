package de.dal33t.powerfolder.test.disk;

import java.io.File;
import java.io.IOException;

import de.dal33t.powerfolder.disk.FolderWatcher;

import junit.framework.TestCase;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

public class JNotifyTest extends TestCase {
    private static final boolean REMOVE_WATCHERS = true;
    private static final boolean REMOVE_TEST_DIR = true;

    public void testWatchers_1_5000() {
        testWatcher(1, 5000);
    }

    public void testWatchers_5_1000() {
        testWatcher(5, 1000);
    }

    public void testWatchers_10_500() {
        testWatcher(10, 500);
    }

    public void testWatchers_50_100() {
        testWatcher(50, 100);
    }

    public void testWatchers_500_10() {
        testWatcher(500, 10);
    }

    public void testWatchers_5000_1() {
        testWatcher(5000, 1);
    }

    private void testWatcher(int nWatches, int nFiles) {
        if (!FolderWatcher.isLibLoaded()) {
            System.err.println("Not checking JNotify. Unable to load lib.");
            return;
        }
        File tmpDir = new File(System.getProperty("java.io.tmpdir"),
            "watchertest");
        int x = 1;
        while (tmpDir.exists()) {
            tmpDir = new File(System.getProperty("java.io.tmpdir"),
                "watchertest (" + x + ")");
            x++;
        }
        try {
            int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
                | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
            boolean watchSubtree = true;
            System.out.println("Testing: " + nWatches + " watcher, " + nFiles
                + " files each.");
            int[] watchIDs = new int[nWatches];
            MyJNotifyListener listener = new MyJNotifyListener();
            for (int i = 0; i < watchIDs.length; i++) {
                File dir = new File(tmpDir, "testdir-" + i);
                recursiveDelete(dir);
                assertTrue(dir.mkdirs());
                int watchID = JNotify.addWatch(dir.getAbsolutePath(), mask,
                    watchSubtree, listener);
                watchIDs[i] = watchID;
                System.out.println("Installed watch " + watchID + " on " + dir);
                writeFiles(dir, nFiles);
            }

            // Wait for events.
            Thread.sleep(2000);

            // // Cleanup
            if (REMOVE_WATCHERS) {
                for (int i = 0; i < watchIDs.length; i++) {
                    int watchID = watchIDs[i];
                    System.out.println("Removing watch " + watchID);
                    JNotify.removeWatch(watchID);
                }
            }

            int expected = nFiles * nWatches;

            assertEquals("nFileCreate event mismatch. Totals: " + listener,
                expected, listener.nFileCreated);

            assertEquals("nFileRenamed event mismatch. Totals: " + listener,
                expected, listener.nFileRenamed);

            assertEquals("nFileDeleted event mismatch. Totals: " + listener,
                expected, listener.nFileDeleted);

            assertTrue("nFileModified event mismatch. Totals: " + listener,
                listener.nFileModified >= expected * 2);

            System.out.println("ALL OK! Tested: " + nWatches + " watcher, "
                + nFiles + " files each.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (REMOVE_TEST_DIR && REMOVE_WATCHERS) {
                try {
                    recursiveDelete(tmpDir);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void writeFiles(File dir, int count) {
        for (int i = 0; i < count; i++) {
            File testFile = new File(dir, "testfile-" + i);
            try {
                testFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Unable to create testfile: " + testFile
                    + ". " + e);
            }
            testFile.renameTo(new File(dir, "testfile_moved-" + count));
            new File(dir, "testfile_moved-" + count).delete();
        }
    }

    private static final class MyJNotifyListener implements JNotifyListener {
        public int nFileCreated;
        public int nFileDeleted;
        public int nFileModified;
        public int nFileRenamed;

        public void fileCreated(int wd, String rootPath, String name) {
            nFileCreated++;
        }

        public void fileDeleted(int wd, String rootPath, String name) {
            nFileDeleted++;
        }

        public void fileModified(int wd, String rootPath, String name) {
            nFileModified++;
        }

        public void fileRenamed(int wd, String rootPath, String oldName,
            String newName)
        {
            nFileRenamed++;
        }

        public String toString() {
            return "create: " + nFileCreated + ". deleted: " + nFileDeleted
                + ". renamed: " + nFileRenamed + ". modified: " + nFileModified;
        }
    }

    /**
     * A recursive delete of a directory.
     * 
     * @param file
     *            directory to delete
     * @throws IOException
     */

    public static void recursiveDelete(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File nextFile : files) {
                recursiveDelete(nextFile);
            }
        }

        if (file.exists() && !file.delete()) {
            throw new IOException("Could not delete file "
                + file.getAbsolutePath());
        }
    }

}
