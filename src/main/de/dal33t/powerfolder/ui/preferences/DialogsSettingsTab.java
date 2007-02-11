package de.dal33t.powerfolder.ui.preferences;

import java.util.prefs.Preferences;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LimitedConnectivityChecker;

public class DialogsSettingsTab extends PFComponent implements PreferenceTab {

    private JCheckBox updateCheck;

    /** Ask to add to friends if user becomes member of a folder */
    private JCheckBox askForFriendship;

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
        Preferences pref = getController().getPreferences();

        boolean checkForUpdate = PreferencesEntry.CHECK_UPDATE
            .getValueBoolean(getController());
        boolean askFriendship = PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
            .getValueBoolean(getController());
        boolean testConnectivity = pref.getBoolean(
            LimitedConnectivityChecker.PREF_NAME_TEST_CONNECTIVITY, true);
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        boolean filenamCheck = PreferencesEntry.FILE_NAME_CHECK
            .getValueBoolean(getController());

        updateCheck = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.check_for_program_updates"),
            checkForUpdate);
        askForFriendship = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.ask_to_add_to_friends_if_node_becomes_member_of_folder"),
            askFriendship);
        warnOnCloseIfNotInSync = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warnoncloseifnotinsync"),
            warnOnClose);
        warnOnLimitedConnectivity = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warnonlimitedconnectivity"),
            testConnectivity);
        warnOnPossibleFilenameProblems = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warnonpossiblefilenameproblems"),
            filenamCheck);
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 7dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.add(updateCheck, cc.xy(1, row));

            row += 2;
            builder.add(warnOnCloseIfNotInSync, cc.xy(1, row));

            row += 2;
            builder.add(warnOnLimitedConnectivity, cc.xy(1, row));

            row += 2;
            builder.add(warnOnPossibleFilenameProblems, cc.xy(1, row));

            row += 2;
            builder.add(askForFriendship, cc.xy(1, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the dialogs settings.
     */
    public void save() {
        Preferences pref = getController().getPreferences();
        boolean checkForUpdate = updateCheck.isSelected();
        boolean testConnectivity = warnOnLimitedConnectivity.isSelected();
        boolean warnOnClose = warnOnCloseIfNotInSync.isSelected();
        boolean filenamCheck = warnOnPossibleFilenameProblems.isSelected();
        boolean askFriendship = askForFriendship.isSelected();

        PreferencesEntry.CHECK_UPDATE.setValue(getController(), checkForUpdate);
        PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN.setValue(
            getController(), askFriendship);
        pref.putBoolean(LimitedConnectivityChecker.PREF_NAME_TEST_CONNECTIVITY,
            testConnectivity);
        PreferencesEntry.WARN_ON_CLOSE.setValue(getController(), warnOnClose);
        PreferencesEntry.FILE_NAME_CHECK
            .setValue(getController(), filenamCheck);
    }

}
