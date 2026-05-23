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
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FolderInfoStatisticTest extends TestCase {

    public void testStoreLoad() throws IOException {
        FolderInfo foInfo = FolderInfoFactory.newTopFolderForTest("Test");
        FolderStatisticInfo stats = new FolderStatisticInfo(foInfo);
        assertNotNull(stats.getPartialSyncStatMap());
        assertTrue(stats.getPartialSyncStatMap().isEmpty());
        Files.createDirectories(TestHelper.getTestDir());
        Path file = TestHelper.getTestDir().resolve("Test.stats");
        assertTrue(stats.save(file));

        FolderStatisticInfo loadedStats = FolderStatisticInfo.load(file);
        assertNotNull(loadedStats);
        assertEquals(stats, loadedStats);
        assertNotNull(loadedStats.getPartialSyncStatMap());
        assertTrue(loadedStats.getPartialSyncStatMap().isEmpty());
    }
}
