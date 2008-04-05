package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.util.Reject;

import java.io.Serializable;

/**
 * Class representing the configuration of a SyncProfile.
 * This determines how a folder with a particular SyncProfile behaves.
 * SyncProfileConfigurations are imutable.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.0 $

 */
public class SyncProfileConfiguration implements Serializable {

    /** Serial version id */
    private static final long serialVersionUID = 100L;

    /**
     * Normally the default daily scan hour is mid-day
     */
    public static final int DAILY_HOUR_DEFAULT = 12;

    /**
     * For dailyDay, 0 is 'scan every day'
     */
    public static final int DAILY_DAY_EVERY_DAY = 0;

    /**
     * For dailyDay, 8 is 'scan weekdays only (Monday to Friday)'
     */
    public static final int DAILY_DAY_WEEKDAYS = 8;

    /**
     * For dailyDay, 9 is 'scan weekends only'
     */
    public static final int DAILY_DAY_WEEKENDS = 9;

    /**
     * For regularTimeType, hourly scans are 'h'
     */
    public static final String REGULAR_TIME_TYPE_HOURS = "h";

    /**
     * For regularTimeType, minute-based scan periods are 'm'
     */
    public static final String REGULAR_TIME_TYPE_MINUTES = "m";

    /**
     * For regularTimeType, second-based scan periods are 's'
     */
    public static final String REGULAR_TIME_TYPE_SECONDS = "s";

    /**
     * Whether to automatically download from friends
     */
    private final boolean autoDownloadFromFriends;

    /**
     * Whether to automatically download from non-friends
     */
    private final boolean autoDownloadFromOthers;

    /**
     * Whether to synchronize deletions from friends
     */
    private final boolean syncDeletionWithFriends;

    /**
     * Whether to synchronize deletions from non-friends
     */
    private final boolean syncDeletionWithOthers;

    /**
     * The time between regular scans.
     * TimeType determins whether this period is hours, minutes or seconds
     */
    private final int timeBetweenRegularScans;

    /**
     * Whther this scan is regular (every n hours, minutes or seconds),
     * or daily (once per day / week period at a particular hour of the day).
     */
    private final boolean dailySync;

    /**
     * The hour of the day to do a daily scan. 0 through 23.
     */
    private final int dailyHour;

    /**
     * Day / week period to do daily scans.
     * 0 == every day,
     * 1 through 7 as Calendar.DAY_OF_WEEK,
     * 8 == weekdays (Monday through Friday),
     * 9 == weekends.
     */
    private final int dailyDay;

    /**
     * The time type to do regular sacns (every n hours, minutes or seconds)
     * Hours, minutes or seconds.
     */
    private final String regularTimeType;

    /**
     * Simple construtor. Default values set for advanced configuration.
     *
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param timeBetweenRegularScans
     */
    public SyncProfileConfiguration(boolean autoDownloadFromFriends,
                                    boolean autoDownloadFromOthers,
                                    boolean syncDeletionWithFriends,
                                    boolean syncDeletionWithOthers,
                                    int timeBetweenRegularScans) {

        this(autoDownloadFromFriends, autoDownloadFromOthers,
                syncDeletionWithFriends, syncDeletionWithOthers,
                timeBetweenRegularScans, false, DAILY_HOUR_DEFAULT,
                DAILY_DAY_EVERY_DAY, REGULAR_TIME_TYPE_MINUTES);
    }

    /**
     * Full construtor.
     *
     * @param autoDownloadFromFriends
     * @param autoDownloadFromOthers
     * @param syncDeletionWithFriends
     * @param syncDeletionWithOthers
     * @param timeBetweenRegularScans
     * @param dailySync
     * @param dailyHour
     * @param dailyDay
     * @param regularTimeType
     */
    public SyncProfileConfiguration(boolean autoDownloadFromFriends,
                                    boolean autoDownloadFromOthers,
                                    boolean syncDeletionWithFriends,
                                    boolean syncDeletionWithOthers,
                                    int timeBetweenRegularScans,
                                    boolean dailySync, int dailyHour,
                                    int dailyDay,
                                    String regularTimeType) {

        Reject.ifBlank(regularTimeType, "Missing regularTimeType");

        this.autoDownloadFromFriends = autoDownloadFromFriends;
        this.autoDownloadFromOthers = autoDownloadFromOthers;
        this.syncDeletionWithFriends = syncDeletionWithFriends;
        this.syncDeletionWithOthers = syncDeletionWithOthers;
        this.timeBetweenRegularScans = timeBetweenRegularScans;
        this.dailySync = dailySync;
        this.dailyHour = dailyHour;
        this.dailyDay = dailyDay;
        this.regularTimeType = regularTimeType;
    }

