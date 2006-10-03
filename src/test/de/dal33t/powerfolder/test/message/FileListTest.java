/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.message;

import java.util.UUID;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Constants;
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
    public void testSplitting() {
        int nFiles = (int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75);
        FileInfo[] files = new FileInfo[nFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo();
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

    private static FileInfo createRandomFileInfo() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeId(), true);

        FileInfo fInfo = new FileInfo(foInfo, UUID.randomUUID().toString());
        return fInfo;
    }
}
