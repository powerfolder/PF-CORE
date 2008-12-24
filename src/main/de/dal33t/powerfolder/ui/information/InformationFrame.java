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
* $Id: InformationFrame.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.downloads.DownloadsInformationCard;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationCard;
import de.dal33t.powerfolder.ui.information.uploads.UploadsInformationCard;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;

import javax.swing.*;
import javax.swing.plaf.RootPaneUI;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * The information window.
 */
public class InformationFrame extends PFUIComponent {

    private JFrame uiComponent;

    private FolderInformationCard folderInformationCard;
    private DownloadsInformationCard downloadsInformationCard;
    private UploadsInformationCard uploadsInformationCard;

    /**
     * Constructor
     *
     * @param controller
     */
    public InformationFrame(Controller controller) {
        super(controller);
    }

    /**
     * Returns the ui component.
     *
     * @return
     */
    public JFrame getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Builds the UI component.
     */
    private void buildUIComponent() {
        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("infoframe4.x", 50), prefs.getInt(
            "infoframe4.y", 50));

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("infoframe4.width", 500);
        int height = prefs.getInt("infoframe4.height", 600);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        if (prefs.getBoolean("infoframe4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = uiComponent.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
            }
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // everything is decided in window listener
        uiComponent.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    /**
     * Initializes the components.
     */
    private void initialize() {
        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.POWERFOLDER_IMAGE);
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {
        Preferences prefs = getController().getPreferences();
        if (uiComponent == null) {
            return;
        }
        if ((uiComponent.getExtendedState() &
                Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            prefs.putBoolean("infoframe4.maximized", true);
        } else {
            prefs.putInt("infoframe4.x", uiComponent.getX());
            if (uiComponent.getWidth() > 0) {
                prefs.putInt("infoframe4.width", uiComponent.getWidth());
            }
            prefs.putInt("infoframe4.y", uiComponent.getY());
            if (uiComponent.getHeight() > 0) {
                prefs.putInt("infoframe4.height", uiComponent.getHeight());
            }
            prefs.putBoolean("infoframe4.maximized", false);
        }
    }

    /**
     * Displays file info for a folder.
     *
     * @param folderInfo
     */
    public void displayFolderFiles(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfo(folderInfo);
        folderInformationCard.showFiles();
        displayCard(folderInformationCard);
    }

    /**
     * Displays settings info for a folder
     *
     * @param folderInfo
     */
    public void displayFolderSettings(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfo(folderInfo);
        folderInformationCard.showSettings();
        displayCard(folderInformationCard);
    }

    /**
     * Displays folder member info
     *
     * @param folderInfo
     */
    public void displayFolderMembers(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfo(folderInfo);
        folderInformationCard.showMembers();
        displayCard(folderInformationCard);
    }

    public void displayDownloads() {
        buildDownloadsInformationCard();
        displayCard(downloadsInformationCard);
    }

    public void displayUploads() {
        buildUploadsInformationCard();
        displayCard(uploadsInformationCard);
    }

    /**
     * Builds the local FolderInformationCard if required.
     */
    private void buildFolderInformationCard() {
        if (folderInformationCard == null) {
            folderInformationCard = new FolderInformationCard(getController());
        }
    }

    /**
     * Builds the local FolderInformationCard if required.
     */
    private void buildDownloadsInformationCard() {
        if (downloadsInformationCard == null) {
            downloadsInformationCard = new DownloadsInformationCard(getController());
        }
    }

    /**
     * Builds the local FolderInformationCard if required.
     */
    private void buildUploadsInformationCard() {
        if (uploadsInformationCard == null) {
            uploadsInformationCard = new UploadsInformationCard(getController());
        }
    }

    /**
     * Displays a card with tile and icon.
     * 
     * @param card
     */
    private void displayCard(InformationCard card) {
        getUIComponent().setIconImage(card.getCardImage());
        getUIComponent().setTitle(card.getCardTitle());
        getUIComponent().getContentPane().removeAll();
        getUIComponent().getContentPane().add(card.getUIComponent());
    }
}
