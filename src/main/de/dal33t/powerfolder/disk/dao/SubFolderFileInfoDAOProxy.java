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
    private final String subfolderPath;

    public SubFolderFileInfoDAOProxy(FileInfoDAO delegate, FolderInfo subfolderInfo) {
        Reject.ifNull(delegate, "delegate");
        Reject.ifNull(subfolderInfo, "subfolderInfo");
        Reject.ifNull(subfolderInfo.getParent(), "subfolderInfo must have a parent FolderInfo");

        this.delegate = delegate;
        this.subfolderInfo = subfolderInfo;
        this.subfolderPath = subfolderInfo.getParent().getRelativeName() + '/' + subfolderInfo.getName();
        logInfo(subfolderInfo + " initialized subfolderPath=" + subfolderPath);
    }

    private FileInfo toTop(FileInfo f) {
        return FileInfoFactory.mapToTopFolder(f);
    }

    private FileInfo toSub(FileInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderInfo)) {
            return FileInfoFactory.mapToSubFolder(f, subfolderInfo);
        }
        return null;
    }

    private DirectoryInfo toSub(DirectoryInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderInfo)) {
            return (DirectoryInfo) FileInfoFactory.mapToSubFolder(f, subfolderInfo);
        }
        return null;
    }


    private Collection<FileInfo> toSub(Collection<FileInfo> files) {
        return files.stream()
                .filter(f -> f.isInSubFolder(subfolderInfo))
                .map(this::toSub)
                .collect(Collectors.toList());
    }

    @Override
    public void stop() {
        // delegate.stop();
    }

    @Override
    public void store(String domain, FileInfo... fInfos) {
        List<FileInfo> mapped = Arrays.stream(fInfos)
                .map(this::toTop)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        delegate.store(domain, mapped);
    }

    @Override
    public void store(String domain, Collection<FileInfo> fInfos) {
        delegate.store(domain,
                fInfos.stream()
                        .map(this::toTop)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Override
    public FileInfo find(FileInfo fInfo, String domain) {
        if (isFine()) {
            logFine("find in " + domain + ": " + fInfo);
            logFine("toTop: " + toTop(fInfo));
            logFine("delegate: " + delegate.find(toTop(fInfo), domain));
        }

        FileInfo topFInfo = delegate.find(toTop(fInfo), domain);
        if (topFInfo == null) {
            return null;
        }
        return toSub(topFInfo);
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
        delegate.delete(domain, toTop(fInfo));
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
                .filter(f -> f.isInSubFolder(subfolderInfo))
                .map(this::toSub)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<DirectoryInfo> findAllDirectories(String domain) {
        return delegate.findAllDirectories(domain).stream()
                .filter(d -> d.getRelativeName().startsWith(subfolderPath))
                .map(this::toSub)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FileInfo> findFiles(FileInfoCriteria criteria) {
        String originalPath = criteria.getPath();

        criteria.mapToSubFolderPath(subfolderPath); // modifies in-place
        try {
            return delegate.findFiles(criteria).stream()
                    .filter(f -> f.isInSubFolder(subfolderInfo))
                    .map(this::toSub)
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
                    .filter(f -> f.isInSubFolder(subfolderInfo))
                    .map(this::toSub)
                    .collect(Collectors.toList());

            // Log output summary
            logInfo("findFilesFast result: " + result.size() + " file(s) in subfolder '" +
                    subfolderInfo.getName() + "' final path of criteria: " + criteria.getPath());

            return result;

        } finally {
            criteria.setPath(originalPath); // restore to avoid breaking caller
        }
    }


    @Override
    public FileHistory getFileHistory(FileInfo fileInfo) {
        return delegate.getFileHistory(toTop(fileInfo));
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
