/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.junit.folder;

import java.io.File;
import java.util.UUID;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.junit.TwoControllerTestCase;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Tests if both instance join the same folder by folder id
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderJoinTest extends TwoControllerTestCase {

    private String location1 = "build/test/controller1/testFolder";
    private String location2 = "build/test/controller2/testFolder";

    private Folder folder1;
    private Folder folder2;

    @Override
    protected void setUp() throws Exception
    {
        // Remove directries
        FileUtils.deleteDirectory(new File(location1));
        FileUtils.deleteDirectory(new File(location2));
        
        super.setUp();

        FolderInfo testFolder = new FolderInfo("testFolder", UUID.randomUUID()
            .toString(), true);

        folder1 = getContoller1().getFolderRepository().createFolder(
            testFolder, new File(location1));

        folder2 = getContoller2().getFolderRepository().createFolder(
            testFolder, new File(location2));

        // Give them time to join
        Thread.sleep(1000);
    }

    public void testJoinByID() {
        assertEquals(2, folder1.getMembersCount());
        assertEquals(2, folder2.getMembersCount());
    }

    public static void main(String[] args) {
        TestSuite suite = new TestSuite(FolderJoinTest.class);
        TestResult res = new TestResult();
        suite.run(res);
    }
}
