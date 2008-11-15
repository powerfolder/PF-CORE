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
* $Id: InformationFilesCard.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationSettingsTab;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Image;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class FolderInformationCard extends InformationCard {

    private static final int TAB_FILES = 0;
    private static final int TAB_MEMBERS = 1;
    private static final int TAB_SETTIGNS = 2;

    private FolderInfo folderInfo;
    private JTabbedPane tabbedPane;
    private FolderInformationSettingsTab settingsTab;

    /**
     * Constructor
     *
     * @param controller
     */
    public FolderInformationCard(Controller controller) {
        super(controller);
        settingsTab = new FolderInformationSettingsTab(getController());
    }

    /**
     * Sets the folder in the tabs.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        this.folderInfo = folderInfo;
        settingsTab.setFolderInfo(folderInfo);
    }

    /**
     * Gets the image for the card.
     *
     * @return
     */
    public Image getCardImage() {
        return Icons.FOLDER_IMAGE;
    }

    /**
     * Gets the title for the card.
     *
     * @return
     */
    public String getCardTitle() {
        return folderInfo.name;
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (tabbedPane == null) {
            initialize();
            buildUIComponent();
        }
        return tabbedPane;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        tabbedPane = new JTabbedPane();
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
        tabbedPane.addTab(Translation.getTranslation(
                "folder_information_card.files.title"), new JPanel());
        tabbedPane.setIconAt(TAB_FILES, Icons.FILES);
        tabbedPane.setToolTipTextAt(TAB_FILES, Translation.getTranslation(
                "folder_information_card.files.tips"));

        tabbedPane.addTab(Translation.getTranslation(
                "folder_information_card.members.title"), new JPanel());
        tabbedPane.setIconAt(TAB_MEMBERS, Icons.NODE_FRIEND_CONNECTED);
        tabbedPane.setToolTipTextAt(TAB_MEMBERS, Translation.getTranslation(
                "folder_information_card.members.tips"));

        tabbedPane.addTab(Translation.getTranslation(
                "folder_information_card.settings.title"),
                settingsTab.getUIComponent());
        tabbedPane.setIconAt(TAB_SETTIGNS, Icons.SETTINGS);
        tabbedPane.setToolTipTextAt(TAB_SETTIGNS, Translation.getTranslation(
                "folder_information_card.settings.tips"));
    }

    /**
     * Display the files tab.
     */
    public void showFiles() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_FILES);
    }

    /**
     * Display the members tab.
     */
    public void showMembers() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_MEMBERS);
    }

    /**
     * Display the settings tab.
     */
    public void showSettings() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_SETTIGNS);
    }
}