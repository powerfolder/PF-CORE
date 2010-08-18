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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.problem.DeviceDisconnectedProblem;
import de.dal33t.powerfolder.disk.problem.FilenameProblemHelper;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.disk.problem.UnsynchronizedFolderProblem;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.LocalMassDeletionEvent;
import de.dal33t.powerfolder.event.RemoteMassDeletionEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.ScanCommand;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.transfer.TransferPriorities;
import de.dal33t.powerfolder.transfer.TransferPriorities.TransferPriority;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Visitor;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.pattern.Pattern;

/**
 * The main class representing a folder. Scans for new files automatically.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Folder extends PFComponent {
    public static final String DB_FILENAME = ".PowerFolder.db";
    public static final String DB_BACKUP_FILENAME = ".PowerFolder.db.bak";
    private static final String LAST_SYNC_INFO_FILENAME = "Last_sync";

    public static final int TEN_MINUTES = 60 * 10;
    public static final int ONE_MINUTE = 60;

    /** The base location of the folder. */
    private final File localBase;

    /**
     * #2056: The directory to commit/mirror the whole folder to when in reaches
     * 100% sync.
     */
    private File commitDir;

    /**
     * TRAC #1422: The DAO to store the FileInfos in.
     */
    private FileInfoDAO dao;

    /**
     * Date of the last directory scan
     */
    private Date lastScan;

    /**
     * Date of the folder last went to 100% synchronized with another member(s).
     */
    private Date lastSyncDate;

    /**
     * The result state of the last scan
     */
    private ScanResult.ResultState lastScanResultState;

    /**
     * The last time the db was cleaned up.
     */
    private Date lastDBMaintenance;

    /**
     * Access lock to the DB/DAO.
     */
    private final Object dbAccessLock = new Object();

    /** files that should(not) be downloaded in auto download */
    private final DiskItemFilter diskItemFilter;

    /**
     * Stores the priorities for downloading of the files in this folder.
     */
    private final TransferPriorities transferPriorities;

    /**
     * TODO THIS IS A HACK. Make it finer. Lock for scan / accessing the actual
     * files
     */
    private final Object scanLock = new Object();

    /** All members of this folder. Key == Value. Use Map for concurrency. */
    private final Map<Member, Member> members;

    /**
     * the folder info, contains important information about
     * id/hash/name/filescount
     */
    private final FolderInfo currentInfo;

    /**
     * Folders sync profile Always access using getSyncProfile (#76 - preview
     * mode)
     */
    private SyncProfile syncProfile;

    /**
     * Flag indicating that folder has a set of own know files will be true
     * after first scan ever
     */
    private volatile boolean hasOwnDatabase;

    /** Flag indicating */
    private volatile boolean shutdown;

    /**
     * Indicates, that the scan of the local filesystem was forced
     */
    private boolean scanForced;

    /**
     * Flag indicating that the setting (e.g. folder db) have changed but not
     * been persisted.
     */
    private volatile boolean dirty;

    /**
     * The FileInfos that have problems inlcuding the desciptions of the
     * problems. DISABLED
     */
    // private Map<FileInfo, List<FilenameProblem>> problemFiles;
    /** The statistic for this folder */
    private final FolderStatistic statistic;

    private volatile FileArchiver archiver;

    /**
     * TRAC #711: Automatic change detection by watching the filesystem.
     */
    private FolderWatcher watcher;

    private final FolderListener folderListenerSupport;
    private final FolderMembershipListener folderMembershipListenerSupport;
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
     * #1538: Script that gets executed after a download has been completed
     * successfully.
     */
    private String downloadScript;

    private final ProblemListener problemListenerSupport;

    private final List<Problem> problems;

    /** True if patterns should be synchronized with others */
    private boolean syncPatterns;

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

        Reject.ifNull(folderSettings.getSyncProfile(), "Sync profile is null");

        currentInfo = new FolderInfo(fInfo.name, fInfo.id).intern();

        // Create listener support
        folderListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderListener.class);
        folderMembershipListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderMembershipListener.class);
        problemListenerSupport = ListenerSupportFactory
            .createListenerSupport(ProblemListener.class);

        // Not until first scan or db load
        hasOwnDatabase = false;
        dirty = false;
        problems = new CopyOnWriteArrayList<Problem>();
        localBase = folderSettings.getLocalBaseDir();
        commitDir = folderSettings.getCommitDir();
        syncProfile = folderSettings.getSyncProfile();
        downloadScript = folderSettings.getDownloadScript();
        syncPatterns = folderSettings.isSyncPatterns();
        previewOnly = folderSettings.isPreviewOnly();

        // Check base dir
        try {
            checkBaseDir(localBase, false);
            logFine("Opened " + toString() + " at '"
                + localBase.getAbsolutePath() + '\'');
        } catch (FolderException e) {
            logWarning("Unable to open " + toString() + " at '"
                + localBase.getAbsolutePath()
                + "'. Local base directory is inaccessable.");
            deviceDisconnected = true;
        }

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
        }

        transferPriorities = new TransferPriorities();

        diskItemFilter = new DiskItemFilter();

        // Initialize the DAO
        initFileInfoDAO();
        checkIfDeviceDisconnected();

        members = new ConcurrentHashMap<Member, Member>();

        // Load folder database, ignore patterns and other metadata stuff.
        loadMetadata();

        // put myself in membership
        join0(controller.getMySelf());

        // Now calc.
        statistic = new FolderStatistic(this);

        // Check desktop ini in Windows environments
        FileUtils.maintainDesktopIni(getController(), localBase);
        // #2047 Remove later after 4.3.0
        FileUtils.setAttributesOnWindows(localBase, null, false);

        // Force the next time scan.
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
            1000L,
            1000L * ConfigurationEntry.FOLDER_DB_PERSIST_TIME
                .getValueInt(getController()));

        if (folderSettings.getArchiveMode() != null) {
            archiver = folderSettings.getArchiveMode().getInstance(this);
        } else {
            archiver = ArchiveMode.NO_BACKUP.getInstance(this);
        }
        archiver.setVersionsPerFile(folderSettings.getVersions());

        // Create invitation
        if (folderSettings.isCreateInvitationFile()) {
            try {
                Invitation inv = createInvitation();
                File invFile = new File(folderSettings.getLocalBaseDir(),
                    FileUtils.removeInvalidFilenameChars(inv.folder.name)
                        + ".invitation");
                InvitationUtil.save(inv, invFile);
                scanChangedFile(FileInfoFactory.lookupInstance(this, invFile));
            } catch (Exception e) {
                // Failure to send invite is not fatal to folder create.
                // Log it and move on.
                logInfo(e);
            }
        }

        watcher = new FolderWatcher(this);
    }

    public void addProblemListener(ProblemListener l) {
        Reject.ifNull(l, "Listener is null");
        ListenerSupportFactory.addListener(problemListenerSupport, l);
    }

    public void removeProblemListener(ProblemListener l) {
        Reject.ifNull(l, "Listener is null");
        ListenerSupportFactory.removeListener(problemListenerSupport, l);
    }

    /**
     * Add a problem to the list of problems.
     * 
     * @param problem
     */
    public void addProblem(Problem problem) {
        problems.add(problem);
        problemListenerSupport.problemAdded(problem);
        logFiner("Added problem");
    }

    /**
     * Remove a problem from the list of known problems.
     * 
     * @param problem
     */
    public void removeProblem(Problem problem) {
        boolean removed = problems.remove(problem);
        if (removed) {
            problemListenerSupport.problemRemoved(problem);
        } else {
            logWarning("Failed to remove problem");
        }
    }

    /**
     * Remove all problems from the list of known problems.
     */
    public void removeAllProblems() {
        List<Problem> list = new ArrayList<Problem>();
        list.addAll(problems);
        problems.clear();
        for (Problem problem : list) {
            problemListenerSupport.problemRemoved(problem);
        }
    }

    /**
     * @return Count problems in folder?
     */
    public int countProblems() {
        return problems.size();
    }

    /**
     * @return unmodifyable list of problems.
     */
    public List<Problem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    /**
     * Sets the FileArchiver to be used.
     * 
     * @param mode
     *            the ArchiveMode
     */
    public void setArchiveMode(ArchiveMode mode) {
        try {
            archiver = mode.getInstance(this);
            // Store on disk
            String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
                .getBytes())));
            String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + md5
                + FolderSettings.FOLDER_SETTINGS_ARCHIVE;
            getController().getConfig().put(syncProfKey, mode.name());
            getController().saveConfig();
        } catch (Exception e) {
            logWarning("Unable to set new archive mode: " + mode
                + ". Falling back to no backup archive. " + e);
            logFiner(e);
            archiver = ArchiveMode.NO_BACKUP.getInstance(this);
        }
    }

    public void setArchiveVersions(int versions) {
        archiver.setVersionsPerFile(versions);
        // Store on disk
        String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
            .getBytes())));
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + md5
            + FolderSettings.FOLDER_SETTINGS_VERSIONS;
        getController().getConfig().put(syncProfKey, String.valueOf(versions));
        getController().saveConfig();
    }

    /**
     * @return the FileArchiver used
     */
    public FileArchiver getFileArchiver() {
        return archiver;
    }

    /**
     * @return the watcher of this folder.
     */
    public FolderWatcher getFolderWatcher() {
        return watcher;
    }

    /**
     * Commits the scan results into the internal file database. Changes get
     * broadcasted to other members if necessary.
     * 
     * @param scanResult
     *            the scanresult to commit.
     * @param ignoreLocalMassDeletions
     *            bypass the local mass delete checks.
     */
    private void commitScanResult(ScanResult scanResult,
        boolean ignoreLocalMassDeletions)
    {

        // See if everything has been deleted.
        if (!ignoreLocalMassDeletions
            && getKnownItemCount() > 0
            && !scanResult.getDeletedFiles().isEmpty()
            && scanResult.getTotalFilesCount() == 0
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {

            // Advise controller of the carnage.
            getController().localMassDeletionDetected(
                new LocalMassDeletionEvent(this));

            return;

        }

        synchronized (scanLock) {
            synchronized (dbAccessLock) {
                // new files
                if (isFiner()) {
                    logFiner("Adding " + scanResult.getNewFiles().size()
                        + " to directory");
                }
                // New files
                store(getController().getMySelf(), scanResult.newFiles);
                // deleted files
                store(getController().getMySelf(), scanResult.deletedFiles);
                // restored files
                store(getController().getMySelf(), scanResult.restoredFiles);
                // changed files
                store(getController().getMySelf(), scanResult.changedFiles);
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
        diskItemFilter.addPattern(pattern);
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
                throw new FolderException(currentInfo,
                    "Unable to create folder at " + localBase.getAbsolutePath());
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
     * Local disk/folder management
     */

    /**
     * Scans a downloaded file, renames tempfile to real name moves possible
     * existing file to file archive.
     * 
     * @param fInfo
     * @param tempFile
     * @return true if the download could be completed and the file got scanned.
     *         false if any problem happend.
     */
    public boolean scanDownloadFile(FileInfo fInfo, File tempFile) {
        try {
            watcher.addIgnoreFile(fInfo);
            return scanDownloadFile0(fInfo, tempFile);
        } finally {
            watcher.removeIgnoreFile(fInfo);
        }
    }

    private boolean scanDownloadFile0(FileInfo fInfo, File tempFile) {
        // FIXME What happens if the file was locally modified before the
        // download finished? There should be a check here if the current local
        // version differs from the version when the download began. In that
        // case a conflict has to be raised!

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

        // Prepare last modification date of tempfile.
        if (!tempFile.setLastModified(fInfo.getModifiedDate().getTime())) {
            logSevere("Failed to set modified date on " + tempFile + " for "
                + fInfo.getModifiedDate().getTime());
            return false;
        }

        if (targetFile.exists()) {
            // if file was a "newer file" the file already exists here
            // Using local var because of possible race condition!!
            FileArchiver arch = archiver;
            if (arch != null) {
                try {
                    FileInfo oldLocalFileInfo = fInfo
                        .getLocalFileInfo(getController().getFolderRepository());
                    if (oldLocalFileInfo != null) {
                        arch.archive(oldLocalFileInfo, targetFile, false);
                    }
                } catch (IOException e) {
                    // Same behavior as below, on failure drop out
                    // TODO Maybe raise folder-problem....
                    logWarning("Unable to archive old file!", e);
                    return false;
                }
            }
            if (targetFile.exists() && !targetFile.delete()) {
                logWarning("Unable to scan downloaded file. Was not able to move old file to file archive "
                    + targetFile.getAbsolutePath()
                    + ". "
                    + fInfo.toDetailString());
                return false;
            }
        }
        if (!tempFile.renameTo(targetFile)) {
            logWarning("Was not able to rename tempfile, copiing "
                + tempFile.getAbsolutePath() + " to "
                + targetFile.getAbsolutePath() + ". " + fInfo.toDetailString());

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
            if (!targetFile.setLastModified(fInfo.getModifiedDate().getTime()))
            {
                logSevere("Failed to set modified date on " + targetFile
                    + " to " + fInfo.getModifiedDate().getTime());
                return false;
            }

            if (tempFile.exists() && !tempFile.delete()) {
                logSevere("Unable to remove temp file: " + tempFile);
            }
        }

        synchronized (scanLock) {
            synchronized (dbAccessLock) {
                // Update internal database
                store(getController().getMySelf(), correctFolderInfo(fInfo));
                fileChanged(fInfo);
            }
        }
        return true;
    }

    /**
     * Scans the local directory for new files. Be carefull! This method is not
     * Thread safe. In most cases you want to use
     * recommendScanOnNextMaintenance() followed by maintain().
     * 
     * @return if the local files where scanned
     */
    public boolean scanLocalFiles() {
        return scanLocalFiles(false);
    }

    /**
     * Scans the local directory for new files. Be careful! This method is not
     * Thread safe. In most cases you want to use
     * recommendScanOnNextMaintenance() followed by maintain().
     * 
     * @param ignoreLocalMassDeletion
     *            bypass the local mass delete checks.
     * @return if the local files where scanned
     */
    public boolean scanLocalFiles(boolean ignoreLocalMassDeletion) {
        boolean wasDeviceDisconnected = deviceDisconnected;
        checkIfDeviceDisconnected();

        if (wasDeviceDisconnected && !deviceDisconnected
            && getKnownItemCount() == 0)
        {
            logWarning("Device reconnected. Loading folder database");
            initFileInfoDAO();
            // Try to load db from connected device now.
            loadMetadata();
            // Re-attach folder watcher
            watcher.reconfigure(syncProfile);
        }

        ScanResult result;
        FolderScanner scanner = getController().getFolderRepository()
            .getFolderScanner();
        // Acquire the folder wait
        boolean scannerBusy;
        do {
            synchronized (scanLock) {
                result = scanner.scanFolder(this);
            }
            scannerBusy = ScanResult.ResultState.BUSY == result
                .getResultState();
            if (scannerBusy) {
                logFine("Folder scanner is busy, waiting...");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    logFiner(e);
                    return false;
                }
            }
        } while (scannerBusy);

        if (checkIfDeviceDisconnected()) {
            if (isFiner()) {
                logFiner("Device disconnected while scanning folder: "
                    + localBase);
            }
            return false;
        }

        try {
            if (result.getResultState() == ScanResult.ResultState.SCANNED) {

                // Push any file problems into the Folder's problems.
                Map<FileInfo, List<Problem>> filenameProblems = result
                    .getProblemFiles();
                for (Map.Entry<FileInfo, List<Problem>> fileInfoListEntry : filenameProblems
                    .entrySet())
                {
                    for (Problem problem : fileInfoListEntry.getValue()) {
                        addProblem(problem);
                    }
                }
                commitScanResult(result, ignoreLocalMassDeletion);
                lastScan = new Date();
                return true;
            }
            // scan aborted, hardware broken, mass local delete?
            return false;
        } finally {
            lastScanResultState = result.getResultState();
            checkLastSyncDate();
        }
    }

    /**
     * @return true if a scan in the background is required of the folder
     */
    private boolean autoScanRequired() {
        if (syncProfile.isManualSync()) {
            return false;
        }
        Date wasLastScan = lastScan;
        if (wasLastScan == null) {
            return true;
        }
        if (syncProfile.isInstantSync()) {
            // On supportes systems, scan every ten minutes in case an instant
            // detection got missed. On unsupported systems, scan every minue.
            long secondsSinceLastSync = (System.currentTimeMillis() - wasLastScan
                .getTime()) / 1000;
            int frequency = watcher.isSupported() ? TEN_MINUTES : ONE_MINUTE;
            if (secondsSinceLastSync < frequency) {
                if (isFiner()) {
                    logFiner("Skipping regular scan");
                }
                return false;
            }
        } else if (syncProfile.isDailySync()) {
            if (!shouldDoDailySync()) {
                if (isFiner()) {
                    logFiner("Skipping daily scan");
                }
                return false;
            }
        } else if (syncProfile.isPeriodicSync()) {
            long secondsSinceLastSync = (System.currentTimeMillis() - wasLastScan
                .getTime()) / 1000;
            if (secondsSinceLastSync < syncProfile.getSecondsBetweenScans()) {
                if (isFiner()) {
                    logFiner("Skipping regular scan");
                }
                return false;
            }
        } else {
            logSevere("Do not know what sort of sync to do!!! Folder = "
                + getName() + ", instant = "
                + syncProfile.getConfiguration().isInstantSync() + ", daily = "
                + syncProfile.getConfiguration().isDailySync()
                + ", periodic = "
                + syncProfile.getConfiguration().isDailySync());
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
        if (requiredSyncHour > currentHour) {
            // Not correct time, so skip.
            if (isFiner()) {
                logFiner("Skipping daily scan (not correct time) "
                    + requiredSyncHour + " > " + currentHour);
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
     * Scans a new, deleted or restored File.
     * 
     * @param fileInfo
     *            the file to scan
     * @return the new {@link FileInfo} or null if file was not actually changed
     */
    public FileInfo scanChangedFile(FileInfo fileInfo) {
        Reject.ifNull(fileInfo, "FileInfo is null");
        FileInfo localFileInfo = scanChangedFile0(fileInfo);
        if (localFileInfo != null) {
            fileChanged(localFileInfo);
        }
        return localFileInfo;
    }

    /**
     * Scans multiple new, deleted or restored File callback for
     * {@link #getFolderWatcher()} only.
     * 
     * @param fileInfos
     *            the files to scan. ATTENTION: Does modify the {@link List}
     */
    void scanChangedFiles(List<FileInfo> fileInfos) {
        Reject.ifNull(fileInfos, "FileInfo collection is null");
        int i = 0;
        for (Iterator<FileInfo> it = fileInfos.iterator(); it.hasNext();) {
            FileInfo fileInfo = (FileInfo) it.next();
            FileInfo localFileInfo = scanChangedFile0(fileInfo);
            if (localFileInfo == null) {
                // No change
                it.remove();
            } else {
                fileInfos.set(i, localFileInfo);
                i++;
            }
        }
        if (fileInfos.size() > 0) {
            fireFilesChanged(fileInfos);
            Message[] msgs = FileList.createFileListMessages(currentInfo,
                fileInfos, diskItemFilter, true);
            broadcastMessages(msgs);
            setDBDirty();
        }
    }

    /**
     * Scans one file and updates the internal db if required.
     * <p>
     * 
     * @param fInfo
     *            the file to be scanned
     * @return null, if the file hasn't changed, the new FileInfo otherwise
     */
    private FileInfo scanChangedFile0(FileInfo fInfo) {
        if (isFiner()) {
            logFiner("Scanning file: " + fInfo + ", folderId: " + fInfo);
        }
        File file = getDiskFile(fInfo);

        // ignore our database file
        if (file.getName().equals(DB_FILENAME)
            || file.getName().equals(DB_BACKUP_FILENAME))
        {
            logFiner("Ignoring folder database file: " + file);
            return null;
        }

        checkFileName(fInfo);

        // First relink modified by memberinfo to
        // actual instance if available on nodemanager
        synchronized (scanLock) {
            synchronized (dbAccessLock) {
                FileInfo localFile = getFile(fInfo);
                if (localFile == null) {
                    if (isFiner()) {
                        logFiner("Scan new file: " + fInfo.toDetailString());
                    }
                    // Update last - modified data
                    MemberInfo modifiedBy = fInfo.getModifiedBy();
                    if (modifiedBy == null) {
                        modifiedBy = getController().getMySelf().getInfo();
                    }
                    Member from = modifiedBy.getNode(getController(), true);
                    Date modDate;
                    long size;
                    boolean deleted;

                    if (fInfo.isLookupInstance()) {
                        size = 0;
                        modDate = new Date();
                        deleted = !file.exists();
                    } else {
                        size = fInfo.getSize();
                        modDate = fInfo.getModifiedDate();
                        deleted = fInfo.isDeleted();
                    }

                    if (from != null) {
                        modifiedBy = from.getInfo();
                    }

                    if (file.exists()) {
                        modDate = new Date(file.lastModified());
                        size = file.length();
                    }

                    if (deleted) {
                        fInfo = FileInfoFactory.unmarshallDeletedFile(
                            currentInfo, fInfo.getRelativeName(), modifiedBy,
                            modDate, fInfo.getVersion(), file.isDirectory());
                    } else {
                        fInfo = FileInfoFactory.unmarshallExistingFile(
                            currentInfo, fInfo.getRelativeName(), size,
                            modifiedBy, modDate, fInfo.getVersion(), file
                                .isDirectory());
                    }

                    store(getController().getMySelf(), fInfo);

                    // get folder icon info and set it
                    if (FileUtils.isDesktopIni(file)) {
                        makeFolderIcon(file);
                    }

                    // Fire folder change event
                    // fireEvent(new FolderChanged());

                    if (isFiner()) {
                        logFiner(toString() + ": Local file scanned: "
                            + fInfo.toDetailString());
                    }
                    return fInfo;
                }

                if (isFiner()) {
                    logFiner("Scan known file: " + fInfo.toDetailString());
                }

                FileInfo syncFile = localFile.syncFromDiskIfRequired(
                    getController(), file);
                if (syncFile != null) {
                    store(getController().getMySelf(), syncFile);
                }
                return syncFile;
            }
        }
    }

    /**
     * Creates/Deletes and scans one directory.
     * 
     * @param dirInfo
     *            the dir to be scanned.
     * @param dir
     *            the directory
     */
    public void scanDirectory(FileInfo dirInfo, File dir) {
        Reject.ifNull(dirInfo, "DirInfo is null");
        if (isFiner()) {
            logFiner("Scanning dir: " + dirInfo.toDetailString());
        }

        if (!dirInfo.getFolderInfo().equals(currentInfo)) {
            logSevere("Unable to scan of directory. not on folder: "
                + dirInfo.toDetailString());
            return;
        }

        if (dir.equals(getSystemSubDir())) {
            logWarning("Ignoring system subdirectory: " + dir);
            return;
        }

        watcher.addIgnoreFile(dirInfo);
        try {
            if (dirInfo.isDeleted()) {
                if (!dir.delete()) {
                    logSevere("Unable to deleted directory: " + dir + ". "
                        + dirInfo.toDetailString());
                    return;
                }
            } else {
                dir.mkdirs();
                dir.setLastModified(dirInfo.getModifiedDate().getTime());
            }
        } finally {
            watcher.removeIgnoreFile(dirInfo);
        }

        store(getController().getMySelf(), correctFolderInfo(dirInfo));
        setDBDirty();
    }

    /**
     * Checks a single filename if there are problems with the name
     * 
     * @param fileInfo
     */
    private void checkFileName(FileInfo fileInfo) {
        List<Problem> problemList = FilenameProblemHelper.getProblems(
            getController(), fileInfo);
        for (Problem problem : problemList) {
            addProblem(problem);
        }
    }

    /**
     * Corrects the folder info
     * 
     * @param theFInfo
     */
    private FileInfo correctFolderInfo(FileInfo theFInfo) {
        // Add to this folder
        FileInfo fInfo = FileInfoFactory.changedFolderInfo(theFInfo,
            currentInfo);
        TransferPriority prio = transferPriorities.getPriority(fInfo);
        transferPriorities.setPriority(fInfo, prio);
        return fInfo;
    }

    /**
     * @param fi
     * @return if this file is known to the internal db
     */
    public boolean isKnown(FileInfo fi) {
        return hasFile(fi);
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
        synchronized (scanLock) {
            if (diskFile != null && diskFile.exists()) {
                if (!deleteFile(fInfo, diskFile)) {
                    logWarning("Unable to remove local file. Was not able to move old file to file archive "
                        + diskFile.getAbsolutePath()
                        + ". "
                        + fInfo.toDetailString());
                    // Failure.
                    return false;
                }
                FileInfo localFile = getFile(fInfo);
                FileInfo synced = localFile.syncFromDiskIfRequired(
                    getController(), diskFile);
                folderChanged = synced != null;
                if (folderChanged) {
                    store(getController().getMySelf(), synced);
                }
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
        removeFilesLocal(Arrays.asList(fInfos));
    }

    /**
     * Removes files from the local disk
     * 
     * @param fInfos
     */
    public void removeFilesLocal(Collection<FileInfo> fInfos) {
        if (fInfos == null || fInfos.size() <= 0) {
            throw new IllegalArgumentException("Files to delete are empty");
        }

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        synchronized (scanLock) {
            for (FileInfo fileInfo : fInfos) {
                Reject.ifTrue(fileInfo.isDiretory(),
                    "Directories not supported for deletion yet");
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

    private void initFileInfoDAO() {
        if (dao != null) {
            // Stop old DAO
            dao.stop();
        }
        dao = new FileInfoDAOHashMapImpl(getController().getMySelf().getId(),
            diskItemFilter);

        // File daoDir = new File(getSystemSubDir(), "db/h2");
        // try {
        // FileUtils.recursiveDelete(daoDir.getParentFile());
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // dao = new FileInfoDAOSQLImpl(getController(), "jdbc:h2:" + daoDir,
        // "sa", "", null);
    }

    /**
     * Loads the folder database from disk
     * 
     * @param dbFile
     *            the file to load as db file
     * @return true if succeeded
     */
    // @SuppressWarnings("unchecked")
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
                // Convert.cleanMemberInfos(getController().getNodeManager(),
                // files);
                synchronized (dbAccessLock) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].getRelativeName().contains(
                            Constants.POWERFOLDER_SYSTEM_SUBDIR))
                        {
                            // Skip #1411
                            continue;
                        }
                        FileInfo fInfo = files[i];
                        files[i] = correctFolderInfo(fInfo);
                        if (fInfo != files[i]) {
                            // Instance has changes.
                            setDBDirty();
                        }
                    }
                    dao.store(null, files);
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
                        diskItemFilter.addPattern(info.getRelativeName());
                        if (isFiner()) {
                            logFiner("ignore@" + info.getRelativeName());
                        }
                    }
                } catch (EOFException e) {
                    logFiner("No ignore list");
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
                } catch (EOFException e) {
                    // ignore nothing available for ignore
                    logFine("No last scan date");
                } catch (Exception e) {
                    logSevere("read ignore error: " + this + e.getMessage(), e);
                }

                in.close();
                fIn.close();

                logFine("Loaded folder database (" + files.length
                    + " files) from " + dbFile.getAbsolutePath());
            } catch (Exception e) {
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
     * Loads the metadata information of this folder. Folder database, ignore
     * patterns and last synchronized date.
     */
    private void loadMetadata() {
        loadFolderDB();
        loadLastSyncDate();
        diskItemFilter.loadPatternsFrom(new File(getSystemSubDir(),
            DiskItemFilter.PATTERNS_FILENAME), false);
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
        if (isFine()) {
            logFine("Shutting down folder " + this);
        }
        shutdown = true;
        watcher.remove();
        if (dirty) {
            persist();
        }
        if (diskItemFilter.isDirty() && !checkIfDeviceDisconnected()) {
            diskItemFilter.savePatternsTo(new File(getSystemSubDir(),
                DiskItemFilter.PATTERNS_FILENAME));
            savePatternsToMetaFolder();
        }
        dao.stop();
        removeAllListeners();
        ListenerSupportFactory.removeAllListeners(folderListenerSupport);
        ListenerSupportFactory
            .removeAllListeners(folderMembershipListenerSupport);
        diskItemFilter.removeAllListener();
    }

    /**
     * This is the date that the folder last 100% synced with other members. It
     * may be null if never synchronized externally.
     * 
     * @return the last sync date.
     */
    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    /**
     * Stores the current file-database to disk
     */
    private void storeFolderDB() {
        File dbFile = new File(getSystemSubDir(), DB_FILENAME);
        File dbFileBackup = new File(getSystemSubDir(), DB_BACKUP_FILENAME);
        try {
            FileInfo[] diskItems;
            synchronized (dbAccessLock) {
                Collection<FileInfo> files = dao.findAllFiles(null);
                Collection<DirectoryInfo> dirs = dao.findAllDirectories(null);
                diskItems = new FileInfo[files.size() + dirs.size()];
                int i = 0;
                for (FileInfo fileInfo : files) {
                    diskItems[i] = fileInfo;
                    i++;
                }
                for (DirectoryInfo dirInfo : dirs) {
                    diskItems[i] = dirInfo;
                    i++;
                }
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
            oOut.writeObject(diskItems);
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
                    logFiner("write lastScan: " + lastScan);
                }
                oOut.writeObject(lastScan);
            }

            oOut.close();
            fOut.close();

            if (isFine()) {
                logFine("Successfully wrote folder database file ("
                    + diskItems.length + " disk items)");
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
                + dbFile.getAbsolutePath() + ". " + e);
            logFiner(e);
        }
    }

    private boolean maintainFolderDBrequired() {
        if (getKnownItemCount() == 0) {
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
        long removeBeforeDate = System.currentTimeMillis()
            - 1000L
            * ConfigurationEntry.MAX_FILEINFO_DELETED_AGE_SECONDS
                .getValueInt(getController());
        int nFilesBefore = getKnownItemCount();
        if (isFiner()) {
            logFiner("Maintaining folder db, known files: " + nFilesBefore
                + ". Expiring deleted files older than "
                + new Date(removeBeforeDate));
        }
        int expired = 0;
        for (FileInfo file : dao.findAllFiles(null)) {
            if (!file.isDeleted()) {
                continue;
            }
            if (file.getModifiedDate().getTime() < removeBeforeDate) {
                // FIXME: Check if file is in ARCHIVE?
                expired++;
                // Remove
                dao.delete(null, file);
                for (Member member : members.values()) {
                    dao.delete(member.getId(), file);
                }
                logFine("FileInfo expired: " + file.toDetailString());
            }
        }
        if (expired > 0) {
            setDBDirty();

            logInfo("Maintained folder db, " + nFilesBefore + " known files, "
                + expired
                + " expired FileInfos. Expiring deleted files older than "
                + new Date(removeBeforeDate));
        } else if (isFiner()) {
            logFiner("Maintained folder db, " + nFilesBefore + " known files, "
                + expired
                + " expired FileInfos. Expiring deleted files older than "
                + new Date(removeBeforeDate));
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
     * @return the script to be executed after a successful download or null if
     *         none set.
     */
    public String getDownloadScript() {
        return downloadScript;
    }

    /**
     * @param downloadScript
     *            the new
     */
    public void setDownloadScript(String downloadScript) {
        this.downloadScript = downloadScript;
        // Store on disk
        String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
            .getBytes())));
        String confKey = FOLDER_SETTINGS_PREFIX_V4 + md5
            + FolderSettings.FOLDER_SETTINGS_DOWNLOAD_SCRIPT;
        String confVal = downloadScript != null ? downloadScript : "";
        getController().getConfig().put(confKey, confVal);
        logInfo("Download script set to '" + confVal + '\'');
        getController().saveConfig();
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
     * Sets the synchronisation profile for this folder.
     * 
     * @param aSyncProfile
     */
    public void setSyncProfile(SyncProfile aSyncProfile) {
        Reject.ifNull(aSyncProfile, "Unable to set null sync profile");
        Reject.ifTrue(previewOnly,
            "Can not change Sync Profile in Preview mode.");

        logFine("Setting " + aSyncProfile.getName());
        syncProfile = aSyncProfile;

        // Store on disk
        String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
            .getBytes())));
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + md5
            + FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;
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
            triggerSyncRemoteDeletedFiles(members.keySet(), false);
        }
        watcher.reconfigure(syncProfile);
        recommendScanOnNextMaintenance();
        fireSyncProfileChanged();
    }

    /**
     * Recommends the scan of the local filesystem on the next maintenace run.
     * Useful when files are detected that have been changed.
     * <p>
     * ATTENTION: Does not force a scan if continuous auto-detection is disabled
     * or scheduled sync is setup.
     */
    public void recommendScanOnNextMaintenance() {
        recommendScanOnNextMaintenance(false);
    }

    /**
     * Recommends the scan of the local filesystem on the next maintenace run.
     * Useful when files are detected that have been changed.
     * <p>
     * ATTENTION: Does not force a scan if continuous auto-detection is disabled
     * or scheduled sync is setup unless manual. 'force' should only be true if
     * the user actually requests a scan from the local UI, like clicks the scan
     * button.
     * 
     * @param force
     *            user actually requested scan, override scanAllowedNow
     */
    public void recommendScanOnNextMaintenance(boolean force) {
        if (scanAllowedNow() || force) {
            if (isFiner()) {
                logFiner("recommendScanOnNextMaintenance");
            }
            scanForced = true;
            lastScan = null;
        }
    }

    /**
     * @return true if auto scanning files on-the-fly is allowed now.
     */
    public boolean scanAllowedNow() {
        return (!syncProfile.isManualSync() && !syncProfile.isDailySync())
            && !getController().isSilentMode();
    }

    /**
     * Runs the maintenance on this folder. This means the folder gets synced
     * with remotesides.
     */
    void maintain() {
        if (isFiner()) {
            logFiner("Maintaining '" + getName() + "' (forced? " + scanForced
                + ')');
        }
        // local files

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
     * Member managing methods
     */
    /**
     * Joins a member to the folder,
     * 
     * @param member
     * @return true if actually joined the folder.
     */
    public boolean join(Member member) {
        boolean memberRead = hasReadPermission(member);
        if (!memberRead || !hasReadPermission(getController().getMySelf())) {
            if (memberRead) {
                if (isFine()) {
                    logFine("Not joining " + member + " / "
                        + member.getAccountInfo()
                        + ". Myself got no read permission");
                }
            } else {
                if (isFine()) {
                    logFine("Not joining " + member + " / "
                        + member.getAccountInfo() + " no read permission");
                }
            }
            if (member.isCompletelyConnected()) {
                member.sendMessagesAsynchron(FileList
                    .createNullList(currentInfo));
            }
            return false;
        }
        join0(member);
        return true;
    }

    /**
     * Joins a member to the folder. Does fire the event
     * 
     * @param member
     */
    private void join0(Member member) {
        Reject.ifNull(member, "Member is null, unable to join");
        // member will be joined, here on local
        boolean wasMember = members.put(member, member) != null;
        if (isFiner()) {
            logFiner("Member joined " + member);
        }
        if (!wasMember && member.isCompletelyConnected()) {
            // FIX for #924
            waitForScan();

            Message[] filelistMsgs = FileList
                .createFileListMessages(this, true);
            for (Message message : filelistMsgs) {
                try {
                    member.sendMessage(message);
                } catch (ConnectionException e) {
                    logWarning("Unable to send filelist to " + member + ". "
                        + e);
                    break;
                }
            }
        }
        if (!wasMember) {
            // Fire event if this member is new
            fireMemberJoined(member);
        }
    }

    public boolean waitForScan() {
        if (!isScanning()) {
            // folder OK!
            return true;
        }
        logFine("Waiting to complete scan");
        ScanResult.ResultState resultState = lastScanResultState;
        while (isScanning() && resultState == lastScanResultState) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
        }
        logFine("Scan completed. Continue with connect.");
        return true;
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
        dao.deleteDomain(member.getId());

        // Fire event
        fireMemberLeft(member);
    }

    /**
     * Delete a FileInfo that has been deleted. This is used to remove a deleted
     * file entry so that it can be restored from the first Member that has the
     * file available in the future.
     * 
     * @param fileInfo
     */
    public void removeDeletedFileInfo(FileInfo fileInfo) {
        Reject.ifFalse(fileInfo.isDeleted(),
            "Should only be removing deleted infos.");
        dao.delete(null, fileInfo);
        dirty = true;
    }

    /**
     * @return true if this folder has beend start. false if shut down
     */
    public boolean isStarted() {
        return !shutdown;
    }

    /**
     * Checks if the folder is syncing. Means: local file scan running or active
     * transfers.
     * 
     * @return if this folder is currently synchronizing.
     */
    public boolean isSyncing() {
        return isScanning() || isTransferring();
    }

    /**
     * Checks if the folder is in Sync, called by FolderRepository
     * 
     * @return if this folder is transferring files
     */
    public boolean isTransferring() {
        return isDownloading() || isUploading();
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
     * Triggers the deletion sync in background.
     * <p>
     * 
     * @param members
     *            selected members to sync deletions with
     * @param force
     */
    public void triggerSyncRemoteDeletedFiles(final Collection<Member> members,
        final boolean force)
    {
        getController().getIOProvider().startIO(new Runnable() {
            public void run() {
                syncRemoteDeletedFiles(members, force);
            }
        });
    }

    /**
     * Synchronizes the deleted files with local folder
     * 
     * @param force
     *            true if the sync is forced with ALL connected members of the
     *            folder. otherwise it checks the modifier.
     */
    public void syncRemoteDeletedFiles(boolean force) {
        syncRemoteDeletedFiles(members.keySet(), force);
    }

    /**
     * Synchronizes the deleted files with local folder
     * 
     * @param members
     *            the members to sync the deletions with.
     * @param force
     *            true if the sync is forced with ALL connected members of the
     *            folder. otherwise it checks the modifier.
     */
    public void syncRemoteDeletedFiles(Collection<Member> members, boolean force)
    {
        if (members.isEmpty()) {
            // Skip.
            return;
        }
        if (isFine()) {
            logFine("Deleting files, which are deleted by friends. con-members: "
                + Arrays.asList(getConnectedMembers()));
        }

        List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        // synchronized (scanLock) {
        for (Member member : members) {
            if (!member.isCompletelyConnected()) {
                // disconnected go to next member
                continue;
            }
            if (!hasWritePermission(member)) {
                if (isWarning()) {
                    logWarning("Not syncing deletions. " + member + " / "
                        + member.getAccountInfo() + " no write permission");
                }
                continue;
            }

            Collection<FileInfo> fileList = getFilesAsCollection(member);
            if (fileList != null) {
                if (isFiner()) {
                    logFiner("RemoteFileDeletion sync. Member '"
                        + member.getNick() + "' has " + fileList.size()
                        + " possible files");
                }

                for (FileInfo remoteFile : fileList) {
                    handleFileDeletion(remoteFile, force, member, removedFiles);
                }
            }

            Collection<DirectoryInfo> dirList = getDirectoriesAsCollection(member);
            if (dirList != null) {
                if (isFiner()) {
                    logFiner("RemoteDirDeletion sync. Member '"
                        + member.getNick() + "' has " + dirList.size()
                        + " possible files");
                }
                List<FileInfo> list = new ArrayList<FileInfo>(dirList);
                Collections.sort(list, new ReverseComparator<FileInfo>(
                    FileInfoComparator
                        .getComparator(FileInfoComparator.BY_RELATIVE_NAME)));
                // logWarning("" + list.size());
                synchronized (scanLock) {
                    for (FileInfo remoteDir : list) {
                        handleFileDeletion(remoteDir, force, member,
                            removedFiles);
                    }
                }
            }
        }
        // }

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

    private void handleFileDeletion(FileInfo remoteFile, boolean force,
        Member member, List<FileInfo> removedFiles)
    {
        if (!remoteFile.isDeleted()) {
            // Not interesting...
            return;
        }

        boolean syncFromMemberAllowed = syncProfile.isSyncDeletion() || force;
        if (!syncFromMemberAllowed) {
            // Not allowed to sync
            return;
        }

        FileInfo localFile = getFile(remoteFile);
        if (localFile != null && !remoteFile.isNewerThan(localFile)) {
            // Remote file is not newer = we are up to date.
            return;
        }

        // Ignored? Skip!
        if (diskItemFilter.isExcluded(remoteFile)) {
            return;
        }

        // Add to local file to database if was deleted on remote
        if (localFile == null) {
            remoteFile = correctFolderInfo(remoteFile);
            store(getController().getMySelf(), remoteFile);
            localFile = getFile(remoteFile);
            // File has been marked as removed at our side
            removedFiles.add(localFile);
        }
        if (localFile.isDeleted()) {
            return;
        }
        File localCopy = localFile.getDiskFile(getController()
            .getFolderRepository());
        if (!localFile.inSyncWithDisk(localCopy)) {
            logFine("Not deleting file from member " + member
                + ", local file not in sync with disk: "
                + localFile.toDetailString() + " at "
                + localCopy.getAbsolutePath());

            if (scanAllowedNow()) {
                if (scanChangedFile(localFile) != null) {
                    // Scan an trigger a sync of deletions later (again).
                    triggerSyncRemoteDeletedFiles(
                        Collections.singleton(member), force);
                }
            }
            // recommendScanOnNextMaintenance();
            return;
        }

        if (isFine()) {
            logFine("File was deleted by " + member + ", deleting local: "
                + localFile.toDetailString() + " at "
                + localCopy.getAbsolutePath());
        }

        // Abort transfers on file.
        if (remoteFile.isFile()) {
            getController().getTransferManager().breakTransfers(remoteFile);
        }

        synchronized (scanLock) {
            if (localCopy.exists()) {
                if (localFile.isDiretory()) {
                    if (isFine()) {
                        logFine("Deleting directory from remote: "
                            + localFile.toDetailString());
                    }
                    watcher.addIgnoreFile(localFile);
                    try {
                        if (!localCopy.delete()) {
                            if (isWarning()) {
                                String[] content = localCopy.list();
                                String contentStr = content != null
                                    ? Arrays.asList(content).toString()
                                    : "(unable to access)";
                                logWarning("Unable to delete directory locally: "
                                    + localCopy
                                    + ". Info: "
                                    + localFile.toDetailString()
                                    + ". contents: " + contentStr);
                            }
                            // Skip. Dir was not actually deleted / could not
                            // sync
                            return;
                        }
                    } finally {
                        watcher.removeIgnoreFile(localFile);
                    }

                } else if (localFile.isFile()) {
                    if (!deleteFile(localFile, localCopy)) {
                        logWarning("Unable to deleted. was not able to move old file to recycle bin "
                            + localCopy.getAbsolutePath()
                            + ". "
                            + localFile.toDetailString());
                        return;
                    }
                } else {
                    logSevere("Unable to apply remote deletion: "
                        + localFile.toDetailString());
                }
            }
        }

        // File has been removed
        // Changed localFile -> remoteFile
        removedFiles.add(remoteFile);
        store(getController().getMySelf(), remoteFile);
    }

    /**
     * Broadcasts a message through the folder
     * 
     * @param message
     */
    public void broadcastMessages(Message... message) {
        for (Member member : getMembersAsCollection()) {
            // Connected?
            if (member.isCompletelyConnected()) {
                // sending all nodes my knows nodes
                member.sendMessagesAsynchron(message);
            }
        }
    }

    /**
     * Broadcasts a message through the folder with support for pre 4.0 clients.
     * <p>
     * Caches the retrieved msgs.
     * 
     * @param msgProvider
     */
    public void broadcastMessages(MessageProvider msgProvider) {
        Message[] msgs = null;
        for (Member member : getMembersAsCollection()) {
            // Connected?
            if (member.isCompletelyConnected()) {
                if (msgs == null) {
                    msgs = msgProvider.getMessages(false);
                }
                if (msgs != null) {
                    member.sendMessagesAsynchron(msgs);
                }
            }
        }
    }

    /**
     * Updated sync patterns have been downloaded to the metaFolder. Update the
     * sync patterns in this (parent) folder.
     * 
     * @param fileInfo
     *            fileInfo of the new sync patterns
     */

    public void handleMetaFolderSyncPatterns(FileInfo fileInfo) {

        if (!syncPatterns) {
            logFine("Not syncing patterns: " + getName());
            return;
        }

        Folder metaFolder = getController().getFolderRepository()
            .getMetaFolderForParent(currentInfo);
        if (metaFolder == null) {
            logWarning("Could not find metaFolder for " + currentInfo);
            return;
        }

        File syncPatternsFile = metaFolder.getDiskFile(fileInfo);
        logFine("Reading syncPatterns " + syncPatternsFile);
        diskItemFilter.loadPatternsFrom(syncPatternsFile, true);
    }

    public DirectoryInfo getBaseDirectoryInfo() {
        return FileInfoFactory.createBaseDirectoryInfo(currentInfo);
    }

    private interface MessageProvider {
        Message[] getMessages(boolean pre4Client);
    }

    /**
     * Broadcasts the remote command to scan the folder.
     */
    public void broadcastScanCommand() {
        if (isFiner()) {
            logFiner("Broadcasting remote scan command");
        }
        Message scanCommand = new ScanCommand(currentInfo);
        broadcastMessages(scanCommand);
    }

    private void broadcastFolderChanges(final ScanResult scanResult) {
        if (getConnectedMembersCount() == 0) {
            return;
        }

        if (!scanResult.getNewFiles().isEmpty()) {
            broadcastMessages(new MessageProvider() {
                public Message[] getMessages(boolean pre4Client) {
                    return FolderFilesChanged.createFolderFilesChangedMessages(
                        currentInfo, scanResult.getNewFiles(), diskItemFilter,
                        true, !pre4Client);
                }
            });
        }
        if (!scanResult.getChangedFiles().isEmpty()) {
            broadcastMessages(new MessageProvider() {
                public Message[] getMessages(boolean pre4Client) {
                    return FolderFilesChanged.createFolderFilesChangedMessages(
                        currentInfo, scanResult.getChangedFiles(),
                        diskItemFilter, true, !pre4Client);
                }
            });
        }
        if (!scanResult.getDeletedFiles().isEmpty()) {
            broadcastMessages(new MessageProvider() {
                public Message[] getMessages(boolean pre4Client) {
                    return FolderFilesChanged.createFolderFilesChangedMessages(
                        currentInfo, scanResult.getDeletedFiles(),
                        diskItemFilter, false, !pre4Client);
                }
            });
        }
        if (!scanResult.getRestoredFiles().isEmpty()) {
            broadcastMessages(new MessageProvider() {
                public Message[] getMessages(boolean pre4Client) {
                    return FolderFilesChanged.createFolderFilesChangedMessages(
                        currentInfo, scanResult.getRestoredFiles(),
                        diskItemFilter, true, !pre4Client);
                }
            });
        }
        if (isFine()) {
            logFine("Broadcasted folder changes for: " + scanResult);
        }
    }

    /**
     * Callback method from member. Called when a new filelist was send
     * 
     * @param from
     * @param newList
     */
    public void fileListChanged(Member from, FileList newList) {
        if (shutdown) {
            return;
        }

        // Correct FolderInfo in case it differs.
        if (newList.files != null) {
            for (int i = 0; i < newList.files.length; i++) {
                FileInfo fInfo = newList.files[i];
                newList.files[i] = FileInfoFactory.changedFolderInfo(fInfo,
                    currentInfo);
            }
        }

        // #1022 - Mass delete detection. Switch to a safe profile if
        // a large percent of files would get deleted by another node.
        if (newList.files != null
            && syncProfile.isSyncDeletion()
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {
            checkForMassDeletion(from, newList.files);
        }

        // Update DAO
        if (newList.isNull()) {
            // Delete files in domain and do nothing
            dao.deleteDomain(from.getId());
            return;
        }
        // Store but also deleted/clear domain before.
        store(from, true, newList.files);

        // Try to find same files
        findSameFiles(from, Arrays.asList(newList.files));

        if (syncProfile.isAutodownload() && from.isCompletelyConnected()) {
            // Trigger file requestor
            if (isFiner()) {
                logFiner("Triggering file requestor because of new remote file list from "
                    + from);
            }
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(newList.folder);
        }

        // Handle remote deleted files
        if (syncProfile.isSyncDeletion() && from.isCompletelyConnected()) {
            syncRemoteDeletedFiles(Collections.singleton(from), false);
        }

        fireRemoteContentsChanged(from, newList);
    }

    /**
     * Callback method from member. Called when a filelist delta received
     * 
     * @param from
     * @param changes
     */
    public void fileListChanged(Member from, FolderFilesChanged changes) {
        if (shutdown) {
            return;
        }

        // Correct FolderInfo in case it differs.
        if (changes.added != null) {
            for (int i = 0; i < changes.added.length; i++) {
                FileInfo fInfo = changes.added[i];
                changes.added[i] = FileInfoFactory.changedFolderInfo(fInfo,
                    currentInfo);
            }
        }
        if (changes.removed != null) {
            for (int i = 0; i < changes.removed.length; i++) {
                FileInfo fInfo = changes.removed[i];
                changes.removed[i] = FileInfoFactory.changedFolderInfo(fInfo,
                    currentInfo);
            }
        }

        // #1022 - Mass delete detection. Switch to a safe profile if
        // a large percent of files would get deleted by another node.
        if (changes.removed != null
            && syncProfile.isSyncDeletion()
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {
            checkForMassDeletion(from, changes.removed);
        }

        // Try to find same files
        if (changes.added != null) {
            store(from, changes.added);
            findSameFiles(from, Arrays.asList(changes.added));
        }
        if (changes.removed != null) {
            store(from, changes.removed);
            findSameFiles(from, Arrays.asList(changes.removed));
        }

        // Avoid hammering of sync remote deletion
        boolean singleFileAddMsg = changes.added != null
            && changes.added.length == 1 && changes.removed == null;

        if (syncProfile.isAutodownload()) {
            // Check if we need to trigger the filerequestor
            boolean triggerFileRequestor = from.isCompletelyConnected();
            if (triggerFileRequestor && singleFileAddMsg) {
                // This was caused by a completed download
                // TODO Maybe check this also on bigger lists!
                FileInfo localfileInfo = getFile(changes.added[0]);
                FileInfo remoteFileInfo = changes.added[0];
                if (localfileInfo != null
                    && !remoteFileInfo.isNewerThan(localfileInfo)
                    && !remoteFileInfo.isDeleted())
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
        if (!singleFileAddMsg && syncProfile.isSyncDeletion()
            && from.isCompletelyConnected())
        {
            syncRemoteDeletedFiles(Collections.singleton(from), false);
        }

        // Fire event
        fireRemoteContentsChanged(from, changes);
    }

    private void store(Member member, FileInfo... fileInfos) {
        store(member, false, fileInfos);
    }

    private void store(Member member, boolean deletedDomain,
        FileInfo... fileInfos)
    {
        store(member, deletedDomain, Arrays.asList(fileInfos));
    }

    private void store(Member member, Collection<FileInfo> fileInfos) {
        store(member, false, fileInfos);
    }

    private void store(Member member, boolean deletedDomain,
        Collection<FileInfo> fileInfos)
    {
        synchronized (dbAccessLock) {
            String domainID = member.isMySelf() ? null : member.getId();
            if (deletedDomain) {
                dao.deleteDomain(domainID);
            }
            dao.store(domainID, fileInfos);
        }
    }

    private void checkForMassDeletion(Member from, FileInfo[] fileInfos) {
        int delsCount = 0;
        for (FileInfo remoteFile : fileInfos) {
            if (!remoteFile.isDeleted()) {
                continue;
            }
            // #1842: Actually check if these files have just been deleted.
            FileInfo localFile = getFile(remoteFile);
            if (localFile != null && !localFile.isDeleted()
                && remoteFile.isNewerThan(localFile))
            {
                delsCount++;
            }
        }

        if (delsCount >= Constants.FILE_LIST_MAX_FILES_PER_MESSAGE) {
            // #1786 - If deletion >= max files per message, switch.
            switchToSafe(from, delsCount, false);
        } else {
            int knownFilesCount = getKnownItemCount();
            if (knownFilesCount > 1) {
                int delPercentage = 100 * delsCount / knownFilesCount;
                logFine("FolderFilesChanged delete percentage " + delPercentage
                    + '%');
                if (delPercentage >= ConfigurationEntry.MASS_DELETE_THRESHOLD
                    .getValueInt(getController()))
                {
                    switchToSafe(from, delPercentage, true);
                }
            }
        }
    }

    private void switchToSafe(Member from, int delsCount, boolean percentage) {
        logWarning("Received a FolderFilesChanged message from "
            + from.getInfo().nick + " which will delete " + delsCount
            + " files in folder " + currentInfo.name
            + ". The sync profile will now be switched from "
            + syncProfile.getName() + " to " + SyncProfile.HOST_FILES.getName()
            + " to protect the files.");

        SyncProfile original = syncProfile;

        // Emergency profile switch to something safe.
        setSyncProfile(SyncProfile.HOST_FILES);

        // Advise the controller of the problem.
        getController().remoteMassDeletionDetected(
            new RemoteMassDeletionEvent(currentInfo, from.getInfo(), delsCount,
                original, syncProfile, percentage));

        logWarning("Switched to " + syncProfile.getName());
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
    private void findSameFiles(Member member,
        Collection<FileInfo> remoteFileInfos)
    {
        Reject.ifNull(remoteFileInfos, "Remote file info list is null");
        if (isFiner()) {
            logFiner("Triing to find same files in remote list with "
                + remoteFileInfos.size() + " files from " + member);
        }
        if (!hasWritePermission(member)) {
            if (isWarning()) {
                logWarning("Not searching same files. " + member + " / "
                    + member.getAccountInfo() + " no write permission");
            }
            return;
        }

        for (FileInfo remoteFileInfo : remoteFileInfos) {
            FileInfo localFileInfo = getFile(remoteFileInfo);
            if (localFileInfo == null) {
                continue;
            }

            if (localFileInfo.isDeleted() || remoteFileInfo.isDeleted()) {
                continue;
            }

            boolean fileSizeSame = localFileInfo.getSize() == remoteFileInfo
                .getSize();
            boolean dateSame = DateUtil.equalsFileDateCrossPlattform(
                localFileInfo.getModifiedDate(), remoteFileInfo
                    .getModifiedDate());
            boolean fileCaseSame = localFileInfo.getRelativeName().equals(
                remoteFileInfo.getRelativeName());

            if (localFileInfo.getVersion() < remoteFileInfo.getVersion()
                && remoteFileInfo.getVersion() > 0)
            {
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
                    // localFileInfo.copyFrom(remoteFileInfo);
                    store(getController().getMySelf(), remoteFileInfo);
                    // FIXME That might produce a LOT of traffic! Single update
                    // message per file! This also might intefere with FileList
                    // exchange at beginning of communication
                    fileChanged(remoteFileInfo);
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
            } else if (!fileCaseSame && dateSame && fileSizeSame) {
                if (localFileInfo.getRelativeName().compareTo(
                    remoteFileInfo.getRelativeName()) <= 0)
                {
                    // Skip this fileinfo. Compare by name is performed
                    // to ensure that the FileInfo with the greatest
                    // lexographic index is taken. This is a
                    // deterministic rule to keep file db repos in sync
                    // among peers.

                    if (isFine()) {
                        logFine("Found identical file remotely with diffrent name-case: local "
                            + localFileInfo.toDetailString()
                            + " remote: "
                            + remoteFileInfo.toDetailString()
                            + ". Taking over all infos");
                    }

                    remoteFileInfo = correctFolderInfo(remoteFileInfo);
                    store(getController().getMySelf(), remoteFileInfo);

                    // FIXME That might produce a LOT of traffic! Single
                    // update
                    // message per file! This also might intefere with
                    // FileList
                    // exchange at beginning of communication
                    fileChanged(remoteFileInfo);
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
            Collection<FileInfo> lastFileList = getFilesAsCollection(member);
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
        // logWarning("DB dirty", new RuntimeException());
    }

    /**
     * Persists settings to disk.
     */
    private void persist() {
        if (checkIfDeviceDisconnected()) {
            logWarning("Unable to persist database. Device is disconnected: "
                + localBase);
            return;
        }
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
            getController().getMySelf().getNick(), dao.findAllFiles(null),
            "FileList of folder " + getName() + ", member " + this + ':');
    }

    /*
     * Simple getters/exposing methods
     */

    /**
     * @return the local base directory
     */
    public File getLocalBase() {
        return localBase;
    }

    /**
     * @return the dir to commit/mirror the whole folder contents to after
     *         folder has been fully updated. null if no commit should be
     *         performed
     */
    public File getCommitDir() {
        return commitDir;
    }

    /**
     * @return the commit dir or the local base if commit dir is null.
     */
    public File getCommitOrLocalDir() {
        if (commitDir != null) {
            return commitDir;
        }
        return localBase;
    }

    /**
     * @param commitDir
     *            the dir to commit/mirror the whole folder contents to after
     *            folder has been fully updated. null if no commit should be
     *            performed
     */
    public void setCommitDir(File commitDir) {
        this.commitDir = commitDir;
        // Store on disk
        String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
            .getBytes())));
        String confKey = FOLDER_SETTINGS_PREFIX_V4 + md5
            + FolderSettings.FOLDER_SETTINGS_COMMIT_DIR;
        String confVal = commitDir != null ? commitDir.getAbsolutePath() : "";
        getController().getConfig().put(confKey, confVal);
        logInfo("Commit dir set to '" + confVal + '\'');
        getController().saveConfig();
    }

    /**
     * @return the system subdir in the local base folder. subdir gets created
     *         if not exists
     */
    public File getSystemSubDir() {
        File systemSubDir = new File(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        if (!systemSubDir.exists()) {
            if (!deviceDisconnected && systemSubDir.mkdirs()) {
                FileUtils.setAttributesOnWindows(systemSubDir, true, true);
            } else if (!deviceDisconnected) {
                logSevere("Failed to create system subdir: " + systemSubDir);
            } else if (isFine()) {
                logFine("Failed to create system subdir: " + systemSubDir);
            }
        }
        return systemSubDir;
    }

    /**
     * Is this directory the system subdirectory?
     * 
     * @param aDir
     * @return
     */
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

    /**
     * Actually checks if the device is disconnected or available. Also sets the
     * property "deviceDisconnected".
     * 
     * @return true if the device is disconnected. false if everything is ok.
     */
    public boolean checkIfDeviceDisconnected() {
        /**
         * Check that we still have a good local base.
         */
        try {
            checkBaseDir(localBase, true);
        } catch (FolderException e) {
            logFiner("invalid local base: " + e);
            return setDeviceDisconnected(true);
        }

        // #1249
        if (getKnownItemCount() > 0 && (OSUtil.isMacOS() || OSUtil.isLinux())) {
            boolean inaccessible = localBase.list() == null
                || localBase.list().length == 0 || !localBase.exists();
            if (inaccessible) {
                logWarning("Local base empty on linux file system, but has known files. "
                    + localBase);
                return setDeviceDisconnected(true);
            }
        }

        // Is OK!
        return setDeviceDisconnected(false);
    }

    private boolean setDeviceDisconnected(boolean disconnected) {
        deviceDisconnected = disconnected;

        boolean addProblem = disconnected;
        for (Problem problem : problems) {
            if (problem instanceof DeviceDisconnectedProblem) {
                if (!deviceDisconnected) {
                    logInfo("Device connected again");
                    removeProblem(problem);
                } else {
                    addProblem = false;
                }
            }
        }
        if (addProblem) {
            logInfo("Device disconnected");
            addProblem(new DeviceDisconnectedProblem());
        }

        return deviceDisconnected;
    }

    public String getName() {
        return currentInfo.name;
    }

    public int getKnownItemCount() {
        // All! Also excluded items
        return dao.count(null, true, false);
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }

    /**
     * WARNING: Contents may change after getting the collection.
     * 
     * @return a unmodifiable collection referecing the internal file database
     *         hashmap (keySet).
     */
    public Collection<FileInfo> getKnownFiles() {
        return dao.findAllFiles(null);
    }

    /**
     * WARNING: Contents may change after getting the collection.
     * 
     * @return a unmodifiable collection referecing the internal directory
     *         database hashmap (keySet).
     */
    public Collection<DirectoryInfo> getKnownDirectories() {
        return dao.findAllDirectories(null);
    }

    /**
     * Common file delete method. Either deletes the file or moves it to the
     * recycle bin.
     * 
     * @param newFileInfo
     * @param file
     */
    private boolean deleteFile(FileInfo newFileInfo, File file) {
        Reject.ifNull(newFileInfo, "FileInfo is null");
        FileInfo fileInfo = getFile(newFileInfo);
        if (isFine()) {
            logFine("Deleting file " + fileInfo.toDetailString()
                + " moving to archive");
        }
        try {
            watcher.addIgnoreFile(newFileInfo);
            if (fileInfo != null) {
                try {
                    archiver.archive(fileInfo, file, false);
                } catch (IOException e) {
                    logSevere("Unable to move file to archive: " + file + ". "
                        + e, e);
                }
            }
            if (file.exists() && !file.delete()) {
                logSevere("Unable to delete file " + file);
                return false;
            }
            return true;
        } finally {
            watcher.removeIgnoreFile(newFileInfo);
        }
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @return the list of files that are incoming/newer available on remote
     *         side as unmodifiable collection.
     */
    public Collection<FileInfo> getIncomingFiles() {
        return getIncomingFiles(true);
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @param includeDeleted
     *            true if also deleted files should be considered.
     * @return the list of files that are incoming/newer available on remote
     *         side as unmodifiable collection.
     */
    public Collection<FileInfo> getIncomingFiles(boolean includeDeleted) {
        // build a temp list
        // Map<FileInfo, FileInfo> incomingFiles = new HashMap<FileInfo,
        // FileInfo>();
        SortedMap<FileInfo, FileInfo> incomingFiles = new TreeMap<FileInfo, FileInfo>(
            new FileInfoComparator(FileInfoComparator.BY_RELATIVE_NAME));
        // add0 expeced files
        for (Member member : getMembersAsCollection()) {
            if (!member.isCompletelyConnected()) {
                // disconnected or myself (=skip)
                continue;
            }
            if (!member.hasCompleteFileListFor(currentInfo)) {
                if (isFine()) {
                    logFine("Skipping " + member
                        + " no complete filelist from him");
                }
                continue;
            }
            if (!hasWritePermission(member)) {
                if (isWarning()) {
                    logWarning("Not downloading files. " + member + " / "
                        + member.getAccountInfo() + " no write permission");
                }
                continue;
            }

            Collection<FileInfo> memberFiles = getFilesAsCollection(member);
            if (memberFiles != null) {
                for (FileInfo remoteFile : memberFiles) {
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
                        // TODO Maby download deleted files from archive of
                        // remote?
                        // and put it directly into own recycle bin.
                        continue;
                    }
                    if (notLocal || newerThanLocal && newestRemote) {
                        // Okay this one is expected
                        if (!diskItemFilter.isExcluded(remoteFile)) {
                            incomingFiles.put(remoteFile, remoteFile);
                        }
                    }
                }
            }
            Collection<DirectoryInfo> memberDirs = dao
                .findAllDirectories(member.getId());
            if (memberDirs != null) {
                for (DirectoryInfo remoteDir : memberDirs) {
                    if (remoteDir.isDeleted() && !includeDeleted) {
                        continue;
                    }

                    // Check if remote file is newer
                    FileInfo localFile = getFile(remoteDir);
                    FileInfo alreadyIncoming = incomingFiles.get(remoteDir);
                    boolean notLocal = localFile == null;
                    boolean newerThanLocal = localFile != null
                        && remoteDir.isNewerThan(localFile);
                    // Check if this remote file is newer than one we may
                    // already have.
                    boolean newestRemote = alreadyIncoming == null
                        || remoteDir.isNewerThan(alreadyIncoming);
                    if (notLocal && remoteDir.isDeleted()) {
                        // A remote deleted file is not incoming!
                        // TODO Maby download deleted files from archive of
                        // remote?
                        // and put it directly into own recycle bin.
                        continue;
                    }
                    if (notLocal || newerThanLocal && newestRemote) {
                        // Okay this one is expected
                        if (!diskItemFilter.isExcluded(remoteDir)) {
                            incomingFiles.put(remoteDir, remoteDir);
                        }
                    }
                }
            }
        }

        if (incomingFiles.isEmpty()) {
            logFiner("No Incoming files");
        } else {
            if (isFine()) {
                logFine(incomingFiles.size() + " incoming files");
            }
        }

        return Collections.unmodifiableCollection(incomingFiles.keySet());
    }

    /**
     * Visits all remote {@link FileInfo}s and {@link DirectoryInfo}s, that
     * <p>
     * 1) Do not exist locally or
     * <p>
     * 2) Are newer than the local version.
     * 
     * @param vistor
     *            the {@link Visitor} to pass the incoming files to.
     */
    public void visitIncomingFiles(Visitor<FileInfo> vistor) {
        // add0 expeced files
        for (Member member : getMembersAsCollection()) {
            if (!member.isCompletelyConnected()) {
                // disconnected or myself (=skip)
                continue;
            }
            if (!member.hasCompleteFileListFor(currentInfo)) {
                if (isFine()) {
                    logFine("Skipping " + member
                        + " no complete filelist from him");
                }
                continue;
            }
            if (!hasWritePermission(member)) {
                if (isWarning()) {
                    logWarning("Not downloading files. " + member + " / "
                        + member.getAccountInfo() + " no write permission");
                }
                continue;
            }

            Collection<FileInfo> memberFiles = getFilesAsCollection(member);
            if (memberFiles != null) {
                for (FileInfo fileInfo : memberFiles) {
                    if (!visitFileIfNewer(fileInfo, vistor)) {
                        // Stop visiting.
                        return;
                    }
                }
            }
            Collection<DirectoryInfo> memberDirs = dao
                .findAllDirectories(member.getId());
            if (memberDirs != null) {
                for (FileInfo fileInfo : memberDirs) {
                    if (!visitFileIfNewer(fileInfo, vistor)) {
                        // Stop visiting.
                        return;
                    }
                }
            }
        }
    }

    private boolean visitFileIfNewer(FileInfo fileInfo, Visitor<FileInfo> vistor)
    {
        // Check if remote file is newer
        FileInfo localFile = getFile(fileInfo);
        boolean notLocal = localFile == null;
        boolean remoteNewerThanLocal = localFile != null
            && fileInfo.isNewerThan(localFile);
        if (notLocal && fileInfo.isDeleted()) {
            return true;
        }
        if (notLocal || remoteNewerThanLocal) {
            // Okay this one is expected
            if (!diskItemFilter.isExcluded(fileInfo)) {
                try {
                    return vistor.visit(fileInfo);
                } catch (Exception e) {
                    logSevere("Error while visiting incoming files. " + e, e);
                }
            }
        }
        return true;
    }

    /**
     * @param member
     * @return the list of files from a member as unmodifiable collection
     */
    public Collection<FileInfo> getFilesAsCollection(Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        return dao.findAllFiles(member.getId());
    }

    /**
     * @param member
     * @return the list of directories from a member as unmodifiable collection
     */
    public Collection<DirectoryInfo> getDirectoriesAsCollection(Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        return dao.findAllDirectories(member.getId());
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
            if (member.isCompletelyConnected()) {
                nConnected++;
            }
        }
        return nConnected;
    }

    /**
     * @return the connected members EXCLUDING myself.
     */
    public Member[] getConnectedMembers() {
        List<Member> connected = new ArrayList<Member>(members.size());
        for (Member member : getMembersAsCollection()) {
            if (member.isCompletelyConnected()) {
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
        return getFile(fInfo) != null;
    }

    /**
     * @param fInfo
     * @return the local fileinfo instance
     */
    public FileInfo getFile(FileInfo fInfo) {
        return dao.find(fInfo, null);
    }

    /**
     * @param fInfo
     * @return the local file from a file info Never returns null, file MAY NOT
     *         exist!! check before use
     */
    public File getDiskFile(FileInfo fInfo) {
        return new File(localBase, fInfo.getRelativeName());
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
     * @return the date of the last maintenance.
     */
    public Date getLastDBMaintenanceDate() {
        return lastDBMaintenance;
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
     * @return the {@link FileInfoDAO}. TRAC #1422
     */
    public FileInfoDAO getDAO() {
        return dao;
    }

    /**
     * @return an Invitation to this folder. Includes a intelligent opposite
     *         sync profile.
     */
    public Invitation createInvitation() {
        Invitation inv = new Invitation(currentInfo, getController()
            .getMySelf().getInfo());
        inv.setFilesCount(statistic.getLocalFilesCount());
        inv.setSize(statistic.getLocalSize());
        inv.setSuggestedSyncProfile(syncProfile);
        if (syncProfile.equals(SyncProfile.BACKUP_SOURCE)
            || syncProfile.equals(SyncProfile.BACKUP_SOURCE_HOUR))
        {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_TARGET);
        } else if (syncProfile.equals(SyncProfile.BACKUP_TARGET)) {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_SOURCE);
        } else if (syncProfile.equals(SyncProfile.HOST_FILES)) {
            inv.setSuggestedSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        }
        inv.setSuggestedLocalBase(getController(), localBase);
        return inv;
    }

    /**
     * Ensures that default ignore patterns are set.
     */
    public void addDefaultExcludes() {
        // Add thumbs to ignore pattern on windows systems
        // Don't duplicate thumbs (like when moving a preview folder)
        addPattern(Pattern.THUMBS_DB);

        // ... and temporary word files
        addPattern(Pattern.OFFICE_TEMP);

        // Add desktop.ini to ignore pattern on windows systems
        // if (ConfigurationEntry.USE_PF_ICON.getValueBoolean(getController()))
        // {
        addPattern(FileUtils.DESKTOP_INI_FILENAME);
        // }

        // Add dsstore to ignore pattern on mac systems
        // Don't duplicate dsstore (like when moving a preview folder)
        addPattern(Pattern.DS_STORE);
    }

    /**
     * Watch for harmonized sync is 100%. If so, set a new lastSync date.
     */
    private void checkLastSyncDate() {
        double percentage = statistic.getHarmonizedSyncPercentage();
        boolean newInSync = Double.compare(percentage, 100.0d) == 0;
        if (newInSync) {
            lastSyncDate = new Date();
            storeLastSyncDate();
        }
        if (isFiner()) {
            logFiner("Harmonized percentage: " + percentage + ". In sync? "
                + newInSync + ". last sync date: " + lastSyncDate
                + " . connected: " + getConnectedMembersCount());
        }
    }

    private void storeLastSyncDate() {
        File lastSyncFile = new File(getSystemSubDir(), LAST_SYNC_INFO_FILENAME);
        try {
            lastSyncFile.createNewFile();
        } catch (IOException e) {
            // Ignore.
        }
        try {
            lastSyncFile.setLastModified(lastSyncDate.getTime());
        } catch (Exception e) {
            logSevere("Unable to update last synced date to " + lastSyncFile);
        }
    }

    private void loadLastSyncDate() {
        File lastSyncFile = new File(getSystemSubDir(), LAST_SYNC_INFO_FILENAME);
        if (lastSyncFile.exists()) {
            lastSyncDate = new Date(lastSyncFile.lastModified());
        } else {
            lastSyncDate = null;
        }
    }

    // Security methods *******************************************************

    public boolean hasReadPermission(Member member) {
        return hasFolderPermission(member, FolderPermission
            .read(getParentFolderInfo()));
    }

    public boolean hasWritePermission(Member member) {
        return hasFolderPermission(member, FolderPermission
            .readWrite(getParentFolderInfo()));
    }

    public boolean hasAdminPermission(Member member) {
        return hasFolderPermission(member, FolderPermission
            .admin(getParentFolderInfo()));
    }

    public boolean hasOwnerPermission(Member member) {
        return hasFolderPermission(member, FolderPermission
            .owner(getParentFolderInfo()));
    }

    private boolean hasFolderPermission(Member member,
        FolderPermission permission)
    {
        if (getController().getOSClient().isCloudServer(member)) {
            return true;
        }
        return getController().getSecurityManager().hasPermission(
            member.getAccountInfo(), permission);
    }

    private FolderInfo getParentFolderInfo() {
        if (!currentInfo.isMetaFolder()) {
            return currentInfo;
        }
        Folder parentFolder = getController().getFolderRepository()
            .getParentFolder(currentInfo);
        if (parentFolder == null) {
            logSevere("Unable to retrieve parent folder for " + currentInfo);
            return currentInfo;
        }
        return parentFolder.currentInfo;
    }

    // General stuff **********************************************************

    @Override
    public String toString() {
        return currentInfo.toString();
    }

    // Logger methods *********************************************************

    @Override
    public String getLoggerName() {
        return super.getLoggerName() + " '" + getName() + '\'';
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

    private void fireFilesChanged(List<FileInfo> fileInfos) {
        if (isFiner()) {
            logFiner("fireFileChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, fileInfos, true);
        folderListenerSupport.fileChanged(folderEvent);
    }

    private void fireFilesDeleted(Collection<FileInfo> fileInfos) {
        if (isFiner()) {
            logFiner("fireFilesDeleted: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, fileInfos);
        folderListenerSupport.filesDeleted(folderEvent);
    }

    private void fireRemoteContentsChanged(Member from, FileList list) {
        if (isFiner()) {
            logFiner("fireRemoteContentsChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, list, from);
        folderListenerSupport.remoteContentsChanged(folderEvent);
    }

    private void fireRemoteContentsChanged(Member from, FolderFilesChanged list)
    {
        if (isFiner()) {
            logFiner("fireRemoteContentsChanged: " + this);
        }
        FolderEvent folderEvent = new FolderEvent(this, list, from);
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
    void notifyStatisticsCalculated() {
        checkLastSyncDate();

        FolderEvent folderEvent = new FolderEvent(this);
        folderListenerSupport.statisticsCalculated(folderEvent);
    }

    /**
     * Call when a folder is being removed to clear any references.
     */
    public void clearAllProblemListeners() {
        ListenerSupportFactory.removeAllListeners(problemListenerSupport);
    }

    /**
     * This creates / removes a warning if the folder has not been synchronized
     * in a long time.
     */
    public void processUnsyncFolder() {
        if (!ConfigurationEntry.FOLDER_SYNC_USE
            .getValueBoolean(getController()))
        {
            return;
        }
        if (isPreviewOnly()) {
            return;
        }

        // Calculate the date that folders should be synced by.
        Integer syncWarnDays = ConfigurationEntry.FOLDER_SYNC_WARN
            .getValueInt(getController());
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -syncWarnDays);
        Date warningDate = cal.getTime();

        // If others are in sync, do not warn because I can sync up with them.
        boolean othersInSync = false;
        Member me = getController().getMySelf();
        for (Member member : members.values()) {
            double memberSync = statistic.getSyncPercentage(member);
            if (!member.equals(me) && Double.compare(memberSync, 100.0) == 0) {
                othersInSync = true;
                break;
            }
        }

        if (lastSyncDate != null && lastSyncDate.before(warningDate)
            && !othersInSync && !isSyncing())
        {

            // Only need one of these.
            UnsynchronizedFolderProblem ufp = null;
            for (Problem problem : problems) {
                if (problem instanceof UnsynchronizedFolderProblem) {
                    ufp = (UnsynchronizedFolderProblem) problem;
                    break;
                }
            }
            if (ufp == null) {
                Problem problem = new UnsynchronizedFolderProblem(currentInfo,
                    syncWarnDays);
                addProblem(problem);
            }
        } else {
            // Perhaps now need to remove it?
            UnsynchronizedFolderProblem ufp = null;
            for (Problem problem : problems) {
                if (problem instanceof UnsynchronizedFolderProblem) {
                    ufp = (UnsynchronizedFolderProblem) problem;
                    break;
                }
            }
            if (ufp != null) {
                removeProblem(ufp);
            }
        }
    }

    public boolean isSyncPatterns() {
        return syncPatterns;
    }

    public void setSyncPatterns(boolean syncPatterns) {
        this.syncPatterns = syncPatterns;
        // Store on disk
        String md5 = new String(Util.encodeHex(Util.md5(currentInfo.id
            .getBytes())));
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + md5
            + FolderSettings.FOLDER_SETTINGS_SYNC_PATTERNS;
        getController().getConfig().put(syncProfKey,
            String.valueOf(syncPatterns));
        getController().saveConfig();
    }

    /**
     * Save patterns to metaFolder for transfer to other computers.
     */
    private void savePatternsToMetaFolder() {
        if (Feature.META_FOLDER.isDisabled()) {
            return;
        }
        // Should the patterns be synchronized?
        if (!syncPatterns) {
            return;
        }
        if (currentInfo.isMetaFolder()) {
            return;
        }
        // Only do this for parent folders.
        FolderRepository folderRepository = getController()
            .getFolderRepository();
        Folder metaFolder = folderRepository
            .getMetaFolderForParent(currentInfo);
        if (metaFolder == null) {
            logWarning("Could not find metaFolder for " + currentInfo);
            return;
        }
        if (metaFolder.isDeviceDisconnected()) {
            logFiner("Not writing synced ignored patterns. Meta folder disconnected");
            return;
        }
        // Write the patterns in the meta directory.
        File file = new File(metaFolder.getLocalBase(),
            DiskItemFilter.PATTERNS_FILENAME);
        FileInfo fInfo = FileInfoFactory.lookupInstance(metaFolder, file);
        diskItemFilter.savePatternsTo(file);
        logWarning("Saving ignore patterns to Meta folder: " + file);
        metaFolder.scanChangedFile(fInfo);
    }

    // Inner classes **********************************************************

    /**
     * Persister task, persists settings from time to time.
     */
    private class Persister extends TimerTask {
        @Override
        public void run() {
            if (shutdown) {
                return;
            }
            if (dirty) {
                persist();
            }
            if (diskItemFilter.isDirty() && !checkIfDeviceDisconnected()) {
                diskItemFilter.savePatternsTo(new File(getSystemSubDir(),
                    DiskItemFilter.PATTERNS_FILENAME));
                if (!shutdown) {
                    savePatternsToMetaFolder();
                }
            }
        }

        @Override
        public String toString() {
            return "FolderPersister for '" + Folder.this;
        }
    }

}