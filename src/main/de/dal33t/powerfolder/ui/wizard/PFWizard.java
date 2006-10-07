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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
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
    public final static String SUCCESS_PANEL = "successpanel";

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