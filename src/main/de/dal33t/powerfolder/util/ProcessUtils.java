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
package de.dal33t.powerfolder.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PFC-3643: run a short-lived external command and capture its output. The single
 * leak-free implementation shared by the macOS Finder integration
 * ({@code jni.osx.Util}) and the OS dark-mode / theme detection
 * ({@code ui.LookAndFeelSupport}), which each carried their own copy before. The
 * output (stdout+stderr) is fully drained and the process plus every stream is
 * always closed/destroyed, even on timeout: an earlier copy leaked the process and
 * file descriptors on every poll, which exhausted the JVM after ~20 min and froze
 * the UI.
 */
public class ProcessUtils {

    private static final Logger LOG = Logger.getLogger(ProcessUtils.class.getName());

    private ProcessUtils() {
    }

    /**
     * Run {@code command} and return its combined stdout+stderr, or {@code null} on
     * error or timeout (never throws). The exit value is not inspected - callers
     * that only need the output do not care, and the few that do can parse it.
     * The output is drained on a separate daemon thread, so the timeout also bounds a
     * child that keeps its output open; on timeout the child is destroyed forcibly.
     *
     * @param timeoutSeconds how long to wait before giving up and destroying the
     *                       process.
     * @param command        the command and its arguments (an argument array, so no
     *                       shell is involved and there is nothing to inject).
     */
    public static String runBounded(long timeoutSeconds, String... command) {
        Process p = null;
        try {
            p = new ProcessBuilder(command).redirectErrorStream(true).start();
            final InputStream in = p.getInputStream();
            CompletableFuture<String> output = new CompletableFuture<>();
            Thread drainer = new Thread(() -> {
                try {
                    output.complete(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                } catch (Throwable t) {
                    output.completeExceptionally(t);
                }
            }, "process-output-" + command[0]);
            drainer.setDaemon(true);
            drainer.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                LOG.fine(String.join(" ", command) + ": timed out after " + timeoutSeconds + "s");
                return null;
            }
            // The process exited; a grandchild may still hold the pipe, so bound the read too.
            long remainingNanos = Math.max(TimeUnit.MILLISECONDS.toNanos(100), deadline - System.nanoTime());
            return output.get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (TimeoutException e) {
            LOG.fine(String.join(" ", command) + ": output still open after exit");
            return null;
        } catch (Exception e) {
            LOG.log(Level.FINE, String.join(" ", command) + ": failed", e);
            return null;
        } finally {
            if (p != null) {
                if (p.isAlive()) {
                    p.destroyForcibly();
                }
                try { p.getOutputStream().close(); } catch (Exception ignore) { }
                try { p.getInputStream().close(); } catch (Exception ignore) { }
                try { p.getErrorStream().close(); } catch (Exception ignore) { }
            }
        }
    }
}
