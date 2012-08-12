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

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowAdapter;
import java.awt.*;
import java.util.prefs.Preferences;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.UIConstants;
import de.dal33t.powerfolder.ui.information.debug.DebugInformationCard;
import de.dal33t.powerfolder.ui.information.downloads.DownloadsInformationCard;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationCard;
import de.dal33t.powerfolder.ui.information.notices.NoticesInformationCard;
import de.dal33t.powerfolder.ui.information.uploads.UploadsInformationCard;

/**
 * The information window.
 */
public class InformationFrame extends PFUIComponent {

    private static final String INFOCARD = "infocard.";
    private static final String INFOCARD_X = ".x";
    private static final String INFOCARD_Y = ".y";
    private static final String INFOCARD_WIDTH = ".width";
    private static final String INFOCARD_HEIGHT = ".height";
    private static final String INFOCARD_SET = ".set";
    private static final String INFOCARD_MAXIMIZED = ".maximized";

    private JFrame uiComponent;

    private FolderInformationCard folderInformationCard;
    private DownloadsInformationCard downloadsInformationCard;
    private UploadsInformationCard uploadsInformationCard;
    private DebugInformationCard debugInformationCard;
    private NoticesInformationCard noticesCard;

    private boolean showingFolder;

    private FolderInfo currentFolderInfo;

    private InformationCardType informationCardType = InformationCardType.NONE;

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

    public InformationCardType getInformationCardType() {
        return informationCardType;
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
        uiComponent.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                getUIController().setActiveFrame(UIController.INFO_FRAME_ID);
            }

            public void windowLostFocus(WindowEvent e) {
                storeLocationSize();
            }
        });

        uiComponent.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                storeLocationSize();
            }
        });

        uiComponent.setIconImage(Icons.getImageById(Icons.SMALL_LOGO));
    }

    /**
     * Remember where the location / size for this type for next time.
     */
    private void storeLocationSize() {

        if (informationCardType == null ||
                informationCardType == InformationCardType.NONE) {
            return;
        }

        String propertyPrefix = INFOCARD +
                informationCardType.name().toLowerCase();
        Preferences preferences = getController().getPreferences();

        if (getUIComponent().getExtendedState() == Frame.NORMAL) {
            preferences.putInt(propertyPrefix + INFOCARD_X,
                    getUIComponent().getX());
            preferences.putInt(propertyPrefix + INFOCARD_Y,
                    getUIComponent().getY());
            preferences.putInt(propertyPrefix + INFOCARD_WIDTH,
                    getUIComponent().getWidth());
            preferences.putInt(propertyPrefix + INFOCARD_HEIGHT,
                    getUIComponent().getHeight());
        }

        preferences.putBoolean(propertyPrefix + INFOCARD_MAXIMIZED,
        (getUIComponent().getExtendedState() &
                Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);

    }

    public boolean isShowingFolder() {
        return showingFolder;
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
        informationCardType = InformationCardType.TRANSFERS;
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
        informationCardType = card.getInformationCardType();
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

    /**
     * Depending on the card being displayed, locate the frame to the last
     * known position / size.
     */
    public void relocate() {

        getUIComponent().setExtendedState(Frame.NORMAL);

        String propertyPrefix = INFOCARD +
                informationCardType.name().toLowerCase();
        Preferences preferences = getController().getPreferences();
        boolean set = preferences.getBoolean(propertyPrefix + INFOCARD_SET,
                false);

        if (set) {
            if (preferences.getBoolean(propertyPrefix + INFOCARD_MAXIMIZED,
                    UIConstants.DEFAULT_FRAME_MAXIMIZED)) {
                getUIComponent().setExtendedState(Frame.MAXIMIZED_BOTH);
            } else {
                getUIComponent().setExtendedState(Frame.NORMAL);
                int x = preferences.getInt(propertyPrefix + INFOCARD_X,
                        UIConstants.DEFAULT_FRAME_X);
                int y = preferences.getInt(propertyPrefix + INFOCARD_Y,
                        UIConstants.DEFAULT_FRAME_Y);
                int w = preferences.getInt(propertyPrefix + INFOCARD_WIDTH,
                        UIConstants.DEFAULT_FRAME_WIDTH);
                int h = preferences.getInt(propertyPrefix + INFOCARD_HEIGHT,
                        UIConstants.DEFAULT_FRAME_HEIGHT);
                getUIComponent().setLocation(x, y);
                getUIComponent().setSize(w, h);
            }
        } else {

            // First time displayed for this card type, use sensible defaults.
            getUIComponent().setExtendedState(Frame.NORMAL);
            getUIComponent().pack();

            if (getUIComponent().getHeight() <
                    UIConstants.DEFAULT_FRAME_HEIGHT) {
                getUIComponent().setSize(getUIComponent().getWidth(),
                        UIConstants.DEFAULT_FRAME_HEIGHT);
            }
            if (getUIComponent().getWidth() <
                    UIConstants.DEFAULT_FRAME_WIDTH) {
                getUIComponent().setSize(UIConstants.DEFAULT_FRAME_WIDTH,
                        getUIComponent().getHeight());
            }
            
            getUIComponent().setLocation(UIConstants.DEFAULT_FRAME_X,
                    UIConstants.DEFAULT_FRAME_Y);

            preferences.putBoolean(propertyPrefix + INFOCARD_SET, true);

            UIUtil.putOnScreen(getUIComponent());
        }

    }

    // /////////////////
    // Inner classes //
    // /////////////////

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

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
