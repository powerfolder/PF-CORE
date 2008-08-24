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
package de.dal33t.powerfolder.clientserver;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Reject;

public class AccountFilterModel extends Model {
    private static final long serialVersionUID = 100L;

    public static final String PROPERTY_DISABLED_ONLY = "disabledOnly";
    public static final String PROPERTY_PRO_USERS_ONLY = "proUsersOnly";
    public static final String PROPERTY_NON_TRIAL_ONLY = "nonTrialOnly";
    public static final String PROPERTY_ACTIVE_TRIAL = "activeTrial";
    public static final String PROPERTY_USERNAME = "username";

    private boolean disabledOnly;
    private boolean proUsersOnly;
    private boolean nonTrialOnly;
    private boolean activeTrial;
    private String username;

    // Getter and Setter ******************************************************

    public boolean isDisabledOnly() {
        return disabledOnly;
    }

    public void setDisabledOnly(boolean disabledOnly) {
        Object oldValue = isDisabledOnly();
        this.disabledOnly = disabledOnly;
        firePropertyChange(PROPERTY_DISABLED_ONLY, oldValue, this.disabledOnly);
    }

    public boolean isProUsersOnly() {
        return proUsersOnly;
    }

    public void setProUsersOnly(boolean proUsersOnly) {
        Object oldValue = isProUsersOnly();
        this.proUsersOnly = proUsersOnly;
        firePropertyChange(PROPERTY_PRO_USERS_ONLY, oldValue, this.proUsersOnly);
    }

    public boolean isNonTrialOnly() {
        return nonTrialOnly;
    }

    public void setNonTrialOnly(boolean nonTrial) {
        Object oldValue = isNonTrialOnly();
        this.nonTrialOnly = nonTrial;
        firePropertyChange(PROPERTY_NON_TRIAL_ONLY, oldValue, this.nonTrialOnly);
    }

    public boolean isActiveTrial() {
        return activeTrial;
    }

    public void setActiveTrial(boolean activeTrial) {
        Object oldValue = isActiveTrial();
        this.activeTrial = activeTrial;
        firePropertyChange(PROPERTY_ACTIVE_TRIAL, oldValue, this.activeTrial);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Object oldValue = getUsername();
        this.username = username != null ? username.toLowerCase().trim() : null;
        firePropertyChange(PROPERTY_USERNAME, oldValue, this.username);
    }

    // Logic ******************************************************************

    public boolean matches(Account account) {
        Reject.ifNull(account, "Account is null");
        if (disabledOnly && !account.getOSSubscription().isDisabled()) {
            return false;
        }
        if (nonTrialOnly && account.getOSSubscription().getType().isTrial()) {
            return false;
        }
        if (proUsersOnly && !account.isProUser()) {
            return false;
        }
        if (!StringUtils.isBlank(username)) {
            if (!account.getUsername().toLowerCase().startsWith(
                username.toLowerCase()))
            {
                return false;
            }
        }
        if (activeTrial) {
            return account.getOSSubscription().getType().isTrial()
                && !account.getOSSubscription().isDisabledExpiration();
        }
        return true;
    }

    public void apply(Collection<AccountDetails> list) {
        for (Iterator<AccountDetails> it = list.iterator(); it.hasNext();) {
            AccountDetails accountDetails = it.next();
            Account account = accountDetails.getAccount();
            if (!matches(account)) {
                it.remove();
            }
        }
    }

}
