package de.dal33t.powerfolder.test.folder.db;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileInfoDAOEhcacheTest extends FileInfoDAOTestCase {
    private Cache cache;
    private FileInfoDAO dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TestHelper.cleanTestDir();

        // Create a CacheManager using defaults
        CacheManager manager = CacheManager.create();

        // Create a Cache specifying its configuration.
        cache = new Cache("test", 30000, MemoryStoreEvictionPolicy.LRU, true,
            "build/test/ehcache", true, 60, 30, true, 0, null);
        manager.addCache(cache);

    }

    @Override
    protected void tearDown() throws Exception {
        CacheManager.getInstance().shutdown();
        super.tearDown();
    }

    public void xtestIndexFileInfo() {
        testIndexFileInfo(dao);
    }

    public void xtestFindNewestVersion() {
        testFindNewestVersion(dao);
    }

    public void xtestFindAll() {
        LoggingManager.setConsoleLogging(Level.SEVERE);
        testFindAll(dao, 5000);
    }

    public void xtestStoreFileInfo() throws SQLException {
        int nFiles = 30000;
        Map<String, FileInfo> fInfos = new HashMap<String, FileInfo>();
        for (int i = 0; i < nFiles; i++) {
            FileInfo fInfo = createRandomFileInfo(i, "Random");
            Element e = new Element(fInfo.getRelativeName(), fInfo);
            cache.put(e);
            fInfos.put(fInfo.getRelativeName(), fInfo);
        }
        cache.flush();
        assertEquals(nFiles, cache.getSize());
        List<String> keys = cache.getKeys();
        for (String key : keys) {
            FileInfo fInfo = (FileInfo) cache.get(key).getValue();
            String fileName = fInfo.getRelativeName();
            assertTrue(fileName.startsWith("subdir1/SUBDIR2/"));
        }
    }
    
    public void testNothing() {
        
    }
}
