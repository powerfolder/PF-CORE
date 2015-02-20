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
 * $Id: Util.java 20555 2012-12-25 04:15:08Z glasgow $
 */
package de.dal33t.powerfolder.util;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.util.net.NetworkUtil.AllTrustingSSLManager;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.ShellLink;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Util helper class.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.64 $
 */
public class Util {

    /** Flag if awt is available */
    private static boolean awtAvailable;
    // Initalize awt check
    static {
        // Okay lets check if we have an AWT system
        try {
            Color col = Color.RED;
            col.brighter();

            SimpleAttributeSet warn = new SimpleAttributeSet();
            StyleConstants.setForeground(warn, Color.RED);

            // Okay we have AWT
            awtAvailable = true;
        } catch (Error e) {
            // ERROR ? Okay no AWT
            awtAvailable = false;
        }
    }

    private static final Logger LOG = Logger.getLogger(Util.class.getName());

    /**
     * Used building output as Hex
     */
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    // No instance possible
    private Util() {
    }

    /**
     * Answers if we have the AWT libs available
     *
     * @return
     */
    public static boolean isAwtAvailable() {
        return awtAvailable;
    }

    public static final boolean equals(Object a, Object b) {
        if (a == null) {
            // a == null
            return b == null;
        }
        if (b == null) {
            // a != null
            return false;
        }
        if (a == b) {
            return true;
        }
        return a.equals(b);
    }

    public static final boolean equalsIgnoreCase(String a, String b) {
        if (equals(a, b)) {
            return true;
        }
        return a.equalsIgnoreCase(b);
    }

    /**
     * @param relativeNameA
     * @param relativeNameB
     * @return true if the relative names are equals on this system. Respects
     *         {@link FileInfo#IGNORE_CASE}
     */
    public static final boolean equalsRelativeName(String relativeNameA,
        String relativeNameB)
    {
        if (relativeNameA == null) {
            // a == null
            return relativeNameB == null;
        }
        if (relativeNameB == null) {
            // a != null
            return false;
        }
        if (relativeNameA == relativeNameB) {
            return true;
        }
        if (FileInfo.IGNORE_CASE) {
            return relativeNameA.equalsIgnoreCase(relativeNameB);
        } else {
            return relativeNameA.equals(relativeNameB);
        }
    }

    /**
     * Safe-get for null char arrays to String conversion.
     *
     * @param chars
     * @return the chars as string. If the input is null output will be null
     */
    public static final String toString(char[] chars) {
        if (chars == null) {
            return null;
        }
        return new String(chars);
    }

    /**
     * Safe-get for null Strings to char array conversion.
     *
     * @param str
     * @return String as char array. If the input is null output will be null
     */
    public static final char[] toCharArray(String str) {
        if (str == null) {
            return null;
        }
        return str.toCharArray();
    }

    /**
     * @param email
     *            the email string to check.
     * @return true if the input is a valid email address.
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        int etIndex = email.indexOf('@');
        boolean orderOk = etIndex > 0 && email.lastIndexOf('.') > etIndex;
        if (!orderOk) {
            return false;
        }
        if (email.trim().contains(" ")) {
            // Whitespaces not allowed
            return false;
        }
        return true;
    }

    /**
     * @return the line feed characters depending on the system architecture.
     */
    public static String getLineFeed() {
        String lf = System.getProperty("line.separator");
        if (StringUtils.isNotBlank(lf)) {
            return lf;
        }
        if (OSUtil.isWindowsSystem()) {
            return "\r\n";
        } else {
            return "\n";
        }
    }

    public static boolean isMySelfPowerFolderComCloud(Controller controller) {
        // WHAT A MESS
        return Feature.CREDITS_SYSTEM.isEnabled();
    }

    /**
     * @param c
     * @param otherIsOnLAN
     * @return true, if this client may request parts from multiple sources
     */
    private static boolean allowSwarming(Controller c, boolean otherIsOnLAN) {
        Reject.ifNull(c, "Controller is null");
        return (ConfigurationEntry.USE_SWARMING_ON_INTERNET.getValueBoolean(c) && !otherIsOnLAN)
            || (ConfigurationEntry.USE_SWARMING_ON_LAN.getValueBoolean(c) && otherIsOnLAN);
    }

