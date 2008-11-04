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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ComputersTab extends PFUIComponent {

    private JPanel uiComponent;

    private JComboBox computerTypeList;

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
        computerTypeList = new JComboBox();
        computerTypeList.setToolTipText(Translation.getTranslation(
                "computers_tab.computer_type_list.text"));
        computerTypeList.addItem(Translation.getTranslation("computers_tab.all_computers"));
        computerTypeList.addItem(Translation.getTranslation("computers_tab.only_online_friends"));
        computerTypeList.addItem(Translation.getTranslation("computers_tab.all_online"));
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton searchComputerButton = new JButton(getUIController()
                .getActionModel().getSearchComputerAction());

        FormLayout layout = new FormLayout("pref, pref:grow, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(searchComputerButton, cc.xy(1, 1));
        builder.add(computerTypeList, cc.xy(3, 1));
        JPanel barPanel = builder.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }
}
