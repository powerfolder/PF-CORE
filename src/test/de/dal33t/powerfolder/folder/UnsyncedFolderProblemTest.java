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
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

public class UnsyncedFolderProblemTest extends TwoControllerTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerBart(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true);
        ConfigurationEntry.FOLDER_SYNC_USE.setValue(getContollerBart(), true);
        ConfigurationEntry.FOLDER_SYNC_USE.setValue(getContollerLisa(), true);
    }

    @Test
    public void testSyncOK() {
        getFolderAtLisa().setSyncWarnSeconds(2);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(3000);
        getFolderAtLisa().checkSync();
        assertEquals(1, getFolderAtLisa().getProblems().size());

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtBart().getLastSyncDate() != null;
            }
        });
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getLastSyncDate() != null;
            }
        });
        assertNotNull(getFolderAtBart().getLastSyncDate());
        assertNotNull(getFolderAtLisa().getLastSyncDate());
    }

    @Test
    public void testSyncFAIL() {
        getFolderAtLisa().setSyncWarnSeconds(2);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());
        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolderAtLisa().getLastSyncDate() != null;
            }
        });

        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2100);
        getFolderAtLisa().checkSync();
        assertEquals(1, getFolderAtLisa().getProblems().size());
    }

}
