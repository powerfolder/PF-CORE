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

import com.jgoodies.binding.value.Trigger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.*;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;

public class DialogsSettingsTab extends PFComponent implements PreferenceTab {

    /** Show chat notifications */
    private JCheckBox showChatNotificationBox;

    /** Show system notifications */
    private JCheckBox showSystemNotificationBox;

    /** Notification translucency */
    private JSlider notificationTranslucentSlider;

    /** Notification dwell period (seconds) */
    private JSlider notificationDisplaySlider;

    /** Ask to add to friends if user becomes member of a folder */
    private JCheckBox askForFriendshipCB;

    /** Add personal message with freindship status change */
    private JCheckBox askForFriendshipMessageCB;

    /** Show folders that have been found in PF folder base & auto-created. */
    private JCheckBox showAutoCreatedFoldersBox;

    /** Show pause options. */
    private JCheckBox showPauseOptionsCB;

    /** warn on limited connectivity */
    private JCheckBox warnOnLimitedConnectivityCB;

    /** warn on posible filename problems */
    private JCheckBox warnOnPossibleFilenameProblemsCB;

    /** warn on close program if a folder is still syncing */
    private JCheckBox warnOnCloseIfNotInSyncCB;

    /** warn if changing profile for multiple folders */
    private JCheckBox warnOnDuplicateFoldersCB;

    /** warn if connection quality is poor */
    private JCheckBox warnOnPoorConnectionQualityCB;

    /** warn if online storage more than 90% full. */
    private JCheckBox warnIfCloudSpaceFullCB;

    private JPanel panel;

    private boolean needsRestart;

    // The triggers the writing into core
    private Trigger writeTrigger;
    private ApplicationModel applicationModel;

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
        applicationModel = getController().getUIController()
            .getApplicationModel();

        writeTrigger = new Trigger();

        // Show chat notifications when minimized
        showChatNotificationBox = new JCheckBox(Translation
            .getTranslation("preferences.dialog.show_chat_notifications"));
        showChatNotificationBox.setSelected((Boolean) applicationModel
            .getChatNotificationsValueModel().getValue());

        // Show system notifications when minimized
        showSystemNotificationBox = new JCheckBox(Translation
            .getTranslation("preferences.dialog.show_system_notifications"));
        showSystemNotificationBox.setSelected((Boolean) applicationModel
            .getSystemNotificationsValueModel().getValue());

        // Show system notifications when minimized
        showAutoCreatedFoldersBox = new JCheckBox(Translation
            .getTranslation("preferences.dialog.show_auto_created_folders"));
        showAutoCreatedFoldersBox.setSelected(
                PreferencesEntry.SHOW_AUTO_CREATED_FOLDERS.getValueBoolean(
                        getController()));

        showPauseOptionsCB = new JCheckBox(Translation
            .getTranslation("preferences.dialog.show_pause_options"));
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

