package de.dal33t.powerfolder.test.folder;

import junit.framework.TestCase;
import de.dal33t.powerfolder.disk.Blacklist;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

public class TestBlacklist extends TestCase {
    public void testBlacklist() {
        Blacklist blacklist = new Blacklist();
        FolderInfo folderInfo = new FolderInfo("foldername", "id", true);
        blacklist.addDoNotAutoDownloadPattern("*thumbs.db");
        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "somewhere/in/a/sub/thumbs.db")));
        assertTrue(blacklist.isAllowedToAutoDownload(new FileInfo(folderInfo, "thusssmbs.db")));
        
        Blacklist blacklist2 = new Blacklist();
        blacklist2.addDoNotAutoDownloadPattern("images/*thumbs.db");
        assertTrue(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "images/thumbs.db")));
        assertFalse(blacklist2.isAllowedToAutoDownload(new FileInfo(folderInfo, "images/deepinimages/thumbs.db")));
        
    }
}
