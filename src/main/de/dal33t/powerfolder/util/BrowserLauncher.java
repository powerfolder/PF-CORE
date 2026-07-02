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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bare Bones Browser Launch
 * <p>
 * Version 1.5
 * <p>
 * December 10, 2005
 * <p>
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP
 * <p>
 * Example Usage: String url = "http:www.centerkey.com/";
 * BareBonesBrowserLaunch.openURL(url);
 * <p>
 * Public Domain Software -- Free to Use as You Like
 *
 * @version $Revision: 1.5 $
 */
public class BrowserLauncher {

    private static final Logger log = Logger.getLogger(BrowserLauncher.class
        .getName());

    private static final String errMsg = "Error attempting to launch web browser";

    private static final String[] LINUX_OPENERS = {
        "xdg-open", "gio", "kde-open6", "kde-open5", "kde-open"
    };

    private static final String[] FALLBACK_BROWSERS = {
        "firefox", "chromium", "google-chrome", "opera",
        "konqueror", "epiphany", "mozilla", "netscape"
    };

    /**
     * Environment variables dumped for diagnostics when launching on
     * Linux/Unix. These reveal the desktop environment, session type and the
     * PATH the child processes will inherit - the usual suspects when browser
     * launching fails on a specific distribution. See PFC-3494.
     */
    private static final String[] DIAG_ENV_VARS = {
        "PATH", "XDG_CURRENT_DESKTOP", "XDG_SESSION_TYPE", "DESKTOP_SESSION",
        "DISPLAY", "WAYLAND_DISPLAY", "DBUS_SESSION_BUS_ADDRESS", "BROWSER",
        "HOME"
    };

    /**
     * Opens the browser in background thread. This method does not BLOCK. Can
     * safely be used from UI-EDT Thread.
     *
     * @param controller
     * @param url
     */
    public static void openURL(Controller controller, final String url) {
        open(controller, new URLProducer() {
            @Override
            public String url() {
                return url;
            }
        });
    }

    /**
     * Opens the browser in background thread. This method does not BLOCK. Can
     * safely be used from UI-EDT Thread.
     *
     * @param controller
     * @param producer
     */
    public static void open(Controller controller, final URLProducer producer) {
        Reject.ifNull(producer, "producer");
        // PFC-2349 : Don't freeze UI
        if (controller != null && controller.getIOProvider() != null) {
            controller.getIOProvider().startIO(new Runnable() {
                public void run() {
                    try {
                        BrowserLauncher.openURL(producer.url());
                    } catch (IOException e) {
                        log.log(Level.WARNING,
                            "Unable to open web browser (async).", e);
                    }
                }
            });
        } else {
            // Fallback
            try {
                openURL(producer.url());
            } catch (IOException e) {
                log.log(Level.WARNING,
                    "Unable to open web browser (sync fallback).", e);
            }
        }
    }

