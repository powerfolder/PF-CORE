package de.dal33t.powerfolder.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Timer that does not get canceled when any timer task throws an exception.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WrappingTimer extends Timer {
    private static final Logger LOG = Logger.getLogger(WrappingTimer.class);

    // Construction ***********************************************************

    public WrappingTimer() {
    }

    public WrappingTimer(boolean isDaemon) {
        super(isDaemon);
    }

    public WrappingTimer(String name) {
        super(name);
    }

    public WrappingTimer(String name, boolean isDaemon) {
        super(name, isDaemon);
    }

    // Overriding public API **************************************************

    @Override
    public void schedule(TimerTask task, Date time) {
        super.schedule(new WrappingTimerTask(task), time);
    }

    @Override
    public void schedule(TimerTask task, long delay, long period) {
        super.schedule(new WrappingTimerTask(task), delay, period);
    }

    @Override
    public void schedule(TimerTask task, long delay) {
        super.schedule(new WrappingTimerTask(task), delay);
    }

    @Override
    public void schedule(TimerTask task, Date firstTime, long period) {
        super.schedule(new WrappingTimerTask(task), firstTime, period);
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        super.schedule(new WrappingTimerTask(task), delay, period);
    }

    @Override
    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period)
    {
        super.schedule(new WrappingTimerTask(task), firstTime, period);
    }

    @Override
    public String toString() {
        return "WT (" + super.toString() + ")";
    }

    // Internal ***************************************************************

    /**
     * Basically wraps around a timer task and catches all exceptions that might
     * cancel the timer.
     */
    private static class WrappingTimerTask extends TimerTask {

        private TimerTask deligate;

        public WrappingTimerTask(TimerTask deligate) {
            super();
            Reject.ifNull(deligate, "Deligating timer task is null");
            this.deligate = deligate;
        }

        @Override
        public boolean cancel() {
            return deligate.cancel();
        }

        @Override
        public long scheduledExecutionTime() {
            return deligate.scheduledExecutionTime();
        }

        @Override
        public void run() {
            try {
                deligate.run();
            } catch (Throwable e) {
                LOG.error("Exception in timertask: " + deligate, e);
            }
        }
    }
}
