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
package de.dal33t.powerfolder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.task.SendMessageTask;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

public class PowerFolderInvitationTest extends TwoControllerTestCase {

    private Folder folderAtLisa;

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        clearInvitationHandlers(getContollerBart());
        getContollerBart().addInvitationHandler(new InvitationHandler() {

            public void gotInvitation(Invitation invitation) {
                Path dir = getContollerBart()
                    .getFolderRepository()
                    .getFoldersBasedir()
                    .resolve(
                        PathUtils
                            .removeInvalidFilenameChars(invitation.folder.getName()));
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

        FolderInfo testFolder = FolderInfoFactory.newTopFolderForTest("testFolder");

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

    @Test
    public void testInviteDirectly() throws Exception {
        Invitation invitation = folderAtLisa
            .createInvitation(FolderPermission.read(folderAtLisa.getInfo()));

        // Send invitation over PF to bart.
        getContollerLisa().getTaskManager().scheduleTask(
            new SendMessageTask(invitation, getContollerBart().getMySelf()
                .getId()));

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return findFolderByName(getContollerBart(), folderAtLisa.getName()) != null;
            }

            @Override
            public String message() {
                return "Bart does not have folder " + folderAtLisa.getName();
            }
        });

        Folder bartFolder = findFolderByName(getContollerBart(), folderAtLisa.getName());

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                Folder bf = findFolderByName(getContollerBart(), folderAtLisa.getName());
                return bf != null && bf.getMembersCount() == 2
                    && folderAtLisa.getMembersCount() == 2;
            }

            @Override
            public String message() {
                Folder bf = findFolderByName(getContollerBart(), folderAtLisa.getName());
                return "Bart members: " + (bf != null ? bf.getMembersCount() : 0)
                    + ", Lisa: " + folderAtLisa.getMembersCount();
            }
        });

        bartFolder = findFolderByName(getContollerBart(), folderAtLisa.getName());
        assertEquals(2, bartFolder.getMembersCount());
        assertEquals(2, folderAtLisa.getMembersCount());
    }

    @SuppressWarnings("unchecked")
    private void clearInvitationHandlers(Controller controller) throws Exception {
        Field field = Controller.class.getDeclaredField("invitationHandlers");
        field.setAccessible(true);
        ((List<?>) field.get(controller)).clear();
    }

    private Folder findFolderByName(Controller controller, String name) {
        for (Folder f : controller.getFolderRepository().getFolders()) {
            if (name.equals(f.getName())) {
                return f;
            }
        }
        return null;
    }
}
