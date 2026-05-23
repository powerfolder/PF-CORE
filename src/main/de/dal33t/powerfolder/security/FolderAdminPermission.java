/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Administration permission on one folder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderAdminPermission extends FolderPermission {

    private static final long serialVersionUID = 100L;

    @Deprecated
    private FolderInfo folder;

    /**
     * Construct externally with {@link FolderPermission#admin(FolderInfo)}
     *
     * @param foInfo
     */
    FolderAdminPermission(FolderInfo foInfo) {
        super(foInfo);
        Reject.ifNull(foInfo, "Folderinfo is null");
        folder = foInfo;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public FolderAdminPermission(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public String getName() {
        return Translation.get("permissions.folder.admin");
    }

    @Override
    public AccessMode getMode() {
        return AccessMode.ADMIN;
    }

    @Override
    protected Class<? extends FolderPermission>[] getImpliedFolderPermissionTypes() {
        return new Class[] {
                FolderReadWritePermission.class,
                FolderReadPermission.class,
                FolderDeletePermission.class
        };
    }

    // Serialization compatibility ********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        if (folder != null) {
            super.folder = folder;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        if (super.folder != null) {
            folder = super.folder;
        }
        out.defaultWriteObject();
    }
}
