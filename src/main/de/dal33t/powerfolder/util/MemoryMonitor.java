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
* $Id$
*/
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import java.awt.EventQueue;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects if PowerFolder is running out of memory.
 */
public class MemoryMonitor implements Runnable {

    private static final Logger log = Logger.getLogger(MemoryMonitor.class.getName());

    private static final String POWERFOLDER_INI_FILE = "PowerFolder.l4j.ini";
    private Controller controller;

    public MemoryMonitor(Controller controller) {
        this.controller = controller;
    }

    public void run() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        while (!controller.isShuttingDown()) {
            try {
                // Check every minute.
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                // Interrupt? ==> quit!
                return;
            }

            long totalMemory = runtime.totalMemory();
            log.fine("Max Memory: " + Format.formatBytesShort(maxMemory)
                    + ", Total Memory: " + Format.formatBytesShort(totalMemory));

            // See if there is any more memory to allocate. Defer if dialog
            // currently shown.
            if (maxMemory == totalMemory && !DialogFactory.isDialogInUse()) {
                showDialog();

                // Do not show dialog repeatedly.
                break;
            }
        }
    }

    /**
     * Show dialog in event thread.
     */
    private void showDialog() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (OSUtil.isWindowsSystem() && !OSUtil.isWebStart()) {
                    int response = DialogFactory.genericDialog(controller, Translation
                            .getTranslation("low_memory.title"), Translation
                            .getTranslation("low_memory.text"), new String[]{
                            Translation.getTranslation("low_memory.increase"),
                            Translation.getTranslation("low_memory.do_nothing")}, 0,
                            GenericDialogType.WARN);
                    if (response == 0) { // Increase memory
                        increaseAvailableMemory();
                    }
                } else {
                    // No ini - Can only warn user.
                    DialogFactory.genericDialog(controller, Translation
                            .getTranslation("low_memory.title"), Translation
                            .getTranslation("low_memory.warn"),
                            new String[]{Translation.getTranslation("general.ok")},
                            0, GenericDialogType.WARN);
                }
            }
        });
    }

    /**
     * Reconfigure ini from (initial) 54M to 256M max memory.
     */
    private void increaseAvailableMemory() {

        // Read the current ini file.
        boolean wroteNewIni = false;
        PrintWriter pw = null;
        try {
            //log.fine("Looking for ini...");
            // br = new BufferedReader(new FileReader("PowerFolder.ini"));
            //Loggable.logFineStatic(MemoryMonitor.class, "Found ini...");
            // String line;
            // boolean found = false;
            // while ((line = br.readLine()) != null) {
            // if (line.startsWith("-Xmx")) {
            // // Found default ini.
            // found = true;
            // Loggable.logFineStatic(MemoryMonitor.class, "Found maximum memory line...");
            // }
            // }

            boolean alreadyMax = Runtime.getRuntime().totalMemory() / 1024 / 1024 > 500;
            // Write a new one if found.
            if (!alreadyMax) {
                pw = new PrintWriter(new FileWriter(POWERFOLDER_INI_FILE));
                log.fine("Writing new ini...");
                pw.println("-Xms16m");
                pw.println("-Xmx512m");
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
                .getTranslation("low_memory.title"), Translation
                .getTranslation("low_memory.configure_success"),
                GenericDialogType.INFO);
        } else {
            DialogFactory.genericDialog(controller, Translation
                .getTranslation("low_memory.title"), Translation
                .getTranslation("low_memory.configure_failure"),
                GenericDialogType.WARN);
        }
    }
}
