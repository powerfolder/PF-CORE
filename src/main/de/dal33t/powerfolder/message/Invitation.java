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
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;

import java.io.File;

/**
 * A Invitation to a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Invitation extends FolderRelatedMessage {

    private static final long serialVersionUID = 101L;

    /** suggestedLocalBase is absolute. */
    private static final int ABSOLUTE = 0;

    /** suggestedLocalBase is relative to apps directory. */
    private static final int RELATIVE_APP_DATA = 1;

    /** suggestedLocalBase is relative to PowerFolder base directory. */
    private static final int RELATIVE_PF_BASE = 2;

    /** suggestedLocalBase is relative to user home directory. */
    private static final int RELATIVE_USER_HOME = 3;

    private MemberInfo invitor;
    // For backward compatibilty to pre 3.1.2 versions.
    private File suggestedLocalBase;
    private String invitationText;
    private String suggestedSyncProfileConfig;
    private String suggestedLocalBasePath;
    private int relative;
    private int permissions;

    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
    }

    public void setInvitor(MemberInfo invitor) {
        this.invitor = invitor;
    }

    public void setSuggestedSyncProfile(SyncProfile suggestedSyncProfile) {
        suggestedSyncProfileConfig = suggestedSyncProfile.getFieldList();
    }

    /**
     * Sets the suggested local base. Parses to get relative paths from apps dir
     * and PowerFolder local base. For subdirs of the PowerFolder base directory
     * and of the apps dir, the relative part of the location is extracted so
     * that the receiver can locate local to his computer's environment.
     * 
     * @param controller
     * @param suggestedLocalBase
     */
    public void setSuggestedLocalBase(Controller controller,
        File suggestedLocalBase)
    {
        Reject.ifNull(suggestedLocalBase, "File is null");
        this.suggestedLocalBase = suggestedLocalBase;
        String folderBase = controller.getFolderRepository()
            .getFoldersBasedir();
        String appsDir = getAppsDir();
        String userHomeDir = getUserHomeDir();
        if (OSUtil.isWindowsSystem()
            && suggestedLocalBase.getAbsolutePath().startsWith(appsDir))
        {
            String filePath = suggestedLocalBase.getAbsolutePath();
            suggestedLocalBasePath = filePath.substring(appsDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(File.separator)) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_APP_DATA;
        } else if (suggestedLocalBase.getAbsolutePath().startsWith(folderBase))
        {
            String filePath = suggestedLocalBase.getAbsolutePath();
            String baseDirPath = ConfigurationEntry.FOLDER_BASEDIR
                .getValue(controller);
            suggestedLocalBasePath = filePath.substring(baseDirPath.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(File.separator)) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_PF_BASE;
        } else if (suggestedLocalBase.getAbsolutePath().startsWith(userHomeDir))
        {
            String filePath = suggestedLocalBase.getAbsolutePath();
            suggestedLocalBasePath = filePath.substring(userHomeDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(File.separator)) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_USER_HOME;
        } else {
            suggestedLocalBasePath = suggestedLocalBase.getAbsolutePath();
            relative = ABSOLUTE;
        }
    }

    /**
     * Get the suggested local base. Uses 'relative' to adjust for the local
     * environment.
     * 
     * @param controller
     * @return
     */
    public File getSuggestedLocalBase(Controller controller) {

        if (suggestedLocalBasePath == null) {
            return new File(controller.getFolderRepository()
                .getFoldersBasedir());
        }

        if (OSUtil.isLinux() || OSUtil.isMacOS()) {
            suggestedLocalBasePath = Util.replace(suggestedLocalBasePath, "\\",
                File.separator);
        } else {
            suggestedLocalBasePath = Util.replace(suggestedLocalBasePath, "/",
                File.separator);
        }

        if (relative == RELATIVE_APP_DATA) {
            return new File(getAppsDir(), suggestedLocalBasePath);
        } else if (relative == RELATIVE_PF_BASE) {
            File powerFolderBaseDir = new File(controller.getFolderRepository()
                .getFoldersBasedir());
            return new File(powerFolderBaseDir, suggestedLocalBasePath);
        } else if (relative == RELATIVE_USER_HOME) {
            return new File(getUserHomeDir(), suggestedLocalBasePath);
        } else {
            return new File(suggestedLocalBasePath);
        }
    }

    public MemberInfo getInvitor() {
        return invitor;
    }

    public String getInvitationText() {
        return invitationText;
    }

    public void setInvitationText(String invitationText) {
        this.invitationText = invitationText;
    }

    public int getRelative() {
        return relative;
    }

    public int getPermissions() {
        return permissions;
    }

    public SyncProfile getSuggestedSyncProfile() {
        if (suggestedSyncProfileConfig == null) {
            // For backward compatibility.
            return SyncProfile.AUTOMATIC_SYNCHRONIZATION;
        }
        return SyncProfile
            .getSyncProfileByFieldList(suggestedSyncProfileConfig);
    }

    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }

    private static String getAppsDir() {
        if (OSUtil.isWindowsSystem()) {
            return System.getenv("APPDATA");
        }

        // Loading a Windows invitation on a Mac/Unix box:
        // no APPDIR, so set to somewhere safe.
        return getUserHomeDir();
    }

    private static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

}