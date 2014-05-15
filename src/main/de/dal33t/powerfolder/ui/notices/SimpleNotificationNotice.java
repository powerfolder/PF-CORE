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

import de.dal33t.powerfolder.Controller;

/**
 * Very simple implementation of Notice that is used for notification only;
 * there is no payload and it is not to be added to the app model notices.
 */
public class SimpleNotificationNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;

    /**
     * Constuct a simple notification notice.
     *
     * @param title
     *            the title to display in the notification.
     * @param summary
     *            the summary message to display in the notification.
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
    public Object getPayload(Controller controller) {
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

    /**
     * Not applicable to unactionable notices.
     *
     * @return
     */
    public boolean isPersistable() {
        return false;
    }

    @Override
    public String toString() {
        return "SimpleNotificationNotice [getTitle()=" + getTitle()
            + ", getSummary()=" + getSummary() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((summary == null) ? 0 : summary.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NoticeBase other = (NoticeBase) obj;
        if (summary == null) {
            if (other.summary != null)
                return false;
        } else if (!summary.equals(other.summary))
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }
}
