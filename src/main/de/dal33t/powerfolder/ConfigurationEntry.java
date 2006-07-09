/* $Id$
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Refelects a entry setting in the configuration file. Provides basic method
 * for accessing and setting the configuration.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum ConfigurationEntry {
    /**
     * The nickname to use.
     */
    NICK("nick", System.getProperty("user.name")),

    /**
     * The node id to use. Advanced entry, usually automatically generated and
     * stored in preferences.
     */
    NODE_ID("nodeid"),

    /**
     * The id of the master node.
     */
    MASTER_NODE_ID("masternodeid"),

    /**
     * The networking mode. See class <code>NetworkingMode</code> for more
     * information.
     * 
     * @see NetworkingMode
     */
    NETWORKING_MODE("networkingmode", NetworkingMode.PRIVATEMODE.name()),

    /**
     * The ip/address where powerfolder should bind to.
     */
    NET_BIND_ADDRESS("net.bindaddress"),

    /**
     * The port(s) to bind to.
     */
    NET_BIND_PORT("port"),

    /**
     * The maximum number of concurrent uploads.
     */
    UPLOADS_MAX_CONCURRENT("uploads", "5"),

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
    UPLOADLIMIT_SILENTMODE_THROTTLE("net.silentmodethrottle"),

    /**
     * My dynamic dns hostname or fix ip.
     */
    DYNDNS_HOSTNAME("mydyndns"),

    /**
     * Setting to enable/disable zip compression on LAN
     */
    USE_ZIP_ON_LAN("use_zip_on_lan", Boolean.FALSE.toString()),

    /**
     * The basedir for all powerfolder.
     */
    FOLDER_BASEDIR("foldersbase", System.getProperty("user.home")
        + System.getProperty("file.separator") + "PowerFolders"),

    /**
     * Contains a comma-separated list of all plugins to load.
     */
    PLUGINS("plugins"),

    /**
     * Contains a comma-separated list of all plugins, which are disabled.
     */
    PLUGINS_DISABLED("plugins.disabled"),

    /**
     * Flag if update at start should performed.
     */
    DYNDNS_UPDATE_ON_START("onStartUpdate", Boolean.FALSE.toString()),

    /**
     * The username to use for the dyndns update.
     */
    DYNDNS_USERNAME("dyndnsUserName"),

    /**
     * The password to use for the dyndns update.
     */
    DYNDNS_PASSWORD("dyndnsPassword"),

    /**
     * The ip of the last dyndns update.
     */
    DYNDNS_LAST_UPDATED_UP("lastUpdatedIP"),

    /**
     * Settings if running in backup server mode.
     */
    BACKUP_SERVER("backupserver", Boolean.FALSE.toString());

    // Methods/Constructors ***************************************************

    private static final Logger LOG = Logger
        .getLogger(ConfigurationEntry.class);

    private String configKey;
    private String defaultValue;

    private ConfigurationEntry(String aConfigKey) {
        this(aConfigKey, null);
    }

    private ConfigurationEntry(String aConfigKey, String theDefaultValue) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        configKey = aConfigKey;
        defaultValue = theDefaultValue;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @param defaultValue
     *            the default value to use if the entry does not exist
     * @return The current value from the configuration for this entry. or
     *         default value if not existing
     */
    public String getValue(Controller controller) {
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
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Integer getValueInt(Controller controller) {
        String value = getValue(controller);
        if (value == null) {
            value = defaultValue;
        }
        try {
            return new Integer(value);
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse configuration entry '" + configKey
                + "' into a int. Value: " + value, e);
            return new Integer(defaultValue);
        }
    }

    /**
     * Parses the configuration entry into a Boolen.
     * 
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or the
     *         default value if value not set/unparseable.
     */
    public Boolean getValueBoolean(Controller controller) {
        String value = getValue(controller);
        if (value == null) {
            value = defaultValue;
        }
        try {
            return Boolean.valueOf(value.equalsIgnoreCase("true"));
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse configuration entry '" + configKey
                + "' into a boolean. Value: " + value, e);
            return Boolean.valueOf(defaultValue.equalsIgnoreCase("true"));
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
