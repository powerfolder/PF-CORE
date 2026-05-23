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
 */
package de.dal33t.powerfolder.d2d;

public enum NodeEvent {

    ACCOUNT_CHANGE_REQUEST,
    ACCOUNT_INFO_REQUEST,
    ACCOUNT_SEARCH_REQUEST,
    ACTIVITY_LIST_REQUEST,
    AVATAR_REQUEST,
    CERTIFICATE_SIGNING_REQUEST,
    CREATE_ACCOUNT_REQUEST,
    DOWNLOAD_ABORT,
    DOWNLOAD_REQUEST,
    FILE_LIST_REPLY,
    FILE_LIST_REQUEST,
    FILE_PART_REPLY,
    FILE_PART_REQUEST,
    FILE_SEARCH_REQUEST,
    FOLDER_CREATE_REQUEST,
    FOLDER_FILES_CHANGED,
    FOLDER_LIST,
    FOLDER_REMOVE_REQUEST,
    FOLDER_RENAME_REQUEST,
    FOLDER_SERVER_NODES_REQUEST,
    GROUP_SEARCH_REQUEST,
    HANDSHAKE_COMPLETED,
    IDENTITY,
    IDENTITY_REPLY,
    INVITATION_ACCEPT_REQUEST,
    INVITATION_CREATE_REQUEST,
    LOGIN_REQUEST,
    PING,
    PONG,
    PERMISSION_CHANGE_REQUEST,
    PERMISSION_INFO_REQUEST,
    PERMISSION_LIST_REQUEST,
    PERMISSION_REMOVE_REQUEST,
    SHARE_LINK_CHANGE_REQUEST,
    SHARE_LINK_CREATE_REQUEST,
    SHARE_LINK_INFO_REQUEST,
    SHARE_LINK_LIST_REQUEST,
    SHARE_LINK_REMOVE_REQUEST,
    THUMBNAIL_REQUEST,
    UPLOAD_ABORT,
    UPLOAD_START,
    UPLOAD_STOP;

}
