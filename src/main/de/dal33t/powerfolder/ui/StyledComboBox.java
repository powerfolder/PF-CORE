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
package de.dal33t.powerfolder.ui;

import java.awt.Rectangle;

import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import de.javasoft.plaf.synthetica.SyntheticaComboBoxUI;
import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

public class StyledComboBox<E> extends JComboBox<E> {
    public StyledComboBox(E[] initial) {
        super(initial);
        // The custom UI extends SyntheticaComboBoxUI purely to constrain the
        // popup width to the combo's preferred size. Since the PFI-93 UI
        // modernization the client runs FlatLaf by default (Synthetica is only a
        // fallback), and SyntheticaComboBoxUI.installUI() NPEs when Synthetica is
        // not the active Look-and-Feel (its style factory is null) - which stopped
        // the login wizard from opening. So only install it under Synthetica;
        // otherwise keep the active L&F's own combo box UI.
        if (UIManager.getLookAndFeel() instanceof SyntheticaLookAndFeel) {
            setUI(new StyledComboBoxUI());
        }
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
