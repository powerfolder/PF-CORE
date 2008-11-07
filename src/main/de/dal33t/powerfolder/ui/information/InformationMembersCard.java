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
* $Id: InformationMembersCard.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JPanel;
import java.awt.Image;

public class InformationMembersCard extends InformationCard {

    private Folder folder;
    private JPanel uiComponent;

    public InformationMembersCard(Controller controller, FolderInfo folderInfo) {
        super(controller);
        folder = controller.getFolderRepository().getFolder(folderInfo);
    }

    public Image getCardImage() {
        return Icons.MEMBERS_IMAGE;
    }

    public String getCardTitle() {
        return Translation.getTranslation("information.members.title",
                folder.getName());
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    private void buildUIComponent() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        uiComponent = builder.getPanel();
    }

    private void initialize() {
    }
}