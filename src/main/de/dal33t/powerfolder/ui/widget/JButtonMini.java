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
* $Id: JButton3Icons.java 5009 2008-08-11 01:25:22Z tot $
*/
package de.dal33t.powerfolder.ui.widget;

import com.jgoodies.forms.factories.Borders;

import javax.swing.*;
import java.awt.*;

/**
 * Class showing image button with no border, except when hover or pressed.
 * Uses Synthetica features to do border.
 */
public class JButtonMini extends JButton {

    public JButtonMini(Action action) {
        this((Icon) action.getValue(Action.SMALL_ICON),
                (String) action.getValue(Action.SHORT_DESCRIPTION));
    }

    public JButtonMini(Icon icon, String toolTipText) {
        if (icon == null) {
            setText("???");
        } else {
            setIcon(icon);
        }

        setOpaque(false);
        setBorder(null);
        setBorder(Borders.EMPTY_BORDER);
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
        if (toolTipText != null && toolTipText.trim().length() > 0) {
            setToolTipText(toolTipText);
        }
    }
}
