package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.light.MemberInfo;

import java.util.Date;
import java.util.UUID;

/**
 * PFS-5653: "modifiedby:" and "device:" are matched word by word - a display name of two words used to be
 * looked for as one single string, which the analyzed index never holds.
 */
public class FileInfoDAOEditorNameFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, byEditor("ledger.xlsx", "Jane Doe", "jane.doe@example.com", "Jane Laptop"));
        dao.store(null, byEditor("minutes.docx", "Erik Sund", "erik@example.com", "Erik Desktop"));
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testNoEditorFilterReturnsAll() {
        assertEquals(2, dao.findFilesFast(byModifiedBy(null)).size());
        assertEquals(2, dao.findFilesFast(byDevice(null)).size());
    }

    public void testDisplayNameOfTwoWordsMatches() {
        assertEquals(1, dao.findFilesFast(byModifiedBy("Jane Doe")).size());
        assertEquals("case does not matter", 1, dao.findFilesFast(byModifiedBy("jane doe")).size());
    }

    /** Every word has to sit in the editor, but the order they were typed in is none of our business. */
    public void testWordOrderDoesNotMatter() {
        assertEquals(1, dao.findFilesFast(byModifiedBy("doe jane")).size());
    }

    public void testASingleWordStillMatches() {
        assertEquals(1, dao.findFilesFast(byModifiedBy("jane")).size());
        assertEquals(1, dao.findFilesFast(byModifiedBy("erik")).size());
    }

    public void testTheUsernameIsMatchedToo() {
        assertEquals(1, dao.findFilesFast(byModifiedBy("jane.doe@example.com")).size());
        assertEquals("both accounts share the domain", 2,
                dao.findFilesFast(byModifiedBy("example.com")).size());
    }

    /**
     * The display name, the username and the device nick are three separate texts. A word may sit in any
     * of them, but never across the seam between two - "doejane" is nobody.
     */
    public void testAWordNeverMatchesAcrossTwoFields() {
        assertEquals("the seam between display name and username", 0,
                dao.findFilesFast(byModifiedBy("doejane")).size());
        assertEquals("and the seam between username and device nick", 0,
                dao.findFilesFast(byModifiedBy("example.comjane")).size());
    }

    public void testUnknownEditorMatchesNothing() {
        assertEquals(0, dao.findFilesFast(byModifiedBy("smith")).size());
        assertEquals("one word of two is not enough", 0,
                dao.findFilesFast(byModifiedBy("jane smith")).size());
    }

    /** The device nick is asked for with "device:", and the same word rule applies to it. */
    public void testDeviceNameOfTwoWordsMatches() {
        assertEquals(1, dao.findFilesFast(byDevice("Jane Laptop")).size());
        assertEquals(1, dao.findFilesFast(byDevice("laptop jane")).size());
        assertEquals(0, dao.findFilesFast(byDevice("jane desktop")).size());
    }

    /** "modifiedby:" reaches the device nick as well - it is who wrote the file, from another angle. */
    public void testEditorFilterAlsoReachesTheDeviceNick() {
        assertEquals(1, dao.findFilesFast(byModifiedBy("Jane Laptop")).size());
    }

    /** A value of nothing but punctuation leaves no word - it must not silently drop every file. */
    public void testPunctuationOnlyFiltersNothing() {
        assertEquals(2, dao.findFilesFast(byModifiedBy("!!!")).size());
        assertEquals(2, dao.findFilesFast(byDevice("---")).size());
    }

    private static FileInfo byEditor(String name, String displayName, String username, String deviceNick) {
        FolderInfo fo = FolderInfoFactory.newTopFolderForTest("TF-" + UUID.randomUUID(), "FOLDERID");
        MemberInfo member = new MemberInfo(deviceNick, "dev-" + username, "net");
        AccountInfo account = new AccountInfo("acc-" + username, username, displayName);
        return FileInfoFactory.unmarshallExistingFile(fo, name, null, 100, member, account, new Date(), 1,
                null, false, null);
    }

    private static FileInfoCriteria byModifiedBy(String editor) {
        FileInfoCriteria criteria = baseCriteria();
        criteria.setModifiedBy(editor);
        return criteria;
    }

    private static FileInfoCriteria byDevice(String deviceName) {
        FileInfoCriteria criteria = baseCriteria();
        criteria.setModifiedByDeviceName(deviceName);
        return criteria;
    }

    private static FileInfoCriteria baseCriteria() {
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addDomain(null);
        criteria.setRecursive(true);
        return criteria;
    }
}
