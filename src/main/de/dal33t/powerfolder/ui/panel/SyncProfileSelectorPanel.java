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
package de.dal33t.powerfolder.ui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.security.ChangeTransferModePermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.CreateEditSyncProfileDialog;
import de.dal33t.powerfolder.ui.dialog.DeleteSyncProfileDialog;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.model.BoundPermission;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;

/**
 * Panel for displaying selected sync profile and opening the
 * CustomSyncProfileDialog.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class SyncProfileSelectorPanel extends PFUIComponent {

    private JComboBox<String> syncProfilesCombo;
    private JPanel panel;
    private ValueModel valueModel;
    private Folder updateableFolder;
    private boolean ignoreChanges;
    private JButtonMini configureButton;
    private JButtonMini deleteButton;

    @SuppressWarnings("unused")
    // Held to prevent gc from collecting it.
    private BoundPermission changeModePermission;

    public SyncProfileSelectorPanel(Controller controller,
        SyncProfile syncProfile)
    {
        super(controller);
        initComponents(syncProfile);
    }

    public SyncProfileSelectorPanel(Controller controller) {
        this(controller, SyncProfile.getDefault(controller));
    }

    /**
     * Builds panel and returns the component.
     *
     * @return the component.
     */
    public Component getUIComponent() {
        if (panel == null) {
            buildPanel();

            changeModePermission = new BoundPermission(getController(),
                ChangeTransferModePermission.INSTANCE)
            {
                @Override
                public void hasPermission(boolean hasPermission) {
                    setEnabled(hasPermission);
                }
            };
        }
        return panel;
    }

    /**
     * Sets a Folder that will have its syncProfile updated if the syncProfile
     * is changed on this panel.
     *
     * @param folder
     *            the Folder to update.
     */
    public void setUpdateableFolder(Folder folder) {
        updateableFolder = folder;
        configureCombo(folder.getSyncProfile());
    }

    /**
     * Initialize the visual components.
     *
     * @param syncProfile
     */
    private void initComponents(SyncProfile syncProfile) {

        syncProfilesCombo = new JComboBox<>();
        syncProfilesCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                udateSyncProfile();
            }
        });

        valueModel = new ValueHolder();
        valueModel.setValue(syncProfile);

        configureButton = new JButtonMini(
            new MyConfigureAction(getController()));
        deleteButton = new JButtonMini(new MyDeleteAction(getController()));

        configureCombo(syncProfile);

        // Warn if changing to delete type profiles
        addModelValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isOkToSwitchToProfile((SyncProfile) evt.getNewValue())) {
                    setSyncProfile((SyncProfile) evt.getOldValue(), false);
                }
            }
        });
        enableButtons();
    }

    private void udateSyncProfile() {
        // Don't update if the combo items are being re-entered.
        if (!ignoreChanges) {
            int i = syncProfilesCombo.getSelectedIndex();
            if (i >= 0) {
                SyncProfile profile = SyncProfile.getSyncProfilesCopy().get(i);

                valueModel.setValue(profile);
                if (updateableFolder != null) {
                    updateableFolder.setSyncProfile(profile);
                }

                // Configure delete for selection.
                enableButtons();
            }
        }
    }

    private void enableButtons() {
        int i = syncProfilesCombo.getSelectedIndex();
        if (i >= 0) {
            SyncProfile profile = SyncProfile.getSyncProfilesCopy().get(i);

            valueModel.setValue(profile);
            if (updateableFolder != null) {
                updateableFolder.setSyncProfile(profile);
            }
        }

        SyncProfile profile = (SyncProfile) valueModel.getValue();
        deleteButton.setEnabled(profile != null && profile.isCustom());
        configureButton.setEnabled(profile != null);
    }

    public void configureCombo(SyncProfile syncProfile) {

        // Don't process itemStateChange events.
        ignoreChanges = true;
        syncProfilesCombo.removeAllItems();
        for (SyncProfile aSyncProfile : SyncProfile.getSyncProfilesCopy()) {
            syncProfilesCombo.addItem(aSyncProfile.getName());
            if (syncProfile.equals(aSyncProfile)) {
                syncProfilesCombo.setSelectedItem(aSyncProfile.getName());
            }
        }

        // Configure edit / delete for initial selection.
        enableButtons();

        // Begin processing itemStateChange events again.
        ignoreChanges = false;
    }

    /**
     * Builds the visible panel.
     */
    private void buildPanel() {
        int iconHeight = configureButton.getIcon().getIconHeight();
        FormLayout layout = new FormLayout(
            "140dlu, 3dlu, pref, pref, pref, pref:grow", "max(pref;"
                + iconHeight + ')');
        panel = new JPanel(layout);

        CellConstraints cc = new CellConstraints();

        panel.add(syncProfilesCombo, cc.xy(1, 1));
        panel.add(configureButton, cc.xy(3, 1));
        panel.add(deleteButton, cc.xy(4, 1));
        panel.add(Help.createWikiLinkButton(getController(),
            WikiLinks.TRANSFER_MODES), cc.xy(5, 1));
    }

    /**
     * Shows a warning if the syncProfile will sync deletions.
     *
     * @param syncProfile
     *            the syncProfile selected
     * @return true only if the profile doesn't sync deletion or the user
     *         approved it
     */
    public boolean isOkToSwitchToProfile(SyncProfile syncProfile) {
        // Don't hassel the user with too many warnings. It's ok! We got an
        // archive!
        return true;
    }

    /**
     * Sets the syncProfile on the panel.
     *
     * @param syncProfile
     *            the SyncProfile
     * @param updateFolder
     *            whether to update the folder. Should be true only when the
     *            profile is changed by the CustomSyncProfileDialog
     */
    public void setSyncProfile(SyncProfile syncProfile, boolean updateFolder) {
        valueModel.setValue(syncProfile);

        if (updateFolder) {
            if (updateableFolder != null) {
                updateableFolder.setSyncProfile(syncProfile);

                // Also need to persist for any other folders that have this
                // profile. This ensures that any minor changes are persisted
                // for all affected folders.
                for (Folder folder : getController().getFolderRepository()
                    .getFolders())
                {
                    if (folder.getSyncProfile().equals(syncProfile)
                        && !updateableFolder.equals(folder))
                    {
                        folder.setSyncProfile(syncProfile);
                    }
                }
            }
        }

        // Always reconfigure combo after setSyncProfile.
        // Cannot use a change listener becaue the sync profile name may have
        // changed, which is not a listen-able 'event' :-(
        configureCombo(syncProfile);
    }

    /**
     * Gets the SyncProfile
     *
     * @return the sync profile
     */
    public SyncProfile getSyncProfile() {
        return (SyncProfile) valueModel.getValue();
    }

    /**
     * Adds a value change listener for the sync profile model. This allows
     * things to react to changes.
     *
     * @param propertyChangeListener
     */
    public void addModelValueChangeListener(
        PropertyChangeListener propertyChangeListener)
    {
        valueModel.addValueChangeListener(propertyChangeListener);
    }

    /**
     * Enable the components of the panel.
     *
     * @param enable
     */
    public void setEnabled(boolean enable) {
        enableButtons();
        syncProfilesCombo.setEnabled(enable);
        configureButton.setVisible(enable);
        deleteButton.setVisible(enable);
    }

    /**
     * Opens a CustomSyncProfileDialog to change the profile configuration.
     */
    private void openCustomSyncProfileDialog(boolean create) {

        // If edit, count the number of folders that use this profile,
        // and warn if more than one.
        int response = 0; // Default to dialog OK
        if (!create) {
            List<Folder> folders = usedFolders();
            if (folders.size() >= 2) {
                response = showDuplicates(folders,
                    "dialog.synchronization.duplicate.edit");
            }
        }
        if (response == 0) { // OK
            CreateEditSyncProfileDialog createEditSyncProfileDialog = new CreateEditSyncProfileDialog(
                getController(), this, create);
            createEditSyncProfileDialog.open();
        }
    }

    public void deleteProfile() {
        List<Folder> folders = usedFolders();

        int response = 0; // Default to dialog OK
        if (folders.size() >= 2) {
            response = showDuplicates(folders,
                "dialog.synchronization.duplicate.delete");
        }

        if (response == 0) { // OK
            DeleteSyncProfileDialog deleteProfileDialog = new DeleteSyncProfileDialog(
                getController(), this);
            deleteProfileDialog.open();
        }

    }

    /**
     * Show duplicate folders to user, warning.
     *
     * @param folders
     * @param messageKey
     * @return
     */
    private int showDuplicates(List<Folder> folders, String messageKey) {

        String title = Translation
            .getTranslation("dialog.synchronization.duplicate.title");
        StringBuilder sb = new StringBuilder();
        int local = 0;
        for (Folder folder : folders) {
            sb.append("    ");
            if (local++ >= 10) {
                // Too many folders - enough!!!
                sb.append(Translation.getTranslation("general.more.lower_case")
                    + "...\n");
                break;
            }
            sb.append(folder.getName() + '\n');
        }

        String message = Translation
            .getTranslation("dialog.synchronization.duplicate.use")
            + "\n\n"
            + sb.toString()
            + '\n'
            + Translation.getTranslation(messageKey);
        return DialogFactory.genericDialog(
                getController(), title, message, new String[]{"OK", "Cancel"}, 0,
                GenericDialogType.WARN);
    }

    private List<Folder> usedFolders() {
        List<Folder> folders = new ArrayList<Folder>();
        SyncProfile syncProfile = getSyncProfile();
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            if (folder.getSyncProfile().equals(syncProfile)) {
                folders.add(folder);
            }
        }
        return folders;
    }

    private class MyConfigureAction extends BaseAction {

        private MyConfigureAction(Controller controller) {
            super("action_configure_transfer_mode", controller);
        }

        public void actionPerformed(ActionEvent e) {
            SyncProfile syncProfile = getSyncProfile();
            if (syncProfile != null) {
                openCustomSyncProfileDialog(!syncProfile.isCustom());
            }
        }
    }

    private class MyDeleteAction extends BaseAction {

        private MyDeleteAction(Controller controller) {
            super("action_delete_transfer_mode", controller);
        }

        public void actionPerformed(ActionEvent e) {
            SyncProfile syncProfile = getSyncProfile();
            if (syncProfile != null) {
                deleteProfile();
            }
        }
    }
}
