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

/**
 * Interface which describes the information that can be retrieved from an
 * branding.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface Branding {
    /**
     * @return name of the branding
     */
    String getName();

    /**
     * Initializes the branding. This should take care of load: Icon set,
     * Translation texts, Preconfiguration file.
     * 
     * @param controller
     *            the controller this branding is loaded on
     */
    void init(Controller controller);

    /**
     * @return true if this branding should link/use the web capabilities of the
     *         attached online storage server.
     */
    boolean supportWeb();

    /**
     * @return the update settings for this branded version. return null to use
     *         default location
     */
    UpdateSetting createUpdateSettings();
}
