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
package de.dal33t.powerfolder.os;


/**
 * The subscription types possible.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
@Deprecated
public enum OnlineStorageSubscriptionType {
    TRIAL("OS-T", 1, true, false),
    TRIAL_PRO("OS-TP", 1, true, false),
    STARTER("OS-1", 1, false, true),
    BASIC("OS-5", 5, false, true),
    ADVANCED("OS-10",  10, false, true),

    /**
     * For JUNIT testing only
     */
    TEST_TRIAL(true),

    SMALL_ENTERPRISE("OS-20", 20, false, true),
    UNLIMITED("OS-U", 9999, false, true),
    NONE("OS-N", 0, false, true),
    GB50("OS-50", 50, false, true),
    GB100("OS-100", 100, false, true),
    GB250("OS-250", 250, false, true),
    TRIAL_5GB("OS-T-5", 5, true, false),

    /**
     * For JUNIT testing only
     */
    TEST_PAYING(false);

    private String articleNo;
    private long storageSize;
    private boolean trial;
    private boolean active;

    private OnlineStorageSubscriptionType(String articleNo,
        long gbs, boolean trial, boolean active)
    {
        this.articleNo = articleNo;
        this.storageSize = gbs * 1024 * 1024 * 1024;
        this.trial = trial;
        this.active = active;
    }

    /**
     * For 1 MB test
     */
    private OnlineStorageSubscriptionType(boolean trial) {
        this.articleNo = "TEST";
        this.storageSize = 1 * 1024 * 1024;
        this.trial = trial;
        this.active = false;
    }

    public static OnlineStorageSubscriptionType getByArticleNo(String articleNo)
    {
        for (OnlineStorageSubscriptionType type : values()) {
            if (type.getArticleNo().equals(articleNo)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
            "No article no const class de.dal33t.powerfolder.os.OnlineStorageSubscriptionType: "
                + articleNo);
    }

    @Deprecated
    public String getArticleNo() {
        return articleNo;
    }

    /**
     * @return the storage size in bytes
     */
    @Deprecated
    public long getStorageSize() {
        return storageSize;
    }

    @Deprecated
    public int getStorageSizeGB() {
        return (int) (storageSize / 1024 / 1024 / 1024);
    }

    @Deprecated
    public boolean isTrial() {
        return trial;
    }

    public boolean isActive() {
        return active;
    }
}
