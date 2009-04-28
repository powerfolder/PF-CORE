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

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DIR;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DONT_RECYCLE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_ID;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_NAME;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX_V3;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX_V4;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREVIEW;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_RECYCLE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_WHITELIST;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DOWNLOAD_SCRIPT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.SynchronizationStatsEvent;
import de.dal33t.powerfolder.event.SynchronizationStatsListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Repository of all known power folders. Local and unjoined.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class FolderRepository extends PFComponent implements Runnable {

    private static final Logger log = Logger.getLogger(FolderRepository.class
        .getName());
    private Map<FolderInfo, Folder> folders;
    private Thread myThread;
    private FileRequestor fileRequestor;
    private Folder currentlyMaintaitingFolder;
    // Flag if the repo is already started
    private boolean started;
    // The trigger to start scanning
    private Object scanTrigger = new Object();
    private boolean triggered;

    /** folder repository listeners */
    private FolderRepositoryListener folderRepositoryListenerSupport;

    /** synchronization stats listeners */
    private SynchronizationStatsListener synchronizationStatsListenerSupport;

    /** handler if files with posible filename problems are found */
    private FileNameProblemHandler fileNameProblemHandler;

    /** The disk scanner */
    private FolderScanner folderScanner;
    
    /**
     * The current synchronizater of all folder memberships
     */
    private AllFolderMembershipSynchronizer folderMembershipSynchronizer;
    private Object folderMembershipSynchronizerLock = new Object();

    /**
     * Field list for backup taget pre #777. Used to convert to new backup
     * target for #787.
     */
    private static final String PRE_777_BACKUP_TARGET_FIELD_LIST = "true,true,true,true,0,false,12,0,m";

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
        fileRequestor = new FileRequestor(controller);
        started = false;

        folderScanner = new FolderScanner(getController());

        // Create listener support
        folderRepositoryListenerSupport = ListenerSupportFactory
            .createListenerSupport(FolderRepositoryListener.class);
        synchronizationStatsListenerSupport = ListenerSupportFactory
            .createListenerSupport(SynchronizationStatsListener.class);
    }

    /** @return the handler that takes care of filename problems */
    public FileNameProblemHandler getFileNameProblemHandler() {
        return fileNameProblemHandler;
    }

    /**
     * @param fileNameProblemHandler
     *            the handler that takes care of filename problems
     */
    public void setFileNameProblemHandler(
        FileNameProblemHandler fileNameProblemHandler)
    {
        this.fileNameProblemHandler = fileNameProblemHandler;
    }

    /** @return The folder scanner that performs the scanning of files on disk */
    public FolderScanner getFolderScanner() {
        return folderScanner;
    }

    public void setSuspendFireEvents(boolean suspended) {
        ListenerSupportFactory.setSuspended(folderRepositoryListenerSupport, suspended);
        logFine("setSuspendFireEvents: " + suspended);
    }

    public boolean isShutdownAllowed() {
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        if (warnOnClose) {
            List<Folder> foldersToWarn = new ArrayList<Folder>(
                getFolders().length);
            for (Folder folder : getFolders()) {
                if (folder.isTransferring()) {
                    logWarning("Close warning on folder: " + folder);
                    foldersToWarn.add(folder);
                }
            }
            if (!foldersToWarn.isEmpty()) {
                StringBuilder folderslist = new StringBuilder();
                for (Folder folder : foldersToWarn) {
                    folderslist.append("\n     - " + folder.getName());
                }
                if (UIUtil.isAWTAvailable() && !getController().isConsoleMode())
                {
                    String title = Translation
                        .getTranslation("folder_repository.warn_on_close.title");
                    String text = Translation.getTranslation(
                        "folder_repository.warn_on_close.text", folderslist
                            .toString());
                    String question = Translation
                        .getTranslation("general.neverAskAgain");
                    NeverAskAgainResponse response = DialogFactory
                        .genericDialog(getController(), title, text, new String[]{
                            Translation.getTranslation("general.ok"),
                            Translation.getTranslation("general.cancel")}, 0,
                            GenericDialogType.QUESTION, question);
                    if (response.isNeverAskAgain()) {
                        PreferencesEntry.WARN_ON_CLOSE.setValue(
                            getController(), false);
                    }
                    return response.getButtonIndex() == 0;

                }
                // server closing someone running a server knows what he is
                // doing
                logWarning("server closing while folders are not synchronized");
                return true;

            }
            // NO Folders unsynced
            return true;

        }
        // do not warn on close so we allow shut down
        return true;
    }

    /**
     * Load folders from disk. Find all possible folder names, then find config
     * for each folder name.
     */
    public void init() {

        processV3Format();

        processV4Format();

        // Set the folders base with a desktop ini.
        FileUtils.maintainDesktopIni(getController(), new File(
            getFoldersBasedir()));
    }

    /**
     * Version 3 (and earlier) folder format was like 'folder.<folderName>.XXXX
     * This is replaced with V4 format, allowing folders with the same name
     * to be saved.
     */
    private void processV3Format() {
        final Properties config = getController().getConfig();

        // Find all folder names.
        Set<String> allFolderNames = new TreeSet<String>();
        for (Enumeration<String> en = (Enumeration<String>) config
            .propertyNames(); en.hasMoreElements();)
        {
            String propName = en.nextElement();

            // Look for a folder.<foldername>.XXXX
            if (propName.startsWith(FOLDER_SETTINGS_PREFIX_V3)) {
                int firstDot = propName.indexOf('.');
                int secondDot = propName.indexOf('.', firstDot + 1);

                if (firstDot > 0 && secondDot > 0
                    && secondDot < propName.length())
                {
                    String folderName = propName.substring(firstDot + 1,
                        secondDot);
                    allFolderNames.add(folderName);
                }
            }
        }

        // Load with 6 concurrent threads. 
        final Semaphore loadPermit = new Semaphore(6); 
        final AtomicInteger nCreated = new AtomicInteger();
        // Scan config for all found folder names.
        for (final String folderName : allFolderNames) {
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
                            .getProperty(FOLDER_SETTINGS_PREFIX_V3 + folderName
                                + FOLDER_SETTINGS_ID);
                        FolderInfo foInfo = new FolderInfo(folderName, folderId);

                        FolderSettings folderSettings = loadV3FolderSettings(folderName);

                        // Do not add if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderSettings != null)
                        {
                            createFolder0(foInfo, folderSettings, false);
                        }
                    } catch (Exception e) {
                        logSevere("Problem loading/creating folder '"
                            + folderName + "'. " + e, e);
                    }
                    loadPermit.release();
                    synchronized (nCreated) {
                        nCreated.incrementAndGet();
                        nCreated.notify();
                    }
                }
            };
            getController().getIOProvider().startIO(folderCreator);
        }

        // Wait for creators to complete
        while (nCreated.get() < allFolderNames.size()) {
            synchronized (nCreated) {
                try {
                    nCreated.wait(10);
                } catch (InterruptedException e) {
                    logFiner(e);
                    return;
                }
            }
        }
    }

    public FolderSettings loadV3FolderSettings(String folderName) {

        Properties config = getController().getConfig();

        String folderDir = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_DIR);

        if (folderDir == null) {
            logSevere("No folder directory for " + folderName);
            removeConfigEntries(FOLDER_SETTINGS_PREFIX_V3 + folderName);
            getController().saveConfig();
            return null;
        }

        String syncProfConfig = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_SYNC_PROFILE);

        // Migration for #603
        if ("autodownload_friends".equals(syncProfConfig)) {
            syncProfConfig = SyncProfile.AUTO_DOWNLOAD_FRIENDS.getFieldList();
        }

        SyncProfile syncProfile;
        if (PRE_777_BACKUP_TARGET_FIELD_LIST.equals(syncProfConfig)) {
            // Migration for #787 (backup target timeBetweenScans changed
            // from 0 to 60).
            syncProfile = SyncProfile.BACKUP_TARGET;
        } else {
            // Load profile from field list.
            syncProfile = SyncProfile.getSyncProfileByFieldList(syncProfConfig);
        }

        // Inverse logic for backward compatability.
        String dontRecycleSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_DONT_RECYCLE);
        boolean useRecycleBin = dontRecycleSetting == null
            || !"true".equalsIgnoreCase(dontRecycleSetting);

        String previewSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_PREVIEW);
        boolean preview = previewSetting != null
            && "true".equalsIgnoreCase(previewSetting);

        String whitelistSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_WHITELIST);
        boolean whitelist = whitelistSetting != null
            && "true".equalsIgnoreCase(whitelistSetting);
        
        String dlScript = config.getProperty(FOLDER_SETTINGS_PREFIX_V3
            + folderName + FOLDER_SETTINGS_DOWNLOAD_SCRIPT);
        return new FolderSettings(new File(folderDir), syncProfile, false,
            useRecycleBin, preview, whitelist, dlScript);
    }

    /**
     * Version 4 format is like f.<md5>.XXXX, where md5 is the MD5 of the folder
     * id. This format allows folders with the same name to be stored. 
     */
    private void processV4Format() {
        final Properties config = getController().getConfig();

        // Find all folder names.
        Set<String> allFolderMD5s = new TreeSet<String>();
        for (Enumeration<String> en = (Enumeration<String>) config
            .propertyNames(); en.hasMoreElements();)
        {
            String propName = en.nextElement();

            // Look for a f.<folderMD5>.XXXX
            if (propName.startsWith(FOLDER_SETTINGS_PREFIX_V4)) {
                int firstDot = propName.indexOf('.');
                int secondDot = propName.indexOf('.', firstDot + 1);

                if (firstDot > 0 && secondDot > 0
                    && secondDot < propName.length())
                {
                    String folderMD5 = propName.substring(firstDot + 1,
                        secondDot);
                    allFolderMD5s.add(folderMD5);
                }
            }
        }

        // Load with 6 concurrent threads.
        final Semaphore loadPermit = new Semaphore(6);
        final AtomicInteger nCreated = new AtomicInteger();
        // Scan config for all found folder MD5s.
        for (final String folderMD5 : allFolderMD5s) {
            try {
                loadPermit.acquire();
            } catch (InterruptedException e) {
                logFiner(e);
                return;
            }
            Runnable folderCreator = new Runnable() {
                public void run() {
                    try {
                        String folderId = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
                            + folderMD5 + FOLDER_SETTINGS_ID);
                        String folderName = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
                            + folderMD5 + FOLDER_SETTINGS_NAME);
                        FolderInfo foInfo = new FolderInfo(folderName, folderId);
                        FolderSettings folderSettings = loadV4FolderSettings(folderMD5);

                        // Do not add if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderSettings != null)
                        {
                            createFolder0(foInfo, folderSettings, false);
                        }
                    } catch (Exception e) {
                        logSevere("Problem loading/creating folder #"
                            + folderMD5 + ". " + e, e);
                    }
                    loadPermit.release();
                    synchronized (nCreated) {
                        nCreated.incrementAndGet();
                        nCreated.notify();
                    }
                }
            };
            getController().getIOProvider().startIO(folderCreator);
        }

        // Wait for creators to complete
        while (nCreated.get() < allFolderMD5s.size()) {
            synchronized (nCreated) {
                try {
                    nCreated.wait(10);
                } catch (InterruptedException e) {
                    logFiner(e);
                    return;
                }
            }
        }
    }

    public FolderSettings loadV4FolderSettings(String folderMD5) {

        Properties config = getController().getConfig();

        String folderDir = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_DIR);

        if (folderDir == null) {
            logSevere("No folder directory for " + folderMD5);
            removeConfigEntries(FOLDER_SETTINGS_PREFIX_V4 + folderMD5);
            getController().saveConfig();
            return null;
        }

        String syncProfConfig = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_SYNC_PROFILE);

        // Migration for #603
        if ("autodownload_friends".equals(syncProfConfig)) {
            syncProfConfig = SyncProfile.AUTO_DOWNLOAD_FRIENDS.getFieldList();
        }

        SyncProfile syncProfile;
        if (PRE_777_BACKUP_TARGET_FIELD_LIST.equals(syncProfConfig)) {
            // Migration for #787 (backup target timeBetweenScans changed
            // from 0 to 60).
            syncProfile = SyncProfile.BACKUP_TARGET;
        } else {
            // Load profile from field list.
            syncProfile = SyncProfile.getSyncProfileByFieldList(syncProfConfig);
        }

        String recycleSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_RECYCLE);
        boolean useRecycleBin = recycleSetting == null
            && "true".equalsIgnoreCase(recycleSetting);

        String previewSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_PREVIEW);
        boolean preview = previewSetting != null
            && "true".equalsIgnoreCase(previewSetting);

        String whitelistSetting = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_WHITELIST);
        boolean whitelist = whitelistSetting != null
            && "true".equalsIgnoreCase(whitelistSetting);
        
        String dlScript = config.getProperty(FOLDER_SETTINGS_PREFIX_V4
            + folderMD5 + FOLDER_SETTINGS_DOWNLOAD_SCRIPT);

        return new FolderSettings(new File(folderDir), syncProfile, false,
            useRecycleBin, preview, whitelist, dlScript);
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
        myThread = new Thread(this, "Folder repository");
        // set to min priority
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();

        // Start filerequestor
        fileRequestor.start();

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
        for (Folder folder : folders.values()) {
            folder.shutdown();
        }

        // make sure that on restart of folder the folders are freshly read
        folders.clear();
        logFine("Stopped");
    }

    /**
     * @return the default basedir for all folders. basedir is just suggested
     */
    public String getFoldersBasedir() {
        return ConfigurationEntry.FOLDER_BASEDIR.getValue(getController());
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
        return folders.containsKey(info);
    }

    /**
     * @param info
     * @return the folder by info, or null if folder is not found
     */
    public Folder getFolder(FolderInfo info) {
        return folders.get(info);
    }

    /**
     * TODO Replace calls to this by {@link #getFoldersAsCollection()}
     * 
     * @return the folders
     */
    public Folder[] getFolders() {
        return folders.values().toArray(new Folder[folders.values().size()]);
    }

    /**
     * The indirect reference to the internal concurrect hashmap. Contents may
     * changed after get. Very fast.
     * 
     * @return the folders as unmodifiable collection
     */
    public Collection<Folder> getFoldersAsCollection() {
        return Collections.unmodifiableCollection(folders.values());
    }

    /**
     * @return the number of folders
     */
    public int getFoldersCount() {
        return folders.size();
    }

    /**
     * @return an unmodifiable, but thread safe collection of all joined
     *         folders.
     */
    public Collection<FolderInfo> getJoinedFolderInfos() {
        return Collections.unmodifiableCollection(folders.keySet());
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
        return createFolder0(folderInfo, folderSettings, true);
    }

    /**
     * Used when creating a preview folder. FolderSettings should be as required
     * for the preview folder. Note that settings are not stored and the caller
     * is responsible for setting the preview config.
     * 
     * @param folderInfo
     * @param folderSettings
     * @return
     */
    public Folder createPreviewFolder(FolderInfo folderInfo,
        FolderSettings folderSettings)
    {
        return createFolder0(folderInfo, folderSettings, false);
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
     * @param saveConfig
     *            true if the configuration file should be saved after creation.
     * @return the freshly created folder
     */
    private Folder createFolder0(FolderInfo folderInfo,
        FolderSettings folderSettings, boolean saveConfig)
    {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(folderSettings, "FolderSettings is null");

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
            }
        }

        if (hasJoinedFolder(folderInfo)) {
            return folders.get(folderInfo);
        }

        // Make name that can be used as part of file name.
        folderInfo.name = StringUtils.replace(folderInfo.name, ".", "_");

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
        
        folders.put(folder.getInfo(), folder);
        saveFolderConfig(folderInfo, folderSettings, saveConfig);

        // Synchronize folder memberships
        triggerSynchronizeAllFolderMemberships();

        // Calc stats
        folder.getStatistic().scheduleCalculate();

        // Trigger scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requestor
        fileRequestor.triggerFileRequesting(folder.getInfo());

        // Trigger server connect
        getController().getOSClient().connectHostingServers();

        // Fire event
        fireFolderCreated(folder);

        logInfo("Joined folder " + folderInfo.name + ", local copy at '"
            + folderSettings.getLocalBaseDir() + '\'');

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
        // #1448 - remove any old V3 config entries before saving V4 ones.
        removeConfigEntries(FOLDER_SETTINGS_PREFIX_V3 + folderInfo.name);

        String md5 = new String(Util.encodeHex(Util.md5(folderInfo.id.getBytes())));
        // store folder in config
        Properties config = getController().getConfig();
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_NAME, folderInfo.name);
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_ID, folderInfo.id);
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DIR, folderSettings.getLocalBaseDir()
            .getAbsolutePath());
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_SYNC_PROFILE, folderSettings.getSyncProfile()
            .getFieldList());
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_RECYCLE, String.valueOf(folderSettings
            .isUseRecycleBin()));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(folderSettings
            .isPreviewOnly()));
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_WHITELIST, String.valueOf(folderSettings
            .isWhitelist()));
        String dlScript = folderSettings.getDownloadScript() != null
            ? folderSettings.getDownloadScript()
            : "";
        config.setProperty(FOLDER_SETTINGS_PREFIX_V4 + md5
            + FOLDER_SETTINGS_DOWNLOAD_SCRIPT, dlScript);

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

        // Remove the desktop shortcut
        folder.removeDesktopShortcut();

        // Remove desktop ini if it exists
        FileUtils.deleteDesktopIni(folder.getLocalBase());

        // remove folder from config
        String md5 = new String(Util.encodeHex(Util.md5(folder.getInfo().id.getBytes())));
        removeConfigEntries(FOLDER_SETTINGS_PREFIX_V4 + md5);

        // Save config
        getController().saveConfig();

        // Remove internal
        folders.remove(folder.getInfo());
        
        // Break transfers
        getController().getTransferManager().breakTransfers(folder.getInfo());

        // Shutdown folder
        folder.shutdown();

        // synchronizememberships
        triggerSynchronizeAllFolderMemberships();

        // Abort scanning
        boolean folderCurrentlyScannng = folder.equals(folderScanner
            .getCurrentScanningFolder());
        if (folderCurrentlyScannng) {
            folderScanner.abortScan();
        }

        // Delete the .PowerFolder dir and contents
        if (deleteSystemSubDir) {
            try {
                FileUtils.recursiveDelete(folder.getSystemSubDir());
            } catch (IOException e) {
                logSevere("Failed to delete: " + folder.getSystemSubDir());
            }
            
            // Try to delete the invitation.
            File invite = new File(folder.getLocalBase(), folder.getName()
                + ".invitation");
            if (invite.exists()) {
                try {
                    invite.delete();
                } catch (Exception e) {
                    logSevere("Failed to delete invitation: "
                        + invite.getAbsolutePath(), e);
                }
            }

            // Remove the folder if totally empty.
            if (folder.getLocalBase().listFiles().length == 0) {
                try {
                    folder.getLocalBase().delete();
                } catch (Exception e) {
                    logSevere("Failed to delete local base: "
                        + folder.getLocalBase().getAbsolutePath(), e);
                }
            }
        }

        // Fire event
        fireFolderRemoved(folder);
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
        for (Folder folder : getFolders()) {
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
    private void triggerSynchronizeAllFolderMemberships() {
        if (!started) {
            logFiner("Not synchronizing Foldermemberships, repo not started, yet");
        }
        synchronized (folderMembershipSynchronizerLock) {
            if (folderMembershipSynchronizer != null) {
                // Cancel the syncer
                folderMembershipSynchronizer.canceled = true;
            }
            folderMembershipSynchronizer =  new AllFolderMembershipSynchronizer();
            if (getController().getThreadPool() != null) {
                getController().getThreadPool().submit(folderMembershipSynchronizer);
            }
        }
    }

    /**
     * Broadcasts a remote scan commando on all folders.
     */
    public void broadcastScanCommandOnAllFolders() {
        if (log.isLoggable(Level.FINE)) {
            logFine("Sending remote scan commando");
        }
        for (Folder folder : getFolders()) {
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
     * Triggers the maintenance on all folders. may or may not scan the folders -
     * depending on settings.
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

        // 500 ms wait
        long waitTime = Controller.getWaitTime() / 10;

        if (getController().isUIEnabled()) {
            // Wait to build up ui
            Waiter w = new Waiter(30L * 1000);
            while (!w.isTimeout()) {
                if (getController().isUIOpen()) {
                    break;
                }
                try {
                    w.waitABit();
                } catch (Exception e) {
                    return;
                }
            }
            try {
                // initial wait before first scan
                synchronized (scanTrigger) {
                    scanTrigger.wait(Controller.getWaitTime() * 4);
                }
            } catch (InterruptedException e) {
                logFiner(e);
                return;
            }
        }

        List<Folder> scanningFolders = new ArrayList<Folder>();
        Controller controller = getController();

        while (!myThread.isInterrupted() && myThread.isAlive()) {
            // Only scan if not in silent mode
            if (!controller.isSilentMode()) {
                scanningFolders.clear();
                for (Folder folder : folders.values()) {
                    if (folder.isMaintenanceRequired()) {
                        scanningFolders.add(folder);
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

                    if (controller.isSilentMode() || myThread.isInterrupted()) {
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
     * General ****************************************************************
     */

    public String toString() {
        return "Folders of " + getController().getMySelf().getNick();
    }

    /**
     * Returns true if any folders are synchronizing.
     * Only use to initialize values - register a SynchronizationStatsListener
     * for on-going updates.
     *
     * @return
     */
    public boolean isSynchronizing() {
        // @todo harry to implement
        return false;
    }

    /**
     * Returns sync date.
     * Only use to initialize values - register a SynchronizationStatsListener
     * for on-going updates.
     *
     * @return
     */
    public Date getSynchronizationDate() {
        // @todo harry to implement
        return new Date();
    }

    // Event support **********************************************************

    private void fireFolderCreated(Folder folder) {
        folderRepositoryListenerSupport.folderCreated(new FolderRepositoryEvent(this, folder));
    }

    private void fireFolderRemoved(Folder folder) {
        folderRepositoryListenerSupport.folderRemoved(new FolderRepositoryEvent(this, folder));
    }

    private void fireMaintanceStarted(Folder folder) {
        folderRepositoryListenerSupport.maintenanceStarted(new FolderRepositoryEvent(this,
            folder));
        // TODO Why not to use existing event maintenanceStarted/fireMaintenanceFinished?
        // @todo harry to implement real event
        synchronizationStatsListenerSupport.synchronizationStatsChanged(
                new SynchronizationStatsEvent(this, new Date(), true));
    }

    private void fireMaintenanceFinished(Folder folder) {
        folderRepositoryListenerSupport.maintenanceFinished(new FolderRepositoryEvent(this,
            folder));
        // TODO Why not to use existing event maintenanceStarted/fireMaintenanceFinished?
        // @todo harry to implement real event
        synchronizationStatsListenerSupport.synchronizationStatsChanged(
                new SynchronizationStatsEvent(this, new Date(), false));
    }

    public void addSynchronizationStatsListener(SynchronizationStatsListener l) {
        ListenerSupportFactory.addListener(synchronizationStatsListenerSupport, l);
    }

    public void removeSynchronizationStatsListener(SynchronizationStatsListener l) {
        ListenerSupportFactory.removeListener(synchronizationStatsListenerSupport, l);
    }

    public void addFolderRepositoryListener(FolderRepositoryListener listener) {
        ListenerSupportFactory.addListener(folderRepositoryListenerSupport, listener);
    }

    public void removeFolderRepositoryListener(FolderRepositoryListener listener)
    {
        ListenerSupportFactory.removeListener(folderRepositoryListenerSupport, listener);
    }

    /**
     * Remove all preview folders
     */
    public void removeAllPreviewFolders() {
        for (Folder folder : folders.values()) {
            if (folder.isPreviewOnly()) {
                removeFolder(folder, true);
            }
        }
    }

    /**
     * Expect something like 'f.c70001efd21928644ee14e327aa94724'
     * or 'folder.TEST-Contacts' to remove config entries beginning with these.
     * @param prefix
     */
    private void removeConfigEntries(String prefix) {
        Properties config = getController().getConfig();
        for (Enumeration<String> en = (Enumeration<String>) config
            .propertyNames(); en.hasMoreElements();)
        {
            String propName = en.nextElement();

            // Add a dot to prefix, like 'folder.TEST-Contacts.', to prevent it
            // from also deleting things like 'folder.TEST.XXXXX'.
            if (propName.startsWith(prefix + '.')) {
                config.remove(propName);
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
                Collection<FolderInfo> myJoinedFolders = getJoinedFolderInfos();
                for (Member node : connectedNodes) {
                    node.synchronizeFolderMemberships(myJoinedFolders);
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