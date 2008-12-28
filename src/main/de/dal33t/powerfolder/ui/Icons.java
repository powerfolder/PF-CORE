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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.Upload;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.IconUIResource;
import java.awt.*;
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
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Contains all icons for the powerfolder application
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.74 $
 */
public class Icons {

    private static final Logger log = Logger.getLogger(Icons.class.getName());

    private static Properties iconProperties;
    private static final String ICON_PROPERTIES_FILENAME = "Icons.properties";

    private static final String DISABLED_EXTENSION_ADDITION = "_disabled";
    private static final Object FILE_LOCK = new Object();

    public static final Icon FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL = getIcon("icons/FilterTextFieldClearButtonNormal.png");
    public static final Icon FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER = getIcon("icons/FilterTextFieldClearButtonHover.png");
    public static final Icon FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH = getIcon("icons/FilterTextFieldClearButtonPush.png");
    public static final Icon FILTER_TEXT_FIELD_GLASS = getIcon("icons/FilterTextFieldGlass.png");

    public static final Icon WARNING = getIcon("icons/Warning.png");
    public static final Icon DEBUG = getIcon("icons/LadyBug.gif");
    public static final Icon UPDATES = getIcon("icons/Updates.gif");
    public static final Icon SYSTEM_MONITOR = getIcon("icons/SystemMonitor.png");
    public static final Icon DIALOG = getIcon("icons/Dialog.gif");
    public static final Icon STOP = getIcon("icons/Abort.gif");
    public static final Icon RUN = getIcon("icons/Play.gif");
    public static final Icon SUSPEND = getIcon("icons/Suspend.gif");
    public static final Icon HOME = getIcon("icons/Home.png");

    public static final Icon WORLD_ADD = getIcon("icons/WorldAdd.png");
    public static final Icon WORLD_EDIT = getIcon("icons/WorldEdit.png");
    public static final Icon WORLD_DELETE = getIcon("icons/WorldDelete.png");
    public static final Icon FORCE_UPDATE = getIcon("icons/ForceUpdate.png");

    public static final Icon UNKNOWN_FILE = getIcon("icons/Unknown.gif");
    public static final Icon UNKNOWN_FILE_GRAY = getGrayIcon(UNKNOWN_FILE);
    public static final Icon UNKNOWN_FILE_RED = convertToRed(UNKNOWN_FILE_GRAY);

    public static final Icon CHAT = getIcon("icons/Chat.gif");
    public static final Icon SETTINGS = getIcon("icons/Settings.png");
    public static final Icon ADVANCED = getIcon("icons/Advanced.png");
    public static final Icon STAR = getIcon("icons/Star.gif");
    public static final Icon INFORMATION = getIcon("icons/information.png");
    public static final Icon COMPUTER = getIcon("icons/Computer.png");

    // Wizard Arrows
    public static final Icon ARROW_LEFT = getIcon("icons/ArrowLeft.gif");
    public static final Icon ARROW_RIGHT = getIcon("icons/ArrowRight.gif");

    // Toolbar
    public static final Icon SEARCH_NODES = getIcon("icons/pictos/Friends.png");
    public static final Icon NEW_FOLDER = getIcon("icons/pictos/NewFolder-48.png");
    public static final Icon JOIN_FOLDER = getIcon("icons/pictos/JoinFolder-48.png");
    public static final Icon REMOVE_FOLDER = getIcon("icons/pictos/LeaveFolder-48.png");
    public static final Icon PREFERENCES = getIcon("icons/Preferences.png");
    public static final Icon PREFERENCES_PICTO = getIcon("icons/pictos/Preferences.png");
    public static final Icon SYNC = getIcon("icons/Sync.png");
    public static final Icon QUESTION = getIcon("icons/Question.gif");

    public static final Icon SORT_UP = getIcon("icons/SortUp.gif");
    public static final Icon SORT_DOWN = getIcon("icons/SortDown.gif");
    public static final Icon SORT_BLANK = getIcon("icons/SortBlank.gif");
    public static final Icon DYN_DNS = getIcon("icons/DynDns.gif");

