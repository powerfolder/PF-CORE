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
 * $Id: UploadsQuickInfoPanel.java 4504 2008-07-02 13:30:59Z harry $
 */
package de.dal33t.powerfolder.util.ui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;

/**
 * Helper class to perform UI updates delayed. If an UI update is scheduled
 * twice only thr last update is actually performed. This is useful if you want
 * to reduce the number of events in EDT thread.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class DelayedUpdater {

    private static final Logger LOG = Logger.getLogger(DelayedUpdater.class
        .getName());

    private long delay = 250L;
    private static Timer timer;
    private volatile TimerTask currentTask;

    /**
     * Constructs a delayed execution. Creates own timer lazily
     */
    public DelayedUpdater() {
    }

    /**
     * Constructs a delayed execution. Uses shared timer from Controller.
     * 
     * @param controller
     */
    public DelayedUpdater(Controller controller) {
        timer = controller.getTimer();
    }

    /**
     * Schedules a task to be performed in EDT thread after a given time. If a
     * former update has not been processed yet the old task gets canceled.
     * 
     * @param task
     */
    public synchronized void schedule(final Runnable task) {
        if (currentTask != null) {
            currentTask.cancel();
        }
        currentTask = new DelayedTimerTask(task);
        if (timer == null) {
            timer = new Timer();
        }
        try {
            timer.schedule(currentTask, delay);
        } catch (Exception e) {
            LOG.log(Level.FINER, "Unable to schedule task to timer: " + e, e);
        }
    }

    private final class DelayedTimerTask extends TimerTask {
        private final Runnable task;

        private DelayedTimerTask(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                UIUtil.invokeAndWaitInEDT(new Runnable() {
                    public void run() {
                        // Ready for new tasks
                        synchronized (DelayedUpdater.this) {
                            currentTask = null;
                        }
                        try {
                            task.run();
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE,
                                "Exception while executing delayed task: " + e,
                                e);
                        }
                    }
                });
            } catch (InterruptedException e) {
            }
        }
    }
}
