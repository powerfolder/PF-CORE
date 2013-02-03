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
package de.dal33t.powerfolder.ui.preferences;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;

public class WarningsNotificationsSettingsTab extends PFComponent implements PreferenceTab {

    /** Show system notifications */
    private JCheckBox showSystemNotificationBox;

    private JCheckBox showHiddenFilesCB;

    /** Notification translucency */
    private JSlider notificationTranslucentSlider;

    /** Notification dwell period (seconds) */
    private JSlider notificationDisplaySlider;

    /** Show pause options. */
    private JCheckBox showPauseOptionsCB;

    /** warn on no direct connectivity */
    private JCheckBox warnOnNoDirectConnectivityCB;

    /** warn on possible filename problems */
    private JCheckBox warnOnPossibleFilenameProblemsCB;

    /** warn on close program if a folder is still syncing */
    private JCheckBox warnOnCloseIfNotInSyncCB;

    /** warn if online storage more than 90% full. */
    private JCheckBox warnIfCloudSpaceFullCB;

    private JPanel panel;

    private boolean needsRestart;

    private ApplicationModel applicationModel;

    public WarningsNotificationsSettingsTab(Controller controller) {
        super(controller);
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.warnings_notifications.title");
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
        applicationModel = getController().getUIController().getApplicationModel();

        showHiddenFilesCB = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("preferences.warnings_notifications.show_hidden_files"));
        showHiddenFilesCB.setSelected(PreferencesEntry.SHOW_HIDDEN_FILES.getValueBoolean(getController()));

        // Show system notifications when minimized
        showSystemNotificationBox = new JCheckBox(
                Translation.getTranslation("preferences.warnings_notifications.show_system_notifications"));
        showSystemNotificationBox.setSelected(
                (Boolean) applicationModel.getSystemNotificationsValueModel().getValue());

        showPauseOptionsCB = new JCheckBox(Translation
            .getTranslation("preferences.warnings_notifications.show_pause_options"));
        showPauseOptionsCB.setSelected(
                PreferencesEntry.SHOW_ASK_FOR_PAUSE.getValueBoolean(
                        getController()));

        notificationDisplaySlider = new JSlider();
        notificationDisplaySlider.setMinimum(0);
        notificationDisplaySlider.setMaximum(30);
        notificationDisplaySlider
            .setValue(PreferencesEntry.NOTIFICATION_DISPLAY
                .getValueInt(getController()));
        notificationDisplaySlider.setMajorTickSpacing(5);
        notificationDisplaySlider.setMinorTickSpacing(1);

        notificationDisplaySlider.setPaintTicks(true);
        notificationDisplaySlider.setPaintLabels(true);

