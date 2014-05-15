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
import de.dal33t.powerfolder.net.RelayFinder;

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
     * @return the short binary name for the branding. e.g. for l4j.ini file or
     *         jar-file retrieval. by default "PowerFolder".
     */
    String getBinaryName();

    /**
     * Initializes the branding. This should take care of load: Icon set,
     * Translation texts, Preconfiguration file.
     *
     * @param controller
     *            the controller this branding is loaded on
     */
    void init(Controller controller);

    /**
     * @return true if this client is a branded client (non PowerFolder)
     */
    public boolean isBrandedClient();

    /**
     * @return true if the credentials in the about box should be shown.
     */
    boolean showCredentials();

    /**
     * @return true if this distribution is allowed to choose from different
     *         skins.
     */
    boolean allowSkinChange();

    /**
     * @return the relayfinder for this distributon. return null for default.
     */
    RelayFinder createRelayFinder();

}
