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

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SAVE_INVITE_LOCALLY;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jwf.WizardPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The start panel of the "what to do" wizard line
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class WhatToDoPanel extends PFWizardPanel {

    static final int PICTO_FONT_SIZE = 6;

    // The options of this screen
    private static final Object synchronizedOption = new Object();
    private static final Object backupOption = new Object();
    private static final Object hostOption = new Object();
    private static final Object customOption = new Object();
    private static final Object onlineOption = new Object();
    private static final Object inviteOption = new Object();

    private ActionLabel synchronizedLink;
    private ActionLabel backupLink;
    private ActionLabel hostLink;
    private ActionLabel customLink;
    private ActionLabel onlineLink;
    private ActionLabel inviteLink;
    private LinkLabel documentationLink;
    private ValueModel decision;

    public WhatToDoPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return decision.getValue() != null;
    }

    public boolean validateNext() {
        Object option = decision.getValue();
        if (option == onlineOption) {
            List<FolderInfo> folderList = findFolderList();

            if (folderList.isEmpty()) {
                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("wizard.error_title"), Translation
                    .getTranslation("wizard.what_to_do.no_os_text"),
                    GenericDialogType.INFO);
                return false;
            }
        }
        return true;
    }

    /**
     * Find list of folders that are online storage but not local.
     * 
     * @return
     */
    private List<FolderInfo> findFolderList() {
        List<FolderInfo> folderList = new ArrayList<FolderInfo>();
        ServerClient client = getController().getOSClient();
        List<FolderInfo> onlineFolderInfos = client.getAccountFolders();
        for (FolderInfo onlineFolderInfo : onlineFolderInfos) {
            Folder folder = onlineFolderInfo.getFolder(getController());
            if (folder == null) {
                folderList.add(onlineFolderInfo);
            }
        }
        return folderList;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref",
            "pref, 12dlu, pref, 12dlu, pref, 12dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("10dlu, 10dlu, 0, 0"));
        CellConstraints cc = new CellConstraints();

        builder.add(synchronizedLink.getUIComponent(), cc.xy(1, 1));
        builder.add(backupLink.getUIComponent(), cc.xy(1, 3));
        builder.add(hostLink.getUIComponent(), cc.xy(1, 5));
        builder.add(customLink.getUIComponent(), cc.xy(1, 7));

        builder.add(onlineLink.getUIComponent(), cc.xy(3, 1));
        builder.add(inviteLink.getUIComponent(), cc.xy(3, 3));
        builder.add(documentationLink.getUiComponent(), cc.xy(3, 5));
        builder.getPanel().setOpaque(false);

        return builder.getPanel();
    }

    public WizardPanel next() {

        Object option = decision.getValue();

        if (option == synchronizedOption) {
            return doSyncOption();
        } else if (option == backupOption) {
            return doBackupOption();
        } else if (option == hostOption) {
            return doHostOption();
        } else if (option == customOption) {
            return doCustomAction();
        } else if (option == onlineOption) {
            return doOnlineOption();
        } else if (option == inviteOption) {
            return doInviteOption();
        }

        return null;
    }

    private WizardPanel doOnlineOption() {
        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        List<FolderInfo> folderList = findFolderList();
        return new SelectOnlineStoragePanel(getController(), folderList);
    }

    private WizardPanel doInviteOption() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // Setup choose disk location panel
        getWizardContext()
            .setAttribute(
                PROMPT_TEXT_ATTRIBUTE,
                Translation
                    .getTranslation("wizard.what_to_do.invite.select_local"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.success_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);
        return new LoadInvitationPanel(getController());
    }

    private WizardPanel doCustomAction() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // Setup choose disk location panel / default text
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE, null);

        // Prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, true);

        // Select backup by OS
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.project_name.folder_project_success")
                + Translation.getTranslation("wizard.what_to_do.pcs_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        MultiFolderSetupPanel setupPanel = new MultiFolderSetupPanel(
            getController());
        return new ChooseMultiDiskLocationPanel(getController(), setupPanel);
    }

    private WizardPanel doHostOption() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is hosting (manual download) profile!
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.HOST_FILES);

        // Setup choose disk location panel
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
            Translation.getTranslation("wizard.what_to_do.host_pcs.select"));

        // Prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, true);

        // Select backup by OS
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_host_success")
                + Translation.getTranslation("wizard.what_to_do.host_pcs_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(getController());

        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, Boolean.TRUE);

        return new ChooseMultiDiskLocationPanel(getController(), createPanel);
    }

    private WizardPanel doBackupOption() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is backup (source) profile!
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.BACKUP_SOURCE);

        // Setup choose disk location panel
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
            Translation.getTranslation("wizard.what_to_do.backp.select"));

        // Don't prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Select backup by OS
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_backup_success")
                + Translation.getTranslation("wizard.what_to_do.pcs_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, true);

        FolderCreatePanel createPanel = new FolderCreatePanel(getController());

        return new ChooseMultiDiskLocationPanel(getController(), createPanel);
    }

    private WizardPanel doSyncOption() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is sync pcs (mirror) profile!
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // No invitation by default.
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Select backup by OS
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup choose disk location panel
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
            Translation.getTranslation("wizard.what_to_do.sync_pcs.select"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.sync_pcs.success")
                + Translation
                    .getTranslation("wizard.what_to_do.sync_pcs.pcs_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(getController());

        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY, true);

        return new ChooseMultiDiskLocationPanel(getController(), createPanel);
    }

    /**
     * Initalizes all nessesary components
     */
    public void initComponents() {
        decision = new ValueHolder();

        // Behavior
        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        synchronizedLink = new ActionLabel(getController(),
            new WhatToDoAction(Translation
                .getTranslation("wizard.what_to_do.synchronized_folder"),
                synchronizedOption, decision));
        synchronizedLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.synchronized_folder.tip"));
        synchronizedLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize((JLabel) synchronizedLink
            .getUIComponent(), PFWizard.MED_FONT_SIZE);

        backupLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.backup_folder"),
            backupOption, decision));
        backupLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.backup_folder.tip"));
        backupLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize(
            (JLabel) backupLink.getUIComponent(), PFWizard.MED_FONT_SIZE);

        hostLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.host_work"),
            hostOption, decision));
        hostLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.host_work.tip"));
        hostLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize((JLabel) hostLink.getUIComponent(),
            PFWizard.MED_FONT_SIZE);

        customLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.custom_sync"),
            customOption, decision));
        customLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.custom_sync.tip"));
        customLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize(
            (JLabel) customLink.getUIComponent(), PFWizard.MED_FONT_SIZE);

        onlineLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.join_online"),
            onlineOption, decision));
        onlineLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.join_online.tip"));
        onlineLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize(
            (JLabel) onlineLink.getUIComponent(), PFWizard.MED_FONT_SIZE);

        inviteLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.load_invite"),
            inviteOption, decision));
        inviteLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.load_invite.tip"));
        inviteLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        SimpleComponentFactory.setFontSize(
            (JLabel) inviteLink.getUIComponent(), PFWizard.MED_FONT_SIZE);

        documentationLink = Help.createQuickstartGuideLabel(getController(),
            Translation
                .getTranslation("wizard.what_to_do.open_online_documentation"));
        documentationLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.open_online_documentation.tip"));
        documentationLink.setFontSize(PFWizard.MED_FONT_SIZE);
        documentationLink.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.what_to_do.title");
    }

    private class WhatToDoAction extends AbstractAction {

        private ValueModel model;
        private Object option;

        private WhatToDoAction(String name, Object option, ValueModel model) {
            this.model = model;
            this.option = option;
            putValue(NAME, name);
        }

        public void actionPerformed(ActionEvent e) {
            model.setValue(option);
            getWizard().next();
        }
    }
}