/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: SingleFileRestoreTableModel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard.table;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.model.SortedTableModel;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

public class SingleFileRestoreTableModel  extends PFComponent implements TableModel, SortedTableModel {
    public int getRowCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getColumnCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getColumnName(int columnIndex) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class<?> getColumnClass(int columnIndex) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addTableModelListener(TableModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeTableModelListener(TableModelListener l) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getSortColumn() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSortAscending() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean sortBy(int columnIndex) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAscending(boolean ascending) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
