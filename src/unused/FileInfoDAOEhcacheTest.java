/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.folder.db;

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
