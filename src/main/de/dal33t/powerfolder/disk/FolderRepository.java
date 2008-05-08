/* $Id: FolderRepository.java,v 1.75 2006/04/23 17:09:00 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DIR;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DONT_RECYCLE;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_ID;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREVIEW;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SECRET;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_SYNC_PROFILE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.InvitationReceivedEvent;
import de.dal33t.powerfolder.event.InvitationReceivedHandler;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderSetupPanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.FileUtils;
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
    private Map<FolderInfo, Folder> folders;
    private Thread myThread;
    private FileRequestor fileRequestor;
    private Folder currentlyMaintaitingFolder;
    // Flag if the repo is already started
    private boolean started;
    // The trigger to start scanning
    private Object scanTrigger = new Object();
    private boolean triggered;

    /** folder repo listners */
    private FolderRepositoryListener listenerSupport;

    /** handler for incomming Invitations */
    private InvitationReceivedHandler invitationReceivedHandler;

    /** handler if files with posible filename problems are found */
    private FileNameProblemHandler fileNameProblemHandler;

    /** The disk scanner */
    private FolderScanner folderScanner;

    /** Field list for backup taget pre #777. Used to convert to new
     * backup target for #787.
     */
    private static final String PRE_777_BACKUP_TARGET_FIELD_LIST =
            "true,true,true,true,0,false,12,0,m";

    public FolderRepository(Controller controller) {
        super(controller);

        this.triggered = false;
        // Rest
        this.folders = new ConcurrentHashMap<FolderInfo, Folder>();
        this.fileRequestor = new FileRequestor(controller);
        // this.netListProcessor = new NetworkFolderListProcessor();
        this.started = false;

        this.folderScanner = new FolderScanner(getController());

        // Create listener support
        this.listenerSupport = (FolderRepositoryListener) ListenerSupportFactory
            .createListenerSupport(FolderRepositoryListener.class);
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
        ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        log().debug("setSuspendFireEvents: " + suspended);
    }

    /**
     * @return true if any folder is currently synching
     */
    public boolean isAnyFolderTransferring() {
        for (Folder folder : folders.values()) {
            if (folder.isTransferring()) {
                return true;
            }
        }
        return false;
    }

    public boolean isShutdownAllowed() {
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        if (warnOnClose) {
            List<Folder> foldersToWarn = new ArrayList<Folder>(
                getFolders().length);
            for (Folder folder : getFolders()) {
                if (folder.isTransferring()) {
                    log().warn("Close warning on folder: " + folder);
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
                    JFrame frame = getController().getUIController()
                        .getMainFrame().getUIComponent();
                    String title = Translation
                        .getTranslation("folderrepository.warnonclose.title");
                    String text = Translation.getTranslation(
                        "folderrepository.warnonclose.text", folderslist
                            .toString());
                    String question = Translation
                        .getTranslation("general.neverAskAgain");
                    NeverAskAgainResponse response = DialogFactory
                        .genericDialog(frame, title, text, new String[]{
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
                log().warn("server closing while folders are not synchronized");
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

        Properties config = getController().getConfig();

        // Find all folder names.
        Set<String> allFolderNames = new TreeSet<String>();
        for (Enumeration<String> en = (Enumeration<String>) config
            .propertyNames(); en.hasMoreElements();)
        {
            String propName = en.nextElement();

            // Look for a folder.<foldername>.XXXX
            if (propName.startsWith(FOLDER_SETTINGS_PREFIX)) {
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

        // Scan config for all found folder names.
        for (final String folderName : allFolderNames) {

            String folderId = config.getProperty(FOLDER_SETTINGS_PREFIX
                + folderName + FOLDER_SETTINGS_ID);
            boolean folderSecret = "true".equalsIgnoreCase(config
                .getProperty(FOLDER_SETTINGS_PREFIX + folderName
                    + FOLDER_SETTINGS_SECRET));
            FolderInfo foInfo = new FolderInfo(folderName, folderId,
                folderSecret);

            FolderSettings folderSettings = loadFolderSettings(folderName);

            try {
                // Do not add if already added
                if (!hasJoinedFolder(foInfo) && folderId != null) {
                    createFolder0(foInfo, folderSettings, false);
                }
            } catch (FolderException e) {

                // Bad / corrupt folder config?
                showFolderException(config, folderName, e, foInfo);
            }
        }

        // Set the folders base with a desktop ini.
        FileUtils.maintainDesktopIni(getController(),
                new File(getFoldersBasedir()));
    }

    public FolderSettings loadFolderSettings(String folderName) {

        Properties config = getController().getConfig();

        String folderDir = config.getProperty(FOLDER_SETTINGS_PREFIX
            + folderName + FOLDER_SETTINGS_DIR);

        String syncProfConfig = config.getProperty(FOLDER_SETTINGS_PREFIX
            + folderName + FOLDER_SETTINGS_SYNC_PROFILE);

        // Migration for #603
        if ("autodownload_friends".equals(syncProfConfig)) {
            syncProfConfig = SyncProfile.AUTO_DOWNLOAD_FRIENDS
                    .getFieldList();
        }

        SyncProfile syncProfile;
        if (syncProfConfig.equals(PRE_777_BACKUP_TARGET_FIELD_LIST)) {

            // Migration for #787 (backup target timeBetweenScans changed
            // from 0 to 60).
            syncProfile = SyncProfile.BACKUP_TARGET;
        } else {

            // Load profile from field list.
            syncProfile = SyncProfile.getSyncProfileByFieldList(syncProfConfig);
        }

        // Inverse logic for backward compatability.
        boolean useRecycleBin = !"true".equalsIgnoreCase(config
            .getProperty(FOLDER_SETTINGS_PREFIX + folderName
                + FOLDER_SETTINGS_DONT_RECYCLE));

        boolean preview = "true".equalsIgnoreCase(config
            .getProperty(FOLDER_SETTINGS_PREFIX + folderName
                + FOLDER_SETTINGS_PREVIEW));

        if (folderDir == null) {
            System.err.println(folderName);
        }
        return new FolderSettings(new File(folderDir), syncProfile, false,
            useRecycleBin, preview);
    }

    /**
     * Folder creation exception. Log and
     *
     * @param config
     * @param folderName
     * @param e
     * @param foInfo
     */
    private void showFolderException(final Properties config,
        final String folderName, final FolderException e,
        final FolderInfo foInfo)
    {

        // log it.
        log().error(e);

        // Delete the bad folder config and advise user.
        Runnable runner = new Runnable() {
            public void run() {
                // TODO Possibly takeover settings from folder into wizard.
                FolderSettings settings = loadFolderSettings(folderName);
                removeFolderFromConfig(config, folderName);
                // Show error
                e.show(getController(), Translation
                    .getTranslation("folderrepository.please_recreate"));

                FolderSetupPanel setupPanel = new FolderSetupPanel(
                    getController(), foInfo.name);
                ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
                    getController(), settings.getLocalBaseDir()
                        .getAbsolutePath(), setupPanel);
                PFWizard wizard = new PFWizard(getController());
                wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                    Icons.FILESHARING_PICTO);
                wizard.open(panel);
            }
        };
        getController().getUIController().invokeLater(runner);
    }

    /**
     * Removes unused folder infos from the config.
     *
     * @param config
     * @param errorFolderNames
     */
    private void removeFolderFromConfig(Properties config, String folderName) {
        List<String> configErrors = new ArrayList<String>();

        // Remove folder info from config
        String folderConfigPrefix = FOLDER_SETTINGS_PREFIX + folderName;
        for (Iterator it = config.keySet().iterator(); it.hasNext();) {
            String configKey = (String) it.next();
            if (configKey.startsWith(folderConfigPrefix)) {
                configErrors.add(configKey);
            }
        }

        // Remove bad config entries.
        for (String configKey : configErrors) {
            config.remove(configKey);
        }

        // Save config.
        getController().saveConfig();
    }

    /**
     * Starts the folder repo maintenance thread
     */
    public void start() {
        if (!ConfigurationEntry.FOLDER_REPOSITORY_ENABLED
            .getValueBoolean(getController()))
        {
            log().warn("Not starting FolderRepository. disabled by config");
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
        log().debug("Stopped");
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
     * @return the folders
     */
    public Folder[] getFolders() {
        return folders.values().toArray(new Folder[0]);
    }

    /**
     * @return the folders, sorted as List
     */
    public List<Folder> getFoldersAsSortedList() {
        List<Folder> foldersList = new ArrayList<Folder>(folders.values());
        Collections.sort(foldersList, new FolderComparator());
        return foldersList;
    }

    /**
     * TODO Experimetal: Hands out a indirect reference to the value of internal
     * hashmap.
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
     * @return a fresh list of all joined folders
     */
    public FolderInfo[] getJoinedFolderInfos() {
        return folders.keySet().toArray(new FolderInfo[0]);
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
     * @throws FolderException
     *             if something went wrong
     */
    public Folder createFolder(FolderInfo folderInfo,
        FolderSettings folderSettings) throws FolderException
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
     * @throws FolderException
     */
    public Folder createPreviewFolder(FolderInfo folderInfo,
        FolderSettings folderSettings) throws FolderException
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
     * @throws FolderException
     *             if something went wrong
     */
    private Folder createFolder0(FolderInfo folderInfo,
        FolderSettings folderSettings, boolean saveConfig)
        throws FolderException
    {
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(folderSettings, "FolderSettings is null");

        // If non-preview folder and already have this folder as preview,
        // silently remove the preview.
        if (!folderSettings.isPreviewOnly()) {
            for (Folder folder : getFoldersAsSortedList()) {
                if (folder.isPreviewOnly() &&
                        folder.getInfo().equals(folderInfo)) {
                    log().info("Removed preview folder " +
                    folder.getName());
                    removeFolder(folder, true);
                    break;
                }
            }
        }

        if (hasJoinedFolder(folderInfo)) {
            throw new FolderException(folderInfo, "Already joined folder");
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
        synchronizeAllFolderMemberships();

        // Calc stats
        folder.getStatistic().scheduleCalculate();

        // Trigger scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requestor
        getController().getFolderRepository().fileRequestor
            .triggerFileRequesting(folder.getInfo());

        // Fire event
        fireFolderCreated(folder);

        log().info(
            "Joined folder " + folderInfo.name + ", local copy at '"
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

        // store folder in config
        Properties config = getController().getConfig();
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name
            + FOLDER_SETTINGS_ID, folderInfo.id);
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name
            + FOLDER_SETTINGS_DIR, folderSettings.getLocalBaseDir()
            .getAbsolutePath());
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name
            + FOLDER_SETTINGS_SECRET, String.valueOf(folderInfo.secret));
        // Save sync profiles as internal configuration for custom profiles.
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name +
                FOLDER_SETTINGS_SYNC_PROFILE, folderSettings.getSyncProfile()
                .getFieldList());
        // Inverse logic for backward compatability.
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name
            + FOLDER_SETTINGS_DONT_RECYCLE, String.valueOf(!folderSettings
            .isUseRecycleBin()));
        config.setProperty(FOLDER_SETTINGS_PREFIX + folderInfo.name
            + FOLDER_SETTINGS_PREVIEW, String.valueOf(folderSettings
            .isPreviewOnly()));

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

        // remove folder from config
        Properties config = getController().getConfig();
        String folderConfigPrefix = FOLDER_SETTINGS_PREFIX
            + folder.getInfo().name;
        synchronized (config) {
            for (Iterator it = config.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                if (key.startsWith(folderConfigPrefix)) {
                    it.remove();
                }
            }
        }

        // Save config
        getController().saveConfig();

        // Remove internal
        folders.remove(folder.getInfo());

        // Shutdown folder
        folder.shutdown();

        // synchronizememberships
        synchronizeAllFolderMemberships();

        // Abort scanning
        boolean folderCurrentlyScannng = folder.equals(getFolderScanner()
            .getCurrentScanningFolder());
        if (folderCurrentlyScannng) {
            getFolderScanner().abortScan();
        }

        // Delete the .PowerFolder dir and contents
        if (deleteSystemSubDir) {
            File systemSubDir = folder.getSystemSubDir();
            File[] files = systemSubDir.listFiles();
            for (File file : files) {
                if (!file.delete()) {
                    log().error("Failed to delete: " + file);
                }
            }
            if (!systemSubDir.delete()) {
                log().error("Failed to delete: " + systemSubDir);
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
        log().warn("Removing node from all folders: " + member);
        for (Folder folder : getFolders()) {
            folder.remove(member);
        }
        log().warn("Node removed from all folders: " + member);
    }

    /**
     * Synchronizes all known members with our folders
     */
    private void synchronizeAllFolderMemberships() {
        if (!started) {
            log().verbose(
                "Not synchronizing Foldermemberships, repo not started, yet");
        }
        if (logVerbose) {
            log().verbose("All Nodes: Synchronize Foldermemberships");
        }
        Collection<Member> connectedNodes = getController().getNodeManager()
            .getConnectedNodes();
        FolderInfo[] myJoinedFolders = getJoinedFolderInfos();
        for (Member node : connectedNodes) {
            node.synchronizeFolderMemberships(myJoinedFolders);
        }
    }

    /**
     * Broadcasts a remote scan commando on all folders.
     */
    public void broadcastScanCommandOnAllFolders() {
        if (logDebug) {
            log().debug("Sending remote scan commando");
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
        if (logVerbose) {
            log().verbose("Scan triggerd");
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
        long waitTime = getController().getWaitTime() / 10;

        if (getController().isUIEnabled()) {
            // Wait to build up ui
            try {
                // inital wait before first scan
                synchronized (scanTrigger) {
                    scanTrigger.wait(getController().getWaitTime() * 4);
                }
            } catch (InterruptedException e) {
                log().verbose(e);
                return;
            }
        }

        List<Folder> scanningFolders = new ArrayList<Folder>();
        while (!myThread.isInterrupted() && myThread.isAlive()) {
            // Only scan if not in silent mode
            if (!getController().isSilentMode()) {
                scanningFolders.clear();
                scanningFolders.addAll(folders.values());
                if (logVerbose) {
                    log()
                        .verbose(
                            "Maintaining " + scanningFolders.size()
                                + " folders...");
                }
                Collections.sort(scanningFolders, new FolderComparator());

                for (Folder folder : scanningFolders) {
                    currentlyMaintaitingFolder = folder;
                    // Fire event
                    fireMaintanceStarted(currentlyMaintaitingFolder);
                    currentlyMaintaitingFolder.maintain();
                    Folder maintainedFolder = currentlyMaintaitingFolder;
                    currentlyMaintaitingFolder = null;
                    // Fire event
                    fireMaintenanceFinished(maintainedFolder);

                    if (getController().isSilentMode()
                        || myThread.isInterrupted())
                    {
                        break;
                    }

                    // Wait a bit to give other waiting sync processes time...
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                if (logVerbose) {
                    log().verbose(
                        "Maintained " + scanningFolders.size() + " folder(s)");
                }
            }

            if (!triggered) {
                try {
                    // use waiter, will quit faster
                    synchronized (scanTrigger) {
                        scanTrigger.wait(waitTime);
                    }
                } catch (InterruptedException e) {
                    log().verbose(e);
                    break;
                }
            }
            triggered = false;
        }
    }

    /**
     * Processes a invitation to a folder TODO: Autojoin invitation, make this
     * configurable in pref screen.
     * <P>
     * 
     * @param invitation
     * @param processSilently
     *            if the invitation should be processed silently if already on
     *            folder (no error)
     */
    public void invitationReceived(final Invitation invitation,
        final boolean processSilently)
    {
        if (invitationReceivedHandler == null) {
            // No invitation handler? do nothing.
            return;
        }
        Reject.ifNull(invitation, "Invitation is null");
        InvitationReceivedEvent event = new InvitationReceivedEvent(this,
            invitation, processSilently);
        invitationReceivedHandler.invitationReceived(event);
    }

    /*
     * General ****************************************************************
     */

    public String toString() {
        return "Folders of " + getController().getMySelf().getNick();
    }

    // Event support **********************************************************

    private void fireFolderCreated(Folder folder) {
        listenerSupport.folderCreated(new FolderRepositoryEvent(this, folder));
    }

    private void fireFolderRemoved(Folder folder) {
        listenerSupport.folderRemoved(new FolderRepositoryEvent(this, folder));
    }

    private void fireMaintanceStarted(Folder folder) {
        listenerSupport.maintenanceStarted(new FolderRepositoryEvent(this,
            folder));
    }

    private void fireMaintenanceFinished(Folder folder) {
        listenerSupport.maintenanceFinished(new FolderRepositoryEvent(this,
            folder));
    }

    public void addFolderRepositoryListener(FolderRepositoryListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeFolderRepositoryListener(FolderRepositoryListener listener)
    {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    public void setInvitationReceivedHandler(
        InvitationReceivedHandler invitationReceivedHandler)
    {
        this.invitationReceivedHandler = invitationReceivedHandler;
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
}