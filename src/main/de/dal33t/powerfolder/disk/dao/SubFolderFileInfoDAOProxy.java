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
package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A FileInfoDAO proxy that scopes all file access to a subfolder.
 * Transforms FileInfos between subfolder and top-level folder views.
 */
public class SubFolderFileInfoDAOProxy extends Loggable implements FileInfoDAO {

    private final FileInfoDAO delegate;
    private final FolderInfo subfolderInfo;
    /** The folder whose database the delegate is: the top folder, or an interrupted subfolder above. */
    private final FolderInfo holderInfo;
    /** Where this subfolder sits inside {@link #holderInfo} - the coordinates the delegate speaks. */
    private final String subfolderPath;

    public SubFolderFileInfoDAOProxy(FileInfoDAO delegate, FolderInfo subfolderInfo,
        FolderInfo holderInfo)
    {
        Reject.ifNull(delegate, "delegate");
        Reject.ifNull(subfolderInfo, "subfolderInfo");
        Reject.ifNull(holderInfo, "holderInfo");
        Reject.ifFalse(subfolderInfo.isSubFolder(), "Must be subfolder");

        this.delegate = delegate;
        this.subfolderInfo = subfolderInfo;
        this.holderInfo = holderInfo;
        String location = subfolderInfo.getLocation().getRelativeName();
        /* PFS-5881: relative to the HOLDER, not to the top folder. An inheriting subfolder inside an
           interrupted one is stored by that interrupted folder - whose database names everything
           relative to itself - and a path in top-folder coordinates would miss it every time. */
        this.subfolderPath = holderInfo.isSubFolder()
            ? FolderInfo.relativeNameIn(holderInfo, subfolderInfo.getTopFolder(), location)
            : location;
        logFine(subfolderInfo + " initialized at subfolderPath=" + subfolderPath + " of " + holderInfo);
    }

    /** This subfolder's coordinates into the holder's. */
    private FileInfo toHolder(FileInfo f) {
        FileInfo top = FileInfoFactory.mapToTopFolder(f);
        return holderInfo.isSubFolder() ? FileInfoFactory.mapToSubFolder(top, holderInfo) : top;
    }

    /** The holder's coordinates back into top-folder ones, which {@link #toSub} maps from. */
    private FileInfo fromHolder(FileInfo f) {
        return holderInfo.isSubFolder() ? FileInfoFactory.mapToTopFolder(f) : f;
    }

