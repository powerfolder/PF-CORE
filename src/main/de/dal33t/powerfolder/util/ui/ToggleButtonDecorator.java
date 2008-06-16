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
package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 * Updates the raised / lowered state of all JToggleButtons in a Toolbar.
 * Buttons must be in a JToolBar!<BR>
 * example:<PRE>
 * ToggleButtonDecorator decorator = new ToggleButtonDecorator(); 
 * // loop all buttons in this toolbar 
 * for (int j = 0; j < toolbar.getComponentCount(); j++) {
 *     Component button = toolbar(j); 
 *     // make sure they are what we think 
 *     if (button instanceof JToggleButton) { 
 *         JToggleButton jToggleButton = (JToggleButton) button; 
 *         jToggleButton.addActionListener(decorator);
 *         jToggleButton.addMouseListener(decorator);
 *         decorator.updateButton(jToggleButton); 
 *     } 
 * }</PRE>
 */
public class ToggleButtonDecorator extends MouseAdapter implements
    ActionListener
{

    public void actionPerformed(ActionEvent e) {
        updateButtons((JToggleButton) e.getSource());
    }

    public void mouseEntered(MouseEvent e) {
        raiseButton((JToggleButton) e.getSource());
    }

    public void mouseExited(MouseEvent e) {
        updateButtons((JToggleButton) e.getSource());
    }

    private void raiseButton(JToggleButton button) {
        if (button.isEnabled()) {
            button.setBorder(BorderFactory.createRaisedBevelBorder());
        }
    }

    public void updateButton(JToggleButton button) {
        if (button.isSelected()) {
            button.setBorder(BorderFactory.createLoweredBevelBorder());
        } else {
            button.setBorder(null);
        }
    }

    private void updateButtons(JToggleButton button) {
        JToolBar toolBar = (JToolBar) button.getParent();
        for (int i = 0; i < toolBar.getComponentCount(); i++) {
            if (toolBar.getComponent(i) instanceof JToggleButton) {
                JToggleButton b = (JToggleButton) toolBar.getComponent(i);
                updateButton(b);
            }
        }
    }
}
