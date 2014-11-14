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
* $Id: LinkJButton.java 4746 2008-07-28 11:34:36Z tot $
*/
package de.dal33t.powerfolder.ui.widget;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import de.dal33t.powerfolder.util.BrowserLauncher;

/**
 * A JButton that links to a URL.
 */
public class LinkJButton extends JButtonMini {

    private static final Logger log = Logger.getLogger(LinkJButton.class.getName());

    public LinkJButton(Icon icon, String toolTipText, final String url) {
        super(icon, toolTipText);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (IOException e1) {
                    log.log(Level.SEVERE, "IOException", e1);
                }
            }
        });
    }
}