        Dictionary<Integer, JLabel> dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 0; i <= 30; i += notificationDisplaySlider
            .getMajorTickSpacing())
        {
            dictionary.put(i, new JLabel(Integer.toString(i)));
        }
        notificationDisplaySlider.setLabelTable(dictionary);

        notificationTranslucentSlider = new JSlider();
        notificationTranslucentSlider.setMinimum(0);
        notificationTranslucentSlider.setMaximum(80);
        notificationTranslucentSlider
            .setValue(PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController()));
        notificationTranslucentSlider.setMajorTickSpacing(20);
        notificationTranslucentSlider.setMinorTickSpacing(5);

        notificationTranslucentSlider.setPaintTicks(true);
        notificationTranslucentSlider.setPaintLabels(true);

        dictionary = new Hashtable<Integer, JLabel>();
        for (int i = 0; i <= 80; i += notificationTranslucentSlider
            .getMajorTickSpacing())
        {
            dictionary.put(i, new JLabel(Integer.toString(i) + '%'));
        }
        notificationTranslucentSlider.setLabelTable(dictionary);

        boolean warnOnNoDirectConnectivity = PreferencesEntry.WARN_ON_NO_DIRECT_CONNECTIVITY
            .getValueBoolean(getController());
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        boolean fileNameCheck = PreferencesEntry.FILE_NAME_CHECK
            .getValueBoolean(getController());
        boolean cloudFull = PreferencesEntry.WARN_FULL_CLOUD
            .getValueBoolean(getController());
        warnOnCloseIfNotInSyncCB = new JCheckBox(
            Translation
                .getTranslation("preferences.warnings_notifications.warn_on_close_if_not_in_sync"),
            warnOnClose);
        warnOnNoDirectConnectivityCB = new JCheckBox(
            Translation.getTranslation("preferences.warnings_notifications.warn_on_no_direct_connectivity"),
            warnOnNoDirectConnectivity);
        warnOnPossibleFilenameProblemsCB = new JCheckBox(
            Translation
                .getTranslation("preferences.warnings_notifications.warn_on_possible_file_name_problems"),
            fileNameCheck);
        warnIfCloudSpaceFullCB = new JCheckBox(
            Translation
                .getTranslation("preferences.warnings_notifications.warn_if_cloud_space_full"),
            cloudFull);
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:pref, 3dlu, pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;

            builder.add(showPauseOptionsCB, cc.xy(3, row));

            row += 2;
            builder.add(showHiddenFilesCB, cc.xy(3, row));

            row += 2;
            builder.add(warnIfCloudSpaceFullCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnNoDirectConnectivityCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnCloseIfNotInSyncCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnPossibleFilenameProblemsCB, cc.xy(3, row));

            // //////////////////////////////////////
            // Notification stuff only below here //
            // //////////////////////////////////////

            row += 2;
            builder.addSeparator(Translation.getTranslation("preferences.warnings_notifications.notifications"),
                    cc.xyw(1, row, 3));

            row += 2;
            builder.add(showSystemNotificationBox, cc.xy(3, row));

            if (Constants.OPACITY_SUPPORTED) {
                row += 2;
                builder.addLabel(Translation.getTranslation("preferences.warnings_notifications.notification_translucency"),
                        cc.xy(1, row));
                builder.add(createNotificationTranslucentSpinnerPanel(), cc.xy(
                    3, row));
            }

            row += 2;
            builder.addLabel(Translation.getTranslation("preferences.warnings_notifications.notification_delay"),
                    cc.xy(1, row));
            builder.add(createNotificationDisplaySpinnerPanel(), cc.xy(3, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    private Component createNotificationDisplaySpinnerPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref, pref:grow",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(notificationDisplaySlider, cc.xy(1, 1));
        JButton preview = new JButton(new PreviewAction(getController()));
        builder.add(preview, cc.xy(3, 1));
        return builder.getPanel();
    }

    private Component createNotificationTranslucentSpinnerPanel() {
        FormLayout layout = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(notificationTranslucentSlider, cc.xy(1, 1));
        return builder.getPanel();
    }

    /**
     * Saves the dialogs settings.
     */
    public void save() {

        boolean warnOnNoDirectConnectivity = warnOnNoDirectConnectivityCB.isSelected();
        boolean warnOnClose = warnOnCloseIfNotInSyncCB.isSelected();
        boolean filenameCheck = warnOnPossibleFilenameProblemsCB.isSelected();
        boolean fullCloudSpace = warnIfCloudSpaceFullCB.isSelected();

        PreferencesEntry.SHOW_HIDDEN_FILES.setValue(getController(), showHiddenFilesCB.isSelected());

        if (showSystemNotificationBox != null) {
            applicationModel.getSystemNotificationsValueModel().setValue(
                showSystemNotificationBox.isSelected());
        }

        PreferencesEntry.NOTIFICATION_TRANSLUCENT.setValue(getController(),
            notificationTranslucentSlider.getValue());

        PreferencesEntry.SHOW_ASK_FOR_PAUSE.setValue(getController(),
            showPauseOptionsCB.isSelected());

        PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
            notificationDisplaySlider.getValue());
        PreferencesEntry.WARN_ON_NO_DIRECT_CONNECTIVITY.setValue(getController(),
            warnOnNoDirectConnectivity);
        PreferencesEntry.WARN_ON_CLOSE.setValue(getController(), warnOnClose);
        PreferencesEntry.WARN_FULL_CLOUD.setValue(getController(),
                fullCloudSpace);
        PreferencesEntry.FILE_NAME_CHECK
            .setValue(getController(), filenameCheck);
    }

    /**
     * Show a preview of the notification.
     */
    private class PreviewAction extends BaseAction {
        private PreviewAction(Controller controller) {
            super("action_preview", controller);
        }

        public void actionPerformed(ActionEvent e) {

            // Remember current
            Integer currentDisplay = PreferencesEntry.NOTIFICATION_DISPLAY
                .getValueInt(getController());
            Integer currentTranslucent = PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController());

            // Set temporary
            PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
                notificationDisplaySlider.getValue());
            PreferencesEntry.NOTIFICATION_TRANSLUCENT.setValue(getController(),
                notificationTranslucentSlider.getValue());

            // Display
            getController()
                .getUIController()
                .previewMessage(
                    Translation
                        .getTranslation("preferences.warnings_notifications.notification_preview_title"),
                    Translation
                        .getTranslation("preferences.warnings_notifications.notification_preview_text"));

            // Reset
            PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
                currentDisplay);
            PreferencesEntry.NOTIFICATION_TRANSLUCENT.setValue(getController(),
                currentTranslucent);
        }
    }
}
