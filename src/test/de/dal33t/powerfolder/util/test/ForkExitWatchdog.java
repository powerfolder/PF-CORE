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
 *
 */
package de.dal33t.powerfolder.util.test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class ForkExitWatchdog implements TestExecutionListener {

    public static final String GRACE_SECONDS_PROPERTY = "powerfolder.test.fork.exit.grace.seconds";

    public static final int DEFAULT_GRACE_SECONDS = 120;

    public static final int SHUTDOWN_SETTLE_SECONDS = 5;

    private static final int EXIT_CODE_TEST_FAILURES = 2;

    private final AtomicInteger failures = new AtomicInteger();

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (identifier.isTest()
            && result.getStatus() == TestExecutionResult.Status.FAILED)
        {
            failures.incrementAndGet();
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        int graceSeconds = graceSeconds();
        if (graceSeconds <= 0) {
            return;
        }
        int exitCode = failures.get() > 0 ? EXIT_CODE_TEST_FAILURES : 0;
        Thread watchdog = new Thread(() -> awaitExitThenHalt(graceSeconds, exitCode),
            "fork-exit-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static boolean shutdownIsRunning() {
        try {
            java.lang.reflect.Field field = Class
                .forName("java.lang.ApplicationShutdownHooks")
                .getDeclaredField("hooks");
            field.setAccessible(true);
            return field.get(null) == null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String registeredHooks() {
        StringBuilder hooks = new StringBuilder();
        try {
            java.lang.reflect.Field field = Class.forName("java.lang.ApplicationShutdownHooks")
                .getDeclaredField("hooks");
            field.setAccessible(true);
            Map<?, ?> registered = (Map<?, ?>) field.get(null);
            if (registered == null) {
                return "Shutdown is already running: the JVM nulls"
                    + " ApplicationShutdownHooks.hooks before it runs them, so exit was"
                    + " requested and one of the hooks above has not returned. Look for a"
                    + " hook thread in the dump, not for a leaked thread.";
            }
            hooks.append("Exit has not been requested yet. Shutdown hooks registered: ")
                .append(registered.size());
            for (Object hook : registered.keySet()) {
                Thread thread = (Thread) hook;
                hooks.append(System.lineSeparator()).append("\t\"").append(thread.getName())
                    .append("\" ").append(thread.getClass().getName())
                    .append(' ').append(thread.getState());
            }
        } catch (Throwable t) {
            hooks.append("Shutdown hooks unreadable, ").append(t);
        }
        return hooks.toString();
    }

    private static int graceSeconds() {
        String configured = System.getProperty(GRACE_SECONDS_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_GRACE_SECONDS;
        }
        try {
            return Integer.parseInt(configured.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_GRACE_SECONDS;
        }
    }

    private static void awaitExitThenHalt(int graceSeconds, int exitCode) {
        long start = System.currentTimeMillis();
        long deadline = start + graceSeconds * 1000L;
        long wedgedDeadline = Long.MAX_VALUE;
        while (System.currentTimeMillis() < Math.min(deadline, wedgedDeadline)) {
            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                return;
            }
            if (wedgedDeadline == Long.MAX_VALUE && shutdownIsRunning()) {
                wedgedDeadline = System.currentTimeMillis()
                    + SHUTDOWN_SETTLE_SECONDS * 1000L;
            }
        }
        printThreadDump((int) ((System.currentTimeMillis() - start) / 1000L),
            wedgedDeadline != Long.MAX_VALUE);
        Runtime.getRuntime().halt(exitCode);
    }

    private static void printThreadDump(int waitedSeconds, boolean wedged) {
        StringBuilder dump = new StringBuilder();
        dump.append("This JVM has not exited ").append(waitedSeconds)
            .append(" seconds after the last test finished. All tests are reported;")
            .append(wedged
                ? " exit was requested and a shutdown hook has not returned, so waiting"
                    + " longer would not help."
                : " something registered by the product or a library is keeping the JVM"
                    + " alive.")
            .append(" Every thread of this JVM follows, then the JVM is halted.")
            .append(System.lineSeparator());
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces()
            .entrySet())
        {
            Thread thread = entry.getKey();
            dump.append(System.lineSeparator()).append('"').append(thread.getName())
                .append("\" ").append(thread.getState())
                .append(thread.isDaemon() ? " daemon" : " non-daemon")
                .append(System.lineSeparator());
            for (StackTraceElement frame : entry.getValue()) {
                dump.append("\tat ").append(frame).append(System.lineSeparator());
            }
        }
        dump.append(System.lineSeparator()).append(registeredHooks());
        System.err.println(dump);
        System.err.flush();
        System.out.flush();
    }
}
