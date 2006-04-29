/* $Id: SyncProfile.java,v 1.5 2005/11/04 13:59:58 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import org.apache.commons.lang.StringUtils;

/**
 * Instance of this class describe how a folder should be synced with the remote
 * sides
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class SyncProfile {
    public static final SyncProfile MANUAL_DOWNLOAD = new SyncProfile(
        "manualdownload", false, false, false, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_FRIENDS = new SyncProfile(
        "autodownload_friends", true, false, false, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_ALL = new SyncProfile(
        "autodownload_all", true, true, false, false, false, false, 30);

    public static final SyncProfile SYNCHRONIZE_PCS = new SyncProfile(
        "syncpcs", true, false, true, false, false, false, 10);

    public static final SyncProfile PROJECT_WORK = new SyncProfile(
        "projectwork", false, false, false, false, false, false, -1);

    public static final SyncProfile LEECHER = new SyncProfile("leecher", true,
        false, false, false, true, false, -1);

    public static final SyncProfile LEECH_RELEASER = new SyncProfile(
        "leechreleaser", false, false, false, false, false, false, 5);

    // All default sync profiles
    public static final SyncProfile[] DEFAULT_SYNC_PROFILES = new SyncProfile[]{
        SyncProfile.MANUAL_DOWNLOAD, SyncProfile.AUTO_DOWNLOAD_FROM_FRIENDS,
        SyncProfile.AUTO_DOWNLOAD_FROM_ALL, SyncProfile.SYNCHRONIZE_PCS,
        SyncProfile.PROJECT_WORK, SyncProfile.LEECHER,
        SyncProfile.LEECH_RELEASER};

    /** The id of this profile */
    private String id;

    private boolean autoDownloadFromFriends;
    private boolean autoDownloadFromOthers;

    private boolean syncDeletionWithFriends;
    private boolean syncDeletionWithOthers;

    private boolean autoDetectLocalChanges;
    private boolean autostartLeechPrograms;

    private boolean createPlaceHolderFiles;

    private int minutesBetweenScans;

    /**
     * Constructor of sync profile. After creation remains immutable
     * 
     * @param id
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param autostartLeechPrograms
     * @param createPlaceHolderFiles
     * @param minutesBetweenScans
     *            the minutes between auto-scans. use a negativ integer to
     *            disable auto-scans
     */
    public SyncProfile(String id, boolean autoDownloadFromFriends,
        boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
        boolean syncDeletionWithOthers, boolean autostartLeechPrograms,
        boolean createPlaceHolderFiles, int minutesBetweenScans)
    {
        this.id = id;
        this.autoDownloadFromFriends = autoDownloadFromFriends;
        this.autoDownloadFromOthers = autoDownloadFromOthers;
        this.syncDeletionWithFriends = syncDeletionWithFriends;
        this.syncDeletionWithOthers = syncDeletionWithOthers;
        this.autostartLeechPrograms = autostartLeechPrograms;
        this.minutesBetweenScans = minutesBetweenScans;
        this.createPlaceHolderFiles = createPlaceHolderFiles;
        this.autoDetectLocalChanges = minutesBetweenScans >= 0;
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
        return autoDetectLocalChanges;
    }

    /**
     * Answers if placeholder files should be created on the folder
     * 
     * @return
     */
    public boolean isCreatePlaceHolderFiles() {
        return createPlaceHolderFiles;
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

    /**
     * Answers if torrent/emule link downloads should start automatically
     * 
     * @return
     */
    public boolean isAutostartLeechPrograms() {
        return autostartLeechPrograms;
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