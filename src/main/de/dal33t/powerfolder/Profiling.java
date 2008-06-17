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

import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

/**
 * Class to monitor and log long-running method calls (only in Verbose mode).
 * Used for analysis and improvements to PowerFolder.
 * Ideally this should be used in a try / finally block,
 * so that the endProfiling is always called.
 * <BR/>
 * <PRE>
 *     log seq = getController().getProfiling().startProfiling(...);
 *     try {
 *         // Do stuff
 *     } finally {
 *         getController().getProfiling().endProfiling(seq);
 *     } 
 * </PRE>
 */
public class Profiling extends PFComponent {

    private final AtomicLong sequentialId = new AtomicLong();
    private final Map<Long, ProfilingEntry> entries =
            Collections.synchronizedMap(new HashMap<Long, ProfilingEntry>());
    private final List<ProfilingStat> stats =
            Collections.synchronizedList(new ArrayList<ProfilingStat>());

    private long totalTime;
    private long minimumTime;
    private long maximumTime;
    private long totalCount;

    public Profiling(Controller controller) {
        super(controller);
    }

    /**
     * Start profiling a method invocation.
     *
     * @param profileMillis
     *          number of milliseconds after which to log the event.
     * @param methodName
     *          the name of the method being invoked.
     * @param args
     *          method arguments.
     * @return
     *          unique sequential id.
     */
    public long startProfiling(int profileMillis, String methodName,
                               Object... args) {
        if (!ConfigurationEntry.VERBOSE.getValueBoolean(getController())) {
            return -1;
        }

        Long seq = sequentialId.getAndIncrement();

        entries.put(seq, new ProfilingEntry(profileMillis, methodName, args));
        return seq;
    }

    /**
     * End profiling a method invocation.
     * The 'Seq' arg MUST be the value returned by the coresponding
     * startProfiling call. If the invocation takes longer than the
     * original profileMillis milli seconds, the profile is logged.
     *
     * @param seq
     *          the sequential event number from startProfiling call.
     */
    public void endProfiling(Long seq) {

        if (!ConfigurationEntry.VERBOSE.getValueBoolean(getController())) {
            return;
        }

        ProfilingEntry profilingEntry = null;
        synchronized (entries) {
            Set<Long> keys = entries.keySet();
            for (Long key : keys) {
                if (key.equals(seq)) {
                    profilingEntry = entries.remove(key);
                    break;
                }
            }
        }

        if (profilingEntry != null) {
            // Delegate to a thread, so things do not get held up.
            ProfilingThread t = new ProfilingThread(profilingEntry);
            t.start();
        }
    }

    /**
     * Thread to log the event and add to stats.
     */
    private class ProfilingThread extends Thread {
        private ProfilingEntry profilingEntry;

        private ProfilingThread(ProfilingEntry profilingEntry) {
            this.profilingEntry = profilingEntry;
        }

        public void run() {
            long elapsed = profilingEntry.elapsedMilliseconds();
            if (elapsed >= profilingEntry.getProfileMillis()) {
                StringBuilder sb =
                        new StringBuilder(profilingEntry.getMethodName() + " [");
                int argsLength = profilingEntry.getArgs().length;
                for (int i = 0; i < argsLength; i++) {
                    sb.append(profilingEntry.getArgs()[i]);
                    if (i < argsLength - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("] took " + elapsed + " milliseconds");
                log().error(sb.toString());
            }

            totalTime += elapsed;
            totalCount ++;
            if (elapsed < minimumTime) {
                minimumTime = elapsed;
            }
            if (elapsed > maximumTime) {
                maximumTime = elapsed;
            }
            stats.add(new ProfilingStat(profilingEntry.getMethodName(), elapsed));
        }
    }

    public String dumpStats() {

        StringBuilder sb = new StringBuilder();

        sb.append("=== Profiling Statistics ===\n");
        sb.append("Total invocations: " + totalCount + '\n');
        sb.append("Total elapsed time: " + totalTime + "ms\n");
        if (totalCount > 0) {
            sb.append("Average elapsed time: " + totalTime / totalCount + "ms\n");
        }
        sb.append("Minimum elapsed time: " + minimumTime + "ms\n");
        sb.append("Maximum elapsed time: " + maximumTime + "ms\n");
        synchronized (stats) {
            Collections.sort(stats);
            long currentCount = 0;
            long currentTotal = 0;
            long currentMinimum = 0;
            long currentMaximum = 0;
            String currentKey = null;
            for (ProfilingStat stat : stats) {
                String key = stat.getMethodName();
                if (currentKey == null || !currentKey.equals(key)) {
                    if (currentKey != null) {
                        logStats(sb, currentKey, currentCount, currentTotal, currentMinimum, currentMaximum);
                    }
                    currentKey = key;
                    currentCount = 0;
                    currentTotal = 0;
                    currentMinimum = 0;
                    currentMaximum = 0;
                }
                currentCount++;
                long elapsed = stat.getElapsedTime();
                currentTotal += elapsed;
                if (elapsed < currentMinimum) {
                    currentMinimum = elapsed;
                }
                if (elapsed > currentMaximum) {
                    currentMaximum = elapsed;
                }
            }

            // Catch the last set...
            if (currentKey != null) {
                logStats(sb, currentKey, currentCount, currentTotal, currentMinimum, currentMaximum);
            }
        }
        sb.append("============================");
        return sb.toString();
    }

    private static void logStats(StringBuilder sb, String currentKey, long currentCount, long currentTotal, long currentMinimum, long currentMaximum) {
        sb.append('\n' + currentKey + '\n');
        sb.append("----------------------------\n");
        sb.append("Total invocations: " + currentCount + '\n');
        sb.append("Total elapsed time: " + currentTotal + "ms\n");
        if (currentCount > 0) {
            sb.append("Average elapsed time: " + currentTotal / currentCount + "ms\n");
        }
        sb.append("Minimum elapsed time: " + currentMinimum + "ms\n");
        sb.append("Maximum elapsed time: " + currentMaximum + "ms\n");
    }

    /**
     * Inner class describing the event being profiled.
     */
    private static class ProfilingEntry {

        private int profileMillis;
        private String methodName;
        private Object[] args;
        private long startTime;

        private ProfilingEntry(int profileMillis,
                               String methodName,
                               Object... args) {
            this.profileMillis = profileMillis;
            this.methodName = methodName;
            this.args = args;
            startTime = new Date().getTime();
        }

        public int getProfileMillis() {
            return profileMillis;
        }

        public String getMethodName() {
            return methodName;
        }

        public Object[] getArgs() {
            return args;
        }

        public long elapsedMilliseconds() {
            return new Date().getTime() - startTime;
        }
    }

    private class ProfilingStat implements Comparable<ProfilingStat> {
        private final String methodName;
        private final long elapsedTime;

        private ProfilingStat(String methodName, long elapsedTime) {
            this.methodName = methodName;
            this.elapsedTime = elapsedTime;
        }

        public String getMethodName() {
            return methodName;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        public int compareTo(ProfilingStat o) {
            return methodName.compareTo(o.methodName);
        }
    }
}
