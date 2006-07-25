/* $Id: Util.java,v 1.64 2006/04/30 19:37:05 schaatser Exp $
 */
package de.dal33t.powerfolder.util;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Util helper class.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.64 $
 */
public class Util {
   
    private static final Logger LOG = Logger.getLogger(Util.class);

    // No instance possible
    private Util() {
    }

    public static String removeInvalidFilenameChars(String folderName) {
        String invalidChars = "/\\:*?\"<>|";
        for (int i = 0; i < invalidChars.length(); i++) {
            char c = invalidChars.charAt(i);
            while (folderName.indexOf(c) != -1) {
                int index = folderName.indexOf(c);
                folderName = folderName.substring(0, index)
                    + folderName.substring(index + 1, folderName.length());
            }
        }
        return folderName;
    }

    public static final boolean equals(Object a, Object b) {
        if (a == null) {
            // a == null here
            return b == null;

        }
        return a.equals(b);
    }

    /**
     * Calculates the total size in bytes of a filelist
     * 
     * @param files
     * @param countDeleted
     *            if deleted files should be counted to the total size
     * @return
     */
    public static long calculateSize(FileInfo[] files, boolean countDeleted) {
        if (files == null || files.length == 0) {
            return 0;
        }
        long totalSize = 0;
        for (int i = 0; i < files.length; i++) {
            if ((countDeleted && files[i].isDeleted()) || !files[i].isDeleted())
            {
                // do not count if file is deleted and count-deleted is enabled
                totalSize += files[i].getSize();
            }
        }
        return totalSize;
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
    public static boolean compareVersions(String versionStr1, String versionStr2)
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
     * is minimal java version 1.5
     */
    public static boolean isMinJava15() {
        try {
            String version = System.getProperty("java.version");
            int index = version.indexOf(".");
            int majorVersion = Integer.parseInt(version.substring(0, index));
            if (majorVersion > 1) {
                return true;
            }
            int index2 = version.indexOf(".", index + 1);
            int minorVersion = Integer.parseInt(version.substring(index + 1,
                index2));
            if (minorVersion >= 5) {
                return true;
            }
        } catch (Exception e) {
            // no access or weird java vm
        }
        return false;
    }

   
   

    /**
     * Starts default mail program on <B>Windows</B> with prepared message.
     * Note that the body cannot be long. Parameters can be ommitted (give null
     * as parameter). Util.isWindowsSystem() must return true for this to work.
     * This is tested on win2000 only for now. If no <B>to</B> parameter is set
     * the SendTo.exe program wil set a default (something like
     * mail@domain.com).
     * 
     * @param to
     *            The optional recipent (email address) to send the mail to. If
     *            no <B>to</B> parameter is set the SendTo.exe program wil set
     *            a default (something like mail@domain.com).
     * @param subject
     *            The optional subject of the mail
     * @param body
     *            The optional body of the mail, cannot be very long please test
     *            before use!
     * @param attachment
     *            The optional file to attatch
     * @see Util#isWindowsSystem()
     */
    public static boolean sendMail(String to, String subject, String body,
        File attachment)
    {
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }
        // sendto.exe usage :
        // sendto.exe -files <file1> <file2> ... -body <content> -to <email
        // address> -subject <content>
        // example : sendto.exe -files "c:\my files\file1.ppt" c:\document.doc

        // prepare params to give to SendTo.exe program
        String params = "";
        if (!StringUtils.isBlank(to)) {
            params += " -to " + to;
        }

        if (!StringUtils.isBlank(subject)) {
            params += " -subject \"" + subject + "\"";
        }

        if (!StringUtils.isBlank(body)) {
            params += " -body \"" + body + "\"";
        }

        if (attachment != null) {
            if (!attachment.exists()) {
                throw new IllegalArgumentException("sendmail file attachment ("
                    + attachment.getAbsolutePath() + ")does not exists");
            }
            if (!attachment.canRead()) {
                throw new IllegalArgumentException(
                    "sendmail file attachment not ("
                        + attachment.getAbsolutePath() + ") readable");
            }
            params += " -files \"" + attachment.getAbsolutePath() + "\"";
        }
        // extract exe file from jar

        try {
            File sendto = copyResourceTo("SendTo.exe", "SendToApp/Release",
                Controller.getTempFilesLocation(), true);

            Runtime.getRuntime().exec(
                sendto.getAbsolutePath() + " " + params + "");
            LOG.debug("Mail send");
            return true;
        } catch (IOException e) {
            LOG.warn("Unable to send mail " + e.getMessage());
            LOG.verbose(e);
            return false;
        }
    }

    /**
     * @return The created file or null if resource not found
     * @param resource
     *            filename of the resource
     * @param altLocation
     *            possible alternative (root is tried first) location (directory
     *            structure like etc/files)
     * @param destination
     *            Directory where to create the file (must exists and must be a
     *            directory))
     * @param deleteOnExit
     *            indicates if this file must be deleted on program exit
     */
    public static File copyResourceTo(String resource, String altLocation,
        File destination, boolean deleteOnExit)
    {
        if (!destination.exists()) {
            throw new IllegalArgumentException("destination must exists");
        }
        if (!destination.isDirectory()) {
            throw new IllegalArgumentException(
                "destination must be a directory");
        }
        InputStream in = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(resource);
        if (in == null) {
            LOG.verbose("Unable to find resource: " + resource);
            // try harder
            in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(altLocation + "/" + resource);
            if (in == null) {
                LOG.warn("Unable to find resource: " + altLocation + "/"
                    + resource);
                return null;
            }
        }
        File target = new File(destination, resource);
        if (deleteOnExit) {
            target.deleteOnExit();
        }
        try {
            FileUtils.copyFromStreamToFile(in, target);
        } catch (IOException ioe) {
            LOG.warn("Unable to create target for resource: " + target);
            return null;
        }
        return target;
    }

