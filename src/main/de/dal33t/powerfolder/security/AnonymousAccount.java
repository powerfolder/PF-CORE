package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.os.OnlineStorageSubscriptionType;

/**
 * Empty/Null account to avoid NPEs on not logged in users.
 * <p>
 * Its the default account for every unauthenticated call.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class AnonymousAccount extends Account {

    public AnonymousAccount() {
        getOSSubscription().setType(OnlineStorageSubscriptionType.NONE);
    }

    @Override
    public boolean isValid() {
        return false;
    }

}
