/* $Id: DownloadsTableModel.java,v 1.11.2.1 2006/04/29 10:01:17 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.lang.reflect.InvocationTargetException;
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
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.MultiSourceDownload;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.compare.TransferComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A Tablemodel adapter which acts upon a transfermanager.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11.2.1 $
 */
public class DownloadsTableModel extends PFComponent implements TableModel {
    private static final int COLTYPE = 0;
    private static final int COLFILE = 1;
    private static final int COLPROGRESS = 2;
    private static final int COLSIZE = 3;
    private static final int COLFOLDER = 4;
    private static final int COLFROM = 5;

    private static final int UPDATE_TIME = 2000;
    private final Collection<TableModelListener> listeners;
    private final List<Download> downloads;
    private final TransferManagerModel model;
    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;

    // private int activeDownloads;

    public DownloadsTableModel(TransferManagerModel model) {
        super(model.getController());
        Reject.ifNull(model, "Model is null");
        this.model = model;
        listeners = Collections
            .synchronizedCollection(new LinkedList<TableModelListener>());
        downloads = Collections.synchronizedList(new LinkedList<Download>());
        // Add listener
        model.getTransferManager().addListener(new MyTransferManagerListener());
        // initalize
        init(model.getTransferManager());

        MyTimerTask task = new MyTimerTask();
        getController().scheduleAndRepeat(task, UPDATE_TIME);
    }

    /**
     * Initalizes the model upon a transfer manager
     * 
     * @param tm
     */
    private void init(TransferManager tm) {
        for (MultiSourceDownload man: tm.getCompletedDownloadsCollection()) {
            downloads.addAll(man.getSources());
        }
        for (MultiSourceDownload man: tm.getActiveDownloads()) {
            downloads.addAll(man.getSources());
        }
        downloads.addAll(tm.getPendingDownloads());
    }

    // Public exposing ********************************************************

    /**
     * @param rowIndex
     * @return the download at the specified download row. Or null if the
     *         rowIndex exceeds the table rows
     */
    public Download getDownloadAtRow(int rowIndex) {
        synchronized (downloads) {
            if (rowIndex >= 0 && rowIndex < downloads.size()) {
                return downloads.get(rowIndex);
            }
        }
        return null;
    }

    public boolean sortBy(int modelColumnNo) {
        switch (modelColumnNo) {
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
            TransferComparator comparator = new TransferComparator(
                fileInfoComparatorType);

            if (sortAscending) {
                Collections.sort(downloads, comparator);
            } else {
                Collections.sort(downloads, new ReverseComparator(comparator));
            }
            return true;
        }
        return false;
    }

    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    DownloadsTableModel.this);
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
        synchronized (downloads) {
            int size = downloads.size();
            List<Download> tmpDisplayList = new ArrayList<Download>(size);
            for (int i = 0; i < size; i++) {
                tmpDisplayList.add(downloads.get(size - 1 - i));
            }
            downloads.clear();
            downloads.addAll(tmpDisplayList);
        }
        fireModelChanged();
    }

    // Application logic ******************************************************

    // Listener on TransferManager ********************************************

    /**
     * Listener on Transfer manager with new event system
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener extends TransferAdapter {
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
            removeDownload(event.getDownload());
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
                removeDownload(event.getDownload());
            }
        }

        public void downloadCompleted(TransferManagerEvent event) {

            // Update table.
            int index = downloads.indexOf(event.getDownload());
            if (index >= 0) {
                rowsUpdated(index, index);
            } else {
                log().error(
                    "Download not found in model: " + event.getDownload());
                rowsUpdatedAll();
            }

            // Auto-cleanup is now done in TransferManager
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            removeDownload(event.getDownload());
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        private void addOrUpdateDownload(Download dl) {
            boolean added = false;
            int index;
            synchronized (downloads) {
                index = findCompletelyIdenticalDownloadIndex(dl);
                Download alreadyDl = index >= 0 ? downloads.get(index) : null;
                if (alreadyDl == null) {
                    downloads.add(dl);
                    added = true;
                } else {
                    // Update if completely identical found
                    downloads.set(index, dl);
                }
            }

            if (added) {
                rowAdded();
            } else {
                rowsUpdated(index, index);
            }
        }

        /**
         * Searches downloads for a download with identical FileInfo.
         * 
         * @param downloadArg
         *            download to search for identical copy
         * @return index of the download with identical FileInfo, -1 if not
         *         found
         */
        private int findCompletelyIdenticalDownloadIndex(Download downloadArg) {
            synchronized (downloads) {
                for (int i = 0; i < downloads.size(); i++) {
                    Download d = downloads.get(i);
                    if (d.getFile()
                        .isCompletelyIdentical(downloadArg.getFile())
                        && (downloadArg.getPartner() == null 
                            || downloadArg.getPartner().equals(d.getPartner())))
                    {
                        return i;
                    }
                }
            }

            // No match
            return -1;
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    // Model helper methods ***************************************************

    /**
     * Removes one download from the model and fires the tablemode event
     * 
     * @param download
     */
    private void removeDownload(Download download) {
        int index;
        synchronized (downloads) {
            index = downloads.indexOf(download);
            if (index >= 0) {
                downloads.remove(index);
            } else {
                log().error(
                    "Unable to remove download from tablemodel, not found: "
                        + download);
            }
        }
        if (index >= 0) {
            rowRemoved(index);
        }
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

    // Permanent updater ******************************************************

    /**
     * Continously updates the UI
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTimerTask extends TimerTask {

        public void run() {
            Runnable wrapper = new Runnable() {
                public void run() {
                    rowsUpdatedAll();
                }
            };
            try {
                SwingUtilities.invokeAndWait(wrapper);
            } catch (InterruptedException e) {
                log().verbose("Interrupteed while updating downloadstable", e);

            } catch (InvocationTargetException e) {
                log().error("Unable to update downloadstable", e);

            }
            // }
        }
    }

    // TableModel interface ***************************************************

    public int getColumnCount() {
        return 6;
    }

    public int getRowCount() {
        return downloads.size();
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

    public Class<Download> getColumnClass(int columnIndex) {
        return Download.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= downloads.size()) {
            log().error(
                "Illegal rowIndex requested. rowIndex " + rowIndex
                    + ", downloads " + downloads.size());
            return null;
        }
        Download download = downloads.get(rowIndex);
        switch (columnIndex) {
            case COLTYPE :
            case COLFILE :
                return download.getFile();
            case COLPROGRESS :
                return download;
            case COLSIZE :
                return download.getFile().getSize();
            case COLFOLDER :
                return download.getFile().getFolderInfo();
            case COLFROM :
                return download.getPartner();
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

    // Helper method **********************************************************

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
        rowsUpdated(0, downloads.size());
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
}