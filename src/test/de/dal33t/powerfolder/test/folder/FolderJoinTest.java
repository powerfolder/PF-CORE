/* $Id: FolderJoinTest.java,v 1.2 2006/04/16 23:01:52 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.test.Condition;
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

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        connectBartAndLisa();
    }

    public void testJoinSecretFolder() {
        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    public void testJoinPublicFolder() {
        // Join on testfolder
        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), false);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, TESTFOLDER_BASEDIR_LISA);

        assertEquals(2, getContollerBart().getFolderRepository().getFolder(
            testFolder).getMembersCount());
        assertEquals(2, getContollerLisa().getFolderRepository().getFolder(
            testFolder).getMembersCount());
    }

    public void testJoinMultipleFolders() {
        int nFolders = 15;
        for (int i = 0; i < nFolders; i++) {
            FolderInfo testFolder = createRandomFolder("r-" + i);
            File folderDirBart = new File(TESTFOLDER_BASEDIR_BART,
                testFolder.name);
            File folderDirLisa = new File(TESTFOLDER_BASEDIR_LISA,
                testFolder.name);
            System.err.println("Joining folder: " + testFolder);
            joinFolder(testFolder, folderDirBart, folderDirLisa);
        }
       
        Folder[] bartsFolders = getContollerBart().getFolderRepository()
            .getFolders();
        Folder[] lisasFolders = getContollerLisa().getFolderRepository()
            .getFolders();
        assertEquals(nFolders, getContollerBart().getFolderRepository()
            .getFoldersCount());
        assertEquals(nFolders, getContollerLisa().getFolderRepository()
            .getFoldersCount());
        assertEquals(nFolders, bartsFolders.length);
        assertEquals(nFolders, lisasFolders.length);
        for (Folder folder : lisasFolders) {
            assertEquals(2, folder.getMembersCount());
        }
        for (Folder folder : bartsFolders) {
            assertEquals(2, folder.getMembersCount());
        }
    }

    private FolderInfo createRandomFolder(String nameSuffix) {
        String folderName = "testFolder-" + nameSuffix;
        return new FolderInfo(folderName, folderName + IdGenerator.makeId(), true);
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
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);
        TestHelper.createRandomFile(TESTFOLDER_BASEDIR_BART);

        final Folder folderBart = getContollerBart().getFolderRepository().createFolder(testFolder,
            TESTFOLDER_BASEDIR_BART, SyncProfile.MANUAL_DOWNLOAD, false);
        
        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderBart.getKnownFiles().length >= 3;
            }
        });

        // Now let lisa join with auto-download
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, TESTFOLDER_BASEDIR_LISA,
                SyncProfile.AUTO_DOWNLOAD_FROM_ALL, false);

        TestHelper.waitForCondition(20, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().length >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownFilesCount());
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
            .createFolder(testFolder, TESTFOLDER_BASEDIR_BART,
                SyncProfile.MANUAL_DOWNLOAD, false);

        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        TestHelper.createRandomFile(folderBart.getLocalBase());
        folderBart.forceScanOnNextMaintenance();
        folderBart.maintain();

        // Set lisa in silent mode
        getContollerLisa().setSilentMode(true);

        // Now let lisa join with auto-download
        final Folder folderLisa = getContollerLisa().getFolderRepository()
            .createFolder(testFolder, TESTFOLDER_BASEDIR_LISA,
                SyncProfile.AUTO_DOWNLOAD_FROM_ALL, false);

        TestHelper.waitForCondition(50, new Condition() {
            public boolean reached() {
                return folderLisa.getKnownFiles().length >= 3;
            }
        });

        assertEquals(3, folderLisa.getKnownFilesCount());
        assertEquals(4, folderLisa.getLocalBase().list().length);
    }
}
