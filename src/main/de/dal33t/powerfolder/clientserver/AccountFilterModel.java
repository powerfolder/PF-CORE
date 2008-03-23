package de.dal33t.powerfolder.clientserver;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.beans.Model;

import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Reject;

public class AccountFilterModel extends Model {
    public static final String PROPERTY_DISABLED_ONLY = "disabledOnly";
    public static final String PROPERTY_PRO_USERS_ONLY = "proUsersOnly";
    public static final String PROPERTY_NON_TRIAL_ONLY = "nonTrialOnly";
    public static final String PROPERTY_USERNAME = "username";

    private boolean disabledOnly;
    private boolean proUsersOnly;
    private boolean nonTrialOnly;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        Object oldValue = getUsername();
        this.username = username != null ? username.toLowerCase() : null;
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
        return true;
    }

    public void apply(List<AccountDetails> list) {
        for (Iterator<AccountDetails> it = list.iterator(); it.hasNext();) {
            AccountDetails accountDetails = it.next();
            Account account = accountDetails.getAccount();
            if (!matches(account)) {
                it.remove();
            }
        }
    }

}
