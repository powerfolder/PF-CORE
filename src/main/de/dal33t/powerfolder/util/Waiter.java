/* $Id: Waiter.java,v 1.7 2005/03/25 00:11:06 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

/**
 * Simple class for waiting some time.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.7 $
 */
public class Waiter {
    private long timeoutTime;
    private long waitTime;
    private boolean interrupted;

    /**
     * Initializes a new waiter which timesout in timeout ms
     * 
     * @param timeout
     *            ms to timeout
     */
    public Waiter(long timeout) {
        interrupted = false;
        waitTime = timeout;
        timeoutTime = System.currentTimeMillis() + timeout;
    }

    public long getTimoutTimeMS() {
        return waitTime;
    }

    /**
     * Answers if this waiter is timed-out
     * 
     * @return
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() > timeoutTime || interrupted;
    }

    /**
     * Waits a short time
     */
    public void waitABit() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException("Waiter was interrupted @ "
                + Thread.currentThread().getName(), e);
        }
    }
}
