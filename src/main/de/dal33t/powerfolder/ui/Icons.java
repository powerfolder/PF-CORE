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
import java.io.*;
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
 * Similarly, Images should be got by calling something like
 * <code>Icons.getImageById(Icon.EXAMPLE)</code>.  
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.74 $
 */
public class Icons {

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
    public static final String FRIENDS = "friends.icon";
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

    public static final String BLACK_LIST = "black_list.icon";
    public static final String WHITE_LIST = "white_list.icon";
    public static final String DOWNLOAD = "download.icon";
    public static final String DOWNLOAD_ACTIVE = "download_active.icon";
    public static final String UPLOAD = "upload.icon";
    public static final String UPLOAD_ACTIVE = "upload_active.icon";
    public static final String INACTIVE = "inactive.icon";
    public static final String EXPECTED = "expected.icon";

    // Folder syncs
    public static final String FOLDER_SYNC_UNKNOWN = "folder_sync_unknown.icon";
    public static final String FOLDER_SYNC_0 = "folder_sync_0.icon";
    public static final String FOLDER_SYNC_1 = "folder_sync_1.icon";
    public static final String FOLDER_SYNC_2 = "folder_sync_2.icon";
    public static final String FOLDER_SYNC_3 = "folder_sync_3.icon";

    public static final String MAC = "mac.icon";
    public static final String CHECKED = "checked.icon";

    // Online state icons
    public static final String CONNECTED = "connected_bright.icon";
    public static final String DISCONNECTED = "connected_not.icon";

    public static final String ONLINE_STORAGE = "web_service.icon";

    // Wizard pico icons
    public static final String SYNC_PCS_PICTO = "sync_pc_picto.icon";
    public static final String PROJECT_WORK_PICTO = "project_work_picto.icon";
    public static final String FILE_SHARING_PICTO = "file_share_picto.icon";
    public static final String WEB_SERVICE_PICTO = "web_service_picto.icon";

    // Wizard pictos from the quick info panels
    public static final String LOGO96X96 = "picto_logo.icon";
    public static final String LOGO400UI = "power_folder_logo_400_ui.icon";
    public static final String USER_PICTO = "user_picto.icon";
    public static final String UPLOAD_PICTO = "upload_picto.icon";
    public static final String DOWNLOAD_PICTO = "download_picto.icon";
    public static final String MYFOLDERS_PICTO = "my_folders_picto.icon";
    public static final String FOLDER_PICTO = "folder_picto.icon";
    public static final String RECYCLE_BIN_PICTO = "recycle_bin_picto.icon";
    public static final String WEBSERVICE_QUICK_INFO_PICTO = "web_service_quick_info.icon";
    public static final String PRO_LOGO = "pro_logo.icon";
    public static final String SMALL_LOGO = "powerfolder_32.icon";
    public static final String SPLASH = "splash.icon";

    // About stuff
    public static final String ABOUT_ANIMATION = "about_animation.icon";

    public static final String PACMAN_00 = "pac_00.icon";
    public static final String PACMAN_01 = "pac_01.icon";
    public static final String PACMAN_02 = "pac_02.icon";
    public static final String PACMAN_03 = "pac_03.icon";
    public static final String PACMAN_04 = "pac_04.icon";
    public static final String PACMAN_05 = "pac_05.icon";
    public static final String PACMAN_06 = "pac_06.icon";
    public static final String PACMAN_07 = "pac_07.icon";
    public static final String PACMAN_08 = "pac_08.icon";
    public static final String PACMAN_09 = "pac_09.icon";
    public static final String PACMAN_10 = "pac_10.icon";
    public static final String PACMAN_11 = "pac_11.icon";
    public static final String PACMAN_12 = "pac_12.icon";
    public static final String PACMAN_13 = "pac_13.icon";
    public static final String PACMAN_14 = "pac_14.icon";
    public static final String PACMAN_15 = "pac_15.icon";
    public static final String PACMAN_16 = "pac_16.icon";
    public static final String PACMAN_DOT = "pac_dot.icon";
    public static final String SYSTRAY_DEFAULT = "systray_default.icon";

    private static final Logger log = Logger.getLogger(Icons.class.getName());
    private static final String DEFAULT_ICON_PROPERTIES_FILENAME = "Icons.properties";
    private static final String DISABLED_EXTENSION_ADDITION = "_disabled";
    private static final Object FILE_LOCK = new Object();

    /** Map of ID - Icon */
    private static final Map<String, Icon> ID_ICON_MAP = new ConcurrentHashMap<String, Icon>();

    /** Map of ID - Image */
    private static final Map<String, Image> ID_IMAGE_MAP = new ConcurrentHashMap<String, Image>();

    /** Map of Extension - Icon */
    private static final Map<String, Icon> EXTENSION_ICON_MAP = new HashMap<String, Icon>();

    private static String overridePropertiesFilename;
    private static Properties iconProperties;
    private static Properties overrideIconProperties;

    /**
     * Constructor - no instances.
     */
    protected Icons() {
        // No instances - everything is static.
    }

