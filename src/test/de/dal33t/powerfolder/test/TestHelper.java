/* $Id$
 */
package de.dal33t.powerfolder.test;

import de.dal33t.powerfolder.util.Reject;

/**
 * Offers several helping methods for junit tests.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class TestHelper {

    private TestHelper() {

    }

    /**
     * Wraps <code>Thread.sleep()</code> and just try/catches the
     * InterruptedException
     * 
     * @param ms
     * @throws RuntimeException
     *             if InterruptedException occoured
     */
    public static void waitMilliSeconds(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Waits for the task to be completed or after timeout.
     * 
     * @param task
     *            the task to wait for
     * @return true when task succesfully completed. false when reached timeout
     */
    public static boolean waitForTask(int secondsTimeout, Task task) {
        Reject.ifNull(task, "Task is null");

        int i = 0;
        while (!task.completed()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            i++;
            if (i > secondsTimeout * 10) {
                return false;
            }
        }
        return true;
    }

    /**
     * General task, which has a completion state
     */
    public static interface Task {
        /**
         * @return if the task was successfuly completed
         */
        boolean completed();
    }
}
