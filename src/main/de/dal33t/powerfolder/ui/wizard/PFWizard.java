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
 * $Id: PFWizard.java 19934 2012-10-14 07:29:40Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;

import jwf.Wizard;
import jwf.WizardContext;
import jwf.WizardListener;
import jwf.WizardPanel;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
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
    private static final AtomicInteger NUMBER_OF_OPEN_WIZARDS = new AtomicInteger();

    // Make sure open / close count change fires exactly once per instance.
    private final AtomicBoolean doneWizardClose = new AtomicBoolean();

    private JDialog dialog;
    private final Wizard wizard;
    private final String title;

    /**
     * @param controller
     *            the controller
     * @param title
     */
    public PFWizard(Controller controller, String title) {
        this(controller, title, false);
    }

    /**
     * @param controller
     *            the controller
     * @param title
     */
    public PFWizard(Controller controller, String title, boolean tiny) {
        super(controller);
        this.title = title;
        NUMBER_OF_OPEN_WIZARDS.incrementAndGet();
        // controller.getUIController().getMainFrame().checkOnTop();
        setSuspendNewFolderSearch(true);
        wizard = new Wizard(tiny);
    }

    /**
     * Make absolutely sure decrementOpenWizards() gets called. Should have been
     * called by Window closed / closing.
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        try {
            decrementOpenWizards();
        } finally {
            super.finalize();
        }
    }

    private void decrementOpenWizards() {
        if (!doneWizardClose.getAndSet(true)) {
            NUMBER_OF_OPEN_WIZARDS.decrementAndGet();
            //getController().getUIController().getMainFrame().checkOnTop();
            setSuspendNewFolderSearch(false);
        }
    }

    /**
     * This is a blanket call to disable the autodetect of folders while a
     * Wizard is open. Typically a Wizard will create a directory and the folder
     * repo will sometimes autocreate the folder at the same time. So
     * FolderRepository.setFolderCreateActivity() suspends this while a Wizard
     * is open.
     */
    private void setSuspendNewFolderSearch(boolean suspend) {
        getController().getFolderRepository()
            .setSuspendNewFolderSearch(suspend);
    }

    public static boolean isWizardOpen() {
        return NUMBER_OF_OPEN_WIZARDS.get() > 0;
    }

    /**
     * Opens the wizard for the basic setup.
     *
     * @param controller
     */
    public static void openBasicSetupWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.folder_title"));
        WizardPanel nextPanel = WhatToDoPanel.doSyncOption(controller,
            wizard.getWizardContext(), false);
        wizard.open(new LoginPanel(controller, nextPanel, !controller
            .isBackupOnly()));
    }

    /**
     * Opens the what to do wizard. Wizard to create / join folders.
     *
     * @param controller
     */
    public static void openWhatToDoWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.folder_title"));
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
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.invitation_title"));
        wizard.getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, foInfo);

        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.send_invitations.send_success"),
            Translation
                .getTranslation("wizard.send_invitations.send_success_info"),
            true);
        wizard.getWizardContext().setAttribute(SUCCESS_PANEL, successPanel);

        wizard.open(new SendInvitationsPanel(controller));
    }

    public static void openOnlineStorageJoinWizard(Controller controller,
        List<FolderInfo> folderInfoList)
    {

        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.online_storage_title"));

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_INFOS, folderInfoList);

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE, false);

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"),
            Translation.getTranslation("wizard.success_join"));
        wizard.getWizardContext().setAttribute(SUCCESS_PANEL, successPanel);

        wizard.open(new MultiOnlineStorageSetupPanel(controller));
    }

    public static void openTypicalFolderJoinWizard(Controller controller,
        FolderInfo folderInfo)
    {

        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.folder_title"));

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_INFO, folderInfo);

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE, false);

        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE,
            controller.getOSClient().isBackupByDefault());

        // Setup success panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.setup_success"),
            Translation.getTranslation("wizard.success_join"), true);
        wizard.getWizardContext().setAttribute(SUCCESS_PANEL, successPanel);

        wizard.open(new TypicalFolderSetupPanel(controller));
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
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.invitation_title"));
        wizard.open(panel);
    }

    public static void openLoginWizard(Controller controller,
        ServerClient client)
    {
        boolean tiny = ConfigurationEntry.SHOW_TINY_WIZARDS
            .getValueBoolean(controller);
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.login_title"), tiny);
        WizardPanel nextFinishPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.finish.os_login_title"),
            Translation.getTranslation("wizard.finish.os_login_text"), true);
        wizard.open(new LoginPanel(controller, client, nextFinishPanel, false));
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
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.folder_title"));
        wizard.open(new FolderOnlineStoragePanel(controller, folderToSetup
            .getInfo()));
    }

    public static void openExistingDirectoryWizard(Controller controller,
        Path directory)
    {
        Reject.ifTrue(directory == null || !Files.exists(directory),
            "No directory supplied");
        PFWizard wizard = new PFWizard(controller,
            Translation.getTranslation("wizard.pfwizard.folder_title"));
        wizard.getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE,
            controller.getOSClient().isBackupByDefault());
        wizard.open(new ConfirmDiskLocationPanel(controller, directory));
    }

    public static boolean hideFolderJoinWizard(Controller controller) {
        return ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
            .getValueBoolean(controller)
            && !PreferencesEntry.EXPERT_MODE.getValueBoolean(controller);
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

        if (PFWizard.hideFolderJoinWizard(getController())
            && wizardPanel instanceof MultiOnlineStorageSetupPanel)
        {
            dialog.setVisible(false);
        } else {
            dialog.setVisible(true);
        }
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
        i18nMap.put(Wizard.BACK_I18N,
            Translation.getTranslation("wizard.control.back"));
        i18nMap.put(Wizard.NEXT_I18N,
            Translation.getTranslation("wizard.control.next"));
        i18nMap.put(Wizard.FINISH_I18N,
            Translation.getTranslation("wizard.control.finish"));
        i18nMap.put(Wizard.CANCEL_I18N,
            Translation.getTranslation("wizard.control.cancel"));
        i18nMap.put(Wizard.HELP_I18N,
            Translation.getTranslation("wizard.control.help"));
        i18nMap.put(Wizard.BACK_I18N_DESCRIPTION,
            Translation.getTranslation("wizard.control.back.description"));
        i18nMap.put(Wizard.NEXT_I18N_DESCRIPTION,
            Translation.getTranslation("wizard.control.next.description"));
        i18nMap.put(Wizard.FINISH_I18N_DESCRIPTION,
            Translation.getTranslation("wizard.control.finish.description"));
        i18nMap.put(Wizard.CANCEL_I18N_DESCRIPTION,
            Translation.getTranslation("wizard.control.cancel.description"));
        i18nMap.put(Wizard.HELP_I18N_DESCRIPTION,
            Translation.getTranslation("wizard.control.help.description"));

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

        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                decrementOpenWizards();
            }

            public void windowClosed(WindowEvent e) {
                decrementOpenWizards();
            }
        });

        dialog.getContentPane().add(wizard);
        // GradientPanel.create(wizard, GradientPanel.VERY_LIGHT_GRAY));
        dialog.pack();
        int x = ((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() - dialog
            .getWidth()) / 2;
        int y = ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - dialog
            .getHeight()) / 3;
        dialog.setLocation(x, y);
        wizard.getContext().setAttribute(
            WizardContextAttributes.DIALOG_ATTRIBUTE, dialog);
        getUIController().setWizardDialogReference(dialog);
        getUIController().setActiveFrame(UIController.WIZARD_DIALOG_ID);
    }

    public static void openFolderAutoCreateWizard(Controller controller,
        FolderInfo folderInfo)
    {
        Folder folder = controller.getFolderRepository().getFolder(folderInfo);
        if (folder == null) {
            DialogFactory.genericDialog(controller, Translation
                .getTranslation("wizard.control.no_folder.title"), Translation
                .getTranslation("wizard.control.no_folder.description"),
                GenericDialogType.INFO);
        } else {
            FolderAutoCreatePanel panel = new FolderAutoCreatePanel(controller,
                folderInfo);
            PFWizard wizard = new PFWizard(controller,
                Translation.getTranslation("wizard.pfwizard.folder_title"));
            wizard.open(panel);
        }
    }

    public static void openMultiFileRestoreWizard(Controller controller,
        List<FileInfo> fileInfosToRestore)
    {
        PFWizard wizard = new PFWizard(controller, Translation.getTranslation("wizard.pfwizard.restore_title"));
        if (fileInfosToRestore.size() == 1) {
            // Just one file? Process it singley.
            FileInfo fInfo = fileInfosToRestore.get(0);
            Folder folder = fInfo.getFolder(controller.getFolderRepository());
            wizard.open(new SingleFileRestorePanel(controller, folder, fInfo));
        } else {
            wizard.open(new MultiFileRestorePanel(controller, fileInfosToRestore));
        }
    }

    public static void openSingleFileRestoreWizard(Controller controller, Folder folder, FileInfo fileInfoToRestore,
                                                   FileInfo selectedFileInfo) {
        PFWizard wizard = new PFWizard(controller, Translation.getTranslation("wizard.pfwizard.restore_title"));
        wizard.open(new SingleFileRestorePanel(controller, folder, fileInfoToRestore, selectedFileInfo));
    }
}