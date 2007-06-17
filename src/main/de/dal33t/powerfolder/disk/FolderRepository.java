/* $Id: FolderRepository.java,v 1.75 2006/04/23 17:09:00 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;

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
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.NeverAskAgainOkCancelDialog;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Repository of all known power folders. Local and unjoined.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class FolderRepository extends PFComponent implements Runnable {
    /**
     * Disables/enables reading of metainfos of imagefiles with old scanning
     * code.
     */
    public static final boolean READ_IMAGE_META_INFOS_WITH_OLD_SCANNING = false;

    private Map<FolderInfo, Folder> folders;
    private Thread myThread;
    private FileRequestor fileRequestor;
    // Flag if the repo is already started
    private boolean started;
    // The trigger to start scanning
    private Object scanTrigger = new Object();

    /** folder repo listners */
    private FolderRepositoryListener listenerSupport;

    /** handler for incomming Invitations */
    private InvitationReceivedHandler invitationReceivedHandler;

    /** handler if files with posible filename problems are found */
    private FileNameProblemHandler fileNameProblemHandler;

    /** The disk scanner */
    private FolderScanner folderScanner;
    private FileMetaInfoReader fileMetaInfoReader;

    public FolderRepository(Controller controller) {
        super(controller);

        // Rest
        this.folders = new ConcurrentHashMap<FolderInfo, Folder>();
        this.fileRequestor = new FileRequestor(controller);
        // this.netListProcessor = new NetworkFolderListProcessor();
        this.started = false;

        this.folderScanner = new FolderScanner(getController());
        this.fileMetaInfoReader = new FileMetaInfoReader(getController());

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
    public boolean isAnyFolderSyncing() {
        for (Folder folder : getFolders()) {
            if (folder.isSynchronizing()) {
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
                if (folder.isSynchronizing()) {
                    log().warn("Close warning on folder: " + folder);
                    foldersToWarn.add(folder);
                }
            }
            if (foldersToWarn.size() > 0) {
                String folderslist = "";
                for (Folder folder : foldersToWarn) {
                    folderslist += "\n     - " + folder.getName();
                }
                if (UIUtil.isAWTAvailable() && !getController().isConsoleMode())
                {
                    JFrame frame = getController().getUIController()
                        .getMainFrame().getUIComponent();
                    String title = Translation
                        .getTranslation("folderrepository.warnonclose.title");
                    String text = Translation.getTranslation(
                        "folderrepository.warnonclose.text", folderslist);
                    String question = Translation
                        .getTranslation("folderrepository.warnonclose.neveraskagain");
                    NeverAskAgainOkCancelDialog dialog = new NeverAskAgainOkCancelDialog(
                        frame, title, text, question);
                    dialog.setVisible(true);
                    if (dialog.getOption() == NeverAskAgainOkCancelDialog.OK) {
                        if (dialog.showNeverAgain()) {
                            PreferencesEntry.WARN_ON_CLOSE.setValue(
                                getController(), false);
                        }
                        return true;
                    }
                    // CANCEL so abort shutdown
                    return false;
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

    /** Load folders from disk */
    public void init() {
        Properties config = getController().getConfig();
        // All folder with errors
        List<String> errorFolderNames = new LinkedList<String>();
        for (Enumeration en = config.propertyNames(); en.hasMoreElements();) {
            String propName = (String) en.nextElement();
            if (propName.startsWith("folder")) {
                int firstDot = propName.indexOf('.');
                int secondDot = propName.indexOf('.', firstDot + 1);

                // valid folder prop folder.<foldername>.XXXX
                if (firstDot > 0 && secondDot > 0
                    && secondDot < propName.length())
                {

                    String folderName = propName.substring(firstDot + 1,
                        secondDot);

                    if (errorFolderNames.contains(folderName)) {
                        // Folder already has error, do not try again
                        continue;
                    }

                    // check if folder already started with that name
                    String folderId = config.getProperty("folder." + folderName
                        + ".id");
                    String folderDir = config.getProperty("folder."
                        + folderName + ".dir");
                    boolean folderSecret = "true".equalsIgnoreCase(config
                        .getProperty("folder." + folderName + ".secret"));
                    // Inverse logic for backward compatability.
                    boolean useRecycleBin = !"true".equalsIgnoreCase(config
                        .getProperty("folder." + folderName
                            + ".dontuserecyclebin"));
                    final FolderInfo foInfo = new FolderInfo(folderName,
                        folderId, folderSecret);
                    String syncProfId = config.getProperty("folder."
                        + folderName + ".syncprofile");
                    SyncProfile syncProfile = SyncProfile
                        .getSyncProfileById(syncProfId);

                    try {
                        // do not add if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderDir != null)
                        {
                            FolderSettings folderSettings = new FolderSettings(
                                new File(folderDir), syncProfile, false,
                                useRecycleBin);
                            createFolder(foInfo, folderSettings);
                        }
                    } catch (FolderException e) {
                        errorFolderNames.add(folderName);
                        log().error(e);
                        // Show error
                        e.show(getController(), "Please re-create it");

                        // Remove folder from config
                        String folderConfigPrefix = "folder." + folderName;
                        for (Iterator it = config.keySet().iterator(); it
                            .hasNext();)
                        {
                            String key = (String) it.next();
                            if (key.startsWith(folderConfigPrefix)) {
                                it.remove();
                            }
                        }

                        // Save config, FIXME: Has no effect!!
                        getController().saveConfig();

                        // Join folder
                        Runnable runner = new Runnable() {
                            public void run() {
                                FolderJoinPanel panel = new FolderJoinPanel(
                                    getController(), foInfo);
                                panel.open();
                            }
                        };
                        getController().getUIController().invokeLater(runner);
                    }
                }
            }
        }
    }

    /**
     * Starts the folder repo maintenance thread
     */
    public void start() {
        folderScanner.start();
        // in shutdown the listeners are suspended, so if started again they
        // need to be enabled.
        // ListenerSupportFactory.setSuspended(listenerSupport, false);
        // Now start thread
        myThread = new Thread(this, "Folder repository");
        // set to min priority
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();

        // Start filerequestor
        fileRequestor.start();

        // Start network list processor
        // netListProcessor.start();

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
        synchronized (folders) {
            for (Folder folder : folders.values()) {
                folder.shutdown();
            }
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
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(folderSettings, "FolderSettings is null");
        if (hasJoinedFolder(folderInfo)) {
            throw new FolderException(folderInfo, "Already joined folder");
        }

        folderInfo.name = StringUtils.replace(folderInfo.name, ".", "_");
        if (folderSettings.getSyncProfile() == null) {
            // Use default syncprofile
            folderSettings.setSyncProfile(SyncProfile.MANUAL_DOWNLOAD);
        }

        Folder folder = new Folder(getController(), folderInfo, folderSettings);
        folders.put(folder.getInfo(), folder);

        // store folder in config
        Properties config = getController().getConfig();
        config.setProperty("folder." + folderInfo.name + ".id", folderInfo.id);
        config.setProperty("folder." + folderInfo.name + ".dir", folderSettings
            .getLocalBaseDir().getAbsolutePath());
        config.setProperty("folder." + folderInfo.name + ".secret", String
            .valueOf(folderInfo.secret));
        config.setProperty("folder." + folderInfo.name + ".syncprofile",
            folderSettings.getSyncProfile().getId());
        // Inverse logic for backward compatability.
        config.setProperty("folder." + folderInfo.name + ".dontuserecyclebin",
            String.valueOf(!folder.isUseRecycleBin()));

        getController().saveConfig();

        log().debug("Created " + folder);
        // Synchroniur folder memberships
        synchronizeAllFolderMemberships();

        // Trigger scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requestor
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting(folder.getInfo());

        // Fire event
        fireFolderCreated(folder);

        log().info(
            "Joined folder " + folderInfo.name + ", local copy at '"
                + folderSettings.getLocalBaseDir() + "'");

        return folder;
    }

    /**
     * @see Member#getLastFileList(FolderInfo)
     * @param folder
     * @param onlyWithFileList
     *            true: only source with a recieved filelist will be returned
     * @return a source where the folder is available.
     */
    public Member getSourceFor(FolderInfo folder, boolean onlyWithFileList) {
        List<Member> nodesWithFileList = getController().getNodeManager()
            .getNodeWithFileListFrom(folder);
        long maxFolderSize = 0;
        Member bestSource = null;
        for (Member node : nodesWithFileList) {
            // Check his last folder list
            FolderList fList = node.getLastFolderList();
            if (fList == null || fList.folders == null
                || fList.folders.length == 0)
            {
                // no folder list
                continue;
            }

            // Search for folder
            int index = fList.indexOf(folder);
            if (index < 0) {
                // not found
                continue;
            }

            FolderInfo fInfo = fList.folders[index];
            if (onlyWithFileList && !node.hasFileListFor(folder)) {
                // Ingore source if filelist is wanted
                // log().warn(
                // "Member " + node.getNick() + " on folder " + folder.name
                // + ", but has no filelist");
                continue;
            }
            if (fInfo.bytesTotal >= maxFolderSize) {
                // We found a better source for this folder
                maxFolderSize = fInfo.bytesTotal;
                bestSource = node;
            }
        }
        return bestSource;
    }

    /**
     * Removes a folder from active folders, will be added as non-local folder
     * 
     * @param folder
     */
    public void removeFolder(Folder folder) {
        if (folder == null) {
            return;
        }
        folder = getFolder(folder.getInfo());
        if (folder == null) {
            log().error("Unable to remove " + folder + ". Not known");
            return;
        }

        // Remove the desktop shortcut
        folder.removeDesktopShortcut();

        // remove folder from config
        Properties config = getController().getConfig();
        String folderConfigPrefix = "folder." + folder.getInfo().name;
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
            getFolderScanner().setAborted(true);
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
        List<Member> connectedNodes = getController().getNodeManager()
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
     * Triggers the maintenance on all folders. may or may not scan the folders -
     * depending on settings.
     */
    public void triggerMaintenance() {
        log().debug("Scan triggerd");
        synchronized (scanTrigger) {
            scanTrigger.notifyAll();
        }
    }

    /**
     * Mainenance thread for the folders
     */
    public void run() {
        long waitTime = getController().getWaitTime() * 12;

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

        while (!myThread.isInterrupted()) {
            // Scan alll folders
            log().debug("Maintaining folders...");

            // Only scan if not in silent mode
            if (!getController().isSilentMode()) {
                List<Folder> scanningFolders = new ArrayList<Folder>(folders
                    .values());
                // TODO: Sort by size, to have the small ones fast
                // Collections.sort(scanningFolders);

                // Fire event
                fireMaintanceStarted();

                for (Iterator it = scanningFolders.iterator(); it.hasNext();) {
                    Folder folder = (Folder) it.next();
                    folder.maintain();

                    if (myThread.isInterrupted()) {
                        break;
                    }

                    // Wait a bit to give other waiting sync processes time...
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                log().debug(
                    "Maintained " + scanningFolders.size() + " folder(s)");

                // Fire event
                fireMaintenanceFinished();
            }

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
    }

    public FileMetaInfoReader getFileMetaInfoReader() {
        return fileMetaInfoReader;
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
     * @param forcePopup
     *            popup application (even when minimized)
     */
    public void invitationReceived(final Invitation invitation,
        final boolean processSilently, final boolean forcePopup)
    {
        if (invitationReceivedHandler == null) {
            // No invitation handler? do nothing.
            return;
        }
        Reject.ifNull(invitation, "Invitation is null");
        InvitationReceivedEvent event = new InvitationReceivedEvent(this,
            invitation, processSilently, forcePopup);
        invitationReceivedHandler.invitationReceived(event);
    }

    /**
     * Moves files recursively from one folder to another. Hidden files are not
     * moved, so the '.PowerFolder' directory is not transferred.
     * 
     * @param oldDir
     *            source directory
     * @param newDir
     *            target directory
     * @throws java.io.IOException
     */
    public void moveFiles(File oldDir, File newDir) throws IOException {
        File[] oldFiles = oldDir.listFiles();
        for (File oldFile : oldFiles) {
            if (oldFile.isDirectory()) {

                // Move non-hidden directories
                if (!oldFile.isHidden()) {
                    File newSubDir = new File(newDir, oldFile.getName());
                    newSubDir.mkdir();
                    moveFiles(oldFile, newSubDir);
                }
            } else {

                // Move non-hidden files.
                if (!oldFile.isHidden()) {
                    oldFile.renameTo(new File(newDir, oldFile.getName()));
                }
            }
        }

        // Delete empty directories.
        if (oldDir.listFiles().length == 0) {
            oldDir.delete();
        }
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

    private void fireMaintanceStarted() {
        listenerSupport.maintenanceStarted(new FolderRepositoryEvent(this));
    }

    private void fireMaintenanceFinished() {
        listenerSupport.maintenanceFinished(new FolderRepositoryEvent(this));
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
}