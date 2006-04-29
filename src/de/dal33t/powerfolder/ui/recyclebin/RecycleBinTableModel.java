package de.dal33t.powerfolder.ui.recyclebin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * Maps the files of the internal RecycleBin to a TableModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinTableModel implements TableModel {
    private Set tableListener = new HashSet();
    private String[] columns = new String[]{
        Translation.getTranslation("general.folder"),
        Translation.getTranslation("general.file"),
        Translation.getTranslation("general.size")};

    
    private List<FileInfo> displayList = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    public RecycleBinTableModel(RecycleBin recycleBin) {   
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

    public Class getColumnClass(int columnIndex) {
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
        if (SwingUtilities.isEventDispatchThread()) {
            runner.run();
        } else {
            SwingUtilities.invokeLater(runner);
        }
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
    }
}
