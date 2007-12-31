package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

/**
 * Dialog for changing the profile configuration. User can select a default
 * profile and then adjust the configuration.
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class CustomSyncProfileDialog extends BaseDialog implements ActionListener {

    private JComboBox syncProfilesCombo;
    private JCheckBox autoDownloadFromFriendsBox;
    private JCheckBox autoDownloadFromOthersBox;
    private JCheckBox syncDeletionWithFriendsBox;
    private JCheckBox syncDeletionWithOthersBox;
    private SpinnerNumberModel scanMinutesModel;
    private JSpinner scanMinutesSpinner;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JLabel scanInfoLabel;
    private JRadioButton regularRadioButton;
    private JRadioButton dailyRadioButton;
    private SpinnerNumberModel hourModel;
    private JSpinner hourSpinner;
    private JComboBox dayCombo;

    /**
     * Constructor.
     * 
     * @param controller
     * @param syncProfileSelectorPanel
     */
    public CustomSyncProfileDialog(Controller controller,
        SyncProfileSelectorPanel syncProfileSelectorPanel)
    {
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
     * 
     * @todo need a better icon.
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
        FormLayout layout = new FormLayout(
            "right:pref, 4dlu, pref, 4dlu, 60dlu, 4dlu, pref",
            "pref, 14dlu, pref, 14dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.setBorder(Borders.createEmptyBorder("0, 0, 30dlu, 0"));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.customsync.text")), cc.xyw(1, 1, 7));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.customsync.syncprofilescombo")), cc
            .xy(1, 3));
        builder.add(syncProfilesCombo, cc.xyw(3, 3, 3));
        JLabel helpLabel = Help.createHelpLinkLabel(Translation
            .getTranslation("general.whatisthis"), "node/syncoptions");
        helpLabel.setBorder(Borders.createEmptyBorder("0,1,0,0"));
        builder.add(helpLabel, cc.xy(7, 3));

        builder.add(autoDownloadFromFriendsBox, cc.xyw(3, 5, 5));
        builder.add(autoDownloadFromOthersBox, cc.xyw(3, 7, 5));
        builder.add(syncDeletionWithFriendsBox, cc.xyw(3, 9, 5));
        builder.add(syncDeletionWithOthersBox, cc.xyw(3, 11, 5));

        builder.add(regularRadioButton, cc.xyw(3, 13, 5));

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.minutesbetweenscans")), cc.xy(1, 15));
        builder.add(scanMinutesSpinner, cc.xy(3, 15));
        builder.add(scanInfoLabel, cc.xyw(5, 15, 3));

        builder.add(dailyRadioButton, cc.xyw(3, 17, 5));

        builder.add(new JLabel(Translation.getTranslation("dialog.customsync.hourDaySync")), cc.xy(1, 19));
        builder.add(hourSpinner, cc.xy(3, 19));
        builder.add(dayCombo, cc.xyw(5, 19, 3));

        ButtonGroup bg = new ButtonGroup();
        bg.add(regularRadioButton);
        bg.add(dailyRadioButton);

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        // Combo
        syncProfilesCombo = new JComboBox();
        syncProfilesCombo.addItem(Translation.getTranslation("syncprofile."
            + SyncProfile.CUSTOM_SYNC_PROFILE_ID + ".name"));
        for (SyncProfile syncProfile : SyncProfile.DEFAULT_SYNC_PROFILES) {
            syncProfilesCombo.addItem(Translation.getTranslation(syncProfile
                .getTranslationId()));
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
        autoDownloadFromFriendsBox = new JCheckBox(Translation
            .getTranslation("dialog.customsync.autodownloadfromfriends"));
        autoDownloadFromFriendsBox.addChangeListener(cl);
        autoDownloadFromOthersBox = new JCheckBox(Translation
            .getTranslation("dialog.customsync.autodownloadfromother"));
        autoDownloadFromOthersBox.addChangeListener(cl);
        syncDeletionWithFriendsBox = new JCheckBox(Translation
            .getTranslation("dialog.customsync.syncdeletionwithfriends"));
        syncDeletionWithFriendsBox.addChangeListener(cl);
        syncDeletionWithOthersBox = new JCheckBox(Translation
            .getTranslation("dialog.customsync.syncdeletionwithothers"));
        syncDeletionWithOthersBox.addChangeListener(cl);

        scanMinutesModel = new SpinnerNumberModel(0, 0, 9999, 1);
        scanMinutesSpinner = new JSpinner(scanMinutesModel);
        scanMinutesModel.addChangeListener(cl);

        scanInfoLabel = new JLabel();
        scanMinutesModel.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (scanMinutesModel.getNumber().intValue() == 0) {
                    scanInfoLabel
                        .setText(Translation
                            .getTranslation("dialog.customsync.changedetection_disabled"));
                } else {
                    scanInfoLabel.setText("");
                }
            }
        });

        regularRadioButton = new JRadioButton(Translation.getTranslation("dialog.customsync.regularSync"));
        regularRadioButton.addActionListener(this);
        dailyRadioButton = new JRadioButton(Translation.getTranslation("dialog.customsync.dailySync"));
        dailyRadioButton.addActionListener(this);

        dayCombo = new JComboBox(new Object[]{
            Translation.getTranslation("dialog.customsync.everyDay"),
            Translation.getTranslation("general.sunday"),
            Translation.getTranslation("general.monday"),
            Translation.getTranslation("general.tuesday"),
            Translation.getTranslation("general.wednesday"),
            Translation.getTranslation("general.thursday"),
            Translation.getTranslation("general.friday"),
            Translation.getTranslation("general.saturday"),
            Translation.getTranslation("dialog.customsync.weekdays"),
            Translation.getTranslation("dialog.customsync.weekends")});
        dayCombo.setMaximumRowCount(10);

        hourModel = new SpinnerNumberModel(12, 0, 23, 1);
        hourSpinner = new JSpinner(hourModel);

        hourModel.addChangeListener(cl);
        dayCombo.addActionListener(this);

        // Initialise settings.
        SyncProfile syncProfile = syncProfileSelectorPanel.getSyncProfile();
        autoDownloadFromFriendsBox.setSelected(syncProfile
            .isAutoDownloadFromFriends());
        autoDownloadFromOthersBox.setSelected(syncProfile
            .isAutoDownloadFromOthers());
        syncDeletionWithFriendsBox.setSelected(syncProfile
            .isSyncDeletionWithFriends());
        syncDeletionWithOthersBox.setSelected(syncProfile
            .isSyncDeletionWithOthers());
        scanMinutesModel.setValue(syncProfile.getMinutesBetweenScans());
        dailyRadioButton.setSelected(syncProfile.isDailySync());
        regularRadioButton.setSelected(!syncProfile.isDailySync());
        hourModel.setValue(syncProfile.getDailyHour());
        dayCombo.setSelectedIndex(syncProfile.getDailyDay());

        //scanMinutesSpinner.setEnabled(!syncProfile.isDailySync());
        hourSpinner.setEnabled(syncProfile.isDailySync());
        dayCombo.setEnabled(syncProfile.isDailySync());
    }

    /**
     * Selects the combo based on the current configuration settings.
     */
    private void preselectCombo() {
        SyncProfile syncProfile = new SyncProfile(
                autoDownloadFromFriendsBox.isSelected(),
                autoDownloadFromOthersBox.isSelected(),
                syncDeletionWithFriendsBox.isSelected(),
                syncDeletionWithOthersBox.isSelected(),
                scanMinutesModel.getNumber().intValue(),
                dailyRadioButton.isSelected(),
                hourModel.getNumber().intValue(),
                dayCombo.getSelectedIndex());

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
            autoDownloadFromFriendsBox.setSelected(syncProfile
                .isAutoDownloadFromFriends());
            autoDownloadFromOthersBox.setSelected(syncProfile
                .isAutoDownloadFromOthers());
            syncDeletionWithFriendsBox.setSelected(syncProfile
                .isSyncDeletionWithFriends());
            syncDeletionWithOthersBox.setSelected(syncProfile
                .isSyncDeletionWithOthers());
            scanMinutesModel.setValue(syncProfile.getMinutesBetweenScans());
            regularRadioButton.setSelected(!syncProfile.isDailySync());
            dailyRadioButton.setSelected(syncProfile.isDailySync());
            hourModel.setValue(syncProfile.getDailyHour());
            dayCombo.setSelectedIndex(syncProfile.getDailyDay());
        }
    }

    /**
     * The OK / Cancel buttons.
     * 
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
        SyncProfile syncProfile = new SyncProfile(
                autoDownloadFromFriendsBox.isSelected(),
                autoDownloadFromOthersBox.isSelected(),
                syncDeletionWithFriendsBox.isSelected(),
                syncDeletionWithOthersBox.isSelected(),
                scanMinutesModel.getNumber().intValue(),
                dailyRadioButton.isSelected(),
                hourModel.getNumber().intValue(),
        dayCombo.getSelectedIndex());
        syncProfileSelectorPanel.setSyncProfile(syncProfile, true);
        close();
    }

    /**
     * User does not want to commit any change.
     */
    private void cancelPressed() {
        close();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(regularRadioButton) ||
                e.getSource().equals(dailyRadioButton)) {
            preselectCombo();
            scanMinutesSpinner.setEnabled(regularRadioButton.isSelected());
            hourSpinner.setEnabled(dailyRadioButton.isSelected());
            dayCombo.setEnabled(dailyRadioButton.isSelected());
        } else if (e.getSource().equals(dayCombo)) {
            preselectCombo();
        }
    }
}
