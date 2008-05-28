package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;

/**
 * Empty/Null account to avoid NPEs on not logged in users.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class InvalidAccount extends Account {

    public InvalidAccount() {
        getOSSubscription().setType(OnlineStorageSubscriptionType.NONE);
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
