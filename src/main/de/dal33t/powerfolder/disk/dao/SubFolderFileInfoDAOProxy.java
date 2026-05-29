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
        Reject.ifFalse(subfolderInfo.isSubFolder(), "Must be subfolder");

        this.delegate = delegate;
        this.subfolderInfo = subfolderInfo;
        this.subfolderPath =  subfolderInfo.getLocation().getRelativeName();
        logInfo(subfolderInfo + " initialized at subfolderPath=" + subfolderPath);
    }

    private FileInfo toTop(FileInfo f) {
        return FileInfoFactory.mapToTopFolder(f);
    }

    private FileInfo toSub(FileInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderPath)) {
            return FileInfoFactory.mapToSubFolder(f, subfolderInfo);
        }
        return null;
    }

    private DirectoryInfo toSub(DirectoryInfo f) {
        Reject.ifNull(f, "FileInfo");
        if (f.isInSubFolder(subfolderPath)) {
            return (DirectoryInfo) FileInfoFactory.mapToSubFolder(f, subfolderInfo);
        }
        return null;
    }


    private Collection<FileInfo> toSub(Collection<FileInfo> files) {
        return files.stream()
                .filter(f -> f.isInSubFolder(subfolderPath))
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
                .map(this::validateAndMapToTop)
                .collect(Collectors.toList());
        delegate.store(domain, mapped);
    }

    @Override
    public void store(String domain, Collection<FileInfo> fInfos) {
        delegate.store(domain,
                fInfos.stream()
                        .map(this::validateAndMapToTop)
                        .collect(Collectors.toList()));
    }

    private FileInfo validateAndMapToTop(FileInfo f) {
        Reject.ifNull(f, "FileInfo");
        FolderInfo fi = f.getFolderInfo();
        if (!subfolderInfo.equals(fi)) {
            throw new IllegalArgumentException(
                    "FileInfo does not belong to subfolder " + subfolderInfo.getName()
                            + ": " + f.getRelativeName() + " (folder: " + fi.getName() + ")");
        }
        return toTop(f);
    }

    @Override
    public FileInfo find(FileInfo fInfo, String domain) {
        if (isFine()) {
            logFine("find    : " + fInfo);
            logFine("toTop   : " + toTop(fInfo));
            logFine("delegate: " + delegate.find(toTop(fInfo), domain));
        }

        FileInfo topFInfo = delegate.find(toTop(fInfo), domain);
        if (topFInfo == null) {
            return null;
        }
        logFine("toSub   : " + toSub(topFInfo));
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
                .filter(f -> f.isInSubFolder(subfolderPath))
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
                    .filter(f -> f.isInSubFolder(subfolderPath))
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
                    .filter(f -> f.isInSubFolder(subfolderPath))
                    .map(this::toSub)
                    .collect(Collectors.toList());

            // Log output summary
            logFine("findFilesFast result: " + result.size() + " file(s) in subfolder '" +
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
