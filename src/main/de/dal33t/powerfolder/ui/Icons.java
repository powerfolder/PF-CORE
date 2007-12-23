/* $Id: Icons.java,v 1.74 2006/04/21 23:04:36 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.Graphics;
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
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.IconUIResource;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.Transfer;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;

/**
 * Contains all icons for the powerfolder application
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.74 $
 */
public class Icons {

    private static Properties iconProperties;
    private static final String ICON_PROPERTIES_FILENAME = "Icons.properties";

    /**
     * text to add to the key for finding a disabled icon in the cache of icons.
     * 
     * @see knownIcons
     */
    private static final String DISABLED_EXTENSION_ADDITION = "_disabled";

    private static final Logger log = Logger.getLogger(Icons.class);

    public static final Icon FILTER_TEXTFIELD_CLEARBUTTON_NORMAL = getIcon("icons/filter_textfield_clearbutton_normal.png");
    public static final Icon FILTER_TEXTFIELD_CLEARBUTTON_HOVER = getIcon("icons/filter_textfield_clearbutton_hover.png");
    public static final Icon FILTER_TEXTFIELD_CLEARBUTTON_PUSH = getIcon("icons/filter_textfield_clearbutton_push.png");

    public static final Icon WARNING = getIcon("icons/Warning.png");
    public static final Icon DEBUG = getIcon("icons/LadyBug.gif");
    public static final Icon STOP = getIcon("icons/Forbid.gif");
    public static final Icon RUN = getIcon("icons/Play.gif");
    public static final Icon SUSPEND = getIcon("icons/Suspend.gif");

    public static final Icon UNKNOWNFILE = getIcon("icons/Unknown.gif");
    public static final Icon UNKNOWNFILE_GRAY = getGrayIcon(UNKNOWNFILE);
    public static final Icon UNKNOWNFILE_RED = convertToRed(UNKNOWNFILE_GRAY);

    public static final Icon CHAT = getIcon("icons/Chat.gif");
    public static final Icon SETTINGS = getIconById("settings.icon");

    // Arrows
    public static final Icon ARROW_UP = getIcon("icons/ArrowUp.gif");
    public static final Icon ARROW_LEFT = getIcon("icons/ArrowLeft.gif");
    public static final Icon ARROW_RIGHT = getIcon("icons/ArrowRight.gif");
    public static final Icon ARROW_UP_GRAY = getIcon("icons/ArrowUp_gray.gif");
    public static final Icon ARROW_LEFT_GRAY = getIcon("icons/ArrowLeft_gray.gif");
    public static final Icon ARROW_RIGHT_GRAY = getIcon("icons/ArrowRight_gray.gif");

    // Toolbar
    public static final Icon WIZARD_OPEN = getIconById("wizard.icon");
    public static final Icon NEW_FOLDER = getIconById("new_folder.icon");
    public static final Icon JOIN_FOLDER = getIconById("join_folder.icon");
    public static final Icon LEAVE_FOLDER = getIconById("leave_folder.icon");
    public static final Icon INVITATION = getIconById("join_by_invitation.icon");
    public static final Icon PREFERENCES = getIconById("preferences.icon");
    public static final Icon ABOUT = getIconById("about.icon");
    public static final Icon SLEEP = getIconById("sleep.icon");
    public static final Icon WAKE_UP = getIconById("wake_up.icon");
    public static final Icon SYNC_NOW = getIconById("sync_now.icon");
    public static final Icon SYNC_NOW_ACTIVE = getIconById("sync_now_active.icon");
    public static final Icon BUY_PRO = getIconById("buypro.icon");

    // Directories in navigation tree
    public static final Icon DIRECTORY = getIcon("icons/Directory.gif");
    public static final Icon DIRECTORY_OPEN = getIcon("icons/Directory_open.gif");
    public static final Icon DIRECTORY_GRAY = getIcon("icons/Directory_gray.gif");
    public static final Icon DIRECTORY_OPEN_GRAY = getIcon("icons/Directory_open_gray.gif");
    public static final Icon DIRECTORY_RED = getIcon("icons/Directory_red.gif");
    public static final Icon DIRECTORY_OPEN_RED = getIcon("icons/Directory_open_red.gif");

