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

import com.jgoodies.forms.factories.Borders;

import javax.swing.*;
import java.awt.*;


public class JButton3Icons extends JButton {

    public JButton3Icons(Icon normalIcon, Icon hoverIcon, Icon pushIcon) {
        super(normalIcon);
        setOpaque(false);
        setBorder(Borders.EMPTY_BORDER);
        setPressedIcon(pushIcon);
        setRolloverIcon(hoverIcon);
        setMargin(new Insets(0, 0, 0, 0));
    }

    public void setIcons(Icon normalIcon, Icon hoverIcon, Icon pushIcon) {
        setIcon(normalIcon);
        setPressedIcon(pushIcon);
        setRolloverIcon(hoverIcon);
    }
}
