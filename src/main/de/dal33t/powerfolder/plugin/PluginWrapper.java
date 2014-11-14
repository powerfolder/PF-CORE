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
import de.dal33t.powerfolder.util.Reject;

/**
 * Plugin wrapper.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class PluginWrapper implements Plugin {
    private Plugin deligate;

    public PluginWrapper(Plugin plugin) {
        Reject.ifNull(plugin, "plugin is null");
        this.deligate = plugin;
    }

    public Plugin getDeligate() {
        return deligate;
    }

    public String getDescription() {
        return deligate.getDescription();
    }

    public String getName() {
        return deligate.getName();
    }

    public void start() {
        deligate.start();
    }

    public void stop() {
        deligate.stop();
    }

    public void init() {
        deligate.init();
    }

    public void destroy() {
        deligate.destroy();
    }

    public boolean hasOptionsDialog() {
        return deligate.hasOptionsDialog();
    }

    public void showOptionsDialog(PreferencesDialog prefDialog) {
        deligate.showOptionsDialog(prefDialog);
    }

}
