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
    private static final int DEFAULT_DAILY_HOUR = 12; // Midday
    public static final int EVERY_DAY = 0;
    public static final int WEEKDAYS = 8;
    public static final int WEEKENDS = 9;
    public static final String CUSTOM_SYNC_PROFILE_ID = "custom";
    private static final String FIELD_DELIMITER = ",";
    public static final String HOURS = "h";
    public static final String MINUTES = "m";
    public static final String SECONDS = "s";

    public static final SyncProfile MANUAL_DOWNLOAD = new SyncProfile(false,
        false, false, false, 30);

    public static final SyncProfile AUTO_DOWNLOAD_FROM_ALL = new SyncProfile(
        true, true, false, false, 30);

    /**
     * The "Mirror" Sync profile. name still this one because of history
     * reasons.
     */
    public static final SyncProfile SYNCHRONIZE_PCS = new SyncProfile(true,
        true, true, true, 5);

    public static final SyncProfile BACKUP_SOURCE = new SyncProfile(false,
        false, false, false, 5);

    public static final SyncProfile BACKUP_TARGET = new SyncProfile(true, true,
        true, true, 60);

    public static final SyncProfile PROJECT_WORK = new SyncProfile(false,
        false, false, false, 0);

    /** Used for preview folders only */
    public static final SyncProfile NO_SYNC = new SyncProfile(false,
        false, false, false, 0);

    private static final String[] defaultIds = new String[]{"syncpcs",
        "backupsource", "backuptarget", "autodownload_all", "manualdownload",
        "projectwork"};

    // All default sync profiles
    public static final SyncProfile[] DEFAULT_SYNC_PROFILES = new SyncProfile[]{
        SYNCHRONIZE_PCS, BACKUP_SOURCE, BACKUP_TARGET, AUTO_DOWNLOAD_FROM_ALL,
        MANUAL_DOWNLOAD, PROJECT_WORK};

    private boolean autoDownloadFromFriends;
    private boolean autoDownloadFromOthers;
    private boolean syncDeletionWithFriends;
    private boolean syncDeletionWithOthers;
    private int timeBetweenScans;
    private boolean dailySync;

    /**
     * 0 through 23.
     */
    private int dailyHour;

    /**
     * 0 == every day,
     * 1 through 7 == like Calendar.DAY_OF_WEEK,
     * 8 == weekdays (Monday through Friday),
     * 9 == weekends.
     */
    private int dailyDay;

    private String timeType = MINUTES;

    /**
     * Constructor of sync profile. After creation remains immutable
     *
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param minutesBetweenScans
     *            the minutes between auto-scans. use zero to disable auto-scans
     */
    public SyncProfile(boolean autoDownloadFromFriends,
        boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
        boolean syncDeletionWithOthers, int timeBetweenScans)
    {
        this.autoDownloadFromFriends = autoDownloadFromFriends;
        this.autoDownloadFromOthers = autoDownloadFromOthers;
        this.syncDeletionWithFriends = syncDeletionWithFriends;
        this.syncDeletionWithOthers = syncDeletionWithOthers;
        this.timeBetweenScans = timeBetweenScans;
        dailyHour = DEFAULT_DAILY_HOUR;
        dailyDay = EVERY_DAY;
    }

    /**
     * Constructor of sync profile. After creation remains immutable
     *
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param minutesBetweenScans
     *            the minutes between auto-scans. use zero to disable auto-scans
     * @param dailySync
     * @param dailyHour
     * @param dailyDay
     */
    public SyncProfile(boolean autoDownloadFromFriends,
                       boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
                       boolean syncDeletionWithOthers, int timeBetweenScans,
                       boolean dailySync, int dailyHour, int dailyDay,
                       String timeType) {
        this.autoDownloadFromFriends = autoDownloadFromFriends;
        this.autoDownloadFromOthers = autoDownloadFromOthers;
        this.syncDeletionWithFriends = syncDeletionWithFriends;
        this.syncDeletionWithOthers = syncDeletionWithOthers;
        this.timeBetweenScans = timeBetweenScans;
        this.dailySync = dailySync;
        this.dailyHour = dailyHour;
        this.dailyDay = dailyDay;
        this.timeType = timeType;
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
    public static String getTranslationId(String id) {
        return "syncprofile." + id + ".name";
    }

    public String getTranslationId() {
        return getTranslationId(getId());
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

    public int getTimeBetweenScans() {
        return timeBetweenScans;
    }

    public void setTimeBetweenScans(int timeBetweenScans) {
        this.timeBetweenScans = timeBetweenScans;
    }

    public String getTimeType() {
        return timeType == null ? MINUTES : timeType;
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
        return timeBetweenScans > 0;
    }

    /**
     * Answers the seconds to wait between disk scans. Only relevant if
     * auto-detect changes is enabled
     * 
     * @return
     */
    public int getSecondsBetweenScans() {
        if (timeType == null) {
            timeType = MINUTES;
        }
        if (SECONDS.equals(timeType)) {
            return timeBetweenScans;
        } else if (HOURS.equals(timeType)) {
            return timeBetweenScans * 3600;
        } else {
            return timeBetweenScans * 60; // Minutes
        }
    }

    public boolean isDailySync() {
        return dailySync;
    }

    public int getDailyHour() {
        return dailyHour;
    }

    public int getDailyDay() {
        return dailyDay;
    }

    // Static accessors *******************************************************

    /**
     * Tries to resolve a sync profile by id. Returns null if nothing was found
     * 
     * @param config
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
        boolean autoDownloadFromFriends = false;
        if (st.hasMoreTokens()) {
            autoDownloadFromFriends = Boolean.parseBoolean(st.nextToken());
        }
        boolean autoDownloadFromOthers = false;
        if (st.hasMoreTokens()) {
            autoDownloadFromOthers = Boolean.parseBoolean(st.nextToken());
        }
        boolean syncDeletionWithFriends = false;
        if (st.hasMoreTokens()) {
            syncDeletionWithFriends = Boolean.parseBoolean(st.nextToken());
        }
        boolean syncDeletionWithOthers = false;
        if (st.hasMoreTokens()) {
            syncDeletionWithOthers = Boolean.parseBoolean(st.nextToken());
        }
        int timeBetweenScans = 0;
        if (st.hasMoreTokens()) {
            timeBetweenScans = Integer.parseInt(st.nextToken());
        }
        boolean dailySync = false;
        if (st.hasMoreTokens()) {
            dailySync = Boolean.parseBoolean(st.nextToken());
        }
        int dailyHour = DEFAULT_DAILY_HOUR;
        if (st.hasMoreTokens()) {
            dailyHour = Integer.parseInt(st.nextToken());
        }
        int dailyDay = EVERY_DAY;
        if (st.hasMoreTokens()) {
            dailyDay = Integer.parseInt(st.nextToken());
        }
        String tt = MINUTES;
        if (st.hasMoreTokens()) {
            tt = st.nextToken();
        }

        // Try to find equal default profile
        SyncProfile temp = new SyncProfile(autoDownloadFromFriends,
                autoDownloadFromOthers, syncDeletionWithFriends,
                syncDeletionWithOthers, timeBetweenScans,
                dailySync, dailyHour, dailyDay, tt);
        for (SyncProfile defaultSyncProfile : DEFAULT_SYNC_PROFILES) {
            if (temp.equals(defaultSyncProfile)) {
                return defaultSyncProfile;
            }
        }

        // Must be a custom sync profile.
        return temp;

    }

    // General ****************************************************************

    /**
     * This is used to persist profiles to the configuration. NOTE: Existing
     * sync profiles may not load if this is changed.
     * 
     * @return string representation of the profile config
     */
    public String getConfiguration() {
        return autoDownloadFromFriends + FIELD_DELIMITER +
                autoDownloadFromOthers + FIELD_DELIMITER +
                syncDeletionWithFriends + FIELD_DELIMITER +
                syncDeletionWithOthers + FIELD_DELIMITER +
                timeBetweenScans + FIELD_DELIMITER +
                dailySync + FIELD_DELIMITER +
                dailyHour + FIELD_DELIMITER +
                dailyDay + FIELD_DELIMITER +
                (timeType == null ? MINUTES : timeType);
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
        if (timeBetweenScans != that.timeBetweenScans) {
            return false;
        }
        if (syncDeletionWithFriends != that.syncDeletionWithFriends) {
            return false;
        }
        if (syncDeletionWithOthers != that.syncDeletionWithOthers) {
            return false;
        }
        if (dailySync != that.dailySync) {
            return false;
        }
        if (dailyHour != that.dailyHour) {
            return false;
        }
        if (dailyDay != that.dailyDay) {
            return false;
        }
        if (timeType != null && that.timeType == null) {
            return false;
        } else if (timeType == null && that.timeType != null) {
            return false;
        } else if (timeType != null && that.timeType != null && !timeType.equals(that.timeType)) {
            return false;
        } // if both null, they are equal.
        return true;
    }

    public int hashCode() {
        int result = 7 + (autoDownloadFromFriends ? 1 : 0);
        result = 31 * result + (autoDownloadFromOthers ? 1 : 0);
        result = 31 * result + (syncDeletionWithFriends ? 1 : 0);
        result = 31 * result + (syncDeletionWithOthers ? 1 : 0);
        result = 31 * result + timeBetweenScans;
        result = 31 * result + (dailySync ? 1 : 0);
        result = 31 * result + dailyHour;
        result = 31 * result + dailyDay;
        result = 31 * result + dailyDay;

        
        result = 31 * result + (timeType == null ? 0 : timeType.hashCode());
        return result;
    }

    public boolean isCustom() {
        for (SyncProfile defaultSyncProfile : DEFAULT_SYNC_PROFILES) {
            if (this.equals(defaultSyncProfile)) {
                return false;
            }
        }
        return true;
    }
}