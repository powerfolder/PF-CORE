/* $Id: PFWizard.java,v 1.8 2005/11/20 00:22:27 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;

import jwf.Wizard;
import jwf.WizardContext;
import jwf.WizardListener;
import jwf.WizardPanel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
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

    // The size of a medium sized font, e.g. the big subpoints on a wizard
    static final int MED_FONT_SIZE = 15;
    
    // The attribute in the wizard context of the success panel. Displayed at
    // end
    public static final String SUCCESS_PANEL = "successpanel";

    // The active pictogram as JLabel
    public static final String PICTO_ICON = "pictoicon";

    private JDialog dialog;
    private Wizard wizard;

    /**
     * @param controller
     *            the controller
     */
    public PFWizard(Controller controller) {
        super(controller);
        wizard = new Wizard();
    }

    /**
     * Opens the wizard for the basic setup.
     * 
     * @param controller
     */
    public static void openBasicSetupWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller);
        wizard.open(new BasicSetupPanel(controller));
    }

    /**
     * Opens the what todo wizard. Qizard to create / join folders.
     * 
     * @param controller
     */
    public static void openWhatToDoWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller);
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
        PFWizard wizard = new PFWizard(controller);
        wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.PROJECT_WORK_PICTO);
        wizard.getWizardContext().setAttribute(
            FOLDERINFO_ATTRIBUTE, foInfo);

        TextPanelPanel successPanel = new TextPanelPanel(controller,
            Translation.getTranslation("wizard.sendinvitations.sendsuccess"),
            Translation
                .getTranslation("wizard.sendinvitations.sendsuccessinfo"));
        wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
            successPanel);

        wizard.open(new SendInvitationsPanel(controller, false));
    }

    /**
     * Opens the wizard to setup a new webservice mirror.
     * 
     * @param controller
     * @param folderSetupAfterwards
     *            true if the folder backup setup dialog should be shown after
     *            successfully login.
     */
    public static void openLoginWebServiceWizard(Controller controller,
        boolean folderSetupAfterwards)
    {
        PFWizard wizard = new PFWizard(controller);
        wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.WEBSERVICE_PICTO);
        WizardPanel nextFinishPanel;
        if (folderSetupAfterwards) {
            nextFinishPanel = new MirrorFolderPanel(controller);
        } else {
            nextFinishPanel = new TextPanelPanel(controller,
                "Online Storage Login Successful",
                "The WebService is now correctly setup.\n"
                    + "You may now start to backup Folders to it.");
        }
        wizard.open(new LoginWebServicePanel(controller, nextFinishPanel, true));
    }

    /**
     * Opens the wizard to setup a new webservice mirror.
     * 
     * @param controller
     */
    public static void openMirrorFolderWizard(Controller controller) {
        PFWizard wizard = new PFWizard(controller);
        wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.WEBSERVICE_PICTO);
        // TextPanelPanel successPanel = new TextPanelPanel(controller,
        // Translation.getTranslation("wizard.sendinvitations.sendsuccess"),
        // Translation
        // .getTranslation("wizard.sendinvitations.sendsuccessinfo"));
        // wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
        // successPanel);
        wizard.open(new MirrorFolderPanel(controller));
    }

    /**
     * Opens the wizard on a panel.
     * 
     * @param wizardPanel
     * @param shift amount to shift the wizard when called by another wizard
     */
    public void open(PFWizardPanel wizardPanel) {
        Reject.ifNull(wizardPanel, "Wizardpanel is null");
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

    /**
     * @return the dialog the wizard is opened in
     */
    public JDialog getDialog() {
        return dialog;
    }

    private void buildUI() {
        // Build the wizard
        dialog = new JDialog(getUIController().getMainFrame().getUIComponent(),
            Translation.getTranslation("wizard.pfwizard.title"), false); // Wizard
        // dialog.setUndecorated(true);
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

        dialog.getContentPane().add(wizard);
        dialog.pack();
        Component parent = dialog.getOwner();
        int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
        int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
    }
}