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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
            "pref, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 2));
        JScrollPane scrollPane = new JScrollPane(foldersList.getUIComponent());
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xy(1, 4));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the required components.
     */
    private void initComponents() {
        foldersList = new FoldersList(getController());
        folderTypeList = new JComboBox();
        folderTypeList.addItem(Translation.getTranslation("folders_tab.all_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_local_folders"));
        folderTypeList.addItem(Translation.getTranslation("folders_tab.only_online_folders"));
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton newFolderButton = new JButton(getUIController().getActionModel()
                .getNewFolderAction());

        FormLayout layout = new FormLayout("pref, pref:grow, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(newFolderButton, cc.xy(1, 1));
        builder.add(folderTypeList, cc.xy(3, 1));
        JPanel barPanel = builder.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }
}
