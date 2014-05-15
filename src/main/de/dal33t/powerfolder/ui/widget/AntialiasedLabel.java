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
package de.dal33t.powerfolder.ui.widget;

import javax.swing.*;
import java.awt.*;

/**
 * A Label which supported antialiasing.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AntialiasedLabel extends JLabel {
    private static final int AA_TRIGGER_FONT_SIZE = 14;

    private boolean antialiased;

    public AntialiasedLabel() {
        super();
    }

    public AntialiasedLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    public AntialiasedLabel(Icon image) {
        super(image);
    }

    public AntialiasedLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    public AntialiasedLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    public AntialiasedLabel(String text) {
        super(text);
    }

    public void paintComponent(Graphics g) {
        if (antialiased) {
            ((Graphics2D) g).addRenderingHints(new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
        }
        super.paintComponent(g);
    }

    private void setAntialiased(boolean newValue) {
        boolean oldValue = antialiased;
        antialiased = newValue;
        if (newValue != oldValue) {
            repaint();
        }
    }

    @Override
    public void setFont(Font font)
    {
        if (font.getSize() > AA_TRIGGER_FONT_SIZE) {
            setAntialiased(true);
        }
        super.setFont(font);
    }
}