    // Node icons
    public static final Icon NODE_FRIEND_CONNECTED = getIcon("icons/Node_Friend_Connected.gif");
    public static final Icon NODE_FRIEND_DISCONNECTED = getIcon("icons/Node_Friend_Disconnected.gif");
    public static final Icon NODE_NON_FRIEND_CONNECTED = getIcon("icons/Node_NonFriend_Connected.gif");
    public static final Icon NODE_NON_FRIEND_DISCONNECTED = getIcon("icons/Node_NonFriend_Disconnected.gif");

    // Folder icons
    public static final Icon FOLDERS = getIcon("icons/Folders.gif");

    public static final Icon FOLDER = getIcon("icons/Folder.gif");
    public static final Icon FOLDER_NON_MEMBER_CONNECTED = getIcon("icons/Folder_disconnected.gif");
    public static final Icon FOLDER_ROTATION_1 = getIcon("icons/Folder_rotation_1.gif");
    public static final Icon FOLDER_ROTATION_2 = getIcon("icons/Folder_rotation_2.gif");
    public static final Icon FOLDER_ROTATION_3 = getIcon("icons/Folder_rotation_3.gif");
    public static final Icon FOLDER_ROTATION_4 = getIcon("icons/Folder_rotation_4.gif");
    public static final Icon FOLDER_ROTATION_5 = getIcon("icons/Folder_rotation_5.gif");
    public static final Icon FOLDER_ROTATION_6 = getIcon("icons/Folder_rotation_6.gif");
    public static final Icon FOLDER_ROTATION_7 = getIcon("icons/Folder_rotation_7.gif");
    public static final Icon FOLDER_ROTATION_8 = getIcon("icons/Folder_rotation_8.gif");

    // Navitree & toolbar
    public static final Icon ROOT = getIcon("icons/Root.gif");
    public static final Icon KNOWN_NODES = getIcon("icons/KnownNodes.gif");
    public static final Icon RECYCLE_BIN = getIcon("icons/KnownNodes.gif");

    public static final Icon SYNC_MODE = getIcon("icons/SyncMode.gif");

    public static final Icon DOWNLOAD = getIcon("icons/Download.gif");
    public static final Icon DOWNLOAD_ACTIVE = getIcon("icons/Download_active.gif");
    public static final Icon UPLOAD = getIcon("icons/Upload.gif");
    public static final Icon UPLOAD_ACTIVE = getIcon("icons/Upload_active.gif");
    public static final Icon IN_ACTIVE = getIcon("icons/In_active.gif");
    public static final Icon EXPECTED = getIcon("icons/Expected.gif");
    public static final Icon DELETE = getIcon("icons/Delete.gif");
    public static final Icon IGNORE = getIcon("icons/Forbid.gif");
    public static final Icon PATTERN = getIconById("pattern");

    // Folder syncs
    public static final Icon FOLDER_SYNC_UNKNOWN = getIcon("icons/FolderSync_unknown.gif");
    public static final Icon FOLDER_SYNC_0 = getIcon("icons/FolderSync_0.gif");
    public static final Icon FOLDER_SYNC_1 = getIcon("icons/FolderSync_1.gif");
    public static final Icon FOLDER_SYNC_2 = getIcon("icons/FolderSync_2.gif");
    public static final Icon FOLDER_SYNC_3 = getIcon("icons/FolderSync_3.gif");

    public static final Icon MAC = getIcon("icons/Mac.gif");
    public static final Icon CHECKED = getIcon("icons/Checked.gif");

    // Online state icons
    public static final Icon CONNECTED = getIcon("icons/Connected.gif");
    public static final Icon DISCONNECTED = getIcon("icons/Disconnected.gif");

    public static final Icon WEBSERVICE = getIcon("icons/WebService.png");

    // Wizard pico icons
    public static final Icon SYNC_PCS_PICTO = getIcon("icons/pictos/SyncPC.gif");
    public static final Icon SYNC_PCS_PICTO_GRAY = getIcon("icons/pictos/SyncPC_gray.gif");
    public static final Icon PROJECT_WORK_PICTO = getIcon("icons/pictos/ProjectWork.gif");
    public static final Icon PROJECT_WORK_PICTO_GRAY = getIcon("icons/pictos/ProjectWork_gray.gif");
    public static final Icon FILESHARING_PICTO = getIcon("icons/pictos/Fileshare.gif");
    public static final Icon FILESHARING_PICTO_GRAY = getIcon("icons/pictos/Fileshare_gray.gif");
    public static final Icon WEBSERVICE_PICTO = getIcon("icons/pictos/WebService.png");

