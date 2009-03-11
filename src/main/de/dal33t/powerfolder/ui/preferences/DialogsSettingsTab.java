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

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class DialogsSettingsTab extends PFComponent implements PreferenceTab {

    private JCheckBox updateCheck;

    /** Show chat notifications */
    private JCheckBox showChatNotificationBox;

    /** Show system notifications */
    private JCheckBox showSystemNotificationBox;

    private SpinnerNumberModel notificationDisplayModel;
    private JSpinner notificationDisplaySpinner;

    /** Ask to add to friends if user becomes member of a folder */
    private JCheckBox askForFriendship;

    /** Add personal message with freindship status change */
    private JCheckBox askForFriendshipMessage;

    /** warn on limited connectivity */
    private JCheckBox warnOnLimitedConnectivity;

    /** warn on posible filename problems */
    private JCheckBox warnOnPossibleFilenameProblems;

    /** warn on close program if a folder is still syncing */
    private JCheckBox warnOnCloseIfNotInSync;

    /** warn if changing profile for multiple folders */
    private JCheckBox warnOnDuplicateFolders;

    private JPanel panel;

    private boolean needsRestart;

    // The triggers the writing into core
    private Trigger writeTrigger;

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

        writeTrigger = new Trigger();

        // Show chat notifications when minimized
        ValueModel scnModel = new ValueHolder(
            ConfigurationEntry.SHOW_CHAT_NOTIFICATIONS
                .getValueBoolean(getController()));
        showChatNotificationBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(scnModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.show_chat_notifications"));

        // Show system notifications when minimized
        ValueModel ssnModel = new ValueHolder(
            ConfigurationEntry.SHOW_SYSTEM_NOTIFICATIONS
                .getValueBoolean(getController()));
        showSystemNotificationBox = BasicComponentFactory.createCheckBox(
            new BufferedValueModel(ssnModel, writeTrigger), Translation
                .getTranslation("preferences.dialog.show_system_notifications"));

        notificationDisplayModel = new SpinnerNumberModel(
                PreferencesEntry.NOTIFICATION_DISPLAY.getValueInt(
                        getController()).intValue(),
                3, 30, 1);
        notificationDisplaySpinner = new JSpinner(notificationDisplayModel);

        boolean checkForUpdate = PreferencesEntry.CHECK_UPDATE
            .getValueBoolean(getController());
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
        updateCheck = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.check_for_program_updates"),
            checkForUpdate);
        askForFriendship = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.ask_to_add_to_friends_if_node_becomes_member_of_folder"),
            askFriendship);
        askForFriendshipMessage = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.ask_to_add_friend_message"),
            askFriendshipMessage);
        warnOnCloseIfNotInSync = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_close_if_not_in_sync"),
            warnOnClose);
        warnOnLimitedConnectivity = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_limited_connectivity"),
            testConnectivity);
        warnOnPossibleFilenameProblems = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_possible_file_name_problems"),
            filenamCheck);
        warnOnDuplicateFolders = new JCheckBox(
            Translation
                .getTranslation("preferences.dialog.dialogs.warn_on_duplicate_folders"),
            duplicateFolders);
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("pref, 3dlu, pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;

            builder.add(updateCheck, cc.xy(3, row));

            row += 2;
            builder.add(warnOnCloseIfNotInSync, cc.xy(3, row));

            row += 2;
            builder.add(warnOnLimitedConnectivity, cc.xy(3, row));

            row += 2;
            builder.add(warnOnPossibleFilenameProblems, cc.xy(3, row));

            row += 2;
            builder.add(askForFriendship, cc.xy(3, row));

            row += 2;
            builder.add(askForFriendshipMessage, cc.xy(3, row));

            row += 2;
            builder.add(warnOnDuplicateFolders, cc.xy(3, row));

            ////////////////////////////////////////
            // Notification stuff only below here //
            ////////////////////////////////////////

            row += 2;
            builder.addSeparator(Translation.getTranslation(
                    "preferences.dialog.dialogs.notifications"),
                    cc.xyw(1, row, 3));

            row += 2;
            builder.add(showChatNotificationBox, cc.xy(3, row));

            row += 2;
            builder.add(showSystemNotificationBox, cc.xy(3, row));

            row += 2;
            builder.addLabel(Translation.getTranslation(
                    "preferences.dialog.dialogs.notification_delay"),
                    cc.xy(1, row));
            builder.add(createNotificationSpinnerPanel(), cc.xy(3, row));

            panel = builder.getPanel();
        }
        return panel;
    }

    private Component createNotificationSpinnerPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref, 3dlu, pref, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(notificationDisplaySpinner, cc.xy(1, 1));
        builder.addLabel(Translation.getTranslation("general.seconds.lower"),
                cc.xy(3, 1));
        JButton preview = new JButton("Preview");
        preview.addActionListener(new MyActionListener());
        builder.add(preview, cc.xy(5, 1));
        return builder.getPanel();
    }

    /**
     * Saves the dialogs settings.
     */
    public void save() {

        // Write properties into core
        writeTrigger.triggerCommit();
        
        boolean checkForUpdate = updateCheck.isSelected();
        boolean testConnectivity = warnOnLimitedConnectivity.isSelected();
        boolean warnOnClose = warnOnCloseIfNotInSync.isSelected();
        boolean filenamCheck = warnOnPossibleFilenameProblems.isSelected();
        boolean askFriendship = askForFriendship.isSelected();
        boolean askFriendshipMessage = askForFriendshipMessage.isSelected();
        boolean duplicateFolders = warnOnDuplicateFolders.isSelected();

        if (showChatNotificationBox != null) {
            // Show Notifications
            ConfigurationEntry.SHOW_CHAT_NOTIFICATIONS.setValue(getController(),
                    Boolean.toString(showChatNotificationBox.isSelected()));
        }

        if (showSystemNotificationBox != null) {
            // Show Notifications
            ConfigurationEntry.SHOW_SYSTEM_NOTIFICATIONS.setValue(getController(),
                    Boolean.toString(showSystemNotificationBox.isSelected()));
        }

        PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(),
                notificationDisplayModel.getNumber().intValue());
        
        PreferencesEntry.CHECK_UPDATE.setValue(getController(), checkForUpdate);
        PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN.setValue(
            getController(), askFriendship);
        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(
            getController(), askFriendshipMessage);
        PreferencesEntry.TEST_CONNECTIVITY.setValue(getController(),
            testConnectivity);
        PreferencesEntry.WARN_ON_CLOSE.setValue(getController(), warnOnClose);
        PreferencesEntry.FILE_NAME_CHECK
            .setValue(getController(), filenamCheck);
        PreferencesEntry.DUPLICATE_FOLDER_USE
            .setValue(getController(), duplicateFolders);
    }

    /**
     * Show a preview of the notification.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Integer current = PreferencesEntry.NOTIFICATION_DISPLAY.getValueInt(getController());
            PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(), notificationDisplayModel.getNumber().intValue());
            getController().getUIController().notifyMessage(
                    Translation.getTranslation("preferences.dialog.dialogs.notification.preview.title"),
                    Translation.getTranslation("preferences.dialog.dialogs.notification.preview.text"),
                    true);
            PreferencesEntry.NOTIFICATION_DISPLAY.setValue(getController(), current);
        }
    }
}
