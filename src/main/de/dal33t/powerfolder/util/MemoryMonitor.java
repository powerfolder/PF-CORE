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
 * $Id: MemoryMonitor.java 18011 2012-02-05 05:29:07Z harry $
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.notices.NoticeSeverity;
import de.dal33t.powerfolder.ui.notices.RunnableNotice;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects if PowerFolder is running out of memory.
 */
public class MemoryMonitor implements Runnable {

    private static final Logger log = Logger.getLogger(MemoryMonitor.class
        .getName());

    private Controller controller;
    private boolean runAlready;

    public MemoryMonitor(Controller controller) {
        this.controller = controller;
    }

    public void run() {

        // Do not show dialog repeatedly.
        if (runAlready) {
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        log.fine("Max Memory: " + Format.formatBytesShort(maxMemory)
            + ", Total Memory: " + Format.formatBytesShort(totalMemory));

        if (totalMemory > 0.95d * maxMemory) {
            addWarning();
            runAlready = true;
        }
    }

    /**
     * Add a warning event for the user.
     */
    private void addWarning() {
        RunnableNotice notice = new RunnableNotice(Translation
            .get("warning_notice.title"), Translation
            .get("warning_notice.low_memory"), new Runnable() {
            public void run() {
                if ((OSUtil.isWindowsSystem() || OSUtil.isMacOS())
                    && !OSUtil.isWebStart())
                {
                    int response = DialogFactory
                        .genericDialog(controller, Translation
                            .get("low_memory.title"), Translation
                            .get("low_memory.text", Help
                                .getWikiArticleURL(controller,
                                    WikiLinks.MEMORY_CONFIGURATION)),
                            new String[]{
                                Translation
                                    .get("low_memory.increase"),
                                Translation
                                    .get("low_memory.do_nothing")},
                            0, GenericDialogType.WARN);
                    if (response == 0) { // Increase memory
                        increaseAvailableMemory();
                    }
                } else {
                    // No ini - Can only warn user.
                    DialogFactory.genericDialog(controller, Translation
                        .get("low_memory.title"), Translation
                        .get("low_memory.warn"),
                        new String[]{Translation.get("general.ok")},
                        0, GenericDialogType.WARN);
                }
            }
        }, NoticeSeverity.WARINING);
        controller.getUIController().getApplicationModel().getNoticesModel()
            .handleNotice(notice);
    }

    public void increaseAvailableMemory() {
        if (!OSUtil.is64BitPlatform()) {
            DialogFactory.genericDialog(controller, Translation
                            .get("low_memory.title"), Translation
                            .get("low_memory.install_64bit"),
                    GenericDialogType.WARN);
            return;
        }
        boolean increased;
        if (OSUtil.isMacOS()) {
            increased = increaseAvailableMemoryMac();
        } else {
            increased = increaseAvailableMemoryWin();
        }
        if (!increased) {
            // Increase on next start
            PreferencesEntry.MEMORY_LIMIT_INCREASE.setValue(controller, true);
        }
    }

    private boolean increaseAvailableMemoryWin() {

        // Read the current ini file.
        boolean wroteNewIni = false;
        PrintWriter pw = null;
        try {
            // log.fine("Looking for ini...");
            // br = new BufferedReader(new FileReader("PowerFolder.ini"));
            // Loggable.logFineStatic(MemoryMonitor.class, "Found ini...");
            // String line;
            // boolean found = false;
            // while ((line = br.readLine()) != null) {
            // if (line.startsWith("-Xmx")) {
            // // Found default ini.
            // found = true;
            // Loggable.logFineStatic(MemoryMonitor.class,
            // "Found maximum memory line...");
            // }
            // }

            boolean alreadyMax = Runtime.getRuntime().totalMemory() / 1024 / 1024 > 8000;
            // Write a new one if found.
            if (!alreadyMax) {
                pw = new PrintWriter(new FileWriter(controller.getL4JININame()));
                log.fine("Writing new ini...");
                pw.println("-Xms256m");
                pw.println("-Xmx8192m");
                pw.println("-XX:NewRatio=8");
                pw.println("-XX:MinHeapFreeRatio=10");
                pw.println("-XX:MaxHeapFreeRatio=20");
                pw.flush();
                wroteNewIni = true;
                log.fine("Wrote new ini...");
            }
        } catch (IOException e) {
            log.log(Level.FINE, "Problem reconfiguring ini: " + e.getMessage());
        } finally {
            // if (br != null) {
            // try {
            // br.close();
            // } catch (IOException e) {
            // // Ignore
            // }
            // }
            if (pw != null) {
                pw.close();
            }
        }

        // Show a response
        if (wroteNewIni) {
            DialogFactory.genericDialog(controller, Translation
                .get("low_memory.title"), Translation
                .get("low_memory.configure_success"),
                GenericDialogType.INFO);
        } else {
            DialogFactory.genericDialog(controller, Translation
                .get("low_memory.title"), Translation
                .get("low_memory.configure_failure"),
                GenericDialogType.WARN);
        }
        return wroteNewIni;
    }

    /**
     * Reconfigure Info.plist from (initial) 512M to 1024M max memory.
     */
    private boolean increaseAvailableMemoryMac() {
        boolean success = false;
        PrintWriter pw = null;
        BufferedReader br = null;
        try {
            log.fine("Looking for Contents/Info.plist...");
            Path orig = Paths.get(controller.getDistribution().getBinaryName() + ".app/Contents/Info.plist");
            log.info("Modifing " + orig.toRealPath());
            Path temp = Paths.get(controller.getDistribution().getBinaryName() + ".app/Contents/Info.plist.temp");
            Path back = Paths.get(controller.getDistribution().getBinaryName() + ".app/Contents/Info.plist.backup");
            br = Files.newBufferedReader(orig, Charset.forName("UTF-8"));
            pw = new PrintWriter(Files.newBufferedWriter(temp, Charset.forName("UTF-8")));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("<string>-Xm")
                        && !line.contains("-Xmx8192m")) {
                    pw
                            .write("      <string>-Xms256m -Xmx8192m "
                                    + "-XX:NewRatio=8 -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10</string>");
                    success = true;
                } else {
                    pw.write(line);
                }
                pw.write('\n');
            }
            br.close();
            pw.close();
            PathUtils.copyFile(orig, back);
            PathUtils.copyFile(temp, orig);
        } catch (IOException e) {
            log.log(Level.WARNING,
                    "Problem reconfiguring Contents/Info.plist: " + e.getMessage(),
                    e);
            success = false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (pw != null) {
                pw.close();
            }
        }
        // Show a response
        if (success) {
            DialogFactory.genericDialog(controller, Translation
                            .get("low_memory.title"), Translation
                            .get("low_memory.configure_success"),
                    GenericDialogType.INFO);
        } else {
            DialogFactory.genericDialog(controller, Translation
                            .get("low_memory.title"), Translation
                            .get("low_memory.configure_failure"),
                    GenericDialogType.WARN);
        }
        return success;
    }
}
