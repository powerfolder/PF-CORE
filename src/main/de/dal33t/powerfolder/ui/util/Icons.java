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
 * $Id: Icons.java 21220 2013-03-18 16:50:29Z sprajc $
 */
package de.dal33t.powerfolder.ui.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.IconUIResource;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.skin.Origin;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.TrayIconManager;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains all icons for the powerfolder application. Icons should be got by
 * calling <code>Icons.getIconById(Icon.EXAMPLE)</code>. This will dereference
 * the icon via properties file and will return the Icon as well as caching it,
 * so that subsequent calls will return the same object. The advantage of this
 * approach is that Icons are only get as required, saving time and memory.
 * Similarly, Images should be got by calling something like
 * <code>Icons.getImageById(Icon.EXAMPLE)</code>.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.74 $
 */
public class Icons {

    // General stuff
    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL = "filter_text_field_clear_button_normal.icon";
    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER = "filter_text_field_clear_button_hover.icon";
    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH = "filter_text_field_clear_button_push.icon";
    public static final String FILTER_TEXT_FIELD_GLASS = "filter_text_field_glass.icon";
    public static final String FILTER_TEXT_FIELD_GLASS_ARROW = "filter_text_field_glass_arrow.icon";

    public static final String WINDOW_MAXIMIZE_NORMAL = "window_maximize_normal.icon";
    public static final String WINDOW_MAXIMIZE_HOVER = "window_maximize_hover.icon";
    public static final String WINDOW_MAXIMIZE_PUSH = "window_maximize_push.icon";

    public static final String WINDOW_PLUS_NORMAL = "window_plus_normal.icon";
    public static final String WINDOW_PLUS_HOVER = "window_plus_hover.icon";
    public static final String WINDOW_PLUS_PUSH = "window_plus_push.icon";

    public static final String WINDOW_MINUS_NORMAL = "window_minus_normal.icon";
    public static final String WINDOW_MINUS_HOVER = "window_minus_hover.icon";
    public static final String WINDOW_MINUS_PUSH = "window_minus_push.icon";

    public static final String ACTION_ARROW = "action_arrow.icon";
    public static final String BLANK = "blank.icon";
    public static final String WARNING = "warning.icon";
    public static final String DEBUG = "bug.icon";
    public static final String SYSTEM_MONITOR = "system_monitor.icon";
    public static final String STOP = "stop.icon";
    public static final String RUN = "run.icon";
    public static final String PAUSE = "pause.icon";
    public static final String STATUS = "status.icon";

    public static final String ADD = "add.icon";
    public static final String EDIT = "edit.icon";
    public static final String DELETE = "delete.icon";

    public static final String UNKNOWN_FILE = "unknown.icon";
    public static final String UNKNOWN_FILE_GRAY = "unknown_file_gray";
    public static final String UNKNOWN_FILE_RED = "unknown_file_red";

    public static final String SETTINGS = "settings.icon";
    public static final String PROBLEMS = "problems.icon";
    public static final String INFORMATION = "information.icon";

    // Wizard Arrows
    public static final String ARROW_LEFT = "arrow_left.icon";
    public static final String ARROW_RIGHT = "arrow_right.icon";
    public static final String EXPAND = "expand.icon";
    public static final String COLLAPSE = "collapse.icon";
    public static final String QUESTION = "question.icon";

    public static final String SORT_UP = "sort_up.icon";
    public static final String SORT_DOWN = "sort_down.icon";
    public static final String SORT_BLANK = "sort_blank.icon";

    // Directories in navigation tree
    public static final String DIRECTORY = "directory.icon";
    public static final String DIRECTORY_OPEN = "directory_open.icon";

    // Node icons
    public static final String NODE_MYSELF = "node_myself.icon";
    public static final String NODE_CONNECTED = "node_friend_connected.icon";
    public static final String NODE_DISCONNECTED = "node_friend_disconnected.icon";
    public static final String NODE_CONNECTING = NODE_DISCONNECTED;
    public static final String NODE_POOR = "node_friend_poor.icon";
    public static final String NODE_MEDIUM = "node_friend_medium.icon";
    public static final String NODE_LAN = "node_friend_lan.icon";
    public static final String NODE_GROUP = "node_group.icon";

