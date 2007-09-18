package de.dal33t.powerfolder.util.ui;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.CustomSyncProfileDialog;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * Panel for displaying selected sync profile and opening the CustomSyncProfileDialog.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class SyncProfileSelectorPanel extends PFUIPanel {

    private JComboBox syncProfilesCombo;
    private JPanel panel;
    private ValueModel valueModel;
    private JButton syncProfileButton;
    private Folder updateableFolder;
    private SyncProfile customProfile;
    private boolean customInList;
    private boolean ignoreChanges;

    /**
     * Constructor.
     *
     * @param controller
     * @param syncProfile
     */
    public SyncProfileSelectorPanel(Controller controller, SyncProfile syncProfile) {
        super(controller);
        initComponents(syncProfile);
    }

    /**
     * Constructor.
     *
     * @param controller
     */
    public SyncProfileSelectorPanel(Controller controller) {
        this(controller, SyncProfile.MANUAL_DOWNLOAD);
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
     * Sets a Folder that will have its syncProfile updated if the syncProfile is changed on this panel.
     *
     * @param folder the Folder to update.
     */
    public void setUpdateableFolder(Folder folder) {
        updateableFolder = folder;
        customProfile = folder.getSyncProfile();
        configureCombo(folder.getSyncProfile());
    }

    /**
     * Initialize the visual components.
     *
     * @param syncProfile
     */
    private void initComponents(SyncProfile syncProfile) {

        syncProfilesCombo = new JComboBox();
        configureCombo(syncProfile);

        syncProfilesCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                udateSyncProfile();
            }
        });
        syncProfileButton = new JButton("...");
        syncProfileButton.addActionListener(new MyActionListener());

        valueModel = new ValueHolder();
        valueModel.addValueChangeListener(new MyPropertyChangeListener());
        valueModel.setValue(syncProfile);
    }

    private void udateSyncProfile() {

        // Don't update if the combo items are being re-entered.
        if (!ignoreChanges) {
            int i = syncProfilesCombo.getSelectedIndex();
            if (i >= 0 && updateableFolder != null) {
                if (customInList) {
                    if (syncProfilesCombo.getSelectedIndex() == 0) {
                        updateableFolder.setSyncProfile(customProfile);
                    } else {
                        updateableFolder.setSyncProfile(SyncProfile.DEFAULT_SYNC_PROFILES[i + 1]);
                    }
                } else {
                    updateableFolder.setSyncProfile(SyncProfile.DEFAULT_SYNC_PROFILES[i]);
                }
            }
        }
    }

    private void configureCombo(SyncProfile syncProfile) {

        // Don't process itemStateChange events.
        ignoreChanges = true;
        syncProfilesCombo.removeAllItems();
        customInList = false;

        if (syncProfile.isCustom()) {
            syncProfilesCombo.addItem(Translation.getTranslation(SyncProfile.getTranslationId(SyncProfile.CUSTOM_SYNC_PROFILE_ID)));
            customInList = true;
        }
        
        for (SyncProfile aSyncProfile : SyncProfile.DEFAULT_SYNC_PROFILES) {
            syncProfilesCombo.addItem(Translation.getTranslation(aSyncProfile.getTranslationId()));
        }

        if (syncProfile.isCustom()) {
            syncProfilesCombo.setSelectedIndex(0);
        } else {
            for (int i = 0; i < SyncProfile.DEFAULT_SYNC_PROFILES.length; i++) {
                if (SyncProfile.DEFAULT_SYNC_PROFILES[i].equals(syncProfile)) {
                    syncProfilesCombo.setSelectedIndex(i);
                }
            }
        }

        // Begin processing itemStateChange events again.
        ignoreChanges = false;
    }

    /**
     * Builds the visible panel.
     */
    private void buildPanel() {
        FormLayout layout = new FormLayout("pref:grow, 4dlu, pref, 4dlu, pref", "pref");
        panel = new JPanel(layout);

        CellConstraints cc = new CellConstraints();

        panel.add(syncProfilesCombo, cc.xy(1, 1));
        panel.add(syncProfileButton, cc.xy(3, 1));

        JLabel helpLabel = Help.createHelpLinkLabel("help", "node/syncoptions");
        panel.add(helpLabel, cc.xy(5, 1));

    }

    /**
     * Called to allow the user to cancel changes.
     *
     * @param folder The folder the user wants to change the SyncProfile. Folder may be null
     * for not yet created folders.
     * @param syncProfile The new profile to apply
     * @return true if the changes were applied (Calling objects may consider to undo
     * their selection if this returns false).
     */
    public static boolean vetoableFolderSyncProfileChange(Folder folder, SyncProfile syncProfile) {

        // See if anything has changed.
        if (folder != null && folder.getSyncProfile().equals(syncProfile)) {
    		// Okay
            return true;
        }

        if (isOkToSwitchToProfile(syncProfile)) {
    		if (folder != null) {
    			folder.setSyncProfile(syncProfile);
            }
    		return true;
    	}
		return false;
    }

    /**
     * Shows a warning if the syncprofile will sync deletions.
     *
     * @param syncProfile
     *            the syncprofile selected
     * @return true only if the profile doesn't sync deletion or the user
     *         approved it
     */
    public static boolean isOkToSwitchToProfile(SyncProfile syncProfile) {

        if (!syncProfile.isSyncDeletion()) {
            return true;
        }

        // Show warning if user wants to switch to a mode
        String profName = Translation.getTranslation(syncProfile
            .getTranslationId());
        return JOptionPane.showConfirmDialog(null,
            Translation
                .getTranslation("synchronisation.warning.automatic_deletions_notice"),
            Translation.getTranslation("synchronisation.notice.title",
                profName), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) ==
                	JOptionPane.OK_OPTION;
    }

    /**
     * Sets the syncProfile on the panel.
     *
     * @param syncProfile the SyncProfile
     * @param updateFolder whether to update the folder.
     *          Should be true only when the profile is changed by the CustomSyncProfileDialog
     */
    public void setSyncProfile(SyncProfile syncProfile, boolean updateFolder) {
        valueModel.setValue(syncProfile);
        if (updateFolder) {
            if (updateableFolder != null) {
                updateableFolder.setSyncProfile(syncProfile);
            }
            customProfile = syncProfile;
        }
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
     * Adds a value change listener for the sync profile model.
     * This allows things to react to changes.
     *
     * @param propertyChangeListener
     */
    public void addModelValueChangeListener(PropertyChangeListener propertyChangeListener) {
        valueModel.addValueChangeListener(propertyChangeListener);
    }

    /**
     * Listener class to update the displayed profile text on a change.
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
         public void propertyChange(PropertyChangeEvent evt) {
             SyncProfile syncProfile = (SyncProfile) evt.getNewValue();
             configureCombo(syncProfile);
         }
    }

    /**
     * Opens a CustomSyncProfileDialog to change the profile configuration.
     */
    private void openCustomSyncProfileDialog() {
        CustomSyncProfileDialog customSyncProfileDialog = new CustomSyncProfileDialog(getController(), this);
        customSyncProfileDialog.getUIComponent().setVisible(true);
    }

    /**
     * Action listener to open the CustomSyncProfileDialog when the button is clicked.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            openCustomSyncProfileDialog();
        }
    }
}
