package de.dal33t.powerfolder.test.folder;

import java.util.regex.Pattern;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class TestBlacklist extends TestCase {
    public void testBlacklist() {
        Blacklist blacklist = new Blacklist();
        FolderInfo folderInfo = new FolderInfo("foldername", "id", true);
        blacklist.addDoNotAutoDownloadPattern("*thumbs.db");
        blacklist.addDoNotSharePattern("*thumbs.db");

        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thusssmbs.db")));
        assertFalse(blacklist.isAllowedToShare(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist.isAllowedToShare(new FileInfo(folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo, "thusssmbs.db")));
                
        blacklist.removeDoNotAutoDownloadPattern("*thumbs.db");
        blacklist.removeDoNotSharePattern("*thumbs.db");
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thusssmbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo, "thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToShare(new FileInfo(folderInfo, "thusssmbs.db")));
        
        
        Blacklist blacklist2 = new Blacklist();
        blacklist2.addDoNotAutoDownloadPattern("images/*thumbs.db");
        blacklist2.addDoNotSharePattern("images/*thumbs.db");
        assertTrue(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "images/thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "images/deepinimages/thumbs.db")));
        assertTrue(blacklist2.isAllowedToShare(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist2.isAllowedToShare(new FileInfo(folderInfo, "images/thumbs.db")));
        assertFalse(blacklist2.isAllowedToShare(new FileInfo(folderInfo, "images/deepinimages/thumbs.db")));
        
    }
}
