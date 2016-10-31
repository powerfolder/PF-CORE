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
package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Embeddable;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.OnlineStorageSubscriptionProto;
import de.dal33t.powerfolder.util.Format;

/**
 * Capsulates all the account information for the Online Storage.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
@Embeddable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OnlineStorageSubscription implements Serializable, D2DObject {
    private static final long serialVersionUID = 8695479753037728184L;
    public static final int UNLIMITED_GB = 9999;

    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_STORAGE_SIZE = "storageSize";
    public static final String PROPERTY_STORAGE_SIZE_GB = "storageSizeGB";
    public static final String PROPERTY_VALID_FROM = "validFrom";
    public static final String PROPERTY_VALID_TILL = "validTill";
    public static final String PROPERTY_WARNED_USAGE_DATE = "warnedUsageDate";
    public static final String PROPERTY_DISABLED_USAGE_DATE = "disabledUsageDate";
    public static final String PROPERTY_WARNED_EXPIRATION_DATE = "warnedExpirationDate";
    public static final String PROPERTY_DISABLED_EXPIRATION_DATE = "disabledExpirationDate";

    private long storageSize;
    private Date validFrom;
    private Date validTill;

    private Date warnedUsageDate;
    private Date disabledUsageDate;
    private Date warnedExpirationDate;
    private Date disabledExpirationDate;

    // Logic ******************************************************************

    /**
     * Serialization constructor
     */
    public OnlineStorageSubscription() {
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public OnlineStorageSubscription(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    /**
     * Use {@link #isExpired()} for checking exactly if a subcription has
     * expired. This method returns "0" during the last day of subscription.
     *
     * @return the days left until expire. 0 if expired -1 if never expires.
     */
    public int getDaysLeft() {
        if (getValidTill() == null) {
            return -1;
        }
        long timeValid = getValidTill().getTime() - System.currentTimeMillis();
        if (timeValid > 0) {
            int daysLeft = (int) (((double) timeValid) / (1000 * 60 * 60 * 24));
            return daysLeft;
        }
        return 0;
    }

    /**
     * @return if the subscription has expired = passed the valid date or is
     *         before the activation date.
     */
    public boolean isExpired() {
        if (validTill == null) {
            // never expires
            return false;
        }
        long timeValid = validTill.getTime() - System.currentTimeMillis();
        return timeValid <= 0;
    }

    /**
     * @return true if now is between valid from and to date.
     */
    public boolean isValid() {
        if (isExpired()) {
            return false;
        }
        if (validFrom == null) {
            return true;
        }
        return System.currentTimeMillis() > validFrom.getTime();
    }

    // Getter / Setter ********************************************************

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTill() {
        return validTill;
    }

    public void setValidTill(Date validTill) {
        this.validTill = validTill;
    }

    public Date getWarnedUsageDate() {
        return warnedUsageDate;
    }

    public void setWarnedUsageDate(Date warnedUsageDate) {
        Object oldValue = getWarnedUsageDate();
        this.warnedUsageDate = warnedUsageDate;
        // firePropertyChange(PROPERTY_WARNED_USAGE_DATE, oldValue,
        // this.warnedUsageDate);
    }

    public boolean isWarnedUsage() {
        return warnedUsageDate != null;
    }

    public Date getDisabledUsageDate() {
        return disabledUsageDate;
    }

    public void setDisabledUsageDate(Date disabledUsageDate) {
        Object oldValue = getDisabledUsageDate();
        this.disabledUsageDate = disabledUsageDate;
        // firePropertyChange(PROPERTY_DISABLED_USAGE_DATE, oldValue,
        // this.disabledUsageDate);
    }

    public boolean isDisabledUsage() {
        return disabledUsageDate != null;
    }

    public Date getWarnedExpirationDate() {
        return warnedExpirationDate;
    }

    public void setWarnedExpirationDate(Date warnedExpirationDate) {
        Object oldValue = getWarnedExpirationDate();
        this.warnedExpirationDate = warnedExpirationDate;
        // firePropertyChange(PROPERTY_WARNED_EXPIRATION_DATE, oldValue,
        // this.warnedExpirationDate);
    }

    public boolean isWarnedExpiration() {
        return warnedExpirationDate != null;
    }

    public Date getDisabledExpirationDate() {
        return disabledExpirationDate;
    }

    public void setDisabledExpirationDate(Date disabledExpirationDate) {
        Object oldValue = getDisabledExpirationDate();
        this.disabledExpirationDate = disabledExpirationDate;
        // firePropertyChange(PROPERTY_DISABLED_EXPIRATION_DATE, oldValue,
        // this.disabledExpirationDate);
    }

    /**
     * @return true if this subscription has been disabled because it expired
     */
    public boolean isDisabledExpiration() {
        return disabledExpirationDate != null;
    }

    /**
     * @return true if this subscription has been disabled because of overuse or
     *         expiration
     */
    public boolean isDisabled() {
        return isDisabledExpiration() || isDisabledUsage();
    }

    public boolean isUnlimited() {
        return getStorageSizeGB() == UNLIMITED_GB;
    }

    /**
     * @return the storage size in bytes
     */
    public long getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(long storageSize) {
        Object oldValue = getStorageSize();
        Object oldGB = getStorageSizeGB();
        this.storageSize = storageSize;
        // firePropertyChange(PROPERTY_STORAGE_SIZE, oldValue,
        // this.storageSize);
        // firePropertyChange(PROPERTY_STORAGE_SIZE_GB, oldGB,
        // getStorageSizeGB());
    }

    public void setStorageSizeGB(int storageSizeGB) {
        setStorageSize(1024L * 1024L * 1024L * storageSizeGB);
    }

    public void setStorageSizeUnlimited() {
        setStorageSizeGB(UNLIMITED_GB);
    }

    /**
     * @return the storage size in GBs
     */
    public int getStorageSizeGB() {
        return (int) (getStorageSize() / 1024 / 1024 / 1024);
    }

    public String getDescription() {
        if (isUnlimited()) {
            return "Unlimited";
        }
        return Format.formatBytesShort(getStorageSize());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("OS Subscription ");
        b.append(getDescription());
        if (validTill != null) {
            b.append(" valid till " + validTill);
        } else {
            b.append(" valid forever");
        }
        return b.toString();
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof OnlineStorageSubscriptionProto.OnlineStorageSubscription) {
            OnlineStorageSubscriptionProto.OnlineStorageSubscription proto = (OnlineStorageSubscriptionProto.OnlineStorageSubscription)mesg;
            this.storageSize            = proto.getStorageSize();
            this.validFrom              = new Date(proto.getValidFrom());
            this.validTill              = new Date(proto.getValidTill());
            this.warnedUsageDate        = new Date(proto.getWarnedUsageDate());
            this.disabledUsageDate      = new Date(proto.getDisabledUsageDate());
            this.warnedExpirationDate   = new Date(proto.getWarnedExpirationDate());
            this.disabledExpirationDate = new Date(proto.getDisabledExpirationDate());
        }
    }

    @Override
    public AbstractMessage toD2D() {
        OnlineStorageSubscriptionProto.OnlineStorageSubscription.Builder builder = OnlineStorageSubscriptionProto.OnlineStorageSubscription.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setStorageSize(this.storageSize);
        if (this.validFrom != null) builder.setValidFrom(this.validFrom.getTime());
        if (this.validTill != null) builder.setValidTill(this.validTill.getTime());
        if (this.warnedUsageDate != null) builder.setWarnedUsageDate(this.warnedUsageDate.getTime());
        if (this.disabledUsageDate != null) builder.setDisabledUsageDate(this.disabledUsageDate.getTime());
        if (this.warnedExpirationDate != null) builder.setWarnedExpirationDate(this.warnedExpirationDate.getTime());
        if (this.disabledExpirationDate != null) builder.setDisabledExpirationDate(this.disabledExpirationDate.getTime());
        return builder.build();
    }
}
