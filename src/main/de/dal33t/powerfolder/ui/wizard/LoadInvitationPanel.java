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
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_PERMISSION_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.MAKE_FRIEND_AFTER;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SAVE_INVITE_LOCALLY;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.FileSelectorFactory;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class that selects an invitation then does the folder setup for that invite.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class LoadInvitationPanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(LoadInvitationPanel.class.getName());

    private JComponent locationField;
    private Invitation invitation;
    private JLabel folderHintLabel;
    private JLabel folderNameLabel;
    private JLabel invitorHintLabel;
    private JLabel invitorLabel;
    private JLabel invitationMessageHintLabel;
    private JTextField invitationMessageLabel;
    private JLabel estimatedSizeHintLabel;
    private JLabel estimatedSize;
    private JLabel syncProfileHintLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JCheckBox previewOnlyCB;

    public LoadInvitationPanel(Controller controller) {
        super(controller);
    }

    /**
     * Can procede if an invitation is selected.
     */

    @Override
    public boolean hasNext() {
        return invitation != null;
    }

    @Override
    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Set folder info
        getWizardContext()
            .setAttribute(FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Set folder permissions
        getWizardContext()
            .setAttribute(FOLDER_PERMISSION_ATTRIBUTE, invitation.getPermission());

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Whether to load as preview
        getWizardContext().setAttribute(PREVIEW_FOLDER_ATTIRBUTE,
            previewOnlyCB.isSelected());

        // If preview, validateNext has created the folder, so all done.
        if (previewOnlyCB.isSelected()) {
            return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        } else {

            // Make the invitor a friend.
            getWizardContext().setAttribute(MAKE_FRIEND_AFTER,
                invitation.getInvitor());

            File base = invitation.getSuggestedLocalBase(getController());
            if (base == null) {
                base = new File(getController().getFolderRepository()
                    .getFoldersBasedir(),
                    FileUtils.removeInvalidFilenameChars(invitation.folder
                        .getName()));
            }

            getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, Boolean.FALSE);

            return new ChooseDiskLocationPanel(getController(), base
                .getAbsolutePath(), new FolderCreatePanel(getController()));
        }
    }

    @Override
    public boolean validateNext() {
        return !previewOnlyCB.isSelected() || createPreviewFolder();
    }

    private boolean createPreviewFolder() {

        FolderSettings folderSettings = new FolderSettings(invitation
            .getSuggestedLocalBase(getController()), syncProfileSelectorPanel
            .getSyncProfile(), false, ArchiveMode.NO_BACKUP, true, null, 0,
                true);

        getController().getFolderRepository().createFolder(invitation.folder,
            folderSettings);
        return true;
    }

    @Override
    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("pref, 3dlu, 140dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Please select invite...
        builder.addLabel(Translation
            .getTranslation("wizard.load_invitation.select_file"), cc.xy(3, 1));

        // Invite selector
        builder.add(locationField, cc.xyw(3, 3, 2));

        // Folder
        builder.add(folderHintLabel, cc.xy(1, 5));
        builder.add(folderNameLabel, cc.xyw(3, 5, 2));

        // From
        builder.add(invitorHintLabel, cc.xy(1, 7));
        builder.add(invitorLabel, cc.xy(3, 7));

        // Message
        builder.add(invitationMessageHintLabel, cc.xy(1, 9));
        builder.add(invitationMessageLabel, cc.xy(3, 9));

        // Est size
        builder.add(estimatedSizeHintLabel, cc.xy(1, 11));
        builder.add(estimatedSize, cc.xy(3, 11));

        // Sync
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder.add(syncProfileHintLabel, cc.xy(1, 13));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);

            FormLayout layout2 = new FormLayout("pref, pref:grow", "pref");
            PanelBuilder builder2 = new PanelBuilder(layout2);
            builder2.add(p, cc.xy(1, 1));

            JPanel panel = builder2.getPanel();
            builder.add(panel, cc.xyw(3, 13, 2));
            panel.setOpaque(false);
        }

        // Preview
        builder.add(previewOnlyCB, cc.xy(3, 15));

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    @Override
    protected void initComponents() {

        ValueModel locationModel = new ValueHolder();

        // Invite behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                loadInvitation((String) evt.getNewValue());
                updateButtons();
            }
        });

        // Invite selector
        locationField = FileSelectorFactory.createFileSelectionField(
            Translation.getTranslation("wizard.load_invitation.choose_file"),
            locationModel, JFileChooser.FILES_AND_DIRECTORIES, InvitationUtil
                .createInvitationsFilefilter(), true);
        locationField.setOpaque(false);

        // Folder name label
        folderHintLabel = new JLabel(Translation
            .getTranslation("general.folder"));
        folderHintLabel.setEnabled(false);
        folderNameLabel = SimpleComponentFactory.createLabel();

        // Invitor label
        invitorHintLabel = new JLabel(Translation
            .getTranslation("general.inviter"));
        invitorHintLabel.setEnabled(false);
        invitorLabel = SimpleComponentFactory.createLabel();

        // Invitation messages
        invitationMessageHintLabel = new JLabel(Translation
            .getTranslation("general.message"));
        invitationMessageHintLabel.setEnabled(false);
        invitationMessageLabel = new JTextField();
        invitationMessageLabel.setEditable(false);

        // Estimated size
        estimatedSizeHintLabel = new JLabel(Translation
            .getTranslation("general.estimated_size"));
        estimatedSizeHintLabel.setEnabled(false);
        estimatedSize = SimpleComponentFactory.createLabel();

        // Sync profile
        syncProfileHintLabel = new JLabel(Translation
            .getTranslation("general.transfer_mode"));
        syncProfileHintLabel.setEnabled(false);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.setEnabled(false);

        // Preview
        previewOnlyCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("general.preview_folder"));
        previewOnlyCB.setOpaque(false);
        previewOnlyCB.setEnabled(false);

        // Do not let user select profile if preview.
        previewOnlyCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncProfileSelectorPanel
                    .setEnabled(!previewOnlyCB.isSelected());
            }
        });
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.load_invitation.select");
    }

    private void loadInvitation(String file) {
        if (file == null) {
            return;
        }
        invitation = InvitationUtil.load(new File(file));
        log.info("Loaded invitation " + invitation);
        if (invitation != null) {
            folderHintLabel.setEnabled(true);
            folderNameLabel.setText(invitation.folder.name);

            invitorHintLabel.setEnabled(true);
            invitorLabel.setText(invitation.getBestUsername());

            invitationMessageHintLabel.setEnabled(true);
            invitationMessageLabel
                .setText(invitation.getInvitationText() == null
                    ? ""
                    : invitation.getInvitationText());

            estimatedSizeHintLabel.setEnabled(true);
            estimatedSize.setText(Format.formatBytes(invitation.getSize())
                + " (" + invitation.getFilesCount() + ' '
                + Translation.getTranslation("general.files") + ')');

            syncProfileHintLabel.setEnabled(true);
            syncProfileSelectorPanel.setEnabled(true);
            SyncProfile suggestedProfile = invitation.getSuggestedSyncProfile();
            syncProfileSelectorPanel.setSyncProfile(suggestedProfile, false);

            previewOnlyCB.setEnabled(true);
        } else {
            folderHintLabel.setEnabled(false);
            folderNameLabel.setText("");
            invitorHintLabel.setEnabled(false);
            invitorLabel.setText("");
            invitationMessageHintLabel.setEnabled(false);
            invitationMessageLabel.setText("");
            estimatedSizeHintLabel.setEnabled(false);
            estimatedSize.setText("");
            syncProfileHintLabel.setEnabled(false);
            syncProfileSelectorPanel.setEnabled(false);
            previewOnlyCB.setEnabled(false);
        }
    }
}