package de.dal33t.powerfolder.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.PatternSyntaxException;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.Reject;

/**
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
 * <TD valign=top>images/*thumbs.db</TD>
 * <TD> Will filter the file thumbs.db in any subdirectory or filename that ends
 * with thumbs.db if its located below the subfolder images.</TD>
 * </TR>
 * </TABLE>
 * <P>
 * BlackList blackList = BlackList.create(folder);<BR>
 * OR<BR>
 * BlackList blackList = BlackList.createFrom(File file);
 */
public class Blacklist {
    private static final String PATTERNS_FILENAME = "ignore.patterns";

    /**
     * The FileInfos that are specificaly marked to Ignore
     */
    private Map<FileInfo, FileInfo> explicitIgnores;

    /**
     * The patterns that may match files so that files wont be downloaded (See
     * class definition for explanation of the patterns)
     */
    private List<String> ignorePatterns;

    /** creates a Blacklist creates all Maps */
    public Blacklist() {
        explicitIgnores = new ConcurrentHashMap<FileInfo, FileInfo>();
        ignorePatterns = new CopyOnWriteArrayList<String>();
    }

    void loadPatternsFrom(File directory) {
        File file = new File(directory, PATTERNS_FILENAME);
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String pattern;
                while ((pattern = reader.readLine()) != null) {
                    String trimmedPattern = pattern.trim();
                    if (trimmedPattern.length() > 0) {
                        addPattern(trimmedPattern);
                    }
                }
            } catch (IOException ioe) {
                // failed loading
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {

                    }
                }
            }
        }
    }

    void savePatternsTo(File directory) {
        File file = new File(directory, PATTERNS_FILENAME);
        File backup = new File(directory, PATTERNS_FILENAME + ".backup");
        if (file.exists()) {
            if (backup.exists()) {
                backup.delete();
            }
            file.renameTo(backup);
        }
        FileWriter writer = null;
        try {
            file.createNewFile();
            writer = new FileWriter(file);
            for (String pattern : getPatterns()) {
                writer.write(pattern + "\r\n");
            }
        } catch (IOException e) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {

                }
            }
        }

    }

    // Mutators of blacklist **************************************************

    /**
     * add a Collection of FileInfos to the list in FileInfos that should be
     * ignored
     */
    public void addExplicit(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            explicitIgnores.put(fileInfo, fileInfo);
        }
    }

    /**
     * add a Collection of FileInfos to the list in FileInfos that should be
     * ignored
     */
    public void addExplicit(FileInfo... fileInfos) {
        addExplicit(Arrays.asList(fileInfos));
    }

    /**
     * Remove 1 or more FileInfos from the list of files that will be ignored.
     * So it wont be ignored anymore
     */
    public void removeExplicit(FileInfo... fileInfos) {
        removeExplicit(Arrays.asList(fileInfos));
    }

    /**
     * Remove a Collection of FileInfos from the list of files that will be
     * ignored. So it wont ignored anymore
     */
    public void removeExplicit(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            explicitIgnores.remove(fileInfo);
        }
    }

    /**
     * Add a pattern to the list of patterns that will filter FileInfos so will
     * be ignored when matching this pattern
     */
    public void addPattern(String pattern) {
        Reject.ifBlank(pattern, "Pattern is blank");
        if (ignorePatterns.contains(pattern)) {
            // Already contained
            return;
        }
        try {
            ignorePatterns.add(pattern.toLowerCase());
        } catch (PatternSyntaxException e) {
            System.out.println(pattern + " not OK!");
        }
    }

    /**
     * Remove a pattern from the list of patterns that will filter FileInfos
     */
    public void removePattern(String strPattern) {
        ignorePatterns.remove(strPattern);
    }

    // Accessors **************************************************************

    /**
     * Will check if this FileInfo is in the list of files that are ignored, or
     * matches a pattern.
     * 
     * @return true if is ignored, false if not
     */
    public boolean isIgnored(FileInfo fileInfo) {
        if (isExplicitIgnored(fileInfo)) {
            return true;
        }
        if (isIgnoredByPattern(fileInfo)) {
            return true;
        }
        return false;
    }

    /**
     * Will check if this FileInfo matches a pattern.
     * 
     * @return true if is ignored by a pattern, false if not
     */
    public boolean isIgnoredByPattern(FileInfo fileInfo) {

        for (String pattern : ignorePatterns) {
            // TODO Optimize this: Perfom match on non-lowercase filename! This
            // saves memory and CPU.
            if (PatternMatch.isMatch(fileInfo.getLowerCaseName(), pattern)) {
                return true;
            }
            // Matcher matcher = pattern.matcher(fileInfo.getName());
            // if (matcher.find()) {
            // return true;
            // }
        }
        return false;
    }

    /**
     * Will check if this FileInfo is in the list of files that are explicitly
     * ignored.
     * 
     * @return true if is explicitly ignored, false if not
     */
    public boolean isExplicitIgnored(FileInfo fileInfo) {
        return explicitIgnores.containsKey(fileInfo);
    }

    /**
     * are all files is this collection ignored?
     * 
     * @return true if all files are ignored, false if at least one is not
     *         ignored
     */
    public boolean areIgnored(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            if (!isIgnored(fileInfo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * are all files is this collection explicit ignored?
     * 
     * @return true if all files are excplicit ignored, false if at least one is
     *         not explicit ignored
     */
    public boolean areExplicitIgnored(Collection<FileInfo> fileInfos) {
        for (FileInfo fileInfo : fileInfos) {
            if (!isExplicitIgnored(fileInfo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the list of files that are explicit marked as not to download
     *         atomaticaly
     */
    public List<FileInfo> getExplicitIgnored() {
        return new ArrayList<FileInfo>(explicitIgnores.keySet());
    }

    /**
     * @return the list of patterns that may match files that should br ignored
     */
    public List<String> getPatterns() {
        return new ArrayList<String>(ignorePatterns);
    }

    /**
     * Applies the blacklisting settings to the list. After calling this method
     * the original list does not longer contain any files that match the ignore
     * blacklistings.
     * <p>
     * ATTENTION: This method changes the content the input list, be sure to act
     * on a copy of your original list if you want to leave the original list
     * untouched.
     * 
     * @param fileInfos
     *            the list that gets filtered.
     * @return the number of removed files from the list
     */
    public int applyIgnore(List<FileInfo> fileInfos) {
        int n = 0;
        for (Iterator<FileInfo> it = fileInfos.iterator(); it.hasNext();) {
            FileInfo fInfo = it.next();
            if (isIgnored(fInfo)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

}
