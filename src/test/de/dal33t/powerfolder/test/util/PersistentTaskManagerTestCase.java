/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
package de.dal33t.powerfolder.test.util;

import org.jmock.Expectations;
import org.jmock.Mockery;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.InvitationHandler;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.task.PersistentTaskManager;
import de.dal33t.powerfolder.task.SendMessageTask;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.test.Condition;
import de.dal33t.powerfolder.util.test.TestHelper;
import de.dal33t.powerfolder.util.test.TwoControllerTestCase;

/**
 * Tests for the TaskManager and the possible tasks.
 *
 * @author Dennis "Bytekeeper" Waldherr
 * @version $revision$
 */
public class PersistentTaskManagerTestCase extends TwoControllerTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        connectBartAndLisa();

        joinTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
    }

    public void testTaskManager() throws InterruptedException {
        PersistentTaskManager man = getContollerBart().getTaskManager();
        man.purgeAllTasks();
        assertFalse(man.hasTasks());
        assertEquals(man.activeTaskCount(), 0);

        MemberInfo inf = new MemberInfo("Nobody", "0", null);
        getContollerBart().getNodeManager().addNode(inf);

        man.scheduleTask(new SendMessageTask(getFolderAtBart()
            .createInvitation(), inf.id));

        man.shutdown();
        man.start();
        assertTrue(man.hasTasks());
        assertEquals(man.activeTaskCount(), 1);

        man.purgeAllTasks();
        assertFalse(man.hasTasks());
        assertEquals(man.activeTaskCount(), 0);

        inf.getNode(getContollerBart(), false).setFriend(true, "");
        assertEquals(man.activeTaskCount(), 1);
        man.purgeAllTasks();
    }

    public void testOfflineInvitation() {
        Member lisaAtBart = getContollerBart().getNodeManager()
            .getConnectedNodes().iterator().next();

        disconnectBartAndLisa();
        final int bartInitialTasks = getContollerBart().getTaskManager().activeTaskCount();

        getContollerLisa().getFolderRepository().removeFolder(
            getFolderAtLisa(), true);
        Invitation inv = new Invitation(getFolderAtBart().getInfo(),
            getContollerBart().getMySelf().getInfo());
        InvitationUtil.invitationToNode(getContollerBart(), inv, lisaAtBart);
        TestHelper.waitMilliSeconds(2500);
        // Should have one more task now.
        assertEquals(1 + bartInitialTasks, getContollerBart().getTaskManager().activeTaskCount());

        Mockery mock = new Mockery();
        final InvitationHandler handler = mock
            .mock(InvitationHandler.class);
        mock.checking(new Expectations() {
            {
                one(handler).gotInvitation(
                    with(any(Invitation.class)));
            }
        });
        getContollerLisa().addInvitationHandler(handler);
        connectBartAndLisa();
        TestHelper.waitForCondition(30, new Condition() {
            public boolean reached() {
                // Additional task should now be gone.
                return getContollerBart().getTaskManager().activeTaskCount() == bartInitialTasks;
            }
        });

        TestHelper.waitMilliSeconds(2500);
        mock.assertIsSatisfied();
    }
}
