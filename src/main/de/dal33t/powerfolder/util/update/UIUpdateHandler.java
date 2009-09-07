package de.dal33t.powerfolder.util.update;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.dialog.DownloadUpdateDialog;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.UIUtil;

public class UIUpdateHandler extends PFUIComponent implements UpdaterHandler {
    private static volatile boolean updateDialogOpen = false;
    private Object option;

    public UIUpdateHandler(Controller controller) {
        super(controller);
    }

    public void newReleaseAvailable(UpdaterEvent event) {
        // Wait for ui to open
        getController().waitForUIOpen();

        final String text = Translation.getTranslation(
            "dialog.update_check.text", Controller.PROGRAM_VERSION, event
                .getNewReleaseVersion());

        final List<String> options = new ArrayList<String>(4);
        String downloadAndUpdateSilent = Translation
            .getTranslation("dialog.update_check.downloadAndUpdateSilent");
        String downloadAndUpdate = Translation
            .getTranslation("dialog.update_check.downloadAndUpdate");
        String gotoHomepage = Translation
            .getTranslation("dialog.update_check.gotoHomepage");
        String nothingNeverAsk = Translation
            .getTranslation("dialog.update_check.nothingNeverAsk");

        if (OSUtil.isWindowsSystem()) {
            options.add(downloadAndUpdate);
            options.add(downloadAndUpdateSilent);
        }
        options.add(gotoHomepage);
        options.add(nothingNeverAsk);

        updateDialogOpen = true;
        try {
            UIUtil.invokeAndWaitInEDT(new Runnable() {
                public void run() {
                    option = JOptionPane.showInputDialog(getParentFrame(),
                        text, Translation
                            .getTranslation("dialog.update_check.title"),
                        JOptionPane.OK_CANCEL_OPTION, null, options.toArray(),
                        options.get(0));
                }
            });
        } catch (InterruptedException ex) {
            logFiner(ex);
            return;
        }
        updateDialogOpen = false;

        if (option == downloadAndUpdate || option == downloadAndUpdateSilent) {
            boolean updateSilently = option == downloadAndUpdateSilent;
            URL releaseURL = event.getNewReleaseExeURL();
            if (releaseURL == null) {
                return;
            }
            DownloadUpdateDialog dlDiag = new DownloadUpdateDialog(
                getController());
            dlDiag.openInEDT();
            boolean success = event.getUpdater().downloadAndUpdate(releaseURL,
                dlDiag.getStreamCallback(), updateSilently) != null;
            dlDiag.close();
            if (!success) {
                try {
                    UIUtil.invokeAndWaitInEDT(new Runnable() {
                        public void run() {
                            // Show warning.
                            DialogFactory
                                .genericDialog(
                                    getController(),
                                    Translation
                                        .getTranslation("dialog.update_check.failed.title"),
                                    Translation
                                        .getTranslation("dialog.update_check.failed.text"),
                                    GenericDialogType.WARN);
                        }
                    });
                } catch (InterruptedException ex) {
                    logFiner(ex);
                    return;
                }
            }
        } else if (option == gotoHomepage) {
            try {
                // Open explorer
                BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_URL
                    .getValue(getController()));
            } catch (IOException e) {
                logFiner(e);
            }
        } else if (option == nothingNeverAsk) {
            // Never ask again
            PreferencesEntry.CHECK_UPDATE.setValue(getController(), false);
        }
    }

    public void noNewReleaseAvailable(UpdaterEvent event) {
        // Do nothing
    }

    public boolean shouldCheckForNewVersion() {
        return !updateDialogOpen
            && PreferencesEntry.CHECK_UPDATE.getValueBoolean(getController());
    }

    /**
     * Retrieves the Frame that is the current parent
     * 
     * @return Parent JFrame
     */
    protected JFrame getParentFrame() {
        return getUIController().getMainFrame().getUIComponent();
    }

}
