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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * PFC-3543: Index of all subfolders whose permission inheritance is currently
 * interrupted. Such a subfolder is managed by its own {@link Folder} and its own
 * {@link de.dal33t.powerfolder.disk.dao.FileInfoDAO}; the top folder must ignore
 * that subtree during scan, watch and DAO writes, so its data never leaks into the
 * top folder's database - a data isolation and security requirement.
 * <p>
 * Held as a field of {@link FolderRepository} (the authority for structural
 * changes). It is read on the scan/watcher/DAO hot path (very frequently), so the
 * lookups are allocation-free (a single volatile read + index loop). The cache is
 * recomputed only on the rare events that can change the set: a folder is added,
 * removed or renamed, or an inheritance interruption is toggled. The common case
 * (feature off / no interruptions) is an empty index and returns immediately.
 *
 * @author Christian Sprajc
 */
class InterruptedSubFolderIndex {

    private static final Path[] NO_BASES = new Path[0];
    private static final FolderInfo[] NO_INFOS = new FolderInfo[0];

    // Two parallel snapshots, swapped atomically on refresh: absolute local bases
    // for the Path-based scanner/watcher checks, and the matching FolderInfos for
    // the allocation-free relative-name FileInfo check.
    private volatile Path[] bases = NO_BASES;
    private volatile FolderInfo[] subFolders = NO_INFOS;

    /**
     * Recomputes the index from the given folders. Allocates only here, never on
     * the read hot path. An interrupted subfolder is one that is a subfolder, does
     * not inherit permissions and has a local base.
     *
     * @param folders all (non-meta) folders of the repository
     */
    void refresh(Collection<Folder> folders) {
        List<Path> newBases = null;
        List<FolderInfo> newSubs = null;
        for (Folder folder : folders) {
            FolderInfo foInfo = folder.getInfo();
            if (!foInfo.isSubFolder() || foInfo.inheritsPermissions()) {
                continue;
            }
            Path base = folder.getLocalBase();
            if (base == null) {
                continue;
            }
            if (newBases == null) {
                newBases = new LinkedList<>();
                newSubs = new LinkedList<>();
            }
            newBases.add(base);
            newSubs.add(foInfo);
        }
        bases = newBases == null ? NO_BASES : newBases.toArray(new Path[0]);
        subFolders = newSubs == null ? NO_INFOS : newSubs.toArray(new FolderInfo[0]);
    }

    /**
     * @return {@code true} if there are currently no interrupted subfolders (the
     *         common case). Lets callers skip any per-item work.
     */
    boolean isEmpty() {
        return bases.length == 0;
    }

    /**
     * Path-based membership check for the scanner and watcher, which already hold
     * an absolute {@link Path}. The scanner descends directory by directory and
     * hits an interrupted subfolder exactly at its base; the watcher sees files
     * deep inside the subtree directly - a single {@code startsWith} covers both.
     * <p>
     * Allocation-free. {@code ownBase} is excluded so an interrupted subfolder
     * still scans and stores its own content. The subfolder's root directory itself
     * ({@code path.equals(base)}) is NOT foreign either: it stays in the top folder so
     * the subfolder remains listed/navigable - only paths strictly below it are foreign.
     * PFC-3575: the visibility model for that kept root node is a follow-up.
     *
     * @param path    an absolute path (a scanned directory or a watched file)
     * @param ownBase the local base of the querying folder (excluded from the match)
     * @return {@code true} if {@code path} is strictly below an interrupted subfolder
     *         other than the querying folder itself
     */
    boolean contains(Path path, Path ownBase) {
        if (path == null) {
            return false;
        }
        Path[] snapshot = bases;
        for (int i = 0; i < snapshot.length; i++) {
            Path base = snapshot[i];
            if (base.equals(ownBase)) {
                // Never treat the querying folder's own subtree as foreign.
                continue;
            }
            if (path.startsWith(base) && !path.equals(base)) {
                return true;
            }
        }
        return false;
    }

    /**
     * FileInfo-based membership check for the DAO store path. Compares relative
     * names as Strings via {@link FileInfo#isInSubFolder(FolderInfo)} and allocates
     * no {@link Path} per file. Only interrupted subfolders of {@code ownInfo} are
     * considered (relative names are only comparable within the same top folder),
     * which also excludes the querying folder's own subtree.
     *
     * @param fInfo   a file of the querying folder
     * @param ownInfo the {@link FolderInfo} of the querying folder
     * @return {@code true} if {@code fInfo} belongs to an interrupted subfolder
     */
    boolean contains(FileInfo fInfo, FolderInfo ownInfo) {
        if (fInfo == null) {
            return false;
        }
        FolderInfo[] snapshot = subFolders;
        for (int i = 0; i < snapshot.length; i++) {
            FolderInfo sub = snapshot[i];
            FolderInfo subTop = sub.getTopFolder();
            if (subTop == null || !subTop.equals(ownInfo)) {
                continue;
            }
            if (fInfo.isInsideSubFolder(sub)) {
                return true;
            }
        }
        return false;
    }
}
