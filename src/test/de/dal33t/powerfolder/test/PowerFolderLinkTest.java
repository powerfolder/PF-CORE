package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.RemoteCommandManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Util;

public class PowerFolderLinkTest extends TwoControllerTestCase {

    private static final String BASEDIR2 = "build/test/controller2/testFolder";

    private Folder folder2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        //implement a replacement for the UI
        getContollerBart().getFolderRepository().setInvitationReceivedHandler(
            new InvitationReceivedHandler() {

                public void invitationReceived(InvitationReceivedEvent invitationRecievedEvent) {
                    File dir = new File(getContollerBart().getFolderRepository()
                        .getFoldersBasedir()
                        + System.getProperty("file.separator")
                        + Util.removeInvalidFilenameChars(invitationRecievedEvent.getInvitation().folder.name));
                    try {
                        getContollerBart().getFolderRepository().createFolder(
                            invitationRecievedEvent.getInvitation().folder, dir);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail(
                            "-----------test failed ------------");
                    }
                }

            });

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folder2 = getContollerLisa().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR2));

        // Give them time to join
        Thread.sleep(500);
    }

    public void testJoinFolderByLink() throws Exception {
        String link = folder2.getInvitation().toPowerFolderLink();
        // will be send to the first controller
        RemoteCommandManager.sendCommand(RemoteCommandManager.OPEN + link);
        // Give time for processing, connecting
        Thread.sleep(1000);

        // controller 1 should now have one folder
        assertEquals(1,
            getContollerBart().getFolderRepository().getFolders().length);
        String otherID = getContollerBart().getFolderRepository().getFolders()[0].getId();
        //Id's should match
        assertEquals(otherID, folder2.getId());
        // and both folders should have 2 members, this may fail if not
        // connected yet
        assertEquals(2, getContollerBart().getFolderRepository().getFolders()[0]
            .getMembersCount());
        assertEquals(2, folder2.getMembersCount());
    }

}
