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
 * $Id: JButtonMini.java 5009 2008-08-11 01:25:22Z tot $
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.event.WeakActionListener;
import de.dal33t.powerfolder.event.WeakPropertyChangeListener;
import de.dal33t.powerfolder.ui.util.CursorUtils;

/**
 * Class showing image button with no border, except when hover or pressed. Uses
 * Synthetica features to do border.
 */
public class JButtonMini extends JButton {

    private MyActionListener actionListener;
    private MyPropertyChangeListener propChangeListener;

    /**
     * Mini button that is bound to a an action
     *
     * @param action
     */
    public JButtonMini(final Action action) {
        this((Icon) action.getValue(Action.SMALL_ICON), (String) action
            .getValue(Action.SHORT_DESCRIPTION));
        CursorUtils.setHandCursor(this);

        actionListener = new MyActionListener(action);
        addActionListener(new WeakActionListener(actionListener, this));
        propChangeListener = new MyPropertyChangeListener(action);
        action.addPropertyChangeListener(new WeakPropertyChangeListener(
            propChangeListener, action));
        setEnabled(action.isEnabled());
    }

    public JButtonMini(Icon icon, String toolTipText) {
        if (icon == null) {
            setText("???");
        } else {
            setIcon(icon);
        }

        setOpaque(true);
        setBorder(null);
        setBorder(Borders.createEmptyBorder("0dlu, 0dlu, 0dlu, 0dlu"));
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
        if (toolTipText != null && toolTipText.trim().length() > 0) {
            setToolTipText(toolTipText);
        }
    }

    public void configureFromAction(Action action) {
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

    private final class MyPropertyChangeListener implements
        PropertyChangeListener
    {
        private final Action action;

        private MyPropertyChangeListener(Action action) {
            this.action = action;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            setEnabled(action.isEnabled());
        }
    }

    private final class MyActionListener implements ActionListener {
        private final Action action;

        private MyActionListener(Action action) {
            this.action = action;
        }

        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
    }
}