    public static final String FOLDER = "folder.icon";
    public static final String FILES = "files.icon";

    public static final String ONLINE_FOLDER = "online_folder.icon";
    public static final String ONLINE_FOLDER_SMALL = "online_folder_small.icon";
    public static final String PREVIEW_FOLDER = "preview_folder.icon";
    public static final String TYPICAL_FOLDER = "typical_folder.icon";

    public static final String BLACK_LIST = "black_list.icon";
    public static final String WHITE_LIST = "white_list.icon";
    public static final String DOWNLOAD = "download.icon";
    public static final String DOWNLOAD_ACTIVE = "download_active.icon";
    public static final String UPLOAD = "upload.icon";
    public static final String INACTIVE = "inactive.icon";
    public static final String TRANSFERS = "transfers.icon";
    public static final String EXPECTED = "expected.icon";
    public static final String CHECKED = "checked.icon";

    public static final String STATS = "stats.icon";

    public static final String STATUS_TAB_TELL_A_FRIEND = "status_tab_tell_friend_icon";
    public static final String TWITTER_BUTTON = "twitter.icon";
    public static final String FACEBOOK_BUTTON = "facebook.icon";
    public static final String LINKEDIN_BUTTON = "linkedin.icon";
    public static final String EMAIL_BUTTON = "email.icon";

    // Sync icons
    public static final String SYNC_COMPLETE = "sync_complete.icon";
    public static final String SYNC_INCOMPLETE = "sync_incomplete.icon";
    public static final String[] SYNC_ANIMATION = {"sync00.icon",
        "sync01.icon", "sync02.icon", "sync03.icon", "sync04.icon",
        "sync05.icon", "sync06.icon", "sync07.icon", "sync08.icon",
        "sync09.icon", "sync10.icon", "sync11.icon"};

    // Systray icons
    public static final String[] SYSTRAY_SYNC_ANIMATION;
    private static final String[] SYSTRAY_SYNC_ANIMATION_LOW_RES = {
        "systray_sync00_lowres.icon", "systray_sync01_lowres.icon", "systray_sync02_lowres.icon",
        "systray_sync03_lowres.icon", "systray_sync04_lowres.icon", "systray_sync05_lowres.icon",
        "systray_sync06_lowres.icon", "systray_sync07_lowres.icon", "systray_sync08_lowres.icon",
        "systray_sync09_lowres.icon", "systray_sync10_lowres.icon", "systray_sync11_lowres.icon"};
    
    private static final String[] SYSTRAY_SYNC_ANIMATION_HI_RES = {
        "systray_sync00_hires.icon", "systray_sync01_hires.icon", "systray_sync02_hires.icon",
        "systray_sync03_hires.icon", "systray_sync04_hires.icon", "systray_sync05_hires.icon",
        "systray_sync06_hires.icon", "systray_sync07_hires.icon", "systray_sync08_hires.icon",
        "systray_sync09_hires.icon", "systray_sync10_hires.icon", "systray_sync11_hires.icon"};
    
    public static final String SYSTRAY_SYNC_COMPLETE;
    private static final String SYSTRAY_SYNC_COMPLETE_LOW_RES = "systray_sync_complete_lowres.icon";
    private static final String SYSTRAY_SYNC_COMPLETE_HIGH_RES = "systray_sync_complete_hires.icon";
    
    public static final String SYSTRAY_SYNC_INCOMPLETE;
    private static final String SYSTRAY_SYNC_INCOMPLETE_LOW_RES = "systray_sync_incomplete_lowres.icon";
    private static final String SYSTRAY_SYNC_INCOMPLETE_HIGH_RES = "systray_sync_incomplete_hires.icon";

    public static final String SYSTRAY_WARNING;
    private static final String SYSTRAY_WARNING_LOW_RES = "systray_warning_lowres.icon";
    private static final String SYSTRAY_WARNING_HIGH_RES = "systray_warning_hires.icon";
    
