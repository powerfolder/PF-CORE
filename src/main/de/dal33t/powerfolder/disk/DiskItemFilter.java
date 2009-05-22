/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.event.PatternChangeListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.CompilingPatternMatch;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

/**
 * Class to hold a number of patterns to filter DiskItems with.
 * The class has two modes:
 * excludeByDefault - DiskItems will be excluded unless they match a pattern (a white list).
 * retainByDefault - DiskItems will be retained unless they match a pattern (a black list).
 */
public class DiskItemFilter {

    private PatternChangeListener listenerSupport;

    private static final Logger log = Logger.getLogger(DiskItemFilter.class.getName());

    /**
     * Patterns file name.
     */
    private static final String PATTERNS_FILENAME = "ignore.patterns";

    /**
     * The mode of the filter.
     * If true, items will be excluded unless they match a pattern.
     * If false, items will be retained unless they match a pattern.
     */
    private boolean excludeByDefault;

    /**
     * The patterns that will be used to match DiskItems with.
     */
    private final List<CompilingPatternMatch> patterns =
            new CopyOnWriteArrayList<CompilingPatternMatch>();

    /**
     * Whether the pattens have been modified since the last save.
     */
    private boolean dirty;

    /**
     * Constructor
     *
     * @param excludeByDefault
     */
    public DiskItemFilter(boolean excludeByDefault) {
        this.excludeByDefault = excludeByDefault;
        listenerSupport = ListenerSupportFactory
                .createListenerSupport(PatternChangeListener.class);
    }

    public void addListener(PatternChangeListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeListener(PatternChangeListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

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
                log.log(Level.SEVERE,
                        "Problem loading pattern from " + directory, ioe);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.log(Level.SEVERE,
                                "Problem loading pattern from " + directory, e);
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
            for (CompilingPatternMatch pattern : patterns) {
                writer.write(pattern.getPatternText() + "\r\n");
            }
            dirty = false;
        } catch (IOException e) {
            log.log(Level.SEVERE,
                    "Problem saving pattern to " + directory, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    log.log(Level.SEVERE,
                            "Problem saving pattern to " + directory, e);
                }
            }
        }
    }

    /**
     * Add a patterns to the list for filtering.
     *
     * NOTE: This should probably be called through the Folder.removePattern method,
     * so that the folder becomes dirty and persists the change.
     *
     * @param pattern
     */
    public void addPattern(String patternText) {
        Reject.ifBlank(patternText, "Pattern is blank");
        CompilingPatternMatch pattern =
                new CompilingPatternMatch(patternText.toLowerCase());
        if (patterns.contains(pattern)) {
            // Already contained
            return;
        }
        try {
            patterns.add(pattern);
            listenerSupport.patternAdded(
                    new PatternChangedEvent(this, patternText, true));
        } catch (PatternSyntaxException e) {
            log.log(Level.SEVERE,
                    "Problem adding pattern " + pattern.getPatternText(), e);
        }
        dirty = true;
    }

    /**
     * Remove a pattern from the list.
     *
     * NOTE: This should probably be called through the Folder.removePattern method,
     * so that the folder becomes dirty and persists the change.
     *
     * @param patternText
     */
    public void removePattern(String patternText) {
        for (CompilingPatternMatch patternMatch : patterns) {
            if (patternMatch.getPatternText().equals(patternText.toLowerCase())) {
                patterns.remove(patternMatch);
            }
        }
        listenerSupport.patternRemoved(
                new PatternChangedEvent(this, patternText, false));
        dirty = true;
    }

    /**
     * Sets the mode of the filter.
     *
     * @param excludeByDefault
     */
    public void setExcludeByDefault(boolean excludeByDefault) {
        this.excludeByDefault = excludeByDefault;
    }

    /**
     * True if patterns have been changed.
     *
     * @return
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Pattern matches diskItem against patterns.
     * Note that Directories have "/*" appended for matching.
     *
     * @param diskItem
     * @return
     */
    private boolean isMatches(DiskItem diskItem) {
        if (diskItem instanceof Directory) {
            Directory dir = (Directory) diskItem;
            String dirName = dir.getName() + "/*";

            for (CompilingPatternMatch pattern : patterns) {
                if (pattern.isMatch(dirName)) {
                    return true;
                }
            }
        } else if (diskItem instanceof FileInfo) {
            String name = ((FileInfo) diskItem).getName();

            for (CompilingPatternMatch pattern : patterns) {
                if (pattern.isMatch(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns patterns.
     * 
     * @return a unmodifable version the list of patterns that may match files
     *         that should be ignored
     */
    public List<String> getPatterns() {
        List<String> result = new ArrayList<String>();
        for (CompilingPatternMatch pattern : patterns) {
            result.add(pattern.getPatternText());
        }
        return result;
    }

    /**
     * Returns the number of items that were filtered out(removed) from the List
     * when the patterns were applied.
     */
    public int filterDirectories(List<Directory> directories) {
        int n = 0;
        for (Iterator<Directory> it = directories.iterator(); it.hasNext();) {
            DiskItem diskItem = it.next();
            if (isExcluded(diskItem)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the number of items that were filtered out (removed) from the
     * List when the patterns were applied.
     */
    public int filterFileInfos(List<FileInfo> fileInfos) {
        int n = 0;
        for (Iterator<FileInfo> it = fileInfos.iterator(); it.hasNext();) {
            DiskItem diskItem = it.next();
            if (isExcluded(diskItem)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /**
     * Returns true if the item is excluded by this filter (is filtered out).
     *
     * @param diskItem
     * @return
     */
    public boolean isExcluded(DiskItem diskItem) {
        return isMatches(diskItem) ^ excludeByDefault;
    }

    /**
     * Returns true if the item is retained by this filter (is not filtered out).
     *
     * @param diskItem
     * @return
     */
    public boolean isRetained(DiskItem diskItem) {
        return !isExcluded(diskItem);
    }
}
