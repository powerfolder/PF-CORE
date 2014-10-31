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
package de.dal33t.powerfolder.ui.util;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;

/**
 * Icon helper which makes it possible to decorate an icon with another one.
 * Overlay, like windows shortcut or shares hand
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class OverlayedIcon implements Icon {
    protected static final int[] VALID_X = {SwingConstants.LEFT,
        SwingConstants.RIGHT, SwingConstants.CENTER};
    protected static final int[] VALID_Y = {SwingConstants.TOP,
        SwingConstants.BOTTOM, SwingConstants.CENTER};

    protected Icon mainIcon, overlayIcon;
    protected int yAlignment = SwingConstants.BOTTOM;
    protected int xAlignment = SwingConstants.LEFT;

    public OverlayedIcon(Icon mainIcon, Icon overlayIcon, int xAlignment,
        int yAlignment)
    {
        if (overlayIcon.getIconWidth() > mainIcon.getIconWidth()) {
            throw new IllegalArgumentException(
                "decorator icon is wider than main icon");
        }
        if (overlayIcon.getIconHeight() > mainIcon.getIconHeight()) {
            throw new IllegalArgumentException(
                "decorator icon is higher than main icon");
        }
        if (!isLegalValue(xAlignment, VALID_X)) {
            throw new IllegalArgumentException(
                "xAlignment must be LEFT, RIGHT or CENTER");
        }
        if (!isLegalValue(yAlignment, VALID_Y)) {
            throw new IllegalArgumentException(
                "yAlignment must be TOP, BOTTOM or CENTER");
        }

        this.mainIcon = mainIcon;
        this.overlayIcon = overlayIcon;
        this.xAlignment = xAlignment;
        this.yAlignment = yAlignment;
    }

    public boolean isLegalValue(int value, int[] legal) {
        for (int i = 0; i < legal.length; i++) {
            if (value == legal[i])
                return true;
        }
        return false;
    }

    public int getIconWidth() {
        return mainIcon.getIconWidth();
    }

    public int getIconHeight() {
        return mainIcon.getIconHeight();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        mainIcon.paintIcon(c, g, x, y);
        int w = getIconWidth();
        int h = getIconHeight();
        if (xAlignment == SwingConstants.CENTER) {
            x += (w - overlayIcon.getIconWidth()) / 2;
        }
        if (xAlignment == SwingConstants.RIGHT) {
            x += (w - overlayIcon.getIconWidth());
        }
        if (yAlignment == SwingConstants.CENTER) {
            y += (h - overlayIcon.getIconHeight()) / 2;
        }
        if (yAlignment == SwingConstants.BOTTOM) {
            y += (h - overlayIcon.getIconHeight());
        }
        overlayIcon.paintIcon(c, g, x, y);
    }

    /**
     * Overlays an main icon with an overlay icon
     *
     * @param mainIcon
     * @param decorator
     * @return
     */
    public static Icon overlayWith(Icon mainIcon, Icon decorator) {
        return new OverlayedIcon(mainIcon, decorator, SwingConstants.CENTER,
            SwingConstants.CENTER);
    }
}