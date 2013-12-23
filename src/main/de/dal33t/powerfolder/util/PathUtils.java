package de.dal33t.powerfolder.util;

import java.awt.Desktop;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

public class PathUtils {

    private static final Logger log = Logger.getLogger(PathUtils.class
        .getName());

    private static final int BYTE_CHUNK_SIZE = 8192;

    public static final String DOWNLOAD_META_FILE = "(downloadmeta) ";
    public static final String DESKTOP_INI_FILENAME = "desktop.ini";

    // no instances
    private PathUtils() {
    }

    /**
     * @param file
     * @return true if this file is the windows desktop.ini
     */
    public static boolean isDesktopIni(Path file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        return file.getFileName().toString()
            .equalsIgnoreCase(DESKTOP_INI_FILENAME);
    }

    /**
     * @param file
     * @return true if the file is a valid zipfile
     */
    public static boolean isValidZipFile(Path file) {
        if (file == null) {
            throw new NullPointerException("File is null");
        }
        try {
            new ZipFile(file.toAbsolutePath().toString());
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Check the path if it is a zyncro backend.
     * 
     * @param path
     * @return true if path is a zyncro path, false otherwise.
     */
    public static boolean isZyncroPath(Path path) {
        return path.getFileSystem().provider().getScheme()
            .equals(Constants.ZYNCRO_SCHEME);
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
    public static Path createEmptyDirectory(Path baseDir, String rawName) {
        Reject.ifNull(baseDir, "Base dir is null");
        Reject.ifBlank(rawName, "Raw name is null");

        String canName = removeInvalidFilenameChars(rawName);
        Path candidate = baseDir.resolve(canName);
        int suffix = 2;

        while (Files.exists(candidate)) {
            candidate = candidate.getParent().resolve(
                canName + " (" + suffix + ")");
            suffix++;
            if (suffix > 1000) {
                throw new IllegalStateException(
                    "Unable to find empty directory Tried " + candidate);
            }
        }

        try {
            Files.createDirectories(candidate);
        } catch (UnsupportedOperationException uoe) {
            log.info("Could not create directory (unsupported). " + uoe);
        } catch (FileAlreadyExistsException faee) {
            log.fine("File already exists. " + faee);
        } catch (IOException ioe) {
            log.info("Could not create driectory. " + ioe);
        }

        return candidate;
    }

    public static int getNumberOfSiblings(Path base) {
        return getNumberOfSiblings(base, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                return true;
            }
        });
    }

    public static int getNumberOfSiblings(Path base, Filter<Path> filter) {
        int i = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(base,
            filter)) {

            if (files == null) {
                return 0;
            }

            Iterator<Path> it = files.iterator();

            if (it == null) {
                return 0;
            }

            while (it.hasNext()) {
                it.next();
                i++;
            }
        } catch (IOException ioe) {
            log.warning("Could not count number of siblings. " + ioe);
            return 0;
        }
        return i;
    }

