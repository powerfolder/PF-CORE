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
package de.dal33t.powerfolder.util;

/**
 * Class for any reusable math functions.
 */
public class MathUtil {

    /** A table of hex digits */
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Convert a nibble to a hex character
     *
     * @param nibble
     *            the nibble to convert.
     * @return
     */
    public static char toHexNibble(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /**
     * Converts a byte to a hex String.
     *
     * @param bite
     *           the byte to convert
     * @return
     */
    public static String toHexByte(int bite) {
        return String.valueOf(toHexNibble(bite >> 4)) + toHexNibble(bite);
    }

}
