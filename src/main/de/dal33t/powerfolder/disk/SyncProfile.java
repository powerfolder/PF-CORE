/* $Id: SyncProfile.java,v 1.5 2005/11/04 13:59:58 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.StringTokenizer;

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
            false, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_FRIENDS = new SyncProfile(
            true, false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_ALL = new SyncProfile(
            true, true, false, false, 30);

    /**
     * The "Mirror" Sync profile. name still this one because of history
     * reasons.
     */
    public static final SyncProfile SYNCHRONIZE_PCS = new SyncProfile(
            true, true, true, true, 5);

    public static final SyncProfile BACKUP_SOURCE = new SyncProfile(
            false, false, false, false, 5);

    public static final SyncProfile BACKUP_TARGET = new SyncProfile(
            true, true, true, true, 30);

    public static final SyncProfile PROJECT_WORK = new SyncProfile(
            false, false, false, false, 0);

    private static final String[] defaultIds = new String[]{
            "manualdownload", "autodownload_friends", "autodownload_all",
            "syncpcs", "backupsource", "backuptarget", "projectwork"};

    // All default sync profiles
    public static final SyncProfile[] DEFAULT_SYNC_PROFILES = new SyncProfile[]{
            MANUAL_DOWNLOAD, AUTO_DOWNLOAD_FROM_FRIENDS, AUTO_DOWNLOAD_FROM_ALL,
            SYNCHRONIZE_PCS, BACKUP_SOURCE, BACKUP_TARGET, PROJECT_WORK};

    public static final String CUSTOM_SYNC_PROFILE_ID = "custom";
    private static final String FIELD_DELIMITER = ",";

    private boolean autoDownloadFromFriends;
    private boolean autoDownloadFromOthers;
    private boolean syncDeletionWithFriends;
    private boolean syncDeletionWithOthers;
    private int minutesBetweenScans;

    /**
     * Constructor of sync profile. After creation remains immutable
     *
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param minutesBetweenScans     the minutes between auto-scans. use zero to
     *                                disable auto-scans
     */
    public SyncProfile(boolean autoDownloadFromFriends,
                       boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
                       boolean syncDeletionWithOthers,
                       int minutesBetweenScans) {
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

        // try to find a default profile like this one.
        for (int i = 0; i < DEFAULT_SYNC_PROFILES.length; i++) {
            if (DEFAULT_SYNC_PROFILES[i].equals(this)) {
                return defaultIds[i];
            }
        }

        // No matching default; must be custom.
        return CUSTOM_SYNC_PROFILE_ID;
    }

    /**
     * Returns the translation id for this profile
     *
     * @return
     */
    public String getTranslationId() {
        return "syncprofile." + getId() + ".name";
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
        return autoDownloadFromFriends || autoDownloadFromOthers;
    }

    /**
     * @return true if syncing deletions with any other user
     */
    public boolean isSyncDeletion() {
        return syncDeletionWithFriends || syncDeletionWithOthers;
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
    public static SyncProfile getSyncProfileByConfig(String config) {
        if (StringUtils.isBlank(config)) {
            return null;
        }

        // Old way was to store the SyncProfile's id
        for (SyncProfile syncProfile : DEFAULT_SYNC_PROFILES) {
            if (config.equals(syncProfile.getId())) {
                return syncProfile;
            }
        }

        // Preferred way is to store the sync profile as its toString.
        // This allows for custom profiles.
        StringTokenizer st = new StringTokenizer(config, FIELD_DELIMITER);
        if (st.countTokens() == 5) {
            boolean autoDownloadFromFriends = Boolean.parseBoolean(st.nextToken());
            boolean autoDownloadFromOthers = Boolean.parseBoolean(st.nextToken());
            boolean syncDeletionWithFriends = Boolean.parseBoolean(st.nextToken());
            boolean syncDeletionWithOthers = Boolean.parseBoolean(st.nextToken());
            int minutesBetweenScans = Integer.parseInt(st.nextToken());

            // Try to find equal default profile
            SyncProfile temp = new SyncProfile(autoDownloadFromFriends,
                    autoDownloadFromOthers, syncDeletionWithFriends,
                    syncDeletionWithOthers, minutesBetweenScans);
            for (SyncProfile defaultSyncProfile : DEFAULT_SYNC_PROFILES) {
                if (temp.equals(defaultSyncProfile)) {
                    return defaultSyncProfile;
                }
            }

            // Must be a custom sync profile.
            return temp;
        }

        // Shoudl not be here!
        throw new IllegalStateException("Profile config currupt: " + config);
    }

    // General ****************************************************************

    /**
     * This is used to persist profiles to the configuration.
     * NOTE: Existing sync profiles may not load if this is changed.
     *
     * @return string representation of the profile config
     */
    public String getConfiguration() {
        return autoDownloadFromFriends + FIELD_DELIMITER +
                autoDownloadFromOthers + FIELD_DELIMITER +
                syncDeletionWithFriends + FIELD_DELIMITER +
                syncDeletionWithOthers + FIELD_DELIMITER +
                minutesBetweenScans;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SyncProfile that = (SyncProfile) obj;

        if (autoDownloadFromFriends != that.autoDownloadFromFriends) {
            return false;
        }
        if (autoDownloadFromOthers != that.autoDownloadFromOthers) {
            return false;
        }
        if (minutesBetweenScans != that.minutesBetweenScans) {
            return false;
        }
        if (syncDeletionWithFriends != that.syncDeletionWithFriends) {
            return false;
        }
        if (syncDeletionWithOthers != that.syncDeletionWithOthers) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = 7 + (autoDownloadFromFriends ? 1 : 0);
        result = 31 * result + (autoDownloadFromOthers ? 1 : 0);
        result = 31 * result + (syncDeletionWithFriends ? 1 : 0);
        result = 31 * result + (syncDeletionWithOthers ? 1 : 0);
        result = 31 * result + minutesBetweenScans;
        return result;
    }
}