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

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.information.debug.DebugInformationCard;
import de.dal33t.powerfolder.ui.information.downloads.DownloadsInformationCard;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationCard;
import de.dal33t.powerfolder.ui.information.notices.NoticesInformationCard;
import de.dal33t.powerfolder.ui.information.uploads.UploadsInformationCard;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Translation;

/**
 * The information window.
 */
public class InformationFrame extends PFUIComponent {

    private JFrame uiComponent;

    private FolderInformationCard folderInformationCard;
    private DownloadsInformationCard downloadsInformationCard;
    private UploadsInformationCard uploadsInformationCard;
    private DebugInformationCard debugInformationCard;
    private NoticesInformationCard noticesCard;

    private boolean showingFolder;

    private FolderInfo currentFolderInfo;

    /**
     * Constructor
     * 
     * @param controller
     */
    public InformationFrame(Controller controller) {
        super(controller);
        controller.getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
    }

    public JFrame getUIComponent() {
        if (uiComponent == null) {
            initialize();
        }
        return uiComponent;
    }

    /**
     * Initializes the components.
     */
    private void initialize() {
        uiComponent = new JFrame();
        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));
    }

    public boolean isShowingFolder() {
        return showingFolder;
    }

    public void displayFile(FileInfo fileInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFileInfo(fileInfo);
        folderInformationCard.showFiles();
        displayCard(folderInformationCard);
        showingFolder = true;
        currentFolderInfo = fileInfo.getFolderInfo();
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
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    public void displayFolderFilesDeleted(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfoDeleted(folderInfo);
        folderInformationCard.showFiles();
        displayCard(folderInformationCard);
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    public void displayFolderFilesUnsynced(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfoUnsynced(folderInfo);
        folderInformationCard.showFiles();
        displayCard(folderInformationCard);
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    /**
     * Displays file info for a folder with filter set to new and sort set to
     * date descending.
     * 
     * @param folderInfo
     */
    public void displayFolderFilesLatest(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfoLatest(folderInfo);
        folderInformationCard.showFiles();
        displayCard(folderInformationCard);
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    /**
     * Displays file info for a folder with filter set to incoming.
     * 
     * @param folderInfo
     */
//    public void displayFolderFilesIncoming(FolderInfo folderInfo) {
//        buildFolderInformationCard();
//        folderInformationCard.setFolderInfoIncoming(folderInfo);
//        folderInformationCard.showFiles();
//        displayCard(folderInformationCard);
//        showingFolder = true;
//        currentFolderInfo = folderInfo;
//    }

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
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    public void moveLocalFolder(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfo(folderInfo);
        folderInformationCard.moveLocalFolder();
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
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    /**
     * Displays folder problems
     * 
     * @param folderInfo
     */
    public void displayFolderProblems(FolderInfo folderInfo) {
        buildFolderInformationCard();
        folderInformationCard.setFolderInfo(folderInfo);
        folderInformationCard.showProblems();
        displayCard(folderInformationCard);
        showingFolder = true;
        currentFolderInfo = folderInfo;
    }

    /**
     * Display downloads and uploads in two tabs.
     */
    public void displayTransfers() {
        buildDownloadsInformationCard();
        buildUploadsInformationCard();
        getUIComponent().getContentPane().removeAll();
        getUIComponent().setTitle(
                Translation.getTranslation("information_frame.transfers.text"));
        getUIComponent().setIconImage(Icons.getImageById(Icons.TRANSFERS));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(downloadsInformationCard.getCardTitle(),
                downloadsInformationCard.getUIComponent());
        tabbedPane.add(uploadsInformationCard.getCardTitle(),
                uploadsInformationCard.getUIComponent());
        getUIComponent().getContentPane().add(tabbedPane);
        showingFolder = false;
    }

    public void displayDebug() {
        buildDebugInformationCard();
        displayCard(debugInformationCard);
        showingFolder = false;
    }

    public void displayNotices() {
        buildNoticesCard();
        displayCard(noticesCard);
        showingFolder = false;
    }

    /**
     * Displays a card with tile and icon.
     * 
     * @param card
     */
    public void displayCard(InformationCard card) {
        getUIComponent().setIconImage(card.getCardImage());
        getUIComponent().setTitle(card.getCardTitle());
        getUIComponent().getContentPane().removeAll();
        getUIComponent().getContentPane().add(card.getUIComponent());
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
            downloadsInformationCard = new DownloadsInformationCard(
                getController());
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
     * Builds the local DebugInformationCard if required.
     */
    private void buildDebugInformationCard() {
        if (debugInformationCard == null) {
            debugInformationCard = new DebugInformationCard(getController());
        }
    }

    /**
     * Builds the local NoticesCard if required.
     */
    private void buildNoticesCard() {
        if (noticesCard == null) {
            noticesCard = new NoticesInformationCard(getController());
        }
    }

    /**
     * Fires when a folder is removed. Hide this if showing the folder.
     * 
     * @param folderInfo
     */
    private void removedFolder(FolderInfo folderInfo) {
        if (showingFolder && currentFolderInfo != null
            && currentFolderInfo.equals(folderInfo)) {
            getUIComponent().setVisible(false);
        }
    }

    // /////////////////
    // Inner classes //
    // /////////////////

    private class MyFolderRepositoryListener
            implements FolderRepositoryListener {

        public void folderRemoved(FolderRepositoryEvent e) {
            removedFolder(e.getFolderInfo());
        }

        public void folderCreated(FolderRepositoryEvent e) {
            if (getController().isUIOpen() && showingFolder) {
                displayFolderFiles(e.getFolderInfo());
            }
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            // Don't care.
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            // Don't care.
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
