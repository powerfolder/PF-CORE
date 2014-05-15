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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * A Label which opens a given link by click it
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class LinkLabel extends PFComponent {

    private static final Logger log = Logger.getLogger(LinkLabel.class
        .getName());
    private String url;
    private JLabel uiComponent;
    private String text;
    private volatile boolean mouseOver;

    public LinkLabel(Controller controller, String text, String url) {
        super(controller);
        this.text = text;
        this.url = url;
        uiComponent = new JLabel();

        setText();

        uiComponent.addMouseListener(new MyMouseAdapter());

        CursorUtils.setHandCursor(uiComponent);
        // FIXME This is a hack because of "Fusch!"
        uiComponent.setBorder(Borders.createEmptyBorder("0, 1px, 0, 0"));
    }

    public void setText(String text) {
        this.text = text;
        setText();
    }

    public void setTextAndURL(String text, String url) {
        this.text = text;
        this.url = url;
        setText();
    }

    public void setForeground(Color c) {
        uiComponent.setForeground(c);
    }

    public void setIcon(Icon icon) {
        uiComponent.setIcon(icon);
    }

    public void convertToBigLabel() {
        uiComponent.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        UIUtil.setFontSize(uiComponent, UIUtil.MED_FONT_SIZE);
    }

    public JLabel getUIComponent() {
        return uiComponent;
    }

    private void setText() {
        if (StringUtils.isBlank(text)) {
            uiComponent.setText(null);
            return;
        }
        if (mouseOver
            || PreferencesEntry.UNDERLINE_LINKS
                .getValueBoolean(getController()))
        {
            Color color = ColorUtil.getTextForegroundColor();
            String rgb = ColorUtil.getRgbForColor(color);
            if (StringUtils.isBlank(url)) {
                // Display link, but action listener will ignore.
                uiComponent.setText("<html><font color=\"" + rgb +
                        "\"><a href=\".\">" + text +
                        "</a></font></html>");
            } else {
                uiComponent.setText("<html><font color=\"" + rgb +
                        "\"><a href=\"" + url + "\">" + text +
                        "</a></font></html>");
            }
        } else {
            uiComponent.setText(text);
        }
    }

    public void setToolTipText(String tips) {
        uiComponent.setToolTipText(tips);
    }

    public void setVisible(boolean visible) {
        uiComponent.setVisible(visible);
    }

    private class MyMouseAdapter extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (StringUtils.isBlank(url)) {
                return;
            }
            BrowserLauncher.openURL(getController(), url);
        }

        public void mouseEntered(MouseEvent e) {
            mouseOver = true;
            setText();
        }

        public void mouseExited(MouseEvent e) {
            mouseOver = false;
            setText();
        }
    }

}