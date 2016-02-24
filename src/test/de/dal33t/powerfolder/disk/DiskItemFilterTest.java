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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;

public class DiskItemFilterTest extends TestCase {

    public void testPFC2794() {
        DiskItemFilter blacklist = new DiskItemFilter();
        blacklist.addPattern("*.part");
       assertTrue(blacklist.isExcluded("subdir/download.part"));
       assertFalse(blacklist.isExcluded("subdir2/eng.CATpart"));
    }
    
    public void testBlackList() {
        DiskItemFilter blacklist = new DiskItemFilter();
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
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

    public void testBlacklistPatterns() {
        DiskItemFilter blacklist = new DiskItemFilter();
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
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

    public void testMulti() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            testBlackList();
            testBlacklistPatterns();
            tearDown();
            setUp();
        }
        long took = System.currentTimeMillis() - start;
        System.err.println("Took " + took + "ms");
    }
}
