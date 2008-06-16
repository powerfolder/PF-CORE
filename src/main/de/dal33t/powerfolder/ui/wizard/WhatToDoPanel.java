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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import jwf.Wizard;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
    private static final Object inviteOption = new Object();

    private JLabel synchronizedLink;
    private JLabel backupLink;
    private JLabel hostLink;
    private JLabel customLink;
    private JLabel inviteLink;
    private JLabel documentationLink;

    private ValueModel decision;

    public WhatToDoPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return decision.getValue() != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("pref",
                "pref, 10dlu, pref, 10dlu, pref, 10dlu, pref, " +
                        "30dlu, pref, 10dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(synchronizedLink, cc.xy(1, 1));
        builder.add(backupLink, cc.xy(1, 3));
        builder.add(hostLink, cc.xy(1, 5));
        builder.add(customLink, cc.xy(1, 7));
        builder.add(inviteLink, cc.xy(1, 9));
        builder.add(documentationLink, cc.xy(1, 11));
        return builder.getPanel();
    }

    public WizardPanel next() {

        Object option = decision.getValue();

        if (option == synchronizedOption) {

            getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.SYNC_PCS_PICTO);

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

            // This is sync pcs (mirror) profile!
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.AUTOMATIC_SYNCHRONIZATION);

            // Prompt for send invitation afterwards
            getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                true);

            // Setup choose disk location panel
            getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.sync_pcs_panel.select"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.syncpcspanel.foldersyncsuccess")
                    + Translation.getTranslation("wizard.syncpcspanel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            FolderSetupPanel setupPanel = new FolderSetupPanel(
                getController(), "");

            return new ChooseDiskLocationPanel(getController(), null, setupPanel);

        } else if (option == backupOption) {

            getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.SYNC_PCS_PICTO);

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

            // This is backup (source) profile!
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.BACKUP_SOURCE);

            // Setup choose disk location panel
            getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.backup_panel.select"));

            // Prompt for send invitation afterwards
            getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                true);

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(
                getController(),
                Translation.getTranslation("wizard.setupsuccess"),
                Translation
                    .getTranslation("wizard.backup_panel.folder_backup_success")
                    + Translation.getTranslation("wizard.backup_panel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            FolderSetupPanel setupPanel = new FolderSetupPanel(
                getController(), "");

            return new ChooseDiskLocationPanel(getController(), null, setupPanel);

        } else if (option == hostOption) {

            getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.SYNC_PCS_PICTO);

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

            // This is hosting (manual download) profile!
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.HOST_FILES);

            // Setup choose disk location panel
            getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.host_panel.select"));

            // Prompt for send invitation afterwards
            getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                true);

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.host_panel.folder_host_success")
                    + Translation.getTranslation("wizard.host_panel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            FolderSetupPanel setupPanel = new FolderSetupPanel(
                getController(), "");

            return new ChooseDiskLocationPanel(getController(), null, setupPanel);

        } else if (option == customOption) {

            getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.PROJECT_WORK_PICTO);

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

            // Setup choose disk location panel
            getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.choose_location.select"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(
                getController(),
                Translation.getTranslation("wizard.setupsuccess"),
                Translation
                    .getTranslation("wizard.project_panel.folder_project_success")
                    + Translation.getTranslation("wizard.backup_panel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            FolderSetupPanel setupPanel = new FolderSetupPanel(
                getController(), null);
            return new ChooseDiskLocationPanel(
                getController(), null, setupPanel);

        } else if (option == inviteOption) {

            getWizardContext().setAttribute(PFWizard.PICTO_ICON,
                Icons.FILESHARING_PICTO);

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                PROMPT_TEXT_ATTRIBUTE,
                Translation
                    .getTranslation("wizard.invite.selectlocaldirectory"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.successjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);
            return new LoadInvitationPanel(getController());
        }

        return null;
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

        synchronizedLink = new ActionLabel(new WhatToDoAction(Translation
                .getTranslation("wizard.whattodo.synchronized_folder"), synchronizedOption,
                decision));
        SimpleComponentFactory.setFontSize(synchronizedLink, PFWizard.MED_FONT_SIZE);

        backupLink = new ActionLabel(new WhatToDoAction(Translation
                .getTranslation("wizard.whattodo.backup_folder"), backupOption,
                decision));
        SimpleComponentFactory.setFontSize(backupLink, PFWizard.MED_FONT_SIZE);

        hostLink = new ActionLabel(new WhatToDoAction(Translation
                .getTranslation("wizard.whattodo.hostwork"), hostOption, decision));
        SimpleComponentFactory.setFontSize(hostLink, PFWizard.MED_FONT_SIZE);

        customLink = new ActionLabel(new WhatToDoAction(Translation
                .getTranslation("wizard.whattodo.custom_sync"), customOption,
                decision));
        SimpleComponentFactory.setFontSize(customLink, PFWizard.MED_FONT_SIZE);

        inviteLink = new ActionLabel(new WhatToDoAction(Translation
                .getTranslation("wizard.whattodo.load_invite"), inviteOption,
                decision));
        SimpleComponentFactory.setFontSize(inviteLink, PFWizard.MED_FONT_SIZE);

        documentationLink = Help.createHelpLinkLabel(Translation
                .getTranslation("wizard.whattodo.openonlinedocumentation"),
                "documentation.html");
        SimpleComponentFactory.setFontSize(documentationLink,
            PFWizard.MED_FONT_SIZE);
    }

    protected Icon getPicto() {
        return Icons.FILESHARING_PICTO;
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.whattodo.title");
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
            Wizard wizard = (Wizard) getWizardContext().getAttribute(
                Wizard.WIZARD_ATTRIBUTE);
            wizard.next();
        }
    }
}