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
* $Id: ColorUtil.java 6135 2008-12-24 08:04:17Z harry $
*/
package de.dal33t.powerfolder.ui.util;

import de.dal33t.powerfolder.util.MathUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Class for any reusable color functions.
 */
public class ColorUtil {

    /** Color for odd rows in tables. */
    public static final Color ODD_TABLE_ROW_COLOR = new Color(240, 240, 240);

    /** Color for even rows in tables. */
    public static final Color EVEN_TABLE_ROW_COLOR = Color.white;

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
}
