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
package de.dal33t.powerfolder.ui.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.model.BoundPermission;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Superclass for all actions used in pf
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public abstract class BaseAction extends AbstractAction {

    private Controller controller;
    private BoundPermission boundPermission;

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
     * Initializes action settings translated. Action gets initialized by
     * actionId. settings are: name, mnemonic key, description and icon.
     * 
     * @param actionId
     *            the action id
     */
    protected void configureFromActionId(String actionId) {
        if (StringUtils.isBlank(actionId)) {
            return;
        }
        putValue(NAME, Translation.getTranslation(actionId + ".name"));
        setMnemonicKey(Translation.getTranslation(actionId + ".key"));
        putValue(SHORT_DESCRIPTION,
            Translation.getTranslation(actionId + ".description"));
        Icon icon = Icons.getIconById(actionId + ".icon");
        if (icon != null && icon.getIconHeight() != -1) { // check if valid
            putValue(SMALL_ICON, icon);
        }
    }

    protected void setIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }

    // Helper methods *********************************************************

    /**
     * @return the name of this action
     */
    public String getName() {
        return (String) getValue(NAME);
    }

    /**
     * Enables this action only if the user has a certain permission.
     * <p>
     * Does actively listen for account changes and re-logins.
     * 
     * @param permission
     */
    public void allowWith(Permission permission) {
        if (!ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
            .getValueBoolean(getController()))
        {
            // Disabled #1958
            return;
        }
        if (permission != null && boundPermission != null
            && permission.equals(boundPermission.getPermission()))
        {
            // Do nothing. Unchanged.
            return;
        }
        boundPermission = null;
        if (permission != null) {
            boundPermission = new BoundPermission(getController(), permission) {
                public void hasPermission(boolean hasPermission) {
                    setEnabled(hasPermission);
                }
            };
        }
    }

    public void dispose() {
        if (boundPermission != null) {
            boundPermission.dispose();
        }
    }

    /**
     * Convinience setter for the mnemonic key
     * 
     * @param key
     */
    protected void setMnemonicKey(String key) {
        if (StringUtils.isBlank(key)) {
            putValue(MNEMONIC_KEY, null);
        } else {
            putValue(MNEMONIC_KEY,
                Integer.valueOf(Character.toUpperCase(key.charAt(0))));
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

    // General ****************************************************************

    public String toString() {
        return getName();
    }
}