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
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.JavaVersion;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Central constants holder for all important constants in PowerFolder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.29 $
 */
public class Constants {

    // General settings *******************************************************
    /**
     * The name of the subdirectory in every folder to store powerfolder
     * relevant files.
     */
    public static final String POWERFOLDER_SYSTEM_SUBDIR;
    static {
        POWERFOLDER_SYSTEM_SUBDIR = System.getProperty("pf.syssubdir",
            ".PowerFolder");
    }

    /** Default config name */
    public static final String DEFAULT_CONFIG_FILE;
    static {
        DEFAULT_CONFIG_FILE = System.getProperty("pf.defconfig",
            "PowerFolder.config");
    }

    /**
     * The directory name the located the misc/config dir in. e.g.
     * user.home/.PowerFolder
     */
    public static final String MISC_DIR_NAME;
    static {
        MISC_DIR_NAME = System.getProperty("pf.configdir", "PowerFolder");
    }

    public static final String DB_FILENAME;
    public static final String DB_BACKUP_FILENAME;
    static {
        DB_FILENAME = System.getProperty("pf.dbfilename", ".PowerFolder.db");
        DB_BACKUP_FILENAME = DB_FILENAME + ".bak";
    }

    /**
     * The name of the 'meta' subdirectory, home of the metaFolder files.
     */
    public static final String METAFOLDER_SUBDIR = "meta";

    public static final String SYSTEM_SUBDIR = "foldermeta";

    /**
     * The prefix for meta folder IDs.
     */
    public static final String METAFOLDER_ID_PREFIX = "meta|";

    /**
     * The subdir name that will be used by default for the PowerFolders base
     * directory.
     *
     * @see ConfigurationEntry#FOLDER_BASEDIR
     */
    public static String FOLDERS_BASE_DIR_SUBDIR_NAME = "PowerFolders";
    
    /**
     * PFC-2538
     */
    public static final String GETTING_STARTED_GUIDE_FILENAME = "Getting started.pdf";

    /**
     * #2056: Temporary directory to download the updates before performing an
     * atomic commit.
     */
    public static final String ATOMIC_COMMIT_TEMP_TARGET_DIR = ".temp-dir";

    /**
     * The URL where to check the connectivity with.
     */
    public static final String LIMITED_CONNECTIVITY_CHECK_URL = "http://checkconnectivity.powerfolder.com/check.php";

    /**
     * Check for updates every hour.
     */
    public static final int UPDATE_CHECK_PERIOD_MINUTES = 60;

    /**
     * Threads in threadpool of {@link Controller}
     */
    public static final int CONTROLLER_THREADS_IN_THREADPOOL = 5;

    /**
     * The number of pixels to stay away from the screen border by default.
     */
    public static final int UI_DEFAULT_SCREEN_BORDER = 50;

    /**
     * The minimum supported version for AWTUtilities.setWindowOpacity is
     * 1.6.0_10-b12. And not in Linux, but okay in Mac and Windows.
     */
    public static final boolean OPACITY_SUPPORTED = !OSUtil.isLinux()
        && JavaVersion.systemVersion().compareTo(
            new JavaVersion(1, 6, 0, 10, 12)) >= 0;

    // Network architecture constants ******************************************

    /**
     * PFC-2455: Network ID to set to connect to clients with any network ID
     */
    public static final String NETWORK_ID_ANY = "ANY";

    /**
     * The maximum number of files on a FileList. If list ist greater, it is
     * splitted into smaller ones
     */
    public static final int FILE_LIST_MAX_FILES_PER_MESSAGE = 500;

    /**
     * The number of supernodes to connect if NOT running as supernode.
     */
    public static final int N_SUPERNODES_TO_CONNECT = 3;

    /**
     * The number of supernodes to contact when performing a nodes search
     */
    public static final int N_SUPERNODES_TO_CONTACT_FOR_NODE_SEARCH = 4;

    /**
     * The number of lan nodes to contact when performing a nodes search
     */
    public static final int N_LAN_NODES_TO_CONTACT_FOR_NODE_SEARCH = 4;

    /**
     * The number of supernodes to contact to request the full list of nodes.
     */
    public static final int N_SUPERNODES_TO_CONTACT_FOR_NODE_LIST = 4;

    /**
     * The number of seconds until nodelist is requested. Currently: 8 minutes
     */
    public static final int NODE_LIST_REQUEST_INTERVAL = 8 * 60;

