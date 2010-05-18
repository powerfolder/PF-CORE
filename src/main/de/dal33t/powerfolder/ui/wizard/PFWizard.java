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

import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;

import jwf.Wizard;
import jwf.WizardContext;
import jwf.WizardListener;
import jwf.WizardPanel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * The main wizard class
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class PFWizard extends PFUIComponent {
    // The size of the header font, e.g. the main question of the wizard pane
    static final int HEADER_FONT_SIZE = 20;

    // The attribute in the wizard context of the success panel. Displayed at
    // end
    public static final String SUCCESS_PANEL = "successpanel";

    private JDialog dialog;
    private Wizard wizard;
    private final String title;

    /**
     * @param controller
     *            the controller
     */
    public PFWizard(Controller controller, String title) {
        super(controller);
        wizard = new Wizard();
        this.title = title;
    }

    /**
     * Opens the wizard for the basic setup.
     * 
     * @param controller
     */
    public static void openBasicSetupWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.folder_title"));
        WhatToDoPanel wtdp = new WhatToDoPanel(controller);
        BasicSetupPanel basicPanel = new BasicSetupPanel(controller, wtdp);
        wizard.open(new LoginOnlineStoragePanel(controller, basicPanel, true));
    }

    /**
     * Opens the what to do wizard. Wizard to create / join folders.
     * 
     * @param controller
     */
    public static void openWhatToDoWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.folder_title"));
        wizard.open(new WhatToDoPanel(controller));
    }

    /**
     * Opens the send-invitation wizard.
     * 
     * @param controller
     *            the controller.
     * @param foInfo
     *            the folder to send the invitation for.
     */
    public static void openSendInvitationWizard(Controller controller,
        FolderInfo foInfo)
    {
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.invitation_title"));
        wizard.getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, foInfo);

        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.send_invitations.send_success"),
            Translation
                .getTranslation("wizard.send_invitations.send_success_info"),
            true);
        wizard.getWizardContext().setAttribute(SUCCESS_PANEL, successPanel);

        wizard.open(new SendInvitationsPanel(controller));
    }

    public static void openSingletonOnlineStorageJoinWizard(
        Controller controller, List<FolderInfo> folderInfoList)
    {

        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.online_storage_title"));

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_INFOS, folderInfoList);

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.CREATE_DESKTOP_SHORTCUT, false);

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"), Translation
                .getTranslation("wizard.success_join"));
        wizard.getWizardContext().setAttribute(SUCCESS_PANEL, successPanel);

        wizard.open(new MultiOnlineStorageSetupPanel(controller));
    }

    /**
     * Handles/Accepts and invitation that has been received.
     * 
     * @param controller
     * @param invitation
     */
    public static void openInvitationReceivedWizard(Controller controller,
        Invitation invitation)
    {
        ReceivedInvitationPanel panel = new ReceivedInvitationPanel(controller,
            invitation);
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.invitation_title"));
        wizard.open(panel);
    }

    public static void openLoginWizard(Controller controller,
        ServerClient client)
    {
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.login_title"));
        WizardPanel nextFinishPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.finish.os_login_title"),
            Translation.getTranslation("wizard.finish.os_login_text"), true);
        wizard.open(new LoginOnlineStoragePanel(controller, client,
            nextFinishPanel, false));
    }

    /**
     * Opens the wizard to setup a new webservice mirror.
     * 
     * @param controller
     * @param folderToSetup
     *            folder to configure for O/S
     */
    public static void openMirrorFolderWizard(Controller controller,
        Folder folderToSetup)
    {
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.folder_title"));
        wizard.open(new FolderOnlineStoragePanel(controller, folderToSetup
            .getInfo()));
    }

    public static void openExistingDirectoryWizard(Controller controller,
        File directory)
    {
        Reject.ifTrue(directory == null || !directory.exists(),
            "No directory supplied");
        PFWizard wizard = new PFWizard(controller, Translation
            .getTranslation("wizard.pfwizard.folder_title"));
        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE, true);
        wizard.open(new ConfirmDiskLocationPanel(controller, directory));
    }

    /**
     * Opens the wizard on a panel.
     * 
     * @param wizardPanel
     */
    public void open(PFWizardPanel wizardPanel) {
        Reject.ifNull(wizardPanel, "Wizard panel is null");
        if (dialog == null) {
            buildUI();
        }
        wizard.start(wizardPanel, false);
        dialog.setVisible(true);
    }

    /**
     * @return the wizard context
     */
    public WizardContext getWizardContext() {
        return wizard.getContext();
    }

    private void buildUI() {
        // Build the wizard
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent(),
            title, false); // Wizard
        dialog.setResizable(false);
        dialog.setModal(true);

        // Add i18n
        Map<String, String> i18nMap = new HashMap<String, String>();
        i18nMap.put(Wizard.BACK_I18N, Translation
            .getTranslation("wizard.control.back"));
        i18nMap.put(Wizard.NEXT_I18N, Translation
            .getTranslation("wizard.control.next"));
        i18nMap.put(Wizard.FINISH_I18N, Translation
            .getTranslation("wizard.control.finish"));
        i18nMap.put(Wizard.CANCEL_I18N, Translation
            .getTranslation("wizard.control.cancel"));
        i18nMap.put(Wizard.HELP_I18N, Translation
            .getTranslation("wizard.control.help"));
        i18nMap.put(Wizard.BACK_I18N_DESCRIPTION, Translation
            .getTranslation("wizard.control.back.description"));
        i18nMap.put(Wizard.NEXT_I18N_DESCRIPTION, Translation
            .getTranslation("wizard.control.next.description"));
        i18nMap.put(Wizard.FINISH_I18N_DESCRIPTION, Translation
            .getTranslation("wizard.control.finish.description"));
        i18nMap.put(Wizard.CANCEL_I18N_DESCRIPTION, Translation
            .getTranslation("wizard.control.cancel.description"));
        i18nMap.put(Wizard.HELP_I18N_DESCRIPTION, Translation
            .getTranslation("wizard.control.help.description"));

        wizard.setI18NMap(i18nMap);
        dialog.getRootPane().setDefaultButton(wizard.getNextButton());

        wizard.addWizardListener(new WizardListener() {
            public void wizardFinished(Wizard wizard) {
                dialog.setVisible(false);
                dialog.dispose();
            }

            public void wizardCancelled(Wizard wizard) {
                dialog.setVisible(false);
                dialog.dispose();
            }

            public void wizardPanelChanged(Wizard wizard) {
            }
        });

        dialog.getContentPane().add(
            GradientPanel.create(wizard, GradientPanel.VERY_LIGHT_GRAY));
        // dialog.getContentPane().add(wizard);
        dialog.pack();
        int x = ((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() - dialog
            .getWidth()) / 2;
        int y = ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - dialog
            .getHeight()) / 2;
        dialog.setLocation(x, y);
        wizard.getContext().setAttribute(
            WizardContextAttributes.DIALOG_ATTRIBUTE, dialog);
        getUIController().setWizardDialogReference(dialog);
        getUIController().setActiveFrame(UIController.WIZARD_DIALOG_ID);
    }
}