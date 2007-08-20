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
		
		setupTestFolder(SyncProfile.AUTO_DOWNLOAD_FROM_ALL);
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
		
		inf.getNode(getController()).setFriend(true);
		assertEquals(man.activeTaskCount(), 1);
		man.purgeAllTasks();
	}
}
