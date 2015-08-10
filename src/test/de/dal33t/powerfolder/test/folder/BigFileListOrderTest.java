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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the transfer of big filelists. Especially check if the splitted list
 * come in the right order.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class BigFileListOrderTest extends TwoControllerTestCase {

    private FileList filelist;
    private boolean receivedInitalFileList = false;
    private int receivedDeltas = 0;
    private boolean error;
    private List<Message> receiveMessages = new ArrayList<Message>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testTransferBigFileList() throws ConnectionException {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeFolderId());

        getContollerBart().getNodeManager().addMessageListenerToAllNodes(
            new MyMessageListener());

        Member bartAtLisa = getContollerLisa().getNodeManager().getNode(
            getContollerBart().getMySelf().getId());

        int nFiles = (int) (Constants.FILE_LIST_MAX_FILES_PER_MESSAGE * 15.75);
        FileInfo[] files = new FileInfo[nFiles];
        for (int i = 0; i < files.length; i++) {
            files[i] = createRandomFileInfo(foInfo);
        }
        // Now split
        final Message[] msgs = FileList.create4Test(foInfo, Arrays
            .asList(files), new DiskItemFilter());

        for (int i = 0; i < msgs.length; i++) {
            Message message = msgs[i];
            bartAtLisa.sendMessage(message);
        }

        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                return receivedInitalFileList
                    && receivedDeltas >= msgs.length - 1;
            }

            public String message() {
                return "Expected " + (msgs.length - 1) + " deltas but got "
                    + receivedDeltas + ". Got initial list? "
                    + receivedInitalFileList;
            }
        });

        assertFalse(
            "Received a delta filelist (FolderFilesChanged) before inital FileList",
            error);
        assertTrue(receivedInitalFileList);
        assertEquals(msgs.length - 1, receivedDeltas);

        // String .intern check after serialization
        assertEquals(files[0].getRelativeName(), filelist.files[0]
            .getRelativeName());
        // assertSame(
        // "Filename string instances are not SAME (==). Intern missing at serialization?",
        // files[0].getRelativeName(), filelist.files[0].getRelativeName());
    }

    private static FileInfo createRandomFileInfo(FolderInfo foInfo) {
        MemberInfo m = new MemberInfo("test", "ID", null);
        AccountInfo a = new AccountInfo("test", "ID", null, false);
        return FileInfoFactory.unmarshallExistingFile(foInfo, UUID.randomUUID()
            .toString().intern(), IdGenerator.makeFileId(), 0, m, a,
            new Date(), 0, null, false, null);
    }

    private final class MyMessageListener implements MessageListener {

        public void handleMessage(Member source, Message message) {
            System.err.println("Received: " + message);
            if (message instanceof FileList) {
                filelist = (FileList) message;
                receivedInitalFileList = true;
            } else if (message instanceof FolderFilesChanged) {
                receivedDeltas++;
                if (!receivedInitalFileList) {
                    fail("Received a delta filelist (FolderFilesChanged) before inital FileList");
                    error = true;
                }
            }
            receiveMessages.add(message);
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }
}
