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
     * Stores the FolderInfo only if the table has no row for it yet - an existing row is left alone.
     * <p>
     * For a copy that arrives inside a permission this is the only sensible write. Storing an account
     * or a group writes back the FolderInfo of every folder it holds a permission on, and that copy is
     * a snapshot from whenever the permission was read: while the migration runs, the folder has moved
     * on - a version, its tags, its inheritance flag - so an update from there can only ever overwrite
     * the newer row with an older one. {@link #store} refuses that by version and said so, thousands of
     * times a night; asking first is cheaper and says nothing.
     *
     * @param folderInfo the folder to insert if it is unknown
     */
    void storeIfMissing(FolderInfo folderInfo);

    /**
     * PFS-5835: Lifts every stored lookup instance back to an ordinary row - the one statement the
     * warning about them has been asking the operator to run.
     * <p>
     * A row with a negative version is a {@link de.dal33t.powerfolder.light.FolderInfoFactory#lookupInstance
     * lookup instance}: a query object that was written to the table. Hibernate re-reads it on every
     * hydration of an account or group holding a permission on that folder, and each read warns. On one
     * node of a customer's production system that was 1.6 million lines for 11 863 rows in three hours.
     * <p>
     * The blank name stays - it is not recoverable here - and nothing is kept from healing: a real
     * FolderInfo overwrites a version-0 row, and a blank name still counts as stale. Idempotent, so
     * every node of a cluster may run it.
     *
     * @return the number of rows corrected, {@code -1} when the statement failed
     */
    int repairLookupInstances();

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
     *         the folder info whose subfolders should be returned; must not be {@code null}. A
     *         SUBFOLDER is a valid question as well: the interruptions nest, so a subfolder can be
     *         the top folder of rows of its own
     * @return
     *         a {@link Collection} of {@link FolderInfo} objects;
     *         empty if no matching subfolders exist
     */
    Collection<FolderInfo> getSubFolders(FolderInfo topFolderInfo);
}
