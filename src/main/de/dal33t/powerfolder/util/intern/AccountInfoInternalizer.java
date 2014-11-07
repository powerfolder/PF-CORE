/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 * $Id: NodeManager.java 12576 2010-06-14 14:28:23Z tot $
 */
package de.dal33t.powerfolder.util.intern;

import java.util.Map;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * To internalize {@link AccountInfo}s into a weak hash map.
 *
 * @author sprajc
 */
public class AccountInfoInternalizer implements Internalizer<AccountInfo> {
    private final Map<AccountInfo, AccountInfo> INSTANCES = new WeakHashMap<>();
    
    @Override
    public AccountInfo intern(AccountInfo accountInfo) {
        if (accountInfo == null) {
            return null;
        }
        AccountInfo internInstance = INSTANCES.get(accountInfo);
        if (internInstance != null) {
            return internInstance;
        }

        // New Intern
        synchronized (INSTANCES) {
            internInstance = INSTANCES.get(accountInfo);
            if (internInstance == null) {
                if (StringUtils.isBlank(accountInfo.getUsername())) {
                    // Not interned folder info without name.
                    // System.err.println("INTERN FAILED: " + folderInfo + " / "
                    // + folderInfo.getId());
                    // new RuntimeException().printStackTrace();
                    return accountInfo;
                }
                INSTANCES.put(accountInfo, accountInfo);
                internInstance = accountInfo;
            }
        }
        return internInstance;
    }

    @Override
    public AccountInfo sudoIntern(AccountInfo accountInfo) {
        if (accountInfo == null) {
            return null;
        }

        AccountInfo oldInstance = INSTANCES.get(accountInfo);

        if (oldInstance != null
            && oldInstance.getUsername().equals(accountInfo.getUsername()))
        {
            return oldInstance;
        }
        if (oldInstance != null
            && oldInstance.getDisplayName() != null
            && oldInstance.getDisplayName()
                .equals(accountInfo.getDisplayName()))
        {
            return oldInstance;
        }

        INSTANCES.put(accountInfo, accountInfo);

        return accountInfo;
    }

}
