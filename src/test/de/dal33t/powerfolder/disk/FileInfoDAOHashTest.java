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
package de.dal33t.powerfolder.disk;

import java.util.logging.Level;

import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.logging.LoggingManager;

public class FileInfoDAOHashTest extends FileInfoDAOTestCase {
    private FileInfoDAO dao;
    private DiskItemFilter filter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filter = new DiskItemFilter();
        dao = new FileInfoDAOHashMapImpl("ME", filter);
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
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

    public void testFindByOID() {
        String[] domains = new String[]{null, "anydomain"};
        FileInfo noID = createFileInfo("subdir/relative/NameNoID.txt", 1, false);
        dao.store(null, noID);

        FileInfo fInfoWithID = createFileInfo("dir/FileWITH_ID.txt", 1, false);
        String testID = IdGenerator.makeFileId();
        fInfoWithID = FileInfoFactory.setOID(fInfoWithID, testID);
        dao.store(null, fInfoWithID);

        // 1) Call with ID only
        FileInfo found = dao.findNewestByOID(testID, domains);
        assertNotNull(found);
        testAssertEquals(fInfoWithID, found);

        // 2) Call with ID and hint FileInfo
        found = dao.findNewestByOID(testID, domains);
        assertNotNull(found);
        testAssertEquals(fInfoWithID, found);

        // 3) Call with ID and WRONG hint FileInfo
        found = dao.findNewestByOID(testID, domains);
        assertNotNull(found);
        testAssertEquals(fInfoWithID, found);

        // Don't find any
        assertNull(dao.findNewestByOID("ID_OTHER", domains));

        // Advanced: Different files (higher version) with same ID:
        FileInfo fInfoWithID_2 = createFileInfo(
            "dir/another/FileWITH_ID_version2.txt", 2, false);
        fInfoWithID_2 = FileInfoFactory.setOID(fInfoWithID_2, testID);
        dao.store(null, fInfoWithID_2);

        found = dao.findNewestByOID(testID, domains);
        assertNotNull(found);
        testAssertEquals(fInfoWithID_2, found);
    }
}
