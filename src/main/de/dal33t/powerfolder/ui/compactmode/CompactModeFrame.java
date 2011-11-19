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
 * $Id: CompactModeFrame.java 16820 2011-11-19 00:30:45Z harry $
 */
package de.dal33t.powerfolder.ui.compactmode;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;

import javax.swing.*;

/**
 * A compact alternative to the MainFrame. Only one of this or the MainFrame
 * should be showing at any one time.
 */
public class CompactModeFrame extends PFUIComponent {

    private JFrame uiComponent;

    public CompactModeFrame(Controller controller) {
        super(controller);
        buildUI();
    }

    private void buildUI() {
        uiComponent = new JFrame("TBA");
        uiComponent.setLocation(200, 200);
        uiComponent.setSize(200, 200);
    }

    public JFrame getUIComponent() {
        return uiComponent;
    }
}
