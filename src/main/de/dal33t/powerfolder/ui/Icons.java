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
 * $Id$
 */
package de.dal33t.powerfolder.ui;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
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
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.util.ui.OverlayedIcon;


/**
 * Contains all icons for the powerfolder application.
 *
 * Icons should be got by calling something like
 * <code>Icons.getIconById(Icon.EXAMPLE)</code>. This will dereference via
 * Icons.properties and will return the Icon as well as caching it, so that
 * subsequent calls will return the same object. The advantage of this aproach
 * is that Icons are only greated as required, saving time and memory.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.74 $
 */
public class Icons {

    private static final Logger log = Logger.getLogger(Icons.class.getName());

    private static final String DEFAULT_ICON_PROPERTIES_FILENAME = "Icons.properties";
    private static String iconPropertiesFilename = DEFAULT_ICON_PROPERTIES_FILENAME;
    private static String overridePropertiesFilename;
    private static Properties iconProperties;
    private static final Map<String, Icon> iconCache = new ConcurrentHashMap<String, Icon>();

    private static final String DISABLED_EXTENSION_ADDITION = "_disabled";
    private static final Object FILE_LOCK = new Object();

    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL = "filter_text_field_clear_button_normal.icon";
    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER = "filter_text_field_clear_button_hover.icon";
    public static final String FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH = "filter_text_field_clear_button_push.icon";
    public static final String FILTER_TEXT_FIELD_GLASS = "filter_text_field_glass.icon";

    public static final String BLANK = "blank.icon";
    public static final String WARNING = "warning.icon";
    public static final String DEBUG = "bug.icon";
    public static final String UPDATES = "updates.icon";
    public static final String SYSTEM_MONITOR = "system_monitor.icon";
    public static final String DIALOG = "dialog.icon";
    public static final String STOP = "stop.icon";
    public static final String RUN = "play.icon";
    public static final String SUSPEND = "suspend.icon";
    public static final String HOME = "home.icon";

    public static final String ADD = "add.icon";
    public static final String EDIT = "edit.icon";
    public static final String DELETE = "delete.icon";
    public static final String FORCE_UPDATE = "force_update.icon";

    public static final String UNKNOWN_FILE = "unknown.icon";
    public static final String UNKNOWN_FILE_GRAY = "unknown_file_gray";
    public static final String UNKNOWN_FILE_RED = "unknown_file_red";

    public static final String SLEEP = "sleep.icon";
    public static final String WAKE_UP = "wake_up.icon";

    public static final String CHAT = "chat.icon";
    public static final String SETTINGS = "settings.icon";
    public static final String ADVANCED = "advanced.icon";
    public static final String INFORMATION = "information.icon";
    public static final String COMPUTER = "computer.icon";

    // Wizard Arrows
    public static final String ARROW_LEFT = "arrow_left.icon";
    public static final String ARROW_RIGHT = "arrow_right.icon";

    // Toolbar
    public static final String SEARCH_NODES = "friends.icon";
    public static final String NEW_FOLDER_48 = "new_folder_48.icon";
    public static final String JOIN_FOLDER_48 = "join_folder_48.icon";
    public static final String SYNC_FOLDER_48 = "sync_48.icon";
    public static final String REMOVE_FOLDER_48 = "leave_folder_48.icon";
    
    public static final String PREFERENCES = "preferences.icon";
    public static final String PREFERENCES_PICTO = "preferences_picto.icon";
    public static final String SYNC = "sync.icon";
    public static final String QUESTION = "question.icon";

    public static final String SORT_UP = "sort_up.icon";
    public static final String SORT_DOWN = "sort_down.icon";
    public static final String SORT_BLANK = "sort_blank.icon";
    public static final String DYN_DNS = "dyn_dns.icon";

