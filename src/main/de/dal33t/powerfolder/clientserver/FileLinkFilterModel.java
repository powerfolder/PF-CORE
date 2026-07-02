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
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.FolderInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class FileLinkFilterModel implements Serializable {
    private static final long serialVersionUID = 100L;

    private Collection<FolderInfo> foldersInfo;
    private String query;

    public void setFolders(final Collection<FolderInfo> folders) {
        this.foldersInfo = folders;
    }
    public Collection<FolderInfo> getFolders() {
        if (foldersInfo == null) {
            return null;
        }
        return Collections.unmodifiableCollection(foldersInfo);
    }

    public void setQuery(String query) {
        this.query= query;
    }
    public String getQuery() {
        return query;
    }
}
