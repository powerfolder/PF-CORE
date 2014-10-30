package de.dal33t.powerfolder.test.folder.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOSQLImpl;
import de.dal33t.powerfolder.disk.dao.FileInfoSQLConverter;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.test.TestHelper;

public class FileInfoDAOH2DBTest extends FileInfoDAOTestCase {
    private Connection con;
    private String createTableSQL;
    private FileInfoDAO dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TestHelper.cleanTestDir();
        Class.forName("org.h2.Driver");
        con = DriverManager.getConnection("jdbc:h2:" + TestHelper.getTestDir()
            + "/db_raw", "sa", "");
        dao = new FileInfoDAOSQLImpl(getController(), "jdbc:h2:"
            + TestHelper.getTestDir() + "/db", "sa", "", null);
        byte[] content = StreamUtils.readIntoByteArray(Thread.currentThread()
            .getContextClassLoader().getResourceAsStream(
                "sql/create_folder_table.sql"));
        createTableSQL = new String(content, "UTF-8").replace("%TABLE_NAME%",
            "FileInfo");
    }

    @Override
    protected void tearDown() throws Exception {
        con.close();
        dao.stop();
        super.tearDown();
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

    public void testCreateTable() throws SQLException {
        Statement s = con.createStatement();
        s.execute(createTableSQL);
        s.close();

        s = con.createStatement();
        assertTrue(s.execute("SELECT * FROM FileInfo"));
        assertFalse(s.getResultSet().next());
        assertFalse(s.getResultSet().next());
        s.close();
    }

    public void testInsert() throws SQLException {
        testCreateTable();

        PreparedStatement ps = con
            .prepareStatement(FileInfoDAOSQLImpl.SQL_INSERT);
        int i = 1;
        ps.setString(i++, "DOMAIN");
        ps.setString(i++, "TestFile.txt");
        ps.setString(i++, "TestFile.txt".toLowerCase());
        ps.setBoolean(i++, false);
        ps.setLong(i++, 1337);
        ps.setString(i++, "[XIDSNNID]");
        ps.setLong(i++, 1238604350944L);
        ps.setLong(i++, 2);
        ps.setBoolean(i++, Boolean.FALSE);
        ps.setString(i++, "[FOLDER_ID]");
        assertFalse(ps.execute());
        ps.close();
    }

    public void testSelect() throws SQLException {
        testInsert();

        // All
        PreparedStatement ps = con.prepareStatement("SELECT * FROM FileInfo");
        assertTrue(ps.execute());
        ResultSet rs = ps.getResultSet();
        assertTrue(rs.next());
        assertEquals("TestFile.txt", rs.getString("fileName"));
        assertEquals("testfile.txt", rs.getString("fileNameLower"));
        assertEquals(1337, rs.getLong("size"));
        assertEquals("[XIDSNNID]", rs.getString("modifiedByNodeId"));
        assertEquals(new Date(1238604350944L), new Date(rs
            .getLong("lastModifiedDate")));
        assertEquals(2, rs.getLong("version"));
        assertEquals(Boolean.FALSE.booleanValue(), rs.getBoolean("deleted"));
        assertEquals("[FOLDER_ID]", rs.getString("folderId"));
        ps.close();

        // Single file match
        ps = con.prepareStatement("SELECT * FROM FileInfo WHERE fileName=?");
        ps.setString(1, "TestFile.txt");
        assertTrue(ps.execute());
        rs = ps.getResultSet();
        assertTrue(rs.next());
        assertEquals("TestFile.txt", rs.getString("fileName"));
        assertEquals(1337, rs.getLong("size"));
        assertEquals("[XIDSNNID]", rs.getString("modifiedByNodeId"));
        assertEquals(new Date(1238604350944L), new Date(rs
            .getLong("lastModifiedDate")));
        assertEquals(2, rs.getLong("version"));
        assertEquals(Boolean.FALSE.booleanValue(), rs.getBoolean("deleted"));
        assertEquals("[FOLDER_ID]", rs.getString("folderId"));
        ps.close();

        ps = con.prepareStatement("SELECT * FROM FileInfo WHERE fileName=?");
        ps.setString(1, "testfile.txt");
        assertTrue(ps.execute());
        rs = ps.getResultSet();
        assertFalse(rs.next());
        ps.close();

        // Single file match by lower case filename
        ps = con
            .prepareStatement("SELECT * FROM FileInfo WHERE fileNameLower=?");
        ps.setString(1, "testfile.txt");
        assertTrue(ps.execute());
        rs = ps.getResultSet();
        assertTrue(rs.next());
        assertEquals("TestFile.txt", rs.getString("fileName"));
        assertEquals(1337, rs.getLong("size"));
        assertEquals("[XIDSNNID]", rs.getString("modifiedByNodeId"));
        assertEquals(new Date(1238604350944L), new Date(rs
            .getLong("lastModifiedDate")));
        assertEquals(2, rs.getLong("version"));
        assertEquals(Boolean.FALSE.booleanValue(), rs.getBoolean("deleted"));
        assertEquals("[FOLDER_ID]", rs.getString("folderId"));
        ps.close();
    }

    public void testStoreFileInfo() throws SQLException {
        testCreateTable();

        int nFiles = 30000;
        Map<String, FileInfo> fInfos = new HashMap<String, FileInfo>();
        PreparedStatement ps = con
            .prepareStatement(FileInfoDAOSQLImpl.SQL_INSERT);
        for (int i = 0; i < nFiles; i++) {
            FileInfo fInfo = createRandomFileInfo(i, "Random");
            ps.setString(1, "DOMAIN");
            FileInfoSQLConverter.set(fInfo, ps);
            fInfos.put(fInfo.getRelativeName(), fInfo);
            assertFalse(ps.execute());
        }
        ps.close();

        ps = con.prepareStatement("SELECT COUNT(*) FROM FileInfo");
        assertTrue(ps.execute());
        ResultSet rs = ps.getResultSet();
        assertTrue(rs.next());
        assertEquals(nFiles, rs.getInt(1));
        ps.close();

        ps = con.prepareStatement("SELECT * FROM FileInfo");
        assertTrue(ps.execute());
        rs = ps.getResultSet();
        while (rs.next()) {
            String fileName = rs.getString("fileName");
            assertTrue(fileName.startsWith("subdir1/SUBDIR2/"));
            assertTrue(rs.getString("fileNameLower").startsWith(
                "subdir1/subdir2/"));

            FileInfo expected = fInfos.get(fileName);
            FileInfo actual = FileInfoSQLConverter.get(null, rs);
            assertEquals(expected, actual);
        }
        ps.close();
    }
}