    /**
     * @param c
     * @param otherIsOnLAN
     * @return true, if this client may request parts and a file parts record
     */
    private static boolean allowDeltaSync(Controller c, boolean otherIsOnLAN) {
        Reject.ifNull(c, "Controller is null");
        return (ConfigurationEntry.USE_DELTA_ON_INTERNET.getValueBoolean(c) && !otherIsOnLAN)
            || (ConfigurationEntry.USE_DELTA_ON_LAN.getValueBoolean(c) && otherIsOnLAN);
    }

    public static boolean useSwarming(Controller c, Member other) {
        Reject.ifNull(c, "Controller is null");
        Reject.ifNull(other, "other is null!");
        Identity id = other.getIdentity();
        if (id == null) {
            return false;
        }
        return id.isSupportingPartTransfers()
            && allowSwarming(c, other.isOnLAN());
    }

    public static boolean useDeltaSync(Controller c, Download d) {
        Validate.notNull(c);
        if (d.getFile().getSize() < Constants.DELTA_SYNC_MIN_FILESIZE) {
            return false;
        }
        return allowDeltaSync(c, d.getPartner().isOnLAN());
    }

    /**
     * Retrieves the URL to an resource within PF.
     *
     * @param res
     *            the filename of the resource
     * @param altLocation
     *            possible alternative (root is tried first) location (directory
     *            structure like etc/files)
     * @return the URL to the resource or null if not possible
     */
    public static URL getResource(String res, String altLocation) {
        URL result = Thread.currentThread().getContextClassLoader()
            .getResource(res);
        if (result == null) {
            result = Thread.currentThread().getContextClassLoader()
                .getResource(altLocation + '/' + res);
        }
        if (result == null) {
            LOG.severe("Unable to load resource " + res + ". alt location "
                + altLocation);
        }
        return result;
    }

    /**
     * @return The created file or null if resource not found
     * @param resource
     *            filename of the resource
     * @param altLocation
     *            possible alternative (root is tried first) location (directory
     *            structure like etc/files)
     * @param destinationFile
     *            The file to create
     * @param forceOverwrite
     *            true if the resource should be copied even if not required.
     */
    public static Path copyResourceTo(String resource, String altLocation,
        Path destinationFile, boolean forceOverwrite, boolean quiet)
    {
        Reject.ifNull(resource, "Resoucse");
        try {
            // Step 1) Check existence of resource
            URL resURL = Thread.currentThread().getContextClassLoader()
                .getResource(resource);
            if (resURL == null && altLocation != null) {
                LOG.finer("Unable to find resource: " + resource);
                // try harder
                resURL = Thread.currentThread().getContextClassLoader()
                    .getResource(altLocation + '/' + resource);
            }
            if (resURL == null) {
                LOG.fine("Unable to find resource: " + altLocation + "/"
                    + resource);
                return null;
            }
            URLConnection resCon = resURL.openConnection();
            long lastMod = resCon.getLastModified();
            long length = resCon.getContentLength();

            // Step 2) Check if update/overwrite is required
            if (!forceOverwrite && Files.exists(destinationFile)) {
                boolean upToDate = length == Files.size(destinationFile)
                    && DateUtil.equalsFileDateCrossPlattform(lastMod,
                        Files.getLastModifiedTime(destinationFile).toMillis());
                if (upToDate) {
                    // No update required
                    LOG.fine("Not required to update " + resURL + " to "
                        + destinationFile);
                    return destinationFile;
                }
            }

            // Step 3) Actually copy
            InputStream in = resCon.getInputStream();
            Files.createDirectories(destinationFile);
            PathUtils.copyFromStreamToFile(in, destinationFile);
            // Preserver last mod for later caching.
            Files.setLastModifiedTime(destinationFile, FileTime.fromMillis(resCon.getLastModified()));
        } catch (IOException ioe) {
            if (quiet) {
                LOG.fine("Unable to create target for resource: "
                    + destinationFile);
            } else {
                LOG.warning("Unable to create target for resource: "
                    + destinationFile);
            }
            return null;
        }
        LOG.finer("created target for resource: " + destinationFile);
        return destinationFile;
    }

    public static boolean isDesktopShortcut(String shortcutName) {
        WinUtils util = WinUtils.getInstance();
        if (util == null) {
            return false;
        }
        Path scut = Paths.get(util.getSystemFolderPath(WinUtils.CSIDL_DESKTOP,
            false), shortcutName + Constants.LINK_EXTENSION);
        return Files.exists(scut);
    }

