/*
 * $Id: WizardAdapter.java,v 1.1 2005/04/05 12:37:47 totmacherr Exp $
 *
 * $Log: WizardAdapter.java,v $
 * Revision 1.1  2005/04/05 12:37:47  totmacherr
 * Added wizard framework
 *
 */

package jwf;

/** This class provides means of abreviating work when using the WizardListener
 * allowing the developer to implement only the needed methods
 *
 * @author  rodrigomalara@users.sourceforge.net
 */
public abstract class WizardAdapter implements WizardListener {

    /** Creates a new instance of WizardAdapter */
    public WizardAdapter() {}

    /** Called when the wizard is cancelled.
     * @param wizard the wizard that was cancelled.
     */
    public void wizardCancelled(Wizard wizard) {}

    /** Called when the wizard finishes.
     * @param wizard the wizard that finished.
     */
    public void wizardFinished(Wizard wizard) {}

    /** Called when a new panel has been displayed in the wizard.
     * @param wizard the wizard that was updated
     */
    public void wizardPanelChanged(Wizard wizard) {}

}
