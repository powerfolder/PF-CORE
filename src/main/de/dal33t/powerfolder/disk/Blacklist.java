package de.dal33t.powerfolder.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.PatternSyntaxException;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.PatternMatch;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Logger;

/**
 * Holds the file patterns that must not be shared or not downloaded:
 * <TABLE>
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
 */
public class Blacklist {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(Blacklist.class);

    /**
     * Patterns file name.
     */
    private static final String PATTERNS_FILENAME = "ignore.patterns";

    /**
     * The patterns that may match files so that files won't be downloaded
     * (See class definition for explanation of the patterns)
     */
    private final List<String> patterns = new CopyOnWriteArrayList<String>();

    /**
     * Whether the pattens have been modified since the last save.
     */
    private boolean dirty;

    /**
     * Whether the Blacklist is acting as a whitelist.
     */
    private boolean whitelist;

    /**
     * Loads patterns from file.
     *
     * @param directory
     */
    public void loadPatternsFrom(File directory) {
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
                LOG.error("Problem loading pattern from " + directory, ioe);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        LOG.error("Problem loading pattern from " + directory, e);
                    }
                }
            }
        }
    }

    /**
     * Saves patterns to a directory.
     *
     * @param directory
     */
    public void savePatternsTo(File directory) {
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
            dirty = false;
        } catch (IOException e) {
            LOG.error("Problem saving pattern to " + directory, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Problem saving pattern to " + directory, e);
                }
            }
        }
    }

    // Mutators of blacklist **************************************************

    /**
     * Add a pattern to the list of patterns that will filter FileInfos so will
     * be ignored when matching this pattern
     *
     * NOTE: This should probably be called through the Folder.addPatterns method,
     * so that the folder becomes dirty and persists the change.
     *
     * @param pattern
     */
    public void addPattern(String pattern) {
        Reject.ifBlank(pattern, "Pattern is blank");
        if (patterns.contains(pattern)) {
            // Already contained
            return;
        }
        try {
            patterns.add(pattern.toLowerCase());
        } catch (PatternSyntaxException e) {
            LOG.error("Problem adding pattern " + pattern, e);
        }
        dirty = true;
    }

    /**
     * Remove a pattern from the list of patterns that will filter FileInfos
     * 
     * NOTE: This should probably be called through the Folder.removePattern method,
     * so that the folder becomes dirty and persists the change.
     *
     * @param strPattern
     */
    public void removePattern(String strPattern) {
        patterns.remove(strPattern);
        dirty = true;
    }

    // Accessors **************************************************************

    /**
     * True if patterns have been changed.
     *
     * @return
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Will check if this FileInfo matches a pattern.
     * 
     * @return true if is ignored by a pattern, false if not
     *         or oposite if whitelist.
     */
    public boolean isIgnored(FileInfo fileInfo) {

        for (String pattern : patterns) {
            if (PatternMatch.isMatch(fileInfo.getName(), pattern)) {
                return !whitelist;
            }
        }
        return whitelist;
    }

    /**
     * Will check if this Directory matches a pattern.
     *
     * @return true if is ignored by a pattern, false if not
     */
    public boolean isIgnored(Directory dir) {
        for (String pattern : patterns) {
            if (PatternMatch.isMatch(dir.getName() + "/*", pattern)) {
                return !whitelist;
            }
        }
        return whitelist;
    }

    /**
     * Returns patterns.
     *
     * @return the list of patterns that may match files that should br ignored
     */
    public List<String> getPatterns() {
        return new ArrayList<String>(patterns);
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
    public int applyPatterns(List<FileInfo> fileInfos) {
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

    /**
     * Causes the Blacklist to function as a whitelist.
     * 
     * @param whitelist
     */
    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public boolean isWhitelist() {
        return whitelist;
    }
}
