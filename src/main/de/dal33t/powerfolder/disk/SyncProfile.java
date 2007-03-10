/* $Id: SyncProfile.java,v 1.5 2005/11/04 13:59:58 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * Instance of this class describe how a folder should be synced with the remote
 * sides
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class SyncProfile implements Serializable {
    private static final long serialVersionUID = 100L;
    
    public static final SyncProfile MANUAL_DOWNLOAD = new SyncProfile(
        "manualdownload", false, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_FRIENDS = new SyncProfile(
        "autodownload_friends", true, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_ALL = new SyncProfile(
        "autodownload_all", true, true, false, false, 30);

    public static final SyncProfile SYNCHRONIZE_PCS = new SyncProfile(
        "syncpcs", true, true, true, true, 10);

    public static final SyncProfile BACKUP_SOURCE = new SyncProfile(
        "backupsource", false, false, false, false, 5);

    public static final SyncProfile BACKUP_TARGET = new SyncProfile(
        "backuptarget", true, true, true, true, 30);

    public static final SyncProfile PROJECT_WORK = new SyncProfile(
        "projectwork", false, false, false, false, -1);

    // All default sync profiles
    public static final SyncProfile[] DEFAULT_SYNC_PROFILES = new SyncProfile[]{
        SyncProfile.MANUAL_DOWNLOAD, SyncProfile.AUTO_DOWNLOAD_FROM_FRIENDS,
        SyncProfile.AUTO_DOWNLOAD_FROM_ALL, SyncProfile.SYNCHRONIZE_PCS,
        SyncProfile.BACKUP_SOURCE, SyncProfile.BACKUP_TARGET,
        SyncProfile.PROJECT_WORK};

    /** The id of this profile */
    private String id;

    private boolean autoDownloadFromFriends;
    private boolean autoDownloadFromOthers;
    private boolean syncDeletionWithFriends;
    private boolean syncDeletionWithOthers;
    private int minutesBetweenScans;

    /**
     * Constructor of sync profile. After creation remains immutable
     * 
     * @param id
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param minutesBetweenScans
     *            the minutes between auto-scans. use a negativ integer to
     *            disable auto-scans
     */
    public SyncProfile(String id, boolean autoDownloadFromFriends,
        boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
        boolean syncDeletionWithOthers,
        int minutesBetweenScans)
    {
        this.id = id;
        this.autoDownloadFromFriends = autoDownloadFromFriends;
        this.autoDownloadFromOthers = autoDownloadFromOthers;
        this.syncDeletionWithFriends = syncDeletionWithFriends;
        this.syncDeletionWithOthers = syncDeletionWithOthers;
        this.minutesBetweenScans = minutesBetweenScans;
    }

    // Getter/Setter **********************************************************

    /**
     * Returns the id for this sync profile
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the translation id for this profile
     * 
     * @return
     */
    public String getTranslationId() {
        return "syncprofile." + id + ".name";
    }

    /**
     * If folder should automatically download new files from friends
     * 
     * @return
     */
    public boolean isAutoDownloadFromFriends() {
        return autoDownloadFromFriends;
    }

    /**
     * If folder should automatically download new files from other people
     * (non-friends)
     * 
     * @return
     */
    public boolean isAutoDownloadFromOthers() {
        return autoDownloadFromOthers;
    }

    /**
     * Convinience method. Anwers if autodownload is enabled (from friends or
     * others)
     * 
     * @return
     */
    public boolean isAutodownload() {
        return isAutoDownloadFromFriends() || isAutoDownloadFromOthers();
    }
    
    /**
     * @return true if syncing deletions with any other user
     */
    public boolean isSyncDeletion() {
        return isSyncDeletionWithFriends() || isSyncDeletionWithOthers();
    }

    /**
     * Answers if the folder syncs file deltions with friends
     * 
     * @return
     */
    public boolean isSyncDeletionWithFriends() {
        return syncDeletionWithFriends;
    }

    /**
     * Answers if the folder syncs file deltions with other people (non-friends)
     * 
     * @return
     */
    public boolean isSyncDeletionWithOthers() {
        return syncDeletionWithOthers;
    }

    /**
     * If folder automatically detects changes to files on disk
     * 
     * @return
     */
    public boolean isAutoDetectLocalChanges() {
        return minutesBetweenScans > 0;
    }

    /**
     * Answers the minutes to wait between disk scans. Only relevant if
     * auto-detect changes is enabled
     * 
     * @return
     */
    public int getMinutesBetweenScans() {
        return minutesBetweenScans;
    }

    // Static accessors *******************************************************

    /**
     * Tries to resolve a sync profile by id. Returns null if nothing was found
     * 
     * @param id
     * @return
     */
    public static SyncProfile getSyncProfileById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        for (int i = 0; i < DEFAULT_SYNC_PROFILES.length; i++) {
            if (id.equals(DEFAULT_SYNC_PROFILES[i].getId())) {
                return DEFAULT_SYNC_PROFILES[i];
            }
        }
        return null;
    }

    // General ****************************************************************

    public String toString() {
        return "SyncProfile '" + id + "'";
    }
}