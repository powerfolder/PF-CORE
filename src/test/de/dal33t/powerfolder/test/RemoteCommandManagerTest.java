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
 * $Id: RemoteCommandManager.java 8439 2009-07-03 02:00:07Z tot $
 */
package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.RemoteCommandManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class RemoteCommandManagerTest extends TwoControllerTestCase {
    private File oldDir;

    @Override
    protected void setUp() throws Exception {
        assertFalse("PowerFolder already running on port 3458", RemoteCommandManager
            .hasRunningInstance(3458));
        super.setUp();
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
    }

    public void testJoinExistingFolder() {
        assertEquals(1, getFolderAtLisa().getMembersCount());
        boolean sent = RemoteCommandManager.sendCommand(3458,
            RemoteCommandManager.MAKEFOLDER + "dir=" + oldDir.getAbsolutePath()
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
        Folder folderAtBart = getContollerBart().getFolderRepository()
            .getFolders().iterator().next();

        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION, folderAtBart
            .getSyncProfile());
        assertEquals("what.bat", folderAtBart.getDownloadScript());
        assertEquals(oldDir.getName(), folderAtBart.getName());
    }

    public void testCreateNewFolder() {
        assertEquals(0, getContollerBart().getFolderRepository()
            .getFoldersCount());
        assertEquals(1, getFolderAtLisa().getMembersCount());
        boolean sent = RemoteCommandManager
            .sendCommand(
                3458,
                RemoteCommandManager.MAKEFOLDER
                    + "dir="
                    + oldDir.getAbsolutePath()
                    + ";name=XXX"
                    + ";syncprofile=false,false,false,false,5,true,22,0,m,Backup daily at 2200");
        assertTrue(sent);
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getContollerBart().getFolderRepository()
                    .getFoldersCount() == 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getMembersCount());
        assertEquals(1, getContollerBart().getFolderRepository()
            .getFoldersCount());
        Folder folderAtBart = getContollerBart().getFolderRepository()
            .getFolders().iterator().next();
        assertEquals("XXX", folderAtBart.getName());
        assertEquals(
            "false,false,false,false,5,true,22,0,m,Backup daily at 2200",
            folderAtBart.getSyncProfile().getFieldList());
    }
}
