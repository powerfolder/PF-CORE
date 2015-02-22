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
package de.dal33t.powerfolder.ui.render;

import java.awt.Component;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Table header renderer for tables with models that are not sorted.
 * This allows for a consistent look to all table headers.
 * Has the same borders and alignment as with the SortedTableHeaderRenderer.
 */
public class UnsortedTableHeaderRenderer extends JLabel implements TableCellRenderer {

    public static void associateHeaderRenderer(TableColumnModel columnModel) {
        UnsortedTableHeaderRenderer uthr = new UnsortedTableHeaderRenderer();
        // Associate columns with this renderer.
        Enumeration<TableColumn> columns = columnModel.getColumns();
        while (columns.hasMoreElements()) {
            columns.nextElement().setHeaderRenderer(uthr);
        }
    }

    /**
     * This method is called each time a column header using this renderer
     * needs to be rendered. Implements the TableCellRenderer method.
     *
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row,
                                                   int column) {
        // Configure component.
        setText(value.toString());
        setBorder(BorderFactory.createEtchedBorder());
        setHorizontalAlignment(CENTER);

        // Since the renderer is a component, return itself
        return this;
    }

    public void validate() {
    }

    public void revalidate() {
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}