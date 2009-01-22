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
 * $Id: FoldersTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class to display the forders tab.
 */
public class FoldersTab extends PFUIComponent {

    public static final int FOLDER_TYPE_ALL = 0;
    public static final int FOLDER_TYPE_LOCAL = 1;
    public static final int FOLDER_TYPE_ONLINE = 2;

    private JPanel uiComponent;
    private FoldersList foldersList;
    private JComboBox folderTypeList;
    private ValueModel folderSelectionTypeVM;
    private JScrollPane scrollPane;
    private JLabel emptyLabel;

    /**
     * Constructor
     *
     * @param controller
     */
    public FoldersTab(Controller controller) {
        super(controller);
        emptyLabel = new JLabel(
                Translation.getTranslation("folders_tab.no_folders_available"),
                SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        Integer initialSelection = PreferencesEntry.FOLDER_TYPE_SELECTION
                .getValueInt(getController());

        folderSelectionTypeVM = new ValueHolder();
        folderSelectionTypeVM.setValue(initialSelection);

        foldersList = new FoldersList(getController(), this,
                folderSelectionTypeVM);

        folderTypeList = new JComboBox();
        folderTypeList.setToolTipText(Translation.getTranslation(
                "folders_tab.folder_type_list.text"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.all_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_local_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_online_folders"));
        folderTypeList.setSelectedIndex(initialSelection);
        folderTypeList.addActionListener(new MyActionListener());
    }

    /**
     * Returns the ui component.
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
     * Builds the ui component.
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
        scrollPane = new JScrollPane(foldersList.getUIComponent());
        foldersList.setScroller(scrollPane);
        UIUtil.removeBorder(scrollPane);

        // emptyLabel and scrollPane occupy the same slot.
        builder.add(emptyLabel, cc.xy(1, 6));
        builder.add(scrollPane, cc.xy(1, 6));

        uiComponent = builder.getPanel();

        updateEmptyLabel();

    }

    public void updateEmptyLabel() {
        if (foldersList != null) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(foldersList.isEmpty());
            }
            if (scrollPane != null) {
                scrollPane.setVisible(!foldersList.isEmpty());
            }
        }
    }

    public ValueModel getFolderSelectionTypeVM() {
        return folderSelectionTypeVM;
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton newFolderButton = new JButton(getApplicationModel().getActionModel()
                .getNewFolderAction());

        FormLayout layout = new FormLayout("3dlu, pref, pref:grow, pref, 3dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(newFolderButton, cc.xy(2, 1));
        builder.add(folderTypeList, cc.xy(4, 1));

        return builder.getPanel();
    }

    /**
     * Populates the folders in the list.
     */
    public void populate() {
        foldersList.populate();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(folderTypeList)) {
                PreferencesEntry.FOLDER_TYPE_SELECTION.setValue(getController(),
                        folderTypeList.getSelectedIndex());
                folderSelectionTypeVM.setValue(folderTypeList.getSelectedIndex());
            }
        }
    }
}
