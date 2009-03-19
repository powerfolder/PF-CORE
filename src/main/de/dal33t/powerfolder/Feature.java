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
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum Feature {

    OS_CLIENT, EXIT_ON_SHUTDOWN,

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
    CORRECT_MOVEMENT_DETECTION,

    /**
     * If typical strings of FileInfo should be cached with a softreference.
     */
    CACHE_FILEINFO_STRINGS,

    /**
     * If file updates get detected newer using the version counter. Otherwise
     * the last modification date is uesd.
     * <P>
     * #658
     * <P>
     */
    DETECT_UPDATE_BY_VERSION,

    /**
     * If the server should offer PowerFolder internal functions.
     * <p>
     * TODO Find abstraction for separated parts.
     */
    SERVER_INTERNAL_FUNCTIONS,

    /**
     * If the internal features of the client console should be activated.
     */
    CLIENT_INTERNAL_FUNCTIONS(false),

    /**
     * Writes the debug filelist CSV into debug directory
     */
    DEBUG_WRITE_FILELIST_CSV(false);

    private static final Logger log = Logger.getLogger(Feature.class.getName());

    private boolean defValue;
    private Boolean enabled;

    private Feature(boolean enabled) {
        defValue = enabled;
    }

    private Feature() {
        this(true);
    }

    public void disable() {
        log.fine(name() + " disabled");
        System.setProperty("powerfolder.feature." + name(), "disabled");
        enabled = false;
    }

    public void enable() {
        log.fine(name() + " enabled");
        System.setProperty("powerfolder.feature." + name(), "enabled");
        enabled = true;
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public boolean isEnabled() {
        if (enabled == null) {
            enabled = "enabled".equalsIgnoreCase(System.getProperty(
                "powerfolder.feature." + name(), defValue
                    ? "enabled"
                    : "disabled"));
        }
        return enabled;
    }

    public static void setupForTests() {
        for (Feature feature : values()) {
            feature.disable();
        }
        Feature.DETECT_UPDATE_BY_VERSION.enable();
        Feature.CORRECT_MOVEMENT_DETECTION.enable();
    }
}
