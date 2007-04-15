package de.dal33t.powerfolder.util.task;

import java.io.Serializable;

import de.dal33t.powerfolder.Controller;

/**
 * This class represents a persistent task which PowerFolder should perform.
 * The task remains stored until remove() is called.  
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision$
 */
public abstract class PersistentTask implements Serializable {
	private transient PersistentTaskManager manager;
	
	/**
	 * Called when a new Task has been created or if one has been loaded from the task file.
	 * @param handler active task handler
	 */
	public void init(PersistentTaskManager handler) {
		this.manager = handler;
	}
	
	/**
	 * Called when PF shuts down or the task has been removed.
	 */
	public void shutdown() {
	}

	/**
	 * Returns the controller.
	 * @return the controller
	 */
	protected final Controller getController() {
		return manager.getController();
	}
	
	/**
	 * Called from the task or a external source to remove this task permanently.
	 * Shortcut for Controller.getTaskManager().removeTask(this).
	 */
	public final void remove() {
		manager.removeTask(this);
	}
}
