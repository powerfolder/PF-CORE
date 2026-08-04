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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>
 * PFS-5510: Every index also publishes its interrupted subfolders process-wide, so {@link #barriers()}
 * can answer "which subfolders are currently decoupled?" without a
 * {@link de.dal33t.powerfolder.Controller}. Its only caller is
 * {@link FolderInfo#findEnclosingSubFolder(java.util.Collection, FolderInfo, String)}, which resolves
 * the subfolder that governs an addressed location: it must stop at an interrupted subfolder even when
 * the evaluated account or group holds no permission on it, because such a subfolder is invisible in
 * its permission structures. Keeping the lookup there means every caller of that resolution honors the
 * interruption without knowing about this index.
 * <p>
 * That resolution runs on the web hot path (every file listing, upload and editor open), so the lookup
 * is allocation-free just like the scan/watch checks: a single volatile read of a pre-merged array. The
 * per-index bookkeeping stays in a registry so several controllers in one JVM (tests) cannot overwrite
 * each other; folder IDs are globally unique and the flag travels inside the replicated
 * {@link FolderInfo}, so the union over all sections cannot contradict itself.
 *
 * @author Christian Sprajc
 */
public class InterruptedSubFolderIndex {

    private static final Path[] NO_BASES = new Path[0];
    private static final FolderInfo[] NO_INFOS = new FolderInfo[0];

    // One section per index instance, replaced atomically on refresh, removed on shutdown, plus the
    // flat union over all sections. Only the union is read by barriers(), so that lookup is a single
    // volatile read - the sections exist to keep several controllers in one JVM apart.
    private static final Map<InterruptedSubFolderIndex, FolderInfo[]> REGISTRY = new ConcurrentHashMap<>();
    private static volatile FolderInfo[] allBarriers = NO_INFOS;

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
        // Always (re)register, also with an empty snapshot - a section that vanished would silently
        // turn a decoupled subfolder back into an inheriting one for the permission evaluation.
        REGISTRY.put(this, subFolders);
        mergeBarriers();
    }

    /**
     * Drops this index' section. Called when the owning {@link FolderRepository} shuts down, otherwise
     * a controller would keep its section for the lifetime of the JVM (relevant for tests, which start
     * and stop many controllers).
     */
    void unregister() {
        REGISTRY.remove(this);
        mergeBarriers();
    }

    /**
     * Rebuilds the flat union read by {@link #barriers()}. Called only from the structural events
     * above, never from a lookup - all allocation for the barrier lookup happens here.
     */
    private static void mergeBarriers() {
        List<FolderInfo> merged = null;
        for (FolderInfo[] section : REGISTRY.values()) {
            for (FolderInfo subFolder : section) {
                if (merged == null) {
                    merged = new ArrayList<>();
                }
                if (!merged.contains(subFolder)) {
                    merged.add(subFolder);
                }
            }
        }
        allBarriers = merged == null ? NO_INFOS : merged.toArray(new FolderInfo[0]);
    }

    /**
     * PFS-5510: All subfolders whose permission inheritance is currently interrupted (PFC-3543) - the
     * barriers a permission evaluation must not look past. Answered without a
     * {@link de.dal33t.powerfolder.Controller}, so the structure-based permission resolution in
     * {@link FolderInfo} can use it.
     * <p>
     * Allocation-free: a single volatile read of a pre-merged array, which the caller filters by top
     * folder in the loop it runs anyway. No feature check is needed - and deliberately none is made:
     * {@link #refresh} selects entries via {@link FolderInfo#inheritsPermissions()}, which reports
     * "inherits" while {@link de.dal33t.powerfolder.Feature#FOLDER_PERMISSION_INHERITANCE_INTERRUPTION}
     * is disabled. With the feature off - the production default, it is a startup switch - the snapshot
     * is therefore the empty constant and the caller skips out on its length.
     * <p>
     * Only MOUNTED folders are known here. That is not a gap in practice: the server mounts all
     * subfolders together with their top folder, and without a mounted top folder there is no target
     * for an operation in the first place.
     *
     * @return the interrupted subfolders, empty when there are none. A SHARED snapshot - never modify it
     */
    public static FolderInfo[] barriers() {
        return allBarriers;
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
