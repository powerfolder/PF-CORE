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
 * $Id: DiskItemFilter.java 21255 2013-03-19 18:23:02Z sprajc $
 */
package de.dal33t.powerfolder.disk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

import de.dal33t.powerfolder.event.DiskItemFilterListener;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.pattern.Pattern;
import de.dal33t.powerfolder.util.pattern.PatternFactory;

/**
 * Class to hold a number of patterns to filter DiskItems with. The class has
 * two modes: excludeByDefault - DiskItems will be excluded unless they match a
 * pattern (a white list). retainByDefault - DiskItems will be retained unless
 * they match a pattern (a black list).
 */
public class DiskItemFilter {

    private DiskItemFilterListener listenerSupport;

    private static final Logger log = Logger.getLogger(DiskItemFilter.class
        .getName());

    /**
     * Patterns file name.
     */
    public static final String PATTERNS_FILENAME = "ignore.patterns";

    /**
     * The patterns that will be used to match DiskItems with.
     */
    private final Set<Pattern> patterns = new CopyOnWriteArraySet<Pattern>();

    /**
     * Whether the patterns have been modified since the last save.
     */
    private boolean dirty;

    /**
     * Constructor
     */
    public DiskItemFilter() {
        listenerSupport = ListenerSupportFactory
            .createListenerSupport(DiskItemFilterListener.class);
    }

    public void addListener(DiskItemFilterListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void addWeakListener(DiskItemFilterListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener, true);
    }

    public void removeListener(DiskItemFilterListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    public void removeAllListener() {
        ListenerSupportFactory.removeAllListeners(listenerSupport);
    }

    /**
     * Loads patterns from file. Removes all previous patterns. Only if there is
     * actually a change to the patterns.
     * 
     * @param file
     *            file to read patterns from
     * @param markDirtyIfChanged
     *            true if this disk item filter should be marked dirty after
     *            loading. Usually done on re-loading load from disk of the
     *            patterns.
     */
    public void loadPatternsFrom(Path file, boolean markDirtyIfChanged) {
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
                Set<Pattern> tempPatterns = new HashSet<Pattern>();
                String readPattern;
                while ((readPattern = reader.readLine()) != null) {
                    String trimmedPattern = readPattern.trim();
                    if (trimmedPattern.length() > 0) {
                        tempPatterns.add(createPattern(trimmedPattern));
                    }
                }

                // Did anything change?
                boolean allTheSame = true;
                if (tempPatterns.size() == patterns.size()) {
                    for (Pattern tempPattern : tempPatterns) {
                        if (!patterns.contains(tempPattern)) {
                            allTheSame = false;
                            break;
                        }
                    }
                } else {
                    allTheSame = false;
                }

                if (allTheSame) {
                    // No change at all.
                    log.fine("Received a pattern file identical to own, so ignoring it.");
                    return;
                }

                // Something changed. Redo the patterns.
                log.fine("Received a pattern file different to own, so loading it.");
                for (Pattern oldPattern : patterns) {
                    patterns.remove(oldPattern);
                    listenerSupport.patternRemoved(new PatternChangedEvent(
                        this, oldPattern.getPatternText(), false));
                }
                for (Pattern newPattern : tempPatterns) {
                    patterns.add(newPattern);
                    listenerSupport.patternAdded(new PatternChangedEvent(this,
                        newPattern.getPatternText(), true));
                }
                dirty = markDirtyIfChanged;
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Problem loading pattern from " + file
                    + ". " + ioe);
            }
        }
    }

    /**
     * Saves patterns to a file.
     * 
     * @param file
     */
    public void savePatternsTo(Path file, boolean createBackup) {
        try {
            if (createBackup) {
                Path backup = file.getParent().resolve(file.getFileName().toString()
                    + ".backup");
                if (Files.exists(file)) {
                    if (Files.exists(backup)) {
                        Files.delete(backup);
                    }
                    Files.move(file, backup);
                }
            }
        } catch (IOException ioe) {
            log.warning(ioe.getMessage());
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {
            for (Pattern pattern : patterns) {
                writer.write(pattern.getPatternText());
                writer.newLine();
            }
            dirty = false;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Problem saving pattern to " + file + ". "
                + e);
            log.log(Level.FINER, e.toString(), e);
        }
    }

    private static Pattern createPattern(String patternText) {
        Reject.ifBlank(patternText, "Pattern is blank");
        String raw = patternText.replaceAll("\\\\", "/").toLowerCase();
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        return PatternFactory.createPattern(raw);
    }

    /**
     * Add a patterns to the list for filtering.
     * 
     * @param patternText
     */
    void addPattern(String patternText) {
        addPattern0(createPattern(patternText));
    }

    /**
     * Add a patterns to the list for filtering.
     * 
     * @param pattern
     */
    private void addPattern0(Pattern pattern) {
        if (patterns.contains(pattern)) {
            // Already contained
            return;
        }
        try {
            patterns.add(pattern);
            dirty = true;
            listenerSupport.patternAdded(new PatternChangedEvent(this, pattern
                .getPatternText(), true));
        } catch (PatternSyntaxException e) {
            log.log(Level.SEVERE,
                "Problem adding pattern " + pattern.getPatternText(), e);
        }
    }

    void removeAllPatterns() {
        for (Pattern pattern : patterns) {
            patterns.remove(pattern);
            dirty = true;
            listenerSupport.patternRemoved(new PatternChangedEvent(this,
                pattern.getPatternText(), false));
        }
    }

    /**
     * Remove a pattern from the list.
     * 
     * @param patternText
     */
    void removePattern(String patternText) {
        Pattern targetPattern = createPattern(patternText);
        for (Pattern pattern : patterns) {
            if (pattern.equals(targetPattern)) {
                patterns.remove(pattern);
                dirty = true;
                listenerSupport.patternRemoved(new PatternChangedEvent(this,
                    pattern.getPatternText(), false));
            }
        }
    }

    /**
     * @return True if patterns have been changed.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Pattern matches diskItem against patterns. Note that Directories have
     * "/*" appended for matching.
     * 
     * @param diskItem
     * @return
     */
    private boolean isMatches(DiskItem diskItem) {
        if (diskItem instanceof DirectoryInfo) {
            DirectoryInfo directoryInfo = (DirectoryInfo) diskItem;
            String dirName = directoryInfo.getRelativeName() + "/*";

            for (Pattern pattern : patterns) {
                if (pattern.isMatch(dirName)) {
                    return true;
                }
            }
        } else if (diskItem instanceof FileInfo) {
            String name = diskItem.getRelativeName();

            for (Pattern pattern : patterns) {
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
        for (Pattern pattern : patterns) {
            result.add(pattern.getPatternText());
        }
        return result;
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
     * @return true if filtered out / excluded
     */
    public boolean isExcluded(DiskItem diskItem) {
        return isMatches(diskItem);
    }

    /**
     * Returns true if the relative name is excluded by this filter (is filtered
     * out).
     * 
     * @param relativeName
     * @return true if filtered out / excluded
     */
    public boolean isExcluded(String relativeName) {
        if (patterns.isEmpty()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.isMatch(relativeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the item is retained by this filter (is not filtered
     * out).
     * 
     * @param diskItem
     * @return
     */
    public boolean isRetained(DiskItem diskItem) {
        return !isExcluded(diskItem);
    }
}
