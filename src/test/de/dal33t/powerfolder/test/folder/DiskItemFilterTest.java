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
package de.dal33t.powerfolder.test.folder;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.DiskItemFilter;

public class DiskItemFilterTest extends TestCase {

    public void testBlackList() {
        DiskItemFilter blacklist = new DiskItemFilter(false);
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
        FileInfo fileInfo = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo2 = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo3 = new FileInfo(folderInfo, "somefile.txt");
        FileInfo fileInfo4 = new FileInfo(folderInfo,
            "A_UPPER_case_FILENAME.xxx");
        blacklist.addPattern(fileInfo.getName());
        assertTrue(blacklist.isExcluded(fileInfo));
        // other instance but equals
        assertTrue(blacklist.isExcluded(fileInfo2));
        // not blacklisted
        assertTrue(blacklist.isRetained(fileInfo3));
        // after remove allow download again
        blacklist.removePattern(fileInfo.getName());
        assertTrue(blacklist.isRetained(fileInfo));
        // Mix-case filename test
        blacklist.addPattern(fileInfo4.getName());
        assertTrue(blacklist.isExcluded(fileInfo4));
    }

    public void testBlacklistPatterns() {
        DiskItemFilter blacklist = new DiskItemFilter(false);
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
        blacklist.addPattern("*thumbs.db");
        blacklist.addPattern("*THAMBS.db");

        assertFalse(blacklist.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist.isRetained(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist
            .isRetained(new FileInfo(folderInfo, "thusssmbs.db")));

        blacklist.removePattern("*thumbs.db");

        assertTrue(blacklist.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertTrue(blacklist.isRetained(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist
            .isRetained(new FileInfo(folderInfo, "thusssmbs.db")));

        DiskItemFilter blacklist2 = new DiskItemFilter(false);
        blacklist2.addPattern("images/*thumbs.db");

        assertTrue(blacklist2.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist2.isRetained(new FileInfo(folderInfo,
            "images/thumbs.db")));
        assertFalse(blacklist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.db")));

        // Mixed case pattern. Should match!
        assertFalse(blacklist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/THUMBS.db")));
        assertFalse(blacklist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.DB")));
    }

    public void testWhiteList() {
        DiskItemFilter whitelist = new DiskItemFilter(true);
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
        FileInfo fileInfo = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo2 = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo3 = new FileInfo(folderInfo, "somefile.txt");
        FileInfo fileInfo4 = new FileInfo(folderInfo,
            "A_UPPER_case_FILENAME.xxx");
        whitelist.addPattern(fileInfo.getName());
        assertTrue(whitelist.isRetained(fileInfo));
        // other instance but equals
        assertTrue(whitelist.isRetained(fileInfo2));
        // not blacklisted
        assertFalse(whitelist.isRetained(fileInfo3));
        // after remove allow download again
        whitelist.removePattern(fileInfo.getName());
        assertFalse(whitelist.isRetained(fileInfo));
        // Mix-case filename test
        whitelist.addPattern(fileInfo4.getName());
        assertTrue(whitelist.isRetained(fileInfo4));
    }

    public void testWhitelistPatterns() {
        DiskItemFilter whitelist = new DiskItemFilter(true);
        FolderInfo folderInfo = new FolderInfo("foldername", "id");
        whitelist.addPattern("*thumbs.db");
        whitelist.addPattern("*THAMBS.db");

        assertTrue(whitelist.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertTrue(whitelist.isRetained(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertFalse(whitelist
            .isRetained(new FileInfo(folderInfo, "thusssmbs.db")));

        whitelist.removePattern("*thumbs.db");

        assertFalse(whitelist.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(whitelist.isRetained(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertFalse(whitelist
            .isRetained(new FileInfo(folderInfo, "thusssmbs.db")));

        DiskItemFilter whitelist2 = new DiskItemFilter(true);
        whitelist2.addPattern("images/*thumbs.db");

        assertFalse(whitelist2.isRetained(new FileInfo(folderInfo, "thumbs.db")));
        assertTrue(whitelist2.isRetained(new FileInfo(folderInfo,
            "images/thumbs.db")));
        assertTrue(whitelist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.db")));

        // Mixed case pattern. Should match!
        assertTrue(whitelist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/THUMBS.db")));
        assertTrue(whitelist2.isRetained(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.DB")));
    }

    public void testMulti() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            testBlackList();
            testBlacklistPatterns();
            testWhiteList();
            testWhitelistPatterns();
            tearDown();
            setUp();
        }
        long took = System.currentTimeMillis() - start;
        System.err.println("Took " + took + "ms");
    }
}
