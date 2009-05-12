/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Refelects a entry setting in the configuration file. Provides basic method
 * for accessing and setting the configuration.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author Dennis "Bytekeeper" Waldherr
 * @version $Revision: 1.5 $
 */
public enum ConfigurationEntry {

    // Basics *****************************************************************

    /**
     * If the GUI should be disabled (=console mode). Default: False (Show GUI).
     */
    DISABLE_GUI("disableui", Boolean.FALSE.toString()),

    // Node setup *************************************************************
    
    /**
     * The nickname to use.
     */
    NICK("nick", System.getenv("COMPUTERNAME") != null ? System.getenv(
        "COMPUTERNAME").toLowerCase() : System.getProperty("user.name")),

    /**
     * The node id to use. Advanced entry, usually automatically generated and
     * stored in preferences.
     */
    NODE_ID("nodeid"),
    
    /**
     * The network ID (#1373). PowerFolder can separate logical peer-to-peer
     * networks. Nodes with different network IDs won't connect to each other.
     * They even don't have other nodes in its local peer-to-peer nodes
     * database.
     * <P>
     * The default network ID of the open PowerFolder network is X.
     */
    NETWORK_ID("networkid", "X"),

    // Provider Settings ******************************************************

    /**
     * URL of the PowerFolder homepage
     */
    PROVIDER_URL(
        "provider.url.main",
        "http://www.powerfolder.com/?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * URL of the Online Storage features
     */
    PROVIDER_ABOUT_URL(
        "provider.url.about",
        "http://www.powerfolder.com/online_storage_features.html?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * Quickstart guides to PowerFolder
     */
    PROVIDER_QUICKSTART_URL(
        "provider.url.quickstart",
        "http://www.powerfolder.com/quickstart.html?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * URL of the PowerFolder Support
     */
    PROVIDER_SUPPORT_URL(
        "provider.url.support",
        "http://www.powerfolder.com/support.html?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * URL where bugs or tickets can be filed.
     */
    PROVIDER_SUPPORT_FILE_TICKET_URL(
        "provider.url.ticket",
        "http://www.powerfolder.com/support/index.php?_m=tickets&_a=submit&step=1&departmentid=4"),

    /**
     * URL of the PowerFolder Pro page
     */
    PROVIDER_BUY_URL("provider.url.buy",
        "http://www.powerfolder.com/buynow.html?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * URL where the contact form resides
     */
    PROVIDER_CONTACT_URL(
        "provider.url.contact",
        "http://www.powerfolder.com/contact.html?utm_source=client&utm_medium=client&utm_campaign=Client"),

    /**
     * URL of the PowerFolder Wiki. ATTENTION: This URL gets extended by article
     * URI from many help links
     */
    PROVIDER_WIKI_URL("provider.url.wiki", "http://www.powerfolder.com/wiki"),

    /**
     * HTTP tunnel relay URL
     */
    PROVIDER_HTTP_TUNNEL_RPC_URL("provider.url.httptunnel",
        "http://relay.powerfolder.com/rpc"),

    // Server settings ********************************************************

    /**
     * The optional name of the sever to connect to.
     */
    SERVER_NAME("server.name", "Online Storage"),
    
    /**
     * The optional url of the server.
     */
    SERVER_WEB_URL("server.url", "https://access.powerfolder.com"),

    /**
     * The node id of the server to connect to. Not mandatory but at
     * recommended. At leat host or ID of a server has to be set to connect to a
     * server.
     */
    SERVER_NODEID("server.nodeid", "WEBSERVICE03"),

    /**
     * The optional server hostname to connect to. Example:
     * server.powerfolder.com:1234
     */
    SERVER_HOST("server.host", "access.powerfolder.com:1337"),
    
    // General settings *******************************************************

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
     * The TCP/IP socket buffer size for TCP/UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_SIZE("net.socket.internet.buffer.size", String
        .valueOf(16 * 1024)),

    /**
     * The TCP/IP socket buffer size for TCP/UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_SIZE("net.socket.lan.buffer.size", String
        .valueOf(64 * 1024)),

    /**
     * The TCP/IP socket buffer size limit for UDT connections over Internet.
     */
    NET_SOCKET_INTERNET_BUFFER_LIMIT("net.socket.internet.buffer.limit", String
        .valueOf(256 * 1024)),

    /**
     * The TCP/IP socket buffer size limit for UDT connections in LAN.
     */
    NET_SOCKET_LAN_BUFFER_LIMIT("net.socket.lan.buffer.limit", String
        .valueOf(1024 * 1024)),

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
     * The maximum size (in bytes) of an {@link FileChunk} used for file transfers
     */
    TRANSFERS_MAX_FILE_CHUNK_SIZE("transfers.max.file.chunk.size", String
        .valueOf(32 * 1024)),

    /**
     * The maximum number of queued request for {@link FileChunk}s
     */
    TRANSFERS_MAX_REQUESTS_QUEUED("transfers.max.request.queued", String
        .valueOf(15)),

    /**
     * My dynamic dns hostname or fix ip.
     */
    DYNDNS_HOSTNAME("mydyndns"),

    /**
     * Setting to enable/disable zip compression on LAN
     */
    USE_ZIP_ON_LAN("use_zip_on_lan", Boolean.FALSE.toString()),

    /**
     * Setting to enable/disable swarming in an LAN environment. If swarming
     * below is set to false, this is ignored!
     */
    USE_SWARMING_ON_LAN("swarming.lan.enabled", "false"),

    /**
     * Delta-sync: Enable/Disable it.
     */
    USE_DELTA_ON_INTERNET("deltasync.internet.enabled", Boolean.TRUE.toString()),

    USE_DELTA_ON_LAN("deltasync.lan.enabled", Boolean.FALSE.toString()),

    /**
     * Setting to enable/disable swarming.
     */
    USE_SWARMING_ON_INTERNET("swarming.internet.enabled", "true"),

    /**
     * The basedir for all powerfolder.
     */
    FOLDER_BASEDIR("foldersbase") {
        @Override
        protected void setDefaults() {
            if (OSUtil.isWindowsSystem() && !OSUtil.isWindowsVistaSystem()) {
                WinUtils util = WinUtils.getInstance();
                if (util != null) {
                    defaultValue = util.getSystemFolderPath(
                        WinUtils.CSIDL_PERSONAL, false)
                        + System.getProperty("file.separator") + "PowerFolders";
                    return;
                }
            }
            // Also place the base dir into user home on Vista.
            defaultValue = System.getProperty("user.home")
                + System.getProperty("file.separator") + "PowerFolders";
        }
    },

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
     * Whether to use the PowerFolder icon in Windows Explorer folders.
     */
    USE_PF_ICON("use.pf.icon", Boolean.TRUE.toString()),

    /**
     * Whether to show chat notifications when minimized.
     */
    SHOW_CHAT_NOTIFICATIONS("show.chat.notifications", Boolean.TRUE.toString()),

    /**
     * Whether to show system notifications when minimized.
     */
    SHOW_SYSTEM_NOTIFICATIONS("show.system.notifications", Boolean.TRUE
        .toString()),

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
     * Whether to show dialog testing panel
     */
    DIALOG_TESTING("dialog.testing", Boolean.FALSE.toString()),

    /**
     * #593 Ugly workaround for #378. Remove empty directories.
     */
    DELETE_EMPTY_DIRECTORIES("delete.empty.dirs", Boolean.FALSE.toString()),

    /**
     * Whether to do auto-cleanup for downloads.
     */
    DOWNLOADS_AUTO_CLEANUP("downloads.auto.cleanup", Boolean.TRUE.toString()),

    /**
     * Whether to do auto-cleanup for uploads.
     */
    UPLOADS_AUTO_CLEANUP("uploads.auto.cleanup", Boolean.TRUE.toString()),

    /**
     * If it should be automatically connected to other nodes. FIX: Currently
     * only affects ReconnectManager.
     */
    AUTO_CONNECT("auto.connect", Boolean.TRUE.toString()),

    /**
     * Enable/Disable relayed connections.
     */
    RELAYED_CONNECTIONS_ENABLED("connections.relayed", Boolean.TRUE.toString()),

    /**
     * Enable/Disable relayed connections.
     */
    UDT_CONNECTIONS_ENABLED("connections.udt", Boolean.TRUE.toString()),

    /**
     * Enable/Disable node manager (for debugging only)
     */
    NODEMANAGER_ENABLED("nodemanager.enabled", Boolean.TRUE.toString()),

    /**
     * Enable/Disable transfermanager (for debugging only)
     */
    TRANSFER_MANAGER_ENABLED("transfermanager.enabled", Boolean.TRUE.toString()),

    /**
     * Enable/Disable folder repo (for debugging only)
     */
    FOLDER_REPOSITORY_ENABLED("folderepository.enabled", Boolean.TRUE
        .toString()),

    /**
     * Whether to show preview folders in nav / folders panles.
     */
    HIDE_PREVIEW_FOLDERS("show.preview.folders", Boolean.FALSE.toString()),
    
    /**
     * The number of seconds to wait to recalc a {@link FolderStatistic} when a
     * change to the folder happend. Applies to large folders only, that host
     * more files than {@link FolderStatistic#MAX_ITEMS}
     */
    FOLDER_STATS_CALC_TIME("filedb.stats.seconds", String.valueOf(10)),

    /**
     * The maximum time powerfolder keeps the folder database dirty in memory
     * before writing it to disk in seconds.
     */
    FOLDER_DB_PERSIST_TIME("filedb.persist.seconds", String.valueOf(5)),

    /**
     * The number of seconds between db maintenance scans (30 minutes).
     */
    DB_MAINTENANCE_SECONDS("filedb.maintenance.seconds", String.valueOf(1800)),

    /**
     * The age of a deleted file until it gets removed by the folder db
     * maintenance. In Seconds! Default: 1 year
     */
    MAX_FILEINFO_DELETED_AGE_SECONDS("filedb.deleted.maxage", "" + 60L * 60
        * 24 * 365),

    /**
     * The http proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_HOST("http.proxy.host"),

    /**
     * The http proxy port to use for HTTP tunneled connections
     */
    HTTP_PROXY_PORT("http.proxy.port", "" + 80),

    /**
     * The http proxy username to use for HTTP tunneled connections
     */
    HTTP_PROXY_USERNAME("http.proxy.username"),

    /**
     * The http password proxy to use for HTTP tunneled connections
     */
    HTTP_PROXY_PASSWORD("http.proxy.password");

    // Methods/Constructors ***************************************************

    private static final Logger LOG = Logger.getLogger(ConfigurationEntry.class
        .getName());

    private String configKey;
    protected String defaultValue;

    ConfigurationEntry(String aConfigKey) {
        this(aConfigKey, null);
    }

    ConfigurationEntry(String aConfigKey, String theDefaultValue) {
        Reject.ifBlank(aConfigKey, "Config key is blank");
        configKey = aConfigKey;
        defaultValue = theDefaultValue;

        if (theDefaultValue == null) {
            setDefaults();
        }
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
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                + configKey + "' into a int. Value: " + value, e);
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
            LOG.log(Level.WARNING, "Unable to parse configuration entry '"
                + configKey + "' into a boolean. Value: " + value, e);
            return defaultValue.equalsIgnoreCase("true");
        }
    }

    /**
     * Creates a model containing the value of the configuration entry.
     * <p>
     * Changes from "below" won't be reflected.
     * <p>
     * TODO Resolve problem: Model not buffered!
     * 
     * @param controller
     * @return a value model bound to the configuration entry.
     * @deprecated do not use util problems are resolved
     */
    @Deprecated
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

    /**
     * @return the default value for this config entry.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the key in config
     */
    public String getConfigKey() {
        return configKey;
    }

    protected void setDefaults() {
    }
}
