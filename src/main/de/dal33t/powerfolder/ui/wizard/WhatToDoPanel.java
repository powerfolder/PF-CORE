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
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import jwf.WizardContext;
import jwf.WizardPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.util.Translation;

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
    private static final Object downloadOption = new Object();
    private static final Object customOption = new Object();

    private ActionLabel synchronizedLink;
    private ActionLabel backupLink;
    private ActionLabel hostLink;
    private ActionLabel downloadLink;

    private ActionLabel moreLink;
    private ActionLabel customLink;
    private LinkLabel documentationLink;
    private ValueModel decision;

    public WhatToDoPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return decision.getValue() != null;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref",
            "pref, 12dlu, pref, 30dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("20dlu, 10dlu, 0, 0"));
        CellConstraints cc = new CellConstraints();

        builder.add(synchronizedLink.getUIComponent(), cc.xy(1, 1));
        builder.add(hostLink.getUIComponent(), cc.xy(1, 3));
        builder.add(backupLink.getUIComponent(), cc.xy(3, 1));
        builder.add(downloadLink.getUIComponent(), cc.xy(3, 3));

        builder.add(moreLink.getUIComponent(), cc.xy(1, 5));
        builder.add(customLink.getUIComponent(), cc.xy(1, 5));
        builder.add(documentationLink.getUIComponent(), cc.xy(3, 5));

        builder.getPanel().setOpaque(false);

        return builder.getPanel();
    }

    public WizardPanel next() {

        Object option = decision.getValue();

        if (option == synchronizedOption) {
            return doSyncOption(getController(), getWizardContext(), true);
        } else if (option == backupOption) {
            return doBackupOption(getController(), getWizardContext());
        } else if (option == hostOption) {
            return doHostOption(getController(), getWizardContext());
        } else if (option == downloadOption) {
            return doDownloadOption(getController(), getWizardContext());
        } else if (option == customOption) {
            return doCustomAction();
        }

        return null;
    }

    private WizardPanel doCustomAction() {
        // Reset folderinfo for disk location
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // Default to Auto. User can change during the wizard process.
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // Setup choose disk location panel / default text
        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE, null);

        // Prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, true);

        // Select backup by OS
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("exp.wizard.project_name.folder_project_success")
                + Translation.getTranslation("wizard.what_to_do.pcs_join"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        MultiFolderSetupPanel setupPanel = new MultiFolderSetupPanel(
            getController());
        return new ChooseMultiDiskLocationPanel(getController(), setupPanel, true);
    }

    public static PFWizardPanel doHostOption(Controller controller,
        WizardContext wizardContext)
    {
        // Reset folderinfo for disk location
        wizardContext.setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is hosting (manual download) profile!
        wizardContext.setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.HOST_FILES);

        // Setup choose disk location panel
        wizardContext.setAttribute(PROMPT_TEXT_ATTRIBUTE, Translation
            .getTranslation("wizard.what_to_do.host_pcs.select"));

        // Prompt for send invitation afterwards
        wizardContext.setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, true);

        // Select backup by OS
        wizardContext.setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_host_success")
                + Translation.getTranslation("wizard.what_to_do.host_pcs_join"));
        wizardContext.setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(controller);

        return new ChooseMultiDiskLocationPanel(controller, createPanel, true);
    }

    public static PFWizardPanel doDownloadOption(Controller controller,
        WizardContext wizardContext)
    {
        // Reset folderinfo for disk location
        wizardContext.setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is hosting (manual download) profile!
        wizardContext.setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.AUTOMATIC_DOWNLOAD);

        // Setup choose disk location panel
        wizardContext.setAttribute(PROMPT_TEXT_ATTRIBUTE, Translation
            .getTranslation("wizard.what_to_do.download.select"));

        // Prompt for send invitation afterwards
        wizardContext.setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Select backup by OS
        wizardContext.setAttribute(BACKUP_ONLINE_STOARGE, false);

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_download_sucess"));
        wizardContext.setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(controller);

        return new ChooseMultiDiskLocationPanel(controller, createPanel, true);
    }

    public static PFWizardPanel doBackupOption(Controller controller,
        WizardContext wizardContext)
    {
        // Reset folderinfo for disk location
        wizardContext.setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is backup (source) profile!
        wizardContext.setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.BACKUP_SOURCE);

        // Setup choose disk location panel
        wizardContext.setAttribute(PROMPT_TEXT_ATTRIBUTE, Translation
            .getTranslation("wizard.what_to_do.backp.select"));

        // Don't prompt for send invitation afterwards
        wizardContext.setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Select backup by OS
        wizardContext.setAttribute(BACKUP_ONLINE_STOARGE, true);

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.folder_backup_success")
                + Translation.getTranslation("wizard.what_to_do.pcs_join"));
        wizardContext.setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        // #1991: Let the user choose if Backup source or target
        // MultiFolderSetupPanel setupPanel = new
        // MultiFolderSetupPanel(controller);
        // return new ChooseMultiDiskLocationPanel(controller, setupPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(controller);
        return new ChooseMultiDiskLocationPanel(controller, createPanel, true);
    }

    public static PFWizardPanel doSyncOption(Controller controller,
        WizardContext wizardContext, boolean cancelNotFinish)
    {
        // Reset folderinfo for disk location
        wizardContext.setAttribute(FOLDERINFO_ATTRIBUTE, null);

        // This is sync pcs (mirror) profile!
        wizardContext.setAttribute(SYNC_PROFILE_ATTRIBUTE,
            SyncProfile.AUTOMATIC_SYNCHRONIZATION);

        // No invitation by default.
        wizardContext.setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Select backup by OS
        wizardContext.setAttribute(BACKUP_ONLINE_STOARGE, controller
            .getOSClient().isBackupByDefault());

        // Setup choose disk location panel
        wizardContext.setAttribute(PROMPT_TEXT_ATTRIBUTE,
            Translation.getTranslation("wizard.what_to_do.sync_pcs.select"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.what_to_do.sync_pcs.success")
                + Translation
                    .getTranslation("wizard.what_to_do.sync_pcs.pcs_join"));
        wizardContext.setAttribute(PFWizard.SUCCESS_PANEL, successPanel);

        FolderCreatePanel createPanel = new FolderCreatePanel(controller);

        return new ChooseMultiDiskLocationPanel(controller, createPanel,
                cancelNotFinish);
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
        synchronizedLink.convertToBigLabel();

        backupLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.backup_folder"),
            backupOption, decision));
        backupLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.backup_folder.tip"));
        backupLink.convertToBigLabel();

        hostLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.host_work"),
            hostOption, decision));
        hostLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.host_work.tip"));
        hostLink.convertToBigLabel();

        downloadLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.download"),
            downloadOption, decision));
        downloadLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.download.tip"));
        downloadLink.convertToBigLabel();

        moreLink = new ActionLabel(getController(), new AbstractAction(
            Translation.getTranslation("wizard.what_to_do.more"))
        {
            public void actionPerformed(ActionEvent e) {
                moreLink.setVisible(false);
                customLink.setVisible(true);
                documentationLink.setVisible(true);
            }
        });
        moreLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.more.tip"));
        moreLink.convertToBigLabel();

        customLink = new ActionLabel(getController(), new WhatToDoAction(
            Translation.getTranslation("wizard.what_to_do.custom_sync"),
            customOption, decision));
        customLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.custom_sync.tip"));
        customLink.convertToBigLabel();
        customLink.setVisible(false);

        documentationLink = Help.createQuickstartGuideLabel(getController(),
            Translation
                .getTranslation("wizard.what_to_do.open_online_documentation"));
        documentationLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.open_online_documentation.tip"));
        documentationLink.convertToBigLabel();
        documentationLink.setVisible(false);
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