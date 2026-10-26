/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.skin.Origin;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.LookAndFeelSupport;
import de.dal33t.powerfolder.ui.TrayIconManager;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

import javax.swing.*;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.IconUIResource;
import java.awt.*;
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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    /** Status glyph id for "connecting / waiting" (hourglass); drawn as vector only, no bitmap. */
    public static final String STATUS_WAITING = "status_waiting";
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

    public static final String LOCAL_FOLDER = "local_folder.icon";
    public static final String ONLINE_FOLDER = "online_folder.icon";
    public static final String ONLINE_FOLDER_SMALL = "online_folder_small.icon";
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
    public static final String OPEN_INVITATIONS = "open_invitations.icon";
    public static final String OPEN_INVITATIONS_WHITE = "open_invitations_white.icon";
    public static final String INVITATION_ACCEPT = "invitation_accept.icon";
    public static final String INVITATION_ACCEPT_WHITE = "invitation_accept_white.icon";
    public static final String INVITATION_DECLINE = "invitation_decline.icon";
    public static final String INVITATION_DECLINE_WHITE = "invitation_decline_white.icon";
    public static final String INVITATION_MANAGE = "invitation_manage.icon";
    public static final String INVITATION_MANAGE_WHITE = "invitation_manage_white.icon";

    // Sync icons
    public static final String SYNC_COMPLETE = "sync_complete.icon";
    public static final String SYNC_INCOMPLETE = "sync_incomplete.icon";
    /** Android-style "online only / in the cloud" glyph (blue circular arrows). */
    public static final String SYNC_CLOUD = "sync_cloud.icon";
    /** PFC-3643: sync badges copied from the Android app - green ring+check for the
     *  synced state, blue circular arrows (rotated) for the syncing spinner. */
    public static final String SYNCED_BADGE = "synced_badge.icon";
    public static final String SYNCING_BADGE = "syncing_badge.icon";
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
        "systray_sync09_lowres.icon", "systray_sync10_lowres.icon", "systray_sync11_lowres.icon",
        "systray_sync12_lowres.icon", "systray_sync13_lowres.icon", "systray_sync14_lowres.icon",
        "systray_sync15_lowres.icon", "systray_sync16_lowres.icon", "systray_sync17_lowres.icon",
        "systray_sync18_lowres.icon", "systray_sync19_lowres.icon", "systray_sync20_lowres.icon",
        "systray_sync21_lowres.icon", "systray_sync22_lowres.icon", "systray_sync23_lowres.icon"};
    private static final String[] SYSTRAY_SYNC_ANIMATION_LOW_LIGHT_RES = {
        "systray_sync00_lowres_light.icon", "systray_sync01_lowres_light.icon", "systray_sync02_lowres_light.icon",
        "systray_sync03_lowres_light.icon", "systray_sync04_lowres_light.icon", "systray_sync05_lowres_light.icon",
        "systray_sync06_lowres_light.icon", "systray_sync07_lowres_light.icon", "systray_sync08_lowres_light.icon",
        "systray_sync09_lowres_light.icon", "systray_sync10_lowres_light.icon", "systray_sync11_lowres_light.icon",
        "systray_sync12_lowres_light.icon", "systray_sync13_lowres_light.icon", "systray_sync14_lowres_light.icon",
        "systray_sync15_lowres_light.icon", "systray_sync16_lowres_light.icon", "systray_sync17_lowres_light.icon",
        "systray_sync18_lowres_light.icon", "systray_sync19_lowres_light.icon", "systray_sync20_lowres_light.icon",
        "systray_sync21_lowres_light.icon", "systray_sync22_lowres_light.icon", "systray_sync23_lowres_light.icon"};

    private static final String[] SYSTRAY_SYNC_ANIMATION_HI_RES = {
        "systray_sync00_hires.icon", "systray_sync01_hires.icon", "systray_sync02_hires.icon",
        "systray_sync03_hires.icon", "systray_sync04_hires.icon", "systray_sync05_hires.icon",
        "systray_sync06_hires.icon", "systray_sync07_hires.icon", "systray_sync08_hires.icon",
        "systray_sync09_hires.icon", "systray_sync10_hires.icon", "systray_sync11_hires.icon",
        "systray_sync12_hires.icon", "systray_sync13_hires.icon", "systray_sync14_hires.icon",
        "systray_sync15_hires.icon", "systray_sync16_hires.icon", "systray_sync17_hires.icon",
        "systray_sync18_hires.icon", "systray_sync19_hires.icon", "systray_sync20_hires.icon",
        "systray_sync21_hires.icon", "systray_sync22_hires.icon", "systray_sync23_hires.icon"};
    private static final String[] SYSTRAY_SYNC_ANIMATION_HI_LIGHT_RES = {
        "systray_sync00_hires_light.icon", "systray_sync01_hires_light.icon", "systray_sync02_hires_light.icon",
        "systray_sync03_hires_light.icon", "systray_sync04_hires_light.icon", "systray_sync05_hires_light.icon",
        "systray_sync06_hires_light.icon", "systray_sync07_hires_light.icon", "systray_sync08_hires_light.icon",
        "systray_sync09_hires_light.icon", "systray_sync10_hires_light.icon", "systray_sync11_hires_light.icon",
        "systray_sync12_hires_light.icon", "systray_sync13_hires_light.icon", "systray_sync14_hires_light.icon",
        "systray_sync15_hires_light.icon", "systray_sync16_hires_light.icon", "systray_sync17_hires_light.icon",
        "systray_sync18_hires_light.icon", "systray_sync19_hires_light.icon", "systray_sync20_hires_light.icon",
        "systray_sync21_hires_light.icon", "systray_sync22_hires_light.icon", "systray_sync23_hires_light.icon"};

    public static final String SYSTRAY_SYNC_COMPLETE;
    private static final String SYSTRAY_SYNC_COMPLETE_LOW_RES = "systray_sync_complete_lowres.icon";
    private static final String SYSTRAY_SYNC_COMPLETE_HIGH_RES = "systray_sync_complete_hires.icon";
    private static final String SYSTRAY_SYNC_COMPLETE_LOW_LIGHT_RES = "systray_sync_complete_lowres_light.icon";
    private static final String SYSTRAY_SYNC_COMPLETE_HIGH_LIGHT_RES = "systray_sync_complete_hires_light.icon";

    public static final String SYSTRAY_SYNC_INCOMPLETE;
    private static final String SYSTRAY_SYNC_INCOMPLETE_LOW_RES = "systray_sync_incomplete_lowres.icon";
    private static final String SYSTRAY_SYNC_INCOMPLETE_HIGH_RES = "systray_sync_incomplete_hires.icon";
    private static final String SYSTRAY_SYNC_INCOMPLETE_LOW_LIGHT_RES = "systray_sync_incomplete_lowres_light.icon";
    private static final String SYSTRAY_SYNC_INCOMPLETE_HIGH_LIGHT_RES = "systray_sync_incomplete_hires_light.icon";

    public static final String SYSTRAY_WARNING;
    private static final String SYSTRAY_WARNING_LOW_RES = "systray_warning_lowres.icon";
    private static final String SYSTRAY_WARNING_HIGH_RES = "systray_warning_hires.icon";
    private static final String SYSTRAY_WARNING_LOW_LIGHT_RES = "systray_warning_lowres_light.icon";
    private static final String SYSTRAY_WARNING_HIGH_LIGHT_RES = "systray_warning_hires_light.icon";

    public static final String SYSTRAY_PAUSE;
    private static final String SYSTRAY_PAUSE_LOW_RES = "systray_pause_lowres.icon";
    private static final String SYSTRAY_PAUSE_HIGH_RES = "systray_pause_hires.icon";
    private static final String SYSTRAY_PAUSE_LOW_LIGHT_RES = "systray_pause_lowres_light.icon";
    private static final String SYSTRAY_PAUSE_HIGH_LIGHT_RES = "systray_pause_hires_light.icon";

    public static final String SYSTRAY_INFORMATION;
    private static final String SYSTRAY_INFORMATION_LOW_RES = "systray_information_lowres.icon";
    private static final String SYSTRAY_INFORMATION_HIGH_RES = "systray_information_hires.icon";
    private static final String SYSTRAY_INFORMATION_LOW_LIGHT_RES = "systray_information_lowres_light.icon";
    private static final String SYSTRAY_INFORMATION_HIGH_LIGHT_RES = "systray_information_hires_light.icon";

    public static final String SYSTRAY_SETUP;
    private static final String SYSTRAY_SETUP_LOW_RES = "systray_setup_lowres.icon";
    private static final String SYSTRAY_SETUP_HIGH_RES = "systray_setup_hires.icon";
    private static final String SYSTRAY_SETUP_LOW_LIGHT_RES = "systray_setup_lowres_light.icon";
    private static final String SYSTRAY_SETUP_HIGH_LIGHT_RES = "systray_setup_hires_light.icon";

    static {
        if (!TrayIconManager.isHiRes()) {
            if (!TrayIconManager.isMacMenuBarDarkMode()) {
                SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_LOW_RES;
                SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_LOW_RES;
                SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_LOW_RES;
                SYSTRAY_WARNING = SYSTRAY_WARNING_LOW_RES;
                SYSTRAY_PAUSE = SYSTRAY_PAUSE_LOW_RES;
                SYSTRAY_INFORMATION = SYSTRAY_INFORMATION_LOW_RES;
                SYSTRAY_SETUP = SYSTRAY_SETUP_LOW_RES;
            }
            else {
                SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_LOW_LIGHT_RES;
                SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_LOW_LIGHT_RES;
                SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_LOW_LIGHT_RES;
                SYSTRAY_WARNING = SYSTRAY_WARNING_LOW_LIGHT_RES;
                SYSTRAY_PAUSE = SYSTRAY_PAUSE_LOW_LIGHT_RES;
                SYSTRAY_INFORMATION = SYSTRAY_INFORMATION_LOW_LIGHT_RES;
                SYSTRAY_SETUP = SYSTRAY_SETUP_LOW_LIGHT_RES;
            }
        } else {
            if (!TrayIconManager.isMacMenuBarDarkMode()) {
                SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_HI_RES;
                SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_HIGH_RES;
                SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_HIGH_RES;
                SYSTRAY_WARNING = SYSTRAY_WARNING_HIGH_RES;
                SYSTRAY_PAUSE = SYSTRAY_PAUSE_HIGH_RES;
                SYSTRAY_INFORMATION = SYSTRAY_INFORMATION_HIGH_RES;
                SYSTRAY_SETUP = SYSTRAY_SETUP_HIGH_RES;
            }
            else {
                SYSTRAY_SYNC_ANIMATION = SYSTRAY_SYNC_ANIMATION_HI_LIGHT_RES;
                SYSTRAY_SYNC_COMPLETE = SYSTRAY_SYNC_COMPLETE_HIGH_LIGHT_RES;
                SYSTRAY_SYNC_INCOMPLETE = SYSTRAY_SYNC_INCOMPLETE_HIGH_LIGHT_RES;
                SYSTRAY_WARNING = SYSTRAY_WARNING_HIGH_LIGHT_RES;
                SYSTRAY_PAUSE = SYSTRAY_PAUSE_HIGH_LIGHT_RES;
                SYSTRAY_INFORMATION = SYSTRAY_INFORMATION_HIGH_LIGHT_RES;
                SYSTRAY_SETUP = SYSTRAY_SETUP_HIGH_LIGHT_RES;
            }
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

    /** Map of Username - Icon */
    private static final Map<String, Icon> USERNAME_ICON_MAP = new WeakHashMap<>();

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
        // Flush icon cache
        for (Map.Entry<String, Icon> entry : ID_ICON_MAP.entrySet()) {
            Icon icon = entry.getValue();
            if (icon instanceof ImageIcon) {
                ImageIcon imageIcon = (ImageIcon)icon;
                imageIcon.getImage().flush();
            }
        }
        ID_ICON_MAP.clear();
        STATUS_GLYPH_MAP.clear();
        // Flush image cache
        for (Map.Entry<String, Image> entry : ID_IMAGE_MAP.entrySet()) {
            Image image = entry.getValue();
            image.flush();
        }
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
    /** Whether the UI is in dark mode; monochrome icons are shown white then. */
    private static volatile boolean darkMode;

    /**
     * Monochrome icon FILES (basenames) that must be re-tinted white in dark mode
     * so they stay visible on the dark background. Filename-based (not icon-id)
     * so every action id that maps to one of these PNGs is covered automatically.
     */
    private static final java.util.Set<String> MONOCHROME_ICON_FILES =
        new java.util.HashSet<>(java.util.Arrays.asList(
            // folders
            "Folder.png", "FolderLocal.png", "FolderOnline.png", "LocalFolder.png",
            "LocalAndOnlineFolder.png", "PreviewFolder.png", "OnlineFolderSmall.png",
            "NewFolder.png", "Directory.png", "DirectoryOpen.png",
            // sync / status
            // NOTE: "SyncComplete.png" is intentionally NOT listed: it is now the
            // green Android-style check and must keep its colour in dark mode
            // (whitening would turn it into a white check).
            "SyncInComplete.png", "StartSync.png",
            "information.png", "Warning.png", "Checked.png", "Pause.png",
            // logo + action arrow
            "Logo400UI.png", "ActionArrow.png",
            // common actions/buttons
            "Add.png", "Delete.png", "Edit.png", "Settings.png", "Preferences.png",
            "Cog.png", "FilterTextFieldGlass.png", "Upload.png", "Download.png",
            "Abort.png", "Clear.png", "Open.png", "ArrowLeft.png", "ArrowRight.png",
            // specialised actions/utility (Material Symbols)
            "Advanced.png", "Bug.png", "Collapse.png", "Expand.png", "ConnectBright.png",
            "Details.png", "Dialog.png", "Download_active.png", "DynDns.png", "Expected.png",
            "Files.png", "FindComputers.png", "Group.png", "Inactive.png", "Invite.png",
            "AddFriend.png", "RemoveFriend.png", "Mac.png", "Question.png", "Restore.png",
            "SortBlank.png", "SortDown.png", "SortUp.png", "SystemMonitor.png",
            "Unknown.png", "Updates.png", "WakeUp.png",
            // node / computer status
            "NodeMyself.png", "NodeFriendConnected.png", "NodeNonFriendConnected.png",
            "NodeFriendDisconnected.png", "NodeNonFriendDisconnected.png",
            "NodeFriendPoor.png", "NodeFriendmedium.png", "NodeFriendLan.png"));
    static {
        for (int i = 0; i <= 11; i++) {
            MONOCHROME_ICON_FILES.add(String.format("Sync%02d.png", i));
        }
    }

    /** @return the file name (basename) of a resource path, or the input if none. */
    private static String baseName(String path) {
        if (path == null) {
            return null;
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /**
     * Switch the icon set between light and dark. Call from LookAndFeelSupport
     * whenever it applies the FlatLaf theme.
     */
    public static void setDarkMode(boolean dark) {
        if (dark != darkMode) {
            darkMode = dark;
            // ID_ICON_MAP is keyed per theme (see getIconById); the file-type/extension
            // caches are not, so clear them to pick up the new tint.
            FILETYPE_ICON_MAP.clear();
            EXTENSION_ICON_MAP.clear();
        }
    }

    /** PFC-3643: ids of the sync-spinner animation frames. These no longer map to
     *  the dated grey bundled PNGs - each frame is the Android "syncing" badge (blue
     *  circular arrows) rotated by the frame index (see {@link #badgeIcon}). */
    private static final java.util.Set<String> SYNC_ANIMATION_IDS =
        new java.util.HashSet<>(java.util.Arrays.asList(SYNC_ANIMATION));

    /** Base size (px) of the in-list / badge sync glyph. */
    private static final int SPINNER_BASE_SIZE = 24;

    /** @return the frame position of a SYNC_ANIMATION id (0 if not found). */
    private static int syncFrameIndex(String id) {
        for (int i = 0; i < SYNC_ANIMATION.length; i++) {
            if (SYNC_ANIMATION[i].equals(id)) {
                return i;
            }
        }
        return 0;
    }

    /** PFC-3643: the account-header status glyphs (synced / syncing / pause / ...)
     *  are a white symbol on a filled brand-accent disc - the same colour, shape and
     *  size as the avatar, so they pair cleanly. (Folders/tray keep the Android
     *  badges.) The disc colour is the shared brand accent
     *  ({@link LookAndFeelSupport#getAccentColor()}), resolved at paint time. */

    /** Generated status glyphs and spinner frames, keyed by glyph, size and accent colour. */
    private static final Map<String, Icon> STATUS_GLYPH_MAP = new ConcurrentHashMap<>();

    private static String statusGlyphKey(String glyph, int size) {
        // The badges' backing disc follows the theme, so the theme is part of the key.
        return glyph + '|' + size + '|' + LookAndFeelSupport.getAccentColor().getRGB() + '|' + darkMode;
    }

    /** Scale factors of the generated HiDPI variants (Windows 100 % to 300 %). */
    private static final double[] HIDPI_SCALES = {1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0};

    /** Paints a glyph in 1x user space; the graphics is already scaled to the variant. */
    private interface GlyphPainter {
        void paint(Graphics2D g);
    }

    /**
     * PFC-3643: draws a glyph as vectors once per {@link #HIDPI_SCALES} entry, so every resolution variant is
     * rendered at its native pixel size instead of upscaling a 1x bitmap.
     */
    private static ImageIcon vectorIcon(int w, int h, GlyphPainter painter) {
        Image[] variants = new Image[HIDPI_SCALES.length];
        for (int i = 0; i < variants.length; i++) {
            int vw = (int) Math.round(w * HIDPI_SCALES[i]), vh = (int) Math.round(h * HIDPI_SCALES[i]);
            BufferedImage img = new BufferedImage(vw, vh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.scale(vw / (double) w, vh / (double) h);
                painter.paint(g);
            } finally {
                g.dispose();
            }
            variants[i] = img;
        }
        return new ImageIcon(new java.awt.image.BaseMultiResolutionImage(variants));
    }

    /**
     * Fills the accent disc of a status glyph and sets the pen to white for the symbol. Inset by 0.75 px so
     * the antialiased edge is not clipped when the icon lands on a fractional device position (150 %).
     */
    private static void paintStatusDisc(Graphics2D g, int size) {
        g.setColor(LookAndFeelSupport.getAccentColor());
        g.fill(new java.awt.geom.Ellipse2D.Double(0.75, 0.75, size - 1.5, size - 1.5));
        g.setColor(Color.WHITE);
    }

    /**
     * @return the syncing status glyph: a white arc (rotated per frame) on the accent
     *         disc, at {@code size} px. Account-header status (syncing / uploading /
     *         downloading / connecting); folders/tray keep the Android badges.
     */
    public static Icon getSyncSpinnerFrame(int frame, int size) {
        if (size <= 0) {
            size = SPINNER_BASE_SIZE;
        }
        int f = frame % SYNC_ANIMATION.length;
        final int px = size;
        return STATUS_GLYPH_MAP.computeIfAbsent(statusGlyphKey("spin" + f, size), k -> drawSyncSpinnerFrame(f, px));
    }

    /**
     * @return the syncing status glyph turned by {@code degrees} (fine steps for a smooth animation), at
     *         {@code size} px. The two arrows repeat every 180°.
     */
    public static Icon getSyncSpinnerGlyph(int degrees, int size) {
        int d = Math.floorMod(degrees, 180);
        final int px = size > 0 ? size : SPINNER_BASE_SIZE;
        return STATUS_GLYPH_MAP.computeIfAbsent(statusGlyphKey("spinDeg" + d, px),
            k -> vectorIcon(px, px, g -> paintSyncingBadge(g, 0.75, 0.75, px - 1.5, d)));
    }

    private static Icon drawSyncSpinnerFrame(int frame, int size) {
        // Same as the folder badge; the two arrows repeat every 180°, so the frames step through 180°.
        double degrees = 180.0 * frame / SYNC_ANIMATION.length;
        return vectorIcon(size, size, g -> paintSyncingBadge(g, 0.75, 0.75, size - 1.5, degrees));
    }

    /** Amber of the "warning" status badge. */
    private static final Color WARNING_BADGE_AMBER = new Color(0xD98200);

    /** @return the colour of the neutral status badges (paused, setup): brand accent, light grey-blue when dark. */
    private static Color neutralBadgeColor() {
        return darkMode ? new Color(0xA9B8C8) : LookAndFeelSupport.getAccentColor();
    }

    /**
     * PFC-3643: an account-header status badge in the style of the folder badges: backing disc, coloured ring and
     * {@code symbol} (drawn on the 60-unit grid, pen already in {@code color}), at {@code size} px.
     */
    private static Icon ringBadge(int size, Color color, java.util.function.Consumer<Graphics2D> symbol) {
        return vectorIcon(size, size, g -> {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(0.75, 0.75);
            g2.scale((size - 1.5) / 60.0, (size - 1.5) / 60.0);
            g2.setColor(badgeBacking());
            g2.fill(new java.awt.geom.Ellipse2D.Double(3, 3, 54, 54));
            g2.setColor(color);
            g2.fill(badgeRing());
            symbol.accept(g2);
            g2.dispose();
        });
    }

    /**
     * @return the "all in sync" status glyph: a white checkmark on the accent disc, at
     *         {@code size} px. Account-header only; folders/tray keep the Android badge.
     */
    public static Icon getSyncCompleteIcon(int size) {
        if (size <= 0) {
            size = SPINNER_BASE_SIZE;
        }
        final int px = size;
        return STATUS_GLYPH_MAP.computeIfAbsent(statusGlyphKey("complete", size), k -> drawSyncCompleteIcon(px));
    }

    private static Icon drawSyncCompleteIcon(int size) {
        return vectorIcon(size, size, g -> paintSyncedBadge(g, 0.75, 0.75, size - 1.5));
    }

    /**
     * @return a status glyph for a bundled monochrome icon ({@code iconId}, e.g.
     *         {@link #PAUSE}): the symbol drawn white and centered on the accent disc,
     *         at {@code size} px - so pause etc. match the drawn check/spinner.
     */
    public static Icon getStatusGlyph(String iconId, int size) {
        if (size <= 0) {
            size = SPINNER_BASE_SIZE;
        }
        final int px = size;
        return STATUS_GLYPH_MAP.computeIfAbsent(statusGlyphKey("id:" + iconId, size), k -> drawStatusGlyph(iconId, px));
    }

    private static Icon drawStatusGlyph(String iconId, int size) {
        // PFC-3643: the known states as ring badges like the folder badges; symbols on the 60-unit grid.
        if (WARNING.equals(iconId)) {
            return ringBadge(size, WARNING_BADGE_AMBER, g -> {
                g.fill(new java.awt.geom.RoundRectangle2D.Double(26.5, 15, 7, 21, 7, 7));
                g.fill(new java.awt.geom.Ellipse2D.Double(26, 39.5, 8, 8));
            });
        } else if (INFORMATION.equals(iconId)) {
            return ringBadge(size, SYNCING_BADGE_BLUE, g -> {
                g.fill(new java.awt.geom.Ellipse2D.Double(26, 13, 8, 8));
                g.fill(new java.awt.geom.RoundRectangle2D.Double(26.5, 25, 7, 21, 7, 7));
            });
        } else if (ACTION_ARROW.equals(iconId)) {
            return ringBadge(size, neutralBadgeColor(), g -> {
                g.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.geom.Path2D.Double chevron = new java.awt.geom.Path2D.Double();
                chevron.moveTo(25, 18);
                chevron.lineTo(37, 30);
                chevron.lineTo(25, 42);
                g.draw(chevron);
            });
        } else if (STATUS_WAITING.equals(iconId)) {
            return ringBadge(size, neutralBadgeColor(), g -> {
                g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Double(21, 15, 39, 15));
                g.draw(new java.awt.geom.Line2D.Double(21, 45, 39, 45));
                java.awt.geom.Path2D.Double glass = new java.awt.geom.Path2D.Double();
                glass.moveTo(23.5, 15);
                glass.curveTo(23.5, 25, 30, 26, 30, 30);
                glass.curveTo(30, 34, 23.5, 35, 23.5, 45);
                glass.moveTo(36.5, 15);
                glass.curveTo(36.5, 25, 30, 26, 30, 30);
                glass.curveTo(30, 34, 36.5, 35, 36.5, 45);
                g.draw(glass);
            });
        } else if (PAUSE.equals(iconId)) {
            return ringBadge(size, neutralBadgeColor(), g -> {
                g.fill(new java.awt.geom.RoundRectangle2D.Double(21, 19, 7, 22, 3, 3));
                g.fill(new java.awt.geom.RoundRectangle2D.Double(32, 19, 7, 22, 3, 3));
            });
        }
        // Symbols without a vector shape fall back to the whitened bitmap on the accent disc.
        Icon white = whitenedIcon(iconId);
        return vectorIcon(size, size, g -> {
            paintStatusDisc(g, size);
            double gs = size * 0.46, off = (size - gs) / 2;
            if (white instanceof ImageIcon) {
                g.drawImage(((ImageIcon) white).getImage(), (int) Math.round(off), (int) Math.round(off),
                    (int) Math.round(gs), (int) Math.round(gs), null);
            }
        });
    }

    /** @return the bitmap icon {@code iconId} drawn white, or {@code null} if unknown. */
    private static Icon whitenedIcon(String iconId) {
        Icon glyph = getIconById(iconId);
        return glyph != null ? whiten(glyph) : null;
    }

    /** @return a copy of {@code icon} with every pixel set to white, alpha kept. */
    private static Icon whiten(Icon icon) {
        int w = icon.getIconWidth(), h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return icon;
        }
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = src.getRGB(x, y) >>> 24;
                out.setRGB(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        return new ImageIcon(out);
    }

    /** Green of the "synced" badge ring and check. */
    private static final Color SYNCED_BADGE_GREEN = new Color(0x1F8F30);

    /** Blue of the "syncing" badge arrows. */
    private static final Color SYNCING_BADGE_BLUE = new Color(0x0078D7);

    /** @return the disc behind the sync badges: translucent white, translucent panel colour in dark mode. */
    private static Color badgeBacking() {
        if (darkMode) {
            Color panel = UIManager.getColor("Panel.background");
            Color c = panel != null && panel.getRed() + panel.getGreen() + panel.getBlue() < 384 ? panel
                : new Color(0x2B2D30);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 240);
        }
        return new Color(255, 255, 255, 245);
    }

    /** The coloured ring both sync badges share, on their 60-unit grid. */
    private static java.awt.geom.Area badgeRing() {
        java.awt.geom.Area ring = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(0, 0, 60, 60));
        ring.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(5, 5, 50, 50)));
        return ring;
    }

    /**
     * PFC-3643: the Android "synced" badge as vectors: a green ring with a check on a translucent white disc,
     * in the box ({@code x}, {@code y}, {@code s}).
     */
    private static void paintSyncedBadge(Graphics2D g, double x, double y, double s) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(s / 60.0, s / 60.0); // 60-unit grid of the former SyncedBadge.png
        g2.setColor(badgeBacking());
        g2.fill(new java.awt.geom.Ellipse2D.Double(3, 3, 54, 54));
        g2.setColor(SYNCED_BADGE_GREEN);
        g2.fill(badgeRing());
        g2.setStroke(new BasicStroke(4.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        java.awt.geom.Path2D.Double check = new java.awt.geom.Path2D.Double();
        check.moveTo(18.2, 30.2);
        check.lineTo(26.9, 38.6);
        check.lineTo(42.5, 22.4);
        g2.draw(check);
        g2.dispose();
    }

    /**
     * PFC-3643: the "syncing" badge in the style of the synced badge: the same backing disc and ring in blue, inside
     * two circular arrows with the check's line weight, rotated by {@code degrees} (spinner frames), in the box
     * ({@code x}, {@code y}, {@code s}).
     */
    private static void paintSyncingBadge(Graphics2D g, double x, double y, double s, double degrees) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.scale(s / 60.0, s / 60.0); // 60-unit grid, as the synced badge
        g2.setColor(badgeBacking());
        g2.fill(new java.awt.geom.Ellipse2D.Double(3, 3, 54, 54));
        g2.setColor(SYNCING_BADGE_BLUE);
        g2.fill(badgeRing());
        // One arrow: an arc over the top ending in a head pointing clockwise; the second is it rotated by 180°.
        double r = 13.5, end = Math.toRadians(25);
        java.awt.geom.Area arrow = new java.awt.geom.Area(
            new BasicStroke(4.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(
                new java.awt.geom.Arc2D.Double(30 - r, 30 - r, 2 * r, 2 * r, 170, -140, java.awt.geom.Arc2D.OPEN)));
        double px = 30 + r * Math.cos(end), py = 30 - r * Math.sin(end);
        double dx = Math.sin(end), dy = Math.cos(end); // clockwise tangent
        double nx = Math.cos(end), ny = -Math.sin(end); // outward normal
        java.awt.geom.Path2D.Double head = new java.awt.geom.Path2D.Double();
        head.moveTo(px + dx * 5.5, py + dy * 5.5);
        head.lineTo(px - dx * 1.0 + nx * 5.2, py - dy * 1.0 + ny * 5.2);
        head.lineTo(px - dx * 1.0 - nx * 5.2, py - dy * 1.0 - ny * 5.2);
        head.closePath();
        arrow.add(new java.awt.geom.Area(head));
        java.awt.geom.Area both = new java.awt.geom.Area(arrow);
        both.add(arrow.createTransformedArea(java.awt.geom.AffineTransform.getRotateInstance(Math.PI, 30, 30)));
        g2.rotate(Math.toRadians(degrees), 30, 30);
        g2.fill(both);
        g2.dispose();
    }

    /** @return the vector Android sync badge for a sync-state icon id, or {@code null} if it has none. */
    private static Icon vectorBadgeIcon(String id, int size) {
        if (SYNC_ANIMATION_IDS.contains(id)) {
            double degrees = 360.0 * syncFrameIndex(id) / SYNC_ANIMATION.length;
            return vectorIcon(size, size, g -> paintSyncingBadge(g, 0, 0, size, degrees));
        } else if (SYNC_COMPLETE.equals(id)) {
            return vectorIcon(size, size, g -> paintSyncedBadge(g, 0, 0, size));
        }
        return null;
    }

    /**
     * PFC-3643: the monochrome "open folder" glyph (former Folder.png) as vectors on its 24-unit grid, scaled
     * to {@code size} px in {@code color}.
     */
    private static void paintFolderGlyph(Graphics2D g, double size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(size / 24.0, size / 24.0);
        g2.setColor(color);
        // Back: tab, top bar and the left wall that tapers along the front panel.
        java.awt.geom.Path2D.Double back = new java.awt.geom.Path2D.Double();
        back.moveTo(1.0, 6.0);
        back.curveTo(1.0, 4.9, 1.9, 4.0, 3.0, 4.0);
        back.lineTo(7.9, 4.0);
        back.lineTo(8.9, 5.7);
        back.lineTo(15.8, 5.7);
        back.quadTo(16.35, 5.7, 16.35, 6.25);
        back.lineTo(16.35, 8.15);
        back.lineTo(4.6, 8.15);
        back.lineTo(1.0, 16.2);
        back.closePath();
        g2.fill(back);
        // Front: the slanted panel.
        java.awt.geom.Path2D.Double front = new java.awt.geom.Path2D.Double();
        front.moveTo(5.47, 9.3);
        front.lineTo(22.75, 9.3);
        front.lineTo(18.5, 20.0);
        front.lineTo(1.25, 20.0);
        front.closePath();
        g2.fill(front);
        g2.dispose();
    }

    /** @return the folder glyph colour: black, white in dark mode. */
    private static Color folderGlyphColor() {
        return darkMode ? Color.WHITE : Color.BLACK;
    }

    /** Cache for composite folder+badge icons, keyed by badge id + dark-mode. */
    private static final Map<String, Icon> FOLDER_BADGE_MAP = new HashMap<>();

    /** PFC-3643 feedback: uniform folder-icon container size (matches the account /
     *  status / bottom-bar icons). */
    public static final int FOLDER_ICON_SIZE = 28;

    /** Cache for the plain (no-badge) folder icon, keyed by dark-mode. */
    private static final Map<String, Icon> FOLDER_ICON_MAP = new HashMap<>();

    /**
     * PFC-3643 feedback: the plain folder icon at the uniform {@link #FOLDER_ICON_SIZE}
     * (dark-mode aware, drawn as vectors per HiDPI scale), so not-synced folders match
     * the badged folders and the other 28px icons. Use instead of
     * {@code getIconById(FOLDER)} in the folder list.
     */
    public static Icon getFolderIcon() {
        String key = "plain|" + darkMode;
        Icon cached = FOLDER_ICON_MAP.get(key);
        if (cached != null) {
            return cached;
        }
        Color color = folderGlyphColor();
        Icon result = vectorIcon(FOLDER_ICON_SIZE, FOLDER_ICON_SIZE,
            g -> paintFolderGlyph(g, FOLDER_ICON_SIZE, color));
        FOLDER_ICON_MAP.put(key, result);
        return result;
    }

    /**
     * Android-style folder glyph: the (dark-mode-aware) black/white folder icon
     * with a small coloured sync-state badge in the bottom-right corner, so a
     * folder card shows both the folder shape and its sync state at once.
     *
     * @param badgeIconId the status badge icon id (e.g. {@link #SYNC_COMPLETE},
     *                    {@link #SYNC_CLOUD}, {@link #SYNC_INCOMPLETE}).
     * @return the composite icon, or the plain folder icon if the badge is
     *         missing.
     */
    /**
     * PFC-3643: the folder icon with the syncing badge turned by {@code degrees} (folder-card animation in fine,
     * smooth steps instead of the 12 coarse spinner frames).
     */
    public static Icon getFolderWithSyncingBadge(int degrees) {
        int d = Math.floorMod(degrees, 360);
        String key = "syncing@" + d + '|' + darkMode;
        Icon cached = FOLDER_BADGE_MAP.get(key);
        if (cached != null) {
            return cached;
        }
        Color color = folderGlyphColor();
        double size = FOLDER_ICON_SIZE, bs = size * 0.66, bxy = size - bs - 0.75;
        Icon result = vectorIcon(FOLDER_ICON_SIZE, FOLDER_ICON_SIZE, g -> {
            paintFolderGlyph(g, size, color);
            paintSyncingBadge(g, bxy, bxy, bs, d);
        });
        FOLDER_BADGE_MAP.put(key, result);
        return result;
    }

    public static Icon getFolderWithBadge(String badgeIconId) {
        String key = badgeIconId + '|' + darkMode;
        Icon cached = FOLDER_BADGE_MAP.get(key);
        if (cached != null) {
            return cached;
        }
        // PFC-3643: folder and the synced/syncing badges are drawn as vectors per HiDPI scale.
        // Other badges (cloud, incomplete) are only available as bitmaps.
        boolean vectorBadge = SYNC_ANIMATION_IDS.contains(badgeIconId) || SYNC_COMPLETE.equals(badgeIconId);
        Image badgeImg = vectorBadge ? null : getImageById(badgeIconId);
        if (!vectorBadge && badgeImg == null) {
            return getFolderIcon();
        }
        Color color = folderGlyphColor();
        // The badge keeps 0.75 px off the right/bottom edge so its antialiased ring is never clipped (150 %).
        double size = FOLDER_ICON_SIZE, bs = size * 0.66, bxy = size - bs - 0.75;
        double degrees = SYNC_ANIMATION_IDS.contains(badgeIconId)
            ? 360.0 * syncFrameIndex(badgeIconId) / SYNC_ANIMATION.length : 0;
        Icon result = vectorIcon(FOLDER_ICON_SIZE, FOLDER_ICON_SIZE, g -> {
            paintFolderGlyph(g, size, color);
            if (SYNC_COMPLETE.equals(badgeIconId)) {
                paintSyncedBadge(g, bxy, bxy, bs);
            } else if (vectorBadge) {
                paintSyncingBadge(g, bxy, bxy, bs, degrees);
            } else {
                int ibs = (int) Math.round(bs);
                g.drawImage(badgeImg, FOLDER_ICON_SIZE - ibs, FOLDER_ICON_SIZE - ibs, ibs, ibs, null);
            }
        });
        FOLDER_BADGE_MAP.put(key, result);
        return result;
    }

    /** Cache for bundled file-type glyphicons, keyed by lower-case extension. */
    private static final Map<String, Icon> FILETYPE_ICON_MAP = new HashMap<>();
    private static final Icon NO_FILETYPE_ICON = new ImageIcon();
    private static final String FILETYPE_ICON_BASE =
        "de/dal33t/powerfolder/skin/origin/icons/filetypes/";

    /**
     * @return a bundled monochrome file-type glyphicon (from the iOS glyphicons
     *         set) for the given extension, re-tinted white in dark mode, or
     *         {@code null} if none is bundled for that extension.
     */
    public static Icon getFileTypeIcon(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        String ext = extension.toLowerCase();
        Icon cached = FILETYPE_ICON_MAP.get(ext);
        if (cached != null) {
            return cached == NO_FILETYPE_ICON ? null : cached;
        }
        java.net.URL url = Thread.currentThread().getContextClassLoader()
            .getResource(FILETYPE_ICON_BASE + ext + ".png");
        if (url == null) {
            FILETYPE_ICON_MAP.put(ext, NO_FILETYPE_ICON);
            return null;
        }
        Icon icon = new ImageIcon(url);
        if (darkMode) {
            icon = whiten(icon);
        }
        FILETYPE_ICON_MAP.put(ext, icon);
        return icon;
    }

    public static Icon getIconById(String id) {
        if (id == null) {
            log.severe("Icon id null ???");
            return null;
        }

        if (UNKNOWN_ICONS.contains(id)) {
            // Already discovered that we do not know this one.
            return null;
        }

        // Keyed per theme, so an icon cached before the theme was applied is never
        // served with the wrong tint. See setDarkMode.
        String key = darkMode ? id + "#dark" : id;
        Icon icon = ID_ICON_MAP.get(key);
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
                ID_ICON_MAP.put(key, icon);
            }
            return icon;
        } else if (id.equals(UNKNOWN_FILE_RED)) {
            icon = convertToRed(getGrayIcon(getIconById(UNKNOWN_FILE)));
            if (icon != null) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Cached icon " + id);
                }
                ID_ICON_MAP.put(key, icon);
            }
            return icon;
        }

        String iconId = getIconId(id);
        if (iconId == null) {
            // Log it.
            if (log.isLoggable(Level.FINER)) {
                log.finer("Icon not found ID: '" + id + '\'');
            }
            UNKNOWN_ICONS.add(id);
            return null;
        }

        // If image file exists in skin folder, load it. If it does not exist, load the image file from the jar.
        URL iconURL;
        if (Files.exists(Paths.get(iconId))) {
            try {
                if (OSUtil.isWindowsSystem()) {
                    iconURL = new URL("file:///" + iconId);
                }
                else {
                    iconURL = new URL("file://" + iconId);
                }
            } catch (MalformedURLException e) {
                log.severe("Invalid icon URL");
                return null;
            }
        }
        else {
            iconURL = Thread.currentThread().getContextClassLoader()
                .getResource(iconId);
            if (iconURL == null) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Icon not found '" + id + '\'');
                }
                return null;
            }
        }

        // Resource path of the bitmap actually shown, for its optional "@4x" sibling (logo only).
        String shownPath = iconId;
        if (darkMode) {
            // Prefer a hand-made white variant "<name>_dark.png" bundled next to
            // the icon (e.g. the proper white PowerFolder logo from PFM);
            // otherwise auto-whiten known monochrome glyphs. Brand logos have no
            // _dark sibling, so they fall through to whitening their own artwork.
            java.net.URL darkURL = darkVariantURL(iconId);
            if (darkURL != null) {
                icon = new ImageIcon(darkURL);
                shownPath = darkVariantPath(iconId);
            } else {
                shownPath = null; // whitened at runtime: a light @4x sibling would not match
                icon = new ImageIcon(iconURL);
                if (MONOCHROME_ICON_FILES.contains(baseName(iconId))) {
                    icon = whiten(icon);
                }
            }
        } else {
            icon = new ImageIcon(iconURL);
        }
        // PFC-3643: the header brand logo and the "powered by" strip are wide
        // bitmaps excluded from the small-icon HiDPI path (> HIDPI_MAX_BASE). Give
        // them HiDPI variants, downscaled from a bundled "@4x" master when there is
        // one (e.g. PoweredBy@4x.png), so they stay sharp on scaled displays.
        if ((LOGO400UI.equals(id) || POWERED_BY.equals(id)) && icon instanceof ImageIcon) {
            icon = toHiDpiIcon((ImageIcon) icon, shownPath != null ? hiResVariantURL(shownPath) : null);
        }
        // PFC-3643: sync glyphs use the Android badges, drawn as vectors - the "syncing"
        // badge (blue arrows) rotated per frame, and the "synced" badge (green ring + check).
        Icon badge = vectorBadgeIcon(id, SPINNER_BASE_SIZE);
        if (badge != null) {
            icon = badge;
        }
        if (log.isLoggable(Level.FINER)) {
            log.finer("Cached icon " + id);
        }
        ID_ICON_MAP.put(key, icon);
        return icon;
    }

    /**
     * PFC-3643: wrap a brand bitmap in a {@link java.awt.image.BaseMultiResolutionImage}
     * so it renders sharply on HiDPI/scaled displays. With a high-resolution master
     * ({@code hiResURL}, same layout) every variant is downscaled from it; without one
     * only bicubic 2x/3x upscales are possible, which add no detail. Returns the
     * original icon on any failure.
     */
    private static Icon toHiDpiIcon(ImageIcon src, URL hiResURL) {
        try {
            int w = src.getIconWidth(), h = src.getIconHeight();
            if (w <= 0 || h <= 0) {
                return src;
            }
            BufferedImage base = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = base.createGraphics();
            g.drawImage(src.getImage(), 0, 0, null);
            g.dispose();
            BufferedImage master = hiResURL != null ? javax.imageio.ImageIO.read(hiResURL) : null;
            if (master == null || master.getWidth() <= w * 2 || master.getHeight() <= h * 2) {
                return new ImageIcon(new java.awt.image.BaseMultiResolutionImage(base,
                    upscale(base, 2), upscale(base, 3)));
            }
            // The hand-tuned 1x bitmap stays the base; higher scales come from the master.
            List<Image> variants = new ArrayList<>();
            variants.add(base);
            for (double s : HIDPI_SCALES) {
                int vw = (int) Math.round(w * s), vh = (int) Math.round(h * s);
                if (s > 1.0 && vw < master.getWidth()) {
                    variants.add(downscale(master, vw, vh));
                }
            }
            variants.add(master);
            return new ImageIcon(new java.awt.image.BaseMultiResolutionImage(variants.toArray(new Image[0])));
        } catch (Exception e) {
            log.warning(hiResURL + ": HiDPI logo variants not created. " + e);
            return src;
        }
    }

    /**
     * @return {@code src} downscaled to {@code w} x {@code h} in halving steps on premultiplied
     *         alpha, so edges neither alias nor pick up dark fringes.
     */
    private static BufferedImage downscale(BufferedImage src, int w, int h) {
        BufferedImage cur = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g0 = cur.createGraphics();
        g0.drawImage(src, 0, 0, null);
        g0.dispose();
        int cw = cur.getWidth(), ch = cur.getHeight();
        do {
            cw = Math.max(w, cw / 2);
            ch = Math.max(h, ch / 2);
            BufferedImage next = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, cw, ch, null);
            g.dispose();
            cur = next;
        } while (cw > w || ch > h);
        return cur;
    }

    /**
     * @return the URL of the optional high-resolution master "&lt;name&gt;@4x.png" next to
     *         the given icon resource or file, or {@code null} if there is none.
     */
    private static URL hiResVariantURL(String path) {
        int dot = path.toLowerCase().lastIndexOf(".png");
        if (dot < 0) {
            return null;
        }
        String hiRes = path.substring(0, dot) + "@4x" + path.substring(dot);
        try {
            if (Files.exists(Paths.get(hiRes))) {
                return Paths.get(hiRes).toUri().toURL();
            }
        } catch (InvalidPathException | MalformedURLException e) {
            return null;
        }
        return Thread.currentThread().getContextClassLoader().getResource(hiRes);
    }

    /**
     * @return the URL of a bundled dark-mode variant "&lt;name&gt;_dark.png"
     *         sitting next to the given classpath icon resource, or {@code null}
     *         if there is none (or the id already refers to a _dark asset).
     */
    private static java.net.URL darkVariantURL(String iconId) {
        String darkId = darkVariantPath(iconId);
        return darkId != null ? Thread.currentThread().getContextClassLoader().getResource(darkId) : null;
    }

    /** @return the "&lt;name&gt;_dark.png" path for an icon path, or {@code null} if not applicable. */
    private static String darkVariantPath(String iconId) {
        if (iconId == null) {
            return null;
        }
        int dot = iconId.toLowerCase().lastIndexOf(".png");
        if (dot < 5 || iconId.regionMatches(true, dot - 5, "_dark", 0, 5)) {
            return null;
        }
        return iconId.substring(0, dot) + "_dark" + iconId.substring(dot);
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

        // If image file exists in skin folder, load it. If it does not exist, load the image file from the jar.
        URL imageURL;
        if (Files.exists(Paths.get(iconId))) {
            try {
                if (OSUtil.isWindowsSystem()) {
                    imageURL = new URL("file:///" + iconId);
                }
                else {
                    imageURL = new URL("file://" + iconId);
                }
            } catch (MalformedURLException e) {
                log.severe("Invalid icon URL");
                return null;
            }
        }
        else {
            imageURL = Thread.currentThread().getContextClassLoader()
                .getResource(iconId);
            if (imageURL == null) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Image not found '" + id + '\'');
                }
                return null;
            }
        }

        // PFC-3643: wrap small bitmap icons in a multi-resolution image so they
        // render smoothly on HiDPI/scaled displays instead of upscaling the bare
        // low-res PNG (which looked blocky/"destroyed" on Windows scaling). Large
        // images (logos, splash) keep the original async Toolkit loading.
        image = loadHiDpiImage(imageURL);
        if (image == null) {
            image = Toolkit.getDefaultToolkit().getImage(imageURL);
        }
        if (log.isLoggable(Level.FINER)) {
            log.finer("Cached image " + id);
        }
        ID_IMAGE_MAP.put(id, image);

        return image;
    }

    /** Max base size (px) for which we generate HiDPI resolution variants. */
    private static final int HIDPI_MAX_BASE = 128;

    /**
     * PFC-3643: load a small icon as a {@link java.awt.image.BaseMultiResolutionImage}
     * with 2x and 3x bicubic-upscaled variants, so a scaled (HiDPI) display picks a
     * higher-resolution variant and the icon no longer looks pixelated. Returns
     * {@code null} for large images or on any failure, so the caller falls back to
     * the plain {@code Toolkit.getImage} path.
     *
     * @param url the icon resource/file URL.
     * @return a multi-resolution image, or {@code null} to use the fallback loader.
     */
    private static Image loadHiDpiImage(URL url) {
        try {
            java.awt.image.BufferedImage base = javax.imageio.ImageIO.read(url);
            if (base == null) {
                return null;
            }
            int w = base.getWidth(), h = base.getHeight();
            if (w <= 0 || h <= 0 || w > HIDPI_MAX_BASE || h > HIDPI_MAX_BASE) {
                return null; // large image -> original loader
            }
            return new java.awt.image.BaseMultiResolutionImage(base,
                upscale(base, 2), upscale(base, 3));
        } catch (Exception e) {
            return null;
        }
    }

    /** High-quality (bicubic) integer upscale used for HiDPI icon variants. */
    private static java.awt.image.BufferedImage upscale(
        java.awt.image.BufferedImage src, int factor)
    {
        int w = src.getWidth() * factor, h = src.getHeight() * factor;
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(
            w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
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

    /**
     * @param member The account to retrieve the avatar for
     * @param controller
     * @return Return the avatar uploaded by the user.
     */
    public static Icon getIconByAccount(AccountInfo member,
        Controller controller)
    {
        String username = member != null ? member.getUsername() : null;

        if (username == null) {
            return getIconById(NODE_DISCONNECTED);
        } else if (USERNAME_ICON_MAP.containsKey(username)) {
            return USERNAME_ICON_MAP.get(username);
        } else {
            new IconBackgroundLoader(member, controller).execute();
            return getIconById(NODE_DISCONNECTED);
        }
    }

    private static class IconBackgroundLoader extends SwingWorker<Void, Void> {
        private final AccountInfo member;
        private final Controller controller;
        private ImageIcon image;

        IconBackgroundLoader(AccountInfo member, Controller controller) {
            this.member = member;
            this.controller = controller;
        }

        @Override
        protected Void doInBackground() throws Exception {
            String urlString = controller.getOSClient().getAvatarURL(member,
                true);

            try {
                URL url = new URL(urlString);

                HttpURLConnection con = (HttpURLConnection) url
                    .openConnection();
                con.setRequestMethod("GET");
                con.connect();
                int code = con.getResponseCode();

                if (code == 200) {
                    ImageIcon tempIcon = new ImageIcon(url);
                    Image img = tempIcon.getImage().getScaledInstance(25, 25,
                        Image.SCALE_SMOOTH);
                    image = new ImageIcon(img);
                }
            } catch (MalformedURLException e) {
                log.warning("Avatar URL was malformed: " + urlString);
            } catch (IOException e) {
                log.warning(e.getMessage());
            }

            return null;
        }

        @Override
        protected void done() {
            USERNAME_ICON_MAP.put(member.getUsername(), image);
            controller.getUIController().getActiveFrame().repaint();
        }
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
                // Prefer the bundled monochrome file-type glyphicon; fall back to
                // the OS system icon.
                icon = getFileTypeIcon(extension);
                if (icon == null) {
                    icon = FileSystemView.getFileSystemView().getSystemIcon(file.toFile());
                }
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
                    icon = getFileTypeIcon(extension);
                    if (icon == null) {
                        icon = getIconExtension(extension);
                    }
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

    public static Image getGrayImage(Image image) {
//        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//        ColorConvertOp colorConvertOp = new ColorConvertOp(colorSpace, null);
//
//        BufferedImage src = toBufferedImage(image);
//
//        BufferedImage des = colorConvertOp.filter(src, null);

        BufferedImage src = toBufferedImage(image);

        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY);

//        Graphics2D graphic = result.createGraphics();
//        graphic.drawImage(src, 0, 0, Color,WHITE, null);
//        graphic.dispose();

//        Graphics g = src.getGraphics();
//        g.drawImage(colorImage, 0, 0, null);
//        g.dispose();

        return result;
    }

}
