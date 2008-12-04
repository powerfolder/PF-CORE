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
* $Id: ChatPanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.chat;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

import javax.swing.JPanel;

public class ChatPanel extends PFComponent {

    private JPanel uiComponent;

    public ChatPanel(Controller controller) {
        super(controller);
    }

    public JPanel getUiComponent() {
        if (uiComponent == null) {
            initialize();
            buildUiComponent();
        }
        return uiComponent;
    }

    private void buildUiComponent() {
        uiComponent = new JPanel();
    }

    private void initialize() {
    }


}
