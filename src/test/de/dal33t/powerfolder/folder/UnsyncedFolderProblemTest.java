package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class UnsyncedFolderProblemTest extends TwoControllerTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerBart(), true);
        PreferencesEntry.EXPERT_MODE.setValue(getContollerLisa(), true);
        ConfigurationEntry.FOLDER_SYNC_USE.setValue(getContollerBart(), true);
        ConfigurationEntry.FOLDER_SYNC_USE.setValue(getContollerLisa(), true);
    }

    public void testSyncOK() {
        getFolderAtLisa().setSyncWarnSeconds(2);
        TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        scanFolder(getFolderAtBart());

        TestHelper.waitMilliSeconds(2100);
        getFolderAtLisa().checkSync();
        assertEquals(0, getFolderAtLisa().getProblems().size());

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
