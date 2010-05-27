/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: SimpleNotificationNotice.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

/**
 * Very simple implementation of Notice that is used for notification only;
 * there is no payload and it is not to be added to the app model notices.
 */
public class SimpleNotificationNotice extends NoticeBase {

    /**
     * Constuct a simple notification notice.
     *
     * @param title
     *          the title to display in the notification.
     * @param summary
     *          the summary message to display in the notification.
     */
    public SimpleNotificationNotice(String title, String summary) {
        super(title, summary);
    }

    /**
     * Just a notification; low importance.
     *
     * @return
     */
    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.INFORMATION;
    }

    /**
     * No payload; notification opnly.
     *
     * @return
     */
    public Object getPayload() {
        return null;
    }

    /**
     * Not actionable; only for notifications.
     * 
     * @return
     */
    public boolean isActionable() {
        return false;
    }

    /**
     * This is a simple notification notice.
     *
     * @return
     */
    public boolean isNotification() {
        return true;
    }
}
