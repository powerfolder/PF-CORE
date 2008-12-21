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
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Class to display the forders tab.
 */
public class FoldersTab extends PFUIComponent {

    private JPanel uiComponent;
    private FoldersList foldersList;
    private JComboBox folderTypeList;

    /**
     * Constructor
     *
     * @param controller
     */
    public FoldersTab(Controller controller) {
        super(controller);
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
        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 2));
        builder.addSeparator(null, cc.xy(1, 4));
        JScrollPane scrollPane = new JScrollPane(foldersList.getUIComponent());
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xy(1, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the required components.
     */
    private void initComponents() {

        foldersList = new FoldersList(getController());

        folderTypeList = new JComboBox();
        folderTypeList.setToolTipText(Translation.getTranslation(
                "folders_tab.folder_type_list.text"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.all_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_local_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_online_folders"));
        Integer initialSelection = PreferencesEntry.FOLDER_TYPE_SELECTION
                .getValueInt(getController());
        folderTypeList.setSelectedIndex(initialSelection);
        folderTypeList.addActionListener(new MyActionListener());
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

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(folderTypeList)) {
                PreferencesEntry.FOLDER_TYPE_SELECTION.setValue(getController(),
                        folderTypeList.getSelectedIndex());
            }
        }
    }
}
