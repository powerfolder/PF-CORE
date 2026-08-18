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
import java.util.*;
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

    // Startup/mount bridge: interrupted subfolders KNOWN to exist (from the configuration or the
    // server database) whose Folder objects are not mounted yet. Top folders are created (and their
    // scan runs asynchronously) before their subfolders - without the seeds the scanner would descend
    // into an interrupted subtree in exactly that window and re-ingest it into the top folder's
    // database. Merged into every refresh; a seed is dropped for good once its folder mounts - the
    // mount state is the authority from then on.
    private final Map<FolderInfo, Path> seeds = new ConcurrentHashMap<>();

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
        Set<FolderInfo> mounted = seeds.isEmpty() ? null : new HashSet<>();
        for (Folder folder : folders) {
            FolderInfo foInfo = folder.getInfo();
            if (mounted != null) {
                mounted.add(foInfo);
            }
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
        // Merge the seeds of subfolders that are not mounted yet. A seed whose folder mounted -
        // interrupted or not - has served its purpose: the mount state is the authority.
        if (mounted != null) {
            for (Map.Entry<FolderInfo, Path> seed : seeds.entrySet()) {
                if (mounted.contains(seed.getKey())) {
                    seeds.remove(seed.getKey());
                    continue;
                }
                if (newBases == null) {
                    newBases = new LinkedList<>();
                    newSubs = new LinkedList<>();
                }
                newBases.add(seed.getValue());
                newSubs.add(seed.getKey());
            }
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
     * PFC-3543: Pre-seeds the index with a subfolder KNOWN to be interrupted (from the configuration
     * or the server database) whose {@link Folder} object is not mounted yet. Top folders are created
     * first and their scan runs asynchronously - without the seed that scan would descend into the
     * interrupted subtree before the subfolder mounts (the mounted-folder refresh cannot know it yet)
     * and the DAO store guard would be the only - also mount-dependent - line of defense. Additive
     * and idempotent; the seed takes part in every {@link #refresh} and is dropped for good once its
     * folder mounts. The caller triggers a refresh afterwards to publish the seed.
     *
     * @param subFolder the interrupted subfolder
     * @param base      its local base, derived as top folder base + location
     */
    void seed(FolderInfo subFolder, Path base) {
        seeds.put(subFolder, base);
    }

    /**
     * Ends a seeding bridge wholesale: every expected subfolder has been created (or failed and was
     * logged), the mounted-folder refresh is the only authority again. Used by the config-based
     * startup, which knows when its folder creation is complete. The caller triggers a refresh
     * afterwards.
     */
    void clearSeeds() {
        seeds.clear();
    }

    /**
     * Rebuilds the flat union read by {@link #barriers()}. Called only from the structural events
     * above, never from a lookup - all allocation for the barrier lookup happens here.
     */
    private static void mergeBarriers() {
        /* The set does the de-duplication: a list scanned with contains() made this quadratic in the
         * number of barriers, and it runs on every single interruption. A migration that interrupts
         * thousands of subfolders spent its time here (measured in a thread dump: ArrayList.indexOf
         * under mergeBarriers). Insertion order is kept, so the snapshot stays stable. */
        Set<FolderInfo> merged = null;
        for (FolderInfo[] section : REGISTRY.values()) {
            for (FolderInfo subFolder : section) {
                if (merged == null) {
                    merged = new LinkedHashSet<>();
                }
                merged.add(subFolder);
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
     * still scans and stores its own content.
     * <p>
     * PFC-3575: the subfolder's root directory itself counts as foreign as well. It used to be
     * excluded so the row stayed behind in the top folder and kept the subfolder listed - which meant
     * the scanner descended into the subtree on every run, produced the entry, and the DAO guard threw
     * the store away again: a permanent warning loop for no gain. The subfolder is surfaced by the
     * folder view now (synthesized for callers who may see it), so the top folder has no business
     * touching that path at all.
     *
     * @param path    an absolute path (a scanned directory or a watched file)
     * @param ownBase the local base of the querying folder (excluded from the match)
     * @return {@code true} if {@code path} is at or below an interrupted subfolder other than the
     *         querying folder itself
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
            if (path.startsWith(base)) {
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
