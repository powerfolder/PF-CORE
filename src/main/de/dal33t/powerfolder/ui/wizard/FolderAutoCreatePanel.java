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

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.DIALOG_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.USE_CLOUD_STORAGE;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.panel.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class to do folder creation for a specified invite.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class FolderAutoCreatePanel extends PFWizardPanel {

    private final FolderInfo folderInfo;

    private JLabel folderNameLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JCheckBox useCloudCB;
    private JCheckBox inviteCB;
    private ActionLabel undoLabel;

    public FolderAutoCreatePanel(Controller controller, FolderInfo folderInfo)
    {
        super(controller);
        this.folderInfo = folderInfo;
    }

    /**
     * Can procede if an invitation exists.
     */
    @Override
    public boolean hasNext() {
        return folderInfo != null;
    }

    public WizardPanel next() {

        // FolderInfo
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE,
            folderInfo);

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Cloud
        getWizardContext().setAttribute(USE_CLOUD_STORAGE,
            useCloudCB.isSelected());

        // Invite
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            inviteCB.isSelected());

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"),
                Translation.getTranslation("wizard.success_configure"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        return new FolderAutoConfigPanel(getController());
    }

    @Override
    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("right:pref, 3dlu, pref, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, "
                + "3dlu, pref, 15dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        // Name
        builder.addLabel(Translation.getTranslation("general.folder"),
            cc.xy(1, row));
        builder.add(folderNameLabel, cc.xy(3, row));
        row += 2;

        // Sync
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder.addLabel(
                Translation.getTranslation("general.synchonisation"),
                cc.xy(1, row));
            JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
            builder.add(p, cc.xyw(3, row, 2));
        }
        row += 2;

        // Cloud space
        builder.add(useCloudCB, cc.xyw(3, row, 2));
        row += 2;

        // Invite
        if (!getController().isBackupOnly() &&
                ConfigurationEntry.SERVER_INVITE_ENABLED.getValueBoolean(
                        getController())) {
            builder.add(inviteCB, cc.xyw(3, row, 2));
        }
        row += 2;

        // Undo
        builder.add(undoLabel.getUIComponent(), cc.xyw(3, row, 2));

        return builder.getPanel();
    }

    /**
     * Initalizes all necesary components
     */
    @Override
    protected void initComponents() {

        // Folder name label
        folderNameLabel = SimpleComponentFactory.createLabel();
        folderNameLabel.setText(folderInfo.getName());

        // Sync profile
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        Folder folder = getController().getFolderRepository().getFolder(
            folderInfo);
        SyncProfile syncProfile = folder.getSyncProfile();
        syncProfileSelectorPanel.setSyncProfile(syncProfile, false);

        // Cloud space
        useCloudCB = new JCheckBox(
            Translation.getTranslation("wizard.folder_auto_create.cloud_space"));
        useCloudCB.setOpaque(false);
        useCloudCB.setSelected(getController().getOSClient()
            .isBackupByDefault());

        // Cloud space
        inviteCB = new JCheckBox(Translation.getTranslation(
                "exp.wizard.choose_disk_location.send_invitation"));
        inviteCB.setOpaque(false);

        // Undo link
        undoLabel = new ActionLabel(getController(),
                new MyUndoAction(getController()));
    }

    @Override
    protected String getTitle() {
        return Translation.getTranslation("wizard.folder_auto_create.title");
    }

    private void undoAutocreate() {
        int i = DialogFactory.genericDialog(getController(),
                Translation.getTranslation("wizard.folder_auto_create.undo.title"),
                Translation.getTranslation("wizard.folder_auto_create.undo.text"),
                new String[]{
                        Translation.getTranslation("wizard.folder_auto_create.undo.button"),
                        Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.QUESTION);
        if (i == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    SwingWorker sw = new SwingWorker() {
                        protected Object doInBackground() throws Exception {
                            JDialog diag = (JDialog) getWizardContext().getAttribute(
                                    DIALOG_ATTRIBUTE);
                            Cursor c = CursorUtils.setWaitCursor(diag);
                            Folder folder = getController().getFolderRepository()
                                    .getFolder(folderInfo);
                            getController().getFolderRepository().removeFolder(folder,
                                    false);
                            ServerClient client = getController().getOSClient();
                            if (client.isConnected()) {
                                client.getFolderService().removeFolder(folderInfo, true,
                                        true);
                            }
                            CursorUtils.returnToOriginal(diag, c);
                            diag.setVisible(false);
                            return null;
                        }
                    };
                    sw.execute();
                }
            });
        }
    }

    // /////////////
    // Inner classes
    // /////////////

    private class MyUndoAction extends BaseAction {
        MyUndoAction(Controller controller) {
            super("action_undo_auto_create", controller);
        }

        public void actionPerformed(ActionEvent e) {
            undoAutocreate();
        }
    }
}