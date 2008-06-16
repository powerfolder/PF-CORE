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
* $Id$
*/
package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.task.PersistentTaskManager;
import de.dal33t.powerfolder.util.task.SendMessageTask;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * Tests for the TaskManager and the possible tasks.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 * @version $revision$
 */
public class PersistentTaskManagerTestCase extends ControllerTestCase {
	public void testTaskManager() throws InterruptedException {
		PersistentTaskManager man = getController().getTaskManager();
		man.purgeAllTasks();
		assertFalse(man.hasTasks());
		assertEquals(man.activeTaskCount(), 0);
		
		setupTestFolder(SyncProfile.AUTOMATIC_DOWNLOAD);
		MemberInfo inf = new MemberInfo("Nobody", "0");
		getController().getNodeManager().addNode(inf);
		
		man.scheduleTask(new SendMessageTask(
				getFolder().createInvitation(), inf.id)); 

		man.shutdown();
		man.start();
		assertTrue(man.hasTasks());
		assertEquals(man.activeTaskCount(),1);
		
		man.purgeAllTasks();
		assertFalse(man.hasTasks());
		assertEquals(man.activeTaskCount(), 0);
		
		inf.getNode(getController()).setFriend(true, "");
		assertEquals(man.activeTaskCount(), 1);
		man.purgeAllTasks();
	}
}
