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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * Utility class to find common user directories.
 * 
 * @author sprajc
 */
public class UserDirectories {

    private static Map<String, File> userDirectories = new TreeMap<String, File>();

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
    private static final String USER_DIR_LIBRARY = "Library";
    private static final String USER_DIR_SITES = "Sites";

    // Vista has issues with these, so instantiate separately
    private static String userDirMyDocuments;
    private static String userDirMyMusic;
    private static String userDirMyPictures;
    private static String userDirMyVideos;
    private static String appsDirOutlook;

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
            appsDirOutlook = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_LOCAL_SETTINGS_APP_DATA, false)
                + File.separator + "Microsoft" + File.separator + "Outlook";
            userDirMyDocuments = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_PERSONAL, false);
            userDirMyMusic = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYMUSIC, false);
            userDirMyPictures = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYPICTURES, false);
            userDirMyVideos = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYVIDEO, false);
        }
    }

    /**
     * @return all user directories name -> target location on disk.
     */
    public static Map<String, File> getUserDirectories() {
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
    public static Map<String, File> getUserDirectoriesFiltered(
        Controller controller)
    {
        Map<String, File> filteredDirs = new TreeMap<String, File>(
            getUserDirectories());
        for (Iterator<File> it = filteredDirs.values().iterator(); it.hasNext();)
        {
            File directory = (File) it.next();

            // See if any folders already exists for this directory.
            // No reason to show if already subscribed.
            for (Folder folder : controller.getFolderRepository().getFolders())
            {
                if (folder.getDirectory().getAbsoluteFile().equals(directory)) {
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
        addTargetDirectory(userHome, USER_DIR_CONTACTS, Translation
            .getTranslation("user.dir.contacts"), false);
        addTargetDirectory(userHome, USER_DIR_DESKTOP, Translation
            .getTranslation("user.dir.desktop"), false);
        addTargetDirectory(userHome, USER_DIR_DOCUMENTS, Translation
            .getTranslation("user.dir.documents"), false);
        addTargetDirectory(userHome, USER_DIR_EVOLUTION, Translation
            .getTranslation("user.dir.evolution"), true);
        addTargetDirectory(userHome, USER_DIR_FAVORITES, Translation
            .getTranslation("user.dir.favorites"), false);
        addTargetDirectory(userHome, USER_DIR_LINKS, Translation
            .getTranslation("user.dir.links"), false);
        addTargetDirectory(userHome, USER_DIR_MUSIC, Translation
            .getTranslation("user.dir.music"), false);
        addTargetDirectory(userHome, USER_DIR_MOVIES, Translation
            .getTranslation("user.dir.movies"), false);
        addTargetDirectory(userHome, USER_DIR_VIDEOS, Translation
            .getTranslation("user.dir.videos"), false);
        addTargetDirectory(userHome, USER_DIR_DOWNLOADS, Translation
            .getTranslation("user.dir.downloads"), false);
        addTargetDirectory(userHome, USER_DIR_PUBLIC, Translation
            .getTranslation("user.dir.public"), false);
        addTargetDirectory(userHome, USER_DIR_SITES, Translation
            .getTranslation("user.dir.sites"), false);

        // Hidden by Vista.
        if (userDirMyDocuments != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyDocuments), Translation
                .getTranslation("user.dir.my_documents"), false);
        }
        if (userDirMyMusic != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyMusic), Translation
                .getTranslation("user.dir.my_music"), false);
        }
        if (userDirMyPictures != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyPictures), Translation
                .getTranslation("user.dir.my_pictures"), false);
        }
        if (userDirMyVideos != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyVideos), Translation
                .getTranslation("user.dir.my_videos"), false);
        }

        addTargetDirectory(userHome, USER_DIR_PICTURES, Translation
            .getTranslation("user.dir.pictures"), false);
        addTargetDirectory(userHome, USER_DIR_RECENT_DOCUMENTS, Translation
            .getTranslation("user.dir.recent_documents"), false);
        addTargetDirectory(userHome, USER_DIR_VIDEOS, Translation
            .getTranslation("user.dir.videos"), false);

        addTargetDirectory(userHome, USER_DIR_VIDEOS, Translation
            .getTranslation("user.dir.videos"), false);

        if (OSUtil.isWindowsSystem()) {
            String appDataname = Util.getAppData();
            if (appDataname != null) {
                File appData = new File(appDataname);
                addTargetDirectory(appData, APPS_DIR_FIREFOX, Translation
                    .getTranslation("apps.dir.firefox"), false);
                addTargetDirectory(appData, APPS_DIR_SUNBIRD, Translation
                    .getTranslation("apps.dir.sunbird"), false);
                addTargetDirectory(appData, APPS_DIR_THUNDERBIRD, Translation
                    .getTranslation("apps.dir.thunderbird"), false);
                if (appsDirOutlook != null) {
                    addTargetDirectory(appData, appsDirOutlook, Translation
                        .getTranslation("apps.dir.outlook"), false);
                }
            } else {
                Logger.getAnonymousLogger().severe(
                    "Application data directory not found.");
            }
        } else if (OSUtil.isLinux()) {
            File appData = new File("/etc");
            addTargetDirectory(appData, APPS_DIR_FIREFOX2, Translation
                .getTranslation("apps.dir.firefox"), false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD2, Translation
                .getTranslation("apps.dir.sunbird"), false);
            addTargetDirectory(appData, APPS_DIR_THUNDERBIRD2, Translation
                .getTranslation("apps.dir.thunderbird"), false);
        } else if (OSUtil.isMacOS()) {
            File appData = new File(userHome, "Library");
            addTargetDirectory(appData, APPS_DIR_FIREFOX, Translation
                .getTranslation("apps.dir.firefox"), false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD, Translation
                .getTranslation("apps.dir.sunbird"), false);
            addTargetDirectory(appData, APPS_DIR_THUNDERBIRD, Translation
                .getTranslation("apps.dir.thunderbird"), false);
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
        String translation, boolean allowHidden)
    {
        File directory = joinFile(root, subdir);
        addTargetDirectory(directory, translation, allowHidden);
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
    private static void addTargetDirectory(File directory, String translation,
        boolean allowHidden)
    {
        if (directory.exists() && directory.isDirectory()
            && (allowHidden || !directory.isHidden()))
        {
            userDirectories.put(translation, directory);
        }
    }
}
