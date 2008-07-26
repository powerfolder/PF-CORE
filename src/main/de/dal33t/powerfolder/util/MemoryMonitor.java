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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.EventQueue;

import javax.swing.JFrame;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Detects if PowerFolder is running out of memory.
 */
public class MemoryMonitor implements Runnable {

    private static final String POWERFOLDER_INI_FILE = "PowerFolder.l4j.ini";
    private Controller controller;
    private static final Logger LOG = Logger.getLogger(MemoryMonitor.class);

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
            LOG.debug("Max Memory: " + Format.formatBytesShort(maxMemory)
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
                JFrame parent = controller.getUIController().getMainFrame()
                        .getUIComponent();
                NeverAskAgainResponse response;
                if (OSUtil.isWindowsSystem() && !OSUtil.isWebStart()) {
                    response = DialogFactory.genericDialog(parent, Translation
                            .getTranslation("lowmemory.title"), Translation
                            .getTranslation("lowmemory.text"), new String[]{
                            Translation.getTranslation("lowmemory.increase"),
                            Translation.getTranslation("lowmemory.do_nothing")}, 0,
                            GenericDialogType.WARN, Translation
                            .getTranslation("lowmemory.dont_autodetect"));
                    if (response.getButtonIndex() == 0) { // Increase memory
                        increaseAvailableMemory();
                    }
                } else {
                    // No ini - Can only warn user.
                    response = DialogFactory.genericDialog(parent, Translation
                            .getTranslation("lowmemory.title"), Translation
                            .getTranslation("lowmemory.warn"),
                            new String[]{Translation.getTranslation("general.ok")},
                            0, GenericDialogType.WARN, Translation
                            .getTranslation("lowmemory.dont_autodetect"));
                }

                if (response.isNeverAskAgain()) {
                    // Foolish user!
                    PreferencesEntry.DETECT_LOW_MEMORY.setValue(controller,
                            false);
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
            LOG.debug("Looking for ini...");
            // br = new BufferedReader(new FileReader("PowerFolder.ini"));
            LOG.debug("Found ini...");
            // String line;
            // boolean found = false;
            // while ((line = br.readLine()) != null) {
            // if (line.startsWith("-Xmx")) {
            // // Found default ini.
            // found = true;
            // log.debug("Found maximum memory line...");
            // }
            // }

            boolean alreadyMax = Runtime.getRuntime().totalMemory() / 1024 / 1024 > 500;
            // Write a new one if found.
            if (!alreadyMax) {
                pw = new PrintWriter(new FileWriter(POWERFOLDER_INI_FILE));
                LOG.debug("Writing new ini...");
                pw.println("-Xms16m");
                pw.println("-Xmx512m");
                pw.println("-XX:MinHeapFreeRatio=10");
                pw.println("-XX:MaxHeapFreeRatio=20");
                pw.flush();
                wroteNewIni = true;
                LOG.debug("Wrote new ini...");
            }
        } catch (IOException e) {
            LOG.debug("Problem reconfiguring ini: " + e.getMessage());
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
        JFrame parent = controller.getUIController().getMainFrame()
            .getUIComponent();
        if (wroteNewIni) {
            DialogFactory.genericDialog(parent, Translation
                .getTranslation("lowmemory.title"), Translation
                .getTranslation("lowmemory.configure_success"),
                GenericDialogType.INFO);
        } else {
            DialogFactory.genericDialog(parent, Translation
                .getTranslation("lowmemory.title"), Translation
                .getTranslation("lowmemory.configure_failure"),
                GenericDialogType.WARN);
        }
    }
}