    // Directories in navigation tree
    public static final Icon DIRECTORY = getIcon("icons/Directory.gif");
    public static final Icon DIRECTORY_OPEN = getIcon("icons/Directory_open.gif");
    public static final Icon DIRECTORY_GRAY = getIcon("icons/Directory_gray.gif");
    public static final Icon DIRECTORY_OPEN_GRAY = getIcon("icons/Directory_open_gray.gif");
    public static final Icon DIRECTORY_RED = getIcon("icons/Directory_red.gif");
    public static final Icon DIRECTORY_OPEN_RED = getIcon("icons/Directory_open_red.gif");

    // Node icons
    public static final Icon NODE_FRIEND_CONNECTED = getIcon("icons/NodeFriendConnected.gif");
    public static final Icon NODE_FRIEND_DISCONNECTED = getIcon("icons/NodeFriendDisconnected.gif");
    public static final Icon NODE_NON_FRIEND_CONNECTED = getIcon("icons/NodeNonFriendConnected.gif");
    public static final Icon NODE_NON_FRIEND_DISCONNECTED = getIcon("icons/NodeNonFriendDisconnected.gif");

    public static final Icon FOLDER = getIcon("icons/Folder.png");
    public static final Icon FILES = getIcon("icons/Files.png");

    public static final Icon PF_LOCAL = getIcon("icons/PFLocal.png");
    public static final Icon PF_LOCAL_AND_ONLINE = getIcon("icons/PFLocalAndOnline.png");
    public static final Icon PF_ONLINE = getIcon("icons/PFOnline.png");
    public static final Icon PF_PREVIEW = getIcon("icons/PFPreview.png");

    public static final Icon BLACK_LIST = getIcon("icons/BlackList.gif");
    public static final Icon WHITE_LIST = getIcon("icons/WhiteList.gif");
    public static final Icon DOWNLOAD = getIcon("icons/Download.png");
    public static final Icon DOWNLOAD_ACTIVE = getIcon("icons/Download_active.gif");
    public static final Icon UPLOAD = getIcon("icons/Upload.png");
    public static final Icon UPLOAD_ACTIVE = getIcon("icons/UploadActive.gif");
    public static final Icon IN_ACTIVE = getIcon("icons/Inactive.gif");
    public static final Icon EXPECTED = getIcon("icons/Expected.gif");
    public static final Icon DELETE = getIcon("icons/Delete.gif");

    // Folder syncs
    public static final Icon FOLDER_SYNC_UNKNOWN = getIcon("icons/FolderSync_unknown.gif");
    public static final Icon FOLDER_SYNC_0 = getIcon("icons/FolderSync_0.gif");
    public static final Icon FOLDER_SYNC_1 = getIcon("icons/FolderSync_1.gif");
    public static final Icon FOLDER_SYNC_2 = getIcon("icons/FolderSync_2.gif");
    public static final Icon FOLDER_SYNC_3 = getIcon("icons/FolderSync_3.gif");

    public static final Icon MAC = getIcon("icons/Mac.gif");
    public static final Icon CHECKED = getIcon("icons/Checked.gif");

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
    public static final Icon LOGO96X96 = getIcon("icons/pictos/PowerFolderLogo96x96.png");
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
    public static final Icon SMALL_LOGO = getIcon("icons/PowerFolder32x32.gif");

    public static final Icon SPLASH = getIcon("icons/Splash.png");

    // Images icons
    public static final Image POWERFOLDER_IMAGE = getImage("icons/PowerFolder32x32.gif");
    public static final Image FOLDER_IMAGE = getImage("icons/Folder.png");
    public static final Image SYSTEM_MONITOR_IMAGE = getImage("icons/SystemMonitor.png");
    public static final Image CHAT_IMAGE = getImage("icons/Chat.gif");

    // About stuff
    public static final Icon ABOUT_ANIMATION = getIcon("icons/about/AboutAnimation.gif");

    // Systray icon file names
    public static final String ST_POWERFOLDER = "PowerFolder32x32.gif";
    public static final String ST_CHAT = "Chat.gif";
    public static final String ST_NODE = "NodeFriendConnected.gif";

    private static final Map<String, Icon> KNOWN_ICONS = new HashMap<String, Icon>();

    protected Icons() {}

