package de.dal33t.powerfolder.disk.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria.Type;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
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
        if (excludeIgnored) {
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
        } else {
            return d.files.size() + (includeDirs ? d.directories.size() : 0);
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
                FileInfo newestFileInfo = findNewestVersion(fInfo,
                    domains.keySet());
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

    public void deleteDomain(String domain, int newInitialSize) {
        String theDomain = StringUtils.isBlank(domain) ? selfDomain : domain;
        domains.remove(theDomain);
        if (newInitialSize > 0) {
            domains.put(theDomain, new Domain(newInitialSize));
            if (isFiner()) {
                logFiner("Created new domain (" + theDomain
                    + ") with initial capacity " + newInitialSize);
            }
        }
    }

    public FileInfo find(FileInfo info, String domain) {
        FileInfo res = getDomain(domain).files.get(info);
        if (res != null) {
            return res;
        }
        return getDomain(domain).directories.get(info);
    }

    @Override
    public FileInfo findNewestByOID(String oid, String... domains) {
        Reject.ifBlank(oid, "OID");
        FileInfo newestVersion = null;
        for (String domain : domains) {
            Domain d = getDomain(domain);
            for (FileInfo candidateFile : d.files.values()) {
                if (StringUtils.isBlank(candidateFile.getOID())) {
                    continue;
                }
                if (candidateFile.getOID().equals(oid)) {
                    if (newestVersion == null
                        || candidateFile.isNewerThan(newestVersion))
                    {
                        newestVersion = candidateFile;
                    }
                }
            }
            for (FileInfo candidateFile : d.directories.values()) {
                if (StringUtils.isBlank(candidateFile.getOID())) {
                    continue;
                }
                if (candidateFile.getOID().equals(oid)) {
                    if (newestVersion == null
                        || candidateFile.isNewerThan(newestVersion))
                    {
                        newestVersion = candidateFile;
                    }
                }
            }
        }
        return newestVersion;
    }

    @Override
    public FileInfo findNewestByHash(String hash, String... domains) {
        Reject.ifBlank(hash, "Hash");
        FileInfo newestVersion = null;
        for (String domain : domains) {
            Domain d = getDomain(domain);
            for (FileInfo candidateFile : d.files.values()) {
                if (candidateFile.isMatchingHash(hash)) {
                    if (newestVersion == null
                        || candidateFile.isNewerThan(newestVersion))
                    {
                        newestVersion = candidateFile;
                    }
                }
            }
            for (FileInfo candidateFile : d.directories.values()) {
                if (candidateFile.isMatchingHash(hash)) {
                    if (newestVersion == null
                        || candidateFile.isNewerThan(newestVersion))
                    {
                        newestVersion = candidateFile;
                    }
                }
            }
        }
        return newestVersion;
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

    public Collection<FileInfo> findInDirectory(String domainStr,
        DirectoryInfo directoryInfo, boolean recursive)
    {
        FileInfoCriteria crit = new FileInfoCriteria();
        crit.addDomain(domainStr);
        crit.setPath(directoryInfo);
        crit.setRecursive(recursive);
        return findFiles(crit);
    }

    public Collection<FileInfo> findInDirectory(String domainStr, String path,
        boolean recursive)
    {
        FileInfoCriteria crit = new FileInfoCriteria();
        crit.addDomain(domainStr);
        crit.setPath(path);
        crit.setRecursive(recursive);
        return findFiles(crit);
    }

    public Collection<FileInfo> findFiles(FileInfoCriteria criteria) {
        Reject.ifTrue(criteria.getDomains().isEmpty(),
            "No domains/members selected in criteria");
        String path = criteria.getPath();
        if (path == null) {
            path = "";
        }
        if (path.equals("/")) {
            path = "";
        }
        if (path.length() > 0 && !path.endsWith("/")) {
            path += "/";
        }
        boolean recursive = criteria.isRecursive();
        Collection<FileInfo> items = new HashSet<FileInfo>();
        for (String domainStr : criteria.getDomains()) {
            Domain domain = getDomain(domainStr);
            if (domain == null) {
                continue;
            }
            if (criteria.getType() == Type.DIRECTORIES_ONLY
                || criteria.getType() == Type.FILES_AND_DIRECTORIES)
            {
                for (DirectoryInfo dInfo : domain.directories.values()) {
                    // if (filter.isExcluded(dInfo)) {
                    // continue;
                    // }

                    if (criteria.getMaxResults() > 0
                        && items.size() >= criteria.getMaxResults())
                    {
                        return items;
                    }

                    if (isInSubDir(dInfo, path, recursive)
                        && !Util.equalsRelativeName(dInfo.getRelativeName(),
                            path))
                    {
                        if (!items.contains(dInfo)
                            && matches(dInfo, criteria.getKeyWords()))
                        {
                            items.add(dInfo);
                        }
                    }
                }
            }

            if (criteria.getType() == Type.FILES_ONLY
                || criteria.getType() == Type.FILES_AND_DIRECTORIES)
            {
                for (FileInfo fInfo : domain.files.values()) {
                    // if (filter.isExcluded(fInfo)) {
                    // continue;
                    // }

                    if (criteria.getMaxResults() > 0
                        && items.size() >= criteria.getMaxResults())
                    {
                        return items;
                    }

                    if (isInSubDir(fInfo, path, recursive)) {
                        if (!items.contains(fInfo)
                            && matches(fInfo, criteria.getKeyWords()))
                        {
                            items.add(fInfo);
                        }
                    }
                }
            }
        }
        return items;
    }

    public FileHistory getFileHistory(FileInfo fileInfo) {
        // TODO Auto-generated method stub
        return null;
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
            d = new Domain(500);
            domains.put(theDomain, d);
            return d;
        }
    }

    /*
     * TODO: Performance optimization
     */
    private boolean matches(FileInfo fInfo, Set<String> keyWords) {
        if (keyWords.isEmpty()) {
            return true;
        }
        String lower = fInfo.getRelativeName().toLowerCase();
        for (String keyWord : keyWords) {
            if (!lower.contains(keyWord)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInSubDir(FileInfo fInfo, String path, boolean recursive) {
        if (!fInfo.getRelativeName().startsWith(path)) {
            return false;
        }
        if (recursive) {
            return true;
        }
        int offset = path.length() + 1;
        int i = fInfo.getRelativeName().indexOf('/', offset);
        // No other subdirectory at end.
        return i < 0;
    }

    private static class Domain {

        private final ConcurrentMap<FileInfo, FileInfo> files;
        private final ConcurrentMap<DirectoryInfo, DirectoryInfo> directories = Util
            .createConcurrentHashMap(4);

        public Domain(int suggestedSize) {
            super();
            files = Util.createConcurrentHashMap(suggestedSize);
        }

        public String toString() {
            return "Domain: " + files.size() + " files, " + directories.size()
                + " dirs";
        }
    }

}
