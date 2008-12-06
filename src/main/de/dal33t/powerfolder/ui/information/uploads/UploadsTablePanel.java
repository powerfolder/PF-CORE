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
* $Id: UploadsTablePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.uploads;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;

public class UploadsTablePanel extends PFUIComponent {

    private JPanel uiComponent;

    public UploadsTablePanel(Controller controller) {
        super(controller);
    }

    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        uiComponent = new JPanel();
        uiComponent.add(new JLabel("Uploads table panel"));
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
    }

}
