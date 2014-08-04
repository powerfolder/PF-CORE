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
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class to display a list of computers.
 */
public class ComputersTab extends PFUIComponent {

    private JPanel uiComponent;
    private ComputersList computersList;
    private JCheckBox showOfflineCB;
    private JScrollPane scrollPane;
    private JLabel emptyLabel;

    /**
     * Constructor
     *
     * @param controller
     */
    public ComputersTab(Controller controller) {
        super(controller);
        emptyLabel = new JLabel(
            Translation.getTranslation("exp.computers_tab.no_computers_available"),
            SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        computersList = new ComputersList(getController(), this);

        showOfflineCB = new JCheckBox(
            Translation.getTranslation("exp.computers_tab.show_offline"));
        showOfflineCB.setToolTipText(Translation
            .getTranslation("exp.computers_tab.show_offline.tip"));
        showOfflineCB.addActionListener(new MyActionListener());
        showOfflineCB.setSelected(PreferencesEntry.SHOW_OFFLINE
            .getValueBoolean(getController()));
        configureNodeManagerModel();
    }

    /**
     * Gets the UI component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Builds the UI component
     */
    private void buildUI() {

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 2));
        builder.addSeparator(null, cc.xy(1, 4));
        scrollPane = new JScrollPane(computersList.getUIComponent());
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        UIUtil.removeBorder(scrollPane);

        // emptyLabel and scrollPane occupy the same slot.
        builder.add(emptyLabel, cc.xy(1, 6));
        builder.add(scrollPane, cc.xy(1, 6));

        uiComponent = builder.getPanel();

        updateEmptyLabel();
    }

    public void updateEmptyLabel() {
        if (emptyLabel != null) {
            emptyLabel.setVisible(computersList.isEmpty());
        }
        if (scrollPane != null) {
            scrollPane.setVisible(!computersList.isEmpty());
        }
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        ActionLabel searchComputerLink = new ActionLabel(getController(),
            getApplicationModel().getActionModel().getFindComputersAction());
        searchComputerLink.convertToBigLabel();
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu:grow, pref, 3dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(searchComputerLink.getUIComponent(), cc.xy(2, 1));
        builder.add(showOfflineCB, cc.xy(4, 1));

        return builder.getPanel();
    }

    /**
     * Configure the node manager model with hide and include settings.
     */
    private void configureNodeManagerModel() {
        NodeManagerModel nodeManagerModel = getUIController()
            .getApplicationModel().getNodeManagerModel();
        nodeManagerModel.getShowOfflineModel().setValue(
            showOfflineCB.isSelected());
        PreferencesEntry.SHOW_OFFLINE.setValue(getController(),
            showOfflineCB.isSelected());
    }

    /**
     * Populates the list of computers.
     */
    public void populate() {
        computersList.populate();
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    /**
     * Action listener for type list.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(showOfflineCB)) {
                configureNodeManagerModel();
            }
        }
    }
}
