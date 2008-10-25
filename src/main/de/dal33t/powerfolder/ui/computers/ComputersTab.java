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
 * $Id: ComputersTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.computers;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.factories.Borders;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;

public class ComputersTab extends PFUIComponent {

    private JPanel uiComponent;

    public ComputersTab(Controller controller) {
        super(controller);
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    private void buildUI() {
        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, 3dlu, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 2));
        builder.add(new JLabel("test"), cc.xy(1, 4));
        uiComponent = builder.getPanel();
    }

    private void initComponents() {
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton searchComputerButton = new JButton(getUIController().getActionModel()
                .getSearchComputerAction());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(searchComputerButton);

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }
}