    /** The number of seconds until a new transfer status is broadcasted */
    public static final int TRANSFER_STATUS_BROADCAST_INTERVAL = 10 * 60;

    /**
     * The time in seconds between broadcast of those nodes, that went online.
     * Currently: every minute.
     */
    public static final long NODES_THAN_WENT_ONLINE_BROADCAST_TIME = 60;

    /**
     * The maximum number of nodes i a message allowed.
     */
    public static final int NODES_LIST_MAX_NODES_PER_MESSAGE = 500;

    /**
     * The maximum numbers of nodes to connection per kilobytes/s upload in
     * supernode mode
     */
    public static final double MAX_NODES_CONNECTIONS_PER_KBS_UPLOAD = 1;

    /**
     * The maximum time for a node to be offline, util no reconnection will be
     * tried. Currently: 10 hours
     */
    public static final long MAX_NODE_OFFLINE_TIME = 10L * 60 * 60 * 1000;

    /** Time until total node invalidation (if not friend). Currently: 60 days */
    public static final long NODE_TIME_TO_INVALIDATE = 1000L * 60 * 60 * 24 * 60;

    /** Time a member is offline to get removed from a folder display */
    public static final long NODE_TIME_TO_REMOVE_MEMBER = 1000 * 60 * 60 * 24
        * 15;
    /**
     * Maximum time shift before node gets invalid in the future of the last
     * connection time. Currently: 1 day
     */
    public static final long NODE_TIME_MAX_IN_FUTURE = 1000 * 60 * 60 * 24;

    /** The min number of reconnectors to spawn */
    public static final int MIN_NUMBER_RECONNECTORS = 2;

    /** The max number of reconnectors to spawn */
    public static final int MAX_NUMBER_RECONNECTORS = 5;

    /**
     * the number of seconds (aprox) of delay till the connection is tested and
     * a warning may be displayed.
     */
    public static final long LIMITED_CONNECTIVITY_CHECK_DELAY = 30;

    /**
     * The delay before checking for unsynchronized folders
     */
    public static final long FOLDER_UNSYNCED_CHECK_DELAY = 120;

    /**
     * the number of seconds (aprox) between request for hosting servers for our
     * joined folders. About 10 minutes.
     */
    public static final long HOSTING_FOLDERS_REQUEST_INTERVAL = 600;

    /**
     * The timeout for a service request in seconds
     */
    public static final long REQUEST_RESPONSE_TIMEOUT = 60;

    // Basic networking options ***********************************************

    /**
     * The time in ms, how long the timeout for the socket connect method should
     * be used. default: 30s
     */
    public static final int SOCKET_CONNECT_TIMEOUT = 30 * 1000;

    /**
     * The number of incoming connections to queue until the connection is
     * throttled. Socket backlog is two times this value.
     */
    public static final int MAX_INCOMING_CONNECTIONS = 50;

    /**
     * The time interval in seconds when the incoming connections should be
     * checked
     */
    public static final long INCOMING_CONNECTION_CHECK_TIME = 60;

    /**
     * The number of seconds with no-response until a connection times out.
     */
    public static final long CONNECTION_KEEP_ALIVE_TIMOUT = 2 * 60;

    /**
     * The time interval to resize the reconnector pool in seconds.
     */
    public static final long RECONNECTOR_POOL_SIZE_RESIZE_TIME = 120;

    // UDT stuff **************************************************************
    /**
     * Maximal pending udt connection count
     */
    public static final int MAX_PENDING_UDT_CONNECTIONS = 10;

    /**
     * TimeOut for pending UDT connections
     */
    public static final long TO_UDT_CONNECTION = 30 * 1000;

    // Transfer settings ******************************************************

    /**
     * The maximum number of enqued download from a internet connected node
     */
    public static final int MAX_DLS_FROM_INET_MEMBER = 10;

    /**
     * Start uploads until 500kb is started in uploads
     */
    public static final long START_UPLOADS_TILL_PLANNED_SIZE_INET = 500 * 1024;

    /**
     * The maximum number of enqued download from a lan connected node
     */
    public static final int MAX_DLS_FROM_LAN_MEMBER = 50;

    /**
     * Start uploads until 3mb is started in uploads
     */
    public static final long START_UPLOADS_TILL_PLANNED_SIZE_LAN = 3 * 1024 * 1024;

