/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.jni.osx;

import java.util.logging.Logger;

public class Util {

    public static Logger LOG = Logger.getLogger(Util.class.getName());

    public static boolean loaded = false;
    static {
        try {
            System.loadLibrary("osxnative");
            loaded = true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError err) {
            LOG.warning("Could not initialize JNI Mac util: " + err);
        }
    }

    public native static void addLoginItem(String path);
    public native static void removeLoginItem(String path);
    public native static boolean hasLoginItem(String path);
    public native static void addFavorite(String path);
    public native static void removeFavorite(String path);
    public native static boolean isOnLocalVolume(String path);
}
