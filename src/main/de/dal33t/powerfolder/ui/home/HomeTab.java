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

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Font;
import java.text.DecimalFormat;

/**
 * Class for the Home tab in the main tab area of the UI.
 */
public class HomeTab extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel synchronizationStatusLabel;
    private JLabel numberOfFoldersLabel;
    private JLabel sizeOfFoldersLabel;
    private JLabel sizeOfFoldersDescriptionLabel;
    private JLabel filesAvailableLabel;
    private JLabel downloadsLabel;
    private JLabel uploadsLabel;
    private final ValueModel downloadsCountVM;
    private final ValueModel uploadsCountVM;
    private final MyFolderListener folderListener;

    /**
     * Constructor
     *
     * @param controller
     */
    public HomeTab(Controller controller) {
        super(controller);
        downloadsCountVM = controller.getUIController()
                .getTransferManagerModel().getCompletedDownloadsCountVM();
        uploadsCountVM = controller.getUIController()
                .getTransferManagerModel().getCompletedUploadsCountVM();
        folderListener = new MyFolderListener();
    }

    /**
     * Returns the UI component after optionally building it.
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * One-off build of UI component.
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref, 3dlu, fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        builder.add(toolbar, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 2));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xy(1, 4));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        synchronizationStatusLabel = new JLabel();
        numberOfFoldersLabel = new JLabel();
        sizeOfFoldersLabel = new JLabel();
        sizeOfFoldersDescriptionLabel = new JLabel();
        filesAvailableLabel = new JLabel();
        downloadsLabel = new JLabel();
        uploadsLabel = new JLabel();
        updateTransferText();
        updateFoldersText();
        recalculateFilesAvailable();
        registerListeners();
    }

    /**
     * Register any listeners.
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
    }

    /**
     * Build the main panel with all the detail lines.
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("3dlu, right:pref, 3dlu, pref:grow, 3dlu",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(synchronizationStatusLabel, cc.xyw(2, row, 3));
        row += 2;

        builder.addSeparator(null, cc.xyw(2, row, 3));
        row +=2;

        JLabel youHaveLabel = new JLabel(Translation.getTranslation("home_tab.you_have"));
        Font f = youHaveLabel.getFont();
        youHaveLabel.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        builder.add(youHaveLabel, cc.xyw(2, row, 3));
        row +=2;

        builder.add(filesAvailableLabel, cc.xy(2, row));
        builder.add(new JLabel(Translation.getTranslation("home_tab.files_available")),
                cc.xy(4, row));
        row += 2;

        builder.add(downloadsLabel, cc.xy(2, row));
        builder.add(new JLabel(Translation.getTranslation("home_tab.files_downloaded")),
                cc.xy(4, row));
        row += 2;

        builder.add(uploadsLabel, cc.xy(2, row));
        builder.add(new JLabel(Translation.getTranslation("home_tab.files_uploaded")),
                cc.xy(4, row));
        row += 2;

        builder.addSeparator(null, cc.xyw(2, row, 3));
        row +=2;

        builder.add(numberOfFoldersLabel, cc.xy(2, row));
        builder.add(new JLabel(Translation.getTranslation("home_tab.folders")),
                cc.xy(4, row));
        row += 2;

        builder.add(sizeOfFoldersLabel, cc.xy(2, row));
        builder.add(sizeOfFoldersDescriptionLabel, cc.xy(4, row));
        row += 2;

        return builder.getPanel();
    }

    /**
     * Updates the text for the number and size of the folders.
     */
    private void updateFoldersText() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        int numberOfFolder = folders.length;
        numberOfFoldersLabel.setText(String.valueOf(numberOfFolder));
        long totalSize = 0;
        for (Folder folder : folders) {
            totalSize += folder.getStatistic().getTotalSize();
        }
        String descriptionKey = "home_tab.total_bytes";
        long divisor = 1;
        if (totalSize >= 1024) {
            divisor *= 1024;
            descriptionKey = "home_tab.total_kilobytes";
        }
        if (totalSize / divisor >= 1024) {
            divisor *= 1024;
            descriptionKey = "home_tab.total_megabytes";
        }
        if (totalSize / divisor >= 1024) {
            divisor *= 1024;
            descriptionKey = "home_tab.total_gigabytes";
        }
        String num;
        if (divisor == 1) {
            num = String.valueOf(totalSize);
        } else {
            DecimalFormat numberFormat = Format.getNumberFormat();
            num = numberFormat.format((double) totalSize / (double) divisor);
        }

        sizeOfFoldersLabel.setText(num);
        sizeOfFoldersDescriptionLabel.setText(
                Translation.getTranslation(descriptionKey));
    }

    /**
     * Updates the upload / download text.
     */
    private void updateTransferText() {
        synchronizationStatusLabel.setText(getSyncText());
        downloadsLabel.setText(String.valueOf(downloadsCountVM.getValue()));
        uploadsLabel.setText(String.valueOf(uploadsCountVM.getValue()));
    }

    /**
     * Updates the synchronization text.
     * @return
     */
    private String getSyncText() {
        return getController().getFolderRepository()
                .isAnyFolderTransferring() ?
                Translation.getTranslation("home_tab.synchronizing") :
                Translation.getTranslation("home_tab.in_sync");
    }

    /**
     * Cretes the toolbar.
     *
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

    /**
     * Sums the number of incoming files in all folders.
     */
    private void recalculateFilesAvailable() {
        Folder[] folders = getController().getFolderRepository().getFolders();
        long count = 0;
        for (Folder folder : folders) {
            count += folder.getStatistic().getIncomingFilesCount();
            logFine("Folder: " + folder.getName() + ", incoming: " +
                    folder.getStatistic().getIncomingFilesCount());
        }
        filesAvailableLabel.setText(String.valueOf(count));
    }

    /**
     * Listener for folder events.
     */
    private class MyFolderListener implements FolderListener {

        public void fileChanged(FolderEvent folderEvent) {
        }

        public void filesDeleted(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            recalculateFilesAvailable();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

    /**
     * Listener for folder repo events.
     */
    private class MyFolderRepositoryListener
            implements FolderRepositoryListener {
        public boolean fireInEventDispathThread() {
            return true;
        }

        public void folderCreated(FolderRepositoryEvent e) {
            e.getFolder().addFolderListener(folderListener);
            updateFoldersText();
            logFine("Added to folder listeners: " + e.getFolder().getName());
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            e.getFolder().removeFolderListener(folderListener);
            updateFoldersText();
            logFine("Removed from folder listeners: " + e.getFolder().getName());
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateFoldersText();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateFoldersText();
        }
    }

    /**
     * Listener for transfer events.
     */
    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateTransferText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateTransferText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateTransferText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateTransferText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

        public void completedUploadRemoved(TransferManagerEvent event) {
            updateTransferText();
        }

    }

}
