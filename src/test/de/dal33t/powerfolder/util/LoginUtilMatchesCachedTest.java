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
package de.dal33t.powerfolder.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PFS-5980: a correct password is verified once, then remembered; a wrong one is verified every time.
 */
public class LoginUtilMatchesCachedTest {

    private boolean argon2;

    @BeforeEach
    public void setUp() {
        argon2 = LoginUtil.isArgon2Enabled();
        LoginUtil.clearVerifiedPasswords();
    }

    @AfterEach
    public void tearDown() {
        LoginUtil.setUseArgon2(argon2);
        LoginUtil.clearVerifiedPasswords();
    }

    @Test
    public void testCorrectPasswordVerifiedOnce() {
        String hash = LoginUtil.hashAndSalt("secret");
        int before = LoginUtil.VERIFICATIONS.get();
        assertTrue(LoginUtil.matchesCached("secret", hash));
        assertTrue(LoginUtil.matchesCached("secret", hash));
        assertTrue(LoginUtil.matchesCached("secret", hash));
        assertEquals(1, LoginUtil.VERIFICATIONS.get() - before);
    }

    @Test
    public void testWrongPasswordNeverRemembered() {
        String hash = LoginUtil.hashAndSalt("secret");
        int before = LoginUtil.VERIFICATIONS.get();
        assertFalse(LoginUtil.matchesCached("guess", hash));
        assertFalse(LoginUtil.matchesCached("guess", hash));
        assertEquals(2, LoginUtil.VERIFICATIONS.get() - before);
    }

    @Test
    public void testChangedHashRefusesOldPassword() {
        assertTrue(LoginUtil.matchesCached("old", LoginUtil.hashAndSalt("old")));
        assertFalse(LoginUtil.matchesCached("old", LoginUtil.hashAndSalt("new")));
    }

    @Test
    public void testNullAndBlank() {
        String hash = LoginUtil.hashAndSalt("secret");
        assertFalse(LoginUtil.matchesCached(null, hash));
        assertFalse(LoginUtil.matchesCached("secret", null));
        assertFalse(LoginUtil.matchesCached("secret", ""));
    }

    @Test
    public void testParallelChecksShareOneVerification() throws Exception {
        LoginUtil.setUseArgon2(true);
        String hash = LoginUtil.hashAndSalt("secret");
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            int before = LoginUtil.VERIFICATIONS.get();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return LoginUtil.matchesCached("secret", hash);
                }));
            }
            start.countDown();
            for (Future<Boolean> result : results) {
                assertTrue(result.get());
            }
            assertEquals(1, LoginUtil.VERIFICATIONS.get() - before);
        } finally {
            executor.shutdownNow();
        }
    }
}
