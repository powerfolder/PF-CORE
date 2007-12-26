package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;

/**
 * Available features to enable/disable. Primary for testing.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum Feature {

    OS_CLIENT, EXIT_ON_SHUTDOWN,

    /**
     * If disabled all peers will be detected as on lan.
     */
    CORRECT_LAN_DETECTION,

    REMIND_COMPLETED_DOWNLOADS,

    /**
     * If file updates get detected newer using the version counter. Otherwise
     * the last modification date is uesd.
     * FIXME: Remove, Customer prototype
     */
    DETECT_UPDATE_BY_VERSION,
    
    /**
     * Use the usual folder scan time. if disabled all folders get scanned AS FAST AS POSSIBLE!!
     * FIXME: Remove, Customer prototype
     */
    SYNC_PROFILE_CONTROLLER_FOLDER_SCAN_TIMING;
    
    private static final Logger LOG = Logger.getLogger(Feature.class);

    public void disable() {
        LOG.warn(name() + " disabled");
        System.setProperty("powerfolder.feature." + name(), "XXX");
    }

    public void enable() {
        LOG.warn(name() + " enabled");
        System.setProperty("powerfolder.feature." + name(), "OK");
    }

    public boolean isDisabled() {
        return "XXX"
            .equals(System.getProperty("powerfolder.feature." + name()));
    }

    public boolean isEnabled() {
        return !isDisabled();
    }

    public static void setupForTests() {
        for (Feature feature : values()) {
            feature.disable();
        }
        Feature.REMIND_COMPLETED_DOWNLOADS.enable();
        Feature.DETECT_UPDATE_BY_VERSION.enable();
        Feature.SYNC_PROFILE_CONTROLLER_FOLDER_SCAN_TIMING.enable();
    }
}
