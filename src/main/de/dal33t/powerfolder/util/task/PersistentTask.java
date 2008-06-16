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
	 * Called when PF shuts down, the task has been removed or if the PersistentTaskManager 
	 * is being shut down. 
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
