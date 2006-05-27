package de.dal33t.powerfolder.test;

import java.io.File;

import de.dal33t.powerfolder.RConManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.test.TwoControllerTestCase;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.IdGenerator;

public class PowerFolderLinkTest extends TwoControllerTestCase {

    private static final String BASEDIR2 = "build/test/controller2/testFolder";

    private Folder folder2;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        FolderInfo testFolder = new FolderInfo("testFolder", IdGenerator
            .makeId(), true);

        folder2 = getContoller2().getFolderRepository().createFolder(
            testFolder, new File(BASEDIR2));

        // Give them time to join
        Thread.sleep(500);
    }

    public void testJoinFolderByLink() throws Exception {
        String link = folder2.getInvitation().toPowerFolderLink();
        // will be send to the first controller
        RConManager.sendCommand(RConManager.OPEN + link);
        // Give time for processing, connecting
        Thread.sleep(1000);

        // controller 1 should now have one folder
        assertEquals(1,
            getContoller1().getFolderRepository().getFolders().length);
        String otherID = getContoller1().getFolderRepository().getFolders()[0].getId();
        //Id's should match
        assertEquals(otherID, folder2.getId());
        // and both folders should have 2 members, this may fail if not
        // connected yet
        assertEquals(2, getContoller1().getFolderRepository().getFolders()[0]
            .getMembersCount());
        assertEquals(2, folder2.getMembersCount());
    }

}
