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
package de.dal33t.powerfolder.util.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Enables to hide/show the columns in a table. default: visible for all
 * columns.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.10 $
 */
public class CustomTableModel implements TableModel {
    /**
     * Tracks visiblitity of individual columns. Key is Integer, columnIndex.
     * Value = Boolean indicating visible or not
     */
    private Map<Integer, Boolean> visibleMap = new HashMap<Integer, Boolean>();
    /** the model of all columns */
    private TableModel tableModel;
    /** List of event listeners */
    private List<TableModelListener> listeners = new LinkedList<TableModelListener>();
    /** number of current visible columns * */
    private int columnCount;

    public CustomTableModel(TableModel model) {
        this.tableModel = model;
        for (int i = 0; i < model.getColumnCount(); i++) {
            visibleMap.put(i, true);
        }
        columnCount = model.getColumnCount();
    }

    /** hides or shows a column * */
    public void setColumnVisible(int columnIndex, boolean visible) {
        if (columnIndex >= tableModel.getColumnCount()) {
            throw new IllegalStateException("columnIndex out of bounds: "
                + columnIndex);
        }
        boolean currentState = isColumnVisible(columnIndex);
        // only change if something realy changed
        if (currentState != visible) {
            if (!visible && columnCount == 1) {
                return; // do not hide the last column!
            }
            visibleMap.put(columnIndex, visible);
            if (visible) {
                columnCount++;
            } else {
                columnCount--;
            }
            fireStructureChanged();
        }
    }

    /** gets the column index of the underlaying model */
    public int mapToColumnIndex(int columnIndex) {
        int count = tableModel.getColumnCount();
        int visibleCount = 0;
        for (int i = 0; i < count; i++) {
            if (isColumnVisible(i)) {
                if (visibleCount == columnIndex) {
                    return i;
                }
                visibleCount++;
            }
        }
        throw new IllegalStateException(
            "should not get to this code ....columnIndex: " + columnIndex + " "
                + "columnCount: " + columnCount);
    }

    /** checkes if this column of the underlaying model is visible * */
    boolean isColumnVisible(int columnIndex) {
        return visibleMap.get(columnIndex);
    }

    public Class getColumnClass(int columnIndex) {
        return tableModel.getColumnClass(mapToColumnIndex(columnIndex));
    }

    public int getColumnCount() {
        return columnCount;
    }

    public String getColumnName(int columnIndex) {
        return tableModel.getColumnName(mapToColumnIndex(columnIndex));
    }

    public int getRowCount() {
        return tableModel.getRowCount();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return tableModel.isCellEditable(rowIndex,
            mapToColumnIndex(columnIndex));
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return tableModel.getValueAt(rowIndex, mapToColumnIndex(columnIndex));
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        tableModel.setValueAt(aValue, rowIndex, mapToColumnIndex(columnIndex));
    }

    /** return the underlaying model* */
    public TableModel getModel() {
        return tableModel;
    }

    // ***** listener support
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
        tableModel.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
        tableModel.removeTableModelListener(l);
    }

    private void fireStructureChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            TableModelListener listener = listeners.get(i);
            listener.tableChanged(new TableModelEvent(CustomTableModel.this,
                TableModelEvent.HEADER_ROW));
        }
    }
}