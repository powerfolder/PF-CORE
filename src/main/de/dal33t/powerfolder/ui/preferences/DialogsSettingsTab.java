package de.dal33t.powerfolder.ui.preferences;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;

public class DialogsSettingsTab extends PFComponent implements PreferenceTab {

    /** warn on limited connectivity */
    private JCheckBox warnOnLimitedConnectivity;

    /** warn on posible filename problems */
    private JCheckBox warnOnPossibleFilenameProblems;

    /** warn on close program if a folder is still syncing */
    private JCheckBox warnOnCloseIfNotInSync;

    private JPanel panel;

    boolean needsRestart = false;

    public DialogsSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.dialogs.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }

    private void initComponents() {
        warnOnCloseIfNotInSync = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warnoncloseifnotinsync"));
        warnOnLimitedConnectivity = new JCheckBox(
            Translation
            .getTranslation("preferences.dialog.dialogs.warnonlimitedconnectivity"));
        warnOnPossibleFilenameProblems = new JCheckBox(
            Translation
            .getTranslation("preferences.dialog.dialogs.warnonpossiblefilenameproblems"));
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("7dlu, pref, 7dlu",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref, 3dlu, top:pref:grow, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();
            int row = 1;

            builder.add(warnOnCloseIfNotInSync, cc.xy(2, row));

            row += 2;

            builder.add(warnOnLimitedConnectivity, cc.xy(2, row));
            row += 2;

            builder.add(warnOnPossibleFilenameProblems, cc.xy(2, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the dialogs settings.
     */
    public void save() {

    }

}
