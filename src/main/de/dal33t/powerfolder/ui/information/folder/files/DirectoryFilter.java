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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria.Type;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.FilterModel;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Class to filter a directory.
 */
public class DirectoryFilter extends FilterModel {

    public static final int FILE_FILTER_MODE_LOCAL_AND_INCOMING = 0;
    public static final int FILE_FILTER_MODE_LOCAL_ONLY = 1;
    public static final int FILE_FILTER_MODE_NEW_ONLY = 2;
    public static final int FILE_FILTER_MODE_DELETED_PREVIOUS = 3;
    public static final int FILE_FILTER_MODE_UNSYNCHRONIZED = 4;

    public static final int SEARCH_MODE_FILE_NAME_DIRECTORY_NAME = 10;
    public static final int SEARCH_MODE_FILE_NAME_ONLY = 11;
    public static final int SEARCH_MODE_MODIFIER = 12;
    public static final int SEARCH_MODE_COMPUTER = 13;

    private Folder folder;
    private DirectoryInfo currentDirectoryInfo;

    private int fileFilterMode;
    private final MyFolderListener folderListener;
    private final AtomicBoolean running;
    private final AtomicBoolean pending;

    private final TransferManager transferManager;

    private final List<DirectoryFilterListener> listeners;

    private final AtomicBoolean refilter = new AtomicBoolean();
    private final ValueModel lastFolderName = new ValueHolder(""); // String

    /**
     * The value model <Integer> of the search we listen to
     */
    private final ValueModel searchModeVM;

