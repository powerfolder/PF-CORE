/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.transfer;

import java.util.concurrent.Callable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.TransferPriorities;
import de.dal33t.powerfolder.transfer.TransferPriorities.TransferPriority;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

public class PriorityTransferTest extends TwoControllerTestCase {
    private interface Helper {
        void downloadNewestVersion(FileInfo fInfo, boolean automatic);
    };

    private class TestTransferManager extends TransferManager {
        private final Helper helper;

        public TestTransferManager(Controller controller, Helper helper) {
            super(controller);
            this.helper = helper;
        }

        @Override
        public DownloadManager downloadNewestVersion(FileInfo info,
            boolean automatic)
        {
            helper.downloadNewestVersion(info, automatic);
            return super.downloadNewestVersion(info, automatic);
        }
    }

    private Mockery mockery;

    private Helper helperLisa;
    private Helper helperBart;

    @Override
    protected void setUp() throws Exception {
        mockery = new Mockery();
        helperLisa = mockery.mock(Helper.class, "Helper Lisa");
        helperBart = mockery.mock(Helper.class, "Helper Bart");
        super.setUp();
    }

    @Override
    protected Controller createControllerLisa() {
        final Controller c = super.createControllerLisa();
        c.setTransferManagerFactory(new Callable<TransferManager>() {
            public TransferManager call() throws Exception {
                return new TestTransferManager(c, helperLisa);
            }
        });
        return c;
    }

    @Override
    protected Controller createControllerBart() {
        final Controller c = super.createControllerBart();
        c.setTransferManagerFactory(new Callable<TransferManager>() {
            public TransferManager call() throws Exception {
                return new TestTransferManager(c, helperBart);
            }
        });
        return c;
    }

    public void testPriorityRequests() {
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

        mockery.checking(new Expectations() {
            {
                Sequence dlSeq = mockery.sequence("Sequence of requests");
                one(helperBart).downloadNewestVersion(fInfos[1], true);
                inSequence(dlSeq);
                one(helperBart).downloadNewestVersion(fInfos[2], true);
                inSequence(dlSeq);
                one(helperBart).downloadNewestVersion(fInfos[0], true);
                inSequence(dlSeq);
            }
        });

        getFolderAtBart().setSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);

        // Wait a fixed amount of time so if something fails
        // above it'll be thrown by assertIsSatisfied below.
        TestHelper.waitMilliSeconds(2500);

        mockery.assertIsSatisfied();
    }
}
