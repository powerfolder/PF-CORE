package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Util;

public class PowerFolderInvitationTest extends TwoControllerTestCase {

    private static final String BASEDIR_LISA = "build/test/controller2/testFolder";

    private Folder folderAtLisa;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
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
                                dir);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("-----------test failed ------------"
                            + e.getMessage());
                    }
                }

            });

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folderAtLisa = getContollerLisa().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR_LISA));

        // Give them time to join
        Thread.sleep(500);
    }

    public void testJoinFolderByInvite() throws Exception {
        Invitation invitation = folderAtLisa.getInvitation();
        File inviteFile = new File(Controller.getTempFilesLocation(),
            folderAtLisa.getName());
        InvitationUtil.save(invitation, inviteFile);

        Invitation inviteAtBart = InvitationUtil.load(inviteFile);
        getContollerBart().getFolderRepository().invitationReceived(
            inviteAtBart, true, false);
        Thread.sleep(1000);

        // controller lisa should now have one folder
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
