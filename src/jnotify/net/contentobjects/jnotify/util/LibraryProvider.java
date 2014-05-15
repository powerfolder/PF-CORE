/*******************************************************************************
 * JNotify - Allow java applications to register to File system events.
 *
 * Copyright (C) 2010 - Content Objects
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 ******************************************************************************
 *
 * Content Objects, Inc., hereby disclaims all copyright interest in the
 * library `JNotify' (a Java library for file system events).
 *
 * Yahali Sherman, 21 November 2005
 *    Content Objects, VP R&D.
 *
 ******************************************************************************
 * Author : Christian Sprajc (http://www.powerfolder.com)
 ******************************************************************************/

package net.contentobjects.jnotify.util;

public abstract class LibraryProvider {

    private static LibraryProvider INSTANCE = new SystemLibraryLoader();

    public static void set(LibraryProvider provider) {
        if (provider == null) {
            throw new NullPointerException("Provider");
        }
        INSTANCE = provider;
    }

    public static LibraryProvider get() {
        return INSTANCE;
    }

    /**
     * Central method to load a library. Override this if you require a
     * different mechanism to load libs.
     *
     * @param libName
     */
    public abstract void loadLibrary(String libName);

    private static final class SystemLibraryLoader extends LibraryProvider {
        @Override
        public void loadLibrary(String libName) {
            System.loadLibrary(libName);
        }
    }
}
