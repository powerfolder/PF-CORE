package de.dal33t.powerfolder.light;

import java.io.File;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FolderInfoStatisticTest extends TestCase {

    public void testStoreLoad() {
        FolderInfo foInfo = new FolderInfo("Test", IdGenerator.makeId());
        FolderStatisticInfo stats = new FolderStatisticInfo(foInfo);
        assertNotNull(stats.getPartialSyncStatMap());
        assertTrue(stats.getPartialSyncStatMap().isEmpty());
        TestHelper.getTestDir().mkdirs();
        File file = new File(TestHelper.getTestDir(), "Test.stats");
        assertTrue(stats.save(file));

        FolderStatisticInfo loadedStats = FolderStatisticInfo.load(file);
        assertNotNull(loadedStats);
        assertEquals(stats, loadedStats);
        assertNotNull(loadedStats.getPartialSyncStatMap());
        assertTrue(loadedStats.getPartialSyncStatMap().isEmpty());
    }
}
