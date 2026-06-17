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
import java.nio.file.Path;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.RemoteCommandManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class RemoteCommandManagerTest extends TwoControllerTestCase {
    private Path oldDir;

    @BeforeEach
    protected void setUp() throws Exception {
        assertFalse(
            RemoteCommandManager.hasRunningInstance(3458),"PowerFolder already running on port 3458");
        super.setUp();
        ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS.setValue(getContollerBart(), false);
        ConfigurationEntry.LOOK_FOR_FOLDER_CANDIDATES.setValue(getContollerBart(), false);
        connectBartAndLisa();
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);
        oldDir = getFolderAtBart().getLocalBase();
        getContollerBart().getFolderRepository().removeFolder(
            getFolderAtBart(), true);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Bart did not leave the folder. members at lisa: "
                    + getFolderAtLisa().getMembersCount();
            }

            public boolean reached() {
                return getFolderAtLisa().getMembersCount() == 1;
            }
        });
        assertTrue(
                RemoteCommandManager.hasRunningInstance(3458),"PowerFolder already running on port 3458");
    }

    @Test
    public void testJoinExistingFolder() {
        getContollerLisa().getOSClient().getAccount().getOSSubscription().setStorageSizeGB(1);
        getContollerBart().getOSClient().getAccount().getOSSubscription().setStorageSizeGB(1);

        assertEquals(1, getFolderAtLisa().getMembersCount());
        boolean sent = RemoteCommandManager.sendCommand(3458,
            RemoteCommandManager.MAKEFOLDER + "dir=" + oldDir.toAbsolutePath()
                + ";id=" + getFolderAtLisa().getId() + ";dlscript=what.bat");
        assertTrue(sent);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Bart did not join the folder. members at lisa: "
                    + getFolderAtLisa().getMembersCount();
            }

            public boolean reached() {
                return getFolderAtLisa().getMembersCount() == 2;
            }
        });
        Folder folderAtBart = findFolderByName(getContollerBart(), "testFolder");
        assertNotNull(folderAtBart, "Bart should have testFolder");

        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION,
            folderAtBart.getSyncProfile());
        assertEquals("what.bat", folderAtBart.getDownloadScript());
    }

    @Test
    public void testCreateNewFolder() {
        getContollerLisa().getOSClient().getAccount().getOSSubscription().setStorageSizeGB(1);
        getContollerBart().getOSClient().getAccount().getOSSubscription().setStorageSizeGB(1);

        assertNull(getFolderAtBart());
        assertEquals(1, getFolderAtLisa().getMembersCount());
        boolean sent = RemoteCommandManager
            .sendCommand(
                3458,
                RemoteCommandManager.MAKEFOLDER
                    + "dir="
                    + oldDir.toAbsolutePath()
                    + ";name=XXX"
                    + ";syncprofile=false,false,false,false,5,true,22,0,m,Backup daily at 2200");
        assertTrue(sent);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return findFolderByName(getContollerBart(), "XXX") != null;
            }
            public String message() {
                return "Bart did not create folder XXX";
            }
        });
        assertEquals(1, getFolderAtLisa().getMembersCount());
        Folder folderAtBart = findFolderByName(getContollerBart(), "XXX");
        assertNotNull(folderAtBart);
        assertEquals(
            "false,false,false,false,5,true,22,0,m,Backup daily at 2200,false",
            folderAtBart.getSyncProfile().getFieldList());

        // Test if dupes don't appear:
        ConfigurationEntry.FOLDER_CREATE_AVOID_DUPES.setValue(
            getContollerBart(), Boolean.TRUE.toString());
        Folder oldFolderAtBart = folderAtBart;
        sent = RemoteCommandManager.sendCommand(3458,
            RemoteCommandManager.MAKEFOLDER + "dir=" + oldDir.toAbsolutePath()
                + ";name=XXX"
                + ";syncprofile=true,true,true,true,5,false,22,0,m,Auto-sync");
        assertTrue(sent);
        TestHelper.waitMilliSeconds(1000);
        folderAtBart = findFolderByName(getContollerBart(), "XXX");
        assertNotNull(folderAtBart);
        assertEquals("true,true,true,true,5,false,22,0,m,Auto-sync,false",
            folderAtBart.getSyncProfile().getFieldList());
        assertEquals(oldFolderAtBart.getId(), folderAtBart.getId());
    }

    @Test
    public void testRemoveFolder() {
        assertEquals(1, getFolderAtLisa().getMembersCount());

        assertTrue(RemoteCommandManager.sendCommand(1155,
            RemoteCommandManager.REMOVEFOLDER));
        assertTrue(RemoteCommandManager.sendCommand(1155,
            RemoteCommandManager.REMOVEFOLDER + "dir=C:\\Dir"));
        assertTrue(RemoteCommandManager.sendCommand(1155,
            RemoteCommandManager.REMOVEFOLDER + "name=Folder"));
        assertTrue(RemoteCommandManager.sendCommand(1155,
            RemoteCommandManager.REMOVEFOLDER + "id=theid"));

        // Wrong commands. Should be still there.
        assertEquals(1, getContollerLisa().getFolderRepository()
            .getFoldersCount());

        assertTrue(RemoteCommandManager.sendCommand(1155,
            RemoteCommandManager.REMOVEFOLDER + "dir="
                + getFolderAtLisa().getLocalBase() + ";id="
                + getFolderAtLisa().getId() + ";name="
                + getFolderAtLisa().getName()));

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public String message() {
                return "Lisa did not remove the folder: " + getFolderAtLisa();
            }

            public boolean reached() {
                return getFolderAtLisa() == null;
            }
        });
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
