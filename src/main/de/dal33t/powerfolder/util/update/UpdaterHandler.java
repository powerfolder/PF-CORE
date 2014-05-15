/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Updater.java 6236 2008-12-31 15:44:10Z tot $
 */
package de.dal33t.powerfolder.util.update;


/**
 * Lister to receive infos
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public interface UpdaterHandler {
    /**
     * @return true if the updater should check for a new version right now.
     */
    boolean shouldCheckForNewVersion();

    /**
     * Should handle a new available release
     *
     * @param event
     */
    void newReleaseAvailable(UpdaterEvent event);

    /**
     * If checked and no new release was available.
     *
     * @param event
     */
    void noNewReleaseAvailable(UpdaterEvent event);
}