    public static final String SYSTRAY_PAUSE;
    private static final String SYSTRAY_PAUSE_LOW_RES = "systray_pause_lowres.icon";
    private static final String SYSTRAY_PAUSE_HIGH_RES = "systray_pause_hires.icon";
    
    static {
        if (!TrayIconManager.isHiRes()) {
            SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_LOW_RES;
            SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_LOW_RES;
            SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_LOW_RES;
            SYSTRAY_WARNING = SYSTRAY_WARNING_LOW_RES;
            SYSTRAY_PAUSE = SYSTRAY_PAUSE_LOW_RES;
        } else {
            SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_HI_RES;
            SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_HIGH_RES;
            SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_HIGH_RES;
            SYSTRAY_WARNING = SYSTRAY_WARNING_HIGH_RES;
            SYSTRAY_PAUSE = SYSTRAY_PAUSE_HIGH_RES;
        }
    }

    // Wizard pictos from the quick info panels
    public static final String LOGO128X128 = "picto_logo_128.icon";
    public static final String LOGO400UI = "power_folder_logo_400_ui.icon";
    public static final String SMALL_LOGO = "powerfolder_32.icon";
    public static final String SPLASH = "splash.icon";
    public static final String POWERED_BY = "poweredby.icon";

    private static final Logger log = Logger.getLogger(Icons.class.getName());
    private static final String DISABLED_EXTENSION_ADDITION = "_disabled";
    private static final Object FILE_LOCK = new Object();

    /** Map of ID - Icon */
    private static final Map<String, Icon> ID_ICON_MAP = new ConcurrentHashMap<String, Icon>();

    /** Map of ID - Image */
    private static final Map<String, Image> ID_IMAGE_MAP = new ConcurrentHashMap<String, Image>();

    /** Map of Extension - Icon */
    private static final Map<String, Icon> EXTENSION_ICON_MAP = new HashMap<String, Icon>();

    // BlueGlobe is our default.
    private static final String DEFAULT_PROPERTIES_FILENAME = Origin.ICON_PROPERTIES_FILENAME;

    private static Properties iconProperties;

    private static final List<String> UNKNOWN_ICONS = new ArrayList<String>();

    /**
     * Constructor - no instances.
     */
    protected Icons() {
        // No instances - everything is static.
    }

    /**
     * Configure the properties file / icons. PowerFolder will takes the icons
     * from this properties.
     * 
     * @param icoProps
     */
    public static void setIconProperties(Properties icoProps) {
        Reject.ifNull(icoProps, "iconProperties");
        Reject.ifTrue(icoProps.isEmpty(), "iconProperties are empty");
        iconProperties = icoProps;
        ID_ICON_MAP.clear();
        ID_IMAGE_MAP.clear();
    }

    /**
     * @return the icon properties. Loads the default icon properties if
     *         null/not set/not loaded.
     */
    public static synchronized Properties getIconProperties() {
        if (iconProperties == null) {
            iconProperties = loadProperties(DEFAULT_PROPERTIES_FILENAME);
        }
        // Don't hand out internal instance to prevent side effects.
        return new Properties(iconProperties);
    }

