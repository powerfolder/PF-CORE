/* $Id: Folder.java,v 1.114 2006/04/30 14:17:45 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.io.*;
import java.util.*;

import javax.swing.tree.MutableTreeNode;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.TreeNodeList;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * The main class representing a folder. Scans for new files automatically.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Folder extends PFComponent {
    public static final String PROPERTY_SYNC_PROFILE = "syncProfile";
    public static final String DB_FILENAME = ".PowerFolder.db";
    public static final String DB_BACKUP_FILENAME = ".PowerFolder.db.bak";
    public final static String PREF_FILE_NAME_CHECK = "folder.check_filenames";

    private File localBase;

    /**
     * Date of the last directory scan
     */
    private Date lastScan;

    /**
     * The internal database of locally known files. Contains FileInfo ->
     * FileInfo
     */
    private Map<FileInfo, FileInfo> knownFiles;

    /** files that should not be downloaded in auto download */
    private Blacklist blacklist;

    /** Map containg the cached File objects */
    private Map<FileInfo, File> diskFileCache;

    /** Lock for scan */
    private Object scanLock = new Object();

    /** All members of this folder */
    private Set<Member> members;
    /** The ui node */
    private TreeNodeList treeNode;

    /** cached Directory object */
    private Directory rootDirectory;

    /**
     * the folder info, contains important information about
     * id/hash/name/filescount
     */
    private FolderInfo currentInfo;

    /** Folders sync profile */
    private SyncProfile syncProfile;

    /**
     * Flag indicating that folder has a set of own know files will be true
     * after first scan ever
     */
    private boolean hasOwnDatabase;

    /** Flag indicating, that the filenames shoule be checked */
    private boolean checkFilenames;

    /** Flag indicating */
    private boolean shutdown;

    /**
     * Indicates, that the scan of the local filesystem was forced
     */
    private boolean scanForced;

    /** The statistic for this folder */
    private FolderStatistic statistic;

    private FolderListener folderListenerSupport;
    private FolderMembershipListener folderMembershipListenerSupport;

    /**
     * Constructor for folder.
     * 
     * @param controller
     * @param fInfo
     * @param localBase
     * @param profile
     *            the syncprofile to use.
     * @throws FolderException
     */
    Folder(Controller controller, FolderInfo fInfo, File localBase,
        SyncProfile profile) throws FolderException
    {
        super(controller);

        if (fInfo == null) {
            throw new NullPointerException("FolderInfo is null");
        }
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.currentInfo = new FolderInfo(fInfo.name, fInfo.id, fInfo.secret);

        if (fInfo.name == null) {
            throw new NullPointerException("Folder name is null");
        }
        if (fInfo.id == null) {
            throw new NullPointerException("Folder id (" + fInfo.id
                + ") is null");
        }
        if (localBase == null) {
            throw new NullPointerException("Folder localdir is null");
        }
        Reject.ifNull(profile, "Sync profile is null");

        // Not until first scan or db load
        this.hasOwnDatabase = false;
        // this.shutdown = false;
        this.localBase = localBase;
        // Should we check filenames?
        this.checkFilenames = getController().getPreferences().getBoolean(
            PREF_FILE_NAME_CHECK, true); // default = true
        this.syncProfile = profile;

        // Create listener support
        this.folderListenerSupport = (FolderListener) ListenerSupportFactory
            .createListenerSupport(FolderListener.class);

        this.folderMembershipListenerSupport = (FolderMembershipListener) ListenerSupportFactory
            .createListenerSupport(FolderMembershipListener.class);

        // Check base dir
        checkBaseDir(localBase);

        statistic = new FolderStatistic(this);
        knownFiles = Collections
            .synchronizedMap(new HashMap<FileInfo, FileInfo>());
        members = Collections.synchronizedSet(new HashSet<Member>());
        diskFileCache = new WeakHashMap<FileInfo, File>();

        // put myself in membership
        join(controller.getMySelf());

        log().debug(
            "Opening " + this.toString() + " at '"
                + localBase.getAbsolutePath() + "'");

        if (localBase.list().length == 0) {
            // Empty folder... no scan required for database
            hasOwnDatabase = true;
        }

        blacklist = new Blacklist();
        blacklist.loadPatternsFrom(getSystemSubDir());
        // Load folder database
        loadFolderDB(); // will also read the blacklist

        // Force the next time scan if autodetect is set
        if (syncProfile.isAutoDetectLocalChanges()) {
            forceScanOnNextMaintenance();
        }

        // // maintain desktop shortcut if wanted
        // setDesktopShortcut();
        if (logVerbose) {
            log().verbose(
                "Has own database (" + getName() + ")? " + hasOwnDatabase);
        }
        if (hasOwnDatabase) {
            // Write filelist
            if (Logger.isLogToFileEnabled()) {
                // Write filelist to disk
                File debugFile = new File(Logger.getDebugDir(), getName() + "/"
                    + getController().getMySelf().getNick() + ".list.txt");
                Debug.writeFileList(knownFiles.keySet(), "FileList of folder "
                    + getName() + ", member " + this + ":", debugFile);
            }
        }
    }

    /**
     * Commits the scan results into the internal file database. Changes get
     * broadcasted to other members if nessesary. public because also called
     * from SyncFolderPanel (until that class maybe handles that itself)
     * 
     * @param scanResult
     *            the scanresult to commit.
     */
    public void commitScanResult(ScanResult scanResult) {
        if (!FolderRepository.USE_NEW_SCANNING_CODE) {
            throw new IllegalStateException("New scanning code not enabled!");
        }
        final List<FileInfo> fileInfosToConvert = new ArrayList<FileInfo>();
        // new files
        for (FileInfo newFileInfo : scanResult.getNewFiles()) {
            FileInfo old = knownFiles.put(newFileInfo, newFileInfo);
            if (old != null) {
                log().error("hmmzzz it was new!?!?!?!");
                // Remove old file from info
                currentInfo.removeFile(old);
            }
            // Add file to folder
            currentInfo.addFile(newFileInfo);

            // Add to the UI
            getDirectory().add(getController().getMySelf(), newFileInfo);

            // if meta then add the meta scan queue
            if (FileMetaInfoReader.isConvertingSupported(newFileInfo)) {
                fileInfosToConvert.add(newFileInfo);
            }
        }

        // deleted files
        for (FileInfo deletedFileInfo : scanResult.getDeletedFiles()) {
            deletedFileInfo.setDeleted(true);
            deletedFileInfo.setVersion(deletedFileInfo.getVersion() + 1);
            deletedFileInfo.setModifiedInfo(getController().getMySelf()
                .getInfo(), new Date());
        }

        // restored files
        for (FileInfo restoredFileInfo : scanResult.getRestoredFiles()) {
            File diskFile = getDiskFile(restoredFileInfo);
            restoredFileInfo.setModifiedInfo(getController().getMySelf()
                .getInfo(), new Date(diskFile.lastModified()));
            restoredFileInfo.setSize(diskFile.length());
            restoredFileInfo.setDeleted(false);
            restoredFileInfo.setVersion(restoredFileInfo.getVersion() + 1);
        }

        // changed files
        for (FileInfo changedFileInfo : scanResult.getChangedFiles()) {
            File diskFile = getDiskFile(changedFileInfo);
            changedFileInfo.setModifiedInfo(getController().getMySelf()
                .getInfo(), new Date(diskFile.lastModified()));
            changedFileInfo.setSize(diskFile.length());
            changedFileInfo.setDeleted(!diskFile.exists());
            changedFileInfo.setVersion(changedFileInfo.getVersion() + 1);
        }

        if (scanResult.getNewFiles().size() > 0
            || scanResult.getChangedFiles().size() > 0
            || scanResult.getDeletedFiles().size() > 0
            || scanResult.getRestoredFiles().size() > 0)
        {
            // broadcast new files on folder
            // TODO: Broadcast only changes !! FolderFilesChanged
            broadcastFileList();
            folderChanged();
        }

        hasOwnDatabase = true;
        lastScan = new Date();
        if (logEnabled) {
            log().debug(
                "Scanned " + scanResult.getTotalFilesCount() + " total, "
                    + scanResult.getChangedFiles().size() + " changed, "
                    + scanResult.getNewFiles().size() + " new, "
                    + scanResult.getRestoredFiles().size() + " restored, "
                    + scanResult.getDeletedFiles().size() + " removed");
        }

        // in new files are found we can convert to meta info please do so..
        if (fileInfosToConvert.size() > 0) {
            // do converting in a differnend Thread
            Runnable runner = new Runnable() {
                public void run() {
                    List<FileInfo> converted = getController()
                        .getFolderRepository().getFileMetaInfoReader().convert(
                            Folder.this, fileInfosToConvert);
                    if (logEnabled) {
                        log().debug("Converted: " + converted);
                    }
                    for (FileInfo convertedFileInfo : converted) {
                        FileInfo old = knownFiles.put(convertedFileInfo,
                            convertedFileInfo);
                        if (old != null) {
                            // Remove old file from info
                            currentInfo.removeFile(old);
                        }
                        // Add file to folder
                        currentInfo.addFile(convertedFileInfo);

                        // update UI
                        getDirectory().add(getController().getMySelf(),
                            convertedFileInfo);
                    }
                    folderChanged();
                }
            };
            Thread thread = new Thread(runner);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    public boolean hasOwnDatabase() {
        return hasOwnDatabase;
    }

    /** package protected, used by FolderScanner */
    HashMap<FileInfo, FileInfo> getKnownFiles() {
        synchronized (knownFiles) {
            return new HashMap<FileInfo, FileInfo>(knownFiles);
        }
    }

    public Blacklist getBlacklist() {
        return blacklist;
    }

    /**
     * Checks the basedir is valid
     * 
     * @param baseDir
     *            the base dir to test
     * @throws FolderException
     *             if base dir is not ok
     */
    private void checkBaseDir(File baseDir) throws FolderException {
        // Basic checks
        if (!localBase.exists()) {
            if (!localBase.mkdirs()) {
                log().error(
                    " not able to create folder(" + getName()
                        + "), (sub) dir (" + localBase + ") creation failed");
                throw new FolderException(getInfo(), Translation
                    .getTranslation("foldercreate.error.unable_to_create",
                        localBase.getAbsolutePath()));
            }
        } else if (!localBase.isDirectory()) {
            log().error(
                " not able to create folder(" + getName() + "), (sub) dir ("
                    + localBase + ") is no dir");
            throw new FolderException(getInfo(), Translation.getTranslation(
                "foldercreate.error.unable_to_open", localBase
                    .getAbsolutePath()));
        }

        // Complex checks
        FolderRepository repo = getController().getFolderRepository();
        if (new File(repo.getFoldersBasedir()).equals(baseDir)) {
            throw new FolderException(getInfo(), Translation.getTranslation(
                "foldercreate.error.it_is_base_dir", baseDir.getAbsolutePath()));
        }
        // FIXME This one does not happen here (Jan)
        // I can choose a base dir that allready has a powerfolder in it
        FolderInfo[] folderInfos = repo.getJoinedFolderInfos();
        for (FolderInfo folderInfo : folderInfos) {
            Folder folder = repo.getFolder(folderInfo);
            if (folder != null) {
                if (folder.getLocalBase().equals(baseDir)) {
                    throw new FolderException(getInfo(), Translation
                        .getTranslation("foldercreate.error.already_taken",
                            folder.getName(), baseDir.getAbsolutePath()));
                }
            }
        }
    }

    /*
     * Local disk/folder management *******************************************
     */

    /**
     * Scans a new File, eg from (drag and) drop.
     * 
     * @param fileInfo
     *            the file to scan
     */
    public void scanNewFile(FileInfo fileInfo) {
        if (scanFile(fileInfo)) {
            folderChanged();
            statistic.scheduleCalculate();
        }
    }

    /**
     * Scans a file that was restored from the recyle bin
     * 
     * @param fileInfo
     *            the file to scan
     */
    public void scanRestoredFile(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        if (scanFile(fileInfo)) {
            folderChanged();
            statistic.scheduleCalculate();

            FileInfo localInfo = getFile(fileInfo);
            broadcastMessage(new FolderFilesChanged(localInfo));
        }
    }

    /**
     * Scans a downloaded file, renames tempfile to real name Moves possible
     * existing file to PowerFolder recycle bin.
     * 
     * @param fInfo
     */
    public void scanDownloadFile(FileInfo fInfo, File tempFile) {
        synchronized (scanLock) {
            // rename file
            File targetFile = fInfo.getDiskFile(getController()
                .getFolderRepository());
            if (targetFile.exists()) {
                // if file was a "newer file" the file already esists here
                if (logVerbose) {
                    log().verbose(
                        "file already exists: " + targetFile
                            + " moving to recycle bin");
                }
                // move to recycle bin
                if (!getController().getRecycleBin().moveToRecycleBin(fInfo,
                    targetFile))
                {
                    log().warn("move to recycle bin failed!: " + targetFile);
                    if (!targetFile.delete()) {
                        log().warn(
                            "delete of file to replace failed!: " + targetFile);
                    }
                }
            }
            if (!tempFile.renameTo(targetFile)) {
                log().warn(
                    "Was not able to rename tempfile, copiing "
                        + tempFile.getAbsolutePath());
                try {
                    FileUtils.copyFile(tempFile, targetFile);
                } catch (IOException e) {
                    // TODO give a diskfull warning?
                    log().verbose(e);
                    log().error(
                        "Unable to store completed download "
                            + targetFile.getAbsolutePath() + ". "
                            + e.getMessage());
                }
            }

            if (tempFile.exists() && !tempFile.delete()) {
                log().error("Unable to remove temp file: " + tempFile);
            }

            // Set modified date of remote
            targetFile.setLastModified(fInfo.getModifiedDate().getTime());

            // Update internal database
            FileInfo dbFile = getFile(fInfo);
            if (dbFile != null) {
                // Update database
                dbFile.setModifiedInfo(fInfo.getModifiedBy(), fInfo
                    .getModifiedDate());
                dbFile.setVersion(fInfo.getVersion());
                dbFile.setSize(fInfo.getSize());
            } else {
                // File new, scan
                scanFile(fInfo);
            }

            // Folder has changed
            folderChanged();
            // Fire just change, store comes later
            // fireFolderChanged();
        }

        // re-calculate statistics
        statistic.scheduleCalculate();

        // Broadcast
        broadcastMessage(new FolderFilesChanged(fInfo));
    }

    /**
     * Scans the local directory for new files. Be carefull! This method is not
     * Thread save. in most cases you want to use forceScanOnNextMaintenance()
     * followed by maintain().
     * 
     * @param force
     *            if the scan should be forced.
     * @return if the local files where scanned
     */
    private boolean scanLocalFilesNew(boolean force) {
        if (!force) {
            if (!getSyncProfile().isAutoDetectLocalChanges()) {
                if (logVerbose) {
                    log().verbose("Skipping scan");
                }
                return false;
            }
            if (lastScan != null) {
                long minutesSinceLastSync = (System.currentTimeMillis() - lastScan
                    .getTime()) / 60000;
                if (minutesSinceLastSync < syncProfile.getMinutesBetweenScans())
                {
                    if (logVerbose) {
                        log().verbose("Skipping scan");
                    }
                    return false;
                }
            }
        }

        FolderScanner scanner = getController().getFolderRepository()
            .getFolderScanner();
        ScanResult result = scanner.scanFolder(this);
        log().debug("Scan result: " + result.getResultState());

        if (result.getResultState().equals(ScanResult.ResultState.SCANNED)) {
            if (result.getProblemFiles().size() > 0) {
                FileNameProblemHandler handler = getController()
                    .getFolderRepository().getFileNameProblemHandler();
                if (handler != null) {
                    handler.fileNameProblemsDetected(new FileNameProblemEvent(
                        this, result));
                }
            }
            commitScanResult(result);
            findSameFilesOnRemote();
            return true;
        }

        // scan aborted or hardware broken?
        return false;
    }

    /**
     * Scans the local directory for new files. Be carefull! This method is not
     * Thread save. in most cases you want to use forceScanOnNextMaintenance()
     * followed by maintain().
     * 
     * @param force
     *            if the scan should be forced.
     * @return if the local files where scanned
     */
    public boolean scanLocalFiles(boolean force) {
        if (FolderRepository.USE_NEW_SCANNING_CODE) {
            return scanLocalFilesNew(force);
        }
        return scanLocalFilesOld(force);
    }

    /**
     * Scans the local directory for new files. Be carefull! This method is not
     * Thread save. in most cases you want to use forceScanOnNextMaintenance()
     * followed by maintain().
     * 
     * @param force
     *            if the scan should be forced.
     * @return if the local files where scanned
     */
    private boolean scanLocalFilesOld(boolean force) {
        if (!force) {
            if (!getSyncProfile().isAutoDetectLocalChanges()) {
                if (logVerbose) {
                    log().verbose("Skipping scan");
                }
                return false;
            }
            if (lastScan != null) {
                long minutesSinceLastSync = (System.currentTimeMillis() - lastScan
                    .getTime()) / 60000;
                if (minutesSinceLastSync < syncProfile.getMinutesBetweenScans())
                {
                    if (logVerbose) {
                        log().verbose("Skipping scan");
                    }
                    return false;
                }
            }
        }

        log().debug("Scanning files");

        int totalFiles;
        int changedFiles;
        int removedFiles;
        Set<FileInfo> remaining;

        synchronized (this) {
            log().verbose("Begin scanning");
            remaining = new HashSet<FileInfo>(knownFiles.keySet());
            totalFiles = knownFiles.size();
            // Scan files
            changedFiles = scanLocalFile(localBase, remaining);
            if (changedFiles < 0) {
                // -1 = error reading device, stop scan
                log().error(
                    "Scan skipped, unable to access: "
                        + localBase.getAbsolutePath());
                return false;
            }
            removedFiles = 0;

            if (!remaining.isEmpty()) {
                for (Iterator<FileInfo> it = remaining.iterator(); it.hasNext();)
                {
                    FileInfo fInfo = it.next();
                    // if (!fInfo.isDeleted()) {

                    boolean changed = fInfo.syncFromDiskIfRequired(
                        getController(), fInfo.getDiskFile(getController()
                            .getFolderRepository()));
                    // fInfo.setDeleted(true);
                    // fInfo.setModifiedInfo(getController().getMySelf()
                    // .getInfo(), fInfo.getModifiedDate());
                    // log()
                    // .verbose("File removed: " + fInfo.toDetailString());

                    if (changed) {
                        // Increase fileversion
                        removedFiles++;
                    } else {
                        // File state correct in database, remove from remaining
                        it.remove();
                    }

                    // } else {
                    // // File state correct in database, remove from remaining
                    // it.remove();
                    // }
                }

                if (logVerbose) {
                    log().verbose(
                        this.toString()
                            + "These files were deleted from local disk: "
                            + remaining);
                }
            }
        }

        findSameFilesOnRemote();

        if (changedFiles > 0 || removedFiles > 0) {
            // broadcast new files on folder
            // TODO: Broadcast only changes !! FolderFilesChanged
            broadcastFileList();
            folderChanged();
        }

        hasOwnDatabase = true;
        lastScan = new Date();
        log().debug(
            this.toString() + ": -> Scanned folder <- " + totalFiles
                + " files total, " + changedFiles + " new/changed, "
                + remaining.size() + " removed");

        return true;
    }

    /**
     * Scans just one file/directory and checks if its included in internal
     * database
     * 
     * @param file
     * @return the count of new/changed files
     */
    private int scanLocalFile(File file, Set remaining) {
        synchronized (scanLock) {
            if (file.isDirectory()) {
                if (file.equals(getSystemSubDir())
                    || getController().getRecycleBin().isRecycleBin(this, file))
                {
                    // Skipping these subdirs
                    if (logVerbose) {
                        log()
                            .verbose(
                                "Skipping system subdir: "
                                    + file.getAbsolutePath());
                    }
                    return 0;
                }
                if (logVerbose) {
                    log().verbose(
                        "Scanning directory: " + file.getAbsolutePath());
                }

                File[] subFiles = file.listFiles();
                int changedFiles = 0;
                if (subFiles.length > 0) {
                    for (File subFile : subFiles) {
                        int chngInSub = scanLocalFile(subFile, remaining);
                        if (chngInSub < 0) {
                            // -1 = error reading device, stop scan
                            return -1;
                        }
                        changedFiles += chngInSub;
                    }
                } else {
                    // directory empty, remove it
                    if (!file.equals(localBase)) {
                        file.delete();
                    }
                }
                // try {
                // // sleep 2 ms to give cpu time
                // Thread.sleep(5);
                // } catch (InterruptedException e) {
                // log().verbose(e);
                // }
                return changedFiles;
            } else if (file.isFile()) {
                FileInfo fInfo = new FileInfo(this, file);
                // file is not longer remaining
                remaining.remove(fInfo);
                int nfiles = scanFile(fInfo) ? 1 : 0;
                // try {
                // // sleep 2 ms to give cpu time
                // Thread.sleep(2);
                // } catch (InterruptedException e) {
                // log().verbose(e);
                // }
                return nfiles;
            } else {
                // Hardware not longer available? BREAK scan!
                log().warn(
                    "Unkown file found/Not longer accessible: "
                        + file.getAbsolutePath());
                // -1 = error reading device, stop scan
                return -1;
            }
        }
    }

    /**
     * Scans one file
     * <p>
     * Package protected because used by Recylcebin to tell, that file was
     * restored
     * 
     * @param fInfo
     *            the file to be scanned
     * @return true if the file was successfully scanned
     */
    private boolean scanFile(FileInfo fInfo) {
        synchronized (scanLock) {
            if (fInfo == null) {
                throw new NullPointerException("File is null");
            }
            // First relink modified by memberinfo to
            // actual instance if available on nodemanager
            Member from = getController().getNodeManager().getNode(
                fInfo.getModifiedBy());
            if (from != null) {
                fInfo.setModifiedInfo(from.getInfo(), fInfo.getModifiedDate());
            }

            if (logVerbose) {
                log().verbose(
                    "Scanning file: " + fInfo + ", folderId: " + fInfo);
            }
            File file = getDiskFile(fInfo);

            if (!file.canRead()) {
                // ignore not readable
                log().warn("File not readable: " + file);
                return false;
            }

            // ignore our database file
            if (file.getName().equals(DB_FILENAME)
                || file.getName().equals(DB_BACKUP_FILENAME))
            {
                if (!file.isHidden()) {
                    FileUtils.makeHiddenOnWindows(file);
                }
                log().verbose("Ignoring folder database file: " + file);
                return false;
            }

            // check for incomplete download files and delete them, if
            // the real file exists
            if (FileUtils.isTempDownloadFile(file)) {
                if (FileUtils.isCompletedTempDownloadFile(file)
                    || fInfo.isDeleted())
                {
                    log().verbose("Removing temp download file: " + file);
                    file.delete();
                } else {
                    log().verbose("Ignoring incomplete download file: " + file);
                }
                return false;
            }

            // ignore placeholderfiles or remove them if file exists
            if (FileUtils.isPlaceHolderFile(file)) {
                if (!syncProfile.isCreatePlaceHolderFiles()) {
                    log().verbose("Removing placeholder file: " + file);
                    file.delete();
                } else {
                    log().verbose("Ignoring placeholder file: " + file);
                }
                return false;
            }

            // ignore a copy backup tempfile
            // created on copy process if overwriting file
            if (FileCopier.isTempBackup(file)) {
                return false;
            }

            // remove placeholder file if exist for this file
            // Commented, no useful until full implementation of placeholder
            // file
            /*
             * if (file.exists()) { File placeHolderFile =
             * Util.getPlaceHolderFile(file); if (placeHolderFile.exists()) {
             * log().verbose("Removing placeholder file for: " + file);
             * placeHolderFile.delete(); } }
             */
            if (checkFilenames) {
                checkFileName(fInfo);
            }

            synchronized (knownFiles) {
                // link new file to our folder
                fInfo.setFolder(this);
                if (!isKnown(fInfo)) {
                    if (logVerbose) {
                        log().verbose(
                            fInfo + ", modified by: " + fInfo.getModifiedBy());
                    }
                    // Update last - modified data
                    MemberInfo modifiedBy = fInfo.getModifiedBy();
                    if (modifiedBy == null) {
                        modifiedBy = getController().getMySelf().getInfo();
                    }

                    if (file.exists()) {
                        fInfo.setModifiedInfo(modifiedBy, new Date(file
                            .lastModified()));
                        fInfo.setSize(file.length());
                    }

                    // add file to folder
                    FileInfo converted = convertToMetaInfoFileInfo(fInfo);
                    addFile(converted);

                    // update directory
                    // don't do this in the server version
                    if (rootDirectory != null) {
                        getDirectory().add(getController().getMySelf(),
                            converted);
                    }
                    // get folder icon info and set it
                    if (FileUtils.isDesktopIni(file)) {
                        makeFolderIcon(file);
                    }

                    // Fire folder change event
                    // fireEvent(new FolderChanged());

                    if (logVerbose) {
                        log().verbose(
                            this.toString() + ": Local file scanned: "
                                + fInfo.toDetailString());
                    }
                    return true;
                }

                // Now process/check existing files
                FileInfo dbFile = getFile(fInfo);

                boolean fileChanged = dbFile.syncFromDiskIfRequired(
                    getController(), file);

                // Convert to meta info loaded fileinfo if nessesary
                FileInfo convertedFile = convertToMetaInfoFileInfo(dbFile);
                if (convertedFile != dbFile) {
                    // DO A IDENTITY MATCH, THIS HERE MEANS, A META INFO
                    // FILEINFO WAS CREATED
                    addFile(convertedFile);
                    fileChanged = true;
                }

                if (logVerbose) {
                    log().verbose("File already known: " + fInfo);
                }

                return fileChanged;
            }
        }
    }

    /**
     * Checks a single filename if there are problems with the name
     * 
     * @param fileInfo
     */
    private void checkFileName(FileInfo fileInfo) {
        if (!checkFilenames) {
            return;
        }
        String totalName = localBase.getName() + fileInfo.getName();

        if (totalName.length() >= 255) {
            log().warn("path maybe to long: " + fileInfo);
            // path may become to long
            if (UIUtil.isAWTAvailable() && !getController().isConsoleMode()) {
                String title = Translation
                    .getTranslation("folder.check_path_length.title");
                String message = Translation.getTranslation(
                    "folder.check_path_length.text", getName(), fileInfo
                        .getName());
                String neverShowAgainText = Translation
                    .getTranslation("folder.check_path_length.never_show_again");
                boolean showAgain = DialogFactory
                    .showNeverAskAgainMessageDialog(getController()
                        .getUIController().getMainFrame().getUIComponent(),
                        title, message, neverShowAgainText);
                if (!showAgain) {
                    getController().getPreferences().putBoolean(
                        PREF_FILE_NAME_CHECK, false);
                    log().warn("store do not show this dialog again");
                }
            } else {
                log().warn(
                    "Path maybe to long for folder: " + getName() + " File:"
                        + fileInfo.getName());
            }
        }
        // check length of path elements and filename
        // TODO: The check if 30 chars or more cuurrently disabled
        // Should only check if on mac system
        // Jan: No longer needed since there is no java 1.5 for mac classic!

        // String[] parts = totalName.split("\\/");
        // for (String pathPart : parts) {
        // if (pathPart.length() > 30) {
        // log().warn("filename or dir maybe to long: " + fileInfo);
        // // (sub)directory name or filename to long
        // if (Util.isAWTAvailable() && !getController().isConsoleMode()) {
        // String title = Translation
        // .getTranslation("folder.check_subdirectory_and_filename_length.title");
        // String message = Translation.getTranslation(
        // "folder.check_subdirectory_and_filename_length.text",
        // getName(), fileInfo.getName());
        // String neverShowAgainText = Translation
        // .getTranslation("folder.check_subdirectory_and_filename_length.never_show_again");
        // boolean showAgain = DialogFactory
        // .showNeverAskAgainMessageDialog(getController()
        // .getUIController().getMainFrame().getUIComponent(),
        // title, message, neverShowAgainText);
        // if (!showAgain) {
        // getController().getPreferences().putBoolean(
        // PREF_FILE_NAME_CHECK, false);
        // log().warn("store do not show this dialog again");
        // }
        // } else {
        // log().warn(
        // "Filename or subdirectory name maybe to long for folder: "
        // + getName() + " File:" + fileInfo.getName());
        //
        // }
        // }
        // }

    }

    /**
     * Converts a fileinfo into a more detailed meta info loaded fileinfo. e.g.
     * with mp3 tags
     * 
     * @param fInfo
     * @return the new / converted fileinfo.
     */
    private FileInfo convertToMetaInfoFileInfo(FileInfo fInfo) {
        if (!(fInfo instanceof MP3FileInfo)
            && fInfo.getFilenameOnly().toUpperCase().endsWith(".MP3"))
        {
            if (logVerbose) {
                log().verbose("Converting to MP3 TAG: " + fInfo);
            }
            // Not an mp3 fileinfo ? convert !
            File diskFile = fInfo.getDiskFile(getController()
                .getFolderRepository());
            // Create mp3 fileinfo
            MP3FileInfo mp3FileInfo = new MP3FileInfo(this, diskFile);
            mp3FileInfo.copyFrom(fInfo);
            return mp3FileInfo;
        }

        if (FolderRepository.READ_IMAGE_META_INFOS_WITH_OLD_SCANNING
            && !(fInfo instanceof ImageFileInfo) && UIUtil.isAWTAvailable()
            && ImageSupport.isReadSupportedImage(fInfo.getFilenameOnly()))
        {
            if (logVerbose) {
                log().verbose("Converting to Image: " + fInfo);
            }
            File diskFile = fInfo.getDiskFile(getController()
                .getFolderRepository());
            // Create image fileinfo
            ImageFileInfo imageFileInfo = new ImageFileInfo(this, diskFile);
            imageFileInfo.copyFrom(fInfo);
            return imageFileInfo;
        }
        // Otherwise file is correct
        return fInfo;
    }

    /**
     * Adds a file to the internal database, does NOT store the DB
     * 
     * @param fInfo
     */
    private void addFile(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }
        // Add to this folder
        fInfo.setFolder(this);

        FileInfo old = knownFiles.put(fInfo, fInfo);

        if (old != null) {
            // Remove old file from info
            currentInfo.removeFile(old);
        }

        // log().warn("Adding " + fInfo.getClass() + ", removing " + old);

        // Add file to folder
        currentInfo.addFile(fInfo);

    }

    /**
     * Answers if this file is known to the internal db
     * 
     * @param fi
     * @return
     */
    public boolean isKnown(FileInfo fi) {
        return knownFiles.containsKey(fi);
    }

    /**
     * Re-downloads a file. Basically drops a file from the database.
     * 
     * @param file
     *            the file to request
     */
    public void reDownloadFile(FileInfo file) {
        synchronized (scanLock) {
            if (isKnown(file)) {
                log().verbose("File re-requested: " + file);
                currentInfo.removeFile(file);
                knownFiles.remove(file);
            } else {
                throw new IllegalArgumentException(file + " not on " + this);
            }
        }
    }

    /**
     * not used Re-downloads files. Basically drops files from the database.
     * 
     * @param files
     *            the file to request
     */
    /*
     * public void reDownloadFiles(FileInfo[] files) { if (files == null) {
     * throw new NullPointerException("Files to redownload are null"); }
     * synchronized (scanLock) { for (int i = 0; i < files.length; i++) {
     * FileInfo file = files[i]; if (isKnown(file)) { log().verbose("File
     * re-requested: " + file); currentInfo.removeFile(file);
     * knownFiles.remove(file); } else { throw new IllegalArgumentException(file + "
     * not on " + this); } } } folderChanged(); }
     */

    /**
     * Removes a file on local folder, diskfile will be removed and file tagged
     * as deleted
     * 
     * @param fInfo
     * @return true if the folder was changed
     */
    private boolean removeFileLocal(FileInfo fInfo) {
        if (logVerbose) {
            log().verbose(
                "Remove file local: " + fInfo + ", Folder equal ? "
                    + Util.equals(fInfo.getFolderInfo(), getInfo()));
        }
        boolean folderChanged = false;
        if (!isKnown(fInfo)) {
            if (logEnabled) {
                log().warn(
                    "Tried to remove a not-known file: "
                        + fInfo.toDetailString());
            }
            return false;
        }

        // Abort downloads of files
        Download dl = getController().getTransferManager().getActiveDownload(
            fInfo);
        if (dl != null) {
            dl.abortAndCleanup();
        }

        synchronized (scanLock) {
            File diskFile = getDiskFile(fInfo);
            if (diskFile != null && diskFile.exists()) {
                RecycleBin recycleBin = getController().getRecycleBin();
                if (!recycleBin.moveToRecycleBin(fInfo, diskFile)) {
                    log().error("Unable to move to recycle bin" + fInfo);

                    if (!diskFile.delete()) {
                        log().error("Unable to remove file" + fInfo);
                    }
                }
                FileInfo localFile = getFile(fInfo);
                folderChanged = localFile.syncFromDiskIfRequired(
                    getController(), diskFile);
            }
        }

        return folderChanged;
    }

    /**
     * Removes files from the local disk
     * 
     * @param fis
     */
    public void removeFilesLocal(FileInfo[] fis) {
        if (fis == null || fis.length <= 0) {
            throw new IllegalArgumentException("Files to delete are empty");
        }

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        synchronized (scanLock) {
            for (FileInfo fileInfo : fis) {
                if (removeFileLocal(fileInfo)) {
                    removedFiles.add(fileInfo);
                }
            }
        }
        if (!removedFiles.isEmpty()) {
            folderChanged();

            // Broadcast to members
            FolderFilesChanged changes = new FolderFilesChanged(getInfo());
            changes.removed = new FileInfo[removedFiles.size()];
            removedFiles.toArray(changes.removed);
            broadcastMessage(changes);
        }
    }

    /**
     * Loads the folder database from disk
     * 
     * @param dbFile
     *            the file to load as db file
     * @return true if succeeded
     */
    private boolean loadFolderDB(File dbFile) {
        synchronized (scanLock) {
            if (!dbFile.exists()) {
                log().debug(
                    this + ": Database file not found: "
                        + dbFile.getAbsolutePath());
                return false;
            }
            try {
                // load files and scan in
                InputStream fIn = new BufferedInputStream(new FileInputStream(
                    dbFile));
                ObjectInputStream in = new ObjectInputStream(fIn);
                FileInfo[] files = (FileInfo[]) in.readObject();
                // if no files yet (always?)
                // init the knownFiles with a hashmap with correct size to
                // reduce abnormal container growth
                if (knownFiles.size() == 0) {
                    knownFiles = Collections
                        .synchronizedMap(new HashMap<FileInfo, FileInfo>(
                            files.length));
                }
                for (FileInfo fileInfo : files) {
                    // scanFile(fileInfo);
                    addFile(fileInfo);
                }

                // read them always ..
                MemberInfo[] members1 = (MemberInfo[]) in.readObject();
                // Do not load members
                if (getController().isBackupServer()) {
                    log().verbose("Loading " + members1.length + " members");
                    for (MemberInfo memberInfo : members1) {
                        Member member = memberInfo.getNode(getController(),
                            true);
                        join(member);
                    }
                }

                try {
                    Object object = in.readObject();
                    blacklist.add((Collection<FileInfo>) object);
                    if (logEnabled) {
                        log().verbose(
                            "ignore@" + getName()
                                + blacklist.getExplicitIgnored().size());
                    }
                } catch (java.io.EOFException e) {
                    // ignore nothing available for ignore
                    log().debug("ignore nothing for " + this);
                } catch (Exception e) {
                    log().error("read ignore error: " + this + e.getMessage(),
                        e);
                }

                in.close();
                fIn.close();
            } catch (IOException e) {
                log().warn(
                    this + ": Unable to read database file: "
                        + dbFile.getAbsolutePath());
                log().verbose(e);
                return false;
            } catch (ClassNotFoundException e) {
                log().warn(
                    this + ": Unable to read database file: "
                        + dbFile.getAbsolutePath());
                log().verbose(e);
                return false;
            }

            // Ok has own database
            hasOwnDatabase = true;
        }

        // Calculate statistic
        statistic.scheduleCalculate();

        return true;
    }

    /**
     * Loads the folder db from disk
     */
    private void loadFolderDB() {
        if (loadFolderDB(new File(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR + "/" + DB_FILENAME)))
        {
            return;
        }

        if (loadFolderDB(new File(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR + "/" + DB_BACKUP_FILENAME)))
        {
            return;
        }

        log().warn(
            "Unable to read folder db, "
                + "even from backup, triing to load from old location");

        // TODO Remove the following on later versions....
        if (loadFolderDB(new File(localBase, DB_FILENAME))) {
            return;
        }

        log().warn(
            "Unable to find db file: " + DB_FILENAME
                + ", trying (old) backup location");

        if (loadFolderDB(new File(localBase, DB_BACKUP_FILENAME))) {
            log().warn("Unable to read folder db, even from backup");
        }
    }

    /**
     * Shuts down the folder
     */
    public void shutdown() {
        log().debug("shutting down folder " + this);

        // Remove listeners, not bothering them by boring shutdown events
        // disabled this so the ui is updated (more or less) that the folders
        // are disabled from debug panel
        // ListenerSupportFactory.removeAllListeners(folderListenerSupport);

        shutdown = true;
        storeFolderDB();
        blacklist.savePatternsTo(getSystemSubDir());
        removeAllListeners();
    }

    /**
     * Stores the current file-database to disk
     */
    private void storeFolderDB() {
        if (logVerbose) {
            log().debug("storeFolderDB. " + getFiles().length + " Files in db");
        }
        if (!shutdown) {
            if (!getController().isStarted()) {
                // Not storing
                return;
            }
        }
        synchronized (scanLock) {
            File dbFile = new File(getSystemSubDir(), DB_FILENAME);
            File dbFileBackup = new File(getSystemSubDir(), DB_BACKUP_FILENAME);
            try {
                FileInfo[] files = getFiles();
                if (dbFile.exists()) {
                    dbFile.delete();
                }
                dbFile.createNewFile();
                OutputStream fOut = new BufferedOutputStream(
                    new FileOutputStream(dbFile));
                ObjectOutputStream oOut = new ObjectOutputStream(fOut);
                // Store files
                oOut.writeObject(files);
                // Store members
                oOut.writeObject(Convert.asMemberInfos(getMembers()));
                // Store blacklist
                if (blacklist != null) {
                    List<FileInfo> ignored = blacklist.getExplicitIgnored();
                    if (logEnabled) {
                        log().verbose("write blacklist: " + ignored.size());
                    }
                    oOut.writeObject(ignored);
                }
                oOut.close();
                fOut.close();
                log().debug("Successfully wrote folder database file");

                // Make backup
                FileUtils.copyFile(dbFile, dbFileBackup);

                // TODO Remove this in later version
                // Cleanup for older versions
                File oldDbFile = new File(localBase, DB_FILENAME);
                oldDbFile.delete();
                File oldDbFileBackup = new File(localBase, DB_BACKUP_FILENAME);
                oldDbFileBackup.delete();
            } catch (IOException e) {
                // TODO: if something failed shoudn't we try to restore the
                // backup (if backup exists and bd file not after this?
                log().error(
                    this + ": Unable to write database file "
                        + dbFile.getAbsolutePath(), e);
                log().verbose(e);
            }
        }
    }

    /**
     * Set the needed folder/file attributes on windows systems, if we have a
     * desktop.ini
     * 
     * @param dektopIni
     */
    private void makeFolderIcon(File desktopIni) {
        if (desktopIni == null) {
            throw new NullPointerException("File (desktop.ini) is null");
        }
        if (!OSUtil.isWindowsSystem()) {
            log().verbose(
                "Not a windows system, ignoring folder icon. "
                    + desktopIni.getAbsolutePath());
            return;
        }

        log().verbose(
            "Setting icon of " + desktopIni.getParentFile().getAbsolutePath());

        FileUtils.setAttributesOnWindows(desktopIni, true, true);
        FileUtils.setAttributesOnWindows(desktopIni.getParentFile(), false,
            true);
    }

    /**
     * Creates or removes a desktop shortcut for this folder. currently only
     * available on windows systems.
     * 
     * @return true if succeeded
     */
    public boolean setDesktopShortcut(boolean active) {
        // boolean createRequested =
        // getController().getPreferences().getBoolean(
        // "createdesktopshortcuts", !getController().isConsoleMode());

        String shortCutName = getName();
        if (getController().isVerbose()) {
            shortCutName = "[" + getController().getMySelf().getNick() + "] "
                + shortCutName;
        }

        if (active) {
            return Util.createDesktopShortcut(shortCutName, localBase
                .getAbsoluteFile());
        }

        // Remove shortcuts to folder if not wanted
        Util.removeDesktopShortcut(shortCutName);
        return false;
    }

    /**
     * Deletes the desktop shortcut of the folder if set in prefs.
     */
    public void removeDesktopShortcut() {
        boolean createShortCuts = getController().getPreferences().getBoolean(
            "createdesktopshortcuts", !getController().isConsoleMode());
        if (!createShortCuts) {
            return;
        }

        String shortCutName = getName();
        if (getController().isVerbose()) {
            shortCutName = "[" + getController().getMySelf().getNick() + "] "
                + shortCutName;
        }

        // Remove shortcuts to folder
        Util.removeDesktopShortcut(shortCutName);
    }

    /**
     * Returns the syncprofile of this folder
     * 
     * @return
     */
    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    /**
     * Sets the synchronisation profile for this folder
     * 
     * @param syncProfile
     */
    public void setSyncProfile(SyncProfile aSyncProfile) {
        if (syncProfile == null) {
            throw new NullPointerException("Unable to set null sync profile");
        }
        SyncProfile oldProfile = syncProfile;
        if (oldProfile == aSyncProfile) {
            // Omitt set
            return;
        }

        log().debug("Setting " + aSyncProfile);
        this.syncProfile = aSyncProfile;

        // Store on disk
        String syncProfKey = "folder." + getName() + ".syncprofile";
        getController().getConfig().put(syncProfKey, syncProfile.getId());
        getController().saveConfig();

        if (oldProfile.isAutodownload() && !syncProfile.isAutodownload()) {
            // Changed from autodownload to manual, we need to abort all
            // Automatic download
            getController().getTransferManager().abortAllAutodownloads(this);
        }
        if (syncProfile.isAutodownload()) {
            // Trigger request files
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting();
        }

        if (syncProfile.isSyncDeletionWithFriends()
            || syncProfile.isSyncDeletionWithOthers())
        {
            handleRemoteDeletedFiles(false);
        }

        firePropertyChange(PROPERTY_SYNC_PROFILE, oldProfile, syncProfile);
        fireSyncProfileChanged();
    }

    /**
     * Forces the scan of the local filesystem on the next maintenace run.
     */
    public void forceScanOnNextMaintenance() {
        log().verbose("forceScanOnNextMaintenance Scan forced");
        scanForced = true;
        lastScan = null;
    }

    /**
     * Runs the maintenance on this folder. This means the folder gets synced
     * with remotesides.
     */
    public void maintain() {
        log().info("Maintaining '" + getName() + "'");

        // // Maintain the desktop shortcut
        // maintainDesktopShortcut();

        synchronized (this) {
            // Handle deletions
            handleRemoteDeletedFiles(false);

            // local files
            log().debug("Forced:" + scanForced);
            boolean forcedNow = scanForced;
            scanForced = false;
            scanLocalFiles(forcedNow);
        }
    }

    /*
     * Member managing methods ************************************************
     */

    /**
     * Joins a member to the folder,
     * 
     * @param member
     */
    public void join(Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null, unable to join");
        }

        // member will be joined, here on local
        synchronized (members) {
            if (!getController().getNodeManager().knowsNode(member)) {
                log().warn(
                    "Unable to join " + member + " to " + this
                        + ": Not a known node");
            }
            if (members.contains(member)) {
                members.remove(member);
            }
            members.add(member);
        }
        log().verbose("Member joined " + member);

        // send him our list of files
        if (member.isConnected()) {
            member.sendMessagesAsynchron(FileList.createFileListMessages(this));
        }

        // Fire event
        fireMemberJoined(member);
    }

    /**
     * Removes a member from this folder
     * 
     * @param member
     */
    public void remove(Member member) {
        if (!members.contains(member)) {
            return;
        }
        synchronized (members) {
            members.remove(member);
        }

        log().debug("Member left " + member);

        // remove files of this member in our datastructure
        // don't do this in the server version
        if (rootDirectory != null) {
            getDirectory().removeFilesOfMember(member);
        }

        // Fire event
        fireMemberLeft(member);
    }

    /**
     * Checks if the folder is in Sync, called by FolderRepository
     * 
     * @return if this folder synchronizing
     */
    public boolean isSynchronizing() {
        return getController().getTransferManager()
            .countNumberOfDownloads(this) > 0;
    }

    /**
     * Synchronizes the deleted files with local folder
     * 
     * @param force
     *            forces to sync deltions even if syncprofile has no deltion
     *            sync option
     */
    public boolean handleRemoteDeletedFiles(boolean force) {
        if (!force) {
            // Check if allowed on folder
            if (!syncProfile.isSyncDeletionWithFriends()
                && !syncProfile.isSyncDeletionWithOthers())
            {
                // No sync wanted
                return false;
            }
        }

        Member[] conMembers = getConnectedMembers();
        log().debug(
            "Deleting files, which are deleted by friends. con-members: "
                + Arrays.asList(conMembers));

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();

        for (Member member : conMembers) {
            if (!member.isConnected()) {
                // disconected in the meantime
                // go to next member
                continue;
            }

            FileInfo[] fileList = member.getLastFileList(this.getInfo());
            if (fileList == null) {
                continue;
            }

            if (logVerbose) {
                log().verbose(
                    "RemoteFileDeletion sync. Member '" + member.getNick()
                        + "' has " + fileList.length + " possible files");
            }
            for (FileInfo remoteFile : fileList) {
                boolean fileFromFriend = remoteFile
                    .isModifiedByFriend(getController());
                if (!fileFromFriend) {
                    // Not modified by friend, skip file
                    continue;
                }

                FileInfo localFile = getFile(remoteFile);
                boolean remoteFileNewer = true;
                if (localFile != null) {
                    remoteFileNewer = remoteFile.isNewerThan(localFile);
                }

                if (!remoteFileNewer) {
                    // Not newer, skip file
                    // log().warn(
                    // "Ingoring file (not newer): " + remoteFile.getName()
                    // + ". local-ver: " + localFile.getVersion()
                    // + ", remote-ver: " + remoteFile.getVersion());
                    continue;
                }

                // log().warn("Remote file has a newer file :" +
                // remoteFile.toDetailString());

                // Okay the remote file is newer

                // Add to local file to database if was deleted on remote
                if (localFile == null && remoteFile.isDeleted()) {
                    addFile(remoteFile);
                    localFile = getFile(remoteFile);
                    // File has been marked as removed at our side
                    removedFiles.add(localFile);
                }

                if (localFile != null) {
                    // log().warn("Okay we have local file :" + localFile);
                    if (remoteFile.isDeleted() && !localFile.isDeleted()) {
                        File localCopy = localFile.getDiskFile(getController()
                            .getFolderRepository());
                        log().warn(
                            "File was deleted by " + member
                                + ", deleting local: " + localCopy);
                        RecycleBin recycleBin = getController().getRecycleBin();
                        if (!recycleBin.moveToRecycleBin(localFile, localCopy))
                        {
                            log().error(
                                "Unable to move file to recycle bin"
                                    + localCopy);
                            if (!localCopy.delete()) {
                                log().error(
                                    "Unable to delete file " + localCopy);
                            }
                        }
                        // }
                        localFile.setDeleted(true);
                        localFile.setModifiedInfo(remoteFile.getModifiedBy(),
                            remoteFile.getModifiedDate());
                        localFile.setVersion(remoteFile.getVersion());

                        // File has been removed
                        removedFiles.add(localFile);

                        // Abort dl if one is active
                        Download dl = getController().getTransferManager()
                            .getActiveDownload(localFile);
                        if (dl != null) {
                            dl.abortAndCleanup();
                        }
                    } else if (localFile.isDeleted() && !remoteFile.isDeleted())
                    {
                        // Local file is deleted, check if version on remote is
                        // higher
                        log().warn("File restored on remote: " + remoteFile);
                        reDownloadFile(remoteFile);
                    }
                }
            }
        }

        // Broadcast folder change if changes happend
        if (!removedFiles.isEmpty()) {
            folderChanged();

            // Broadcast to memebers
            FolderFilesChanged changes = new FolderFilesChanged(getInfo());
            changes.removed = new FileInfo[removedFiles.size()];
            removedFiles.toArray(changes.removed);
            broadcastMessage(changes);
        }

        return true;
    }

    /**
     * Broadcasts a message through the folder
     * 
     * @param message
     */
    public void broadcastMessage(Message message) {
        for (Member member : getConnectedMembers()) {
            // still connected?
            if (member.isCompleteyConnected()) {
                // sending all nodes my knows nodes
                member.sendMessageAsynchron(message, null);
            }
        }
    }

    /**
     * Broadcasts the filelist
     */
    private void broadcastFileList() {
        if (logVerbose) {
            log().verbose("Broadcasting filelist");
        }
        if (getConnectedMembers().length > 0) {
            Message[] fileListMessages = FileList.createFileListMessages(this);
            for (Message message : fileListMessages) {
                broadcastMessage(message);
            }
        }
    }

    /**
     * Callback method from member. Called when a new filelist was send
     * 
     * @param from
     * @param newList
     */
    public void fileListChanged(Member from, FileList newList) {
        log().debug(
            "New Filelist received from " + from + " #files: "
                + newList.files.length);

        // Try to find same files
        findSameFiles(newList.files);

        // don't do this in the server version
        if (rootDirectory != null) {
            getDirectory().addAll(from, newList.files);
        }

        if (getSyncProfile().isAutodownload()) {
            // Trigger file requestor
            if (logVerbose) {
                log().verbose(
                    "Triggering file requestor because of new remote file list from "
                        + from);
            }
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting();
        }

        // Handle remote deleted files
        handleRemoteDeletedFiles(false);

        // TODO should be done by Directory that has actualy changed?
        fireRemoteContentsChanged();
    }

    /**
     * Callback method from member. Called when a filelist delta received
     * 
     * @param from
     * @param changes
     */
    public void fileListChanged(Member from, FolderFilesChanged changes) {
        log().debug("File changes received from " + from);

        // Try to find same files
        if (changes.added != null) {
            findSameFiles(changes.added);
        }
        if (changes.modified != null) {
            findSameFiles(changes.modified);
        }

        // TODO this should be done in differnt thread:
        // don't do this in the server version
        if (rootDirectory != null) {
            if (changes.added != null) {
                getDirectory().addAll(from, changes.added);
            }
            if (changes.modified != null) {
                getDirectory().addAll(from, changes.modified);
            }
            if (changes.removed != null) {
                getDirectory().addAll(from, changes.removed);
            }
            // /TODO
        }
        if (getSyncProfile().isAutodownload()) {
            // Check if we need to trigger the filerequestor
            boolean triggerFileRequestor = true;
            if (changes.added != null && changes.added.length == 1) {
                // This was caused by a completed download
                // TODO Maybe check this also on bigger lists!
                FileInfo localfileInfo = getFile(changes.added[0]);
                FileInfo remoteFileInfo = changes.added[0];
                if (localfileInfo != null
                    && !remoteFileInfo.isNewerThan(localfileInfo))
                {
                    // We have this or a newer version of the file. = Dont'
                    // trigger filerequestor.
                    triggerFileRequestor = false;
                }
            }

            if (triggerFileRequestor) {
                if (logVerbose) {
                    log().verbose(
                        "Triggering file requestor because of remote file list change "
                            + changes + " from " + from);
                }
                getController().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting();
            } else if (logVerbose) {
                log().verbose(
                    "Not triggering filerequestor, no new files in remote filelist"
                        + changes + " from " + from);
            }
        }

        // Handle remote deleted files
        handleRemoteDeletedFiles(false);

        // Fire event
        fireRemoteContentsChanged();
    }

    /**
     * Tries to find same files in the list of remotefiles. This methods takes
     * over the file information from remote under following circumstances:
     * <p>
     * 1. When the last modified date and size matches our file. (=good guess
     * that this is the same file)
     * <p>
     * 2. Our file has version 0 (=Scanned by initial scan)
     * <p>
     * 3. The remote file version is >0
     * <p>
     * This if files moved from node to node without PowerFolder. e.g. just copy
     * over windows share. Helps to identifiy same files and prevents unessesary
     * downloads.
     * 
     * @param fInfos
     */
    private void findSameFiles(FileInfo[] remoteFileInfos) {
        Reject.ifNull(remoteFileInfos, "Remote file info list is null");
        log().debug(
            "Triing to find same files in remote list with "
                + remoteFileInfos.length + " files");
        for (FileInfo remoteFileInfo : remoteFileInfos) {
            FileInfo localFileInfo = getFile(remoteFileInfo);
            if (localFileInfo == null) {
                continue;
            }
            if (!localFileInfo.isDeleted() && localFileInfo.getVersion() == 0
                && !remoteFileInfo.isDeleted()
                && remoteFileInfo.getVersion() > 0)
            {
                boolean fileSizeSame = localFileInfo.getSize() == remoteFileInfo
                    .getSize();
                // boolean dateSame = Convert
                // .convertToGlobalPrecision(localFileInfo.getModifiedDate()
                // .getTime()) == Convert
                // .convertToGlobalPrecision(remoteFileInfo.getModifiedDate()
                // .getTime());
                boolean dateSame = Util.equalsFileDateCrossPlattform(
                    localFileInfo.getModifiedDate(), remoteFileInfo
                        .getModifiedDate());
                // System.out.println("fileSizeSame: " + fileSizeSame + "
                // dateSame: " + dateSame + " " +
                // localFileInfo.getModifiedDate() + " == "
                // +remoteFileInfo.getModifiedDate() + " ?????????????????");
                if (fileSizeSame && dateSame) {
                    log().warn(
                        "Found same file: local " + localFileInfo + " remote: "
                            + remoteFileInfo
                            + ". Taking over modification infos");
                    localFileInfo.copyFrom(remoteFileInfo);
                }
            }
        }
    }

    /**
     * Tries to find same files in the list of remotefiles of all members. This
     * methods takes over the file information from remote under following
     * circumstances: See #findSameFiles(FileInfo[])
     * 
     * @see #findSameFiles(FileInfo[])
     */
    private void findSameFilesOnRemote() {
        for (Member member : getConnectedMembers()) {
            FileInfo[] lastFileList = member.getLastFileList(this.getInfo());
            if (lastFileList != null) {
                findSameFiles(lastFileList);
            }
        }
    }

    /**
     * Methods which updates all nessesary components if the folder changed
     */
    private void folderChanged() {
        storeFolderDB();
        blacklist.savePatternsTo(getSystemSubDir());
        // Write filelist
        if (Logger.isLogToFileEnabled()) {
            // Write filelist to disk
            File debugFile = new File(Logger.getDebugDir(), getName() + "/"
                + getController().getMySelf().getNick() + ".list.txt");
            Debug.writeFileList(knownFiles.keySet(), "FileList of folder "
                + getName() + ", member " + this + ":", debugFile);
        }

        // Fire general folder change event
        fireFolderChanged();
    }

    /*
     * Simple getters/exposing methods ****************************************
     */

    /**
     * Answers the local base directory
     * 
     * @return
     */
    public File getLocalBase() {
        return localBase;
    }

    /**
     * @return the system subdir in the local base folder. subdir gets created
     *         if not existing
     */
    public File getSystemSubDir() {
        File systemSubDir = new File(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (!systemSubDir.exists()) {
            systemSubDir.mkdirs();
            FileUtils.makeHiddenOnWindows(systemSubDir);
        }

        return systemSubDir;
    }

    public boolean isSystemSubDir(File aDir) {
        return aDir.isDirectory()
            && getSystemSubDir().getAbsolutePath().equals(
                aDir.getAbsolutePath());
    }

    public String getName() {
        return currentInfo.name;
    }

    public boolean isSecret() {
        return currentInfo.secret;
    }

    public int getFilesCount() {
        return knownFiles.size();
    }

    /**
     * Returns the list of all files in local folder database
     * 
     * @return
     */
    public FileInfo[] getFiles() {
        synchronized (knownFiles) {
            FileInfo[] files = new FileInfo[knownFiles.size()];
            knownFiles.values().toArray(files);
            return files;
        }
    }

    public List<FileInfo> getFilesAsList() {
        synchronized (knownFiles) {
            return new ArrayList<FileInfo>(knownFiles.values());
        }
    }

    /**
     * get the Directories in this folder (including the subs and files)
     * 
     * @return Directory with all sub dirs and treeNodes set
     */
    public Directory getDirectory() {
        if (rootDirectory == null) {
            return getDirectory0(false);
        }
        return rootDirectory;
    }

    /**
     * Initernal method //TODO: add a task to read this in the background?
     * 
     * @param initalizeCall
     * @return
     */
    private Directory getDirectory0(boolean initalizeCall) {
        FileInfo[] knownFilesArray;

        synchronized (knownFiles) {
            knownFilesArray = new FileInfo[knownFiles.size()];
            Set<FileInfo> knownFilesSet = knownFiles.keySet();
            knownFilesArray = knownFilesSet.toArray(knownFilesArray);
        }

        Directory directory = Directory.buildDirsRecursive(getController()
            .getNodeManager().getMySelf(), knownFilesArray, this);
        if (treeNode != null) {
            if (!initalizeCall && treeNode.getChildCount() > 0) {
                treeNode.remove(0);
            }
            List<Directory> subs = directory.listSubDirectories();
            for (int i = 0; i < subs.size(); i++) {
                treeNode.insert(subs.get(i), i);
            }
        }

        return directory;
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @param includeNonFriendFiles
     *            if files should be included, that are modified by non-friends
     * @return the list of files that are incoming/newer available on remote
     *         side
     */
    public List<FileInfo> getIncomingFiles(boolean includeNonFriendFiles) {
        // build a temp list
        Map<FileInfo, FileInfo> incomingFiles = new HashMap<FileInfo, FileInfo>();
        // add expeced files
        Member[] conMembers = getConnectedMembers();
        for (Member member : conMembers) {
            if (!member.isCompleteyConnected()) {
                // disconnected in the meantime
                continue;
            }

            FileInfo[] memberFiles = getFiles(member);

            if (memberFiles != null) {
                for (FileInfo remoteFile : memberFiles) {
                    boolean modificatorOk = includeNonFriendFiles
                        || remoteFile.isModifiedByFriend(getController());
                    if (!modificatorOk) {
                        continue;
                    }

                    // Check if remote file is newer
                    FileInfo localFile = getFile(remoteFile);
                    FileInfo alreadyIncoming = incomingFiles.get(remoteFile);
                    boolean notLocal = localFile == null;
                    boolean newerThanLocal = localFile != null
                        && remoteFile.isNewerThan(localFile);
                    // Check if this remote file is newer than one we may
                    // already have.
                    boolean newestRemote = alreadyIncoming == null
                        || remoteFile.isNewerThan(alreadyIncoming);
                    if (notLocal || (newerThanLocal && newestRemote)) {
                        // Okay this one is expected
                        incomingFiles.put(remoteFile, remoteFile);
                    }
                }
            }
        }

        log().debug("Incoming files " + incomingFiles.size());
        return new ArrayList<FileInfo>(incomingFiles.values());
    }

    /**
     * Returns the list of files from a member
     * 
     * @param member
     * @return
     */
    public FileInfo[] getFiles(Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        if (member.isMySelf()) {
            return getFiles();
        }
        FileInfo[] list = member.getLastFileList(getInfo());
        if (list == null) {
            return null;
        }
        return list;
    }

    /**
     * Answers all files, those on local disk and expected files.
     * 
     * @param includeOfflineUsers
     *            true if also files of offline user should be included
     * @return
     */
    public FileInfo[] getAllFiles(boolean includeOfflineUsers) {
        // build a temp list
        Map<FileInfo, FileInfo> filesMap = new HashMap<FileInfo, FileInfo>(
            knownFiles.size());

        Member[] theMembers;
        if (includeOfflineUsers) {
            theMembers = getMembers();
        } else {
            theMembers = getConnectedMembers();
        }
        for (Member member : theMembers) {
            if (member.isMySelf()) {
                continue;
            }

            FileInfo[] memberFiles = getFiles(member);
            if (memberFiles == null) {
                continue;
            }
            for (FileInfo remoteFile : memberFiles) {
                // Add only if not already added or file is not deleted on
                // remote side
                FileInfo inMap = filesMap.get(remoteFile);
                if (inMap == null || inMap.isDeleted()) {
                    filesMap.put(remoteFile, remoteFile);
                }
            }
        }

        // Put own files over filelist till now
        synchronized (knownFiles) {
            filesMap.putAll(knownFiles);
        }

        FileInfo[] allFiles = new FileInfo[filesMap.size()];
        // Has to be the values, keys dont get overwritten on putAll !
        filesMap.values().toArray(allFiles);

        // log().warn(
        // this + ": Has " + allFiles.length + " total files, size: "
        // + Util.formatBytes(Util.calculateSize(allFiles, true))
        // + ", local size: "
        // + Util.formatBytes(Util.calculateSize(files, true)));

        return allFiles;
    }

    /**
     * Returns all members
     * 
     * @return
     */
    public Member[] getMembers() {
        synchronized (members) {
            Member[] membersArr = new Member[members.size()];
            members.toArray(membersArr);
            return membersArr;
        }
    }

    public Member[] getConnectedMembers() {
        List<Member> connected = new ArrayList<Member>(members.size());
        synchronized (members) {
            for (Member member : members) {
                if (member.isCompleteyConnected()) {
                    if (member.isMySelf()) {
                        continue;
                    }
                    connected.add(member);
                }
            }
        }
        Member[] asArray = new Member[connected.size()];
        return connected.toArray(asArray);
    }

    /**
     * @param member
     * @return true if that member is on this folder
     */
    public boolean hasMember(Member member) {
        if (members == null) { // FIX a rare NPE at startup
            return false;
        }
        if (member == null) {
            return false;
        }
        return members.contains(member);
    }

    /**
     * Answers the number of members
     * 
     * @return
     */
    public int getMembersCount() {
        return members.size();
    }

    /**
     * Answers if folder has this file
     * 
     * @param fInfo
     * @return
     */
    public boolean hasFile(FileInfo fInfo) {
        return knownFiles.containsKey(fInfo);
    }

    /**
     * Gets a file by fileinfo
     * 
     * @param fInfo
     * @return
     */
    public FileInfo getFile(FileInfo fInfo) {
        return knownFiles.get(fInfo);
    }

    /**
     * @param fInfo
     * @return the local file from a file info Never returns null, file MAY NOT
     *         exist!! check before use
     */
    public File getDiskFile(FileInfo fInfo) {
        // File diskFile = diskFileCache.get(fInfo);
        // if (diskFile != null) {
        // return diskFile;
        // }

        File diskFile = new File(localBase, fInfo.getName());
        // diskFileCache.put(fInfo, diskFile);
        return diskFile;
    }

    /**
     * @return the globally unique folder ID, generate once at folder creation
     */
    public String getId() {
        return currentInfo.id;
    }

    /**
     * Returns a folder info object
     * 
     * @return
     */
    public FolderInfo getInfo() {
        return currentInfo;
    }

    /**
     * Returns the statistic for this folder
     * 
     * @return
     */
    public FolderStatistic getStatistic() {
        return statistic;
    }

    /** returns an Invitation to this folder */
    public Invitation getInvitation() {
        return new Invitation(getInfo(), getController().getMySelf().getInfo());
    }

    public String toString() {
        return getInfo().toString();
    }

    // Logger methods *********************************************************

    public String getLoggerName() {
        return "Folder '" + getName() + "'";
    }

    // UI-Swing methods *******************************************************

    /**
     * Returns the treenode representation of this object.
     * <p>
     * TODO Move this into a <code>FolderModel</code> similar to
     * <code>NodeManagerModel</code> and <code>FolderRepositoryModel</code>
     * 
     * @return
     */
    public MutableTreeNode getTreeNode() {
        if (treeNode == null) {
            // Initalize treenode now lazily
            treeNode = new TreeNodeList(this, getController().getUIController()
                .getFolderRepositoryModel().getMyFoldersTreeNode());
            // treeNode.sortBy(MemberComparator.IN_GUI);

            // first make sure we have a fresh copy
            rootDirectory = getDirectory0(true);// 

            // Now sort
            treeNode.sort();
        } // else {
        // getDirectory();
        // }

        return treeNode;
    }

    /**
     * Sets the parent of folders treenode
     * 
     * @param parent
     */
    public void setTreeNodeParent(MutableTreeNode parent) {
        treeNode.setParent(parent);
    }

    // *************** Event support
    public void addMembershipListener(FolderMembershipListener listener) {
        ListenerSupportFactory.addListener(folderMembershipListenerSupport,
            listener);
    }

    public void removeMembershipListener(FolderMembershipListener listener) {
        ListenerSupportFactory.removeListener(folderMembershipListenerSupport,
            listener);
    }

    public void addFolderListener(FolderListener listener) {
        ListenerSupportFactory.addListener(folderListenerSupport, listener);
    }

    public void removeFolderListener(FolderListener listener) {
        ListenerSupportFactory.removeListener(folderListenerSupport, listener);
    }

    private void fireMemberJoined(Member member) {
        FolderMembershipEvent folderMembershipEvent = new FolderMembershipEvent(
            this, member);
        folderMembershipListenerSupport.memberJoined(folderMembershipEvent);
    }

    private void fireMemberLeft(Member member) {
        FolderMembershipEvent folderMembershipEvent = new FolderMembershipEvent(
            this, member);
        folderMembershipListenerSupport.memberLeft(folderMembershipEvent);
    }

    private void fireFolderChanged() {
        // log().debug("fireChanged: " + this);
        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.folderChanged(folderEvent);
    }

    private void fireSyncProfileChanged() {
        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.syncProfileChanged(folderEvent);
    }

    private void fireRemoteContentsChanged() {
        // log().debug("fireRemoteContentsChanged: " + this);
        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    /** package protected because fired by FolderStatistics */
    void fireStatisticsCalculated() {
        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.statisticsCalculated(folderEvent);
    }
}