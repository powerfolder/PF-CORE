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
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class to display a list of computers.
 */
public class ComputersTab extends PFUIComponent {

    private JPanel uiComponent;
    private ComputersList computersList;
    private JComboBox computerTypeList;
    private JScrollPane scrollPane;
    private JLabel emptyLabel;

    /**
     * Constructor
     *
     * @param controller
     */
    public ComputersTab(Controller controller) {
        super(controller);
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
        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 2));
        builder.addSeparator(null, cc.xy(1, 4));
        scrollPane = new JScrollPane(computersList.getUIComponent());
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
     * Initializes components
     */
    private void initComponents() {

        emptyLabel = new JLabel(
                Translation.getTranslation("computers_tab.no_computers_available"),
                SwingConstants.CENTER);
        emptyLabel.setEnabled(false);
        
        computersList = new ComputersList(getController(), this);

        computerTypeList = new JComboBox();
        computerTypeList.setToolTipText(Translation.getTranslation(
                "computers_tab.computer_type_list.text"));
        computerTypeList.addItem(Translation.getTranslation(
                "computers_tab.all_friends_online_lan"));
        computerTypeList.addItem(Translation.getTranslation(
                "computers_tab.online_friends"));
        computerTypeList.addItem(Translation.getTranslation(
                "computers_tab.online_friends_online_lan"));
        computerTypeList.addActionListener(new MyActionListener());
        Integer initialSelection = PreferencesEntry.COMPUTER_TYPE_SELECTION
                .getValueInt(getController());
        computerTypeList.setSelectedIndex(initialSelection);
        configureNodeManagerModel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton searchComputerButton = new JButton(getApplicationModel()
                .getActionModel().getFindComputersAction());

        FormLayout layout = new FormLayout("3dlu, pref, pref:grow, pref, 3dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(searchComputerButton, cc.xy(2, 1));
        builder.add(computerTypeList, cc.xy(4, 1));

        return builder.getPanel();
    }

    /**
     * Configure the node manager model with hide and include settings.
     */
    private void configureNodeManagerModel() {
        NodeManagerModel nodeManagerModel = getUIController()
                .getApplicationModel().getNodeManagerModel();
        int index = computerTypeList.getSelectedIndex();
        PreferencesEntry.COMPUTER_TYPE_SELECTION.setValue(
                getController(), index);
        if (index == 0) { // All friends / online lan
            nodeManagerModel.getHideOfflineFriendsModel().setValue(false);
            nodeManagerModel.getIncludeOnlineLanModel().setValue(true);
        } else if (index == 1) { // Online friends
            nodeManagerModel.getHideOfflineFriendsModel().setValue(true);
            nodeManagerModel.getIncludeOnlineLanModel().setValue(false);
        } else if (index == 2) { // Online friends / online lan
            nodeManagerModel.getHideOfflineFriendsModel().setValue(true);
            nodeManagerModel.getIncludeOnlineLanModel().setValue(true);
        } else {
            logSevere("Bad computerTypeList index " + index);
        }
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Action listener for type list.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(computerTypeList)) {
                configureNodeManagerModel();
            }
        }
    }
}
