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

public enum NodeState {

    LISTEN,
    OPEN_IDENTITY_REPLY_WAIT,
    OPEN_LOGIN_REQUEST_WAIT,
    OPEN_ACCOUNT_INFO_REQUEST_WAIT,
    OPEN_FOLDER_LIST_WAIT,
    OPEN_FILE_LIST_WAIT,
    OPEN_HANDSHAKE_COMPLETED_WAIT,
    ESTABLISHED,
    CLOSED;

}
