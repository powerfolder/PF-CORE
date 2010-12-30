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
 * $Id: SystemUtil.java 14679 2010-12-21 13:39:27Z tot $
 */
package de.dal33t.powerfolder.util.os;

import de.dal33t.powerfolder.Feature;

import java.util.logging.Logger;
import java.io.IOException;

public class SystemUtil {

    private static final Logger log =
            Logger.getLogger(SystemUtil.class.getName());

    /**
     * Shutdown local computer.
     * @return
     */
    public static boolean shutdown() {
        // Keep isShutdownSupported() in sync with this method's abilities.
        if (OSUtil.isWindowsSystem()) {
            try {
                Process proc = Runtime.getRuntime().exec("shutdown -s");
                proc.getOutputStream();
                proc.waitFor();
                return true;
            } catch (InterruptedException e) {
                log.severe(e.getMessage());
            } catch (IOException e) {
                log.severe(e.getMessage());
            }
        }
        return false;
    }

    /**
     * Returns true if shutdown() is functional for this OS.
     * @return
     */
    public static boolean isShutdownSupported() {
        if (Feature.SYNC_SHUTDOWN.isDisabled()) {
            return false;
        }
        return OSUtil.isWindowsSystem();
    }
}
