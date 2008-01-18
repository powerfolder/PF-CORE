package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.ui.DialogFactory;

import java.awt.*;

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
        while(!controller.isShuttingDown()) {
            try {
                // Check every minute.
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                // ignore
            }

            long totalMemory = runtime.totalMemory();
            log.debug("Max Memory: " + maxMemory + ", Total Memory: " + totalMemory);

            // See if there is any more memory to allocate.
            if (maxMemory == totalMemory) {
                Frame parent = controller.getUIController().getMainFrame()
                    .getUIComponent();
                boolean showAgain = DialogFactory.showNeverAskAgainMessageDialog(parent,
                        Translation.getTranslation("lowmemory.title"),
                        Translation.getTranslation("lowmemory.text"),
                        Translation.getTranslation("lowmemory.dont_autodetect"));
                if (!showAgain) {
                    // Foolish user!
                    PreferencesEntry.DETECT_LOW_MEMORY.setValue(controller,
                        false);
                }

                // Do not show dialog repeatedly.
                break;
            }
        }
    }
}