    /**
     * Creates a desktop shortcut. currently only available on windows systems
     *
     * @param shortcutName
     * @param shortcutTarget
     * @return true if succeeded
     */
    public static boolean createDesktopShortcut(String shortcutName,
        Path shortcutTarget)
    {
        WinUtils util = WinUtils.getInstance();
        if (util == null) {
            return false;
        }
        LOG.finer("Creating desktop shortcut to "
            + shortcutTarget.toAbsolutePath());
        ShellLink link = new ShellLink(null, shortcutName,
            shortcutTarget.toAbsolutePath().toString(), null);

        Path scut = Paths.get(util.getSystemFolderPath(WinUtils.CSIDL_DESKTOP,
            false), shortcutName + Constants.LINK_EXTENSION);
        try {
            util.createLink(link, scut.toAbsolutePath().toString());
            return true;
        } catch (IOException e) {
            LOG.warning("Couldn't create shortcut " + scut.toAbsolutePath());
            LOG.log(Level.FINER, "IOException", e);
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
        WinUtils util = WinUtils.getInstance();
        if (util == null) {
            return false;
        }
        LOG.finer("Removing desktop shortcut: " + shortcutName);
        try {
            Path scut = Paths.get(
                util.getSystemFolderPath(WinUtils.CSIDL_DESKTOP, false),
                shortcutName + Constants.LINK_EXTENSION);
            Files.deleteIfExists(scut);
            return true;
        } catch (Exception ioe) {
            return false;
        }
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
        InputStream in = null;
        try {
            Object content = url.getContent();
            if (!(content instanceof InputStream)) {
                LOG.severe("Unable to get content from " + url
                    + ". content is of type " + content.getClass().getName());
                return null;
            }
            in = (InputStream) content;

            StringBuilder buf = new StringBuilder();
            while (in.available() > 0) {
                buf.append((char) in.read());
            }
            return buf.toString();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to get content from " + url + ". "
                + e.toString(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
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
     * Retrieve a String on the clipboard.
     */
    public static String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = contents != null
            && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents
                    .getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                LOG.severe(ex.getMessage());
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
            }
        }
        return result;
    }

    /**
     * Encodes a url fragment, thus special characters are tranferred into a url
     * compatible style
     *
     * @param aURLFragment
     * @return the string encoded for URL usage.
     */
    public static String endcodeForURL(String aURLFragment) {
        String result = null;
        try {
            // FIX1: Corrected relative filenames including path separator /
            result = URLEncoder.encode(aURLFragment, "UTF-8").replace("%2F",
                "/");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    /**
     * Removes the last '/' from an URI and trims the string. Example:
     * http://www.powerfolder.com/ gets converted into
     * http://www.powerfolder.com
     *
     * @param uri
     *            the URI to trim and remove last slash from
     * @return the new URI string
     */
    public static String removeLastSlashFromURI(String uri) {
        if (uri == null) {
            return null;
        }
        String newURI = uri.trim();
        if (newURI.endsWith("/") && !newURI.endsWith("://")) {
            // Remove last '/' if existing
            newURI = newURI.substring(0, newURI.length() - 1);
        }
        return newURI;
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

    /**
     * Splits an array into a list of smaller arrays with the maximum size of
     * <code>size</code>.
     *
     * @param src
     *            the source array
     * @param size
     *            the maximum size of the chunks
     * @return the list of resulting arrays
     */
    public static List<byte[]> splitArray(byte[] src, int size) {
        int nChunks = src.length / size;
        List<byte[]> chunkList = new ArrayList<byte[]>(nChunks + 1);
        if (size >= src.length) {
            chunkList.add(src);
            return chunkList;
        }
        for (int i = 0; i < nChunks; i++) {
            byte[] chunk = new byte[size];
            System.arraycopy(src, i * size, chunk, 0, chunk.length);
            chunkList.add(chunk);
        }
        int lastChunkSize = src.length % size;
        if (lastChunkSize > 0) {
            byte[] lastChunk = new byte[lastChunkSize];
            System.arraycopy(src, nChunks * size, lastChunk, 0,
                lastChunk.length);
            chunkList.add(lastChunk);
        }
        return chunkList;
    }

    /**
     * Merges a list of arrays into one big array.
     *
     * @param arrayList
     *            the list of byte[] arryas.
     * @return the resulting array.
     */
    public static byte[] mergeArrayList(List<byte[]> arrayList) {
        Reject.ifNull(arrayList, "list of arrays is null");
        int totalSize = 0;
        for (byte[] bs : arrayList) {
            totalSize += bs.length;
        }
        if (totalSize == 0) {
            return new byte[0];
        }
        byte[] result = new byte[totalSize];
        int pos = 0;
        for (byte[] bs : arrayList) {
            System.arraycopy(bs, 0, result, pos, bs.length);
            pos += bs.length;
        }
        return result;
    }

    /**
     * Converts an array of bytes into an array of characters representing the
     * hexidecimal values of each byte in order. The returned array will be
     * double the length of the passed array, as it takes two characters to
     * represent any given byte.
     *
     * @param data
     *            a byte[] to convert to Hex characters
     * @return A char[] containing hexidecimal characters
     */
    public static char[] encodeHex(byte[] data) {
        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }

    // Encoding stuff *********************************************************

    /**
     * Calculates the MD5 digest and returns the value as a 16 element
     * <code>byte[]</code>.
     *
     * @param data
     *            Data to digest
     * @return MD5 digest
     */
    public static byte[] md5(byte[] data) {
        return getMd5Digest().digest(data);
    }

    /**
     * Returns a MessageDigest for the given <code>algorithm</code>.
     *
     * @param algorithm
     *            The MessageDigest algorithm name.
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Returns an MD5 MessageDigest.
     *
     * @return An MD5 digest instance.
     * @throws RuntimeException
     *             when a {@link java.security.NoSuchAlgorithmException} is
     *             caught,
     */
    private static MessageDigest getMd5Digest() {
        return getDigest("MD5");
    }

    /**
     * Checks if a program version is between (including border versions) two
     * versions
     *
     * @param lowVersion
     * @param compareVersion
     * @param highVersion
     * @return
     */
    public static boolean betweenVersion(String lowVersion,
        String compareVersion, String highVersion)
    {
        Reject.ifNull(lowVersion, "lowVersion is null");
        Reject.ifNull(compareVersion, "compareVersion is null");
        Reject.ifNull(highVersion, "highVersion is null");

        boolean isBetween = !compareVersions(lowVersion, compareVersion);
        if (!isBetween) {
            return false;
        }
        return compareVersions(highVersion, compareVersion);
    }

    /**
     * Comparse two version string which have the format "x.x.x aaa".
     * <p>
     * The last " aaa" is optional.
     *
     * @param higherVersion
     * @param compareVersion
     * @return true if higherVersion is greater than compareVersion
     */
    public static boolean compareVersions(String higherVersion,
        String compareVersion)
    {
        Reject.ifNull(higherVersion, "higherVersion is null");
        Reject.ifNull(compareVersion, "compareVersion is null");

        higherVersion = higherVersion.trim();
        compareVersion = compareVersion.trim();

        String addition1 = "";
        int addStart1 = higherVersion.indexOf(' ');
        if (addStart1 >= 0) {
            // Get addition text "x.x.x additionaltext"
            addition1 = higherVersion.substring(addStart1 + 1,
                higherVersion.length());
            higherVersion = higherVersion.substring(0, addStart1);
        }

        StringTokenizer nizer1 = new StringTokenizer(higherVersion, ".");
        int major1 = 0;
        try {
            major1 = Integer.valueOf(nizer1.nextToken());
        } catch (Exception e) {
        }
        int minor1 = 0;
        try {
            minor1 = Integer.valueOf(nizer1.nextToken());
        } catch (Exception e) {
            // e.printStackTrace();
        }
        int bugfix1 = 0;
        try {
            bugfix1 = Integer.valueOf(nizer1.nextToken());
        } catch (Exception e) {
        }

        String addition2 = "";
        int addStart2 = compareVersion.indexOf(' ');
        if (addStart2 >= 0) {
            // Get addition text "x.x.x additionaltext"
            addition2 = compareVersion.substring(addStart2 + 1,
                compareVersion.length());
            compareVersion = compareVersion.substring(0, addStart2);
        }

        StringTokenizer nizer2 = new StringTokenizer(compareVersion, ".");
        int major2 = 0;
        try {
            major2 = Integer.valueOf(nizer2.nextToken());
        } catch (Exception e) {
        }
        int minor2 = 0;
        try {
            minor2 = Integer.valueOf(nizer2.nextToken());
        } catch (Exception e) {
        }
        int bugfix2 = 0;
        try {
            bugfix2 = Integer.valueOf(nizer2.nextToken());
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
     * Interprets a string as connection string and returns the address. null is
     * returns if parse failed. Format is expeced as ' <connect ip>' or '
     * <connect ip>: <port>'
     *
     * @param connectStr
     *            The connectStr to parse
     * @return a InetSocketAddress created based on the connecStr
     */
    public static InetSocketAddress parseConnectionString(String connectStr) {
        if (connectStr == null) {
            return null;
        }
        String ip = connectStr.trim();
        int remotePort = ConnectionListener.DEFAULT_PORT;

        // format <ip/dns> or <ip/dns>:<port> expected
        // e.g. localhost:544
        int dotdot = connectStr.indexOf(':');
        if (dotdot >= 0 && dotdot < connectStr.length()) {
            ip = connectStr.substring(0, dotdot);
            try {
                remotePort = Integer.parseInt(connectStr.substring(dotdot + 1,
                    connectStr.length()));
            } catch (NumberFormatException e) {
                LOG.warning("Illegal port in " + connectStr
                    + ", trying default port");
            }
        }

        // try to connect
        return new InetSocketAddress(ip, remotePort);
    }

    /**
     * Replace every occurences of a string within a string
     *
     * @param target
     * @param from
     * @param to
     * @return
     */
    public static String replace(String target, String from, String to) {
        int start = target.indexOf(from);
        if (start == -1) {
            return target;
        }
        int lf = from.length();
        char[] targetChars = target.toCharArray();
        StringBuilder buffer = new StringBuilder();
        int copyFrom = 0;
        while (start != -1) {
            buffer.append(targetChars, copyFrom, start - copyFrom);
            buffer.append(to);
            copyFrom = start + lf;
            start = target.indexOf(from, copyFrom);
        }
        buffer.append(targetChars, copyFrom, targetChars.length - copyFrom);
        return buffer.toString();
    }

    /**
     * Creates a concurrent map with lesser segements to save memory. The
     * default concurrency of the maps are 4 (instead of 16 default).
     * <p>
     * 4 should be more suitable value for in-powerfolder us and procudes lesser
     * Segements.
     *
     * @param <K>
     * @param <V>
     * @return the concurrent hashmap
     */
    public static final <K, V> ConcurrentHashMap<K, V> createConcurrentHashMap()
    {
        return createConcurrentHashMap(16);
    }

    /**
     * Creates a concurrent map with lesser segements to save memory. The
     * default concurrency of the maps are 4 (instead of 16 default).
     * <p>
     * 4 should be more suitable value for in-powerfolder us and procudes lesser
     * Segements.
     *
     * @param <K>
     * @param <V>
     * @param intialSize
     *            the initial size of the map.
     * @return the concurrent hashmap
     */
    public static final <K, V> ConcurrentHashMap<K, V> createConcurrentHashMap(
        int intialSize)
    {
        return new ConcurrentHashMap<K, V>(intialSize, 0.75f, 4);
    }

    /**
     * PFC-2669
     * 
     * @param controller
     * @return a prepared client builder with default HTTP proxy settings and
     *         optional disabling of SSL cert validation.
     */
    public static final HttpClientBuilder createHttpClientBuildder(
        Controller controller)
    {
        Reject.ifNull(controller, "Controller");
        HttpClientBuilder builder = HttpClientBuilder.create();
        // PFC-2669: For HTTP Proxy
        builder.useSystemProperties();

        if (StringUtils.isNotBlank(ConfigurationEntry.HTTP_PROXY_HOST
            .getValue(controller)))
        {
            String proxyUsername = ConfigurationEntry.HTTP_PROXY_USERNAME
                .getValue(controller);
            if (proxyUsername == null) {
                proxyUsername = "";
            }
            String proxyPassword = ConfigurationEntry.HTTP_PROXY_PASSWORD
                .getValue(controller);
            if (proxyPassword == null) {
                proxyPassword = "";
            }
            String proxyHost = ConfigurationEntry.HTTP_PROXY_HOST
                .getValue(controller);
            if (proxyHost == null) {
                proxyHost = "";
            }
            int proxyPost = ConfigurationEntry.HTTP_PROXY_PORT
                .getValueInt(controller);

            Credentials credentials = new UsernamePasswordCredentials(
                proxyUsername, proxyPassword);
            AuthScope authScope = new AuthScope(proxyHost, proxyPost);
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(authScope, credentials);
            builder.setDefaultCredentialsProvider(credsProvider);
        }

        if (ConfigurationEntry.SECURITY_SSL_TRUST_ANY
            .getValueBoolean(controller))
        {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{new AllTrustingSSLManager()};
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                builder.setSslcontext(sc);
            } catch (Exception e) {
                LOG.severe("Unable to setup SSL to trust any certificate. " + e);
            }
        }
        return builder;
    }
}
