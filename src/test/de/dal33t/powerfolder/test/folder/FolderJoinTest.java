/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests if both instance join the same folder by folder id
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderJoinTest extends TwoControllerTestCase {

    private String location1 = "build/test/controller1/testFolder";
    private String location2 = "build/test/controller2/testFolder";

    @Override
    protected void setUp() throws Exception
    {
        // Remove directries
        FileUtils.deleteDirectory(new File(location1));
        FileUtils.deleteDirectory(new File(location2));

        super.setUp();
    }

    public void testJoinByID() {
        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        joinFolder(testFolder, new File(location1), new File(location2));

        assertEquals(2, getContoller1().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContoller2().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }
}
