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

import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;

/**
 * Plugin Interface to PowerFolder. We recomment extending AbstractPFPlugin.<BR>
 * add plugins by changing the plugin= setting in the config file. This should
 * be a comma ',' seperated list of classname in the classpath.<BR>
 * In the preferences dialog of PowerFolder the currently installed plugins will
 * be listed.<BR>
 * If you plugins has a settings dialog hasOptionsDialog should return true.
 * Then this options dialog will be available by selecting the plugin in the
 * preferences dialog and clicking the settings button.<BR>
 * Your plugin should take care of its own settings, the best way to do that is
 * access the configfile like this:
 * <code>Properties config = getController().getConfig();</code>
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public interface Plugin {
    /**
     * @return the name of the plugin
     */
    String getName();

    /**
     * @return the description of the plugin
     */
    String getDescription();

    /**
     * Called at the very beginning of Controller start. For every plugin
     * (enabled and disabled)
     */
    void init();

    /** called to (re) start the plugin. Only for enabled plugins */
    void start();

    /**
     * called to stop the plugin (e.g. on program exit). Only for enabled
     * plugins
     */
    void stop();

    /**
     * called before the plugin and pluginmanager are shutdown. For every plugin
     * (enabled and disabled)
     */
    void destroy();

    /**
     * does this plugin has an options dialog?
     *
     * @return true if options are available
     */
    boolean hasOptionsDialog();

    /**
     * should show an options dialog
     *
     * @param prefDialog
     *            the preferences dialog
     */
    public void showOptionsDialog(PreferencesDialog prefDialog);
}
