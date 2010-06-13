package de.dal33t.powerfolder.test.folder.dao;

import java.util.logging.Level;

import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileInfoDAOHashTest extends FileInfoDAOTestCase {
    private FileInfoDAO dao;
    private DiskItemFilter filter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestHelper.cleanTestDir();
        filter = new DiskItemFilter();
        dao = new FileInfoDAOHashMapImpl("ME", filter);
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testStats() {
        testStats(dao, filter, 1);
        testStats(dao, filter, 1000);
        testStats(dao, filter, 5000);
    }

    public void testIndexFileInfo() {
        testIndexFileInfo(dao);
    }

    public void testFindNewestVersion() {
        testFindNewestVersion(dao);
    }

    public void testFindAll() {
        LoggingManager.setConsoleLogging(Level.SEVERE);
        testFindAll(dao, 5000);
    }

    public void testFindInDir() {
        testFindInDir(dao, 1);
        testFindInDir(dao, 100);
        testFindInDir(dao, 5000);
    }

}
