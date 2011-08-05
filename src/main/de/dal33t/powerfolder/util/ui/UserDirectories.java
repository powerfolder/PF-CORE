/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: ChooseDiskLocationPanel.java 9522 2009-09-11 16:47:01Z harry $
 */
package de.dal33t.powerfolder.util.ui;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Utility class to find common user directories.
 * 
 * @author sprajc
 */
public class UserDirectories {

    private static final Map<String, UserDirectory> userDirectories = new TreeMap<String, UserDirectory>();

    // Some standard user directory names from various OS.
    private static final String USER_DIR_CONTACTS = "Contacts";
    private static final String USER_DIR_DESKTOP = "Desktop";
    private static final String USER_DIR_DOCUMENTS = "Documents";
    // Ubuntu mail client
    private static final String USER_DIR_EVOLUTION = ".evolution";
    private static final String USER_DIR_FAVORITES = "Favorites";
    private static final String USER_DIR_LINKS = "Links";
    private static final String USER_DIR_MUSIC = "Music";
    private static final String USER_DIR_PICTURES = "Pictures";
    private static final String USER_DIR_RECENT_DOCUMENTS = "Recent Documents";
    private static final String USER_DIR_VIDEOS = "Videos";
    // Mac additionals
    private static final String USER_DIR_MOVIES = "Movies";
    private static final String USER_DIR_DOWNLOADS = "Downloads";
    private static final String USER_DIR_PUBLIC = "Public";
    private static final String USER_DIR_SITES = "Sites";
    private static final String USER_DIR_DROPBOX = "My Dropbox";

    // Vista has issues with these, so instantiate separately
    private static final String USER_DIR_MY_DOCUMENTS;
    private static final String USER_DIR_MY_MUSIC;
    private static final String USER_DIR_MY_PICTURES;
    private static final String USER_DIR_MY_VIDEOS;
    private static final String APPS_DIR_OUTLOOK;
    private static final String APPS_DIR_WINDOWS_MAIL;

    private static final String APPS_DIR_FIREFOX = "Mozilla" + File.separator
        + "Firefox";
    private static final String APPS_DIR_SUNBIRD = "Mozilla" + File.separator
        + "Sunbird";
    private static final String APPS_DIR_THUNDERBIRD = "Thunderbird";
    private static final String APPS_DIR_FIREFOX2 = "firefox"; // Linux
    private static final String APPS_DIR_SUNBIRD2 = "sunbird"; // Linux
    private static final String APPS_DIR_THUNDERBIRD2 = "thunderbird"; // Linux

