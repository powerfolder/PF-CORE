package de.dal33t.powerfolder.test.folder;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.pattern.DefaultExcludes;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class SyncedDiskItemFilterTest extends TwoControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        joinTestFolder(SyncProfile.AUTOMATIC_SYNCHRONIZATION);
        getFolderAtBart().addDefaultExcludes();
        getFolderAtLisa().addDefaultExcludes();
        assertEquals(DefaultExcludes.values().length, getFolderAtBart()
            .getDiskItemFilter().getPatterns().size());
        assertEquals(DefaultExcludes.values().length, getFolderAtLisa()
            .getDiskItemFilter().getPatterns().size());
    }

    public void testSyncExcludes() {
        final String testPattern = "xxx";
        getFolderAtBart().addPattern(testPattern);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getDiskItemFilter().getPatterns()
                    .contains(testPattern);
            }

            public String message() {
                return "Lisa did not sync ignore patterns: "
                    + getFolderAtLisa().getDiskItemFilter().getPatterns();
            }
        });
    }

}
