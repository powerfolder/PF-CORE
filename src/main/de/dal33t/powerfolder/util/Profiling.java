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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to monitor and log long-running method calls (only in Verbose mode).
 * Used for analysis and improvements to PowerFolder.
 */
public class Profiling {
    private static final Logger LOG = Logger.getLogger(Profiling.class
        .getName());

    /**
     * Allow public access for faster check
     */
    public static boolean ENABLED;

    private static long totalTime;
    private static long minimumTime = Long.MAX_VALUE;
    private static long maximumTime;
    private static long totalCount;

    private static final Map<String, ProfilingStat> stats = new ConcurrentHashMap<String, ProfilingStat>();

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
     * @return instance of ProfilingeEntry.
     */
    public static ProfilingEntry start() {
        if (!ENABLED) {
            return null;
        }
        StackTraceElement[] st = new RuntimeException().getStackTrace();
        String opName = st[1].getClassName() + ':' + st[1].getMethodName();
        return new ProfilingEntry(opName, null);
    }

    /**
     * Start profiling a method invocation.
     *
     * @param operationName
     *            the name of the method being invoked.
     * @return instance of ProfilingeEntry.
     */
    public static ProfilingEntry start(String operationName) {
        if (!ENABLED) {
            return null;
        }
        return new ProfilingEntry(operationName, null);
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
            LOG.log(Level.SEVERE, "Cannot end profiling, entry is null",
                new RuntimeException("Cannot end profiling, entry is null"));
            return;
        }

        // Don't execute this asychronously. Might produce
        // uncontrollable # of thread.
        long elapsed = profilingEntry.elapsedMilliseconds();
        String operationName = profilingEntry.getOperationName();
        if (profileMillis > 0 && elapsed >= profileMillis) {
            // String t = profilingEntry.getOperationName();
            // if (profilingEntry.getDetails() != null) {
            // t += " [" + profilingEntry.getDetails() + "]";
            // }
            // t += " took " + elapsed + " milliseconds";
            // LOG.error(t);
        }
        totalTime += elapsed;
        totalCount++;
        if (elapsed < minimumTime) {
            minimumTime = elapsed;
        }
        if (elapsed > maximumTime) {
            maximumTime = elapsed;
        }

        synchronized (stats) {
            ProfilingStat stat = stats.get(operationName);
            if (stat != null) {
                stat.addElapsed(elapsed);
                return;
            }
            stat = new ProfilingStat(operationName, elapsed);
            stats.put(operationName, stat);
        }
    }

    public static String dumpStats() {
        if (!ENABLED) {
            LOG.severe("Unable to dump stats. Profiling is disabled");
            return "Unable to dump stats. Profiling is disabled";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("=== Profiling Statistics ===\n");
        sb.append("Total invocations: " + totalCount + '\n');
        sb.append("Total elapsed time: " + Format.formatTimeframe(totalTime)
            + "\n");
        if (totalCount > 0) {
            sb.append("Avg time: "
                + Format.formatTimeframe(totalTime / totalCount) + "\n");
        }
        sb.append("Min elapsed time: " + Format.formatTimeframe(minimumTime)
            + "\n");
        sb.append("Max elapsed time: " + Format.formatTimeframe(maximumTime)
            + "\n");
        sb.append("\n");
        List<String> keys = new ArrayList<String>(stats.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            ProfilingStat stat = stats.get(key);
            sb.append("'" + stat.getOperationName() + "' invocations "
                + stat.getCount() + " elapsed "
                + Format.formatTimeframe(stat.getElapsed()) + " average "
                + Format.formatTimeframe(stat.getElapsed() / stat.getCount())
                + "\n");
        }
        sb.append("============================");
        return sb.toString();
    }

    public static void reset() {
        stats.clear();
        maximumTime = 0;
        minimumTime = Long.MAX_VALUE;
        totalCount = 0;
        totalTime = 0;
    }

}
