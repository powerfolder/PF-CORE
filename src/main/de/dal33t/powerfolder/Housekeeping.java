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
    public void execute(JobExecutionContext jobExecutionContext)
        throws JobExecutionException
    {
        Controller controller = null;
        try {
            controller = (Controller) jobExecutionContext.getScheduler()
                .getContext().get("controller");
        } catch (SchedulerException e) {
            logWarning(
                "Could not perform housekeeping, could not access the controller. " +
                    e);
            return;
        }
        controller.performHousekeeping(true);
    }
}