    /**
     * Protected because only this class, subclasses and Translation.properties
     * refer to images
     * 
     * @param name
     * @return
     */
    protected static Icon getIcon(String name) {
        if (name == null) {
            log.severe("Icon name is null");
            return null;
        }
        if (name.length() <= 6) { // required prefix = icons/
            // Loggable.logSevereStatic(Icons.class, "Icon not found '" + name + "'");
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
                .getResourceAsStream(ICON_PROPERTIES_FILENAME);
            BufferedInputStream buffered = null;

            try {
                buffered = new BufferedInputStream(in);
                iconProperties.load(buffered);
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Cannot read: " + ICON_PROPERTIES_FILENAME, ioe);
            } finally {
                if (buffered != null) {
                    try {
                        buffered.close();
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Exception", e);
                    }
                }
            }
        }
        return iconProperties;
    }

    /**
     * Returns the icons for the specified id
     * 
     * @param id
     *            the icon id
     * @deprecated This should only be used for getting action icons, using
     * Icons.properties. Everything else, use getIcon(). Keeps icon - file
     * relationship more simple and direct.
     * @return the icon
     */
    @Deprecated
    public static Icon getIconById(String id) {
        Properties prop = getIconProperties();
        String iconId = prop.getProperty(id);
        if (iconId == null) {
            return null;
        }
        return getIcon(prop.getProperty(id));
    }

    // Open helper

    /**
     * Returns a simplified version of the node icon. Does not reflect the
     * online- and supernode-state
     * 
     * @param node
     * @return
     */
    public static Icon getSimpleIconFor(Member node) {
        if (node == null) {
            // Unknown
            return NODE_NON_FRIEND_CONNECTED;
        }
        Icon icon;
        // Render friendship things
        if (node.isFriend()) {
            icon = NODE_FRIEND_CONNECTED;

        } else {
            // Orange head for non-friends
            icon = NODE_NON_FRIEND_CONNECTED;
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
            return NODE_NON_FRIEND_CONNECTED;
        }
        Icon icon;

        boolean connected = node.isCompleteyConnected() || node.isMySelf();
        if (connected) {
            if (node.isFriend()) {
                icon = NODE_FRIEND_CONNECTED;
            } else {
                icon = NODE_NON_FRIEND_CONNECTED;
            }
        } else {
            if (node.isFriend()) {
                icon = NODE_FRIEND_DISCONNECTED;
            } else {
                icon = NODE_NON_FRIEND_DISCONNECTED;
            }
        }

        return icon;
    }

    /**
     * returns a icon based on the state of the fileinfo this maybe a normal
     * gray or red icon
     * 
     * @param fileInfo
     *            the fileinfo to return a icon for
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
                    return UNKNOWN_FILE;
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
                    icon = UNKNOWN_FILE_GRAY;
                } else {
                    icon = getIconExtension(extension);
                    if (icon == null) {
                        icon = UNKNOWN_FILE_GRAY;
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
     * Returns the default status icon for the transfer
     * 
     * @param transfer
     * @return
     */
    public static Icon getIconFor(Transfer transfer) {
        if (transfer instanceof Upload) {
            return transfer.isStarted() ? UPLOAD_ACTIVE : UPLOAD;
        }
        return transfer.isStarted() ? DOWNLOAD_ACTIVE : DOWNLOAD;
    }

    /**
     * returns a icon (never gray or red)
     * 
     * @param fileInfo
     *            the fileinfo to return a icon for
     * @return the icon
     */
    public static Icon getEnabledIconFor(FileInfo fileInfo,
        Controller controller)
    {
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return UNKNOWN_FILE;
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
            return UNKNOWN_FILE;
        }
        if (fileInfo.isDeleted()) {
            return UNKNOWN_FILE_RED;
        }
        return UNKNOWN_FILE_GRAY;
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
                } else {
                    log.severe("Couldn't create temporary file for icon retrieval for extension:'"
                            + extension + '\'');
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception", e);
        }
        return null;
    }

    /** converts Icon to red, note: first convert to gray * */
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
     * @return the icons
     */
    public static Icon getIconFor(Directory dir, boolean isOpen,
        Controller controller)
    {
        if (dir.isDeleted()) {
            return isOpen ? DIRECTORY_OPEN_RED : DIRECTORY_RED;
        } else if (dir.isExpected(controller.getFolderRepository())) {
            return isOpen ? DIRECTORY_OPEN_GRAY : DIRECTORY_GRAY;
        } else {
            return isOpen ? DIRECTORY_OPEN : DIRECTORY;
        }
    }

    /**
     * Returns the icon for a file
     * 
     * @param controller
     * @param fInfo
     *            the file
     * @return
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
            icon = DELETE;
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
            log.log(Level.SEVERE, "HeadlessException", e);
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