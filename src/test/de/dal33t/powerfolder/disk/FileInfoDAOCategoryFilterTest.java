package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileInfoDAOCategoryFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, createFileInfo("holiday.jpg", 1, false));
        dao.store(null, createFileInfo("clip.mp4", 1, false));
        dao.store(null, createFileInfo("report.pdf", 1, false));
        dao.store(null, createFileInfo("backup.zip", 1, false));
        dao.store(null, createFileInfo("folderX", 1, true));
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    @Test
    public void testNoCategoryReturnsAll() {
        assertEquals(5, dao.findFilesFast(crit(null)).size());
    }

    @Test
    public void testImage() {
        assertEquals(1, dao.findFilesFast(crit("image")).size());
    }

    @Test
    public void testVideo() {
        assertEquals(1, dao.findFilesFast(crit("video")).size());
    }

    @Test
    public void testPdfIsItsOwnCategory() {
        assertEquals(1, dao.findFilesFast(crit("pdf")).size());
        assertEquals(0, dao.findFilesFast(crit("document")).size(), "a pdf is not a document any more");
    }

    @Test
    public void testArchive() {
        assertEquals(1, dao.findFilesFast(crit("archive")).size());
    }

    @Test
    public void testExtensionNoTypeClaimsCountsAsOther() {
        /* Nothing here is of an unknown type, and "other" is not offered as a filter either. */
        assertEquals(0, dao.findFilesFast(crit("other")).size());
    }

    @Test
    public void testFolderCategoryMatchesDirectory() {
        assertEquals(1, dao.findFilesFast(crit("folder")).size());
    }

    /** PFS-5653: several categories are OR-combined - one question, more than one acceptable answer. */
    @Test
    public void testSeveralCategoriesAreOrCombined() {
        FileInfoCriteria c = crit("image");
        c.addCategory("pdf");
        assertEquals(2, dao.findFilesFast(c).size());

        c = crit("image");
        c.addCategory("pdf");
        c.addCategory("archive");
        assertEquals(3, dao.findFilesFast(c).size());
    }

    @Test
    public void testAudioHasNoMatch() {
        assertEquals(0, dao.findFilesFast(crit("audio")).size());
    }

    @Test
    public void testCategoryIsCaseInsensitive() {
        assertEquals(1, dao.findFilesFast(crit("IMAGE")).size());
    }

    private static FileInfoCriteria crit(String category) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.addCategory(category);
        return c;
    }
}