    /**
     * The download timeout of a request. After that time of inactivity the
     * download is assumed to be timed out. -> Aborts the download.
     */
    public static final long DOWNLOAD_REQUEST_TIMEOUT_LIMIT = 3L * 60 * 1000;

    /**
     * The upload timeout for request if not remote hashing. After the time of
     * inactivity the upload is assumed to be timed out.
     */
    public static final long UPLOAD_REQUEST_TIMEOUT = 3L * 60 * 1000;

    /**
     * The upload timeout during remote hashing. After the time of inactivity
     * the upload is assumed to be timed out. This value is very large since
     * delta sync allows for quite alot of time to pass before requesting. (time
     * it takes to hash)
     */
    public static final long UPLOAD_REMOTEHASHING_PART_REQUEST_TIMEOUT = 5L * 60 * 60 * 1000;

    /**
     * 100KB file minimum size for DELTA sync.
     */
    public static final long DELTA_SYNC_MIN_FILESIZE = 50 * 1024;

    // ConnectionHandler constants ********************************************

    public static final int MIN_SIZE_FOR_PARTTRANSFERS = 8 * 1024;

    // Time estimation constants

    public static final long ESTIMATION_WINDOW_MILLIS = 300 * 1000;

    public static final int ESTIMATION_MINVALUES = 5;

    // Time constants

    /**
     * Number of milliseconds in a standard second.
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_SECOND = 1000;
    /**
     * Number of milliseconds in a standard minute.
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    /**
     * Number of milliseconds in a standard hour.
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    /**
     * Number of milliseconds in a standard day.
     *
     * @since 2.1
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    // Pro related ************************************************************

    public static final String PACKAGE_PREFIX = "de.dal33t.powerfolder.";

    public static final String PRO_LOADER_PLUGIN_CLASS = "CD";

    public static final String ENCRYPTION_PLUGIN_CLASS = "BC";

    public static final String SESSIONS_SUB_DIR = "database/sessions";

    public static final String SHIBBOLETH_USERNAME_SEPARATOR = "!";

    // Web stuff **************************************************************

    public static final String UI_LOCK_UNLOCK_URI = "/unlock";
    public static final String LOGIN_URI = "/login";
    public static final String REGISTER_URI = "/register";
    public static final String ACTIVATE_URI = "/activate";
    public static final String GET_LINK_URI = "/getlink";
    public static final String DL_LINK_URI = "/dl";
    public static final String OPEN_LINK_URI = "/open";
    public static final String LOGIN_SHIBBOLETH_URI = LOGIN_URI + "/shibboleth";
    public static final String LOGIN_SHIBBOLETH_CLIENT_URI = LOGIN_SHIBBOLETH_URI
        + "/client";

    public static final String LOGIN_PARAM_USERNAME = "Username";
    public static final String LOGIN_PARAM_PASSWORD = "Password";
    public static final String LOGIN_PARAM_PASSWORD_OBF = "PasswordOBF";
    public static final String LOGIN_PARAM_ORIGINAL_URI = "originalURI";
    public static final String MY_ACCOUNT_URI = "/myaccount";

    /** Cleanup immediately, 1, 10, 30, never days. */
    public static final int[] CLEANUP_VALUES = {0, 1, 10, 30, Integer.MAX_VALUE};

    public static final int DEFAULT_NORMAL_DOCKED_WIDTH = 1100;
    public static final int DEFAULT_NORMAL_HEIGHT = 600;

    public static final String LINK_EXTENSION = ".lnk";

    // Zyncro related *********************************************************

    public static final String ZYNCRO_SCHEME = "zyncro";
    public static final String ZYNCRO_GROUP_TOKEN = "$group ";
    public static final String ZYNCRO_DEPARTMENT_TOKEN = "$department ";
    public static final String ZYNCRO_COMPANY_TOKEN = "$company ";
    public static final String FOLDER_PERSONAL_FILES = "$personal_files";
    public static final String FOLDER_PUBLIC_SHARED_FILES = "$public_shared_files";
    
    // Locking ****************************************************************
    
    public static final String MS_OFFICE_FILENAME_PREFIX = "~$";
    public static final String LIBRE_OFFICE_FILENAME_PREFIX = ".~lock.";

    private Constants() {
        // No instance allowed
    }
}
