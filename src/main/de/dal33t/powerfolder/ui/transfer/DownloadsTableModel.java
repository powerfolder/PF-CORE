/* $Id: DownloadsTableModel.java,v 1.11.2.1 2006/04/29 10:01:17 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A Tablemodel adapter which acts upon a transfermanager.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11.2.1 $
 */
public class DownloadsTableModel extends PFComponent implements TableModel {
    private static final int COLFILE = 0;
    private static final int COLPROGRESS = 1;
    private static final int COLETA = 2;
    private static final int COLSIZE = 3;
    private static final int COLFOLDER = 4;
    private static final int COLFROM = 5;

    private int UPDATE_TIME = 2000;
    private MyTimerTask task;
    private Collection<TableModelListener> listeners;
    private List<Download> downloads;

    // private int activeDownloads;

    public DownloadsTableModel(TransferManager transferManager) {
        super(transferManager.getController());
        this.listeners = Collections
            .synchronizedCollection(new LinkedList<TableModelListener>());
        this.downloads = Collections
            .synchronizedList(new LinkedList<Download>());
        // Add listener
        transferManager.addListener(new MyTransferManagerListener());
        // initalize
        init(transferManager);

        task = new MyTimerTask();
        getController().scheduleAndRepeat(task, UPDATE_TIME);
    }

    /**
     * Initalizes the model upon a transfer manager
     * 
     * @param tm
     */
    private void init(TransferManager tm) {
        Download[] completedDls = tm.getCompletedDownloads();
        downloads.addAll(Arrays.asList(completedDls));

        Download[] pendingDls = tm.getPendingDownloads();
        downloads.addAll(Arrays.asList(pendingDls));
    }

    // Public exposing ********************************************************

    /**
     * Returns the download at the specified download row. Or null if the
     * rowIndex exceeds the table rows
     * 
     * @param rowIndex
     * @return
     */
    public Download getDownloadAtRow(int rowIndex) {
        synchronized (downloads) {
            if (rowIndex >= 0 && rowIndex < downloads.size()) {
                return downloads.get(rowIndex);
            }
        }
        return null;
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
            log().warn("Download queued: " + event.getDownload());
            int index = removeDownload(event.getDownload());
            synchronized (downloads) {
                if (index >= 0) {
                    downloads.add(index, event.getDownload());
                } else {
                    downloads.add(event.getDownload());
                }
            }
            rowsUpdatedAll();
        }

        public void downloadStarted(TransferManagerEvent event) {
            // activeDownloads++;
            // removeDownload(event.getDownload());
            // synchronized (downloads) {
            // // Move ontop of list
            // downloads.add(0, event.getDownload());
            // }
            int index = removeDownload(event.getDownload());
            synchronized (downloads) {
                if (index >= 0) {
                    downloads.add(index, event.getDownload());
                } else {
                    downloads.add(event.getDownload());
                }
            }
            rowsUpdatedAll();
        }

        public void downloadAborted(TransferManagerEvent event) {
            log().warn("Download aborted: " + event.getDownload());
            if (event.getDownload() == null) {
                return;
            }
            if (event.getDownload().isCompleted()) {
                return;
            }
            int index = removeDownload(event.getDownload());
            if (index >= 0) {
                // if (event.getDownload().isStarted()) {
                // activeDownloads--;
                // }
                rowRemoved(index);
            }
        }

        public void downloadBroken(TransferManagerEvent event) {

            if (event.getDownload() == null) {
                return;
            }
            if (event.getDownload().isCompleted()) {
                return;
            }
            if (event.getDownload().isRequestedAutomatic()) {
                log().warn("Download broken, removing: " + event.getDownload());
                int index = removeDownload(event.getDownload());
                if (index >= 0) {
                    // if (event.getDownload().isStarted()) {
                    // activeDownloads--;
                    // }
                    rowRemoved(index);
                }
            }
        }

        public void downloadCompleted(TransferManagerEvent event) {
            // int index = removeDownload(event.getDownload());
            // if (index >= 0) {
            // activeDownloads--;
            // // rowRemoved(index);
            // synchronized (downloads) {
            // downloads.add(activeDownloads, event.getDownload());
            // }
            // rowsUpdated(activeDownloads, index);
            // }
            rowsUpdatedAll();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            removeDownload(event.getDownload());
            // Update whole table
            rowsUpdatedAll();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            addOrUpdateDownload(event.getDownload());
        }

        private void addOrUpdateDownload(Download dl) {
            boolean added = false;
            synchronized (downloads) {
                if (!downloads.contains(dl)) {
                    downloads.add(dl);
                    added = true;
                } else {
                    // Update
                    int index = downloads.indexOf(dl);
                    downloads.set(index, dl);
                }
            }

            if (added) {
                rowAdded();
            }
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }     
    }

    // Model helper methods ***************************************************

    /**
     * Removes one download from the model an returns its previous index
     * 
     * @param download
     * @return
     */
    private int removeDownload(Download download) {
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
        return index;
    }

    // Permanent updater ******************************************************

    /**
     * Continously updates the UI
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTimerTask extends TimerTask {

        public void run() {
            // if (activeDownloads > 0) {
            Runnable wrapper = new Runnable() {
                public void run() {
                    // rowsUpdated(0, activeDownloads - 1);
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
            case COLETA :
                return Translation.getTranslation("transfers.eta");
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
            case COLFILE :
                return download.getFile();
            case COLPROGRESS :
                return download;
            case COLSIZE :
                return new Long(download.getFile().getSize());
            case COLFOLDER :
                return download.getFile().getFolderInfo();
            case COLFROM :
                return download.getPartner();
            case COLETA :
                return new EstimatedTime(download.getCounter()
                    .calculateEstimatedMillisToCompletion(), !download
                    .isCompleted()
                    && download.isStarted());
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
                    for (Iterator<TableModelListener> it = listeners.iterator(); it
                        .hasNext();)
                    {
                        TableModelListener listener = it.next();
                        listener.tableChanged(e);
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }
}