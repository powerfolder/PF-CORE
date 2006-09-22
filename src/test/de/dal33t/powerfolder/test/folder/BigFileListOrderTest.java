/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.folder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests the transfer of big filelists. Especially check if the splitted list
 * come in the right order.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class BigFileListOrderTest extends TwoControllerTestCase {

    private boolean receivedInitalFileList = false;
    private int receivedDeltas = 0;
    private boolean error;
    private List<Message> receiveMessages = new ArrayList<Message>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    public void testTransferBigFileList() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeId(), true);

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
        final Message[] msgs = FileList.createFileListMessages(foInfo, files);

        bartAtLisa.sendMessagesAsynchron(msgs);

        TestHelper.waitForCondition(10, new TestHelper.Condition() {
            public boolean reached() {
                return receivedInitalFileList
                    && receivedDeltas >= msgs.length - 1;
            }
        });

        assertFalse(
            "Received a delta filelist (FolderFilesChanged) before inital FileList",
            error);
        assertTrue(receivedInitalFileList);
        assertEquals(msgs.length - 1, receivedDeltas);
    }

    private static FileInfo createRandomFileInfo(FolderInfo foInfo) {
        FileInfo fInfo = new FileInfo(foInfo, UUID.randomUUID().toString());
        return fInfo;
    }

    private final class MyMessageListener implements MessageListener {
        private MyMessageListener() {
        }

        public void handleMessage(Member source, Message message) {
            if (message instanceof FileList) {
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
    }
}
