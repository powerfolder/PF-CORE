package de.dal33t.powerfolder;

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

    REMIND_COMPLETED_DOWNLOADS;

    public void disable() {
        System.setProperty("powerfolder.feature." + name(), "XXX");
    }

    public void enable() {
        System.setProperty("powerfolder.feature." + name(), "OK");
    }

    public boolean isDisabled() {
        return "XXX"
            .equals(System.getProperty("powerfolder.feature." + name()));
    }

    public boolean isEnabled() {
        return !isDisabled();
    }

    public static void disableAll() {
        for (Feature feature : values()) {
            feature.disable();
        }
    }
}
