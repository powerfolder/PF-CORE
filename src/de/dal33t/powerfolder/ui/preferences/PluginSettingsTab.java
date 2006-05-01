package de.dal33t.powerfolder.ui.preferences;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.plugin.Plugin;
import de.dal33t.powerfolder.plugin.PluginManager;
import de.dal33t.powerfolder.ui.action.SelectionBaseAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

public class PluginSettingsTab extends PFUIComponent implements PreferenceTab {

    private final static int PLUGIN_NAME_COL = 0;
    private final static int PLUGIN_DESCR_COL = 1;
    private final static int PLUGIN_CLASS_COL = 2;

    private PreferencesDialog preferencesDialog;
    
    private JPanel panel;
    private JTable pluginJTable;
    private JScrollPane pluginPane;
    private JButton settingsButton;
    private SelectionModel selectionModel;

    public PluginSettingsTab(Controller controller, PreferencesDialog preferencesDialog) {
        super(controller);
        this.preferencesDialog = preferencesDialog;
        selectionModel = new SelectionModel();
        initComponents();
    }

    public boolean needsRestart() {
        return false;
    }

    public void save() {
    }

    public boolean validate() {
        return true;
    }

    public String getTabName() {
        return Translation
            .getTranslation("preferences.dialog.pluginsettings.tabname");
    }

    public void undoChanges() {
    }

    /**
     * Creates the JPanel for plugin settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, fill:pref:grow, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(pluginPane, cc.xy(2, 2));
            builder.add(getButtonBar(), cc.xy(2, 4));
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        pluginJTable = new JTable(new PluginTableModel());
        pluginJTable
            .setDefaultRenderer(Plugin.class, new PluginTableRenderer());
        pluginPane = new JScrollPane(pluginJTable);
        Util.whiteStripTable(pluginJTable);
        Util.removeBorder(pluginPane);
        Util.setZeroHeight(pluginPane);

        pluginJTable.getSelectionModel().addListSelectionListener(
            new PluginTableListSelectionListener());
        settingsButton = createSettingsButton();
    }

    private JButton createSettingsButton() {
        JButton SettingsButton = new JButton(new SettingsAction(
            getController(), selectionModel));
        return SettingsButton;
    }

    private Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(settingsButton);
    }

    private class PluginTableModel extends AbstractTableModel {

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return Plugin.class;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column) {
                case PLUGIN_NAME_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.pluginsettings.pluginname");
                }
                case PLUGIN_DESCR_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.pluginSettings.plugindescription");
                }
                case PLUGIN_CLASS_COL : {
                    return Translation
                        .getTranslation("preferences.dialog.pluginSettings.pluginclassname");
                }
                default :
                    return null;
            }

        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            PluginManager pluginManager = getController().getPluginManager();
            return pluginManager.getPlugins().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            return plugins.get(rowIndex);

        }
    }

    private class PluginTableRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            String newValue = "";
            PluginManager pluginManager = getController().getPluginManager();
            List<Plugin> plugins = pluginManager.getPlugins();
            Plugin plugin = plugins.get(row);
            int columnInModel = Util.toModel(table, column);
            switch (columnInModel) {
                case PLUGIN_NAME_COL : {
                    newValue = plugin.getName();
                    setToolTipText(plugin.getName());
                    break;
                }
                case PLUGIN_DESCR_COL : {
                    newValue = plugin.getDescription();
                    setToolTipText(plugin.getDescription());
                    break;
                }
                case PLUGIN_CLASS_COL : {
                    newValue = plugin.getClass().getName();
                    setToolTipText(plugin.getClass().getName());
                    break;
                }
                default :
                    return null;
            }
            return super.getTableCellRendererComponent(table, newValue,
                isSelected, hasFocus, row, column);
        }
    }

    private class SettingsAction extends SelectionBaseAction {

        public SettingsAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("pluginsettings", controller, selectionModel);
            setEnabled(false);
        }

        public void selectionChanged(SelectionChangeEvent event) {
            Plugin plugin = (Plugin) event.getSelection();
            if (plugin != null) {
                setEnabled(plugin.hasOptionsDialog());
            }
        }

        public void actionPerformed(ActionEvent e) {
            int index = pluginJTable.getSelectedRow();
            PluginManager pluginManager = getController().getPluginManager();
            Plugin plugin = pluginManager.getPlugins().get(index);
            if (plugin.hasOptionsDialog()) {
                plugin.showOptionsDialog(preferencesDialog.getDialog());
            }
        }

    }

    /**
     * updates the SelectionModel if some selection has changed in the plugin
     * table
     */
    private class PluginTableListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = pluginJTable.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = pluginJTable.getModel().getValueAt(
                        selectedRows[i], 0);
                }
                selectionModel.setSelections(selectedObjects);
            } else {
                selectionModel.setSelection(null);
            }
        }
    }
}
