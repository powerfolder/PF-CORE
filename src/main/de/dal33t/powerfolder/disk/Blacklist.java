package de.dal33t.powerfolder.disk;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private Map<FileInfo, Object> doNotAutoDownload;
    private Map<FileInfo, Object> doNotShare;

    private List<String> doNotAutoDownloadStringPatterns;
    private List<String> doNotShareStringPatterns;

    private List<Pattern> doNotAutoDownloadPatterns;
    private List<Pattern> doNotSharePatterns;

    public Blacklist() {
        doNotAutoDownload = Collections
            .synchronizedMap(new HashMap<FileInfo, Object>(2));
        doNotShare = Collections.synchronizedMap(new HashMap<FileInfo, Object>(
            2));
        doNotAutoDownloadStringPatterns = Collections
            .synchronizedList(new ArrayList<String>(2));
        doNotAutoDownloadPatterns = Collections
        .synchronizedList(new ArrayList<Pattern>(2));
        doNotShareStringPatterns = Collections
            .synchronizedList(new ArrayList<String>(2));
        doNotSharePatterns = Collections
        .synchronizedList(new ArrayList<Pattern>(2));
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
        doNotAutoDownloadStringPatterns.add(pattern);
        doNotAutoDownloadPatterns.add(Pattern.compile(convert(pattern)));
    }

    

    public void removeDoNotAutoDownloadPattern(String pattern) {
        doNotAutoDownloadPatterns.remove(pattern);
    }

    public void addDoNotSharePattern(String pattern) {
        doNotShareStringPatterns.add(pattern);
        doNotSharePatterns.add(Pattern.compile(convert("*/thumbs.db")));
    }

    public void removeDoNotSharePattern(String pattern) {
        doNotSharePatterns.remove(pattern);
    }

    // Accessors **************************************************************

    public boolean isAllowedToAutoDownload(FileInfo fileInfo) {
        if (doNotAutoDownload.containsKey(fileInfo)) {
            return false;
        }
        for (Pattern pattern : doNotAutoDownloadPatterns) {
            
            Matcher matcher = 
                pattern.matcher (fileInfo.getName());
            if (matcher.find()) {
                return false;
            }
        }
        return true;
        
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
        return new ArrayList<String>(doNotAutoDownloadStringPatterns);
    }

    public List<String> getDoNotSharePatterns() {
        return new ArrayList<String>(doNotShareStringPatterns);
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
    
    // internal helpers
    
    /** converts from File wildcard format to regexp format replaces * with .* */
    private final String convert(String pattern) {        
        return pattern.replaceAll("\\*", "\\.\\*");
    }
}
