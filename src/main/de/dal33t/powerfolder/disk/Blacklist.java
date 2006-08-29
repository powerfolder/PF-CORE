package de.dal33t.powerfolder.disk;

import java.util.*;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Black list API draft.
 * <p>
 * TODO API docs
 * <p>
 * TODO Implementation
 * <p>
 * TODO Does this really need to know Controller or Folder? I don't think so?! ->
 * Lesser dependencies. Try to avoid extending PFComponent
 * <P>
 * TODO matching code.
 * <P>
 * TODO Need a factory method or a load method to create this object I think.
 * BlackList blackList = BlackList.create(folder);<BR>
 * OR<BR>
 * BlackList blackList = BlackList.createFrom(File file);
 */
public class Blacklist {

    /** Dummy value to associate with an FileInfo in the Map */
    private static final Object DUMMY = new Object();

    // using MAPs to get fast access. (not using HashSet because that one does
    // excactly the same)
    Map<FileInfo, Object> doNotAutoDownload;
    Map<FileInfo, Object> doNotShare;

    List<String> doNotAutoDownloadPatterns;
    List<String> doNotSharePatterns;

    public Blacklist() {
        doNotAutoDownload = Collections
            .synchronizedMap(new HashMap<FileInfo, Object>(2));
        doNotShare = Collections.synchronizedMap(new HashMap<FileInfo, Object>(
            2));
        doNotAutoDownloadPatterns = Collections
            .synchronizedList(new ArrayList<String>(2));
        doNotSharePatterns = Collections
            .synchronizedList(new ArrayList<String>(2));
    }

    // Mutators of blacklist **************************************************

    public void addToDoNotAutoDownload(FileInfo... fileInfos) {
        addToDoNotAutoDownload(Arrays.asList(fileInfos));
    }

    public void addToDoNotAutoDownload(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotAutoDownload.put(fileInfo, DUMMY);
        }
    }

    public void removeFromDoNotAutoDownload(FileInfo... fileInfos) {
        removeFromDoNotAutoDownload(Arrays.asList(fileInfos));
    }

    public void removeFromDoNotAutoDownload(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotAutoDownload.remove(fileInfo);
        }
    }

    public void addToDoNotShare(FileInfo... fileInfos) {
        addToDoNotShare(Arrays.asList(fileInfos));
    }

    public void addToDoNotShare(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotShare.put(fileInfo, DUMMY);
        }
    }

    public void removeFromDoNotShare(FileInfo... fileInfos) {
        removeFromDoNotShare(Arrays.asList(fileInfos));
    }

    public void removeFromDoNotShare(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotShare.remove(fileInfo);
        }
    }

    public void addDoNotAutoDownloadPattern(String pattern) {
        doNotAutoDownloadPatterns.add(pattern);
    }

    public void removeDoNotAutoDownloadPattern(String pattern) {
        doNotAutoDownloadPatterns.remove(pattern);
    }

    public void addDoNotSharePattern(String pattern) {
        doNotSharePatterns.add(pattern);
    }

    public void removeDoNotSharePattern(String pattern) {
        doNotSharePatterns.remove(pattern);
    }

    // Accessors **************************************************************

    public boolean isAllowedToAutoDownload(FileInfo fileInfo) {
        if (doNotAutoDownload.containsKey(fileInfo)) {
            return false;
        }
        // todo match
        return false;
    }

    public boolean isAllowedToShare(FileInfo fileInfo) {
        if (doNotShare.containsKey(fileInfo)) {
            return false;
        }
        // todo match
        return false;
    }

    public List<FileInfo> getDoNotAutodownload() {
        return new ArrayList<FileInfo>(doNotAutoDownload.keySet());
    }

    public List<FileInfo> getDoNotShared() {
        return new ArrayList<FileInfo>(doNotShare.keySet());
    }
    
    public List<String> getDoNotAutoDownloadPatterns() {
        return new ArrayList<String>(doNotAutoDownloadPatterns);
    }

    public List<String> getDoNotSharePatterns() {
        return new ArrayList<String>(doNotSharePatterns);
    }

    /**
     * Applies the blacklisting settings "DoNotShare" to the list. After calling
     * this method the original list does not longer contain any files that
     * match the "DoNotShare" blacklistings.
     * <p>
     * ATTENTION: This method changes the content the input list, be sure to act
     * on a copy of your original list if you want to leave the original list
     * untouched.
     * 
     * @param files
     *            the list that gets filtered.
     */
    public void applyDoNotShare(List<FileInfo> files) {
    }

    /**
     * Applies the blacklisting settings "DoNotAutodownload" to the list. After
     * calling this method the original list does not longer contain any files
     * that match the "DoNotAutodownload" blacklistings.
     * <p>
     * ATTENTION: This method changes the content the input list, be sure to act
     * on a copy of your original list if you want to leave the original list
     * untouched.
     * 
     * @param files
     *            the list that gets filtered.
     */
    public void applyDoNotAutoDownload(List<FileInfo> files) {
    }
}
