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
package de.dal33t.powerfolder.util.os.Win32;

import de.dal33t.powerfolder.util.os.OSUtil;

public class RecycleDeleteImpl {
    public final static String LIBRARY = "delete";

    public static boolean loadLibrary() {
        return OSUtil.loadLibrary(RecycleDeleteImpl.class, LIBRARY);
    }


    /**
     * @param filename
     *            a fully qualified path/filename
     * @param confirm
     *            show a yes/No confirm dialog
     * @param showProgress
     *            if set to true progress dialog is shown
     */
    public static native void delete(String filename, boolean confirm,
        boolean showProgress);

    /**
     * @param filename
     *            a fully qualified path/filename
     * @param confirm
     *            show a yes/No confirm dialog
     */
    public static native void delete(String filename, boolean confirm);

    /**
     * @param filename
     *            a fully qualified path/filename
     */
    public static native void delete(String filename);

}
