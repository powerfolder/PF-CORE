package de.dal33t.powerfolder.disk.dao;

import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.util.Reject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A FileInfoDAO proxy that scopes all file access to a subfolder.
 * Transforms FileInfos between subfolder and top-level folder views.
 */
public class SubFolderFileInfoDAOProxy implements FileInfoDAO {

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
    }

    private FileInfo toTop(FileInfo f) {
        return FileInfoFactory.mapToTopFolder(f);
    }

    private FileInfo toSub(FileInfo f) {
        if (f.isInSubFolder(subfolderInfo)) {
            return FileInfoFactory.mapToSubFolder(f, subfolderInfo);
        }
        return null;
    }

    private Collection<FileInfo> toSub(Collection<FileInfo> files) {
        return files.stream().map(this::toSub).filter(Objects::nonNull).collect(Collectors.toList());
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
        return toSub(delegate.find(toTop(fInfo), domain));
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
        return toSub(delegate.findAllFiles(domain));
    }

    @Override
    public Collection<DirectoryInfo> findAllDirectories(String domain) {
        return delegate.findAllDirectories(domain).stream()
                .filter(d -> d.getRelativeName().startsWith(subfolderPath))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FileInfo> findFiles(FileInfoCriteria criteria) {
        return toSub(delegate.findFiles(criteria));
    }

    @Override
    public Collection<FileInfo> findFilesFast(FileInfoCriteria criteria) {
        return toSub(delegate.findFilesFast(criteria));
    }

    @Override
    public FileHistory getFileHistory(FileInfo fileInfo) {
        return delegate.getFileHistory(toTop(fileInfo));
    }

    @Override
    public int count(String domain, boolean includeDirs, boolean excludeIgnored) {
        FileInfoCriteria fc = new FileInfoCriteria();
        fc.addDomain(domain);
        fc.setPath(subfolderPath);
        fc.setRecursive(true);
        fc.setIncludeDeleted(true);
        return findFilesFast(fc).size();
    }

    @Override
    public boolean hasDomainWithFiles(String domain) {
        return count(domain, true, true) > 0;
    }
}
