/*
 * Copyright 2004 - 2016 Christian Sprajc. All rights reserved.
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

package de.dal33t.powerfolder.util.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities for linux
 *
 * @author <a href="mailto:kappel@powerfolder">Christoph Kappel</a>
 * @version $Id$
 */
public class LinuxUtil {

    /**
     * Get current desktop environment (like: Unity, KDE) based on $XDG_CURRENT_DESKTOP env variable
     *
     * @return name of current desktop environment or null if unset
     */
    public static String getDesktopEnvironment() {
        return System.getenv("XDG_CURRENT_DESKTOP");
    }

    /**
     * Get path to desktop directory based on system settings
     *   Order: $XDG_CURRENT_DIR > xdg-user-dir > $HOME/Desktop
     *
     * @return path to the desktop dir or null if none set/found
     */
    public static Path getDesktopDirPath() {
        /* 1. Check environment variable XDG_DESKTOP_DIR */
        String path = System.getenv("XDG_DESKTOP_DIR");

        if (null == path) {
            /* 2. Check commandline tool xdg-user-dir */
            ProcessBuilder pb = new ProcessBuilder("xdg-user-dir", "DESKTOP");

            try {
                Process proc = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));

                path = reader.readLine();
            } catch (IOException e) {
                /* We ignore that here */
            }

            if (null == path) {
                /* 3. Fallback to educated guess */
                path = "" + System.getProperty("user.home") + ///< Avoid NullPointerException when property is unset
                    System.getProperty("file.separator") + "Desktop";
            }
        }

        return (null != path ? Paths.get(path) : null);
    }
}