    /**
     * Opens the given URL in the system browser. Method does BLOCK. Never call
     * directly from User Interface code! Use
     * {@link #open(Controller, URLProducer)} instead
     *
     * @param url
     * @throws IOException
     * @Deprecated favor {@link #openURL(Controller, String)} or
     *             {@link #open(Controller, URLProducer)}
     */
    public static void openURL(String url) throws IOException {
        if (StringUtils.isBlank(url)) {
            log.warning("Not opening blank url!");
            return;
        }

        log.info("openURL requested for: " + url);

        // Prefer Desktop if it works
        if (java6impl(url)) {
            return;
        }

        try {
            if (OSUtil.isMacOS()) {
                log.info("Opening URL via macOS com.apple.eio.FileManager");
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[]{String.class});
                openURL.invoke(null, url);
                log.info("macOS FileManager.openURL invoked");
            } else if (OSUtil.isWindowsSystem()) {
                log.info("Opening URL via Windows rundll32");
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                log.info("Windows rundll32 launched");
            } else {
                log.info("Opening URL via Linux/Unix native openers");
                openURLOnLinux(url);
            }
        } catch (Exception e) {
            log.log(Level.WARNING,
                "All browser-launch strategies failed for URL: " + url, e);
            throw (IOException) new IOException(errMsg).initCause(e);
        }
    }

    private static void openURLOnLinux(String url) throws Exception {
        logLinuxEnvironment();

        // Dedicated URL openers first: wait for them, a 0 exit means success.
        for (String opener : LINUX_OPENERS) {
            String[] args = "gio".equals(opener)
                ? new String[]{"open", url}
                : new String[]{url};
            log.info("Trying URL opener: " + opener);
            if (launch(opener, args, true)) {
                log.info("Successfully opened URL using opener: " + opener);
                return;
            }
            log.info("URL opener not usable, trying next: " + opener);
        }

        // Then known browsers: fire and forget, a successful spawn is enough.
        for (String browser : FALLBACK_BROWSERS) {
            log.info("Trying browser: " + browser);
            if (launch(browser, new String[]{url}, false)) {
                log.info("Successfully launched browser: " + browser);
                return;
            }
            log.info("Browser not usable, trying next: " + browser);
        }

        throw new Exception("Could not find web browser (no URL opener and no"
            + " known browser could be launched). Tried openers "
            + Arrays.toString(LINUX_OPENERS) + " and browsers "
            + Arrays.toString(FALLBACK_BROWSERS));
    }

    /**
     * Logs the desktop/session environment relevant to browser launching, so a
     * failing customer setup can be diagnosed from the log alone. See
     * PFC-3494.
     */
    private static void logLinuxEnvironment() {
        try {
            StringBuilder sb = new StringBuilder(
                "Linux browser-launch environment:");
            sb.append("\n  os.name           = ").append(System.getProperty("os.name"));
            sb.append("\n  os.version        = ").append(System.getProperty("os.version"));
            sb.append("\n  user.name         = ").append(System.getProperty("user.name"));
            sb.append("\n  java.version      = ").append(System.getProperty("java.version"));
            sb.append("\n  java.awt.headless = ").append(System.getProperty("java.awt.headless"));
            for (String var : DIAG_ENV_VARS) {
                sb.append("\n  $").append(var).append(" = ").append(System.getenv(var));
            }
            log.info(sb.toString());
        } catch (Exception e) {
            log.log(Level.FINE, "Could not gather Linux environment diagnostics", e);
        }
    }

    /**
     * Tries to launch {@code command} with {@code args}, resolving the
     * executable both via the inherited PATH and via the common bin
     * directories. Deliberately does NOT depend on the external {@code which}
     * command: it is not installed by default on some distributions (e.g.
     * openSUSE Tumbleweed), and a missing/incomplete PATH in the child
     * environment must not defeat the fallback either. See PFC-3494.
     *
     * @param waitForExit if {@code true}, block and treat only exit code 0 as
     *                    success; if {@code false}, a successful spawn is
     *                    treated as success.
     * @return {@code true} if the command was launched (and, when
     *         {@code waitForExit}, exited with code 0).
     */
    private static boolean launch(String command, String[] args, boolean waitForExit) {
        String[] candidates = resolveExecutables(command);
        for (String bin : candidates) {
            String[] cmd = new String[args.length + 1];
            cmd[0] = bin;
            System.arraycopy(args, 0, cmd, 1, args.length);
            log.fine("Attempting to launch: " + String.join(" ", cmd));
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                if (!waitForExit) {
                    // Fire and forget: discard output so a chatty child process
                    // cannot block on a full pipe.
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    pb.start();
                    log.info("Launched (fire-and-forget): " + bin);
                    return true;
                }
                Process p = pb.start();
                String output = readOutput(p);
                int exit = p.waitFor();
                if (exit == 0) {
                    log.info("Command succeeded (exit 0): " + bin
                        + (output.isEmpty() ? "" : " | output: " + output));
                    return true;
                }
                log.info("Command exited with code " + exit + ": " + bin
                    + (output.isEmpty() ? "" : " | output: " + output)
                    + " -- trying next candidate");
            } catch (IOException e) {
                // Executable not found at this location / cannot run: try next.
                log.fine("Could not launch " + bin + ": " + e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.log(Level.WARNING, "Interrupted while waiting for " + bin, e);
                return false;
            }
        }
        log.fine("No runnable executable found for command '" + command
            + "' (tried " + Arrays.toString(candidates) + ")");
        return false;
    }

    private static String[] resolveExecutables(String command) {
        // Bare name (resolved by the OS via PATH) plus absolute fallbacks, so a
        // missing or incomplete PATH in the child environment cannot break us.
        return new String[]{
            command,
            "/usr/bin/" + command,
            "/bin/" + command,
            "/usr/local/bin/" + command
        };
    }

    /**
     * Fully drains the (error-merged) output stream of the process and returns
     * it as a single line for logging. Draining also prevents the child from
     * blocking on a full pipe.
     */
    private static String readOutput(Process p) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(line);
            }
        } catch (IOException e) {
            log.finer("Could not read process output: " + e);
        }
        return sb.toString();
    }

    private static boolean java6impl(String url) {
        log.info("Attempting java.awt.Desktop.browse() for: " + url);
        try {
            boolean desktopSupported = Desktop.isDesktopSupported();
            log.info("Desktop.isDesktopSupported() = " + desktopSupported
                + " (java.awt.headless=" + System.getProperty("java.awt.headless") + ")");
            if (desktopSupported) {
                Desktop desktop = Desktop.getDesktop();
                boolean browseSupported = desktop.isSupported(Desktop.Action.BROWSE);
                log.info("Desktop.Action.BROWSE supported = " + browseSupported);
                if (browseSupported) {
                    log.info("Using Desktop.browse()");
                    desktop.browse(new URI(url));
                    log.info("Desktop.browse() returned without error");
                    return true;
                }
            }
            log.info("Desktop.browse() not usable on this platform,"
                + " falling back to native openers");
        } catch (LinkageError err) {
            log.log(Level.WARNING,
                "Desktop.browse() LinkageError, falling back to native openers", err);
        } catch (RuntimeException re) {
            // Covers HeadlessException, UnsupportedOperationException and other
            // runtime failures seen on some Linux/KDE/CI environments.
            log.log(Level.WARNING,
                "Desktop.browse() runtime failure, falling back to native openers", re);
        } catch (URISyntaxException | IOException e) {
            log.log(Level.WARNING,
                "Desktop.browse() failed, falling back to native openers", e);
        }
        return false;
    }

    public static interface URLProducer {
        String url();
    }
}
