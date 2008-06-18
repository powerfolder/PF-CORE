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
package de.dal33t.powerfolder.ui.folder;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MP3FileInfo;
import de.dal33t.powerfolder.ui.FilterModel;

/**
 * Based on the settings in this model it filters a filelist.
 * <p>
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class FileFilterModel extends FilterModel {

    public static final int MODE_LOCAL_AND_INCOMING = 0;
    public static final int MODE_LOCAL_ONLY = 1;
    public static final int MODE_INCOMING_ONLY = 2;
    public static final int MODE_NEW_ONLY = 3;
    public static final int MODE_DELETED_PREVIOUS = 4;

    private final List<FileFilterChangeListener> listeners;
    private final List<DiskItem> fileList;
    private int mode;

    /**
     * Constructor.
     *
     * @param controller
     */
    public FileFilterModel(Controller controller) {
        super(controller);
        fileList = new ArrayList<DiskItem>();
        listeners = new ArrayList<FileFilterChangeListener>();
        getController().getTransferManager().addListener(new MyTransferAdapter());
    }

    /**
     * Clear the search value.
     */
    public void reset() {
        getSearchField().setValue("");
    }

    /**
     * Do the actual filtering.
     */
    public void filter() {
        new FilterThread().start();
    }

    private static boolean matches(Directory directory, String[] keywords)
    {
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
                // remove the "-"
                keyword = keyword.substring(1);
                // must be something left
                if (keyword.length() != 0) {
                    // Match for directory name
                    String directoryname = directory.getName().toLowerCase();
                    if (directoryname.indexOf(keyword) >= 0) {
                        // matches nagative search
                        return false;
                    }
                    // does not match negative search
                    continue;
                }
                // only a minus sign in the keyword ignore
                continue;
            }

            // Match for directory name
            String directoryname = directory.getName().toLowerCase();
            if (directoryname.indexOf(keyword) >= 0) {
                // Match by name. Ok, continue
                continue;
            }
            // keyword did not match
            return false;
        }// all keywords matched!
        return true;
    }

    private static boolean matchesMeta(FileInfo file, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        return file instanceof MP3FileInfo &&
                matchesMP3((MP3FileInfo) file, keywords);
    }

    private static boolean matchesMP3(MP3FileInfo file, String[] keywords)
    {
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            // Match for title
            if (file.getTitle() != null
                && file.getTitle().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by title. Ok, continue
                continue;
            }

            // Match for album
            if (file.getAlbum() != null
                && file.getAlbum().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by album. Ok, continue
                continue;
            }
            // Match for artist
            if (file.getArtist() != null
                && file.getArtist().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by artist. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }
        // all keywords match!
        return true;
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
                    String filename = file.getName().toLowerCase();
                    if (filename.indexOf(keyword) >= 0) {
                        // if negative keyword match we don't want to see this
                        // file
                        return false;
                    }
                    // OR for nick
                    String modifierNick = file.getModifiedBy().nick
                        .toLowerCase();
                    if (modifierNick.indexOf(keyword) >= 0) {
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
            String filename = file.getName().toLowerCase();
            if (filename.indexOf(keyword) >= 0) {
                // Match by name. Ok, continue
                continue;
            }

            // OR for nick
            String modifierNick = file.getModifiedBy().nick.toLowerCase();
            if (modifierNick.indexOf(keyword) >= 0) {
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
     * Add a FileFilterChangeListener.
     *
     * @param fileFilterChangeListener
     */
    public void addFileFilterChangeListener(FileFilterChangeListener fileFilterChangeListener) {
       listeners.add(fileFilterChangeListener);
    }

    /**
     * Sets the files to filter.
     *
     * @param fileListArg
     */
    public void setFiles(List<DiskItem> fileListArg) {
        synchronized (fileList) {
            fileList.clear();
            fileList.addAll(fileListArg);
        }
    }

    /**
     * Sets the filter mode.
     *
     * @param mode
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Class to do the actual filtering in a thread.
     */
    private class FilterThread extends Thread {

        public void run() {

            List<DiskItem> resultList = new ArrayList<DiskItem>();

            // Prepare keywords from text filter
            String textFilter = (String) getSearchField().getValue();
            String[] keywords = null;
            if (StringUtils.isBlank(textFilter)) {
                // Set to null to improve performance later in loop
                textFilter = null;
            } else {
                // Match lowercase
                textFilter = textFilter.toLowerCase();
                StringTokenizer nizer = new StringTokenizer(textFilter, " ");
                keywords = new String[nizer.countTokens()];
                int i = 0;
                while (nizer.hasMoreTokens()) {
                    keywords[i++] = nizer.nextToken();
                }
            }

            RecycleBin recycleBin = getController().getRecycleBin();

            int localFiles = 0;
            int incomingFiles = 0;
            int deletedFiles = 0;
            int recycledFiles = 0;

            synchronized (fileList) {
                for (DiskItem diskItem : fileList) {
                    if (diskItem instanceof FileInfo) {
                        FileInfo fInfo = (FileInfo) diskItem;
    
                        // text filter
                        boolean showFile = true;
                        if (textFilter != null) {
                            // Check for match
                            showFile = matches(fInfo, keywords)
                                    || matchesMeta(fInfo, keywords);
                        }

                        if (showFile) {
                            boolean isNew = recentlyDownloaded(fInfo);
                            boolean isDeleted = fInfo.isDeleted();
                            FileInfo newestVersion = null;
                            if (fInfo.getFolder(getController()
                                    .getFolderRepository()) != null) {
                                newestVersion = fInfo.getNewestNotDeletedVersion(
                                        getController().getFolderRepository());
                            }

                            boolean isIncoming = fInfo.isDownloading(getController())
                                    || fInfo.isExpected(getController().getFolderRepository())
                                    || newestVersion != null &&
                                    newestVersion.isNewerThan(fInfo);
                            switch (mode)
                            {
                                case MODE_LOCAL_ONLY:
                                    showFile = !isIncoming && !isDeleted;
                                    break;
                                case MODE_INCOMING_ONLY:
                                    showFile = isIncoming;
                                    break;
                                case MODE_NEW_ONLY:
                                    showFile = isNew;
                                    break;
                                case MODE_DELETED_PREVIOUS:
                                    showFile = isDeleted;
                                    break;
                                case MODE_LOCAL_AND_INCOMING:
                                default:
                                    showFile = !isDeleted;
                                    break;
                            }

                            if (isDeleted) {
                                deletedFiles++;
                                if (recycleBin.isInRecycleBin(fInfo)) {
                                    recycledFiles++;
                                }
                            } else if (isIncoming) {
                                incomingFiles++;
                            } else {
                                localFiles++;
                            }

                            if (showFile) {
                                resultList.add(fInfo);
                            }
                        }

                    } else if (diskItem instanceof Directory) {
                        Directory directory = (Directory) diskItem;

                        // Text filter
                        if (textFilter != null) {
                            // Check for match
                            if (!matches(directory, keywords)) {
                                continue;
                            }
                        }

                        if (mode == MODE_NEW_ONLY) {
                            // See if the directory has new files.
                            boolean newInSub = false;
                            x:
                            for (DownloadManager downloadManager :
                                    getController().getTransferManager()
                                            .getCompletedDownloadsCollection()) {
                                for (FileInfo fileInfo :
                                        directory.getFilesRecursive()) {
                                    if (downloadManager.getFileInfo().equals(fileInfo)) {
                                        newInSub = true;
                                        break x;
                                    }
                                }
                            }
                            if (!newInSub) {
                                continue;
                            }
                        }

                        resultList.add(directory);
                    } else {
                        throw new IllegalStateException(
                                "Unknown type, cannot filter");
                    }

                }
            }

            // Check that the filter text has not changed.
            String finalTextFilter = (String) getSearchField().getValue();
            if (StringUtils.isBlank(finalTextFilter)) {
                finalTextFilter = null;
            }
            if (finalTextFilter == null || finalTextFilter.equals(textFilter)) {
                FileFilterChangedEvent event = new FileFilterChangedEvent(
                        FileFilterModel.this, resultList, localFiles,
                        incomingFiles, deletedFiles, recycledFiles);
                for (FileFilterChangeListener listener : listeners) {
                    listener.filterChanged(event);
                }
            }
        }
    }

    /**
     * Return true if there is a download manager for this file info.
     *
     * @param fInfo
     * @return
     */
    private boolean recentlyDownloaded(FileInfo fInfo) {
        for (DownloadManager downloadManager : getController()
                .getTransferManager().getCompletedDownloadsCollection()) {
            if (downloadManager.getFileInfo().equals(fInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Respond to changes in completed downloads.
     */
    private class MyTransferAdapter extends TransferAdapter {

        public void completedDownloadRemoved(TransferManagerEvent event) {
            filter();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            filter();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

}
