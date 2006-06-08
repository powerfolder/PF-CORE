/* $Id: FolderRepository.java,v 1.75 2006/04/23 17:09:00 totmacherr Exp $
 */
package de.dal33t.powerfolder.disk;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.*;
import de.dal33t.powerfolder.transfer.FileRequestor;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.FolderJoinPanel;
import de.dal33t.powerfolder.util.*;
import de.dal33t.powerfolder.util.ui.NeverAskAgainOkCancelDialog;
import de.dal33t.powerfolder.util.ui.TreeNodeList;

/**
 * Repository of all local power folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.75 $
 */
public class FolderRepository extends PFComponent implements Runnable {
    private final static String warnOnClosePropertyName = "folderrepository.warnonclose";

    private Map<FolderInfo, Folder> folders;
    private Thread myThread;
    private FileRequestor fileRequestor;
    /** The shares which may contain additional files */
    private Shares shares;
    // Flag if the repo is already started
    private boolean started;
    // The trigger to start scanning
    private Object scanTrigger = new Object();

    // private FolderRepositoryListener listenerSupport;
    List<FolderRepositoryListener> listeners;

    /** The date of the last request for network folder list */
    private Date lastNetworkFolderListRequest;
    /** The date when the list cleanup of the network folder list was made */
    private Date lastNetworkFolderListCleanup;
    /** model for unjoined folders. contains FolderInfo -> FolderDetails */
    private Map<FolderInfo, FolderDetails> networkFolders;
    /**
     * The received network folder lists. lists will be processed from time to
     * time
     */
    private List<NetworkFolderList> receivedNetworkFolderLists;
    /**
     * Processor, which processes the received network folder lists from time to
     * time
     */
    private NetworkFolderListProcessor netListProcessor;

    // a list containing all joined folder
    private TreeNodeList joinedFolders;

    public FolderRepository(Controller controller) {
        super(controller);

        // UI Code
        TreeNode rootNode = controller.isUIOpen() ? controller
            .getUIController().getControlQuarter().getNavigationTreeModel()
            .getRootNode() : null;
        this.joinedFolders = new TreeNodeList("JOINED_FOLDERS", rootNode);
        this.networkFolders = Collections
            .synchronizedMap(new HashMap<FolderInfo, FolderDetails>());
        this.receivedNetworkFolderLists = Collections
            .synchronizedList(new ArrayList<NetworkFolderList>());

        // Rest
        this.joinedFolders.sortBy(new FolderComparator());
        this.folders = Collections.synchronizedMap(new HashMap());
        this.fileRequestor = new FileRequestor(controller);
        this.shares = new Shares(controller);
        this.netListProcessor = new NetworkFolderListProcessor();
        this.started = false;

        // Create listener support
        // this.listenerSupport = (FolderRepositoryListener)
        // ListenerSupportFactory
        // .createListenerSupport(FolderRepositoryListener.class);
        listeners = Collections
            .synchronizedList(new ArrayList<FolderRepositoryListener>());
    }

    private boolean warnOnClose() {
        Properties config = getController().getConfig();
        boolean warnOnClose = false;
        if (config.containsKey(warnOnClosePropertyName)) {
            warnOnClose = config.getProperty(warnOnClosePropertyName)
                .toString().equalsIgnoreCase("true");
        } else {// this is a new prop assume we have to ask this
            config.put(warnOnClosePropertyName, "true");
            warnOnClose = true;
        }
        return warnOnClose;
    }

    /** for debug * */
    public void setSuspendFireEvents(boolean suspended) {
        // ListenerSupportFactory.setSuspended(listenerSupport, suspended);
        // log().debug("setSuspendFireEvents: " + suspended);
        log().error("setSuspendFireEvents Not Implemented ");
    }

