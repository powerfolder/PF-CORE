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
 * $Id: CursorUtils.java 15990 2011-08-03 12:43:11Z harry $
 */
package de.dal33t.powerfolder.ui.util;

import java.awt.*;

/**
 * Cursor utilities.
 *
 * The general approach is
 * 1) to set a component's cursor to something like WAIT
 * and remember the original cursor,
 * 2) carry out some task,
 * 3) return the cursor back to the original when complete.
 */
public class CursorUtils {

    public static Cursor setDefaultCursor(Component component) {
        return setCursor(component,
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public static Cursor setWaitCursor(Component component) {
        return setCursor(component,
                Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public static Cursor setHandCursor(Component component) {
        return setCursor(component,
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void returnToOriginal(Component component, Cursor original) {
        component.setCursor(original);
    }

    private static Cursor setCursor(Component component, Cursor cursor) {
        Cursor current = component.getCursor();
        component.setCursor(cursor);
        return current;
    }
}
