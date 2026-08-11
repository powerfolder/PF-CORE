package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;

public class FileInfoDAOCategoryFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

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

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testNoCategoryReturnsAll() {
        assertEquals(5, dao.findFilesFast(crit(null)).size());
    }

    public void testImage() {
        assertEquals(1, dao.findFilesFast(crit("image")).size());
    }

    public void testVideo() {
        assertEquals(1, dao.findFilesFast(crit("video")).size());
    }

    public void testPdfIsItsOwnCategory() {
        assertEquals(1, dao.findFilesFast(crit("pdf")).size());
        assertEquals("a pdf is not a document any more", 0, dao.findFilesFast(crit("document")).size());
    }

    public void testArchive() {
        assertEquals(1, dao.findFilesFast(crit("archive")).size());
    }

    public void testExtensionNoTypeClaimsCountsAsOther() {
        /* Nothing here is of an unknown type, and "other" is not offered as a filter either. */
        assertEquals(0, dao.findFilesFast(crit("other")).size());
    }

    public void testFolderCategoryMatchesDirectory() {
        assertEquals(1, dao.findFilesFast(crit("folder")).size());
    }

    /** PFS-5653: several categories are OR-combined - one question, more than one acceptable answer. */
    public void testSeveralCategoriesAreOrCombined() {
        FileInfoCriteria c = crit("image");
        c.addCategory("pdf");
        assertEquals(2, dao.findFilesFast(c).size());

        c = crit("image");
        c.addCategory("pdf");
        c.addCategory("archive");
        assertEquals(3, dao.findFilesFast(c).size());
    }

    public void testAudioHasNoMatch() {
        assertEquals(0, dao.findFilesFast(crit("audio")).size());
    }

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
