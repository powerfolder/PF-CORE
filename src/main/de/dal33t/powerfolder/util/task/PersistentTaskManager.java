package de.dal33t.powerfolder.util.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * Loads, stores and initializes persistent Tasks.
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision$
 */
public class PersistentTaskManager extends PFComponent {
	private List<PersistentTask> tasks;
	/**
	 * Pending tasks that await initialization.
	 */
	private volatile int pendingTasks;
	private boolean shuttingDown = false;
	
	public PersistentTaskManager(Controller controller) {
		super(controller);
	}

	/**
	 * Returns the file which represents the persistent store of tasks.
	 * @return the tasklist-file
	 */
	private File getTaskFile() {
		String filename = getController().getConfigName() + ".tasks";
		return new File(Controller.getMiscFilesLocation(), 
				filename);
		
	}
	
	/**
	 * Starts this manager.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void start() {
		shuttingDown = false;
		File taskfile = getTaskFile();
		if (taskfile.exists()) {
			log().info("Loading taskfile");
			try {
				ObjectInputStream oin = new ObjectInputStream(
						new FileInputStream(taskfile));
				tasks = (List<PersistentTask>) oin.readObject();
				oin.close();
				log().info("Loaded " + tasks.size() + " tasks.");
			} catch (FileNotFoundException e) {
				log().error(e);
			} catch (IOException e) {
				log().error(e);
			} catch (ClassNotFoundException e) {
				log().error(e); 
			} catch (ClassCastException e) {
				log().error(e);
			}
		} else {
			log().info("No taskfile found - probably first start of PF.");
		}
		// If no taskfile was found or errors occured while loading it
		if (tasks == null) {
			tasks = new LinkedList<PersistentTask>();
		}
		for (PersistentTask t: tasks.toArray(new PersistentTask[0])) {
			t.init(this);
		}
	}
	
	/**
	 * Shuts down the manager.
	 * Saves all remaining tasks - they'll continue execution once the manager has been restarted (Not 
	 * neccesarrily in this session of PowerFolder)
	 */
	public synchronized void shutdown() {
		shuttingDown = true;
		waitForPendingTasks();
		for (PersistentTask t: tasks) {
			t.shutdown();
		}
		File taskFile = getTaskFile();
		try {
			log().info("There are " + tasks.size() + " tasks not completed yet.");
			ObjectOutputStream oout = new ObjectOutputStream(
					new FileOutputStream(taskFile));
			oout.writeUnshared(tasks);
			oout.close();
			tasks = null;
		} catch (FileNotFoundException e) {
			log().error(e);
		} catch (IOException e) {
			log().error(e);
		}
	}
	
	/**
	 * Schedules a new task.
	 * The given task will be started as soon as possible by the shared Timer of the Controller class.
	 * @param task the task to start
	 */
	public synchronized void scheduleTask(final PersistentTask task) {
		if (!tasks.contains(task) && !shuttingDown) {
			tasks.add(task);
			pendingTasks++;
			getController().schedule(
					new TimerTask() {
						@Override
						public void run() {
							task.init(PersistentTaskManager.this);
							pendingTasks--;
							synchronized (PersistentTaskManager.this) {
								PersistentTaskManager.this.notify();
							}
						}
					}, 0);
		}
	}
	
	/**
	 * Shuts down and removes a given task.
	 * This method will block until all tasks are properly initialized before removing the
	 * given task.
	 * @param task the task to remove
	 */
	public synchronized void removeTask(PersistentTask task) {
		shuttingDown = true;
		waitForPendingTasks();
		task.shutdown();
		tasks.remove(task);
		shuttingDown = false;
	}
	
	/**
	 * Removes all pending tasks.
	 * This is useful for tests or to clear all tasks in case some are errornous.
	 * This method will block until all tasks are properly initialized.
	 */
	public synchronized void purgeAllTasks() {
		shuttingDown = true;
		waitForPendingTasks();
		while (!tasks.isEmpty()) {
			tasks.remove(0).shutdown();
		}
		shuttingDown = false;
	}
	
	/**
	 * Returns if there are any pending tasks.
	 * @return true if there are 1 or more active tasks
	 */
	public synchronized boolean hasTasks() {
		return !tasks.isEmpty();
	}
	
	/**
	 * Returns the number of active tasks
	 * @return the active task count
	 */
	public synchronized int activeTaskCount() {
		return tasks.size();
	}

	/** Assumes the caller to have locked the manager */
	private void waitForPendingTasks() {
		while (pendingTasks > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				log().error(e);
			}
		}
	}
}
