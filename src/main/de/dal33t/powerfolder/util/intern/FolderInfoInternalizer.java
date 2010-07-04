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

/**
 * To internalize {@link FolderInfo}s into a weak hash map.
 * 
 * @author sprajc
 */
public class FolderInfoInternalizer implements Internalizer<FolderInfo> {
    private static final Map<FolderInfo, FolderInfo> INSTANCES = new WeakHashMap<FolderInfo, FolderInfo>();

    public FolderInfo intern(FolderInfo folderInfo) {
        if (folderInfo == null) {
            return null;
        }
        FolderInfo internInstance = INSTANCES.get(folderInfo);
        if (internInstance != null) {
            return internInstance;
        }

        // New Intern
        synchronized (INSTANCES) {
            internInstance = INSTANCES.get(folderInfo);
            if (internInstance == null) {
                INSTANCES.put(folderInfo, folderInfo);
                internInstance = folderInfo;
            }
        }
        return internInstance;
    }

}
