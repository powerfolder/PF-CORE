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
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.Trigger;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;

public class DialogsSettingsTab extends PFComponent implements PreferenceTab {

    private JCheckBox updateCheck;

    /** Show chat notifications */
    private JCheckBox showChatNotificationBox;

    /** Show system notifications */
    private JCheckBox showSystemNotificationBox;

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
            FormLayout layout = new FormLayout("pref",
                "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("4dlu, 7dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;

            builder.add(showChatNotificationBox, cc.xy(1, row));

            row += 2;
            builder.add(showSystemNotificationBox, cc.xy(1, row));

            row += 2;
            builder.add(updateCheck, cc.xy(1, row));

            row += 2;
            builder.add(warnOnCloseIfNotInSync, cc.xy(1, row));

            row += 2;
            builder.add(warnOnLimitedConnectivity, cc.xy(1, row));

            row += 2;
            builder.add(warnOnPossibleFilenameProblems, cc.xy(1, row));

            row += 2;
            builder.add(askForFriendship, cc.xy(1, row));

            row += 2;
            builder.add(askForFriendshipMessage, cc.xy(1, row));

            row += 2;
            builder.add(warnOnDuplicateFolders, cc.xy(1, row));

            panel = builder.getPanel();
        }
        return panel;
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

}
