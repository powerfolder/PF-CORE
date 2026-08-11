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
 * PFS-5653: this DAO has no extracted document metadata, so <code>title:</code> falls back to the file name
 * and <code>author:</code> to the account that changed the file last, instead of returning nothing.
 */
public class FileInfoDAOTitleAuthorFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, byUser("Annual Earnings.pdf", "Jane Doe", "jane@x"));
        dao.store(null, byUser("Board Minutes.docx", "Richard Roe", "roe@x"));
        dao.store(null, byUser("earnings-draft.txt", "Jane Doe", "jane@x"));
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testTitleFallsBackToFileName() {
        assertEquals(2, dao.findFilesFast(crit("earnings", null)).size());
        assertEquals(1, dao.findFilesFast(crit("minutes", null)).size());
    }

    public void testTitleIsCaseInsensitive() {
        assertEquals(2, dao.findFilesFast(crit("EARNINGS", null)).size());
    }

    public void testUnknownTitleReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit("balance", null)).size());
    }

    public void testAuthorMatchesLastChangingAccount() {
        assertEquals(2, dao.findFilesFast(crit(null, "jane")).size());
        assertEquals(1, dao.findFilesFast(crit(null, "roe")).size());
    }

    public void testAuthorMatchesDisplayNameAndUsername() {
        assertEquals(1, dao.findFilesFast(crit(null, "Richard Roe")).size());
        assertEquals(1, dao.findFilesFast(crit(null, "roe@x")).size());
    }

    public void testUnknownAuthorReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit(null, "nobody")).size());
    }

    public void testTitleAndAuthorCombine() {
        assertEquals(2, dao.findFilesFast(crit("earnings", "jane")).size());
        assertEquals(0, dao.findFilesFast(crit("earnings", "roe")).size());
    }

    /** The regression this replaces: title/author used to short-circuit the whole scan to an empty result. */
    public void testTitleDoesNotSuppressUnrelatedCriteria() {
        FileInfoCriteria c = crit("earnings", null);
        c.addKeyWord("draft");
        assertEquals(1, dao.findFilesFast(c).size());
    }

    private static FileInfo byUser(String name, String displayName, String username) {
        FolderInfo fo = FolderInfoFactory.newTopFolderForTest("TF-" + UUID.randomUUID(), "FOLDERID");
        MemberInfo m = new MemberInfo("nick", "dev-1", "net");
        AccountInfo a = new AccountInfo("acc-" + username, username, displayName);
        return FileInfoFactory.unmarshallExistingFile(fo, name, null, 100, m, a, new Date(), 1, null, false, null);
    }

    private static FileInfoCriteria crit(String title, String author) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setTitle(title);
        c.setAuthor(author);
        return c;
    }
}