    /**
     * Configure PowerFolder to use a different properties file / icons.
     * If PF can not find the icon in the override set, it will default to
     * internal icons, so users do not need to define a full set of icons.
     *
     * @param iconSetFile
     */
    public static void loadOverrideFile(String iconSetFile) {
        log.info("Loaded override icons file " + iconSetFile);
        overridePropertiesFilename = iconSetFile;
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

        Icon icon = ID_ICON_MAP.get(id);
        if (icon != null) {
            return icon;
        }

        // Special cases for unknown file icons.
        if (id.equals(UNKNOWN_FILE_GRAY)) {
            icon = getGrayIcon(getIconById(UNKNOWN_FILE));
            if (icon != null) {
                log.fine("Cached icon " + id);
                ID_ICON_MAP.put(id, icon);
            }
            return icon;
        } else if (id.equals(UNKNOWN_FILE_RED)) {
            icon = convertToRed(getGrayIcon(getIconById(UNKNOWN_FILE)));
            if (icon != null) {
                log.fine("Cached icon " + id);
                ID_ICON_MAP.put(id, icon);
            }
            return icon;
        }

        String iconId = getIconId(id);
        if (iconId == null) {
            log.severe("Icon not found ID: '" + id + '\'');
            return null;
        }

        URL iconURL = Thread.currentThread().getContextClassLoader()
            .getResource(iconId);
        if (iconURL == null) {
            log.severe("Icon not found '" + id + '\'');
            return null;
        }

        icon = new ImageIcon(iconURL);
        log.fine("Cached icon " + id);
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
        image = Toolkit.getDefaultToolkit().getImage(imageURL);
        log.fine("Cached image " + id);
        ID_IMAGE_MAP.put(id, image);

        return image;
    }

    /**
     * Get the icon id from the properties for an id.
     * So if there is a line
     * <pre>stop.icon=icons/Abort.gif</pre>
     * then id of 'stop.icon' would return 'icons/Abort.gif'.
     * Tries override properties first, then the normal PowerFolder ones.
     *
     * @param id
     * @return
     */
    private static String getIconId(String id) {
        Properties overrideProperties = getOverrideIconProperties();
        if (overrideProperties != null) {
            String iconId = overrideProperties.getProperty(id);
            if (iconId != null) {
                return iconId;
            }
        }

        // Not found in override icon set, try local.
        return getIconProperties().getProperty(id);
    }

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
                    EXTENSION_ICON_MAP.put(extension, icon);// put in cache
                    Icon disabled = getGrayIcon(icon);
                    // put in cache
                    EXTENSION_ICON_MAP.put(extension + DISABLED_EXTENSION_ADDITION,
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

        File file = fileInfo.getDiskFile(controller.getFolderRepository());
        boolean exists = file != null && file.exists();

        Icon icon;
        if (exists) {
            icon = FileSystemView.getFileSystemView().getSystemIcon(file);
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
        return icon;
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
    private static Icon getCachedIcon(String extension, boolean disabled)
    {
        if (disabled) {
            if (EXTENSION_ICON_MAP.containsKey(extension)) { // getIcon from cache
                return EXTENSION_ICON_MAP.get(extension);
            }
        } else {// file does not exist try to get Disabled icon
            if (EXTENSION_ICON_MAP
                .containsKey(extension + DISABLED_EXTENSION_ADDITION))
            {
                // get disabled Icon from cache
                return EXTENSION_ICON_MAP.get(extension + DISABLED_EXTENSION_ADDITION);
            }
        }
        return null;
    }

    /**
     * @param fileInfo
     * @param controller
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
            log.severe("Image Icon not found for extension '" + extension + '\'');
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

    private static synchronized Properties getIconProperties() {
        if (iconProperties == null) {
            iconProperties = new Properties();

            InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(DEFAULT_ICON_PROPERTIES_FILENAME);
            BufferedInputStream buffered = null;
            if (in == null) {
                throw new IllegalArgumentException(
                    "Icon properties file not found: " + DEFAULT_ICON_PROPERTIES_FILENAME);
            }

            try {
                buffered = new BufferedInputStream(in);
                iconProperties.load(buffered);
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Cannot read: " + DEFAULT_ICON_PROPERTIES_FILENAME, ioe);
            } finally {
                if (buffered != null) {
                    try {
                        buffered.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
        return iconProperties;
    }

    /**
     * This gets icon mappings from an override set. Referenced icons must be
     * in the classpath. On Windows environments, file separators in the
     * properties file need to be \\
     *
     * e.g. play.icon=myIcons\\Play.gif
     *
     * @return
     */
    private static synchronized Properties getOverrideIconProperties() {

        if (overridePropertiesFilename != null && overrideIconProperties == null) {
            overrideIconProperties = new Properties();

            BufferedInputStream buf = null;
            try {
                buf = new BufferedInputStream(new FileInputStream(overridePropertiesFilename));
                overrideIconProperties.load(buf);
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Cannot read: " + overridePropertiesFilename, ioe);
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
        return overrideIconProperties;
    }

}