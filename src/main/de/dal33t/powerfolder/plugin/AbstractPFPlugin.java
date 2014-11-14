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
package de.dal33t.powerfolder.plugin;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;
import de.dal33t.powerfolder.util.Reject;

/**
 * For your convenience an implementation of the plugin interface that does not
 * have a options dialog and is an PFComponent. PFComoment gives you access to
 * the Controller. The Controller is the access point for all main program
 * elelements. Overwrite the hasOptionsDialog() and showOptionsDialog(JDialog
 * parent) if you do have an options dialog.
 */
public abstract class AbstractPFPlugin extends PFComponent implements Plugin {

    /**
     * this contructor will be called always, even if disabled. disabled means
     * that start will not be called, so when overwiting this contructor make
     * sure not to do much in there move all code to start().
     *
     * @param controller
     */
    public AbstractPFPlugin(Controller controller) {
        super(controller);
    }

    public void destroy() {
    }

    public void init() {
    }

    /**
     * @param controller
     * @param searchClass
     * @return the first plugin with the given class.
     */
    public static Plugin findPluginByClass(Controller controller,
        Class<?> searchClass)
    {
        Reject.ifNull(controller, "Controller is null");
        return controller.getPluginManager().findPluginByClass(searchClass);
    }

    /** overwrite this to return true if you implement showOptionsDialog */
    public boolean hasOptionsDialog() {
        return false;
    }

    public void showOptionsDialog(PreferencesDialog prefDialog) {
        throw new IllegalStateException(
            "default no options dialog, overwrite this method");
    }

    public String toString() {
        return getName();
    }

    protected boolean isEnabled() {
        return getPluginManager().isEnabled(this);
    }

    protected PluginManager getPluginManager() {
        return getController().getPluginManager();
    }
}
