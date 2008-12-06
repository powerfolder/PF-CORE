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
* $Id: DownloadsDetailsPanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.downloads;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class DownloadsDetailsPanel extends PFUIComponent {

    private JPanel uiComponent;

    public DownloadsDetailsPanel(Controller controller) {
        super(controller);
    }

    public JPanel getUiComponent() {
        if (uiComponent == null) {
            buildUiComponent();
        }
        return uiComponent;
    }

    private void buildUiComponent() {

        FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, 3dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xy(1, 1));
        builder.add(new JLabel("(details panel here)"), cc.xy(1, 3));
        uiComponent = builder.getPanel();
        uiComponent.setVisible(false);
    }
}
