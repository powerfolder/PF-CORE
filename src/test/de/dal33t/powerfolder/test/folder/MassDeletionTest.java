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
 * $Id: MassDeletionTest.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests the correct response to mass deletions.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry</a>
 * @version $Revision: 4.0 $
 */
public class MassDeletionTest extends TwoControllerTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testMassDeletion() throws Exception {

        // Check with no protection
        massDeletion(false);
        
        tearDown();
        setUp();

        // Check with protection
        massDeletion(true);
    }

    public void massDeletion(boolean protection) throws Exception {

        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        PreferencesEntry.MASS_DELETE_THRESHOLD.setValue(getFolderAtLisa().getController(), 80);
        PreferencesEntry.MASS_DELETE_PROTECTION.setValue(getFolderAtLisa().getController(), protection);

        for (int i = 0; i < 100; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }

        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getKnownFilesCount() == 100;
            }
        });

        scanFolder(getFolderAtLisa());

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getKnownFilesCount() == 100;
            }
        });

        assertEquals(100, getFolderAtLisa().getKnownFiles().size());

        // Delete all Bart's files
        for (final File file : getFolderAtBart().getLocalBase().listFiles()) {
            if (!file.isDirectory()) {
                TestHelper.waitForCondition(10, new Condition() {
                    public boolean reached() {
                        return file.delete();
                    }
                });
            }
        }

        scanFolder(getFolderAtBart());
        scanFolder(getFolderAtLisa());

        if (protection) {
            // Files should survive and profile switch to HOST_FILE.
            TestHelper.waitForCondition(20, new Condition() {
                public boolean reached() {
                    return getFolderAtLisa().getLocalBase().listFiles().length
                            == 101; // The files + .PowerFolder dir
                }
            });
            assertEquals(getFolderAtLisa().getSyncProfile(), SyncProfile.HOST_FILES);
        } else {
            // Files should have been deleted and profile remains same.
            TestHelper.waitForCondition(20, new Condition() {
                public boolean reached() {
                    return getFolderAtLisa().getLocalBase().listFiles().length
                            == 1; // The .PowerFolder dir
                }
            });
            assertEquals(getFolderAtLisa().getSyncProfile(), SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        }

    }

}