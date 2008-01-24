/* $Id: WhatToDoPanel.java,v 1.13 2005/11/20 03:18:51 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.FolderCreatePanel;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import jwf.Wizard;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static final Object mirrorOption = new Object();
    private static final Object backupOption = new Object();
    private static final Object projectOption = new Object();
    private static final Object customOption = new Object();
    private static final Object inviteOption = new Object();

    private boolean initalized;

    // option lables
    private JLabel mirrorLink;
    private JLabel backupLink;
    private JLabel projectLink;
    private JLabel customLink;
    private JLabel inviteLink;
    private JLabel documentationLink;

    private ValueModel decision;

    public WhatToDoPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return decision.getValue() != null;
    }

    public boolean validateNext(List list) {
        if (decision.getValue() == customOption) {

            // Get the folder to use.
            FolderCreatePanel panel = new FolderCreatePanel(getController());
            panel.open();
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE,
                panel.getFolderInfo());
            return panel.folderCreated();
        }
        return true;
    }

    public WizardPanel next() {

        Object option = decision.getValue();
        if (option == mirrorOption) {

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

            // This is sync pcs (mirror) profile!
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.SYNC_PROFILE_ATTRIBUTE,
                    SyncProfile.SYNCHRONIZE_PCS);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                    Translation.getTranslation("wizard.syncpcspanel.select"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                    Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.syncpcspanel.foldersyncsuccess")
                    + Translation.getTranslation("wizard.syncpcspanel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                    successPanel);

            return new ChooseDiskLocationPanel(getController());

        } else if (option == backupOption) {

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

            // This is backupm (source) profile!
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.SYNC_PROFILE_ATTRIBUTE,
                    SyncProfile.BACKUP_SOURCE);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                    Translation.getTranslation("wizard.backup_panel.select"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                    Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.backup_panel.folder_backup_success")
                    + Translation.getTranslation("wizard.backup_panel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                    successPanel);

            return new ChooseDiskLocationPanel(getController());

        } else if (option == projectOption) {

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

            // This is project profile!
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.SYNC_PROFILE_ATTRIBUTE,
                    SyncProfile.PROJECT_WORK);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                    ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
                    Translation.getTranslation("wizard.project_panel.select"));

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                    Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.project_panel.folder_project_success")
                    + Translation.getTranslation("wizard.backup_panel.pcsjoin"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                    successPanel);

            return new ChooseDiskLocationPanel(getController());

        } else if (option == customOption) {

            // Setup sucess panel of this wizard path
            TextPanelPanel successPanel = new TextPanelPanel(getController(),
                Translation.getTranslation("wizard.setupsuccess"), Translation
                    .getTranslation("wizard.filesharing.sharedsuccess"));
            getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                successPanel);

            // The user may now send invitations for the folder.
            return new SendInvitationsPanel(getController(), true);
            
        } else if (option == inviteOption) {

            // Reset folderinfo for disk location
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

            // Setup choose disk location panel
            getWizardContext().setAttribute(
                ChooseDiskLocationPanel.PROMPT_TEXT_ATTRIBUTE,
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

    public boolean canFinish() {
        return false;
    }

    public boolean validateFinish(List list) {
        return true;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 40dlu, pref:grow, 20dlu",
            "5dlu, pref, 20dlu, " +
                    "pref, 10dlu, " +
                    "pref, 10dlu, " +
                    "pref, 10dlu, " +
                    "pref, 10dlu, " +
                    "pref, 40dlu, " +
                    "pref");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.whattodo.title")), cc.xywh(2, 2, 3, 1,
            CellConstraints.CENTER, CellConstraints.DEFAULT));

        builder.add(mirrorLink, cc.xy(4, 4));
        builder.add(backupLink, cc.xy(4, 6));
        builder.add(projectLink, cc.xy(4, 8));
        builder.add(customLink, cc.xy(4, 10));
        builder.add(inviteLink, cc.xy(4, 12));

        builder.add(new JLabel(Icons.FILESHARING_PICTO), cc.xywh(2, 4, 1, 10, "center, top"));

        builder.add(documentationLink, cc.xywh(2, 14, 3, 1, "center, top"));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        decision = new ValueHolder();

        // Behavior
        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });
        
        mirrorLink = new ActionLabel(new WhatToDoAction(
                Translation.getTranslation("wizard.whattodo.mirror_folder"),
                mirrorOption, decision));
        SimpleComponentFactory.setFontSize(mirrorLink,
            PFWizard.HEADER_FONT_SIZE);

        backupLink = new ActionLabel(new WhatToDoAction(
                Translation.getTranslation("wizard.whattodo.backup_folder"),
                backupOption, decision));
        SimpleComponentFactory.setFontSize(backupLink,
            PFWizard.HEADER_FONT_SIZE);

        projectLink = new ActionLabel(new WhatToDoAction(
                Translation.getTranslation("wizard.whattodo.projectwork"),
                projectOption, decision));
        SimpleComponentFactory.setFontSize(projectLink,
            PFWizard.HEADER_FONT_SIZE);

        customLink = new ActionLabel(new WhatToDoAction(
                Translation.getTranslation("wizard.whattodo.custom_sync"),
                customOption, decision));
        SimpleComponentFactory.setFontSize(customLink,
            PFWizard.HEADER_FONT_SIZE);

        inviteLink = new ActionLabel(new WhatToDoAction(
                Translation.getTranslation("wizard.whattodo.load_invite"), 
                inviteOption, decision));
        SimpleComponentFactory.setFontSize(inviteLink,
            PFWizard.HEADER_FONT_SIZE);

        documentationLink = Help.createHelpLinkLabel(Translation
            .getTranslation("wizard.whattodo.openonlinedocumentation"),
            "node/documentation");
        SimpleComponentFactory.setFontSize(documentationLink,
            PFWizard.HEADER_FONT_SIZE);
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
            Wizard wizard = (Wizard) getWizardContext().getAttribute(Wizard.WIZARD_ATTRIBUTE);
            wizard.next();
        }
    }
}