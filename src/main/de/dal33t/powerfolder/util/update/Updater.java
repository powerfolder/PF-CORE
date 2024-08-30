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
 * $Id: Updater.java 6236 2008-12-31 15:44:10Z tot $
 */
package de.dal33t.powerfolder.util.update;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Thread that checks for updates on powerfolder
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class Updater extends Thread {
    private static Logger LOG = Logger.getLogger(Updater.class.getName());
    protected Controller controller;
    protected UpdateSetting settings;
    private UpdaterHandler handler;

    public Updater(Controller controller, UpdaterHandler handler) {
        this(controller, controller.getUpdateSettings(), handler);
    }

    public Updater(Controller controller, UpdateSetting settings,
        UpdaterHandler handler)
    {
        super("Update checker");
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(settings, "Settings are null");
        Reject.ifNull(handler, "Handler is null");

        this.controller = controller;
        this.settings = settings;
        this.handler = handler;
    }

    public void run() {
        checkForNewRelease();
    }

    /**
     * Installs a periodical (usually once per hour) update check with the given
     * handler.
     *
     * @param controller
     * @param updateHandler
     */
    public static void installPeriodicalUpdateCheck(
        final Controller controller, final UpdaterHandler updateHandler)
    {
        Reject.ifNull(controller, "Controller is null");
        // PFS-2461: disable all updates, needed for MSI installation
        if (!ConfigurationEntry.ENABLE_UPDATE.getValueBoolean(controller)) {
            return;
        }
        TimerTask updateCheckTask = new TimerTask() {
            @Override
            public void run() {
                // Check for an update
                new Updater(controller, updateHandler).start();
            }
        };
        // Check after 15 seconds on start and every hour
        controller.scheduleAndRepeat(updateCheckTask,
            1000L * 15,1000L * 60 * Constants.UPDATE_CHECK_PERIOD_MINUTES);

    }

    /**
     * Checks for new application release at the remote location
     */
    private void checkForNewRelease() {
        LOG.fine("Checking for newer version");
        if (!handler.shouldCheckForNewVersion()) {
            return;
        }
        final String newerVersion = newerReleaseVersionAvailable();
        if (newerVersion != null) {
            handler.newReleaseAvailable(new UpdaterEvent(this, newerVersion,
                getReleaseExeURL()));
        } else {
            handler.noNewReleaseAvailable(new UpdaterEvent(this));
        }
    }

    /**
     * Method that downloads and installs the version of PowerFolder from the
     * given URL.
     *
     * @param url
     * @param progressCallback
     * @param silentUpdate
     * @return the updater Process or null if failed.
     */
    public Process downloadAndUpdate(URL url, StreamCallback progressCallback,
        boolean silentUpdate)
    {
        Path releaseExe = download(url, progressCallback);
        if (releaseExe == null) {
            return null;
        }

        try {
            // Signaturprüfung
            if (!verifySignatureWithPowerShell(releaseExe)) {
                Files.delete(releaseExe);
                LOG.warning("Signature not existing on " + releaseExe + ". The file has been deleted.");
                return null;
            }
        } catch (Exception e) {
            LOG.warning("Signature not existing on " + releaseExe + ". " + e);
            return null;
        }

        return openReleaseExe(releaseExe, silentUpdate);
    }

    private boolean verifySignatureWithPowerShell(Path exePath) throws IOException, InterruptedException {
        // PowerShell-Befehl zur Überprüfung der Authenticode-Signatur ohne sprachabhängige Ausdrücke
        String command = String.format(
                "powershell.exe -Command \"& { " +
                        "$signature = Get-AuthenticodeSignature -FilePath '%s'; " +
                        "if ($signature.Status -eq 'Valid') { " +
                        "Write-Output 'Valid'; " +
                        "} elseif ($signature.Status -eq 'UnknownError' -and $signature.StatusMessage -match '(certificate|zeitstempel|timestamp)') { " +
                        "Write-Output 'Valid-Ignoring-Timestamp'; " +
                        "} else { " +
                        "Write-Output 'Invalid'; " +
                        "}; " +
                        "}\"",
                exePath.toString().replace("\\", "\\\\")
        );

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                LOG.fine(line); // Debugging-Ausgabe
                if (line.contains("Valid") || line.contains("Valid-Ignoring-Timestamp")) {
                    return true; // Signatur ist gültig, Zeitstempel ggf. ignoriert
                }
            }

            // Fehlerausgabe lesen
            while ((line = errorReader.readLine()) != null) {
                LOG.warning("Error: " + line); // Debugging-Ausgabe der Fehlernachrichten
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            LOG.warning("PowerShell script failed with exit code " + exitCode);
        }

        return false; // Signatur ist ungültig oder Fehler aufgetreten
    }

    /**
     * Downloads a new powerfolder release file from a URL.
     *
     * @param url
     *            the url
     * @param progressCallback
     *            the callback to monitor the download.
     * @return the downloaded file if succeeded or null if failed
     */
    public Path download(URL url, StreamCallback progressCallback) {
        URLConnection con;
        String filename = url.getFile();
        if (StringUtils.isBlank(filename)) {
            filename = "PowerFolder_Latest_Win32_Installer.exe";
        }
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }
        Path targetFile = Controller.getTempFilesLocation().resolve(filename);
        try {
            con = url.openConnection();
            con.connect();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to download from " + url, e);
            return null;
        }

        LOG.log(Level.INFO, "Downloading latest version from "
            + con.getURL());
        Path tempFile = targetFile.getParent().resolve("(downloading) "
            + targetFile.getFileName().toString());
        try {
            // Copy/Download from URL
            con.connect();
            PathUtils.copyFromStreamToFile(con.getInputStream(), tempFile, progressCallback, con.getContentLength());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Unable to download from " + url, e);
            return null;
        }

        try {
            // Rename file and set modified/build time
            Files.deleteIfExists(targetFile);
            Files.move(tempFile, targetFile);
            Files.setLastModifiedTime(targetFile, FileTime.fromMillis(con.getLastModified()));

            if (targetFile.getFileName().toString().toLowerCase().endsWith("jar")) {
                // Additional jar check
                if (!PathUtils.isValidZipFile(targetFile)) {
                    // Invalid file downloaded
                    Files.delete(targetFile);
                    return null;
                }
            }
        } catch (IOException ioe) {
            LOG.warning("Unable to download auto update: " + ioe);
            return null;
        }

        return targetFile;
    }

    /**
     * @return the newer program version available on the net. Otherwise returns
     *         null.
     */
    private String newerReleaseVersionAvailable() {
        String latestVersion = latestReleaseVersionAvailable();
        if (latestVersion == null) {
            LOG.warning("Unable to retrieve latest version from "
                + settings.versionCheckURL);
            return null;
        }
        if (Util.compareVersions(latestVersion, Controller.PROGRAM_VERSION)) {
            LOG.info("Latest available version (" + latestVersion + ") is newer than this version (" +
                    Controller.PROGRAM_VERSION + ")");
            return latestVersion;
        }
        LOG.fine("This version is up-to-date (" + Controller.PROGRAM_VERSION + ")");
        return null;
    }

    /**
     * @return the latest program version available on the net.
     * @private public because of test
     */
    public String latestReleaseVersionAvailable() {
        URL url;
        try {
            url = new URL(settings.versionCheckURL);
        } catch (MalformedURLException e) {
            LOG.log(Level.FINER, e.toString(), e);
            return null;
        }
        try {
            InputStream in = (InputStream) url.getContent();
            String latestVersion = "";
            int read;
            while ((read = in.read()) >= 0) {
                latestVersion += (char) read;
            }
            if (latestVersion != null) {
                latestVersion = latestVersion.trim();
                if (latestVersion.length() > 50) {
                    LOG.log(Level.WARNING,
                        "Received illegal response while checking latest available version from "
                            + settings.versionCheckURL + ": " + latestVersion);
                    return null;
                }
                LOG.fine("Latest available version: " + latestVersion + " @ "
                    + settings.versionCheckURL);
                return latestVersion;
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING,
                "Unable to retrieve latest available version for: "
                    + settings.versionCheckURL + ". " + e);
            LOG.log(Level.FINER, e.toString(), e);
        }
        return null;
    }

    /**
     * Returns the download URL for the latest program version
     *
     * @return
     */
    private URL getReleaseExeURL() {
        URL releaseExeURL = null;
        try {
            if (StringUtils.isNotBlank(settings.downloadLinkInfoURL)) {
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
            }
        } catch (MalformedURLException e) {
            LOG.log(Level.FINER, e.toString(), e);
        } catch (IOException e) {
            LOG.log(Level.FINER, e.toString(), e);
        }
        if (releaseExeURL == null) {
            // Fallback to standart settings
            try {
                releaseExeURL = new URL(settings.windowsExeURL);
            } catch (MalformedURLException e) {
                LOG.log(Level.SEVERE, "Invalid release exec download location",
                    e);
            }
        }
        return releaseExeURL;
    }

    private Process openReleaseExe(Path file, boolean updateSilently) {
        try {
            String c = "cmd.exe";
            c += " /c ";
            c += '"';
            c += file.toAbsolutePath().toString();
            if (updateSilently) {
                c += " /S";
            }
            c += '"';
            LOG.info("Executing: " + c);
            return Runtime.getRuntime().exec(c);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to start update exe at "
                + file.toAbsolutePath() + ". " + e, e);
            return null;
        }
    }
}