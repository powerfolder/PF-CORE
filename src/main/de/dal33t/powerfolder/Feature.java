package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;

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
     * If disabled all peers will be detected as on lan.
     */
    CORRECT_LAN_DETECTION,

    /**
     * Customer requirement.
     * <p>
     * #798
     * <p>
     */
    HIGH_FREQUENT_FOLDER_DB_MAINTENANCE(false),

    /**
     * If file updates get detected newer using the version counter. Otherwise
     * the last modification date is uesd.
     * <P>
     * #658
     * <P>
     */
    DETECT_UPDATE_BY_VERSION;

    private static final Logger LOG = Logger.getLogger(Feature.class);
    private boolean defValue;
    private Boolean enabled;

    private Feature(boolean enabled) {
        defValue = enabled;
    }

    private Feature() {
        this(true);
    }

    public void disable() {
        LOG.warn(name() + " disabled");
        System.setProperty("powerfolder.feature." + name(), "disabled");
        enabled = false;
    }

    public void enable() {
        LOG.warn(name() + " enabled");
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
    }
}
