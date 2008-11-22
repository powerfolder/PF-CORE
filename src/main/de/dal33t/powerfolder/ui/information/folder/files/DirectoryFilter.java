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
* $Id: DirectoryFilter.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.FilterModel;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DirectoryFilter extends FilterModel {

    public static final int MODE_LOCAL_AND_INCOMING = 0;
    public static final int MODE_LOCAL_ONLY = 1;
    public static final int MODE_INCOMING_ONLY = 2;
    public static final int MODE_NEW_ONLY = 3;
    public static final int MODE_DELETED_PREVIOUS = 4;

    private Folder folder;
    private int filterMode;
    private final MyFolderListener folderListener;
    private final AtomicBoolean running;
    private final AtomicBoolean pending;
    private final FilteredDirectoryModel model;

    /**
     * Filter of a folder directory.
     *
     * @param controller
     */
    public DirectoryFilter(Controller controller) {
        super(controller);
        folderListener = new MyFolderListener();
        running = new AtomicBoolean();
        pending = new AtomicBoolean();
        model = new FilteredDirectoryModel();
    }

    /**
     * Sets the folder to filter the directory for.
     *
     * @param folder
     */
    public void setFolder(Folder folder) {
        if (this.folder != null) {
            this.folder.removeFolderListener(folderListener);
        }
        this.folder = folder;
        folder.addFolderListener(folderListener);
        queueFilterEvent();
    }

    /**
     * Sets the mode of the filter. See the MODE constants.
     *
     * @param filterMode
     */
    public void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
        logInfo("Set filter mode to " + filterMode);
        queueFilterEvent();
    }

    /**
     * Clears the filter text.
     */
    public void reset() {
        getSearchField().setValue("");
        queueFilterEvent();
    }

    /**
     * Called from the FilterModel when text search field changed.
     */
    public void scheduleFiltering() {
        logInfo("Set search field to " + getSearchField());
        queueFilterEvent();
    }

    /**
     * Fires a filter event, or queues one if currently running.
     */
    private void queueFilterEvent() {
        if (folder == null) {
            return;
        }
        if (running.get()) {
            pending.set(true);
        } else {
            fireFilterEvent();
        }
    }

    /**
     * Runs a filter process.
     * Only one can run at a time, to avoid multiple,
     * similar filtering on the same directory.
     */
    private void fireFilterEvent() {
        logFine("Firing filter even for folder " + folder);
        running.set(true);
        getController().getThreadPool().submit(new Runnable() {
            public void run() {
                while(true) {
                    doFilter();
                    if (!pending.get()) {
                        break;
                    }
                    pending.set(false);
                    // Things changed during filter run. Go again.
                }
                running.set(false);
            }
        });

    }

    /**
     * Actually does the filtering.
     * Call via fireFilterEvent() to avoid multiple concurrent runs.
     */
    private void doFilter() {

        if (folder == null) {
            return;
        }

        // Get original and create
        Directory originalDirectory = folder.getDirectory();

        // Prepare keywords from text filter
        String textFilter = (String) getSearchField().getValue();
        String[] keywords = null;
        if (!StringUtils.isBlank(textFilter)) {

            // Match lower case
            textFilter = textFilter.toLowerCase();
            keywords = textFilter.toLowerCase().split("\\s+");
        }

        AtomicLong originalFileCount = new AtomicLong();
        AtomicLong filteredFileCount = new AtomicLong();

        model.clear();

        filterDirectory(originalDirectory, model, keywords, originalFileCount,
                filteredFileCount);

        logFine("Filtered directory " + originalDirectory.getName() +
                ", original count " + originalFileCount.get() +
                ", filtered count " + filteredFileCount.get());
    }

    private void filterDirectory(Directory directory,
                                 FilteredDirectoryModel model,
                                 String[] keywords, AtomicLong originalFileCount,
                                 AtomicLong filteredFileCount) {
        for (FileInfo fileInfo : directory.getFiles()) {

            originalFileCount.incrementAndGet();

            // text filter
            boolean showFile = true;
            if (keywords != null) {

                // Check for match
                showFile = matches(fileInfo, keywords);
            }

            // @todo filter by mode - too tired now :-)

            if (showFile) {
                filteredFileCount.incrementAndGet();
                model.getFiles().add(fileInfo);
            }
        }

        for (Directory subDirectory : directory.getSubDirectoriesAsCollection()) {
            FilteredDirectoryModel subModel = new FilteredDirectoryModel();
            model.getSubdirectories().put(subDirectory.getName(), subModel);
        }
    }

    /**
     * Answers if the file matches the searching keywords. Keywords have to be
     * in lowercase. A file must match all keywords. (AND)
     *
     * @param file
     *            the file
     * @param keywords
     *            the keyword array, all lowercase
     * @return the file matches the keywords
     */
    private static boolean matches(FileInfo file, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }

        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            if (keyword.startsWith("-")) {
                // negative search:
                keyword = keyword.substring(1);
                if (keyword.length() != 0) {
                    // Match for filename
                    String filename = file.getFilenameOnly().toLowerCase();
                    if (filename.contains(keyword)) {
                        // if negative keyword match we don't want to see this
                        // file
                        return false;
                    }
                    // OR for nick
                    String modifierNick = file.getModifiedBy().nick
                        .toLowerCase();
                    if (modifierNick.contains(keyword)) {
                        // if negative keyword match we don't want to see this
                        // file
                        return false;
                    }
                    // does not match the negative keyword
                    continue;
                }
                // only a minus sign in the keyword ignore
                continue;
            } // normal search

            // Match for filename
            String filename = file.getFilenameOnly().toLowerCase();
            if (filename.contains(keyword)) {
                // Match by name. Ok, continue
                continue;
            }

            // OR for nick
            String modifierNick = file.getModifiedBy().nick.toLowerCase();
            if (modifierNick.contains(keyword)) {
                // Match by name. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }

        // All keywords matched !
        return true;
    }



    /**
     * Listener to respond to folder events. Queue filter event if our folder.
     */
    private class MyFolderListener implements FolderListener {
        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void fileChanged(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        public void filesDeleted(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            checkAndQueue(folderEvent);
        }

        private void checkAndQueue(FolderEvent folderEvent) {
            if (folderEvent.getFolder().getInfo().equals(folder.getInfo())) {
                queueFilterEvent();
            }
        }
    }
}