    // Directories in navigation tree
    public static final String DIRECTORY = "directory.icon";
    public static final String DIRECTORY_OPEN = "directory_open.icon";
    public static final String DIRECTORY_GRAY = "directory_gray.icon";
    public static final String DIRECTORY_OPEN_GRAY = "directory_open_gray.icon";
    public static final String DIRECTORY_RED = "directory_red.icon";
    public static final String DIRECTORY_OPEN_RED = "directory_open_red.icon";

    // Node icons
    public static final String NODE_FRIEND_CONNECTED = "node_friend_connected.icon";
    public static final String NODE_FRIEND_DISCONNECTED = "node_friend_disconnected.icon";
    public static final String NODE_NON_FRIEND_CONNECTED = "node_non_friend_connected.icon";
    public static final String NODE_NON_FRIEND_DISCONNECTED = "node_non_friend_disconnected.icon";

    public static final String FOLDER = "folder.icon";
    public static final String FILES = "files.icon";

    public static final String LOCAL_FOLDER = "local_folder.icon";
    public static final String LOCAL_AND_ONLINE_FOLDER = "local_and_online_folder.icon";
    public static final String ONLINE_FOLDER = "online_folder.icon";
    public static final String PREVIEW_FOLDER = "preview_folder.icon";

    public static final Icon BLACK_LIST = getIcon("icons/BlackList.gif");
    public static final Icon WHITE_LIST = getIcon("icons/WhiteList.gif");
    public static final Icon DOWNLOAD = getIcon("icons/Download.png");
    public static final Icon DOWNLOAD_ACTIVE = getIcon("icons/Download_active.gif");
    public static final Icon UPLOAD = getIcon("icons/Upload.png");
    public static final Icon UPLOAD_ACTIVE = getIcon("icons/UploadActive.gif");
    public static final Icon IN_ACTIVE = getIcon("icons/Inactive.gif");
    public static final Icon EXPECTED = getIcon("icons/Expected.gif");

    // Folder syncs
    public static final Icon FOLDER_SYNC_UNKNOWN = getIcon("icons/FolderSync_unknown.gif");
    public static final Icon FOLDER_SYNC_0 = getIcon("icons/FolderSync_0.gif");
    public static final Icon FOLDER_SYNC_1 = getIcon("icons/FolderSync_1.gif");
    public static final Icon FOLDER_SYNC_2 = getIcon("icons/FolderSync_2.gif");
    public static final Icon FOLDER_SYNC_3 = getIcon("icons/FolderSync_3.gif");

    public static final String MAC = "mac.icon";
    public static final String CHECKED = "checked.icon";

    // Online state icons
    public static final Icon CONNECTED = getIcon("icons/ConnectBright.png");
    public static final Icon DISCONNECTED = getIcon("icons/ConnectNot.png");

    public static final Icon WEBSERVICE = getIcon("icons/WebService.png");

    // Wizard pico icons
    public static final Icon SYNC_PCS_PICTO = getIcon("icons/pictos/SyncPC.gif");
    public static final Icon PROJECT_WORK_PICTO = getIcon("icons/pictos/ProjectWork.gif");
    public static final Icon FILE_SHARING_PICTO = getIcon("icons/pictos/FileShare.gif");
    public static final Icon WEB_SERVICE_PICTO = getIcon("icons/pictos/WebService.png");

