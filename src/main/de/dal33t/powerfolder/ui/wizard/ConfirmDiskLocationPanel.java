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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ConfirmDiskLocationPanel extends PFWizardPanel {

    private File localBase;

    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox sendInviteAfterCB;

    private JLabel folderSizeLabel;

    public ConfirmDiskLocationPanel(Controller controller, File localBase) {
        super(controller);
        this.localBase = localBase;
    }

    public WizardPanel next() {
        getWizardContext().setAttribute(FOLDER_LOCAL_BASE, localBase);
        String initialFolderName = FileUtils.getSuggestedFolderName(localBase);
        getWizardContext().setAttribute(INITIAL_FOLDER_NAME, initialFolderName);
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            return new FolderSetupPanel(getController());
        } else {
            // Set FolderInfo
            // NOTE: this is more or less a copy of FolderSetupPanel next(), for non experts.
            // Changes may need to be applied to both.
            FolderInfo folderInfo = new FolderInfo(initialFolderName,
                    '[' + IdGenerator.makeId() + ']');
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, folderInfo);

            // Set sync profile
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                    SyncProfile.AUTOMATIC_SYNCHRONIZATION);

            // Setup choose disk location panel
            getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                    Translation.getTranslation(
                            "wizard.what_to_do.invite.select_local"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                    Translation.getTranslation("wizard.setup_success"),
                    Translation.getTranslation("wizard.success_join"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

            getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, Boolean.TRUE);

            return new FolderCreatePanel(getController());
        }
    }

    public boolean hasNext() {
        return true;
    }

    public boolean validateNext() {
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected());
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        return true;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout(
            "pref, 3dlu, pref, 3dlu, max(pref;100dlu), 0",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JComponent locationField = new JLabel(Translation
            .getTranslation("general.directory"));

        int row = 1;

        builder.add(locationField, cc.xy(1, row));
        builder.add(new JLabel(localBase.getAbsolutePath()), cc.xy(3, row));

        row += 2;
        builder.add(folderSizeLabel, cc.xyw(1, row, 5));

        if (getController().getOSClient().isBackupByDefault()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            row += 2;
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 5));
        }

        // Send Invite
        if (getController().isBackupOnly() ||
                !ConfigurationEntry.SERVER_INVITE_ENABLED.getValueBoolean(
                        getController())) {
            sendInviteAfterCB.setSelected(false);
        } else {
            row += 2;
            builder.add(sendInviteAfterCB, cc.xyw(1, row, 5));
        }

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {

        folderSizeLabel = new JLabel();

        // Online Storage integration
        boolean backupByOS = getController().getOSClient().isBackupByDefault()
            && Boolean.TRUE.equals(getWizardContext().getAttribute(
                BACKUP_ONLINE_STOARGE));
        backupByOnlineStorageBox = new JCheckBox(
            Translation
                .getTranslation("wizard.choose_disk_location.backup_by_online_storage"));
        // Is backup suggested?
        if (backupByOS) {
            backupByOnlineStorageBox.setSelected(true);
        }
        backupByOnlineStorageBox.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (backupByOnlineStorageBox.isSelected()) {
                    getController().getUIController().getApplicationModel()
                        .getServerClientModel().checkAndSetupAccount();
                }
            }
        });
        backupByOnlineStorageBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
            .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("wizard.choose_disk_location.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

    }

    protected String getTitle() {
        return Translation
            .getTranslation("wizard.choose_disk_location.options");
    }
}