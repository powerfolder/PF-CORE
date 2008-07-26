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
package de.dal33t.powerfolder.ui.recyclebin;

import java.util.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.event.RecycleBinEvent;
import de.dal33t.powerfolder.event.RecycleBinListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.ReverseComparator;
import de.dal33t.powerfolder.util.compare.RecycleBinComparator;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Maps the files of the internal RecycleBin to a TableModel.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    private static final int COLFOLDER = 0;
    private static final int COLTYPE = 1;
    private static final int COLFILE = 2;
    private static final int COLSIZE = 3;
    private static final int COLMODIFIED = 4;

    private int fileInfoComparatorType = -1;
    private boolean sortAscending = true;
    private int sortColumn;
    private RecycleBin recycleBin;

    private Set<TableModelListener> tableListener = new HashSet<TableModelListener>();
    private String[] columns = new String[]{
            Translation.getTranslation("general.folder"),
            "",
            Translation.getTranslation("general.file"),
            Translation.getTranslation("general.size"),
            Translation.getTranslation("fileinfo.modifieddate")};

    private List<FileInfo> displayList = Collections
        .synchronizedList(new ArrayList<FileInfo>());

    public RecycleBinTableModel(Controller controller, RecycleBin recycleBin) {
        super(controller);
        // listen to changes of the RecycleBin:
        this.recycleBin = recycleBin;
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
                for (TableModelListener listener : tableListener) {
                    listener.tableChanged(e);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public boolean sortBy(int modelColumnNo) {
        sortColumn = modelColumnNo;
        switch (modelColumnNo) {
            case COLFOLDER :
                return sortMe(RecycleBinComparator.BY_FOLDER);
            case COLTYPE :
                return sortMe(RecycleBinComparator.BY_FILETYPE);
            case COLFILE :
                return sortMe(RecycleBinComparator.BY_NAME);
            case COLSIZE :
                return sortMe(RecycleBinComparator.BY_SIZE);
            case COLMODIFIED :
                return sortMe(RecycleBinComparator.BY_MODIFIED_DATE);
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
            RecycleBinComparator comparator = new RecycleBinComparator(
                fileInfoComparatorType, recycleBin);

            if (sortAscending) {
                Collections.sort(displayList, comparator);
            } else {
                Collections.sort(displayList, new ReverseComparator(comparator));
            }
            return true;
        }
        return false;
    }

    public void reverseList() {
        sortAscending = !sortAscending;
        List<FileInfo> tmpDisplayList =
                Collections.synchronizedList(new ArrayList<FileInfo>(
            displayList.size()));
        synchronized (displayList) {
            int size = displayList.size();
            for (int i = 0; i < size; i++) {
                tmpDisplayList.add(displayList.get(size - 1 - i));
            }
            displayList = tmpDisplayList;
        }
        fireModelChanged();
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
                logSevere("file not there: " + e.getFile());
            }

            displayList.add(e.getFile());
            fireModelChanged();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }
}
