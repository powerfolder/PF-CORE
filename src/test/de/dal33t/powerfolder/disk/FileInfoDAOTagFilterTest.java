package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;


public class FileInfoDAOTagFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, tagged("a.txt", "[\"Contract\",\"2026\"]"));
        dao.store(null, tagged("b.txt", "[\"Contract\"]"));
        dao.store(null, tagged("c.txt", "[\"Invoice\"]"));
        dao.store(null, createFileInfo("d.txt", 1, false)); // untagged
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testNoTagFilterReturnsAll() {
        assertEquals(4, dao.findFilesFast(crit()).size());
    }

    public void testSingleTagFilter() {
        assertEquals(2, dao.findFilesFast(crit("Contract")).size());
        assertEquals(1, dao.findFilesFast(crit("Invoice")).size());
    }

    public void testMultipleTagsAreAndCombined() {
        // Only a.txt has both Contract AND 2026
        assertEquals(1, dao.findFilesFast(crit("Contract", "2026")).size());
        // No file has both Contract AND Invoice
        assertEquals(0, dao.findFilesFast(crit("Contract", "Invoice")).size());
    }

    public void testTagMatchIsCaseInsensitive() {
        assertEquals(2, dao.findFilesFast(crit("contract")).size());
        assertEquals(1, dao.findFilesFast(crit("iNVoIce")).size());
    }

    public void testUnknownTagReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit("DoesNotExist")).size());
    }

    public void testCriteriaHasSearchCriteriaWithTags() {
        assertFalse(new FileInfoCriteria().hasSearchCriteria());
        FileInfoCriteria c = new FileInfoCriteria();
        c.addTag("x");
        assertTrue(c.hasSearchCriteria());
        assertEquals(1, c.getTags().size());
        // blank tags are ignored
        c.addTag("   ");
        assertEquals(1, c.getTags().size());
    }

    private static FileInfo tagged(String name, String tagsJson) {
        return FileInfoFactory.withTags(createFileInfo(name, 1, false), tagsJson);
    }

    private static FileInfoCriteria crit(String... tags) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        for (String tag : tags) {
            c.addTag(tag);
        }
        return c;
    }
}
