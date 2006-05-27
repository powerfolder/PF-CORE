/* $Id: Shares.java,v 1.2 2005/03/13 12:38:59 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Class manages all shares additonal to the folders. Shares are search paths,
 * which may also hold the files of a folder.
 * 
 * TODO: Complete this class !
 * TODO: Find a better name
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class Shares extends PFComponent {
    /** Contains the name -> File[] of all shares to speed up search */
    private Map fileDatabase;

    /**
     * Contains a list of <code>File</code>s, where to search for additinal
     * files.
     */
    private List shareLocations;

    /**
     * @param controller
     */
    public Shares(Controller controller) {
        super(controller);
        fileDatabase = Collections.synchronizedMap(new HashMap());
        shareLocations = Collections.synchronizedList(new ArrayList());
    }

    /**
     * Tries to find a file in the shares. Returns the file or null if file was
     * not found
     * 
     * @param fInfo
     *            the folder file
     * @return
     */
    public File findFile(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("FileInfo is null");
        }
        log().warn("Searching for file " + fInfo);
        String fileName = fInfo.getFilenameOnly();
        File[] possibleFiles = (File[]) fileDatabase.get(fileName);

        if (possibleFiles == null || possibleFiles.length == 0) {
            return null;
        }

        // Search in possible matches
        File file = null;
        for (int i = 0; i < possibleFiles.length; i++) {
            if (matches(possibleFiles[i], fInfo)) {
                file = possibleFiles[i];
                break;
            }
        }

        if (file != null && file.exists() && file.isFile()) {
            log().warn("File found at " + file.getAbsolutePath());
        } else {
            // File not found/good
            file = null;
        }

        return file;
    }

    /**
     * Adds a directory as a share to the shares. Files will be found there
     * 
     * @param directory
     */
    public void addShare(File directory) {
        if (directory == null) {
            throw new NullPointerException("Share directory is null");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Share is not a directory");
        }

        // Add to share locations
        shareLocations.add(directory);

        // Begin scan
        log().warn("Adding share at " + directory.getAbsolutePath());

        // Scan share now
        int nFiles = scanShare(directory);

        log().warn(
            nFiles + " Files found in share, database size: "
                + fileDatabase.size());
    }

    /**
     * Scans a file for building up the database. Recursively scans
     * subdirectories
     * 
     * @param file
     *            the file to scan
     * @return number of files scanned
     */
    private int scanShare(File file) {
        int nFiles = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                nFiles += scanShare(files[i]);
            }
        } else if (file.isFile() && file.exists()) {
            // Add file to share database
            addToFileDatabase(file);
            nFiles = 1;
        }
        return nFiles;
    }

    /**
     * Adds a file to the internal files database
     * 
     * @param file
     *            the file to add
     */
    private void addToFileDatabase(File file) {
        File[] dbFiles = (File[]) fileDatabase.get(file.getName());
        if (dbFiles == null) {
            // New file entry
            dbFiles = new File[1];
            dbFiles[0] = file;
            fileDatabase.put(file.getName(), dbFiles);
            return;
        }

        // Add to file
        File[] oldDbFiles = dbFiles;
        dbFiles = new File[oldDbFiles.length + 1];
        System.arraycopy(oldDbFiles, 0, dbFiles, 1, oldDbFiles.length);
        dbFiles[0] = file;
        fileDatabase.put(file.getName(), dbFiles);
    }

    /**
     * Checks if the diskfile matches the fileinfo
     * 
     * @param file
     *            the disk file
     * @param fInfo
     *            the folder file
     * @return true if matches by name, size and date
     */
    private boolean matches(File file, FileInfo fInfo) {
        boolean nameMatches = file.getName().equals(fInfo.getFilenameOnly());
        boolean sizeMatches = file.length() == fInfo.getSize();
        boolean dateMatches = new Date(file.lastModified()).equals(fInfo
            .getModifiedDate());

        boolean fileMatches = nameMatches && sizeMatches && dateMatches;
        return fileMatches;
    }
}