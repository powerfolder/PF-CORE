package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.RemoteCommandManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Util;

public class PowerFolderLinkTest extends TwoControllerTestCase {

    private Folder folderAtLisa;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        // implement a replacement for the UI
        getContollerBart().getFolderRepository().setInvitationReceivedHandler(
            new InvitationReceivedHandler() {

                public void invitationReceived(
                    InvitationReceivedEvent invitationRecievedEvent)
                {
                    File dir = new File(invitationRecievedEvent
                        .getFolderRepository().getFoldersBasedir()
                        + System.getProperty("file.separator")
                        + Util
                            .removeInvalidFilenameChars(invitationRecievedEvent
                                .getInvitation().folder.name));
                    try {
                        invitationRecievedEvent.getFolderRepository()
                            .createFolder(
                                invitationRecievedEvent.getInvitation().folder,
                                dir, SyncProfile.MANUAL_DOWNLOAD, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("-----------test failed ------------");
                    }
                }

            });

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folderAtLisa = getContollerLisa().getFolderRepository().createFolder(
            testFolder, TESTFOLDER_BASEDIR_LISA, SyncProfile.MANUAL_DOWNLOAD, false);
        
        Thread.sleep(500);
    }

    public void testJoinFolderByLink() throws Exception {
        String link = folderAtLisa.getInvitation().toPowerFolderLink();
        // will be send to the first controller
        RemoteCommandManager.sendCommand(RemoteCommandManager.OPEN + link);
        // Give time for processing, connecting
        Thread.sleep(1000);

        // controller bart should now have one folder
        assertEquals(1,
            getContollerBart().getFolderRepository().getFolders().length);
        String otherID = getContollerBart().getFolderRepository().getFolders()[0]
            .getId();
        // Id's should match
        assertEquals(otherID, folderAtLisa.getId());
        // and both folders should have 2 members, this may fail if not
        // connected yet
        assertEquals(2,
            getContollerBart().getFolderRepository().getFolders()[0]
                .getMembersCount());
        assertEquals(2, folderAtLisa.getMembersCount());
    }

}
