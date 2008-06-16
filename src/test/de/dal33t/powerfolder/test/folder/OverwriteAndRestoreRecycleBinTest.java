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
* $Id$
*/
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.RecycleBinConfirmEvent;
import de.dal33t.powerfolder.event.RecycleBinConfirmationHandler;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * TODO ADD JAVADOC
 */
public class OverwriteAndRestoreRecycleBinTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
        makeFriends();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    /**
     * Test the overwrite of file (due to sync) results in copy of old one in
     * RecycleBin. After that the file is restored.
     */
    public void testOverwriteToRecycleAndRestore() {
        final File testFileBart = TestHelper.createRandomFile(getFolderAtBart()
            .getLocalBase());

        scanFolder(getFolderAtBart());

        final FileInfo fInfoBart = getFolderAtBart().getKnownFiles().iterator().next();

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() >= 1;
            }
        });
        assertEquals(1, getFolderAtLisa().getKnownFilesCount());
        final FileInfo fInfoLisa = getFolderAtLisa().getKnownFiles().iterator().next();
        final File testFileLisa = fInfoLisa.getDiskFile(getContollerLisa()
            .getFolderRepository());

        assertTrue(fInfoLisa.isCompletelyIdentical(fInfoBart));
        assertEquals(testFileBart.length(), testFileLisa.length());

        // overwrite file at Bart
        TestHelper.createTestFile(getFolderAtBart().getLocalBase(),
            testFileBart.getName(), new byte[]{6, 5, 6, 7});
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(500);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fInfoLisa.isCompletelyIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }
        });

        RecycleBin binAtLisa = getContollerLisa().getRecycleBin();
        assertTrue(binAtLisa.isInRecycleBin(fInfoLisa));
        assertEquals(1, binAtLisa.countAllRecycledFiles());

        FileInfo infoAtLisa = binAtLisa.getAllRecycledFiles().get(0);

        // add NO reply to overwrite question on restore
        binAtLisa
            .setRecycleBinConfirmationHandler(new RecycleBinConfirmationHandlerNo());
        assertFalse(binAtLisa.restoreFromRecycleBin(infoAtLisa));
        // file should still be in recycle bin
        assertEquals(1, binAtLisa.countAllRecycledFiles());

        // add Yes reply to overwrite question on restore
        binAtLisa
            .setRecycleBinConfirmationHandler(new RecycleBinConfirmationHandlerYes());
        assertTrue(binAtLisa.restoreFromRecycleBin(infoAtLisa));
        assertEquals(0, binAtLisa.countAllRecycledFiles());
        TestHelper.waitMilliSeconds(500);

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return fInfoLisa.isCompletelyIdentical(fInfoBart)
                    && (testFileBart.length() == testFileLisa.length());
            }
        });
    }

    class RecycleBinConfirmationHandlerNo implements
        RecycleBinConfirmationHandler
    {
        public boolean confirmOverwriteOnRestore(
            RecycleBinConfirmEvent recycleBinConfirmEvent)
        {
            return false;
        }
    }

    class RecycleBinConfirmationHandlerYes implements
        RecycleBinConfirmationHandler
    {
        public boolean confirmOverwriteOnRestore(
            RecycleBinConfirmEvent recycleBinConfirmEvent)
        {
            return true;
        }
    }
}
