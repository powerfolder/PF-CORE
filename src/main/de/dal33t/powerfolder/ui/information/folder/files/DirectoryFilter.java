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
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.FilterModel;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to filter a directory.
 */
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

    private final TransferManager transferManager;
    private final RecycleBin recycleBin;

    private FilteredDirectoryModel model;
    private long deletedFiles;
    private long recycledFiles;
    private long incomingFiles;
    private long localFiles;

    private final List<DirectoryFilterListener> listeners;

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
        transferManager = getController().getTransferManager();
        recycleBin = getController().getRecycleBin();
        listeners = new CopyOnWriteArrayList<DirectoryFilterListener>();
    }

    /**
     * Add a DirectoryFilterListener to list of listeners.
     *
     * @param listener
     */
    public void addListener(DirectoryFilterListener listener) {
        System.out.println("added " + listener);
        listeners.add(listener);
    }

    /**
     * Remove a DirectoryFilterListener from list of listeners.
     *
     * @param listener
     */
    public void removeListener(DirectoryFilterListener listener) {
        listeners.remove(listener);
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
     * Get count of deleted files.
     *
     * @return
     */
    public long getDeletedFiles() {
        return deletedFiles;
    }

    /**
     * Get count of incoming files.
     *
     * @return
     */
    public long getIncomingFiles() {
        return incomingFiles;
    }

    /**
     * Get count of locl files.
     *
     * @return
     */
    public long getLocalFiles() {
        return localFiles;
    }

    /**
     * Get count of recycled files.
     *
     * @return
     */
    public long getRecycledFiles() {
        return recycledFiles;
    }

    /**
     * Get the filtered file model.
     * 
     * @return
     */
    public FilteredDirectoryModel getModel() {
        return model;
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

        AtomicLong filteredFileCount = new AtomicLong();
        AtomicLong originalFileCount = new AtomicLong();
        AtomicLong deletedCount = new AtomicLong();
        AtomicLong recycledCount = new AtomicLong();
        AtomicLong incomingCount = new AtomicLong();
        AtomicLong localCount = new AtomicLong();
        FilteredDirectoryModel filteredDirectoryModel
                = new FilteredDirectoryModel();

        // Recursive filter.
        filterDirectory(originalDirectory, filteredDirectoryModel, keywords,
                originalFileCount, filteredFileCount, deletedCount,
                recycledCount, incomingCount, localCount);

        model = filteredDirectoryModel;
        deletedFiles = deletedCount.get();
        recycledFiles = recycledCount.get();
        incomingFiles = incomingCount.get();
        localFiles = localCount.get();

        for (DirectoryFilterListener listener : listeners) {
            listener.adviseOfChange();
        }

        logFine("Filtered directory " + originalDirectory.getName() +
                ", original count " + originalFileCount.get() +
                ", filtered count " + filteredFileCount.get());
    }

    /**
     * Recursive filter call.
     *
     * @param directory
     * @param filteredDirectoryModel
     * @param keywords
     * @param originalCount
     * @param filteredCount
     * @param deletedCount
     * @param recycledCount
     * @param incomingCount
     * @param localCount
     */
    private void filterDirectory(Directory directory,
                                 FilteredDirectoryModel filteredDirectoryModel,
                                 String[] keywords,
                                 AtomicLong originalCount,
                                 AtomicLong filteredCount,
                                 AtomicLong deletedCount,
                                 AtomicLong recycledCount,
                                 AtomicLong incomingCount,
                                 AtomicLong localCount) {

        for (FileInfo fileInfo : directory.getFiles()) {

            originalCount.incrementAndGet();

            // Text filter
            boolean showFile = true;
            if (keywords != null) {

                // Check for match
                showFile = matches(fileInfo, keywords);
            }

            boolean isDeleted = fileInfo.isDeleted();
            FileInfo newestVersion = null;
            if (fileInfo.getFolder(getController().getFolderRepository())
                    != null) {
                newestVersion = fileInfo.getNewestNotDeletedVersion(
                        getController().getFolderRepository());
            }
            boolean isIncoming = fileInfo.isDownloading(getController())
                || fileInfo.isExpected(getController().getFolderRepository())
                || newestVersion != null
                && newestVersion.isNewerThan(fileInfo);

            if (showFile) {
                boolean isNew = transferManager.isCompletedDownload(fileInfo);

                switch (filterMode) {
                    case MODE_LOCAL_ONLY :
                        showFile = !isIncoming && !isDeleted;
                        break;
                    case MODE_INCOMING_ONLY :
                        showFile = isIncoming;
                        break;
                    case MODE_NEW_ONLY :
                        showFile = isNew;
                        break;
                    case MODE_DELETED_PREVIOUS :
                        showFile = isDeleted;
                        break;
                    case MODE_LOCAL_AND_INCOMING :
                    default :
                        showFile = !isDeleted;
                        break;
                }

                if (showFile) {
                    filteredCount.incrementAndGet();
                    filteredDirectoryModel.getFiles().add(fileInfo);
                }
            }

            if (isDeleted) {
                deletedCount.incrementAndGet();
                if (recycleBin.isInRecycleBin(fileInfo)) {
                    recycledCount.incrementAndGet();
                }
            } else if (isIncoming) {
                incomingCount.incrementAndGet();
            } else {
                localCount.incrementAndGet();
            }
        }

        for (Directory subDirectory : directory.getSubDirectoriesAsCollection())
        {
            FilteredDirectoryModel subModel = new FilteredDirectoryModel();
            filterDirectory(subDirectory, subModel, keywords, originalCount,
                    filteredCount,  deletedCount, recycledCount,
                    incomingCount, localCount);

            // Only keep if files lower in tree.
            if (!subModel.getFiles().isEmpty()
                    || !subModel.getSubdirectories().isEmpty()) {
                filteredDirectoryModel.getSubdirectories().put(
                        subDirectory.getName(), subModel);
            }
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

                // Negative search:
                keyword = keyword.substring(1);
                if (keyword.length() != 0) {

                    // Match for filename
                    String filename = file.getFilenameOnly().toLowerCase();
                    if (filename.contains(keyword)) {
                        // If negative keyword match we don't want to see this
                        // file
                        return false;
                    }

                    // Does not match the negative keyword
                    continue;
                }

                // Only a minus sign in the keyword ignore
                continue;
            }

            // Normal search

            // Match for filename
            String filename = file.getFilenameOnly().toLowerCase();
            if (filename.contains(keyword)) {

                // Match by name. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }

        // All keywords matched!
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
