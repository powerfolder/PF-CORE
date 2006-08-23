/* $Id: UploadsTableModel.java,v 1.5.2.1 2006/04/29 10:02:55 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.transfer;

import java.lang.reflect.InvocationTargetException;
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

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * A Tablemodel adapter which acts upon a transfermanager.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5.2.1 $
 */
public class UploadsTableModel extends PFComponent implements TableModel {
    private int UPDATE_TIME = 2000;
    private MyTimerTask task;
    private Collection<TableModelListener> listeners;
    private List<Upload> uploads;
    private int activeUploads;

    public UploadsTableModel(TransferManager transferManager) {
        super(transferManager.getController());
        this.listeners = Collections
            .synchronizedCollection(new LinkedList<TableModelListener>());
        this.uploads = Collections.synchronizedList(new LinkedList<Upload>());
        // Add listener
        transferManager.addListener(new UploadTransferManagerListener());

        task = new MyTimerTask();
        getController().scheduleAndRepeat(task, UPDATE_TIME);
    }

    // Public exposing ********************************************************

    /**
     * Returns the upload at the specified upload row
     * 
     * @param rowIndex
     * @return
     */
    public Upload getUploadAtRow(int rowIndex) {
        synchronized (uploads) {
            if (rowIndex >= uploads.size() || rowIndex == -1) {
                log().error(
                    "Illegal rowIndex requested. rowIndex " + rowIndex
                        + ", uploads " + uploads.size());
                return null;
            }
            return uploads.get(rowIndex);
        }
    }

    // Application logic ******************************************************

    public void clearCompleted() {
        log().warn("Clearing completed uploads");

        List<Upload> ul2remove = new LinkedList<Upload>();
        synchronized (uploads) {
            for (Iterator it = uploads.iterator(); it.hasNext();) {
                Upload upload = (Upload) it.next();
                if (upload.isCompleted()) {
                    ul2remove.add(upload);
                }
            }
        }

        // Remove ul and Fire ui model change
        for (Iterator it = ul2remove.iterator(); it.hasNext();) {
            Upload upload = (Upload) it.next();
            int index = removeUpload(upload);
            rowRemoved(index);
        }
    }

    // Listener on TransferManager ********************************************

    /**
     * Listener on Transfer manager with new event system. TODO: Consolidate
     * removing uploads on abort/complete/broken
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class UploadTransferManagerListener extends TransferAdapter {

        public void uploadRequested(TransferManagerEvent event) {
            int index = -1;
            synchronized (uploads) {
                if (uploads.contains(event.getUpload())) {
                    index = removeUpload(event.getUpload());
                }
                // Move ontop of list
                uploads.add(event.getUpload());
            }
            if (index >= 0) {
                rowsUpdated(0, index);
            }
            rowAdded();
        }

        public void uploadStarted(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                activeUploads++;
                index = removeUpload(event.getUpload());
                // Move ontop of list
                uploads.add(0, event.getUpload());
            }
            rowsUpdated(0, index);
        }

        public void uploadAborted(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
                if (index >= 0) {
                    if (event.getUpload().isStarted()) {
                        activeUploads--;
                    }
                }
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }

        public void uploadBroken(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
                if (index >= 0) {
                    if (event.getUpload().isStarted()) {
                        activeUploads--;
                    }
                }
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }

        public void uploadCompleted(TransferManagerEvent event) {
            int index;
            synchronized (uploads) {
                index = removeUpload(event.getUpload());
                if (index >= 0) {
                    activeUploads--;
                    // uploads.add(activeUploads, event.getUpload());
                    // rowsUpdated(activeUploads, index);
                }
            }
            if (index >= 0) {
                rowRemoved(index);
            }
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    // Model helper methods ***************************************************

    /**
     * Removes one upload from the model an returns its previous index
     * 
     * @param upload
     * @return
     */
    private int removeUpload(Upload upload) {
        int index;
        synchronized (uploads) {
            index = uploads.indexOf(upload);
            if (index >= 0) {
                log().verbose("Remove upload from tablemodel: " + upload);
                uploads.remove(index);
            } else {
                log().error(
                    "Unable to remove upload from tablemodel, not found: "
                        + upload);
            }
        }
        return index;
    }

    // Permanent updater ******************************************************

    /**
     * Updates the ui continously
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.5.2.1 $
     */
    private class MyTimerTask extends TimerTask {

        public void run() {
            if (activeUploads > 0) {
                Runnable wrapper = new Runnable() {
                    public void run() {
                        rowsUpdated(0, activeUploads - 1);
                    }
                };
                try {
                    SwingUtilities.invokeAndWait(wrapper);
                } catch (InterruptedException e) {
                    log().verbose("Interrupteed while updating downloadstable",
                        e);

                } catch (InvocationTargetException e) {
                    log().error("Unable to update downloadstable", e);

                }
            }
        }
    }

    // TableModel interface ***************************************************

    public int getColumnCount() {
        return 5;
    }

    public int getRowCount() {
        return uploads.size();
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0 :
                return Translation.getTranslation("general.file");
            case 1 :
                return Translation.getTranslation("transfers.progress");
            case 2 :
                return Translation.getTranslation("general.size");
            case 3 :
                return Translation.getTranslation("general.folder");
            case 4 :
                return Translation.getTranslation("transfers.to");
        }
        return null;
    }

    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0 :
                return FileInfo.class;
            case 1 :
                return Upload.class;
            case 2 :
                return Long.class;
            case 3 :
                return FolderInfo.class;
            case 4 :
                return Member.class;
        }
        return null;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= uploads.size()) {
            log().error(
                "Illegal rowIndex requested. rowIndex " + rowIndex
                    + ", uploads " + uploads.size());
            return null;
        }
        Upload upload = uploads.get(rowIndex);
        switch (columnIndex) {
            case 0 :
                return upload.getFile();
            case 1 :
                return upload;
            case 2 :
                return new Long(upload.getFile().getSize());
            case 3 :
                return upload.getFile().getFolderInfo();
            case 4 :
                return upload.getPartner();
        }
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
            "Unable to set value in UploadTableModel, not editable");
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

    private synchronized void rowRemoved(int row) {
        TableModelEvent e = new TableModelEvent(this, row, row,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        modelChanged(e);
    }

    private void rowsUpdated(int start, int end) {
        TableModelEvent e = new TableModelEvent(this, start, end);
        modelChanged(e);
    }

    /**
     * Fires an modelevent to all listeners, that model has changed
     */
    private void modelChanged(final TableModelEvent e) {
        // log().verbose("Upload tablemodel changed");
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (Iterator it = listeners.iterator(); it.hasNext();) {
                        TableModelListener listener = (TableModelListener) it
                            .next();
                        listener.tableChanged(e);
                    }
                }
            }
        };

        UIUtil.invokeLaterInEDT(runner);
    }
}