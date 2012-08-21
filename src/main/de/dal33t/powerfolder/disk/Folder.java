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
import java.io.Externalizable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.disk.dao.FileInfoDAOHashMapImpl;
import de.dal33t.powerfolder.disk.problem.DeviceDisconnectedProblem;
import de.dal33t.powerfolder.disk.problem.FileConflictProblem;
import de.dal33t.powerfolder.disk.problem.FilenameProblemHelper;
import de.dal33t.powerfolder.disk.problem.FolderDatabaseProblem;
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
import de.dal33t.powerfolder.message.FileRequestCommand;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageProducer;
import de.dal33t.powerfolder.message.ScanCommand;
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
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Visitor;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.logging.LoggingManager;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.pattern.Pattern;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * The main class representing a folder. Scans for new files automatically.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Folder extends PFComponent {

    private static final String LAST_SYNC_INFO_FILENAME = "Last_sync";
    public static final String METAFOLDER_MEMBERS = "Members";

    private static final int TEN_MINUTES = 60 * 10;
    private static final int ONE_MINUTE = 60;
    private static final int TEN_SECONDS = 10;

    /** The base location of the folder. */
    private final TFile localBase;

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
     * The entry id in config file. Usually MD5(random ID, folderID or a
     * meaningful unique name, e.g. Desktop).
     */
    private String configEntryId;

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

    private boolean encrypted;

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
     * The number of seconds the folder is allowed to be out of sync. If it is
     * longer out of sync it produces an {@link UnsynchronizedFolderProblem}. If
     * not set (0) the default global will be assumed
     * {@link ConfigurationEntry#FOLDER_SYNC_WARN_DAYS}.
     */
    private int syncWarnSeconds;
    private Persister persister;

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
        encrypted = folderSettings.getLocalBaseDir().getName()
            .endsWith(".pfzip");
        problems = new CopyOnWriteArrayList<Problem>();
        if (encrypted) {
            localBase = new TFile(folderSettings.getLocalBaseDir());
            localBase.mkdirs();
            try {
                new TFile(localBase, "dummy.txt").createNewFile();
                new TFile(localBase, "dummy.txt").rm();
            } catch (IOException e) {
                logWarning("Unable to initialize encrypted storage at "
                    + localBase);
            }
            if (!localBase.isArchive()) {
                throw new IllegalStateException();
            }
        } else if (folderSettings.getLocalBaseDir().isAbsolute()) {
            localBase = new TFile(folderSettings.getLocalBaseDir());
        } else {
            localBase = new TFile(getController().getFolderRepository()
                .getFoldersAbsoluteDir(), folderSettings.getLocalBaseDir()
                .getPath());
            logWarning("Original path: " + folderSettings.getLocalBaseDir()
                + ". Choosen relative path: " + localBase);
            if (folderSettings.getLocalBaseDir().exists()
                && !localBase.exists())
            {
                localBase.mkdirs();
            }
        }

        // Support for meta folder.
        if (fInfo.isMetaFolder()
            && folderSettings.getLocalBaseDir().getAbsolutePath()
                .contains(".pfzip"))
        {
            encrypted = true;
        }
        Reject.ifTrue(localBase.equals(getController().getFolderRepository()
            .getFoldersAbsoluteDir()),
            "Folder cannot be located at base directory for all folders");

        if (folderSettings.getCommitDir() != null) {
            if (folderSettings.getCommitDir().isAbsolute()) {
                commitDir = folderSettings.getCommitDir();
            } else {
                commitDir = new File(getController().getFolderRepository()
                    .getFoldersAbsoluteDir(), folderSettings.getCommitDir()
                    .getPath());
            }
        }
        syncProfile = folderSettings.getSyncProfile();
        downloadScript = folderSettings.getDownloadScript();
        syncPatterns = folderSettings.isSyncPatterns();
        syncWarnSeconds = folderSettings.getSyncWarnSeconds();
        previewOnly = folderSettings.isPreviewOnly();
        configEntryId = folderSettings.getConfigEntryId();

        // Check base dir
        try {
            checkBaseDir(false);
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
        // join0(controller.getMySelf());
        members.put(controller.getMySelf(), controller.getMySelf());

        // Now calc.
        statistic = new FolderStatistic(this);

        // Check desktop ini in Windows environments
        if (!currentInfo.isMetaFolder()) {
            FileUtils.maintainDesktopIni(getController(), localBase);
        }

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
            writeFilelist(getController().getMySelf());
        }

        persister = new Persister();
        getController().scheduleAndRepeat(
            persister,
            1000L,
            1000L * ConfigurationEntry.FOLDER_DB_PERSIST_TIME
                .getValueInt(getController()));

        if (folderSettings.getArchiveMode() != null) {
            try {
                archiver = folderSettings.getArchiveMode().getInstance(this);
            } catch (Exception e) {
                logWarning("Unable to setup file archive - disabled now. Please check the folder base dir: "
                    + localBase + ". " + e.getMessage());
                archiver = ArchiveMode.NO_BACKUP.getInstance(this);
            }

        } else {
            archiver = ArchiveMode.NO_BACKUP.getInstance(this);
        }
        archiver.setVersionsPerFile(folderSettings.getVersions());

        // Create invitation
        if (folderSettings.isCreateInvitationFile()) {
            try {
                Invitation inv = createInvitation();
                File invFile = new TFile(localBase,
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

        // Remove desktop.ini. Was accidentally created in 4.3.0 release.
        if (currentInfo.isMetaFolder()) {
            File desktopIni = new TFile(localBase,
                FileUtils.DESKTOP_INI_FILENAME);
            if (desktopIni.exists() && desktopIni.delete()) {
                scanChangedFile(FileInfoFactory
                    .lookupInstance(this, desktopIni));
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
            String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
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
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
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
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())
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
    public void addPattern(String pattern) {
        diskItemFilter.addPattern(pattern);
        triggerPersist();
    }

    /**
     * Convenience method to remove a pattern.
     * 
     * @param pattern
     */
    public void removePattern(String pattern) {
        diskItemFilter.removePattern(pattern);
        triggerPersist();
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
     * @throws FolderException
     *             if base dir is not ok
     */
    private void checkBaseDir(boolean quite) throws FolderException {
        // Basic checks
        if (!localBase.exists()) {
            // TRAC #1249
            if ((OSUtil.isMacOS() || OSUtil.isLinux())
                && localBase.getAbsolutePath().toLowerCase()
                    .startsWith("/volumes"))
            {
                throw new FolderException(currentInfo,
                    "Unmounted volume not available at "
                        + localBase.getAbsolutePath());
            }

            // #2329
            throw new FolderException(currentInfo,
                "Local base dir not available " + localBase.getAbsolutePath());

            // Old code:
            // if (!localBase.mkdirs()) {
            // if (!quite) {
            // logSevere(" not able to create folder(" + getName()
            // + "), (sub) dir (" + localBase + ") creation failed");
            // }
            // throw new FolderException(currentInfo,
            // "Unable to create folder at " + localBase.getAbsolutePath());
            // } else {
            // // logWarning("Created base dir at " + localBase, new
            // RuntimeException("here"));
            // }
        } else if (!localBase.isDirectory()) {
            if (!quite) {
                logSevere(" not able to create folder(" + getName()
                    + "), (sub) dir (" + localBase + ") is no dir");
            }
            throw new FolderException(currentInfo, Translation.getTranslation(
                "foldercreate.error.unable_to_open",
                localBase.getAbsolutePath()));
        }

        // Complex checks
        FolderRepository repo = getController().getFolderRepository();
        if (repo.getFoldersAbsoluteDir().equals(localBase)) {
            throw new FolderException(currentInfo, Translation.getTranslation(
                "foldercreate.error.it_is_base_dir",
                localBase.getAbsolutePath()));
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
            boolean ok = false;
            // Hack to solve the rarely occurring 0 byte directory files
            if (targetFile.getParentFile().isFile()
                && targetFile.getParentFile().length() == 0)
            {
                if (targetFile.getParentFile().delete()) {
                    ok = targetFile.getParentFile().mkdirs();
                }
            }
            if (!ok) {
                logSevere("Unable to scan downloaded file. Parent dir is not a directory: "
                    + targetFile + ". " + fInfo.toDetailString());
                return false;
            }
        }

        synchronized (scanLock) {
            // Prepare last modification date of tempfile.
            if (!tempFile.setLastModified(fInfo.getModifiedDate().getTime())) {
                logSevere("Failed to set modified date on " + tempFile
                    + " for " + fInfo.getModifiedDate().getTime());
                return false;
            }

            if (targetFile.exists()) {
                // if file was a "newer file" the file already exists here
                // Using local var because of possible race condition!!
                FileArchiver arch = archiver;
                if (arch != null) {
                    try {
                        FileInfo oldLocalFileInfo = fInfo
                            .getLocalFileInfo(getController()
                                .getFolderRepository());
                        if (oldLocalFileInfo != null) {
                            if (!currentInfo.isMetaFolder()
                                && ConfigurationEntry.CONFLICT_DETECTION
                                    .getValueBoolean(getController()))
                            {
                                try {
                                    doSimpleConflictDetection(fInfo,
                                        targetFile, oldLocalFileInfo);
                                } catch (Exception e) {
                                    logSevere("Problem withe conflict detection. "
                                        + e);
                                }
                            }

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
                // TODO: Set last modified only if required
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

            synchronized (dbAccessLock) {
                // Update internal database
                store(getController().getMySelf(), correctFolderInfo(fInfo));
                fileChanged(fInfo);
            }
        }
        return true;
    }

    private FileInfo doSimpleConflictDetection(FileInfo fInfo, File targetFile,
        FileInfo oldLocalFileInfo)
    {
        boolean conflict = oldLocalFileInfo.getVersion() == fInfo.getVersion()
            && fInfo.isNewerThan(oldLocalFileInfo)
            && (oldLocalFileInfo.getVersion() + fInfo.getVersion() != 0);
        conflict |= oldLocalFileInfo.getVersion() <= fInfo.getVersion()
            && DateUtil.isNewerFileDateCrossPlattform(
                oldLocalFileInfo.getModifiedDate(), fInfo.getModifiedDate());
        if (conflict) {
            logWarning("Conflict detected on file " + fInfo.toDetailString()
                + ". old: " + oldLocalFileInfo.toDetailString());
            // Really basic raw conflict detection.
            addProblem(new FileConflictProblem(fInfo));

            // String fn = fInfo.getFilenameOnly();
            // String extraInfo = "_";
            // extraInfo += oldLocalFileInfo.getModifiedBy().getNick();
            // extraInfo += "_";
            // extraInfo += oldLocalFileInfo.getVersion();
            // if (fn.contains(".")) {
            // int i = fn.lastIndexOf('.');
            // fn = fn.substring(0, i) + extraInfo
            // + fn.substring(i, fn.length());
            // } else {
            // fn += extraInfo;
            // }
            // File oldCopy = new File(targetFile.getParentFile(),
            // FileUtils.removeInvalidFilenameChars(fn));
            // FileInfo oldCopyFInfo = FileInfoFactory.lookupInstance(this,
            // oldCopy);
            // watcher.addIgnoreFile(oldCopyFInfo);
            // try {
            // FileUtils.copyFile(targetFile, oldCopy);
            // logInfo("Saved copy of conflicting file to " + oldCopy);
            // return scanChangedFile(oldCopyFInfo);
            // } catch (Exception e) {
            // logWarning("Unable to save old copy on conflict file to "
            // + oldCopy + ": " + e);
            // } finally {
            // watcher.removeIgnoreFile(oldCopyFInfo);
            //
            // }
        }
        return null;
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
            long secondsSinceLastSync = (System.currentTimeMillis() - wasLastScan
                .getTime()) / 1000;

            int items = getKnownItemCount();
            // Dynamically adapt fallback scan time.
            // The less files we have, the faster we scan.
            // 0 files = every 10 seconds (MIN)
            // 100 files = every minute
            // 1.000 files = every 10 minutes (MAX)
            // 30.000 files = every 10 minutes
            // If folder watch is not supported max is 1 minute.
            int frequency = (int) (60L * items / 100L);

            // Min
            if (frequency < TEN_SECONDS) {
                frequency = TEN_SECONDS;
            }

            // Max
            if (watcher.isSupported()) {
                if (frequency > TEN_MINUTES) {
                    frequency = TEN_MINUTES;
                }
            } else {
                if (frequency > ONE_MINUTE) {
                    frequency = ONE_MINUTE;
                }
            }

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
            FileInfo existinfFInfo = findSameFile(localFileInfo);
            if (existinfFInfo != null) {
                localFileInfo = existinfFInfo;
            }
            fileChanged(localFileInfo);
        }
        return localFileInfo;
    }

    /**
     * Scans all parent directories of a file. Useful after restoring single
     * files in deleted subdirs.
     * 
     * @param fileInfo
     */
    public void scanAllParentDirectories(FileInfo fileInfo) {
        FileInfo dirInfo = fileInfo.getDirectory();
        dirInfo = getFile(dirInfo);
        if (dirInfo == null || !dirInfo.isDeleted()) {
            // No need.
            return;
        }
        DirectoryInfo baseDir = getBaseDirectoryInfo();
        int i = 0;
        while (!dirInfo.equals(baseDir)) {
            if (isFiner()) {
                logFiner("Scanning parent dir: " + dirInfo);
            }
            scanChangedFile(dirInfo);
            dirInfo = dirInfo.getDirectory();
            if (i++ > 10000) {
                break;
            }
        }
    }

    /**
     * Scans multiple new, deleted or restored File callback for
     * {@link #getFolderWatcher()} only.
     * 
     * @param fileInfos
     *            the files to scan. ATTENTION: Does modify the {@link List}
     */
    void scanChangedFiles(final List<FileInfo> fileInfos) {
        Reject.ifNull(fileInfos, "FileInfo collection is null");
        boolean checkRevert = isRevertLocalChanges();
        int i = 0;
        for (Iterator<FileInfo> it = fileInfos.iterator(); it.hasNext();) {
            FileInfo fileInfo = (FileInfo) it.next();
            FileInfo localFileInfo = scanChangedFile0(fileInfo);
            if (localFileInfo == null) {
                // No change
                it.remove();
            } else {
                if (checkRevert && checkRevertLocalChanges(localFileInfo)) {
                    // No change
                    it.remove();
                } else {
                    // Allowed to change files
                    FileInfo existinfFInfo = findSameFile(localFileInfo);
                    if (existinfFInfo != null) {
                        localFileInfo = existinfFInfo;
                    }
                    fileInfos.set(i, localFileInfo);
                    i++;
                }
            }
        }
        if (!fileInfos.isEmpty()) {
            fireFilesChanged(fileInfos);
            setDBDirty();

            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo, fileInfos,
                        diskItemFilter, useExt);
                }
            });
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
        if (file.getName().equals(Constants.DB_FILENAME)
            || file.getName().equals(Constants.DB_BACKUP_FILENAME))
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
                            modifiedBy, modDate, fInfo.getVersion(),
                            file.isDirectory());
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

                FileInfo syncFile = localFile
                    .syncFromDiskIfRequired(this, file);
                if (syncFile != null) {
                    store(getController().getMySelf(), syncFile);
                    if (isFiner()) {
                        logFiner("Scan file changed: "
                            + syncFile.toDetailString());
                    }
                } else {
                    if (isFiner()) {
                        logFiner("Scan file unchanged: "
                            + localFile.toDetailString());
                    }
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

        if (dir.equals(getSystemSubDir0())) {
            logWarning("Ignoring system subdirectory: " + dir);
            return;
        }

        watcher.addIgnoreFile(dirInfo);
        try {
            synchronized (scanLock) {
                if (dirInfo.isDeleted()) {
                    if (!dir.delete()) {
                        logSevere("Unable to deleted directory: " + dir + ". "
                            + dirInfo.toDetailString());
                        return;
                    }
                } else {
                    // #2627 / ASR-771-79727
                    if (dir.exists() && dir.isFile() && dir.length() == 0) {
                        dir.delete();
                    }
                    dir.mkdirs();
                    dir.setLastModified(dirInfo.getModifiedDate().getTime());
                }
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
     * @return The new deleted FileInfo, null if unchanged.
     */
    private FileInfo removeFileLocal(FileInfo fInfo) {
        if (isFiner()) {
            logFiner("Remove file local: " + fInfo + ", Folder equal ? "
                + Util.equals(fInfo.getFolderInfo(), currentInfo));
        }
        if (!isKnown(fInfo)) {
            if (isWarning()) {
                logWarning("Tried to remove a unknown file: "
                    + fInfo.toDetailString());
            }
            return null;
        }

        // Abort transfers files
        if (fInfo.isFile()) {
            getController().getTransferManager().breakTransfers(fInfo);
        }

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
                    return null;
                }
                FileInfo localFile = getFile(fInfo);
                FileInfo synced = localFile.syncFromDiskIfRequired(this,
                    diskFile);
                folderChanged = synced != null;
                if (folderChanged) {
                    store(getController().getMySelf(), synced);
                    return synced;
                }
            }
        }

        return null;
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
        Reject.ifNull(fInfos, "Files null");
        if (fInfos.isEmpty()) {
            return;
        }
        final List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        Comparator<FileInfo> comparator = new ReverseComparator<FileInfo>(
            FileInfoComparator
                .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        Set<FileInfo> dirs = new TreeSet<FileInfo>(comparator);
        synchronized (scanLock) {
            for (FileInfo fileInfo : fInfos) {
                if (fileInfo.isDiretory()) {
                    dirs.add(fileInfo);
                    continue;
                }
                FileInfo deletedFileInfo = removeFileLocal(fileInfo);
                if (deletedFileInfo != null) {
                    removedFiles.add(deletedFileInfo);
                }
            }
            for (FileInfo dirInfo : dirs) {
                FileInfoCriteria c = new FileInfoCriteria();
                c.addMySelf(this);
                c.setPath((DirectoryInfo) dirInfo);
                logInfo("Deleting directory: " + dirInfo);
                removeFilesLocal(dao.findFiles(c));
                FileInfo deletedDirInfo = removeFileLocal(dirInfo);
                if (deletedDirInfo != null) {
                    removedFiles.add(deletedDirInfo);
                }
            }
        }

        if (!removedFiles.isEmpty()) {
            fireFilesDeleted(removedFiles);
            setDBDirty();

            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo, removedFiles,
                        diskItemFilter, useExt);
                }
            });
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
    @SuppressWarnings({"unchecked"})
    private boolean loadFolderDB(File dbFile) {
        synchronized (scanLock) {
            if (!dbFile.exists()) {
                logFine(this + ": Database file not found: "
                    + dbFile.getAbsolutePath());
                return false;
            }
            try {
                // load files and scan in
                InputStream fIn = new BufferedInputStream(new TFileInputStream(
                    dbFile));
                ObjectInputStream in = new ObjectInputStream(fIn);
                FileInfo[] files = (FileInfo[]) in.readObject();
                // Convert.cleanMemberInfos(getController().getNodeManager(),
                // files);
                synchronized (dbAccessLock) {
                    for (int i = 0; i < files.length; i++) {
                        FileInfo fInfo = files[i];
                        files[i] = correctFolderInfo(fInfo);
                        if (fInfo != files[i]) {
                            // Instance has changes.
                            setDBDirty();
                        }
                    }
                    // Help with initial capacity info.
                    dao.deleteDomain(null, files.length);
                    dao.store(null, files);
                }

                // read them always ..
                MemberInfo[] members1 = (MemberInfo[]) in.readObject();
                // Do not load members
                logFiner("Loading " + members1.length + " members");
                for (MemberInfo memberInfo : members1) {
                    Member member = memberInfo.getNode(getController(), true);
                    if (member.isMySelf()) {
                        continue;
                    }
                    join0(member, true);
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
                    + dbFile.getAbsolutePath() + ". " + e);
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
        diskItemFilter.loadPatternsFrom(new TFile(getSystemSubDir0(),
            DiskItemFilter.PATTERNS_FILENAME), false);
    }

    /**
     * Loads the folder db from disk
     */
    private void loadFolderDB() {
        if (loadFolderDB(new TFile(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR + '/' + Constants.DB_FILENAME)))
        {
            return;
        }

        if (loadFolderDB(new TFile(localBase,
            Constants.POWERFOLDER_SYSTEM_SUBDIR + '/'
                + Constants.DB_BACKUP_FILENAME)))
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
            diskItemFilter.savePatternsTo(new TFile(getSystemSubDir(),
                DiskItemFilter.PATTERNS_FILENAME), true);
            savePatternsToMetaFolder();
        }
        dao.stop();
        removeAllListeners();
        ListenerSupportFactory.removeAllListeners(folderListenerSupport);
        ListenerSupportFactory
            .removeAllListeners(folderMembershipListenerSupport);
        diskItemFilter.removeAllListener();
        if (encrypted && !currentInfo.isMetaFolder()) {
            try {
                TFile.umount(localBase);
            } catch (Exception e) {
                logWarning("Problem unmounting " + localBase + ". " + e);
            }
        }
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
        File dbFile = new TFile(getSystemSubDir(), Constants.DB_FILENAME);
        File dbFileBackup = new TFile(getSystemSubDir(),
            Constants.DB_BACKUP_FILENAME);
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
            OutputStream fOut = new BufferedOutputStream(new TFileOutputStream(
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
            File oldDbFile = new TFile(localBase, Constants.DB_FILENAME);
            if (!oldDbFile.delete()) {
                logFiner("Failed to delete 'old' database file: " + oldDbFile);
            }
            File oldDbFileBackup = new TFile(localBase,
                Constants.DB_BACKUP_FILENAME);
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
     * <p>
     * Also: #2759 Check sync consistency
     * 
     * @param removeBefore
     */
    public void maintainFolderDB(long removeBefore) {
        Date removeBeforeDate = new Date(removeBefore);
        int nFilesBefore = getKnownItemCount();
        if (isFiner()) {
            logFiner("Maintaining folder db, known files: " + nFilesBefore
                + ". Expiring deleted files older than " + removeBeforeDate);
        }
        int expired = 0;
        int keepDeleted = 0;
        int total = 0;
        List<FileInfo> brokenExisting = new LinkedList<FileInfo>();
        for (FileInfo file : dao.findAllFiles(null)) {
            total++;
            if (!file.isDeleted()) {
                // #2759 Check sync consistency
                for (Member member : members.keySet()) {
                    FileInfo remoteFile = member.getFile(file);
                    if (remoteFile == null
                        || remoteFile.getVersion() != file.getVersion())
                    {
                        continue;
                    }
                    // Same version
                    if (remoteFile.isVersionDateAndSizeIdentical(file)) {
                        // File is ok
                        continue;
                    }
                    boolean sameDate = remoteFile.getModifiedDate().equals(
                        file.getModifiedDate());
                    boolean remoteOlder = !sameDate
                        && remoteFile.getModifiedDate().before(
                            file.getModifiedDate());
                    boolean remoteSmaller = remoteFile.getSize() < file
                        .getSize();
                    if (remoteOlder || (sameDate && remoteSmaller)) {
                        // Our version is "better" newer or bigger.
                        // Increase version to force re-sync
                        if (!brokenExisting.contains(file)) {
                            logWarning("Fixing file entry. Local: "
                                + file.toDetailString() + ".\n@"
                                + member.getNick() + ": "
                                + remoteFile.toDetailString());
                            brokenExisting.add(file);
                        }
                    }
                }
                continue;
            }
            if (file.getModifiedDate().before(removeBeforeDate)) {
                // Don't remove. We have archived files.
                if (archiver.hasArchivedFileInfo(file)) {
                    continue;
                }
                expired++;
                // Remove
                dao.delete(null, file);
                for (Member member : members.values()) {
                    dao.delete(member.getId(), file);
                }
                if (isFiner()) {
                    logFiner("FileInfo expired: " + file.toDetailString());
                }
            } else {
                keepDeleted++;
            }
        }
        if (!brokenExisting.isEmpty()) {
            for (int i = 0; i < brokenExisting.size(); i++) {
                FileInfo fileInfo = brokenExisting.get(i);
                FileInfo newFileInfo = FileInfoFactory.unmarshallExistingFile(
                    currentInfo, fileInfo.getRelativeName(),
                    fileInfo.getSize(), fileInfo.getModifiedBy(),
                    fileInfo.getModifiedDate(), fileInfo.getVersion() + 1,
                    fileInfo.isDiretory());
                brokenExisting.set(i, newFileInfo);
            }
            store(getController().getMySelf(), brokenExisting);
            filesChanged(brokenExisting);
        }

        if (expired > 0 || brokenExisting.size() > 0) {
            setDBDirty();
            logInfo("Maintained folder db, " + nFilesBefore + " known files, "
                + expired + " expired FileInfos, " + brokenExisting.size()
                + " fixed entries. Expiring deleted files older than "
                + removeBeforeDate);
            statistic.scheduleCalculate();
        } else if (isFiner()) {
            logFiner("Maintained folder db, " + nFilesBefore + " known files, "
                + expired
                + " expired FileInfos. Expiring deleted files older than "
                + removeBeforeDate);
        }
        lastDBMaintenance = new Date();

        if (total > 75025 && total - keepDeleted * 2 < 0) {
            addProblem(new FolderDatabaseProblem(currentInfo));
        }

        // Also maintain meta folder
        Folder mFolder = getController().getFolderRepository()
            .getMetaFolderForParent(currentInfo);
        if (mFolder != null) {
            mFolder.maintainFolderDB(removeBefore);
        }
    }

    /**
     * #2311: Revert local changes.
     * 
     * @return
     */
    private boolean isRevertLocalChanges() {
        boolean mySelfReadOnly = hasReadPermission(getController().getMySelf())
            && !hasWritePermission(getController().getMySelf());
        return mySelfReadOnly || syncProfile.equals(SyncProfile.BACKUP_TARGET);
    }

    private void checkRevertLocalChanges() {
        if (!isRevertLocalChanges()) {
            return;
        }
        if (isFine()) {
            logFine("Checking revert on my files");
        }
        boolean reverted = false;
        for (FileInfo fileInfo : dao.findAllFiles(null)) {
            if (checkRevertLocalChanges(fileInfo)) {
                reverted = true;
            }
        }
        if (reverted) {
            getController().getFolderRepository().getFileRequestor()
                .triggerFileRequesting(currentInfo);
            syncRemoteDeletedFiles(false);
        }
    }

    private boolean checkRevertLocalChanges(FileInfo fileInfo) {
        FileInfo newestVersion = fileInfo.getNewestVersion(getController()
            .getFolderRepository());
        if (newestVersion != null && !fileInfo.isNewerThan(newestVersion)) {
            // Ok in sync
            return false;
        }
        getFolderWatcher().addIgnoreFile(fileInfo);
        try {
            if (newestVersion == null) {
                logWarning("Reverting local change: "
                    + fileInfo.toDetailString() + ". File not in repository.");
            } else {
                logWarning("Reverting local change: "
                    + fileInfo.toDetailString() + ". Found newer version: "
                    + newestVersion.toDetailString());
            }
            File file = fileInfo.getDiskFile(getController()
                .getFolderRepository());
            synchronized (scanLock) {
                if (file.exists()) {
                    try {
                        archiver.archive(fileInfo, file, false);
                    } catch (IOException e) {
                        logWarning("Unable to revert changes on file " + file
                            + ". Cannot overwrite local change. " + e);
                        return false;
                    }
                }
                dao.delete(null, fileInfo);
            }
            return true;
        } finally {
            getFolderWatcher().removeIgnoreFile(fileInfo);
        }
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
        String shortCutName = getName();
        if (getController().isVerbose()) {
            shortCutName = '[' + getController().getMySelf().getNick() + "] "
                + shortCutName;
        }

        if (active) {
            return Util.createDesktopShortcut(shortCutName,
                localBase.getAbsoluteFile());
        }

        // Remove shortcuts to folder if not wanted
        Util.removeDesktopShortcut(shortCutName);
        return false;
    }

    /**
     * Deletes the desktop shortcut of the folder if set in prefs.
     */
    public void removeDesktopShortcut() {
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
        String confKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
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
        if (syncProfile.equals(aSyncProfile)) {
            // Skip.
            return;
        }
        logFine("Setting " + aSyncProfile.getName());
        Reject.ifTrue(previewOnly,
            "Can not change Sync Profile in Preview mode.");
        syncProfile = aSyncProfile;

        if (!currentInfo.isMetaFolder()) {
            String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
                + FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;
            getController().getConfig().put(syncProfKey,
                syncProfile.getFieldList());
            getController().saveConfig();
        }

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
        return !syncProfile.isManualSync() && !syncProfile.isDailySync()
            && !getController().isPaused();
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
            if (scanLocalFiles()) {
                checkRevertLocalChanges();
            }
        }
        if (maintainFolderDBrequired()) {
            long removeBefore = System.currentTimeMillis()
                - 1000L
                * ConfigurationEntry.MAX_FILEINFO_DELETED_AGE_SECONDS
                    .getValueInt(getController());
            maintainFolderDB(removeBefore);
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
     * Join a Member from its MemberInfo
     * 
     * @param memberInfo
     */
    private boolean join0(MemberInfo memberInfo) {
        if (memberInfo.isInvalid(getController())) {
            return false;
        }
        Member member = memberInfo.getNode(getController(), true);
        if (member.isMySelf()) {
            return false;
        }
        Date deadLine = new Date(System.currentTimeMillis()
            - Constants.NODE_TIME_TO_REMOVE_MEMBER);
        boolean offline2Long = memberInfo.getLastConnectTime() == null
            || memberInfo.getLastConnectTime().before(deadLine);
        if (offline2Long) {
            logFine(member + " was offline too long. "
                + "Hiding in memberslist: " + member + " last seen online: "
                + memberInfo.getLastConnectTime());
            return false;
        }
        // Ok let him join
        return join(member);
    }

    /**
     * Joins a member to the folder,
     * 
     * @param member
     * @return true if actually joined the folder.
     */
    public boolean join(Member member) {
        boolean memberRead = hasReadPermission(member);
        boolean mySelfRead = hasReadPermission(getController().getMySelf());
        if (!memberRead || !mySelfRead) {
            if (memberRead) {
                if (isFine()) {
                    String msg = "Not joining " + member + " / "
                        + member.getAccountInfo()
                        + ". Myself got no read permission";
                    if (getController().isStarted()
                        && member.isCompletelyConnected()
                        && getController().getOSClient().isConnected())
                    {
                        logWarning(msg);
                    } else {
                        logFine(msg);
                    }
                }
            } else {
                if (isFine()) {
                    String msg = "Not joining " + member + " / "
                        + member.getAccountInfo() + " no read permission";
                    if (getController().isStarted()
                        && member.isCompletelyConnected()
                        && getController().getOSClient().isConnected())
                    {
                        logWarning(msg);
                    } else {
                        logFine(msg);
                    }
                }
            }
            if (member.isCompletelyConnected()) {
                member.sendMessagesAsynchron(FileList.createEmpty(currentInfo,
                    supportExternalizable(member)));
            }
            return false;
        }
        join0(member, false);
        return true;
    }

    /**
     * Joins a member to the folder.
     * 
     * @param member
     */
    private boolean join0(Member member, boolean init) {
        Reject.ifNull(member, "Member is null, unable to join");
        // member will be joined, here on local
        boolean wasMember = members.put(member, member) != null;
        if (!wasMember && isInfo() && !init) {
            logInfo("Member joined " + member);
        }
        if (!init) {
            if (!wasMember && member.isCompletelyConnected()) {
                // FIX for #924
                waitForScan();

                Message[] filelistMsgs = FileList.create(this,
                    supportExternalizable(member));
                member.sendMessagesAsynchron(filelistMsgs);
            }
            if (!wasMember) {
                // Fire event if this member is new
                fireMemberJoined(member);
                updateMetaFolderMembers();
                // Persist new members list
                setDBDirty();
            }
        }
        return !wasMember;
    }

    /**
     * Merge members with metafolder Members file and write back if there was a
     * change. Also join any new members found in the file.
     */
    public void updateMetaFolderMembers() {
        // Only do this for parent folders.
        if (currentInfo.isMetaFolder()) {
            return;
        }
        FolderRepository folderRepository = getController()
            .getFolderRepository();
        Folder metaFolder = folderRepository
            .getMetaFolderForParent(currentInfo);
        if (metaFolder == null) {
            // May happen at startup
            logFine("Could not yet find metaFolder for " + currentInfo);
            return;
        }
        if (metaFolder.deviceDisconnected) {
            logFine("Not writing members. Meta folder disconnected.");
            return;
        }
        // Update in the meta directory.
        File file = new TFile(metaFolder.localBase, METAFOLDER_MEMBERS);
        FileInfo fileInfo = FileInfoFactory.lookupInstance(metaFolder, file);
        // Read in.
        Map<String, MemberInfo> membersMap = readMetaFolderMembers(fileInfo);
        Map<String, MemberInfo> originalMap = new HashMap<String, MemberInfo>();
        originalMap.putAll(membersMap);
        // Update members with any new ones from this file.
        for (MemberInfo memberInfo : membersMap.values()) {
            Member memberCanidate = memberInfo.getNode(getController(), true);
            if (members.containsKey(memberCanidate)) {
                continue;
            }
            if (!memberInfo.isOnSameNetwork(getController())) {
                continue;
            }
            if (join0(memberInfo)) {
                logInfo("Discovered new Member " + memberInfo);
            }
        }
        // Update members map with my members.
        for (Member member : members.keySet()) {
            membersMap.put(member.getId(), member.getInfo());
        }
        // See if there has been a change to the members map.
        boolean changed = false;
        if (originalMap.size() == membersMap.size()) {
            for (String s : membersMap.keySet()) {
                if (!originalMap.containsKey(s)) {
                    changed = true;
                    break;
                }
            }
        } else {
            changed = true;
        }
        if (changed && !checkIfDeviceDisconnected()) {
            // Write back and scan.
            writewMetaFolderMembers(membersMap, fileInfo);
            metaFolder.scanChangedFile(fileInfo);
        }
    }

    /**
     * Read the metafolder Members file from disk. It is a Map<String,
     * MemberInfo>.
     * 
     * @param fileInfo
     * @return
     */
    @SuppressWarnings({"unchecked"})
    private Map<String, MemberInfo> readMetaFolderMembers(FileInfo fileInfo) {
        if (isFine()) {
            logFine("Loading metafolder members from " + fileInfo + '.');
        }
        Map<String, MemberInfo> membersMap = new TreeMap<String, MemberInfo>();
        InputStream is = null;
        ObjectInputStream ois = null;
        File f = fileInfo.getDiskFile(getController().getFolderRepository());
        if (!f.exists()) {
            return membersMap;
        }
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            ois = new ObjectInputStream(is);
            membersMap.putAll((Map<String, MemberInfo>) ois.readObject());
        } catch (IOException e) {
            logWarning("Unable to read members file " + fileInfo + ". " + e);
        } catch (ClassNotFoundException e) {
            logWarning("Unable to read members file " + fileInfo + ". " + e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // Don't care.
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Don't care.
                }
            }
        }
        if (isFine()) {
            logFine("Loaded " + membersMap.size() + " metafolder members.");
        }
        return membersMap;
    }

    /**
     * Write the metafolder Members file with all known members.
     * 
     * @param membersMap
     * @param fileInfo
     */
    private void writewMetaFolderMembers(Map<String, MemberInfo> membersMap,
        FileInfo fileInfo)
    {
        if (isFine()) {
            logFine("Saving " + membersMap.size() + " metafolder member(s) to "
                + fileInfo + '.');
        }
        if (isFiner()) {
            for (MemberInfo memberInfo : membersMap.values()) {
                logFiner("Saved " + memberInfo.getNick());
            }
        }

        OutputStream os = null;
        ObjectOutputStream oos = null;
        try {
            os = new BufferedOutputStream(new TFileOutputStream(
                fileInfo.getDiskFile(getController().getFolderRepository())));
            oos = new ObjectOutputStream(os);
            oos.writeObject(membersMap);
        } catch (IOException e) {
            logSevere(e);
        } finally {
            if (oos != null) {
                try {
                    oos.flush();
                    oos.close();
                } catch (IOException e) {
                    // Don't care.
                }
            }
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    // Don't care.
                }
            }
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
        dao.deleteDomain(member.getId(), -1);

        // Fire event
        fireMemberLeft(member);
        // updateMetaFolderMembers();
        // TODO: Trigger file requestor. Other folders may have files to
        // download.
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
        setDBDirty();
    }

    /**
     * @return true if this folder has beend start. false if shut down
     */
    public boolean isStarted() {
        return !shutdown;
    }

    /**
     * In sync = Folders is 100% synced and all syncing actions (
     * {@link #isSyncing()}) have stopped.
     * 
     * @return true if this folder is 100% in sync
     */
    public boolean isInSync() {
        if (isSyncing()) {
            return false;
        }
        return statistic.getHarmonizedSyncPercentage() >= 100.0d;
    }

    /**
     * Checks if the folder is syncing. Means: local file scan running or active
     * transfers.
     * 
     * @return if this folder is currently synchronizing.
     */
    public boolean isSyncing() {
        return isScanning()
            || isTransferring()
            || getController().getFolderRepository()
                .getCurrentlyMaintainingFolder() == this;
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
     * @param collection
     *            selected members to sync deletions with
     * @param force
     */
    public void triggerSyncRemoteDeletedFiles(
        final Collection<Member> collection, final boolean force)
    {
        getController().getIOProvider().startIO(new Runnable() {
            public void run() {
                syncRemoteDeletedFiles(collection, force);
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
    public void syncRemoteDeletedFiles(Collection<Member> collection,
        boolean force)
    {
        if (collection.isEmpty()) {
            // Skip.
            return;
        }
        if (isFine()) {
            logFine("Deleting files, which are deleted by friends. con-members: "
                + Arrays.asList(getConnectedMembers()));
        }

        final List<FileInfo> removedFiles = new ArrayList<FileInfo>();
        // synchronized (scanLock) {
        for (Member member : collection) {
            if (!member.isCompletelyConnected()) {
                // disconnected go to next member
                continue;
            }
            if (!hasWritePermission(member)) {
                if (isFine()) {
                    logFine("Not syncing deletions. " + member + " / "
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
                    handleFileDeletion(remoteFile, force, member, removedFiles,
                        0);
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
                Collections.sort(
                    list,
                    new ReverseComparator<FileInfo>(FileInfoComparator
                        .getComparator(FileInfoComparator.BY_RELATIVE_NAME)));
                synchronized (scanLock) {
                    for (FileInfo remoteDir : list) {
                        handleFileDeletion(remoteDir, force, member,
                            removedFiles, 0);
                    }
                }
            }
        }
        // }

        // Broadcast folder change if changes happend
        if (!removedFiles.isEmpty()) {
            fireFilesDeleted(removedFiles);
            setDBDirty();

            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo, removedFiles,
                        diskItemFilter, useExt);
                }
            });
        }
    }

    private void handleFileDeletion(FileInfo remoteFile, boolean force,
        Member member, List<FileInfo> removedFiles, int nTried)
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
            return;
        }
        if (localFile.isDeleted()) {
            if (remoteFile.isNewerThan(localFile)) {
                if (isFine()) {
                    logFine("Taking over deletion file info: "
                        + remoteFile.toDetailString());
                }
                // Take over modification infos
                remoteFile = correctFolderInfo(remoteFile);
                store(getController().getMySelf(), remoteFile);
                localFile = getFile(remoteFile);
                removedFiles.add(localFile);
            }
            return;
        }

        // Local file NOT deleted / still existing. So do a local delete
        File localCopy = localFile.getDiskFile(getController()
            .getFolderRepository());
        if (!localFile.inSyncWithDisk(localCopy)) {
            if (isFine()) {
                logFine("Not deleting file from member " + member
                    + ", local file not in sync with disk: "
                    + localFile.toDetailString() + " at "
                    + localCopy.getAbsolutePath());
            }

            if (scanAllowedNow() && scanChangedFile(localFile) != null
                && nTried < 10)
            {
                // Scan an trigger a sync of deletions later (again).
                handleFileDeletion(remoteFile, force, member, removedFiles,
                    ++nTried);
            }
            return;
        }

        if (isFine()) {
            logFine("File was deleted by " + remoteFile.getModifiedBy()
                + ", deleting local: " + localFile.toDetailString() + " at "
                + localCopy.getAbsolutePath());
        }

        // Abort transfers on file.
        if (remoteFile.isFile()) {
            getController().getTransferManager().breakTransfers(remoteFile);
        }

        if (localCopy.exists()) {
            synchronized (scanLock) {
                if (localFile.isDiretory()) {
                    if (isFine()) {
                        logFine("Deleting directory from remote: "
                            + localFile.toDetailString());
                    }
                    watcher.addIgnoreFile(localFile);
                    try {
                        if (!localCopy.delete()) {
                            // #1977
                            String[] remaining = localCopy.list();
                            if (remaining != null) {
                                for (String path : remaining) {
                                    String pathL = path.toLowerCase();
                                    if (pathL.endsWith("thumbs.db")
                                        || pathL.endsWith(".ds_store")
                                        || pathL.endsWith("desktop.ini"))
                                    {
                                        new TFile(path).delete();
                                    }
                                }
                                if (!localCopy.delete()) {
                                    if (isWarning()) {
                                        remaining = localCopy.list();
                                        String contentStr = remaining != null
                                            ? Arrays.asList(remaining)
                                                .toString()
                                            : "(unable to access)";
                                        logWarning("Unable to delete directory locally: "
                                            + localCopy
                                            + ". Info: "
                                            + localFile.toDetailString()
                                            + ". contents: " + contentStr);
                                    }
                                    // Skip. Dir was not actually deleted /
                                    // could
                                    // not
                                    // sync
                                    return;
                                }
                            }
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
     * Broadcasts a message through the folder.
     * <p>
     * Caches the built messages.
     * 
     * @param msgProvider
     */
    public void broadcastMessages(MessageProducer msgProvider) {
        Message[] msgs = null;
        Message[] msgsExt = null;
        for (Member member : getMembersAsCollection()) {
            // Connected?
            if (member.isCompletelyConnected()) {
                if (supportExternalizable(member)) {
                    if (msgsExt == null) {
                        msgsExt = msgProvider.getMessages(true);
                    }
                    if (msgsExt != null && msgsExt.length > 0) {
                        member.sendMessagesAsynchron(msgsExt);
                    }
                } else {
                    if (msgs == null) {
                        msgs = msgProvider.getMessages(false);
                    }
                    if (msgs != null && msgs.length > 0) {
                        member.sendMessagesAsynchron(msgs);
                    }
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
        logInfo("Reading syncPatterns " + syncPatternsFile);
        diskItemFilter.loadPatternsFrom(syncPatternsFile, true);
        // Trigger resync
        getController().getTransferManager().checkActiveTranfersForExcludes();
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(currentInfo);
    }

    public DirectoryInfo getBaseDirectoryInfo() {
        return FileInfoFactory.createBaseDirectoryInfo(currentInfo);
    }

    /**
     * Broadcasts the remote command to scan the folder.
     */
    public void broadcastScanCommand() {
        if (isFiner()) {
            logFiner("Broadcasting remote scan command");
        }
        Message commando = new ScanCommand(currentInfo);
        broadcastMessages(commando);
    }

    /**
     * Broadcasts the remote command to requests files of the folder.
     */
    public void broadcastFileRequestCommand() {
        if (isFiner()) {
            logFiner("Broadcasting remote file request command");
        }
        Message commando = new FileRequestCommand(currentInfo);
        broadcastMessages(commando);
    }

    private void broadcastFolderChanges(final ScanResult scanResult) {
        if (getConnectedMembersCount() == 0) {
            return;
        }

        if (!scanResult.getNewFiles().isEmpty()) {
            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo,
                        scanResult.getNewFiles(), diskItemFilter, useExt);
                }
            });
        }
        if (!scanResult.getChangedFiles().isEmpty()) {
            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo,
                        scanResult.getChangedFiles(), diskItemFilter, useExt);
                }
            });
        }
        if (!scanResult.getDeletedFiles().isEmpty()) {
            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo,
                        scanResult.getDeletedFiles(), diskItemFilter, useExt);
                }
            });
        }
        if (!scanResult.getRestoredFiles().isEmpty()) {
            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(currentInfo,
                        scanResult.getRestoredFiles(), diskItemFilter, useExt);
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
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {
            checkForMassDeletion(from, newList.files);
        }

        // Update DAO
        if (newList.isNull()) {
            // Delete files in domain and do nothing
            dao.deleteDomain(from.getId(), -1);
            return;
        }
        // Store but also deleted/clear domain before.
        int expectedItems = newList.nFollowingDeltas * newList.files.length;
        store(from, expectedItems, newList.files);

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

        // Logging
        writeFilelist(from);

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
        if (changes.getFiles() != null) {
            for (int i = 0; i < changes.getFiles().length; i++) {
                FileInfo fInfo = changes.getFiles()[i];
                changes.getFiles()[i] = FileInfoFactory.changedFolderInfo(
                    fInfo, currentInfo);
            }
        }
        if (changes.getRemoved() != null) {
            for (int i = 0; i < changes.getRemoved().length; i++) {
                FileInfo fInfo = changes.getRemoved()[i];
                changes.getRemoved()[i] = FileInfoFactory.changedFolderInfo(
                    fInfo, currentInfo);
            }
        }

        // #1022 - Mass delete detection. Switch to a safe profile if
        // a large percent of files would get deleted by another node.
        if (changes.getFiles() != null
            && syncProfile.isSyncDeletion()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {
            checkForMassDeletion(from, changes.getFiles());
        }
        if (changes.getRemoved() != null
            && syncProfile.isSyncDeletion()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())
            && ConfigurationEntry.MASS_DELETE_PROTECTION
                .getValueBoolean(getController()))
        {
            checkForMassDeletion(from, changes.getRemoved());
        }

        // Try to find same files
        if (changes.getFiles() != null) {
            store(from, changes.getFiles());
            findSameFiles(from, Arrays.asList(changes.getFiles()));
        }
        if (changes.getRemoved() != null) {
            store(from, changes.getRemoved());
            findSameFiles(from, Arrays.asList(changes.getRemoved()));
        }

        // Avoid hammering of sync remote deletion
        boolean singleExistingFileMsg = changes.getFiles() != null
            && changes.getFiles().length == 1
            && !changes.getFiles()[0].isDeleted();

        if (syncProfile.isAutodownload()) {
            // Check if we need to trigger the filerequestor
            boolean triggerFileRequestor = from.isCompletelyConnected();
            if (triggerFileRequestor && singleExistingFileMsg) {
                // This was caused by a completed download
                // TODO Maybe check this also on bigger lists!
                FileInfo localfileInfo = getFile(changes.getFiles()[0]);
                FileInfo remoteFileInfo = changes.getFiles()[0];
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
        if (!singleExistingFileMsg && syncProfile.isSyncDeletion()
            && from.isCompletelyConnected())
        {
            syncRemoteDeletedFiles(Collections.singleton(from), false);
        }

        // Fire event
        fireRemoteContentsChanged(from, changes);
    }

    private void store(Member member, FileInfo... fileInfos) {
        store(member, -1, fileInfos);
    }

    private void store(Member member, int newDomainSize, FileInfo... fileInfos)
    {
        store(member, newDomainSize, Arrays.asList(fileInfos));
    }

    private void store(Member member, Collection<FileInfo> fileInfos) {
        store(member, -1, fileInfos);
    }

    private void store(Member member, int newDomainSize,
        Collection<FileInfo> fileInfos)
    {
        synchronized (dbAccessLock) {
            String domainID = member.isMySelf() ? null : member.getId();
            if (newDomainSize > 0) {
                dao.deleteDomain(domainID, newDomainSize);
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
                if (isFiner()) {
                    logFiner("FolderFilesChanged delete percentage "
                        + delPercentage + '%');
                }
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
     * 3. The remote file version is > 0 and is not deleted
     * <p>
     * This if files moved from node to node without PowerFolder. e.g. just copy
     * over windows share. Helps to identifiy same files and prevents unessesary
     * downloads.
     * 
     * @param remoteFileInfos
     */
    private boolean findSameFiles(Member member,
        Collection<FileInfo> remoteFileInfos)
    {
        Reject.ifNull(remoteFileInfos, "Remote file info list is null");
        if (isFiner()) {
            logFiner("Triing to find same files in remote list with "
                + remoteFileInfos.size() + " files from " + member);
        }

        Boolean hasWrite = null;
        List<FileInfo> found = new LinkedList<FileInfo>();
        for (FileInfo remoteFileInfo : remoteFileInfos) {
            FileInfo localFileInfo = getFile(remoteFileInfo);
            if (localFileInfo == null) {
                continue;
            }

            if (localFileInfo.isDeleted() != remoteFileInfo.isDeleted()) {
                continue;
            }

            boolean fileSizeSame = localFileInfo.getSize() == remoteFileInfo
                .getSize();
            boolean dateSame = DateUtil.equalsFileDateCrossPlattform(
                localFileInfo.getModifiedDate(),
                remoteFileInfo.getModifiedDate());
            boolean fileCaseSame = localFileInfo.getRelativeName().equals(
                remoteFileInfo.getRelativeName());

            if (localFileInfo.getVersion() < remoteFileInfo.getVersion()
                && remoteFileInfo.getVersion() > 0)
            {
                // boolean localFileNewer = Util.isNewerFileDateCrossPlattform(
                // localFileInfo.getModifiedDate(), remoteFileInfo
                // .getModifiedDate());
                if (fileSizeSame && dateSame) {
                    if (hasWrite == null) {
                        hasWrite = hasWritePermission(member);
                    }
                    if (!hasWrite) {
                        if (isFine()) {
                            logFine("Not searching same files. " + member
                                + " / " + member.getAccountInfo()
                                + " no write permission");
                        }
                        return false;
                    }

                    if (isFine()) {
                        logFine("Found identical file remotely: local "
                            + localFileInfo.toDetailString() + " remote: "
                            + remoteFileInfo.toDetailString()
                            + ". Taking over modification infos");
                    }

                    // localFileInfo.copyFrom(remoteFileInfo);
                    found.add(remoteFileInfo);
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
                    if (hasWrite == null) {
                        hasWrite = hasWritePermission(member);
                    }
                    if (!hasWrite) {
                        if (isInfo()) {
                            logInfo("Not searching same files. " + member
                                + " / " + member.getAccountInfo()
                                + " no write permission");
                        }
                        return false;
                    }

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
                    found.add(remoteFileInfo);
                }
            }
        }

        if (!found.isEmpty()) {
            store(getController().getMySelf(), found);
            filesChanged(found);
            return true;
        }
        return false;
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
     * @param fInfo
     * @return the remotely found better fileinfo. null if not found.
     */
    private FileInfo findSameFile(FileInfo fInfo) {
        for (Member member : getConnectedMembers()) {
            FileInfo remoteFInfo = member.getFile(fInfo);
            if (remoteFInfo != null) {
                if (findSameFiles(member, Collections.singleton(remoteFInfo))) {
                    return fInfo.getLocalFileInfo(getController()
                        .getFolderRepository());
                }
            }
        }
        return null;
    }

    /**
     * TRAC #2072
     * 
     * @param member
     * @return if this member supports the {@link Externalizable} versions of
     *         {@link FileList} and {@link FolderFilesChanged}
     */
    public boolean supportExternalizable(Member member) {
        return member.getProtocolVersion() >= 105;
    }

    /**
     * Sets the DB to a dirty state. e.g. through a change. gets stored on next
     * persisting run.
     */
    private void setDBDirty() {
        dirty = true;
    }

    /**
     * Triggers the persisting.
     */
    private void triggerPersist() {
        getController().schedule(persister, 1000L);
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

        if ((hasOwnDatabase || getKnownItemCount() > 0)
            && !getSystemSubDir0().exists())
        {
            logWarning("Not storting folder database. Local system directory does not exists: "
                + getLocalBase());
            return;
        }

        storeFolderDB();

        // Write filelist
        if (LoggingManager.isLogToFile()
            && Feature.DEBUG_WRITE_FILELIST_CSV.isEnabled())
        {
            // And members' filelists.
            for (Member member : members.keySet()) {
                writeFilelist(member);
            }
        }
        dirty = false;
    }

    private void writeFilelist(Member member) {
        if (!LoggingManager.isLogToFile()
            || Feature.DEBUG_WRITE_FILELIST_CSV.isDisabled())
        {
            return;
        }
        // Write filelist to disk
        Debug.writeFileListCSV(getName(), member.getNick(),
            dao.findAllFiles(member.getId()), "FileList of folder " + getName()
                + ", member " + member + ':');
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
        String confKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
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
        File systemSubDir = getSystemSubDir0();
        if (!systemSubDir.exists()) {
            if (!checkIfDeviceDisconnected() && systemSubDir.mkdirs()) {
                // logWarning("Create local directory at: " + systemSubDir,
                // new RuntimeException("here"));
                FileUtils.setAttributesOnWindows(systemSubDir, true, true);
            } else if (!deviceDisconnected) {
                logSevere("Failed to create system subdir: " + systemSubDir);
            } else if (isFine()) {
                logFine("Failed to create system subdir: " + systemSubDir);
            }
        }
        return systemSubDir;
    }

    private File getSystemSubDir0() {
        return new TFile(localBase, Constants.POWERFOLDER_SYSTEM_SUBDIR);
    }

    /**
     * Is this directory the system subdirectory?
     * 
     * @param aDir
     * @return
     */
    public boolean isSystemSubDir(File aDir) {
        return aDir.isDirectory()
            && getSystemSubDir0().getAbsolutePath().equals(
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
            checkBaseDir(true);
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
                if (deviceDisconnected) {
                    addProblem = false;
                } else {
                    logInfo("Device connected again");
                    removeProblem(problem);
                }
            }
        }
        if (addProblem) {
            logInfo("Device disconnected. Folder disappeared from "
                + getLocalBase());
            String bd = getController().getFolderRepository()
                .getFoldersBasedir();
            boolean inBaseDir = false;
            if (bd != null) {
                inBaseDir = getLocalBase().getAbsolutePath().startsWith(bd);
            }

            if (inBaseDir && !currentInfo.isMetaFolder()
                && !getController().getMySelf().isServer())
            {
                // Schedule for removal
                getController().schedule(new Runnable() {
                    public void run() {
                        getController().getFolderRepository().removeFolder(
                            Folder.this, false);
                    }
                }, 5000L);
            } else {
                addProblem(new DeviceDisconnectedProblem(currentInfo));
            }
        }

        return deviceDisconnected;
    }

    public String getName() {
        return currentInfo.name;
    }

    public String getConfigEntryId() {
        return configEntryId;
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
            synchronized (scanLock) {
                if (fileInfo != null && fileInfo.isFile()) {
                    try {
                        archiver.archive(fileInfo, file, false);
                    } catch (IOException e) {
                        logSevere("Unable to move file to archive: " + file
                            + ". " + e, e);
                    }
                }
                if (file.exists() && !file.delete()) {
                    logSevere("Unable to delete file " + file);
                    return false;
                }
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
        return getIncomingFiles(true, -1);
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
        return getIncomingFiles(includeDeleted, -1);
    }

    /**
     * Gets all the incoming files. That means files that exist on the remote
     * side with a higher version.
     * 
     * @param includeDeleted
     *            true if also deleted files should be considered.
     * @param maxPerMember
     *            the aproximately maximum number of incoming files (not
     *            directories) to be included per member. This prevents the
     *            resulting Collection from abnormal growth.
     * @return the list of files that are incoming/newer available on remote
     *         side as unmodifiable collection.
     */
    public Collection<FileInfo> getIncomingFiles(boolean includeDeleted,
        int maxPerMember)
    {
        // build a temp list
        // Map<FileInfo, FileInfo> incomingFiles = new HashMap<FileInfo,
        // FileInfo>();
        SortedMap<FileInfo, FileInfo> incomingFiles = new TreeMap<FileInfo, FileInfo>(
            new FileInfoComparator(FileInfoComparator.BY_RELATIVE_NAME));
        // add0 expeced files
        Map<Member, Integer> incomingCount = maxPerMember > 0
            ? new HashMap<Member, Integer>(getMembersCount())
            : null;
        boolean revert = isRevertLocalChanges();
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
                if (isFine()) {
                    logFine("Not downloading files. " + member + " / "
                        + member.getAccountInfo() + " no write permission");
                }
                continue;
            }

            Collection<FileInfo> memberFiles = getFilesAsCollection(member);
            if (incomingCount != null) {
                incomingCount.put(member, 0);
            }
            if (memberFiles != null) {
                for (FileInfo remoteFile : memberFiles) {
                    if (incomingCount != null
                        && incomingCount.get(member) > maxPerMember)
                    {
                        continue;
                    }
                    if (remoteFile.isDeleted() && !includeDeleted) {
                        continue;
                    }

                    // Check if remote file is newer
                    FileInfo localFile = getFile(remoteFile);
                    if (revert && localFile != null) {
                        FileInfo newestFileInfo = remoteFile
                            .getNewestVersion(getController()
                                .getFolderRepository());
                        if (localFile.isNewerThan(newestFileInfo)) {
                            // Ignore/Rever local files
                            logWarning("Local change detected, but has no write permission: "
                                + localFile.toDetailString());
                            localFile = null;
                        }
                    }
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
                            if (incomingCount != null) {
                                Integer i = incomingCount.get(member);
                                incomingCount.put(member, ++i);
                            }
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
                logFine((incomingCount != null ? "" : "Aprox. ")
                    + incomingFiles.size() + " incoming files");
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
                if (isFine()) {
                    logFine("Not downloading files. " + member + " / "
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
     * Visits all {@link Member}s of this folder
     * 
     * @param visitor
     */
    public void visitMembers(Visitor<Member> visitor) {
        for (Member member : members.keySet()) {
            if (!visitor.visit(member)) {
                return;
            }
        }
    }

    /**
     * Visits all fully connected {@link Member}s of this folder.
     * 
     * @param visitor
     */
    public void visitMembersConnected(Visitor<Member> visitor) {
        for (Member member : members.keySet()) {
            if (!member.isCompletelyConnected()) {
                continue;
            }
            if (!visitor.visit(member)) {
                return;
            }
        }
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
        return new TFile(localBase, FileInfoFactory.encodeIllegalChars(fInfo
            .getRelativeName()));
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
        if (syncProfile.equals(SyncProfile.BACKUP_SOURCE)) {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_TARGET);
        } else if (syncProfile.equals(SyncProfile.BACKUP_TARGET)) {
            inv.setSuggestedSyncProfile(SyncProfile.BACKUP_SOURCE);
        } else if (syncProfile.equals(SyncProfile.HOST_FILES)) {
            inv.setSuggestedSyncProfile(SyncProfile.AUTOMATIC_DOWNLOAD);
        }
        inv.setSuggestedLocalBase(getController(), localBase);
        String username = getController().getOSClient().getUsername();
        if (StringUtils.isNotBlank(username)) {
            inv.setUsername(username);
        }
        return inv;
    }

    /**
     * Ensures that default ignore patterns are set.
     */
    public void addDefaultExcludes() {
        File pFile = new TFile(getSystemSubDir(),
            DiskItemFilter.PATTERNS_FILENAME);
        boolean init = !pFile.exists();

        addPattern(Pattern.THUMBS_DB);
        addPattern(Pattern.OFFICE_TEMP);
        addPattern(FileUtils.DESKTOP_INI_FILENAME);
        addPattern(Pattern.DS_STORE);
        addPattern(Pattern.ITHUMB);

        if (WinUtils.getAppDataCurrentUser() != null
            && localBase.getAbsolutePath().equals(
                WinUtils.getAppDataCurrentUser()))
        {
            addPattern("PowerFolder/logs/*");
        }
        // #2083
        if (UserDirectories.getDocumentsReported() != null
            && localBase.getAbsolutePath().equals(
                UserDirectories.getDocumentsReported()))
        {
            logFine("My documents @ " + UserDirectories.getDocumentsReported());
            logFine("Folder @ " + localBase.getAbsolutePath());

            logWarning("Adding transition ignore patterns for My documents folder");

            // Ignore My Pictures, My Music, My Videos, PowerFolders (basedir)
            File baseDir = getController().getFolderRepository()
                .getFoldersAbsoluteDir();
            addPattern(baseDir.getName() + '*');

            if (UserDirectories.getDocumentsReported() != null) {
                int i = UserDirectories.getDocumentsReported().length();
                if (UserDirectories.getMusicReported() != null
                    && UserDirectories.getMusicReported().startsWith(
                        UserDirectories.getDocumentsReported()))
                {
                    addPattern(UserDirectories.getMusicReported().substring(
                        i + 1) + '*');
                }
                if (UserDirectories.getPicturesReported() != null
                    && UserDirectories.getPicturesReported().startsWith(
                        UserDirectories.getDocumentsReported()))
                {
                    addPattern(UserDirectories.getPicturesReported().substring(
                        i + 1) + '*');
                }
                if (UserDirectories.getVideosReported() != null
                    && UserDirectories.getVideosReported().startsWith(
                        UserDirectories.getDocumentsReported()))
                {
                    addPattern(UserDirectories.getVideosReported().substring(
                        i + 1) + '*');
                }
            }
        }

        if (init) {
            diskItemFilter.savePatternsTo(pFile, false);
            // Defaults have 0
            pFile.setLastModified(0);
        }
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
        File lastSyncFile = new TFile(getSystemSubDir0(),
            LAST_SYNC_INFO_FILENAME);
        if (!getSystemSubDir0().exists()) {
            return;
        }
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
        File lastSyncFile = new TFile(getSystemSubDir0(),
            LAST_SYNC_INFO_FILENAME);
        if (lastSyncFile.exists()) {
            lastSyncDate = new Date(lastSyncFile.lastModified());
        } else {
            lastSyncDate = null;
        }
    }

    // Security methods *******************************************************

    public boolean hasReadPermission(Member member) {
        return hasFolderPermission(member,
            FolderPermission.read(getParentFolderInfo()));
    }

    public boolean hasWritePermission(Member member) {
        return hasFolderPermission(member,
            FolderPermission.readWrite(getParentFolderInfo()));
    }

    public boolean hasAdminPermission(Member member) {
        return hasFolderPermission(member,
            FolderPermission.admin(getParentFolderInfo()));
    }

    public boolean hasOwnerPermission(Member member) {
        return hasFolderPermission(member,
            FolderPermission.owner(getParentFolderInfo()));
    }

    private boolean hasFolderPermission(Member member,
        FolderPermission permission)
    {
        if (getController().getOSClient().isCloudServer(member)) {
            return true;
        }
        return getController().getSecurityManager().hasPermission(
            member.getInfo(), permission);
    }

    private FolderInfo getParentFolderInfo() {
        if (!currentInfo.isMetaFolder()) {
            return currentInfo;
        }
        Folder parentFolder = getController().getFolderRepository()
            .getParentFolder(currentInfo);
        if (parentFolder == null) {
            logWarning("Unable to retrieve parent folder for " + currentInfo);
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

    private void fileChanged(FileInfo... fileInfos) {
        filesChanged(Arrays.asList(fileInfos));
    }

    private void filesChanged(final List<FileInfo> fileInfosList) {
        Reject.ifNull(fileInfosList, "FileInfo is null");

        for (int i = 0; i < fileInfosList.size(); i++) {
            FileInfo fileInfo = fileInfosList.get(i);
            // TODO Bulk fire event
            fireFileChanged(fileInfo);
            final FileInfo localInfo = getFile(fileInfo);
            fileInfosList.set(i, localInfo);
        }

        setDBDirty();

        if (fileInfosList.size() >= 1
            || diskItemFilter.isRetained(fileInfosList.get(0)))
        {
            broadcastMessages(new MessageProducer() {
                public Message[] getMessages(boolean useExt) {
                    return FolderFilesChanged.create(getInfo(), fileInfosList,
                        diskItemFilter, useExt);
                }
            });
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
        checkSync();

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
    public void checkSync() {
        if (!ConfigurationEntry.FOLDER_SYNC_USE
            .getValueBoolean(getController()))
        {
            removeUnsyncedProblem();
            return;
        }
        if (previewOnly) {
            return;
        }
        // Calculate the date that folders should be synced by.
        int warnSeconds = syncWarnSeconds;
        if (warnSeconds == 0) {
            warnSeconds = ConfigurationEntry.FOLDER_SYNC_WARN_SECONDS
                .getValueInt(getController());
        }
        if (warnSeconds <= 0) {
            removeUnsyncedProblem();
            return;
        }
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.SECOND, -warnSeconds);
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

        Date myLastSyncDate = lastSyncDate;
        if (myLastSyncDate != null && myLastSyncDate.before(warningDate)) {
            if (!(othersInSync && isSyncing())) {
                // Only need one of these.
                UnsynchronizedFolderProblem ufp = null;
                for (Problem problem : problems) {
                    if (problem instanceof UnsynchronizedFolderProblem) {
                        ufp = (UnsynchronizedFolderProblem) problem;
                        break;
                    }
                }
                if (ufp == null
                    && PreferencesEntry.EXPERT_MODE
                        .getValueBoolean(getController()))
                {
                    if (new Date().before(lastSyncDate)) {
                        logWarning("Last sync date in future: " + lastSyncDate);
                    }
                    Problem problem = new UnsynchronizedFolderProblem(
                        currentInfo, myLastSyncDate);
                    addProblem(problem);
                }
            }
        } else {
            removeUnsyncedProblem();
        }
    }

    private void removeUnsyncedProblem() {
        if (problems.isEmpty()) {
            return;
        }
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

    public boolean isSyncPatterns() {
        return syncPatterns;
    }

    public void setSyncPatterns(boolean syncPatterns) {
        this.syncPatterns = syncPatterns;
        String syncProfKey = FOLDER_SETTINGS_PREFIX_V4 + configEntryId
            + FolderSettings.FOLDER_SETTINGS_SYNC_PATTERNS;
        getController().getConfig().put(syncProfKey,
            String.valueOf(syncPatterns));
        getController().saveConfig();
    }

    public int getSyncWarnSeconds() {
        return syncWarnSeconds;
    }

    /**
     * @param syncWarnSeconds
     *            The number of seconds after the folder will raise a
     *            {@link UnsynchronizedFolderProblem} if not 100% sync. 0 or
     *            lower means use default.
     */
    public void setSyncWarnSeconds(int syncWarnSeconds) {
        if (syncWarnSeconds == this.syncWarnSeconds) {
            return;
        }
        this.syncWarnSeconds = syncWarnSeconds;
        if (syncWarnSeconds != 0) {
            getController().getConfig().setProperty(
                FOLDER_SETTINGS_PREFIX_V4 + configEntryId
                    + FolderSettings.FOLDER_SETTINGS_SYNC_WARN_SECONDS,
                String.valueOf(syncWarnSeconds));
        } else {
            getController().getConfig().remove(
                FOLDER_SETTINGS_PREFIX_V4 + configEntryId
                    + FolderSettings.FOLDER_SETTINGS_SYNC_WARN_SECONDS);
        }
        getController().saveConfig();
        checkSync();
    }

    /**
     * Save patterns to metaFolder for transfer to other computers.
     */
    private void savePatternsToMetaFolder() {
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
        if (metaFolder.deviceDisconnected) {
            logFiner("Not writing synced ignored patterns. Meta folder disconnected");
            return;
        }
        // Write the patterns in the meta directory.
        File file = new TFile(metaFolder.localBase,
            DiskItemFilter.PATTERNS_FILENAME);
        FileInfo fInfo = FileInfoFactory.lookupInstance(metaFolder, file);
        diskItemFilter.savePatternsTo(file, false);
        if (isFine()) {
            logFine("Saving ignore patterns to Meta folder: " + file);
        }
        metaFolder.scanChangedFile(fInfo);
    }

    /**
     * Delete any file archives over a specified age.
     */
    public void cleanupOldArchiveFiles(Date cleanupDate) {
        archiver.cleanupOldArchiveFiles(cleanupDate);
    }

    // Inner classes **********************************************************

    /**
     * Persister task, persists settings from time to time.
     */
    private class Persister extends TimerTask {
        @Override
        public synchronized void run() {
            if (shutdown) {
                return;
            }
            if (dirty) {
                persist();
            }
            if (diskItemFilter.isDirty() && !checkIfDeviceDisconnected()) {
                diskItemFilter.savePatternsTo(new TFile(getSystemSubDir(),
                    DiskItemFilter.PATTERNS_FILENAME), true);
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

    public boolean isEncrypted() {
        return encrypted;
    }

}