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
import java.util.concurrent.TimeUnit;
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
            String out;
            try (InputStream in = p.getInputStream()) {
                out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                // Don't thrash if it hangs; the process is destroyed in finally.
                LOG.fine("command timed out: " + String.join(" ", command));
                return null;
            }
            return out;
        } catch (Exception e) {
            LOG.log(Level.FINE, "command failed: " + String.join(" ", command), e);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
                try { p.getOutputStream().close(); } catch (Exception ignore) { }
                try { p.getInputStream().close(); } catch (Exception ignore) { }
                try { p.getErrorStream().close(); } catch (Exception ignore) { }
            }
        }
    }
}
