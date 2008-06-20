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
* $Id:$
*/package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;

/**
 * Class to monitor and log long-running method calls (only in Verbose mode).
 * Used for analysis and improvements to PowerFolder.
 */
public class Profiling {

    private static volatile boolean enabled;

    private static long totalTime;
    private static long minimumTime;
    private static long maximumTime;
    private static long totalCount;
    private static final Logger log = Logger.getLogger(Profiling.class);

    /**
     * No instances allowed.
     */
    private Profiling() {
    }

    /**
     * Enables the profiler.
     *
     * @param enabled
     */
    public static void setEnabled(boolean enabled) {
        Profiling.enabled = enabled;
    }

    /**
     * Returns whether the profiler is active.
     *
     * @return
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Start profiling a method invocation.
     *
     * @param operationName
     *          the name of the method being invoked.
     * @return
     *          instance of ProfilingeEntry.
     */
    public static ProfilingEntry startProfiling(String operationName) {
        if (!enabled) {
            return null;
        }

        return new ProfilingEntry(operationName);
    }

    /**
     * End profiling a method invocation.
     * The 'ProfileEntry' arg should be the value returned by the coresponding
     * startProfiling call. If the invocation takes longer than the
     * original profileMillis milli seconds, the profile is logged.
     *
     * @param profilingEntry
     *          the profile entry instance.
     * @param profileMillis
     *          number of milliseconds after which to log the event.
     */
    public static void endProfiling(ProfilingEntry profilingEntry, int profileMillis) {

        if (!enabled) {
            return;
        }

        if (profilingEntry != null) {
            // Delegate to a thread, so things do not get held up.
            ProfilingThread t = new ProfilingThread(profilingEntry, profileMillis);
            t.start();
        }
    }

    /**
     * Thread to log the event and add to stats.
     */
    private static class ProfilingThread extends Thread {

        private ProfilingEntry profilingEntry;
        private long profileMillis;

        private ProfilingThread(ProfilingEntry profilingEntry,
                                long profileMillis) {
            this.profilingEntry = profilingEntry;
            this.profileMillis = profileMillis;
        }

        public void run() {
            long elapsed = profilingEntry.elapsedMilliseconds();
            if (elapsed >= profileMillis) {
                log.error(profilingEntry.getOperationName() +
                " took " + elapsed + " milliseconds");
            }

            totalTime += elapsed;
            totalCount ++;
            if (elapsed < minimumTime) {
                minimumTime = elapsed;
            }
            if (elapsed > maximumTime) {
                maximumTime = elapsed;
            }
        }
    }

    public static String dumpStats() {

        StringBuilder sb = new StringBuilder();

        sb.append("=== Profiling Statistics ===\n");
        sb.append("Total invocations: " + totalCount + '\n');
        sb.append("Total elapsed time: " + totalTime + "ms\n");
        if (totalCount > 0) {
            sb.append("Average elapsed time: " + totalTime / totalCount + "ms\n");
        }
        sb.append("Minimum elapsed time: " + minimumTime + "ms\n");
        sb.append("Maximum elapsed time: " + maximumTime + "ms\n");
        sb.append("============================");
        return sb.toString();
    }

}
