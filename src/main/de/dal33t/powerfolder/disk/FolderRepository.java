/* $Id: FolderRepository.java,v 1.75 2006/04/23 17:09:00 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
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
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.NetworkFolderList;
import de.dal33t.powerfolder.message.RequestNetworkFolderList;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.FolderComparator;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
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
     * Flag which switches old and new scanning code.
     * <p>
     * For 1.0.2 the old scanning code is still active!
     * <p>
     * See trac #273
     */
    public static final boolean USE_NEW_SCANNING_CODE = false;
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
    /** The date of the last request for network folder list */
    private Date lastNetworkFolderListRequest;
    /** The date when the list cleanup of the network folder list was made */
    private Date lastNetworkFolderListCleanup;
    /** model for unjoined folders. contains FolderInfo -> FolderDetails */
    private Map<FolderInfo, FolderDetails> networkFolders;
    /**
     * The received network folder lists. lists will be processed from time to
     * time.
     */
    private List<NetworkFolderList> receivedNetworkFolderLists;
    /**
     * Processor, which processes the received network folder lists from time to
     * time
     */
    private NetworkFolderListProcessor netListProcessor;

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

        this.networkFolders = Collections
            .synchronizedMap(new HashMap<FolderInfo, FolderDetails>());
        this.receivedNetworkFolderLists = Collections
            .synchronizedList(new ArrayList<NetworkFolderList>());

        // Rest
        this.folders = Collections
            .synchronizedMap(new HashMap<FolderInfo, Folder>());
        this.fileRequestor = new FileRequestor(controller);
        this.netListProcessor = new NetworkFolderListProcessor();
        this.started = false;

        if (USE_NEW_SCANNING_CODE) {
            this.folderScanner = new FolderScanner(getController());
            this.fileMetaInfoReader = new FileMetaInfoReader(getController());
        }

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

    /** for debug * */
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
                            createFolder(foInfo, new File(folderDir),
                                syncProfile, false);
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
        if (USE_NEW_SCANNING_CODE) {
            folderScanner.start();
        }
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
        netListProcessor.start();

        started = true;
    }

    /**
     * Shuts down folder repo
     */
    public void shutdown() {
        if (USE_NEW_SCANNING_CODE) {
            folderScanner.shutdown();
        }

        if (myThread != null) {
            myThread.interrupt();
        }

        // Stop processor
        netListProcessor.shutdown();

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
     * Returns the default basedir for all folders. basedir is just suggested
     * 
     * @return
     */
    public String getFoldersBasedir() {
        return ConfigurationEntry.FOLDER_BASEDIR.getValue(getController());
    }

    /**
     * Returns the file requestor
     * 
     * @return
     */
    public FileRequestor getFileRequestor() {
        return fileRequestor;
    }

    /**
     * Answers if folder is in repo
     * 
     * @param info
     * @return
     */
    public boolean hasJoinedFolder(FolderInfo info) {
        return folders.containsKey(info);
    }

    /**
     * Returns the folder by info, or null if folder is not found
     * 
     * @param info
     * @return
     */
    public Folder getFolder(FolderInfo info) {
        return folders.get(info);
    }

    /**
     * Returns the folders
     * 
     * @return
     */
    public Folder[] getFolders() {
        synchronized (folders) {
            Folder[] realFolders = new Folder[folders.size()];
            return folders.values().toArray(realFolders);
        }
    }

    /**
     * Returns the folders, sorted as List
     * 
     * @return
     */
    public List<Folder> getFoldersAsSortedList() {
        synchronized (folders) {
            List<Folder> foldersList = new ArrayList<Folder>(folders.values());
            Collections.sort(foldersList, new FolderComparator());
            return foldersList;
        }
    }

    /**
     * Answers the number of folders
     * 
     * @return
     */
    public int getFoldersCount() {
        return folders.size();
    }

    /**
     * Returns a list of all joined folders
     * 
     * @return
     */
    public FolderInfo[] getJoinedFolderInfos() {
        synchronized (folders) {
            FolderInfo[] fis = new FolderInfo[folders.size()];
            return folders.keySet().toArray(fis);
        }
    }

    /**
     * Returns the folderdetails for a folder. Returns a new folderdetails for a
     * folder, if the folder is joined
     * 
     * @param foInfo
     * @return
     */
    public FolderDetails getFolderDetails(FolderInfo foInfo) {
        Folder folder = getFolder(foInfo);
        if (folder != null) {
            return new FolderDetails(folder);
        }
        return networkFolders.get(foInfo);
    }

    /**
     * Answers if we already have folder details about that folder
     * 
     * @param foInfo
     * @return
     */
    public boolean hasFolderDetails(FolderInfo foInfo) {
        return networkFolders.containsKey(foInfo);
    }

    /**
     * Answers the number of know network folder (not joind)
     * 
     * @return
     */
    public int getNumberOfNetworkFolder() {
        int netSize, foldersSize;
        synchronized (networkFolders) {
            netSize = networkFolders.size();
        }
        synchronized (folders) {
            foldersSize = folders.size();
        }
        return Math.max(netSize - foldersSize, 0);
    }

    /**
     * Returns a list of all know folders on the network.
     * 
     * @return
     */
    public FolderDetails[] getNetworkFolders() {
        FolderDetails[] netList;
        synchronized (networkFolders) {
            netList = new FolderDetails[networkFolders.size()];
            networkFolders.values().toArray(netList);
        }
        return netList;
    }

    /**
     * Returns a list of all know folders on the network as fresh list.
     * 
     * @return
     */
    public List<FolderDetails> getNetworkFoldersAsList() {
        synchronized (networkFolders) {
            return new ArrayList<FolderDetails>(networkFolders.values());
        }
    }

    /**
     * Creates a folder from a folder info object and sets the sync profile.
     * <p>
     * Also stores a invitation file for the folder in the local directory if
     * wanted.
     * 
     * @param foInfo
     *            the folder info object
     * @param localDir
     *            the local base directory
     * @param profile
     *            the profile for the folder
     * @param saveInvitation
     *            if a invitation file for the folder should be placed in the
     *            local dir
     * @return the freshly created folder
     * @throws FolderException
     *             if something went wrong
     */
    public Folder createFolder(FolderInfo foInfo, File localDir,
        SyncProfile profile, boolean saveInvitation) throws FolderException
    {
        Reject.ifNull(foInfo, "FolderInfo is null");
        if (hasJoinedFolder(foInfo)) {
            throw new FolderException(foInfo, "Already joined folder");
        }

        foInfo.name = StringUtils.replace(foInfo.name, ".", "_");
        if (profile == null) {
            // Use default syncprofile
            profile = SyncProfile.MANUAL_DOWNLOAD;
        }

        Folder folder = new Folder(getController(), foInfo, localDir, profile);
        folders.put(folder.getInfo(), folder);

        // store folder in config
        Properties config = getController().getConfig();
        config.setProperty("folder." + foInfo.name + ".id", foInfo.id);
        config.setProperty("folder." + foInfo.name + ".dir", localDir
            .getAbsolutePath());
        config.setProperty("folder." + foInfo.name + ".secret", ""
            + foInfo.secret);
        config.put("folder." + foInfo.name + ".syncprofile", profile.getId());
        getController().saveConfig();

        if (saveInvitation) {
            Invitation inv = folder.getInvitation();
            InvitationUtil.save(inv, new File(localDir, Util
                .removeInvalidFilenameChars(inv.folder.name)
                + ".invitation"));
        }

        log().debug("Created " + folder);
        // Synchroniur folder memberships
        synchronizeAllFolderMemberships();

        // Trigger scan
        getController().getFolderRepository().triggerMaintenance();

        // Trigger file requestor
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Fire event
        fireFolderCreated(folder);

        log().info(
            "Joined folder " + foInfo.name + ", local copy at '" + localDir
                + "'");

        // Now remove unjoined folder
        removeUnjoinedFolder(foInfo);

        return folder;
    }

    /**
     * Returns the list of all known unjoined folder
     * 
     * @return
     */
    public List<FolderInfo> getUnjoinedFoldersList() {
        List<FolderInfo> unjoinedList;
        synchronized (networkFolders) {
            unjoinedList = new ArrayList<FolderInfo>(networkFolders.keySet());
        }

        List folderList;
        synchronized (folders) {
            folderList = new ArrayList<FolderInfo>(folders.keySet());
        }
        // Remove joined folders
        unjoinedList.removeAll(folderList);
        return unjoinedList;
    }

    /**
     * Adds a folder to the unjoined list
     * 
     * @param foDetails
     * @return
     */
    public boolean addUnjoinedFolder(FolderDetails foDetails) {
        if (foDetails == null || hasJoinedFolder(foDetails.getFolderInfo())
            || foDetails.getFolderInfo().secret)
        {
            return false;
        }
        fireUnjoinedFolderAdded(foDetails.getFolderInfo());
        return true;
    }

    /**
     * Adds a non local folder. NOT stores SECRET folders !!
     * 
     * @param foInfo
     * @return true if this folder is new
     */
    public boolean addUnjoinedFolder(FolderInfo foInfo, Member member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        if (foInfo == null) {
            throw new NullPointerException("FolderInfo is null");
        }
        FolderDetails foDetails = new FolderDetails(foInfo);
        foDetails.addMember(member.getInfo());
        return addUnjoinedFolder(foDetails);
    }

    /**
     * Removes the folder for the list of unjoined folders. Fires evet
     * 
     * @param foInfo
     */
    public void removeUnjoinedFolder(FolderInfo foInfo) {
        log().verbose("Unjoined folder removed: " + foInfo);
        // Fire event
        fireUnjoinedFolderRemoved(foInfo);
    }

    /**
     * Returns a source where the folder is available.
     * 
     * @see Member#getLastFileList(FolderInfo)
     * @param folder
     * @param onlyWithFileList
     *            true: only source with a recieved filelist will be returned
     * @return
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

        // Add folder only if ppl are in it and not secret
        if (folder.getMembersCount() > 1 && !folder.isSecret()) {
            log().warn("Adding folder to unjoined: " + folder);
            FolderDetails foDetails = new FolderDetails(folder);
            foDetails.removeMember(getController().getMySelf().getInfo());
            addUnjoinedFolder(foDetails);
        }

        // Fire event
        fireFolderRemoved(folder);
    }

    /**
     * Removes a member from all Groups. Does NOT update the UI, may cause
     * deadlock
     * 
     * @param member
     */
    public void removeFromAllFolders(Member member) {
        log().warn("Removing node from all folders: " + member);
        FolderInfo[] myJoinedFolders = getJoinedFolderInfos();
        for (int i = 0; i < myJoinedFolders.length; i++) {
            Folder folder = getFolder(myJoinedFolders[i]);
            if (folder != null) {
                folder.remove(member);
            }
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
     * Triggers the folder scan immedeately
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
                fireScansStarted();

                for (Iterator it = scanningFolders.iterator(); it.hasNext();) {
                    Folder folder = (Folder) it.next();
                    folder.maintain();

                    if (myThread.isInterrupted()) {
                        break;
                    }
                }
                log().debug(
                    "Maintained " + scanningFolders.size() + " folder(s)");

                // Fire event
                fireScansFinished();
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
        InvitationReceivedEvent event = new InvitationReceivedEvent(this,
            invitation, processSilently, forcePopup);
        invitationReceivedHandler.invitationReceived(event);
    }

    // Logic for handling folders in network **********************************

    /**
     * Requests the network folder list from all supernodes if required
     */
    public void requestNetworkFolderListIfRequired() {
        if (lastNetworkFolderListRequest != null
            && lastNetworkFolderListRequest.getTime()
                + Constants.NETWORK_FOLDER_LIST_REQUEST_DELAY * 1000 > System
                .currentTimeMillis())
        {
            log().warn("Not requesting new network folder list");
            return;
        }

        requestNetworkFolderList();
    }

    /**
     * Requests the network folder list from all supernodes
     */
    private void requestNetworkFolderList() {
        log().debug("Requesting network folder list");
        int nRequests = getController().getNodeManager()
            .broadcastMessageToSupernodes(
                RequestNetworkFolderList.COMPLETE_LIST,
                Constants.N_SUPERNODES_TO_CONTACT_FOR_NETWORK_FOLDER_LIST);
        if (nRequests > 0) {
            lastNetworkFolderListRequest = new Date();
        }
    }

    /**
     * Callback method from Member.
     * <p>
     * Enques a network folder list and add that information later to internal
     * db
     * 
     * @param source
     * @param netFolders
     */
    public void receivedNetworkFolderList(Member source,
        NetworkFolderList netFolders)
    {
        if (netFolders.isEmpty()) {
            // Ingnore
            return;
        }

        log().debug(
            "Received network folder list with "
                + netFolders.folderDetails.length + " folders from "
                + source.getNick());

        synchronized (receivedNetworkFolderLists) {
            receivedNetworkFolderLists.add(netFolders);
            // Notify processor
            receivedNetworkFolderLists.notifyAll();
        }
    }

    /**
     * Processes a network folder list
     * <p>
     * Processes a network folder list and add that information to internal db
     * 
     * @param netFolders
     */
    private void processNetworkFolderList(NetworkFolderList netFolders) {
        List<FolderDetails> newFolders = new ArrayList<FolderDetails>();

        // Update internal network folder database
        synchronized (networkFolders) {
            for (int i = 0; i < netFolders.folderDetails.length; i++) {
                FolderDetails remoteDetails = netFolders.folderDetails[i];
                FolderDetails localDetails = networkFolders.get(remoteDetails
                    .getFolderInfo());

                if (!remoteDetails.isValid()) {
                    // Skip if remote details are not valid anymore
                    continue;
                }

                boolean changed = false;

                if (localDetails == null) {
                    localDetails = new FolderDetails(remoteDetails
                        .getFolderInfo());
                    // We do not have that folder details, add it
                    networkFolders.put(localDetails.getFolderInfo(),
                        remoteDetails);
                    newFolders.add(remoteDetails);
                    changed = true;
                } else {
                    // Merge our details with remote one
                    changed = localDetails.merge(remoteDetails);
                }

                // Connect to new users
                if (hasJoinedFolder(localDetails.getFolderInfo()) && changed
                    && localDetails.isSomeoneOnline(getController()))
                {
                    log().debug(
                        "Triggering connecto to members of joined folder "
                            + localDetails.getFolderInfo().name);
                    localDetails.connectToMembers(getController(), false);
                }
            }
        }

        // Remove inactive folders
        cleanupNetworkFoldersIfNessesary();

        // Add new folders to unjoined list
        for (Iterator it = newFolders.iterator(); it.hasNext();) {
            FolderDetails foDetails = (FolderDetails) it.next();
            addUnjoinedFolder(foDetails);
        }
        int size;
        synchronized (networkFolders) {
            size = networkFolders.size();
        }
        if (logVerbose) {
            log().verbose(size + " Folders now in the network");
        }
    }

    /**
     * Callback method from Member.
     * <p>
     * Processes a new received folder list. basically updates internal database
     * 
     * @param filelist
     */
    public void receivedFolderList(Member source, FolderList folderList) {
        if (folderList.isEmpty()) {
            // Ignore
            return;
        }
        if (logVerbose) {
            log().verbose(
                "Processing new folderlist from " + source.getNick()
                    + ", he has joined " + folderList.folders.length
                    + " public folder");
        }
        // Proceess his folder list
        Set<FolderInfo> remoteFolders = new HashSet<FolderInfo>(Arrays
            .asList(folderList.folders));
        MemberInfo sourceInfo = source.getInfo();
        Set removedUnjoinedFolders = new HashSet();

        synchronized (networkFolders) {
            for (Iterator it = networkFolders.values().iterator(); it.hasNext();)
            {
                FolderDetails localDetails = (FolderDetails) it.next();
                if (remoteFolders.contains(localDetails.getFolderInfo())) {
                    if (!localDetails.hasMember(sourceInfo)) {
                        localDetails.addMember(sourceInfo);
                    }
                    // Folder found in local db, remove from list
                    remoteFolders.remove(localDetails.getFolderInfo());
                } else {
                    // Remove from folder
                    localDetails.removeMember(sourceInfo);
                }
            }

            // Add unkown/new folders
            for (Iterator it = remoteFolders.iterator(); it.hasNext();) {
                FolderInfo foInfo = (FolderInfo) it.next();
                FolderDetails foDetails = new FolderDetails(foInfo);
                foDetails.addMember(sourceInfo);
                networkFolders.put(foInfo, foDetails);
            }
        }

        // Remove not longer used unjoined folders
        for (Iterator it = removedUnjoinedFolders.iterator(); it.hasNext();) {
            FolderDetails foDetails = (FolderDetails) it.next();
            fireUnjoinedFolderRemoved(foDetails.getFolderInfo());
        }

        // Cleanup folders
        cleanupNetworkFoldersIfNessesary();

        // Send source folders details which are intersting for him
        if (getController().getMySelf().isSupernode() || source.isFriend()) {
            sendPreparedNetworkFolderList(source, folderList);
        }
        int size;
        synchronized (networkFolders) {
            size = networkFolders.size();
        }
        if (logVerbose) {
            log().verbose(size + " Folders now in the network");
        }

    }

    /**
     * Cleans up the network folder list if nessesary
     */
    private void cleanupNetworkFoldersIfNessesary() {
        long time2Wait = 1000 * 60 * 5;
        boolean nessesary = lastNetworkFolderListCleanup == null
            || (lastNetworkFolderListCleanup.getTime() < System
                .currentTimeMillis()
                - time2Wait);
        if (nessesary) {
            // Cleanup
            cleanupNetworkFolder();
        } else {
            if (logVerbose) {
                log().verbose("Omitting cleanup of network folder list");
            }
        }
    }

    /**
     * Removes all inactive/unuseful network folders
     */
    private void cleanupNetworkFolder() {
        List<FolderDetails> removedFolders = new ArrayList<FolderDetails>();

        synchronized (networkFolders) {
            for (Iterator it = networkFolders.values().iterator(); it.hasNext();)
            {
                FolderDetails foDetail = (FolderDetails) it.next();
                if (!foDetail.isValid()) {
                    // Remove an inactive folder
                    it.remove();
                    removedFolders.add(foDetail);
                }
            }
        }

        // Tell removed folders
        for (Iterator it = removedFolders.iterator(); it.hasNext();) {
            FolderDetails removedFolder = (FolderDetails) it.next();
            removeUnjoinedFolder(removedFolder.getFolderInfo());
        }

        lastNetworkFolderListCleanup = new Date();

        log().debug(
            "Cleanup result: Removed " + removedFolders.size()
                + " network folder");
    }

    /**
     * Sends a specially prepared network folder list to the node
     * 
     * @param node
     * @param folderList
     */
    private void sendPreparedNetworkFolderList(Member node,
        FolderList folderList)
    {
        if (folderList.isEmpty()) {
            // Not send any list
            return;
        }
        // Create network folder list
        Message[] netLists = NetworkFolderList.createNetworkFolderLists(this,
            folderList.folders);

        log().debug(
            "Sending " + netLists.length + " NetworkFolder lists to "
                + node.getNick());

        // Send intersting folders to him
        node.sendMessagesAsynchron(netLists);
    }

    // Network folder list processor ******************************************

    /**
     * Thread which processes the incoming network folder lists
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class NetworkFolderListProcessor extends PFComponent implements
        Runnable
    {
        private Thread procThread;

        private NetworkFolderListProcessor() {

        }

        public void start() {
            procThread = new Thread(this, "Network folder list processor");
            procThread.setPriority(Thread.MIN_PRIORITY);
            procThread.start();
            log().debug("Started");

        }

        public void shutdown() {
            if (procThread != null) {
                procThread.interrupt();
            }
            log().debug("Stopped");
        }

        public void run() {
            while (started) {
                synchronized (receivedNetworkFolderLists) {
                    try {
                        receivedNetworkFolderLists.wait();
                    } catch (InterruptedException e) {
                        log().verbose("Stopping network folder list processor",
                            e);
                        break;
                    }
                }
                if (logVerbose) {
                    log().verbose("Network folder list processor triggerd");
                }
                try {
                    // Wait a bit to avoid spamming
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log().verbose("Stopping network folder list processor", e);
                    break;
                }
                while (!receivedNetworkFolderLists.isEmpty()) {
                    NetworkFolderList netList = receivedNetworkFolderLists
                        .remove(0);
                    processNetworkFolderList(netList);
                }
            }
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

    private void fireUnjoinedFolderAdded(FolderInfo info) {
        listenerSupport.unjoinedFolderAdded(new FolderRepositoryEvent(this,
            info));
    }

    private void fireUnjoinedFolderRemoved(FolderInfo info) {
        listenerSupport.unjoinedFolderRemoved(new FolderRepositoryEvent(this,
            info));
    }

    private void fireScansStarted() {
        listenerSupport.scansStarted(new FolderRepositoryEvent(this));
    }

    private void fireScansFinished() {
        listenerSupport.scansFinished(new FolderRepositoryEvent(this));
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