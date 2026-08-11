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
package de.dal33t.powerfolder.search;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public void testSearchByModifiedDateRange() throws Exception {
        Folder folder = getFolder();
        Path oldFile = TestHelper.createRandomFile(folder.getLocalBase(), "OldDoc.pdf");
        Path newFile = TestHelper.createRandomFile(folder.getLocalBase(), "NewDoc.pdf");

        long cutoff = 1_500_000_000_000L;
        Files.setLastModifiedTime(oldFile, FileTime.fromMillis(1_400_000_000_000L));
        Files.setLastModifiedTime(newFile, FileTime.fromMillis(1_600_000_000_000L));

        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria after = dateRangeCriteria();
        after.setModifiedAfter(new Date(cutoff));
        List<FileInfo> afterResults = getIndexManager().searchFiles(after);
        assertEquals(1, afterResults.size());
        assertEquals("NewDoc.pdf", afterResults.get(0).getFilenameOnly());

        FileInfoCriteria before = dateRangeCriteria();
        before.setModifiedBefore(new Date(cutoff));
        List<FileInfo> beforeResults = getIndexManager().searchFiles(before);
        assertEquals(1, beforeResults.size());
        assertEquals("OldDoc.pdf", beforeResults.get(0).getFilenameOnly());

        FileInfoCriteria between = dateRangeCriteria();
        between.setModifiedAfter(new Date(1_350_000_000_000L));
        between.setModifiedBefore(new Date(1_450_000_000_000L));
        List<FileInfo> betweenResults = getIndexManager().searchFiles(between);
        assertEquals(1, betweenResults.size());
        assertEquals("OldDoc.pdf", betweenResults.get(0).getFilenameOnly());
    }

    public void testSearchByModifiedDateInclusiveBoundary() throws Exception {
        Folder folder = getFolder();
        Path file = TestHelper.createRandomFile(folder.getLocalBase(), "Boundary.pdf");

        long exact = 1_500_000_000_000L;
        Files.setLastModifiedTime(file, FileTime.fromMillis(exact));

        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria atLower = dateRangeCriteria();
        atLower.setModifiedAfter(new Date(exact));
        assertEquals(1, getIndexManager().searchFiles(atLower).size());

        FileInfoCriteria atUpper = dateRangeCriteria();
        atUpper.setModifiedBefore(new Date(exact));
        assertEquals(1, getIndexManager().searchFiles(atUpper).size());

        FileInfoCriteria justAfter = dateRangeCriteria();
        justAfter.setModifiedAfter(new Date(exact + 1));
        assertEquals(0, getIndexManager().searchFiles(justAfter).size());

        FileInfoCriteria justBefore = dateRangeCriteria();
        justBefore.setModifiedBefore(new Date(exact - 1));
        assertEquals(0, getIndexManager().searchFiles(justBefore).size());
    }

    public void testSearchByModifiedDateCombinedWithKeyword() throws Exception {
        Folder folder = getFolder();
        Path oldReport = TestHelper.createRandomFile(folder.getLocalBase(), "Report_Old.pdf");
        Path newReport = TestHelper.createRandomFile(folder.getLocalBase(), "Report_New.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Invoice_New.pdf");

        long cutoff = 1_500_000_000_000L;
        Files.setLastModifiedTime(oldReport, FileTime.fromMillis(1_400_000_000_000L));
        Files.setLastModifiedTime(newReport, FileTime.fromMillis(1_600_000_000_000L));

        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria criteria = dateRangeCriteria();
        criteria.addKeyWord("report");
        criteria.setModifiedAfter(new Date(cutoff));

        List<FileInfo> results = getIndexManager().searchFiles(criteria);
        assertEquals(1, results.size());
        assertEquals("Report_New.pdf", results.get(0).getFilenameOnly());
    }

    public void testSearchBySizeRange() throws Exception {
        Folder folder = getFolder();
        Path small = folder.getLocalBase().resolve("small.bin");
        Path big = folder.getLocalBase().resolve("big.bin");
        Files.write(small, new byte[100]);
        Files.write(big, new byte[50_000]);

        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria minCrit = dateRangeCriteria();
        minCrit.setMinSize(1_000L);
        List<FileInfo> large = getIndexManager().searchFiles(minCrit);
        assertEquals(1, large.size());
        assertEquals("big.bin", large.get(0).getFilenameOnly());

        FileInfoCriteria maxCrit = dateRangeCriteria();
        maxCrit.setMaxSize(1_000L);
        List<FileInfo> smallRes = getIndexManager().searchFiles(maxCrit);
        assertEquals(1, smallRes.size());
        assertEquals("small.bin", smallRes.get(0).getFilenameOnly());

        FileInfoCriteria between = dateRangeCriteria();
        between.setMinSize(50L);
        between.setMaxSize(200L);
        List<FileInfo> betweenRes = getIndexManager().searchFiles(between);
        assertEquals(1, betweenRes.size());
        assertEquals("small.bin", betweenRes.get(0).getFilenameOnly());

        FileInfoCriteria none = dateRangeCriteria();
        none.setMinSize(1_000_000L);
        assertEquals(0, getIndexManager().searchFiles(none).size());
    }

    public void testSearchByCategory() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "holiday.jpg");
        TestHelper.createRandomFile(folder.getLocalBase(), "clip.mp4");
        TestHelper.createRandomFile(folder.getLocalBase(), "report.pdf");
        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria img = dateRangeCriteria();
        img.addCategory("image");
        List<FileInfo> images = getIndexManager().searchFiles(img);
        assertEquals(1, images.size());
        assertEquals("holiday.jpg", images.get(0).getFilenameOnly());

        FileInfoCriteria vid = dateRangeCriteria();
        vid.addCategory("video");
        assertEquals(1, getIndexManager().searchFiles(vid).size());

        FileInfoCriteria pdf = dateRangeCriteria();
        pdf.addCategory("pdf");
        assertEquals("a pdf has its own category", 1, getIndexManager().searchFiles(pdf).size());

        FileInfoCriteria doc = dateRangeCriteria();
        doc.addCategory("document");
        assertEquals("and is therefore no document", 0, getIndexManager().searchFiles(doc).size());

        /* Several categories are OR-combined - the image and the pdf, not the video. */
        FileInfoCriteria imageOrPdf = dateRangeCriteria();
        imageOrPdf.addCategory("image");
        imageOrPdf.addCategory("pdf");
        assertEquals(2, getIndexManager().searchFiles(imageOrPdf).size());

        FileInfoCriteria audio = dateRangeCriteria();
        audio.addCategory("audio");
        assertEquals(0, getIndexManager().searchFiles(audio).size());
    }

    public void testSortBySizeDescendingAndNameAscending() throws Exception {
        Folder folder = getFolder();
        Files.write(folder.getLocalBase().resolve("banana.bin"), new byte[300]);
        Files.write(folder.getLocalBase().resolve("apple.bin"), new byte[100]);
        Files.write(folder.getLocalBase().resolve("cherry.bin"), new byte[200]);
        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria sizeDesc = dateRangeCriteria();
        sizeDesc.setSortField(FileInfoCriteria.SortField.SIZE);
        sizeDesc.setSortDescending(true);
        List<FileInfo> bySize = getIndexManager().searchFiles(sizeDesc);
        assertEquals(3, bySize.size());
        assertEquals("banana.bin", bySize.get(0).getFilenameOnly());
        assertEquals("cherry.bin", bySize.get(1).getFilenameOnly());
        assertEquals("apple.bin", bySize.get(2).getFilenameOnly());

        FileInfoCriteria nameAsc = dateRangeCriteria();
        nameAsc.setSortField(FileInfoCriteria.SortField.NAME);
        List<FileInfo> byName = getIndexManager().searchFiles(nameAsc);
        assertEquals(3, byName.size());
        assertEquals("apple.bin", byName.get(0).getFilenameOnly());
        assertEquals("banana.bin", byName.get(1).getFilenameOnly());
        assertEquals("cherry.bin", byName.get(2).getFilenameOnly());
    }

    public void testSortByDateDescending() throws Exception {
        Folder folder = getFolder();
        Path older = folder.getLocalBase().resolve("older.bin");
        Path newer = folder.getLocalBase().resolve("newer.bin");
        Files.write(older, new byte[10]);
        Files.write(newer, new byte[10]);
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_400_000_000_000L));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(1_600_000_000_000L));
        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria dateDesc = dateRangeCriteria();
        dateDesc.setSortField(FileInfoCriteria.SortField.DATE);
        dateDesc.setSortDescending(true);
        List<FileInfo> byDate = getIndexManager().searchFiles(dateDesc);
        assertEquals(2, byDate.size());
        assertEquals("newer.bin", byDate.get(0).getFilenameOnly());
        assertEquals("older.bin", byDate.get(1).getFilenameOnly());
    }

    public void testSuggestTermsByExtensionRankedAndPrefixed() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "a.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "b.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "c.docx");
        scanFolder(folder);
        indexAndWait();

        Map<String, Integer> pdf = getIndexManager().suggestTerms("extensionExact", "pd");
        assertEquals(Integer.valueOf(2), pdf.get("pdf"));
        assertFalse(pdf.containsKey("docx"));

        Map<String, Integer> all = getIndexManager().suggestTerms("extensionExact", "");
        assertTrue(all.containsKey("pdf"));
        assertTrue(all.containsKey("docx"));

        assertTrue(getIndexManager().suggestTerms("extensionExact", "zzz").isEmpty());
    }

    private FileInfoCriteria dateRangeCriteria() {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addMySelf(getFolder());
        criteria.setRecursive(true);
        criteria.setType(FileInfoCriteria.Type.FILES_AND_DIRECTORIES);
        criteria.setIncludeDeleted(false);
        return criteria;
    }

    public void testPhraseSearchRespectsWordOrder() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Annual Financial Report Draft.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Financial Report Annual Draft.pdf");
        scanFolder(folder);
        indexAndWait();

        List<FileInfo> exact = getIndexManager().searchFiles("\"annual financial report\"", 10);
        assertEquals(1, exact.size());
        assertEquals("Annual Financial Report Draft.pdf", exact.get(0).getFilenameOnly());

        List<FileInfo> loose = getIndexManager().searchFiles("annual financial report", 10);
        assertEquals(2, loose.size());

        assertEquals(0, getIndexManager().searchFiles("\"report financial\"", 10).size());
    }

    /**
     * PFS-5653: a suggest field holds the whole value as one term, so a value containing a space survives as
     * one suggestion. Suggesting from an analyzed field would offer its single words instead - which is why
     * every remote operator points at a {@code *Exact} field (see SearchOperatorTest in PF-PRO).
     */
    public void testSuggestTermsKeepMultiWordValuesInOneTerm() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "minutes.pdf");
        scanFolder(folder);
        FileInfo file = getFolder().getKnownFiles().iterator().next();
        getFolder().updateTags(file, "[\"Project North\"]");
        indexAndWait();

        Map<String, Integer> exact = getIndexManager().suggestTerms("tagsExact", "pro");
        assertEquals("the whole tag is one suggestion", 1, exact.size());
        assertTrue("lowercased, so the prefix scan is case-insensitive", exact.containsKey("project north"));

        Map<String, Integer> analyzed = getIndexManager().suggestTerms("tags", "pro");
        assertTrue("the analyzed field only ever holds single words", analyzed.containsKey("project"));
        assertFalse(analyzed.containsKey("project north"));
    }

    /**
     * PFS-5653: name: looks at the file name only, while a plain keyword also matches the path a file sits
     * in - that is the whole reason for the operator to exist.
     */
    public void testNameFilterIgnoresThePath() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase().resolve("budget"), "notes.txt");
        TestHelper.createRandomFile(folder.getLocalBase(), "budget final.txt");
        scanFolder(folder);
        indexAndWait();

        FileInfoCriteria byKeyword = filesOnlyCriteria();
        byKeyword.addKeyWord("budget");
        assertEquals("the keyword also matches the file inside the budget directory", 2,
                getIndexManager().searchFiles(byKeyword).size());

        List<FileInfo> named = searchByFileName("budget");
        assertEquals(1, named.size());
        assertEquals("budget final.txt", named.get(0).getFilenameOnly());
    }

    public void testNameFilterMatchesEveryWordAnywhereInTheName() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Annual Report 2024.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Report Draft.pdf");
        scanFolder(folder);
        indexAndWait();

        assertEquals(1, searchByFileName("annual report").size());
        assertEquals(2, searchByFileName("report").size());
        assertEquals("a partial word still matches", 2, searchByFileName("repo").size());
        assertEquals(0, searchByFileName("invoice").size());
    }

    private List<FileInfo> searchByFileName(String fileName) {
        FileInfoCriteria criteria = filesOnlyCriteria();
        criteria.setFileName(fileName);
        return getIndexManager().searchFiles(criteria);
    }

    /** Directories carry a file name too - excluding them keeps the name: assertions about files. */
    private FileInfoCriteria filesOnlyCriteria() {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addMySelf(getFolder());
        criteria.setRecursive(true);
        criteria.setType(FileInfoCriteria.Type.FILES_ONLY);
        return criteria;
    }

    /** PFS-5653: typo tolerance is a fallback - a query that found something is not re-run fuzzily. */
    public void testFuzzyOnlyKicksInWhenNothingWasFound() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Mueller Vertrag.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Mueler Notiz.pdf");
        scanFolder(folder);
        indexAndWait();

        List<FileInfo> exact = getIndexManager().searchFiles("mueller", 10);
        assertEquals("the near-miss must not dilute a query that matches exactly", 1, exact.size());
        assertEquals("Mueller Vertrag.pdf", exact.get(0).getFilenameOnly());

        /* "muellre" matches nothing exactly, so the fallback runs and both near-misses come back - which is
         * precisely the widening that must not happen while "mueller" still has an exact hit. */
        assertEquals("a typo that matches nothing exactly falls back to fuzzy",
                2, getIndexManager().searchFiles("muellre", 10).size());
    }

    public void testFuzzyCanBeSwitchedOff() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Mueller Vertrag.pdf");
        scanFolder(folder);
        indexAndWait();

        assertEquals(1, getIndexManager().searchFiles("muellre", 10).size());

        ConfigurationEntry.SEARCH_INDEX_FUZZY_ENABLED.setValue(getController(), false);
        assertEquals("no typo tolerance once disabled", 0, getIndexManager().searchFiles("muellre", 10).size());
        assertEquals("exact matching still works", 1, getIndexManager().searchFiles("mueller", 10).size());
    }

    /** PFS-5653: a quoted single word asks for the exact term, not for a prefix/wildcard/fuzzy match. */
    public void testQuotedSingleWordIsMatchedExactly() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Mueller Vertrag.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Muellerhaus Notiz.pdf");
        scanFolder(folder);
        indexAndWait();

        List<FileInfo> quoted = getIndexManager().searchFiles("\"mueller\"", 10);
        assertEquals(1, quoted.size());
        assertEquals("Mueller Vertrag.pdf", quoted.get(0).getFilenameOnly());

        assertEquals("unquoted still matches the longer name too",
                2, getIndexManager().searchFiles("mueller", 10).size());
    }

    public void testFuzzyTypoTolerance() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Mueller Vertrag.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Report Summary.pdf");
        scanFolder(folder);
        indexAndWait();

        assertTrue("typo 'mueler' should still find Mueller",
                getIndexManager().searchFiles("mueler", 10).size() >= 1);
        assertTrue("typo 'reprot' should still find Report",
                getIndexManager().searchFiles("reprot", 10).size() >= 1);
        assertEquals("nonsense must not match",
                0, getIndexManager().searchFiles("xyzqwk", 10).size());
    }

    public void testNegationExcludesTerm() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "Report_Annual.pdf");
        TestHelper.createRandomFile(folder.getLocalBase(), "Report_Monthly.pdf");
        scanFolder(folder);
        indexAndWait();

        assertEquals(2, getIndexManager().searchFiles("report", 10).size());

        List<FileInfo> excluded = getIndexManager().searchFiles("report -annual", 10);
        assertEquals(1, excluded.size());
        assertEquals("Report_Monthly.pdf", excluded.get(0).getFilenameOnly());
    }

    public void testNegationOnlyReturnsEverythingElse() throws Exception {
        Folder folder = getFolder();
        TestHelper.createRandomFile(folder.getLocalBase(), "KeepMe.txt");
        TestHelper.createRandomFile(folder.getLocalBase(), "DropVideo.txt");
        scanFolder(folder);
        indexAndWait();

        List<FileInfo> results = getIndexManager().searchFiles("-dropvideo", 10);
        assertEquals(1, results.size());
        assertEquals("KeepMe.txt", results.get(0).getFilenameOnly());
    }

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