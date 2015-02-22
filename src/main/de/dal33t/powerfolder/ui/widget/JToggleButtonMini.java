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
 * $Id: JToggleButtonMini.java 5009 2008-08-11 01:25:22Z tot $
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.CursorUtils;

/**
 * Class showing image button with no border, except when hover or pressed. Uses
 * Synthetica features to do border.
 */
public class JToggleButtonMini extends JToggleButton {

    /**
     * Mini button that is configured from action which does act on it
     *
     * @param action
     */
    public JToggleButtonMini(Action action) {
        this(action, true);
    }

    /**
     * Mini button that is configured from action and also can act on it
     *
     * @param action
     * @param act
     */
    public JToggleButtonMini(final Action action, boolean act) {
        this((Icon) action.getValue(Action.SMALL_ICON), (String) action
            .getValue(Action.SHORT_DESCRIPTION));
        CursorUtils.setHandCursor(this);
        if (act) {
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    action.actionPerformed(e);
                }
            });
        }
    }

    public JToggleButtonMini(Icon icon, String toolTipText) {
        if (icon == null) {
            setText("???");
        } else {
            setIcon(icon);
        }

        setOpaque(false);
        setBorder(null);
        setBorder(Borders.DLU2_BORDER);
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
        if (toolTipText != null && toolTipText.trim().length() > 0) {
            setToolTipText(toolTipText);
        }
    }

    public void configureFromAction(BaseAction action) {
        Object value = action.getValue(Action.SMALL_ICON);
        if (value != null && value instanceof Icon) {
            Icon icon = (Icon) value;
            setIcon(icon);
        }
        value = action.getValue(Action.SHORT_DESCRIPTION);
        if (value != null && value instanceof String) {
            String text = (String) value;
            setToolTipText(text);
        }
    }
}