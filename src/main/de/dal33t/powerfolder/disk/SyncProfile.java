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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Instance of this class describe how a folder should be synchronized with the
 * remote sides. Profiles are shared within PowerFolder. There should never be
 * two profiles with identical configurations or name. Thus if a folder has a
 * particular profile, and that profile is edited, all other folders that have
 * the same profile are directly affected. It is illegal to set a profile's
 * profileName the same as another profile. SyncProfile maintains two static
 * caches, one for preset (non-editable) profiles, and one for custom profiles.
 * The preset profiles and the preset cache can not be modified. Both caches are
 * protected inside this class and are not intended for direct external
 * modification. Access to the custom caches should always be synchronized.
 * SyncProfile has no public constructor. Custom profiles can be created using
 * the retrieveSyncProfile() method. These will first try to find from the
 * caches a profile with the same internal configuration as requested. Failing
 * this a new (custom) profile will be created and is added to the custom cache.
 * Note that if a matching profile is found in one of the caches, the
 * profileName of the returned profile will probably not equal the
 * profileNameArg supplied. Profiles can be saved and loaded as comma-separated
 * lists. Note that the old way to store a profile was as a simple 'profile id'.
 * getSyncProfileByFieldList() still supports this method for backward
 * compatability. If a profile is loaded from the config file and has the same
 * name but different internal configuration as another profile already in one
 * of the caches, then it is given an auto-generated name by adding a unique
 * number to the profileName, like 'Custom profile 3'. Preset profiles always
 * get their name from an id. This ensures that the true translated name is
 * showen if the language is changed (restart). Do not serialize SyncProfiles.
 * They will not be accepted into the caches on the target system when
 * deserialized. Use getFieldList() to transfer. This implements Serializable
 * ONLY for compliance with old Invitations.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 * @see Invitation
 */
public class SyncProfile implements Serializable {

    private static final long serialVersionUID = 100L;

    /** Field delimiter for field list */
    public static final String FIELD_LIST_DELIMITER = ",";

    /**
     * Host files preset profile.
     */
    public static final SyncProfile HOST_FILES = new SyncProfile("host_files",
        false, new SyncProfileConfiguration(false, false, false, false, 30,
            false, 12, 1, SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES,
            true));

    /**
     * Automatic download preset profile.
     */
    public static final SyncProfile AUTOMATIC_DOWNLOAD = new SyncProfile(
        "automatic_download", false, new SyncProfileConfiguration(true, true,
            false, false, 30, false, 12, 1,
            SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES, true));

    /**
     * Automatic synchronization preset profile. Uses JNotify to do instant.
     * sync.
     */
    public static final SyncProfile AUTOMATIC_SYNCHRONIZATION = new SyncProfile(
        "auto_sync", false, new SyncProfileConfiguration(true, true, true,
            true, 1, false, 12, 1,
            SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES, true));

    /**
     * Backup source preset profile. Uses JNotify to do instant.
     */
    public static final SyncProfile BACKUP_SOURCE = new SyncProfile(
        "backup_source", false, new SyncProfileConfiguration(false, false,
            false, false, 1, false, 12, 1,
            SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES, true));

    /**
     * Backup target preset profile.
     */
    public static final SyncProfile BACKUP_TARGET = new SyncProfile(
        "backup_target", false, new SyncProfileConfiguration(true, true, true,
            true, 60));

    /**
     * Manual synchronization preset profile.
     */
    public static final SyncProfile MANUAL_SYNCHRONIZATION = new SyncProfile(
        "manual_synchronization", false, new SyncProfileConfiguration(false,
            false, false, false, 0));

    // All preset sync profiles
    private static final SyncProfile[] PRESET_SYNC_PROFILES = {
        AUTOMATIC_SYNCHRONIZATION, MANUAL_SYNCHRONIZATION, BACKUP_SOURCE,
        BACKUP_TARGET, AUTOMATIC_DOWNLOAD, HOST_FILES};

    /** Special no-sync profile for preview folders. Same config as PROJECT_WORK */
    public static final SyncProfile NO_SYNC = new SyncProfile("no_sync", false,
        new SyncProfileConfiguration(false, false, false, false, 0));

    /**
     * Special no-sync profile for disabled folders in Online Storage. Only
     * syncs file deletions
     */
    public static final SyncProfile DISABLED = new SyncProfile("disabled",
        false, new SyncProfileConfiguration(false, false, true, true, 0));

