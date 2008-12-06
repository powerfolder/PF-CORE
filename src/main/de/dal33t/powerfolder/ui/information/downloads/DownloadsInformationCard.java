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
package de.dal33t.powerfolder.ui.information.downloads;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Image;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class DownloadsInformationCard extends InformationCard {

    private JPanel uiComponent;

    /**
     * Constructor
     *
     * @param controller
     */
    public DownloadsInformationCard(Controller controller) {
        super(controller);
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
        return Translation.getTranslation("downloads_information_card.title");
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        uiComponent = new JPanel();
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
    }
}