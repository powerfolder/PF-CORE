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
package de.dal33t.powerfolder.test;

import java.nio.file.Path;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.task.SendMessageTask;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class PowerFolderInvitationTest extends TwoControllerTestCase {

    private Folder folderAtLisa;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        // implement a replacement for the UI
        getContollerBart().addInvitationHandler(new InvitationHandler() {

            public void gotInvitation(Invitation invitation) {
                Path dir = getContollerBart()
                    .getFolderRepository()
                    .getFoldersBasedir()
                    .resolve(
                        PathUtils
                            .removeInvalidFilenameChars(invitation.folder.name));
                try {
                    FolderSettings folderSettings = new FolderSettings(dir,
                        SyncProfile.HOST_FILES, 0);
                    getContollerBart().getFolderRepository().createFolder(
                        invitation.folder, folderSettings);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("-----------test failed ------------" + e.getMessage());
                }
            }

        });

        FolderInfo testFolder = new FolderInfo("testFolder",
            IdGenerator.makeFolderId());

        FolderSettings folderSettings = new FolderSettings(
            TESTFOLDER_BASEDIR_LISA, SyncProfile.HOST_FILES, 0);
        folderAtLisa = getContollerLisa().getFolderRepository().createFolder(
            testFolder, folderSettings);

        Thread.sleep(500);
    }

    // public void testInviteViaFile() throws Exception {
    // Invitation invitation = folderAtLisa.createInvitation();
    // Path inviteFile = Controller.getTempFilesLocation().resolve(
    // folderAtLisa.getName());
    // InvitationUtil.save(invitation, inviteFile);
    //
    // Invitation inviteAtBart = InvitationUtil.load(inviteFile);
    // getContollerBart().invitationReceived(inviteAtBart);
    // Thread.sleep(5000);
    //
    // // controller bart should now have one folder
    // assertEquals(1, getContollerBart().getFolderRepository().getFolders()
    // .size());
    // String otherID = getContollerBart().getFolderRepository().getFolders()
    // .iterator().next().getId();
    // // Id's should match
    // assertEquals(otherID, folderAtLisa.getId());
    // // and both folders should have 2 members, this may fail if not
    // // connected yet
    // assertEquals(2, getContollerBart().getFolderRepository().getFolders()
    // .iterator().next().getMembersCount());
    // assertEquals(2, folderAtLisa.getMembersCount());
    // }

    public void testInviteDirectly() throws Exception {
        Invitation invitation = folderAtLisa.createInvitation();

        LoggingManager.setConsoleLogging(Level.FINER);
        // Send invitation over PF to bart.
        getContollerLisa().getTaskManager().scheduleTask(
            new SendMessageTask(invitation, getContollerBart().getMySelf()
                .getId()));

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return getContollerBart().getFolderRepository().getFolders()
                    .size() == 1;
            }

            @Override
            public String message() {
                return "Bart does not have any folder setup: "
                    + getContollerBart().getFolderRepository().getFolders()
                        .size();
            }
        });

        // controller bart should now have one folder
        assertEquals(1, getContollerBart().getFolderRepository().getFolders()
            .size());
        String otherID = getContollerBart().getFolderRepository().getFolders()
            .iterator().next().getId();
        // Id's should match
        assertEquals(otherID, folderAtLisa.getId());
        // and both folders should have 2 members, this may fail if not
        // connected yet

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return getContollerBart().getFolderRepository().getFolders()
                    .iterator().next().getMembersCount() == 2
                    && folderAtLisa.getMembersCount() == 2;
            }

            @Override
            public String message() {
                return "Bart does not have 2 members in folder: "
                    + getContollerBart().getFolderRepository().getFolders()
                        .iterator().next().getMembersCount() + " or Lisa: "
                    + folderAtLisa.getMembersCount();
            }
        });

        assertEquals(2, getContollerBart().getFolderRepository().getFolders()
            .iterator().next().getMembersCount());
        assertEquals(2, folderAtLisa.getMembersCount());
    }
}
