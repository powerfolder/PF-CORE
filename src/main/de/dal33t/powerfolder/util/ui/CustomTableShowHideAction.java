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

import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.UnsortedTableHeaderRenderer;

public class CustomTableShowHideAction extends AbstractAction {
    private Controller controller;
    private int columnIndex;
    private CustomTableModel customTableModel;
    private String prefName;
    private JTable table;

    public CustomTableShowHideAction(Controller controller,
        CustomTableModel customTableModel, int columnIndex, String prefName,
        JTable table)
    {
        this.controller = controller;
        this.customTableModel = customTableModel;
        this.prefName = prefName;
        this.table = table;
        TableModel model = customTableModel.getModel();
        this.columnIndex = columnIndex;
        putValue(Action.NAME, model.getColumnName(columnIndex));
        Preferences pref = controller.getPreferences();
        
        boolean visible = pref.getBoolean(prefName, true);
        if (visible) { // should this column be visible
            putValue(Action.SMALL_ICON, Icons.getIconById(Icons.CHECKED)); // add a chekced
            // icon
        } else {
            putValue(Action.SMALL_ICON, null); // no icon
        }

        // listen to the column change events
        customTableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    checkState();
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        boolean status = !customTableModel.isColumnVisible(columnIndex);
        customTableModel.setColumnVisible(columnIndex, status);
        UnsortedTableHeaderRenderer.associateHeaderRenderer(table
                .getColumnModel());
    }

    public void checkState() {
        boolean visible = customTableModel.isColumnVisible(columnIndex);
        if (visible) {
            putValue(Action.SMALL_ICON, Icons.getIconById(Icons.CHECKED));
        } else {
            putValue(Action.SMALL_ICON, null);
        }
        Preferences pref = controller.getPreferences();
        pref.putBoolean(prefName, visible);        
    }

}
