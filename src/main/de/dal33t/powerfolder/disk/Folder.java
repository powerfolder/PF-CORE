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
package de.dal33t.powerfolder.disk;

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.ScanCommand;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferPriorities;
import de.dal33t.powerfolder.transfer.TransferPriorities.TransferPriority;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.DiskItemComparator;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * The main class representing a folder. Scans for new files automatically.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Folder extends PFComponent {
    public static final String DB_FILENAME = ".PowerFolder.db";
    public static final String DB_BACKUP_FILENAME = ".PowerFolder.db.bak";
    public static final String THUMBS_DB = "*thumbs.db";
    public static final String WORD_TEMP = "*~*.tmp";
    public static final String DS_STORE = "*.DS_Store";

    /** The base location of the folder. */
    private File localBase;

    /**
     * Date of the last directory scan
     */
    private Date lastScan;

    /**
     * The result state of the last scan
     */
    private ScanResult.ResultState lastScanResultState;

    /**
     * The last time the db was cleaned up.
     */
    private Date lastDBMaintenance;

    /**
     * The internal database of locally known files. Contains FileInfo ->
     * FileInfo
     */
    private Map<FileInfo, FileInfo> knownFiles;

    /** files that should(not) be downloaded in auto download */
    private DiskItemFilter diskItemFilter;

    /**
     * Stores the priorities for downloading of the files in this folder.
     */
    private TransferPriorities transferPriorities;

    /** Lock for scan */
    private final Object scanLock = new Object();

    /**
     * Lock to prevent multiple threads to execute deletions.
     */
    private final Object deleteLock = new Object();

    /** All members of this folder */
    private Map<Member, Member> members;

    /** cached Directory object */
    private final Directory rootDirectory;

    /**
     * the folder info, contains important information about
     * id/hash/name/filescount
     */
    private FolderInfo currentInfo;

    /**
     * Folders sync profile Always access using getSyncProfile (#76 - preview
     * mode)
     */
    private SyncProfile syncProfile;

    /**
     * Flag indicating that folder has a set of own know files will be true
     * after first scan ever
     */
    private boolean hasOwnDatabase;

    /** Flag indicating */
    private boolean shutdown;

    /**
     * Indicates, that the scan of the local filesystem was forced
     */
    private boolean scanForced;

    /**
     * Flag indicating that the setting (e.g. folder db) have changed but not
     * been persisted.
     */
    private boolean dirty;

    /**
     * Persister task, persists settings from time to time.
     */
    private Persister persister;

    private boolean whitelist;

    /**
     * The FileInfos that have problems inlcuding the desciptions of the
     * problems. DISABLED
     */
    // private Map<FileInfo, List<FilenameProblem>> problemFiles;
    /** The statistic for this folder */
    private FolderStatistic statistic;

    private FolderListener folderListenerSupport;
    private FolderMembershipListener folderMembershipListenerSupport;

    /** Whether to move deleted items to the recycle bin */
    private boolean useRecycleBin;

    /**
     * If the folder is only preview then the files do not actually download and
     * the folder displays in the Available Folders group.
     */
    private boolean previewOnly;

    /**
     * The Date of the lastchange to a folder file.
     */
    private Date lastFileChangeDate;

    /**
     * True if the base dir is inaccessible.
     */
    private boolean deviceDisconnected;

    /**
     * Constructor for folder.
     * 
     * @param controller
     * @param fInfo
     * @param folderSettings
     * @throws FolderException
     */
    Folder(Controller controller, FolderInfo fInfo,
        FolderSettings folderSettings)
    {
        super(controller);

        if (fInfo == null) {
            throw new NullPointerException("FolderInfo is null");
        }
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.currentInfo = new FolderInfo(fInfo.name, fInfo.id);

        if (fInfo.name == null) {
            throw new NullPointerException("Folder name is null");
        }
        if (fInfo.id == null) {
            throw new NullPointerException("Folder id (" + fInfo.id
                + ") is null");
        }
        if (folderSettings.getLocalBaseDir() == null) {
            throw new NullPointerException("Folder localdir is null");
        }
        Reject.ifNull(folderSettings.getSyncProfile(), "Sync profile is null");

        // Not until first scan or db load
        this.hasOwnDatabase = false;
        this.dirty = false;

        localBase = folderSettings.getLocalBaseDir();

        syncProfile = folderSettings.getSyncProfile();

        // Create listener support
        this.folderListenerSupport = (FolderListener) getController().getListenerSupportFactory()
            .createListenerSupport(FolderListener.class);

        this.folderMembershipListenerSupport = (FolderMembershipListener) getController().getListenerSupportFactory()
            .createListenerSupport(FolderMembershipListener.class);

        useRecycleBin = folderSettings.isUseRecycleBin();

        whitelist = folderSettings.isWhitelist();

        // Check base dir
        try {
            checkBaseDir(localBase);
        } catch (FolderException e) {
            getLogger().warn("local base inaccessible for " + fInfo.name);
            deviceDisconnected = true;
        }

        statistic = new FolderStatistic(this);
        knownFiles = new ConcurrentHashMap<FileInfo, FileInfo>();
        members = new ConcurrentHashMap<Member, Member>();
        // diskFileCache = new WeakHashMap<FileInfo, File>();

        // put myself in membership
        join0(controller.getMySelf());

        log().debug(
            "Opening " + this.toString() + " at '"
                + localBase.getAbsolutePath() + "'");

        if (localBase.list() != null && localBase.list().length == 0) {
            // Empty folder... no scan required for database
            hasOwnDatabase = true;
        }

        diskItemFilter = new DiskItemFilter(whitelist);
        diskItemFilter.loadPatternsFrom(getSystemSubDir());

        transferPriorities = new TransferPriorities();

        // Load folder database
        loadFolderDB(); // will also read the blacklist

        // Check desktop ini in Windows environments
        FileUtils.maintainDesktopIni(getController(), localBase);

        // Force the next time scan if autodetect is set
        // and in regular sync mode (not daily sync).
        recommendScanOnNextMaintenance();

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
                Debug.writeFileListCSV(knownFiles.keySet(),
                    "FileList of folder " + getName() + ", member " + this
                        + ":", debugFile);
            }
        }

        // Register persister
        // FIXME: There is no way to remove the persister on shutdown.
        // Only on controller shutdown
        this.persister = new Persister();
        getController().scheduleAndRepeat(persister, 5000);

        // Create invitation
        if (folderSettings.isCreateInvitationFile()) {
            Invitation inv = createInvitation();
            InvitationUtil.save(inv, new File(folderSettings.getLocalBaseDir(),
                Util.removeInvalidFilenameChars(inv.folder.name)
                    + ".invitation"));
        }

        previewOnly = folderSettings.isPreviewOnly();

        rootDirectory = Directory.buildDirsRecursive(getController()
            .getNodeManager().getMySelf(), knownFiles.values(), this);
    }

    /**
     * Commits the scan results into the internal file database. Changes get
     * broadcasted to other members if nessesary. public because also called
     * from SyncFolderPanel (until that class maybe handles that itself)
     * 
     * @param scanResult
     *            the scanresult to commit.
     */
    private void commitScanResult(final ScanResult scanResult) {
        // Disabled, causing bug #293
        // final List<FileInfo> fileInfosToConvert = new ArrayList<FileInfo>();
        // new files
        for (FileInfo newFileInfo : scanResult.getNewFiles()) {
            // add to the DB
            FileInfo old = knownFiles.put(newFileInfo, newFileInfo);
            if (old != null) {
                log().error(
                    "hmmzzz it was new!?!?!?!: old: " + old.toDetailString()
                        + " , new: " + newFileInfo.toDetailString());
                // Remove old file from info
                currentInfo.removeFile(old);
            }
            // Add file to folder
            currentInfo.addFile(newFileInfo);

            // if meta then add the meta scan queue
            // Disabled, causing bug #293
            // if (FileMetaInfoReader.isConvertingSupported(newFileInfo)) {
            // fileInfosToConvert.add(newFileInfo);
            // }
        }
        // Add new files to the UI this is relatively slow on folders with a
        // lot of new files (initial scan) so done in different thread
        if (!scanResult.getNewFiles().isEmpty()) {
            // Runnable runner = new Runnable() {
            // public void run() {
            if (rootDirectory != null) {
                if (logVerbose) {
                    log().verbose(
                        "Adding " + scanResult.getNewFiles().size()
                            + " to directory");
                }
                for (FileInfo newFileInfo : scanResult.getNewFiles()) {
                    getDirectory()
                        .add(getController().getMySelf(), newFileInfo);

                }
            }
            // }
            // };
            // getController().getThreadPool().submit(runner);
        }

        // deleted files
        for (FileInfo deletedFileInfo : scanResult.getDeletedFiles()) {
            deletedFileInfo.setDeleted(true);
            deletedFileInfo.setSize(0);
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
            // DISABLED because of #644
            // changedFileInfo.invalidateFilePartsRecord();
        }

        // if (scanResult.getProblemFiles().size() > 0) {
        // problemFiles = scanResult.getProblemFiles();
        // if (problemFiles != null && problemFiles.size() > 0) {
        // fireProblemsFound();
        // }
        // }

        hasOwnDatabase = true;
        if (logEnabled) {
            log().debug(
                "Scanned " + scanResult.getTotalFilesCount() + " total, "
                    + scanResult.getChangedFiles().size() + " changed, "
                    + scanResult.getNewFiles().size() + " new, "
                    + scanResult.getRestoredFiles().size() + " restored, "
                    + scanResult.getDeletedFiles().size() + " removed, "
                    + scanResult.getProblemFiles().size() + " problems");
        }

        // Fire scan result
        fireScanResultCommited(scanResult);

        if (scanResult.getNewFiles().size() > 0
            || scanResult.getChangedFiles().size() > 0
            || scanResult.getDeletedFiles().size() > 0
            || scanResult.getRestoredFiles().size() > 0)
        {
            setDBDirty();
            // broadcast changes on folder
            broadcastFolderChanges(scanResult);
        }

        // Disabled, causing bug #293
        // in new files are found we can convert to meta info please do so..
        // if (fileInfosToConvert.size() > 0) {
        // convertToMeta(fileInfosToConvert);
        // }
        if (logVerbose) {
            log().verbose("commitScanResult DONE");
        }
    }

    // Disabled, causing bug #293
    // private void convertToMeta(final List<FileInfo> fileInfosToConvert) {
    // // do converting in a differnend Thread
    // Runnable runner = new Runnable() {
    // public void run() {
    // List<FileInfo> converted = getController()
    // .getFolderRepository().getFileMetaInfoReader().convert(
    // Folder.this, fileInfosToConvert);
    // if (logEnabled) {
    // log().debug("Converted: " + converted);
    // }
    // for (FileInfo convertedFileInfo : converted) {
    // FileInfo old = knownFiles.put(convertedFileInfo,
    // convertedFileInfo);
    // if (old != null) {
    // // Remove old file from info
    // currentInfo.removeFile(old);
    // }
    // // Add file to folder
    // currentInfo.addFile(convertedFileInfo);
    //
    // // update UI
    // if (getController().isUIEnabled()) {
    // getDirectory().add(getController().getMySelf(),
    // convertedFileInfo);
    // }
    // }
    // folderChanged();
    // }
    // };
    // getController().getThreadPool().submit(runner);
    // }

    /** @return true if this folder has possible problems, like filename problems */
    public boolean hasProblems() {
        // return !(problemFiles == null) && problemFiles.size() > 0;
        return false;
    }

    /**
     * @return the files that have possible problems inlcuding their
     *         descriptions
     */
    public Map<FileInfo, List<FilenameProblem>> getProblemFiles() {
        return Collections.EMPTY_MAP;
        // return problemFiles;
    }

    public boolean hasOwnDatabase() {
        return hasOwnDatabase;
    }

    public DiskItemFilter getDiskItemFilter() {
        return diskItemFilter;
    }

    /**
     * Convenience method to add a pattern if it does not exist.
     * 
     * @param pattern
     */
    private void addPattern(String pattern) {
        List<String> patterns = diskItemFilter.getPatterns();
        if (!patterns.contains(pattern)) {
            patterns.add(pattern);
        }
    }

    /**
     * Retrieves the transferpriorities for file in this folder.
     * 
     * @return the associated TransferPriorities object
     */
    public TransferPriorities getTransferPriorities() {
        return transferPriorities;
    }

    public ScanResult.ResultState getLastScanResultState() {
        return lastScanResultState;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
        diskItemFilter.setExcludeByDefault(whitelist);
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
            fileChanged(fileInfo);
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
            fileChanged(fileInfo);
        }
    }

    /**
     * Scans a downloaded file, renames tempfile to real name Moves possible
     * existing file to PowerFolder recycle bin.
     * 
     * @param fInfo
     * @param tempFile
     * @return true if the download could be completed and the file got scanned.
     *         false if any problem happend.
     */
    public boolean scanDownloadFile(FileInfo fInfo, File tempFile) {
        // rename file
        File targetFile = fInfo.getDiskFile(getController()
            .getFolderRepository());
        if (!targetFile.getParentFile().exists()
            && !targetFile.getParentFile().mkdirs())
        {
            log()
                .error("Couldn't create directory structure for " + targetFile);
            return false;
        }
        synchronized (deleteLock) {
            if (targetFile.exists()) {
                // if file was a "newer file" the file already esists here
                if (logVerbose) {
                    log().verbose(
                        "file already exists: " + targetFile
                            + " moving to recycle bin");
                }
                deleteFile(fInfo, targetFile);
            }
        }
        synchronized (scanLock) {
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
                            + e.getMessage() + ". " + fInfo.toDetailString());
                    return false;
                }
            }

            if (tempFile.exists() && !tempFile.delete()) {
                log().error("Unable to remove temp file: " + tempFile);
            }

            // Set modified date of remote
            if (!targetFile.setLastModified(fInfo.getModifiedDate().getTime()))
            {
                log().error(
                    "Failed to set modified date on " + targetFile + " to "
                        + fInfo.getModifiedDate().getTime());
                return false;
            }

            // Update internal database
            FileInfo dbFile = getFile(fInfo);
            if (dbFile != null) {
                // Update database
                dbFile.copyFrom(fInfo);
                fileChanged(dbFile);
            } else {
                // File new, scan
                if (scanFile(fInfo)) {
                    fileChanged(fInfo);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Scans the local directory for new files. Be carefull! This method is not
     * Thread save. In most cases you want to use
     * recommendScanOnNextMaintenance() followed by maintain().
     * 
     * @return if the local files where scanned
     */
    public boolean scanLocalFiles() {

        /**
         * Check that we still have a good local base.
         */
        try {
            checkBaseDir(localBase);
            deviceDisconnected = false;
        } catch (FolderException e) {
            getLogger().warn("invalid local base", e);
            deviceDisconnected = true;
            return false;
        }

        ScanResult result;
        synchronized (scanLock) {
            FolderScanner scanner = getController().getFolderRepository()
                .getFolderScanner();
            result = scanner.scanFolderWaitIfBusy(this);
            log().verbose("Scan result: " + result.getResultState());
        }

        try {
            if (result.getResultState().equals(ScanResult.ResultState.SCANNED))
            {
                if (!result.getProblemFiles().isEmpty()) {
                    FileNameProblemHandler handler = getController()
                        .getFolderRepository().getFileNameProblemHandler();
                    if (handler != null) {
                        handler
                            .fileNameProblemsDetected(new FileNameProblemEvent(
                                this, result));
                    }
                }
                commitScanResult(result);
                lastScan = new Date();
                dirty = true;
                findSameFilesOnRemote();
                return true;
            }
            // scan aborted or hardware broken?
            return false;
        } finally {
            lastScanResultState = result.getResultState();
        }
    }

    /**
     * @return true if a scan in the background is required of the folder
     */
    private boolean autoScanRequired() {
        if (!syncProfile.isAutoDetectLocalChanges()) {
            if (logVerbose) {
                log().verbose("Skipping scan");
            }
            return false;
        }
        if (lastScan == null) {
            return true;
        }

        if (syncProfile.getConfiguration().isDailySync()) {
            if (!shouldDoDailySync()) {
                if (logVerbose) {
                    log().verbose("Skipping daily scan");
                }
                return false;
            }

        } else {
            long secondsSinceLastSync = (System.currentTimeMillis() - lastScan
                .getTime()) / 1000;
            if (secondsSinceLastSync < syncProfile.getSecondsBetweenScans()) {
                if (logVerbose) {
                    log().verbose("Skipping regular scan");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if a daily scan is required.
     */
    private boolean shouldDoDailySync() {
        Calendar lastScannedCalendar = new GregorianCalendar();
        lastScannedCalendar.setTime(lastScan);
        int lastScannedDay = lastScannedCalendar.get(Calendar.DAY_OF_YEAR);
        if (logVerbose) {
            log().verbose("Last scanned " + lastScannedCalendar.getTime());
        }

        Calendar todayCalendar = new GregorianCalendar();
        todayCalendar.setTime(new Date());
        int currentDayOfYear = todayCalendar.get(Calendar.DAY_OF_YEAR);

        if (lastScannedDay == currentDayOfYear
            && lastScannedCalendar.get(Calendar.YEAR) == todayCalendar
                .get(Calendar.YEAR))
        {
            // Scanned today, so skip.
            if (logVerbose) {
                log().verbose("Skipping daily scan (already scanned today)");
            }
            return false;
        }

        int requiredSyncHour = syncProfile.getConfiguration().getDailyHour();
        int currentHour = todayCalendar.get(Calendar.HOUR_OF_DAY);
        if (requiredSyncHour != currentHour) {
            // Not correct time, so skip.
            if (logVerbose) {
                log().verbose("Skipping daily scan (not correct time)");
            }
            return false;
        }

        int requiredSyncDay = syncProfile.getConfiguration().getDailyDay();
        int currentDay = todayCalendar.get(Calendar.DAY_OF_WEEK);

        // Check daily synchronization day of week.
        if (requiredSyncDay != SyncProfileConfiguration.DAILY_DAY_EVERY_DAY) {

            if (requiredSyncDay == SyncProfileConfiguration.DAILY_DAY_WEEKDAYS)
            {
                if (currentDay == Calendar.SATURDAY
                    || currentDay == Calendar.SUNDAY)
                {
                    if (logVerbose) {
                        log().verbose("Skipping daily scan (not weekday)");
                    }
                    return false;
                }
            } else if (requiredSyncDay == SyncProfileConfiguration.DAILY_DAY_WEEKENDS)
            {
                if (currentDay != Calendar.SATURDAY
                    && currentDay != Calendar.SUNDAY)
                {
                    if (logVerbose) {
                        log().verbose("Skipping daily scan (not weekend)");
                    }
                    return false;
                }
            } else {
                if (currentDay != requiredSyncDay) {
                    if (logVerbose) {
                        log().verbose("Skipping daily scan (not correct day)");
                    }
                    return false;
                }
            }
        }
        return true;
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
                    if (!file.delete()) {
                        log().error(
                            "Failed to remove temp download file: " + file);
                    }
                } else {
                    log().verbose("Ignoring incomplete download file: " + file);
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
            if (PreferencesEntry.FILE_NAME_CHECK
                .getValueBoolean(getController()))
            {
                checkFileName(fInfo);
            }

            // link new file to our folder
            fInfo.setFolderInfo(this.currentInfo);
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
                // FileInfo converted = FileMetaInfoReader
                // .convertToMetaInfoFileInfo(this, fInfo);
                // addFile(converted);
                addFile(fInfo);

                // update directory
                // don't do this in the server version
                if (rootDirectory != null) {
                    getDirectory().add(getController().getMySelf(), fInfo);
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

            if (logVerbose) {
                log().verbose("File already known: " + fInfo);
            }

            return fileChanged;
        }

    }

    /**
     * Checks a single filename if there are problems with the name
     * 
     * @param fileInfo
     */
    private void checkFileName(FileInfo fileInfo) {
        if (!PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController()))
        {
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
                    .getTranslation("general.neverAskAgain");
                NeverAskAgainResponse response = DialogFactory.genericDialog(
                    getController().getUIController().getMainFrame()
                        .getUIComponent(), title, message,
                    new String[]{Translation.getTranslation("general.ok")}, 0,
                    GenericDialogType.INFO, neverShowAgainText);
                if (response.isNeverAskAgain()) {
                    PreferencesEntry.FILE_NAME_CHECK.setValue(getController(),
                        false);
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
     * Adds a file to the internal database, does NOT store the DB
     * 
     * @param fInfo
     */
    private void addFile(FileInfo fInfo) {
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }
        // Add to this folder
        fInfo.setFolderInfo(this.currentInfo);

        FileInfo old = knownFiles.put(fInfo, fInfo);

        TransferPriority prio = TransferPriority.NORMAL;

        if (old != null) {
            prio = transferPriorities.getPriority(old);
            // Remove old file from info
            currentInfo.removeFile(old);
        }

        // log().warn("Adding " + fInfo.getClass() + ", removing " + old);

        // Add file to folder
        currentInfo.addFile(fInfo);

        transferPriorities.setPriority(fInfo, prio);
    }

    /**
     * @param fi
     * @return if this file is known to the internal db
     */
    public boolean isKnown(FileInfo fi) {
        return knownFiles.containsKey(fi);
    }

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
        DownloadManager dl = getController().getTransferManager()
            .getActiveDownload(fInfo);
        if (dl != null) {
            dl.abortAndCleanup();
        }

        File diskFile = getDiskFile(fInfo);
        synchronized (deleteLock) {
            if (diskFile != null && diskFile.exists()) {
                deleteFile(fInfo, diskFile);
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
     * @param fInfos
     */
    public void removeFilesLocal(FileInfo... fInfos) {
        if (fInfos == null || fInfos.length < 0) {
            throw new IllegalArgumentException("Files to delete are empty");
        }

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        synchronized (scanLock) {
            for (FileInfo fileInfo : fInfos) {
                if (removeFileLocal(fileInfo)) {
                    removedFiles.add(fileInfo);
                }
            }
        }

        if (!removedFiles.isEmpty()) {
            fireFilesDeleted(removedFiles);
            setDBDirty();

            // Broadcast to members
            diskItemFilter.filterFileInfos(removedFiles);
            FolderFilesChanged changes = new FolderFilesChanged(getInfo());
            changes.removed = removedFiles.toArray(new FileInfo[0]);
            if (changes.removed.length > 0) {
                broadcastMessages(changes);
            }
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
                Convert.cleanMemberInfos(getController().getNodeManager(),
                    files);
                for (FileInfo fileInfo : files) {
                    // scanFile(fileInfo);
                    addFile(fileInfo);
                }

                // read them always ..
                MemberInfo[] members1 = (MemberInfo[]) in.readObject();
                // Do not load members
                log().verbose("Loading " + members1.length + " members");
                for (MemberInfo memberInfo : members1) {
                    if (memberInfo.isInvalid(getController())) {
                        continue;
                    }
                    Member member = memberInfo.getNode(getController(), true);
                    join0(member);
                }

                // Old blacklist explicit items.
                // Now disused, but maintained for backward compatability.
                try {
                    Object object = in.readObject();
                    Collection<FileInfo> infos = (Collection<FileInfo>) object;
                    for (FileInfo info : infos) {
                        diskItemFilter.addPattern(info.getName());
                        if (logEnabled) {
                            log().verbose("ignore@" + info.getName());
                        }
                    }
                } catch (java.io.EOFException e) {
                    log().debug("ignore nothing for " + this);
                } catch (Exception e) {
                    log().error("read ignore error: " + this + e.getMessage(),
                        e);
                }

                try {
                    Object object = in.readObject();
                    if (object instanceof Date) {
                        lastScan = (Date) object;
                        if (logEnabled) {
                            log().verbose("lastScan" + lastScan);
                        }
                    }
                } catch (java.io.EOFException e) {
                    // ignore nothing available for ignore
                    log().debug("ignore nothing for " + this);
                } catch (Exception e) {
                    log().error("read ignore error: " + this + e.getMessage(),
                        e);
                }

                try {
                    Object object = in.readObject();
                    if (object instanceof Date) {
                        lastFileChangeDate = (Date) object;
                        if (logEnabled) {
                            log().verbose(
                                "lastFileChangeDate" + lastFileChangeDate);
                        }
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

                log().debug(
                    "Loaded folder database (" + files.length + " files) from "
                        + dbFile.getAbsolutePath());
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

        log().debug(
            "Unable to read folder db, even from backup. Maybe new folder?");
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
        if (dirty) {
            persist();
        }
        if (diskItemFilter.isDirty()) {
            diskItemFilter.savePatternsTo(getSystemSubDir());
        }
        removeAllListeners();
    }

    /**
     * Stores the current file-database to disk
     */
    private void storeFolderDB() {
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
                FileInfo[] files = knownFiles.values().toArray(new FileInfo[0]);
                if (dbFile.exists()) {
                    if (!dbFile.delete()) {
                        log()
                            .error("Failed to delete database file: " + dbFile);
                    }
                }
                if (!dbFile.createNewFile()) {
                    log().error("Failed to create database file: " + dbFile);
                }
                OutputStream fOut = new BufferedOutputStream(
                    new FileOutputStream(dbFile));
                ObjectOutputStream oOut = new ObjectOutputStream(fOut);
                // Store files
                oOut.writeObject(files);
                // Store members
                oOut.writeObject(Convert.asMemberInfos(getMembersAsCollection()
                    .toArray(new Member[0])));
                // Old blacklist. Maintained for backward serialization
                // compatability. Do not remove.
                oOut.writeObject(new ArrayList<FileInfo>());
                if (lastScan == null) {
                    if (logEnabled) {
                        log().verbose("write default time: " + new Date());
                    }
                    oOut.writeObject(new Date());
                } else {
                    if (logEnabled) {
                        log().verbose("write time: " + lastScan);
                    }
                    oOut.writeObject(lastScan);
                }
                if (lastFileChangeDate == null) {
                    if (logEnabled) {
                        log().verbose("write default time: " + new Date());
                    }
                    oOut.writeObject(new Date());
                } else {
                    if (logEnabled) {
                        log().verbose("write time: " + lastFileChangeDate);
                    }
                    oOut.writeObject(lastFileChangeDate);
                }
                oOut.close();
                fOut.close();

                if (logDebug) {
                    log().debug(
                        "Successfully wrote folder database file ("
                            + files.length + " files)");
                }

                // Make backup
                FileUtils.copyFile(dbFile, dbFileBackup);

                // TODO Remove this in later version
                // Cleanup for older versions
                File oldDbFile = new File(localBase, DB_FILENAME);
                if (!oldDbFile.delete()) {
                    log().verbose(
                        "Failed to delete 'old' database file: " + oldDbFile);
                }
                File oldDbFileBackup = new File(localBase, DB_BACKUP_FILENAME);
                if (!oldDbFileBackup.delete()) {
                    log().verbose(
                        "Failed to delete backup of 'old' database file: "
                            + oldDbFileBackup);
                }
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

    private boolean maintainFolderDBrequired() {
        // TODO Implement
        if (Feature.HIGH_FREQUENT_FOLDER_DB_MAINTENANCE.isEnabled()) {
            if (lastDBMaintenance == null) {
                return true;
            }
            // About every 5 second now
            return lastDBMaintenance.getTime() + 5 * 1000L < System
                .currentTimeMillis();
        }
        return false;

    }

    /**
     * Cleans up fileinfos of deleted files that are old than the configured max
     * age.
     * 
     * @see ConfigurationEntry#MAX_FILEINFO_DELETED_AGE_SECONDS
     */
    private void maintainFolderDB() {
        long removeBeforeDate = System.currentTimeMillis()
            - 1000L
            * ConfigurationEntry.MAX_FILEINFO_DELETED_AGE_SECONDS
                .getValueInt(getController());
        int nFilesBefore = knownFiles.size();
        log().warn(
            "Maintaing folder db, files before: " + nFilesBefore
                + " removing all deleted files older than "
                + new Date(removeBeforeDate));
        int deleted = 0;
        for (FileInfo file : knownFiles.keySet()) {
            if (!file.isDeleted()) {
                continue;
            }
            if (file.getModifiedDate().getTime() < removeBeforeDate) {
                // log().warn("Would remove file: " + file.toDetailString());
                deleted++;
                knownFiles.remove(file);
                transferPriorities.removeFile(file);
            }
        }
        log().warn(
            "Maintaing folder db, files after: " + knownFiles.size()
                + ". Removed: " + deleted);
        if (deleted > 0) {
            dirty = true;
        }
        // TODO Fire Event!
        lastDBMaintenance = new Date();
    }

    /**
     * Set the needed folder/file attributes on windows systems, if we have a
     * desktop.ini
     * 
     * @param desktopIni
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
     * @param active
     *            true if the desktop shortcut should be created.
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
     * Gets the sync profile.
     * 
     * @return the syncprofile of this folder
     */
    public SyncProfile getSyncProfile() {
        return syncProfile;
    }

    /**
     * Sets the synchronisation profile for this folder
     * 
     * @param aSyncProfile
     */
    public void setSyncProfile(SyncProfile aSyncProfile) {
        if (aSyncProfile == null) {
            throw new NullPointerException("Unable to set null sync profile");
        }
        if (previewOnly) {
            throw new IllegalStateException(
                "Can not set Sync Profile in Preview mode.");
        }
        SyncProfile oldProfile = syncProfile;

        log().debug("Setting " + aSyncProfile.getProfileName());
        syncProfile = aSyncProfile;

        // Store on disk
        String syncProfKey = FOLDER_SETTINGS_PREFIX + getName()
            + FOLDER_SETTINGS_SYNC_PROFILE;
        getController().getConfig()
            .put(syncProfKey, syncProfile.getFieldList());
        getController().saveConfig();

        if (!syncProfile.isAutodownload()) {
            // Possibly changed from autodownload to manual, we need to abort
            // all automatic download
            getController().getTransferManager().abortAllAutodownloads(this);
        }
        if (syncProfile.isAutodownload()) {
            // Trigger request files
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(this.currentInfo);
        }

        if (syncProfile.isSyncDeletion()) {
            syncRemoteDeletedFiles(false);
        }

        recommendScanOnNextMaintenance();
        fireSyncProfileChanged();
    }

    /**
     * Recommends the scan of the local filesystem on the next maintenace run.
     * Useful when files are detected that have been changed.
     * <p>
     * ATTENTION: Does not force a scan if "hot" auto-detection is not enabled.
     */
    public void recommendScanOnNextMaintenance() {
        if (!getSyncProfile().isAutoDetectLocalChanges()
            || getSyncProfile().getConfiguration().isDailySync())
        {
            return;
        }
        if (logVerbose) {
            log().verbose("recommendScanOnNextMaintenance");
        }
        scanForced = true;
        lastScan = null;
    }

    /**
     * Runs the maintenance on this folder. This means the folder gets synced
     * with remotesides.
     */
    public void maintain() {
        log().verbose("Maintaining '" + getName() + '\'');

        // local files
        log().verbose("Forced: " + scanForced);
        boolean forcedNow = scanForced;
        scanForced = false;
        if (forcedNow || autoScanRequired()) {
            scanLocalFiles();
        }
        if (maintainFolderDBrequired()) {
            maintainFolderDB();
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
        join0(member);

        // Fire event
        fireMemberJoined(member);
    }

    /**
     * Joins a member to the folder. Does not fire the event
     * 
     * @param member
     */
    private void join0(Member member) {
        Reject.ifNull(member, "Member is null, unable to join");
        // member will be joined, here on local
        boolean wasMember = members.put(member, member) != null;
        if (logVerbose) {
            log().verbose("Member joined " + member);
        }
        // send him our list of files if completely connected. otherwise this
        // gets sent by Member.completeHandshake();
        if (!wasMember && member.isCompleteyConnected()) {
            member.sendMessagesAsynchron(FileList.createFileListMessages(this));
        }
    }

    /**
     * Removes a member from this folder
     * 
     * @param member
     */
    public void remove(Member member) {
        if (members.remove(member) == null) {
            // Skip if not member
            return;
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
    public boolean isTransferring() {
        return getController().getTransferManager()
            .countNumberOfDownloads(this) > 0
            || getController().getTransferManager().countUploadsOn(this) > 0;
    }

    /**
     * @return true if the folder get currently scanned
     */
    public boolean isScanning() {
        return getController().getFolderRepository().getFolderScanner()
            .getCurrentScanningFolder() == this;
    }

    /**
     * Checks if the folder is in Downloading, called by FolderRepository
     * 
     * @return if this folder downloading
     */
    public boolean isDownloading() {
        return getController().getTransferManager()
            .countNumberOfDownloads(this) > 0;
    }

    /**
     * Checks if the folder is in Uploading, called by FolderRepository
     * 
     * @return if this folder uploading
     */
    public boolean isUploading() {
        return getController().getTransferManager().countUploadsOn(this) > 0;
    }

    /**
     * Synchronizes the deleted files with local folder
     * 
     * @param force
     *            true if the sync is forced with ALL connected members of the
     *            folder. otherwise it checks the modifier.
     */
    public void syncRemoteDeletedFiles(boolean force) {
        if (logVerbose) {
            log().verbose(
                "Deleting files, which are deleted by friends. con-members: "
                    + Arrays.asList(getConnectedMembers()));
        }

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();

        for (Member member : getMembersAsCollection()) {
            if (!member.isCompleteyConnected()) {
                // disconected go to next member
                continue;
            }

            Collection<FileInfo> fileList = member
                .getLastFileListAsCollection(currentInfo);
            if (fileList == null) {
                continue;
            }

            if (logVerbose) {
                log().verbose(
                    "RemoteFileDeletion sync. Member '" + member.getNick()
                        + "' has " + fileList.size() + " possible files");
            }
            for (FileInfo remoteFile : fileList) {
                boolean modifiedByFriend = remoteFile
                    .isModifiedByFriend(getController());
                boolean syncFromMemberAllowed = (modifiedByFriend && syncProfile
                    .getConfiguration().isSyncDeletionWithFriends())
                    || (!modifiedByFriend && syncProfile.getConfiguration()
                        .isSyncDeletionWithOthers()) || force;

                if (!syncFromMemberAllowed) {
                    // Not allowed to sync from that guy.
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

                        log().verbose(
                            "File was deleted by " + member
                                + ", deleting local: " + localCopy);

                        // Abort dl if one is active
                        DownloadManager dl = getController()
                            .getTransferManager().getActiveDownload(localFile);
                        if (dl != null) {
                            dl.abortAndCleanup();
                        }

                        synchronized (deleteLock) {
                            if (localCopy.exists()) {
                                deleteFile(localFile, localCopy);
                            }
                        }
                        // FIXME: Size might not be correct
                        localFile.setDeleted(true);
                        localFile.setModifiedInfo(remoteFile.getModifiedBy(),
                            remoteFile.getModifiedDate());
                        localFile.setVersion(remoteFile.getVersion());

                        // File has been removed
                        removedFiles.add(localFile);
                    }
                }
            }
        }

        // Broadcast folder change if changes happend
        if (!removedFiles.isEmpty()) {
            fireFilesDeleted(removedFiles);
            setDBDirty();

            // Broadcast to members
            diskItemFilter.filterFileInfos(removedFiles);
            FolderFilesChanged changes = new FolderFilesChanged(getInfo());
            changes.removed = removedFiles.toArray(new FileInfo[0]);
            broadcastMessages(changes);
        }
    }

    /**
     * Broadcasts a message through the folder
     * 
     * @param message
     */
    public void broadcastMessages(Message... message) {
        for (Member member : getMembersAsCollection()) {
            // Connected?
            if (member.isCompleteyConnected()) {
                // sending all nodes my knows nodes
                member.sendMessagesAsynchron(message);
            }
        }
    }

    /**
     * Broadcasts the remote commando to scan the folder.
     */
    public void broadcastScanCommand() {
        if (logVerbose) {
            log().verbose("Broadcasting remote scan commando");
        }
        Message scanCommand = new ScanCommand(getInfo());
        broadcastMessages(scanCommand);
    }

    private void broadcastFolderChanges(ScanResult scanResult) {
        int addedMsgs = 0;
        int changedMsgs = 0;
        int deletedMsgs = 0;
        int restoredMsgs = 0;
        if (getConnectedMembersCount() == 0) {
            return;
        }
        if (scanResult.getNewFiles().size() > 0) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(this.currentInfo, scanResult
                    .getNewFiles(), diskItemFilter, true);
            if (msgs != null) {
                addedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        if (scanResult.getChangedFiles().size() > 0) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(this.currentInfo, scanResult
                    .getChangedFiles(), diskItemFilter, true);
            if (msgs != null) {
                changedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        if (scanResult.getDeletedFiles().size() > 0) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(this.currentInfo, scanResult
                    .getDeletedFiles(), diskItemFilter, false);
            if (msgs != null) {
                deletedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        if (scanResult.getRestoredFiles().size() > 0) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(this.currentInfo, scanResult
                    .getRestoredFiles(), diskItemFilter, true);
            if (msgs != null) {
                restoredMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        if (logDebug) {
            log().debug(
                "Broadcasted folder changes " + addedMsgs + " addedmsgs, "
                    + changedMsgs + " changedmsgs, " + deletedMsgs
                    + " deletedmsgs, " + restoredMsgs + " restoredmsgs");
        }
    }

    /**
     * Callback method from member. Called when a new filelist was send
     * 
     * @param from
     * @param newList
     */
    public void fileListChanged(Member from, FileList newList) {
        // log().debug(
        // "New Filelist received from " + from + " #files: "
        // + newList.files.length);

        // Try to find same files
        findSameFiles(from, Arrays.asList(newList.files));

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
                .triggerFileRequesting(newList.folder);
        }

        // Handle remote deleted files
        if (getSyncProfile().isSyncDeletion()) {
            // FIXME: Is wrong, since it syncs with completely connected members
            // only
            // FIXME: Is called too often, should be called after finish of
            // filelist sending only.
            syncRemoteDeletedFiles(false);
        }

        fireRemoteContentsChanged(newList);
    }

    /**
     * Callback method from member. Called when a filelist delta received
     * 
     * @param from
     * @param changes
     */
    public void fileListChanged(final Member from,
        final FolderFilesChanged changes)
    {
        // log().debug("File changes received from " + from);

        // Try to find same files
        if (changes.added != null) {
            findSameFiles(from, Arrays.asList(changes.added));
        }
        if (changes.removed != null) {
            findSameFiles(from, Arrays.asList(changes.removed));
        }

        // don't do this in the server version
        if (rootDirectory != null) {
            if (changes.added != null) {
                getDirectory().addAll(from, changes.added);
            }
            if (changes.removed != null) {
                getDirectory().addAll(from, changes.removed);
            }
        }

        // Avoid hammering of sync remote deletion
        boolean singleFileMsg = changes.added != null
            && changes.added.length == 1 && changes.removed == null;

        if (getSyncProfile().isAutodownload()) {
            // Check if we need to trigger the filerequestor
            boolean triggerFileRequestor = true;
            if (singleFileMsg) {
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
                    .triggerFileRequesting(changes.folder);
            } else if (logVerbose) {
                log().verbose(
                    "Not triggering filerequestor, no new files in remote filelist"
                        + changes + " from " + from);
            }
        }

        // Handle remote deleted files
        if (!singleFileMsg && getSyncProfile().isSyncDeletion()) {
            // FIXME: Is wrong, since it syncs with completely connected members
            // only
            // FIXME: Is called too often, should be called after finish of
            // filelist sending only.
            syncRemoteDeletedFiles(false);
        }

        // Fire event
        fireRemoteContentsChanged(changes);
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
     * 3. The remote file version is >0 and is not deleted
     * <p>
     * This if files moved from node to node without PowerFolder. e.g. just copy
     * over windows share. Helps to identifiy same files and prevents unessesary
     * downloads.
     * 
     * @param remoteFileInfos
     */
    private void findSameFiles(Member remotePeer,
        Collection<FileInfo> remoteFileInfos)
    {
        Reject.ifNull(remoteFileInfos, "Remote file info list is null");
        if (logVerbose) {
            log().verbose(
                "Triing to find same files in remote list with "
                    + remoteFileInfos.size() + " files from " + remotePeer);
        }
        boolean checkForFilenameProblems = OSUtil.isWindowsSystem();
        Map<String, FileInfo> problemCanidates = new HashMap<String, FileInfo>();
        for (FileInfo remoteFileInfo : remoteFileInfos) {
            FileInfo localFileInfo = getFile(remoteFileInfo);
            if (localFileInfo == null) {
                if (checkForFilenameProblems) {
                    // Possible check canidate for case-problem file matching
                    problemCanidates.put(remoteFileInfo.getLowerCaseName(),
                        remoteFileInfo);
                }
                continue;
            }
            if (localFileInfo.isDeleted() || remoteFileInfo.isDeleted()) {
                continue;
            }
            if (localFileInfo.getVersion() < remoteFileInfo.getVersion()
                && remoteFileInfo.getVersion() > 0)
            {
                boolean fileSizeSame = localFileInfo.getSize() == remoteFileInfo
                    .getSize();
                boolean dateSame = Util.equalsFileDateCrossPlattform(
                    localFileInfo.getModifiedDate(), remoteFileInfo
                        .getModifiedDate());
                boolean localFileNewer = Util.isNewerFileDateCrossPlattform(
                    localFileInfo.getModifiedDate(), remoteFileInfo
                        .getModifiedDate());
                if (fileSizeSame && dateSame) {
                    if (logWarn) {
                        log().warn(
                            "Found identical file remotely: local "
                                + localFileInfo.toDetailString() + " remote: "
                                + remoteFileInfo.toDetailString()
                                + ". Taking over modification infos");
                    }
                    localFileInfo.copyFrom(remoteFileInfo);
                    // FIXME That might produce a LOT of traffic! Single update
                    // message per file! This also might intefere with FileList
                    // exchange at beginning of communication
                    fileChanged(localFileInfo);
                }
                if (localFileNewer) {
                    if (logWarn) {
                        log().warn(
                            "Found file remotely, but local is newer: local "
                                + localFileInfo.toDetailString() + " remote: "
                                + remoteFileInfo.toDetailString()
                                + ". Increasing local version to "
                                + (remoteFileInfo.getVersion() + 1));
                    }
                    localFileInfo.setVersion(remoteFileInfo.getVersion() + 1);
                    // FIXME That might produce a LOT of traffic! Single update
                    // message per file! This also might intefere with FileList
                    // exchange at beginning of communication
                    fileChanged(localFileInfo);
                }
            }
        }

        // log().warn("Canidates files: " + problemCanidates);

        // Check for problematic files (TRAC #232)
        // Disabled for windows #836
        if (!checkForFilenameProblems || OSUtil.isWindowsSystem()) {
            return;
        }

        Map<FileInfo, List<FilenameProblem>> problemFiles = new HashMap<FileInfo, List<FilenameProblem>>();
        for (FileInfo localInfo : knownFiles.keySet()) {
            FileInfo problemFileInfo = problemCanidates.get(localInfo
                .getLowerCaseName());
            if (problemFileInfo == null) {
                continue;
            }

            // Duplicate problem!
            problemFiles
                .put(localInfo, Collections.singletonList(new FilenameProblem(
                    localInfo, problemFileInfo)));
        }

        if (logWarn && !problemFiles.isEmpty()) {
            log().warn("Got " + problemFiles.size() + " problematic files");
        }

        FileNameProblemHandler handler = getController().getFolderRepository()
            .getFileNameProblemHandler();
        // log().warn("Problem handler: " + handler);
        if (handler != null && !problemFiles.isEmpty()) {
            handler.fileNameProblemsDetected(new FileNameProblemEvent(this,
                problemFiles));
        }

        // log().warn("Handled problematic files: " + problemFiles);
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
            Collection<FileInfo> lastFileList = member
                .getLastFileListAsCollection(this.getInfo());
            if (lastFileList != null) {
                findSameFiles(member, lastFileList);
            }
        }
    }

    /**
     * Sets the DB to a dirty state. e.g. through a change. gets stored on next
     * persisting run.
     */
    private void setDBDirty() {
        dirty = true;
    }

    /**
     * Persists settings to disk.
     */
    private void persist() {
        log().verbose("Persisting settings");

        storeFolderDB();

        // Write filelist
        if (Logger.isLogToFileEnabled()) {
            // Write filelist to disk
            File debugFile = new File(Logger.getDebugDir(), getName() + '/'
                + getController().getMySelf().getNick() + ".list.txt");
            Debug.writeFileListCSV(knownFiles.keySet(), "FileList of folder "
                + getName() + ", member " + this + ':', debugFile);
        }

        dirty = false;
    }

    /*
     * Simple getters/exposing methods ****************************************
     */

    /**
     * @return the local base directory
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
            if (systemSubDir.mkdirs()) {
                FileUtils.makeHiddenOnWindows(systemSubDir);
            } else {
                log().error("Failed to create system subdir: " + systemSubDir);
            }
        }

        return systemSubDir;
    }

    public boolean isSystemSubDir(File aDir) {
        return aDir.isDirectory()
            && getSystemSubDir().getAbsolutePath().equals(
                aDir.getAbsolutePath());
    }

    /**
     * @return true if this folder is disconnected/not available at the moment.
     */
    public boolean isDeviceDisconnected() {
        return deviceDisconnected;
    }

    public String getName() {
        return currentInfo.name;
    }

    public int getKnownFilesCount() {
        return knownFiles.size();
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }

    /**
     * ATTENTION: DO NOT USE!!
     * 
     * @return the internal file database as array. ONLY FOR TESTs
     */
    public FileInfo[] getKnowFilesAsArray() {
        return knownFiles.keySet().toArray(new FileInfo[0]);
    }

    /**
     * WARNING: Contents may change after getting the collection.
     * 
     * @return a unmodifiable collection referecing the internal database
     *         hashmap (keySet).
     */
    public Collection<FileInfo> getKnownFiles() {
        return Collections.unmodifiableCollection(knownFiles.keySet());
    }

    /** package protected, used by FolderScanner */
    Map<FileInfo, FileInfo> getKnownFilesMap() {
        return Collections.unmodifiableMap(knownFiles);
    }

    /**
     * get the Directories in this folder (including the subs and files)
     * 
     * @return Directory with all sub dirs and treeNodes set
     */
    public Directory getDirectory() {
        return rootDirectory;
    }

    /**
     * Common file delete method. Either deletes the file or moves it to the
     * recycle bin.
     * 
     * @param fileInfo
     * @param file
     */
    private void deleteFile(FileInfo fileInfo, File file) {
        if (useRecycleBin) {
            RecycleBin recycleBin = getController().getRecycleBin();
            if (!recycleBin.moveToRecycleBin(fileInfo, file)) {
                log().error("Unable to move file to recycle bin" + file);
                if (!file.delete()) {
                    log().error("Unable to delete file " + file);
                }
            }
        } else {
            if (!file.delete()) {
                log().error("Unable to delete file " + file);
            }
        }
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @param includeNonFriendFiles
     *            if files should be included, that are modified by non-friends
     * @return the list of files that are incoming/newer available on remote
     *         side as unmodifiable collection.
     */
    public Collection<FileInfo> getIncomingFiles(boolean includeNonFriendFiles)
    {
        // build a temp list
        // Map<FileInfo, FileInfo> incomingFiles = new HashMap<FileInfo,
        // FileInfo>();
        SortedMap<FileInfo, FileInfo> incomingFiles = new TreeMap<FileInfo, FileInfo>(
            new DiskItemComparator(DiskItemComparator.BY_NAME));
        // add expeced files
        for (Member member : getMembersAsCollection()) {
            if (!member.isCompleteyConnected()) {
                // disconnected
                continue;
            }
            if (!member.hasCompleteFileListFor(getInfo())) {
                log().warn(
                    "Skipping " + member + " no complete filelist from him");
                continue;
            }

            Collection<FileInfo> memberFiles = getFilesAsCollection(member);
            if (memberFiles == null) {
                continue;
            }
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
                if (notLocal && remoteFile.isDeleted()) {
                    // A remote deleted file is not incoming!
                    // TODO Maby download deleted files from archive of remote?
                    // and put it directly into own recycle bin.
                    continue;
                }
                if (notLocal || (newerThanLocal && newestRemote)) {
                    // Okay this one is expected
                    incomingFiles.put(remoteFile, remoteFile);
                }
            }
        }

        if (incomingFiles.isEmpty()) {
            log().verbose("No Incoming files");
        } else {
            log().debug("Incoming files " + incomingFiles.size());
        }

        return Collections.unmodifiableCollection(incomingFiles.keySet());
    }

    /**
     * @param member
     * @return the list of files from a member as unmodifiable collection
     */
    public Collection<FileInfo> getFilesAsCollection(Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        if (member.isMySelf()) {
            return getKnownFiles();
        }
        Collection<FileInfo> list = member
            .getLastFileListAsCollection(getInfo());
        if (list == null) {
            return null;
        }
        return list;
    }

    /**
     * This list also includes myself!
     * 
     * @return all members in a collection. The collection is a unmodifiable
     *         referece to the internal member storage. May change after has
     *         been returned!
     */
    public Collection<Member> getMembersAsCollection() {
        return Collections.unmodifiableCollection(members.values());
    }

    /**
     * @return the number of members
     */
    public int getMembersCount() {
        return members.size();
    }

    /**
     * @return the number of connected members EXCLUDING myself.
     */
    public int getConnectedMembersCount() {
        int nConnected = 0;
        for (Member member : members.values()) {
            if (member.isCompleteyConnected()) {
                nConnected++;
            }
        }
        return nConnected;
    }

    public Member[] getConnectedMembers() {
        List<Member> connected = new ArrayList<Member>(members.size());
        for (Member member : getMembersAsCollection()) {
            if (member.isCompleteyConnected()) {
                if (member.isMySelf()) {
                    continue;
                }
                connected.add(member);
            }
        }
        return connected.toArray(new Member[0]);
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
        return members.keySet().contains(member);
    }

    /**
     * @param fInfo
     * @return if folder has this file
     */
    public boolean hasFile(FileInfo fInfo) {
        return knownFiles.containsKey(fInfo);
    }

    /**
     * @param fInfo
     * @return the local fileinfo instance
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
     * Date of the last file change for this folder.
     * 
     * @return
     */
    public Date getLastFileChangeDate() {
        return lastFileChangeDate;
    }

    /**
     * @return the info object of this folder
     */
    public FolderInfo getInfo() {
        return currentInfo;
    }

    /**
     * @return the statistic for this folder
     */
    public FolderStatistic getStatistic() {
        return statistic;
    }

    /**
     * @return an Invitation to this folder. Includes a intelligent opposite
     *         sync profile.
     */
    public Invitation createInvitation() {
        Invitation inv = new Invitation(currentInfo, getController()
            .getMySelf().getInfo());
        inv.setSuggestedSyncProfile(syncProfile);
        if (syncProfile.equals(SyncProfile.BACKUP_SOURCE)) {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_TARGET);
        } else if (syncProfile.equals(SyncProfile.BACKUP_TARGET)) {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_SOURCE);
        } else if (syncProfile.equals(SyncProfile.HOST_FILES)) {
            inv.setSuggestedSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        }
        inv.setSuggestedLocalBase(localBase);
        return inv;
    }

    public String toString() {
        return currentInfo.toString();
    }

    // Logger methods *********************************************************

    public String getLoggerName() {
        return "Folder '" + getName() + '\'';
    }

    /**
     * Ensures that default ignore patterns are set.
     */
    public void addDefaultExcludes() {
        if (OSUtil.isWindowsSystem()) {
            // Add thumbs to ignore pattern on windows systems
            // Don't duplicate thumbs (like when moving a preview folder)
            addPattern(THUMBS_DB);

            // ... and temporary word files
            addPattern(WORD_TEMP);

            // Add desktop.ini to ignore pattern on windows systems
            if (!OSUtil.isWindowsVistaSystem()
                && ConfigurationEntry.USE_PF_ICON
                    .getValueBoolean(getController()))
            {
                addPattern(FileUtils.DESKTOP_INI_FILENAME);
            }
        }
        if (OSUtil.isMacOS()) {
            // Add dsstore to ignore pattern on mac systems
            // Don't duplicate dsstore (like when moving a preview folder)
            addPattern(DS_STORE);
        }

    }

    // Inner classes **********************************************************

    /**
     * Persister task, persists settings from time to time.
     */
    private class Persister extends TimerTask {
        @Override
        public void run() {
            if (dirty) {
                persist();
            }
            if (diskItemFilter.isDirty()) {
                diskItemFilter.savePatternsTo(getSystemSubDir());
            }
        }
    }

    /**
     * Whether this folder moves deleted files to the recycle bin.
     * 
     * @return true if the recycle bin is used.
     */
    public boolean isUseRecycleBin() {
        return useRecycleBin;
    }

    /**
     * Sets whether to use the recycle bin.
     * 
     * @param useRecycleBin
     *            true if recycle bin is to be used.
     */
    public void setUseRecycleBin(boolean useRecycleBin) {
        this.useRecycleBin = useRecycleBin;
    }

    // *************** Event support
    public void addMembershipListener(FolderMembershipListener listener) {
        getController().getListenerSupportFactory().addListener(folderMembershipListenerSupport,
            listener);
    }

    public void removeMembershipListener(FolderMembershipListener listener) {
        getController().getListenerSupportFactory().removeListener(folderMembershipListenerSupport,
            listener);
    }

    public void addFolderListener(FolderListener listener) {
        getController().getListenerSupportFactory().addListener(folderListenerSupport, listener);
    }

    public void removeFolderListener(FolderListener listener) {
        getController().getListenerSupportFactory().removeListener(folderListenerSupport, listener);
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

    private void fileChanged(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");

        fireFileChanged(fileInfo);
        setDBDirty();

        // TODO FIXME Check if this is necessary
        FileInfo localInfo = getFile(fileInfo);
        if (diskItemFilter.isRetained(localInfo)) {
            broadcastMessages(new FolderFilesChanged(localInfo));
        }
    }

    private void fireFileChanged(FileInfo fileInfo) {
        if (log().isVerbose()) {
            log().verbose("fireFileChanged: " + this);
        }
        lastFileChangeDate = new Date();
        FolderEvent folderEvent = new FolderEvent(this, fileInfo);
        folderListenerSupport.fileChanged(folderEvent);
    }

    private void fireFilesDeleted(Collection<FileInfo> fileInfos) {
        if (log().isVerbose()) {
            log().verbose("fireFilesDeleted: " + this);
        }
        lastFileChangeDate = new Date();
        FolderEvent folderEvent = new FolderEvent(this, fileInfos);
        folderListenerSupport.filesDeleted(folderEvent);
    }

    private void fireRemoteContentsChanged(FileList list) {
        if (log().isVerbose()) {
            log().verbose("fireRemoteContentsChanged: " + this);
        }
        lastFileChangeDate = new Date();
        FolderEvent folderEvent = new FolderEvent(this, list);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    private void fireRemoteContentsChanged(FolderFilesChanged list) {
        if (log().isVerbose()) {
            log().verbose("fireRemoteContentsChanged: " + this);
        }
        lastFileChangeDate = new Date();
        FolderEvent folderEvent = new FolderEvent(this, list);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    private void fireSyncProfileChanged() {
        FolderEvent folderEvent = new FolderEvent(this, getSyncProfile());
        folderListenerSupport.syncProfileChanged(folderEvent);
    }

    private void fireScanResultCommited(ScanResult scanResult) {
        if (log().isVerbose()) {
            log().debug("fireScanResultCommited: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, scanResult);
        folderListenerSupport.scanResultCommited(folderEvent);
    }

    /** package protected because fired by FolderStatistics */
    void fireStatisticsCalculated() {
        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.statisticsCalculated(folderEvent);
    }
}