package de.dal33t.powerfolder.test.encryptedStorage;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test for file encryption with cryptomator lib, cryptofs and PowerFolder.
 *
 * @author Jan Wiegmann <wiegmann@powerfolder.com>
 * @since <pre>Aug 23, 2016</pre>
 */

public class EncryptedStorageTest extends ControllerTestCase {

    @Before


    @Test
    public void testStartFolderRepository() {

        // Setup a test folder.
        setupTestFolder(SyncProfile.HOST_FILES);
    }

}