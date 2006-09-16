/* $Id: Constants.java,v 1.29 2006/04/23 16:01:35 totmacherr Exp $
 */
package de.dal33t.powerfolder;

/**
 * Central constants holder for all important constants in PowerFolder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.29 $
 */
public class Constants {
    // General settings *******************************************************

    /**
     * URL of the PowerFolder homepage
     */
    public static final String POWERFOLDER_URL = "http://www.powerfolder.com";

    /**
     * The name of the subdirectory in every folder to store powerfolder
     * relevant files
     */
    public static final String POWERFOLDER_SYSTEM_SUBDIR = ".PowerFolder";

    // Network architecture contants ******************************************

    /**
     * The lenght in seconds of a request of filelist on a unjoined folder
     * (FolderDetails)
     */
    public static final int FOLDER_FILELIST_REQUEST_LENGTH = 60;

    /**
     * The maximum number of files on a FileList. If list ist greater, it is
     * splitted into smaller ones
     */
    public static final int FILE_LIST_MAX_FILES_PER_MESSAGE = 2000;

    /**
     * The number of supernodes to contact when a new network folder list is
     * requested
     */
    public static final int N_SUPERNODES_TO_CONTACT_FOR_NETWORK_FOLDER_LIST = 4;

    /**
     * The number of supernodes to contact when perfoming a nodes search
     */
    public static final int N_SUPERNODES_TO_CONTACT_FOR_NODE_SEARCH = 4;

    /**
     * The number of supernodes to contact to request the full list of nodes.
     */
    public static final int N_SUPERNODES_TO_CONTACT_FOR_NODE_LIST = 6;

    /**
     * The number of seconds until nodel ist is requested. Currently: 10 minutes
     */
    public static final int NODE_LIST_REQUEST_INTERVAL = 10 * 60;

    /** The number of seconds until a new transfer status is broadcasted */
    public static final int TRANSFER_STATUS_BROADCAST_INTERVAL = 10 * 60;

    /**
     * The number of seconds until the network folder list gets requested
     */
    public static final int NETWORK_FOLDER_LIST_REQUEST_INTERVAL = 20 * 60;

    /**
     * The delay in seconds before another request for network folder list is
     * broadcasted
     */
    public static final int NETWORK_FOLDER_LIST_REQUEST_DELAY = 30;

    /**
     * The maximum number of folder on a NetworkFolderList. If list ist greater,
     * it is splitted into smaller ones
     */
    public static final int NETWORK_FOLDER_LIST_MAX_FOLDERS = 100;

    /**
     * The time in seconds between broadcast of thoses nodes, that went online.
     * Currently: 30 seconds
     */
    public static final long NODES_THAN_WENT_ONLINE_BROADCAST_TIME = 30;

    /**
     * The maximum number of nodes i a message allowed.
     */
    public static final int NODES_LIST_MAX_NODES_PER_MESSAGE = 250;

    /**
     * The Number of seconds of inactivity on a folder that causes removal of
     * folder from internal db. Currently: 14 days
     */
    public static final long MAX_TIME_OF_FOLDER_INACTIVITY_UNTIL_REMOVED = 30 * 24 * 60 * 60;

    /**
     * The maximum numbers of nodes to connection per kilobytes/s upload in
     * supernode mode
     */
    public static final double MAX_NODES_CONNECTIONS_PER_KBS_UPLOAD = 2;

    /**
     * The maximum time for a node to be offline, util no reconnection will be
     * tried. Currently: 3 days
     */
    public static final long MAX_NODE_OFFLINE_TIME = 3 * 24 * 60 * 60 * 1000;

    /** Time until total node invalidation (if not friend). Currently: 60 days */
    public static final long NODE_TIME_TO_INVALIDATE = 1000 * 60 * 60 * 24 * 60;

    /** The max number of reconnectors to spawn */
    public static final int MAX_NUMBER_RECONNECTORS = 10;

    /** The free memory in megabytes to trigger the garbage collector */
    public static final int FREE_MEM_TO_TRIGGER_GC_IN_MB = 4;

    // Basic networking options ***********************************************

    /**
     * The time in ms, how long the timeout for the socket connect method should
     * be used. default: 60s
     */
    public static final int SOCKET_CONNECT_TIMEOUT = 60 * 1000;

    /**
     * The number of incoming connections to queue until the connection is
     * refused. Here: 16
     */
    public static final int MAX_INCOMING_CONNECTIONS = 16;

    /**
     * The time interval in seconds when the incoming connections should be
     * checked
     */
    public static final long INCOMING_CONNECTION_CHECK_TIME = 60;

    /**
     * The maxium time to take for a incoming connection to be processed. (in
     * seconds)
     */
    public static final int INCOMING_CONNECTION_TIMEOUT = 60;

    // Transfer settings ******************************************************

    /**
     * The maximun number of enqued download from a internet connected node
     */
    public static final int MAX_DLS_FROM_INET_MEMBER = 8;

    /**
     * The maximun number of enqued download from a lan connected node
     */
    public static final int MAX_DLS_FROM_LAN_MEMBER = 20;

    // ConnectionHandler constants ********************************************

    /** The maximum messages is send queue until buffer overflow */
    public static final int LIGHT_OVERFLOW_SEND_BUFFER = 10;

    /** The number of message in send buffer until disconnect */
    public static final int HEAVY_OVERFLOW_SEND_BUFFER = 300;

    /**
     * The time in ms, how long the send buffer overrun is allowed to take
     * before disconnect
     */
    public static final long MAX_TIME_WITH_SEND_BUFFER_OVERFLOW = 180 * 1000;

    private Constants() {
        // No instance allowed
    }
}