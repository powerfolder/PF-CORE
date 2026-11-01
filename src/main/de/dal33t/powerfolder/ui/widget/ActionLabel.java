/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

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
 * @author Christian Sprajc
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

        // Re-apply the theme-aware foreground when the L&F is switched live
        // (FlatLaf light <-> dark). updateComponentTreeUI fires "UI" on setUI.
        uiComponent.addPropertyChangeListener("UI",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    displayText();
                }
            });

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
        // PFC-3643: "never underline" controls only the underline style, not
        // interactivity - an ActionLabel is still clickable, so always keep the
        // hand cursor. (Previously neverUnderline forced the default arrow, which
        // made clickable labels like the account name and the sync-status text
        // look non-clickable and inconsistent with the other hand-cursor targets.)
        CursorUtils.setHandCursor(uiComponent);
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
            // PFC-3643: underline through the font, not as HTML link - HTML has another line height and baseline,
            // so the text jumped on hover.
            uiComponent.setForeground(ColorUtil.getTextForegroundColor());
            setUnderlined(!neverUnderline && (mouseOver || underline) && StringUtils.isNotBlank(text));
            putText(StringUtils.isNotBlank(text) || !mouseOver ? text : " ");
        } else {
            uiComponent.setForeground(ColorUtil.getTextInactiveColor());
            setUnderlined(false);
            putText(text);
        }
    }

    private void setUnderlined(boolean underlined) {
        Font font = uiComponent.getFont();
        if (font == null) {
            return;
        }
        Object current = font.getAttributes().get(TextAttribute.UNDERLINE);
        boolean isUnderlined = TextAttribute.UNDERLINE_ON.equals(current);
        if (isUnderlined != underlined) {
            Map<TextAttribute, Object> attributes = new HashMap<>();
            attributes.put(TextAttribute.UNDERLINE, underlined ? TextAttribute.UNDERLINE_ON : null);
            uiComponent.setFont(font.deriveFont(attributes));
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
