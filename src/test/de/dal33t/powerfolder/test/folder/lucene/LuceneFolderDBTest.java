package de.dal33t.powerfolder.test.folder.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
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
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * Test for utilizing Lucene as folder db.
 * 
 * @author sprajc
 */
public class LuceneFolderDBTest extends ControllerTestCase {

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
            "subdir1/SUBDIR2/MYEXCELSHEET.XLS", expected.getFolderInfo().id,
            null));
        assertNotNull(dao.findByNameIgnoreCase(
            "SUBdir1/subDIR2/mYeXCELsHEET.xls", expected.getFolderInfo().id,
            null));
    }

    public void testIndexFileInfo() {
        FileInfo expected = createRandomFileInfo(10, "MyExcelsheet.xls");
        dao.store(null, expected);
        FileInfo retrieved = dao.find(expected.getName(), expected
            .getFolderInfo().id, null);

        // Should overwrite
        dao.store(null, retrieved);

        assertEquals(1, dao.findAll(null).size());
        assertEquals(1, dao.count(null));
    }

    public void testFindNewestVersion() {
        FileInfo expected = createRandomFileInfo(10, "MyExcelsheet.xls");
        expected.setVersion(1);
        dao.store(null, expected);

        FileInfo remote = (FileInfo) expected.clone();
        remote.setVersion(0);
        dao.store("REMOTE1", remote);

        FileInfo remote2 = (FileInfo) expected.clone();
        remote2.setVersion(2);
        dao.store("REMOTE2", remote2);

        assertNotNull(dao.findNewestVersion(expected, ""));
        assertEquals(1, dao.findNewestVersion(expected, "").getVersion());
        assertNotNull(dao.findNewestVersion(expected, "REMOTE1", "REMOTE2",
            null));
        assertEquals(2, dao.findNewestVersion(expected, "REMOTE1", "REMOTE2",
            null).getVersion());
        assertNotNull(dao.findNewestVersion(expected, "REMOTE1", "REMOTE2"));
        assertEquals(2, dao.findNewestVersion(expected, "REMOTE1", "REMOTE2")
            .getVersion());
    }

    public void testFindAll() {
        LoggingManager.setConsoleLogging(Level.SEVERE);
        int n = 5000;
        Collection<FileInfo> fInfos = new ArrayList<FileInfo>();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store(null, fInfos);
        fInfos.clear();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store("XXX", fInfos);
        fInfos.clear();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store("WWW", fInfos);

        assertEquals(n, dao.count(null));
        assertEquals(n, dao.count("XXX"));
        assertEquals(n, dao.count("WWW"));
        assertEquals(0, dao.count("123"));

        assertEquals(n, dao.findAll(null).size());
        assertEquals(n, dao.findAll("XXX").size());
        assertEquals(n, dao.findAll("WWW").size());
        assertEquals(0, dao.findAll("123").size());

        assertEquals(n, dao.findAllAsMap(null).size());
        assertEquals(n, dao.findAllAsMap("XXX").size());
        assertEquals(n, dao.findAllAsMap("WWW").size());
        assertEquals(0, dao.findAllAsMap("123").size());
    }

    public void testConvert() {
        FileInfo source = createRandomFileInfo(10, IdGenerator.makeId());
        Document doc = FileInfoDocumentConverter.convertToDocument(source);
        FileInfo target = FileInfoDocumentConverter
            .convertToFileInfo(null, doc);
        assertMatching(source, target);
    }

    private void assertMatching(FileInfo expected, FileInfo actual) {
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

    private static FileInfo createRandomFileInfo(int n, String name) {
        FolderInfo foInfo = createRandomFolderInfo();
        FileInfo fInfo = new FileInfo(foInfo, "subdir1/SUBDIR2/" + name);
        fInfo.setSize((long) Math.random() * Long.MAX_VALUE);
        MemberInfo mInfo = new MemberInfo(IdGenerator.makeId(), IdGenerator
            .makeId(), IdGenerator.makeId());
        fInfo.setModifiedInfo(mInfo, new Date());
        return fInfo;
    }

    private static FolderInfo createRandomFolderInfo() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            IdGenerator.makeId());
        return foInfo;
    }
}
