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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Test the filelist message.
 * <p>
 * TODO Test in combination with blacklist
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileListTest extends TestCase {

    public void testListSplitting() {
        testListSplitting((int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75));
        testListSplitting(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
        testListSplitting(1);
    }

    public void testMulipleListSplitting() {
        for (int i = 0; i < 40; i++) {
            testListSplitting(i);
        }
        for (int i = 0; i < 40; i++) {
            testListSplitting(i * 343);
        }
    }

    /**
     * Tests the correct splitting of the filelist.
     */
    private void testListSplitting(int nFiles) {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 0; i < nFiles; i++) {
            files.add(createRandomFileInfo(i));
        }

        // Now split. Empty blacklist
        Message[] msgs = FileList.createFileListMessages(
            createRandomFolderInfo(), files, new DiskItemFilter(false));

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

    public void testDeltaSplitting() {
        testDeltaSplittingAdded((int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75));
        testDeltaSplittingAdded(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
        testDeltaSplittingAdded(1);

        testDeltaSplittingRemoved((int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 3.75));
        testDeltaSplittingRemoved(Constants.FILE_LIST_MAX_FILES_PER_MESSAGE);
        testDeltaSplittingRemoved(1);
    }

    /**
     * Tests the correct splitting of the folder files changed.
     */
    private void testDeltaSplittingAdded(int nFiles) {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 0; i < nFiles; i++) {
            files.add(createRandomFileInfo(i));
        }

        // Now split. Empty blacklist
        Message[] msgs = FolderFilesChanged.createFolderFilesChangedMessages(
            createRandomFolderInfo(), files, new DiskItemFilter(false), true);

        // Test
        for (int i = 0; i < msgs.length; i++) {
            assertTrue(msgs[i] instanceof FolderFilesChanged);
        }

        int t = 0;
        for (int i = 0; i < files.size(); i++) {
            t = i / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE;
            FolderFilesChanged msg = (FolderFilesChanged) msgs[t];
            assertEquals(files.get(i), msg.added[i - t
                * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
        }
    }

    private void testDeltaSplittingRemoved(int nFiles) {
        List<FileInfo> files = new ArrayList<FileInfo>();
        for (int i = 0; i < nFiles; i++) {
            files.add(createRandomFileInfo(i));
        }

        // Now split. Empty blacklist
        Message[] msgs = FolderFilesChanged.createFolderFilesChangedMessages(
            createRandomFolderInfo(), files, new DiskItemFilter(false), false);

        // Test
        for (int i = 0; i < msgs.length; i++) {
            assertTrue(msgs[i] instanceof FolderFilesChanged);
        }

        int t = 0;
        for (int i = 0; i < files.size(); i++) {
            t = i / Constants.FILE_LIST_MAX_FILES_PER_MESSAGE;
            FolderFilesChanged msg = (FolderFilesChanged) msgs[t];
            assertEquals(files.get(i), msg.removed[i - t
                * Constants.FILE_LIST_MAX_FILES_PER_MESSAGE]);
        }
    }

    private static FileInfo createRandomFileInfo(int n) {
        FolderInfo foInfo = createRandomFolderInfo();
        FileInfo fInfo = new FileInfo(foInfo, "F # " + n + " / "
            + UUID.randomUUID().toString());
        return fInfo;
    }

    private static FolderInfo createRandomFolderInfo() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeId());
        return foInfo;
    }
}
