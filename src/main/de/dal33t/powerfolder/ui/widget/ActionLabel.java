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
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * A Label which executes the action when clicked.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ActionLabel extends PFComponent {

    private JLabel uiComponent;
    private volatile boolean enabled = true;
    private String text;
    private Action action;
    private volatile boolean mouseOver;
    private boolean neverUnderline;
    private boolean underline;

    public ActionLabel(Controller controller, final Action action) {
        super(controller);
        Reject.ifNull(action, "Action");
        this.action = action;
        underline = PreferencesEntry.UNDERLINE_LINKS
            .getValueBoolean(getController());
        neverUnderline = false;
        uiComponent = new JLabel();
        text = (String) action.getValue(Action.NAME);
        displayText();
        String toolTips = (String) action.getValue(Action.SHORT_DESCRIPTION);
        if (toolTips != null && toolTips.length() > 0) {
            uiComponent.setToolTipText(toolTips);
        }
        Reject.ifNull(action, "Action listener is null");
        uiComponent.addMouseListener(new MyMouseAdapter());
        CursorUtils.setHandCursor(uiComponent);

        action.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                setEnabled(action.isEnabled());

                if (Action.NAME.equals(evt.getPropertyName())) {
                    text = (String) action.getValue(Action.NAME);
                    displayText();
                } else if (Action.SHORT_DESCRIPTION.equals(evt
                    .getPropertyName()))
                {
                    String toolTips = (String) action
                        .getValue(Action.SHORT_DESCRIPTION);
                    if (toolTips != null && toolTips.length() > 0) {
                        uiComponent.setToolTipText(toolTips);
                    } else {
                        uiComponent.setToolTipText("");
                    }
                }
            }
        });
        setEnabled(action.isEnabled());
    }

    public JComponent getUIComponent() {
        return uiComponent;
    }

    /**
     * IMPORTANT - make component text changes here, not in the uiComponent.
     * Otherwise mouse-over activity will over-write the text.
     * 
     * @param text
     */
    public void setText(String text) {
        this.text = text;
        displayText();
    }

    public void setIcon(Icon icon) {
        uiComponent.setIcon(icon);
    }

    public void setToolTipText(String text) {
        uiComponent.setToolTipText(text);
    }

    public void setForeground(Color c) {
        uiComponent.setForeground(c);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        displayText();
    }

    public void setNeverUnderline(boolean neverUnderline) {
        this.neverUnderline = neverUnderline;
        displayText();
    }
    
    public void setVisible(boolean visible) {
        uiComponent.setVisible(visible);
    }

    public void setFontSize(int fontSize) {
        SimpleComponentFactory.setFont(uiComponent, fontSize, uiComponent
            .getFont().getStyle());
    }

    public void setFontStyle(int style) {
        SimpleComponentFactory.setFont(uiComponent, uiComponent.getFont()
            .getSize(), style);
    }

    public void convertToBigLabel() {
        uiComponent.setIcon(Icons.getIconById(Icons.ARROW_RIGHT));
        setFontSize(UIUtil.MED_FONT_SIZE);
    }

    public void displayText() {
        if (enabled) {
            if (!neverUnderline && (mouseOver || underline)) {
                if (StringUtils.isNotBlank(text)) {
                    Color color = ColorUtil.getTextForegroundColor();
                    String rgb = ColorUtil.getRgbForColor(color);
                    putText("<html><font color=\"" + rgb + "\"><a href=\"#\">"
                        + text + "</a></font></html>");
                } else {
                    putText(" ");
                }

            } else {
                uiComponent.setForeground(SystemColor.textText);
                putText(text);
            }
        } else {
            uiComponent.setForeground(SystemColor.textInactiveText);
            putText(text);
        }
    }

    private void putText(String text) {
        String oldTest = uiComponent.getText();
        if (oldTest == null) {
            if (text != null) {
                uiComponent.setText(text);
            }
            return;
        }
        if (!oldTest.equals(text)) {
            uiComponent.setText(text);
        }
    }

    private class MyMouseAdapter extends MouseAdapter {

        public void mouseEntered(MouseEvent e) {
            mouseOver = true;
            displayText();
        }

        public void mouseExited(MouseEvent e) {
            mouseOver = false;
            displayText();
        }

        public void mouseClicked(MouseEvent e) {
            if (enabled) {
                action.actionPerformed(new ActionEvent(e.getSource(), 0,
                    "clicked"));
            }
        }
    }
}
