/* $Id$
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Refelects a entry setting in the configuration file. Provides basic method
 * for accessing and setting configuration settings.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum ConfigurationEntry {
    /**
     * The nickname to use.
     */
    NICK("nick"),

    /**
     * The node id to use. Advanced entry, usually automatically generated and
     * stored in preferences.
     */
    NODE_ID("nodeid"),

    /**
     * The ip/address where powerfolder should bind to.
     */
    NET_BIND_ADDRESS("net.bindaddress"),

    /**
     * The port(s) powerfolder should bind to.
     */
    NET_BIND_PORT("port"),

    /**
     * The upload limit for WAN (Internet) connections.
     */
    UPLOADLIMIT_WAN("uploadlimit"),

    /**
     * The upload limit for LAN connections.
     */
    UPLOADLIMIT_LAN("lanuploadlimit"),

    /**
     * The percentage to throttle the uploadlimits in silentmode.
     */
    UPLOADLIMIT_SILENTMODE_THROTTLE("net.silentmodethrottle");

    // Methods/Constructors ***************************************************
    private static final Logger LOG = Logger
        .getLogger(ConfigurationEntry.class);

    private String configKey;

    private ConfigurationEntry(String aConfigKey) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        configKey = aConfigKey;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or null
     *         if not existing
     */
    public String getValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        return getValue(controller, null);
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @param defaultValue
     *            the default value to use if the entry does not exist
     * @return The current value from the configuration for this entry. or
     *         default value if not existing
     */
    public String getValue(Controller controller, String defaultValue) {
        Reject.ifNull(controller, "Controller is null");
        String value = controller.getConfig().getProperty(configKey);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Parses the configuration entry into a Integer.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or null
     *         if not existing/not parsable
     */
    public Integer getValueInt(Controller controller) {
        String value = getValue(controller);
        if (value == null) {
            return null;
        }
        try {
            return new Integer(value);
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse configuration entry '" + configKey
                + "' into a int. Value: " + value, e);
            return null;
        }
    }

    /**
     * Sets the value of this config entry.
     * 
     * @param controller
     *            the controller of the config
     * @param value
     *            the value to set
     */
    public void setValue(Controller controller, String value) {
        Reject.ifNull(controller, "Controller is null");
        controller.getConfig().setProperty(configKey, value);
    }

    /**
     * Removes the entry from the configuration.
     * 
     * @param controller
     *            the controller to use
     */
    public void removeValue(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        controller.getConfig().remove(configKey);
    }
}