    /**
     * Backup source preset profile for Online Storage. Don't scan for local
     * changes
     */
    public static final SyncProfile BACKUP_TARGET_NO_CHANGE_DETECT = new SyncProfile(
        "backup_target_no_change", false, new SyncProfileConfiguration(true,
            true, true, true, 0));

    /**
     * Especially for meta-folders. Scan every minute.
     */
    public static final SyncProfile META_FOLDER_SYNC = new SyncProfile(
        "backup_target_no_change", false, new SyncProfileConfiguration(true,
            true, true, true, 1));

    /**
     * All custom profiles
     */
    private static final List<SyncProfile> customProfiles = new ArrayList<SyncProfile>();

    /**
     * The name of the profile (for custom profiles)
     */
    private String profileName;

    /**
     * The id of the profile (for preset profiles)
     */
    private String profileId;

    /**
     * Indicates that this is a custom profile. This should only ever be false
     * for the static preset profiles created inside this class.
     */
    private final boolean custom;

    /**
     * The internal configuration of the profile. This determines how a folder
     * synchronizes with other nodes.
     */
    private SyncProfileConfiguration configuration;

    /**
     * Constructor.
     * 
     * @param profileNameId
     *            name (custom) or id (preset) of the profile
     * @param custom
     *            whether this is a custom profile
     * @param configuration
     *            the configuration of the profile
     */
    private SyncProfile(String profileNameId, boolean custom,
        SyncProfileConfiguration configuration)
    {
        if (custom) {
            profileName = profileNameId;
        } else {
            profileId = profileNameId;
        }
        this.custom = custom;
        this.configuration = configuration;
    }

    /**
     * Returns tue if this is a custom profile.
     * 
     * @return
     */
    public boolean isCustom() {
        return custom;
    }

    /**
     * @return the profile name.
     */
    public String getName() {
        if (custom) {
            return profileName;
        } else {
            return translateId(profileId);
        }
    }

    /**
     * Sets the profile name. It is illegal to set the profileName the same as
     * another profile, because this breaks the required uniquness of profiles.
     * Always test for name uniqueness first with the safe checkName() method.
     * 
     * @param profileName
     */
    public void setName(String profileName) {

        Reject.ifFalse(custom, "Cannot set the profileName of preset profile "
            + getName() + " to " + profileName);
        Reject.ifBlank(profileName, "ProfileName not supplied");

        // Ensure that the name is not being set to an existing sync profile
        // name
        for (SyncProfile profile : PRESET_SYNC_PROFILES) {
            if (!equals(profile) && profile.getName().equals(profileName)) {
                throw new RuntimeException(
                    "Preset profile name already exists.");
            }
        }
        synchronized (customProfiles) {
            for (SyncProfile customProfile : customProfiles) {
                if (!equals(customProfile)
                    && customProfile.getName().equals(profileName))
                {
                    throw new RuntimeException(
                        "Custom profile name already exists.");
                }
            }
        }

        this.profileName = profileName;
    }

