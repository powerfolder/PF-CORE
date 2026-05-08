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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * The permission to read files in the folder. Write
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderReadPermission extends FolderPermission {
    private static final long serialVersionUID = 100L;

    /**
     * Construct externally with {@link FolderPermission#read(FolderInfo)}
     *
     * @param foInfo
     */
    FolderReadPermission(FolderInfo foInfo) {
        super(foInfo);
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public FolderReadPermission(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public String getName() {
        return Translation.get("permissions.folder.read");
    }

    @Override
    public AccessMode getMode() {
        return AccessMode.READ;
    }

}
