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
 */
package de.dal33t.powerfolder.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to monitor and log long-running method calls (only in Verbose mode).
 * Used for analysis and improvements to PowerFolder.
 */
public class Profiling {
    private static final Logger LOG = Logger.getLogger(Profiling.class);

    /**
     * Allow public access for faster check
     */
    public static boolean ENABLED;

    private static long totalTime;
    private static long minimumTime;
    private static long maximumTime;
    private static long totalCount;

    private static final List<ProfilingStat> stats = new CopyOnWriteArrayList<ProfilingStat>();

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
        Profiling.ENABLED = enabled;
    }

    /**
     * @return whether the profiler is active.
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Start profiling a method invocation.
     * 
     * @param operationName
     *            the name of the method being invoked.
     * @param details
     *            method details
     * @return instance of ProfilingeEntry.
     */
    public static ProfilingEntry start(String operationName, String details) {
        if (!ENABLED) {
            return null;
        }
        return new ProfilingEntry(operationName, details);
    }

    /**
     * End profiling a method invocation. The 'ProfileEntry' arg should be the
     * value returned by the coresponding startProfiling call. If the invocation
     * takes longer than the original profileMillis milli seconds, the profile
     * is logged.
     * 
     * @param profilingEntry
     *            the profile entry instance.
     */
    public static void end(ProfilingEntry profilingEntry) {
        end(profilingEntry, -1);
    }

    /**
     * End profiling a method invocation. The 'ProfileEntry' arg should be the
     * value returned by the coresponding startProfiling call. If the invocation
     * takes longer than the original profileMillis milli seconds, the profile
     * is logged.
     * 
     * @param profilingEntry
     *            the profile entry instance.
     * @param profileMillis
     *            maximum number of milliseconds this event should take.
     *            Otherwise a error is logged.
     */
    public static void end(ProfilingEntry profilingEntry, int profileMillis) {
        if (!ENABLED) {
            return;
        }
        if (profilingEntry == null) {
            // This i
            LOG.error(new RuntimeException(
                "Cannot end profiling, entry is null"));
            return;
        }

        // Don't execute this asychronously. Might produce
        // uncontrollable # of thread.
        long elapsed = profilingEntry.elapsedMilliseconds();
        String operationName = profilingEntry.getOperationName();
        if (profileMillis > 0 && elapsed >= profileMillis) {
            LOG.error(profilingEntry.getOperationName() + " took " + elapsed
                + " milliseconds");
        }
        totalTime += elapsed;
        totalCount++;
        if (elapsed < minimumTime) {
            minimumTime = elapsed;
        }
        if (elapsed > maximumTime) {
            maximumTime = elapsed;
        }

        for (ProfilingStat profilingStat : stats) {
            if (profilingStat.getOperationName().equals(operationName)) {
                profilingStat.addElapsed(elapsed);
                return;
            }
        }
        ProfilingStat stat = new ProfilingStat(operationName, elapsed);
        stats.add(stat);
    }

    public static String dumpStats() {

        StringBuilder sb = new StringBuilder();

        sb.append("=== Profiling Statistics ===\n");
        sb.append("Total invocations: " + totalCount + '\n');
        sb.append("Total elapsed time: " + totalTime + "ms\n");
        if (totalCount > 0) {
            sb.append("Average elapsed time: " + totalTime / totalCount
                + "ms\n");
        }
        sb.append("Minimum elapsed time: " + minimumTime + "ms\n");
        sb.append("Maximum elapsed time: " + maximumTime + "ms\n");
        for (ProfilingStat stat : stats) {
            sb.append(stat.getOperationName() + " invocations "
                + stat.getCount() + " elapsed " + stat.getElapsed()
                + "ms average " + stat.getElapsed() / stat.getCount() + "ms\n");
        }
        sb.append("============================");
        return sb.toString();
    }

}
