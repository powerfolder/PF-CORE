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
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_IS_INVITE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_PERMISSION_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.MAKE_FRIEND_AFTER;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SAVE_INVITE_LOCALLY;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ArchiveMode;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class to do folder creation for a specified invite.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class ReceivedInvitationPanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(ReceivedInvitationPanel.class.getName());

    private final Invitation invitation;

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

    public ReceivedInvitationPanel(Controller controller, Invitation invitation)
    {
        super(controller);
        this.invitation = invitation;
    }

    /**
     * Can procede if an invitation exists.
     */
    @Override
    public boolean hasNext() {
        return invitation != null;
    }

    @Override
    public boolean validateNext() {
        return !previewOnlyCB.isSelected() || createPreviewFolder();
    }

    private boolean createPreviewFolder() {

        FolderSettings folderSettings = new FolderSettings(
            invitation.getSuggestedLocalBase(getController()),
            syncProfileSelectorPanel.getSyncProfile(), false,
            ArchiveMode.NO_BACKUP, true, null, 0, true);

        getController().getFolderRepository().createFolder(invitation.folder,
            folderSettings);
        return true;
    }

    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Set folder info
        getWizardContext()
            .setAttribute(FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Set folder permission
        getWizardContext().setAttribute(FOLDER_PERMISSION_ATTRIBUTE,
            invitation.getPermission());

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(FOLDER_IS_INVITE, true);

        // Whether to open as preview
        getWizardContext().setAttribute(PREVIEW_FOLDER_ATTIRBUTE,
            previewOnlyCB.isSelected());

        // Setup choose disk location panel
        getWizardContext()
            .setAttribute(
                PROMPT_TEXT_ATTRIBUTE,
                Translation
                    .getTranslation("wizard.what_to_do.invite.select_local"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"),
            Translation.getTranslation("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        // If preview, validateNext has created the folder, so all done.
        if (previewOnlyCB.isSelected()) {
            return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        } else {

            getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, Boolean.FALSE);

            getWizardContext().setAttribute(MAKE_FRIEND_AFTER,
                invitation.getInvitor());

            return new ChooseDiskLocationPanel(getController(), invitation
                .getSuggestedLocalBase(getController()).getAbsolutePath(),
                new FolderCreatePanel(getController()));
        }
    }

    @Override
    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("right:pref, 3dlu, pref, pref:grow",
            "pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        // Invite info

        builder.addLabel(Translation.getTranslation(
            "wizard.folder_invitation.intro", invitation.getBestUsername(),
            invitation.folder.name), cc.xyw(1, 1, 4));

        // Message

        int row = 3;
        String message = invitation.getInvitationText();
        if (message != null && message.trim().length() > 0) {
            builder.add(invitationMessageHintLabel, cc.xy(1, row));
            builder.add(invitationMessageLabel, cc.xy(3, row));
            row += 2;
        }

        // Est size
        builder.add(estimatedSizeHintLabel, cc.xy(1, row));
        builder.add(estimatedSize, cc.xy(3, row));
        row += 2;

        // Sync
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder.add(syncProfileHintLabel, cc.xy(1, row));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            p.setOpaque(false);
            builder.add(p, cc.xyw(3, row, 2));
        }
        row += 2;

        // Preview
        builder.add(previewOnlyCB, cc.xy(3, row));
        row += 2;

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    @Override
    protected void initComponents() {
        // Folder name label
        folderHintLabel = new JLabel(
            Translation.getTranslation("general.folder"));
        folderHintLabel.setEnabled(false);
        folderNameLabel = SimpleComponentFactory.createLabel();

        // Invitor label
        invitorHintLabel = new JLabel(
            Translation.getTranslation("general.inviter"));
        invitorHintLabel.setEnabled(false);
        invitorLabel = SimpleComponentFactory.createLabel();

        // Invitation messages
        invitationMessageHintLabel = new JLabel(
            Translation.getTranslation("general.message"));
        invitationMessageHintLabel.setEnabled(false);
        invitationMessageLabel = new JTextField();
        invitationMessageLabel.setEditable(false);

        // Estimated size
        estimatedSizeHintLabel = new JLabel(
            Translation.getTranslation("general.estimated_size"));
        estimatedSizeHintLabel.setEnabled(false);
        estimatedSize = SimpleComponentFactory.createLabel();

        // Sync profile
        syncProfileHintLabel = new JLabel(
            Translation.getTranslation("general.synchonisation"));
        syncProfileHintLabel.setEnabled(false);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.setEnabled(false);

        // Preview
        previewOnlyCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("general.preview_folder"));
        previewOnlyCB.setOpaque(false);
        previewOnlyCB.setEnabled(false);
        previewOnlyCB.setVisible(PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController()));

        // Do not let user select profile if preview.
        previewOnlyCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncProfileSelectorPanel.setEnabled(!previewOnlyCB.isSelected());
            }
        });

        loadInvitation();
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.folder_invitation.title");
    }

    private void loadInvitation() {
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