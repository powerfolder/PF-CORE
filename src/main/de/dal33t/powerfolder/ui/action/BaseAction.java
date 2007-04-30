/* $Id: BaseAction.java,v 1.6 2005/11/04 14:08:24 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Superclass for all actions used in pf
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public abstract class BaseAction extends AbstractAction {
    private Logger log;
    private Controller controller;

    /**
     * Initalizes a action tranlated and loaded from the tranlastion/resource
     * file
     * 
     * @param actionId
     * @param controller
     */
    protected BaseAction(String actionId, Controller controller) {
        this(null, null, controller);
        configureFromActionId(actionId);
    }

    /**
     * @param name
     * @param icon
     * @param controller
     */
    protected BaseAction(String name, Icon icon, Controller controller) {
        super(name, icon);
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;
    }

    // I18n *******************************************************************

    /**
     * Initalizes action settings translated. Action gets initalized by
     * actionId. settings are: name, mnemonic key, description and icon.
     * 
     * @param actionId
     *            the action id
     */
    protected void configureFromActionId(String actionId) {
        // log().verbose("Configuring from id: " + actionId);
        putValue(Action.NAME, Translation.getTranslation(actionId + ".name"));
        setMnemonicKey(Translation.getTranslation(actionId + ".key"));
        putValue(Action.SHORT_DESCRIPTION, Translation.getTranslation(actionId
            + ".description"));
        Icon icon = Icons.getIconById(actionId + ".icon");
        if (icon != null && icon.getIconHeight() != -1) { // check if valid
            putValue(Action.SMALL_ICON, icon);
        }
    }

    // Helper methods *********************************************************

    /**
     * @return the name of this action
     */
    public String getName() {
        return (String) getValue(Action.NAME);
    }

    /**
     * Convinience setter for the mnemonic key
     * 
     * @param key
     */
    private void setMnemonicKey(String key) {
        if (!StringUtils.isBlank(key)) {
            putValue(Action.MNEMONIC_KEY, new Integer(Character.toUpperCase(key
                .charAt(0))));
        } else {
            putValue(Action.MNEMONIC_KEY, null);
        }
    }

    /**
     * @return the assosiated controller
     */
    protected Controller getController() {
        return controller;
    }

    /**
     * @return ui controller
     */
    protected UIController getUIController() {
        return controller.getUIController();
    }

    /**
     * @return a logger for this action
     */
    protected Logger log() {
        if (log == null) {
            log = Logger.getLogger(this);
        }
        return log;
    }

    // General ****************************************************************

    public String toString() {
        return getName();
    }
}