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
package de.dal33t.powerfolder.domain;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfoFactory;
import de.dal33t.powerfolder.util.Base58;
import de.dal33t.powerfolder.util.LoginUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-5980: password and token of a protected link, one verification per access check.
 */
public class FileLinkPasswordTest {

    private boolean argon2;
    private FileLink link;
    private FileInfo otherFolderFile;

    @BeforeEach
    public void setUp() {
        // The routing is independent of the hash algorithm - SHA-256 keeps the test fast
        argon2 = LoginUtil.isArgon2Enabled();
        LoginUtil.setUseArgon2(false);
        link = new FileLink(FileInfoFactory.lookupInstance(FolderInfoFactory.newTopFolder("Link"), "a.txt"), true);
        link.setPassword("secret");
        otherFolderFile = FileInfoFactory.lookupInstance(FolderInfoFactory.newTopFolder("Other"), "a.txt");
    }

    @AfterEach
    public void tearDown() {
        LoginUtil.setUseArgon2(argon2);
    }

    @Test
    public void testPasswordAndToken() {
        assertTrue(link.isCorrectPassword("secret"));
        assertFalse(link.isCorrectPassword("guess"));
        String token = link.getToken("secret");
        assertFalse(token.isEmpty());
        assertTrue(link.isCorrectToken(token));
        assertEquals("", link.getToken("guess"));
        assertFalse(link.isCorrectToken(Base58.encode((link.getId() + "guess").getBytes())));
        assertFalse(link.isCorrectToken(Base58.encode(("x" + link.getId() + "secret").getBytes())));
        assertFalse(link.isCorrectToken("0OIl"));
        assertFalse(link.isCorrectToken(null));
    }

    @Test
    public void testChangedPasswordRefusesOld() {
        String oldToken = link.getToken("secret");
        link.setPassword("changed");
        assertFalse(link.isCorrectPassword("secret"));
        assertFalse(link.isCorrectToken(oldToken));
        assertTrue(link.isCorrectPassword("changed"));
    }

    @Test
    public void testOneVerificationPerAccessCheck() throws Exception {
        Controller controller = Mockito.mock(Controller.class);
        // Other folder: the check ends after the password, which is all this test counts
        assertEquals(0, verificationsOf(() -> link.hasReadPermissions(controller, otherFolderFile, null)));
        assertEquals(0, verificationsOf(() -> link.hasReadPermissions(controller, otherFolderFile, " ")));
        assertEquals(1, verificationsOf(() -> link.hasReadPermissions(controller, otherFolderFile, "guess")));
        assertEquals(1, verificationsOf(() -> link.hasReadPermissions(controller, otherFolderFile, "0OIl")));
        String wrongToken = Base58.encode((link.getId() + "guess").getBytes());
        assertEquals(1, verificationsOf(() -> link.hasReadPermissions(controller, otherFolderFile, wrongToken)));
    }

    private static int verificationsOf(Runnable check) throws Exception {
        Field field = LoginUtil.class.getDeclaredField("VERIFICATIONS");
        field.setAccessible(true);
        AtomicInteger verifications = (AtomicInteger) field.get(null);
        int before = verifications.get();
        check.run();
        return verifications.get() - before;
    }
}
