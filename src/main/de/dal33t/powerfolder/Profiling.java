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

import de.dal33t.powerfolder.util.Loggable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;

/**
 * Class to monitor and log long-running method calls.
 * Used for analysis and improvements to PowerFolder.
 */
public class Profiling extends Loggable {

    private final AtomicBoolean enabled = new AtomicBoolean();
    private final AtomicLong sequentialId = new AtomicLong();
    private final Map<Long, ProfilingEntry> entries = 
            Collections.synchronizedMap(new HashMap<Long, ProfilingEntry>());

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

        if (!enabled.get()) {
            return -1;
        }

        Long seq = sequentialId.getAndIncrement();

        entries.put(seq, new ProfilingEntry(profileMillis, methodName, args));
        return seq;
    }

    /**
     * End profiling a method invocation.
     * The 'Seq' arg must be the value returned by the coresponding
     * startProfiling call. If the invocation took longer than the
     * original profileMillis milli seconds, the profile is logged.
     *
     * @param seq
     *          the sequential event number from startProfiling call.
     */
    public void endProfiling(Long seq) {

        if (!enabled.get()) {
            return;
        }

        Set<Long> keys = entries.keySet();
        ProfilingEntry profilingEntry = null;
        synchronized (entries) {
            for (Long key : keys) {
                if (key.equals(seq)) {
                    profilingEntry = entries.remove(key);
                    break;
                }
            }
        }

        if (profilingEntry != null) {
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
        }
    }

    /**
     * Enable profiling.
     *
     * @param b
     */
    public void setEnabled(boolean b) {
        enabled.set(b);
    }

    /**
     * Whether profiling is enabled.
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled.get();
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
}
