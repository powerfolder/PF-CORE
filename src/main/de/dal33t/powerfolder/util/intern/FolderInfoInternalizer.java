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
 * $Id: NodeManager.java 12576 2010-06-14 14:28:23Z tot $
 */
package de.dal33t.powerfolder.util.intern;

import java.util.Map;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * To internalize {@link FolderInfo}s into a weak hash map.
 *
 * @author sprajc
 */
public class FolderInfoInternalizer implements Internalizer<FolderInfo> {
    private final Map<FolderInfo, FolderInfo> INSTANCES = new WeakHashMap<FolderInfo, FolderInfo>();

    public FolderInfo intern(FolderInfo folderInfo) {
        if (folderInfo == null) {
            return null;
        }
        FolderInfo internInstance = null;
        synchronized (INSTANCES) {
            internInstance = INSTANCES.get(folderInfo);
        }
        if (internInstance != null) {
            return internInstance;
        }

        // New Intern
        synchronized (INSTANCES) {
            internInstance = INSTANCES.get(folderInfo);
            if (internInstance == null) {
                if (StringUtils.isBlank(folderInfo.getName())) {
                    // Not interned folder info without name.
                    // System.err.println("INTERN FAILED: " + folderInfo + " / "
                    // + folderInfo.getId());
                    // new RuntimeException().printStackTrace();
                    return folderInfo;
                }
                INSTANCES.put(folderInfo, folderInfo);
                internInstance = folderInfo;
            } else {

            }
        }
        return internInstance;
    }

    public FolderInfo rename(FolderInfo foInfo) {
        if (foInfo == null) {
            return null;
        }

        FolderInfo oldInstance = null;
        synchronized (INSTANCES) {
            oldInstance = INSTANCES.get(foInfo);

            if (oldInstance != null
                    && oldInstance.getName().equals(foInfo.getName()))
            {
                return oldInstance;
            }

            INSTANCES.put(foInfo, foInfo);
        }

        return foInfo;
    }
}
