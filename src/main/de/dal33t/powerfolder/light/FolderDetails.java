/* $Id: FolderDetails.java,v 1.9 2005/11/20 03:13:44 totmacherr Exp $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.util.*;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.MessageListener;
import de.dal33t.powerfolder.message.RequestFileList;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.MemberComparator;
import de.dal33t.powerfolder.util.Util;

/**
 * A Lightweight object for holding information about a folder. Currently only
 * useable for public folders.
 * <p>
 * TODO: Make this a real class with private fields
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderDetails extends Loggable implements Serializable {
    private static final long serialVersionUID = 100L;
    /**
     * Time to cache number of online node
     */
    private static final long ONLINE_MEMBER_COUNT_CACHE_TIME = 5000;

    private FolderInfo folderInfo;
    private MemberInfo[] members;
    private Date lastModified;

    /**
     * The time, when the online members where counted the last time
     */
    private transient long lastTimeOnlineMemberCounted;
    /**
     * The number of members online on this folder (cache)
     */
    private transient int nMembersOnline;

    /**
     * Initalizes a new folder details from a given folder
     */
    public FolderDetails(FolderInfo folder) {
        super();
        folderInfo = (FolderInfo) folder.clone();
        lastModified = new Date();
    }

    /**
     * Initalizes a new folder details from a given folder
     */
    public FolderDetails(Folder folder) {
        this.folderInfo = (FolderInfo) folder.getInfo().clone();
        this.members = Util.asMemberInfos(folder.getMembers());
    }

    // Logic ******************************************************************

    public FolderInfo getFolderInfo() {
        return folderInfo;
    }

    /**
     * Returns the members of this folder
     * 
     * @return
     */
    public MemberInfo[] getMembers() {
        return members;
    }

    /**
     * Returns the last modififaction date of this folder
     * 
     * @return
     */
    public Date getLastModifiedDate() {
        return lastModified;
    }

    /**
     * Returns the number of members of this folder
     * 
     * @return
     */
    public int memberCount() {
        return members == null ? 0 : members.length;
    }

    /**
     * Answers if the folder contains this member
     * 
     * @param member
     * @return
     */
    public boolean hasMember(MemberInfo member) {
        if (members == null) {
            return false;
        }
        return Arrays.asList(members).contains(member);
    }

    /**
     * Adds a member to this folder details
     * 
     * @param member
     */
    public synchronized void addMember(MemberInfo member) {
        if (member == null) {
            throw new NullPointerException("Member is null");
        }
        if (members == null) {
            // Initalize
            members = new MemberInfo[1];
            // lastMemberActivity = new Date[1];
        } else {
            // Increase array
            MemberInfo[] newMembers = new MemberInfo[members.length + 1];
            System.arraycopy(members, 0, newMembers, 0, members.length);
            members = newMembers;
        }

        // Add member
        members[members.length - 1] = member;
        lastModified = new Date();
        fileListAvailable = null;
    }

    /**
     * Removes a member if not found
     * 
     * @param member
     */
    public synchronized void removeMember(MemberInfo member) {
        if (true) {
            // FIXME: Make it working
            return;
        }
        if (members == null || members.length == 0) {
            return;
        }
        int index = Arrays.asList(members).indexOf(member);
        if (index < 0) {
            return;
        }
        if (members.length - 1 == 0) {
            // Empty now!
            members = null;
            return;
        }
        // Remove the one
        MemberInfo[] newMembers = new MemberInfo[members.length - 1];
        // Copy members before
        if (index > 0) {
            System.arraycopy(member, 0, newMembers, 0, index);
        }
        // Copy members after index
        if (index <= newMembers.length) {
            System.arraycopy(member, index + 1, newMembers, index,
                newMembers.length - index);
        }

        members = newMembers;
    }

    /**
     * Merges the information from another details object into this one
     * 
     * @return if there where new informations added from the other
     *         folderdetails
     * @param details
     */
    public synchronized boolean merge(FolderDetails details) {
        if (details == null) {
            throw new NullPointerException("Details is null");
        }
        if (!this.folderInfo.equals(details.folderInfo)) {
            throw new IllegalArgumentException("Merge mismatch. this folder '"
                + folderInfo + "', merge folder '" + details.folderInfo + "'");
        }
        if (details.members == null || details.members.length <= 0) {
            // Do nothing
            return false;
        }

        boolean changed = false;

        // TODO: Check for lastmodified of remote details
        if (details.folderInfo.bytesTotal > this.folderInfo.bytesTotal) {
            // Replace folderinfo
            this.folderInfo = details.folderInfo;
            changed = true;
        }

        for (int i = 0; i < details.members.length; i++) {
            MemberInfo node = details.members[i];
            if (node != null && !hasMember(node)) {
                // Add node
                addMember(node);
                changed = true;
            }
        }

        // Reset cached availibility status
        if (changed) {
            fileListAvailable = null;
        }

        return changed;
    }

    /**
     * Answers if this folder is valid.
     * 
     * @return
     */
    public boolean isValid() {
        if (lastModified == null) {
            return false;
        }
        // FIXME: Synchronize?
        for (int i = 0; i < members.length; i++) {
            if (members[i] == null) {
                return false;
            }
        }
        return hasMember()
            && lastModified.getTime() > System.currentTimeMillis()
                - Constants.MAX_TIME_OF_FOLDER_INACTIVITY_UNTIL_REMOVED * 1000;
    }

    /**
     * Answers if the folder has members
     * 
     * @return
     */
    public boolean hasMember() {
        return memberCount() > 0;
    }

    /**
     * Asks if someone of this folder is online atm
     * 
     * @param controller
     * @return
     */
    public boolean isSomeoneOnline(Controller controller) {
        return countOnlineMembers(controller) > 0;
    }

    /**
     * Returns the number of potential online users
     * 
     * @param controller
     * @return
     */
    public int countOnlineMembers(Controller controller) {
        if (members== null || members.length ==0) {
            return 0;
        }
        boolean cacheInvalid = (lastTimeOnlineMemberCounted < System
            .currentTimeMillis()
            - ONLINE_MEMBER_COUNT_CACHE_TIME);
        if (!cacheInvalid) {
            return nMembersOnline;
        }
        synchronized (this) {
            nMembersOnline = 0;
            for (int i = 0; i < members.length; i++) {
                Member node = members[i].getNode(controller);
                if (node != null && node.isConnectedToNetwork()) {
                    nMembersOnline++;
                }
            }
            lastTimeOnlineMemberCounted = System.currentTimeMillis();
//            log().warn(
//                folderInfo.name + ": Calculated number of online users ("
//                    + nMembersOnline + ")");
        }

        return nMembersOnline;
    }

    // Requesting information methods *****************************************

    /**
     * Flag indicating that we have a running request
     */
    private transient boolean currentlyRequesting;
    private transient Boolean fileListAvailable;

    /**
     * Answers if there is a request for filelist is currently running on this
     * folder
     * 
     * @return
     */
    public boolean isCurrentlyRequesting() {
        return currentlyRequesting;
    }

    /**
     * Begins to request filelists for this folder. Callback informs about
     * status and incoming filelists
     * 
     * @param controller
     * @param callback
     */
    public void requestFileList(final Controller controller,
        final RequestFileListCallback callback)
    {
        if (callback == null) {
            throw new NullPointerException("Callback is null");
        }

        log().warn("Started filelist retrieval for " + folderInfo.name);

        if (connectToMembers(controller, true) <= 0) {
            // Filelist not available
            fileListAvailable = Boolean.FALSE;
            // Request over
            callback.fileListRequestOver();
            return;
        }

        // Request started
        currentlyRequesting = true;

        // Listener on nodemanager. Send requests for filelist on folder
        final NodeManagerListener nmListener = new NodeManagerListener() {
            public void nodeConnected(NodeManagerEvent e) {
                if (hasMember(e.getNode().getInfo())) {
                    log().warn(
                        "Requesting filelist for " + folderInfo.name + " from "
                            + e.getNode().getNick());
                    e.getNode().sendMessageAsynchron(
                        new RequestFileList(folderInfo), null);
                }
            }

            public void nodeRemoved(NodeManagerEvent e) {
            }

            public void nodeAdded(NodeManagerEvent e) {
            }

            public void nodeDisconnected(NodeManagerEvent e) {
            }

            public void friendAdded(NodeManagerEvent e) {
            }

            public void friendRemoved(NodeManagerEvent e) {
            }

            public void settingsChanged(NodeManagerEvent e) {
            }
            
            public boolean fireInEventDispathThread() {
                return false;
            }
        };
        controller.getNodeManager().addNodeManagerListener(nmListener);

        // Listener for filelists
        final MessageListener messageListener = new MessageListener() {
            public void handleMessage(Member source, Message message) {
                // log().warn("Got message " + message + " from " + source);
                if (message instanceof FileList) {
                    FileList fileList = (FileList) message;
                    if (!folderInfo.equals(fileList.folder)) {
                        // Does not match our folder
                        return;
                    }

                    // Yeah, filelist available
                    fileListAvailable = Boolean.TRUE;
                    // Tell callback
                    callback.fileListReceived(source, fileList);
                }

            }
        };

        controller.getNodeManager().addMessageListenerToAllNodes(
            messageListener);

        // Now request on nodes, that are already connected
        synchronized (this) {
            for (int i = 0; i < members.length; i++) {
                Member node = members[i].getNode(controller);
                if (node != null && node.isCompleteyConnected()) {
                    // Request filelist
                    node.sendMessageAsynchron(new RequestFileList(folderInfo),
                        null);
                    log().warn(
                        "Sent filelist request for " + folderInfo.name + " to "
                            + node.getNick());
                }
            }
        }

        // Request cleanup afterwards
        Thread requestCleaner = new Thread("FileList request for "
            + folderInfo.name)
        {
            public void run() {
                try {
                    Thread
                        .sleep(Constants.FOLDER_FILELIST_REQUEST_LENGTH * 1000);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    return;
                }

                log().warn("Filelist request for " + folderInfo.name + " over");
                // Request over / Cleanup
                currentlyRequesting = false;

                if (fileListAvailable == null) {
                    // Filelist not available
                    fileListAvailable = Boolean.FALSE;
                }

                controller.getNodeManager().removeMessageListener(
                    messageListener);
                controller.getNodeManager().removeNodeManagerListener(
                    nmListener);
                // Tell callback
                callback.fileListRequestOver();

                synchronized (FolderDetails.this) {
                    if (memberCount() > 0) {
                        for (int i = 0; i < members.length; i++) {
                            Member member = members[i].getNode(controller);
                            if (member != null) {
                                // Remove interesting mark
                                member.removedInterestingMark();
                            }
                        }
                    }
                }
            }
        };
        requestCleaner.setDaemon(true);
        requestCleaner.start();
    }

    /**
     * Triggers to connect to the members of that folder
     */
    public int connectToMembers(Controller controller,
        boolean markNode)
    {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        if (memberCount() <= 0) {
            return 0;
        }
        List<Member> connectMembers = new ArrayList<Member>(members.length);
        synchronized (this) {
            for (int i = 0; i < members.length; i++) {
                Member member = members[i].getNode(controller, true);

                // Mark as interesting
                connectMembers.add(member);
            }
        }

        Collections.sort(connectMembers,
            MemberComparator.BY_RECONNECTION_PRIORITY);

        for (Iterator it = connectMembers.iterator(); it.hasNext();) {
            Member member = (Member) it.next();
            if (markNode) {
                // Mark the node as interesting
                member.markAsIntersting();
            }

            // Mark for connection
            member.markForImmediateConnect();
        }

        return members.length;
    }

    // General ****************************************************************

    public int hashCode() {
        return folderInfo.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof FolderDetails) {
            return Util.equals(this.folderInfo,
                ((FolderDetails) other).folderInfo);
        }
        return false;
    }

    public String toString() {
        int nMembers = members != null ? members.length : 0;
        return "FolderDetails of '" + folderInfo.name + "' (" + nMembers
            + " members)";
    }
}