    /**
     * Answers if any folder is currently synching
     * 
     * @return
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
        if (warnOnClose()) {
            List<Folder> foldersToWarn = new ArrayList(getFolders().length);
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
                if (Util.isAWTAvailable() && !getController().isConsoleMode()) {
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
                            Properties config = getController().getConfig();
                            config.put(warnOnClosePropertyName, "false");
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
        List errorFolderNames = new LinkedList();
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

                    try {
                        // do not add if already added
                        if (!hasJoinedFolder(foInfo) && folderId != null
                            && folderDir != null)
                        {
                            createFolder(foInfo, new File(folderDir));
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
        // Remove listners, not bothering them by boring shutdown events
        // ListenerSupportFactory.removeAllListeners(listenerSupport);
        // ListenerSupportFactory.setSuspended(listenerSupport, true);
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
        String folderbase = getController().getConfig().getProperty(
            "foldersbase");
        if (StringUtils.isBlank(folderbase)) {
            folderbase = System.getProperty("user.home")
                + System.getProperty("file.separator") + "PowerFolders";
        }
        return folderbase;
    }

    /**
     * Returns the additonal shares
     * 
     * @return
     */
    public Shares getShares() {
        return shares;
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
            List<Folder> foldersList = new ArrayList(folders.values());
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
        synchronized(networkFolders) {
            netSize = networkFolders.size();
        }
        synchronized(folders) {
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
     * Creates a folder from a folder info object and sets the sync profile.
     * <p>
     * Also stores a invitation file for the folder in the local directory if
     * wanted.
     * 
     * @param fInfo
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
    public Folder createFolder(FolderInfo fInfo, File localDir,
        SyncProfile profile, boolean saveInvitation) throws FolderException
    {
        Reject.ifNull(profile, "Sync profile is null");
        Folder folder = createFolder(fInfo, localDir);
        folder.setSyncProfile(profile);
        if (saveInvitation) {
            Invitation inv = folder.getInvitation();
            Util.saveInvitation(inv, new File(localDir, Util
                .removeInvalidFilenameChars(inv.folder.name)
                + ".invitation"));
        }

        return folder;
    }

    /**
     * Creates a folder from a folder info object.
     * <p>
     * Does not store a invitation file in the local base directory.
     * <p>
     * Tries to restore the syncprofile from configuration entry. If this could
     * not be found the default syncprofile is assumed
     * 
     * @param fInfo
     *            the folder info object
     * @param localDir
     *            the local directory to store the folder in
     * @return the create folder
     * @throws FolderException
     *             if something went wrong
     */
    public Folder createFolder(FolderInfo fInfo, File localDir)
        throws FolderException
    {

        if (fInfo == null) {
            throw new NullPointerException("FolderInfo is null");
        }

        if (hasJoinedFolder(fInfo)) {
            throw new FolderException(fInfo, "Already joined folder");
        }

        fInfo.name = StringUtils.replace(fInfo.name, ".", "_");

        Folder folder = new Folder(getController(), fInfo, localDir);
        folders.put(folder.getInfo(), folder);

        // store folder in config
        Properties config = getController().getConfig();
        config.setProperty("folder." + fInfo.name + ".id", fInfo.id);
        config.setProperty("folder." + fInfo.name + ".dir", localDir
            .getAbsolutePath());
        config.setProperty("folder." + fInfo.name + ".secret", ""
            + fInfo.secret);
        getController().saveConfig();

        joinedFolders.addChild(folder.getTreeNode());

        log().debug("Created " + folder);
        // Synchroniur folder memberships
        synchronizeAllFolderMemberships();

        // Trigger file requestor
        getController().getFolderRepository().getFileRequestor()
            .triggerFileRequesting();

        // Fire event
        fireFolderCreated(folder);

        log().info(
            "Joined folder " + fInfo.name + ", local copy at '" + localDir
                + "'");

        // Now remove unjoined folder
        removeUnjoinedFolder(fInfo);

        return folder;
    }

    /**
     * Returns the list of all known unjoined folder
     * 
     * @return
     */
    public List getUnjoinedFoldersList() {
        List unjoinedList ;
        synchronized (networkFolders) {             
            unjoinedList = new ArrayList(networkFolders.keySet());
        }
        
        List folderList;
        synchronized (folders) {
            folderList = new ArrayList(folders.keySet());
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
        Member[] nodes = getController().getNodeManager().getNodes();
        long maxFolderSize = 0;
        Member bestSource = null;
        for (int i = 0; i < nodes.length; i++) {
            Member node = nodes[i];

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

        // UI Code
        // ui tree node
        joinedFolders.remove(folder.getTreeNode());

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
        log().verbose("All Nodes: Synchronize Foldermemberships");
        Member[] nodes = getController().getNodeManager().getNodes();
        FolderInfo[] myJoinedFolders = getJoinedFolderInfos();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].synchronizeFolderMemberships(myJoinedFolders);
        }
    }

    /**
     * Triggers the folder scan immedeately
     */
    public void triggerScan() {
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
            log().debug("Scanning folders...");

            // Only scan if not in silent mode
            if (!getController().isSilentMode()) {
                List scanningFolders = new ArrayList(folders.values());
                // TODO: Sort by size, to have the small ones fast
                // Collections.sort(scanningFolders);

                int scanned = 0;
                // Fire event
                fireScansStarted();

                for (Iterator it = scanningFolders.iterator(); it.hasNext();) {
                    Folder folder = (Folder) it.next();
                    if (folder.scan()) {
                        log().debug("Scanned " + folder.getName());
                        scanned++;
                    }
                    if (myThread.isInterrupted()) {
                        break;
                    }
                }
                log().debug("Scanned " + scanned + " folder(s)");
                if (scanned > 0) {
                    log().info("Foldersscan completed");
                }
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

    /**
     * Processes a invitation to a folder TODO: Autojoin invitation, make this
     * configurable in pref screen
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
        if (invitation == null || invitation.folder == null) {
            throw new NullPointerException("Invitation/Folder is null");
        }
        if (getController().isUIOpen()) {
            Runnable worker = new Runnable() {
                public void run() {
                    // Check if already on folder
                    if (hasJoinedFolder(invitation.folder)) {
                        // Already on folder, show message if not processing
                        // silently
                        if (!processSilently) {
                            // Popup application
                            getController().getUIController().getMainFrame()
                                .getUIComponent().setVisible(true);
                            getController().getUIController().getMainFrame()
                                .getUIComponent()
                                .setExtendedState(Frame.NORMAL);

                            getController().getUIController()
                                .showWarningMessage(
                                    Translation.getTranslation(
                                        "joinfolder.already_joined_title",
                                        invitation.folder.name),
                                    Translation.getTranslation(
                                        "joinfolder.already_joined_text",
                                        invitation.folder.name));
                        }
                        return;
                    }
                    final FolderJoinPanel panel = new FolderJoinPanel(
                        getController(), invitation, invitation.invitor);
                    final JFrame jFrame = getController().getUIController()
                        .getMainFrame().getUIComponent();
                    if (forcePopup
                        || !(Util.isSystraySupported() && !jFrame.isVisible()))
                    {
                        // Popup whole application
                        getController().getUIController().getMainFrame()
                            .getUIComponent().setVisible(true);
                        getController().getUIController().getMainFrame()
                            .getUIComponent().setExtendedState(Frame.NORMAL);
                        open(panel);
                    } else {
                        // Only show systray blinking
                        getController().getUIController().getBlinkManager()
                            .setBlinkingTrayIcon(Icons.ST_INVITATION);
                        jFrame.addWindowFocusListener(new WindowFocusListener()
                        {
                            public void windowGainedFocus(WindowEvent e) {
                                jFrame.removeWindowFocusListener(this);
                                open(panel);
                            }

                            public void windowLostFocus(WindowEvent e) {
                            }
                        });
                    }
                }

                private void open(FolderJoinPanel panel) {
                    // Turn off blinking tray icon
                    getController().getUIController().getBlinkManager()
                        .setBlinkingTrayIcon(null);
                    panel.open();
                    // Adding invitor to friends
                    if (panel.addInvitorToFriendsRequested()) {
                        Member node = invitation.invitor.getNode(
                            getController(), true);
                        // Set friend state
                        node.setFriend(true);
                    }

                    // Add folder to unjoin folder list if
                    // not joined and not secret
                    if (!hasJoinedFolder(invitation.folder)) {
                        addUnjoinedFolder(invitation.folder, invitation.invitor
                            .getNode(getController()));
                    }
                }
            };

            // Invoke later
            SwingUtilities.invokeLater(worker);
        } else {
            // Ui not open
            // FIXME this is a ugly hack (tm)
            if ("true".equalsIgnoreCase(System.getProperty("powerfolder.test")))
            {
                // if in test mode
                File dir = new File(getController().getFolderRepository()
                    .getFoldersBasedir()
                    + System.getProperty("file.separator")
                    + Util.removeInvalidFilenameChars(invitation.folder.name));
                try {
                    getController().getFolderRepository().createFolder(
                        invitation.folder, dir);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "-----------test failed ------------");
                }
            }
        }
    }

    /**
     * Processes a received backup request
     * 
     * @param from
     * @param backupRequest
     */
    public boolean backupRequestReceived(Member from,
        RequestBackup backupRequest)
    {
        if (!getController().isBackupServer()) {
            log().warn(
                "Not backup server. Backup request from " + from + ". "
                    + backupRequest);
        }
        if (!from.isFriend()) {
            // Only accept from friends
            return false;
        }
        if (hasJoinedFolder(backupRequest.folder)) {
            return false;
        }
        if (!backupRequest.folder.secret) {
            // Allow backup on secret folder only
            return false;
        }
        log().warn("Creating backup of " + backupRequest.folder);
        File baseDir = new File(getFoldersBasedir() + "/"
            + backupRequest.folder.name);
        try {
            // TODO: Add a special sync profile for backup server
            Folder folder = createFolder(backupRequest.folder, baseDir,
                SyncProfile.AUTO_DOWNLOAD_FROM_ALL, false);
            // Join requesting node into folder
            folder.join(from);
        } catch (FolderException e) {
            log().error(
                "Unable to create Backup of " + backupRequest.folder.name, e);
            return false;
        }
        return true;
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
        List newFolders = new ArrayList();

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
        synchronized(networkFolders) {
            size = networkFolders.size();
        }
        log().verbose(size + " Folders now in the network");
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

        log().verbose(
            "Processing new folderlist from " + source.getNick()
                + ", he has joined " + folderList.folders.length
                + " public folder");

        // Proceess his folder list
        Set remoteFolders = new HashSet(Arrays.asList(folderList.folders));
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
        synchronized(networkFolders) {
            size = networkFolders.size();
        }
        log().verbose(size + " Folders now in the network");
        
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
            log().verbose("Omitting cleanup of network folder list");
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
        NetworkFolderList netList = new NetworkFolderList(this, folderList);

        if (netList.isEmpty()) {
            // We have no info for him
            return;
        }
        log().debug(
            "Sending " + netList.folderDetails.length + " FolderDetails to "
                + node.getNick());
        // Send intersting folders to him
        node
            .sendMessageAsynchron(netList, "Unable to send network folder list");
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
                log().verbose("Network folder list processor triggerd");
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

    /*
     * Swing UI methods *******************************************************
     */

    /**
     * Returns the treenode for all joined folders
     * 
     * @return
     */
    public TreeNodeList getJoinedFoldersTreeNode() {
        return joinedFolders;
    }

    // new event support

    private void fireFolderCreated(Folder folder) {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this, folder);
        // Call to listener support
        // listenerSupport.folderCreated(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.folderCreated(e);
            }
        }
    }

    private void fireFolderRemoved(Folder folder) {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this, folder);
        // Call to listener support
        // listenerSupport.folderRemoved(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.folderRemoved(e);
            }
        }
    }

    private void fireUnjoinedFolderAdded(FolderInfo info) {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this, info);
        // Call to listener support
        // listenerSupport.unjoinedFolderAdded(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.unjoinedFolderAdded(e);
            }
        }
    }

    private void fireUnjoinedFolderRemoved(FolderInfo info) {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this, info);
        // Call to listener support
        // listenerSupport.unjoinedFolderRemoved(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.unjoinedFolderRemoved(e);
            }
        }
    }

    private void fireScansStarted() {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this);
        // Call to listener support
        // listenerSupport.scansStarted(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.scansStarted(e);
            }
        }
    }

    private void fireScansFinished() {
        final FolderRepositoryEvent e = new FolderRepositoryEvent(this);
        // Call to listener support
        // listenerSupport.scansFinished(e);
        synchronized (listeners) {
            for (FolderRepositoryListener listener : listeners) {
                listener.scansFinished(e);
            }
        }
    }

    public void addFolderRepositoryListener(FolderRepositoryListener listener) {
        // ListenerSupportFactory.addListener(listenerSupport, listener);
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeFolderRepositoryListener(FolderRepositoryListener listener)
    {
        // ListenerSupportFactory.removeListener(listenerSupport, listener);
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}