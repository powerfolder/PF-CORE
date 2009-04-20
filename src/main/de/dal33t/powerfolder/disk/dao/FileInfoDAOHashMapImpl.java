package de.dal33t.powerfolder.disk.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.light.FileInfo;
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
    private ConcurrentMap<String, Domain> domains = new ConcurrentHashMap<String, Domain>();

    public FileInfoDAOHashMapImpl() {
        super();
        ignoreFileNameCase = OSUtil.isWindowsSystem();
        //ignoreFileNameCase = false;
    }

    public int count(String domain) {
        return getDomain(domain).files.size();
    }

    public void delete(String domain, FileInfo info) {
        if (ignoreFileNameCase) {
            getDomain(domain).files.remove(info.getLowerCaseName());
        } else {
            getDomain(domain).files.remove(info.getName());
        }
    }

    public void deleteDomain(String domain) {
        domains.remove(domain);
    }

    public FileInfo find(FileInfo info, String domain) {
        if (ignoreFileNameCase) {
            return getDomain(domain).files.get(info.getLowerCaseName());
        } else {
            return getDomain(domain).files.get(info.getName());
        }
    }

    public Collection<FileInfo> findAll(String domain) {
        return Collections.unmodifiableCollection(getDomain(domain).files
            .values());
    }

    public FileInfo findNewestVersion(FileInfo info, String... domainStrings) {
        FileInfo newestVersion = null;
        for (String domain : domainStrings) {
            Domain d = getDomain(domain);

            // Get remote file
            FileInfo candidateFile;
            if (ignoreFileNameCase) {
                candidateFile = d.files.get(info.getLowerCaseName());
            } else {
                candidateFile = d.files.get(info.getName());
            }
            if (candidateFile == null) {
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
                d.files.put(fileInfo.getLowerCaseName(), fileInfo);
            } else {
                d.files.put(fileInfo.getName(), fileInfo);
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
        private ConcurrentMap<String, FileInfo> files = new ConcurrentHashMap<String, FileInfo>();
    }
}
