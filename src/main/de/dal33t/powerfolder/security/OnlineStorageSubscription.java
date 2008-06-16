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

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;

/**
 * Capsulates all the account information for the Online Storage.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class OnlineStorageSubscription extends Model implements Serializable {
    private static final long serialVersionUID = 8695479753037728184L;
    
    public static final String PROPERTY_VALID_TILL = "validTill";
    public static final String PROPERTY_WARNED_USAGE_DATE = "warnedUsageDate";
    public static final String PROPERTY_DISABLED_USAGE_DATE = "disabledUsageDate";
    public static final String PROPERTY_WARNED_EXPIRATION_DATE = "warnedExpirationDate";
    public static final String PROPERTY_DISABLED_EXPIRATION_DATE = "disabledExpirationDate";
    public static final String PROPERTY_TYPE = "type";

    private Date validTill;

    private Date warnedUsageDate;
    private Date disabledUsageDate;
    private Date warnedExpirationDate;
    private Date disabledExpirationDate;

    private OnlineStorageSubscriptionType type;

    // Logic ******************************************************************

    /**
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

    // Getter / Setter ********************************************************

    public Date getValidTill() {
        return validTill;
    }

    public void setValidTill(Date validTill) {
        Object oldValue = getValidTill();
        this.validTill = validTill;
        firePropertyChange(PROPERTY_VALID_TILL, oldValue, this.validTill);
    }

    public Date getWarnedUsageDate() {
        return warnedUsageDate;
    }

    public void setWarnedUsageDate(Date warnedUsageDate) {
        Object oldValue = getWarnedUsageDate();
        this.warnedUsageDate = warnedUsageDate;
        firePropertyChange(PROPERTY_WARNED_USAGE_DATE, oldValue,
            this.warnedUsageDate);
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
        firePropertyChange(PROPERTY_DISABLED_USAGE_DATE, oldValue,
            this.disabledUsageDate);
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
        firePropertyChange(PROPERTY_WARNED_EXPIRATION_DATE, oldValue,
            this.warnedExpirationDate);
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
        firePropertyChange(PROPERTY_DISABLED_EXPIRATION_DATE, oldValue,
            this.disabledExpirationDate);
    }

    public boolean isDisabledExpiration() {
        return disabledExpirationDate != null;
    }
    
    public boolean isDisabled() {
        return isDisabledExpiration() || isDisabledUsage();
    }

    public OnlineStorageSubscriptionType getType() {
        return type;
    }

    public void setType(OnlineStorageSubscriptionType type) {
        Object oldValue = getType();
        this.type = type;
        firePropertyChange(PROPERTY_TYPE, oldValue, this.type);
    }
}
