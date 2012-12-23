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

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_ID;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX_V4;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.event.FolderAutoCreateEvent;
import de.dal33t.powerfolder.event.FolderAutoCreateListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.task.CreateFolderOnServerTask;
import de.dal33t.powerfolder.task.FolderObtainPermissionTask;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.collection.CompositeCollection;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.os.mac.MacUtils;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * Repository of all known power folders. Local and unjoined.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class FolderRepository extends PFComponent implements Runnable {

    private static final Logger log = Logger.getLogger(FolderRepository.class
        .getName());
    private final Map<FolderInfo, Folder> folders;
    private final Map<FolderInfo, Folder> metaFolders;
    private Thread myThread;
    private final FileRequestor fileRequestor;
    private Folder currentlyMaintaitingFolder;
    private final Set<String> onLoginFolderEntryIds;
    // Flag if the repo is started
    private boolean started;
    // The trigger to start scanning
    private final Object scanTrigger = new Object();
    private boolean triggered;
    private final AtomicInteger suspendNewFolderSearch = new AtomicInteger(0);
    private File foldersBasedir;

    /** folder repository listeners */
    private final FolderRepositoryListener folderRepositoryListenerSupport;

    /** The disk scanner */
    private final FolderScanner folderScanner;

    /**
     * The current synchronizater of all folder memberships
     */
    private AllFolderMembershipSynchronizer folderMembershipSynchronizer;
    private final Object folderMembershipSynchronizerLock = new Object();

    /**
     * Registered to ALL folders to deligate problem event of any folder to
     * registered listeners.
     * <p>
     * TODO: Value listeners deteriorate the UI refresh speed.
     */
    private final ProblemListener valveProblemListenerSupport;

    private FolderAutoCreateListener folderAutoCreateListener;

    /**
     * A list of folder base directories that have been removed in the past.
     */
    private final Set<File> removedFolderDirectories = new CopyOnWriteArraySet<File>();

    /**
     * Constructor
     * 
     * @param controller
     */
    public FolderRepository(Controller controller) {
        super(controller);

        triggered = false;
        // Rest
        folders = new ConcurrentHashMap<FolderInfo, Folder>();
        metaFolders = new ConcurrentHashMap<FolderInfo, Folder>();
        onLoginFolderEntryIds = new HashSet<String>();
        fileRequestor = new FileRequestor(controller);
        started = false;
        loadRemovedFolderDirectories();

        folderScanner = new FolderScanner(getController());

        // Create listener support
        folderRepositoryListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderRepositoryListener.class);
        valveProblemListenerSupport = ListenerSupportFactory
            .createListenerSupport(ProblemListener.class);
        folderAutoCreateListener = ListenerSupportFactory
            .createListenerSupport(FolderAutoCreateListener.class);
    }

    private void loadRemovedFolderDirectories() {
        String list = ConfigurationEntry.REMOVED_FOLDER_FILES
            .getValue(getController());
        String[] parts = list.split("\\$");
        for (String s : parts) {
            File f = new TFile(s);
            if (f.exists() && f.isDirectory()) {
                removedFolderDirectories.add(f);
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

        // #1697
        // Init required. To avoid extracting ZIP/JAR files
        TFile.setDefaultArchiveDetector(new TArchiveDetector(
            TArchiveDetector.NULL, "pfzip", new JarDriver(
                IOPoolLocator.SINGLETON)));
        TFile.setLenient(false);

        initFoldersBasedir();

        processV4Format();

        // Maintain link
        boolean useFavLink = ConfigurationEntry.USE_PF_LINK
            .getValueBoolean(getController());
        if (useFavLink && WinUtils.isSupported()) {
            try {
                WinUtils.getInstance().setPFLinks(true, getController());
                ConfigurationEntry.USE_PF_LINK.setValue(getController(), false);
            } catch (IOException e) {
                logSevere(e);
            }
        }
        if (useFavLink && MacUtils.isSupported()) {
            try {
                MacUtils.getInstance().setPFPlaces(true, getController());
                ConfigurationEntry.USE_PF_LINK.setValue(getController(), false);
            } catch (IOException e) {
                logSevere(e);
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
        }

        // Check if this a windows network drive.
        boolean winNetworkDrive = baseDir != null && baseDir.contains(":\\")
            && baseDir.charAt(1) == ':';
        boolean ok = false;

        if ((OSUtil.isWindowsSystem() && winNetworkDrive) || (!winNetworkDrive))
        {
            foldersBasedir = new TFile(baseDir).getAbsoluteFile();
            if (!foldersBasedir.exists()) {
                if (foldersBasedir.mkdirs()) {
                    logInfo("Created base path for folders: " + foldersBasedir);
                } else {
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir);
                }
            }
            ok = foldersBasedir.exists() && foldersBasedir.canRead()
                && foldersBasedir.isDirectory();
        }

        if (!OSUtil.isWindowsSystem() && winNetworkDrive) {
            foldersBasedir = new TFile(
                ConfigurationEntry.FOLDER_BASEDIR.getDefaultValue());
            if (!foldersBasedir.exists()) {
                if (foldersBasedir.mkdirs()) {
                    logInfo("Created base path for folders: " + foldersBasedir);
                } else {
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir);
                }
            }
            ok = foldersBasedir.exists() && foldersBasedir.canRead()
                && foldersBasedir.isDirectory();
        }

        // Use default as fallback
        if (!ok
            && ConfigurationEntry.FOLDER_BASEDIR_FALLBACK_TO_DEFAULT
                .getValueBoolean(getController()))
        {
            foldersBasedir = new TFile(
                ConfigurationEntry.FOLDER_BASEDIR.getDefaultValue());
            if (!foldersBasedir.exists()) {
                if (foldersBasedir.mkdirs()) {
                    logInfo("Created base path for folders: " + foldersBasedir);
                } else {
                    logWarning("Unable to create base path for folders: "
                        + foldersBasedir);
                }
            }
        }

        ok = foldersBasedir.exists() && foldersBasedir.canRead()
            && foldersBasedir.isDirectory();

        if (ok) {
            logInfo("Using base path for folders: " + foldersBasedir);
            FileUtils.maintainDesktopIni(getController(), foldersBasedir);
        } else {
            logWarning("Unable to access base path for folders: "
                + foldersBasedir);
        }
    }

    /**
     * Version 4 format is like f.<md5>.XXXX, where md5 is the MD5 of the folder
     * id. This format allows folders with the same name to be stored.
     */
    private void processV4Format() {
        final Properties config = getController().getConfig();

        // Find all folder entries.
        Set<String> entryIds = FolderSettings.loadEntryIds(config);

        // Load on all processor
        int loaders = Math.min(Runtime.getRuntime().availableProcessors(), 8);
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
                            .getProperty(FOLDER_SETTINGS_PREFIX_V4
                                + folderEntryId + FOLDER_SETTINGS_ID);
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

                        boolean spawned = false;
                        if (folderId
                            .contains(FolderSettings.FOLDER_ID_GENERATE))
                        {
                            String generatedId = '[' + IdGenerator.makeId() + ']';
                            folderId = folderId.replace(
                                FolderSettings.FOLDER_ID_GENERATE, generatedId);
                            logInfo("Spawned new folder id for config entry "
                                + folderEntryId + ": " + folderId);
                            spawned = true;
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

                        // Do not add0 if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderSettings != null)
                        {
                            createFolder0(foInfo, folderSettings, false);
                        }

                        if (spawned) {
                            removeConfigEntries(folderEntryId);
                            folderSettings.set(foInfo, config);
                            getController().saveConfig();
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

        // Start filerequestor
        fileRequestor.start();

        // Defer 2 minutes, so it is not 'in-your-face' at start up.
        // Also run this every minute.
        getController().scheduleAndRepeat(new CheckSyncTask(),
            1000L * Constants.FOLDER_UNSYNCED_CHECK_DELAY,
            Constants.MILLIS_PER_MINUTE);

        started = true;
    }

    /**
     * Shuts down folder repo
     */
    public void shutdown() {
        synchronized (folderMembershipSynchronizerLock) {
            if (folderMembershipSynchronizer != null) {
                folderMembershipSynchronizer.canceled = true;
            }
        }
        folderScanner.shutdown();

        if (myThread != null) {
            myThread.interrupt();
        }
        synchronized (scanTrigger) {
            scanTrigger.notifyAll();
        }
        // Stop processor
        // netListProcessor.shutdown();

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
        return getFoldersBasedir() != null ? getFoldersBasedir().getAbsolutePath() : null;
    }

    /**
     * @return the default basedir for all folders. basedir is just suggested
     */
    public File getFoldersBasedir() {
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
     * All real-folders WITHOUT Meta-folders (#1548). Returns the indirect
     * reference to the internal {@link ConcurrentMap}. Contents may changed
     * after get.
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
    public Folder findExistingFolder(File targetDir) {
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            try {
                if (folder.getLocalBase().equals(targetDir)
                    || folder.getCommitOrLocalDir().getCanonicalPath()
                        .equals(targetDir.getCanonicalPath()))
                {
                    return folder;
                }
            } catch (IOException e) {
                logWarning(e);
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
        if (!folderSettings.getLocalBaseDir().getName().endsWith(".pfzip")) {
            folderSettings.getLocalBaseDir().mkdirs();
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
            return folders.get(folderInfo);
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
                    logSevere("Tried to create duplicate folder "
                        + folder.getName() + " at "
                        + folder.getCommitOrLocalDir());
                    throw new IllegalStateException(
                        "Tried to create duplicate folder " + folder.getName()
                            + " at " + folder.getCommitOrLocalDir());
                }
            }
        }

        // PFC-2226: Option to restrict new folder creation to the default storage path.
        // Note, this is a last check. User should never get here because of other checks.
        if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
            .getValueBoolean(getController()))
        {
            boolean inBaseDir = folderSettings.getLocalBaseDir().getParentFile().equals(getFoldersBasedir());
            if (!inBaseDir) {
                logSevere("Not allowed to create " + folderInfo.getName()
                    + " at " + folderSettings.getLocalBaseDirString()
                    + ". Must be in base directory: " + getFoldersBasedir());
                throw new IllegalStateException("Not allowed to create "
                    + folderInfo.getName() + " at "
                    + folderSettings.getLocalBaseDirString()
                    + ". Must be in base directory: " + getFoldersBasedir());
            }
        }

        if (Feature.FOLDER_ATOMIC_COMMIT.isEnabled()
            && folderSettings.getCommitDir() == null)
        {
            File newBaseDir = new TFile(folderSettings.getLocalBaseDir(),
       Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR);
            newBaseDir.mkdirs();
            FileUtils.setAttributesOnWindows(newBaseDir, true, true);
            File commitDir = folderSettings.getLocalBaseDir();
            SyncProfile syncProfile = SyncProfile.NO_SYNC;

            folderSettings = new FolderSettings(newBaseDir, syncProfile,
                folderSettings.isCreateInvitationFile(),
                folderSettings.getArchiveMode(),
                folderSettings.isPreviewOnly(),
                folderSettings.getDownloadScript(),
                folderSettings.getVersions(), folderSettings.isSyncPatterns(),
                commitDir, folderSettings.getSyncWarnSeconds());
            logWarning("Auto-commit setup. temp dir: " + newBaseDir
                + ". commit dir:" + commitDir);
        }

        Folder folder;
        if (folderSettings.isPreviewOnly()) {
            // Need to use preview folder settings.
            FolderSettings previewFolderSettings = FolderPreviewHelper
                .createPreviewFolderSettings(folderInfo.name);
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
        File systemSubdir = new TFile(folder.getLocalBase(),
            Constants.POWERFOLDER_SYSTEM_SUBDIR);
        FolderSettings metaFolderSettings = new FolderSettings(new TFile(
            systemSubdir, Constants.METAFOLDER_SUBDIR),
            SyncProfile.META_FOLDER_SYNC, false, ArchiveMode.NO_BACKUP, 0);
        boolean deviceDisconnected = folder.checkIfDeviceDisconnected();
        if (!deviceDisconnected) {
            metaFolderSettings.getLocalBaseDir().mkdirs();
        }
        Folder metaFolder = new Folder(getController(), metaFolderInfo,
            metaFolderSettings);
        if (!deviceDisconnected) {
            metaFolder.getSystemSubDir().mkdirs();
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
        logFine("Created metaFolder " + metaFolderInfo.name
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
            String log = "Setup " + (folder.isEncrypted() ? "encrypted " : "")
                + "folder " + folderInfo.name + " at " + folder.getLocalBase();
            logFine(log);
        }

        removeFromRemovedFolderDirectories(folder);

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
        Reject.ifNull(folder, "Folder is null");
        try {
            suspendNewFolderSearch.incrementAndGet();

            // Remember that we have removed this folder.
            addToRemovedFolderDirectories(folder);

            // Remove the desktop shortcut
            folder.removeDesktopShortcut();

            // Detach any problem listeners.
            folder.clearAllProblemListeners();

            // Remove desktop ini if it exists
            FileUtils.deleteDesktopIni(folder.getLocalBase());

            // remove folder from config
            removeConfigEntries(folder.getConfigEntryId());

            // Save config
            getController().saveConfig();

            // Remove internal
            folders.remove(folder.getInfo());
            folder.removeProblemListener(valveProblemListenerSupport);

            // Break transfers
            getController().getTransferManager().breakTransfers(
                folder.getInfo());

            // Shutdown folder
            folder.shutdown();

            // Shutdown meta folder aswell
            Folder metaFolder = getMetaFolderForParent(folder.getInfo());
            if (metaFolder != null) {
                metaFolder.shutdown();
                metaFolders.remove(metaFolder.getInfo());
                metaFolders.remove(folder.getInfo());
            }

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
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                try {
                    FileUtils.recursiveDelete(folder.getSystemSubDir());
                } catch (IOException e) {
                    logSevere("Failed to delete: " + folder.getSystemSubDir());
                }

                // Try to delete the invitation.
                File invite = new TFile(folder.getLocalBase(), folder.getName()
                    + ".invitation");
                if (invite.exists()) {
                    try {
                        invite.delete();
                    } catch (Exception e) {
                        logSevere(
                            "Failed to delete invitation: "
                                + invite.getAbsolutePath(), e);
                    }
                }

                // Remove the folder if totally empty.
                File[] files = folder.getLocalBase().listFiles();
                if (files != null && files.length == 0) {
                    try {
                        FileUtils.recursiveDelete(folder.getLocalBase());
                    } catch (Exception e) {
                        logSevere("Failed to delete local base: "
                            + folder.getLocalBase().getAbsolutePath(), e);
                    }
                }
            }
        } finally {
            suspendNewFolderSearch.decrementAndGet();
        }

        // Fire event
        fireFolderRemoved(folder);

        // Trigger sync on other folders.
        fileRequestor.triggerFileRequesting();

        if (isFine()) {
            logFine(folder + " removed");
        }
    }

    private void addToRemovedFolderDirectories(Folder folder) {
        if (removedFolderDirectories.add(folder.getLocalBase())) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<File> iterator = removedFolderDirectories.iterator(); iterator
                .hasNext();)
            {
                String s = iterator.next().getAbsolutePath();
                sb.append(s);
                if (iterator.hasNext()) {
                    sb.append('$');
                }
            }
            ConfigurationEntry.REMOVED_FOLDER_FILES.setValue(getController(),
                sb.toString());
        }
    }

    private void removeFromRemovedFolderDirectories(Folder folder) {
        if (removedFolderDirectories.remove(folder.getLocalBase())) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<File> iterator = removedFolderDirectories.iterator(); iterator
                .hasNext();)
            {
                String s = iterator.next().getAbsolutePath();
                sb.append(s);
                if (iterator.hasNext()) {
                    sb.append('$');
                }
            }
            ConfigurationEntry.REMOVED_FOLDER_FILES.setValue(getController(),
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
                folderMembershipSynchronizer.canceled = true;
            }
            folderMembershipSynchronizer = new AllFolderMembershipSynchronizer();
            getController().schedule(folderMembershipSynchronizer, 0);
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
        return currentlyMaintaitingFolder;
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
        // try {
        // // initial wait before first scan
        // synchronized (scanTrigger) {
        // scanTrigger.wait(Controller.getWaitTime() * 4);
        // }
        // } catch (InterruptedException e) {
        // logFiner(e);
        // return;
        // }

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
                    currentlyMaintaitingFolder = folder;
                    // Fire event
                    fireMaintanceStarted(currentlyMaintaitingFolder);
                    currentlyMaintaitingFolder.maintain();
                    Folder maintainedFolder = currentlyMaintaitingFolder;
                    currentlyMaintaitingFolder = null;
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
     * Scan the PowerFolder base directory for new directories that might be new
     * folders.
     */
    public void lookForNewFolders() {
        if (suspendNewFolderSearch.get() > 0) {
            return;
        }
        if (!getController().getMySelf().isServer()) {
            if (!getController().getOSClient().isLoggedIn()) {
                if (isFine()) {
                    logFine("Skipping searching for new folders...");
                }
                return;
            }
            if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                .getValueBoolean(getController())
                && !getController().getOSClient().getAccount()
                    .hasPermission(FolderCreatePermission.INSTANCE))
            {
                if (isFine()) {
                    logFine("Skipping searching for new folders (no permission)...");
                }
                return;
            }
        }
        if (getController().isPaused()) {
            logFine("Skipping searching for new folders (paused)...");
            return;
        }

        if (isFine()) {
            logFine("Searching for new folders...");
        }
        // TODO BOTTLENECK: Takes much CPU -> Implement via jnotify
        String baseDirName = getFoldersBasedirString();
        File baseDir = new TFile(baseDirName);
        if (baseDir.exists() && baseDir.canRead()) {
            // Get all directories
            File[] directories = baseDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String filename = file.getName();
                    if (filename.equals(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                        return false;
                    }
                    if (filename.equals("BACKUP_REMOVE")) {
                        return false;
                    }
                    if (!file.isDirectory()) {
                        return false;
                    }
                    // Don't autocreate if it has been removed previously.
                    if (removedFolderDirectories.contains(file)) {
                        return false;
                    }
                    return true;
                }
            });
            for (File dir : directories) {
                boolean known = false;
                for (Folder folder : getFolders()) {
                    if (folder.getName().equals(dir.getName())) {
                        known = true;
                        break;
                    }
                    File localBase = folder.getLocalBase();
                    if (localBase.equals(dir)
                        || localBase.getAbsolutePath().startsWith(
                            dir.getAbsolutePath()))
                    {
                        known = true;
                        break;
                    }
                }
                if (!known && FileUtils.hasFiles(dir)) {
                    handleNewFolder(dir);
                }
            }
        }
    }

    // Found a new directory in the folder base. Create a new folder.
    // Only doing this if logged in.
    private void handleNewFolder(File file) {
        FolderInfo fi = null;
        Controller controller = getController();
        ServerClient client = controller.getOSClient();
        if (client.isConnected() && client.isLoggedIn()) {
            for (FolderInfo folderInfo : client.getAccountFolders()) {
                if (folderInfo.getName().equals(file.getName())) {
                    fi = folderInfo;
                    break;
                }
            }
        }
        if (fi == null) {
            fi = new FolderInfo(file.getName(),
                '[' + IdGenerator.makeId() + ']');
        }
        FolderSettings fs = new FolderSettings(file,
            SyncProfile.AUTOMATIC_SYNCHRONIZATION, false,
            ArchiveMode.FULL_BACKUP,
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS.getValueInt(controller));
        Folder folder = createFolder(fi, fs);
        folder.addDefaultExcludes();

        if (client.isBackupByDefault()) {
            if (client.isConnected() && client.isLoggedIn()) {
                boolean joined = client.joinedByCloud(folder);
                if (!joined) {
                    new CreateFolderOnServerTask(client.getAccountInfo(), fi,
                        null).scheduleTask(getController());
                }
            }
        }

        logInfo("Auto-created new folder: " + folder + " @ "
            + folder.getLocalBase());

        folderAutoCreateListener
            .folderAutoCreated(new FolderAutoCreateEvent(fi));
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
        int i = metaFolderInfo.getId().indexOf(Constants.METAFOLDER_ID_PREFIX);
        String folderId = metaFolderInfo.getId().substring(
            i + Constants.METAFOLDER_ID_PREFIX.length());
        return getFolder(folderId);
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

        File suggestedLocalBase;
        if (ConfigurationEntry.FOLDER_CREATE_USE_EXISTING
            .getValueBoolean(getController()))
        {
            // Moderate strategy. Use existing folders.
            suggestedLocalBase = new TFile(getController().getFolderRepository()
                .getFoldersBasedir(), invitation.folder.name);
            if (suggestedLocalBase.exists()) {
                logWarning("Using existing directory " + suggestedLocalBase
                    + " for " + invitation.folder);
            }
        } else {
            // Defensive strategy. Find free new empty directory.
            suggestedLocalBase = FileUtils.createEmptyDirectory(getController()
                .getFolderRepository().getFoldersBasedir(),
                invitation.folder.name);
        }

        // Is this invitation from a friend?
        boolean invitorIsFriend = false;
        MemberInfo memberInfo = invitation.getInvitor();
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
            invitation.getSuggestedSyncProfile(), false,
            ArchiveMode.FULL_BACKUP,
            ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                .getValueInt(getController()));
        createFolder(invitation.folder, folderSettings);
        return true;
    }

    // Callbacks from ServerClient on login ***********************************

    private ReentrantLock accountSyncLock = new ReentrantLock();

    public void updateFolders(Account a) {
        // TODO: Called too often
        Reject.ifNull(a, "Account");
        if (getController().getMySelf().isServer()) {
            return;
        }

        accountSyncLock.lock();
        try {
            logInfo("Syncing folder setup with account permissions("
                + a.getFolders().size() + "): " + a.getUsername() + ", "
                + a.getFolders());
            Collection<FolderInfo> created = createLocalFolders(a);
            if (ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
                .getValueBoolean(getController()))
            {
                if (ProUtil.isServerConfig(getController())) {
                    logSevere("Found server config running with client installation. "
                        + "Won't delete local folders.");
                    return;
                }
                removeLocalFolders(a, created);
            }
        } finally {
            accountSyncLock.unlock();
        }
    }

    private void removeLocalFolders(Account a, Collection<FolderInfo> skip) {
        for (Folder folder : getFolders()) {
            if (skip.contains(folder.getInfo())) {
                continue;
            }
            if (!a.hasReadPermissions(folder.getInfo())
                && getController().getOSClient().isConnected())
            {
                logWarning("Removing local " + folder + ' ' + a
                    + " does not have read permission");
                removeFolder(folder, true);
            }
        }
    }

    private synchronized Collection<FolderInfo> createLocalFolders(Account a) {
        if (!a.isValid()) {
            return Collections.emptyList();
        }
        Collection<FolderInfo> folderInfos = new ArrayList<FolderInfo>();
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
                    FOLDER_SETTINGS_PREFIX_V4 + folderEntryId
                        + FolderSettings.FOLDER_SETTINGS_DIR);
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
            if (foInfo != null) {
                // Load existing.
                createFolder0(foInfo, settings, true);
            } else {
                // Spawn/Create a new one.
                foInfo = new FolderInfo(folderName,
                    '[' + IdGenerator.makeId() + ']');
                Folder folder = createFolder(foInfo, settings);
                folder.addDefaultExcludes();
                logWarning("Folder NOT found on account " + a.getUsername()
                    + ". Created new: " + foInfo);
            }

            // Make sure it is backed up by the server.
            CreateFolderOnServerTask task = new CreateFolderOnServerTask(
                a.createInfo(), foInfo, null);
            task.setArchiveVersions(settings.getVersions());
            getController().getTaskManager().scheduleTask(task);

            // Remove from pending entries.
            it.remove();
            folderInfos.add(foInfo);
        }

        if (ConfigurationEntry.AUTO_SETUP_ACCOUNT_FOLDERS
            .getValueBoolean(getController()))
        {
            for (FolderInfo folderInfo : a.getFolders()) {
                if (hasJoinedFolder(folderInfo)) {
                    continue;
                }
                SyncProfile profile = SyncProfile.getDefault(getController());
                File suggestedLocalBase = new TFile(getController()
                    .getFolderRepository().getFoldersBasedir(),
                    folderInfo.name);
                if (removedFolderDirectories.contains(suggestedLocalBase)) {
                    continue;
                }

                UserDirectory userDir = UserDirectories.getUserDirectories()
                    .get(folderInfo.name);

                if (userDir != null) {
                    if (removedFolderDirectories.contains(userDir
                        .getDirectory()))
                    {
                        continue;
                    }
                    suggestedLocalBase = userDir.getDirectory();
                } else if (ConfigurationEntry.FOLDER_CREATE_USE_EXISTING
                    .getValueBoolean(getController()))
                {
                    // Moderate strategy. Use existing folders.
                    suggestedLocalBase = new TFile(getController()
                        .getFolderRepository().getFoldersBasedir(),
                        folderInfo.name);
                    if (suggestedLocalBase.exists()) {
                        logWarning("Using existing directory "
                            + suggestedLocalBase + " for " + folderInfo);
                    }
                } else {
                    // Defensive strategy. Find free new empty directory.
                    suggestedLocalBase = FileUtils.createEmptyDirectory(
                        getController().getFolderRepository()
                            .getFoldersBasedir(), folderInfo.name);
                }

                logInfo("Auto setting up folder " + folderInfo
                    + " for account " + a.getUsername() + " @ "
                    + suggestedLocalBase);

                // Correct local path if in UserDirectories.
                FolderSettings settings = new FolderSettings(
                    suggestedLocalBase, profile, false,
                    ArchiveMode.FULL_BACKUP,
                    ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                        .getValueInt(getController()));

                Folder folder = createFolder0(folderInfo, settings, true);
                folder.addDefaultExcludes();
                folderInfos.add(folderInfo);
            }
        }

        return folderInfos;
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
     * Delete any file archives over a specified age.
     */
    public void cleanupOldArchiveFiles() {
        int period = ConfigurationEntry.DEFAULT_ARCHIVE_CLEANUP_DAYS
            .getValueInt(getController());
        if (period == Integer.MAX_VALUE || period <= 0) { // cleanup := never
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -period);
        Date cleanupDate = cal.getTime();
        for (Folder folder : getFolders()) {
            folder.cleanupOldArchiveFiles(cleanupDate);
        }
    }

    /**
     * Do we already have a folder that has this file as its base?
     * 
     * @param file
     */
    public boolean doesFolderAlreadyExist(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        for (Folder folder : folders.values()) {
            if (file.equals(folder.getBaseDirectoryInfo().getDiskFile(this))) {
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
            for (Folder folder : getController().getFolderRepository()
                .getFolders())
            {
                if (folder.isPreviewOnly()) {
                    continue;
                }
                folder.checkSync();
            }
        }
    }

    private class AllFolderMembershipSynchronizer implements Runnable {
        private volatile boolean canceled;

        public void run() {
            ProfilingEntry pe = Profiling
                .start("synchronizeAllFolderMemberships");
            try {
                if (canceled) {
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
                    node.synchronizeFolderMemberships();
                    if (canceled) {
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

}