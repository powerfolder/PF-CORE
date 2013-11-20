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
package de.dal33t.powerfolder.ui;

import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import de.javasoft.plaf.synthetica.SyntheticaComboBoxUI;

public class StyledComboBox<E> extends JComboBox<E> {
    public StyledComboBox(E[] initial) {
        super(initial);
        setUI(new StyledComboBoxUI());
    }

    private class StyledComboBoxUI extends SyntheticaComboBoxUI {
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected Rectangle computePopupBounds(int px, int py, int pw,
                    int ph)
                {
                    return super.computePopupBounds(px, py, (int) comboBox
                        .getPreferredSize().getWidth(), ph);
                }
            };

            popup.getAccessibleContext().setAccessibleParent(comboBox);
            return popup;
        }
    }
}