    public SyncProfileConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SyncProfileConfiguration configuration) {

        Reject.ifFalse(custom,
            "Cannot set the configuration of preset profile " + getName());
        Reject.ifNull(configuration, "configuration not supplied");

        // Ensure that the config is unique
        for (SyncProfile profile : PRESET_SYNC_PROFILES) {
            if (!equals(profile) && profile.configuration.equals(configuration))
            {
                throw new RuntimeException(
                    "Preset profile config already exists.");
            }
        }
        synchronized (customProfiles) {
            for (SyncProfile customProfile : customProfiles) {
                if (!equals(customProfile)
                    && customProfile.configuration.equals(configuration))
                {
                    throw new RuntimeException(
                        "Custom profile config already exists.");
                }
            }
        }

        this.configuration = configuration;
    }

    /**
     * This is used to persist profiles to the configuration. NOTE: Existing
     * sync profiles may not load if this is changed. Add any new fields to the
     * end of the list.
     * 
     * @return string representation of the profile config as a list of fields
     */
    public String getFieldList() {
        // Twice for backward compatibility. TRAC #1626
        return configuration.isAutoDownload()
            + FIELD_LIST_DELIMITER
            + configuration.isAutoDownload()
            + FIELD_LIST_DELIMITER
            // Twice for backward compatibility. TRAC #1626
            + configuration.isSyncDeletion() + FIELD_LIST_DELIMITER
            + configuration.isSyncDeletion() + FIELD_LIST_DELIMITER
            + configuration.getTimeBetweenRegularScans() + FIELD_LIST_DELIMITER
            + configuration.isDailySync() + FIELD_LIST_DELIMITER
            + configuration.getDailyHour() + FIELD_LIST_DELIMITER
            + configuration.getDailyDay() + FIELD_LIST_DELIMITER
            + configuration.getRegularTimeType() + FIELD_LIST_DELIMITER
            + getName() + FIELD_LIST_DELIMITER + configuration.isInstantSync();
    }

    /**
     * For preset config, the name is i18n using 'transfer_mode.x.name'.
     * 
     * @param id
     *            translate 'syncprofile.[id].name'
     * @return
     */
    private static String translateId(String id) {
        return Translation.getTranslation("transfer_mode." + id + ".name");
    }

    /**
     * Method for either retrieving or creating a sync profile from the caches.
     * Note that if a profile is retrieved, it may not have the same name as the
     * profileNameArg arg, but it will have the same configuration.
     */
    public static SyncProfile retrieveSyncProfile(String profileNameArg,
        SyncProfileConfiguration syncProfileConfigurationArg)
    {

        Reject.ifNull(syncProfileConfigurationArg,
            "Null sync profile configuration");

        List<String> names = new ArrayList<String>();

        // Check presetProfiles
        for (SyncProfile profile : PRESET_SYNC_PROFILES) {
            if (profile.configuration.equals(syncProfileConfigurationArg)) {

                return profile;
            }
            names.add(profile.getName());
        }

        // Check existing profiles
        synchronized (customProfiles) {
            for (SyncProfile customProfile : customProfiles) {
                if (customProfile.configuration
                    .equals(syncProfileConfigurationArg))
                {
                    return customProfile;
                }
                names.add(customProfile.getName());
            }
        }

        // Ensure new profile has a unique name;
        boolean emptyName = profileNameArg.trim().length() == 0;
        String workingProfileName = emptyName
            ? translateId("custom")
            : profileNameArg;
        SyncProfile syncProfile;
        if (names.contains(workingProfileName) || emptyName) {
            int i = 1;
            while (names.contains(workingProfileName + ' ' + i)) {
                i++;
            }
            syncProfile = new SyncProfile(workingProfileName + ' ' + i, true,
                syncProfileConfigurationArg);
        } else {
            syncProfile = new SyncProfile(workingProfileName, true,
                syncProfileConfigurationArg);
        }

        // Store in the custom cache.
        synchronized (customProfiles) {
            customProfiles.add(syncProfile);
        }

        return syncProfile;
    }

    /**
     * Gets a copy of the sync profiles. Adding or deleting from this list does
     * not affect the SyncProfile caches, but changing the profile config does.
     * 
     * @return Shallow copy of SyncProfile caches.
     */
    public static List<SyncProfile> getSyncProfilesCopy() {
        List<SyncProfile> list = new ArrayList<SyncProfile>();
        list.addAll(Arrays.asList(PRESET_SYNC_PROFILES));
        synchronized (customProfiles) {
            for (SyncProfile customProfile : customProfiles) {
                if (!list.contains(customProfile)) {
                    list.add(customProfile);
                }
            }
        }
        return list;
    }

    /**
     * Tries to resolve a sync profile by id (the old way of storing sync
     * profiles). Else it expects a comma-separated list of profile fieldList.
     * 
     * @param fieldList
     * @return
     * @see #getFieldList()
     */
    public static SyncProfile getSyncProfileByFieldList(String fieldList) {

        Reject.ifNull(fieldList, "Null sync profile fieldList");

        // Old way was to store the SyncProfile's id. search presets
        if (!fieldList.contains(FIELD_LIST_DELIMITER)) {
            for (SyncProfile syncProfile : PRESET_SYNC_PROFILES) {
                if (fieldList.equals(syncProfile.profileId)) {
                    return syncProfile;
                }
            }
        }

        // Preferred way is to store the sync profile as its getFieldList().
        // This allows for custom profiles.
        StringTokenizer st = new StringTokenizer(fieldList,
            FIELD_LIST_DELIMITER);
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
        int dailyHour = SyncProfileConfiguration.DAILY_HOUR_DEFAULT;
        if (st.hasMoreTokens()) {
            dailyHour = Integer.parseInt(st.nextToken());
        }
        int dailyDay = SyncProfileConfiguration.DAILY_DAY_EVERY_DAY;
        if (st.hasMoreTokens()) {
            dailyDay = Integer.parseInt(st.nextToken());
        }
        String timeType = SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES;
        if (st.hasMoreTokens()) {
            timeType = st.nextToken();
        }
        String profileName = "";
        if (st.hasMoreTokens()) {
            profileName = st.nextToken();
        }
        boolean instantSync = false;
        if (st.hasMoreTokens()) {
            instantSync = Boolean.parseBoolean(st.nextToken());
        }

        return retrieveSyncProfile(profileName, new SyncProfileConfiguration(
            autoDownloadFromFriends, autoDownloadFromOthers,
            syncDeletionWithFriends, syncDeletionWithOthers, timeBetweenScans,
            dailySync, dailyHour, dailyDay, timeType, instantSync));
    }

    /**
     * #2132
     * 
     * @param controller
     * @return the default sync profile according to the configuration.
     */
    public static SyncProfile getDefault(Controller controller) {
        String defFieldList = ConfigurationEntry.DEFAULT_TRANSFER_MODE
            .getValue(controller);
        try {
            return SyncProfile.getSyncProfileByFieldList(defFieldList);
        } catch (Exception e) {
            Logger.getLogger(SyncProfile.class.getName()).severe(
                "Unable to get default transfer mode for " + defFieldList
                    + ". Please check your config: " + e);
            return SyncProfile
                .getSyncProfileByFieldList(ConfigurationEntry.DEFAULT_TRANSFER_MODE
                    .getDefaultValue());
        }
    }

    /**
     * @return true if folder automatically detects changes to files on disk
     */
    public boolean isInstantSync() {
        return configuration.isInstantSync();
    }

    /**
     * @return true if folder detects changes in periodic timeframes (e.g. every
     *         hour).
     */
    public boolean isPeriodicSync() {
        return configuration.isPeriodicSync();
    }

    /**
     * @return true if this profile only detects changes when user presses
     *         manually the sync button.
     */
    public boolean isManualSync() {
        return configuration.isManualSync();
    }

    /**
     * @return true if folder detects changes in daily/scheduled timeframes
     *         (e.g. Mo / 1200 pm).
     */
    public boolean isDailySync() {
        return configuration.isDailySync();
    }

    /**
     * Answers the seconds to wait between disk scans. Only relevant if
     * auto-detect changes is enabled
     * 
     * @return
     */
    public int getSecondsBetweenScans() {
        String timeType = configuration.getRegularTimeType();
        if (configuration.getRegularTimeType() == null) {
            timeType = SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES;
        }
        if (SyncProfileConfiguration.REGULAR_TIME_TYPE_SECONDS.equals(timeType))
        {
            return configuration.getTimeBetweenRegularScans();
        } else if (SyncProfileConfiguration.REGULAR_TIME_TYPE_HOURS
            .equals(timeType))
        {
            return configuration.getTimeBetweenRegularScans() * 3600;
        } else {
            return configuration.getTimeBetweenRegularScans() * 60;
        }
    }

    /**
     * @return true if new/update files should be automatically downloaded;
     */
    public boolean isAutodownload() {
        return configuration.isAutoDownload();
    }

    /**
     * @return true if syncing deletions with any other user
     */
    public boolean isSyncDeletion() {
        return configuration.isSyncDeletion();
    }

    /**
     * Remove a profile from the cache.
     * 
     * @param profileArg
     */
    public static void deleteProfile(SyncProfile profileArg) {
        synchronized (customProfiles) {
            for (Iterator<SyncProfile> iter = customProfiles.iterator(); iter
                .hasNext();)
            {
                SyncProfile profile = iter.next();
                if (profile.equals(profileArg)) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * Check equality on configuration only. This is the important field.
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
        SyncProfile that = (SyncProfile) obj;
        return configuration.equals(that.configuration);
    }

    /**
     * Like equal.
     * 
     * @return
     */
    public int hashCode() {
        return configuration.hashCode();
    }

}