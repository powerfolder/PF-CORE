package de.dal33t.powerfolder.disk.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * A {@link FileInfoDAO} implementation based on fast, in-memory
 * {@link ConcurrentHashMap}s.
 * 
 * @author sprajc
 */
public class FileInfoDAOHashMapImpl extends Loggable implements FileInfoDAO {
    private final ConcurrentMap<String, Domain> domains = Util
        .createConcurrentHashMap(4);

    private String selfDomain;
    private DiskItemFilter filter;

    public FileInfoDAOHashMapImpl(String selfDomain, DiskItemFilter filter) {
        super();
        this.selfDomain = selfDomain;
        this.filter = filter;
        if (filter == null) {
            this.filter = new DiskItemFilter();
        }
    }

    public int count(String domain, boolean includeDirs, boolean excludeIgnored)
    {
        Domain d = getDomain(domain);
        if (!excludeIgnored) {
            return d.files.size() + (includeDirs ? d.directories.size() : 0);
        } else {
            int c = 0;
            for (FileInfo fInfo : d.files.keySet()) {
                if (filter.isRetained(fInfo) && !fInfo.isDeleted()) {
                    c++;
                }
            }
            if (includeDirs) {
                for (FileInfo dInfo : d.directories.keySet()) {
                    if (filter.isRetained(dInfo) && !dInfo.isDeleted()) {
                        c++;
                    }
                }
            }
            return c;
        }
    }

    public int countInSync(String domain, boolean includeDirs,
        boolean excludeIgnored)
    {
        Domain d = getDomain(domain);
        int c = 0;
        for (FileInfo fInfo : d.files.values()) {
            if (filter.isExcluded(fInfo) || fInfo.isDeleted()) {
                continue;
            }
            FileInfo newestFileInfo = findNewestVersion(fInfo, domains.keySet());
            if (inSync(fInfo, newestFileInfo)) {
                c++;
            }
        }
        if (includeDirs) {
            for (FileInfo fInfo : d.directories.values()) {
                if (filter.isExcluded(fInfo) || fInfo.isDeleted()) {
                    continue;
                }
                FileInfo newestFileInfo = findNewestVersion(fInfo, domains
                    .keySet());
                if (inSync(fInfo, newestFileInfo)) {
                    c++;
                }
            }
        }
        return c;
    }

    public long bytesInSync(String domain) {
        Domain d = getDomain(domain);
        long bytes = 0;
        for (FileInfo fInfo : d.files.values()) {
            if (filter.isExcluded(fInfo) || fInfo.isDeleted()) {
                continue;
            }
            FileInfo newestFileInfo = findNewestVersion(fInfo, domains.keySet());
            if (inSync(fInfo, newestFileInfo)) {
                bytes += fInfo.getSize();
            }
        }
        return bytes;
    }

    private static boolean inSync(FileInfo fileInfo, FileInfo newestFileInfo) {
        if (newestFileInfo == null) {
            // It is intended not to use Reject.ifNull for performance reasons.
            throw new NullPointerException("Newest FileInfo not found of "
                + fileInfo.toDetailString());
        }
        if (fileInfo == null) {
            return false;
        }
        return !newestFileInfo.isNewerThan(fileInfo);
    }

    public void delete(String domain, FileInfo info) {
        if (info.isFile()) {
            getDomain(domain).files.remove(info);
        } else {
            logWarning("Deleting directory: " + info.toDetailString());
            getDomain(domain).directories.remove(info);
        }
    }

    public void deleteDomain(String domain) {
        domains.remove(domain);
    }

    public FileInfo find(FileInfo info, String domain) {
        FileInfo res = getDomain(domain).files.get(info);
        if (res != null) {
            return res;
        }
        return getDomain(domain).directories.get(info);
    }

    public Collection<FileInfo> findAllFiles(String domain) {
        return Collections.unmodifiableCollection(getDomain(domain).files
            .values());
    }

    public Collection<DirectoryInfo> findAllDirectories(String domain) {
        return Collections.unmodifiableCollection(getDomain(domain).directories
            .values());
    }

    public FileInfo findNewestVersion(FileInfo info, String... domainStrings) {
        return findNewestVersion(info, Arrays.asList(domainStrings));
    }

