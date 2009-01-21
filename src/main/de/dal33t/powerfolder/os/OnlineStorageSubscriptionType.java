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
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public enum OnlineStorageSubscriptionType {
    TRIAL("OS-T", "1 GB (30 days)", 1, true, false),
    TRIAL_PRO("OS-TP", "DONTUSE: 1 GB (60)", 1, true, false), 
    STARTER("OS-1", "1 GB", 1, false, false),
    BASIC("OS-5", "5 GB", 5, false, true),
    ADVANCED("OS-10", "10 GB", 10, false, true),

    /**
     * For JUNIT testing only
     */
    TEST(),

    SMALL_ENTERPRISE("OS-20", "20 GB", 20, false, true),
    UNLIMITED("OS-U", "Unlimited", 9999, false, true),
    NONE("OS-N", "None", 0, false, true),
    GB50("OS-50", "50 GB", 50, false, true),
    GB100("OS-100", "100 GB", 100, false, true),
    GB250("OS-250", "250 GB", 250, false, true),
    TRIAL_5GB("OS-T-5", "5 GB (30 days)", 5, true, false);

    private String articleNo;
    private String description;
    private long storageSize;
    private boolean trial;
    private boolean active;

    private OnlineStorageSubscriptionType(String articleNo, String description,
        long gbs, boolean trial, boolean active)
    {
        this.articleNo = articleNo;
        this.description = description;
        this.storageSize = gbs * 1024 * 1024 * 1024;
        this.trial = trial;
        this.active = active;
    }

    /**
     * For 1 MB test
     */
    private OnlineStorageSubscriptionType() {
        this.articleNo = "TEST";
        this.description = "DONTUSE: 1 MB Test subscription";
        this.storageSize = 1 * 1024 * 1024;
        this.trial = true;
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

    public String getArticleNo() {
        return articleNo;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return the storage size in bytes
     */
    public long getStorageSize() {
        return storageSize;
    }

    public int getStorageSizeGB() {
        return (int) (storageSize / 1024 / 1024 / 1024);
    }

    public boolean isTrial() {
        return trial;
    }

    public boolean isActive() {
        return active;
    }
}
