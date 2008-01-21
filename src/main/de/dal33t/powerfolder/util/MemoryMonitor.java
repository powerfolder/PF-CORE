package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;

import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
 * Detects if PowerFolder is running out of memory.
 */
public class MemoryMonitor implements Runnable {

    private Controller controller;
    private static final Logger log = Logger.getLogger(MemoryMonitor.class);

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
                //Interrupt? ==> quit!
                return;
            }

            long totalMemory = runtime.totalMemory();
            log.debug("Max Memory: " + maxMemory + ", Total Memory: " + totalMemory);

            // See if there is any more memory to allocate.
            if (maxMemory == totalMemory) {
                Frame parent = controller.getUIController().getMainFrame()
                        .getUIComponent();
                NeverAskAgainResponse response = DialogFactory.showNeverAskAgainMessageDialog(parent,
                        Translation.getTranslation("lowmemory.title"),
                        Translation.getTranslation("lowmemory.text"),
                        Translation.getTranslation("lowmemory.dont_autodetect"),
                        new String[]{"Increase Memory", "Do nothing"});
                if (response.isNeverAskAgain()) {
                    // Foolish user!
                    PreferencesEntry.DETECT_LOW_MEMORY.setValue(controller,
                            false);
                }

                if (response.getButtonIndex() == 0) { // Increase memory
                    increaseAvailableMemory();
                }

                // Do not show dialog repeatedly.
                break;
            }
        }
    }

    /**
     * Reconfigure ini from 54M to 256M.
     */
    private void increaseAvailableMemory() {
        // Read the current ini file.
        boolean wroteNewIni = false;
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            log.debug("Looking for ini...");
            br = new BufferedReader(new FileReader("PowerFolder.ini"));
            log.debug("Found ini...");
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                if (line.equals("-Xmx54m")) {
                    // Found existing ini.
                    found = true;
                    log.debug("Found maximum memory line...");
                }
            }
            if (found) {
                pw = new PrintWriter(new FileWriter("PowerFolder.ini"));
                log.debug("Writing new ini...");
                pw.println("-Xms16m");
                pw.println("-Xmx256m");
                pw.println("-XX:MinHeapFreeRatio=10");
                pw.println("-XX:MaxHeapFreeRatio=20");
                pw.flush();
                wroteNewIni = true;
                log.debug("Wrote new ini...");
            }
        } catch (IOException e) {
            log.debug("Problem reconfiguring ini: " + e.getMessage());
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

        Frame parent = controller.getUIController().getMainFrame()
                .getUIComponent();
        if (wroteNewIni) {
            DialogFactory.showMessageDialog(parent,
                    Translation.getTranslation("lowmemory.title"),
                    Translation.getTranslation("lowmemory.configure_success"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            DialogFactory.showMessageDialog(parent,
                    Translation.getTranslation("lowmemory.title"),
                    Translation.getTranslation("lowmemory.configure_failure"),
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}
