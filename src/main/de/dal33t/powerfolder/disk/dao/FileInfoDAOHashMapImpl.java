package de.dal33t.powerfolder.disk.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileHistory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * A {@link FileInfoDAO} implementation based on fast, in-memory
 * {@link ConcurrentHashMap}s.
 * 
 * @author sprajc
 */
public class FileInfoDAOHashMapImpl extends Loggable implements FileInfoDAO {
    private boolean ignoreFileNameCase;
    private final ConcurrentMap<String, Domain> domains = Util.createConcurrentHashMap();

    public FileInfoDAOHashMapImpl() {
        super();
        ignoreFileNameCase = OSUtil.isWindowsSystem();
        // ignoreFileNameCase = false;
    }

    /**
     * FOR TESTS ONLY!!!.
     * 
     * @param ignoreFileNameCase
     */
    public void setIgnoreFileNameCase(boolean ignoreFileNameCase) {
        this.ignoreFileNameCase = ignoreFileNameCase;
    }

    public int count(String domain) {
        Domain d = getDomain(domain);
        return d.files.size() + d.directories.size();
    }

    public void delete(String domain, FileInfo info) {
        if (ignoreFileNameCase) {
            if (info.isFile()) {
                getDomain(domain).files.remove(info);
            } else {
                logWarning("Deleting directory: " + info.toDetailString());
                getDomain(domain).directories.remove(info);
            }
        } else {
            if (info.isFile()) {
                getDomain(domain).files.remove(info);
            } else {
                logWarning("Deleting directory: " + info.toDetailString());
                getDomain(domain).directories.remove(info);
            }
        }
    }

    public void deleteDomain(String domain) {
        domains.remove(domain);
    }

    public FileInfo find(FileInfo info, String domain) {
        if (ignoreFileNameCase) {
            if (info.isFile()) {
                return getDomain(domain).files.get(info);
            } else {
                return getDomain(domain).directories.get(info);
            }
        } else {
            if (info.isFile()) {
                return getDomain(domain).files.get(info);
            } else {
                return getDomain(domain).directories.get(info);
            }
        }
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
        FileInfo newestVersion = null;
        for (String domain : domainStrings) {
            Domain d = getDomain(domain);

            // Get remote file
            FileInfo candidateFile;
            if (ignoreFileNameCase) {
                candidateFile = d.files.get(info);
                if (candidateFile == null) {
                    candidateFile = d.directories.get(info);
                }
            } else {
                candidateFile = d.files.get(info);
                if (candidateFile == null) {
                    candidateFile = d.directories.get(info);
                }
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
            if (ignoreFileNameCase) {
                // TODO Might produce a lot extra strings in RAM
                if (fileInfo.isFile()) {
                    d.files.put(fileInfo, fileInfo);
                } else {
                    if (isFiner()) {
                        logFiner("Storing directory: "
                            + fileInfo.toDetailString());
                    }
                    d.directories.put((DirectoryInfo) fileInfo,
                        (DirectoryInfo) fileInfo);
                }
            } else {
                if (fileInfo.isFile()) {
                    d.files.put(fileInfo, fileInfo);
                } else {
                    if (isFiner()) {
                        logFiner("Storing directory: "
                            + fileInfo.toDetailString());
                    }
                    d.directories.put((DirectoryInfo) fileInfo,
                        (DirectoryInfo) fileInfo);
                }
            }
        }
    }

    // Internals **************************************************************

    private Domain getDomain(String domain) {
        String theDomain = domain != null ? domain : "";
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

    public Collection<FileInfo> findInDirectory(String path, String domainStr) {
        List<FileInfo> files = new ArrayList<FileInfo>();
        Domain domain = getDomain(domainStr);
        for (FileInfo fInfo : domain.files.values()) {
            if (isInSubDir(fInfo, path, false)) {
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
