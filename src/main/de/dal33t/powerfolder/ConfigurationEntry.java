/* $Id$
 */
package de.dal33t.powerfolder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

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
     * If true, powerfolder tries to open it's ports on the firewall. (It also
     * will try to close them when exiting)
     */
    NET_FIREWALL_OPENPORT("net.openport", Boolean.TRUE.toString()),

    /**
     * Use a random port in the (49152) 0 to 65535 range, overides NET_BIND_PORT
     */
    NET_BIND_RANDOM_PORT("random-port", Boolean.TRUE.toString()),

    /**
     * The maximum number of concurrent uploads.
     */
    UPLOADS_MAX_CONCURRENT("uploads", "10"),

    /**
     * The upload limit for WAN (Internet) connections in KB/s
     */
    UPLOADLIMIT_WAN("uploadlimit", "0"),

    /**
     * The download limit for WAN (Internet) connections in KB/s
     */
    DOWNLOADLIMIT_WAN("downloadlimit", "0"),

    /**
     * The upload limit for LAN connections in KB/s
     */
    UPLOADLIMIT_LAN("lanuploadlimit", "0"),

    /**
     * The download limit for LAN connections in KB/s
     */
    DOWNLOADLIMIT_LAN("landownloadlimit", "0"),

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
    DYNDNS_AUTO_UPDATE("dyndns.autoUpdate", Boolean.FALSE.toString()) {

        @Override
        public String getValue(Controller controller) {
            String value = super.getValue(controller);
            if (value == null) {
                value = controller.getConfig().getProperty("onStartUpdate");
            }
            return value != null ? value : Boolean.FALSE.toString();
        }

    },

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
    DYNDNS_LAST_UPDATED_IP("lastUpdatedIP"),

    /**
     * Settings if running in backup server mode.
     */
    BACKUP_SERVER("backupserver", Boolean.FALSE.toString()),

    /**
     * Comma-seperated list of ip-ranges that are (forced) in our LAN.
     */
    LANLIST("lanlist", ""),

    /**
     * The username/account of the webservice
     */
    WEBSERVICE_USERNAME("webservice.username"),

    /**
     * The password of the webservice
     */
    WEBSERVICE_PASSWORD("webservice.password"),

    /**
     * Whether to use the recycle bin by default.
     */
    USE_RECYCLE_BIN("use.recycle.bin", Boolean.TRUE.toString()),

    /**
     * Whether to log verbose.
     */
    VERBOSE("verbose", Boolean.FALSE.toString()),

    /**
     * Whether to request debug reports
     * 
     * @see de.dal33t.powerfolder.message.RequestNodeInformation
     */
    DEBUG_REPORTS("debug.reports", Boolean.FALSE.toString()),

    /**
     * Delta-sync: Enable/Disable it.
     */
    TRANSFER_SUPPORTS_PARTTRANSFERS("transfer.parttransfers", Boolean.TRUE
        .toString()),

    /**
     * #593 Ugly workaround for #378. Remove empty directories.
     */
    DELETE_EMPTY_DIRECTORIES("delete.empty.dirs", Boolean.FALSE.toString()),

    /**
     * If it should be automatically connected to other nodes.
     * FIX: Currently only affects ReconnectManager.
     */
    AUTO_CONNECT("auto.connect", Boolean.TRUE.toString()),

    /**
     * Which navigation node should be selected initially?
     */
    START_PANEL("start.panel", StartPanel.OVERVIEW.getName());

    // Methods/Constructors ***************************************************

    private static final Logger LOG = Logger
        .getLogger(ConfigurationEntry.class);

    private String configKey;
    private String defaultValue;

    ConfigurationEntry(String aConfigKey) {
        this(aConfigKey, null);
    }

    ConfigurationEntry(String aConfigKey, String theDefaultValue) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        configKey = aConfigKey;
        defaultValue = theDefaultValue;
    }

    /**
     * @param controller
     *            the controller to read the config from
     * @return The current value from the configuration for this entry. or
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
            return value.equalsIgnoreCase("true");
        } catch (NumberFormatException e) {
            LOG.warn("Unable to parse configuration entry '" + configKey
                + "' into a boolean. Value: " + value, e);
            return defaultValue.equalsIgnoreCase("true");
        }
    }

    /**
     * Creates a model containing the value of the configuration entry.
     * <p>
     * Changes from "below" won't be reflected.
     * 
     * @param controller
     * @return a value model bound to the configuration entry.
     */
    public ValueModel getModel(final Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        ValueModel model = new ValueHolder(getValue(controller), false);
        model.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setValue(controller, (String) evt.getNewValue());
            }
        });
        return model;
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
