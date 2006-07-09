/* $Id: Util.java,v 1.64 2006/04/30 19:37:05 schaatser Exp $
 */
package de.dal33t.powerfolder.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang.StringUtils;

import snoozesoft.systray4j.SysTrayMenu;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/**
 * Util helper class.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.64 $
 */
public class Util {

    // The local offset to UTC time in MS
    private static final long TIMEZONE_OFFSET_TO_UTC_MS = ((Calendar
        .getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(
        Calendar.DST_OFFSET)));

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
     * Converts an int to a 4 bytes arrays
     * 
     * @param i
     * @return
     */
    public static byte[] convert2Bytes(int i) {
        byte[] b = new byte[4];

        b[3] = (byte) (i & 0xFF);
        b[2] = (byte) (0xFF & (i >> 8));
        b[1] = (byte) (0xFF & (i >> 16));
        b[0] = (byte) (0xFF & (i >> 24));
        return b;
    }

    /**
     * Converts an arry of bytes to an int
     * 
     * @param b
     * @return
     */
    public static int convert2Int(byte[] b) {
        int w = 0;
        for (int i = 0; i < b.length; i++) {
            w <<= 8;
            if (b[i] < 0) {
                w += b[i] + 256;
            } else {
                w += b[i];
            }
        }
        return w;
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
     * Converts a array of members into a array of memberinfos calling the
     * getInfo method on each
     * 
     * @param members
     * @return
     */
    public static MemberInfo[] asMemberInfos(Member[] members) {
        if (members == null) {
            throw new NullPointerException("Memebers is null");
        }
        MemberInfo[] memberInfos = new MemberInfo[members.length];
        for (int i = 0; i < members.length; i++) {
            memberInfos[i] = members[i].getInfo();
        }
        return memberInfos;
    }

    /**
     * Answers if this is a temporary download file
     * 
     * @param file
     * @return
     */
    public static boolean isTempDownloadFile(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        String fileName = file.getName();
        return fileName.startsWith("(incomplete) ");
    }

    /**
     * Answers if the file is a placeholder file
     * 
     * @param file
     * @return
     */
    public static boolean isPlaceHolderFile(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        return file.getName().endsWith(".pf") && file.length() == 0;
    }

    /**
     * Answers if the file is a placeholder file
     * 
     * @param file
     * @return
     */
    public static boolean isPlaceHolderFile(FileInfo file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        return file.getFilenameOnly().endsWith(".pf");
    }

    /**
     * Answers if this file is a completed download file, means there exists a
     * targetfile with full name
     * 
     * @param file
     * @return
     */
    public static boolean isCompletedTempDownloadFile(File file) {
        if (!isTempDownloadFile(file)) {
            return false;
        }
        // String targetFilename = file.getName().substring(11);
        String targetFilename = file.getName().substring(13);
        File targetFile = new File(file.getParentFile(), targetFilename);
        return targetFile.exists() && (targetFile.length() == file.length());
    }

    /**
     * Answers if this file is the windows desktop.ini
     * 
     * @param file
     * @return
     */
    public static boolean isDesktopIni(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        return file.getName().equalsIgnoreCase("DESKTOP.INI");
    }

    /**
     * Checks if the file is a valid zipfile
     * 
     * @param file
     * @return
     */
    public static boolean isValidZipFile(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        try {
            new ZipFile(file);
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
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
     * Answers if current system is running windows
     * 
     * @return
     */
    public static boolean isWindowsSystem() {
        String os = System.getProperty("os.name");
        return (os != null) ? os.toLowerCase().indexOf("windows") >= 0 : false;
    }

    /**
     * Answers if the operating system is win Me or older (98, 95)
     * 
     * @return
     */
    public static boolean isWindowsMEorOlder() {
        String os = System.getProperty("os.name");
        return os.endsWith("Me") || os.endsWith("98") || os.endsWith("95");
    }

    /**
     * Answers if the operating system is mac os
     * 
     * @return
     */
    public static boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("mac");
    }

    /**
     * Determines if this is a web start via Java WebStart
     * 
     * @return true if started via web
     */
    public static boolean isWebStart() {
        return !System.getProperty("using.webstart", "false").equals("false");
    }

    /**
     * Systray only on win2000 and newer. win 98/ME gives a "could not create
     * main-window error"
     */
    public static boolean isSystraySupported() {
        return Util.isWindowsSystem() && !Util.isWindowsMEorOlder()
            && SysTrayMenu.isAvailable();
    }

    /**
     * Copies a file
     * 
     * @param from
     * @param to
     *            if file exists it will be overwritten!
     * @throws IOException
     */
    public static void copyFile(File from, File to) throws IOException {
        if (from == null) {
            throw new NullPointerException("From file is null");
        }
        if (!from.exists()) {
            throw new IOException("From file does not exists "
                + from.getAbsolutePath());
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("cannot copy onto itself");
        }
        copyFromStreamToFile(
            new BufferedInputStream(new FileInputStream(from)), to);
    }

    /**
     * Copies a file to disk from a stream. Overwrites the target file if exists
     * 
     * @see #copyFromStreamToFile(InputStream, File, StreamCallback)
     * @param in
     *            the input stream
     * @param to
     *            the file where the stream should be written in
     * @throws IOException
     */
    public static void copyFromStreamToFile(InputStream in, File to)
        throws IOException
    {
        copyFromStreamToFile(in, to, null, 0);
    }

    /**
     * Copies a file to disk from a stream. Overwrites the target file if
     * exists. The processe may be observed with a stream callback
     * 
     * @param in
     *            the input stream
     * @param to
     *            the file wher the stream should be written in
     * @param callback
     *            the callback to get information about the process, may be left
     *            null
     * @param totalAvailableBytes
     *            the byte total available
     * @throws IOException
     *             any io excetion or the stream read is broken by the callback
     */
    public static void copyFromStreamToFile(InputStream in, File to,
        StreamCallback callback, int totalAvailableBytes) throws IOException
    {
        if (in == null) {
            throw new NullPointerException("InputStream file is null");
        }
        if (to == null) {
            throw new NullPointerException("To file is null");
        }
        if (to.exists()) {
            if (!to.delete()) {
                throw new IOException("Unable to delete old file "
                    + to.getAbsolutePath());
            }
        }
        if (!to.createNewFile()) {
            throw new IOException("Unable to create file "
                + to.getAbsolutePath());
        }
        if (!to.canWrite()) {
            throw new IOException("Unable to write to " + to.getAbsolutePath());
        }

        OutputStream out = new BufferedOutputStream(new FileOutputStream(to));

        byte[] buffer = new byte[1024];
        int read;
        int position = 0;
        try {
            do {
                read = in.read(buffer);
                if (read < 0) {
                    break;
                }
                out.write(buffer, 0, read);
                position += read;
                if (callback != null) {
                    // Execute callback
                    boolean breakStream = callback.streamPositionReached(
                        position, totalAvailableBytes);
                    if (breakStream) {
                        throw new IOException(
                            "Stream read break requested by callback. "
                                + callback);
                    }
                }
            } while (read >= 0);
        } finally {
            // Close streams
            in.close();
            out.close();
        }
    }

    /**
     * Execute the file, uses rundll approach to start on windows
     * 
     * @param file
     * @throws IOException
     */
    public static final void executeFile(File file) throws IOException {
        if (isMacOS()) {
            Runtime.getRuntime().exec("open " + file.getAbsolutePath());
        } else if (isWindowsSystem()) {
            URL url = new URL("file://" + file.getAbsolutePath());
            // Use rundll approach
            Runtime.getRuntime().exec(
                "rundll32 url.dll,FileProtocolHandler " + url.toString());
        } else {
            LOG.error("Unable to start file '" + file
                + "', system not supported");
        }
    }

    /**
     * Makes a file hidden on windows system
     * 
     * @param file
     * @return
     */
    public static boolean makeHiddenOnWindows(File file) {
        if (!isWindowsSystem()) {
            return false;
        }
        try {
            Process proc = Runtime.getRuntime().exec(
                "attrib.exe +h \"" + file.getAbsolutePath() + "\"");
            proc.waitFor();
            return true;
        } catch (IOException e) {
            LOG.verbose(e);
            return false;
        } catch (InterruptedException e) {
            LOG.verbose(e);
            return false;
        }
    }

    /**
     * Sets file attributes on windows system
     * 
     * @param file
     * @return
     */
    public static boolean setAttributesOnWindows(File file, boolean hidden,
        boolean system)
    {
        if (!isWindowsSystem() || isWindowsMEorOlder()) {
            // Not set attributes on non-windows systems or win ME or older
            return false;
        }
        try {
            Process proc = Runtime.getRuntime().exec(
                "attrib " + (hidden ? "+h" : "") + " " + (system ? "+s" : "")
                    + " \"" + file.getAbsolutePath() + "\"");
            proc.getOutputStream();
            proc.waitFor();
            return true;
        } catch (IOException e) {
            LOG.verbose(e);
            return false;
        } catch (InterruptedException e) {
            LOG.verbose(e);
            return false;
        }
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
        if (!Util.isWindowsSystem()) {
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
            copyFromStreamToFile(in, target);
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
        if (!Util.isWindowsSystem()) {
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
            Util.copyFromStreamToFile(in, createShortCutVBS);
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
        if (!Util.isWindowsSystem()) {
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
     * Converts a date to the value in UTC
     * 
     * @param date
     * @return
     */
    public static long convertToUTC(Date date) {
        return date.getTime() - TIMEZONE_OFFSET_TO_UTC_MS;
    }

    /**
     * Chops a date (in MS) to a (lower) precision to make cross plattform
     * modified values comparable. All millisecond precision will be lost
     * 
     * @param date
     * @return
     */
    public static long convertToGlobalPrecision(long date) {
        return date / 2000 * 2000;
    }

    /**
     * Loads an invitation from a file. Return the invitation or null if not
     * possible to load the file
     * 
     * @param file
     * @return
     */
    public static Invitation loadInvitation(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            return null;
        }
        LOG.verbose("Loading invitation " + file);
        try {
            FileInputStream fIn = new FileInputStream(file);
            return loadInvitation(fIn);
        } catch (IOException e) {
            LOG.error("Unable to read invitation file stream", e);
        }
        return null;
    }

    /**
     * Loads an invitation from a file. Return the invitation or null if not
     * possible to load the file
     * 
     * @param file
     * @return
     */
    public static Invitation loadInvitation(InputStream in) {
        if (in == null) {
            throw new NullPointerException("File is null");
        }
        LOG.verbose("Loading invitation from " + in);
        try {
            ObjectInputStream oIn = new ObjectInputStream(in);
            Invitation invitation = (Invitation) oIn.readObject();

            if (invitation.invitor == null) {
                // Old file version, has another member info at end
                // New invitation files have memberinfo inclueded in invitation
                try {
                    MemberInfo from = (MemberInfo) oIn.readObject();
                    if (invitation.invitor == null) {
                        // Use invitation
                        invitation.invitor = from;
                    }
                } catch (IOException e) {
                    // Ingnore
                }
            }

            in.close();

            return invitation;
        } catch (ClassCastException e) {
            LOG.error("Unable to read invitation file stream", e);
        } catch (IOException e) {
            LOG.error("Unable to read invitation file stream", e);
        } catch (ClassNotFoundException e) {
            LOG.error("Unable to read invitation file stream", e);
        }
        return null;
    }

    public static void saveInvitation(Invitation invitation, File file) {
        try {
            saveInvitation(invitation, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            LOG.error("Unable to read invitation file stream", e);
        }
    }

    public static void saveInvitation(Invitation invitation, OutputStream out) {
        LOG.verbose("Saving invitation to " + out);
        ObjectOutputStream oOut;
        try {
            oOut = new ObjectOutputStream(out);
            oOut.writeObject(invitation);
            oOut.close();
        } catch (IOException e) {
            LOG.error("Unable to save invitation file stream", e);
        }
    }

    /**
     * Creates a file filter for powerfolder invitations
     * 
     * @return
     */
    public static FileFilter createInvitationsFilefilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".invitation") || f.isDirectory();
            }

            public String getDescription() {
                return Translation
                    .getTranslation("invitationfiles.description");
            }
        };
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
     * compares to ip addresses.
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