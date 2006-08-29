package de.dal33t.powerfolder.test.folder;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class TestBlacklist extends TestCase {

    public void testBlackList() {
        Blacklist blacklist = new Blacklist();
        FolderInfo folderInfo = new FolderInfo("foldername", "id", true);
        FileInfo fileInfo = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo2 = new FileInfo(folderInfo, "thumbs.db");
        FileInfo fileInfo3 = new FileInfo(folderInfo, "somefile.txt");
        blacklist.addToDoNotAutoDownload(fileInfo);
        assertFalse(blacklist.isAllowedToAutoDownload(fileInfo));
        // other instance but equals
        assertFalse(blacklist.isAllowedToAutoDownload(fileInfo2));
        // not blacklisted
        assertTrue(blacklist.isAllowedToAutoDownload(fileInfo3));
        // after remove allow download again
        blacklist.removeFromDoNotAutoDownload(fileInfo);
        assertTrue(blacklist.isAllowedToAutoDownload(fileInfo));

        blacklist.addToDoNotShare(fileInfo);
        assertFalse(blacklist.isAllowedToShare(fileInfo));
        blacklist.removeFromDoNotShare(fileInfo);
        assertTrue(blacklist.isAllowedToShare(fileInfo));

    }

    public void testBlacklistPatterns() {
        Blacklist blacklist = new Blacklist();
        FolderInfo folderInfo = new FolderInfo("foldername", "id", true);
        blacklist.addDoNotAutoDownloadPattern("*thumbs.db");
        blacklist.addDoNotSharePattern("*thumbs.db");

        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "thumbs.db")));
        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "thusssmbs.db")));
        assertFalse(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "thumbs.db")));
        assertFalse(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "thusssmbs.db")));

        blacklist.removeDoNotAutoDownloadPattern("*thumbs.db");
        blacklist.removeDoNotSharePattern("*thumbs.db");
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "thusssmbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo,
            "thusssmbs.db")));

        Blacklist blacklist2 = new Blacklist();
        blacklist2.addDoNotAutoDownloadPattern("images/*thumbs.db");
        blacklist2.addDoNotSharePattern("images/*thumbs.db");
        assertTrue(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "images/thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.db")));
        assertTrue(blacklist2.isAllowedToShare(new FileInfo(folderInfo,
            "thumbs.db")));
        assertFalse(blacklist2.isAllowedToShare(new FileInfo(folderInfo,
            "images/thumbs.db")));
        assertFalse(blacklist2.isAllowedToShare(new FileInfo(folderInfo,
            "images/deepinimages/thumbs.db")));

    }
}
