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
 * $Id: FolderService.java 4655 2008-07-19 15:32:32Z bytekeeper $
 */
package de.dal33t.powerfolder.clientserver;

import java.util.List;

public interface NotificationService {
    /**
     * @return all notifications for the logged in client, that have not yet
     *         been marked as received.
     */
    List<NotificationService> getNotReceivedNotifications();

    /**
     * Marks the {@link Notification} with the given OID as received in client.
     * 
     * @param oid
     * @return true if succeeded, false if notification was not found or has
     *         already been marked as received.
     */
    boolean markReceived(String oid);
}
