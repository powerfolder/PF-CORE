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

import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;

public class CustomTableHelper {

    public static JPopupMenu createSetUpColumnsMenu(Controller controller,
        CustomTableModel customTableModel, String prefName, JTable table)
    {
        TableModel model = customTableModel.getModel();
        JPopupMenu menu = new JPopupMenu("set up columns");
        for (int i = 0; i < model.getColumnCount(); i++) {
            String columnPrefName = prefName + ".column." + i;
            Action action = new CustomTableShowHideAction(controller,
                customTableModel, i, columnPrefName, table);
            menu.add(action);
        }
        return menu;
    }

    public static void setupFromPref(Controller controller,
        final CustomTableModel customTableModel, final String prefName,
        final boolean[] defaults)
    {
        final TableModel model = customTableModel.getModel();
        final Preferences pref = controller.getPreferences();
        if (model.getColumnCount() != defaults.length) {
            throw new IllegalArgumentException(
                "tablemodel column count should equal defaults!"
                    + model.getColumnCount() + "!=" + defaults.length);
        }
        Runnable runner = new Runnable() {
            public void run() {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    String completePrefName = prefName + ".column." + i;
                    boolean visible = pref.getBoolean(completePrefName,
                        defaults[i]);
                    customTableModel.setColumnVisible(i, visible);
                    // Directly store into prefs, to have it persistent
                    pref.putBoolean(completePrefName, visible);
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }
}
