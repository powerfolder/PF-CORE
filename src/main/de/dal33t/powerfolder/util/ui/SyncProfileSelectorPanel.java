package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.ArrayList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.dialog.CreateEditSyncProfileDialog;
import de.dal33t.powerfolder.ui.dialog.DeleteSyncProfileDialog;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;

/**
 * Panel for displaying selected sync profile and opening the
 * CustomSyncProfileDialog.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class SyncProfileSelectorPanel extends PFUIPanel {

    private JComboBox syncProfilesCombo;
    private JPanel panel;
    private ValueModel valueModel;
    private Folder updateableFolder;
    private boolean ignoreChanges;
    private JLabel helpLabel;
    private CreateAction createAction;
    private EditAction editAction;
    private DeleteAction deleteAction;
    private boolean enabled = true;
    private boolean settingSyncProfile;

    public SyncProfileSelectorPanel(Controller controller,
        SyncProfile syncProfile)
    {
        super(controller);
        initComponents(syncProfile);
    }

    public SyncProfileSelectorPanel(Controller controller) {
        this(controller, SyncProfile.AUTOMATIC_SYNCHRONIZATION);
    }

    /**
     * Builds panel and returns the component.
     * 
     * @return
     */
    public Component getUIComponent() {
        if (panel == null) {
            buildPanel();
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

        createAction = new CreateAction(getController());
        editAction = new EditAction(getController());
        deleteAction = new DeleteAction(getController());

        syncProfilesCombo = new JComboBox();
        syncProfilesCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                udateSyncProfile();
            }
        });
        configureCombo(syncProfile);

        valueModel = new ValueHolder();
        valueModel.setValue(syncProfile);

        helpLabel = Help.createWikiLinkLabel(Translation
            .getTranslation("general.what_is_this"), "Sync_Profiles");

        // Warn if changing to delete type profiles
        addModelValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!isOkToSwitchToProfile((SyncProfile) evt.getNewValue())) {
                    setSyncProfile((SyncProfile) evt.getOldValue(), false);
                }
            }
        });
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

                // Configure edit / delete for selection.
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

            editAction.setEnabled(enabled && profile != null
                && profile.isCustom());
            deleteAction.setEnabled(enabled && profile != null
                && profile.isCustom());
            createAction.setEnabled(enabled);
        } else {
            editAction.setEnabled(false);
            deleteAction.setEnabled(false);
            createAction.setEnabled(false);
        }
    }

    public void configureCombo(SyncProfile syncProfile) {

        // Don't process itemStateChange events.
        ignoreChanges = true;
        syncProfilesCombo.removeAllItems();
        for (SyncProfile aSyncProfile : SyncProfile.getSyncProfilesCopy()) {
            syncProfilesCombo.addItem(aSyncProfile.getProfileName());
            if (syncProfile.equals(aSyncProfile)) {
                syncProfilesCombo
                    .setSelectedItem(aSyncProfile.getProfileName());
            }
        }

        // Configure edit / delete for initial selection.
        int i = syncProfilesCombo.getSelectedIndex();
        if (i >= 0) {
            SyncProfile profile = SyncProfile.getSyncProfilesCopy().get(i);
            editAction.setEnabled(profile.isCustom());
            deleteAction.setEnabled(profile.isCustom());
        }

        // Begin processing itemStateChange events again.
        ignoreChanges = false;
    }

    /**
     * Builds the visible panel.
     */
    private void buildPanel() {
        FormLayout layout = new FormLayout("pref:grow, 4dlu, pref",
            "pref, 4dlu, pref");
        panel = new JPanel(layout);

        CellConstraints cc = new CellConstraints();

        panel.add(syncProfilesCombo, cc.xy(1, 1));
        panel.add(helpLabel, cc.xy(3, 1));
        panel.add(createButtonBar(), cc.xyw(1, 3, 3));
    }

    private JPanel createButtonBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(createAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(editAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(deleteAction));
        bar.setOpaque(false);

        return bar.getPanel();
    }

    /**
     * Shows a warning if the syncprofile will sync deletions.
     * 
     * @param syncProfile
     *            the syncprofile selected
     * @return true only if the profile doesn't sync deletion or the user
     *         approved it
     */
    public boolean isOkToSwitchToProfile(SyncProfile syncProfile) {

        if (!syncProfile.isSyncDeletion()) {
            return true;
        }

        // If the sync profile is being changed, do not ask.
        // Only interested with manual changes to dropdown.
        if (settingSyncProfile) {
            return true;
        }

        // Show warning if user wants to switch to a mode
        String profName = syncProfile.getProfileName();
        return JOptionPane
            .showConfirmDialog(
                null,
                Translation
                    .getTranslation("synchronisation.warning.automatic_deletions_notice"),
                Translation.getTranslation("synchronisation.notice.title",
                    profName), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
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

        settingSyncProfile = true;
        valueModel.setValue(syncProfile);

        if (updateFolder) {
            if (updateableFolder != null) {
                updateableFolder.setSyncProfile(syncProfile);
            }
        }

        // Always reconfigure combo after setSyncProfile.
        // Cannot use a change listener becaue the sync profile name may have
        // changed, which is not a listen-able 'event' :-(
        configureCombo(syncProfile);
        settingSyncProfile = false;
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
        enabled = enable;
        enableButtons();
        syncProfilesCombo.setEnabled(enable);
        helpLabel.setVisible(enable);
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
            createEditSyncProfileDialog.getUIComponent().setVisible(true);
        }
    }

    private void deleteProfile() {
        List<Folder> folders = usedFolders();

        int response = 0; // Default to dialog OK
        if (folders.size() >= 2) {
            response = showDuplicates(folders,
                "dialog.synchronization.duplicate.delete");
        }

        if (response == 0) { // OK
            DeleteSyncProfileDialog deleteProfileDialog = new DeleteSyncProfileDialog(
                getController(), this);
            deleteProfileDialog.getUIComponent().setVisible(true);
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

        if (!PreferencesEntry.DUPLICATE_FOLDER_USE
            .getValueBoolean(getController()))
        {
            return 0;
        }

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
        NeverAskAgainResponse response = DialogFactory.genericDialog(
            getController().getUIController().getMainFrame().getUIComponent(),
            title, message, new String[]{"OK", "Cancel"}, 0,
            GenericDialogType.WARN, Translation
                .getTranslation("general.neverAskAgain"));
        if (response.isNeverAskAgain()) {
            PreferencesEntry.DUPLICATE_FOLDER_USE.setValue(getController(),
                false);
        }
        return response.getButtonIndex();
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

    private class EditAction extends BaseAction {

        private EditAction(Controller controller) {
            super("dialog.create_edit_profile.edit_button", controller);
        }

        public void actionPerformed(ActionEvent e) {
            openCustomSyncProfileDialog(false);
        }
    }

    private class DeleteAction extends BaseAction {

        private DeleteAction(Controller controller) {
            super("dialog.create_edit_profile.delete_button", controller);
        }

        public void actionPerformed(ActionEvent e) {
            deleteProfile();
        }
    }

    private class CreateAction extends BaseAction {

        private CreateAction(Controller controller) {
            super("dialog.create_edit_profile.create_button", controller);
        }

        public void actionPerformed(ActionEvent e) {
            openCustomSyncProfileDialog(true);
        }
    }

}
