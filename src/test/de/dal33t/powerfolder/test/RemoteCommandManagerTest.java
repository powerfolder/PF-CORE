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

import java.awt.GraphicsEnvironment;
import java.io.File;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.RemoteCommandManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Util;
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
        assertFalse("PowerFolder already running on port 3458",
            RemoteCommandManager.hasRunningInstance(3458));
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
            RemoteCommandManager.MAKE_FOLDER + "dir=" + oldDir.getAbsolutePath()
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

        assertEquals(SyncProfile.AUTOMATIC_SYNCHRONIZATION,
            folderAtBart.getSyncProfile());
        assertEquals("what.bat", folderAtBart.getDownloadScript());
        assertEquals("testFolder", folderAtBart.getName());
    }

    public void testCreateNewFolder() {
        assertEquals(0, getContollerBart().getFolderRepository()
            .getFoldersCount());
        assertEquals(1, getFolderAtLisa().getMembersCount());
        boolean sent = RemoteCommandManager
            .sendCommand(
                3458,
                RemoteCommandManager.MAKE_FOLDER
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
            "false,false,false,false,5,true,22,0,m,Backup daily at 2200,false",
            folderAtBart.getSyncProfile().getFieldList());

        // Test if dupes don't appear:
        ConfigurationEntry.FOLDER_CREATE_AVOID_DUPES.setValue(
            getContollerBart(), Boolean.TRUE.toString());
        Folder oldFolderAtBart = folderAtBart;
        sent = RemoteCommandManager.sendCommand(3458,
            RemoteCommandManager.MAKE_FOLDER + "dir=" + oldDir.getAbsolutePath()
                + ";name=XXX"
                + ";syncprofile=true,true,true,true,5,false,22,0,m,Auto-sync");
        assertTrue(sent);
        TestHelper.waitMilliSeconds(1000);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getContollerBart().getFolderRepository()
                    .getFoldersCount() == 1;
            }

            public String message() {
                return "Expected folders at bart: 1. But got: "
                    + getContollerBart().getFolderRepository()
                        .getFoldersCount();
            }
        });
        assertEquals(1, getFolderAtLisa().getMembersCount());
        assertEquals(1, getContollerBart().getFolderRepository()
            .getFoldersCount());
        folderAtBart = getContollerBart().getFolderRepository().getFolders()
            .iterator().next();
        assertEquals("XXX", folderAtBart.getName());
        assertEquals("true,true,true,true,5,false,22,0,m,Auto-sync,false",
            folderAtBart.getSyncProfile().getFieldList());
        // Should be the same
        assertEquals(oldFolderAtBart.getId(), folderAtBart.getId());
    }

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

    public void testCopyLink() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        assertEquals(1, getFolderAtLisa().getMembersCount());
        File dir = new File(getFolderAtLisa().getLocalBase(), "subdir/xxß@äääx/de  ep");
        File file = TestHelper.createRandomFile(dir);

        FileInfo expected = FileInfoFactory.lookupInstance(getFolderAtLisa(), file);
        final String expectedLink = getContollerLisa().getOSClient().getFileLinkURL(expected);
        
        assertTrue(RemoteCommandManager
            .sendCommand(1155, RemoteCommandManager.COPYLINK + file.getAbsolutePath()));
        
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            
            public boolean reached() {
                return expectedLink.equals(Util.getClipboardContents());
            }
            
            public String message() {
                return "Clipboard does not contain link: " + Util.getClipboardContents();
            }
        });

    }
}
