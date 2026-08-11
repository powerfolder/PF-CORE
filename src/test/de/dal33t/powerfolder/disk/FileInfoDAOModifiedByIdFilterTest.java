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

public class FileInfoDAOModifiedByIdFilterTest extends FileInfoDAOTestCase {

    private FileInfoDAOHashMapImpl dao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dao = new FileInfoDAOHashMapImpl("ME", new DiskItemFilter());

        dao.store(null, byUser("a1.txt", "acc-alice", "dev-1"));
        dao.store(null, byUser("a2.txt", "acc-alice", "dev-1"));
        dao.store(null, byUser("b1.txt", "acc-bob", "dev-2"));
    }

    @Override
    protected void tearDown() throws Exception {
        dao.stop();
        super.tearDown();
    }

    public void testNoIdFilterReturnsAll() {
        assertEquals(3, dao.findFilesFast(crit(null, null)).size());
    }

    public void testAccountIdMatchesExactly() {
        assertEquals(2, dao.findFilesFast(crit("acc-alice", null)).size());
        assertEquals(1, dao.findFilesFast(crit("acc-bob", null)).size());
    }

    public void testUnknownAccountIdReturnsNothing() {
        assertEquals(0, dao.findFilesFast(crit("acc-none", null)).size());
    }

    public void testDeviceIdMatchesExactly() {
        assertEquals(1, dao.findFilesFast(crit(null, "dev-2")).size());
        assertEquals(2, dao.findFilesFast(crit(null, "dev-1")).size());
    }

    public void testAccountAndDeviceCombined() {
        assertEquals(2, dao.findFilesFast(crit("acc-alice", "dev-1")).size());
        assertEquals(0, dao.findFilesFast(crit("acc-alice", "dev-2")).size());
    }

    /** PFS-5653: device: matches the device name as a case-insensitive substring, unlike the exact ids. */
    public void testDeviceNameMatchesSubstringCaseInsensitively() {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setModifiedByDeviceName("NICK");
        assertEquals(3, dao.findFilesFast(c).size());

        c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setModifiedByDeviceName("ick");
        assertEquals(3, dao.findFilesFast(c).size());

        c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setModifiedByDeviceName("other-device");
        assertEquals(0, dao.findFilesFast(c).size());
    }

    public void testPartialIdIsNotAMatch() {
        assertEquals(0, dao.findFilesFast(crit("acc", null)).size());
    }

    public void testCriteriaHasSearchCriteriaWithId() {
        assertFalse(new FileInfoCriteria().hasSearchCriteria());
        FileInfoCriteria c = new FileInfoCriteria();
        c.setModifiedByAccountId("acc-alice");
        assertTrue(c.hasSearchCriteria());
    }

    private static FileInfo byUser(String name, String accountOid, String deviceId) {
        FolderInfo fo = FolderInfoFactory.newTopFolderForTest("TF-" + UUID.randomUUID(), "FOLDERID");
        MemberInfo m = new MemberInfo("nick", deviceId, "net");
        AccountInfo a = new AccountInfo(accountOid, accountOid + "@x");
        return FileInfoFactory.unmarshallExistingFile(fo, name, null, 100, m, a, new Date(), 1, null, false, null);
    }

    private static FileInfoCriteria crit(String accountId, String deviceId) {
        FileInfoCriteria c = new FileInfoCriteria();
        c.addDomain(null);
        c.setRecursive(true);
        c.setModifiedByAccountId(accountId);
        c.setModifiedByDeviceId(deviceId);
        return c;
    }
}
