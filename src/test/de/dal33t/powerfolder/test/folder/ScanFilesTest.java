/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.test.ControllerTestCase;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TestHelper.Condition;

/**
 * Tests the scanning of file in the local folders.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ScanFilesTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        setupTestFolder(SyncProfile.MANUAL_DOWNLOAD);
    }

    /**
     * Test the scan of file and dirs, that just change the case.
     * <p>
     * e.g. "TestDir/SubDir/MyFile.txt" to "testdir/subdir/myfile.txt"
     * <p>
     * TRAC #232
     */
    public void testCaseChangeScan() {
        File testFile = TestHelper.createRandomFile(getFolder().getLocalBase(),
            "TESTFILE.TXT");

        getFolder().forceScanOnNextMaintenance();
        getFolder().maintain();

        TestHelper.waitForCondition(10, new Condition() {
            public boolean reached() {
                return getFolder().getFilesCount() == 1;
            }
        });

        assertEquals(testFile.getName(), getFolder().getFiles()[0]
            .getFilenameOnly());

        // Change case
        testFile.renameTo(new File(getFolder().getLocalBase(), "testfile.txt"));

        getFolder().forceScanOnNextMaintenance();
        getFolder().maintain();

        // HOW TO HANDLE THAT? WHAT TO EXPECT??
        // assertEquals(1, getFolderAtBart().getFilesCount());
    }

}
