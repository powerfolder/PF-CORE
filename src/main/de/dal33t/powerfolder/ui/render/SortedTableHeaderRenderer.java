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
import java.awt.ComponentOrientation;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;

/**
 * Table header renderer for tables with models that support SortedTableModel.
 * Sets the header cell icon to a ^ or v icon if it is the sorted column.
 */
public class SortedTableHeaderRenderer extends JLabel implements TableCellRenderer {

    /**
     * Model that gives details of the sorder column and direction.
     */
    private SortedTableModel sortedTableModel;

    public static void associateHeaderRenderer(
            final SortedTableModel sortedTableModelArg,
            TableColumnModel columnModel,
            final int initialSortColumn, final boolean ascending) {
        SortedTableHeaderRenderer uthr = new SortedTableHeaderRenderer();

        uthr.sortedTableModel = sortedTableModelArg;

        // Associate columns with this renderer.
        Enumeration<TableColumn> columns = columnModel.getColumns();
        while (columns.hasMoreElements()) {
            columns.nextElement().setHeaderRenderer(uthr);
        }

        // Initialize the sorted data to match the headers.
        Runnable r = new Runnable() {
            public void run() {
                sortedTableModelArg.setAscending(ascending);
                sortedTableModelArg.sortBy(initialSortColumn);
            }
        };
        UIUtil.invokeLaterInEDT(r);
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

        // Place the icon on the right of the text.
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // Set icon depending on the sort column / direction
        if (column == sortedTableModel.getSortColumn()) {
            if (sortedTableModel.isSortAscending()) {
                setIcon(Icons.getIconById(Icons.SORT_UP));
            } else {
                setIcon(Icons.getIconById(Icons.SORT_DOWN));
            }
        } else {

            // Set to blank icon to stop the text position jumping.
            setIcon(Icons.getIconById(Icons.SORT_BLANK));
        }

        // Since the renderer is a component, return itself.
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
