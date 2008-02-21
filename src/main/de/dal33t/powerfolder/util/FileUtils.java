package de.dal33t.powerfolder.util;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.Date;
import java.util.Collection;
import java.util.ArrayList;

import de.dal33t.powerfolder.util.os.OSUtil;

public class FileUtils {
    
    private static final Logger LOG = Logger.getLogger(FileUtils.class);
    private static final int BYTE_CHUNK_SIZE = 1024;

    //no instances
    private FileUtils() {
        
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

        byte[] buffer = new byte[BYTE_CHUNK_SIZE];
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
        Reject.ifNull(file, "File is null");

        if (OSUtil.isMacOS()) {
            Runtime.getRuntime().exec("open " + file.getAbsolutePath());
        } else if (OSUtil.isWindowsSystem()) {
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
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }
        try {
            Process proc = Runtime.getRuntime().exec(
                "attrib.exe +h \"" + file.getAbsolutePath() + '\"');
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
     * Makes a directory 'system' on windows system
     *
     * @param file
     * @return
     */
    public static boolean makeSystemOnWindows(File file) {
        if (!OSUtil.isWindowsSystem()) {
            return false;
        }
        try {
            Process proc = Runtime.getRuntime().exec(
                "attrib.exe +s \"" + file.getAbsolutePath() + '\"');
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
        if (!OSUtil.isWindowsSystem() || OSUtil.isWindowsMEorOlder()) {
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
     * Creates a random folder in the user's .PowerFolder dir.
     * @return random file
     * @throws IOException
     */
    public static File createTemporaryDirectory() throws IOException {
        // Create a random temporary directory based on the current time.
        String randomString = new String(Util.encodeHex(Util.md5(String.valueOf(new Date().getTime()).getBytes())));
        File tempDir = new File(System.getProperty("user.home") + "/.PowerFolder/" + randomString);
        if (tempDir.exists()) {
            throw new IOException("Temporary directory " + tempDir + " already exists.");
        }
        if (!tempDir.mkdir()) {
            throw new IOException("Could not create temporary directory " + tempDir);
        }

        return tempDir;
    }

    /**
     * A recursive delete of a directory.
     *
     * @param file directory to delete
     * @throws IOException
     */

    public static void recursiveDelete(File file) throws IOException {
        if (file != null) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File nextFile : files) {
                    recursiveDelete(nextFile);
                }
            }

            if (!file.delete()) {
                throw new IOException("Could not delete file " + file.getAbsolutePath());
            }
        }
    }

    /**
     * A recursive copy of one directory to another.
     *
     * @param originalFile
     * @param targetFile
     * @throws IOException
     */
    public static void recursiveCopy(File originalFile, File targetFile) throws IOException {
        if (originalFile != null && targetFile != null) {
            if (originalFile.isDirectory() && targetFile.isDirectory()) {
                if (!targetFile.exists()) {
                    throw new UnsupportedOperationException("Target directory must exist");
                }

                File[] files = originalFile.listFiles();
                for (File nextOriginalFile : files) {

                    // Synthesize target file name.
                    String lastPart = nextOriginalFile.getName();
                    File nextTargetFile = new File(targetFile, lastPart);

                    if (nextOriginalFile.isDirectory()) {

                        // Create target directory.
                        nextTargetFile.mkdir();

                        // Hide target if original is hidden.
                        if (nextOriginalFile.isHidden()) {
                            makeHiddenOnWindows(nextTargetFile);
                        }
                    }
                    recursiveCopy(nextOriginalFile, nextTargetFile);
                }

            } else if (!originalFile.isDirectory() && !targetFile.isDirectory()) {
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(originalFile);
                    fos = new FileOutputStream(targetFile);
                    byte[] buf = new byte[1024];
                    int i;
                    while ((i = fis.read(buf)) != -1) {
                        fos.write(buf, 0, i);
                    }
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            } else {
                throw new UnsupportedOperationException("Can only copy directory to directory or file to file: " +
                        originalFile.getAbsolutePath() + " --> " +
                        targetFile.getAbsolutePath());
            }
        }
    }

    /**
     * Calculates the MD5 of a file's contents.
     *
     * @param file file to create MD5
     * @return Hex representation of MD5,
     * empty string if there is a problem.
     */
    public static String calculateMD5(File file) {

        // No file --> empty MD5
        if (file == null) {
            return "";
        }

        FileInputStream inputStream = null;
        try {

            // Read file and build collection of 1KB chunks.
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[BYTE_CHUNK_SIZE];
            Collection<byte[]> byteCollection = new ArrayList<byte[]>();
            int bytesRead;
            do {
                bytesRead = inputStream.read(bytes);
                byteCollection.add(bytes);
            } while (bytesRead == BYTE_CHUNK_SIZE);

            // Concatenate the chunks.
            byte[] allBytes = new byte[BYTE_CHUNK_SIZE * byteCollection.size()];
            int i = 0;
            for (byte[] bytes1 : byteCollection) {
                System.arraycopy(bytes1, 0, allBytes, i++ * BYTE_CHUNK_SIZE,
                        bytes1.length);
            }

            // Calculate hash.
            return new String(Util.encodeHex(Util.md5(allBytes)));
        } catch (Exception e) {
            // Failure to calculate hash is not fatal.
            LOG.error("Problem creating MD5", e);
            return "";
        } finally {

            // Close file
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
