/* $Id: UpdateChecker.java,v 1.27 2006/04/29 00:16:36 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.DownloadUpdateDialog;

/**
 * A Thread that checks for updates on powerfolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class UpdateChecker extends Thread {
    private static Logger log = Logger.getLogger(UpdateChecker.class);
    protected Controller controller;
    protected UpdateSetting settings;
    private static boolean downloadingVersion = false;
    private static boolean alreadyDownloaded = false;

    public UpdateChecker(Controller controller, UpdateSetting settings) {
        super("Update checker");
        Reject.ifNull(controller, "Controller is null");
        Reject.ifNull(settings, "Settings are null");
        this.controller = controller;
        this.settings = settings;
    }

    public void run() {
        if (controller.isTester()) {
            checkForNewDevBuild();
        } else {
            checkForNewRelease();
        }
    }

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

            String text = Translation.getTranslation("dialog.updatecheck.text",
                Controller.PROGRAM_VERSION, newerVersion);

            List<String> options = new ArrayList<String>(4);

            String downloadAndUpdate = Translation
                .getTranslation("dialog.updatecheck.downloadAndUpdate");
            String gotoHomepage = Translation
                .getTranslation("dialog.updatecheck.gotoHomepage");
            String nothingNeverAsk = Translation
                .getTranslation("dialog.updatecheck.nothingNeverAsk");

            if (OSUtil.isWindowsSystem()) {
                options.add(downloadAndUpdate);
            }
            options.add(gotoHomepage);
            options.add(nothingNeverAsk);

            Object option = JOptionPane.showInputDialog(getParentFrame(), text,
                Translation.getTranslation("dialog.updatecheck.title"),
                JOptionPane.OK_CANCEL_OPTION, null, options.toArray(), options
                    .get(0));

            if (option == downloadAndUpdate) {
                URL releaseURL;
                try {
                    releaseURL = new URL(settings.releaseExeURL);
                } catch (MalformedURLException e) {
                    log.error(e);
                    return;
                }
                File targetFile = new File(Controller.getTempFilesLocation(),
                    "PowerFolder_Latest_Win32_Installer.exe");
                // Download
                boolean completed = downloadFromURL(releaseURL, targetFile,
                    settings.httpUser, settings.httpPassword);
                // And start
                if (completed) {
                    log.warn("Download completed. "
                        + targetFile.getAbsolutePath());
                    try {
                        FileUtils.executeFile(targetFile);
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            } else if (option == gotoHomepage) {
                try {
                    // Open explorer
                    BrowserLauncher.openURL("http://www.powerfolder.com");
                } catch (IOException e) {
                    log.verbose(e);
                }
            } else if (option == nothingNeverAsk) {
                // Never ask again
                controller.getPreferences().putBoolean(
                    "updatechecker.askfornewreleaseversion", false);
            }
        }

        if (newerVersion == null) {
            notifyNoUpdateAvailable();
        }
    }

    /**
     * Checks for new development build of application release at the remote
     * location
     */
    private void checkForNewDevBuild() {
        // Check for new development version
        if (!downloadingVersion && !alreadyDownloaded) {
            try {
                final Date newerVersionDate = newerDevelopmentVersionAvailable();
                if (newerVersionDate != null) {
                    downloadingVersion = true;
                    URL devURL = new URL(settings.developmentJarURL);
                    boolean downloadSuccesfull = downloadFromURL(devURL,
                        new File("PowerFolder.new.jar"), settings.httpUser,
                        settings.httpPassword);

                    // Check downloaded file
                    if (downloadSuccesfull) {
                        // Okay download ready
                        alreadyDownloaded = true;

                        if (controller.isConsoleMode()) {
                            System.out
                                .println(Translation
                                    .getTranslation("console.updatecheck.new_developmentversion"));
                            // Exit to make restart
                            controller.exit(107);
                        }

                        // Ask user about restart otherwise
                        Runnable runner = new Runnable() {
                            public void run() {
                                Object[] options = new Object[]{Translation
                                    .getTranslation("dialog.updatecheck.developmentversion.optionrestart")};
                                int option = JOptionPane
                                    .showOptionDialog(
                                        getParentFrame(),
                                        Translation
                                            .getTranslation(
                                                "dialog.updatecheck.developmentversion.text",
                                                controller.getBuildTime(),
                                                newerVersionDate),

                                        Translation
                                            .getTranslation("dialog.updatecheck.developmentversion.title"),
                                        JOptionPane.DEFAULT_OPTION,
                                        JOptionPane.INFORMATION_MESSAGE, null,
                                        options, options[0]);

                                if (option == 1) {
                                    controller.getPreferences().putBoolean(
                                        "development.autorestart", true);
                                }

                                // Exit to system with restart errorcode
                                controller.exit(107);
                            }
                        };
                        controller.getUIController().invokeLater(runner);
                    }
                } else {
                    log
                        .verbose("Not loading down development version, current version build: "
                            + controller.getBuildTime());

                    notifyNoDevelopmentUpdateAvailable();
                }
            } catch (MalformedURLException e) {
                log.error("Unable to check for new development version at '"
                    + settings.developmentJarURL + "'", e);
            } finally {
                downloadingVersion = false;
            }
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
            log.error("Unable to download from " + url, e);
            return false;
        }

        // Build update download dialog
        DownloadUpdateDialog dlDialog = null;
        if (controller.isUIOpen()) {
            dlDialog = new DownloadUpdateDialog(controller);
        }

        log.warn("Downloading latest version from " + con.getURL());

        File tempFile = new File(destFile.getParentFile(), "(downloading) "
            + destFile.getName());
        try {
            // Copy/Download from URL^
            con.connect();
            FileUtils.copyFromStreamToFile(con.getInputStream(), tempFile,
                dlDialog != null ? dlDialog.getStreamCallback() : null, con
                    .getContentLength());
        } catch (IOException e) {
            log.warn("Unable to download from " + url, e);
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
            log.verbose(e);
            return null;
        }
        try {
            InputStream in = (InputStream) url.getContent();
            String latestVersion = "";
            while (in.available() > 0) {
                latestVersion += (char) in.read();
            }

            if (latestVersion != null) {
                log.info("Latest available version: " + latestVersion);

                if (compareVersions(latestVersion, Controller.PROGRAM_VERSION))
                {
                    log.warn("Latest version is newer than this one");
                    return latestVersion;
                }
                log.info("This version is up-to-date");
            }

        } catch (IOException e) {
            log.verbose(e);
        }
        return null;
    }

    /**
     * Comparse two version string which have the format "x.x.x aaa".
     * <p>
     * The last " aaa" is optional.
     * 
     * @param versionStr1
     * @param versionStr2
     * @return true if versionStr1 is greater than versionStr2
     */
    private static boolean compareVersions(String versionStr1,
        String versionStr2)
    {
        Reject.ifNull(versionStr1, "Version1 is null");
        Reject.ifNull(versionStr2, "Version2 is null");

        versionStr1 = versionStr1.trim();
        versionStr2 = versionStr2.trim();

        int major1 = 0;
        int minor1 = 0;
        int bugfix1 = 0;
        String addition1 = "";
        int addStart1 = versionStr1.indexOf(' ');
        if (addStart1 >= 0) {
            // Get addition text "x.x.x additionaltext"
            addition1 = versionStr1.substring(addStart1 + 1, versionStr1
                .length());
            versionStr1 = versionStr1.substring(0, addStart1);
        }

        StringTokenizer nizer1 = new StringTokenizer(versionStr1, ".");
        try {
            major1 = Integer.valueOf(nizer1.nextToken()).intValue();
        } catch (Exception e) {
        }
        try {
            minor1 = Integer.valueOf(nizer1.nextToken()).intValue();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            bugfix1 = Integer.valueOf(nizer1.nextToken()).intValue();
        } catch (Exception e) {
        }

        int major2 = 0;
        int minor2 = 0;
        int bugfix2 = 0;
        String addition2 = "";
        int addStart2 = versionStr2.indexOf(' ');
        if (addStart2 >= 0) {
            // Get addition text "x.x.x additionaltext"
            addition2 = versionStr2.substring(addStart2 + 1, versionStr2
                .length());
            versionStr2 = versionStr2.substring(0, addStart2);
        }

        StringTokenizer nizer2 = new StringTokenizer(versionStr2, ".");
        try {
            major2 = Integer.valueOf(nizer2.nextToken()).intValue();
        } catch (Exception e) {
        }
        try {
            minor2 = Integer.valueOf(nizer2.nextToken()).intValue();
        } catch (Exception e) {
        }
        try {
            bugfix2 = Integer.valueOf(nizer2.nextToken()).intValue();
        } catch (Exception e) {
        }

        // Actually check
        if (major1 == major2) {
            if (minor1 == minor2) {
                if (bugfix1 == bugfix2) {
                    return addition1.length() < addition2.length();
                }
                return bugfix1 > bugfix2;
            }
            return minor1 > minor2;
        }
        return major1 > major2;
    }

    /**
     * Answers if there is a newer development version available.
     * 
     * @return the Date of the newer version or null if no new version is
     *         available
     */
    protected Date newerDevelopmentVersionAvailable() {
        URL devURL;
        try {
            devURL = new URL(settings.developmentJarURL);
        } catch (MalformedURLException e) {
            // Should never happen
            log.error(e);
            return null;
        }
        URLConnection con;
        try {
            con = devURL.openConnection();
        } catch (IOException e) {
            // Should never happen
            log.warn("Unable to check for newer development version at "
                + settings.developmentJarURL, e);
            return null;
        }
        Date devAvailable = new Date(con.getLastModified());
        log.verbose("Latest development build available on net: "
            + devAvailable);

        boolean newerAvail = controller.getBuildTime() != null
            && controller.getBuildTime().before(devAvailable)
            && con.getContentLength() > settings.minimumJarSize;

        return newerAvail ? devAvailable : null;
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
     * Notifies user that no development update is available
     * <p>
     * No actual implementation. Included for override in
     * {@link ManuallyInvokedUpdateChecker}
     */
    protected void notifyNoDevelopmentUpdateAvailable() {
        // Do nothing here
        // Method included for override in ManuallyInvokedUpdateChecker
    }

    /**
     * Determines if the application should check for a newer version
     * 
     * @return true if yes, false if no
     */
    protected boolean shouldCheckForNewerVersion() {
        return controller.getPreferences().getBoolean(
            "updatechecker.askfornewreleaseversion", true);
    }

    /**
     * Retrieves the Frame that is the current parent
     * 
     * @return Parent JFrame
     */
    protected JFrame getParentFrame() {
        return controller.getUIController().getMainFrame().getUIComponent();
    }

    /**
     * Contains settings for the updatecheck.
     */
    public static class UpdateSetting {
        public String versionCheckURL = "http://checkversion.powerfolder.com/PowerFolder_LatestVersion.txt";
        public String releaseExeURL = "http://download.powerfolder.com/PowerFolder_Latest_Win32_Installer.exe";
        public String developmentJarURL = "http://webstart.powerfolder.com/development/PowerFolder.jar";

        // Minimum size of a jar to start DL
        public int minimumJarSize = 2024 * 1024;

        public String httpUser;
        public String httpPassword;
    }
}