package de.dal33t.powerfolder.test.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Test the filelist message.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileListTest extends TestCase {

    /**
     * Tests the correct splitting of the filelist.
     */
    public void testOldSplitting() {
        int nFiles = (int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75);
        FileInfo[] files = new FileInfo[nFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo(i);
        }

        // Now split
        Message[] msgs = FileList.createFileListMessages(files[0]
            .getFolderInfo(), files);

        // Test
        assertEquals(4, msgs.length);
        assertTrue(msgs[0] instanceof FileList);
        assertTrue(msgs[1] instanceof FolderFilesChanged);
        assertTrue(msgs[2] instanceof FolderFilesChanged);
        assertTrue(msgs[3] instanceof FolderFilesChanged);

        // Check content
        FileList fileList1 = (FileList) msgs[0];
        FolderFilesChanged fileList2 = (FolderFilesChanged) msgs[1];
        FolderFilesChanged fileList3 = (FolderFilesChanged) msgs[2];
        FolderFilesChanged fileList4 = (FolderFilesChanged) msgs[3];

        assertEquals(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE,
            fileList1.files.length);
        assertEquals(3, fileList1.nFollowingDeltas);
        assertEquals(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE,
            fileList2.added.length);
        assertEquals(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE,
            fileList3.added.length);
        assertEquals((int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 0.75),
            fileList4.added.length);

        for (int i = 0; i < files.length; i++) {
            if (i < Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                assertEquals(files[i], fileList1.files[i]);
            } else if (i < 2 * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                assertEquals(files[i], fileList2.added[i
                    - Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            } else if (i < 3 * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                assertEquals(files[i], fileList3.added[i - 2
                    * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            } else if (i < 4 * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                assertEquals(files[i], fileList4.added[i - 3
                    * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            }
        }
    }

    public void testNewSplitting() {
        testNewSplitting((int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75));
        testNewSplitting(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
    }

    public void testMulstipleNewSplitting() {
        for (int i = 0; i < 40; i++) {
            testNewSplitting(i * 343);
        }

    }

    /**
     * Tests the correct splitting of the filelist.
     */
    private void testNewSplitting(int nFiles) {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 0; i < nFiles; i++) {
            files.add(createRandomFileInfo(i));
        }

        // Now split. Empty blacklist
        Message[] msgs = FileList.createFileListMessages(
            createRandomFolderInfo(), files, new Blacklist());

        // Test
        assertTrue(msgs[0] instanceof FileList);
        for (int i = 1; i < msgs.length; i++) {
            assertTrue(msgs[i] instanceof FolderFilesChanged);
        }
        // Check content
        FileList fileList1 = (FileList) msgs[0];

        assertEquals("Number of expected number of delta filelists mismatch",
            fileList1.nFollowingDeltas, msgs.length - 1);

        int t = 0;
        for (int i = 0; i < files.size(); i++) {
            if (i < Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
                assertEquals(files.get(i), fileList1.files[i]);
            } else {
                t = i / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE;
                FolderFilesChanged msg = (FolderFilesChanged) msgs[t];
                // System.err.println("INDEX: " + t);
                assertEquals(files.get(i), msg.added[i - t
                    * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            }
            // else if (i < 3 * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
            // assertEquals(files.get(i), fileList3.added[i - 2
            // * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            // } else if (i < 4 * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
            // assertEquals(files.get(i), fileList4.added[i - 3
            // * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
            // }
        }
    }

    private static FileInfo createRandomFileInfo(int n) {
        FolderInfo foInfo = createRandomFolderInfo();
        FileInfo fInfo = new FileInfo(foInfo, "F # " + n + " / "
            + UUID.randomUUID().toString());
        return fInfo;
    }

    /**
     * @return
     */
    private static FolderInfo createRandomFolderInfo() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeId(), true);
        return foInfo;
    }
}
