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
package de.dal33t.powerfolder.test.folder;

import java.util.ArrayList;
import java.util.List;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the correct automatic sending of the filelist.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SendFileListTest extends TwoControllerTestCase {
    private MyMessageListener lisasListener;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        makeFriends();
        lisasListener = new MyMessageListener();
        getContollerLisa().getNodeManager().addMessageListenerToAllNodes(
            lisasListener);
    }

    public void testAfterFolderJoin() {
        joinTestFolder(SyncProfile.HOST_FILES);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return !lisasListener.messages.isEmpty();
            }
        });
        assertEquals(1, lisasListener.messages.size());
        assertTrue(lisasListener.messages.get(0) instanceof FileList);
        FileList list = (FileList) lisasListener.messages.get(0);
        assertEquals(0, list.nFollowingDeltas);
        assertEquals(0, list.files.length);
    }

    public void testSendAfterScan() {
        joinTestFolder(SyncProfile.HOST_FILES);
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return !lisasListener.messages.isEmpty();
            }
        });
        lisasListener.messages.clear();

        // Create scenario
        int nFiles = 10;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(5, new Condition() {
            public boolean reached() {
                return !lisasListener.messages.isEmpty();
            }
        });

        // Test
        assertEquals(1, lisasListener.messages.size());
        assertTrue(lisasListener.messages.get(0) instanceof FolderFilesChanged);
        FolderFilesChanged list = (FolderFilesChanged) lisasListener.messages
            .get(0);
        assertEquals(nFiles, list.added.length);
        assertNull(list.removed);
    }

    private static final class MyMessageListener implements MessageListener {
        public List<Message> messages = new ArrayList<Message>();

        public void handleMessage(Member source, Message message) {
            if (message instanceof FileList
                || message instanceof FolderFilesChanged)
            {
                messages.add(message);
            }
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }
}
