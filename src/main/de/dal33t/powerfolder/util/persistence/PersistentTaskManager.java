package de.dal33t.powerfolder.util.persistence;

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
	
	public PersistentTaskManager(Controller controller) {
		super(controller);
	}

	private File getTaskFile() {
		String filename = getController().getConfigName() + ".tasks";
		return new File(Controller.getMiscFilesLocation(), 
				filename);
		
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void start() {
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
		for (final PersistentTask t: tasks) {
			getController().schedule(new TimerTask() {
				@Override
				public void run() {
					t.init(PersistentTaskManager.this);
				}
			}, 0);
		}
	}
	
	public synchronized void shutdown() {
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
		} catch (FileNotFoundException e) {
			log().error(e);
		} catch (IOException e) {
			log().error(e);
		}
	}
	
	public synchronized void scheduleTask(final PersistentTask task) {
		if (!tasks.contains(task)) {
			tasks.add(task);
			getController().schedule(
					new TimerTask() {
						@Override
						public void run() {
							task.init(PersistentTaskManager.this);
						}
					}, 0);
		}
	}
	
	public synchronized void removeTask(PersistentTask task) {
		task.shutdown();
		tasks.remove(task);
	}
}
