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

import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Pair;
import de.dal33t.powerfolder.util.SimpleCache;

/**
 * Base class for {@link SecurityManager} implementations: central, cached
 * read/write permission checks on folders.
 *
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 */
public abstract class AbstractSecurityManager extends PFComponent implements
    SecurityManager
{

    // PFS-638: Moved here from Folder.
    private static final long HAS_PERMISSION_CACHE_TIMEOUT = 987L;
    private final SimpleCache<Pair<Member, FolderInfo>, Boolean> hasReadCache =
        new SimpleCache<>(HAS_PERMISSION_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
    private final SimpleCache<Pair<Member, FolderInfo>, Boolean> hasWriteCache =
        new SimpleCache<>(HAS_PERMISSION_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);

    protected AbstractSecurityManager(Controller controller) {
        super(controller);
    }

    /**
     * PFC-3550 / PFS-4787: Whether permission answers are currently
     * authoritative. Client implementations return defaults while not
     * connected and logged in at the server - such answers must not be
     * cached or acted upon destructively.
     *
     * @return true if permission answers are authoritative.
     */
    protected boolean isAuthoritative() {
        return true;
    }

    @Override
    public boolean hasReadPermission(Member member, FolderInfo foInfo) {
        foInfo = contentFolderInfo(foInfo);
        Pair<Member, FolderInfo> key = new Pair<>(member, foInfo);
        Boolean hasRead = hasReadCache.getValidEntry(key);
        if (hasRead != null) {
            if (hasReadCache.getCacheHits() % 100000 == 0 && isFine()) {
                logFine("Permission read: " + hasReadCache);
            }
            return hasRead;
        }
        // PFC-3550 / PFS-4787: Only cache authoritative answers. Checked
        // before and after to not cache an answer computed while the login
        // state was changing.
        boolean authoritative = isAuthoritative();
        hasRead = hasPermission(member.getInfo(),
            FolderPermission.read(foInfo));
        if (authoritative && isAuthoritative()) {
            hasReadCache.put(key, hasRead);
        }
        return hasRead;
    }

    @Override
    public boolean hasWritePermission(Member member, FolderInfo foInfo) {
        foInfo = contentFolderInfo(foInfo);
        Pair<Member, FolderInfo> key = new Pair<>(member, foInfo);
        Boolean hasWrite = hasWriteCache.getValidEntry(key);
        if (hasWrite != null) {
            if (hasWriteCache.getCacheHits() % 100000 == 0 && isFine()) {
                logFine("Permission write: " + hasWriteCache);
            }
            return hasWrite;
        }
        // PFC-3550 / PFS-4787: Only cache authoritative answers. Checked
        // before and after to not cache an answer computed while the login
        // state was changing.
        boolean authoritative = isAuthoritative();
        hasWrite = hasPermission(member.getInfo(),
            FolderPermission.readWrite(foInfo));
        if (authoritative && isAuthoritative()) {
            hasWriteCache.put(key, hasWrite);
        }
        return hasWrite;
    }

    @Override
    public void clearPermissionCache(Member node) {
        hasReadCache.invalidateKey(key -> key.getFirst().equals(node));
        hasWriteCache.invalidateKey(key -> key.getFirst().equals(node));
    }

    @Override
    public void clearPermissionCache() {
        hasReadCache.invalidateAll();
        hasWriteCache.invalidateAll();
    }

    private static FolderInfo contentFolderInfo(FolderInfo foInfo) {
        if (foInfo.isMetaFolder()) {
            return foInfo.lookupContentFolderInfo();
        }
        return foInfo;
    }
}
