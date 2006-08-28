/* $Id$
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.disk;

import java.util.List;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Black list API draft.
 * <p>
 * TODO API docs
 * <p>
 * TODO Implementation
 * <p>
 * TODO Does this really need to know Controller or Folder? I don't think so?! ->
 * Lesser dependencies. Avoid extending PFComponent
 */
public class Blacklist {

    public Blacklist() {
    }

    // Mutators of blacklist **************************************************

    public void addToDoNotAutoDownload(FileInfo... fileInfos) {
    }

    public void removeFromDoNotAutoDownload(FileInfo... fileInfos) {
    }

    public void addToDoNotShare(FileInfo... fileInfos) {
    }

    public void removeFromDoNotShare(FileInfo... fileInfos) {
    }

    public void addDoNotAutoDownloadPattern(String pattern) {
    }

    public void removeDoNotAutoDownloadPattern(String... patterns) {
    }

    public void addDoNotSharePattern(String pattern) {
    }

    public void removeDoNotSharePatterns(String... patterns) {
    }

    // Accessors **************************************************************

    public boolean isAllowedToAutoDownload(FileInfo fileInfo) {
        return false;
    }

    public boolean isAllowedToShare(FileInfo fileInfo) {
        return false;
    }

    /**
     * Those files that are marked explicetly not to share. Method maybe not
     * needed see below
     */
    public List<FileInfo> getDoNotShared() {
        return null;
    }

    public List<FileInfo> getDoNotAutodownload() {
        return null;
    }

    public List<String> getDoNotAutoDownloadPatterns() {
        return null;
    }

    public List<String> getDoNotSharePatterns() {
        return null;
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
