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
 * <TD>Will filter the file thumbs.db in any subdirectory or filename that ends
 * with thumbs.db</TD>
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
    /** The FileInfos that are specificaly marked to not automaticaly download */
    private Map<FileInfo, Object> doNotAutoDownload;

    /**
     * The FileInfos that are specificaly marked as do not share, other clients
     * will never see this file
     */
    private Map<FileInfo, Object> doNotShare;

    /**
     * The patterns as Strings that may match files so that files wont be
     * downloaded (See class definition for explanation of the patterns)
     */

    private List<String> doNotAutoDownloadStringPatterns;
    /**
     * The patterns as Strings that may match files so that files wont be shared
     * (See class definition for explanation of the patterns)
     */
    private List<String> doNotShareStringPatterns;

    /**
     * The patterns as Pattern objects that may match files so that files wont
     * be downloaded (See class definition for explanation of the patterns)
     */
    private List<Pattern> doNotAutoDownloadPatterns;
    /**
     * The patterns as Pattern objects that may match files so that files wont
     * be shared (See class definition for explanation of the patterns)
     */
    private List<Pattern> doNotSharePatterns;

    /** creates a Blacklist creates all Maps */
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

    /**
     * add 1 or more FileInfos to the list in FileInfos that should not be
     * automacicaly downloaded
     */
    public void addToDoNotAutoDownload(FileInfo... fileInfos) {
        addToDoNotAutoDownload(Arrays.asList(fileInfos));
    }

    /**
     * add a Collection of FileInfos to the list in FileInfos that should not be
     * automacicaly downloaded
     */
    public void addToDoNotAutoDownload(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotAutoDownload.put(fileInfo, DUMMY);
        }
    }

    /**
     * Remove 1 or more FileInfos from the list of files that won't be
     * automaticaly downloaded
     */
    public void removeFromDoNotAutoDownload(FileInfo... fileInfos) {
        removeFromDoNotAutoDownload(Arrays.asList(fileInfos));
    }

    /**
     * Remove a Collection of FileInfos from the list of files that won't be
     * automaticaly downloaded
     */
    public void removeFromDoNotAutoDownload(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotAutoDownload.remove(fileInfo);
        }
    }

    /**
     * add 1 or more FileInfos to the list in FileInfos that should not be
     * shared
     */
    public void addToDoNotShare(FileInfo... fileInfos) {
        addToDoNotShare(Arrays.asList(fileInfos));
    }

    /**
     * add a Collection of FileInfos to the list in FileInfos that should not be
     * shared
     */
    public void addToDoNotShare(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotShare.put(fileInfo, DUMMY);
        }
    }

    /**
     * Remove 1 or more FileInfos from the list of files that won't be shared
     */
    public void removeFromDoNotShare(FileInfo... fileInfos) {
        removeFromDoNotShare(Arrays.asList(fileInfos));
    }

    /**
     * Remove a Collection of FileInfos from the list of files that won't be
     * shared
     */
    public void removeFromDoNotShare(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            doNotShare.remove(fileInfo);
        }
    }

    /**
     * Add a pattern to the list of patterns that will filter FileInfos so they
     * won't be auto downloaded when matching this pattern
     */
    public void addDoNotAutoDownloadPattern(String pattern) {
        doNotAutoDownloadStringPatterns.add(pattern);
        doNotAutoDownloadPatterns.add(Pattern.compile(convert(pattern)));
    }

    /**
     * Remove a pattern from the list of patterns that will filter FileInfos
     */
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

    /**
     * Add a pattern to the list of patterns that will filter FileInfos so they
     * won't be shared when matching this pattern
     */
    public void addDoNotSharePattern(String pattern) {
        doNotShareStringPatterns.add(pattern);
        doNotSharePatterns.add(Pattern.compile(convert(pattern)));
    }

    /**
     * Remove a pattern from the list of patterns that will filter FileInfos
     */
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

    /**
     * Will check if this FileInfo is in the list of files that should not be
     * auto download, or matches a pattern.
     * 
     * @return true if allowed to auto download, false if not
     */
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

    /**
     * Will check if this FileInfo is in the list of files that should not be
     * shared, or matches a pattern.
     * 
     * @return true if allowed to share, false if not
     */
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

    /**
     * @return the list of files that are explicilt marked as not to download
     *          atomaticaly
     */
    public List<FileInfo> getDoNotAutodownload() {
        return new ArrayList<FileInfo>(doNotAutoDownload.keySet());
    }

    /** @return the list of files that are explicilt marked as not share */
    public List<FileInfo> getDoNotShared() {
        return new ArrayList<FileInfo>(doNotShare.keySet());
    }

    /**
     * @return the list of patterns that may match files that should not be
     *          auto downloaded
     */
    public List<String> getDoNotAutoDownloadPatterns() {
        return new ArrayList<String>(doNotAutoDownloadStringPatterns);
    }

    /**
     * @return the list of patterns that may match files that should not be
     *          shared
     */
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
