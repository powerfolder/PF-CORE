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
package de.dal33t.powerfolder.transfer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferPriorities.TransferPriority;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;
import static org.junit.jupiter.api.Assertions.*;

public class PriorityTransferTest extends TwoControllerTestCase {

    private final List<String> downloadSequence = Collections.synchronizedList(new ArrayList<>());
    private final Set<String> targetFiles = Collections.synchronizedSet(new HashSet<>());
    private CountDownLatch allDownloaded;

    private class TestTransferManager extends TransferManager {
        public TestTransferManager(Controller controller) {
            super(controller);
        }

        @Override
        public DownloadManager downloadNewestVersion(FileInfo info,
            boolean automatic)
        {
            String name = info.getRelativeName();
            if (automatic && targetFiles.contains(name) && !downloadSequence.contains(name)) {
                downloadSequence.add(name);
                allDownloaded.countDown();
            }
            return super.downloadNewestVersion(info, automatic);
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Controller createControllerBart() {
        final Controller c = super.createControllerBart();
        c.setTransferManagerFactory(new Callable<TransferManager>() {
            public TransferManager call() throws Exception {
                return new TestTransferManager(c);
            }
        });
        return c;
    }

    @Test
    public void testPriorityRequests() throws Exception {
        connectBartAndLisa();
        joinTestFolder(SyncProfile.HOST_FILES);
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        TestHelper.createRandomFile(getFolderAtLisa().getLocalBase());
        scanFolder(getFolderAtLisa());
        final FileInfo[] fInfos = getFolderAtLisa().getKnownFiles().toArray(
            new FileInfo[0]);
        assertEquals(3, fInfos.length);

        TransferPriorities prio = getFolderAtBart().getTransferPriorities();
        prio.setPriority(fInfos[0], TransferPriority.LOW);
        prio.setPriority(fInfos[1], TransferPriority.HIGH);

        assertTrue(prio.getComparator().compare(fInfos[0], fInfos[1]) > 0);

        targetFiles.add(fInfos[0].getRelativeName());
        targetFiles.add(fInfos[1].getRelativeName());
        targetFiles.add(fInfos[2].getRelativeName());
        allDownloaded = new CountDownLatch(3);
        downloadSequence.clear();

        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        assertTrue(allDownloaded.await(30, TimeUnit.SECONDS),
            "Not all files downloaded within 30s. Got: " + downloadSequence);

        assertEquals(fInfos[1].getRelativeName(), downloadSequence.get(0),
            "HIGH priority file should be downloaded first");
        assertEquals(fInfos[2].getRelativeName(), downloadSequence.get(1),
            "NORMAL priority file should be downloaded second");
        assertEquals(fInfos[0].getRelativeName(), downloadSequence.get(2),
            "LOW priority file should be downloaded last");
    }
}
