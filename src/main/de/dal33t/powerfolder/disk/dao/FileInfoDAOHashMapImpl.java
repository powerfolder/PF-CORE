package de.dal33t.powerfolder.disk.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * A {@link FileInfoDAO} implementation based on fast, in-memory
 * {@link ConcurrentHashMap}s.
 * 
 * @author sprajc
 */
public class FileInfoDAOHashMapImpl extends Loggable implements FileInfoDAO {
    private ConcurrentMap<String, Domain> domains = new ConcurrentHashMap<String, Domain>();

    public int count(String domain) {
        return getDomain(domain).files.size();
    }

    public void delete(String domain, FileInfo info) {
        getDomain(domain).files.remove(info);
    }

    public void deleteDomain(String domain) {
        domains.remove(domain);
    }

    public FileInfo find(FileInfo info, String domain) {
        return getDomain(domain).files.get(info);
    }

    public Collection<FileInfo> findAll(String domain) {
        return Collections.unmodifiableCollection(getDomain(domain).files
            .values());
    }

    public Map<FileInfo, FileInfo> findAllAsMap(String domain) {
        return Collections.unmodifiableMap(getDomain(domain).files);
    }

    public FileInfo findNewestVersion(FileInfo info, String... domainStrings) {
        FileInfo newestVersion = null;
        for (String domain : domainStrings) {
            Domain d = getDomain(domain);

            // Get remote file
            FileInfo candidateFile = d.files.get(info);
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
        Domain d = getDomain(domain);
        for (FileInfo fileInfo : infos) {
            d.files.put(fileInfo, fileInfo);
        }
    }

    public void store(String domain, Collection<FileInfo> infos) {
        Domain d = getDomain(domain);
        for (FileInfo fileInfo : infos) {
            d.files.put(fileInfo, fileInfo);
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
        private ConcurrentMap<FileInfo, FileInfo> files = new ConcurrentHashMap<FileInfo, FileInfo>();
    }
}