    /**
     * Filter of a folder directory.
     * 
     * @param controller
     */
    public DirectoryFilter(Controller controller, ValueModel searchFieldVM,
        ValueModel searchModeVM)
    {
        super(controller, searchFieldVM);
        this.searchModeVM = searchModeVM;
        searchModeVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                queueFilterEvent();
            }
        });
        folderListener = new MyFolderListener();
        running = new AtomicBoolean();
        pending = new AtomicBoolean();
        transferManager = getController().getTransferManager();
        listeners = new CopyOnWriteArrayList<DirectoryFilterListener>();
    }

    /**
     * Add a DirectoryFilterListener to list of listeners.
     * 
     * @param listener
     */
    public void addListener(DirectoryFilterListener listener) {
        listeners.add(listener);
    }

    public void preScheduleFiltering() {
        refilter.set(true);
    }

    /**
     * Remove a DirectoryFilterListener from list of listeners.
     * 
     * @param listener
     */
    public void removeListener(DirectoryFilterListener listener) {
        listeners.remove(listener);
    }

    public ValueModel getSearchModeVM() {
        return searchModeVM;
    }

    public DirectoryInfo getCurrentDirectoryInfo() {
        return currentDirectoryInfo;
    }

    /**
     * Sets the folder to filter the directory for.
     * 
     * @param folder
     * @param currentDirectoryInfo
     */
    public void setFolder(Folder folder, DirectoryInfo currentDirectoryInfo) {
        boolean changedFolder = this.folder == null
            || !this.folder.equals(folder);
        if (changedFolder && this.folder != null) {
            this.folder.removeFolderListener(folderListener);
        }
        this.folder = folder;
        boolean changedDir = this.currentDirectoryInfo != null
            && !this.currentDirectoryInfo.equals(currentDirectoryInfo);
        this.currentDirectoryInfo = currentDirectoryInfo;

        if (changedFolder) {
            folder.addFolderListener(folderListener);
            for (DirectoryFilterListener listener : listeners) {
                listener.invalidate();
            }
        }
        if (changedDir || changedFolder) {
            refilter.set(true);
            queueFilterEvent();
        }
    }

    /**
     * Sets the mode of the filter. See the MODE constants.
     * 
     * @param fileFilterMode
     */
    public void setFileFilterMode(int fileFilterMode) {
        int oldValue = this.fileFilterMode;
        if (oldValue != fileFilterMode) {
            this.fileFilterMode = fileFilterMode;
            refilter.set(true);
            logFine("Set filter mode to " + fileFilterMode);
            queueFilterEvent();
        }
    }

    /**
     * Called from the FilterModel when text search field changed.
     */
    public void scheduleFiltering() {
        logFine("Set search field to " + getSearchFieldVM());
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
     * Runs a filter process. Only one can run at a time, to avoid multiple,
     * similar filtering on the same directory.
     */
    private void fireFilterEvent() {
        logFine("Firing filter even for folder " + folder);
        running.set(true);
        getController().getThreadPool().submit(new Runnable() {
            public void run() {
                while (true) {
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
     * Actually does the filtering. Call via fireFilterEvent() to avoid multiple
     * concurrent runs.
     */
    private void doFilter() {

        if (folder == null || currentDirectoryInfo == null) {
            return;
        }

        Runnable runner = new Runnable() {
            public void run() {
                for (DirectoryFilterListener listener : listeners) {
                    listener.adviseOfFilteringBegin();
                }
            }
        };
        try {
            UIUtil.invokeAndWaitInEDT(runner);
        } catch (InterruptedException e) {
            logWarning(e.toString());
            return;
        }

        Date start = new Date();
        if (isFiner()) {
            logFiner("Starting filter of " + folder.getName());
        }
        // Prepare keywords from text filter
        String textFilter = (String) getSearchFieldVM().getValue();
        String[] keywords = null;
        if (!StringUtils.isBlank(textFilter)) {
            // Match lower case
            keywords = textFilter.split("\\s+");
        }

        final DirectoryFilterResult result = new DirectoryFilterResult();
        final FilteredDirectoryModel filteredDirectoryModel;
        FilteredDirectory filteredDirectory;

        synchronized (refilter) {
            filteredDirectoryModel = new FilteredDirectoryModel(
                folder.getName(), currentDirectoryInfo.getRelativeName());
            filteredDirectory = filteredDirectoryModel.getFilteredDirectory();
        }

        // Recursive filter.
        if (filteredDirectory != null) {
            if (isFlatMode()) {
                filterDirectoryFlat(folder.getDAO(), currentDirectoryInfo,
                    filteredDirectoryModel, keywords, result,
                    currentDirectoryInfo.getRelativeName().length() == 0);
            } else {
                filterDirectory(folder.getDAO(), currentDirectoryInfo,
                    filteredDirectoryModel, keywords, result);
            }
        }

        final boolean changed = !folder.getName().equals(
            lastFolderName.getValue());
        lastFolderName.setValue(folder.getName());

        final boolean defaultFilter = fileFilterMode == FILE_FILTER_MODE_LOCAL_AND_INCOMING
            && !isFlatMode() && StringUtils.isBlank(textFilter);

        runner = new Runnable() {
            public void run() {
                FilteredDirectoryEvent event = new FilteredDirectoryEvent(
                    result.getDeletedCount().get(), result.getIncomingCount()
                        .get(), result.getLocalCount().get(),
                    filteredDirectoryModel, changed, fileFilterMode,
                    defaultFilter);
                for (DirectoryFilterListener listener : listeners) {
                    listener.adviseOfChange(event);
                }
            }
        };
        try {
            UIUtil.invokeAndWaitInEDT(runner);
        } catch (InterruptedException e) {
            logWarning(e.toString());
            return;
        }

        Date end = new Date();
        logFine("Filtered " + folder.getName() + ", original count "
            + result.getOriginalCount().get() + ", filtered count "
            + result.getFilteredCount().get() + " in "
            + (end.getTime() - start.getTime()) + "ms");
    }

    /**
     * Recursive filter call.
     * 
     * @param dao
     * @param directoryInfo
     * @param filteredDirectoryModel
     * @param filteredDirectory
     * @param keywords
     * @param result
     */
    private void filterDirectoryFlat(FileInfoDAO dao,
        DirectoryInfo directoryInfo,
        FilteredDirectoryModel filteredDirectoryModel,
        String[] keywords,
        DirectoryFilterResult result, boolean targetOrDeeper)
    {
        boolean target = currentDirectoryInfo.getRelativeName().equals(
            directoryInfo.getRelativeName());

        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addWriteMembersAndMyself(folder);
        criteria.setPath(directoryInfo);
        criteria.setRecursive(true);
        criteria.setType(Type.FILES_ONLY);
        Collection<FileInfo> infoCollection = dao.findFiles(criteria);
        for (FileInfo fileInfo : infoCollection) {
            if (fileInfo instanceof DirectoryInfo) {
                // DirectoryInfo di = (DirectoryInfo) fileInfo;
                // FilteredDirectory fd = new FilteredDirectory(
                // di.getFilenameOnly(), di.getRelativeName());
                // filterDirectoryFlat(dao, di, filteredDirectoryModel, fd,
                // keywords, result, targetOrDeeper || target);
            } else {
                filterFileInfo(filteredDirectoryModel, keywords, result, fileInfo, targetOrDeeper || target);
            }
        }
    }

    /**
     * Recursive filter call.
     * 
     * @param dao
     * @param directoryInfo
     * @param filteredDirectoryModel
     * @param filteredDirectory
     * @param keywords
     * @param result
     */
    private void filterDirectory(FileInfoDAO dao, DirectoryInfo directoryInfo,
        FilteredDirectoryModel filteredDirectoryModel,
        String[] keywords, DirectoryFilterResult result)
    {

        boolean target = currentDirectoryInfo.getRelativeName().equals(
            directoryInfo.getRelativeName());
        FileInfoCriteria criteria = new FileInfoCriteria();
        criteria.addWriteMembersAndMyself(folder);
        criteria.setPath(directoryInfo);
        Collection<FileInfo> infoCollection = dao.findFiles(criteria);
        for (FileInfo fileInfo : infoCollection) {
            if (fileInfo instanceof DirectoryInfo) {

                // Hidden directory?
                try {
                    if (!PreferencesEntry.SHOW_HIDDEN_FILES.getValueBoolean(getController()) &&
                            Files.isHidden(fileInfo.getDiskFile(getController().getFolderRepository()))) {
                        return;
                    }
                } catch (IOException ioe) {
                    logInfo(ioe.getMessage());
                    return;
                }

                DirectoryInfo di = (DirectoryInfo) fileInfo;
                // Add local files to table list.
                if (target) {
                    if (fileFilterMode != FILE_FILTER_MODE_DELETED_PREVIOUS
                        ^ di.isDeleted())
                    {
                        filteredDirectoryModel.getFileInfos().add(di);
                    }
                }
            } else {
                filterFileInfo(filteredDirectoryModel, keywords, result, fileInfo, target);
            }
        }
    }

    private void filterFileInfo(FilteredDirectoryModel filteredDirectoryModel,
                                String[] keywords, DirectoryFilterResult result,
                                FileInfo fileInfo, boolean addFiles) {
        result.getOriginalCount().incrementAndGet();

        int searchMode = (Integer) searchModeVM.getValue();

        // Hidden file?
        try {
            if (!PreferencesEntry.SHOW_HIDDEN_FILES.getValueBoolean(getController()) &&
                    Files.isHidden(fileInfo.getDiskFile(getController().getFolderRepository()))) {
                return;
            }
        } catch (IOException ioe) {
            logInfo(ioe.getMessage());
            return;
        }

        // Text filter
        boolean showFile = true;
        if (keywords != null) {
            // Check for match
            showFile = matches(fileInfo, keywords, searchMode);
        }

        boolean isDeleted = fileInfo.isDeleted();
        FileInfo newestVersion = null;
        if (fileInfo.getFolder(getController().getFolderRepository()) != null) {
            newestVersion = fileInfo.getNewestNotDeletedVersion(getController()
                .getFolderRepository());
        }
        boolean isIncoming = fileInfo.isDownloading(getController())
            || fileInfo.isExpected(getController().getFolderRepository())
            || newestVersion != null && newestVersion.isNewerThan(fileInfo);

        if (!showFile) {
            return;
        }

        boolean isNew = transferManager.isCompletedDownload(fileInfo);

        switch (fileFilterMode) {
            case FILE_FILTER_MODE_LOCAL_ONLY :
                showFile = !isIncoming && !isDeleted;
                break;
            case FILE_FILTER_MODE_NEW_ONLY :
                showFile = isNew;
                break;
            case FILE_FILTER_MODE_DELETED_PREVIOUS :
                showFile = isDeleted;
                break;
            case FILE_FILTER_MODE_UNSYNCHRONIZED :
                // See if all peers have this file with this version but ignore
                // excluded files because these are not synchronized.
                if (folder.getDiskItemFilter().isExcluded(fileInfo)) {
                    showFile = false;
                } else {
                    showFile = isIncoming || !isSynchronized(fileInfo);
                }
                break;
            case FILE_FILTER_MODE_LOCAL_AND_INCOMING :
            default :
                showFile = !isDeleted;
                break;
        }

        if (showFile) {
            if (addFiles) {
                result.getFilteredCount().incrementAndGet();
                filteredDirectoryModel.getFileInfos().add(fileInfo);
            }
        }

        if (addFiles) {
            if (isDeleted) {
                result.getDeletedCount().incrementAndGet();
            } else if (isIncoming) {
                result.getIncomingCount().incrementAndGet();
            } else {
                result.getLocalCount().incrementAndGet();
            }
        }
    }

    private boolean isSynchronized(FileInfo fileInfo) {
        Folder localFolder = fileInfo.getFolder(getController()
            .getFolderRepository());
        if (localFolder == null) {
            return true;
        }
        boolean synced = true;
        for (Member member : localFolder.getConnectedMembers()) {
            FileInfo memberFileInfo = member.getFile(fileInfo);
            if (memberFileInfo == null
                || memberFileInfo.getVersion() != fileInfo.getVersion())
            {
                synced = false;
                break;
            }
        }
        return synced;
    }

    /**
     * Answers if the file matches the searching keywords. Keywords have to be
     * in lowercase. A file must match all keywords. (AND)
     * 
     * @param fileInfo
     *            the file
     * @param keywords
     *            the keyword array, all lowercase
     * @return the file matches the keywords
     */
    private boolean matches(FileInfo fileInfo, String[] keywords, int searchMode)
    {
        if (keywords == null || keywords.length == 0) {
            return true;
        }

        for (String keyword : keywords) {
            if (keyword.startsWith("-")) {

                // Negative search:
                keyword = keyword.substring(1);
                if (keyword.length() == 0) {

                    // Only a minus sign in the keyword - ignore
                    continue;
                }

                if (matchFileInfo(fileInfo, keyword, searchMode)) {
                    // If negative match we don't want to see this file
                    return true;
                }

                // Does not match the negative keyword
                continue;
            }

            // Normal search

            if (matchFileInfo(fileInfo, keyword, searchMode)) {
                // Match Ok, continue
                continue;
            }

            // Keyword does not match, break
            return false;
        }

        // All keywords matched!
        return true;
    }

    /**
     * keyword should be the member id if searchMode == SEARCH_MODE_COMPUTER.
     * 
     * @param fileInfo
     * @param keyword
     * @param searchMode
     * @return
     */
    private boolean matchFileInfo(FileInfo fileInfo, String keyword,
        int searchMode)
    {
        if (searchMode == SEARCH_MODE_FILE_NAME_DIRECTORY_NAME) {
            String filename = fileInfo.getLowerCaseFilenameOnly().toLowerCase();
            if (filename.contains(keyword.toLowerCase())) {
                return true;
            }
        } else if (searchMode == SEARCH_MODE_FILE_NAME_ONLY) {
            String filename = fileInfo.getFilenameOnly().toLowerCase();
            if (filename.contains(keyword.toLowerCase())) {
                return true;
            }
        } else if (searchMode == SEARCH_MODE_MODIFIER) {
            MemberInfo modifiedBy = fileInfo.getModifiedBy();
            if (modifiedBy != null) {
                Member node = modifiedBy.getNode(getController(), false);
                if (node != null) {
                    if (node.getNick().toLowerCase()
                        .contains(keyword.toLowerCase()))
                    {
                        return true;
                    }
                }
            }
        } else if (searchMode == SEARCH_MODE_COMPUTER) {
            FileInfo remoteFileInfo = folder.getDAO().find(fileInfo, keyword);
            return remoteFileInfo != null;
        }
        return false;
    }

    /**
     * Listener to respond to folder events. Queue filter event if our folder.
     */
    private class MyFolderListener extends FolderAdapter {
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
            if (folderEvent.getMember().hasCompleteFileListFor(
                folderEvent.getFolder().getInfo()))
            {
                checkAndQueue(folderEvent);
            }
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            if (folderEvent.getScanResult().isChangeDetected()) {
                checkAndQueue(folderEvent);
            }
        }

        private void checkAndQueue(FolderEvent folderEvent) {
            if (folderEvent.getFolder().getInfo().equals(folder.getInfo())) {
                queueFilterEvent();
            }
        }
    }
}
