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
package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.db.GenericDAO;

import java.util.Collection;

/**
 * Data Access Object for FolderInfo objects.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public interface FolderInfoDAO extends GenericDAO<FolderInfo> {
    /**
     * PFS-809
     *
     * @param folder
     * @return the number of potential entities (accounts or groups) which can
     *         access this folder by permissions. Does not count admins.
     */
    int countMembers(FolderInfo folder);

    /**
     * Returns all subfolders belonging to the specified top-level folder.
     *
     * <p>
     * A <em>top-level folder</em> represents a logical root, while <em>subfolders</em>
     * are folders that are explicitly marked as subfolders and associated with the
     * given top-level folder.
     * </p>
     *
     * <h3>Folder Structure Example</h3>
     *
     * <pre>{@code
     * /projects                    (top folder)
     * ├── alpha                    (subfolder)
     * ├── beta                     (subfolder)
     * │   ├── docs                 (subfolder)
     * │   └── tmp                  (directory, NOT a subfolder)
     * └── internal                 (directory, NOT a subfolder)
     * }</pre>
     *
     * <p>
     * In this example:
     * <ul>
     *   <li>{@code alpha}, {@code beta}, and {@code beta/docs} are marked as subfolders</li>
     *   <li>{@code beta/tmp} and {@code internal} exist in the directory tree but are
     *       <strong>not</strong> marked as subfolders and are therefore ignored</li>
     * </ul>
     * </p>
     *
     * <h3>Resulting Collection for {@code topFolder = /projects}</h3>
     *
     * <pre>{@code
     * {
     *   FolderInfo(alpha), FolderInfo(beta), FolderInfo(beta/docs)
     * }
     * }</pre>
     *
     * @param topFolderInfo
     *         the top-level folder info whose subfolders should be returned;
     *         must not be {@code null}
     * @return
     *         a {@link Collection} of {@link FolderInfo} objects;
     *         empty if no matching subfolders exist
     */
    Collection<FolderInfo> getSubFolders(FolderInfo topFolderInfo);
}
