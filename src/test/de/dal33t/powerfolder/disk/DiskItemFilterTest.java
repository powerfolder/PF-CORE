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
 *
 */
package de.dal33t.powerfolder.disk;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.pattern.DefaultExcludes;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;

import java.nio.file.Path;

public class DiskItemFilterTest {

    @Test
    public void testOfficePatterns() {
        DiskItemFilter filter = new DiskItemFilter();
        int i = 0;

        filter.addPattern(DefaultExcludes.OFFICEX_TEMP.getPattern());
        i++;
        assertEquals(i, filter.getPatterns().size());

        filter.addPattern(DefaultExcludes.LIBRE_TEMP.getPattern());
        i++;
        assertEquals(i, filter.getPatterns().size());

        filter.addPattern(DefaultExcludes.OFFICE_TEMP.getPattern());
        i++;
        assertEquals(i, filter.getPatterns().size());
    }

    @Test
    public void testPFC2794() {
        DiskItemFilter blacklist = new DiskItemFilter();
        blacklist.addPattern("*.part");
        assertTrue(blacklist.isExcluded("subdir/download.part"));
        assertFalse(blacklist.isExcluded("subdir2/eng.CATpart"));
    }
    
    @Test
    public void testBlackList() {
        DiskItemFilter blacklist = new DiskItemFilter();
        FolderInfo folderInfo = FolderInfoFactory.newTopFolderForTest("foldername");
        FileInfo fileInfo = FileInfoFactory.lookupInstance(folderInfo,
            "thumbs.db");
        FileInfo fileInfo2 = FileInfoFactory.lookupInstance(folderInfo,
            "thumbs.db");
        FileInfo fileInfo3 = FileInfoFactory.lookupInstance(folderInfo,
            "somefile.txt");
        FileInfo fileInfo4 = FileInfoFactory.lookupInstance(folderInfo,
            "A_UPPER_case_FILENAME.xxx");
        blacklist.addPattern(fileInfo.getRelativeName());
        assertTrue(blacklist.isExcluded(fileInfo));
        // other instance but equals
        assertTrue(blacklist.isExcluded(fileInfo2));
        // not blacklisted
        assertTrue(blacklist.isRetained(fileInfo3));
        // after remove allow download again
        blacklist.removePattern(fileInfo.getRelativeName());
        assertTrue(blacklist.isRetained(fileInfo));
        // Mix-case filename test
        blacklist.addPattern(fileInfo4.getRelativeName());
        assertTrue(blacklist.isExcluded(fileInfo4));
    }

    @Test
    public void testBlacklistPatterns() {
        DiskItemFilter blacklist = new DiskItemFilter();
        FolderInfo folderInfo = FolderInfoFactory.newTopFolderForTest("foldername");
        blacklist.addPattern("*thumbs.db");
        blacklist.addPattern("*THAMBS.db");

        assertFalse(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "thumbs.db")));
        assertFalse(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "thusssmbs.db")));

        blacklist.removePattern("*thumbs.db");

        assertTrue(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "thumbs.db")));
        assertTrue(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "thusssmbs.db")));

        DiskItemFilter blacklist2 = new DiskItemFilter();
        blacklist2.addPattern("images/*thumbs.db");

        assertTrue(blacklist2.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "thumbs.db")));
        assertFalse(blacklist2.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "images/thumbs.db")));
        assertFalse(blacklist2.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "images/deepinimages/thumbs.db")));

        // Mixed case pattern. Should match!
        assertFalse(blacklist2.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "images/deepinimages/THUMBS.db")));
        assertFalse(blacklist2.isRetained(FileInfoFactory.lookupInstance(
            folderInfo, "images/deepinimages/thumbs.DB")));

        blacklist2.addPattern("*gc.2010*");
        assertTrue(blacklist2.isExcluded(FileInfoFactory.lookupInstance(
            folderInfo, "file.gc.20100412.gc")));
        assertFalse(blacklist2.isExcluded(FileInfoFactory.lookupInstance(
            folderInfo, "file.gc")));

    }

    @Test
    public void testMulti() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            testBlackList();
            testBlacklistPatterns();
        }
        long took = System.currentTimeMillis() - start;
        System.err.println("Took " + took + "ms");
    }

    /**
     * Test for PF-1153
     */
    @Test
    public void testSaveLoadDefaultExcludes() {
        DiskItemFilter filter = new DiskItemFilter();
        for (DefaultExcludes defExclude: DefaultExcludes.values()) {
            filter.addPattern(defExclude.getPattern());
        }
        Path p = TestHelper.getTestDir().resolve("ignore.patterns");
        filter.savePatternsTo(TestHelper.getTestDir().resolve("ignore.patterns"), false);

        filter = new DiskItemFilter();
        filter.loadPatternsFrom(p, false);

        for (DefaultExcludes defExclude: DefaultExcludes.values()) {
            assertTrue( filter.getPatterns().contains(defExclude.getPattern()),defExclude.getPattern());
        }
    }

    @Test
    public void testPF1153() {
        DiskItemFilter filter = new DiskItemFilter();
        filter.addPattern("Friesenstraße 36-Papenburg/*");
    }
}
