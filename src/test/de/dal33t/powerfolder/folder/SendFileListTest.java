/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.FolderRelatedMessage;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the correct automatic sending of the filelist.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SendFileListTest extends TwoControllerTestCase {
    private MyMessageListener lisasListener;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        makeFriends();
        lisasListener = new MyMessageListener();
        getContollerLisa().getNodeManager().addMessageListenerToAllNodes(
            lisasListener);
    }

    @Test
    public void testAfterFolderJoin() {
        joinTestFolder(SyncProfile.HOST_FILES);
        final FolderInfo folderInfo = getFolderAtBart().getInfo();
        final FolderInfo metaFolderInfo = getContollerBart()
            .getFolderRepository().getMetaFolder(folderInfo).getInfo();
        TestHelper.waitForCondition(20, new ConditionWithMessage() {
            public boolean reached() {
                FileList metaList = lisasListener.fileListFor(metaFolderInfo);
                return lisasListener.fileListFor(folderInfo) != null
                    && metaList != null && containsMembersFile(metaList);
            }

            public String message() {
                return "Lisa did not receive a file list for both folders. Received: "
                    + lisasListener.messages;
            }
        });
        assertEquals(2, lisasListener.fileLists().size(),
            "Received: " + lisasListener.messages);
        assertFalse(lisasListener.deltaPrecededFileList(),
            "No delta may precede the file lists. Received: " + lisasListener.messages);
        FileList list = lisasListener.fileListFor(folderInfo);
        assertEquals(0, list.nFollowingDeltas);
        assertEquals(0, list.files.length);
        list = lisasListener.fileListFor(metaFolderInfo);
        assertEquals(0, list.nFollowingDeltas);
        // Members file
        assertEquals( 1,
            list.files.length,"Files? " + Arrays.asList(list.files));
    }

    @Test
    public void testSendAfterScan() {
        joinTestFolder(SyncProfile.HOST_FILES);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return lisasListener.receivedMessagesFor(getFolderAtBart()
                    .getInfo());
            }
        });
        TestHelper.waitMilliSeconds(2000);
        lisasListener.ignoreMetaFolder = true;
        lisasListener.messages.clear();

        // Create scenario
        int nFiles = 10;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return lisasListener.receivedMessagesFor(getFolderAtBart()
                    .getInfo());
            }
        });

        // Test
        assertEquals(1, lisasListener.messages.size());
        assertTrue(
            lisasListener.messages.get(0) instanceof FolderFilesChanged,"Wrong message received: " + lisasListener.messages.get(0));
        FolderFilesChanged list = (FolderFilesChanged) lisasListener.messages
            .get(0);
        assertEquals(nFiles, list.getFiles().length);
        assertNull(list.getRemoved());
    }

    private static boolean containsMembersFile(FileList list) {
        for (FileInfo fInfo : list.files) {
            if (Folder.METAFOLDER_MEMBERS.equals(fInfo.getRelativeName())) {
                return true;
            }
        }
        return false;
    }

    private static final class MyMessageListener implements MessageListener {
        public List<FolderRelatedMessage> messages = new CopyOnWriteArrayList<FolderRelatedMessage>();
        public boolean ignoreMetaFolder;

        public List<FolderFilesChanged> deltas() {
            List<FolderFilesChanged> deltas = new ArrayList<FolderFilesChanged>();
            for (FolderRelatedMessage m : messages) {
                if (m instanceof FolderFilesChanged) {
                    deltas.add((FolderFilesChanged) m);
                }
            }
            return deltas;
        }

        public boolean deltaPrecededFileList() {
            Set<FolderInfo> listed = new HashSet<FolderInfo>();
            for (FolderRelatedMessage m : messages) {
                if (m instanceof FileList) {
                    listed.add(m.folder);
                } else if (m instanceof FolderFilesChanged
                    && !listed.contains(m.folder))
                {
                    return true;
                }
            }
            return false;
        }

        public List<FileList> fileLists() {
            List<FileList> fileLists = new ArrayList<FileList>();
            for (FolderRelatedMessage m : messages) {
                if (m instanceof FileList) {
                    fileLists.add((FileList) m);
                }
            }
            return fileLists;
        }

        public FileList fileListFor(FolderInfo foInfo) {
            for (FileList list : fileLists()) {
                if (list.folder.equals(foInfo)) {
                    return list;
                }
            }
            return null;
        }

        public void handleMessage(Member source, Message message) {
            if (message instanceof FileList
                || message instanceof FolderFilesChanged)
            {
                FolderRelatedMessage fm = (FolderRelatedMessage) message;
                if (ignoreMetaFolder && fm.folder.isMetaFolder()) {
                    return;
                }
                messages.add(fm);
            }
        }

        public boolean receivedMessagesFor(FolderInfo foInfo) {
            for (FolderRelatedMessage m : messages) {
                if (m.folder.equals(foInfo)) {
                    return true;
                }
            }
            return false;
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }
}
