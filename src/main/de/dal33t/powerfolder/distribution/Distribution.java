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
package de.dal33t.powerfolder.distribution;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.util.Updater.UpdateSetting;

/**
 * Interface which describes the information that can be retrieved from an
 * branding.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface Distribution {
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
     * @return true if this client supports registration of new trial account
     *         via activation wizard
     */
    boolean supportsWebRegistration();

    /**
     * @return true if this distribution allows the user to change the server
     *         via Prefs.
     */
    boolean allowUserToSelectServer();

    /**
     * @return the update settings for this branded version. return null to use
     *         default location
     */
    UpdateSetting createUpdateSettings();

    /**
     * #1488
     * 
     * @param node
     * @return true if this node is a relay server.
     */
    boolean isRelay(Member node);
}
