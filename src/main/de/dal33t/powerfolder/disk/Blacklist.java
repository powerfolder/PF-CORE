package de.dal33t.powerfolder.disk;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Black list API draft.
 * <p>
 * Holds the FileInfo that must not be shared or not downloaded. Also filters
 * based on patterns: <TABLE>
 * <TR>
 * <TD valign=top>thumbs.db</TD>
 * <TD> Will filter the file thumbs.db</TD>
 * </TR>
 * <TR>
 * <TD valign=top>*thumbs.db</TD>
 * <TD>Will filter the file thumbs.db in any subdirectory or filename that ends with thumbs.db</TD>
 * </TR>
 * <TR>
 * <TD valign=top>images/*thumbs.db </TD>
 * <TD> Will filter the file thumbs.db in any subdirectory or filename that ends
 * with thumbs.db if its located below the subfolder images.</TD>
 * </TR>
 * </TABLE>
 * <P>
 * TODO Need a factory method or a load method to create this object I think.
 * BlackList blackList = BlackList.create(folder);<BR>
 * OR<BR>
 * BlackList blackList = BlackList.createFrom(File file);
 */
public class Blacklist {

    /** Dummy value to associate with a FileInfo in the Map */
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

    public void removeDoNotAutoDownloadPattern(String strPattern) {
        doNotAutoDownloadStringPatterns.remove(strPattern);
        String converted = convert(strPattern);
        Pattern toRemove = null;
        for (Pattern pattern : doNotAutoDownloadPatterns) {
            if (pattern.pattern().equals(converted)) {
                toRemove = pattern;
            }
        }
        if (toRemove == null) {
            throw new IllegalArgumentException("pattern " + strPattern
                + " not found");
        }
        doNotAutoDownloadPatterns.remove(toRemove);
    }

    public void addDoNotSharePattern(String pattern) {
        doNotShareStringPatterns.add(pattern);
        doNotSharePatterns.add(Pattern.compile(convert(pattern)));
    }

    public void removeDoNotSharePattern(String strPattern) {
        doNotShareStringPatterns.remove(strPattern);
        String converted = convert(strPattern);
        Pattern toRemove = null;
        for (Pattern pattern : doNotSharePatterns) {
            if (pattern.pattern().equals(converted)) {
                toRemove = pattern;
            }
        }
        if (toRemove == null) {
            throw new IllegalArgumentException("pattern " + strPattern
                + " not found");
        }
        doNotSharePatterns.remove(toRemove);
    }

    // Accessors **************************************************************

    public boolean isAllowedToAutoDownload(FileInfo fileInfo) {
        if (doNotAutoDownload.containsKey(fileInfo)) {
            return false;
        }
        for (Pattern pattern : doNotAutoDownloadPatterns) {

            Matcher matcher = pattern.matcher(fileInfo.getName());
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
        for (Pattern pattern : doNotSharePatterns) {
            Matcher matcher = pattern.matcher(fileInfo.getName());
            if (matcher.find()) {
                return false;
            }
        }
        return true;
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
     * Applies the blacklisting settings "DoNotAutodownload" to the list. After
     * calling this method the original list does not longer contain any files
     * that match the "DoNotAutodownload" blacklistings.
     * <p>
     * ATTENTION: This method changes the content the input list, be sure to act
     * on a copy of your original list if you want to leave the original list
     * untouched.
     * 
     * @param fileInfos
     *            the list that gets filtered.
     */
    public void applyDoNotAutoDownload(List<FileInfo> fileInfos) {
        List<FileInfo> toRemove = new ArrayList<FileInfo>(2);
        for (FileInfo fileInfo : fileInfos) {
            if (!isAllowedToAutoDownload(fileInfo)) {
                toRemove.add(fileInfo);
            }
        }
        fileInfos.removeAll(toRemove);
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
     * @param fileInfos
     *            the list that gets filtered.
     */
    public void applyDoNotShare(List<FileInfo> fileInfos) {
        List<FileInfo> toRemove = new ArrayList<FileInfo>(2);
        for (FileInfo fileInfo : fileInfos) {
            if (!isAllowedToShare(fileInfo)) {
                toRemove.add(fileInfo);
            }
        }
        fileInfos.removeAll(toRemove);
    }

    // internal helpers

    /** converts from File wildcard format to regexp format, replaces * with .* */
    private final String convert(String pattern) {
        return pattern.replaceAll("\\*", "\\.\\*");
    }

}
