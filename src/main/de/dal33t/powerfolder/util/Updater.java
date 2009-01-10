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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.dialog.DownloadUpdateDialog;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A Thread that checks for updates on powerfolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class Updater extends Thread {

    private static final Logger log = Logger.getLogger(Updater.class.getName());
    protected Controller controller;
    protected UpdateSetting settings;
    private static boolean updateDialogOpen = false;

    public Updater(Controller controller, UpdateSetting settings) {
        super("Update checker");
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(settings, "Settings are null");
        this.controller = controller;
        this.settings = settings;
    }

    public void run() {
        checkForNewRelease();
    }

    Object option;

    /**
     * Checks for new application release at the remote location
     */
    private void checkForNewRelease() {
        log.info("Checking for newer version");

        final String newerVersion = newerReleaseVersionAvailable();

        if (shouldCheckForNewerVersion() && newerVersion != null
            && controller.isUIEnabled())
        {
            // Wait for ui to open
            controller.waitForUIOpen();

            final String text = Translation.getTranslation(
                "dialog.update_check.text", Controller.PROGRAM_VERSION,
                newerVersion);

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
                            JOptionPane.OK_CANCEL_OPTION, null, options
                                .toArray(), options.get(0));
                    }
                });
            } catch (InterruptedException ex) {
                log.log(Level.FINER, "InterruptedException", ex);
                return;
            }
            updateDialogOpen = false;

            if (option == downloadAndUpdate
                || option == downloadAndUpdateSilent)
            {
                boolean updateSilently = option == downloadAndUpdateSilent;
                URL releaseURL = getReleaseExeURL();
                if (releaseURL == null) {
                    return;
                }
                File targetFile = new File(Controller.getTempFilesLocation(),
                    "PowerFolder_Latest_Win32_Installer.exe");
                // Download
                boolean completed = downloadFromURL(releaseURL, targetFile,
                    settings.httpUser, settings.httpPassword);
                // And start
                if (completed) {
                    log.log(Level.INFO, "Download completed. "
                        + targetFile.getAbsolutePath());
                    openReleaseExe(targetFile, updateSilently);
                } else {
                    try {
                        UIUtil.invokeAndWaitInEDT(new Runnable() {
                            public void run() {
                                // Show warning.
                                DialogFactory
                                    .genericDialog(controller, Translation
                                            .getTranslation("dialog.update_check.failed.title"),
                                        Translation
                                            .getTranslation("dialog.update_check.failed.text"),
                                        GenericDialogType.WARN);
                            }
                        });
                    } catch (InterruptedException ex) {
                        log.log(Level.FINER, "InterruptedException", ex);
                        return;
                    }
                }
                if (!Util.isRunningProVersion()) {
                    try {
                        // Open explorer
                        BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_URL
                            .getValue(controller));
                    } catch (IOException e) {
                        log.log(Level.FINER, "IOException", e);
                    }
                }
            } else if (option == gotoHomepage) {
                try {
                    // Open explorer
                    BrowserLauncher.openURL(ConfigurationEntry.PROVIDER_URL
                        .getValue(controller));
                } catch (IOException e) {
                    log.log(Level.FINER, "IOException", e);
                }
            } else if (option == nothingNeverAsk) {
                // Never ask again
                PreferencesEntry.CHECK_UPDATE.setValue(controller, false);
            }
        }

        if (newerVersion == null) {
            notifyNoUpdateAvailable();
        }
    }

    /**
     * Downloads a new powerfolder jar from a URL
     * 
     * @param url
     *            the url
     * @param destFile
     *            the file to store the content in
     * @return true if succeeded
     */
    private boolean downloadFromURL(URL url, File destFile, String username,
        String pw)
    {
        URLConnection con;
        try {
            con = url.openConnection();
            if (!StringUtils.isEmpty(username)) {
                String s = username + ":" + pw;
                String base64 = "Basic " + Base64.encodeBytes(s.getBytes());
                con.setDoInput(true);
                con.setRequestProperty("Authorization", base64);
                con.connect();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to download from " + url, e);
            return false;
        }

        // Build update download dialog
        DownloadUpdateDialog dlDialog = null;
        if (controller.isUIOpen()) {
            dlDialog = new DownloadUpdateDialog(controller);
            dlDialog.openInEDT();
        }

        log.log(Level.WARNING,
                "Downloading latest version from " + con.getURL());
        File tempFile = new File(destFile.getParentFile(), "(downloading) "
            + destFile.getName());
        try {
            // Copy/Download from URL
            con.connect();
            FileUtils.copyFromStreamToFile(con.getInputStream(), tempFile,
                dlDialog != null ? dlDialog.getStreamCallback() : null, con
                    .getContentLength());
        } catch (IOException e) {
            log.log(Level.WARNING,
                "Unable to download from " + url, e);
            return false;
        } finally {
            if (dlDialog != null) {
                dlDialog.close();
            }
        }

        // Rename file and set modified/build time
        destFile.delete();
        tempFile.renameTo(destFile);
        destFile.setLastModified(con.getLastModified());

        if (destFile.getName().toLowerCase().endsWith("jar")) {
            // Additional jar check
            if (!FileUtils.isValidZipFile(destFile)) {
                // Invalid file downloaded
                destFile.delete();
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the newer program version available on the net. Otherwise returns
     * null
     * 
     * @return
     */
    protected String newerReleaseVersionAvailable() {
        URL url;
        try {
            url = new URL(settings.versionCheckURL);
        } catch (MalformedURLException e) {
            log.log(Level.FINER, "MalformedURLException", e);
            return null;
        }
        try {
            InputStream in = (InputStream) url.getContent();
            String latestVersion = "";
            while (in.available() > 0) {
                latestVersion += (char) in.read();
            }

            if (latestVersion != null) {
                if (latestVersion.length() > 50) {
                    log
                        .severe("Received illegal response while checking latest available version from "
                            + settings.versionCheckURL);
                    return null;
                }
                log.info("Latest available version: " + latestVersion);

                if (Util.compareVersions(latestVersion,
                    Controller.PROGRAM_VERSION))
                {
                    log.info("Latest version is newer than this one");
                    return latestVersion;
                }
                log.info("This version is up-to-date");
            }

        } catch (IOException e) {
            log.warning("Unable to retrieve latest available version for: "
                + settings.versionCheckURL);
            log.log(Level.FINER, "IOException", e);
        }
        return null;
    }

    /**
     * Returns the download URL for the latest program version
     * 
     * @return
     */
    protected URL getReleaseExeURL() {
        URL releaseExeURL = null;
        try {
            if (settings.downloadLinkInfoURL != null) {
            URL url = new URL(settings.downloadLinkInfoURL);
            InputStream in = (InputStream) url.getContent();
            StringBuilder b = new StringBuilder();
            while (in.available() > 0) {
                b.append((char) in.read());
            }
            in.close();

            releaseExeURL = new URL(b.toString());
            log.info("Latest available version download: "
                + releaseExeURL.toExternalForm());
            }
        } catch (MalformedURLException e) {
            log.log(Level.FINER, "MalformedURLException", e);
        } catch (IOException e) {
            log.log(Level.FINER, "IOException", e);
        }
        if (releaseExeURL == null) {
            // Fallback to standart settings
            try {
                releaseExeURL = new URL(settings.releaseExeURL);
            } catch (MalformedURLException e) {
                log.log(Level.SEVERE, "Invalid release exec download location",
                        e);
            }
        }
        return releaseExeURL;
    }

    /**
     * Notifies user that no update is available
     * <p>
     */
    protected void notifyNoUpdateAvailable() {
        // Do nothing here
        // Method included for override in ManuallyInvokedUpdateChecker
    }

    /**
     * Determines if the application should check for a newer version
     * 
     * @return true if yes, false if no
     */
    protected boolean shouldCheckForNewerVersion() {
        return !updateDialogOpen
            && PreferencesEntry.CHECK_UPDATE.getValueBoolean(controller);
    }

    /**
     * Retrieves the Frame that is the current parent
     * 
     * @return Parent JFrame
     */
    protected JFrame getParentFrame() {
        return controller.getUIController().getMainFrame().getUIComponent();
    }

    private void openReleaseExe(File file, boolean updateSilently) {
        // try {
        // FileUtils.openFile(targetFile);
        // } catch (IOException e) {
        // log.error(e);
        // }
        try {
            String c = "cmd.exe";
            c += " /c ";
            c += "\"";
            c += file.getAbsolutePath();
            if (updateSilently) {
                c += " /S";
            }
            log.log(Level.WARNING, "Executing: " + c);
            c += "\"";
            Runtime.getRuntime().exec(c);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to start update exe at " + file.getAbsolutePath()
                + ". " + e, e);
        }
    }

    /**
     * Contains settings for the updatecheck.
     */
    public static class UpdateSetting {
        public String versionCheckURL = "http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt";
        /**
         * A info file containing the link that may override
         * <code>releaseExeURL</code> if existing.
         */
        public String downloadLinkInfoURL = "http://checkversion.powerfolder.com/PowerFolder_DownloadLocation.txt";
        public String releaseExeURL = "http://download.powerfolder.com/free/PowerFolder_Latest_Win32_Installer.exe";

        public String httpUser;
        public String httpPassword;
    }
}