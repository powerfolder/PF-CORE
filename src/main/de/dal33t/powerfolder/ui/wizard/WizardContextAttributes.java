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
package de.dal33t.powerfolder.ui.wizard;

/**
 * Names of attributes passed in a Wizard Context.
 */
public interface WizardContextAttributes {
    /** The wizard object */
    String WIZARD_ATTRIBUTE = "wizard";

    /** The dialog the wizard is displayed in */
    String DIALOG_ATTRIBUTE = "dialog";

    /** The attribute in wizard context, which will be displayed */
    String PROMPT_TEXT_ATTRIBUTE = "disklocation.prompt_text";

    /** The attribute in wizard context, which will be displayed */
    String INITIAL_FOLDER_NAME = "disklocation.initial_folder_name";

    /** The folder info object for the targeted folder */
    String FOLDERINFO_ATTRIBUTE = "disklocation.folder_info";

    /** The folder info object for the targeted folder */
    String FOLDER_PERMISSION_ATTRIBUTE = "folder_permission";

    /** The local file location for the folder */
    String FOLDER_LOCAL_BASE = "disklocation.localbase";

    /** The local file locations for the folders. List<FolderCreateItem> */
    String FOLDER_CREATE_ITEMS = "disklocation.folder_create_items";

    /** The folder info object for the targeted folder */
    String SYNC_PROFILE_ATTRIBUTE = "disklocation.sync_profile";

    /** Determines if user should be prompted to send invitation afterwards */
    String SEND_INVIATION_AFTER_ATTRIBUTE = "disklocation.send_invitations";

    /** If the folder should be backed up by the Online Storage */
    String BACKUP_ONLINE_STOARGE = "folder_create.backup_by_os";

    /** Count of files in directory */
    String FILE_COUNT = "file_count";

    /** Determines if to set the configured folder as default synced folder */
    String SET_DEFAULT_SYNCHRONIZED_FOLDER = "set_default_synced_folder";

    /**
     * Whether the folder being created is from an invitation. If it is, do not try
     * to locate an existing online folder to join.
     */
    String FOLDER_IS_INVITE = "folder.is.invite";

    /**
     * Whether to make the Member a friend on creation of a folder. This is done
     * when creating a folder from an invitation.
     */
    String MAKE_FRIEND_AFTER = "make.friend.after";

    /**
     * FolderInfo
     */
    String FOLDER_INFO = "folder.info";

    /**
     * List<FolderInfo>
     */
    String FOLDER_INFOS = "folder.infos";

    /**
     * List<FolderInfo>
     */
    String USE_CLOUD_STORAGE = "use.cloud.storage";

}
