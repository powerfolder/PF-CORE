package de.dal33t.powerfolder.test.folder;

import java.io.File;

import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class SyncMetaFolderTest extends TwoControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
    }

    public void testSyncSingleFile() {
        if (Feature.META_FOLDER.isDisabled()) {
            return;
        }
        joinTestFolder(SyncProfile.MANUAL_SYNCHRONIZATION);

        FolderRepository lisaRepo = getContollerLisa().getFolderRepository();
        FolderRepository bartRepo = getContollerBart().getFolderRepository();
        Folder bartMeta = bartRepo.getMetaFolderForParent(getFolderAtLisa()
            .getInfo());
        final Folder lisaMeta = lisaRepo
            .getMetaFolderForParent(getFolderAtLisa().getInfo());
        assertEquals(2, bartMeta.getMembersCount());
        assertEquals(2, lisaMeta.getMembersCount());

        final File bartFile = TestHelper.createRandomFile(bartMeta
            .getLocalBase());
        scanFolder(bartMeta);
        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisaMeta.getKnownItemCount() == 1;
            }

            public String message() {
                return "Lisa did not download meta data file: " + bartFile;
            }
        });

        assertTrue(bartFile.delete());
        scanFolder(bartMeta);

        TestHelper.waitForCondition(10, new ConditionWithMessage() {
            public boolean reached() {
                return lisaMeta.getKnownFiles().iterator().next().isDeleted();
            }

            public String message() {
                return "Lisa did not delete meta data file: " + bartFile;
            }
        });

    }
}
