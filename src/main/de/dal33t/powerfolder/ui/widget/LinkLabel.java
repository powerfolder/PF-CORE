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

import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.ui.ColorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jgoodies.forms.factories.Borders;

/**
 * A Label which opens a given link by click it
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class LinkLabel extends JLabel {

    private static final Logger log = Logger.getLogger(LinkLabel.class.getName());
    private String url;

    public LinkLabel(String aText, String aUrl) {

        setTextAndURL(aText, aUrl);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (IOException e1) {
                    log.log(Level.SEVERE, "IOException", e1);
                }
            }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // FIXME This is a hack because of "Fusch!"
        setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
    }

    public void setTextAndURL(String text, String url) {
        this.url = url;
        Object object = UIManager.getColor("Label.foreground");
        Color color;
        if (object != null && object instanceof Color) {
            color = (Color) object;
        } else {
            // Fall back, in case of UIManager problem.
            color = getForeground();
        }
        String rgb = ColorUtil.getRgbForColor(color);

        setText("<html><font color=\"" + rgb + "\"><a href=\"" + url + "\">" + text
            + "</a></font></html>");
    }
}