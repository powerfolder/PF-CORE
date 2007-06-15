package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.task.SendMessageTask;

public class PowerFolderInvitationTest extends TwoControllerTestCase {

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
                        FolderSettings folderSettings = 
                                new FolderSettings(dir, SyncProfile.MANUAL_DOWNLOAD, false, true);
                        invitationRecievedEvent.getFolderRepository()
                            .createFolder(
                                invitationRecievedEvent.getInvitation().folder,
                                folderSettings);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail("-----------test failed ------------"
                            + e.getMessage());
                    }
                }

            });

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        FolderSettings folderSettings =
                new FolderSettings(TESTFOLDER_BASEDIR_LISA,
                        SyncProfile.MANUAL_DOWNLOAD, false, true);
        folderAtLisa = getContollerLisa().getFolderRepository().createFolder(
            testFolder, folderSettings);

        Thread.sleep(500);
    }

    public void testInviteViaFile() throws Exception {
        Invitation invitation = folderAtLisa.createInvitation();
        File inviteFile = new File(Controller.getTempFilesLocation(),
            folderAtLisa.getName());
        InvitationUtil.save(invitation, inviteFile);

        Invitation inviteAtBart = InvitationUtil.load(inviteFile);
        getContollerBart().getFolderRepository().invitationReceived(
            inviteAtBart, true, false);
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

    /**
     * FIXME Re-Enable this test #441
     * @throws Exception
     */
    public void xtestInviteDirectly() throws Exception {
        Invitation invitation = folderAtLisa.createInvitation();

        // Send inviation over PF to bart.
        getContollerLisa().getTaskManager().scheduleTask(
        	new SendMessageTask(invitation, 
        			getContollerLisa().getNodeManager()
        			.getConnectedNodes().get(0).getInfo()));
//        getContollerLisa().getNodeManager().getConnectedNodes().get(0)
//            .sendMessage(invitation);

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
