/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder.lucene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import de.dal33t.powerfolder.disk.dao.FileInfoDAOLuceneImpl;
import de.dal33t.powerfolder.disk.dao.FileInfoDocumentConverter;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.test.folder.db.FileInfoDAOTest;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.logging.LoggingManager;

/**
 * Test for utilizing Lucene as folder db.
 * 
 * @author sprajc
 */
public class LuceneFolderDBTest extends FileInfoDAOTest {

    private FileInfoDAOLuceneImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOLuceneImpl(getController(), new File(
            "build/test/lucene"));
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testFindFileInfo() {
        FileInfo expected = createRandomFileInfo(10, "MyExcelsheet.xls");
        dao.store(null, expected);
        FileInfo actual = dao.find(expected.getName(),
            expected.getFolderInfo().id, null);
        assertNotNull("FileInfo not found: " + expected.toDetailString(),
            actual);
        assertMatching(expected, actual);

        assertNull(dao.find(expected.getName().toLowerCase(), "AnuthaFolderID",
            null));
        assertNull(dao.find(expected.getName(), "AnuthaFolderID", null));
        assertNull(dao.find(expected.getName(), expected.getFolderInfo().id,
            "OTHERDOMAIN"));
        assertNotNull(dao.find(expected.getName(), expected.getFolderInfo().id,
            ""));

        // Test ignore case
        assertNotNull(dao.findByNameIgnoreCase(expected.getName(), expected
            .getFolderInfo().id, null));
        assertNotNull(dao
            .findByNameIgnoreCase(expected.getName().toLowerCase(), expected
                .getFolderInfo().id, null));
        assertNotNull(dao.findByNameIgnoreCase(
            "subdir1/SUBDIR2/MYEXCELSHEET.XLS-10", expected.getFolderInfo().id,
            null));
        assertNotNull(dao.findByNameIgnoreCase(
            "SUBdir1/subDIR2/mYeXCELsHEET.xls-10", expected.getFolderInfo().id,
            null));
    }

    public void testIndexFileInfo() {
        testIndexFileInfo(dao);
    }

    public void testFindNewestVersion() {
        testFindNewestVersion(dao);
    }

    public void testFindAll() {
        LoggingManager.setConsoleLogging(Level.SEVERE);
        testFindAll(dao, 5000);
    }

    public void testConvert() {
        FileInfo source = createRandomFileInfo(10, IdGenerator.makeId());
        Document doc = FileInfoDocumentConverter.convertToDocument(source);
        FileInfo target = FileInfoDocumentConverter
            .convertToFileInfo(null, doc);
        assertMatching(source, target);
    }

    private void assertMatching(FileInfo expected, FileInfo actual) {
        assertEquals(expected, actual);
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getFolderInfo(), actual.getFolderInfo());
        assertEquals(expected, actual);
        assertTrue(expected.isVersionAndDateIdentical(actual));
        assertTrue(expected.isCompletelyIdentical(actual));
        assertEquals(expected.getModifiedBy(), actual.getModifiedBy());
        assertEquals(expected.getModifiedDate(), actual.getModifiedDate());
    }

    public void testStoreFileInfo() throws CorruptIndexException, IOException,
        ParseException
    {
        Analyzer analyzer = new StandardAnalyzer();

        Directory directory = FSDirectory.getDirectory(new File(
            "build/debug/lucene"));

        // Store the index in memory:
        // Directory directory = new RAMDirectory();
        // To store an index on disk, use this instead:
        // Directory directory = FSDirectory.getDirectory("/tmp/testindex");
        IndexWriter iwriter = new IndexWriter(directory, analyzer, true,
            MaxFieldLength.LIMITED);
        iwriter.setMaxFieldLength(25000);

        FileInfo fInfo = createRandomFileInfo(12, "ExcelSheet1_Reports.xls");

        iwriter.addDocument(FileInfoDocumentConverter.convertToDocument(fInfo));
        iwriter.optimize();
        iwriter.close();

        // Now search the index:
        IndexSearcher isearcher = new IndexSearcher(directory);

        Query q = new TermQuery(new Term(FileInfo.PROPERTYNAME_FILE_NAME, fInfo
            .getName()));
        // Parse a simple query that searches for "text":
        // QueryParser parser = new QueryParser("fieldname", analyzer);
        // Query q = parser.parse("this*");
        TopDocs docs = isearcher.search(q, null, 1);

        assertEquals(1, docs.totalHits);
        // Iterate through the results:
        for (int i = 0; i < docs.totalHits; i++) {
            Document hitDoc = isearcher.doc(docs.scoreDocs[i].doc);
            FileInfo hitFInfo = FileInfoDocumentConverter.convertToFileInfo(
                null, hitDoc);
            assertMatching(fInfo, hitFInfo);
        }
        isearcher.close();
        directory.close();
    }
}