    // Wizard pictos from the quick info panels
    public static final Icon LOGO96X96 = getIcon("icons/pictos/PowerFolderLogo96x96.png");
    public static final Icon FRIENDS_PICTO = getIcon("icons/pictos/Friends.png");
    public static final Icon USER_PICTO = getIcon("icons/pictos/User.png");
    public static final Icon UPLOAD_PICTO = getIcon("icons/pictos/Upload.png");
    public static final Icon DOWNLOAD_PICTO = getIcon("icons/pictos/Download.png");
    public static final Icon MYFOLDERS_PICTO = getIcon("icons/pictos/MyFolders.png");
    public static final Icon FOLDER_PICTO = getIcon("icons/pictos/Folder.png");
    public static final Icon RECYCLE_BIN_PICTO = getIcon("icons/pictos/RecycleBin.png");
    public static final Icon WEBSERVICE_QUICK_INFO_PICTO = getIcon("icons/pictos/WebServiceQuickInfo.png");

    public static final Icon PRO_LOGO = getIcon("icons/ProLogo.png");
    public static final Icon SMALL_LOGO = getIconById("small_logo.icon");

    // Images icons
    public static final Image POWERFOLDER_IMAGE = getImage("icons/PowerFolder_32x32.gif");
    public static final Icon SPLASH = getIcon("icons/Splash.png");

    // About stuff
    public static final Icon ABOUT_ANIMATION = getIcon("icons/about/AboutAnimation.gif");

    // Systray icon names
    public static final String ST_POWERFOLDER = "PowerFolder";
    public static final String ST_CHAT = "Chat";
    public static final String ST_NODE = "Node";

    private static final HashMap<String, Icon> KNOWN_ICONS = new HashMap<String, Icon>();

