package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class FileInfoDAOTagFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, tagged("a.txt", "[\"Contract\",\"2026\"]"));
        dao.store(null, tagged("b.txt", "[\"Contract\"]"));
        dao.store(null, tagged("c.txt", "[\"Invoice\"]"));
        dao.store(null, tagged("e.txt", "[\"offer\"]")); // stored lowercase
        dao.store(null, createFileInfo("d.txt", 1, false)); // untagged
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    @Test
    public void testNoTagFilterReturnsAll() {
        assertEquals(5, dao.findFilesFast(crit()).size());
    }

    @Test
    public void testSingleTagFilter() {
        assertEquals(2, dao.findFilesFast(crit("Contract")).size());
        assertEquals(1, dao.findFilesFast(crit("Invoice")).size());
    }

    @Test
    public void testMultipleTagsAreAndCombined() {
        // Only a.txt has both Contract AND 2026
        assertEquals(1, dao.findFilesFast(crit("Contract", "2026")).size());
        // No file has both Contract AND Invoice
        assertEquals(0, dao.findFilesFast(crit("Contract", "Invoice")).size());
    }

    @Test
    public void testTagMatchIsCaseInsensitive() {
        assertEquals(2, dao.findFilesFast(crit("contract")).size());
        assertEquals(1, dao.findFilesFast(crit("iNVoIce")).size());
    }

    /** Tags are matched case-insensitively in both directions - a lowercase tag is found by any casing. */
    @Test
    public void testTagStoredInLowerCaseIsFoundByAnyCasing() {
        assertEquals(1, dao.findFilesFast(crit("OFFER")).size());
        assertEquals(1, dao.findFilesFast(crit("Offer")).size());
        assertEquals(1, dao.findFilesFast(crit("offer")).size());
    }

    @Test
    public void testPaddedTagQueryIsTrimmed() {
        assertEquals(2, dao.findFilesFast(crit("  contract  ")).size());
    }

    @Test
    public void testUnknownTagReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit("DoesNotExist")).size());
    }

    @Test
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