        boolean askFriendship = PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
            .getValueBoolean(getController());
        boolean askFriendshipMessage = PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE
            .getValueBoolean(getController());
        boolean testConnectivity = PreferencesEntry.TEST_CONNECTIVITY
            .getValueBoolean(getController());
        boolean warnOnClose = PreferencesEntry.WARN_ON_CLOSE
            .getValueBoolean(getController());
        boolean filenamCheck = PreferencesEntry.FILE_NAME_CHECK
            .getValueBoolean(getController());
        boolean duplicateFolders = PreferencesEntry.DUPLICATE_FOLDER_USE
            .getValueBoolean(getController());
        boolean poorConnection = PreferencesEntry.WARN_POOR_QUALITY
            .getValueBoolean(getController());
        boolean cloudFull = PreferencesEntry.WARN_FULL_CLOUD
            .getValueBoolean(getController());
        askForFriendshipCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.ask_to_add_to_friends_if_node_becomes_member_of_folder"),
            askFriendship);
        askForFriendshipMessageCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.ask_to_add_friend_message"),
            askFriendshipMessage);
        warnOnCloseIfNotInSyncCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_close_if_not_in_sync"),
            warnOnClose);
        warnOnLimitedConnectivityCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_limited_connectivity"),
            testConnectivity);
        warnOnPossibleFilenameProblemsCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_possible_file_name_problems"),
            filenamCheck);
        warnOnDuplicateFoldersCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_duplicate_folders"),
            duplicateFolders);
        warnOnPoorConnectionQualityCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_poor_connection_quality"),
            poorConnection);
        warnIfCloudSpaceFullCB = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_ifcloud_space_full"),
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

            builder.add(warnOnCloseIfNotInSyncCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnLimitedConnectivityCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnPossibleFilenameProblemsCB, cc.xy(3, row));

            row += 2;
            builder.add(askForFriendshipCB, cc.xy(3, row));

            row += 2;
            builder.add(askForFriendshipMessageCB, cc.xy(3, row));

            row += 2;
            builder.add(showAutoCreatedFoldersBox, cc.xy(3, row));

            row += 2;
            builder.add(showPauseOptionsCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnDuplicateFoldersCB, cc.xy(3, row));

            row += 2;
            builder.add(warnOnPoorConnectionQualityCB, cc.xy(3, row));

            row += 2;
            builder.add(warnIfCloudSpaceFullCB, cc.xy(3, row));

            // //////////////////////////////////////
            // Notification stuff only below here //
            // //////////////////////////////////////

            row += 2;
            builder.addSeparator(Translation
                .getTranslation("preferences.dialog.dialogs.notifications"), cc
                .xyw(1, row, 3));

            row += 2;
            builder.add(showChatNotificationBox, cc.xy(3, row));

            row += 2;
            builder.add(showSystemNotificationBox, cc.xy(3, row));

            if (Constants.OPACITY_SUPPORTED) {
                row += 2;
                builder
                    .addLabel(
                        Translation
                            .getTranslation("preferences.dialog.dialogs.notification_translucency"),
                        cc.xy(1, row));
                builder.add(createNotificationTranslucentSpinnerPanel(), cc.xy(
                    3, row));
            }

            row += 2;
            builder
                .addLabel(
                    Translation
                        .getTranslation("preferences.dialog.dialogs.notification_delay"),
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

        // Write properties into core
        writeTrigger.triggerCommit();

        boolean testConnectivity = warnOnLimitedConnectivityCB.isSelected();
        boolean warnOnClose = warnOnCloseIfNotInSyncCB.isSelected();
        boolean filenamCheck = warnOnPossibleFilenameProblemsCB.isSelected();
        boolean askFriendship = askForFriendshipCB.isSelected();
        boolean askFriendshipMessage = askForFriendshipMessageCB.isSelected();
        boolean duplicateFolders = warnOnDuplicateFoldersCB.isSelected();
        boolean poorConnection = warnOnPoorConnectionQualityCB.isSelected();
        boolean fullColudSpace = warnIfCloudSpaceFullCB.isSelected();

        if (showChatNotificationBox != null) {
            applicationModel.getChatNotificationsValueModel().setValue(
                showChatNotificationBox.isSelected());
        }

        if (showSystemNotificationBox != null) {
            applicationModel.getSystemNotificationsValueModel().setValue(
                showSystemNotificationBox.isSelected());
        }

        PreferencesEntry.NOTIFICATION_TRANSLUCENT.setValue(getController(),
            notificationTranslucentSlider.getValue());

        PreferencesEntry.SHOW_AUTO_CREATED_FOLDERS.setValue(getController(),
            showAutoCreatedFoldersBox.isSelected());

        PreferencesEntry.SHOW_ASK_FOR_PAUSE.setValue(getController(),
            showPauseOptionsCB.isSelected());

        PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
            notificationDisplaySlider.getValue());

        PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN.setValue(
            getController(), askFriendship);
        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(getController(),
            askFriendshipMessage);
        PreferencesEntry.TEST_CONNECTIVITY.setValue(getController(),
            testConnectivity);
        PreferencesEntry.WARN_ON_CLOSE.setValue(getController(), warnOnClose);
        PreferencesEntry.WARN_FULL_CLOUD.setValue(getController(),
                fullColudSpace);
        PreferencesEntry.FILE_NAME_CHECK
            .setValue(getController(), filenamCheck);
        PreferencesEntry.DUPLICATE_FOLDER_USE.setValue(getController(),
            duplicateFolders);
        PreferencesEntry.WARN_POOR_QUALITY.setValue(getController(),
            poorConnection);
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
            Integer currentTranlucent = PreferencesEntry.NOTIFICATION_TRANSLUCENT
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
                        .getTranslation("preferences.dialog.dialogs.notification.preview.title"),
                    Translation
                        .getTranslation("preferences.dialog.dialogs.notification.preview.text"));

            // Reset
            PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
                currentDisplay);
            PreferencesEntry.NOTIFICATION_TRANSLUCENT.setValue(getController(),
                currentTranlucent);
        }
    }
}
