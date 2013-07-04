package jwf;

import javax.swing.JPanel;
import javax.swing.border.Border;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;

/**
 * The base class used for implementing a panel that is displayed in a Wizard.
 * 
 * @author Christopher Brind
 */
public abstract class WizardPanel extends JPanel {

    /** The context of the wizard process. */
    private WizardContext wizardContext;

    /** Sets the context this wizard should use. */
    protected void setWizardContext(WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    /** Called when the panel is set. */
    public abstract void display();

    /**
     * Is there be a next panel?
     * 
     * @return true if there is a panel to move to next
     */
    public abstract boolean hasNext();

    /**
     * Called to validate the panel before moving to next panel.
     * 
     * @param panelList
     *            a List of error messages to be displayed.
     * @return true if the panel is valid,
     */
    public abstract boolean validateNext();

    /** Get the next panel to go to. */
    public abstract WizardPanel next();

    /**
     * Can this panel finish the wizard?
     * 
     * @return true if this panel can finish the wizard.
     */
    public abstract boolean canFinish();

    /**
     * Whether a wizard can go back to this panel. Some flows may not allow this
     * like where folders have been created.
     * 
     * @return
     */
    public boolean canGoBackTo() {
        return true;
    }

    /**
     * Can this panel cancel the wizard? Terminating panels, where Finish is the
     * only logical option, should set this to false.
     * 
     * @return true if this panel can cancel the wizard.
     */
    public abstract boolean canCancel();

    /**
     * Called to validate the panel before finishing the wizard. Should return
     * false if canFinish returns false.
     * 
     * @param panelList
     *            a List of error messages to be displayed.
     * @return true if it is valid for this wizard to finish.
     */
    public abstract boolean validateFinish();

    /** Handle finishing the wizard. */
    public abstract void finish();

    /**
     * Has this panel got help? Defaults to false, override to change.
     * 
     * @return false if there is no help for this panel.
     */
    public boolean hasHelp() {
        return false;
    }

    /** Override this method to provide help. */
    public void help() {
    }

    /**
     * Get the wizard context.
     * 
     * @return a WizardContext object
     */
    protected WizardContext getWizardContext() {
        return wizardContext;
    }

    /**
     * @return the wizard
     */
    protected Wizard getWizard() {
        return (Wizard) getWizardContext().getAttribute(
            WizardContextAttributes.WIZARD_ATTRIBUTE);
    }

    /**
     * Method for updating the buttons
     */
    protected void updateButtons() {
        if (wizardContext == null) {
            return;
        }
        if (wizardContext
            .getAttribute(WizardContextAttributes.WIZARD_ATTRIBUTE) instanceof Wizard)
        {
            Wizard wizard = (Wizard) wizardContext
                .getAttribute(WizardContextAttributes.WIZARD_ATTRIBUTE);
            wizard.updateButtons();
        }
    }

    protected Border createFewContentBorder() {
        if (getWizard().isTiny()) {
            return Borders.createEmptyBorder("0, 0, 0, 0");
        } else {
            return Borders.createEmptyBorder("30dlu, 10dlu, 0, 0");            
        }
    }
}