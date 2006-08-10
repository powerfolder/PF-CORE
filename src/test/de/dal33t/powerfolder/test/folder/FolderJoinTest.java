/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.TestHelper;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests if both instance join the same folder by folder id
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderJoinTest extends TwoControllerTestCase {

    private static final String BASEDIR1 = "build/test/controllerBart/testFolder";
    private static final String BASEDIR2 = "build/test/controllerLisa/testFolder";

    @Override
    protected void setUp() throws Exception
    {
        // Remove directries
        FileUtils.deleteDirectory(new File(BASEDIR1));
        FileUtils.deleteDirectory(new File(BASEDIR2));

        super.setUp();
    }

    public void testJoinByID() {
        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        joinFolder(testFolder, new File(BASEDIR1), new File(BASEDIR2));

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     * 
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownload() throws FolderException {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        // Prepare folder on "host" Bart.
        TestHelper.createRandomFile(new File(BASEDIR1));
        TestHelper.createRandomFile(new File(BASEDIR1));
        TestHelper.createRandomFile(new File(BASEDIR1));

        getContollerBart().getFolderRepository().createFolder(testFolder,
            new File(BASEDIR1));

        // Now let lisa join with auto-download
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, new File(BASEDIR2),
                SyncProfile.AUTO_DOWNLOAD_FROM_ALL, false);

        TestHelper.waitForCondition(20, new TestHelper.Condition() {
            public boolean reached() {
                return folderLisa.getFiles().length >= 3;
            }
        });

        assertEquals(3, folderLisa.getFilesCount());
        assertEquals(4, folderLisa.getLocalBase().list().length);
    }

    /**
     * Test the download starting after joined a folder with auto-download.
     * <p>
     * Trac #19
     * 
     * @throws FolderException
     * @throws IOException
     */
    public void testStartAutoDownloadInSilentMode() throws FolderException {
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        // Prepare folder on "host" Bart.
        Folder folderBart = getContollerBart().getFolderRepository()
            .createFolder(testFolder, new File(BASEDIR1));

        TestHelper.createRandomFile(new File(BASEDIR1));
        TestHelper.createRandomFile(new File(BASEDIR1));
        TestHelper.createRandomFile(new File(BASEDIR1));
        folderBart.scanLocalFiles(true);

        // Set lisa in silent mode
        getContollerLisa().setSilentMode(true);

        // Now let lisa join with auto-download
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, new File(BASEDIR2),
                SyncProfile.AUTO_DOWNLOAD_FROM_ALL, false);

        TestHelper.waitForCondition(50, new TestHelper.Condition() {
            public boolean reached() {
                return folderLisa.getFiles().length >= 3;
            }
        });

        assertEquals(3, folderLisa.getFilesCount());
        assertEquals(4, folderLisa.getLocalBase().list().length);
    }
}
