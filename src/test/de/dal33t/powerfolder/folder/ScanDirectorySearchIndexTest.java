package de.dal33t.powerfolder.folder;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ScanDirectorySearchIndexTest extends ControllerTestCase {

    @Override
    protected void setUp() throws Exception {
        System.setProperty("powerfolder.index.startupDelayMs", "0");
        System.setProperty("powerfolder.index.minCommitIntervalMs", "0");
        super.setUp();
        ConfigurationEntry.SEARCH_INDEX_ENABLED.setValue(getController(), true);
        ConfigurationEntry.SEARCH_INDEX_CONTENT_EXTRACTION_ENABLED.setValue(getController(), false);
        ConfigurationEntry.SEARCH_INDEX_OCR_ENABLED.setValue(getController(), false);
        setupTestFolder(SyncProfile.HOST_FILES);
    }

    public void testADirectoryCreatedThroughScanDirectoryIsFound() throws Exception {
        final Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "seed.txt");
        scanFolder(folder);
        waitUntilFound(folder, "seed", 1);

        Path created = folder.getLocalBase().resolve("madeInTheWeb");
        DirectoryInfo directory = (DirectoryInfo) FileInfoFactory.newFile(folder, created, null,
            getController().getMySelf().getInfo(), null, null, true, null);
        Files.createDirectories(created);
        folder.scanDirectory(directory, created);

        waitUntilFound(folder, "madeInTheWeb", 1);
        List<FileInfo> hits = folder.searchFiles(criteria(folder, "madeInTheWeb"));
        assertTrue(hits.get(0).isDiretory());
        assertEquals("madeInTheWeb", hits.get(0).getRelativeName());
    }

    private static void waitUntilFound(final Folder folder, final String keyword, final int expected) {
        TestHelper.waitForCondition(30, new ConditionWithMessage() {
            @Override
            public boolean reached() {
                return folder.getSearchIndexManager() != null
                    && !folder.getSearchIndexManager().isRebuilding()
                    && folder.getSearchIndexManager().getPendingCount() == 0
                    && folder.searchFiles(criteria(folder, keyword)).size() == expected;
            }

            @Override
            public String message() {
                return folder + ": '" + keyword + "' found "
                    + folder.searchFiles(criteria(folder, keyword)).size() + " times, expected " + expected;
            }
        });
    }

    private static FileInfoCriteria criteria(Folder folder, String keyword) {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addMySelf(folder);
        criteria.addKeyWord(keyword);
        criteria.setRecursive(true);
        criteria.setPath("");
        return criteria;
    }
}
