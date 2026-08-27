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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.logging.LoggingManager;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PFS-5739: Writes a thread dump every few minutes so that a support package created AFTER an
 * incident still contains the stack traces from the time it happened. Until now a dump only existed
 * at the moment somebody asked for one, which is always the moment the server is healthy again.
 * <p>
 * The dumps are written next to the log files ({@link LoggingManager#getDebugDir()}, subdirectory
 * {@value #DUMP_SUBDIR}) and therefore travel into the support package with them - in a cluster each
 * node contributes its own. How long they are kept is the shorter of
 * {@link ConfigurationEntry#THREAD_DUMP_KEEP_DAYS} and the log retention
 * ({@link ConfigurationEntry#LOG_FILE_DELETE_DAYS}) - see {@link #retentionDays()}.
 * <p>
 * Recording is meant to be unnoticeable: one zip per run holding the dump as a .txt (a dump of a busy
 * server is several MB of very repetitive text, and a zip opens with a double click on every platform
 * we support), and any failure - a full disk above all - produces a warning and
 * nothing else. The task also swallows errors on purpose, because a scheduled task that throws is
 * never run again.
 */
public class ThreadDumpRecorder extends PFComponent {

    /** Subdirectory of the debug directory holding the recorded dumps. */
    public static final String DUMP_SUBDIR = "Threaddumps";

    private static final String FILE_PREFIX = "Threads-";
    private static final String FILE_SUFFIX = ".zip";
    /** The name of the single entry inside {@link #FILE_SUFFIX}. */
    private static final String ENTRY_SUFFIX = ".txt";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    public ThreadDumpRecorder(Controller controller) {
        super(controller);
    }

    /**
     * Starts the periodic recording, unless it is switched off
     * ({@code threaddump.interval.seconds} = 0 or less).
     */
    public void start() {
        int seconds = ConfigurationEntry.THREAD_DUMP_INTERVAL_SECONDS.getValueInt(getController());
        if (seconds <= 0) {
            logFine("Thread dump recording is disabled");
            return;
        }
        long period = 1000L * seconds;
        getController().scheduleAndRepeat(this::record, period, period);
        int keep = retentionDays();
        logInfo(dumpDir() + ": Recording a thread dump every " + seconds + " second(s), kept for "
            + (keep < 0 ? "as long as the disk allows" : keep + " day(s)"));
    }

    /**
     * How long dumps are kept: the shorter of {@link ConfigurationEntry#THREAD_DUMP_KEEP_DAYS} and
     * the log retention. Negative means keep - a value that says so is not a limit, so the other
     * one decides, and where both say it nothing is deleted.
     */
    int retentionDays() {
        int ownDays = ConfigurationEntry.THREAD_DUMP_KEEP_DAYS.getValueInt(getController());
        int logDays = ConfigurationEntry.LOG_FILE_DELETE_DAYS.getValueInt(getController());
        if (ownDays < 0) {
            return logDays;
        }
        if (logDays < 0) {
            return ownDays;
        }
        return Math.min(ownDays, logDays);
    }

    /**
     * Deletes recorded dumps that have outlived {@link #retentionDays()} - called by the same
     * housekeeping that prunes the log files.
     */
    public void removeOldDumps() {
        int maxAgeDays = retentionDays();
        if (maxAgeDays < 0) {
            return;
        }
        Path dir = dumpDir();
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        DirectoryStream.Filter<Path> outdated = entry -> {
            String name = entry.getFileName().toString();
            if (!name.startsWith(FILE_PREFIX) || !name.endsWith(FILE_SUFFIX)) {
                return false;
            }
            long msOld = System.currentTimeMillis() - Files.getLastModifiedTime(entry).toMillis();
            return msOld / 1000 / 60 / 60 / 24 >= maxAgeDays;
        };
        int deleted = 0;
        try (DirectoryStream<Path> oldDumps = Files.newDirectoryStream(dir, outdated)) {
            for (Path dump : oldDumps) {
                Files.delete(dump);
                deleted++;
            }
        } catch (IOException e) {
            logWarning(dir + ": Unable to remove thread dumps older than " + maxAgeDays + " day(s). " + e);
        }
        if (deleted > 0) {
            logFine(dir + ": Removed " + deleted + " thread dump(s) older than " + maxAgeDays + " day(s)");
        }
    }

    /**
     * @return the directory the recorded dumps are written to, or {@code null} if it is unavailable
     */
    public static Path getDumpDir() {
        Path debugDir = LoggingManager.getDebugDir();
        return debugDir != null ? debugDir.resolve(DUMP_SUBDIR) : null;
    }

    private void record() {
        Path dir = dumpDir();
        if (dir == null) {
            return;
        }
        String name = FILE_PREFIX + new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        Path dumpFile = dir.resolve(name + FILE_SUFFIX);
        try {
            Files.createDirectories(dir);
            String dump = Debug.dumpCurrentStacktraces(false);
            try (ZipOutputStream out = new ZipOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(dumpFile)), StandardCharsets.UTF_8))
            {
                out.putNextEntry(new ZipEntry(name + ENTRY_SUFFIX));
                // No Writer around it: closing one would close the stream and the entry with it.
                out.write(dump.getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
            if (isFiner()) {
                logFiner(dumpFile + ": Recorded thread dump (" + dump.length() + " chars)");
            }
        } catch (Throwable t) {
            // A scheduled task that throws is never run again - and a full disk must not end the
            // recording for the rest of the server's uptime.
            logWarning(dumpFile + ": Unable to record thread dump. " + t);
        }
    }

    private Path dumpDir() {
        Path dir = getDumpDir();
        if (dir == null) {
            logWarning("No debug directory available, cannot record thread dumps");
        }
        return dir;
    }
}