    /**
     * Returns the icons for the specified id.
     * 
     * @param id
     *            the icon id
     * @return the icon
     */
    public static Icon getIconById(String id) {
        if (id == null) {
            log.severe("Icon id null ???");
            return null;
        }

        if (UNKNOWN_ICONS.contains(id)) {
            // Already discovered that we do not know this one.
            return null;
        }

        Icon icon = ID_ICON_MAP.get(id);
        if (icon != null) {
            return icon;
        }

        // Special cases for unknown file icons.
        if (id.equals(UNKNOWN_FILE_GRAY)) {
            icon = getGrayIcon(getIconById(UNKNOWN_FILE));
            if (icon != null) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Cached icon " + id);
                }
                ID_ICON_MAP.put(id, icon);
            }
            return icon;
        } else if (id.equals(UNKNOWN_FILE_RED)) {
            icon = convertToRed(getGrayIcon(getIconById(UNKNOWN_FILE)));
            if (icon != null) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Cached icon " + id);
                }
                ID_ICON_MAP.put(id, icon);
            }
            return icon;
        }

        String iconId = getIconId(id);
        if (iconId == null) {
            // Log it.
            if (log.isLoggable(Level.FINE)) {
                log.fine("Icon not found ID: '" + id + '\'');
            }
            UNKNOWN_ICONS.add(id);
            return null;
        }

        URL iconURL = Thread.currentThread().getContextClassLoader()
            .getResource(iconId);
        if (iconURL == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Icon not found '" + id + '\'');
            }
            return null;
        }

        icon = new ImageIcon(iconURL);
        if (log.isLoggable(Level.FINER)) {
            log.finer("Cached icon " + id);
        }
        ID_ICON_MAP.put(id, icon);
        return icon;
    }

    /**
     * Returns the image for the specified id
     * 
     * @param id
     *            the image id
     * @return the image
     */
    public static Image getImageById(String id) {

        Image image = ID_IMAGE_MAP.get(id);
        if (image != null) {
            return image;
        }

        String iconId = getIconId(id);
        if (iconId == null) {
            log.severe("Image not found ID: '" + id + '\'');
            return null;
        }

        URL imageURL = Thread.currentThread().getContextClassLoader()
            .getResource(iconId);
        if (imageURL == null) {
            return null;
        }
        image = Toolkit.getDefaultToolkit().getImage(imageURL);
        if (log.isLoggable(Level.FINER)) {
            log.finer("Cached image " + id);
        }
        ID_IMAGE_MAP.put(id, image);

        return image;
    }

    /**
     * Get the icon id from the properties for an id. So if there is a line
     * 
     * <pre>
     * stop.icon = icons / Abort.png
     * </pre>
     * 
     * then id of 'stop.icon' would return 'icons/Abort.png'. Tries override
     * properties first, then the normal PowerFolder ones.
     * 
     * @param id
     * @return
     */
    private static String getIconId(String id) {
        return getIconProperties().getProperty(id);
    }

    /**
     * @param node
     * @return a simplified version of the node icon. Does not reflect the
     *         online- and supernode-state
     */
    public static Icon getSimpleIconFor(Member node) {
        if (node == null) {
            // Unknown
            return getIconById(NODE_CONNECTED);
        }
        Icon icon;
        // Render friendship things
        if (node.isFriend()) {
            icon = getIconById(NODE_CONNECTED);

        } else {
            // Orange head for non-friends
            icon = getIconById(NODE_CONNECTED);
        }
        if (!node.isOnSameNetwork()) {
            icon = new OverlayedIcon(icon, getIconById(DELETE), 0, 0);
        }
        return icon;
    }

    /**
     * Returns the icon for that node
     * 
     * @param node
     * @return the icon
     */
    public static Icon getIconFor(Member node) {
        if (node == null) {
            // Unknown
            return getIconById(NODE_CONNECTED);
        }

        String iconID;
        if (node.isMySelf()) {
            iconID = NODE_MYSELF;
        } else if (node.isCompletelyConnected()) {
            ConnectionHandler peer = node.getPeer();
            iconID = NODE_CONNECTED;
            if (node.isOnLAN()) {
                iconID = NODE_LAN;
            } else if (peer != null) {
                ConnectionQuality quality = peer.getConnectionQuality();
                if (quality != null) {
                    switch (quality) {
                        case GOOD :
                            iconID = NODE_CONNECTED;
                            break;
                        case MEDIUM :
                            iconID = NODE_MEDIUM;
                            break;
                        case POOR :
                            iconID = NODE_POOR;
                            break;
                    }
                }
            }
        } else {
            iconID = NODE_DISCONNECTED;
        }
        Icon icon = getIconById(iconID);
        if (!node.isOnSameNetwork()) {
            icon = new OverlayedIcon(icon, getIconById(DELETE), 0, 0);
        }
        return icon;
    }

    public static Icon getIconByAccount(AccountInfo member,
        Controller controller)
    {
        String username = member.getUsername();

        if (username == null) {
            return getIconById(NODE_DISCONNECTED);
        }

        String urlString = controller.getOSClient()
            .getAvatarURL(member);

        try {
            URL url = new URL(urlString);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();
            int code = con.getResponseCode();

            if (code == 200) {
                ImageIcon tempIcon = new ImageIcon(url);
                Image img = tempIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);

                return new ImageIcon(img);
            }
        } catch (MalformedURLException e) {
            log.warning("Avatar URL was malformed: " + urlString);
        } catch (IOException e) {
            log.fine(e.getMessage());
        }

        return getIconById(NODE_DISCONNECTED);
    }
    
    /**
     * returns a icon based on the state of the fileinfo this maybe a normal
     * gray or red icon
     * 
     * @param fileInfo
     *            the fileinfo to return a icon for
     * @param controller
     * @return the icon
     */
    public static Icon getIconFor(FileInfo fileInfo, Controller controller) {
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return getUnknownIcon(fileInfo, controller);
        }

        Path file = fileInfo.getDiskFile(controller.getFolderRepository());

        boolean exists = file != null && Files.exists(file);
        Icon icon = getCachedIcon(extension, exists);

        if (icon == null) {// no icon found in cache
            if (exists) { // create one if local file is there
                icon = FileSystemView.getFileSystemView().getSystemIcon(file.toFile());
                if (icon == null) {
                    return getIconById(UNKNOWN_FILE);
                }
                if (!hasUniqueIcon(extension)) {// do not cache executables
                    EXTENSION_ICON_MAP.put(extension, icon);// put in cache
                    Icon disabled = getGrayIcon(icon);
                    // put in cache
                    EXTENSION_ICON_MAP.put(extension
                        + DISABLED_EXTENSION_ADDITION, disabled);
                }
            } else { // local file doesnot exists
                if (hasUniqueIcon(extension)) {// if *.exe or *.ico we don't
                    // know the icon
                    // fixes speed with lots of *.ico or *.exe files
                    icon = getIconById(UNKNOWN_FILE_GRAY);
                } else {
                    icon = getIconExtension(extension);
                    if (icon == null) {
                        icon = getIconById(UNKNOWN_FILE_GRAY);
                    } else {
                        Icon disabled = getGrayIcon(icon);
                        if (!hasUniqueIcon(extension)) {// do not cache *.exe
                            // and *.ico etc
                            // put in cache
                            EXTENSION_ICON_MAP.put(extension, icon);
                            EXTENSION_ICON_MAP.put(extension
                                + DISABLED_EXTENSION_ADDITION, disabled);
                        }
                        icon = disabled;
                    }
                }
            }
        }
        if (fileInfo.isDeleted()) {
            icon = convertToRed(icon); // it's already gray because local file
            // does not exists
        }
        return icon;
    }

    /**
     * returns a icon (never gray or red)
     * <p>
     * TODO THIS IS A MESS
     * 
     * @param fileInfo
     *            the fileinfo to return a icon for
     * @param controller
     * @return the icon
     */
    public static Icon getEnabledIconFor(FileInfo fileInfo,
        Controller controller)
    {
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return getIconById(UNKNOWN_FILE);
        }
        if (EXTENSION_ICON_MAP.containsKey(extension)) { // getIcon from cache
            return EXTENSION_ICON_MAP.get(extension);
        }

        Path file = fileInfo.getDiskFile(controller.getFolderRepository());
        boolean exists = file != null && Files.exists(file);

        Icon icon;
        if (exists) {
            icon = FileSystemView.getFileSystemView().getSystemIcon(file.toFile());
            if (!hasUniqueIcon(extension)) { // do not cache executables
                EXTENSION_ICON_MAP.put(extension, icon);// put in cache
                Icon disabled = getGrayIcon(icon); // think ahead we may need
                // the disabled version somewhere later
                // put in cache
                EXTENSION_ICON_MAP.put(extension + DISABLED_EXTENSION_ADDITION,
                    disabled);
            }
            return icon;
        }
        // local file doesnot exists
        icon = getIconExtension(extension);
        EXTENSION_ICON_MAP.put(extension, icon);// put in cache
        Icon disabled = getGrayIcon(icon); // think ahead we may need
        // the disabled version somewhere later
        // put in cache
        EXTENSION_ICON_MAP.put(extension + DISABLED_EXTENSION_ADDITION,
            disabled);
        return icon;
    }

    /**
     * @param controller
     * @param fInfo
     *            the file
     * @return the icon for a file
     */
    public static Icon getIconFor(Controller controller, FileInfo fInfo) {
        Icon icon;

        if (fInfo.isDownloading(controller)) {
            DownloadManager dl = controller.getTransferManager()
                .getActiveDownload(fInfo);
            if (dl != null && dl.isStarted()) {
                icon = getIconById(DOWNLOAD_ACTIVE);
            } else {
                icon = getIconById(DOWNLOAD);
            }
        } else if (fInfo.isDeleted()) {
            icon = getIconById(DELETE);
        } else if (fInfo.isExpected(controller.getFolderRepository())) {
            icon = getIconById(EXPECTED);
        } else if (fInfo.getFolder(controller.getFolderRepository()) == null) {
            icon = getIconById(EXPECTED);
        } else {
            icon = null;
        }
        return icon;
    }

    /**
     * we don't want to cache icons, executables and screensavers because they
     * have unique icons
     * 
     * @param extension
     * @return true if extension is one of "EXE", "SCR", "ICO" else false
     */
    private static boolean hasUniqueIcon(String extension) {
        return extension.equals("EXE") || extension.equals("SCR")
            || extension.equals("ICO");
    }

    /**
     * @param extension
     * @param disabled
     * @return a icon from cache.
     */
    private static Icon getCachedIcon(String extension, boolean disabled) {
        if (disabled) {
            if (EXTENSION_ICON_MAP.containsKey(extension)) { // getIcon from
                // cache
                return EXTENSION_ICON_MAP.get(extension);
            }
        } else {// file does not exist try to get Disabled icon
            if (EXTENSION_ICON_MAP.containsKey(extension
                + DISABLED_EXTENSION_ADDITION))
            {
                // get disabled Icon from cache
                return EXTENSION_ICON_MAP.get(extension
                    + DISABLED_EXTENSION_ADDITION);
            }
        }
        return null;
    }

    /**
     * @param fileInfo
     * @param controller
     * @return the unknown icon, normal grey or red based on state of fileinfo
     */
    private static Icon getUnknownIcon(FileInfo fileInfo, Controller controller)
    {
        if (fileInfo.diskFileExists(controller)) {
            return getIconById(UNKNOWN_FILE);
        }
        if (fileInfo.isDeleted()) {
            return getIconById(UNKNOWN_FILE_RED);
        }
        return getIconById(UNKNOWN_FILE_GRAY);
    }

    /**
     * Creates a tmp file and get image.
     * FileSystemView.getFileSystemView().getSystemIcon(file) needs a existing
     * file to get a image for.
     * 
     * @param extension
     *            the extension to get a Image for
     * @return the Image
     */
    public static Image getImageExtension(String extension) {
        Icon icon = getIconExtension(extension);
        if (icon == null) {
            log.severe("Image Icon not found for extension '" + extension
                + '\'');
            return null;
        }
        return getImageFromIcon(icon);
    }

    /**
     * Creates a tmp file and get icon.
     * FileSystemView.getFileSystemView().getSystemIcon(file) needs a existing
     * file to get a icon for.
     * 
     * @param extension
     *            the extension to get a Icon for
     * @return the icon
     */
    private static Icon getIconExtension(String extension) {
        Path tempFile = Controller.getTempFilesLocation().resolve("temp."
            + extension);
        synchronized (FILE_LOCK) { // synchronized otherwise we may try
            // to create the same file twice at once
            try {
                Files.createFile(tempFile);
                Icon icon = FileSystemView.getFileSystemView()
                    .getSystemIcon(tempFile.toFile());
                try {
                    Files.delete(tempFile);
                } catch (IOException ioe) {
                    log.warning("Failed to delete temporary file.");
                    tempFile.toFile().deleteOnExit();
                }
                return icon;
            } catch (IOException ioe) {
                log.severe("Couldn't create temporary file for icon retrieval for extension:'"
                    + extension + '\'');
            }
        }
        return null;
    }

    /**
     * converts Icon to red, note: first convert to gray *
     * 
     * @param icon
     * @return the red icon
     */
    private static ImageIcon convertToRed(Icon icon) {
        Image image = getImageFromIcon(icon);
        BufferedImage src = toBufferedImage(image);

        int targetColor = 0x00FF0000; // Red; format: 0x00RRGGBB in hex

        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dst = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = src.getRGB(x, y);
                int istrans = rgb & 0xFF000000;
                if (istrans == 0xFF000000) { // map to alpha
                    int alpha = 255 - (src.getRGB(x, y) & 0x000000FF);
                    dst.setRGB(x, y, alpha << 24 | targetColor);
                } else {// set original if transparent
                    dst.setRGB(x, y, rgb);
                }
            }
        }
        return new ImageIcon(dst);
    }

    /**
     * create a disabled (Gray) version of the Icon much better way than
     * GrayFilter, because GrayFilter does not handle the transparancy well.
     * 
     * @param icon
     *            the icon to convert to gray icon
     * @return icon grayed out for use as disabled icon
     */
    private static Icon getGrayIcon(Icon icon) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp colorConvertOp = new ColorConvertOp(colorSpace, null);

        Image image = getImageFromIcon(icon);

        // on failure, a colored icon is better than nothing
        if (image == null) {
            return icon;
        }

        return new ImageIcon(
            colorConvertOp.filter(toBufferedImage(image), null));
    }

    /**
     * Extracts the image from an Icon. If the icon is not an ImageIcon but
     * wrapped into an IconUIResource, this method tries to get the image via
     * reflection.
     * 
     * @param icon
     *            The icon to get the image from.
     * @return The image or null on failure
     */
    private static Image getImageFromIcon(Icon icon) {
        if (icon == null) {
            log.log(Level.SEVERE, "Icon is null", new RuntimeException(
                "Icon is null"));
            return null;
        }

        // simple case: we have an ImageIcon to get the image from
        if (icon instanceof ImageIcon) {
            ImageIcon imageIcon = (ImageIcon) icon;
            return imageIcon.getImage();
        }

        // if the icon is wrapped in an IconUIResource, try to unwrap
        if (icon instanceof IconUIResource) {
            // try to get the image from the icon via Reflection
            try {
                IconUIResource iconUIResource = (IconUIResource) icon;
                Field delegateField = iconUIResource.getClass()
                    .getDeclaredField("delegate");
                delegateField.setAccessible(true);
                Icon inner = (Icon) delegateField.get(iconUIResource);
                return getImageFromIcon(inner);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Could not get icon from IconUIResource",
                    e);
            }
        }

        // Fallback
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        GraphicsEnvironment ge = GraphicsEnvironment
            .getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(w, h);
        Graphics2D g = image.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }

    /**
     * This method returns a buffered image with the contents of an image.
     * "Converting" by drawing on image, but there seems to be no other way.
     * <P>
     * ATTENTION: Needs to be public. Used by PowerFolder Pro code.
     * 
     * @param image
     * @return the buffered image.
     */
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent
        // Pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the
        // screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment
            .getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null),
                image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            log.log(Level.FINER, "HeadlessException", e);
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null),
                image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    // This method returns true if the specified image has transparent pixels
    private static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            log.log(Level.INFO, "InterruptedException", e);
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    /**
     * Loads a properties file from classpath.
     * 
     * @param filename
     *            the filename to load
     * @return the properties that have been loaded. Or <code>null</code> if not
     *         found.
     */
    public static Properties loadProperties(String filename) {
        Reject.ifBlank(filename, "Properties blank");

        BufferedInputStream buf = null;
        try {
            Properties props = new Properties();
            InputStream inputStream = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(filename);
            if (inputStream == null) {
                throw new FileNotFoundException("File not found");
            }
            buf = new BufferedInputStream(inputStream);
            props.load(buf);
            return props;
        } catch (IOException ioe) {
            log.log(Level.INFO, "Cannot read properties file: " + filename
                + ". " + ioe, ioe);
            return null;
        } finally {
            if (buf != null) {
                try {
                    buf.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}