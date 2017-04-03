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
 * $Id: FolderRepository.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.problem.AccessDeniedProblem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.FolderStatisticInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileListRequest;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.task.CreateFolderOnServerTask;
import de.dal33t.powerfolder.task.FolderObtainPermissionTask;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.collection.CompositeCollection;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.os.LinuxUtil;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.dal33t.powerfolder.disk.FolderSettings.ID;
import static de.dal33t.powerfolder.disk.FolderSettings.PREFIX_V4;

/**
 * Repository of all known power folders. Local and unjoined.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class FolderRepository extends PFComponent implements Runnable {

    private static final Logger log = Logger.getLogger(FolderRepository.class
        .getName());
    // PFS-1657
    private static final String DIRNAME_SNAPSHOT = ".snapshot";
    private final Map<FolderInfo, Folder> folders;
    private final Map<FolderInfo, Folder> metaFolders;
    private Thread myThread;
    private final FileRequestor fileRequestor;
    private Folder currentlyMaintainingFolder;
    private final Set<String> onLoginFolderEntryIds = new HashSet<String>();
    // Flag if the repo is started
    private boolean started;
    // The trigger to start scanning
    private final Object scanTrigger = new Object();
    private boolean triggered;
    private final AtomicInteger suspendNewFolderSearch = new AtomicInteger(0);
    private Path foldersBasedir;

    /** folder repository listeners */
    private final FolderRepositoryListener folderRepositoryListenerSupport;

    /** The disk scanner */
    private final FolderScanner folderScanner;

    /**
     * PFC-1962: For locking files
     */
    private final Locking locking;

    /**
     * The current synchronizater of all folder memberships
     */
    private AllFolderMembershipSynchronizer folderMembershipSynchronizer;
    private final Object folderMembershipSynchronizerLock = new Object();

    /**
     * Registered to ALL folders to delegate problem event of any folder to
     * registered listeners.
     * <p>
     * TODO: Value listeners deteriorate the UI refresh speed.
     */
    private final ProblemListener valveProblemListenerSupport;

    private FolderAutoCreateListener folderAutoCreateListener;

    /**
     * A list of folder base directories that have been removed in the past.
     */
    private final Set<Path> ignoredFolderDirectories = new CopyOnWriteArraySet<Path>();

    /**
     * Mutex for the periodical looking for new folders / removing folders not
     * present on the server any more and the spontaneous event of a
     * "disconnected device". Also mutex all folder creation processes.
     *
     * @see #scanBasedir()
     * @see #handleDeviceDisconnected(Folder)
     * @author krickl@powerfolder.com
     */
    private final ReentrantLock scanBasedirLock = new ReentrantLock();
    private ScheduledFuture<?> scanBaseDirFuture;

    /**
     * Constructor
     *
     * @param controller
     */
    public FolderRepository(Controller controller) {
        super(controller);

        triggered = false;
        // Rest
        folders = new ConcurrentHashMap<>();
        metaFolders = new ConcurrentHashMap<>();
        fileRequestor = new FileRequestor(controller);
        started = false;
        loadIgnoredFolders();

        folderScanner = new FolderScanner(getController());
        locking = new Locking(getController());

        // Create listener support
        folderRepositoryListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderRepositoryListener.class);
        valveProblemListenerSupport = ListenerSupportFactory
            .createListenerSupport(ProblemListener.class);
        folderAutoCreateListener = ListenerSupportFactory
            .createListenerSupport(FolderAutoCreateListener.class);
    }

    private void loadIgnoredFolders() {
        String list = ConfigurationEntry.AUTO_SETUP_FOLDERS_IGNORED
            .getValue(getController());
        String[] parts = list.split("\\$");
        for (String s : parts) {
            try {
                if (!EncryptedFileSystemUtils.endsWithEncryptionSuffix(s)) {
                    Path p = Paths.get(s);
                    ignoredFolderDirectories.add(p);
                }
            } catch (Exception e) {
                logFine("Unable to check removed dir: " + s + ". " + e);
            }
        }
    }

    public void addProblemListenerToAllFolders(ProblemListener listener) {
        ListenerSupportFactory.addListener(valveProblemListenerSupport,
            listener);
    }

    public void removeProblemListenerFromAllFolders(ProblemListener listener) {
        ListenerSupportFactory.removeListener(valveProblemListenerSupport,
            listener);
    }

    public void addFolderAutoCreateListener(FolderAutoCreateListener listener) {
        ListenerSupportFactory.addListener(folderAutoCreateListener, listener);
    }

    public void removeFolderAutoCreateListener(FolderAutoCreateListener listener)
    {
        ListenerSupportFactory.removeListener(folderAutoCreateListener,
            listener);
    }

    /** @return The folder scanner that performs the scanning of files on disk */
    public FolderScanner getFolderScanner() {
        return folderScanner;
    }

    public Locking getLocking() {
        return locking;
    }

    public void setSuspendFireEvents(boolean suspended) {
        ListenerSupportFactory.setSuspended(folderRepositoryListenerSupport,
            suspended);
        logFine("setSuspendFireEvents: " + suspended);
    }

    /**
     * Load folders from disk. Find all possible folder names, then find config
     * for each folder name.
     */
    public void init() {
        initFoldersBasedir();

        processV4Format();

        // Maintain link
        if (getController().isFirstStart()) {
            createShortcuts();
        }

        tidyOldLinks();
    }

    public void createShortcuts() {
        if (PreferencesEntry.CREATE_BASEDIR_DESKTOP_SHORTCUT
            .getValueBoolean(getController()))
        {
            Path base = getController().getFolderRepository()
                .getFoldersBasedir();
            Path path = base.getFileName();
            if (path == null) {
                path = base.getRoot();
            }
            String shortcutName = path.toString();
            if (Util.isDesktopShortcut(shortcutName)) {
                Util.removeDesktopShortcut(shortcutName);
            }
            Util.createDesktopShortcut(shortcutName, getController()
                .getFolderRepository().getFoldersBasedir());
        }
        if (PreferencesEntry.CREATE_FAVORITES_SHORTCUT
            .getValueBoolean(getController()))
        {
            try {
                if (WinUtils.isSupported()) {
                    WinUtils.getInstance().setPFLinks(true, getController());
                } else if (MacUtils.isSupported()) {
                    MacUtils.getInstance().setPFPlaces(true, getController());
                }
            } catch (IOException e) {
                logSevere(e);
            }
        }
    }

    public void updateShortcuts(String oldShortcutName) {
        if (PreferencesEntry.CREATE_BASEDIR_DESKTOP_SHORTCUT
            .getValueBoolean(getController()))
        {
            if (Util.isDesktopShortcut(oldShortcutName)) {
                Util.removeDesktopShortcut(oldShortcutName);
                Path base = getController().getFolderRepository()
                    .getFoldersBasedir();
                Path path = base.getFileName();
                if (path == null) {
                    path = base.getRoot();
                }
                String shortcutName = path.toString();
                Util.createDesktopShortcut(shortcutName, getController()
                    .getFolderRepository().getFoldersBasedir());
            }
        }
        if (PreferencesEntry.CREATE_FAVORITES_SHORTCUT
            .getValueBoolean(getController()))
        {
            try {
                if (WinUtils.isSupported()
                    && WinUtils.isPFLinks(oldShortcutName))
                {
                    WinUtils.removePFLinks(oldShortcutName);
                    WinUtils.getInstance().setPFLinks(true, getController());
                } else if (MacUtils.isSupported()) {
                    MacUtils.getInstance().setPFPlaces(true, getController());
                }
            } catch (IOException e) {
                logSevere(e);
            }
        }
    }

    /**
     * Make sure there are no old links to deleted folders. These should be
     * maintained when folders are removed. This is just a legacy check.
     */
    private void tidyOldLinks() {
        Path baseDir = getFoldersBasedir();

        if (Files.exists(baseDir)) {
            try (DirectoryStream<Path> links = Files.newDirectoryStream(
                baseDir, "*" + Constants.LINK_EXTENSION)) {
                for (Path link : links) {
                    boolean haveFolder = false;

                    for (Folder folder : getFolders()) {
                        if ((folder.getName() + Constants.LINK_EXTENSION)
                            .equals(link.getFileName().toString()))
                        {
                            haveFolder = true;
                            break;
                        }
                    }

                    if (!haveFolder) {
                        boolean deleted = Files.deleteIfExists(link);
                        logInfo("Removed old link " + link.getFileName() + "? "
                            + deleted);
                    }
                }
            } catch (IOException ioe) {
                // TODO:
                logWarning(ioe.getMessage() + ". " + ioe);
            }
        }
    }

    private void initFoldersBasedir() {
        String baseDir;
        String cmdBaseDir = getController().getCommandLine() != null
            ? getController().getCommandLine().getOptionValue("b")
            : null;
        if (StringUtils.isNotBlank(cmdBaseDir)) {
            baseDir = cmdBaseDir;
        } else {
            baseDir = ConfigurationEntry.FOLDER_BASEDIR
                .getValue(getController());
            // Read folder base path from registry if possible
            boolean overwriteBaseDir = PreferencesEntry.FOLDER_BASE_PATH_OVERWRITE.getValueBoolean(getController());
            if (overwriteBaseDir == true && !PreferencesEntry.FOLDER_BASE_PATH.getValueString(getController()).isEmpty()) {
                baseDir = PreferencesEntry.FOLDER_BASE_PATH.getValueString(getController());
                // Set folder base path in config
                ConfigurationEntry.FOLDER_BASEDIR.setValue(getController(), baseDir);
                // Disable registry entry
                PreferencesEntry.FOLDER_BASE_PATH_OVERWRITE.setValue(getController(), false);
            }
        }

        // PFC-2544: Start
        try {
            if (StringUtils.isNotBlank(baseDir)) {
                // Fallback: Take system username. OS Client is not initialized
                // when this method is called.
                String username = System.getProperty("user.name");
                if (StringUtils.isNotBlank(getController().getOSClient()
                    .getUsername()))
                {
                    username = getController().getOSClient().getUsername();
                }
                if (baseDir.contains("%username%")) {
                    baseDir = baseDir.replace("%username%", username);
                    logWarning("New basedir: " + baseDir);
                }
                if (baseDir.contains("$username")) {
                    baseDir = baseDir.replace("$username", username);
                    logWarning("New basedir: " + baseDir);
                }
            }
        } catch (Exception e) {
            logWarning("Unable to resolve 'username' placeholder in basepath: "
                + baseDir + ". " + e);
        }
        // PFC-2544: End

        // Check if this a windows network drive.
        // TODO: Check: Does this really work?
        boolean winNetworkDrive = baseDir != null && baseDir.contains(":\\")
            && baseDir.charAt(1) == ':';

        boolean ok = false;

        if (OSUtil.isWindowsSystem() && winNetworkDrive || !winNetworkDrive) {
            foldersBasedir = Paths.get(baseDir).toAbsolutePath();
            if (Files.notExists(foldersBasedir)) {
                try {
                    Files.createDirectories(foldersBasedir);
                    logInfo("Created base path for folders: " + foldersBasedir);
                } catch (FileAlreadyExistsException faee) {
                    // ignore
                } catch (Exception e) {
                    // TODO: take a closer look at the different Exceptions that
                    // can be caught.
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir + ". " + e.getMessage());
                }
            }
            ok = Files.exists(foldersBasedir)
                && Files.isReadable(foldersBasedir)
                && Files.isDirectory(foldersBasedir);
        }

        if (!OSUtil.isWindowsSystem() && winNetworkDrive) {
            foldersBasedir = Paths.get(ConfigurationEntry.FOLDER_BASEDIR
                .getDefaultValue());
            if (Files.notExists(foldersBasedir)) {
                try {
                    Files.createDirectories(foldersBasedir);
                    logInfo("Created base path for folders: " + foldersBasedir);
                } catch (FileAlreadyExistsException faee) {
                    // ignore
                } catch (Exception e) {
                    // TODO: take a closer look at the different Exceptions that
                    // can be caught.
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir + ". " + e.getMessage());
                }
            }
            ok = Files.exists(foldersBasedir)
                && Files.isReadable(foldersBasedir)
                && Files.isDirectory(foldersBasedir);
        }

        // Use default as fallback
        if (!ok
            && ConfigurationEntry.FOLDER_BASEDIR_FALLBACK_TO_DEFAULT
                .getValueBoolean(getController()))
        {
            foldersBasedir = Paths.get(ConfigurationEntry.FOLDER_BASEDIR
                .getDefaultValue());
            if (Files.notExists(foldersBasedir)) {
                try {
                    Files.createDirectories(foldersBasedir);
                    logInfo("Created base path for folders: " + foldersBasedir);
                } catch (FileAlreadyExistsException faee) {
                    // ignore
                } catch (Exception e) {
                    // TODO: take a closer look at the different Exceptions that
                    // can be caught.
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir + ". " + e.getMessage());
                }
            }
        }

        ok = Files.exists(foldersBasedir) && Files.isReadable(foldersBasedir)
            && Files.isDirectory(foldersBasedir);

        if (ok) {
            logFine("Using base path for folders: " + foldersBasedir);
            if (OSUtil.isMacOS()) {
                try {
                    Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>(3);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(foldersBasedir, perms);
                } catch (IOException e) {
                    logInfo("Could not set permissions to base path for folders: "
                        + foldersBasedir + ". " + e);
                }
            }
            PathUtils.maintainDesktopIni(getController(), foldersBasedir);
            // PFC-2538
            try {
                if (ConfigurationEntry.COPY_GETTING_STARTED_GUIDE
                    .getValueBoolean(getController()))
                {
                    Path gsFile = foldersBasedir
                        .resolve(Constants.GETTING_STARTED_GUIDE_FILENAME);
                    if (Files.notExists(gsFile)) {
                        Util.copyResourceTo(
                            Constants.GETTING_STARTED_GUIDE_FILENAME, null,
                            gsFile, false, true);
                    }
                }
            } catch (Exception e) {
                logWarning("Unable to copy getting started guide. " + e);
            }
        } else {
            logWarning("Unable to access base path for folders: "
                + foldersBasedir);
        }
        // Save folder base path to registry
        PreferencesEntry.FOLDER_BASE_PATH.setValue(getController(), baseDir);
    }

    /**
     * Version 4 format is like f.<md5>.XXXX, where md5 is the MD5 of the folder
     * id. This format allows folders with the same name to be stored.
     */
    private void processV4Format() {
        final Properties config = getController().getConfig();

        // Find all folder entries.
        Set<String> entryIds = FolderSettings.loadEntryIds(config);

        // Load on many processors
        int loaders = Math.min(Runtime.getRuntime().availableProcessors() - 2,
            8);
        if (loaders <= 0) {
            loaders = 1;
        }
        final Semaphore loadPermit = new Semaphore(loaders);
        final AtomicInteger nCreated = new AtomicInteger();
        // Scan config for all found folder MD5s.
        for (final String folderEntryId : entryIds) {
            try {
                loadPermit.acquire();
            } catch (InterruptedException e) {
                logFiner(e);
                return;
            }
            Runnable folderCreator = new Runnable() {
                public void run() {
                    try {
                        String folderId = config
                            .getProperty(PREFIX_V4
                                + folderEntryId + ID);
                        if (StringUtils.isBlank(folderId)) {
                            logWarning("Folder id blank. Removed illegal folder config entry: "
                                + folderEntryId);
                            removeConfigEntries(folderEntryId);
                            return;
                        }
                        String folderName = FolderSettings.loadFolderName(
                            getController().getConfig(), folderEntryId);
                        if (StringUtils.isBlank(folderName)) {
                            logWarning("Foldername not found."
                                + "Removed illegal folder config entry: "
                                + folderName + '/' + folderEntryId);
                            removeConfigEntries(folderEntryId);
                            return;
                        }

                        // #2203 Load later if folder id should be taken from
                        // account.
                        if (folderId
                            .contains(FolderSettings.FOLDER_ID_FROM_ACCOUNT))
                        {
                            logFine("Folder load scheduled after first login: "
                                + folderName + '/' + folderEntryId);
                            onLoginFolderEntryIds.add(folderEntryId);
                            return;
                        }

                        FolderInfo foInfo = new FolderInfo(folderName, folderId)
                            .intern();
                        FolderSettings folderSettings = FolderSettings.load(
                            getController(), folderEntryId);

                        if (folderSettings == null) {
                            logWarning("Unable to load folder settings."
                                    + "Removed folder config entry: " + folderName
                                    + '/' + folderEntryId);
                            removeConfigEntries(folderEntryId);
                            return;
                        }

                        // Fix for PFS-2319: Repair broken encrypted folders
                        if (folderSettings.getLocalBaseDirString().equals(Constants.FOLDER_ENCRYPTED_CONTAINER_ROOT_DIR)){

                            // Construct temporary basePath
                            folderName = folderName + Constants.FOLDER_ENCRYPTION_SUFFIX;
                            Path folderDirectoryForRecoveredFolders = getFoldersBasedir().resolve("RECOVERED").resolve(folderName);
                            Path temporaryBasePath = PathUtils.createEmptyDirectory(folderDirectoryForRecoveredFolders);

                            folderSettings = folderSettings.changeBaseDir(temporaryBasePath);
                            logWarning("Repaired broken encrypted Folder " + folderName + "/" + foInfo.getId() +
                                    ". New storage path: " + temporaryBasePath);
                        }

                        // Do not add0 if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderSettings != null)
                        {
                            createFolder0(foInfo, folderSettings, false);
                        }
                    } catch (Exception e) {
                        logSevere("Problem loading/creating folder #"
                            + folderEntryId + ". " + e, e);
                    } finally {
                        loadPermit.release();
                        synchronized (nCreated) {
                            nCreated.incrementAndGet();
                            nCreated.notify();
                        }
                    }
                }
            };
            getController().getIOProvider().startIO(folderCreator);
        }

        // Wait for creators to complete
        while (nCreated.get() < entryIds.size()) {
            synchronized (nCreated) {
                try {
                    nCreated.wait(10);
                } catch (InterruptedException e) {
                    logFiner(e);
                    return;
                }
            }
        }
        logInfo("Loaded " + getFoldersCount() + " folders");
    }

    /**
     * Starts the folder repo maintenance thread
     */
    public void start() {
        if (!ConfigurationEntry.FOLDER_REPOSITORY_ENABLED
            .getValueBoolean(getController()))
        {
            logWarning("Not starting FolderRepository. disabled by config");
            return;
        }

        folderScanner.start();

        // Now start thread
        myThread = new Thread(this, getClass().getName());
        // set to min priority
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();

        // Start file requestor
        fileRequestor.start();

        // Defer 2 minutes, so it is not 'in-your-face' at start up.
        // Also run this every minute.
        getController().scheduleAndRepeat(new CheckSyncTask(),
            1000L * Constants.FOLDER_UNSYNCED_CHECK_DELAY,
            Constants.MILLIS_PER_MINUTE);

        // ============
        // Monitor the default directory for possible new folders.
        // ============

        scanBaseDirFuture = getController().getThreadPool()
            .scheduleWithFixedDelay(() -> {
                scanBasedir();
            } , 10L, 10L, TimeUnit.SECONDS);

        // PFS-1956 -- TODO: remove after release of v14
        boolean is0byteRecoveryRun = getController().getPreferences()
            .getBoolean("is0byteRecoveryRun", false);
        if (!is0byteRecoveryRun && ConfigurationEntry.RECOVER_0BYTE_FILES
            .getValueBoolean(getController())
            && !getMySelf().isServer())
        {
            getController().getIOProvider().startIO(new Runnable() {
                @Override
                public void run() {
                    try {
                    restoreZeroByteFiles();
                    getController().getPreferences()
                        .putBoolean("is0byteRecoveryRun", true);
                    } catch (RuntimeException re) {
                        logSevere("An error occured while trying to recover zero byte files: " + re, re);
                    }
                }
            });
        }

        started = true;
    }

    /**
     * Restore files that are zero bytes big and changed by the server.
     * This was an issue with a previous version of the clustering protocol.
     * PFS-1956 -- TODO: remove after release of v12
     */
    private void restoreZeroByteFiles() {
        logFine("Start recovering 0-byte files.");
        for (Folder folder : getFolders()) {
            FileArchiver fa = folder.getFileArchiver();

            for (FileInfo file : folder.getKnownFiles()) {
                if (file.getSize() > 0) {
                    continue;
                }
                // Only if there is a version in the history and the last
                // modifier is a server
                Member lastModifier = file.getModifiedBy()
                    .getNode(getController(), false);
                if (lastModifier == null || !lastModifier.isServer()) {
                    continue;
                }
                if (!fa.hasArchivedFileInfo(file)) {
                    logWarning(
                        "Found 0 byte file, but no old version available to restore for "
                            + file.toDetailString());
                    continue;
                }
                Path fileOnDisk = file.getDiskFile(this);
                try {
                    // Check the file size to be 0 bytes
                    if (Files.size(fileOnDisk) > 0) {
                        continue;
                    }
                    List<FileInfo> history = fa
                        .getSortedArchivedFilesInfos(file);
                    if (history.isEmpty()) {
                        logWarning(
                            "Found 0 byte file, but no old version available to restore for "
                                + file.toDetailString());
                        continue;
                    }
                    FileInfo toRestore = history.get(history.size() - 1);
                    logFine(file.toDetailString()
                        + " was lastly changed by a server "
                        + file.getModifiedBy()
                        + " and has a size of 0 bytes. Restoring old version: "
                        + toRestore.toDetailString());

                    // Now, only restore when the version of the file in
                    // the history is lesser than the version of the
                    // file itself.
                    if (toRestore.getVersion() < file.getVersion()) {
                        logInfo("Restoring previous version of "
                            + file.toDetailString() + ": "
                            + toRestore.toDetailString() + " to " + fileOnDisk);
                        fa.restore(toRestore, fileOnDisk);
                    } else {
                        logWarning("Not restoring previous version of "
                            + file.toDetailString() + ": "
                            + toRestore.toDetailString() + " to " + fileOnDisk);
                    }
                } catch (IOException e) {
                    logWarning("Unable to restore old file version of "
                        + file.toDetailString() + ". " + e);
                }
            }
        }
    }

    /**
     * Shuts down folder repo
     */
    public void shutdown() {
        if (scanBaseDirFuture != null) {
            // Stop any further scan
            getController().removeScheduled(scanBaseDirFuture);
            // Wait for a running task to finish
            // PFC-2798 -> commented may block when #createFolder is saving the
            // configuration
//            addAndRemoveFolderLock.lock();
//            addAndRemoveFolderLock.unlock();
        }

        synchronized (folderMembershipSynchronizerLock) {
            if (folderMembershipSynchronizer != null) {
                folderMembershipSynchronizer.canceled.set(true);
            }
        }
        folderScanner.shutdown();

        if (myThread != null) {
            myThread.interrupt();
        }
        synchronized (scanTrigger) {
            scanTrigger.notifyAll();
        }

        // Stop file requestor
        fileRequestor.shutdown();

        // shutdown all folders
        for (Folder metaFolder : metaFolders.values()) {
            metaFolder.shutdown();
        }
        for (Folder folder : folders.values()) {
            folder.shutdown();
        }
        // make sure that on restart of folder the folders are freshly read
        folders.clear();
        metaFolders.clear();
        logFine("Stopped");
    }

    /**
     * @return the default basedir for all folders. basedir is just suggested
     */
    public String getFoldersBasedirString() {
        return getFoldersBasedir() != null ? getFoldersBasedir()
            .toAbsolutePath().toString() : null;
    }

    /**
     * @return the default basedir for all folders. basedir is just suggested
     */
    public Path getFoldersBasedir() {
        if (foldersBasedir == null) {
            initFoldersBasedir();
        }

        return foldersBasedir;
    }

    /**
     * Sets the new base path
     *
     * @param path
     */
    public void setFoldersBasedir(String path) {
        if (path == null) {
            ConfigurationEntry.FOLDER_BASEDIR.removeValue(getController());
            return;
        }
        ConfigurationEntry.FOLDER_BASEDIR.setValue(getController(), path);
        PreferencesEntry.FOLDER_BASE_PATH.setValue(getController(), path);
        initFoldersBasedir();
    }

    /**
     * @return the file requestor
     */
    public FileRequestor getFileRequestor() {
        return fileRequestor;
    }

    /**
     * @param info
     * @return if folder is in repo
     */
    public boolean hasJoinedFolder(FolderInfo info) {
        if (!info.isMetaFolder()) {
            return folders.containsKey(info);
        }
        for (Folder folder : metaFolders.values()) {
            if (folder.getInfo().equals(info)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param folderId
     * @return the folder by folder id, or null if folder is not found
     */
    public Folder getFolder(String folderId) {
        return getFolder(new FolderInfo("", folderId));
    }

    /**
     * @param info
     * @return the folder by info, or null if folder is not found
     */
    public Folder getFolder(FolderInfo info) {
        if (!info.isMetaFolder()) {
            return folders.get(info);
        }
        // #1548: Speed this up.
        for (Folder metaFolder : metaFolders.values()) {
            if (metaFolder.getInfo().equals(info)) {
                return metaFolder;
            }
        }
        return null;
    }

    /**
     * All real-folders WITHOUT Meta-folders (#1548) and WITHOUT unmounted
     * {@link Folder Folders}. Returns the indirect reference to the internal
     * {@link ConcurrentMap}. Contents may change after get.
     *
     * @return the folders as unmodifiable collection
     */
    public Collection<Folder> getFolders() {
        return getFolders(false);
    }

    /**
     * All real-folders WITH or WITHOUT Meta-folders (#1548). Returns the
     * indirect reference to the internal {@link ConcurrentMap}. Contents may
     * changed after get.
     *
     * @param includeMetaFolders
     * @return the folders as unmodifiable collection
     */
    public Collection<Folder> getFolders(boolean includeMetaFolders) {
        if (!includeMetaFolders) {
            return Collections.unmodifiableCollection(folders.values());
        }
        CompositeCollection<Folder> composite = new CompositeCollection<Folder>();
        composite.addComposited(folders.values(), metaFolders.values());
        return Collections.unmodifiableCollection(composite);
    }

    /**
     * @return the number of folders. Does NOT include the meta-folders (#1548).
     */
    public int getFoldersCount() {
        return getFoldersCount(false);
    }

    /**
     * @param includeMetaFolders
     * @return the number of folders
     */
    public int getFoldersCount(boolean includeMetaFolders) {
        return folders.size() + (includeMetaFolders ? metaFolders.size() : 0);
    }

    public int getFolderProblemsCount() {
        int i = 0;
        for (Folder folder : getFolders()) {
            i += folder.getProblems().size();
        }
        return i;
    }

    /**
     * @return an unmodifiable, but thread safe collection of all joined real
     *         folders, does NOT include meta-folders (#1548)
     */
    public Collection<FolderInfo> getJoinedFolderInfos() {
        return Collections.unmodifiableCollection(folders.keySet());
    }

    /**
     * Finds an folder on the give target directory.
     *
     * @param targetDir
     * @return the folder with the targetDir as local base or null if not found
     */
    public Folder findExistingFolder(Path targetDir) {
        return findExistingFolder(targetDir, true);
    }

    /**
     * Finds an folder on the give target directory.
     *
     * @param targetDir
     * @param toRealPath
     *            if paths should be checked against their "real" paths. Costs
     *            extra I/O
     * @return the folder with the targetDir as local base or null if not found
     * @return
     */
    public Folder findExistingFolder(Path targetDir, boolean toRealPath) {
        if (!targetDir.isAbsolute()) {
            targetDir = foldersBasedir
                .resolve(targetDir);
            logInfo("Original path: " + targetDir
                + ". Choosen relative path: " + targetDir);
        }

        for (Folder folder : getController().getFolderRepository()
            .getFolders())
        {
            if (folder.getLocalBase().equals(targetDir)) {
                return folder;
            }
            if (toRealPath) {
                try {
                    if (folder.getCommitOrLocalDir().toRealPath()
                        .equals(targetDir.toRealPath()))
                    {
                        return folder;
                    }
                } catch (IOException e) {
                    logFine("Unable to access: " + folder.getLocalBase() + ". "
                        + e);
                }
            }
        }
        return null;
    }

    /**
     * Finds an folder with the give folder name. Search is non-case sensitive!
     *
     * @param folderName
     * @return the folder with the given name or null if not found
     */
    public Folder findExistingFolder(String folderName) {
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            if (folder.getName().equalsIgnoreCase(folderName)) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Find the folder that contains the file sprecified by {@code pathName}.
     *
     * @param path
     * @return The folder containing the file
     */
    public Folder findContainingFolder(Path path) {
        for (Folder folder : folders.values()) {
            if (path.startsWith(folder.getLocalBase().toAbsolutePath())) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Creates a folder from a folder info object and sets the sync profile.
     * <p>
     * Also stores a invitation file for the folder in the local directory if
     * wanted.
     *
     * @param folderInfo
     *            the folder info object
     * @param folderSettings
     *            the settings for the folder
     * @return the freshly created folder
     */
    public Folder createFolder(FolderInfo folderInfo,
        FolderSettings folderSettings)
    {
        try {
            if (ConfigurationEntry.FOLDER_CREATE_USE_EXISTING
                .getValueBoolean(getController()))
            {
                Files.createDirectories(folderSettings.getLocalBaseDir());
            } else if (Files.notExists(folderSettings.getLocalBaseDir()) ||
                    !PathUtils.isEmptyDir(folderSettings.getLocalBaseDir()))
            {
                Path baseDir = folderSettings.getLocalBaseDir().getParent();
                String rawName = folderSettings.getLocalBaseDir().getFileName()
                    .toString();
                Path newBaseDir = PathUtils.createEmptyDirectory(baseDir, rawName);
                if (!newBaseDir.equals(baseDir)) {
                    folderSettings = folderSettings.changeBaseDir(newBaseDir);
                }
            }
        } catch (IOException ioe) {
            logWarning("Unable to create Folder: " + folderInfo.getName() + " @ " +
                    folderSettings.getLocalBaseDir() + " : " + ioe.getMessage());
        }
        Folder folder = createFolder0(folderInfo, folderSettings, true);

        // Obtain permission. Don't do this on startup (createFolder0)
        if (getController().getOSClient().isLoggedIn()
            && !getController().getOSClient().getAccount()
                .hasPermission(FolderPermission.read(folderInfo)))
        {
            getController().getTaskManager().scheduleTask(
                new FolderObtainPermissionTask(getController().getOSClient()
                    .getAccountInfo(), folder.getInfo()));
        }

        return folder;
    }

    /**
     * Used when creating a preview folder. FolderSettings should be as required
     * for the preview folder. Note that settings are not stored and the caller
     * is responsible for setting the preview config.
     *
     * @param folderInfo
     * @param folderSettings
     * @return the preview folder.
     */
    public Folder createPreviewFolder(FolderInfo folderInfo,
        FolderSettings folderSettings)
    {
        return createFolder0(folderInfo, folderSettings, false);
    }

    /**
     * Creates a folder from a folder info object and sets the sync profile.
     * <p>
     * Also stores an invitation file for the folder in the local directory if
     * wanted.
     *
     * @param folderInfo
     *            the folder info object
     * @param folderSettings
     *            the settings for the folder
     * @param saveConfig
     *            true if the configuration file should be saved after creation.
     * @return the freshly created folder
     */
    private Folder createFolder0(FolderInfo folderInfo,
        FolderSettings folderSettings, boolean saveConfig)
    {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(folderSettings, "FolderSettings is null");

        if (hasJoinedFolder(folderInfo)) {
            Folder existingFolder = folders.get(folderInfo);
            if (!existingFolder.getLocalBase().equals(folderSettings.getLocalBaseDir())) {
                logWarning("Tried to create new folder " + folderInfo.getName() + " @ " +
                        folderSettings.getLocalBaseDirString() + ", but already existing @ " +
                        existingFolder.getLocalBase());
            }
            return existingFolder;
        }

        // If non-preview folder and already have this folder as preview,
        // silently remove the preview.
        if (!folderSettings.isPreviewOnly()) {
            for (Folder folder : folders.values()) {
                if (folder.isPreviewOnly()
                    && folder.getInfo().equals(folderInfo))
                {
                    logInfo("Removed preview folder " + folder.getName());
                    removeFolder(folder, true);
                    break;
                }
                if (folder.getCommitOrLocalDir().equals(
                    folderSettings.getLocalBaseDir()))
                {
                    logWarning("Tried to create duplicate folder "
                        + folder.getName() + ". at "
                        + folder.getCommitOrLocalDir()
                        + ". Existing folder ID: " + folder.getId()
                        + ". Requested folder ID: " + folderInfo.getId());
                    throw new IllegalStateException(
                        "Tried to create duplicate folder " + folder.getName()
                            + ". at " + folder.getCommitOrLocalDir()
                            + ". Existing folder ID: " + folder.getId()
                            + ". Requested folder ID: " + folderInfo.getId());
                }
            }
        }

        // PFC-2226: Option to restrict new folder creation to the default
        // storage path.
        // Note, this is a last check. User should never get here because of
        // other checks.
        if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
            .getValueBoolean(getController()))
        {
            Path localBaseDirParent = folderSettings.getLocalBaseDir()
                .getParent();
            if (localBaseDirParent != null) {
                boolean inBaseDir = localBaseDirParent
                    .equals(getFoldersBasedir());
                if (!inBaseDir) {
                    logWarning("Not allowed to create " + folderInfo.getName()
                        + " at " + folderSettings.getLocalBaseDir()
                        + ". Must be in base directory: " + getFoldersBasedir());
                    throw new IllegalStateException("Not allowed to create "
                        + folderInfo.getName() + " at "
                        + folderSettings.getLocalBaseDir()
                        + ". Must be in base directory: " + getFoldersBasedir());
                }
            }
        }

        // PFC-2572
        if (!ConfigurationEntry.FOLDER_CREATE_ALLOW_NETWORK
            .getValueBoolean(getController()))
        {
            if (PathUtils.isNetworkPath(folderSettings.getLocalBaseDir())) {
                if (saveConfig) {
                    getController().saveConfig();
                }
                logWarning("Not allowed to create " + folderInfo.getName()
                    + " at " + folderSettings.getLocalBaseDir()
                    + ". Network shares not allowed");
                throw new IllegalStateException("Not allowed to create "
                    + folderInfo.getName() + " at "
                    + folderSettings.getLocalBaseDir()
                    + ". Network shares not allowed");
            }
        }

        if (Feature.FOLDER_ATOMIC_COMMIT.isEnabled()
            && folderSettings.getCommitDir() == null)
        {
            Path newBaseDir = folderSettings.getLocalBaseDir().resolve(
                Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR);
            try {
                Files.createDirectories(newBaseDir);
            } catch (IOException e) {
                logInfo(e.getMessage());
            }
            PathUtils.setAttributesOnWindows(newBaseDir, true, true);
            Path commitDir = folderSettings.getLocalBaseDir();
            SyncProfile syncProfile = SyncProfile.NO_SYNC;

            folderSettings = new FolderSettings(newBaseDir, syncProfile,
                folderSettings.getDownloadScript(),
                folderSettings.getVersions(), folderSettings.isSyncPatterns(),
                commitDir, folderSettings.getSyncWarnSeconds());
            logWarning("Auto-commit setup. temp dir: " + newBaseDir
                + ". commit dir:" + commitDir);
        }

        //PFS-1918: Start: Folder WebDAV support. Mount this folder as WebDAV resource.
        if (folderSettings.getLocalBaseDirString().
                toLowerCase().startsWith(Constants.FOLDER_WEBDAV_PREFIX) && OSUtil.isLinux()){

            // This is inevitable because the WebDAV URL is initially a path object and path does
            // not support '//' notations.
            String webDAVURL = folderSettings.getLocalBaseDirString();
            if (webDAVURL.contains(Constants.FOLDER_WEBDAV_PREFIX) && !webDAVURL.contains("://")) {
                webDAVURL = webDAVURL.replace(":/", "://");
            }

            // Create physical mount location.
            Path folderDirectory = this.getFoldersBasedir();
            String folderName = PathUtils.removeInvalidFilenameChars(folderInfo.getLocalizedName()
                    + Constants.FOLDER_WEBDAV_SUFFIX);
            Path folderBaseDir = folderDirectory.resolve("webdav").resolve(folderName);

            try {
                // Mount it.
                String mountMessage = LinuxUtil.mountWebDAV(webDAVURL, folderBaseDir);
                if (mountMessage.startsWith("N")){
                    logSevere("Failed to mount folder " +
                            folderInfo.getName() + " as WebDAV resource. Error message: " + mountMessage);
                }
                folderSettings = folderSettings.changeBaseDir(folderBaseDir);
            } catch (MalformedURLException e) {
                logSevere("Failed to mount folder " +
                        folderInfo.getName() + " as WebDAV resource @ " + e, e);
            }
        } else if (folderSettings.getLocalBaseDir().toString().contains(Constants.FOLDER_WEBDAV_SUFFIX) && !OSUtil.isLinux()){
            logSevere("WebDAV folder mounting is only supported with Linux as operating system.");
            throw new IllegalStateException("WebDAV folder mounting is only supported with Linux as operating system.");
        }
        //PFS-1918: End: Folder WebDAV support. Mount this folder as WebDAV resource.

        Folder folder;
        if (folderSettings.isPreviewOnly()) {
            // Need to use preview folder settings.
            FolderSettings previewFolderSettings = FolderPreviewHelper
                .createPreviewFolderSettings(folderInfo.getName());
            folder = new Folder(getController(), folderInfo,
                previewFolderSettings);
        } else {
            folder = new Folder(getController(), folderInfo, folderSettings);
        }
        folder.addProblemListener(valveProblemListenerSupport);

        // Now create metaFolder and map to the same FolderInfo key.
        FolderInfo metaFolderInfo = new FolderInfo(
            Constants.METAFOLDER_ID_PREFIX + folderInfo.getName(),
            Constants.METAFOLDER_ID_PREFIX + folderInfo.id);
        Path systemSubdir = folder.getSystemSubDir();
        FolderSettings metaFolderSettings = new FolderSettings(systemSubdir
                .resolve(Constants.METAFOLDER_SUBDIR),
            SyncProfile.META_FOLDER_SYNC, 0);
        boolean deviceDisconnected = folder.checkIfDeviceDisconnected();
        if (!deviceDisconnected) {
            try {
                if (Files.notExists(metaFolderSettings.getLocalBaseDir())) {
                    Files.createDirectory(metaFolderSettings.getLocalBaseDir());
                }
            } catch (IOException ioe) {
                logInfo("Unable to create metafolder directory: "
                    + metaFolderSettings.getLocalBaseDir() + "."
                    + ioe.toString());
            }
        }
        Folder metaFolder = new Folder(getController(), metaFolderInfo,
            metaFolderSettings);
        if (!deviceDisconnected) {
            try {
                if (Files.notExists(metaFolder.getSystemSubDir())) {
                    Files.createDirectory(metaFolder.getSystemSubDir());
                }
            } catch (IOException e) {
                // Ignore.
            }
        }

        // Set datamodel
        metaFolders.put(folderInfo, metaFolder);
        folders.put(folder.getInfo(), folder);
        saveFolderConfig(folderInfo, folderSettings, saveConfig);

        if (!metaFolder.hasOwnDatabase()) {
            // Scan once. To get it working.
            metaFolder.setSyncProfile(SyncProfile.MANUAL_SYNCHRONIZATION);
            metaFolder.recommendScanOnNextMaintenance(true);
        }
        logFine("Created metaFolder " + metaFolderInfo.getName()
            + ", local copy at '" + metaFolderSettings.getLocalBaseDir() + '\'');

        // Synchronize folder memberships
        triggerSynchronizeAllFolderMemberships();

        // Calc stats
        folder.getStatistic().scheduleCalculate();

        // Trigger scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requestor
        fileRequestor.triggerFileRequesting(folder.getInfo());

        // Fire event
        fireFolderCreated(folder);

        if (isFine()) {
            String message = "Setup folder " + folderInfo.getLocalizedName()
                + " at " + folder.getLocalBase();
            logFine(message);
        }

        removeFromIgnoredFolders(folder);

        return folder;
    }

    /**
     * Saves settings and info details to the config.
     *
     * @param folderInfo
     * @param folderSettings
     * @param saveConfig
     */
    public void saveFolderConfig(FolderInfo folderInfo,
        FolderSettings folderSettings, boolean saveConfig)
    {
        // store folder in config
        Properties config = getController().getConfig();

        folderSettings.set(folderInfo, config);

        if (saveConfig) {
            getController().saveConfig();
        }
    }

    /**
     * Removes a folder from active folders, will be added as non-local folder
     *
     * @param folder
     * @param deleteSystemSubDir
     */
    public void removeFolder(Folder folder, boolean deleteSystemSubDir) {
        removeFolder(folder, deleteSystemSubDir, true);
    }

    /**
     * Removes a folder from active folders, will be added as non-local folder
     *
     * @param folder
     * @param deleteSystemSubDir
     * @param saveConfig
     */
    public void removeFolder(Folder folder, boolean deleteSystemSubDir,
        boolean saveConfig)
    {
        Reject.ifNull(folder, "Folder is null");

        try {
            scanBasedirLock.lock();

            // Remove link if it exists.
            removeLink(folder);

            // Remove the desktop shortcut
            folder.removeDesktopShortcut();

            // Detach any problem listeners.
            folder.clearAllProblemListeners();

            // Remove desktop ini if it exists
            if (OSUtil.isWindowsSystem()) {
                PathUtils.deleteDesktopIni(folder.getLocalBase());
            }

            // remove folder from config
            removeConfigEntries(folder.getConfigEntryId());

            // Save config
            if (saveConfig) {
                getController().saveConfig();
            }

            // Shutdown meta folder as well
            Folder metaFolder = getMetaFolderForParent(folder.getInfo());
            if (metaFolder != null) {
                metaFolders.remove(metaFolder.getInfo());
                metaFolders.remove(folder.getInfo());

                // Break transfers
                getController().getTransferManager().breakTransfers(
                        metaFolder.getInfo());

                metaFolder.shutdown();
            }

            // Remove internal
            folders.remove(folder.getInfo());
            folder.removeProblemListener(valveProblemListenerSupport);

            // Break transfers
            getController().getTransferManager().breakTransfers(
                folder.getInfo());

            // Shutdown folder
            folder.shutdown();

            // synchronize memberships
            triggerSynchronizeAllFolderMemberships();

            // Abort scanning
            boolean folderCurrentlyScannng = folder.equals(folderScanner
                .getCurrentScanningFolder());
            if (folderCurrentlyScannng) {
                folderScanner.abortScan();
            }

            // Delete the .PowerFolder dir and contents
            if (deleteSystemSubDir) {
                // Sleep a couple of seconds for things to settle,
                // before removing dirs, to avoid conflicts.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }

                try {
                    PathUtils.recursiveDeleteVisitor(folder.getSystemSubDir());
                } catch (IOException e) {
                    logSevere("Failed to delete: " + folder.getSystemSubDir(), e);
                }

                if (!PathUtils.isZyncroPath(folder.getLocalBase()) && !PathUtils.isWebDAVFolder(folder.getLocalBase())) {
                    // Remove the folder if totally empty.
                    Path localBase = folder.getLocalBase();
                    try {
                        if (EncryptedFileSystemUtils.isCryptoInstance(localBase) && PathUtils.isEmptyDir(localBase)) {
                            PathUtils.recursiveDeleteVisitor(EncryptedFileSystemUtils.getPhysicalStorageLocation(localBase));
                        } else {
                            Files.delete(localBase);
                        }
                    } catch (DirectoryNotEmptyException | NoSuchFileException e) {
                        // this can happen, and is just fine
                    } catch (IOException ioe) {
                        logSevere("Failed to delete local base: "
                                + localBase.toAbsolutePath() + ": "
                                + ioe);
                    }
                }
            }

        } finally {
            scanBasedirLock.unlock();
        }

        // Fire event
        fireFolderRemoved(folder);

        // Trigger sync on other folders.
        fileRequestor.triggerFileRequesting();

        if (isFine()) {
            logFine(folder + " removed");
        }

    }

    /**
     * Remove the link to the folder if it exists.
     *
     * @param folder
     */
    private void removeLink(Folder folder) {
        FolderRepository repository = getController().getFolderRepository();
        Path baseDir = repository.getFoldersBasedir();
        if (Files.exists(baseDir)) {
            try {
                Path shortcutFile = baseDir.resolve(folder.getName()
                    + Constants.LINK_EXTENSION);
                boolean deleted = Files.deleteIfExists(shortcutFile);
                logFine("Removed link " + shortcutFile.getFileName() + "? "
                    + deleted);
            } catch (Exception e) {
                logWarning(e.getMessage());
            }
        }
    }

    public void addToIgnoredFolders(Folder folder) {
        Path path = folder.getLocalBase();
        if (ignoredFolderDirectories.add(path)) {
            StringBuilder sb = new StringBuilder();
            Iterator<Path> iterator = ignoredFolderDirectories.iterator();
            while (iterator.hasNext()) {
                String s = iterator.next().toAbsolutePath().toString();
                sb.append(s);
                if (iterator.hasNext()) {
                    sb.append('$');
                }
            }
            ConfigurationEntry.AUTO_SETUP_FOLDERS_IGNORED.setValue(getController(),
                sb.toString());
        }
    }

    private void removeFromIgnoredFolders(Folder folder) {
        if (ignoredFolderDirectories.remove(folder.getLocalBase())) {
            StringBuilder sb = new StringBuilder();
            Iterator<Path> iterator = ignoredFolderDirectories.iterator();
            while (iterator.hasNext()) {
                String s = iterator.next().toAbsolutePath().toString();
                sb.append(s);
                if (iterator.hasNext()) {
                    sb.append('$');
                }
            }
            ConfigurationEntry.AUTO_SETUP_FOLDERS_IGNORED.setValue(getController(),
                sb.toString());
        }
    }

    /**
     * Removes a member from all Folders.
     *
     * @param member
     */
    public void removeFromAllFolders(Member member) {
        if (isFiner()) {
            logFiner("Removing node from all folders: " + member);
        }
        for (Folder folder : getFolders(true)) {
            folder.remove(member);
        }
        if (isFiner()) {
            logFiner("Node removed from all folders: " + member);
        }
    }

    /**
     * Triggers the synchronization of all known members with our folders. The
     * work is done in background thread. Former synchronization processed the
     * canceled.
     */
    public void triggerSynchronizeAllFolderMemberships() {
        if (!started) {
            logFiner("Not synchronizing Foldermemberships, repo not started, yet");
        }
        synchronized (folderMembershipSynchronizerLock) {
            if (folderMembershipSynchronizer != null) {
                // Cancel the syncer
                folderMembershipSynchronizer.canceled.set(true);
            }
            folderMembershipSynchronizer = new AllFolderMembershipSynchronizer();
            getController().getIOProvider().startIO(
                folderMembershipSynchronizer);
        }
    }

    /**
     * Broadcasts a remote scan commando on all folders.
     */
    public void broadcastScanCommandOnAllFolders() {
        if (log.isLoggable(Level.FINE)) {
            logFine("Sending remote scan commando");
        }
        for (Folder folder : getFolders(true)) {
            folder.broadcastScanCommand();
        }
    }

    /**
     * @return the folder that currently gets maintainted or null if not
     *         maintaining any folder.
     */
    public Folder getCurrentlyMaintainingFolder() {
        return currentlyMaintainingFolder;
    }

    /**
     * Triggers the maintenance on all folders. may or may not scan the folders
     * - depending on settings.
     */
    public void triggerMaintenance() {
        if (isFiner()) {
            logFiner("Scan triggerd");
        }
        triggered = true;
        synchronized (scanTrigger) {
            scanTrigger.notifyAll();
        }
    }

    /**
     * Mainenance thread for the folders
     */
    public void run() {

        // 1000 ms wait
        long waitTime = Controller.getWaitTime() / 5;

        // Wait to build up ui
        Waiter w = new Waiter(30L * 1000);
        while (!w.isTimeout()) {
            if (getController().isUIEnabled() && getController().isUIOpen()) {
                break;
            }
            if (getController().isStarted()) {
                break;
            }
            try {
                w.waitABit();
            } catch (Exception e) {
                return;
            }
        }

        List<Folder> scanningFolders = new ArrayList<Folder>();
        Controller controller = getController();

        while (!myThread.isInterrupted() && myThread.isAlive()) {
            // Only scan if not in paused mode
            if (!controller.isPaused()) {
                scanningFolders.clear();
                for (Folder folder : folders.values()) {
                    if (folder.isMaintenanceRequired()) {
                        scanningFolders.add(folder);
                    }
                }
                for (Folder metaFolder : metaFolders.values()) {
                    if (metaFolder.isMaintenanceRequired()) {
                        scanningFolders.add(metaFolder);
                    }
                }
                Collections.sort(scanningFolders, FolderComparator.INSTANCE);
                if (isFiner()) {
                    logFiner("Maintaining " + scanningFolders.size()
                        + " folders...");
                }
                for (Folder folder : scanningFolders) {
                    currentlyMaintainingFolder = folder;
                    // Fire event
                    fireMaintanceStarted(currentlyMaintainingFolder);
                    try {
                        currentlyMaintainingFolder.maintain();
                    } catch (RuntimeException e) {
                        // PFS-2000:
                        logWarning("Unable to maintain folder "
                            + currentlyMaintainingFolder.getName() + "/"
                            + currentlyMaintainingFolder.getId() + ": " + e, e);
                    }
                    Folder maintainedFolder = currentlyMaintainingFolder;
                    currentlyMaintainingFolder = null;
                    // Fire event
                    fireMaintenanceFinished(maintainedFolder);

                    if (controller.isPaused() || myThread.isInterrupted()) {
                        break;
                    }

                    // Wait a bit to give other waiting sync processes time...
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (isFiner()) {
                    logFiner("Maintained " + scanningFolders.size()
                        + " folder(s)");
                }
            }

            if (!triggered) {
                try {
                    // use waiter, will quit faster
                    synchronized (scanTrigger) {
                        scanTrigger.wait(waitTime);
                    }
                } catch (InterruptedException e) {
                    logFiner(e);
                    break;
                }
            }
            triggered = false;
        }
    }

    /*
     * General
     */

    @Override
    public String toString() {
        return "Folders of " + getController().getMySelf().getNick();
    }

    /**
     * ATTENTION: This is a stack based system. When suspending the search do it
     * only ONCE and make sure you release the lock in a finally block Can be
     * set by the UI when we are creating folders so that lookForNewFolders does
     * not jump in while the user is setting up a new folder in a Wizard or
     * something. Don't forget to set this back to false when finished.
     *
     * @param activity
     */
    public void setSuspendNewFolderSearch(boolean activity) {
        if (activity) {
            suspendNewFolderSearch.incrementAndGet();
        } else {
            suspendNewFolderSearch.decrementAndGet();
        }
        logFine("setSuspendNewFolderSearch to " + activity + " now: "
            + suspendNewFolderSearch.get());
    }

    /**
     * First, scan the base dir for new folders. Second, scan the base dir for
     * folders that can be removed.<br />
     * <br />
     * This method is synchronized with
     * {@link #handleDeviceDisconnected(Folder)}.
     * @return true if executed, false if skipped, e.g. pause
     */
    public boolean scanBasedir() {
        if (suspendNewFolderSearch.get() > 0) {
            return false;
        }
        // sync with #handleDeviceDisconnectd(Folder)
        scanBasedirLock.lock();
        boolean ok = false;
        try {
            if (ConfigurationEntry.LOOK_FOR_FOLDER_CANDIDATES
                .getValueBoolean(getController()))
            {
                ok = lookForNewFolders();
            }
            if (ConfigurationEntry.LOOK_FOR_FOLDERS_TO_BE_REMOVED
                .getValueBoolean(getController()))
            {
                ok = lookForFoldersToBeRemoved() && ok;
            }
        } finally {
            scanBasedirLock.unlock();
        }
        return ok;
    }

    /**
     * Scan the PowerFolder base directory for new directories that might be new
     * folders.
     * @return true if successfully checked for new folders. false is skipped/failed
     */
    private boolean lookForNewFolders() {
        if (!getMySelf().isServer()) {
            if (!getController().getOSClient().isLoggedIn()) {
                if (isFine()) {
                    logFine("Skipping searching for new folders...");
                }
                return false;
            }
            if (!getController().getOSClient().isAllowedToCreateFolders()) {
                if (isFine()) {
                    logFine("Skipping searching for new folders (no permission)...");
                }
                return false;
            }
            Account a = getController().getOSClient().getAccount();
            if (!a.hasOwnStorage()) {
                if (isFine()) {
                    logFine("Account "
                        + a.getUsername()
                        + " does not have storage, not checking for new folders.");
                }
                if (getController().isUIEnabled()) {
                    WarningNotice notice = new WarningNotice(
                        Translation.get("warning_notice.title"),
                        Translation
                            .get("warning_notice.no_folder_create_summary"),
                        Translation
                            .get("warning_notice.no_folder_create_message"));
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().handleNotice(notice);
                }
                return false;
            }
        }
        if (getController().isPaused()) {
            logFine("Skipping searching for new folders (paused)...");
            return false;
        }

        if (isFine()) {
            logFine("Searching for new folders...");
        }
        // TODO BOTTLENECK: Takes much CPU -> Implement via jnotify
        Path baseDir = getFoldersBasedir();
        if (Files.notExists(baseDir) || !Files.isReadable(baseDir)) {
            return false;
        }
        // Get all directories
        Filter<Path> filter = entry -> {
            String name = entry.getFileName().toString();
            if (name.equals(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                return false;
            }
            if (name.equals(ConfigurationEntry.FOLDER_BASEDIR_DELETED_DIR
                .getValue(getController()))
                || name
                    .equals(ConfigurationEntry.FOLDER_BASEDIR_DELETED_DIR
                        .getDefaultValue()))
            {
                return false;
            }
            // PFS-2319: Remove after major version 14.
            if (name.equals("RECOVERED")) {
                return false;
            }
            if (name.equalsIgnoreCase(DIRNAME_SNAPSHOT)) {
                return false;
            }
            if (!Files.isDirectory(entry)) {
                return false;
            }

            if (name.contains(Constants.FOLDER_ENCRYPTION_SUFFIX)) {
                return false;
            }

            /* PFC-1905: Ignore folders with webdav suffix */
            if (name.endsWith(Constants.FOLDER_WEBDAV_SUFFIX)) {
                return false;
            }

            return !isIgnoredFolderDirectory(entry);
        };

        try (DirectoryStream<Path> directories = Files.newDirectoryStream(
            baseDir, filter)) {
            for (Path dir : directories) {
                boolean known = false;
                for (Folder folder : getFolders()) {
                    if (!getMySelf().isServer()) {
                        if (folder.getName().equals(dir.getFileName().toString())) {
                            known = true;
                            break;
                        }
                    }
                    Path localBase = folder.getLocalBase();

                    // PFS-2871: Functionality to setup directories behind symlinks as folders.
                    if (Files.isSymbolicLink(localBase)){
                        localBase = localBase.toRealPath();
                    }

                    if (Files.isSymbolicLink(dir)){
                        dir = dir.toRealPath();
                    }

                    if (EncryptedFileSystemUtils.isCryptoInstance(localBase)){
                        localBase = EncryptedFileSystemUtils.getPhysicalStorageLocation(localBase);
                    }

                    if (localBase.equals(localBase.getFileSystem().getPath(dir.toString()))
                            || localBase.toAbsolutePath().startsWith(localBase.getFileSystem().getPath(dir.toAbsolutePath().toString()))
                            || localBase.toAbsolutePath().startsWith(dir.toAbsolutePath())
                            || localBase.equals(dir)) {
                        known = true;
                        break;
                    }

                }
                if (!known) {
                    handleNewFolder(dir);
                }
            }
        } catch (IOException ioe) {
            logWarning("Could not access base dir while looking for new folders @ " + baseDir.toString() + ". " + ioe.toString());
            return false;
        }

        if (!getMySelf().isServer() && getController().isUIEnabled()) {
            filter = entry -> {
                if (Files.isDirectory(entry)) {
                    return false;
                }
                if (PathUtils.isDesktopIni(entry)) {
                    return false;
                }
                if (entry.getFileName().toString().toLowerCase()
                    .endsWith(".lnk"))
                {
                    return false;
                }
                if (entry
                    .getFileName()
                    .toString()
                    .equalsIgnoreCase(
                        Constants.GETTING_STARTED_GUIDE_FILENAME))
                {
                    return false;
                }
                try {
                    if (Files.isHidden(entry)) {
                        return false;
                    }
                } catch (IOException e) {
                    logFine("Could not find out if '"
                        + entry.toAbsolutePath().toString()
                        + "' is hidden. " + e);
                    return false;
                }
                return true;
            };

            // Clear all FileInBasePathWarnings before generating new ones
            getController().getUIController().getApplicationModel().getNoticesModel().clearAllFileInBasePathWarnings();
            // Search for files in base path
            try (DirectoryStream<Path> files = Files.newDirectoryStream(
                baseDir, filter)) {
                for (Path file : files) {
                    WarningNotice notice = new FileInBasePathWarning(Translation
                        .get("notice.file_in_base_path.title"), Translation
                        .get("notice.file_in_base_path.message"), Translation
                        .get("notice.file_in_base_path.summary", file.getFileName().toString(), getController().getDistribution().getName(), file.toString()));

                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().handleNotice(notice);
                }
            } catch (IOException ioe) {
                logWarning(ioe);
            }
        }
        return true;
    }

    /**
     * Check for the whole path, and for the file name only.
     *
     * @param entry
     *            The path to check
     * @return {@code True} if the path or file name is in the removed
     *         folders list, {@code false} otherwise.
     */
    private boolean isIgnoredFolderDirectory(Path entry) {
        boolean ignored = ignoredFolderDirectories
            .contains(entry);
        boolean isRenamedFolder = checkSystemSubdirForFolder(entry) != null && isFolderRenamed(entry);

        // return early, don't iterate the whole list again.
        if (ignored) {
            if (isRenamedFolder) {
                ignoredFolderDirectories.remove(entry);
                return false;
            }
            return true;
        }

        if (entry.getFileName() == null) {
            return false;
        }

        for (Path ignoredPath : ignoredFolderDirectories) {
            if (ignoredPath.getFileName() == null) {
                continue;
            }
            if (ignoredPath.getFileName().equals(entry.getFileName())) {
                ignored = true;
            }
        }
        return ignored;
    }

    /**
     * Check the file name of the {@code entry} to be different from the Folder
     * name of the FolderStatisticInfo in the meta data of the folder.
     *
     * @param entry
     *            A path that might be a location of a folder.
     * @return {@code True} if the name of {@code entry} is different from the
     *         one in the statistics, {@code false} otherwise.
     */
    private boolean isFolderRenamed(Path entry) {
        Path meta = entry.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR).resolve(
            Folder.FOLDER_STATISTIC);
        FolderStatisticInfo info = FolderStatisticInfo.load(meta);

        if (info == null) {
            return false;
        }

        if (entry.getFileName() == null) {
            return false;
        }

        String fileName = entry.getFileName().toString();
        String folderName = info.getFolder().getLocalizedName();

        return !fileName.equals(folderName);
    }

    // Found a new directory in the folder base. Create a new folder.
    // Only doing this if logged in.
    private void handleNewFolder(Path file) {
        if (getController().isShuttingDown() || !getController().isStarted()) {
            return;
        }
        FolderInfo foInfo = null;
        boolean renamedOnServer = false;
        boolean stillPresent = false;
        boolean createdNew = false;
        ServerClient client = getController().getOSClient();

        if (client.isConnected() && client.isLoggedIn()) {
            foInfo = checkSystemSubdirForFolder(file);
            stillPresent = folderStillExists(foInfo);

            try {
                if (foInfo != null) {
                    Folder existingFolder = foInfo.getFolder(getController());
                    if (existingFolder != null && existingFolder.checkIfDeviceDisconnected()) {
                        removeFolder(existingFolder, false, false);
                    }
                    FolderInfo renamedFI = tryRenaming(client, file, foInfo, stillPresent);
                    if (renamedFI != null && foInfo != null && renamedFI.equals(foInfo)
                            && !renamedFI.getName().equals(foInfo.getName())) {
                        foInfo = renamedFI;
                        renamedOnServer = true;
                    }
                }
            } catch (FolderRenameException fre) {
                logInfo("Could not rename Folder on server " + fre);
                logFine(fre);
            }
        } else if (getMySelf().isServer()) {
            foInfo = checkSystemSubdirForFolder(file);
            stillPresent = folderStillExists(foInfo);
        }

        logInfo("handleNewFolder.p1 of " + file);
        if (foInfo != null) {
            logInfo(" Folder name: " + foInfo.getLocalizedName());
            logInfo(" Folder ID: " + foInfo.getId());
        }
        logInfo(" renamedOnServer: " + renamedOnServer);
        logInfo(" stillPresent: " + stillPresent);
        logInfo(" createdNew: " + createdNew);

        if (foInfo == null || stillPresent) {
            foInfo = new FolderInfo(file.getFileName().toString(),
                IdGenerator.makeFolderId());
            createdNew = true;
        } else {
            if (!getController().getSecurityManager().hasPermission(
                getMySelf().getInfo(), FolderPermission.read(foInfo)))
            {
                cleanupMetaInformation(file);
                foInfo = new FolderInfo(file.getFileName().toString(),
                    IdGenerator.makeFolderId()).intern();
                createdNew = true;
            } else {
                createdNew = false;
            }
        }
        FolderSettings fs = new FolderSettings(file,
            SyncProfile.getDefault(getController()),
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                .getValueInt(getController()));

        // 1) Create at cloud service
        boolean scheduleCreateOnServer = false;
        if (client.isBackupByDefault() && !client.joinedByCloud(foInfo)) {
            // Make sure it is backed up by the server.
            try {
                // Do it synchronous. Otherwise we might get race conditions.
                getController().getOSClient().getFolderService()
                    .createFolder(foInfo, null);
                if (fs != null) {
                    getController().getOSClient().getFolderService()
                        .setArchiveMode(foInfo, fs.getVersions());
                }
            } catch (Exception e) {
                scheduleCreateOnServer = true;
            }
        }

        // 2) Sync locally
        Folder folder = createFolder0(foInfo, fs, true);
        folder.addDefaultExcludes();

        if (scheduleCreateOnServer) {
            logFine("Scheduling setup of folder: " + foInfo.getName());
            CreateFolderOnServerTask task = new CreateFolderOnServerTask(foInfo,
                null);
            task.setArchiveVersions(fs.getVersions());
            getController().getTaskManager().scheduleTask(task);
        }

        logInfo("Auto-setup " + (createdNew ? "new" : "existing") + " folder: "
            + folder.getName() + "/" + folder.getId() + " @ "
            + folder.getLocalBase());

        if (!renamedOnServer && createdNew) {
            folderAutoCreateListener
                .folderAutoCreated(new FolderAutoCreateEvent(foInfo));
        }

        logInfo("handleNewFolder.p2 of " + file);
        logInfo(" Folder name: " + foInfo.getLocalizedName());
        logInfo(" Folder ID: " + foInfo.getId());
        logInfo(" renamedOnServer: " + renamedOnServer);
        logInfo(" stillPresent: " + stillPresent);
        logInfo(" createdNew: " + createdNew);

    }

    /**
     * Remove information about other members of a folder at {@code basedir}.
     * <ol>
     * <li>(meta)Members</li>
     * <li>(meta)Locks</li>
     * <li>(systemSubdir)FolderStatistic</li>
     * <li>(systemSubdir).PowerFolder.db(.bak)</li>
     * </ol>
     *
     * @param basedir
     */
    private void cleanupMetaInformation(Path basedir) {
        Reject.ifNull(basedir, "Base dir is null");
        try {
            Path systemSubdir = basedir
                .resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR);
            Path metaSubfolder = systemSubdir
                .resolve(Constants.METAFOLDER_SUBDIR);

            // remove members
            Files.deleteIfExists(
                metaSubfolder.resolve(Folder.METAFOLDER_MEMBERS));
            // remove folder statistic
            Files.deleteIfExists(systemSubdir.resolve(Folder.FOLDER_STATISTIC));
            // remove database
            Files.deleteIfExists(systemSubdir.resolve(Constants.DB_FILENAME));
            Files.deleteIfExists(
                systemSubdir.resolve(Constants.DB_BACKUP_FILENAME));
            // remove locks
            PathUtils.recursiveDelete(
                metaSubfolder.resolve(Folder.METAFOLDER_LOCKS_DIR));
        } catch (IOException e) {
            logInfo("Could not delete members list and/or lock files in case of possible hijack. " + e);
        }
    }

    /**
     * First check, if there is a Folder with the name equal to the {@code file}
     * 's name. Only if {@code fi} is not {@code null}, there is no known Folder
     * with the same name, the new and old name are not equal and
     * {@code stillPresent} is false, the Folder is renamed locally and on the
     * remote server.
     *
     * @param client
     *            The client to check for the currently logged in users folders.
     * @param file
     *            The file that is to be handled as a new folder
     * @param foInfo
     *            A Folder that might be the one found at {@code file}
     * @param stillPresent
     *            Is the Folder {@code fi} still present?
     * @return A FolderInfo containing renamed information, an already known
     *         Folder at {@code file} or {@code null} if it is a new Folder.
     * @throws FolderRenameException
     *             If the server was not able to rename the folder or any other
     *             exception occured during the renameing process
     */
    private FolderInfo tryRenaming(ServerClient client, Path file,
        FolderInfo foInfo, boolean stillPresent) throws FolderRenameException
    {
        Folder folder = foInfo.getFolder(getController());
        FolderInfo knownFolderWithSameName = null;

        for (FolderInfo folderInfo : client.getAccountFolders()) {
            if (folderInfo.getLocalizedName().equals(
                file.getFileName().toString()))
            {
                knownFolderWithSameName = folderInfo;
                break;
            }
        }

        if (foInfo == null) {
            return knownFolderWithSameName;
        }

        String oldName = foInfo.getName();
        String newName = file.getFileName().toString();

        logInfo("tryRenaming");
        logInfo(" file: " + file);
        logInfo(" name: " + foInfo.getName());
        logInfo(" ID: " + foInfo.getId());
        logInfo(" knownFolderWithSameName: " + knownFolderWithSameName);
        logInfo(" stillPresent: " + stillPresent);

        if (knownFolderWithSameName == null
            && !PathUtils.isSameName(oldName, newName) && !stillPresent)
        {
            /*
             * Change the name locally before the server is called. The
             * server will notify all clients to update their folder names.
             * Renaming the folder first prevents that the client which
             * renamed the folder changes it via the server's update.
             */
            logInfo("Renaming Folder '" + oldName + "' to '" + newName + "'");

            if (folder != null && folder.checkIfDeviceDisconnected()) {
                removeFolder(folder, false, false);
                ignoredFolderDirectories.remove(folder.getLocalBase());
            }

            FolderService foServ = client.getFolderService();
            try {
                boolean isFolderAdmin = getController().getSecurityManager().hasPermission(
                        getMySelf().getInfo(), FolderPermission.admin(foInfo));
                if (isFolderAdmin && foServ.renameFolder(foInfo, newName)) {
                    foInfo = new FolderInfo(newName, foInfo.getId());
                    foInfo.intern(true);
                } else {
                    logWarning("Could not rename the Folder " + oldName
                            + " on the server to " + foInfo.getName());
                    final String copyNewName = newName;
                    final String copyOldName = oldName;

                    if (getController().isUIEnabled() && getController().getUIController().isStarted()) {
                        // FIXME: Use Notifications instead of in-your-face dialog:
                        UIUtil.invokeLaterInEDT(() -> DialogFactory.genericDialog(
                                getController(),
                                Translation
                                        .get("notice.rename_folder_failed.title"),
                                Translation
                                        .get(
                                                "notice.rename_folder_failed.summary",
                                                copyNewName,
                                                copyOldName),
                                GenericDialogType.WARN));
                    }
                    throw new FolderRenameException(file, foInfo);
                }
            } catch (de.dal33t.powerfolder.security.SecurityException e) {
                logWarning(e.getMessage());
                throw new FolderRenameException(file, foInfo, e);
            } catch (RuntimeException e) {
                logSevere("Unable to rename folder: " + oldName + ": " + e, e);
                throw new FolderRenameException(file, foInfo, e);
            }
        }

        return foInfo;
    }

    /**
     * Check if the Folder referenced by {@code fi} is still known, the base
     * directory exists and it contains the folder statistic subdirectory.
     *
     * @param fi
     * @return {@code True} if the folder is known to PowerFolder, its base
     *         directory exists and the statistic subdirectory exists and is connected to storage.
     *         {@code False} otherwise.
     */
    private boolean folderStillExists(FolderInfo fi) {
        if (fi == null) {
            return false;
        }

        Folder fo = fi.getFolder(getController());
        if (fo == null) {
            return false;
        }

        Path base = fo.getLocalBase();

        if (Files.notExists(base)) {
            return false;
        }

        Path systemSubDir = fo.getSystemSubDir().resolve(
            Folder.FOLDER_STATISTIC);

        if (Files.notExists(systemSubDir)) {
            return false;
        }

        if (fo.checkIfDeviceDisconnected()) {
            return false;
        }

        return true;
    }

    /**
     * Checks for the meta directory in the {@code file} to determine if this
     * file points to a directory, that is already a {@link Folder}.<br />
     * <br />
     * This method takes a look at the {@link FolderStatisticInfo} stored in the
     * meta direcoty of the Folder to get the {@link FolderInfo}.
     *
     * @param file
     * @return The {@link FolderInfo} of the Folder the file points to, or
     *         {@code null}, if the file does not point to a Folder.
     */
    private FolderInfo checkSystemSubdirForFolder(Path file) {

        Path meta = file.resolve(Constants.POWERFOLDER_SYSTEM_SUBDIR).resolve(
            Folder.FOLDER_STATISTIC);
        FolderStatisticInfo info = FolderStatisticInfo.load(meta);

        if (info == null) {
            return null;
        }

        return info.getFolder();
    }

    /**
     * Scan the PowerFolder base directory for directories that should be
     * deleted.
     * @return true if executed. false if not executed, e.g. not logged in, paused, etcs
     */
    private boolean lookForFoldersToBeRemoved() {
        if (!getMySelf().isServer()) {
            if (!getController().getOSClient().isLoggedIn()) {
                if (isFine()) {
                    logFine("Skipping searching for folders to be removed...");
                }
                return false;
            }
            if (!ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                .getValueBoolean(getController()))
            {
                if (isFine()) {
                    logFine("Skipping searching for folders to be deleted (no strict security)...");
                }
                return false;
            }
        }
        if (isFine()) {
            logFine("Searching for folders to be removed...");
        }
        // TODO BOTTLENECK: Takes much CPU -> Implement via jnotify
        Path baseDir = getFoldersBasedir();
        if (Files.notExists(baseDir) || !Files.isReadable(baseDir)) {
            return false;
        }
        final Set<String> ignoredFoldersLC = new HashSet<>();
        Account a = getController().getOSClient().getAccount();
        for (FolderInfo foInfo : a.getFolders()) {
            ignoredFoldersLC
                .add(foInfo.getLocalizedName().toLowerCase().trim());
        }

        // Get all directories
        Filter<Path> filter = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String name = entry.getFileName().toString();
                if (name.equals(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                    return false;
                }
                if (name.equals(ConfigurationEntry.FOLDER_BASEDIR_DELETED_DIR
                    .getValue(getController()))
                    || name
                        .equals(ConfigurationEntry.FOLDER_BASEDIR_DELETED_DIR
                            .getDefaultValue()))
                {
                    return false;
                }
                if (name.equalsIgnoreCase(DIRNAME_SNAPSHOT)) {
                    return false;
                }
                if (ignoredFoldersLC.contains(name.toLowerCase())) {
                    return false;
                }
                if (!Files.isDirectory(entry)) {
                    return false;
                }
                return true;
            }
        };

        try (DirectoryStream<Path> directories = Files.newDirectoryStream(
            baseDir, filter)) {
            for (Path dir : directories) {
                boolean known = false;
                for (Folder folder : getFolders()) {
                    Path localBase = folder.getLocalBase();
                    if (localBase.equals(dir)
                        || localBase.toAbsolutePath().startsWith(
                            dir.toAbsolutePath()))
                    {
                        known = true;
                        break;
                    }
                }
                if (known) {
                    // Is know/a shared folder. don't delete
                    continue;
                }

                // If directory is not mapped to a folder, delete it
                String deletedBaseDir = ConfigurationEntry.FOLDER_BASEDIR_DELETED_DIR
                    .getValue(getController());
                if (StringUtils.isNotBlank(deletedBaseDir)) {
                    Path deletedTargetDir = PathUtils.createEmptyDirectory(
                        getFoldersBasedir().resolve(deletedBaseDir), dir
                            .getFileName().toString());
                    PathUtils.recursiveMove(dir, deletedTargetDir);
                } else {
                    PathUtils.recursiveDelete(dir);
                }
            }
        } catch (IOException ioe) {
            logWarning(ioe.getMessage());
            return false;
        }
        return true;
    }

    /**
     * In sync = all folders are 100% synced and all syncing actions have
     * stopped.
     *
     * @return true if all folders are 100% in sync
     */
    public boolean isInSync() {
        for (Folder folder : getFolders()) {
            if (!folder.isInSync()) {
                if (isWarning()) {
                    logWarning(folder + " not in sync yet");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a metaFolder for a FolderInfo. NOTE: the folderInfo is the parent
     * Folder's FolderInfo, NOT the FolderInfo of the metaFolder. BUT the
     * metaFolders Map key holds the parent FolderInfo
     *
     * @param parentFolderInfo
     *            parent Folder's FolderInfo
     * @return the meta folder.
     */
    public Folder getMetaFolderForParent(FolderInfo parentFolderInfo) {
        return metaFolders.get(parentFolderInfo);
    }

    /**
     * @param metaFolderInfo
     * @return the parent folder for a metaFolder's info.
     */
    public Folder getParentFolder(FolderInfo metaFolderInfo) {
        if (!metaFolderInfo.isMetaFolder()) {
            return null;
        }
        return getFolder(metaFolderInfo.getParentFolderInfo());
    }

    /**
     * Automatically accept an invitation. If not able to, silently return
     * false.
     *
     * @param invitation
     * @return true if the invitation was accepted.
     */
    public boolean autoAcceptInvitation(Invitation invitation) {

        // Defensive strategy: Place in PowerFolders\...

        Path suggestedLocalBase;
        if (ConfigurationEntry.FOLDER_CREATE_USE_EXISTING
            .getValueBoolean(getController()))
        {
            // Moderate strategy. Use existing folders.
            suggestedLocalBase = getController().getFolderRepository()
                .getFoldersBasedir().resolve(invitation.folder.getLocalizedName());
            if (Files.exists(suggestedLocalBase)) {
                logWarning("Using existing directory " + suggestedLocalBase
                    + " for " + invitation.folder);
            }
        } else {
            // Defensive strategy. Find free new empty directory.
            suggestedLocalBase = PathUtils.createEmptyDirectory(getController()
                .getFolderRepository().getFoldersBasedir(),
                invitation.folder.getLocalizedName());
        }

        suggestedLocalBase = PathUtils.removeInvalidFilenameChars(suggestedLocalBase);

        // Is this invitation from a friend?
        boolean invitorIsFriend = false;
        MemberInfo memberInfo = invitation.getSenderDevice();
        if (memberInfo != null) {
            Member node = getController().getNodeManager().getNode(memberInfo);
            if (node != null) {
                invitorIsFriend = node.isFriend();
            }
        }
        if (!invitorIsFriend) {
            logInfo("Not auto accepting " + invitation + " because "
                + memberInfo + " is not a friend.");
            return false;
        }

        logInfo("AutoAccepting " + invitation + " from " + memberInfo + '.');

        FolderSettings folderSettings = new FolderSettings(suggestedLocalBase,
            invitation.getSuggestedSyncProfile(),
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                .getValueInt(getController()));
        createFolder(invitation.folder, folderSettings);
        return true;
    }

    // Callbacks from ServerClient on login ***********************************

    private ReentrantLock accountSyncLock = new ReentrantLock();

    public void updateFolders(AccountDetails ad) {
        Reject.ifNull(ad, "AccountDetails");
        Account a = ad.getAccount();
        if (getMySelf().isServer()) {
            return;
        }
        if (!a.isValid()) {
            return;
        }
        if (!accountSyncLock.tryLock()) {

            // Skip if currently setting up folders.
            // Especially not to remove recently created local folders.
            // Usecase: Client Backup / Personal folders.
            logFine("Skip syncing folder setup with account permissions("
                + a.getFolders() + "): " + a.getUsername());
            return;
        }
        try {
            for (FolderInfo foInfo : a.getFolders()) {
                Folder folder = folders.get(foInfo);
                if (folder == null) {
                    // Not synced locally
                    continue;
                }
                FolderInfo localFolder = folder.getInfo();
                if (PathUtils.isSameName(localFolder.getLocalizedName(), foInfo.getLocalizedName())) {
                    // Same name, not renamed.
                    continue;
                }
                if (isFine()) {
                    logFine("localFolder: " + localFolder);
                    logFine("remoteFolder: " + foInfo);
                }
                Path currentDirectory = folder.getLocalBase();
                String currentDirectoryName = currentDirectory.getFileName().toString();
                if (!PathUtils.isSameName(currentDirectoryName, localFolder.getLocalizedName())) {
                    logWarning("Not renaming Folder " + localFolder.getName()
                            + " to " + foInfo.getName()
                            + ". Current local directory name (" + currentDirectoryName
                            + ") does not match folder name ("
                            + localFolder.getLocalizedName() + ")");
                    continue;
                }

                logInfo("Renaming Folder " + localFolder.getName() + " to "
                        + foInfo.getName());
                foInfo = foInfo.intern(true);

                Path newDirectory = folder.getLocalBase().getParent()
                        .resolve(PathUtils
                                .removeInvalidFilenameChars(foInfo.getLocalizedName()));
                folder = moveLocalFolder(folder, newDirectory);
                if (folder == null) {
                    logWarning("Failed to move folder " + foInfo.getName() + "/" + foInfo.getId()
                            + " to new directory " + newDirectory);
                }
            }

            logInfo("Syncing folder setup with account permissions("
                + a.getFolders().size() + "): " + a.getUsername());
            Collection<FolderInfo> created = createLocalFolders(ad);
            if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                .getValueBoolean(getController()))
            {
                if (!created.isEmpty()) {
                    // Let things (Account update) settle down / See above.
                    // Usecase: Client Backup / Personal folders.
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (ProUtil.isServerConfig(getController())) {
                    logSevere("Found server config running with client installation. "
                        + "Won't delete local folders.");
                    return;
                }
                removeLocalFolders(a, created);
            } else {
                // PFC-2486
                for (Folder folder : getFolders()) {
                    if (!a.hasReadPermissions(folder.getInfo())) {
                        AccessDeniedProblem problem = new AccessDeniedProblem(
                            folder.getInfo());
                        folder.removeProblem(problem);
                        folder.addProblem(problem);
                    } else if (folder.countProblems() > 0) {
                        AccessDeniedProblem problem = new AccessDeniedProblem(
                            folder.getInfo());
                        folder.removeProblem(problem);
                    }
                }
            }
        } finally {
            accountSyncLock.unlock();
        }
    }

    public Folder moveLocalFolder(Folder folder, Path targetPath) {
        Reject.ifTrue(EncryptedFileSystemUtils.isCryptoInstance(targetPath), "Only physical paths supported aas new directory");

        boolean sourceEncrypted = EncryptedFileSystemUtils.isCryptoInstance(folder.getLocalBase());
        boolean targetEncrypted = targetPath.getFileName().toString().endsWith(Constants.FOLDER_ENCRYPTION_SUFFIX);
        if (sourceEncrypted && !targetEncrypted) {
            logWarning("Trying to move encrypted folder to unencrypted target directory. From: "
                    + folder.getLocalBase() + " to " + targetPath);
        }

        targetPath = PathUtils.removeInvalidFilenameChars(targetPath);

        if (Files.exists(targetPath) && !PathUtils.isEmptyDir(targetPath)) {
            Path localBase = folder.getLocalBase();
            if (EncryptedFileSystemUtils.isCryptoInstance(folder.getLocalBase())) {
                localBase = EncryptedFileSystemUtils.getPhysicalStorageLocation(localBase);
            }
            logSevere("Not moving folder " + folder + " to new directory "
                + targetPath.toString()
                    + ". The new directory already exists! "
                    + "Keeping the old directory " + localBase);
            return null;
        }

        try {
            scanBasedirLock.lock();

            Path sourceDirectory = folder.getLocalBase().toRealPath();
            if (EncryptedFileSystemUtils.isCryptoInstance(sourceDirectory)) {
                sourceDirectory = EncryptedFileSystemUtils.getPhysicalStorageLocation(sourceDirectory);
            }

            if (sourceDirectory.equals(targetPath)) {
                logFine("Not required to move folder from/to " + targetPath);
                return folder;
            }
            FolderSettings fs = FolderSettings.load(getController(),
                    folder.getConfigEntryId());

            boolean deleteOriginalDirectory = false;
            boolean moved = false;

            // Remember patterns if content not moving.
            List<String> patterns = folder.getDiskItemFilter().getPatterns();

            // Remove the old folder from the repository.
            removeFolder(folder, false);

            // Move it.
            try {
                if (Files.exists(targetPath) && PathUtils.isEmptyDir(targetPath)) {
                    // Delete empty target target path. Might have been created through resolveTargetDirectory
                    PathUtils.recursiveDelete(targetPath);
                }
                PathUtils.recursiveMoveVisitor(sourceDirectory, targetPath);
                fs = fs.changeBaseDir(targetPath);
                moved = true;
            } catch (IOException e) {
                try {
                    PathUtils.recursiveCopyVisitor(sourceDirectory, targetPath);
                    fs = fs.changeBaseDir(targetPath);
                    moved = true;
                    deleteOriginalDirectory = true;
                } catch (IOException ex) {
                    logWarning("Unable to move/copy folder " + folder.getName() + " to " + targetPath + ". @" + ex);
                    deleteOriginalDirectory = false;
                }
            }

            // Create the new Folder in the repository.
            folder = createFolder0(folder.getInfo().intern(), fs, true);

            // Restore patterns
            for (String pattern : patterns) {
                folder.addPattern(pattern);
            }

            if (moved) {
                logInfo("Successfully moved folder from " + sourceDirectory + " to " + targetPath + ".");
            } else {
                logInfo("Not moved folder from " + sourceDirectory + " to " + targetPath + ". Using old directory");
            }

            // If the folder just has been copied, delete the old directory
            if (deleteOriginalDirectory) {
                try {
                    if (Files.exists(sourceDirectory)) {
                        PathUtils.recursiveDeleteVisitor(sourceDirectory);
                    }
                } catch (IOException e) {
                    logWarning("Failed to delete source directory " + sourceDirectory
                            + " after moving folder " + folder.getName() + " to " + targetPath, e);
                }
            }

        } catch (IOException e) {
            logSevere("Unable to move folder " + folder.getName() + " to " + targetPath + ". " + e);
            logFine(e);
            return null;
        } finally {
            scanBasedirLock.unlock();
        }

        return folder;
    }

    private void removeLocalFolders(Account a, Collection<FolderInfo> skip) {
        if (!a.isValid()) {
            return;
        }
        for (Folder folder : getFolders()) {
            if (skip.contains(folder.getInfo())) {
                continue;
            }
            if (!a.hasReadPermissions(folder.getInfo())) {
                logWarning("Removing local " + folder + ' ' + a
                    + " does not have read permission. Wiping out data.");
                removeFolder(folder, true);
                final Path localBase = folder.getLocalBase();
                getController().getIOProvider().startIO(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(2000L);
                            PathUtils.recursiveDelete(localBase);
                        } catch (Exception e) {
                            logWarning("Unable to delete directory: "
                                + localBase);
                        }
                    }
                });
            }
        }
    }

    private synchronized Collection<FolderInfo> createLocalFolders(AccountDetails ad) {
        Account a = ad.getAccount();
        if (!a.isValid()) {
            return Collections.emptyList();
        }
        Map<FolderInfo, FolderSettings> folderInfos = new HashMap<>();
        for (Iterator<String> it = onLoginFolderEntryIds.iterator(); it
            .hasNext();)
        {
            String folderEntryId = it.next();
            FolderSettings settings = FolderSettings.load(getController(),
                folderEntryId);
            String folderName = FolderSettings.loadFolderName(getController()
                .getConfig(), folderEntryId);
            if (settings == null) {
                String folderDirStr = getController().getConfig().getProperty(
                    PREFIX_V4 + folderEntryId
                        + FolderSettings.DIR);
                logWarning("Not setting up folder " + folderName + " / "
                    + folderEntryId + " local base dir not found: "
                    + folderDirStr);
                continue;
            }
            FolderInfo foInfo = null;
            for (FolderInfo candidate : a.getFolders()) {
                if (candidate.getName().equals(folderName)) {
                    logInfo("Folder found on account " + a.getUsername()
                        + ". Loading it: " + candidate);
                    foInfo = candidate;
                    break;
                }
            }
            // Actually create the directory
            try {
                scanBasedirLock.lock();
                try {
                    Files.createDirectories(settings.getLocalBaseDir());
                } catch (IOException ioe) {
                    if (isFine()) {
                        logFine(ioe.getMessage());
                    }
                }

                if (foInfo == null) {
                    // Spawn/Create a new one.
                    foInfo = new FolderInfo(folderName, a.createInfo());
                    logInfo("Folder not found on account " + a.getUsername()
                        + ". Created new: " + foInfo);
                }

                // Make sure it is backed up by the server.
                boolean scheduleCreateOnServer = false;
                try {
                    // Do it synchronous. Otherwise we might get race conditions.
                    getController().getOSClient().getFolderService()
                        .createFolder(foInfo, null);
                    if (settings != null) {
                        getController().getOSClient().getFolderService()
                            .setArchiveMode(foInfo, settings.getVersions());
                    }
                } catch (Exception e) {
                    scheduleCreateOnServer = true;
                }

                Folder folder = createFolder0(foInfo, settings, true);
                folder.addDefaultExcludes();

                if (scheduleCreateOnServer) {
                    logFine("Scheduling setup of folder: " + foInfo.getName());
                    CreateFolderOnServerTask task = new CreateFolderOnServerTask(foInfo,
                        null);
                    task.setArchiveVersions(settings.getVersions());
                    getController().getTaskManager().scheduleTask(task);
                }

                // Remove from pending entries.
                it.remove();
                folderInfos.put(foInfo, settings);
            } catch (Exception e) {
                logWarning("Unable to create folder " + folderName + " at "
                    + settings.getLocalBaseDir() + ". " + e);
            } finally {
                scanBasedirLock.unlock();
            }
        }
        // If a UI client is running and AUTO_SETUP_ACCOUNT_FOLDERS is enabled, check if there is enough disk space for all folders.
        // If there is not enough disk space, disable AUTO_SETUP_ACCOUNT_FOLDERS and send a notification
        if (getController().isUIEnabled() && ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS.getValueBoolean(getController()) && !checkDiskSpace(ad)) {
            ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS.setValue(getController(), false);
            getController().saveConfig();
            Notice notice = new WarningNotice(Translation.get("disc_space_warning.title"), Translation.get("disc_space_warning.summary"), null);
            getController().getUIController().getApplicationModel().getNoticesModel().handleNotice(notice);
        }
        if (ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS
            .getValueBoolean(getController()))
        {
            for (FolderInfo folderInfo : a.getFolders()) {
                if (hasJoinedFolder(folderInfo)) {
                    continue;
                }

                String folderName = PathUtils.removeInvalidFilenameChars(folderInfo.getLocalizedName());

                SyncProfile profile = SyncProfile.getDefault(getController());
                Path suggestedLocalBase = getController().getFolderRepository()
                    .getFoldersBasedir().resolve(folderName);
                if (isIgnoredFolderDirectory(suggestedLocalBase)) {
                    continue;
                }

                UserDirectory userDir = UserDirectories
                    .getUserDirectories(getController()).get(folderName);

                if (userDir != null) {
                    if (isIgnoredFolderDirectory(userDir
                        .getDirectory()))
                    {
                        continue;
                    }
                    suggestedLocalBase = userDir.getDirectory();
                } else if (ConfigurationEntry.FOLDER_CREATE_USE_EXISTING
                    .getValueBoolean(getController()))
                {
                    // Moderate strategy. Use existing folders.
                    suggestedLocalBase = getController().getFolderRepository()
                        .getFoldersBasedir().resolve(folderName);
                    if (Files.exists(suggestedLocalBase)) {
                        logInfo("Using existing directory "
                            + suggestedLocalBase + " for " + folderInfo);
                    }
                } else {
                    // Take folder name as subdir name
                    suggestedLocalBase = getController().getFolderRepository()
                        .getFoldersBasedir().resolve(folderName);
                }

                logInfo("Auto setting up folder " + folderInfo.getName() + "/"
                    + folderInfo.getId() + " @ " + suggestedLocalBase);

                // Correct local path if in UserDirectories.
                FolderSettings settings = new FolderSettings(
                    suggestedLocalBase, profile,
                    ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                        .getValueInt(getController()));

                try {
                    // Actually create the directory
                    scanBasedirLock.lock();
                    Files.createDirectories(settings.getLocalBaseDir());

                    Folder folder = createFolder0(folderInfo, settings, true);
                    folder.addDefaultExcludes();
                    folderInfos.put(folderInfo, settings);
                } catch (IOException ioe) {
                    if (isFine()) {
                        logFine(ioe.getMessage());
                    }
                } catch (Exception e) {
                    logWarning("Unable to create folder "
                        + folderInfo.getName() + " at "
                        + settings.getLocalBaseDir() + ". " + e);
                } finally {
                    scanBasedirLock.unlock();
                }
            }
        }

        return folderInfos.keySet();
    }

    // Event support **********************************************************

    private void fireFolderCreated(Folder folder) {
        folderRepositoryListenerSupport
            .folderCreated(new FolderRepositoryEvent(this, folder));
    }

    private void fireFolderRemoved(Folder folder) {
        folderRepositoryListenerSupport
            .folderRemoved(new FolderRepositoryEvent(this, folder));
    }

    private void fireMaintanceStarted(Folder folder) {
        folderRepositoryListenerSupport
            .maintenanceStarted(new FolderRepositoryEvent(this, folder));
    }

    private void fireMaintenanceFinished(Folder folder) {
        folderRepositoryListenerSupport
            .maintenanceFinished(new FolderRepositoryEvent(this, folder));
    }

    private void fireCleanupStarted() {
        folderRepositoryListenerSupport
            .cleanupStarted(new FolderRepositoryEvent(this));
    }

    private void fireCleanupFinished() {
        folderRepositoryListenerSupport
            .cleanupFinished(new FolderRepositoryEvent(this));
    }

    public void addFolderRepositoryListener(FolderRepositoryListener listener) {
        ListenerSupportFactory.addListener(folderRepositoryListenerSupport,
            listener);
    }

    public void removeFolderRepositoryListener(FolderRepositoryListener listener)
    {
        ListenerSupportFactory.removeListener(folderRepositoryListenerSupport,
            listener);
    }

    private void removeConfigEntries(String folderEntryId) {
        Properties config = getController().getConfig();
        FolderSettings.removeEntries(config, folderEntryId);
    }

    /**
     * Delete any file archives over a specified age, if history is not set to
     * "forever".
     */
    public void cleanupOldArchiveFiles() {
        cleanupOldArchiveFiles(false);
    }

    /**
     * @see #cleanupOldArchiveFiles()
     * @param force
     *            If {@code true} ignore the
     *            {@link ConfigurationEntry#DEFAULT_ARCHIVE_CLEANUP_DAYS} else
     *            take that setting into account.
     */
    public void cleanupOldArchiveFiles(boolean force) {
        int period = ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS
            .getValueInt(getController());
        if (!force && (period == Integer.MAX_VALUE || period <= 0)) { // cleanup := never
            return;
        }
        try {
            fireCleanupStarted();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -period);
            Date cleanupDate = cal.getTime();
            for (Folder folder : getFolders()) {
                folder.cleanupOldArchiveFiles(cleanupDate);
            }
        } finally {
            fireCleanupFinished();
        }
    }

    /**
     * Do we already have a folder that has this file as its base?
     *
     * @param
     */
    public boolean doesFolderAlreadyExist(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        for (Folder folder : folders.values()) {
            if (path.equals(folder.getBaseDirectoryInfo().getDiskFile(this))) {
                return true;
            }
        }
        return false;
    }

    public boolean areAllFoldersInSync() {
        for (Folder folder : folders.values()) {
            if (Double.compare(folder.getStatistic()
                .getHarmonizedSyncPercentage(), 100.0d) < 0)
            {
                return false;
            }
        }
        return true;
    }

    private class CheckSyncTask implements Runnable {
        public void run() {
            boolean syncMemberShips = false;
            for (Folder folder : getController().getFolderRepository()
                .getFolders())
            {
                // PFS-1800: Start
                if (folder.getStatistic().getTotalFilesCount() == 0) {
                    folder.getStatistic().scheduleCalculate();
                }
                // PFS-1800: End
                if (folder.isPreviewOnly()) {
                    continue;
                }
                folder.checkSync();

                if (folder.getStatistic().getHarmonizedSyncPercentage() == 100.0d)
                {
                    continue;
                }
                if (getController().getOSClient().isConnected()
                    && !getController().getOSClient().joinedByCloud(folder))
                {
                    if (isFine()) {
                        logFine("Re-sync memberships with server: "
                            + getController().getOSClient().getServerString()
                            + " / " + folder);
                    }
                    syncMemberShips = true;
                }
                if (folder.getConnectedMembersCount() == 0) {
                    continue;
                }
                if (folder.getKnownItemCount() == 0) {
                    continue;
                }
                if (!folder.hasReadPermission(getController().getMySelf())) {
                    continue;
                }

                // PFS-1144: Fallback: Re-schedule calculations
                folder.getStatistic().scheduleCalculate();

                // Rationale: We might have not received file list from a server
                // PFC-2368
                for (Member member : folder.getConnectedMembers()) {
                    if (!member.isCompletelyConnected()) {
                        // Skin if not fully connected
                        continue;
                    }
                    Date lastConnectTime = member.getLastConnectTime();
                    if (lastConnectTime == null) {
                        continue;
                    }
                    long connectAgo = System.currentTimeMillis()
                        - lastConnectTime.getTime();
                    if (!member.hasCompleteFileListFor(folder.getInfo())
                        && connectAgo < 1000L * 60)
                    {
                        // Might still be transferring those filelists within
                        // the first minute.
                        continue;
                    }
                    int nMemberItems = folder.getDAO().count(member.getId(),
                        true, false);
                    if (nMemberItems > 0) {
                        continue;
                    }
                    // OK: Handle it. There is a connected member on an unsyced
                    // folder, which has send ZERO files.
                    if (isFine()) {
                        logFine("Re-requesting file list for "
                            + folder.getName() + " from " + member.getNick());
                    }
                    member.sendMessageAsynchron(new FileListRequest(folder
                        .getInfo()));
                }
            }
            if (syncMemberShips) {
                for (Member node : getController().getNodeManager()
                    .getNodesAsCollection())
                {
                    if (node.isServer() && node.isCompletelyConnected()) {
                        node.synchronizeFolderMemberships();
                    }
                }
            }
        }
    }

    private class AllFolderMembershipSynchronizer implements Runnable {
        private AtomicBoolean canceled = new AtomicBoolean(false);

        public void run() {
            ProfilingEntry pe = Profiling
                .start("synchronizeAllFolderMemberships");
            try {
                if (canceled.get()) {
                    logFine("Not synchronizing Foldermemberships, "
                        + "operation already canceled yet");
                    return;
                }

                if (isFiner()) {
                    logFiner("All Nodes: Synchronize Foldermemberships");
                }
                Collection<Member> connectedNodes = getController()
                    .getNodeManager().getConnectedNodes();
                for (Member node : connectedNodes) {
                    node.synchronizeFolderMemberships(canceled);
                    if (canceled.get()) {
                        logFiner("Foldermemberships synchroniziation cancelled");
                        return;
                    }
                }

            } finally {
                Profiling.end(pe);
                // Termination, remove synchronizer
                synchronized (folderMembershipSynchronizerLock) {
                    // Got already new syner started in the meanwhile? if yes,
                    // don't set to null
                    if (folderMembershipSynchronizer == this) {
                        folderMembershipSynchronizer = null;
                    }
                }
            }
        }
    }

    /**
     * Set the {@link Folder} {@code folder} to be disconnected from storage.
     * That is remove it locally from the {@link FolderRepository}.<br />
     * <br />
     * This method is synchronized with {@link FolderRepository#scanBasedir()}.
     *
     * @param folder
     *            The {@link Folder} to be reomved from the
     *            {@link FolderRepository}.
     * @return {@code True} if the folder was removed locally, {@code false}
     *         otherwise.
     */
    boolean handleDeviceDisconnected(final Folder folder) {
        Reject.ifNull(folder, "Folder");

        if (folder.getInfo().isMetaFolder()) {
            return false;
        }

        if (!ConfigurationEntry.FOLDER_REMOVE_IN_BASEDIR_WHEN_DISAPPEARED
            .getValueBoolean(getController()))
        {
            return false;
        }

        Path bd = getFoldersBasedir().toAbsolutePath();
        boolean inBaseDir = false;
        if (bd != null) {
            if (EncryptedFileSystemUtils.isCryptoInstance(folder.getLocalBase())) {
                inBaseDir = EncryptedFileSystemUtils.getPhysicalStorageLocation(folder.getLocalBase()).startsWith(bd);
            } else {
                inBaseDir = folder.getLocalBase().toAbsolutePath().startsWith(bd);
            }
        }

        if (!inBaseDir) {
            return false;
        }

        // Schedule for removal
        getController().schedule(() -> {

            if (hasJoinedFolder(folder.getInfo())) {

                // Handle possible renames
                scanBasedir();
                Folder currentFolder = getFolder(folder.getInfo());
                if (currentFolder != null
                    && !currentFolder.checkIfDeviceDisconnected())
                {
                    return;
                }

                logFine("Removing " + folder.toString());
                removeFolder(folder, false);
                getController().getFolderRepository().scanBasedir();
            }
        }, 5000L);

        return true;
    }


    /**
     * Checks if there is enough space on the disk to store all folders of an account
     *
     * @param accountDetails The account details to check vs.
     * @return True if there is enough space on the disk to store all folders
     */
    private boolean checkDiskSpace(AccountDetails accountDetails) {
        long dataSize = accountDetails.getSpaceUsed();
        long freeDiskSpace = 0;
        try {
            freeDiskSpace = Files.getFileStore(getFoldersBasedir()).getUsableSpace();
        } catch (IOException e) {
            logSevere("Cannot get file store: " + e);
            return true;
        }
        return dataSize < freeDiskSpace;
    }

}
