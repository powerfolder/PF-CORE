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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.dialog.DownloadUpdateDialog;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * A Thread that checks for updates on powerfolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class Updater extends Thread {
    private static Logger LOG = Logger.getLogger(Updater.class);
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
        LOG.info("Checking for newer version");

        final String newerVersion = newerReleaseVersionAvailable();

        if (shouldCheckForNewerVersion() && newerVersion != null
            && controller.isUIEnabled())
        {
            // Wait for ui to open
            controller.waitForUIOpen();

            final String text = Translation.getTranslation(
                "dialog.updatecheck.text", Controller.PROGRAM_VERSION,
                newerVersion);

            final List<String> options = new ArrayList<String>(4);
            String downloadAndUpdateSilent = Translation
                .getTranslation("dialog.updatecheck.downloadAndUpdateSilent");
            String downloadAndUpdate = Translation
                .getTranslation("dialog.updatecheck.downloadAndUpdate");
            String gotoHomepage = Translation
                .getTranslation("dialog.updatecheck.gotoHomepage");
            String nothingNeverAsk = Translation
                .getTranslation("dialog.updatecheck.nothingNeverAsk");

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
                                .getTranslation("dialog.updatecheck.title"),
                            JOptionPane.OK_CANCEL_OPTION, null, options
                                .toArray(), options.get(0));
                    }
                });
            } catch (InterruptedException ex) {
                LOG.verbose(ex);
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
                    LOG.warn("Download completed. "
                        + targetFile.getAbsolutePath());
                    openReleaseExe(targetFile, updateSilently);
                } else {
                    try {
                        UIUtil.invokeAndWaitInEDT(new Runnable() {
                            public void run() {
                                // Show warning.
                                DialogFactory
                                    .genericDialog(
                                        controller.getUIController()
                                            .getMainFrame().getUIComponent(),
                                        Translation
                                            .getTranslation("dialog.updatecheck.failed.title"),
                                        Translation
                                            .getTranslation("dialog.updatecheck.failed.text"),
                                        GenericDialogType.WARN);
                            }
                        });
                    } catch (InterruptedException ex) {
                        LOG.verbose(ex);
                        return;
                    }
                }
                if (!Util.isRunningProVersion()) {
                    try {
                        // Open explorer
                        BrowserLauncher.openURL(Constants.POWERFOLDER_URL);
                    } catch (IOException e) {
                        LOG.verbose(e);
                    }
                }
            } else if (option == gotoHomepage) {
                try {
                    // Open explorer
                    BrowserLauncher.openURL(Constants.POWERFOLDER_URL);
                } catch (IOException e) {
                    LOG.verbose(e);
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
            LOG.error("Unable to download from " + url, e);
            return false;
        }

        // Build update download dialog
        DownloadUpdateDialog dlDialog = null;
        if (controller.isUIOpen()) {
            dlDialog = new DownloadUpdateDialog(controller);
            dlDialog.openInEDT();
        }

        LOG.warn("Downloading latest version from " + con.getURL());
        File tempFile = new File(destFile.getParentFile(), "(downloading) "
            + destFile.getName());
        try {
            // Copy/Download from URL
            con.connect();
            FileUtils.copyFromStreamToFile(con.getInputStream(), tempFile,
                dlDialog != null ? dlDialog.getStreamCallback() : null, con
                    .getContentLength());
        } catch (IOException e) {
            LOG.warn("Unable to download from " + url, e);
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
            LOG.verbose(e);
            return null;
        }
        try {
            InputStream in = (InputStream) url.getContent();
            String latestVersion = "";
            while (in.available() > 0) {
                latestVersion += (char) in.read();
            }

            if (latestVersion != null) {
                LOG.info("Latest available version: " + latestVersion);

                if (Util.compareVersions(latestVersion,
                    Controller.PROGRAM_VERSION))
                {
                    LOG.info("Latest version is newer than this one");
                    return latestVersion;
                }
                LOG.info("This version is up-to-date");
            }

        } catch (IOException e) {
            LOG.verbose(e);
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
            URL url = new URL(settings.downloadLinkInfoURL);
            InputStream in = (InputStream) url.getContent();
            StringBuilder b = new StringBuilder();
            while (in.available() > 0) {
                b.append((char) in.read());
            }
            in.close();

            releaseExeURL = new URL(b.toString());
            LOG.info("Latest available version download: "
                + releaseExeURL.toExternalForm());
        } catch (MalformedURLException e) {
            LOG.verbose(e);
        } catch (IOException e) {
            LOG.verbose(e);
        }
        if (releaseExeURL == null) {
            // Fallback to standart settings
            try {
                releaseExeURL = new URL(settings.releaseExeURL);
            } catch (MalformedURLException e) {
                LOG.error("Invalid release exec download location", e);
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
            LOG.warn("Executing: " + c);
            c += "\"";
            Runtime.getRuntime().exec(c);
        } catch (IOException e) {
            LOG.error("Unable to start update exe at " + file.getAbsolutePath()
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