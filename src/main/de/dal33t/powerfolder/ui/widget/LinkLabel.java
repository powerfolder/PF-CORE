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

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import de.dal33t.powerfolder.util.BrowserLauncher;

import javax.swing.*;

/**
 * A Label which opens a given link by click it
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class LinkLabel extends AntialiasedLabel {

    private String url;

    public LinkLabel(Icon icon, String toolTips, final String url) {
        super(icon);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        setToolTipText(toolTips);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public LinkLabel(String aText, String aUrl) {
        super("<html><font color=\"#00000\"><a href=\"" + aUrl + "\">" + aText
            + "</a></font></html>");
        url = aUrl;
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setTextAndURL(String text, String url) {
        this.url = url;
        setText("<html><font color=\"#00000\"><a href=\"" + url + "\">" + text
            + "</a></font></html>");
    }
}