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
package de.dal33t.powerfolder.ui.information;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Image;

public class FolderInformationCard extends InformationCard {

    private static final int TAB_FILES = 0;
    private static final int TAB_MEMBERS = 1;
    private static final int TAB_SETTIGNS = 2;

    private Folder folder;
    private JTabbedPane tabbedPane;

    public FolderInformationCard(Controller controller) {
        super(controller);
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
    }

    public Image getCardImage() {
        return Icons.FOLDER_IMAGE;
    }

    public String getCardTitle() {
        return folder.getName();
    }

    public JComponent getUIComponent() {
        if (tabbedPane == null) {
            initialize();
            buildUIComponent();
        }
        return tabbedPane;
    }

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
                "folder_information_card.settings.title"), new JPanel());
        tabbedPane.setIconAt(TAB_SETTIGNS, Icons.SETTINGS);
        tabbedPane.setToolTipTextAt(TAB_SETTIGNS, Translation.getTranslation(
                "folder_information_card.settings.tips"));
    }

    private void initialize() {
        tabbedPane = new JTabbedPane();
    }

    public void showFiles() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_FILES);
    }

    public void showMembers() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_MEMBERS);
    }

    public void showSettings() {
        ((JTabbedPane) getUIComponent()).setSelectedIndex(TAB_SETTIGNS);
    }
}