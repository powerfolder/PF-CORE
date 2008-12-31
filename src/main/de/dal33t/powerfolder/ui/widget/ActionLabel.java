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

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.ColorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A Label which executes the action when clicked.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ActionLabel extends JLabel {

    private volatile boolean enabled = true;
    private String text;

    public ActionLabel(final Action action) {
        text = (String) action.getValue(Action.NAME);
        displayText();
        String toolTips = (String) action.getValue(Action.SHORT_DESCRIPTION);
        if (toolTips != null && toolTips.length() > 0) {
            setToolTipText(toolTips);
        }
        Reject.ifNull(action, "Action listener is null");
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (enabled) {
                    action.actionPerformed(new ActionEvent(e.getSource(), 0,
                        "clicked"));
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void configureFromAction(final Action action) {
        text = (String) action.getValue(Action.NAME);
        displayText();
        String toolTips = (String) action.getValue(Action.SHORT_DESCRIPTION);
        if (toolTips != null && toolTips.length() > 0) {
            setToolTipText(toolTips);
        }
        Reject.ifNull(action, "Action listener is null");
        for (MouseListener mouseListener : getMouseListeners()) {
            removeMouseListener(mouseListener);
        }
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (enabled) {
                    action.actionPerformed(new ActionEvent(e.getSource(), 0,
                        "clicked"));
                }
            }
        });
    }

    public void setText(String text) {
        this.text = text;
        displayText();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        displayText();
    }

    public void displayText() {
        if (enabled) {
            // @todo not picking up Synthetica colors...?
            setForeground(SystemColor.textText);
            updateUI();
            Object object = getClientProperty("Synthetica.menu.toplevel.textColor");
            Color color;
            if (object != null && object instanceof Color) {
                color = (Color) object;
            } else { 
                color = getForeground();
            }
            String rgb = ColorUtil.getRgbForColor(color);
            setForeground(SystemColor.textText);
            super.setText("<html><font color=\"" + rgb + "\"><a href=\"#\">" + text
                + "</a></font></html>");
        } else {
            setForeground(SystemColor.textInactiveText);
            super.setText(text);
        }
    }


}