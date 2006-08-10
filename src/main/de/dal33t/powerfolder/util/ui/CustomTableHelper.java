package de.dal33t.powerfolder.util.ui;

import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;

public class CustomTableHelper {

    public static JPopupMenu createSetUpColumnsMenu(Controller controller,
        CustomTableModel customTableModel, String prefName)
    {
        TableModel model = customTableModel.getModel();
        JPopupMenu menu = new JPopupMenu("set up columns");
        for (int i = 0; i < model.getColumnCount(); i++) {
            String columnPrefName = prefName + ".column." + i;
            Action action = new CustomTableShowHideAction(controller,
                customTableModel, i, columnPrefName);
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
