package de.dal33t.powerfolder.ui.dialog;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;

/**
 * Dialog for changing the profile configuration.
 * User can select a default profile and then adjust the configuration.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class CustomSyncProfileDialog extends BaseDialog {

    private JComboBox syncProfilesCombo;
    private JCheckBox autoDownloadFromFriendsBox;
    private JCheckBox autoDownloadFromOthersBox;
    private JCheckBox syncDeletionWithFriendsBox;
    private JCheckBox syncDeletionWithOthersBox;
    private SpinnerNumberModel scanMinutesModel;
    private JSpinner scanMinutesSpinner;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;

    /**
     * Constructor.
     *
     * @param controller
     * @param syncProfileSelectorPanel
     */
    public CustomSyncProfileDialog(Controller controller, SyncProfileSelectorPanel syncProfileSelectorPanel) {
        super(controller, true);
        this.syncProfileSelectorPanel = syncProfileSelectorPanel;
    }

    /**
     * Gets the title of the dialog.
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.customsync.title");
    }

    /**
     * Gets the icon for the dialog.
     * @todo need a better icon.
     *
     * @return
     */
    protected Icon getIcon() {
        return Icons.NEW_FOLDER;
    }

    /**
     * Creates the visual component.
     *
     * @return
     */
    protected Component getContent() {
        initComponents();
        FormLayout layout = new FormLayout("right:pref, 4dlu, pref, 4dlu, pref:grow, 4dlu, pref",
            "pref, 14dlu, pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.text")), cc.xyw(1, 1, 7));

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.syncprofilescombo")), cc.xy(1, 3));
        builder.add(syncProfilesCombo, cc.xyw(3, 3, 3));
        JLabel helpLabel = Help.createHelpLinkLabel("help", "node/syncoptions");
        builder.add(helpLabel, cc.xy(7, 3));

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.autodownloadfromfriends")), cc.xy(1, 5));
        builder.add(autoDownloadFromFriendsBox, cc.xy(3, 5));
        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.autodownloadfromother")), cc.xy(1, 7));
        builder.add(autoDownloadFromOthersBox, cc.xy(3, 7));
        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.syncdeletionwithfriends")), cc.xy(1, 9));
        builder.add(syncDeletionWithFriendsBox, cc.xy(3, 9));
        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.syncdeletionwithothers")), cc.xy(1, 11));
        builder.add(syncDeletionWithOthersBox, cc.xy(3, 11));

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.minutesbetweenscans")), cc.xy(1, 13));
        builder.add(scanMinutesSpinner, cc.xy(3, 13));

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        // Combo
        syncProfilesCombo = new JComboBox();
        syncProfilesCombo.addItem(Translation.getTranslation("syncprofile." + SyncProfile.CUSTOM_SYNC_PROFILE_ID + ".name"));
        for (SyncProfile syncProfile : SyncProfile.DEFAULT_SYNC_PROFILES) {
            syncProfilesCombo.addItem(Translation.getTranslation(syncProfile.getTranslationId()));
        }
        syncProfilesCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                preselectDefaults();
            }
        });

        // Configuration components
        ChangeListener cl = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                preselectCombo();
            }
        };
        autoDownloadFromFriendsBox = new JCheckBox();
        autoDownloadFromFriendsBox.addChangeListener(cl);
        autoDownloadFromOthersBox = new JCheckBox();
        autoDownloadFromOthersBox.addChangeListener(cl);
        syncDeletionWithFriendsBox = new JCheckBox();
        syncDeletionWithFriendsBox.addChangeListener(cl);
        syncDeletionWithOthersBox = new JCheckBox();
        syncDeletionWithOthersBox.addChangeListener(cl);

        scanMinutesModel = new SpinnerNumberModel(0, 0, 9999, 1);
        scanMinutesSpinner = new JSpinner(scanMinutesModel);
        scanMinutesModel.addChangeListener(cl);

        // Initialise settings.
        SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();
        autoDownloadFromFriendsBox.setSelected(syncProfile.isAutoDownloadFromFriends());
        autoDownloadFromOthersBox.setSelected(syncProfile.isAutoDownloadFromOthers());
        syncDeletionWithFriendsBox.setSelected(syncProfile.isSyncDeletionWithFriends());
        syncDeletionWithOthersBox.setSelected(syncProfile.isSyncDeletionWithOthers());
        scanMinutesModel.setValue(syncProfile.getMinutesBetweenScans());
    }

    /**
     * Selects the combo based on the current configuration settings.
     */
    private void preselectCombo() {
        SyncProfile syncProfile = new SyncProfile(autoDownloadFromFriendsBox.isSelected(),
                autoDownloadFromOthersBox.isSelected(),
                syncDeletionWithFriendsBox.isSelected(),
                syncDeletionWithOthersBox.isSelected(),
                scanMinutesModel.getNumber().intValue());

        // Try to find a matching default profile.
        for (int i = 0; i < SyncProfile.DEFAULT_SYNC_PROFILES.length; i++) {
            if (SyncProfile.DEFAULT_SYNC_PROFILES[i].equals(syncProfile)) {

                // Add one because zeroth element is 'Custom Profile'
                syncProfilesCombo.setSelectedIndex(i + 1);
                return;
            }
        }

        // Custom profile
        syncProfilesCombo.setSelectedIndex(0);        
    }

    /**
     * Sets configuration settings based on the current combo selection.
     */
    private void preselectDefaults() {
        if (syncProfilesCombo.getSelectedIndex() == 0) {
            // Custom. Do nothing
        } else {
            // Subtract one because zeroth element is 'Custom Profile'
            int index = syncProfilesCombo.getSelectedIndex() - 1;
            SyncProfile syncProfile = SyncProfile.DEFAULT_SYNC_PROFILES[index];
            autoDownloadFromFriendsBox.setSelected(syncProfile.isAutoDownloadFromFriends());
            autoDownloadFromOthersBox.setSelected(syncProfile.isAutoDownloadFromOthers());
            syncDeletionWithFriendsBox.setSelected(syncProfile.isSyncDeletionWithFriends());
            syncDeletionWithOthersBox.setSelected(syncProfile.isSyncDeletionWithOthers());
            scanMinutesModel.setValue(syncProfile.getMinutesBetweenScans());
        }
    }

    /**
     * The OK / Cancel buttons.
     * @return
     */
    protected Component getButtonBar() {

        // Buttons
       JButton okButton = createOKButton(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               okPressed();
           }
       });

       JButton cancelButton = createCancelButton(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               cancelPressed();
           }
       });


       return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    // Methods fo FolderPreferencesPanel **************************************

    /**
     * If user clicks okay, update the profile in the selector panel.
     */
    private void okPressed() {
        SyncProfile syncProfile = new SyncProfile(autoDownloadFromFriendsBox.isSelected(),
                autoDownloadFromOthersBox.isSelected(),
                syncDeletionWithFriendsBox.isSelected(),
                syncDeletionWithOthersBox.isSelected(),
                scanMinutesModel.getNumber().intValue());
        syncProfileSelectorPanel.setSyncProfile(syncProfile, true);
        close();
    }

    /**
     * User does not want to commit any change.
     */
    private void cancelPressed() {
        close();
    }
}