    static {
        if (WinUtils.getInstance() != null) {
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                APPS_DIR_OUTLOOK = WinUtils.getInstance().getSystemFolderPath(
                    WinUtils.CSIDL_LOCAL_APP_DATA, false)
                    + File.separator + "Microsoft" + File.separator + "Outlook";
            } else {
                APPS_DIR_OUTLOOK = null;
            }
            USER_DIR_MY_DOCUMENTS = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_PERSONAL, false);
            USER_DIR_MY_MUSIC = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYMUSIC, false);
            USER_DIR_MY_PICTURES = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYPICTURES, false);
            USER_DIR_MY_VIDEOS = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYVIDEO, false);
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                APPS_DIR_WINDOWS_MAIL = WinUtils.getInstance()
                    .getSystemFolderPath(WinUtils.CSIDL_LOCAL_APP_DATA, false)
                    + File.separator
                    + "Microsoft"
                    + File.separator
                    + "Windows Mail";
            } else {
                APPS_DIR_WINDOWS_MAIL = null;
            }
        } else {
            USER_DIR_MY_DOCUMENTS = null;
            USER_DIR_MY_MUSIC = null;
            USER_DIR_MY_PICTURES = null;
            USER_DIR_MY_VIDEOS = null;
            APPS_DIR_OUTLOOK = null;
            APPS_DIR_WINDOWS_MAIL = null;
        }
    }

    /**
     * @return the "My documents". Only available on Windows XP.
     */
    public static String getMyDocuments() {
        return USER_DIR_MY_DOCUMENTS;
    }

    /**
     * @return the "My music". Only available on Windows XP.
     */
    public static String getMyMusic() {
        return USER_DIR_MY_MUSIC;
    }

    /**
     * @return the "My videos". Only available on Windows XP.
     */
    public static String getMyVideos() {
        return USER_DIR_MY_VIDEOS;
    }

    /**
     * @return the "My pictures". Only available on Windows XP.
     */
    public static String getMyPictures() {
        return USER_DIR_MY_PICTURES;
    }

    /**
     * @return all user directories name -> target location on disk.
     */
    public static Map<String, UserDirectory> getUserDirectories() {
        if (userDirectories.isEmpty()) {
            synchronized (userDirectories) {
                findUserDirectories();
            }
        }
        return userDirectories;
    }

    /**
     * @param controller
     * @return @return user directories that are not setup as folder yet. name
     *         -> target location on disk.
     */
    public static Map<String, UserDirectory> getUserDirectoriesFiltered(
        Controller controller)
    {
        Map<String, UserDirectory> filteredDirs = new TreeMap<String, UserDirectory>(
            getUserDirectories());
        for (Iterator<UserDirectory> it = filteredDirs.values().iterator(); it
            .hasNext();)
        {
            File directory = it.next().getDirectory();
            // See if any folders already exists for this directory.
            // No reason to show if already subscribed.
            for (Folder folder : controller.getFolderRepository().getFolders())
            {
                if (folder.getLocalBase().equals(directory)) {
                    it.remove();
                    break;
                }
            }
        }
        return filteredDirs;
    }

    /**
     * Find some generic user directories. Not all will be valid for all os, but
     * that is okay.
     */
    private static void findUserDirectories() {
        File userHome = new File(System.getProperty("user.home"));
        addTargetDirectory(userHome, USER_DIR_CONTACTS, "user.dir.contacts",
            false);
        addTargetDirectory(userHome, USER_DIR_DESKTOP, "user.dir.desktop",
            false);

        addTargetDirectory(userHome, USER_DIR_EVOLUTION, "user.dir.evolution",
            true);
        addTargetDirectory(userHome, USER_DIR_FAVORITES, "user.dir.favorites",
            false);
        addTargetDirectory(userHome, USER_DIR_LINKS, "user.dir.links", false);
        addTargetDirectory(userHome, USER_DIR_MOVIES, "user.dir.movies", false);
        addTargetDirectory(userHome, USER_DIR_DOWNLOADS, "user.dir.downloads",
            false);
        addTargetDirectory(userHome, USER_DIR_PUBLIC, "user.dir.public", false);
        addTargetDirectory(userHome, USER_DIR_SITES, "user.dir.sites", false);
        addTargetDirectory(userHome, USER_DIR_DROPBOX, "user.dir.dropbox",
            false);

        boolean foundDocuments = false;
        boolean foundMusic = false;
        boolean foundPictures = false;
        boolean foundVideos = false;
        // Hidden by Vista and 7
        if (OSUtil.isWindowsSystem()) {

            if (USER_DIR_MY_DOCUMENTS != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundDocuments = addTargetDirectory(new File(
                    USER_DIR_MY_DOCUMENTS), "user.dir.my_documents", false,
                    "user.dir.documents");
            }

            if (USER_DIR_MY_MUSIC != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundMusic = addTargetDirectory(new File(USER_DIR_MY_MUSIC),
                    "user.dir.my_music", false, "user.dir.music");
            }
            if (USER_DIR_MY_PICTURES != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundPictures = addTargetDirectory(new File(
                    USER_DIR_MY_PICTURES), "user.dir.my_pictures", false,
                    "user.dir.pictures");
            }
            if (USER_DIR_MY_VIDEOS != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundVideos = addTargetDirectory(new File(USER_DIR_MY_VIDEOS),
                    "user.dir.my_videos", false, "user.dir.videos");
            }
        }

        if (!foundDocuments) {
            addTargetDirectory(userHome, USER_DIR_DOCUMENTS,
                "user.dir.documents", false);
        }
        if (!foundMusic) {
            addTargetDirectory(userHome, USER_DIR_MUSIC, "user.dir.music",
                false);
        }
        if (!foundPictures) {
            addTargetDirectory(userHome, USER_DIR_PICTURES,
                "user.dir.pictures", false);
        }
        if (!foundVideos) {
            addTargetDirectory(userHome, USER_DIR_VIDEOS, "user.dir.videos",
                false);
        }

        addTargetDirectory(userHome, USER_DIR_RECENT_DOCUMENTS,
            "user.dir.recent_documents", false);
        addTargetDirectory(userHome, USER_DIR_VIDEOS, "user.dir.videos", false);

        if (OSUtil.isWindowsSystem()) {
            String appDataname = WinUtils.getAppDataCurrentUser();
            if (appDataname != null) {
                File appData = new File(appDataname);
                addTargetDirectory(appData, "apps.dir", true);
                addTargetDirectory(appData, APPS_DIR_FIREFOX,
                    "apps.dir.firefox", false);
                addTargetDirectory(appData, APPS_DIR_SUNBIRD,
                    "apps.dir.sunbird", false);
                if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                    addTargetDirectory(appData, APPS_DIR_THUNDERBIRD,
                        "apps.dir.thunderbird", false);
                }
                if (APPS_DIR_OUTLOOK != null) {
                    addTargetDirectory(new File(APPS_DIR_OUTLOOK),
                        "apps.dir.outlook", false);
                }
                if (APPS_DIR_WINDOWS_MAIL != null) {
                    addTargetDirectory(new File(APPS_DIR_WINDOWS_MAIL),
                        "apps.dir.windows_mail", false);
                }
            } else {
                Logger.getAnonymousLogger().severe(
                    "Application data directory not found.");
            }
        } else if (OSUtil.isLinux()) {
            File appData = new File("/etc");
            addTargetDirectory(appData, APPS_DIR_FIREFOX2, "apps.dir.firefox",
                false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD2, "apps.dir.sunbird",
                false);
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                addTargetDirectory(appData, APPS_DIR_THUNDERBIRD2,
                    "apps.dir.thunderbird", false);
            }
        } else if (OSUtil.isMacOS()) {
            File appData = new File(userHome, "Library");
            addTargetDirectory(appData, APPS_DIR_FIREFOX, "apps.dir.firefox",
                false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD, "apps.dir.sunbird",
                false);
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                addTargetDirectory(appData, APPS_DIR_THUNDERBIRD,
                    "apps.dir.thunderbird", false);
            }
        }
    }

    /**
     * Adds a generic user directory if if exists for this os.
     * 
     * @param root
     * @param subdir
     * @param translation
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private static void addTargetDirectory(File root, String subdir,
        String translationId, boolean allowHidden)
    {
        File directory = joinFile(root, subdir);
        addTargetDirectory(directory, translationId, allowHidden);
    }

    private static File joinFile(File root, String subdir) {
        return new File(root + File.separator + subdir);
    }

    /**
     * Adds a generic user directory if if exists for this os.
     * 
     * @param translation
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private static void addTargetDirectory(File directory,
        String translationId, boolean allowHidden)
    {
        addTargetDirectory(directory, translationId, allowHidden, translationId);
    }

    /**
     * Adds a generic user directory if if exists for this os.
     */
    private static boolean addTargetDirectory(File directory,
        String translationId, boolean allowHidden, String placeholder)
    {
        if (directory.exists() && directory.isDirectory()
            && (allowHidden || !directory.isHidden()))
        {
            String translation = Translation.getTranslation(translationId);
            UserDirectory userDir = new UserDirectory(translation,
                '$' + placeholder, directory);
            userDirectories.put(translation, userDir);
            return true;
        }
        return false;
    }
}
