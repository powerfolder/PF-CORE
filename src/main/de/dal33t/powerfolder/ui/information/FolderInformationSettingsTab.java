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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_PREFIX;
import static de.dal33t.powerfolder.disk.FolderSettings.FOLDER_SETTINGS_DONT_RECYCLE;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Properties;

/**
 * UI component for the information settings tab
 */
public class FolderInformationSettingsTab extends PFUIComponent
        implements FolderInformationTab {

    private JPanel uiComponent;
    private Folder folder;
    private SyncProfileSelectorPanel transferModeSelectorPanel;
    private JCheckBox useRecycleBinBox;

    /**
     * Constructor
     *
     * @param controller
     */
    public FolderInformationSettingsTab(Controller controller) {
        super(controller);
        transferModeSelectorPanel = new SyncProfileSelectorPanel(getController());
        useRecycleBinBox = new JCheckBox(Translation.getTranslation(
                "folder_information_settings_tab.use_recycle_bin"));
        useRecycleBinBox.addActionListener(new MyActionListener());
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        transferModeSelectorPanel.setUpdateableFolder(folder);
        useRecycleBinBox.setSelected(folder.isUseRecycleBin());
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
                "3dlu, pref, 3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation.getTranslation(
                "folder_information_settings_tab.transfer_mode")),
                cc.xy(2, 2));
        builder.add(transferModeSelectorPanel.getUIComponent(), cc.xy(4, 2));

        builder.add(useRecycleBinBox, cc.xy(4, 4));

        uiComponent = builder.getPanel();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            folder.setUseRecycleBin(useRecycleBinBox.isSelected());
            Properties config = getController().getConfig();
            // Inverse logic for backward compatability.
            config.setProperty(FOLDER_SETTINGS_PREFIX + folder.getName()
                + FOLDER_SETTINGS_DONT_RECYCLE, String
                .valueOf(!useRecycleBinBox.isSelected()));
            getController().saveConfig();            
        }
    }
}
