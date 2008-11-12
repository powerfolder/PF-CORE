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
* $Id: FolderInformationSettingsTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import de.dal33t.powerfolder.light.FolderInfo;

import javax.swing.*;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * UI component for the information settings tab
 */
public class FolderInformationSettingsTab extends PFUIComponent
        implements FolderInformationTab {

    private JPanel uiComponent;
    private Folder folder;
    private SyncProfileSelectorPanel transferModeSelectorPanel;

    /**
     * Constructor
     *
     * @param controller
     */
    public FolderInformationSettingsTab(Controller controller) {
        super(controller);
        transferModeSelectorPanel = new SyncProfileSelectorPanel(getController());
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        transferModeSelectorPanel.setUpdateableFolder(folder);
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * initializes the components
     */
    private void initialize() {
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {

        FormLayout layout = new FormLayout(
            "3dlu, right:pref, 3dlu, pref, 3dlu, pref:grow",
                "3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.transfer_mode")),
                cc.xy(2, 2));

        builder.add(transferModeSelectorPanel.getUIComponent(), cc.xy(4, 2));

        uiComponent = builder.getPanel();
    }
}