    /**
     * Creates a desktop shortcut. currently only available on windows systems
     * 
     * @param shortcutName
     * @param shortcutTarget
     * @return true if succeeded
     */
    public static boolean createDesktopShortcut(String shortcutName,
        File shortcutTarget)
    {
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }

        LOG.verbose("Creating desktop shortcut to "
            + shortcutTarget.getAbsolutePath());

        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (!desktop.exists()) {
            LOG.warn("Unable to create desktop shortcut '" + shortcutName
                + "' to " + shortcutTarget + ". Desktop not found at "
                + desktop.getAbsolutePath());
            return false;
        }

        File folderLnk = new File(desktop, shortcutName + ".lnk");
        if (folderLnk.exists()) {
            LOG.verbose("Desktop shortcut '" + shortcutName
                + "' already exists");
            return false;
        }

        InputStream in = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("etc/createshortcut.vbs");
        if (in == null) {
            // try harder
            in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("createshortcut.vbs");
            if (in == null) {
                LOG.warn("Unable to create desktop shortcut '" + shortcutName
                    + "'");
                return false;
            }
        }

        File createShortCutVBS = null;
        try {
            createShortCutVBS = new File("createshortcut.vbs");
            createShortCutVBS.deleteOnExit();
            FileUtils.copyFromStreamToFile(in, createShortCutVBS);
            Process vbProc = Runtime.getRuntime().exec(
                "cscript \"" + createShortCutVBS.getAbsolutePath() + "\" \""
                    + shortcutName + "\" \"" + shortcutTarget.getAbsolutePath()
                    + "\"");
            vbProc.waitFor();
            createShortCutVBS.delete();
            LOG.debug("Desktop shortcut for "
                + shortcutTarget.getAbsolutePath() + " created");
            return true;
        } catch (IOException e) {
            LOG.warn("Unable to create desktop shortcut '" + shortcutName
                + "'. " + e.getMessage());
            LOG.verbose(e);
        } catch (InterruptedException e) {
            LOG.warn("Unable to create desktop shortcut '" + shortcutName
                + "'. " + e.getMessage());
            LOG.verbose(e);
        }
        return false;
    }

    /**
     * Removes a desktop shortcut. currently only available on windows systems
     * 
     * @param shortcutName
     * @return true if succeeded
     */
    public static boolean removeDesktopShortcut(String shortcutName) {
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }

        LOG.verbose("Removing desktop shortcut: " + shortcutName);

        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        if (!desktop.exists()) {
            LOG.warn("Unable to remove desktop shortcut '" + shortcutName
                + "'. Desktop not found at " + desktop.getAbsolutePath());
            return false;
        }

        File folderLnk = new File(desktop, shortcutName + ".lnk");
        if (folderLnk.exists() && folderLnk.isFile()) {
            return folderLnk.delete();
        }
        return false;
    }

    

    /**
     * Returns the plain url content as string
     * 
     * @param url
     * @return
     */
    public static String getURLContent(URL url) {
        if (url == null) {
            throw new NullPointerException("URL is null");
        }
        try {
            Object content = url.getContent();
            if (!(content instanceof InputStream)) {
                LOG.error("Unable to get content from " + url
                    + ". content is of type " + content.getClass().getName());
                return null;
            }
            InputStream in = (InputStream) content;

            StringBuffer buf = new StringBuffer();
            while (in.available() > 0) {
                buf.append((char) in.read());
            }
            return buf.toString();
        } catch (IOException e) {
            LOG.error(
                "Unable to get content from " + url + ". " + e.toString(), e);
        }
        return null;
    }

    /**
     * Place a String on the clipboard
     * 
     * @param aString
     *            the string to place in the clipboard
     */
    public static void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, new ClipboardOwner() {
            public void lostOwnership(Clipboard aClipboard,
                Transferable contents)
            {
                // Ignore
            }
        });
    }

    /**
     * Encodes a url fragment, thus special characters are tranferred into a url
     * compatible style
     * 
     * @param aURLFragment
     * @return
     */
    public static String endcodeForURL(String aURLFragment) {
        String result = null;
        try {
            result = URLEncoder.encode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    /**
     * Decodes a url fragment, thus special characters are tranferred from a url
     * compatible style
     * 
     * @param aURLFragment
     * @return
     */
    public static String decodeFromURL(String aURLFragment) {
        String result = null;
        try {
            result = URLDecoder.decode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    /**
     * compares two ip addresses.
     * 
     * @param ip1
     * @param ip2
     * @return true if different, false otherwise.
     */

    public static boolean compareIpAddresses(byte[] ip1, byte[] ip2) {
        for (int i = 0; i < ip1.length; i++) {
            if (ip1[i] != ip2[i]) {
                return true;
            }
        }
        return false;
    }
}