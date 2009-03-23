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

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX_V4;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
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
import java.util.concurrent.ConcurrentMap;

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
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.ScanCommand;
import de.dal33t.powerfolder.transfer.TransferPriorities;
import de.dal33t.powerfolder.transfer.TransferPriorities.TransferPriority;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.compare.DiskItemComparator;
import de.dal33t.powerfolder.util.logging.LoggingManager;
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
    private ConcurrentMap<FileInfo, FileInfo> knownFiles;

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

    /**
     * The lock to hold when initializing the root directory.
     */
    private final Object rootDirectoryInitLock = new Object();

    /** cached Directory object */
    private Directory rootDirectory;

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
    private volatile boolean dirty;

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
        currentInfo = new FolderInfo(fInfo.name, fInfo.id);

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
        hasOwnDatabase = false;
        dirty = false;

        localBase = folderSettings.getLocalBaseDir();

        syncProfile = folderSettings.getSyncProfile();

        // Create listener support
        folderListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderListener.class);

        folderMembershipListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderMembershipListener.class);

        useRecycleBin = folderSettings.isUseRecycleBin();

        whitelist = folderSettings.isWhitelist();

        // Check base dir
        try {
            checkBaseDir(localBase, false);
        } catch (FolderException e) {
            logWarning("local base inaccessible for " + fInfo.name);
            deviceDisconnected = true;
        }

        statistic = new FolderStatistic(this);
        knownFiles = new ConcurrentHashMap<FileInfo, FileInfo>();
        members = new ConcurrentHashMap<Member, Member>();

        // put myself in membership
        join0(controller.getMySelf());

        logFine("Opening " + toString() + " at '"
            + localBase.getAbsolutePath() + '\'');

        FileFilter allExceptSystemDirFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return !isSystemSubDir(pathname);
            }
        };
        if (localBase.list() != null
            && localBase.listFiles(allExceptSystemDirFilter).length == 0)
        {
            // Empty folder... no scan required for database
            hasOwnDatabase = true;
            setDBDirty();
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
        if (isInfo()) {
            if (hasOwnDatabase) {
                logFiner("Has own database (" + getName() + ")? "
                    + hasOwnDatabase);
            } else {
                logFine("Has own database (" + getName() + ")? "
                    + hasOwnDatabase);
            }
        }
        if (hasOwnDatabase) {
            // Write filelist
            if (LoggingManager.isLogToFile()
                && Feature.DEBUG_WRITE_FILELIST_CSV.isEnabled())
            {
                writeFilelist();
            }
        }

        // Register persister
        // FIXME: There is no way to remove the persister on shutdown.
        // Only on controller shutdown
        Persister persister = new Persister();
        getController().scheduleAndRepeat(
                persister,
            1000L * ConfigurationEntry.FOLDER_DB_PERSIST_TIME
                .getValueInt(getController()));

        // Create invitation
        if (folderSettings.isCreateInvitationFile()) {
            Invitation inv = createInvitation();
            InvitationUtil.save(inv, new File(folderSettings.getLocalBaseDir(),
                FileUtils.removeInvalidFilenameChars(inv.folder.name)
                    + ".invitation"));
        }

        previewOnly = folderSettings.isPreviewOnly();

        // Initialized lazyliy
        rootDirectory = null;
    }

    /**
     * Commits the scan results into the internal file database. Changes get
     * broadcasted to other members if nessesary. public because also called
     * from SyncFolderPanel (until that class maybe handles that itself)
     * 
     * @param scanResult
     *            the scanresult to commit.
     */
    private void commitScanResult(ScanResult scanResult) {
        synchronized (scanLock) {
            synchronized (knownFiles) {
                for (FileInfo newFileInfo : scanResult.getNewFiles()) {
                    // add to the DB
                    FileInfo localFile = knownFiles.putIfAbsent(newFileInfo,
                        newFileInfo);
                    // boolean reallyNew = localFile == null || localFile ==
                    // newFileInfo;
                    if (!(localFile == null || localFile == newFileInfo)) {
                        // Probably this got into database thru download in the
                        // meantime
                        logSevere("Skipping add of new scanned file, already in db: "
                            + localFile.toDetailString()
                            + ", scanned: "
                            + newFileInfo.toDetailString());
                    } else {
                        // Add file to folder
                        currentInfo.addFile(newFileInfo);
                        if (rootDirectory != null) {
                            if (isFiner()) {
                                logFiner("Adding "
                                    + scanResult.getNewFiles().size()
                                    + " to directory");
                            }
                            rootDirectory.add(getController().getMySelf(),
                                newFileInfo);
                        }
                    }
                }

                // deleted files
                for (FileInfo deletedFileInfo : scanResult.getDeletedFiles()) {
                    deletedFileInfo.setDeleted(true);
                    deletedFileInfo.setSize(0);
                    deletedFileInfo
                        .setVersion(deletedFileInfo.getVersion() + 1);
                    deletedFileInfo.setModifiedInfo(getController().getMySelf()
                        .getInfo(), new Date());
                }

                // restored files
                for (FileInfo restoredFileInfo : scanResult.getRestoredFiles())
                {
                    File diskFile = getDiskFile(restoredFileInfo);
                    restoredFileInfo.setModifiedInfo(getController()
                        .getMySelf().getInfo(), new Date(diskFile
                        .lastModified()));
                    restoredFileInfo.setSize(diskFile.length());
                    restoredFileInfo.setDeleted(false);
                    restoredFileInfo
                        .setVersion(restoredFileInfo.getVersion() + 1);
                }

                // changed files
                for (FileInfo changedFileInfo : scanResult.getChangedFiles()) {
                    File diskFile = getDiskFile(changedFileInfo);
                    changedFileInfo.setModifiedInfo(getController().getMySelf()
                        .getInfo(), new Date(diskFile.lastModified()));
                    changedFileInfo.setSize(diskFile.length());
                    changedFileInfo.setDeleted(!diskFile.exists());
                    changedFileInfo
                        .setVersion(changedFileInfo.getVersion() + 1);
                    // DISABLED because of #644
                    // changedFileInfo.invalidateFilePartsRecord();
                }
            }
        }

        hasOwnDatabase = true;
        if (isFine()) {
            logFine("Scanned " + scanResult.getTotalFilesCount() + " total, "
                + scanResult.getChangedFiles().size() + " changed, "
                + scanResult.getNewFiles().size() + " new, "
                + scanResult.getRestoredFiles().size() + " restored, "
                + scanResult.getDeletedFiles().size() + " removed, "
                + scanResult.getProblemFiles().size() + " problems");
        }

        // Fire scan result
        fireScanResultCommited(scanResult);

        if (scanResult.isChangeDetected()) {
            // Check for identical files
            findSameFilesOnRemote();
            setDBDirty();
            // broadcast changes on folder
            broadcastFolderChanges(scanResult);
        }

        if (isFiner()) {
            logFiner("commitScanResult DONE");
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
    // logFine("Converted: " + converted);
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
    private void checkBaseDir(File baseDir, boolean quite)
        throws FolderException
    {
        // Basic checks
        if (!localBase.exists()) {
            // TRAC #1249
            if ((OSUtil.isMacOS() || OSUtil.isLinux())
                && localBase.getAbsolutePath().toLowerCase().startsWith(
                    "/volumes"))
            {
                throw new FolderException(currentInfo,
                    "Unmounted volume not available at "
                        + localBase.getAbsolutePath());
            }

            if (!localBase.mkdirs()) {
                if (!quite) {
                    logSevere(" not able to create folder(" + getName()
                        + "), (sub) dir (" + localBase + ") creation failed");
                }
                throw new FolderException(currentInfo, Translation
                    .getTranslation("foldercreate.error.unable_to_create",
                        localBase.getAbsolutePath()));
            }
        } else if (!localBase.isDirectory()) {
            if (!quite) {
                logSevere(" not able to create folder(" + getName()
                    + "), (sub) dir (" + localBase + ") is no dir");
            }
            throw new FolderException(currentInfo, Translation.getTranslation(
                "foldercreate.error.unable_to_open", localBase
                    .getAbsolutePath()));
        }

        // Complex checks
        FolderRepository repo = getController().getFolderRepository();
        if (new File(repo.getFoldersBasedir()).equals(baseDir)) {
            throw new FolderException(currentInfo, Translation.getTranslation(
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

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        if (!targetFile.getParentFile().isDirectory()) {
            logSevere("Unable to scan downloaded file. Parent dir is not a directory: "
                + targetFile + ". " + fInfo.toDetailString());
            return false;
        }

        synchronized (deleteLock) {
            if (targetFile.exists()) {
                // if file was a "newer file" the file already esists here
                deleteFile(fInfo, targetFile);
            }
        }

        // Prepare last modification date of tempfile.
        if (!tempFile.setLastModified(fInfo.getModifiedDate().getTime())) {
            logSevere(
                "Failed to set modified date on " + tempFile + " for "
                    + fInfo.getModifiedDate().getTime());
            return false;
        }

        // TODO BOTTLENECK for many transfers
        synchronized (scanLock) {
            if (!tempFile.renameTo(targetFile)) {
                logWarning("Was not able to rename tempfile, copiing "
                    + tempFile.getAbsolutePath() + " to "
                    + targetFile.getAbsolutePath() + ". "
                    + fInfo.toDetailString());

                try {
                    FileUtils.copyFile(tempFile, targetFile);
                } catch (IOException e) {
                    // TODO give a diskfull warning?
                    logSevere("Unable to store completed download "
                        + targetFile.getAbsolutePath() + ". " + e.getMessage()
                        + ". " + fInfo.toDetailString());
                    logFiner(e);
                    return false;
                }

                // Set modified date of remote
                if (!targetFile.setLastModified(fInfo.getModifiedDate()
                    .getTime()))
                {
                    logSevere("Failed to set modified date on " + targetFile
                        + " to " + fInfo.getModifiedDate().getTime());
                    return false;
                }

                if (tempFile.exists() && !tempFile.delete()) {
                    logSevere("Unable to remove temp file: " + tempFile);
                }
            }

            synchronized (knownFiles) {
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

        boolean wasDeviceDisconnected = deviceDisconnected;
        /**
         * Check that we still have a good local base.
         */
        try {
            checkBaseDir(localBase, true);
            deviceDisconnected = false;
        } catch (FolderException e) {
            logFiner("invalid local base: " + e);
            deviceDisconnected = true;
            return false;
        }

        // #1249
        if (!knownFiles.isEmpty() && (OSUtil.isMacOS() || OSUtil.isLinux())) {
            boolean inaccessible = localBase.list() == null
                || localBase.list().length == 0 || !localBase.exists();
            if (inaccessible) {
                logWarning("Local base empty on linux file system, but has known files. "
                    + localBase);
                deviceDisconnected = true;
                return false;
            }
        }
        if (wasDeviceDisconnected && !deviceDisconnected
            && knownFiles.isEmpty())
        {
            // Try to load db from connected device now.
            loadFolderDB();
        }

        ScanResult result;
        synchronized (scanLock) {
            FolderScanner scanner = getController().getFolderRepository()
                .getFolderScanner();
            result = scanner.scanFolderWaitIfBusy(this);
            if (!result.getResultState().equals(ScanResult.ResultState.SCANNED))
            {
                logWarning("Scan result: " + result.getResultState());
            }
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
            if (isFiner()) {
                logFiner("Skipping scan");
            }
            return false;
        }
        Date wasLastScan = lastScan;
        if (wasLastScan == null) {
            return true;
        }

        if (syncProfile.getConfiguration().isDailySync()) {
            if (!shouldDoDailySync()) {
                if (isFiner()) {
                    logFiner("Skipping daily scan");
                }
                return false;
            }

        } else {
            long secondsSinceLastSync = (System.currentTimeMillis() - wasLastScan
                .getTime()) / 1000;
            if (secondsSinceLastSync < syncProfile.getSecondsBetweenScans()) {
                if (isFiner()) {
                    logFiner("Skipping regular scan");
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
        if (isFiner()) {
            logFiner("Last scanned " + lastScannedCalendar.getTime());
        }

        Calendar todayCalendar = new GregorianCalendar();
        todayCalendar.setTime(new Date());
        int currentDayOfYear = todayCalendar.get(Calendar.DAY_OF_YEAR);

        if (lastScannedDay == currentDayOfYear
            && lastScannedCalendar.get(Calendar.YEAR) == todayCalendar
                .get(Calendar.YEAR))
        {
            // Scanned today, so skip.
            if (isFiner()) {
                logFiner("Skipping daily scan (already scanned today)");
            }
            return false;
        }

        int requiredSyncHour = syncProfile.getConfiguration().getDailyHour();
        int currentHour = todayCalendar.get(Calendar.HOUR_OF_DAY);
        if (requiredSyncHour != currentHour) {
            // Not correct time, so skip.
            if (isFiner()) {
                logFiner("Skipping daily scan (not correct time)");
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
                    if (isFiner()) {
                        logFiner("Skipping daily scan (not weekday)");
                    }
                    return false;
                }
            } else if (requiredSyncDay == SyncProfileConfiguration.DAILY_DAY_WEEKENDS)
            {
                if (currentDay != Calendar.SATURDAY
                    && currentDay != Calendar.SUNDAY)
                {
                    if (isFiner()) {
                        logFiner("Skipping daily scan (not weekend)");
                    }
                    return false;
                }
            } else {
                if (currentDay != requiredSyncDay) {
                    if (isFiner()) {
                        logFiner("Skipping daily scan (not correct day)");
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
        if (fInfo == null) {
            throw new NullPointerException("File is null");
        }
        if (isFiner()) {
            logFiner("Scanning file: " + fInfo + ", folderId: " + fInfo);
        }
        File file = getDiskFile(fInfo);

        if (!file.canRead()) {
            // ignore not readable
            logWarning("File not readable: " + file);
            return false;
        }

        // ignore our database file
        if (file.getName().equals(DB_FILENAME)
            || file.getName().equals(DB_BACKUP_FILENAME))
        {
            if (!file.isHidden()) {
                FileUtils.makeHiddenOnWindows(file);
            }
            logFiner("Ignoring folder database file: " + file);
            return false;
        }

        // check for incomplete download files and delete them, if
        // the real file exists
        if (FileUtils.isTempDownloadFile(file)) {
            if (FileUtils.isCompletedTempDownloadFile(file)
                || fInfo.isDeleted())
            {
                logFiner("Removing temp download file: " + file);
                if (!file.delete()) {
                    logSevere("Failed to remove temp download file: " + file);
                }
            } else {
                logFiner("Ignoring incomplete download file: " + file);
            }
            return false;
        }

        // remove placeholder file if exist for this file
        // Commented, no useful until full implementation of placeholder
        // file
        /*
         * if (file.exists()) { File placeHolderFile =
         * Util.getPlaceHolderFile(file); if (placeHolderFile.exists()) {
         * logFiner("Removing placeholder file for: " + file);
         * placeHolderFile.delete(); } }
         */
        if (PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController())) {
            checkFileName(fInfo);
        }

        // First relink modified by memberinfo to
        // actual instance if available on nodemanager
        synchronized (scanLock) {
            synchronized (knownFiles) {
                // link new file to our folder
                fInfo.setFolderInfo(this.currentInfo);
                if (!isKnown(fInfo)) {
                    if (isFiner()) {
                        logFiner(
                            fInfo + ", modified by: " + fInfo.getModifiedBy());
                    }
                    // Update last - modified data
                    MemberInfo modifiedBy = fInfo.getModifiedBy();
                    if (modifiedBy == null) {
                        modifiedBy = getController().getMySelf().getInfo();
                    }
                    Member from = modifiedBy.getNode(getController(), true);
                    if (from != null) {
                        fInfo.setModifiedInfo(from.getInfo(), fInfo
                            .getModifiedDate());
                    }

                    if (file.exists()) {
                        fInfo.setModifiedInfo(modifiedBy, new Date(file
                            .lastModified()));
                        fInfo.setSize(file.length());
                    }

                    addFile(fInfo);

                    // update directory
                    // don't do this in the server version
                    if (rootDirectory != null) {
                        rootDirectory.add(getController().getMySelf(), fInfo);
                    }

                    // get folder icon info and set it
                    if (FileUtils.isDesktopIni(file)) {
                        makeFolderIcon(file);
                    }

                    // Fire folder change event
                    // fireEvent(new FolderChanged());

                    if (isFiner()) {
                        logFiner(
                            this.toString() + ": Local file scanned: "
                                + fInfo.toDetailString());
                    }
                    return true;
                }

                // Now process/check existing files
                FileInfo dbFile = getFile(fInfo);
                dbFile.syncFromDiskIfRequired(getController(), file);
                if (isFiner()) {
                    logFiner("File already known: " + fInfo);
                }
                return true;
            }
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
            logWarning("path maybe to long: " + fileInfo);
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
                    getController(), title, message,
                    new String[]{Translation.getTranslation("general.ok")}, 0,
                    GenericDialogType.INFO, neverShowAgainText);
                if (response.isNeverAskAgain()) {
                    PreferencesEntry.FILE_NAME_CHECK.setValue(getController(),
                        false);
                    logWarning("store do not show this dialog again");
                }
            } else {
                logWarning("Path maybe to long for folder: " + getName()
                    + " File:" + fileInfo.getName());
            }
        }
        // check length of path elements and filename
        // TODO: The check if 30 chars or more cuurrently disabled
        // Should only check if on mac system
        // Jan: No longer needed since there is no java 1.5 for mac classic!

        // String[] parts = totalName.split("\\/");
        // for (String pathPart : parts) {
        // if (pathPart.length() > 30) {
        // logWarning("filename or dir maybe to long: " + fileInfo);
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
        // logWarning("store do not show this dialog again");
        // }
        // } else {
        // logWarning(
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
        fInfo.setFolderInfo(currentInfo);

        FileInfo old = knownFiles.put(fInfo, fInfo);

        TransferPriority prio = TransferPriority.NORMAL;

        if (old != null) {
            prio = transferPriorities.getPriority(old);
            // Remove old file from info
            currentInfo.removeFile(old);
        }

        // logWarning("Adding " + fInfo.getClass() + ", removing " + old);

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
        if (isFiner()) {
            logFiner("Remove file local: " + fInfo + ", Folder equal ? "
                + Util.equals(fInfo.getFolderInfo(), currentInfo));
        }
        if (!isKnown(fInfo)) {
            if (isWarning()) {
                logWarning("Tried to remove a not-known file: "
                    + fInfo.toDetailString());
            }
            return false;
        }

        // Abort transfers files
        getController().getTransferManager().breakTransfers(fInfo);

        File diskFile = getDiskFile(fInfo);
        boolean folderChanged = false;
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
            FolderFilesChanged changes = new FolderFilesChanged(currentInfo);
            changes.removed = removedFiles.toArray(new FileInfo[removedFiles
                .size()]);
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
    @SuppressWarnings("unchecked")
    private boolean loadFolderDB(File dbFile) {
        synchronized (scanLock) {
            if (!dbFile.exists()) {
                logFine(this + ": Database file not found: "
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
                synchronized (knownFiles) {
                    knownFiles.clear();
                    for (FileInfo fileInfo : files) {
                        if (fileInfo.getName().contains(
                            Constants.POWERFOLDER_SYSTEM_SUBDIR))
                        {
                            // Skip #1411
                            continue;
                        }
                        // scanFile(fileInfo);
                        addFile(fileInfo);
                    }
                }

                // read them always ..
                MemberInfo[] members1 = (MemberInfo[]) in.readObject();
                // Do not load members
                logFiner("Loading " + members1.length + " members");
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
                        if (isFiner()) {
                            logFiner("ignore@" + info.getName());
                        }
                    }
                } catch (java.io.EOFException e) {
                    logFine("ignore nothing for " + this);
                } catch (Exception e) {
                    logSevere("read ignore error: " + this + e.getMessage(), e);
                }

                try {
                    Object object = in.readObject();
                    if (object instanceof Date) {
                        lastScan = (Date) object;
                        if (isFiner()) {
                            logFiner("lastScan" + lastScan);
                        }
                    }
                } catch (java.io.EOFException e) {
                    // ignore nothing available for ignore
                    logFine("ignore nothing for " + this);
                } catch (Exception e) {
                    logSevere("read ignore error: " + this + e.getMessage(), e);
                }
                in.close();
                fIn.close();

                logFine("Loaded folder database (" + files.length
                    + " files) from " + dbFile.getAbsolutePath());
            } catch (IOException e) {
                logWarning(this + ": Unable to read database file: "
                    + dbFile.getAbsolutePath());
                logFiner(e);
                return false;
            } catch (ClassNotFoundException e) {
                logWarning(this + ": Unable to read database file: "
                    + dbFile.getAbsolutePath());
                logFiner(e);
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
            Constants.POWERFOLDER_SYSTEM_SUBDIR + '/' + DB_FILENAME)))
        {
            return;
        }

        if (loadFolderDB(new File(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR + '/' + DB_BACKUP_FILENAME)))
        {
            return;
        }

        logFine("Unable to read folder db, even from backup. Maybe new folder?");
    }

    /**
     * Shuts down the folder
     */
    public void shutdown() {
        logFine("shutting down folder " + this);

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

        File dbFile = new File(getSystemSubDir(), DB_FILENAME);
        File dbFileBackup = new File(getSystemSubDir(), DB_BACKUP_FILENAME);
        try {
            FileInfo[] files;
            synchronized (knownFiles) {
                files = knownFiles.keySet().toArray(new FileInfo[knownFiles.keySet().size()]);
            }
            if (dbFile.exists()) {
                if (!dbFile.delete()) {
                    logSevere("Failed to delete database file: " + dbFile);
                }
            }
            if (!dbFile.createNewFile()) {
                logSevere("Failed to create database file: " + dbFile);
            }
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
                dbFile));
            ObjectOutputStream oOut = new ObjectOutputStream(fOut);
            // Store files
            oOut.writeObject(files);
            // Store members
            oOut.writeObject(Convert.asMemberInfos(getMembersAsCollection()
                .toArray(new Member[getMembersAsCollection().size()])));
            // Old blacklist. Maintained for backward serialization
            // compatability. Do not remove.
            oOut.writeObject(new ArrayList<FileInfo>());
            if (lastScan == null) {
                if (isFiner()) {
                    logFiner("write default time: " + new Date());
                }
                oOut.writeObject(new Date());
            } else {
                if (isFiner()) {
                    logFiner("write time: " + lastScan);
                }
                oOut.writeObject(lastScan);
            }

            oOut.close();
            fOut.close();

            if (isFine()) {
                logFine("Successfully wrote folder database file ("
                    + files.length + " files)");
            }

            // Make backup
            FileUtils.copyFile(dbFile, dbFileBackup);

            // TODO Remove this in later version
            // Cleanup for older versions
            File oldDbFile = new File(localBase, DB_FILENAME);
            if (!oldDbFile.delete()) {
                logFiner("Failed to delete 'old' database file: " + oldDbFile);
            }
            File oldDbFileBackup = new File(localBase, DB_BACKUP_FILENAME);
            if (!oldDbFileBackup.delete()) {
                logFiner("Failed to delete backup of 'old' database file: "
                    + oldDbFileBackup);
            }
        } catch (IOException e) {
            // TODO: if something failed shoudn't we try to restore the
            // backup (if backup exists and bd file not after this?
            logSevere(this + ": Unable to write database file "
                + dbFile.getAbsolutePath(), e);
            logFiner(e);
        }
    }

    private boolean maintainFolderDBrequired() {
        if (knownFiles.isEmpty()) {
            return false;
        }
        if (lastDBMaintenance == null) {
            return true;
        }
        return lastDBMaintenance.getTime()
            + ConfigurationEntry.DB_MAINTENANCE_SECONDS
                .getValueInt(getController()) * 1000L < System
            .currentTimeMillis();
    }

    /**
     * Creates a list of fileinfos of deleted files that are old than the
     * configured max age. These files do not get written to the DB. So files
     * deleted long ago do not stay in DB for ever.
     */
    private void maintainFolderDB() {
        RecycleBin recycleBin = getController().getRecycleBin();

        long removeBeforeDate = System.currentTimeMillis()
            - 1000L
            * ConfigurationEntry.MAX_FILEINFO_DELETED_AGE_SECONDS
                .getValueInt(getController());
        int nFilesBefore = knownFiles.size();
        if (isFiner()) {
            logFiner("Maintaining folder db, known files: " + nFilesBefore
                + ". Expiring deleted files older than "
                + new Date(removeBeforeDate));
        }
        int expired = 0;
        for (FileInfo file : knownFiles.values()) {
            if (!file.isDeleted()) {
                continue;
            }
            if (file.getModifiedDate().getTime() < removeBeforeDate) {
                if (recycleBin.isInRecycleBin(file)) {
                    // Skip candidate
                    continue;
                }
                expired++;
                knownFiles.remove(file);

                if (rootDirectory != null) {
                    rootDirectory.removeFileInfo(file);
                }
                for (Member member : members.values()) {
                    member.removeFileInfo(file);
                }
                logFine("FileInfo expired: " + file.toDetailString());
            }
        }
        logInfo("Maintained folder db, " + nFilesBefore + " known files, "
            + expired
            + " expired FileInfos. Expiring deleted files older than "
            + new Date(removeBeforeDate));
        if (expired > 0) {
            dirty = true;
        }
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
            logFiner("Not a windows system, ignoring folder icon. "
                + desktopIni.getAbsolutePath());
            return;
        }

        logFiner("Setting icon of "
            + desktopIni.getParentFile().getAbsolutePath());

        FileUtils.setAttributesOnWindows(desktopIni, true, true);
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
            shortCutName = '[' + getController().getMySelf().getNick() + "] "
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
            shortCutName = '[' + getController().getMySelf().getNick() + "] "
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
        Reject.ifNull(aSyncProfile, "Unable to set null sync profile");
        if (previewOnly) {
            throw new IllegalStateException(
                "Can not set Sync Profile in Preview mode.");
        }
        if (aSyncProfile.equals(syncProfile)) {
            // Not changed
            return;
        }

        logFine("Setting " + aSyncProfile.getProfileName());
        syncProfile = aSyncProfile;

        // Store on disk
        String md5 =
                new String(Util.encodeHex(Util.md5(currentInfo.id.getBytes())));
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + md5
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
                .triggerFileRequesting(currentInfo);
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
        if (!syncProfile.isAutoDetectLocalChanges()
            || syncProfile.getConfiguration().isDailySync())
        {
            return;
        }
        if (isFiner()) {
            logFiner("recommendScanOnNextMaintenance");
        }
        scanForced = true;
        lastScan = null;
    }

    /**
     * Runs the maintenance on this folder. This means the folder gets synced
     * with remotesides.
     */
    public void maintain() {
        logFiner("Maintaining '" + getName() + '\'');

        // local files
        logFiner("Forced: " + scanForced);
        boolean forcedNow = scanForced;
        scanForced = false;
        if (forcedNow || autoScanRequired()) {
            scanLocalFiles();
        }
        if (maintainFolderDBrequired()) {
            maintainFolderDB();
        }
    }

    /**
     * @return true if this folder requires the maintenance to be run.
     */
    public boolean isMaintenanceRequired() {
        return scanForced || autoScanRequired() || maintainFolderDBrequired();
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
        if (join0(member)) {
            // Fire event if this member is new
            fireMemberJoined(member);
        }
    }

    /**
     * Joins a member to the folder. Does not fire the event
     * 
     * @param member
     * @return true if this member is new to the folder. false if he was already
     *         a member.
     */
    private boolean join0(Member member) {
        Reject.ifNull(member, "Member is null, unable to join");
        // member will be joined, here on local
        boolean wasMember = members.put(member, member) != null;
        if (isFiner()) {
            logFiner("Member joined " + member);
        }
        // send him our list of files if completely connected. otherwise this
        // gets sent by Member.completeHandshake();
        if (!wasMember && member.isCompleteyConnected()) {
            member.sendMessagesAsynchron(FileList.createFileListMessages(this));
        }
        return !wasMember;
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
        logFine("Member left " + member);

        // remove files of this member in our datastructure
        member.removeFileList(currentInfo);
        if (rootDirectory != null) {
            rootDirectory.removeFilesOfMember(member);
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
        if (isFiner()) {
            logFiner(
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

            if (isFiner()) {
                logFiner("RemoteFileDeletion sync. Member '" + member.getNick()
                    + "' has " + fileList.size() + " possible files");
            }
            for (FileInfo remoteFile : fileList) {
                if (!remoteFile.isDeleted()) {
                    // Not interesting...
                    continue;
                }
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
                if (localFile != null && !remoteFile.isNewerThan(localFile)) {
                    // Local file is newer
                    continue;
                }

                // Add to local file to database if was deleted on remote
                if (localFile == null) {
                    addFile(remoteFile);
                    localFile = getFile(remoteFile);
                    // File has been marked as removed at our side
                    removedFiles.add(localFile);
                }
                if (localFile.isDeleted()) {
                    continue;
                }
                File localCopy = localFile.getDiskFile(getController()
                    .getFolderRepository());
                if (!localFile.inSyncWithDisk(localCopy)) {
                    logFine(
                        "Not deleting file from member " + member
                            + ", local file not in sync with disk: "
                            + localFile.toDetailString() + " at "
                            + localCopy.getAbsolutePath());
                    recommendScanOnNextMaintenance();
                    continue;
                }

                if (isFine()) {
                    logFine(
                        "File was deleted by " + member + ", deleting local: "
                            + localFile.toDetailString() + " at "
                            + localCopy.getAbsolutePath());
                }

                // Abort transfers on file.
                getController().getTransferManager().breakTransfers(localFile);

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

        // Broadcast folder change if changes happend
        if (!removedFiles.isEmpty()) {
            fireFilesDeleted(removedFiles);
            setDBDirty();

            // Broadcast to members
            diskItemFilter.filterFileInfos(removedFiles);
            FolderFilesChanged changes = new FolderFilesChanged(currentInfo);
            changes.removed = removedFiles.toArray(new FileInfo[removedFiles
                .size()]);
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
        if (isFiner()) {
            logFiner("Broadcasting remote scan commando");
        }
        Message scanCommand = new ScanCommand(currentInfo);
        broadcastMessages(scanCommand);
    }

    private void broadcastFolderChanges(ScanResult scanResult) {
        if (getConnectedMembersCount() == 0) {
            return;
        }
        int addedMsgs = 0;
        if (!scanResult.getNewFiles().isEmpty()) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(currentInfo, scanResult
                    .getNewFiles(), diskItemFilter, true);
            if (msgs != null) {
                addedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        int changedMsgs = 0;
        if (!scanResult.getChangedFiles().isEmpty()) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(currentInfo, scanResult
                    .getChangedFiles(), diskItemFilter, true);
            if (msgs != null) {
                changedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        int deletedMsgs = 0;
        if (!scanResult.getDeletedFiles().isEmpty()) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(currentInfo, scanResult
                    .getDeletedFiles(), diskItemFilter, false);
            if (msgs != null) {
                deletedMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        int restoredMsgs = 0;
        if (!scanResult.getRestoredFiles().isEmpty()) {
            Message[] msgs = FolderFilesChanged
                .createFolderFilesChangedMessages(currentInfo, scanResult
                    .getRestoredFiles(), diskItemFilter, true);
            if (msgs != null) {
                restoredMsgs += msgs.length;
                broadcastMessages(msgs);
            }
        }
        if (isFine()) {
            logFine("Broadcasted folder changes " + addedMsgs + " addedmsgs, "
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
        // logFine(
        // "New Filelist received from " + from + " #files: "
        // + newList.files.length);

        // Try to find same files
        findSameFiles(from, Arrays.asList(newList.files));

        // don't do this in the server version
        if (rootDirectory != null) {
            rootDirectory.addAll(from, newList.files);
        }

        if (syncProfile.isAutodownload() && from.isCompleteyConnected()) {
            // Trigger file requestor
            if (isFiner()) {
                logFiner("Triggering file requestor because of new remote file list from "
                    + from);
            }
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(newList.folder);
        }

        // Handle remote deleted files
        if (syncProfile.isSyncDeletion()) {
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
    public void fileListChanged(Member from,
        FolderFilesChanged changes)
    {
        // logFine("File changes received from " + from);

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
                rootDirectory.addAll(from, changes.added);
            }
            if (changes.removed != null) {
                rootDirectory.addAll(from, changes.removed);
            }
        }

        // Avoid hammering of sync remote deletion
        boolean singleFileMsg = changes.added != null
            && changes.added.length == 1 && changes.removed == null;

        if (syncProfile.isAutodownload()) {
            // Check if we need to trigger the filerequestor
            boolean triggerFileRequestor = from.isCompleteyConnected();
            if (triggerFileRequestor && singleFileMsg) {
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
                if (isFiner()) {
                    logFiner("Triggering file requestor because of remote file list change "
                        + changes + " from " + from);
                }
                getController().getFolderRepository().getFileRequestor()
                    .triggerFileRequesting(changes.folder);
            } else if (isFiner()) {
                logFiner("Not triggering filerequestor, no new files in remote filelist"
                    + changes + " from " + from);
            }
        }

        // Handle remote deleted files
        if (!singleFileMsg && syncProfile.isSyncDeletion()) {
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
        if (isFiner()) {
            logFiner("Triing to find same files in remote list with "
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
                // boolean localFileNewer = Util.isNewerFileDateCrossPlattform(
                // localFileInfo.getModifiedDate(), remoteFileInfo
                // .getModifiedDate());
                if (fileSizeSame && dateSame) {
                    if (isFine()) {
                        logFine("Found identical file remotely: local "
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
                // Disabled because of TRAC #999. Causes strange behavior.
                // if (localFileNewer) {
                // if (isWarning()) {
                // logWarning(
                // "Found file remotely, but local is newer: local "
                // + localFileInfo.toDetailString() + " remote: "
                // + remoteFileInfo.toDetailString()
                // + ". Increasing local version to "
                // + (remoteFileInfo.getVersion() + 1));
                // }
                // localFileInfo.setVersion(remoteFileInfo.getVersion() + 1);
                // // FIXME That might produce a LOT of traffic! Single update
                // // message per file! This also might intefere with FileList
                // // exchange at beginning of communication
                // fileChanged(localFileInfo);
                // }
            }
        }

        // logWarning("Canidates files: " + problemCanidates);

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

        if (isWarning() && !problemFiles.isEmpty()) {
            logWarning("Got " + problemFiles.size() + " problematic files");
        }

        FileNameProblemHandler handler = getController().getFolderRepository()
            .getFileNameProblemHandler();
        // logWarning("Problem handler: " + handler);
        if (handler != null && !problemFiles.isEmpty()) {
            handler.fileNameProblemsDetected(new FileNameProblemEvent(this,
                problemFiles));
        }

        // logWarning("Handled problematic files: " + problemFiles);
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
                .getLastFileListAsCollection(currentInfo);
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
        logFiner("Persisting settings");

        storeFolderDB();

        // Write filelist
        if (LoggingManager.isLogToFile()
            && Feature.DEBUG_WRITE_FILELIST_CSV.isEnabled())
        {
            writeFilelist();

            // And members' filelists.
            for (Member member : members.keySet()) {
                if (!member.isMySelf()) {
                    Collection<FileInfo> memberFiles = getFilesAsCollection(member);
                    if (memberFiles != null) {
                        Debug.writeFileListCSV(getName(), member.getNick(),
                            memberFiles, "FileList of folder " + getName()
                                + ", member " + member.getNick() + ':');
                    }
                }
            }
        }
        dirty = false;
    }

    private void writeFilelist() {
        // Write filelist to disk
        Debug.writeFileListCSV(getName(),
            getController().getMySelf().getNick(), knownFiles.keySet(),
            "FileList of folder " + getName() + ", member " + this + ':');
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
                logSevere("Failed to create system subdir: " + systemSubDir);
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
        return knownFiles.keySet().toArray(
            new FileInfo[knownFiles.keySet().size()]);
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
     * @return Directory with all sub dirs and files set
     */
    public Directory getDirectory() {
        if (rootDirectory != null) {
            return rootDirectory;
        }
        synchronized (rootDirectoryInitLock) {
            if (rootDirectory == null) {
                rootDirectory = Directory.buildDirsRecursive(getController()
                    .getNodeManager().getMySelf(), knownFiles.values(), this);
            }
        }
        return rootDirectory;
    }

    /**
     * Common file delete method. Either deletes the file or moves it to the
     * recycle bin.
     * 
     * @param fileInfo
     * @param file
     */
    private void deleteFile(FileInfo newFileInfo, File file) {
        FileInfo fileInfo = getFile(newFileInfo);
        if (useRecycleBin) {
            if (isFine()) {
                logFine("Deleting file " + fileInfo.toDetailString()
                    + " moving to recycle bin");
            }
            RecycleBin recycleBin = getController().getRecycleBin();
            if (!recycleBin.moveToRecycleBin(fileInfo, file)) {
                logSevere("Unable to move file to recycle bin" + file);
                if (!file.delete()) {
                    logSevere("Unable to delete file " + file);
                }
            }
        } else {
            if (isFine()) {
                logFine("Deleting file " + fileInfo.toDetailString()
                    + " NOT moving to recycle bin (disabled)");
            }
            if (!file.delete()) {
                logSevere("Unable to delete file " + file);
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
        return getIncomingFiles(includeNonFriendFiles, true);
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @param includeNonFriendFiles
     *            if files should be included, that are modified by non-friends
     * @param includeDeleted
     *            true if also deleted files should be considered.
     * @return the list of files that are incoming/newer available on remote
     *         side as unmodifiable collection.
     */
    public Collection<FileInfo> getIncomingFiles(boolean includeNonFriendFiles,
        boolean includeDeleted)
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
            if (!member.hasCompleteFileListFor(currentInfo)) {
                if (isFine()) {
                    logFine("Skipping " + member
                        + " no complete filelist from him");
                }
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
                if (remoteFile.isDeleted() && !includeDeleted) {
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
            logFiner("No Incoming files");
        } else {
            logFine("Incoming files " + incomingFiles.size());
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
            .getLastFileListAsCollection(currentInfo);
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
        return connected.toArray(new Member[connected.size()]);
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
        return new File(localBase, fInfo.getName());
    }

    /**
     * @return true if members are there, were files can be downloaded from.
     *         Remote nodes have to have free upload capacity.
     */
    public boolean hasUploadCapacity() {
        for (Member member : members.values()) {
            if (getController().getTransferManager().hasUploadCapacity(member))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the globally unique folder ID, generate once at folder creation
     */
    public String getId() {
        return currentInfo.id;
    }

    /**
     * @return Date of the last file change for this folder.
     */
    public Date getLastFileChangeDate() {
        return statistic.getLastFileChangeDate();
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
        inv.setSuggestedLocalBase(getController(), localBase);
        return inv;
    }

    public String toString() {
        return currentInfo.toString();
    }

    // Logger methods *********************************************************

    @Override
    public String getLoggerName() {
        return super.getLoggerName() + " '" + getName() + '\'';
    }

    /**
     * Ensures that default ignore patterns are set.
     */
    void addDefaultExcludes() {
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
        if (isFiner()) {
            logFiner("fireFileChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, fileInfo);
        folderListenerSupport.fileChanged(folderEvent);
    }

    private void fireFilesDeleted(Collection<FileInfo> fileInfos) {
        if (isFiner()) {
            logFiner("fireFilesDeleted: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, fileInfos);
        folderListenerSupport.filesDeleted(folderEvent);
    }

    private void fireRemoteContentsChanged(FileList list) {
        if (isFiner()) {
            logFiner("fireRemoteContentsChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, list);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    private void fireRemoteContentsChanged(FolderFilesChanged list) {
        if (isFiner()) {
            logFiner("fireRemoteContentsChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, list);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    private void fireSyncProfileChanged() {
        FolderEvent folderEvent = new FolderEvent(this, syncProfile);
        folderListenerSupport.syncProfileChanged(folderEvent);
    }

    private void fireScanResultCommited(ScanResult scanResult) {
        if (isFiner()) {
            logFiner("fireScanResultCommited: " + this);
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