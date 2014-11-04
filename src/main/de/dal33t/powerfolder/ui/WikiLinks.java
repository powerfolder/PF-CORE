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
 * $Id: WikiLinks.java 7764 2009-04-24 10:31:44Z tot $
 */
package de.dal33t.powerfolder.ui;

/**
 * Repository of links to Wiki pages. Any references to the PowerFolder Wiki
 * should go here, so that we can ensure the Wiki is up to date.
 */
public interface WikiLinks {

    String SETTINGS_GENERAL = "General";
    String SETTINGS_UI = "User_Interface";
    String SETTINGS_NETWORK = "Network";
    String SETTINGS_DYN_DNS = "DynDNS";
    String SETTINGS_EXPERT = "Expert";
    String SETTINGS_DIALOG = "Dialogs";
    String SETTINGS_PLUGIN = "Plugins";
    String SETTINGS_INFO = "Information";

    String PROBLEM_UNSYNCED_FOLDER = "Unsynchronized-Folder";
    String PROBLEM_FOLDER_DATABASE = "Folder-Database";
    String PROBLEM_DEVICE_DISCONNECTED = "Disconnected-Device";
    String PROBLEM_DUPLICATE_FILENAME = "Duplicate-Filename";
    String PROBLEM_ILLEGAL_END_CHARS = "Illegal-End-Chars";
    String PROBLEM_ILLEGAL_CHARS = "Illegal-Chars";
    String PROBLEM_RESERVED_WORD = "Reserved-Word";
    String PROBLEM_FILENAME_TOO_LONG = "File-Name-Too-Long";
    String PROBLEM_NO_CONFLICT_DETECTION_POSSIBLE = "Version_Conflict_With_Old_Client";
    String PROBLEM_NO_OWNER = "No-Owner";
    String PROBLEM_FILE_CONFLICT = "Conflict_handling";
    String SCRIPT_EXECUTION = "Script_execution";
    String DEFAULT_FOLDER = "Default_Folder";
    String TRANSFER_MODES = "Transfer_Modes";
    String SECURITY_PERMISSION = "Security_Permissions";
    String SERVER_CLIENT_DEPLOYMENT = "Server_client_deployment";
    String UI_LOCK = "User-interface-lock";
    String SYSTEM_SERVICE = "System_Service";
    String EXCLUDING_FILES_FROM_SYNCHRONIZATION = "Excluding_Files_from_Synchronization";
    String MEMORY_CONFIGURATION = "Memory_configuration";
    String LIMITED_CONNECTIVITY = "Limited_connectivity";
    String WEBDAV = "WebDAV";
    String CONNECT_ON_DEMAND = "Connect_on_demand";
    String LIMITATIONS = "Limitations";
    String REFERRAL_REWARD_SYSTEM = "Referral_reward_system";
    String FILE_ARCHIVE = "File_Archive";
    String CLOUD_SPACE = "Cloud_Space";
}
