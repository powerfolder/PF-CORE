package de.dal33t.powerfolder.ui.transfer;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.TransferProblemBean;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A TableModel implementation which acts upon a transfermanager transfer problems.
 *
 * @author <a href="mailto:harryglasgow@gmail.com">Harry Glasgow</a>
 * @version $Revision: 2.0 $
 */
public class TransferProblemsTableModel extends PFComponent implements TableModel {

    /**
     * Two second refresh period.
     */
    private static final int UPDATE_TIME = 2000;

    /**
     * Table model listeners.
     */
    private final CopyOnWriteArrayList<TableModelListener> listeners;

    /**
     * Details of the transfer problems.
     */
    private final List<TransferProblemBean> transferProblemBeans;

    /**
     * Constructor
     *
     * @param transferManager
     */
    public TransferProblemsTableModel(TransferManager transferManager) {
        super(transferManager.getController());
        listeners = new CopyOnWriteArrayList<TableModelListener>();
        transferProblemBeans = Collections.synchronizedList(new LinkedList<TransferProblemBean>());

        // Add listener to deal with problem events from the transfer manager.
        transferManager.addListener(new MyTransferManagerListener());

        // Timer task to refresh the table periodically.
        MyTimerTask task = new MyTimerTask();
        getController().scheduleAndRepeat(task, UPDATE_TIME);
    }

    /**
     * Listener on Transfer manager with new event system
     */
    private class MyTransferManagerListener extends TransferAdapter {

        /**
         * Adds transfer problem bean to the table for a TransferManagerEvent
         *
         * @param event the event containing the problem info
         */
        public void transferProblem(TransferManagerEvent event) {
            try {
                TransferProblemBean transferProblemBean = new TransferProblemBean(event.getFile(),
                        new Date(),
                        Translation.getTranslation(event.getTransferProblem().getTranslationId(),
                                event.getFile().getFilenameOnly(),
                                event.getProblemInformation()));
                addOrUpdateDownload(transferProblemBean);
            } catch (Exception e) {
                log().error(e);
            }
        }

        /**
         * Handle clear command from transfer manager,
         * when the problems are to be discarded.
         */
        public void clearTransferProblems() {
            int size = transferProblemBeans.size();
            for (int i = 0; i < size; i++) {
                rowRemoved(i);
            }
            transferProblemBeans.clear();
        }

        /**
         * Adds or updates a TransferProblemBean to the table.
         *
         * @param bean the TransferProblemBean to add / update
         */
        private void addOrUpdateDownload(TransferProblemBean bean) {
            if (transferProblemBeans.contains(bean)) {
                // Update
                int index = -1;
                index = transferProblemBeans.indexOf(bean);
                transferProblemBeans.set(index, bean);
                rowsUpdated(index, index);
            } else {
                // Add
                transferProblemBeans.add(bean);
                rowAdded();
            }
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * Continously updates the UI
     */
    private class MyTimerTask extends TimerTask {

        public void run() {
            Runnable wrapper = new Runnable() {
                public void run() {
                    rowsUpdated(0, transferProblemBeans.size());
                }
            };

            try {
                SwingUtilities.invokeAndWait(wrapper);
            } catch (InterruptedException e) {
                log().verbose("Interrupted while updating downloadstable", e);
            } catch (InvocationTargetException e) {
                log().error("Unable to update downloadstable", e);
            }
        }
    }

    /**
     * Gets the TransferProblemBean at a specific index
     *
     * @param index index to get TransferProblemBean for
     * @return the TransferProblemBean at the index
     */
    public TransferProblemBean getTransferProblemItemAt(int index) {
        return transferProblemBeans.get(index);
    }

    private void rowAdded() {
        TableModelEvent e = new TableModelEvent(this, getRowCount(), getRowCount(),
                TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
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
     * Fires a modelevent to all listeners, that model has changed.
     *
     * @param e the TableModelEvent
     */
    private void modelChanged(final TableModelEvent e) {
        Runnable runner = new Runnable() {
            public void run() {
                for (TableModelListener listener : listeners) {
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    // TableModel interface ***************************************************

    /**
     * Gets the transferProblemBean for the row
     *
     * @param rowIndex    row to select the bean for
     * @param columnIndex ignored; always same object
     * @return a transferProblemBean
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        return transferProblemBeans.get(rowIndex);
    }

    public int getColumnCount() {
        return 5;
    }

    public int getRowCount() {
        return transferProblemBeans.size();
    }

    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "";
            case 1:
                return Translation.getTranslation("filelist.name");
            case 2:
                return Translation.getTranslation("general.folder");
            case 3:
                return Translation.getTranslation("filelist.date");
            case 4:
                return Translation.getTranslation("general.transfer.problem");
        }
        return null;
    }

    public Class<Download> getColumnClass(int columnIndex) {
        return Download.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException(
                "Unable to set value in DownloadTableModel, not editable");
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.addIfAbsent(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
}