    private FileInfo toSub(FileInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderPath)) {
            FileInfo mapped = FileInfoFactory.mapToSubFolder(fromHolder(f), subfolderInfo);
            if (mapped.isBaseDirectory()) {
                return null;
            }
            return mapped;
        }
        return null;
    }

    private DirectoryInfo toSub(DirectoryInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderPath)) {
            DirectoryInfo mapped = (DirectoryInfo) FileInfoFactory.mapToSubFolder(fromHolder(f),
                subfolderInfo);
            if (mapped.isBaseDirectory()) {
                return null;
            }
            return mapped;
        }
        return null;
    }


    private Collection<FileInfo> toSub(Collection<FileInfo> files) {
        return files.stream()
                .filter(f -> f.isInSubFolder(subfolderPath))
                .map(this::toSub)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        // delegate.stop();
    }

    @Override
    public void store(String domain, FileInfo... fInfos) {
        List<FileInfo> mapped = Arrays.stream(fInfos)
                .map(this::validateAndMapToHolder)
                .collect(Collectors.toList());
        delegate.store(domain, mapped);
    }

    @Override
    public void store(String domain, Collection<FileInfo> fInfos) {
        delegate.store(domain,
                fInfos.stream()
                        .map(this::validateAndMapToHolder)
                        .collect(Collectors.toList()));
    }

    private FileInfo validateAndMapToHolder(FileInfo f) {
        Reject.ifNull(f, "FileInfo");
        FolderInfo fi = f.getFolderInfo();
        if (!subfolderInfo.equals(fi)) {
            throw new IllegalArgumentException(
                    "FileInfo does not belong to subfolder " + subfolderInfo.getName()
                            + ": " + f.getRelativeName() + " (folder: " + fi.getName() + ")");
        }
        return toHolder(f);
    }

    @Override
    public FileInfo find(FileInfo fInfo, String domain) {
        FileInfo topFInfo = delegate.find(toHolder(fInfo), domain);
        if (topFInfo == null) {
            if (isFiner()) {
                logFiner(subfolderInfo.getName() + ": find " + fInfo.getRelativeName() + ": not found");
            }
            return null;
        }
        FileInfo subFInfo = toSub(topFInfo);
        if (isFiner()) {
            logFiner(subfolderInfo.getName() + ": find " + fInfo.getRelativeName() + " -> " + subFInfo);
        }
        return subFInfo;
    }

    @Override
    public FileInfo findNewestByOID(String oid, String... domains) {
        FileInfo result = delegate.findNewestByOID(oid, domains);
        return toSub(result);
    }

    @Override
    public FileInfo findNewestByHash(String hash, String... domains) {
        FileInfo result = delegate.findNewestByHash(hash, domains);
        return toSub(result);
    }

    @Override
    public void delete(String domain, FileInfo fInfo) {
        delegate.delete(domain, toHolder(fInfo));
    }

    @Override
    public void deleteDomain(String domain, int newInitialSize) {
        Collection<FileInfo> toDelete = findAllFiles(domain);
        for (FileInfo file : toDelete) {
            delete(domain, file);
        }
        Collection<DirectoryInfo> dirInfosToDelete = findAllDirectories(domain);
        for (DirectoryInfo dir : dirInfosToDelete) {
            delete(domain, dir);
        }
    }

    @Override
    public Collection<FileInfo> findAllFiles(String domain) {
        return delegate.findAllFiles(domain).stream()
                .filter(f -> f.isInSubFolder(subfolderPath))
                .map(this::toSub)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<DirectoryInfo> findAllDirectories(String domain) {
        return delegate.findAllDirectories(domain).stream()
                .filter(d -> d.isInSubFolder(subfolderPath))
                .map(this::toSub)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FileInfo> findFiles(FileInfoCriteria criteria) {
        String originalPath = criteria.getPath();

        criteria.mapToSubFolderPath(subfolderPath); // modifies in-place
        try {
            return delegate.findFiles(criteria).stream()
                    .filter(f -> f.isInSubFolder(subfolderPath))
                    .map(this::toSub)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } finally {
            criteria.setPath(originalPath); // restore path afterward
        }
    }

    @Override
    public Collection<FileInfo> findFilesFast(FileInfoCriteria criteria) {
        String originalPath = criteria.getPath();

        criteria.mapToSubFolderPath(subfolderPath); // modifies in-place
        try {
            Collection<FileInfo> result = delegate.findFilesFast(criteria).stream()
                    .filter(f -> f.isInSubFolder(subfolderPath))
                    .map(this::toSub)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (isFiner()) {
                logFiner(subfolderInfo.getName() + ": findFilesFast returned " + result.size() + " file(s) for "
                    + criteria.getPath());
            }

            return result;

        } finally {
            criteria.setPath(originalPath); // restore to avoid breaking caller
        }
    }


    @Override
    public FileHistory getFileHistory(FileInfo fileInfo) {
        return delegate.getFileHistory(toHolder(fileInfo));
    }

    @Override
    public int count(String domain, boolean includeDirs, boolean excludeIgnored) {
        FileInfoCriteria fc = new FileInfoCriteria();
        fc.addDomain(domain);
        fc.setRecursive(true);
        fc.setIncludeDeleted(true);
        return findFilesFast(fc).size();
    }

    @Override
    public boolean hasDomainWithFiles(String domain) {
        return count(domain, true, true) > 0;
    }
}
