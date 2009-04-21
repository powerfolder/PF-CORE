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
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.SyncProfileConfiguration;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Dialog for creatigng or editing profile configuration. User can select a
 * default profile and then adjust the configuration.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class CreateEditSyncProfileDialog extends BaseDialog implements
    ActionListener, KeyListener
{

    private JTextField syncProfileName;
    private JComboBox syncProfilesCombo;
    private JCheckBox autoDownloadFromFriendsBox;
    private JCheckBox autoDownloadFromOthersBox;
    private JCheckBox syncDeletionWithFriendsBox;
    private JCheckBox syncDeletionWithOthersBox;
    private SpinnerNumberModel scanTimeModel;
    private JSpinner scanTimeSpinner;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JLabel scanInfoLabel;
    private JRadioButton periodicRadioButton;
    private JRadioButton dailyRadioButton;
    private SpinnerNumberModel hourModel;
    private JSpinner hourSpinner;
    private JComboBox dayCombo;
    private JComboBox timeTypeCombo;
    private final boolean create;
    private JButton saveButton;
    private final SyncProfileConfiguration originalConfiguration;
    private final String originalProfileName;

    /**
     * Constructor.
     * 
     * @param controller
     * @param syncProfileSelectorPanel
     */
    public CreateEditSyncProfileDialog(Controller controller,
        SyncProfileSelectorPanel syncProfileSelectorPanel,
        boolean create)
    {
        super(controller, true);
        this.syncProfileSelectorPanel = syncProfileSelectorPanel;
        this.create = create;
        originalConfiguration = syncProfileSelectorPanel.getSyncProfile()
                .getConfiguration();
        originalProfileName = syncProfileSelectorPanel.getSyncProfile().getProfileName();
    }

    /**
     * Gets the title of the dialog.
     * 
     * @return
     */
    public String getTitle() {
        if (create) {
            return Translation.getTranslation("dialog.create_edit_profile.title_create");
        } else {
            return Translation.getTranslation("dialog.create_edit_profile.title_edit");
        }
    }

    /**
     * Gets the icon for the dialog.
     * 
     * @return
     */
    protected Icon getIcon() {
        return Icons.getIconById(Icons.NEW_FOLDER);
    }

    /**
     * Creates the visual component.
     * 
     * @return
     */
    protected Component getContent() {
        initComponents();
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, pref",
            "pref, 14dlu, pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.setBorder(Borders.createEmptyBorder("0, 0, 30dlu, 0"));

        // Profile name
        builder.add(new JLabel(Translation
            .getTranslation("dialog.create_edit_profile.profile_name")), cc.xy(1, 1));
        builder.add(syncProfileName, cc.xy(3, 1));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.create_edit_profile.configure_from")), cc
            .xy(1, 3));
        builder.add(createSyncComboPanel(), cc.xy(3, 3));

        builder.add(autoDownloadFromFriendsBox, cc.xy(3, 5));
        builder.add(autoDownloadFromOthersBox, cc.xy(3, 7));
        builder.add(syncDeletionWithFriendsBox, cc.xy(3, 9));
        builder.add(syncDeletionWithOthersBox, cc.xy(3, 11));

        builder.add(periodicRadioButton, cc.xy(3, 13));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.create_edit_profile.time_between_scans")), cc
            .xy(1, 15));
        builder.add(createRegularPanel(), cc.xy(3, 15));

        builder.add(dailyRadioButton, cc.xy(3, 17));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.create_edit_profile.hour_day_sync")), cc.xy(1, 19));
        builder.add(createDailyComboPanel(), cc.xy(3, 19));

        ButtonGroup bg = new ButtonGroup();
        bg.add(periodicRadioButton);
        bg.add(dailyRadioButton);

        return builder.getPanel();
    }

    private Component createSyncComboPanel() {
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(syncProfilesCombo, cc.xy(1, 1));
        return builder.getPanel();
    }

    public Component createDailyComboPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(hourSpinner, cc.xy(1, 1));
        builder.add(dayCombo, cc.xy(3, 1));
        return builder.getPanel();
    }

    public Component createRegularPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(scanTimeSpinner, cc.xy(1, 1));
        builder.add(scanInfoLabel, cc.xy(3, 1));
        builder.add(timeTypeCombo, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        SyncProfile initialProfile = syncProfileSelectorPanel.getSyncProfile();

        // Profile name
        syncProfileName = new JTextField();
        if (create) {
            String suggestedName = calculateBestName(initialProfile.getProfileName());
            syncProfileName.setText(suggestedName);
        } else {
            syncProfileName.setText(initialProfile.getProfileName());
        }

        syncProfileName.addKeyListener(this);

        syncProfilesCombo = new JComboBox();
        syncProfilesCombo.addItem("");
        for (SyncProfile syncProfile : SyncProfile.getSyncProfilesCopy()) {
            syncProfilesCombo.addItem(syncProfile.getProfileName());
        }
        syncProfilesCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                preselectDefaults();
            }
        });

        autoDownloadFromFriendsBox = new JCheckBox(Translation
            .getTranslation("dialog.create_edit_profile.auto_download_from_friends"));
        autoDownloadFromOthersBox = new JCheckBox(Translation
            .getTranslation("dialog.create_edit_profile.auto_download_from_other"));
        syncDeletionWithFriendsBox = new JCheckBox(Translation
            .getTranslation("dialog.create_edit_profile.sync_deletion_with_friends"));
        syncDeletionWithOthersBox = new JCheckBox(Translation
            .getTranslation("dialog.create_edit_profile.sync_deletion_with_others"));

        scanTimeModel = new SpinnerNumberModel(0, 0, 9999, 1);
        scanTimeSpinner = new JSpinner(scanTimeModel);
        scanInfoLabel = new JLabel(Translation
            .getTranslation("dialog.create_edit_profile.change_detection_manual"));
        scanTimeModel.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                infoTypeVisible();
            }
        });

        periodicRadioButton = new JRadioButton(Translation
            .getTranslation("dialog.create_edit_profile.periodic_sync"));
        periodicRadioButton.addActionListener(this);
        dailyRadioButton = new JRadioButton(Translation
            .getTranslation("dialog.create_edit_profile.daily_sync"));
        dailyRadioButton.addActionListener(this);

        dayCombo = new JComboBox(new Object[]{
            Translation.getTranslation("dialog.create_edit_profile.every_day"),
            Translation.getTranslation("general.sunday"),
            Translation.getTranslation("general.monday"),
            Translation.getTranslation("general.tuesday"),
            Translation.getTranslation("general.wednesday"),
            Translation.getTranslation("general.thursday"),
            Translation.getTranslation("general.friday"),
            Translation.getTranslation("general.saturday"),
            Translation.getTranslation("dialog.create_edit_profile.weekdays"),
            Translation.getTranslation("dialog.create_edit_profile.weekends")});
        dayCombo.setMaximumRowCount(10);

        hourModel = new SpinnerNumberModel(12, 0, 23, 1);
        hourSpinner = new JSpinner(hourModel);

        dayCombo.addActionListener(this);

        timeTypeCombo = new JComboBox(new Object[]{
            Translation.getTranslation("general.hours"),
            Translation.getTranslation("general.minutes"),
            Translation.getTranslation("general.seconds")});
        timeTypeCombo.addActionListener(this);

        // Initialise settings.
        SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();
        SyncProfileConfiguration configuration = syncProfile
                .getConfiguration();
        autoDownloadFromFriendsBox.setSelected(configuration
                .isAutoDownloadFromFriends());
        autoDownloadFromOthersBox.setSelected(configuration
                .isAutoDownloadFromOthers());
        syncDeletionWithFriendsBox.setSelected(configuration
                .isSyncDeletionWithFriends());
        syncDeletionWithOthersBox.setSelected(configuration
            .isSyncDeletionWithOthers());
        scanTimeModel.setValue(configuration.getTimeBetweenRegularScans());
        dailyRadioButton.setSelected(configuration.isDailySync());
        periodicRadioButton.setSelected(!configuration.isDailySync());
        hourModel.setValue(configuration.getDailyHour());
        dayCombo.setSelectedIndex(configuration.getDailyDay());
        if (configuration.getRegularTimeType().equals(SyncProfileConfiguration
                .REGULAR_TIME_TYPE_HOURS)) {
            timeTypeCombo.setSelectedIndex(0);
        } else if (configuration.getRegularTimeType().equals(
                SyncProfileConfiguration.REGULAR_TIME_TYPE_SECONDS)) {
            timeTypeCombo.setSelectedIndex(2);
        } else {
            timeTypeCombo.setSelectedIndex(1);
        }

        scanTimeSpinner.setEnabled(!configuration.isDailySync());
        hourSpinner.setEnabled(configuration.isDailySync());
        dayCombo.setEnabled(configuration.isDailySync());
        timeTypeCombo.setEnabled(!configuration.isDailySync());

        infoTypeVisible();
    }

    /**
     * Show scanInfoLabel and timeTypeCombo depending on scanTimeModel value.
     */
    private void infoTypeVisible() {
        if (scanTimeModel.getNumber().intValue() == 0) {
            scanInfoLabel.setVisible(true);
            timeTypeCombo.setVisible(false);
        } else {
            scanInfoLabel.setVisible(false);
            timeTypeCombo.setVisible(true);
        }
    }

    /**
     * Create a suggested profile name based on current profile.
     * Like 'Automatic download (copy)', or
     * 'Manual synchronization (copy 6)'.
     *
     * @param profileName
     * @return
     */
    private static String calculateBestName(String profileName) {

        int copy = 0;
        boolean unique;
        String profileNameCopy;

        do {

            unique = true;

            if (copy == 0) {
                profileNameCopy = Translation.getTranslation(
                        "dialog.create_edit_profile.suggestNameTemplate0",
                        profileName);
            } else {
                profileNameCopy = Translation.getTranslation(
                        "dialog.create_edit_profile.suggestNameTemplateN",
                        profileName, String.valueOf(copy));
            }

            for (SyncProfile loopSyncProfile :
                    SyncProfile.getSyncProfilesCopy()) {
                String loopProfileName = loopSyncProfile.getProfileName();
                if (loopProfileName.equalsIgnoreCase(profileNameCopy)) {
                    unique = false;
                    break;
                }
            }

            copy ++;

        } while (!unique);

        return profileNameCopy;
    }

    /**
     * Sets configuration settings based on the current combo selection.
     */
    private void preselectDefaults() {
        int index = syncProfilesCombo.getSelectedIndex() - 1; // Remove blank initial entry
        List<SyncProfile> syncProfileList = SyncProfile.getSyncProfilesCopy();
        if (index >= 0 && index < syncProfileList.size()) {
            SyncProfile syncProfile = SyncProfile.getSyncProfilesCopy().get(index);
            SyncProfileConfiguration configuration = syncProfile
                    .getConfiguration();
            autoDownloadFromFriendsBox.setSelected(configuration
                .isAutoDownloadFromFriends());
            autoDownloadFromOthersBox.setSelected(configuration
                .isAutoDownloadFromOthers());
            syncDeletionWithFriendsBox.setSelected(configuration
                .isSyncDeletionWithFriends());
            syncDeletionWithOthersBox.setSelected(configuration
                .isSyncDeletionWithOthers());
            scanTimeModel.setValue(configuration.getTimeBetweenRegularScans());
            periodicRadioButton.setSelected(!configuration.isDailySync());
            dailyRadioButton.setSelected(configuration.isDailySync());
            hourModel.setValue(configuration.getDailyHour());
            dayCombo.setSelectedIndex(configuration.getDailyDay());
            if (configuration.getRegularTimeType().equals(SyncProfileConfiguration
                    .REGULAR_TIME_TYPE_HOURS)) {
                timeTypeCombo.setSelectedIndex(0);
            } else if (configuration.getRegularTimeType().equals(
                    SyncProfileConfiguration.REGULAR_TIME_TYPE_SECONDS)) {
                timeTypeCombo.setSelectedIndex(2);
            } else {
                timeTypeCombo.setSelectedIndex(1);
            }

            enableTimeDate();

            syncProfilesCombo.setSelectedIndex(0);
        }
    }

    /**
     * The OK / Cancel buttons.
     * 
     * @return
     */
    protected Component getButtonBar() {

        saveButton = new JButton(Translation.getTranslation("general.save"));
        saveButton.setMnemonic(Translation.getTranslation("general.save.key")
            .trim().charAt(0));
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                savePressed();
            }
        });

        configureSaveButton();

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        return ButtonBarFactory.buildCenteredBar(saveButton, cancelButton);
    }

    // Methods for FolderPreferencesPanel **************************************

    /**
     * If user clicks save, update the profile in the selector panel.
     */
    private void savePressed() {
        String timeType = SyncProfileConfiguration.REGULAR_TIME_TYPE_MINUTES;
        if (timeTypeCombo.getSelectedIndex() == 0) {
            timeType = SyncProfileConfiguration.REGULAR_TIME_TYPE_HOURS;
        } else if (timeTypeCombo.getSelectedIndex() == 2) {
            timeType = SyncProfileConfiguration.REGULAR_TIME_TYPE_SECONDS;
        }

        SyncProfileConfiguration newConfiguration = new SyncProfileConfiguration(
           autoDownloadFromFriendsBox.isSelected(),
                autoDownloadFromOthersBox.isSelected(),
            syncDeletionWithFriendsBox.isSelected(), syncDeletionWithOthersBox
                .isSelected(), scanTimeModel.getNumber().intValue(),
            dailyRadioButton.isSelected(), hourModel.getNumber().intValue(),
            dayCombo.getSelectedIndex(), timeType
        );

        String newProfileName = syncProfileName.getText().trim();

        if (create) {

            // Name and config must be unique.
            if (checkDuplicateProfileName(newProfileName) ||
                    checkDuplicateConfiguration(newConfiguration)) {
                return;
            }

            // Store new profile
            SyncProfile syncProfile = SyncProfile.retrieveSyncProfile(
                    newProfileName, newConfiguration);
            syncProfileSelectorPanel.setSyncProfile(syncProfile, true);

        } else {

            // If no change, do nothing.
            if (originalProfileName.equals(newProfileName) &&
                    originalConfiguration.equals(newConfiguration)) {
                close();
            }

            // Get the current sync profile.
            SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();

            if (!originalConfiguration.equals(newConfiguration)) {

                // Check new configuration is unique.
                if (checkDuplicateConfiguration(newConfiguration)) {
                    return;
                }

                // Update the profile with configuration changes.
                syncProfile.setConfiguration(newConfiguration);

            }

            if (!originalProfileName.equals(newProfileName)) {

                // Check new profileName is unique.
                if (checkDuplicateProfileName(newProfileName)) {
                    return;
                }

                // Update the profile with name change.
                syncProfile.setProfileName(newProfileName);

                // Set in the selector panel so it sees the name change.
                syncProfileSelectorPanel.setSyncProfile(syncProfile, true);

            }
        }

        close();
    }

    /**
     * Check if there are already profiles with this name.
     *
     * @param profileName name to check
     * @return true if a profile already exists with this name
     */
    private boolean checkDuplicateProfileName(String profileName) {

        for (SyncProfile syncProfile : SyncProfile.getSyncProfilesCopy()) {
            if (syncProfile.getProfileName().equals(profileName)) {
                String title = Translation.
                        getTranslation("dialog.create_edit_profile.duplicate_profile_title");
                String message = Translation
                        .getTranslation("dialog.create_edit_profile.cannot_save_name", 
                        syncProfile.getProfileName());
                DialogFactory.genericDialog(getController(),
                        title, message, GenericDialogType.ERROR);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there are already profiles with this configuration.
     *
     * @param newConfiguration
     * @return
     */
    private boolean checkDuplicateConfiguration(SyncProfileConfiguration newConfiguration) {

        for (SyncProfile syncProfile : SyncProfile.getSyncProfilesCopy()) {
            if (syncProfile.getConfiguration().equals(newConfiguration)) {
                String title = Translation.
                        getTranslation("dialog.create_edit_profile.duplicate_profile_title");
                String message = Translation
                        .getTranslation("dialog.create_edit_profile.cannot_save_profile",
                        syncProfile.getProfileName());
                DialogFactory.genericDialog(getController(),
                        title, message, GenericDialogType.ERROR);
                return true;
            }
        }
        return false;
    }

    /**
     * User does not want to commit any change.
     */
    private void cancelPressed() {
        close();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(periodicRadioButton)
            || e.getSource().equals(dailyRadioButton))
        {
            enableTimeDate();
        }
    }

    private void enableTimeDate() {
        scanTimeSpinner.setEnabled(periodicRadioButton.isSelected());
        scanInfoLabel.setEnabled(periodicRadioButton.isSelected());
        timeTypeCombo.setEnabled(periodicRadioButton.isSelected());
        hourSpinner.setEnabled(dailyRadioButton.isSelected());
        dayCombo.setEnabled(dailyRadioButton.isSelected());
    }

    public void keyTyped(KeyEvent e) {
        if (e.getSource() == syncProfileName) {
            configureSaveButton();
        }
    }

    private void configureSaveButton() {

        // Enable the OK button if a neam is entered.
        saveButton.setEnabled(syncProfileName.getText().trim().length() > 0);
    }

    public void keyPressed(KeyEvent e) {
        // Not required
    }

    public void keyReleased(KeyEvent e) {
        // Veto any comma (,) characters, because these may make the config fail.
        // Commas are used to separate profile fields.
        String text = syncProfileName.getText();
        while (text.endsWith(SyncProfile.FIELD_LIST_DELIMITER)) {
            syncProfileName.removeKeyListener(this);
            syncProfileName.setText(text.substring(0, text.length() - 1));
            syncProfileName.addKeyListener(this);

            // Keep going; user may have held key to get auto-repeat!
            text = syncProfileName.getText();
        }
    }
}
