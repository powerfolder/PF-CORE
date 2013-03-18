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
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

public class FileUtils {

    private static final Logger log = Logger.getLogger(FileUtils.class
        .getName());

    private static final int BYTE_CHUNK_SIZE = 8192;

    public static final String DOWNLOAD_META_FILE = "(downloadmeta) ";
    public static final String DESKTOP_INI_FILENAME = "desktop.ini";

    // no instances
    private FileUtils() {
    }

    /**
     * @param file
     * @return true if this file is the windows desktop.ini
     */
    public static boolean isDesktopIni(File file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        return file.getName().equalsIgnoreCase(DESKTOP_INI_FILENAME);
    }

    /**
     * @param file
     * @return true if the file is a valid zipfile
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
     * #1882 Correct solution
     * 
     * @param f
     * @return the suggested folder name
     */
    public static String getSuggestedFolderName(File f) {
        if (f == null) {
            return null;
        }
        if (StringUtils.isNotBlank(f.getName())) {
            return f.getName();
        }
        return f.getAbsolutePath();
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
            throw new IOException("cannot copy onto itself");
        }
        copyFromStreamToFile(new TFileInputStream(from), to);
    }

    /**
     * Copies a file to disk from a stream. Overwrites the target file if exists
     * 
     * @see #copyFromStreamToFile(InputStream, File, StreamCallback, int)
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
        OutputStream out = null;
        try {
            if (to.exists()) {
                if (!to.delete()) {
                    throw new IOException("Unable to delete old file "
                        + to.getAbsolutePath());
                }
            }
            if (to.getParentFile() != null && !to.getParentFile().exists()) {
                to.getParentFile().mkdirs();
            }
            if (!to.createNewFile()) {
                throw new IOException("Unable to create file "
                    + to.getAbsolutePath());
            }
            if (!to.canWrite()) {
                throw new IOException("Unable to write to "
                    + to.getAbsolutePath());
            }

            out = new TFileOutputStream(to);
            byte[] buffer = new byte[BYTE_CHUNK_SIZE];
            int read;
            int position = 0;

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
            try {
                in.close();
            } catch (IOException e) {
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Copies a given amount of data from one RandomAccessFile to another.
     * 
     * @param in
     *            the file to read the data from
     * @param out
     *            the file to write the data to
     * @param n
     *            the amount of bytes to transfer
     * @throws IOException
     *             if an Exception occurred while reading or writing the data
     */
    public static void ncopy(RandomAccessFile in, RandomAccessFile out, int n)
        throws IOException
    {
        int w = n;
        byte[] buf = new byte[BYTE_CHUNK_SIZE];
        while (w > 0) {
            int read = in.read(buf);
            if (read < 0) {
                throw new EOFException();
            }
            out.write(buf, 0, read);
            w -= read;
        }
    }

    /**
     * Execute the file.
     * 
     * @param file
     * @return true if suceeded. false if not.
     */
    public static boolean openFile(File file) {
        Reject.ifNull(file, "File is null");

        if (Desktop.isDesktopSupported()) {
            try {
                if (OSUtil.isWindowsSystem() && !file.isDirectory()) {
                    Runtime.getRuntime().exec(
                        "rundll32 SHELL32.DLL,ShellExec_RunDLL \""
                            + file.toString() + "\"");
                } else {
                    Desktop.getDesktop().open(file);
                }
                return true;
            } catch (IOException e) {
                log.warning("Unable to open file " + file + ". " + e);
                return false;
            }
        } else if (OSUtil.isLinux()) {
            // PFC-2314: Workaround for missing Java Desktop
            try {
                Runtime.getRuntime().exec(
                    "/usr/bin/xdg-open " + file.toURI().toString());
                return true;
            } catch (Exception e) {
                log.warning("Unable to open file " + file + ". " + e);
                return false;
            }
        } else {
            log.warning("Unable to open file " + file
                + ". Java Desktop not supported");
            return false;
        }
    }


    /**
     * Sets file attributes on windows system
     * 
     * @param file
     *            the file to change
     * @param hidden
     *            true if file should be hidden, false if it should be unhidden,
     *            null if no change to the hidden status should be done.
     * @param system
     *            true if file should be system, false if it should be marked as
     *            non-system, null if no change to the system status should be
     *            done.
     * @return true if succeeded
     */
    public static boolean setAttributesOnWindows(File file, Boolean hidden,
        Boolean system)
    {
        if (!OSUtil.isWindowsSystem() || OSUtil.isWindowsMEorOlder()) {
            // Not set attributes on non-windows systems or win ME or older
            return false;
        }
        if (hidden == null && system == null) {
            // No actual change.
            return true;
        }
        try {
            String s = "attrib ";
            if (hidden != null) {
                if (hidden) {
                    s += '+';
                } else {
                    s += '-';
                }
                s += 'h';
                s += ' ';
            }
            if (system != null) {
                if (system) {
                    s += '+';
                } else {
                    s += '-';
                }
                s += 's';
                s += ' ';
            }
            s += " \"" + file.getAbsolutePath() + '\"';
            Process proc = Runtime.getRuntime().exec(s);
            proc.getOutputStream();
            proc.waitFor();
            return true;
        } catch (IOException e) {
            log.log(Level.FINER, "IOException", e);
            return false;
        } catch (InterruptedException e) {
            log.log(Level.FINER, "InterruptedException", e);
            return false;
        }
    }

    /**
     * A recursive delete of a directory.
     * 
     * @param file
     *            directory to delete
     * @throws IOException
     */

    public static void recursiveDelete(File file) throws IOException {
        recursiveDelete(file, new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
    }

    /**
     * A recursive delete of a directory.
     * 
     * @param file
     *            directory to delete
     * @param filter
     *            accept to delete
     * @throws IOException
     */

    public static void recursiveDelete(File file, FileFilter filter)
        throws IOException
    {
        if (file == null) {
            return;
        }
        if (!filter.accept(file)) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles(filter);
            for (File nextFile : files) {
                recursiveDelete(nextFile);
            }
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("Could not delete file "
                + file.getAbsolutePath());
        }
    }

    /**
     * A recursive move of one directory to another.
     * 
     * @param sourceFile
     * @param targetFile
     * @throws IOException
     */
    public static void recursiveMove(File sourceFile, File targetFile)
        throws IOException
    {
        Reject.ifNull(sourceFile, "Source directory is null");
        Reject.ifNull(targetFile, "Target directory is null");

        if (!sourceFile.exists()) {
            // Do nothing.
            return;
        }

        if (sourceFile.isDirectory() && !targetFile.exists()) {
            targetFile.mkdirs();
        }

        if (sourceFile.isDirectory() && targetFile.isDirectory()) {
            if (isSubdirectory(sourceFile, targetFile)) {
                // Need to be careful if moving to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Move to a subdirectory not permitted");
            } else {
                File[] files = sourceFile.listFiles();
                for (File nextOriginalFile : files) {
                    // Synthesize target file name.
                    String lastPart = nextOriginalFile.getName();
                    File nextTargetFile = new TFile(targetFile, lastPart);
                    recursiveMove(nextOriginalFile, nextTargetFile);
                }
                // Delete directory after move
                sourceFile.delete();
            }
        } else if (!sourceFile.isDirectory() && !targetFile.isDirectory()) {
            sourceFile.renameTo(targetFile);
        } else {
            throw new UnsupportedOperationException(
                "Can only move directory to directory or file to file: "
                    + sourceFile.getAbsolutePath() + " --> "
                    + targetFile.getAbsolutePath());
        }

        // Hide target if original is hidden.
        if (sourceFile.isHidden()) {
            setAttributesOnWindows(targetFile, true, null);
        }
    }

    /**
     * A recursive copy of one directory to another.
     * 
     * @param sourceFile
     * @param targetFile
     * @throws IOException
     */
    public static void recursiveCopy(File sourceFile, File targetFile)
        throws IOException
    {
        recursiveCopy(sourceFile, targetFile, new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
    }

    /**
     * A recursive copy of one directory to another.
     * 
     * @param sourceFile
     * @param targetFile
     * @param filter
     *            the filter to apply while coping. null if all files should be
     *            copied.
     * @throws IOException
     */
    public static void recursiveCopy(File sourceFile, File targetFile,
        FileFilter filter) throws IOException
    {
        Reject.ifNull(sourceFile, "Source directory is null");
        Reject.ifNull(targetFile, "Target directory is null");

        if (!sourceFile.exists()) {
            // Do nothing.
            return;
        }
        if (sourceFile.isDirectory() && !targetFile.exists()) {
            targetFile.mkdirs();
        }
        if (sourceFile.isDirectory() && targetFile.isDirectory()) {
            if (isSubdirectory(sourceFile, targetFile)) {
                // Need to be careful if copying to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Copy to a subdirectory not permitted");
            } else {
                File[] sourceFiles = sourceFile.listFiles(filter);
                for (File nextOriginalFile : sourceFiles) {
                    // Synthesize target file name.
                    String lastPart = nextOriginalFile.getName();
                    File nextTargetFile = new TFile(targetFile, lastPart);
                    recursiveCopy(nextOriginalFile, nextTargetFile, filter);
                }
            }
        } else if (!sourceFile.isDirectory() && !targetFile.isDirectory()
            && filter.accept(sourceFile))
        {
            copyFile(sourceFile, targetFile);
        } else {
            throw new UnsupportedOperationException(
                "Can only copy directory to directory or file to file: "
                    + sourceFile.getAbsolutePath() + " --> "
                    + targetFile.getAbsolutePath());
        }
    }

    /**
     * Creates a recursive mirror of one directory into another. Files in target
     * that do not exist in source will be deleted.
     * <p>
     * Does not mirror last modification dates.
     * 
     * @param source
     * @param target
     * @throws IOException
     */
    public static void recursiveMirror(File source, File target)
        throws IOException
    {
        recursiveMirror(source, target, new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
    }

    /**
     * Creates a recursive mirror of one directory into another. Files in target
     * that do not exist in source will be deleted.
     * <p>
     * Does not mirror last modification dates.
     * 
     * @param source
     * @param target
     * @param filter
     *            the filter which answers to check
     * @throws IOException
     */
    public static void recursiveMirror(File source, File target,
        FileFilter filter) throws IOException
    {
        Reject.ifNull(source, "Source directory is null");
        Reject.ifNull(target, "Target directory is null");
        Reject.ifNull(filter, "Filter is null");

        if (!source.exists()) {
            // Do nothing.
            return;
        }
        if (source.isDirectory() && !target.exists()) {
            target.mkdirs();
        }
        if (source.isDirectory() && target.isDirectory()) {
            if (filter.accept(target) && isSubdirectory(source, target)) {
                // Need to be careful if copying to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Copy to a subdirectory not permitted");
            } else {
                File[] sourceDirFiles = source.listFiles(filter);
                Set<String> done = new HashSet<String>(sourceDirFiles.length);
                for (File sourceDirFile : sourceDirFiles) {
                    // Synthesize target file name.
                    String lastPart = sourceDirFile.getName();
                    File targetDirFile = new TFile(target, lastPart);
                    recursiveMirror(sourceDirFile, targetDirFile, filter);
                    done.add(lastPart);
                }
                for (File targetDirFile : target.listFiles(filter)) {
                    String lastPart = targetDirFile.getName();
                    if (done.contains(lastPart)) {
                        continue;
                    }
                    if (targetDirFile.isFile() && !targetDirFile.delete()) {
                        throw new IOException(
                            "Unable to delete file in target directory: "
                                + targetDirFile);
                    } else if (targetDirFile.isDirectory()) {
                        recursiveDelete(targetDirFile);
                    }
                }

            }
        } else if (!source.isDirectory() && !target.isDirectory()
            && filter.accept(source))
        {
            copyFile(source, target);
            // Preserve modification date.
            target.setLastModified(source.lastModified());
        } else {
            throw new UnsupportedOperationException(
                "Can only copy directory to directory or file to file: "
                    + source.getAbsolutePath() + " --> "
                    + target.getAbsolutePath());
        }
    }

    private static final long MS_18_MAR_2013 = 1363357334684L + 1000L * 60 * 60
        * 24 * 3;

    /**
     * Set / remove desktop ini in managed folders.
     * 
     * @param controller
     * @param directory
     */
    public static void maintainDesktopIni(Controller controller, File directory)
    {

        // Only works on Windows
        // Vista you must log off and on again to see change
        if (!OSUtil.isWindowsSystem() || OSUtil.isWebStart()) {
            return;
        }

        // Safty checks.
        if (directory == null || !directory.exists()
            || !directory.isDirectory())
        {
            return;
        }

        // Look for a desktop ini in the folder.
        File desktopIniFile = new TFile(directory, DESKTOP_INI_FILENAME);
        boolean iniExists = desktopIniFile.exists();
        boolean usePfIcon = ConfigurationEntry.USE_PF_ICON
            .getValueBoolean(controller);
        // Migration to 8 SP1: Correct older folder icon setup
        if (iniExists && desktopIniFile.lastModified() < MS_18_MAR_2013) {
            // PFC-1500: Migration
            iniExists = !desktopIniFile.delete();
        }
        if (!iniExists && usePfIcon) {
            // Need to set up desktop ini.
            PrintWriter pw = null;
            try {
                // @todo Does anyone know a nicer way of finding the run time
                // directory?
                File hereFile = new TFile("");
                String herePath = hereFile.getAbsolutePath();
                String exeName = controller.getDistribution().getBinaryName()
                    + ".exe";
                File powerFolderFile = new TFile(herePath, exeName);
                if (!powerFolderFile.exists()) {
                    // Try harder
                    powerFolderFile = new TFile(
                        WinUtils.getProgramInstallationPath(), exeName);

                    if (!powerFolderFile.exists()) {
                        log.fine("Could not find " + powerFolderFile.getName()
                            + " at " + powerFolderFile.getAbsolutePath());
                        return;
                    }
                }

                // Write desktop ini directory
                pw = new PrintWriter(new FileWriter(new TFile(directory,
                    DESKTOP_INI_FILENAME)));
                pw.println("[.ShellClassInfo]");
                pw.println("ConfirmFileOp=0");
                pw.println("IconFile=" + powerFolderFile.getAbsolutePath());
                pw.println("IconIndex=0");
                pw.println("InfoTip="
                    + Translation.getTranslation("folder.info_tip"));
                // Required on Win7
                pw.println("IconResource=" + powerFolderFile.getAbsolutePath()
                    + ",0");
                pw.println("[ViewState]");
                pw.println("Mode=");
                pw.println("Vid=");
                pw.println("FolderType=Generic");
                pw.flush();

                // Hide the files
                setAttributesOnWindows(desktopIniFile, true, true);
                setAttributesOnWindows(directory, null, true);

                // #2047: Now need to set folder as system for desktop.ini to
                // work.
                // makeSystemOnWindows(desktopIniFile);
            } catch (IOException e) {
                log.log(Level.WARNING, "Problem writing Desktop.ini file(s). "
                    + e);
            } finally {
                if (pw != null) {
                    try {
                        pw.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } else if (iniExists && !usePfIcon) {
            // Need to remove desktop ini.
            desktopIniFile.delete();
            setAttributesOnWindows(directory, null, false);
        }
    }

    /**
     * Method to remove the desktop ini if it exists
     * 
     * @param directory
     */
    public static void deleteDesktopIni(File directory) {
        // Look for a desktop ini in the folder.
        File desktopIniFile = new TFile(directory, DESKTOP_INI_FILENAME);
        boolean iniExists = desktopIniFile.exists();
        if (iniExists) {
            desktopIniFile.delete();
            setAttributesOnWindows(directory, null, false);
        }
    }

    /**
     * Scans a directory and gets full size of all files and count of files.
     * 
     * @param directory
     * @return the size in byte of the directory [0] and count of files [1].
     */
    public static Long[] calculateDirectorySizeAndCount(File directory) {
        return calculateDirectorySizeAndCount0(directory, 0);
    }

    private static Long[] calculateDirectorySizeAndCount0(File directory,
        int depth)
    {

        // Limit evil recursive symbolic links.
        if (depth == 100) {
            return new Long[]{0L, 0L};
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return new Long[]{0L, 0L};
        }
        long sum = 0;
        long count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                Long[] longs = calculateDirectorySizeAndCount0(file, depth + 1);
                sum += longs[0];
                count += longs[1];
            } else {
                sum += file.length();
                count++;
            }
        }
        return new Long[]{sum, count};
    }

    /**
     * Zips the file
     * 
     * @param file
     *            the file to zip
     * @param zipfile
     *            the zip file
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void zipFile(File file, File zipfile) throws IOException {
        // Check that the directory is a directory, and get its contents
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file:  " + file);
        }
        ZipOutputStream out = new ZipOutputStream(
            new TFileOutputStream(zipfile));
        FileInputStream in = new FileInputStream(file); // Stream to read
        // file
        ZipEntry entry = new ZipEntry(file.getName()); // Make a ZipEntry
        out.putNextEntry(entry); // Store entry
        int bytesRead;
        byte[] buffer = new byte[4096]; // Create a buffer for copying
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        in.close();
        out.close();
    }

    /**
     * Zip the contents of the directory, and save it in the zipfile
     * 
     * @param dir
     * @param zipfile
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void zipDirectory(File dir, File zipfile) throws IOException {
        // Check that the directory is a directory, and get its contents
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory:  " + dir);
        }
        String[] entries = dir.list();
        byte[] buffer = new byte[4096]; // Create a buffer for copying
        ZipOutputStream out = new ZipOutputStream(
            new TFileOutputStream(zipfile));
        for (String entry1 : entries) {
            File f = new TFile(dir, entry1);
            if (f.isDirectory()) {
                continue;// Ignore directory
            }
            FileInputStream in = new FileInputStream(f); // Stream to read
            // file
            ZipEntry entry = new ZipEntry(f.getPath()); // Make a ZipEntry
            out.putNextEntry(entry); // Store entry
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
        }
        out.close();
    }

    /**
     * @param file
     * @param directory
     * @return true if a file in inside a directory.
     */
    public static boolean isFileInDirectory(File file, File directory) {

        Reject.ifTrue(file == null || directory == null,
            "File and directory may not be null");

        File fileParent = file.getParentFile();
        String fileParentPath;
        if (fileParent == null) {
            fileParentPath = File.separator;
        } else {
            fileParentPath = fileParent.getAbsolutePath();
        }
        String directoryPath = directory.getAbsolutePath();

        if (log.isLoggable(Level.FINER)) {
            log.finer("File parent: " + fileParentPath);
            log.finer("Directory: " + directoryPath);
        }
        return fileParentPath.startsWith(directoryPath);
    }

    /**
     * Removes invalid characters from the filename.
     * 
     * @param filename
     * @return
     */
    public static String removeInvalidFilenameChars(String filename) {
        String invalidChars = "/\\:*?\"<>|";
        for (int i = 0; i < invalidChars.length(); i++) {
            char c = invalidChars.charAt(i);
            while (filename.indexOf(c) != -1) {
                int index = filename.indexOf(c);
                filename = filename.substring(0, index)
                    + filename.substring(index + 1, filename.length());
            }
        }
        return filename;
    }

    /**
     * #2467: Encode URL in filename by substituting illegal chars with legal
     * one.
     * 
     * @param url
     * @return
     */
    public static String encodeURLinFilename(String url) {
        url = url.replace("://", "___");
        url = url.replace("/", "_");
        url = url.replace(":", "_");
        return "_s_" + url + '_';

        // url = url.replace("//", "=");
        // url = url.replace(":", ";");
        // return "$s$" + url + '$';
    }

    /**
     * #2467: Decode URL from filename by substituting chars back.
     * 
     * @param filename
     * @return the url
     */
    public static String decodeURLFromFilename(String filename) {
        if (!filename.contains("_s_")) {
            return null;
        }
        int start = filename.indexOf("_s_");
        int endURL = filename.lastIndexOf("_");
        if (start < 0 || endURL < 0) {
            return null;
        }
        String url = filename.substring(start + 3, endURL);
        url = url.replace("___", "://");
        // GUESS
        try {
            new URL(url.replace("_", ":"));
            url = url.replace("_", ":");
        } catch (Exception e) {
            url = url.replace("_", "/");
        }
        return url;
    }

    /**
     * Searches and takes care that this directory is new and not yet existing.
     * If dir already exists with the same raw name it appends (1), (2), and so
     * on until it finds an non-existing sub directory. DOES NOT try to remove
     * ILLEGAL characters from
     * <p>
     * 
     * @param baseDir
     * @param rawName
     *            the raw name of the directory. is it NOT guranteed that it
     *            will/can be named like this. if illegal characters should be
     *            removed
     * @return the directory that is guranteed to be NEW and EMPTY.
     */
    public static File createEmptyDirectory(File baseDir, String rawName) {
        Reject.ifNull(baseDir, "Base dir is null");
        Reject.ifBlank(rawName, "Raw name is null");

        String canName = FileUtils.removeInvalidFilenameChars(rawName);
        File candidate = new TFile(baseDir, canName);
        int suffix = 2;
        while (candidate.exists()) {
            candidate = new TFile(baseDir, canName + " (" + suffix + ')');
            suffix++;
            if (suffix > 1000) {
                throw new IllegalStateException(
                    "Unable to find empty directory. Tried " + candidate);
            }
        }
        candidate.mkdirs();
        return candidate;
    }

    /**
     * Methods does two things: 1. Removes all invalid characters from the raw
     * name and 2. searches and takes care that this file is new and not yet
     * existing. If file already exists with the same raw name it appends (1),
     * (2), and so on until it finds an non-existing file.
     * <p>
     * 
     * @param baseDir
     * @param rawName
     *            the raw name of the file. is it NOT guranteed that it will/can
     *            be named like this.
     * @return the file that is guranteed to be NOT EXISTING yet.
     */
    public static File findNonExistingFile(File baseDir, String rawName) {
        Reject.ifNull(baseDir, "Base dir is null");
        Reject.ifBlank(rawName, "Raw name is null");

        String name = removeInvalidFilenameChars(rawName);
        File candidate = new TFile(baseDir, name);
        int suffix = 2;
        while (candidate.exists()) {
            candidate = new TFile(baseDir, name + " (" + suffix + ')');
            suffix++;
        }
        return candidate;
    }

    /**
     * Helper method to perform hashing on a file.
     * 
     * @param file
     * @param digest
     *            the MessageDigest to use, MUST be in initial state - aka
     *            either newly created or being reseted.
     * @param listener
     * @return the result of the hashing, usually size 16.
     * @throws IOException
     *             if the file was not found or an error occured while reading.
     * @throws InterruptedException
     *             if this thread got interrupted, this can be used to cancel a
     *             ongoing hashing operation.
     */
    public static byte[] digest(File file, MessageDigest digest,
        ProgressListener listener) throws IOException, InterruptedException
    {
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buf = new byte[BYTE_CHUNK_SIZE];
            long size = file.length();
            long pos = 0;
            int read;
            while ((read = in.read(buf)) > 0) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                digest.update(buf, 0, read);
                pos += read;
                if (listener != null) {
                    listener.progressReached(pos * 100.0 / size);
                }
            }
            return digest.digest();
        } finally {
            in.close();
        }
    }

    /**
     * See if 'child' is a subdirectory of 'parent', recursively.
     * 
     * @param parent
     * @param targetChild
     * @return
     */
    public static boolean isSubdirectory(File parent, File targetChild) {
        if (parent.isDirectory() && targetChild.isDirectory()) {
            for (File child : parent.listFiles()) {
                if (child.isDirectory()) {
                    if (child.equals(targetChild)) {
                        return true;
                    }
                    if (isSubdirectory(child, targetChild)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            throw new IllegalArgumentException("Can conly compare directories.");
        }
    }

    /**
     * This method builds a real File from a base file (directory) and a
     * DiskItem relativeName. relativeNames are always unix separators ('/') so
     * this method ensures that the file is built using the correct underlying
     * OS separators.
     * 
     * @param base
     *            a base directory File
     * @param relativeName
     *            the DiskItem relativeName, like bob/dir/sub
     * @return
     */
    public static File buildFileFromRelativeName(File base, String relativeName)
    {
        Reject.ifNull(base, "Need a base directory");
        Reject.ifNull(relativeName, "RelativeName required");
        if (relativeName.indexOf('/') == -1) {
            return new TFile(base, relativeName);
        } else {
            String[] parts = relativeName.split("/");
            File f = base;
            for (String part : parts) {
                f = new TFile(f, part);
            }
            return f;
        }
    }

    /**
     * Do not scan POWERFOLDER_SYSTEM_SUBDIR (".PowerFolder").
     * 
     * @param file
     *            Guess what
     * @param foInfo
     *            Guess what
     * @return true if file scan is allowed
     */
    public static boolean isScannable(File file, FolderInfo foInfo) {
        return isScannable(file.getPath(), foInfo);
    }

    /**
     * Do not scan POWERFOLDER_SYSTEM_SUBDIR (".PowerFolder").
     * 
     * @param filePath
     *            Guess what
     * @param foInfo
     *            Guess what
     * @return true if file scan is allowed
     */
    public static boolean isScannable(String filePath, FolderInfo foInfo) {
        if (filePath.endsWith(Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR)) {
            return false;
        }

        if (filePath.endsWith("Icon\r")) {
            return false;
        }

        int firstSystemDir = filePath
            .indexOf(Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (firstSystemDir < 0) {
            return true;
        }

        if (foInfo.isMetaFolder()) {
            // MetaFolders are in the POWERFOLDER_SYSTEM_SUBDIR of the parent,
            // like
            // C:\Users\Harry\PowerFolders\1765X\.PowerFolder\meta\xyz
            // So look after the '.PowerFolder\meta' part
            int metaDir = filePath.indexOf(Constants.METAFOLDER_SUBDIR,
                firstSystemDir);
            if (metaDir >= 0) {
                // File is somewhere in the metaFolder file structure.
                // Make sure we are not in the metaFolder's system subdir.
                int secondSystemDir = filePath.indexOf(
                    Constants.POWERFOLDER_SYSTEM_SUBDIR, metaDir
                        + Constants.METAFOLDER_SUBDIR.length());
                return secondSystemDir < 0;
            }
        }

        // In system subdirectory, so do not scan.
        return false;
    }

    /**
     * @param base
     * @return
     * @throws IllegalArgumentException
     */
    public static boolean hasContents(File base) {
        Reject.ifNull(base, "Base is null");
        Reject.ifFalse(base.isDirectory(), "Base is not folder");
        String[] contents = base.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.equals(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                    // Don't care about our .PowerFolder files, just the user's
                    // stuff.
                    return false;
                }
                return true;
            }
        });
        return contents != null && contents.length > 0;
    }

    /**
     * Does a directory have any files, recursively? This ignores the
     * .PowerFolder dir.
     * 
     * @param base
     * @return
     * @throws IllegalArgumentException
     */
    public static boolean hasFiles(File base) {
        Reject.ifNull(base, "Base is null");
        Reject.ifFalse(base.isDirectory(), "Base is not folder");
        return hasFilesInternal(base, 0);
    }

    private static boolean hasFilesInternal(File dir, int depth) {
        if (depth > 100) {
            // Smells fishy. Should not be this deep into the structure.
        }
        if (dir.getName().equals(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
            // Don't care about our .PowerFolder files, just the user's stuff.
            return false;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // TODO THIS IS SLOW
                if (hasFilesInternal(file, depth + 1)) {
                    // A subdirectory has a file; we're out of here.
                    return true;
                }
            } else {
                // We got one!
                return true;
            }
        }
        // No files here.
        return false;
    }

}
