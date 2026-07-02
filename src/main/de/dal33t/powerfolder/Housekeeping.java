/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.logging.Loggable;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 * Daily housekeeping. HAS TO BE A NON-INTERNAL CLASS AND PUBLIC!
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class Housekeeping extends Loggable implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        Controller controller;
        try {
            controller = (Controller) jobExecutionContext.getScheduler()
                .getContext().get("controller");
            controller.performHousekeeping(true);
        } catch (SchedulerException e) {
            logWarning(
                "Could not perform housekeeping, could not access the controller. " +
                    e);
        }
    }
}
