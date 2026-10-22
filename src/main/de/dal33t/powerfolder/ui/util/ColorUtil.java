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
package de.dal33t.powerfolder.ui.util;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.UIManager;

import de.dal33t.powerfolder.util.MathUtil;
import de.dal33t.powerfolder.util.UIThemeState;

/**
 * Class for any reusable color functions.
 */
public class ColorUtil {

    /** Color for odd rows in tables (light mode). */
    public static final Color ODD_TABLE_ROW_COLOR = new Color(240, 240, 240);

    /** Color for even rows in tables (light mode). */
    public static final Color EVEN_TABLE_ROW_COLOR = Color.white;

    /**
     * PFC-3643: theme-aware even-row (zebra) background. The light colour in light
     * mode; the LaF table background in dark mode - the hardcoded light colours above
     * painted white row bands under the near-white dark-mode text, hiding it. Table
     * renderers must call this instead of {@link #EVEN_TABLE_ROW_COLOR} directly.
     */
    public static Color getEvenTableRowColor() {
        return UIThemeState.isDark() ? darkTableBackground() : EVEN_TABLE_ROW_COLOR;
    }

    /**
     * PFC-3643: theme-aware odd-row (zebra) background - light grey in light mode, a
     * subtly lifted dark shade in dark mode. See {@link #getEvenTableRowColor()}.
     */
    public static Color getOddTableRowColor() {
        return UIThemeState.isDark()
            ? lighten(darkTableBackground(), 12) : ODD_TABLE_ROW_COLOR;
    }

    /** The active LaF's table background, with a dark fallback. */
    private static Color darkTableBackground() {
        Color bg = UIManager.getColor("Table.background");
        return bg != null ? bg : new Color(0x3C, 0x3F, 0x41);
    }

    /** Return {@code c} lightened by {@code delta} per channel (clamped to 255). */
    private static Color lighten(Color c, int delta) {
        return new Color(Math.min(255, c.getRed() + delta),
            Math.min(255, c.getGreen() + delta),
            Math.min(255, c.getBlue() + delta));
    }

    /**
     * Creates a '#rrggbb' String for a Color.
     *
     * @param color
     * @return
     */
    public static String getRgbForColor(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        StringBuilder sb = new StringBuilder("#");
        sb.append(MathUtil.toHexByte(red));
        sb.append(MathUtil.toHexByte(green));
        sb.append(MathUtil.toHexByte(blue));
        return sb.toString();
    }

    /**
     * Method to get the Synthetica text foreground color. This is not the same
     * color as normal getForeground().
     *
     * @return
     */
    public static Color getTextForegroundColor() {
        Object object = UIManager.getColor("Label.foreground");
        Color color;
        if (object != null && object instanceof Color) {
            color = (Color) object;
        } else {
            // Fall back, in case of UIManager problem.
            color = new JLabel().getForeground();
        }
        return color;
    }

    /**
     * Theme-aware inactive/disabled text colour. Use instead of
     * {@code SystemColor.textInactiveText}, which does not follow the current
     * look and feel / macOS dark mode.
     *
     * @return the disabled label foreground of the active LaF (light in dark
     *         mode, dark in light mode).
     */
    public static Color getTextInactiveColor() {
        Object object = UIManager.getColor("Label.disabledForeground");
        if (object == null) {
            object = UIManager.getColor("textInactiveText");
        }
        return object instanceof Color ? (Color) object : getTextForegroundColor();
    }
}