    public static boolean isEmptyDir(Path path) {
        return isEmptyDir(path, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                return true;
            }
        });
    }

    public static boolean isEmptyDir(Path path, Filter<Path> filter) {
        if (path == null) {
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(path,
            filter)) {
            if (files == null) {
                return false;
            }

            Iterator<Path> it = files.iterator();

            if (it == null) {
                return false;
            }

            return !it.hasNext();
        } catch (IOException ioe) {
            log.warning("Error checking for empty directory. " + ioe);
        }

        return false;
    }

    /**
     * #1882 Correct solution
     * 
     * @param f
     * @return the suggested folder name
     */
    public static String getSuggestedFolderName(Path f) {
        if (f == null) {
            return null;
        }
        if (StringUtils.isNotBlank(f.getFileName().toString())) {
            return f.getFileName().toString();
        }
        return f.toAbsolutePath().toString();
    }

    /**
     * PFC-2374 & SYNC-180 Workaround for JNotify on Mac to get the "real" file
     * name.
     * 
     * @param rootPath
     * @param name
     * @return
     */
    public static String getDiskFileName(String rootPath, String name) {
        if (!OSUtil.isMacOS()) {
            return name;
        }
        Path p = Paths.get(rootPath).relativize(
            Paths.get(rootPath, name).toAbsolutePath());
        return p.toString();
    }

    /**
     * Copies a file
     * 
     * @param from
     * @param to
     *            if file exists it will be overwritten!
     * @throws IOException
     */
    public static void copyFile(Path from, Path to) throws IOException {
        if (from == null) {
            throw new NullPointerException("From file is null");
        }
        if (Files.notExists(from)) {
            throw new IOException("From file does not exists "
                + from.toAbsolutePath().toString());
        }
        if (from.equals(to)) {
            throw new IOException("cannot copy onto itself");
        }
        copyFromStreamToFile(Files.newInputStream(from), to);
    }

    /**
     * Copies a file to disk from a stream. Overwrites the target file if exists
     * 
     * @see #copyFromStreamToFile(InputStream, Path, StreamCallback, int)
     * @param in
     *            the input stream
     * @param to
     *            the file where the stream should be written in
     * @throws IOException
     */
    public static void copyFromStreamToFile(InputStream in, Path to)
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
    public static void copyFromStreamToFile(InputStream in, Path to,
        StreamCallback callback, int totalAvailableBytes) throws IOException
    {
        if (in == null) {
            throw new NullPointerException("InputStream file is null");
        }
        if (to == null) {
            throw new NullPointerException("To file is null");
        }

        try {
            Files.deleteIfExists(to);
        } catch (IOException ioe) {
            throw new IOException("Unable to delete old file "
                + to.toAbsolutePath().toString(), ioe);
        }
        if (to.getParent() != null && Files.notExists(to.getParent())) {
            Files.createDirectories(to.getParent());
        }

        try {
            Files.createFile(to);
        } catch (IOException ioe) {
            throw new IOException("Unable to create file "
                + to.toAbsolutePath().toString(), ioe);
        }
        if (!Files.isWritable(to)) {
            throw new IOException("Unable to write to "
                + to.toAbsolutePath().toString());
        }

        try (OutputStream out = new BufferedOutputStream(
            Files.newOutputStream(to))) {
            byte[] buffer = new byte[BYTE_CHUNK_SIZE];
            int read;
            long position = 0;

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
        }
    }

    /**
     * Copy a file using raw file system access using input/output streams.
     * 
     * @param from
     * @param to
     * @throws IOException
     */
    public static void rawCopy(Path from, Path to) throws IOException {
        Reject.ifNull(from, "Source file is null");
        Reject.ifNull(to, "Target file is null");

        FileSystemProvider outProv = to.getFileSystem().provider();
        FileSystemProvider inProv = from.getFileSystem().provider();

        try (OutputStream os = outProv.newOutputStream(to);
            InputStream is = inProv.newInputStream(from)) {
            int BUFFER_SIZE = 8192;
            byte[] BUFFER = new byte[BUFFER_SIZE];

            int ret = is.read(BUFFER);

            while (ret != -1) {
                os.write(BUFFER, 0, ret);
                ret = is.read(BUFFER);
            }
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    public static void rawCopy(InputStream from, OutputStream to)
        throws IOException
    {
        Reject.ifNull(from, "Source is null");
        Reject.ifNull(to, "Target is null");

        try (InputStream is = from; OutputStream os = to) {
            int BUFFER_SIZE = 8192;
            byte[] BUFFER = new byte[BUFFER_SIZE];

            int ret = is.read(BUFFER);

            while (ret != -1) {
                os.write(BUFFER, 0, ret);
                ret = is.read(BUFFER);
            }
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    /**
     * A recursive delete of a directory.
     * 
     * @param file
     *            directory to delete
     * @throws IOException
     */

    public static void recursiveDelete(Path file) throws IOException {
        recursiveDelete(file, new Filter<Path>() {
            public boolean accept(Path pathname) {
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

    public static void recursiveDelete(Path file, Filter<Path> filter)
        throws IOException
    {
        if (file == null) {
            return;
        }
        if (!filter.accept(file)) {
            return;
        }
        if (Files.isDirectory(file)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file,
                filter)) {
                for (Path path : stream) {
                    recursiveDelete(path);
                }
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ioe) {
            throw new IOException("Could not delete file "
                + file.toAbsolutePath(), ioe);
        }
    }

    /**
     * A recursive move of one directory to another.
     * 
     * @param sourceFile
     * @param targetFile
     * @throws IOException
     */
    public static void recursiveMove(Path sourceFile, Path targetFile)
        throws IOException
    {
        Reject.ifNull(sourceFile, "Source directory is null");
        Reject.ifNull(targetFile, "Target directory is null");

        if (Files.notExists(sourceFile)) {
            // Do nothing.
            return;
        }
        boolean wasHidden = Files.isHidden(sourceFile);

        if (Files.isDirectory(sourceFile) && Files.notExists(targetFile)) {
            Files.createDirectories(targetFile);
        }

        if (Files.isDirectory(sourceFile) && Files.isDirectory(targetFile)) {
            if (isSubdirectory(sourceFile, targetFile)) {
                // Need to be careful if moving to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Move to a subdirectory not permitted");
            } else {
                try (DirectoryStream<Path> stream = Files
                    .newDirectoryStream(sourceFile)) {
                    for (Path nextOriginalFile : stream) {
                        recursiveMove(nextOriginalFile,
                            targetFile.resolve(nextOriginalFile.getFileName()));
                    }
                }
                // Delete directory after move
                Files.delete(sourceFile);
            }
        } else if (!Files.isDirectory(sourceFile)
            && !Files.isDirectory(targetFile))
        {
            Files.move(sourceFile, targetFile);
        } else {
            throw new UnsupportedOperationException(
                "Can only move directory to directory or file to file: "
                    + sourceFile.toAbsolutePath().toString() + " --> "
                    + targetFile.toAbsolutePath().toString());
        }

        // Hide target if original is hidden.
        if (wasHidden) {
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
    public static void recursiveCopy(Path sourceFile, Path targetFile)
        throws IOException
    {
        recursiveCopy(sourceFile, targetFile, new Filter<Path>() {
            public boolean accept(Path pathname) {
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
    public static void recursiveCopy(Path sourceFile, Path targetFile,
        Filter<Path> filter) throws IOException
    {
        Reject.ifNull(sourceFile, "Source directory is null");
        Reject.ifNull(targetFile, "Target directory is null");

        if (Files.notExists(sourceFile)) {
            // Do nothing.
            return;
        }
        if (Files.isDirectory(sourceFile) && Files.notExists(targetFile)) {
            Files.createDirectories(targetFile);
        }
        if (Files.isDirectory(sourceFile) && Files.isDirectory(targetFile)) {
            if (isSubdirectory(sourceFile, targetFile)) {
                // Need to be careful if copying to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Copy to a subdirectory not permitted");
            } else {
                try (DirectoryStream<Path> sourceStream = Files
                    .newDirectoryStream(sourceFile, filter)) {
                    for (Path nextOriginalFile : sourceStream) {
                        // Synthesize target file name.
                        String lastPart = nextOriginalFile.getFileName()
                            .toString();
                        Path nextTargetFile = targetFile.resolve(lastPart);
                        recursiveCopy(nextOriginalFile, nextTargetFile, filter);
                    }
                }
            }
        } else if (!Files.isDirectory(sourceFile)
            && !Files.isDirectory(targetFile) && filter.accept(sourceFile))
        {
            copyFile(sourceFile, targetFile);
        } else {
            throw new UnsupportedOperationException(
                "Can only copy directory to directory or file to file: "
                    + sourceFile.toAbsolutePath().toString() + " --> "
                    + targetFile.toAbsolutePath().toString());
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
    public static void recursiveMirror(Path source, Path target)
        throws IOException
    {
        recursiveMirror(source, target, new Filter<Path>() {
            @Override
            public boolean accept(Path pathname) {
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
    public static void recursiveMirror(Path source, Path target,
        Filter<Path> filter) throws IOException
    {
        Reject.ifNull(source, "Source directory is null");
        Reject.ifNull(target, "Target directory is null");
        Reject.ifNull(filter, "Filter is null");

        if (Files.notExists(source)) {
            // Do nothing.
            return;
        }
        if (Files.isDirectory(source) && Files.notExists(target)) {
            Files.createDirectories(target);
        }
        if (Files.isDirectory(source) && Files.isDirectory(target)) {
            if (filter.accept(target) && isSubdirectory(source, target)) {
                // Need to be careful if copying to a subdirectory,
                // avoid infinite recursion.
                throw new IOException("Copy to a subdirectory not permitted");
            } else {

                Set<String> done = new HashSet<String>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    source, filter)) {
                    for (Path entry : stream) {
                        Path targetDirFile = target
                            .resolve(entry.getFileName());
                        recursiveMirror(entry, targetDirFile, filter);
                        done.add(targetDirFile.getFileName().toString());
                    }
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                    target, filter)) {
                    for (Path entry : stream) {
                        if (done.contains(entry.getFileName().toString())) {
                            continue;
                        }
                        if (Files.isRegularFile(entry)) {
                            try {
                                Files.delete(entry);
                            } catch (IOException ioe) {
                                throw new IOException(
                                    "Unable to delete file in target directory: "
                                        + entry.toAbsolutePath() + ". " + ioe);
                            }
                        } else if (Files.isDirectory(entry)) {
                            recursiveDelete(entry);
                        }
                    }
                }
            }
        } else if (!Files.isDirectory(source) && !Files.isDirectory(target)
            && filter.accept(source))
        {
            copyFile(source, target);
            // Preserve modification date.
            Files
                .setLastModifiedTime(target, Files.getLastModifiedTime(source));
        } else {
            throw new UnsupportedOperationException(
                "Can only copy directory to directory or file to file: "
                    + source.toAbsolutePath() + " --> "
                    + target.toAbsolutePath());
        }
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
    public static byte[] digest(Path file, MessageDigest digest,
        ProgressListener listener) throws IOException, InterruptedException
    {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[BYTE_CHUNK_SIZE];
            long size = Files.size(file);
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
        }
    }

    /**
     * See if 'child' is a subdirectory of 'parent', recursively.
     * 
     * @param parent
     * @param targetChild
     * @return
     */
    public static boolean isSubdirectory(Path parent, Path targetChild) {
        String parentPathString = parent.toAbsolutePath().toString();
        String childPathString = targetChild.getParent().toAbsolutePath()
            .toString();

        if (parentPathString == null || childPathString == null) {
            return false;
        }

        return childPathString.startsWith(parentPathString);
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
    public static Path buildFileFromRelativeName(Path base, String relativeName)
    {
        Reject.ifNull(base, "Need a base directory");
        Reject.ifNull(relativeName, "RelativeName required");
        if (relativeName.indexOf('/') == -1) {
            return base.resolve(relativeName);
        } else {
            String[] parts = relativeName.split("/");
            Path f = base;
            for (String part : parts) {
                f = f.resolve(part);
            }
            return f;
        }
    }

    /**
     * @param file
     * @param directory
     * @return true if a file in inside a directory.
     */
    public static boolean isFileInDirectory(Path file, Path directory) {
        Reject.ifTrue(file == null || directory == null,
            "File and directory may not be null");

        Path fileParent = file.getParent();
        String fileParentPath;
        if (fileParent == null) {
            fileParentPath = file.getFileSystem().getSeparator();
        } else {
            fileParentPath = fileParent.toAbsolutePath().toString();
        }
        String directoryPath = directory.toAbsolutePath().toString();

        if (log.isLoggable(Level.FINER)) {
            log.finer("File parent: " + fileParentPath);
            log.finer("Directory: " + directoryPath);
        }
        return fileParentPath.startsWith(directoryPath);
    }

    private static final long MS_31_OCT_2013 = 1383177600000L;

    /**
     * Set / remove desktop ini in managed folders.
     * 
     * @param controller
     * @param directory
     */
    public static void maintainDesktopIni(Controller controller, Path directory)
    {
        // Only works on Windows
        // Vista you must log off and on again to see change
        if (!OSUtil.isWindowsSystem() || OSUtil.isWebStart()) {
            return;
        }

        // Safty checks.
        if (directory == null || Files.notExists(directory)
            || !Files.isDirectory(directory))
        {
            return;
        }

        // Look for a desktop ini in the folder.
        Path desktopIniFile = directory.resolve(DESKTOP_INI_FILENAME);
        boolean iniExists = Files.exists(desktopIniFile);
        boolean usePfIcon = ConfigurationEntry.USE_PF_ICON
            .getValueBoolean(controller);
        // Migration to 8 SP1: Correct older folder icon setup
        try {
            if (iniExists
                && Files.getLastModifiedTime(desktopIniFile).toMillis() < MS_31_OCT_2013)
            {
                // PFC-1500 / PFC-2373: Migration
                try {
                    Files.delete(desktopIniFile);
                    iniExists = false;
                } catch (IOException ioe) {
                    iniExists = true;
                }
            }
        } catch (IOException ioe) {
            log.info("Could not access last modification date. " + ioe);
            return;
        }
        if (!iniExists && usePfIcon) {
            // Need to set up desktop ini.
            PrintWriter pw = null;
            try {
                Path iconFile = findDistributionFile("Folder.ico");
                if (iconFile == null) {
                    // Try harder, use EXE file icon
                    String exeName = controller.getDistribution()
                        .getBinaryName() + ".exe";
                    iconFile = findDistributionFile(exeName);
                }

                if (iconFile == null || Files.notExists(iconFile)) {
                    return;
                }

                // Write desktop ini directory
                pw = new PrintWriter(Files.newOutputStream(directory
                    .resolve(DESKTOP_INI_FILENAME)));
                pw.println("[.ShellClassInfo]");
                pw.println("ConfirmFileOp=0");
                pw.println("IconFile=" + iconFile.toAbsolutePath());
                pw.println("IconIndex=0");
                pw.println("InfoTip="
                    + Translation.getTranslation("folder.info_tip"));
                // Required on Win7
                pw.println("IconResource=" + iconFile.toAbsolutePath() + ",0");
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
                log.warning("Problem writing Desktop.ini file(s). " + e);
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
            try {
                Files.delete(desktopIniFile);
                setAttributesOnWindows(directory, null, false);
            } catch (IOException ioe) {
                log.info("Could not delete ini file. " + ioe);
            }
        }
    }

    private static Path findDistributionFile(String filename) {
        Path distroFile = Paths.get(".").toAbsolutePath().resolve(filename);
        if (Files.notExists(distroFile)) {
            // Try harder
            distroFile = WinUtils.getProgramInstallationPath()
                .resolve(filename);

            if (Files.notExists(distroFile)) {
                log.fine("Could not find " + distroFile.getFileName() + " at "
                    + distroFile.getParent().toAbsolutePath());
                return null;
            }
        }
        return distroFile;
    }

    /**
     * Method to remove the desktop ini if it exists
     * 
     * @param directory
     */
    public static void deleteDesktopIni(Path directory) {
        // Look for a desktop ini in the folder.
        Path desktopIniFile = directory.resolve(DESKTOP_INI_FILENAME);
        boolean iniExists = Files.exists(desktopIniFile);
        if (iniExists) {
            try {
                Files.delete(desktopIniFile);
                setAttributesOnWindows(directory, null, false);
            } catch (IOException ioe) {
                log.info("Could not delete ini file. " + ioe);
            }
        }
    }

    /**
     * Scans a directory and gets full size of all files and count of files.
     * 
     * @param directory
     * @return the size in byte of the directory [0] and count of files [1].
     */
    public static Long[] calculateDirectorySizeAndCount(Path directory) {
        return calculateDirectorySizeAndCount0(directory, 0);
    }

    private static Long[] calculateDirectorySizeAndCount0(Path directory,
        int depth)
    {

        // Limit evil recursive symbolic links.
        if (depth == 100) {
            return new Long[]{0L, 0L};
        }

        long sum = 0;
        long count = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    Long[] longs = calculateDirectorySizeAndCount0(file,
                        depth + 1);
                    sum += longs[0];
                    count += longs[1];
                } else {
                    sum += Files.size(file);
                    count++;
                }
            }
        } catch (IOException ioe) {
            return new Long[]{0L, 0L};
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
    public static void zipFile(Path file, Path zipfile) throws IOException {
        // Check that the directory is a directory, and get its contents
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Not a file:  " + file);
        }
        ZipOutputStream out = new ZipOutputStream(
            Files.newOutputStream(zipfile));
        InputStream in = Files.newInputStream(file); // Stream to read file
        ZipEntry entry = new ZipEntry(file.getFileName().toString()); // Make a
                                                                      // ZipEntry
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
     * Copies a given amount of data from one RandomAccessFile to another.
     * 
     * @param in
     *            the inputstream to read the data from
     * @param out
     *            the file to write the data to
     * @param n
     *            the amount of bytes to transfer
     * @throws IOException
     *             if an Exception occurred while reading or writing the data
     */
    public static void ncopy(InputStream in, RandomAccessFile out, int n)
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
    public static boolean openFile(Path file) {
        Reject.ifNull(file, "File is null");

        if (Desktop.isDesktopSupported()) {
            try {
                if (OSUtil.isWindowsSystem() && !Files.isDirectory(file)) {
                    Runtime.getRuntime().exec(
                        "rundll32 SHELL32.DLL,ShellExec_RunDLL \""
                            + file.toString() + "\"");
                } else {
                    Desktop.getDesktop().open(file.toFile());
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
                    "/usr/bin/xdg-open " + file.toUri().toString());
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
    public static boolean setAttributesOnWindows(Path file, Boolean hidden,
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
            s += " \"" + file.toAbsolutePath().toString() + '\"';
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
     * Do not scan POWERFOLDER_SYSTEM_SUBDIR (".PowerFolder").
     * 
     * @param file
     *            Guess what
     * @param folder
     *            Guess what
     * @return true if file scan is allowed
     */
    public static boolean isScannable(Path file, Folder folder) {
        return isScannable(file.toString(), folder);
    }

    /**
     * Do not scan POWERFOLDER_SYSTEM_SUBDIR (".PowerFolder").
     * 
     * @param relOrAbsfilePath
     *            The relative OR absolute path.
     * @param folder
     *            Guess what
     * @return true if file scan is allowed
     */
    public static boolean isScannable(String relOrAbsfilePath, Folder folder) {
        if (relOrAbsfilePath.endsWith(Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR))
        {
            return false;
        }

        if (relOrAbsfilePath.endsWith("Icon\r")) {
            return false;
        }

        if (ConfigurationEntry.ARCHIVE_DIRECTORY_NAME.hasValue(folder
            .getController()))
        {
            String archiveDirText = ConfigurationEntry.ARCHIVE_DIRECTORY_NAME
                .getValue(folder.getController());
            archiveDirText = archiveDirText.replace(".", "");
            archiveDirText = archiveDirText.replace("\\", "");
            archiveDirText = archiveDirText.replace("/", "");

            if (relOrAbsfilePath.contains(archiveDirText)) {
                return false;
            }
        }

        int firstSystemDir = relOrAbsfilePath
            .indexOf(Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (firstSystemDir < 0) {
            return true;
        }

        if (folder.getInfo().isMetaFolder()) {
            // MetaFolders are in the POWERFOLDER_SYSTEM_SUBDIR of the parent,
            // like
            // C:\Users\Harry\PowerFolders\1765X\.PowerFolder\meta\xyz
            // So look after the '.PowerFolder\meta' part
            int metaDir = relOrAbsfilePath.indexOf(Constants.METAFOLDER_SUBDIR,
                firstSystemDir);
            if (metaDir >= 0) {
                // File is somewhere in the metaFolder file structure.
                // Make sure we are not in the metaFolder's system subdir.
                int secondSystemDir = relOrAbsfilePath.indexOf(
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
    public static boolean hasContents(Path base) {
        Reject.ifNull(base, "Base is null");
        Reject.ifFalse(Files.isDirectory(base), "Base is not folder");

        Filter<Path> filter = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                return !entry.getFileName().toString()
                    .equals(Constants.POWERFOLDER_SYSTEM_SUBDIR);
            }
        };

        try (DirectoryStream<Path> contents = Files.newDirectoryStream(base,
            filter)) {

            if (contents == null) {
                return false;
            }

            Iterator<Path> it = contents.iterator();

            if (it == null) {
                return false;
            }

            return it.hasNext();
        } catch (IOException ioe) {
            log.info("Could not check for content. " + ioe);
        }

        return false;
    }

    /**
     * Does a directory have any files, recursively? This ignores the
     * .PowerFolder dir.
     * 
     * @param base
     * @return
     * @throws IllegalArgumentException
     */
    public static boolean hasFiles(Path base) {
        Reject.ifNull(base, "Base is null");
        Reject.ifFalse(Files.isDirectory(base), "Base is not folder");
        return hasFilesInternal(base, 0);
    }

    private static boolean hasFilesInternal(Path dir, int depth) {
        if (depth > 100) {
            // Smells fishy. Should not be this deep into the structure.
        }
        if (dir.getFileName().toString()
            .equals(Constants.POWERFOLDER_SYSTEM_SUBDIR))
        {
            // Don't care about our .PowerFolder files, just the user's stuff.
            return false;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            for (Path file : files) {
                if (Files.isDirectory(file)) {
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
        } catch (IOException ioe) {
            log.info(ioe.getMessage());
            return false;
        }
    }
}
