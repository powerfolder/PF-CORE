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
 *
 */
package de.dal33t.powerfolder.folder;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Path;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Visitor;
import de.dal33t.powerfolder.util.test.ConditionWithMessage;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

public class AtomicCommitTest extends TwoControllerTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        connectBartAndLisa();
        Path lisaDir = TESTFOLDER_BASEDIR_LISA.resolve(".temp-dir");
        Path bartDir = TESTFOLDER_BASEDIR_BART;
        joinTestFolder(bartDir, lisaDir, SyncProfile.MANUAL_SYNCHRONIZATION);
        getFolderAtLisa().setCommitDir(TESTFOLDER_BASEDIR_LISA);
    }

    @Test
    public void testCommit() throws IOException {
        final int nFiles = 20;
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());
        TestHelper.waitMilliSeconds(1000);

        MyVistor v = new MyVistor();
        getFolderAtLisa().visitIncomingFiles(v);
        assertEquals(nFiles, v.count);

        // System subdir
        assertEquals(1, getFolderAtLisa().getLocalBase().toFile().list().length);
        // TEMP subdir
        assertEquals(1, getFolderAtLisa().getCommitDir().toFile().list().length);
        // Create some garbage/oldstuff
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtLisa().getCommitDir());
        }

        // Start the transfer.
        getFolderAtLisa().setSyncProfile(SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getCommitDir().toFile().list().length == nFiles + 1;
            }

            public String message() {
                return "Files mismatch at lisas commit dir. Got: "
                    + getFolderAtLisa().getCommitDir().toFile().list().length
                    + ". Expected: " + (nFiles + 1);
            }
        });

        // Reset/New — retry delete for Windows file locks
        for (int retry = 0; retry < 5; retry++) {
            TestHelper.waitMilliSeconds(2000);
            try {
                PathUtils.recursiveDelete(getFolderAtBart().getLocalBase());
                break;
            } catch (Exception e) {
                if (retry == 4) throw e;
            }
        }
        for (int i = 0; i < nFiles; i++) {
            TestHelper.createRandomFile(getFolderAtBart().getLocalBase());
        }
        scanFolder(getFolderAtBart());

        TestHelper.waitForCondition(60, new ConditionWithMessage() {
            public boolean reached() {
                return getFolderAtLisa().getCommitDir().toFile().list().length == nFiles + 1;
            }

            public String message() {
                return "Files mismatch at lisas commit dir. Got: "
                    + getFolderAtLisa().getCommitDir().toFile().list().length
                    + ". Expected: " + (nFiles + 1);
            }
        });

    }

    private static class MyVistor implements Visitor<FileInfo> {
        private int count;

        public boolean visit(FileInfo object) {
            count++;
            return true;
        }

    }
}
