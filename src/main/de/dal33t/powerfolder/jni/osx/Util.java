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
 */
package de.dal33t.powerfolder.jni.osx;

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * macOS integration helpers.
 *
 * PFI-93: these used to be JNI bindings backed by the Intel-only
 * {@code libosxnative.jnilib}, which could not load on an arm64 (Apple Silicon)
 * JVM. They are now implemented in pure Java (no native library) so they work
 * natively on both Intel and Apple Silicon:
 *
 * <ul>
 *   <li>Login item (start at login) via {@code osascript} / "System Events".</li>
 *   <li>{@link #isOnLocalVolume(String)} via {@link java.nio.file.FileStore}.</li>
 *   <li>Finder sidebar favorites via the optional {@code mysides} tool; if it is
 *       not installed the calls are logged and skipped.</li>
 * </ul>
 *
 * PERFORMANCE: {@link #isOnLocalVolume(String)} sits on a hot path
 * (PathUtils / FolderRepository call it per path) and the login-item / favorites
 * helpers shell out to {@code osascript} / {@code mysides}. To avoid stalling the
 * UI or slowing scans, results are memoized and every external command runs with
 * a short bounded timeout and never throws. On the previous Intel-only build the
 * JNI lib failed to load on Apple Silicon and these calls were skipped entirely;
 * here they must be just as cheap.
 *
 * The public API (static method signatures and {@link #loaded}) is unchanged, so
 * existing callers keep working. {@link #loaded} is {@code true} because these no
 * longer depend on a native library being present.
 */
public class Util {

    public static Logger LOG = Logger.getLogger(Util.class.getName());

    /**
     * Kept for API compatibility. Always {@code true}: the helpers below are pure
     * Java and no longer require {@code libosxnative.jnilib} to be loaded.
     */
    public static boolean loaded = true;

    /** Bounded so a hung osascript/mysides can never wedge a caller. */
    private static final long CMD_TIMEOUT_SECONDS = 5;

    /** Memoize the volume check — it is called per-path during scans. */
    private static final ConcurrentHashMap<String, Boolean> LOCAL_VOLUME_CACHE =
        new ConcurrentHashMap<>();
    private static final int LOCAL_VOLUME_CACHE_MAX = 8192;

    /** Cache the login-item status per path (invalidated on add/remove). */
    private static final ConcurrentHashMap<String, Boolean> LOGIN_ITEM_CACHE =
        new ConcurrentHashMap<>();

    /** Probe for the optional 'mysides' tool once, not on every favorite call. */
    private static volatile Boolean mySidesAvailable;

    private Util() {
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    // ----------------------------------------------------------------- Login item

    public static void addLoginItem(String path) {
        if (!isMac() || path == null) {
            return;
        }
        String p = asAppleScriptString(path);
        String script =
            "tell application \"System Events\"\n"
            + "  if not (exists login item whose path is " + p + ") then\n"
            + "    make new login item at end with properties {path:" + p
            + ", hidden:false}\n"
            + "  end if\n"
            + "end tell";
        runOsascript(script);
        LOGIN_ITEM_CACHE.put(path, Boolean.TRUE);
    }

    public static void removeLoginItem(String path) {
        if (!isMac() || path == null) {
            return;
        }
        String p = asAppleScriptString(path);
        String script = "tell application \"System Events\" to delete "
            + "(every login item whose path is " + p + ")";
        runOsascript(script);
        LOGIN_ITEM_CACHE.put(path, Boolean.FALSE);
    }

    public static boolean hasLoginItem(String path) {
        if (!isMac() || path == null) {
            return false;
        }
        Boolean cached = LOGIN_ITEM_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        String p = asAppleScriptString(path);
        String script = "tell application \"System Events\" to return "
            + "(exists login item whose path is " + p + ")";
        String out = runOsascript(script);
        boolean has = out != null && out.trim().equalsIgnoreCase("true");
        LOGIN_ITEM_CACHE.put(path, has);
        return has;
    }

    // ------------------------------------------------------------- Sidebar favorite

    public static void addFavorite(String path) {
        if (!isMac() || path == null || !hasMySides()) {
            return;
        }
        File f = new File(path);
        runProcess("mysides", "add", f.getName(), f.toURI().toString());
    }

    public static void removeFavorite(String path) {
        if (!isMac() || path == null || !hasMySides()) {
            return;
        }
        File f = new File(path);
        runProcess("mysides", "remove", f.getName());
    }

    // --------------------------------------------------------------- Volume check

    /**
     * @param path a filesystem path or a {@code file://} URI
     * @return {@code true} if the path is on a local volume, {@code false} if it
     *         is on a network mount (or {@code true} as a safe default on error).
     */
    public static boolean isOnLocalVolume(String path) {
        if (path == null) {
            return true;
        }
        Boolean cached = LOCAL_VOLUME_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        boolean local = computeIsOnLocalVolume(path);
        if (LOCAL_VOLUME_CACHE.size() > LOCAL_VOLUME_CACHE_MAX) {
            LOCAL_VOLUME_CACHE.clear();
        }
        LOCAL_VOLUME_CACHE.put(path, local);
        return local;
    }

    private static boolean computeIsOnLocalVolume(String path) {
        try {
            String p = path;
            if (p.startsWith("file://")) {
                p = new java.net.URI(p).getPath();
            }
            Path np = Paths.get(p);
            // Walk up to an existing ancestor so getFileStore works for paths
            // that do not exist yet.
            while (np != null && Files.notExists(np)) {
                np = np.getParent();
            }
            if (np == null) {
                return true;
            }
            FileStore fs = Files.getFileStore(np);
            String type = fs.type() == null ? "" : fs.type().toLowerCase();
            String[] networkTypes = {"smbfs", "afpfs", "nfs", "webdav", "cifs",
                "ftp", "fuse", "smb", "afp"};
            for (String net : networkTypes) {
                if (type.contains(net)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOG.log(Level.FINE, "isOnLocalVolume failed for " + path
                + "; assuming local", e);
            return true;
        }
    }

    // -------------------------------------------------------------------- Helpers

    /** Quote a path as an AppleScript string literal. */
    private static String asAppleScriptString(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String runOsascript(String script) {
        return runProcess("osascript", "-e", script);
    }

    private static boolean hasMySides() {
        Boolean available = mySidesAvailable;
        if (available == null) {
            String out = runProcess("/bin/sh", "-c", "command -v mysides || true");
            available = out != null && !out.trim().isEmpty();
            mySidesAvailable = available;
            if (!available) {
                LOG.fine("Finder sidebar favorites unsupported ('mysides' not "
                    + "installed); skipping.");
            }
        }
        return available;
    }

    /**
     * Run a command, return its stdout (trimmed), or {@code null} on failure.
     * Never throws; bounded by {@link #CMD_TIMEOUT_SECONDS}.
     */
    private static String runProcess(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream(),
                    java.nio.charset.StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            if (!proc.waitFor(CMD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                LOG.warning("Command timed out: " + String.join(" ", cmd));
                return null;
            }
            if (proc.exitValue() != 0) {
                LOG.fine("Command exit " + proc.exitValue() + ": "
                    + String.join(" ", cmd) + " -> " + sb.toString().trim());
            }
            return sb.toString();
        } catch (Exception e) {
            LOG.log(Level.FINE, "Command failed: " + String.join(" ", cmd), e);
            return null;
        }
    }
}
