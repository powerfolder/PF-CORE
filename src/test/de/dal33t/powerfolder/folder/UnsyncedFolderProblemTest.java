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
import java.util.Date;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.UnsynchronizedFolderProblem;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
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

        waitForLastSyncDate(getFolderAtBart());
        waitForLastSyncDate(getFolderAtLisa());
        waitForUnsyncedProblem(getFolderAtLisa());
    }

    @Test
    public void testSyncFAIL() {
        getFolderAtLisa().setSyncWarnSeconds(2);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());
        waitForLastSyncDate(getFolderAtLisa());

        getFolderAtLisa().setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        waitForUnsyncedProblem(getFolderAtLisa());
    }

    private static void waitForUnsyncedProblem(final Folder folder) {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            public boolean reached() {
                folder.checkSync();
                for (Problem problem : folder.getProblems()) {
                    if (problem instanceof UnsynchronizedFolderProblem) {
                        return true;
                    }
                }
                return false;
            }

            public String message() {
                return folder + " reported no unsynced problem. Last sync: "
                    + folder.getLastSyncDate() + ", warn after "
                    + folder.getSyncWarnSeconds() + "s, problems: "
                    + folder.getProblems();
            }
        });
        assertEquals(1, folder.getProblems().size());
    }

    private static void waitForLastSyncDate(final Folder folder) {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return folder.getLastSyncDate() != null;
            }

            public String message() {
                return folder + " never recorded a sync date. Harmonized sync: "
                    + folder.getStatistic().getHarmonizedSyncPercentage();
            }
        });
    }

    private static void waitForSyncWarningDue(final Folder folder) {
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                Date lastSync = folder.getLastSyncDate();
                return lastSync != null
                    && lastSync.getTime() + folder.getSyncWarnSeconds() * 1000L
                        < System.currentTimeMillis();
            }

            public String message() {
                return folder + " sync warning never became due. Last sync: "
                    + folder.getLastSyncDate() + ", warn seconds: "
                    + folder.getSyncWarnSeconds();
            }
        });
    }

}
