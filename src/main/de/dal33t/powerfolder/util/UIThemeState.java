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
*
*/
package de.dal33t.powerfolder.util;

/**
 * PFC-3643: the dark/light state currently applied to the UI, in a neutral location
 * so non-ui code (e.g. the log {@code DocumentHandler}) can colour itself for the
 * theme without depending upward on the {@code ui} package. The {@code ui} layer
 * ({@code LookAndFeelSupport}) is the single writer; everyone else only reads.
 */
public final class UIThemeState {

    private static volatile boolean dark;

    private UIThemeState() {
    }

    /** @return {@code true} when the UI is currently in dark mode. */
    public static boolean isDark() {
        return dark;
    }

    /** Set by {@code LookAndFeelSupport} whenever it applies a light/dark theme. */
    public static void setDark(boolean dark) {
        UIThemeState.dark = dark;
    }
}
