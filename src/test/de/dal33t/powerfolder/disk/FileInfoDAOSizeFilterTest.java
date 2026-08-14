package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileInfoDAOSizeFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, createRandomFileInfo(100, "tiny"));
        dao.store(null, createRandomFileInfo(5000, "medium"));
        dao.store(null, createRandomFileInfo(200000, "large"));
    }

    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    @Test
    public void testNoSizeFilterReturnsAll() {
        assertEquals(3, dao.findFilesFast(crit(null, null)).size());
    }

    @Test
    public void testMinSizeExcludesSmaller() {
        assertEquals(2, dao.findFilesFast(crit(1000L, null)).size());
    }

    @Test
    public void testMaxSizeExcludesLarger() {
        assertEquals(1, dao.findFilesFast(crit(null, 1000L)).size());
    }

    @Test
    public void testBetweenRange() {
        assertEquals(1, dao.findFilesFast(crit(1000L, 100000L)).size());
    }

    @Test
    public void testMinBoundIsInclusive() {
        assertEquals(3, dao.findFilesFast(crit(100L, null)).size());
    }

    @Test
    public void testMaxBoundIsInclusive() {
        assertEquals(1, dao.findFilesFast(crit(null, 100L)).size());
    }

    @Test
    public void testNoMatchReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit(1_000_000L, null)).size());
    }

    @Test
    public void testCriteriaHasSearchCriteriaWithSizeBounds() {
        assertFalse(new FileInfoCriteria().hasSearchCriteria());
        FileInfoCriteria min = new FileInfoCriteria();
        min.setMinSize(100L);
        assertTrue(min.hasSearchCriteria());
        FileInfoCriteria max = new FileInfoCriteria();
        max.setMaxSize(100L);
        assertTrue(max.hasSearchCriteria());
    }

    private static FileInfoCriteria crit(Long minSize, Long maxSize) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setMinSize(minSize);
        c.setMaxSize(maxSize);
        return c;
    }
}
