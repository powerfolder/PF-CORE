package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.FileInfo;

import java.util.Date;

public class FileInfoDAODateFilterTest extends FileInfoDAOTestCase {

    private static final long OLD = 1_400_000_000_000L;
    private static final long MID = 1_500_000_000_000L;
    private static final long NEW = 1_600_000_000_000L;

    private FileInfoDAOHashMapImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, dated("old.txt", false, OLD));
        dao.store(null, dated("new.txt", false, NEW));
        dao.store(null, dated("archive", true, OLD));
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testNoDateFilterReturnsAll() {
        assertEquals(3, dao.findFilesFast(crit(null, null)).size());
    }

    public void testModifiedAfterExcludesOlder() {
        assertEquals(1, dao.findFilesFast(crit(MID, null)).size());
    }

    public void testModifiedBeforeExcludesNewer() {
        assertEquals(2, dao.findFilesFast(crit(null, MID)).size());
    }

    public void testBetweenRange() {
        assertEquals(2, dao.findFilesFast(crit(1_350_000_000_000L, 1_450_000_000_000L)).size());
    }

    public void testLowerBoundIsInclusive() {
        assertEquals(3, dao.findFilesFast(crit(OLD, null)).size());
    }

    public void testUpperBoundIsInclusive() {
        assertEquals(2, dao.findFilesFast(crit(null, OLD)).size());
    }

    public void testNoMatchReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit(1_700_000_000_000L, null)).size());
    }

    public void testCriteriaHasSearchCriteriaWithDateBounds() {
        assertFalse(new FileInfoCriteria().hasSearchCriteria());
        FileInfoCriteria after = new FileInfoCriteria();
        after.setModifiedAfter(new Date(OLD));
        assertTrue(after.hasSearchCriteria());
        FileInfoCriteria before = new FileInfoCriteria();
        before.setModifiedBefore(new Date(NEW));
        assertTrue(before.hasSearchCriteria());
    }

    private static FileInfo dated(String name, boolean directory, long millis) {
        return version(createFileInfo(name, 1, directory), 1, new Date(millis));
    }

    private static FileInfoCriteria crit(Long after, Long before) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setModifiedAfter(after == null ? null : new Date(after));
        c.setModifiedBefore(before == null ? null : new Date(before));
        return c;
    }
}
