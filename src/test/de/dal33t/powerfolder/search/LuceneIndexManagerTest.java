package de.dal33t.powerfolder.search;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.nio.file.Path;
import java.util.List;

public class LuceneIndexManagerTest extends ControllerTestCase {

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

    private LuceneIndexManager getIndexManager() {
        return getFolder().getSearchIndexManager();
    }

    private void indexAndWait() {
        int expectedCount = getFolder().getKnownFiles().size();
        LuceneIndexManager im = getIndexManager();
        im.indexFiles(getFolder().getKnownFiles());
        TestHelper.waitForCondition(30, new Condition() {
            @Override
            public boolean reached() {
                return im.getIndexEntryCount() >= expectedCount;
            }
        });
    }

    // -----------------------------------------------------------------------
    // Basic indexing + search
    // -----------------------------------------------------------------------

    public void testIndexAndSearchByFilename() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Report_2024.pdf");
        scanFolder(folder);
        assertEquals(1, folder.getKnownFiles().size());

        indexAndWait();

        List<FileInfo> results = getIndexManager().searchFiles("report", 10);
        assertEquals(1, results.size());
        assertEquals("Report_2024.pdf", results.get(0).getFilenameOnly());
    }

    public void testPrefixSearch() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Bestaetigung_des_Wohnungsgebers.pdf");
        scanFolder(folder);

        indexAndWait();

        assertTrue("Search for 'woh' should find the file",
                getIndexManager().searchFiles("woh", 10).size() > 0);
        assertTrue("Search for 'bes' should find the file",
                getIndexManager().searchFiles("bes", 10).size() > 0);
        assertTrue("Search for 'wohn' should find the file",
                getIndexManager().searchFiles("wohn", 10).size() > 0);
    }

    public void testSearchMultipleFiles() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Vertrag_Miete.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Vertrag_Kauf.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Rechnung_2024.pdf");
        scanFolder(folder);
        assertEquals(3, folder.getKnownFiles().size());

        indexAndWait();

        List<FileInfo> results = getIndexManager().searchFiles("vertrag", 10);
        assertEquals(2, results.size());

        results = getIndexManager().searchFiles("rechnung", 10);
        assertEquals(1, results.size());
    }

    public void testSearchNoResults() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "document.pdf");
        scanFolder(folder);

        indexAndWait();

        List<FileInfo> results = getIndexManager().searchFiles("nonexistent", 10);
        assertEquals(0, results.size());
    }

    // -----------------------------------------------------------------------
    // Purge files
    // -----------------------------------------------------------------------

    public void testPurgeRemovesFromIndex() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "ToPurge.txt");
        scanFolder(folder);

        indexAndWait();
        assertEquals(1, getIndexManager().searchFiles("topurge", 10).size());

        getIndexManager().purgeFiles(folder.getKnownFiles());

        assertEquals(0, getIndexManager().searchFiles("topurge", 10).size());
        assertEquals(0, getIndexManager().getIndexEntryCount());
    }

    // -----------------------------------------------------------------------
    // Subdirectory search
    // -----------------------------------------------------------------------

    public void testSearchInSubdirectory() throws Exception {
        Folder folder = getFolder();
        Path subDir = folder.getLocalBase().resolve("invoices");
        TestHelper.createRandomFile(subDir, "Invoice_001.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Invoice_002.pdf");
        scanFolder(folder);

        indexAndWait();

        List<FileInfo> all = getIndexManager().searchFiles("invoice", 10);
        assertTrue("Should find at least both invoice files", all.size() >= 2);
    }

    // -----------------------------------------------------------------------
    // Rebuild
    // -----------------------------------------------------------------------

    public void testRebuildIndex() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "BeforeRebuild.txt");
        scanFolder(folder);

        indexAndWait();

        assertEquals(1, getIndexManager().searchFiles("beforerebuild", 10).size());

        getIndexManager().rebuildIndex();
        TestHelper.waitForCondition(10, () -> !getIndexManager().isRebuilding());
        TestHelper.waitMilliSeconds(500);

        assertEquals(1, getIndexManager().searchFiles("beforerebuild", 10).size());
    }

    // -----------------------------------------------------------------------
    // Index entry count
    // -----------------------------------------------------------------------

    public void testIndexEntryCount() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "File1.txt");
        TestHelper.createRandomFile(folder.getLocalBase(), "File2.txt");
        TestHelper.createRandomFile(folder.getLocalBase(), "File3.txt");
        scanFolder(folder);

        indexAndWait();

        assertEquals(3, getIndexManager().getIndexEntryCount());
    }

    // -----------------------------------------------------------------------
    // Empty query
    // -----------------------------------------------------------------------

    public void testEmptyQueryReturnsAllFiles() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "AnyFile.txt");
        scanFolder(folder);

        indexAndWait();

        assertTrue("Empty query should return all indexed files",
                getIndexManager().searchFiles("", 10).size() >= 1);
    }

    // -----------------------------------------------------------------------
    // Umlaut / accent handling
    // -----------------------------------------------------------------------

    public void testAccentFolding() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Uebergabe_Protokoll.pdf");
        scanFolder(folder);

        indexAndWait();

        assertTrue("Search for 'ueberg' should find umlauted file",
                getIndexManager().searchFiles("ueberg", 10).size() > 0);
    }

    // -----------------------------------------------------------------------
    // Multiple index + search cycles
    // -----------------------------------------------------------------------

    public void testIncrementalIndexing() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "First.txt");
        scanFolder(folder);
        indexAndWait();
        assertEquals(1, getIndexManager().getIndexEntryCount());

        TestHelper.createRandomFile(folder.getLocalBase(), "Second.txt");
        scanFolder(folder);
        indexAndWait();
        assertEquals(2, getIndexManager().getIndexEntryCount());

        assertEquals(1, getIndexManager().searchFiles("first", 10).size());
        assertEquals(1, getIndexManager().searchFiles("second", 10).size());
    }
}