    // Wizard pictos from the quick info panels
    public final Icon LOGO96X96 = getIconById("picto.logo.icon");
    public static final Icon LOGO400UI = getIcon("icons/pictos/PowerFolderLogo400UI.png");
    public static final Icon FRIENDS_PICTO = getIcon("icons/pictos/Friends.png");
    public static final Icon USER_PICTO = getIcon("icons/pictos/User.png");
    public static final Icon UPLOAD_PICTO = getIcon("icons/pictos/Upload.png");
    public static final Icon DOWNLOAD_PICTO = getIcon("icons/pictos/Download.png");
    public static final Icon MYFOLDERS_PICTO = getIcon("icons/pictos/MyFolders.png");
    public static final Icon FOLDER_PICTO = getIcon("icons/pictos/Folder.png");
    public static final Icon RECYCLE_BIN_PICTO = getIcon("icons/pictos/RecycleBin.png");
    public static final Icon WEBSERVICE_QUICK_INFO_PICTO = getIcon("icons/pictos/WebServiceQuickInfo.png");
    public static final Image PACMAN_00 = getImage("icons/pac/pac00.gif");
    public static final Image PACMAN_01 = getImage("icons/pac/pac01.gif");
    public static final Image PACMAN_02 = getImage("icons/pac/pac02.gif");
    public static final Image PACMAN_03 = getImage("icons/pac/pac03.gif");
    public static final Image PACMAN_04 = getImage("icons/pac/pac04.gif");
    public static final Image PACMAN_05 = getImage("icons/pac/pac05.gif");
    public static final Image PACMAN_06 = getImage("icons/pac/pac06.gif");
    public static final Image PACMAN_07 = getImage("icons/pac/pac07.gif");
    public static final Image PACMAN_08 = getImage("icons/pac/pac08.gif");
    public static final Image PACMAN_09 = getImage("icons/pac/pac09.gif");
    public static final Image PACMAN_10 = getImage("icons/pac/pac10.gif");
    public static final Image PACMAN_11 = getImage("icons/pac/pac11.gif");
    public static final Image PACMAN_12 = getImage("icons/pac/pac12.gif");
    public static final Image PACMAN_13 = getImage("icons/pac/pac13.gif");
    public static final Image PACMAN_14 = getImage("icons/pac/pac14.gif");
    public static final Image PACMAN_15 = getImage("icons/pac/pac15.gif");
    public static final Image PACMAN_16 = getImage("icons/pac/pac16.gif");
    public static final Image PACMAN_DOT = getImage("icons/pac/pacDot.gif");

    public static final Icon PRO_LOGO = getIcon("icons/ProLogo.png");
    public static final Icon SMALL_LOGO = getIconById("powerfolder32x32.icon");

    public static final Icon SPLASH = getIconById("splash.icon");

    // Images icons
    public static final Image POWERFOLDER_IMAGE = getImageFromIcon(SMALL_LOGO);
    public static Image FOLDER_IMAGE = getImageFromIcon(getIconById(FOLDER));
    public static Image SYSTEM_MONITOR_IMAGE = getImageFromIcon(getIconById(
            SYSTEM_MONITOR));
    public static Image CHAT_IMAGE = getImageFromIcon(getIconById(CHAT));
    public static Image DEBUG_IMAGE = getImageFromIcon(getIconById(DEBUG));
    public static Image BLANK_IMAGE = getImageFromIcon(getIconById(BLANK));

    // About stuff
    public static final Icon ABOUT_ANIMATION = getIconById("about.animation");

    // Systray icon file names
    public static final Image SYSTRAY_DEFAULT_ICON = getImageById("systray.default.icon");
    public static final Image SYSTRAY_CHAT_ICON = getImage("icons/Chat.gif");
    public static final Image SYSTRAY_FRIEND_ICON = getImage("icons/Node_Friend_Connected.gif");

    private static final Map<String, Icon> KNOWN_ICONS = new HashMap<String, Icon>();

    protected Icons() {
        // No instances - everything is static.
    }

    public static void loadOverrideFile(String iconSetFile) {
        overridePropertiesFilename = iconSetFile;
    }

    /**
     * Protected because only this class, subclasses and Translation.properties
     * refer to images
     * 
     * @deprecated use getIconById to redirect via Icons.properties for better
     *             configurability
     * @param name
     * @return
     */
    protected static Icon getIcon(String name) {
        if (name == null) {
            log.severe("Icon name is null");
            return null;
        }
        if (name.length() <= 6) { // required prefix = icons/
            // log.error("Icon not found '" + name + "'");
            return null;
        }
        URL iconURL = Thread.currentThread().getContextClassLoader()
            .getResource(name);
        if (iconURL == null) {
            log.severe("Icon not found '" + name + '\'');
            return null;
        }

        return new ImageIcon(iconURL);
    }

