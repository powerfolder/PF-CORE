package de.dal33t.powerfolder.ui.recyclebin;

import java.util.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps the files of the internal RecycleBin to a TableModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinTableModel extends PFComponent implements TableModel {
    private Set<TableModelListener> tableListener = new HashSet<TableModelListener>();
    private String[] columns = new String[]{
        Translation.getTranslation("general.folder"),
        Translation.getTranslation("general.file"),
        Translation.getTranslation("general.size"),
        Translation.getTranslation("fileinfo.modifieddate")
        };

    
    private List<FileInfo> displayList = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    public RecycleBinTableModel(Controller controller, RecycleBin recycleBin) {
        super(controller);
        //listen to changes of the RecycleBin:
        recycleBin.addRecycleBinListener(new MyRecycleBinListener());
        displayList.addAll(recycleBin.getAllRecycledFiles());
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        synchronized (displayList) {
            return displayList.size();
        }
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (displayList) {
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

    public Class<FileInfo> getColumnClass(int columnIndex) {
        return FileInfo.class;
    }

    /**
     * Fires event, that table has changed
     */
    private void fireModelChanged() {
        Runnable runner = new Runnable() {
            public void run() {
                TableModelEvent e = new TableModelEvent(
                    RecycleBinTableModel.this);
                for (Iterator it = tableListener.iterator(); it.hasNext();) {
                    TableModelListener listener = (TableModelListener) it
                        .next();
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    private class MyRecycleBinListener implements RecycleBinListener {

        public void fileAdded(RecycleBinEvent e) {
            displayList.add(e.getFile());
            fireModelChanged();
        }

        public void fileRemoved(RecycleBinEvent e) {
            displayList.remove(e.getFile());
            fireModelChanged();
        }
        
        public void fileUpdated(RecycleBinEvent e) {
            if (displayList.contains(e.getFile())) {
                displayList.remove(e.getFile());    
            } else {
                log().error("file not there: " + e.getFile());
            }
            
            displayList.add(e.getFile());
            fireModelChanged();
        }
    }
}
