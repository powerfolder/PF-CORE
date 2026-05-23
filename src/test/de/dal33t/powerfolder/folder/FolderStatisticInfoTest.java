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

import de.dal33t.powerfolder.light.FolderStatisticInfo;
import junit.framework.TestCase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderStatisticInfoTest extends TestCase {

    // public void testLoadAll() {
    // File baseDir = new File(
    // "C:\\Users\\sprajc.POWERFOLDER\\Desktop\\powerfolder_daily_2013-06-15_04-10");
    // testLoad(baseDir);
    // }

    /**
     * PFS-818: Exceptions while running with dynamic folder mounting / shared
     * storage
     */
    public void testCorruptFiles() {
        //assertNull(testCorruptFile(Paths
        //    .get("src/test-resources/FolderStatisticInfo_OOM.txt")));
        assertNotNull(testCorruptFile(Paths
            .get("src/test-resources/FolderStatisticInfo_NPE.txt")));
        assertNotNull(testCorruptFile(Paths
            .get("src/test-resources/FolderStatisticInfo_OK.txt")));

    }

    private FolderStatisticInfo testCorruptFile(Path file) {
        if (Files.notExists(file)) {
            fail("Testfile not found " + file);
        }
        try {
            return FolderStatisticInfo.load(file);
        } catch (Throwable t) {
            fail("Throwable " + t);
            t.printStackTrace();
        }
        return null;
    }
}
