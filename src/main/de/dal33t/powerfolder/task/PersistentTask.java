/*
 * Copyright 2004 - 2010 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
package de.dal33t.powerfolder.task;

import java.io.Serializable;
import java.util.Calendar;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Reject;

/**
 * This class represents a persistent task which PowerFolder should perform. The
 * task remains stored until remove() is called.
 *
 * @author Christian Sprajc
 * @author Dennis "Bytekeeper" Waldherr
 * @version $Revision$
 */
public abstract class PersistentTask implements Serializable {
    protected static final int DEFAULT_DAYS_TO_EXIPRE = 14;

    // For backward compatibility
    private static final long serialVersionUID = -2476895105703987123L;

    private transient PersistentTaskManager manager;

    private Calendar expires;

    public PersistentTask(int daysToExpire) {
        if (daysToExpire > 0) {
            expires = Calendar.getInstance();
            expires.add(Calendar.DAY_OF_MONTH, daysToExpire);
        }
    }

    /**
     * Schedules this task for execution
     *
     * @param controller
     * @return if succeeded
     */
    public boolean scheduleTask(Controller controller) {
        Reject.ifNull(controller, "Controller");
        if (!controller.getTaskManager().isStarted()) {
            Logger.getLogger(PersistentTask.class.getName()).warning(
                "Unable to schedule task. Task manager not started. " + this);
            return false;
        }
        controller.getTaskManager().scheduleTask(this);
        return true;
    }

    /**
     * @return if this task is expired.
     */
    protected boolean isExpired() {
        return expires != null
            && Calendar.getInstance().compareTo(expires) >= 0;
    }

    /**
     * @return the day/time this task expires. null if never expires
     */
    protected Calendar getExpires() {
        return expires;
    }

    /**
     * Called when a new Task has been created or if one has been loaded from
     * the task file.
     *
     * @param handler
     *            active task handler
     */
    public final void init(PersistentTaskManager handler) {
        this.manager = handler;
        initialize();
    }

    /**
     * Called when a new Task has been created or if one has been loaded from
     * the task file.
     */
    public abstract void initialize();

    /**
     * Called when PF shuts down, the task has been removed or if the
     * PersistentTaskManager is being shut down.
     */
    public void shutdown() {
    }

    /**
     * Returns the controller.
     *
     * @return the controller
     */
    protected final Controller getController() {
        return manager.getController();
    }

    /**
     * Called from the task or a external source to remove this task
     * permanently. Shortcut for Controller.getTaskManager().removeTask(this).
     */
    public void remove() {
        manager.removeTask(this);
    }
}