    /**
     * Method to scale an ImageIcon.
     * 
     * @param source
     *            the source ImageIcon to scale
     * @param scaleFactor
     *            the factor to scale it by
     * @return the scaled image
     */
    public static ImageIcon scaleIcon(ImageIcon source, double scaleFactor) {
        Image image = source.getImage().getScaledInstance(
            (int) (source.getIconWidth() * scaleFactor),
            (int) (source.getIconHeight() * scaleFactor), Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private static Image getImage(String name) {
        URL imageURL = Thread.currentThread().getContextClassLoader()
            .getResource(name);
        return Toolkit.getDefaultToolkit().getImage(imageURL);
    }

    private static synchronized Properties getIconProperties() {
        if (iconProperties == null) {
            iconProperties = new Properties();

            InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(iconPropertiesFilename);
            BufferedInputStream buffered = null;
            if (in == null) {
                throw new IllegalArgumentException(
                    "Icon properties file not found: " + iconPropertiesFilename);
            }

            try {
                buffered = new BufferedInputStream(in);
                iconProperties.load(buffered);
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Cannot read: " + iconPropertiesFilename, ioe);
            } finally {
                if (buffered != null) {
                    try {
                        buffered.close();
                    } catch (Exception e) {

                    }
                }
            }
            
            if (overridePropertiesFilename != null) {
                in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(overridePropertiesFilename);
                buffered = null;
                if (in == null) {
                    throw new IllegalArgumentException(
                        "Icon override properties file not found: " + overridePropertiesFilename);
                }

                try {
                    buffered = new BufferedInputStream(in);
                    iconProperties.load(buffered);
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Cannot read: " + overridePropertiesFilename, ioe);
                } finally {
                    if (buffered != null) {
                        try {
                            buffered.close();
                        } catch (Exception e) {

                        }
                    }
                }
            }
            
            if (overridePropertiesFilename != null) {
                in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(overridePropertiesFilename);
                buffered = null;
                if (in == null) {
                    throw new IllegalArgumentException(
                        "Icon override properties file not found: " + overridePropertiesFilename);
                }

                try {
                    buffered = new BufferedInputStream(in);
                    iconProperties.load(buffered);
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Cannot read: " + overridePropertiesFilename, ioe);
                } finally {
                    if (buffered != null) {
                        try {
                            buffered.close();
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }
        return iconProperties;
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

        Icon icon = iconCache.get(id);
        if (icon != null) {
            return icon;
        }

        // Special cases for unknown file icons.
        if (id.equals(UNKNOWN_FILE_GRAY)) {
            icon = getGrayIcon(getIconById(UNKNOWN_FILE));
            if (icon != null) {
                log.fine("Cached icon " + id);
                iconCache.put(id, icon);
            }
            return icon;
        } else if (id.equals(UNKNOWN_FILE_RED)) {
            icon = convertToRed(getGrayIcon(getIconById(UNKNOWN_FILE)));
            if (icon != null) {
                log.fine("Cached icon " + id);
                iconCache.put(id, icon);
            }
            return icon;
        }

        Properties prop = getIconProperties();
        String iconId = prop.getProperty(id);
        if (iconId == null) {
            log.severe("Icon not found ID: '" + id + '\'');
            return null;
        }
        icon = getIcon(prop.getProperty(id));
        if (icon != null) {
            log.fine("Cached icon " + id);
            iconCache.put(id, icon);
        }
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
        Properties prop = getIconProperties();
        String iconId = prop.getProperty(id);
        if (iconId == null) {
            log.severe("Image not found ID: '" + id + '\'');
            return null;
        }
        return getImage((String) prop.get(id));
    }

    // Open helper

    /**
     * @param node
     * @return a simplified version of the node icon. Does not reflect the
     * online- and supernode-state
     */
    public static Icon getSimpleIconFor(Member node) {
        if (node == null) {
            // Unknown
            return getIconById(NODE_NON_FRIEND_CONNECTED);
        }
        Icon icon;
        // Render friendship things
        if (node.isFriend()) {
            icon = getIconById(NODE_FRIEND_CONNECTED);

        } else {
            // Orange head for non-friends
            icon = getIconById(NODE_NON_FRIEND_CONNECTED);
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
            return getIconById(NODE_NON_FRIEND_CONNECTED);
        }
        Icon icon;

        boolean connected = node.isCompleteyConnected() || node.isMySelf();
        if (connected) {
            if (node.isFriend()) {
                icon = getIconById(NODE_FRIEND_CONNECTED);
            } else {
                icon = getIconById(NODE_NON_FRIEND_CONNECTED);
            }
        } else {
            if (node.isFriend()) {
                icon = getIconById(NODE_FRIEND_DISCONNECTED);
            } else {
                icon = getIconById(NODE_NON_FRIEND_DISCONNECTED);
            }
        }
        if (!node.isOnSameNetwork()) {
            icon = new OverlayedIcon(icon, getIconById(DELETE), 0, 0);
        }
        return icon;
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
    public static Icon getIconFor(FileInfo fileInfo, Controller controller)
    {
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return getUnknownIcon(fileInfo, controller);
        }

        File file = fileInfo.getDiskFile(controller.getFolderRepository());

        boolean exists = file != null && file.exists();
        Icon icon = getCachedIcon(extension, exists);

        if (icon == null) {// no icon found in cache
            if (exists) { // create one if local file is there
                icon = FileSystemView.getFileSystemView().getSystemIcon(file);
                if (icon == null) {
                    return getIconById(UNKNOWN_FILE);
                }
                if (!hasUniqueIcon(extension)) {// do not cache executables
                    KNOWN_ICONS.put(extension, icon);// put in cache
                    Icon disabled = getGrayIcon(icon);
                    // put in cache
                    KNOWN_ICONS.put(extension + DISABLED_EXTENSION_ADDITION,
                        disabled);
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
                            KNOWN_ICONS.put(extension, icon);
                            KNOWN_ICONS.put(extension
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
        if (KNOWN_ICONS.containsKey(extension)) { // getIcon from cache
            return KNOWN_ICONS.get(extension);
        }

        File file = fileInfo.getDiskFile(controller.getFolderRepository());
        boolean exists = file != null && file.exists();

        Icon icon;
        if (exists) {
            icon = FileSystemView.getFileSystemView().getSystemIcon(file);
            if (!hasUniqueIcon(extension)) { // do not cache executables
                KNOWN_ICONS.put(extension, icon);// put in cache
                Icon disabled = getGrayIcon(icon); // think ahead we may need
                // the disabled version somewhere later
                // put in cache
                KNOWN_ICONS.put(extension + DISABLED_EXTENSION_ADDITION,
                    disabled);
            }
            return icon;
        }
        // local file doesnot exists
        icon = getIconExtension(extension);
        return icon;
    }

    /**
     * we don't want to cache icons, executables and screensavers because they
     * have unique icons
     * 
     * @return true if extension is one of "EXE", "SCR", "ICO" else false
     */
    private static boolean hasUniqueIcon(String extension) {
        return extension.equals("EXE") || extension.equals("SCR")
            || extension.equals("ICO");
    }

    /**
     * @return a icon from cache.
     */
    private static Icon getCachedIcon(String extension, boolean disabled)
    {
        if (disabled) {
            if (KNOWN_ICONS.containsKey(extension)) { // getIcon from cache
                return KNOWN_ICONS.get(extension);
            }
        } else {// file does not exist try to get Disabled icon
            if (KNOWN_ICONS
                .containsKey(extension + DISABLED_EXTENSION_ADDITION))
            {
                // get disabled Icon from cache
                return KNOWN_ICONS.get(extension + DISABLED_EXTENSION_ADDITION);
            }
        }
        return null;
    }

    /**
     * @return the unknown icon, normal grey or red based on state of fileinfo
     */
    private static Icon getUnknownIcon(FileInfo fileInfo,
        Controller controller)
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
     * Creates a tmp file and get icon.
     * FileSystemView.getFileSystemView().getSystemIcon(file) needs a existing
     * file to get a icon for TODO: Does it really require an "existing" file ?
     * 
     * @param extension
     *            the extension to get a Icon for
     * @return the icon
     */
    public static Icon getIconExtension(String extension) {
        File tempFile = new File(Controller.getTempFilesLocation(), "temp."
            + extension);

        try {
            synchronized (FILE_LOCK) { // synchronized otherwise we may try
                // to create the same file twice at once
                if (tempFile.createNewFile()) {
                    Icon icon = FileSystemView.getFileSystemView()
                        .getSystemIcon(tempFile);
                    if (!tempFile.delete()) {
                        log.warning("Failed to delete temporary file.");
                        tempFile.deleteOnExit();
                    }
                    return icon;
                }
                log
                    .severe("Couldn't create temporary file for icon retrieval for extension:'"
                        + extension + '\'');

            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception", e);
        }
        return null;
    }

    /** converts Icon to red, note: first convert to gray * 
     * @param icon 
     * @return the red icon */
    public static ImageIcon convertToRed(Icon icon) {
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
     * Return the correct icon for a subdirectory
     * 
     * @param dir
     *            the directory
     * @param isOpen
     *            if it is opend
     * @param controller 
     * @return the icons
     */
    public static Icon getIconFor(Directory dir, boolean isOpen,
        Controller controller)
    {
        if (dir.isDeleted()) {
            return isOpen ? getIconById(DIRECTORY_OPEN_RED) : getIconById(DIRECTORY_RED);
        } else if (dir.isExpected(controller.getFolderRepository())) {
            return isOpen ? getIconById(DIRECTORY_OPEN_GRAY) : getIconById(DIRECTORY_GRAY);
        } else {
            return isOpen ? getIconById(DIRECTORY_OPEN) : getIconById(DIRECTORY);
        }
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
                icon = DOWNLOAD_ACTIVE;
            } else {
                icon = DOWNLOAD;
            }
        } else if (fInfo.isDeleted()) {
            icon = getIconById(DELETE);
        } else if (fInfo.isExpected(controller.getFolderRepository())) {
            icon = EXPECTED;
        } else if (fInfo.getFolder(controller.getFolderRepository()) == null) {
            icon = EXPECTED;
        } else {
            icon = null;
        }
        return icon;
    }

    /**
     * create a disabled (Gray) version of the Icon much better way than
     * GrayFilter, because GrayFilter does not handle the transparancy well.
     * 
     * @param icon
     *            the icon to convert to gray icon
     * @return icon grayed out for use as disabled icon
     */
    public static Icon getGrayIcon(Icon icon) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp colorConvertOp = new ColorConvertOp(colorSpace, null);

        Image image = getImageFromIcon(icon);

        // on failure, a colored icon is better than nothing
        if (image == null) {
            return icon;
        }

        return new ImageIcon(colorConvertOp
            .filter(toBufferedImage(image), null));
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
    public static Image getImageFromIcon(Icon icon) {
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
                log.log(Level.SEVERE, "Could not get icon from IconUIResource", e);
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

    // This method returns a buffered image with the contents of an image.
    // "Converting" by drawing on image, but there seems to be no other way.
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
            bimage = gc.createCompatibleImage(image.getWidth(null), image
                .getHeight(null), transparency);
        } catch (HeadlessException e) {
            log.log(Level.FINER, "HeadlessException", e);
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image
                .getHeight(null), type);
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
}