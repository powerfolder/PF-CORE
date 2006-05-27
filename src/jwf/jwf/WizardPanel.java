package jwf;

import java.util.List;

import javax.swing.JPanel;

/**
 * The base class used for implementing a panel that is displayed in a Wizard.
 * 
 * @author Christopher Brind
 */
public abstract class WizardPanel extends JPanel {
    /** A default constructor. */
    public WizardPanel() {
    }

    /** The context of the wizard process. */
    protected WizardContext wizardContext;

    /** Sets the context this wizard should use. */
    protected final void setWizardContext(WizardContext wizardContext) {
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
     * @param list
     *            a List of error messages to be displayed.
     * @return true if the panel is valid,
     */
    public abstract boolean validateNext(List list);

    /** Get the next panel to go to. */
    public abstract WizardPanel next();

    /**
     * Can this panel finish the wizard?
     * 
     * @return true if this panel can finish the wizard.
     */
    public abstract boolean canFinish();

    /**
     * Called to validate the panel before finishing the wizard. Should return
     * false if canFinish returns false.
     * 
     * @param list
     *            a List of error messages to be displayed.
     * @return true if it is valid for this wizard to finish.
     */
    public abstract boolean validateFinish(List list);

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
    public final WizardContext getWizardContext() {
        return wizardContext;
    }

    /**
     * Method for updating the buttons
     */
    protected final void updateButtons() {
        if (wizardContext == null) {
            return;
        }
        if (wizardContext.getAttribute(Wizard.WIZARD_ATTRIBUTE) instanceof Wizard)
        {
            Wizard wizard = (Wizard) wizardContext
                .getAttribute(Wizard.WIZARD_ATTRIBUTE);
            wizard.updateButtons();
        }
    }
}