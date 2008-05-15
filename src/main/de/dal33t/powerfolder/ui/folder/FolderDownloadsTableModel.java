package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Maps downloads for a folder to a tablemodel.
 *
 * @author <A HREF="mailto:hglasgow@powerfolder.com">Harry Glasgow</A>
 * @version $Revision: 3.1 $
 */
public class FolderDownloadsTableModel extends PFComponent implements TableModel {

    private Set<TableModelListener> tableListener = new HashSet<TableModelListener>();
    private Folder folder;

    private String[] columns = new String[]{
            Translation.getTranslation("filelist.name"),
            Translation.getTranslation("general.size"),
            Translation.getTranslation("filelist.modifiedby"),
            Translation.getTranslation("filelist.date")};

    private final List<FileInfo> displayList = Collections.synchronizedList(new ArrayList<FileInfo>());

    public FolderDownloadsTableModel(TransferManagerModel model) {
        super(model.getController());
        Reject.ifNull(model, "Model is null");
        model.getTransferManager().addListener(new MyTransferManagerListener());
    }

    public Folder getFolder() {
        return folder;
    }

    /**
     * Set the Directory that should be visible in the Table
     *
     * @param directory     the Directory to use
     * @param clear         immediately clear the display list (eg use if new Folder is
     *                      set)
     * @param oldSelections The selections that need to be restored if they are, after
     *                      filtering this folder, still present.
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
        init();
    }

    private void init() {
        displayList.clear();
        if (folder != null) {
            List<DownloadManager> downloadsCollection = getController().getTransferManager().getCompletedDownloadsCollection();
            for (DownloadManager downloadManager : downloadsCollection) {
                if (downloadManager.getFileInfo().getFolderInfo().equals(folder.getInfo())) {
                    displayList.add(downloadManager.getFileInfo());
                }
            }
        }
    }

    public Class getColumnClass(int columnIndex) {
        return FileInfo.class;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        synchronized (displayList) {
            return displayList.size();
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (displayList) {
            if (rowIndex >= displayList.size() || rowIndex < 0) {
                log().error(
                        "Illegal access. want to get row " + rowIndex + ", have "
                                + displayList.size());
                return null;
            }
            return displayList.get(rowIndex);
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new IllegalStateException("editing not allowed");
    }

    public void addTableModelListener(TableModelListener l) {
        tableListener.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        tableListener.remove(l);
    }

    /**
     * Fires event, that table has changed
     */
    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                        FolderDownloadsTableModel.this);
                for (Object aTableListener : tableListener) {
                    TableModelListener listener =
                            (TableModelListener) aTableListener;
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    private class MyTransferManagerListener extends TransferAdapter {

        public void downloadCompleted(TransferManagerEvent event) {
            FileInfo fileInfo = event.getDownload().getFile();
            if (fileInfo.getFolderInfo().equals(folder.getInfo())) {
                if (!displayList.contains(fileInfo)) {
                    displayList.add(fileInfo);
                    fireModelChanged();
                }
            }
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            FileInfo fileInfo = event.getDownload().getFile();
            if (fileInfo.getFolderInfo().equals(folder.getInfo())) {
                if (displayList.contains(fileInfo)) {
                    displayList.remove(fileInfo);
                    fireModelChanged();
                }
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}