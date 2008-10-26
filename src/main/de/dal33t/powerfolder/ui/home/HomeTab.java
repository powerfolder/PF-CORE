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
 * $Id: HomeTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.home;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HomeTab extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel synchronizationStatusLabel;
    private JLabel numberOfFoldersLabel;

    public HomeTab(Controller controller) {
        super(controller);
    }

    private void updateFoldersText() {
        int numberOfFolder = getController().getFolderRepository().getFolders().length;
        numberOfFoldersLabel.setText(String.valueOf(numberOfFolder));
    }

    private void updateSyncText() {
        synchronizationStatusLabel.setText(getSyncText());
    }

    private String getSyncText() {
        return getController().getFolderRepository()
                .isAnyFolderTransferring() ?
                Translation.getTranslation("home_tab.synchronizing") :
                Translation.getTranslation("home_tab.in_sync");
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    private void buildUI() {
        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, 3dlu, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 2));
        JPanel mainPanel = buildMainPanel();
        builder.add(mainPanel, cc.xy(1, 4));
        uiComponent = builder.getPanel();
    }

    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("3dlu, right:pref, 3dlu, pref:grow, 3dlu",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(synchronizationStatusLabel, cc.xyw(2, row, 3));
        row += 2;

        builder.addSeparator(null, cc.xyw(2, row, 3));
        row +=2;

        builder.add(new JLabel(Translation.getTranslation("home_tab.you_have")),
                cc.xyw(2, row, 3));
        row +=2;

        builder.add(numberOfFoldersLabel, cc.xy(2, row));
        builder.add(new JLabel(Translation.getTranslation("home_tab.folders")),
                cc.xy(4, row));
        row += 2;

        return builder.getPanel();
    }

    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        numberOfFoldersLabel = new JLabel();
        updateSyncText();
        updateFoldersText();
        registerListeners();
    }

    private void registerListeners() {
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
    }


    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        JButton newFolderButton = new JButton(getUIController().getActionModel()
                .getNewFolderAction());
        JButton searchComputerButton = new JButton(getUIController().getActionModel()
                .getSearchComputerAction());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(newFolderButton);
        bar.addRelatedGap();
        bar.addGridded(searchComputerButton);

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    private class MyFolderRepositoryListener
            implements FolderRepositoryListener {
        public boolean fireInEventDispathThread() {
            return true;
        }

        public void folderCreated(FolderRepositoryEvent e) {
            updateFoldersText();
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            updateFoldersText();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateSyncText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateSyncText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateSyncText();
        }

    }



}
