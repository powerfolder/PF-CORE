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
package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.disk.Folder;
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
    private static final String USER_DIR_DOCUMENTS_DEFAULT = "Documents";
    private static final String USER_DIR_MUSIC_DEFAULT = "Music";
    private static final String USER_DIR_PICTURES_DEFAULT = "Pictures";
    private static final String USER_DIR_VIDEOS_DEFAULT = "Videos";
    private static final String USER_DIR_FAVORITES_DEFAULT = "Favorites";
    private static final String USER_DIR_LINKS = "Links";
    private static final String USER_DIR_RECENT_DOCUMENTS = "Recent Documents";
    private static final String USER_DIR_CONTACTS = "Contacts";
    private static final String USER_DIR_DESKTOP = "Desktop";

    // Vista has issues with these, so instantiate separately
    private static final String USER_DIR_DOCUMENTS_REPORTED;
    private static final String USER_DIR_MUSIC_REPORTED;
    private static final String USER_DIR_PICTURES_REPORTED;
    private static final String USER_DIR_VIDEOS_REPORTED;
    private static final String USER_DIR_FAVORITES_REPORTED;
    private static final String APPS_DIR_OUTLOOK;
    private static final String APPS_DIR_WINDOWS_MAIL;

    // Ubuntu mail client
    private static final String USER_DIR_EVOLUTION = ".evolution";
    // Mac additionals
    private static final String USER_DIR_MOVIES = "Movies";
    private static final String USER_DIR_DOWNLOADS = "Downloads";
    private static final String USER_DIR_PUBLIC = "Public";
    private static final String USER_DIR_SITES = "Sites";
    private static final String USER_DIR_DROPBOX = "My Dropbox";

    private static final String APPS_DIR_FIREFOX = "Mozilla"
        + FileSystems.getDefault().getSeparator() + "Firefox";
    private static final String APPS_DIR_SUNBIRD = "Mozilla"
        + FileSystems.getDefault().getSeparator() + "Sunbird";
    private static final String APPS_DIR_THUNDERBIRD = "Thunderbird";
    private static final String APPS_DIR_FIREFOX2 = "firefox"; // Linux
    private static final String APPS_DIR_SUNBIRD2 = "sunbird"; // Linux
    private static final String APPS_DIR_THUNDERBIRD2 = "thunderbird"; // Linux

    static {
        if (WinUtils.getInstance() != null) {
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                APPS_DIR_OUTLOOK = WinUtils.getInstance().getSystemFolderPath(
                    WinUtils.CSIDL_LOCAL_APP_DATA, false)
                    + FileSystems.getDefault().getSeparator()
                    + "Microsoft"
                    + FileSystems.getDefault().getSeparator() + "Outlook";
            } else {
                APPS_DIR_OUTLOOK = null;
            }
            USER_DIR_DOCUMENTS_REPORTED = WinUtils.getInstance()
                .getSystemFolderPath(WinUtils.CSIDL_PERSONAL, false);
            USER_DIR_MUSIC_REPORTED = WinUtils.getInstance()
                .getSystemFolderPath(WinUtils.CSIDL_MYMUSIC, false);
            USER_DIR_PICTURES_REPORTED = WinUtils.getInstance()
                .getSystemFolderPath(WinUtils.CSIDL_MYPICTURES, false);
            USER_DIR_VIDEOS_REPORTED = WinUtils.getInstance()
                .getSystemFolderPath(WinUtils.CSIDL_MYVIDEO, false);
            USER_DIR_FAVORITES_REPORTED = WinUtils.getInstance()
                .getSystemFolderPath(WinUtils.CSIDL_FAVORITES, false);
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                APPS_DIR_WINDOWS_MAIL = WinUtils.getInstance()
                    .getSystemFolderPath(WinUtils.CSIDL_LOCAL_APP_DATA, false)
                    + FileSystems.getDefault().getSeparator()
                    + "Microsoft"
                    + FileSystems.getDefault().getSeparator()
                    + "Windows Mail";
            } else {
                APPS_DIR_WINDOWS_MAIL = null;
            }
        } else {
            USER_DIR_DOCUMENTS_REPORTED = null;
            USER_DIR_MUSIC_REPORTED = null;
            USER_DIR_PICTURES_REPORTED = null;
            USER_DIR_VIDEOS_REPORTED = null;
            USER_DIR_FAVORITES_REPORTED = null;
            APPS_DIR_OUTLOOK = null;
            APPS_DIR_WINDOWS_MAIL = null;
        }
    }

    /**
     * @return the "Documents" or "My documents" directory. Only available on
     *         Windows.
     */
    public static String getDocumentsReported() {
        return USER_DIR_DOCUMENTS_REPORTED;
    }

    /**
     * @return the "Musci" or "My music" directory. Only available on Windows.
     */
    public static String getMusicReported() {
        return USER_DIR_MUSIC_REPORTED;
    }

    /**
     * @return the "Videos" or "My videos" directory. Only available on Windows.
     */
    public static String getVideosReported() {
        return USER_DIR_VIDEOS_REPORTED;
    }

    /**
     * @return the "Pictures" or "My pictures" directory. Only available on
     *         Windows.
     */
    public static String getPicturesReported() {
        return USER_DIR_PICTURES_REPORTED;
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
        Controller controller, boolean includeAppData)
    {
        Map<String, UserDirectory> filteredDirs = new TreeMap<String, UserDirectory>(
            getUserDirectories());
        for (Iterator<UserDirectory> it = filteredDirs.values().iterator(); it
            .hasNext();)
        {
            UserDirectory userDir = it.next();

            // #2398
            if (!includeAppData
                && "APP DATA".equalsIgnoreCase(userDir.getTranslatedName()))
            {
                it.remove();
                continue;
            }

            Path directory = userDir.getDirectory();
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
        Path userHome = Paths.get(System.getProperty("user.home"));
        addTargetDirectory(userHome, USER_DIR_CONTACTS, "user.dir.contacts",
            false);
        addTargetDirectory(userHome, USER_DIR_DESKTOP, "user.dir.desktop",
            false);

        addTargetDirectory(userHome, USER_DIR_EVOLUTION, "user.dir.evolution",
            true);
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

            if (USER_DIR_DOCUMENTS_REPORTED != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundDocuments = addTargetDirectory(Paths.get(
                    USER_DIR_DOCUMENTS_REPORTED), "user.dir.documents", false,
                    "user.dir.documents");
            }
            if (USER_DIR_MUSIC_REPORTED != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundMusic = addTargetDirectory(Paths.get(
                    USER_DIR_MUSIC_REPORTED), "user.dir.music", false,
                    "user.dir.music");
            }
            if (USER_DIR_PICTURES_REPORTED != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundPictures = addTargetDirectory(Paths.get(
                    USER_DIR_PICTURES_REPORTED), "user.dir.pictures", false,
                    "user.dir.pictures");
            }
            if (USER_DIR_VIDEOS_REPORTED != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundVideos = addTargetDirectory(Paths.get(
                    USER_DIR_VIDEOS_REPORTED), "user.dir.videos", false,
                    "user.dir.videos");
            }
            if (USER_DIR_FAVORITES_REPORTED != null) {
                // #2203 Use same placeholder as on Vista or Win 7
                foundVideos = addTargetDirectory(Paths.get(
                    USER_DIR_FAVORITES_REPORTED), "user.dir.favorites", false);
            }
        }

        if (!foundDocuments) {
            addTargetDirectory(userHome, USER_DIR_DOCUMENTS_DEFAULT,
                "user.dir.documents", false);
        }
        if (!foundMusic) {
            addTargetDirectory(userHome, USER_DIR_MUSIC_DEFAULT,
                "user.dir.music", false);
        }
        if (!foundPictures) {
            addTargetDirectory(userHome, USER_DIR_PICTURES_DEFAULT,
                "user.dir.pictures", false);
        }
        if (!foundVideos) {
            addTargetDirectory(userHome, USER_DIR_VIDEOS_DEFAULT,
                "user.dir.videos", false);
        }
        boolean foundFavorites = false;
        if (!foundFavorites) {
            addTargetDirectory(userHome, USER_DIR_FAVORITES_DEFAULT,
                "user.dir.favorites", false);
        }

        addTargetDirectory(userHome, USER_DIR_RECENT_DOCUMENTS,
            "user.dir.recent_documents", false);
        addTargetDirectory(userHome, USER_DIR_VIDEOS_DEFAULT,
            "user.dir.videos", false);

        if (OSUtil.isWindowsSystem()) {
            String appDataname = WinUtils.getAppDataCurrentUser();
            if (appDataname != null) {
                Path appData = Paths.get(appDataname);
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
                    addTargetDirectory(Paths.get(APPS_DIR_OUTLOOK),
                        "apps.dir.outlook", false);
                }
                if (APPS_DIR_WINDOWS_MAIL != null) {
                    addTargetDirectory(Paths.get(APPS_DIR_WINDOWS_MAIL),
                        "apps.dir.windows_mail", false);
                }
            } else {
                Logger.getAnonymousLogger().severe(
                    "Application data directory not found.");
            }
        } else if (OSUtil.isLinux()) {
            Path appData = Paths.get("/etc");
            addTargetDirectory(appData, APPS_DIR_FIREFOX2, "apps.dir.firefox",
                false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD2, "apps.dir.sunbird",
                false);
            if (Feature.USER_DIRECTORIES_EMAIL_CLIENTS.isEnabled()) {
                addTargetDirectory(appData, APPS_DIR_THUNDERBIRD2,
                    "apps.dir.thunderbird", false);
            }
        } else if (OSUtil.isMacOS()) {
            Path appData = userHome.resolve("Library");
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
     * @param translationId
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private static void addTargetDirectory(Path root, String subdir,
        String translationId, boolean allowHidden)
    {
        Path directory = joinFile(root, subdir);
        addTargetDirectory(directory, translationId, allowHidden);
    }

    private static Path joinFile(Path root, String subdir) {
        return root.resolve(subdir);
    }

    /**
     * Adds a generic user directory if if exists for this os.
     *
     * @param translationId
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private static boolean addTargetDirectory(Path directory,
        String translationId, boolean allowHidden)
    {
        return addTargetDirectory(directory, translationId, allowHidden,
            translationId);
    }

    /**
     * Adds a generic user directory if if exists for this os.
     */
    private static boolean addTargetDirectory(Path directory,
        String translationId, boolean allowHidden, String placeholder)
    {
        try {
            if (Files.exists(directory) && Files.isDirectory(directory)
                && (allowHidden || !Files.isHidden(directory)))
            {
                String translation = Translation.getTranslation(translationId);
                UserDirectory userDir = new UserDirectory(translation,
                    '$' + placeholder, directory);
                userDirectories.put(translation, userDir);
                return true;
            }
        } catch (IOException ioe) {
            // TODO:
        }
        return false;
    }
}
