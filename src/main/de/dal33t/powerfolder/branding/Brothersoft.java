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
 * $Id: ServerClient.java 6435 2009-01-21 23:35:04Z tot $
 */
package de.dal33t.powerfolder.branding;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Updater.UpdateSetting;

public class Brothersoft extends AbstractBranding {

    public String getName() {
        return "Brothersoft";
    }

    public void init(Controller controller) {
        loadPreConfiguration("brothersoft", controller.getConfig());
    }

    public boolean supportWeb() {
        return true;
    }

    public UpdateSetting createUpdateSettings() {
        return null;
    }
}