    private FileInfo findNewestVersion(FileInfo info,
        Collection<String> domainStrings)
    {
        FileInfo newestVersion = null;
        for (String domain : domainStrings) {
            Domain d = getDomain(domain);

            // Get remote file
            FileInfo candidateFile = d.files.get(info);
            if (candidateFile == null) {
                candidateFile = d.directories.get(info);
            }

            if (candidateFile == null) {
                continue;
            }
            if (!candidateFile.isValid()) {
                continue;
            }
            // Check if remote file in newer
            if (newestVersion == null
                || candidateFile.isNewerThan(newestVersion))
            {
                // log.finer("Newer version found at " + member);
                newestVersion = candidateFile;
            }
        }
        return newestVersion;
    }

    public void stop() {
        domains.clear();
    }

    public void store(String domain, FileInfo... infos) {
        store(domain, Arrays.asList(infos));
    }

    public void store(String domain, Collection<FileInfo> infos) {
        Domain d = getDomain(domain);
        for (FileInfo fileInfo : infos) {
            if (fileInfo.isFile()) {
                d.files.put(fileInfo, fileInfo);
                // Make sure not dir is left with name name.
                d.directories.remove(fileInfo);
            } else {
                if (isFiner()) {
                    logFiner("Storing directory: " + fileInfo.toDetailString());
                }
                d.directories.put((DirectoryInfo) fileInfo,
                    (DirectoryInfo) fileInfo);
                // Make sure not file is left with name name.
                d.files.remove(fileInfo);
            }
        }
    }

    // Internals **************************************************************

    private Domain getDomain(String domain) {
        String theDomain = StringUtils.isBlank(domain) ? selfDomain : domain;
        synchronized (domains) {
            Domain d = domains.get(theDomain);
            if (d != null) {
                return d;
            }
            if (isFiner()) {
                logFiner("Domain '" + theDomain + "' created");
            }
            d = new Domain();
            domains.put(theDomain, d);
            return d;
        }
    }

    private static class Domain {
        private final ConcurrentMap<FileInfo, FileInfo> files = new ConcurrentHashMap<FileInfo, FileInfo>();
        private final ConcurrentMap<DirectoryInfo, DirectoryInfo> directories = new ConcurrentHashMap<DirectoryInfo, DirectoryInfo>();
    }

    public Iterator<FileInfo> findDifferentFiles(int maxResults,
        String... domains)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public FileHistory getFileHistory(FileInfo fileInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<FileInfo> findInDirectory(String path, String domainStr,
        boolean recursive)
    {
        List<FileInfo> files = new ArrayList<FileInfo>();
        Domain domain = getDomain(domainStr);
        for (FileInfo fInfo : domain.files.values()) {
            if (isInSubDir(fInfo, path, recursive)) {
                // In subdir, do not consider
                continue;
            }
            files.add(fInfo);
        }
        return files;
    }

    // public Collection<FileInfo> findInDirectory(String path, String...
    // domains)
    // {
    // Map<FileInfo, FileInfo> files = new HashMap<FileInfo, FileInfo>();
    // boolean findInOwnDomain = false;
    // for (String domain : domains) {
    // if (StringUtils.isBlank(domain)) {
    // findInOwnDomain = true;
    // // Add later.
    // continue;
    // }
    // Domain d = getDomain(domain);
    // for (FileInfo fInfo : d.files.values()) {
    // if (isInSubDir(fInfo, path)) {
    // // In subdir, do not consider
    // continue;
    // }
    // FileInfo existingFInfo = files.get(fInfo);
    // if (existingFInfo == null || fInfo.isNewerThan(fInfo)) {
    // // Add to files if not in or newer.
    // files.put(fInfo, fInfo);
    // }
    // }
    // }
    //
    // if (findInOwnDomain) {
    // Domain own = getDomain(null);
    // for (FileInfo fInfo : own.files.values()) {
    // if (isInSubDir(fInfo, path)) {
    // // In subdir, do not consider
    // continue;
    // }
    // files.put(fInfo, fInfo);
    // }
    // }
    // logWarning("Found " + files.size() + " files in subdir '" + path + "'");
    // return files.keySet();
    // }

    private boolean isInSubDir(FileInfo fInfo, String path, boolean recursive) {
        if (!fInfo.getRelativeName().startsWith(path)) {
            return false;
        }
        if (recursive) {
            return true;
        }
        int i = fInfo.getRelativeName().indexOf('/', path.length() + 2);
        // No other subdirectory at end.
        return i != -1;
    }

}
