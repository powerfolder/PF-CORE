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
package de.dal33t.powerfolder.ui.information.downloads;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.DownloadManagerComparator;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.compare.TransferComparator;

/**
 * A Tablemodel adapter which acts upon a transfermanager.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11.2.1 $
 */
public class DownloadManagersTableModel extends PFComponent implements
    TableModel, SortedTableModel
{
    public static final int COLTYPE = 0;
    public static final int COLFILE = 1;
    public static final int COLPROGRESS = 2;
    public static final int COLSIZE = 3;
    public static final int COLFOLDER = 4;
    public static final int COLFROM = 5;

    private static final int UPDATE_TIME = 1000;
    private final Collection<TableModelListener> listeners;
    private final List<DownloadManager> downloadManagers;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private volatile boolean dirty;
    private int sortColumn;
    private final TransferManagerModel model;
    private volatile boolean periodicUpdate;

    public DownloadManagersTableModel(TransferManagerModel model) {
        super(model.getController());
        this.model = model;
        Reject.ifNull(model, "Model is null");
        listeners = new LinkedList<TableModelListener>();
        downloadManagers = new ArrayList<DownloadManager>();
        // Add listener
        model.getTransferManager().addListener(new MyTransferManagerListener());

        periodicUpdate = false;
        MyTimerTask task = new MyTimerTask();
        getController().scheduleAndRepeat(task, UPDATE_TIME);
    }

    /**
     * Initalizes the model upon a transfer manager
     *
     * @param tm
     */
    public void initialize() {
        TransferManager tm = model.getTransferManager();
        for (DownloadManager downloadManager : tm
            .getCompletedDownloadsCollection())
        {
            if (!isMetaFolderDownload(downloadManager)) {
                downloadManagers.add(downloadManager);
            }
        }
        for (DownloadManager downloadManager : tm.getActiveDownloads()) {
            if (!isMetaFolderDownload(downloadManager)) {
                downloadManagers.add(downloadManager);
            }
        }
        dirty = true;
        // #1732 FIXME: Does not work addAll(tm.getPendingDownloads());
    }

    /**
     * UI does not care about metaFolder events.
     *
     * @param downloadManager
     * @return
     */
    private static boolean isMetaFolderDownload(DownloadManager downloadManager) {
        return downloadManager.getFileInfo().getFolderInfo().isMetaFolder();
    }

    public boolean isPeriodicUpdate() {
        return periodicUpdate;
    }

    public void setPeriodicUpdate(boolean periodicUpdate) {
        // Transition no update -> update.
        if (!this.periodicUpdate && periodicUpdate) {
            resortAndUpdate();
        }
        this.periodicUpdate = periodicUpdate;
    }

    /**
     * @param rowIndex
     * @return the download at the specified download row. Or null if the
     *         rowIndex exceeds the table rows
     */
    public DownloadManager getDownloadManagerAtRow(int rowIndex) {
        synchronized (downloadManagers) {
            if (rowIndex >= 0 && rowIndex < downloadManagers.size()) {
                return downloadManagers.get(rowIndex);
            }
        }
        return null;
    }

    public DownloadManager[] getDownloadManagersAtRows(int[] ints) {
        DownloadManager[] rows = new DownloadManager[ints.length];
        synchronized (downloadManagers) {
            int x = 0;
            for (int i : ints) {
                if (i < downloadManagers.size()) {
                    rows[x] = downloadManagers.get(i);
                }
                x++;
            }
        }
        return rows;
    }

    public boolean sortBy(int columnIndex) {
        sortColumn = columnIndex;
        switch (columnIndex) {
            case COLTYPE :
                return sortMe(TransferComparator.BY_EXT);
            case COLFILE :
                return sortMe(TransferComparator.BY_FILE_NAME);
            case COLPROGRESS :
                return sortMe(TransferComparator.BY_PROGRESS);
            case COLSIZE :
                return sortMe(TransferComparator.BY_SIZE);
            case COLFOLDER :
                return sortMe(TransferComparator.BY_FOLDER);
            case COLFROM :
                return sortMe(TransferComparator.BY_MEMBER);
        }

        sortColumn = -1;
        return false;
    }

    /**
     * Re-sorts the file list with the new comparator only if comparator differs
     * from old one
     *
     * @param newComparator
     * @return if the table was freshly sorted
     */
    public boolean sortMe(int newComparatorType) {
        int oldComparatorType = fileInfoComparatorType;

        fileInfoComparatorType = newComparatorType;
        if (oldComparatorType != newComparatorType) {
            boolean sorted = sort();
            if (sorted) {
                fireModelChanged();
                return true;
            }
        }
        return false;
    }

    private boolean sort() {
        if (fileInfoComparatorType != -1) {
            DownloadManagerComparator comparator = new DownloadManagerComparator(
                fileInfoComparatorType);

            if (sortAscending) {
                Collections.sort(downloadManagers, comparator);
            } else {
                Collections.sort(downloadManagers, new ReverseComparator(
                    comparator));
            }
            return true;
        }
        return false;
    }

    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    DownloadManagersTableModel.this);
                for (Object aTableListener : listeners) {
                    TableModelListener listener = (TableModelListener) aTableListener;
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        synchronized (downloadManagers) {
            Collections.reverse(downloadManagers);
        }
        fireModelChanged();
        dirty = true;
    }

    public int getColumnCount() {
        return 6;
    }

    public int getRowCount() {
        return downloadManagers.size();
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case COLTYPE :
                return "";
            case COLFILE :
                return Translation.getTranslation("general.file");
            case COLPROGRESS :
                return Translation.getTranslation("transfers.progress");
            case COLSIZE :
                return Translation.getTranslation("general.size");
            case COLFOLDER :
                return Translation.getTranslation("general.folder");
            case COLFROM :
                return Translation.getTranslation("transfers.from");
        }
        return null;
    }

    public Class<DownloadManager> getColumnClass(int columnIndex) {
        return DownloadManager.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= downloadManagers.size()) {
            logSevere("Illegal rowIndex requested. rowIndex " + rowIndex
                + ", downloadManagers " + downloadManagers.size());
            return null;
        }
        DownloadManager downloadManager = downloadManagers.get(rowIndex);
        if (downloadManager == null) {
            logSevere("Illegal rowIndex requested. rowIndex " + rowIndex
                + ", downloadManagers " + downloadManagers.size());
            return null;
        }
        switch (columnIndex) {
            case COLTYPE :
            case COLFILE :
                return downloadManager.getFileInfo();
            case COLPROGRESS :
                return downloadManager;
            case COLSIZE :
                return downloadManager.getFileInfo().getSize();
            case COLFOLDER :
                return downloadManager.getFileInfo().getFolderInfo();
            case COLFROM :
                return downloadManager.getSources();
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in DownloadTableModel, not editable");
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    /**
     * Tells listeners, that a new row at the end of the table has been added
     */
    private void rowAdded() {
        TableModelEvent e = new TableModelEvent(this, getRowCount(),
            getRowCount(), TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        modelChanged(e);
    }

    private void rowRemoved(int row) {
        TableModelEvent e = new TableModelEvent(this, row, row,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        modelChanged(e);
    }

    private void rowsUpdated(int start, int end) {
        TableModelEvent e = new TableModelEvent(this, start, end);
        modelChanged(e);
    }

    /**
     * fire change on whole model
     */
    private void rowsUpdatedAll() {
        rowsUpdated(0, downloadManagers.size());
    }

    /**
     * Fires an modelevent to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        // log().verbose("Download tablemodel changed");
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (TableModelListener listener : listeners) {
                        listener.tableChanged(e);
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    /**
     * Only some types of problem are relevant for display.
     * <p>
     * TODO COPIED to TransferTableCellRenderer
     *
     * @param problem
     *            the transfer problem
     * @return true if it should be displayed.
     */
    private static boolean shouldShowProblem(TransferProblem problem) {
        return TransferProblem.FILE_NOT_FOUND_EXCEPTION.equals(problem)
            || TransferProblem.IO_EXCEPTION.equals(problem)
            || TransferProblem.TEMP_FILE_DELETE.equals(problem)
            || TransferProblem.TEMP_FILE_OPEN.equals(problem)
            || TransferProblem.TEMP_FILE_WRITE.equals(problem)
            || TransferProblem.MD5_ERROR.equals(problem);
    }

    /**
     * Removes one download from the model and fires the tablemodel event
     *
     * @param download
     */
    private void removeDownload(Download download,
        boolean forceRemoveDownloadManager)
    {
        if (download.getFile().getFolderInfo().isMetaFolder()
            || isMetaFolderDownload(download.getDownloadManager()))
        {
            return;
        }
        int index = downloadManagers.indexOf(download.getDownloadManager());
        if (index >= 0) {
            if (!download.getDownloadManager().hasSources()
                || forceRemoveDownloadManager)
            {
                downloadManagers.remove(index);
                rowRemoved(index);
            } else {
                rowsUpdated(index, index);
            }
            dirty = true;
        } else {
            logSevere("Unable to remove download from tablemodel, not found: "
                + download);
        }
    }

    public void setAscending(boolean ascending) {
        this.sortAscending = ascending;
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    /**
     * Listener on Transfer manager with new event system
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener extends TransferManagerAdapter {
        public void downloadRequested(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        public void downloadQueued(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        public void downloadStarted(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        public void downloadAborted(TransferManagerEvent event) {
            if (event.getDownload() == null) {
                return;
            }
            if (event.getDownload().isCompleted()) {
                return;
            }
            removeDownload(event.getDownload(), false);
        }

        public void downloadBroken(TransferManagerEvent event) {
            if (event.getDownload() == null) {
                return;
            }
            if (event.getDownload().isCompleted()) {
                return;
            }
            if (shouldShowProblem(event.getDownload().getTransferProblem())) {
                addOrUpdateDownload(event.getDownload());
            } else if (event.getDownload().isRequestedAutomatic()) {
                removeDownload(event.getDownload(), false);
            }
        }

        public void downloadCompleted(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            removeDownload(event.getDownload(), true);
        }

        public void pendingDownloadEnqueued(TransferManagerEvent event) {
            // #1732 FIXME
            // addOrUpdateDownload(event.getDownload());
        }

        /**
         * Searches downloads for a download with identical FileInfo.
         *
         * @param dl
         *            download to search for identical copy
         * @return index of the download with identical FileInfo, -1 if not
         *         found
         */
        private int findDownloadIndex(Download dl) {
            return downloadManagers.indexOf(dl.getDownloadManager());
            // for (int i = 0; i < downloadManagers.size(); i++) {
            // DownloadManager downloadManager = downloadManagers.get(i);
            // if (downloadManager == null) {
            // // Skip
            // continue;
            // }
            // if (downloadManager.equals(dl.getDownloadManager())) {
            // return i;
            // }
            // // for (Download download : downloadManager.getSources()) {
            // // if (download.getFile().isVersionDateAndSizeIdentical(
            // // dl.getFile())
            // // && (Util.equals(download.getPartner(), dl.getPartner()) ||
            // // download
            // // .isPending()))
            // // {
            // // return i;
            // // }
            // // }
            // }
            //
            // // No match
            // return -1;
        }

        private void addOrUpdateDownload(Download dl) {
            if (isMetaFolderDownload(dl.getDownloadManager())) {
                return;
            }
            int index = findDownloadIndex(dl);
            DownloadManager alreadyDl = index >= 0 ? downloadManagers
                .get(index) : null;
            if (alreadyDl == null) {
                downloadManagers.add(dl.getDownloadManager());
                rowAdded();
            } else {
                // @todo DownloadManager already knows of change???
                rowsUpdated(index, index);
            }
            dirty = true;
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Continously updates the UI
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTimerTask extends TimerTask {

        public void run() {
            if (!periodicUpdate) {
                // Skip
                return;
            }
            resortAndUpdate();
        }

    }

    private void resortAndUpdate() {
        Runnable wrapper = new Runnable() {
            public void run() {
                dirty = dirty
                    || getController().getTransferManager()
                        .countActiveDownloads() > 0;
                if (dirty) {
                    if (fileInfoComparatorType == TransferComparator.BY_PROGRESS)
                    {
                        // Always sort on a PROGRESS change, so that the table
                        // reorders correctly.
                        sort();
                    }
                    rowsUpdatedAll();
                }
                dirty = false;
            }
        };
        SwingUtilities.invokeLater(wrapper);
    }
}