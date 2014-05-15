/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.util.Reject;

import java.io.Serializable;

/**
 * Class representing the configuration of a SyncProfile. This determines how a
 * folder with a particular SyncProfile behaves. SyncProfileConfigurations are
 * immutable.
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
     * The time between regular scans. TimeType determins whether this period is
     * hours, minutes or seconds
     */
    private final int timeBetweenRegularScans;

    /**
     * Whther this scan is regular (every n hours, minutes or seconds), or daily
     * (once per day / week period at a particular hour of the day). Tied to
     * instantSync.
     */
    private final boolean dailySync;

    /**
     * The hour of the day to do a daily scan. 0 through 23.
     */
    private final int dailyHour;

    /**
     * Day / week period to do daily scans. 0 == every day, 1 through 7 as
     * Calendar.DAY_OF_WEEK, 8 == weekdays (Monday through Friday), 9 ==
     * weekends.
     */
    private final int dailyDay;

    /**
     * The time type to do regular sacns (every n hours, minutes or seconds)
     * Hours, minutes or seconds.
     */
    private final String regularTimeType;

    /**
     * True if synchronization is instant. Not this is tied to daily sync.
     * Sync mode is effectively instantSync ? true : dailySync;
     * Need to do this to keep good serialization.
     */
    private final boolean instantSync;

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
        boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
        boolean syncDeletionWithOthers, int timeBetweenRegularScans)
    {

        this(autoDownloadFromFriends, autoDownloadFromOthers,
            syncDeletionWithFriends, syncDeletionWithOthers,
            timeBetweenRegularScans, false, DAILY_HOUR_DEFAULT,
            DAILY_DAY_EVERY_DAY, REGULAR_TIME_TYPE_MINUTES, false);
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
     * @param instantSync
     */
    public SyncProfileConfiguration(boolean autoDownloadFromFriends,
        boolean autoDownloadFromOthers, boolean syncDeletionWithFriends,
        boolean syncDeletionWithOthers, int timeBetweenRegularScans,
        boolean dailySync, int dailyHour, int dailyDay, String regularTimeType,
        boolean instantSync)
    {

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
        this.instantSync = instantSync;
    }

    /**
     * @return true if new/update files should be automatically downloaded;
     */
    public boolean isAutoDownload() {
        return autoDownloadFromFriends || autoDownloadFromOthers;
    }

    /**
     * @return to synchronize deletions
     */
    public boolean isSyncDeletion() {
        return syncDeletionWithFriends || syncDeletionWithOthers;
    }

    /**
     * The time between regular scans. TimeType determins whether this period is
     * hours, minutes or seconds.
     *
     * @return
     */
    public int getTimeBetweenRegularScans() {
        return timeBetweenRegularScans;
    }

    /**
     * Whther this scan is periodic.
     *
     * @return
     */
    public boolean isPeriodicSync() {
        return !instantSync && !dailySync;
    }

    /**
     * @return true if this profile only detects changes when user presses
     *         manually the sync button.
     */
    public boolean isManualSync() {
        return isPeriodicSync() && timeBetweenRegularScans == 0;
    }

    /**
     * Whther this scan is regular (every n hours, minutes or seconds), or daily
     * (once per day / week period at a particular hour of the day).
     *
     * @return
     */
    public boolean isDailySync() {
        return !instantSync && dailySync;
    }

    /**
     * Whther this scan is instant.
     *
     * @return
     */
    public boolean isInstantSync() {
        return instantSync;
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
     * Day / week period to do daily scans. 0 == every day, 1 through 7 as
     * Calendar.DAY_OF_WEEK, 8 == weekdays (Monday through Friday), 9 ==
     * weekends.
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (autoDownloadFromFriends ? 1231 : 1237);
        result = prime * result + (autoDownloadFromOthers ? 1231 : 1237);
        result = prime * result + dailyDay;
        result = prime * result + dailyHour;
        result = prime * result + (dailySync ? 1231 : 1237);
        result = prime * result + (instantSync ? 1231 : 1237);
        result = prime * result
            + ((regularTimeType == null) ? 0 : regularTimeType.hashCode());
        result = prime * result + (syncDeletionWithFriends ? 1231 : 1237);
        result = prime * result + (syncDeletionWithOthers ? 1231 : 1237);
        result = prime * result + timeBetweenRegularScans;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SyncProfileConfiguration other = (SyncProfileConfiguration) obj;
        if (autoDownloadFromFriends != other.autoDownloadFromFriends)
            return false;
        if (autoDownloadFromOthers != other.autoDownloadFromOthers)
            return false;
        if (dailyDay != other.dailyDay)
            return false;
        if (dailyHour != other.dailyHour)
            return false;
        if (dailySync != other.dailySync)
            return false;
        if (instantSync != other.instantSync)
            return false;
        if (regularTimeType == null) {
            if (other.regularTimeType != null)
                return false;
        } else if (!regularTimeType.equals(other.regularTimeType))
            return false;
        if (syncDeletionWithFriends != other.syncDeletionWithFriends)
            return false;
        if (syncDeletionWithOthers != other.syncDeletionWithOthers)
            return false;
        if (timeBetweenRegularScans != other.timeBetweenRegularScans)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SyncProfileConfiguration{" +
                "autoDownloadFromFriends=" + autoDownloadFromFriends +
                ", autoDownloadFromOthers=" + autoDownloadFromOthers +
                ", syncDeletionWithFriends=" + syncDeletionWithFriends +
                ", syncDeletionWithOthers=" + syncDeletionWithOthers +
                ", timeBetweenRegularScans=" + timeBetweenRegularScans +
                ", dailySync=" + dailySync +
                ", dailyHour=" + dailyHour +
                ", dailyDay=" + dailyDay +
                ", regularTimeType='" + regularTimeType + '\'' +
                ", instantSync=" + instantSync +
                '}';
    }
}