    protected Icons() {
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
    protected static final Icon getIcon(String name) {
        if (name == null) {
            log.error("Icon name is null");
            return null;
        }
        if (name.length() <= 6) { // required prefix = icons/
            // log.error("Icon not found '" + name + "'");
            return null;
        }
        URL iconURL = Thread.currentThread().getContextClassLoader()
            .getResource(name);
        if (iconURL == null) {
            log.error("Icon not found '" + name + '\'');
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

    private synchronized static Properties getIconProperties() {
        if (iconProperties == null) {
            iconProperties = new Properties();
            InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ICON_PROPERTIES_FILENAME);
            BufferedInputStream buffered = new BufferedInputStream(in);

            try {
                iconProperties.load(buffered);
            } catch (IOException ioe) {
                log.error("Cannot read: " + ICON_PROPERTIES_FILENAME, ioe);
            } finally {
                if (buffered != null) {
                    try {
                        buffered.close();
                    } catch (Exception e) {

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
     * @return the icon
     */
    public static final Icon getIconById(String id) {
        Properties prop = getIconProperties();
        String iconId = prop.getProperty(id);
        if (iconId == null) {
            return null;
        }
        return getIcon((String) prop.get(id));
    }

    // Open helper

    /**
     * Returns a simplified version of the node icon. Does not reflect the
     * online- and supernode-state
     * 
     * @param node
     * @return
     */
    public static final Icon getSimpleIconFor(Member node) {
        if (node == null) {
            // Unknown
            return Icons.NODE_NON_FRIEND_CONNECTED;
        }
        Icon icon;
        // Render friendship things
        if (node.isFriend()) {
            icon = Icons.NODE_FRIEND_CONNECTED;

        } else {
            // Orange head for non-friends
            icon = Icons.NODE_NON_FRIEND_CONNECTED;
        }

        return icon;
    }

    /**
     * Returns the icon for that node
     * 
     * @param node
     * @return the icon
     */
    public static final Icon getIconFor(Member node) {
        if (node == null) {
            // Unknown
            return Icons.NODE_NON_FRIEND_CONNECTED;
        }
        Icon icon;

        boolean connected = node.isCompleteyConnected() || node.isMySelf();
        if (connected) {
            if (node.isFriend()) {
                icon = Icons.NODE_FRIEND_CONNECTED;
            } else {
                icon = Icons.NODE_NON_FRIEND_CONNECTED;
            }
        } else {
            if (node.isFriend()) {
                icon = Icons.NODE_FRIEND_DISCONNECTED;
            } else {
                icon = Icons.NODE_NON_FRIEND_DISCONNECTED;
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
    public static final Icon getIconFor(FileInfo fileInfo, Controller controller) {
        Icon icon = null;
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return getUnknownIcon(fileInfo, controller);
        }

        File file = fileInfo.getDiskFile(controller.getFolderRepository());

        boolean exists = (file != null && file.exists());
        icon = getCachedIcon(extension, exists);

        if (icon == null) {// no icon found in cache
            if (exists) { // create one if local file is there
                icon = FileSystemView.getFileSystemView().getSystemIcon(file);
                if (icon == null) {
                    return UNKNOWNFILE;
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
                    icon = UNKNOWNFILE_GRAY;
                } else {
                    icon = getIconExtension(extension);
                    if (icon == null) {
                        icon = UNKNOWNFILE_GRAY;
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
    public static final Icon getIconFor(Transfer transfer) {
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
    public static final Icon getEnabledIconFor(FileInfo fileInfo,
        Controller controller)
    {
        Icon icon = null;
        String extension = fileInfo.getExtension();
        if (extension == null) { // no file extension
            return UNKNOWNFILE;
        }
        if (KNOWN_ICONS.containsKey(extension)) { // getIcon from cache
            return KNOWN_ICONS.get(extension);
        }

        File file = fileInfo.getDiskFile(controller.getFolderRepository());
        boolean exists = (file != null && file.exists());

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
        if (extension.equals("EXE") || extension.equals("SCR")
            || extension.equals("ICO"))
        {
            return true;
        }
        return false;
    }

    /**
     * @return a icon from cache.
     */
    private static final Icon getCachedIcon(String extension, boolean disabled) {
        if (disabled) {
            if (KNOWN_ICONS.containsKey(extension)) { // getIcon from cache
                return KNOWN_ICONS.get(extension);
            }
        } else {// file does not exist try to get Disabled icon
            if (KNOWN_ICONS.containsKey(extension + DISABLED_EXTENSION_ADDITION))
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
    private static final Icon getUnknownIcon(FileInfo fileInfo, Controller controller)
    {
        if (fileInfo.diskFileExists(controller)) {
            return UNKNOWNFILE;
        }
        if (fileInfo.isDeleted()) {
            return UNKNOWNFILE_RED;
        }
        return UNKNOWNFILE_GRAY;
    }

    /**
     * Creates a tmp file and get icon.
     * FileSystemView.getFileSystemView().getSystemIcon(file) needs a existing
     * file to get a icon for
     * TODO: Does it really require an "existing" file ?
     * 
     * @param extension
     *            the extension to get a Icon for
     * @return the icon
     */
    public static final Icon getIconExtension(String extension) {
        File tempFile = new File(Controller.getTempFilesLocation(), "temp."
            + extension);
        try {
            synchronized (KNOWN_ICONS) { // synchronized otherwise we may try
                // to create the same file twice at once
                if (tempFile.createNewFile()) {
	                Icon icon = FileSystemView.getFileSystemView().getSystemIcon(
	                    tempFile);
	                if (!tempFile.delete()) {
	                	log.warn("Failed to delete temporary file.");
	                	tempFile.deleteOnExit();
	                }
	                return icon;
                } else {
                	log.error("Couldn't create temporary file for icon retrieval for extension:'" + extension + "'");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** converts Icon to red, note: first convert to gray * */
    public static ImageIcon convertToRed(Icon icon) {
        Image image = ((ImageIcon) icon).getImage();
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
                if (istrans != 0xFF000000) {// set original if transparent
                    dst.setRGB(x, y, rgb);
                } else { // map to alpha
                    int alpha = 255 - (src.getRGB(x, y) & 0x000000FF);
                    dst.setRGB(x, y, alpha << 24 | targetColor);
                }
            }
        }
        return new ImageIcon(dst);
    }

    /**
     * @param folder
     *            the folder to get the icon for.
     * @return the icon for the folder depending on its status, e.g.
     *         disconnected, connected, sync, new files, etc.
     */
    public static final Icon getIconFor(Folder folder) {
        Reject.ifNull(folder, "Folder is null");

        boolean isMembersConnected = folder.getConnectedMembers().length > 0;
        boolean isRecentlyCompleted = isRecentlyCompleted(folder);
        boolean isSyncing = folder.isTransferring() || folder.isScanning();

        Icon fIcon = Icons.FOLDER;

        if (isSyncing) {
            long time = System.currentTimeMillis() / 400;
            int iconNum = (int) (time % 8);
            switch (iconNum) {
                case 0 :
                    fIcon = Icons.FOLDER_ROTATION_1;
                    break;
                case 1 :
                    fIcon = Icons.FOLDER_ROTATION_2;
                    break;
                case 2 :
                    fIcon = Icons.FOLDER_ROTATION_3;
                    break;
                case 3 :
                    fIcon = Icons.FOLDER_ROTATION_4;
                    break;
                case 4 :
                    fIcon = Icons.FOLDER_ROTATION_5;
                    break;
                case 5 :
                    fIcon = Icons.FOLDER_ROTATION_6;
                    break;
                case 6 :
                    fIcon = Icons.FOLDER_ROTATION_7;
                    break;
                case 7 :
                    fIcon = Icons.FOLDER_ROTATION_8;
                    break;
                default :
                    break;
            }
            // if (time % 8 == 0) {
            //               
            // fIcon = Icons.FOLDER_NEW_FILE_COMPLETED;
            // } else {
            // fIcon = Icons.FOLDER_SYNC;
            // }
        } else if (isRecentlyCompleted) {
            fIcon = Icons.FOLDER_ROTATION_1;
        } else if (!isMembersConnected) {
            fIcon = Icons.FOLDER_NON_MEMBER_CONNECTED;
        }

        return fIcon;
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
    public static final Icon getIconFor(Directory dir, boolean isOpen,
        Controller controller)
    {
        if (dir.isDeleted()) {
            return isOpen ? Icons.DIRECTORY_OPEN_RED : Icons.DIRECTORY_RED;
        } else if (dir.isExpected(controller.getFolderRepository())) {
            return isOpen ? Icons.DIRECTORY_OPEN_GRAY : Icons.DIRECTORY_GRAY;
        } else {
            return isOpen ? Icons.DIRECTORY_OPEN : Icons.DIRECTORY;
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
    public static final Icon getIconFor(Controller controller, FileInfo fInfo) {
        Icon icon;

        if (fInfo.isDownloading(controller)) {
            Download dl = controller.getTransferManager().getActiveDownload(
                fInfo);
            if (dl != null && dl.isStarted()) {
                icon = Icons.DOWNLOAD_ACTIVE;
            } else {
                icon = Icons.DOWNLOAD;
            }
        } else if (fInfo.isDeleted()) {
            icon = Icons.DELETE;
        } else if (fInfo.isExpected(controller.getFolderRepository())) {
            icon = Icons.EXPECTED;
        } else if (fInfo.getFolder(controller.getFolderRepository()) == null) {
            icon = Icons.EXPECTED;
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
    public static final Icon getGrayIcon(Icon icon) {
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp colorConvertOp = new ColorConvertOp(colorSpace, null);

        Image image = getImageFromIcon(icon);

        // on failure, a colored icon is better than nothing
        if (image == null)
            return icon;

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
                log.error("Could not get icon from IconUIResource", e);
                return null;
            }
        }

        log.error("icon is of unidentified type " + icon.getClass().getName());
        return null;
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
            // The system does not have a screen
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
    private static final boolean hasAlpha(Image image) {
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
        }

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }

    private static boolean isRecentlyCompleted(Folder folder) {
        int completedDls = 0;
        for (Download dl : folder.getController().getTransferManager()
            .getCompletedDownloadsCollection())
        {
            if (dl.getFile().getFolderInfo().equals(folder.getInfo())) {
                completedDls++;
            }
        }
        return completedDls > 0;
    }
}