    /**
     * Whether to automatically download from friends
     *
     * @return
     */
    public boolean isAutoDownloadFromFriends() {
        return autoDownloadFromFriends;
    }

    /**
     * Whether to automatically download from non-friends
     *
     * @return
     */
    public boolean isAutoDownloadFromOthers() {
        return autoDownloadFromOthers;
    }

    /**
     * Whether to synchronize deletions from friends
     *
     * @return
     */
    public boolean isSyncDeletionWithFriends() {
        return syncDeletionWithFriends;
    }

    /**
     * Whether to synchronize deletions from non-friends
     *
     * @return
     */
    public boolean isSyncDeletionWithOthers() {
        return syncDeletionWithOthers;
    }

    /**
     * The time between regular scans.
     * TimeType determins whether this period is hours, minutes or seconds.
     *
     * @return
     */
    public int getTimeBetweenRegularScans() {
        return timeBetweenRegularScans;
    }

    /**
     * Whther this scan is regular (every n hours, minutes or seconds),
     * or daily (once per day / week period at a particular hour of the day).
     *
     * @return
     */
    public boolean isDailySync() {
        return dailySync;
    }

    /**
     * The hour of the day to do a daily scan. 0 through 23.
     *
     * @return
     */
    public int getDailyHour() {
        return dailyHour;
    }

    /**
     * Day / week period to do daily scans.
     * 0 == every day,
     * 1 through 7 as Calendar.DAY_OF_WEEK,
     * 8 == weekdays (Monday through Friday),
     * 9 == weekends.
     *
     * @return
     */
    public int getDailyDay() {
        return dailyDay;
    }

    /**
     * The time type to do regular sacns (every n hours, minutes or seconds)
     * Hours, minutes or seconds.
     *
     * @return
     */
    public String getRegularTimeType() {
        return regularTimeType;
    }

    /**
     * True if object is identical to this.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SyncProfileConfiguration that = (SyncProfileConfiguration) obj;

        if (autoDownloadFromFriends != that.autoDownloadFromFriends) {
            return false;
        }
        if (autoDownloadFromOthers != that.autoDownloadFromOthers) {
            return false;
        }
        if (dailyDay != that.dailyDay) {
            return false;
        }
        if (dailyHour != that.dailyHour) {
            return false;
        }
        if (dailySync != that.dailySync) {
            return false;
        }
        if (syncDeletionWithFriends != that.syncDeletionWithFriends) {
            return false;
        }
        if (syncDeletionWithOthers != that.syncDeletionWithOthers) {
            return false;
        }
        if (timeBetweenRegularScans != that.timeBetweenRegularScans) {
            return false;
        }
        if (regularTimeType != null ?
                !regularTimeType.equals(that.regularTimeType) :
                that.regularTimeType != null) {
            return false;
        }

        return true;
    }

    /**
     * Reasonably unique hash.
     *
     * @return
     */
    public int hashCode() {
        int result = autoDownloadFromFriends ? 1 : 0;
        result = 31 * result + (autoDownloadFromOthers ? 1 : 0);
        result = 31 * result + (syncDeletionWithFriends ? 1 : 0);
        result = 31 * result + (syncDeletionWithOthers ? 1 : 0);
        result = 31 * result + timeBetweenRegularScans;
        result = 31 * result + (dailySync ? 1 : 0);
        result = 31 * result + dailyHour;
        result = 31 * result + dailyDay;
        result = 31 * result + (regularTimeType != null ? regularTimeType.hashCode() : 0);
        return result;
    }

    /**
     * String representation.
     *
     * @return
     */
    public String toString() {
        return "SyncProfileConfiguration [" +
                " autoDownloadFromFriends = " + autoDownloadFromFriends +
                " autoDownloadFromOthers = " + autoDownloadFromOthers +
                " syncDeletionWithFriends = " + syncDeletionWithFriends +
                " syncDeletionWithOthers = " + syncDeletionWithOthers +
                " timeBetweenRegularScans = " + timeBetweenRegularScans +
                " dailySync = " + dailySync + " dailyHour = " + dailyHour +
                " dailyDay = " + dailyDay +
                " regularTimeType =     " + regularTimeType +
                ']';
    }
}