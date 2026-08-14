package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.FileInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class FileInfoDAODateFilterTest extends FileInfoDAOTestCase {

    private static final long OLD = 1_400_000_000_000L;
    private static final long MID = 1_500_000_000_000L;
    private static final long NEW = 1_600_000_000_000L;

    private FileInfoDAOHashMapImpl dao;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, dated("old.txt", false, OLD));
        dao.store(null, dated("new.txt", false, NEW));
        dao.store(null, dated("archive", true, OLD));
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    @Test
    public void testNoDateFilterReturnsAll() {
        assertEquals(3, dao.findFilesFast(crit(null, null)).size());
    }

    @Test
    public void testModifiedAfterExcludesOlder() {
        assertEquals(1, dao.findFilesFast(crit(MID, null)).size());
    }

    @Test
    public void testModifiedBeforeExcludesNewer() {
        assertEquals(2, dao.findFilesFast(crit(null, MID)).size());
    }

    @Test
    public void testBetweenRange() {
        assertEquals(2, dao.findFilesFast(crit(1_350_000_000_000L, 1_450_000_000_000L)).size());
    }

    @Test
    public void testLowerBoundIsInclusive() {
        assertEquals(3, dao.findFilesFast(crit(OLD, null)).size());
    }

    @Test
    public void testUpperBoundIsInclusive() {
        assertEquals(2, dao.findFilesFast(crit(null, OLD)).size());
    }

    @Test
    public void testNoMatchReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit(1_700_000_000_000L, null)).size());
    }

    @Test
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
