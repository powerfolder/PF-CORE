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
package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class CompoundIcon implements Icon {
    protected static final int[] VALID_ORIENTATION = {SwingConstants.VERTICAL,
        SwingConstants.HORIZONTAL};
    protected int orientation, gap;
    protected Icon iconOne, iconTwo;

    public CompoundIcon(Icon iconOne, Icon iconTwo, int orientation, int gap) {
        if (!isLegalValue(orientation, VALID_ORIENTATION)) {
            throw new IllegalArgumentException(
                "Orientation must be either VERTICAL or HORIZONTAL");
        }
        this.orientation = orientation;
        this.iconOne = iconOne;
        this.iconTwo = iconTwo;
        this.gap = gap;
    }

    public boolean isLegalValue(int value, int[] legal) {
        for (int i = 0; i < legal.length; i++) {
            if (value == legal[i])
                return true;
        }
        return false;
    }

    public int getIconWidth() {
        int widthOne = iconOne.getIconWidth();
        int widthTwo = iconTwo.getIconWidth();
        if (orientation == SwingConstants.VERTICAL) {
            return Math.max(widthOne, widthTwo);
        }
        return widthOne + widthTwo + gap;
    }

    public int getIconHeight() {
        int heightOne = iconOne.getIconHeight();
        int heightTwo = iconTwo.getIconHeight();
        if (orientation == SwingConstants.HORIZONTAL) {
            return Math.max(heightOne, heightOne);
        }
        return heightOne + heightTwo + gap;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        iconOne.paintIcon(c, g, x, y);
        if (orientation == SwingConstants.VERTICAL) {
            y += iconOne.getIconHeight() + gap;
        } else {
            x += iconOne.getIconWidth() + gap;
        }
        iconTwo.paintIcon(c, g, x, y);
    }

    /**
     * Appends an icon to the main icon at the right side leaving a small gap
     * 
     * @param mainIcon
     * @param appendIcon
     * @return the new icon
     */
    public static Icon appendRight(Icon mainIcon, Icon appendIcon) {
        return new CompoundIcon(mainIcon, appendIcon,
            SwingConstants.HORIZONTAL, 3);
    }

    /**
     * Appends an icon to the main icon at the left side leaving a small gap
     * 
     * @param mainIcon
     * @param appendIcon
     * @return the new icon
     */
    public static Icon appendLeft(Icon mainIcon, Icon appendIcon) {
        return new CompoundIcon(appendIcon, mainIcon,
            SwingConstants.HORIZONTAL, 3);
    }
}