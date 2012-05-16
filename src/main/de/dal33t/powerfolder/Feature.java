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
package de.dal33t.powerfolder;

import java.util.logging.Logger;

/**
 * Available features to enable/disable. Primary for testing.
 * <p>
 * By default ALL features are enabled.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum Feature {

    OS_CLIENT, EXIT_ON_SHUTDOWN,

    /**
     * If the configuration should be loaded from the "All users" common appdata
     * directory instead of the local user appdata directory.
     */
    CONFIGURATION_ALL_USERS(false),

    /**
     * Tries to use the APPDATA directory on Windows systems.
     */
    WINDOWS_MISC_DIR_USE_APP_DATA,

    /**
     * If the nodes of a server clusters should automatically connect.
     */
    CLUSTER_NODES_CONNECT,

    /**
     * If disabled all peers will be detected as on LAN.
     */
    CORRECT_LAN_DETECTION,

    /**
     * If disabled all peers will be detected as on Internet. Requires
     * CORRECT_LAN_DETECTION to be ENABLED!
     */
    CORRECT_INTERNET_DETECTION,

    /**
     * If file movements should be checked after scan.
     */
    CORRECT_MOVEMENT_DETECTION(false),

    /**
     * If typical strings of FileInfo should be cached with a softreference.
     * <p>
     * Disabled: FileInfo:322 and 282
     */
    // CACHE_FILEINFO_STRINGS,

    /**
     * Writes the debug filelist CSV into debug directory
     */
    DEBUG_WRITE_FILELIST_CSV(false),

    /**
     * Display Tip Of Day.
     */
    TIP_OF_DAY(false),

    /**
     * True if running in beta mode.
     */
    BETA(false),

    /**
     * TRAC #198
     */
    CONFLICT_DETECTION(false),

    /**
     * TRAC #1901 for internal use only.
     */
    CREDITS_SYSTEM(false),

    /**
     * #2051: Disable email client directories until fully supported.
     */
    USER_DIRECTORIES_EMAIL_CLIENTS(false),

    /**
     * #2056 If all new created folders should first download into a temp
     * directory and mirror all contents to the actual target directory after it
     * is 100% sync.
     */
    FOLDER_ATOMIC_COMMIT(false),
    
    /**
     * Remove after #2336
     */
    MISSING_OWNER_OBTAIN(false),

    /**
     * TRAC #1962 For later use
     */
    REGION_SELECTOR(false),

    COMPACT_MODE(true),
    
    /**
     * #2533 By default disabled to save memory. Otherwise swingx.jar would be required.
     */
    SYSTEM_MONITOR(false),

    /**
     * #2726 - disable manual sync check box for now.
     */
    MANUAL_SYNC_CB(false), SYSTRAY_ALL_FOLDERS(false);

    private static final Logger log = Logger.getLogger(Feature.class.getName());

    private boolean defValue;
    private Boolean enabled;

    Feature(boolean enabled) {
        defValue = enabled;
    }

    Feature() {
        this(true);
    }

    public void disable() {
        log.fine(name() + " disabled");
        System.setProperty(getSystemPropertyKey(), "disabled");
        enabled = false;
    }

    public void enable() {
        log.fine(name() + " enabled");
        System.setProperty(getSystemPropertyKey(), "enabled");
        enabled = true;
    }

    public String getSystemPropertyKey() {
        return "powerfolder.feature." + name();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public boolean isEnabled() {
        if (enabled == null) {
            String value = System.getProperty("powerfolder.feature." + name(),
                defValue ? "enabled" : "disabled");
            enabled = "enabled".equalsIgnoreCase(value)
                || "true".equalsIgnoreCase(value)
                || "1".equalsIgnoreCase(value);
        }
        return enabled;
    }

    public static void setupForTests() {
        for (Feature feature : values()) {
            feature.disable();
        }
        // Feature.DETECT_UPDATE_BY_VERSION.enable();
        // Feature.CORRECT_MOVEMENT_DETECTION.enable();
        Feature.CREDITS_SYSTEM.enable();
    }
}
