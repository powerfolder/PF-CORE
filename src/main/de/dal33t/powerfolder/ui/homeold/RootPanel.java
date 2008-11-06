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
package de.dal33t.powerfolder.ui.homeold;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.util.PFUIPanel;

import javax.swing.JComponent;

/**
 * Holds the root items in a table.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class RootPanel extends PFUIPanel {
    private JComponent panel;

    private RootQuickInfoPanel quickInfo;

    // private JPanel toolbar;

    public RootPanel(Controller controller) {
        super(controller);
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        quickInfo = new RootQuickInfoPanel(getController());
    }
}
