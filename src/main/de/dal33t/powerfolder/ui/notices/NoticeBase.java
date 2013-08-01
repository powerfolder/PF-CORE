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
 * $Id: NoticeBase.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import java.util.Date;
import java.util.UUID;

/**
 * Abstract basics of Notice.
 */
public abstract class NoticeBase implements Notice {

    private static final long serialVersionUID = 100L;

    private final Date date;
    private Date readDate;
    protected final String title;
    protected final String summary;

    /**
     * Allows this abstract ancestor to provide a consistent equals method
     * across subclasses.
     */
    private final UUID uuid;

    protected NoticeBase(String title, String summary) {
        date = new Date();
        this.title = title;
        this.summary = summary;
        uuid = UUID.randomUUID();
    }

    public Date getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setRead() {
        readDate = new Date();
    }

    public boolean isRead() {
        return readDate != null;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NoticeBase that = (NoticeBase) obj;
        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }
}
