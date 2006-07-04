/* $Id: PFUIComponent.java,v 1.2 2006/03/04 20:57:41 schaatser Exp $
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.ui.UIController;

/**
 * A element which is owned by a ui controller
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class PFUIComponent extends PFComponent {
    /**
     * @param controller
     */
    protected PFUIComponent(Controller controller) {
        super(controller);
    }

    /**
     * Answers the ui controller gives acces to all user interface componenets *
     * 
     * @return The UIController.
     */
    protected UIController getUIController() {
        return getController().getUIController();
    }

}
